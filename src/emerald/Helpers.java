package emerald;

import sev7n.lang.Interpreter;
import sev7n.lang.runtime.SevenCallable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Things every game needs, built in so nobody has to write them again:
 *
 *   burst(x, y, [n], [colour], [speed])   a puff of particles (drawn and moved for you)
 *   shake([amount])                       shake the screen (it settles by itself)
 *   say(text)                             a dialogue box at the bottom, typed out letter by letter;
 *                                         Z / X / click to close it (or skip the typing). saying() is it open?
 *   after(seconds, func)                  call a function once, later
 *   every(seconds, func)                  call a function again and again (stoptimers() stops them all)
 */
final class Helpers {

    private static final class Particle {
        double x, y, vx, vy, life, maxLife;
        int colour;
    }

    private static final class Timer {
        double left, every;   // every = 0: once
        SevenCallable function;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final List<Timer> timers = new ArrayList<>();
    private final Random random = new Random();
    private double shake;
    private String dialogue;          // null: no box
    private double typed;             // how many letters are showing

    void reset() {
        particles.clear();
        timers.clear();
        shake = 0;
        dialogue = null;
    }

    void install(Interpreter seven, Console console) {
        seven.defineNative("burst", 2, 5, a -> {
            int n = a.has(2) ? Math.min(500, Math.max(1, a.integer(2))) : 10;
            int colour = a.has(3) ? a.integer(3) & 15 : 7;
            double speed = a.has(4) ? a.number(4) : 1;
            for (int i = 0; i < n; i++) {
                Particle p = new Particle();
                double angle = random.nextDouble() * Math.PI * 2, s = (0.3 + random.nextDouble()) * speed;
                p.x = a.number(0);
                p.y = a.number(1);
                p.vx = Math.cos(angle) * s;
                p.vy = Math.sin(angle) * s - 0.5 * speed;
                p.maxLife = p.life = 15 + random.nextInt(20);
                p.colour = colour;
                particles.add(p);
            }
            return null;
        });
        seven.defineNative("shake", 0, 1, a -> {
            shake = Math.max(shake, a.has(0) ? a.number(0) : 3);
            return null;
        });
        seven.defineNative("say", 1, 1, a -> {
            dialogue = sev7n.lang.runtime.Values.stringify(a.get(0));
            typed = 0;
            return null;
        });
        seven.defineNative("saying", 0, 0, a -> dialogue != null);
        seven.defineNative("after", 2, 2, a -> {
            Timer t = new Timer();
            t.left = a.number(0);
            t.function = a.function(1);
            timers.add(t);
            return null;
        });
        seven.defineNative("every", 2, 2, a -> {
            Timer t = new Timer();
            t.left = t.every = Math.max(1.0 / 60, a.number(0));
            t.function = a.function(1);
            timers.add(t);
            return null;
        });
        seven.defineNative("stoptimers", 0, 0, a -> {
            timers.clear();
            return null;
        });
    }

    /** Before the game's update: timers fire, and the dialogue box reacts to buttons. */
    void update(Interpreter seven, Input input) {
        double dt = 1.0 / Console.FPS;
        for (Timer t : new ArrayList<>(timers)) {
            t.left -= dt;
            if (t.left > 0) continue;
            if (t.every > 0) t.left += t.every;
            else timers.remove(t);
            seven.callFunction(t.function);
        }
        if (dialogue != null) {
            typed += 0.6;
            boolean next = input.button(4, true) || input.button(5, true) || input.mouseClicked();
            if (next) {
                if (typed < dialogue.length()) typed = dialogue.length(); // first press: show it all
                else dialogue = null;
            }
        }
        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.06;
            p.vx *= 0.97;
            p.life--;
        }
        particles.removeIf(p -> p.life <= 0);
    }

    /** The shake for this frame (it fades), as an offset for the screen. */
    void applyShake(Screen.Pen pen) {
        if (shake > 0.1) {
            pen.shakeX = (int) Math.round((random.nextDouble() * 2 - 1) * shake);
            pen.shakeY = (int) Math.round((random.nextDouble() * 2 - 1) * shake);
            shake *= 0.85;
        } else {
            pen.shakeX = pen.shakeY = 0;
            shake = 0;
        }
    }

    /** After the game's draw: particles (through the game's camera), then the dialogue box on top. */
    void draw(Screen s, PixelFont font) {
        for (Particle p : particles) {
            s.pset((int) Math.round(p.x), (int) Math.round(p.y), p.colour);
            if (p.life > p.maxLife * 0.6) s.pset((int) Math.round(p.x) + 1, (int) Math.round(p.y), p.colour); // bigger when new
        }
        if (dialogue == null) return;
        Screen.Pen pen = s.pen;
        int camX = pen.camX, camY = pen.camY, sx = pen.shakeX, sy = pen.shakeY;
        pen.camX = pen.camY = pen.shakeX = pen.shakeY = 0; // the box stays put on the screen
        pen.camX = -(s.width - Screen.SIZE) / 2;           // (in the middle of a wide screen)
        s.rectfill(3, 92, 124, 124, 0);
        s.rect(3, 92, 124, 124, 7);
        String shown = dialogue.substring(0, (int) Math.min(dialogue.length(), typed));
        int y = 96;
        StringBuilder line = new StringBuilder();
        for (String word : shown.split(" ", -1)) {
            if (line.length() + word.length() + 1 > 29) {
                font.draw(s, line.toString(), 6, y, 7);
                y += PixelFont.LINE;
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (y <= 114) font.draw(s, line.toString(), 6, y, 7);
        if (typed >= dialogue.length() && (System.nanoTime() / 300_000_000L) % 2 == 0) s.rectfill(118, 119, 120, 121, 11); // "press on"
        pen.camX = camX;
        pen.camY = camY;
        pen.shakeX = sx;
        pen.shakeY = sy;
    }
}
