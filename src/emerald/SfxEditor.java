package emerald;

import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The SFX editor: paint a sound's 32 notes with the mouse.
 *
 *   tall area    left mouse sets notes' pitch (higher up = higher note) with the chosen instrument
 *                and effect; right mouse silences them
 *   low strip    each note's volume
 *   buttons      which sfx (< >), speed (- +), instrument, effect, loop, PLAY (or Space)
 *   Ctrl+Z / Y   undo and redo
 */
final class SfxEditor {

    private static final int GRAPH_TOP = 28, GRAPH_BOTTOM = 91;   // pitch 63 at the top, 0 at the bottom
    private static final int VOLUME_TOP = 96, VOLUME_BOTTOM = 110;
    private static final int COLUMN = 4;

    private final Audio audio;
    private Cart cart;
    int current;                       // which sfx (0..63)
    private int wave = 1;              // the instrument new notes get
    private int effect;                // the effect new notes get
    private boolean dirty;
    private boolean stroking;
    private final Deque<String> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    String hover = "";
    boolean overButton;

    SfxEditor(Audio audio) {
        this.audio = audio;
    }

    void setCart(Cart cart) {
        this.cart = cart;
        undo.clear();
        redo.clear();
        dirty = false;
    }

    boolean dirty() {
        return dirty;
    }

    void saved() {
        dirty = false;
    }

    private Sound.Sfx sfx() {
        return cart.sfx[current];
    }

    // ---------------------------------------------------------------- input

    void update(Input input) {
        hover = "";
        overButton = false;
        int mx = input.mouseX, my = input.mouseY;
        for (Input.KeyPress k : input.presses()) {
            if (k.ctrl() && k.code() == KeyEvent.VK_Z) undoRedo(k.shift() ? redo : undo, k.shift() ? undo : redo);
            if (k.ctrl() && k.code() == KeyEvent.VK_Y) undoRedo(redo, undo);
            if (!k.ctrl() && k.code() == KeyEvent.VK_SPACE) togglePlay();
        }
        if (!input.mouseHeld() && !input.rightHeld()) stroking = false;
        Sound.Sfx s = sfx();

        // the note graph
        if (mx >= 0 && mx < 128 && my >= GRAPH_TOP - 2 && my <= GRAPH_BOTTOM + 2) {
            int note = Math.min(Sound.NOTES - 1, mx / COLUMN);
            int pitch = Math.max(0, Math.min(63, GRAPH_BOTTOM - my));
            hover = "NOTE " + note + "  " + noteName(pitch);
            if (input.mouseHeld() || input.rightHeld()) {
                if (!stroking) remember();
                stroking = true;
                if (input.rightHeld()) s.volume[note] = 0;
                else {
                    s.pitch[note] = pitch;
                    s.wave[note] = wave;
                    s.effect[note] = effect;
                    if (s.volume[note] == 0) s.volume[note] = 5;
                }
                dirty = true;
            }
        }
        // the volume strip
        if (mx >= 0 && mx < 128 && my >= VOLUME_TOP - 1 && my <= VOLUME_BOTTOM + 1) {
            int note = Math.min(Sound.NOTES - 1, mx / COLUMN);
            int volume = Math.max(0, Math.min(7, (VOLUME_BOTTOM - my + 1) / 2));
            hover = "NOTE " + note + "  VOLUME " + volume;
            if (input.mouseHeld()) {
                if (!stroking) remember();
                stroking = true;
                s.volume[note] = volume;
                dirty = true;
            }
        }
        // the buttons
        button(input, 2, 10, "<", () -> current = (current + Sound.SFX_COUNT - 1) % Sound.SFX_COUNT);
        button(input, 38, 10, ">", () -> current = (current + 1) % Sound.SFX_COUNT);
        button(input, 64, 10, "-", () -> change(() -> s.speed = Math.max(1, s.speed - 1)));
        button(input, 84, 10, "+", () -> change(() -> s.speed = Math.min(64, s.speed + 1)));
        button(input, 104, 10, audio.playingNote(current) >= 0 ? "STOP" : "PLAY", this::togglePlay);
        for (int w = 0; w < Sound.WAVES.length; w++) {
            final int which = w;
            if (button(input, 2 + w * 12, 19, Sound.WAVE_SHORT[w], () -> wave = which)) hover = Sound.WAVES[w];
        }
        if (button(input, 76, 19, "FX " + Sound.EFFECTS[effect], () -> effect = (effect + 1) % Sound.EFFECTS.length)) {
            hover = "CLICK: NEXT EFFECT";
        }
        button(input, 22, 113, "-", () -> change(() -> s.loopStart = Math.max(0, s.loopStart - 1)));
        button(input, 36, 113, "+", () -> change(() -> s.loopStart = Math.min(Sound.NOTES, s.loopStart + 1)));
        button(input, 54, 113, "-", () -> change(() -> s.loopEnd = Math.max(0, s.loopEnd - 1)));
        button(input, 68, 113, "+", () -> change(() -> s.loopEnd = Math.min(Sound.NOTES, s.loopEnd + 1)));
        button(input, 104, 113, "CLEAR", () -> change(() -> {
            for (int i = 0; i < Sound.NOTES; i++) s.volume[i] = 0;
        }));
        if (input.wheel() != 0 && my >= 9 && my < 17) current = Math.floorMod(current + input.wheel(), Sound.SFX_COUNT);
    }

    private void togglePlay() {
        if (audio.playingNote(current) >= 0) audio.sfx(-1, -1);
        else audio.sfx(current, -1);
    }

    private void change(Runnable edit) {
        remember();
        edit.run();
        dirty = true;
    }

    /** A button (label in a box). Returns true while the mouse is over it. */
    private boolean button(Input input, int x, int y, String label, Runnable action) {
        int w = label.length() * PixelFont.ADVANCE + 3;
        boolean over = input.mouseX >= x && input.mouseY >= y && input.mouseX < x + w && input.mouseY < y + 7;
        if (over) {
            overButton = true;
            if (input.mouseClicked()) action.run();
        }
        return over;
    }

    private void remember() {
        undo.push(current + ":" + sfx().encode());
        if (undo.size() > 100) undo.removeLast();
        redo.clear();
    }

    private void undoRedo(Deque<String> from, Deque<String> to) {
        if (from.isEmpty()) return;
        String step = from.pop();
        int which = Integer.parseInt(step.substring(0, step.indexOf(':')));
        to.push(which + ":" + cart.sfx[which].encode());
        cart.sfx[which] = Sound.Sfx.decode(step.substring(step.indexOf(':') + 1));
        current = which;
        dirty = true;
    }

    static String noteName(int pitch) {
        return Sound.NOTE_NAMES[pitch % 12] + (pitch / 12 + 2);
    }

    // ---------------------------------------------------------------- drawing

    void draw(Screen s, PixelFont font) {
        Sound.Sfx sfx = sfx();
        s.rectfill(0, 8, 127, 120, 1);
        // the top rows: which sfx, speed, play; instruments and effect
        label(s, font, 2, 10, "<", false);
        font.draw(s, String.format("SFX %02d", current), 11, 11, 7);
        label(s, font, 38, 10, ">", false);
        font.draw(s, "SPD", 48, 11, 6);
        label(s, font, 64, 10, "-", false);
        font.draw(s, String.format("%2d", sfx.speed), 72, 11, 7);
        label(s, font, 84, 10, "+", false);
        boolean playing = audio.playingNote(current) >= 0;
        label(s, font, 104, 10, playing ? "STOP" : "PLAY", playing);
        for (int w = 0; w < Sound.WAVES.length; w++) {
            int x = 2 + w * 12;
            s.rectfill(x, 19, x + 10, 25, w == wave ? Sound.WAVE_COLOURS[w] : 0);
            font.draw(s, Sound.WAVE_SHORT[w], x + 2, 20, w == wave ? 1 : Sound.WAVE_COLOURS[w]);
        }
        label(s, font, 76, 19, "FX " + Sound.EFFECTS[effect], effect != 0);

        // the graph: octave lines, the loop, then a bar for every note that sounds
        s.rectfill(0, GRAPH_TOP, 127, GRAPH_BOTTOM, 0);
        for (int octave = 12; octave < 64; octave += 12) s.line(0, GRAPH_BOTTOM - octave, 127, GRAPH_BOTTOM - octave, 1);
        if (sfx.loops()) {
            int from = sfx.loopStart * COLUMN, to = sfx.loopEnd * COLUMN - 1;
            s.line(from, GRAPH_TOP, from, GRAPH_BOTTOM, 3);
            s.line(to, GRAPH_TOP, to, GRAPH_BOTTOM, 3);
        }
        int playingNote = audio.playingNote(current);
        for (int i = 0; i < Sound.NOTES; i++) {
            int x = i * COLUMN;
            if (i == playingNote) s.rectfill(x, GRAPH_TOP, x + COLUMN - 2, GRAPH_BOTTOM, 1);
            if (sfx.volume[i] == 0) continue;
            int top = GRAPH_BOTTOM - sfx.pitch[i];
            int colour = Sound.WAVE_COLOURS[sfx.wave[i]];
            s.rectfill(x, top, x + COLUMN - 2, GRAPH_BOTTOM, i == playingNote ? 7 : 5);
            s.rectfill(x, top, x + COLUMN - 2, top + 1, colour);
            if (sfx.effect[i] != 0) s.pset(x + 1, top - 2, 7); // a dot: this note has an effect
        }
        // volumes
        s.rectfill(0, VOLUME_TOP - 1, 127, VOLUME_BOTTOM + 1, 0);
        for (int i = 0; i < Sound.NOTES; i++) {
            int v = sfx.volume[i];
            if (v > 0) s.rectfill(i * COLUMN, VOLUME_BOTTOM - v * 2 + 1, i * COLUMN + COLUMN - 2, VOLUME_BOTTOM, 3);
        }
        // the loop and clear
        font.draw(s, "LOOP", 2, 114, 6);
        label(s, font, 22, 113, "-", false);
        font.draw(s, String.format("%02d", sfx.loopStart), 28, 114, 7);
        label(s, font, 36, 113, "+", false);
        font.draw(s, "TO", 44, 114, 6);
        label(s, font, 54, 113, "-", false);
        font.draw(s, String.format("%02d", sfx.loopEnd), 60, 114, 7);
        label(s, font, 68, 113, "+", false);
        label(s, font, 104, 113, "CLEAR", false);
    }

    private static void label(Screen s, PixelFont font, int x, int y, String text, boolean on) {
        int w = text.length() * PixelFont.ADVANCE + 3;
        s.rectfill(x, y, x + w - 1, y + 6, on ? 11 : 5);
        font.draw(s, text, x + 2, y + 1, on ? 1 : 7);
    }
}
