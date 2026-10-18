package emerald;

import sev7n.lang.Interpreter;
import sev7n.lang.runtime.Args;

import java.util.Random;

/**
 * Everything an Emerald game can call, on top of the Seven language's own functions:
 *
 *   cls([c])  pset(x, y, c)  pget(x, y)  line(x0, y0, x1, y1, [c])  rect(...)  rectfill(...)
 *   circ(x, y, r, [c])  circfill(...)  print(text, [x, y], [c])  color(c)  spr(...)  sget  sset
 *   map(...)  mget(x, y)  mset(x, y, n)  sspr(...)  camera(x, y)  pal(c0, c1)  palt(c, on)  fget  fset
 *   cartdata(name)  dget(slot)  dset(slot, n)
 *   btn(b)  btnp(b)  key("a")  keyp("space")  mousex()  mousey()  mousedown()  click()  cursor("hand")
 *   sfx(n, [channel])  music(n)
 *   rnd([n])  flr(x)  t()  log(...)
 *
 * Colours are 0..15 (see Screen.PALETTE). Leave a colour out and the last one is used.
 */
final class Api {

    private Api() { }

    static void install(Interpreter seven, Console console) {
        Screen s = console.screen;
        Random random = new Random();

        seven.defineNative("cls", 0, 1, a -> {
            s.cls(a.has(0) ? a.integer(0) : 0);
            console.printX = 0;
            console.printY = 0;
            return null;
        });
        seven.defineNative("color", 1, 1, a -> {
            s.color = a.integer(0) & 15;
            return null;
        });
        seven.defineNative("pset", 2, 3, a -> {
            s.pset(flr(a, 0), flr(a, 1), color(a, 2, s));
            return null;
        });
        seven.defineNative("pget", 2, 2, a -> (double) s.pget(flr(a, 0), flr(a, 1)));
        seven.defineNative("line", 4, 5, a -> {
            s.line(flr(a, 0), flr(a, 1), flr(a, 2), flr(a, 3), color(a, 4, s));
            return null;
        });
        seven.defineNative("rect", 4, 5, a -> {
            s.rect(flr(a, 0), flr(a, 1), flr(a, 2), flr(a, 3), color(a, 4, s));
            return null;
        });
        seven.defineNative("rectfill", 4, 5, a -> {
            s.rectfill(flr(a, 0), flr(a, 1), flr(a, 2), flr(a, 3), color(a, 4, s));
            return null;
        });
        seven.defineNative("circ", 3, 4, a -> {
            s.circ(flr(a, 0), flr(a, 1), flr(a, 2), color(a, 3, s));
            return null;
        });
        seven.defineNative("circfill", 3, 4, a -> {
            s.circfill(flr(a, 0), flr(a, 1), flr(a, 2), color(a, 3, s));
            return null;
        });
        // print(text): the next line, like a terminal · print(text, x, y, [c]): there
        seven.defineNative("print", 1, 4, a -> {
            String text = sev7n.lang.runtime.Values.stringify(a.get(0));
            int c = a.size() == 2 ? color(a, 1, s) : color(a, 3, s);
            if (a.size() >= 3) {
                console.font.draw(s, text, flr(a, 1), flr(a, 2), c);
            } else {
                console.font.draw(s, text, console.printX, console.printY, c);
                console.printY += PixelFont.LINE * (1 + (int) text.chars().filter(ch -> ch == '\n').count());
            }
            return null;
        });

        // sprites: spr(n, x, y, [w, h], [flipX], [flipY]) draws sprite n (w x h sprites big); colour 0 is see-through
        seven.defineNative("spr", 3, 7, a -> {
            int n = flr(a, 0), x = flr(a, 1), y = flr(a, 2);
            int w = a.has(3) ? Math.max(1, flr(a, 3)) : 1, h = a.has(4) ? Math.max(1, flr(a, 4)) : 1;
            boolean flipX = a.has(5) && sev7n.lang.runtime.Values.isTruthy(a.get(5));
            boolean flipY = a.has(6) && sev7n.lang.runtime.Values.isTruthy(a.get(6));
            int sx = (n & 255) % 16 * 8, sy = (n & 255) / 16 * 8, pw = w * 8, ph = h * 8;
            for (int py = 0; py < ph; py++) {
                for (int px = 0; px < pw; px++) {
                    int gx = sx + px, gy = sy + py;
                    if (gx >= Cart.SHEET || gy >= Cart.SHEET) continue;
                    int c = console.sheet[gy * Cart.SHEET + gx];
                    if (!s.pen.transparent[c]) s.pset(x + (flipX ? pw - 1 - px : px), y + (flipY ? ph - 1 - py : py), c);
                }
            }
            return null;
        });
        seven.defineNative("sget", 2, 2, a -> {
            int x = flr(a, 0), y = flr(a, 1);
            return x < 0 || y < 0 || x >= Cart.SHEET || y >= Cart.SHEET ? 0.0 : (double) console.sheet[y * Cart.SHEET + x];
        });
        seven.defineNative("sset", 3, 3, a -> {
            int x = flr(a, 0), y = flr(a, 1);
            if (x >= 0 && y >= 0 && x < Cart.SHEET && y < Cart.SHEET) console.sheet[y * Cart.SHEET + x] = (byte) (flr(a, 2) & 15);
            return null;
        });

        // the map: map([tileX, tileY], [screenX, screenY], [width, height]) draws part of it (16 x 16 tiles, one
        // screen, if left out); tile 0 is empty. mget(x, y) reads a tile, mset(x, y, n) changes one
        // sspr(sx, sy, sw, sh, dx, dy, [dw, dh], [flipX], [flipY]): any part of the sprite sheet, stretched to dw x dh
        seven.defineNative("sspr", 6, 10, a -> {
            int sx = flr(a, 0), sy = flr(a, 1), sw = flr(a, 2), sh = flr(a, 3), dx = flr(a, 4), dy = flr(a, 5);
            int dw = a.has(6) ? flr(a, 6) : sw, dh = a.has(7) ? flr(a, 7) : sh;
            boolean flipX = a.has(8) && sev7n.lang.runtime.Values.isTruthy(a.get(8));
            boolean flipY = a.has(9) && sev7n.lang.runtime.Values.isTruthy(a.get(9));
            if (sw <= 0 || sh <= 0 || dw == 0 || dh == 0) return null;
            for (int py = 0; py < Math.abs(dh); py++) {
                for (int px = 0; px < Math.abs(dw); px++) {
                    int u = px * sw / Math.abs(dw), v = py * sh / Math.abs(dh);
                    if (flipX) u = sw - 1 - u;
                    if (flipY) v = sh - 1 - v;
                    int gx = sx + u, gy = sy + v;
                    if (gx < 0 || gy < 0 || gx >= Cart.SHEET || gy >= Cart.SHEET) continue;
                    int c = console.sheet[gy * Cart.SHEET + gx];
                    if (!s.pen.transparent[c]) s.pset(dx + px, dy + py, c);
                }
            }
            return null;
        });

        // the camera: camera(x, y) shifts everything drawn after it (draw the world at its real place, and
        // the camera follows the player); camera() puts it back
        // widescreen(true): the screen becomes 228 x 128 (16:9), filling wide monitors; screenwidth() says how wide
        seven.defineNative("widescreen", 0, 1, a -> {
            console.gameWidth = !a.has(0) || sev7n.lang.runtime.Values.isTruthy(a.get(0)) ? Screen.WIDE : Screen.SIZE;
            s.width = console.gameWidth;
            return (double) console.gameWidth;
        });
        seven.defineNative("screenwidth", 0, 0, a -> (double) s.width);
        seven.defineNative("camera", 0, 2, a -> {
            s.pen.camX = a.has(0) ? flr(a, 0) : 0;
            s.pen.camY = a.has(1) ? flr(a, 1) : 0;
            return null;
        });
        // colour swaps: pal(c0, c1) draws colour c0 as c1 from now on; pal(c0, c1, 1) shows c0 as c1 on the
        // whole screen (flashes, fades); pal() undoes every swap. palt(c, true) makes colour c see-through in
        // sprites and the map (palt(0, false) to draw black), palt() back to just 0
        seven.defineNative("pal", 0, 3, a -> {
            if (!a.has(0)) {
                s.pen.resetColours();
                return null;
            }
            int from = flr(a, 0) & 15, to = a.has(1) ? flr(a, 1) & 15 : from;
            if (a.has(2) && a.number(2) == 1) s.pen.show[from] = to;
            else s.pen.draw[from] = to;
            return null;
        });
        seven.defineNative("palt", 0, 2, a -> {
            if (!a.has(0)) {
                for (int i = 0; i < 16; i++) s.pen.transparent[i] = i == 0;
                return null;
            }
            s.pen.transparent[flr(a, 0) & 15] = !a.has(1) || sev7n.lang.runtime.Values.isTruthy(a.get(1));
            return null;
        });

        // sprite flags: fget(n, f) is flag f (0-7) of sprite n on? fget(n) all 8 as a number; fset(n, f, on) / fset(n, value)
        seven.defineNative("fget", 1, 2, a -> {
            int bits = console.flags[flr(a, 0) & 255] & 255;
            if (!a.has(1)) return (double) bits;
            return (bits >> (flr(a, 1) & 7) & 1) != 0;
        });
        seven.defineNative("fset", 2, 3, a -> {
            int n = flr(a, 0) & 255;
            if (a.has(2)) {
                int bit = 1 << (flr(a, 1) & 7);
                boolean on = sev7n.lang.runtime.Values.isTruthy(a.get(2));
                console.flags[n] = (byte) (on ? console.flags[n] | bit : console.flags[n] & ~bit);
            } else {
                console.flags[n] = (byte) (flr(a, 1) & 255);
            }
            return null;
        });

        // saves: cartdata("mygame") once, then dset(slot, number) / dget(slot): 64 numbers kept after the game closes
        seven.defineNative("cartdata", 1, 1, a -> {
            if (!console.saves.open(a.string(0))) throw a.error("cartdata needs a name with letters or numbers, like cartdata(\"mygame\")");
            return null;
        });
        seven.defineNative("dget", 1, 1, a -> console.saves.get(flr(a, 0)));
        seven.defineNative("dset", 2, 2, a -> {
            if (!console.saves.isOpen()) throw a.error("call cartdata(\"yourgame\") first, so Emerald knows where to save");
            int slot = flr(a, 0);
            if (slot < 0 || slot >= SaveData.SLOTS) throw a.error("dset slots go from 0 to " + (SaveData.SLOTS - 1));
            console.saves.set(slot, a.number(1));
            return null;
        });

        seven.defineNative("map", 0, 7, a -> {
            int tx = a.has(0) ? flr(a, 0) : 0, ty = a.has(1) ? flr(a, 1) : 0;
            int sx = a.has(2) ? flr(a, 2) : 0, sy = a.has(3) ? flr(a, 3) : 0;
            int w = a.has(4) ? flr(a, 4) : 16, h = a.has(5) ? flr(a, 5) : 16;
            int layers = a.has(6) ? flr(a, 6) : 0; // only tiles with any of these flags (0: every tile)
            for (int row = 0; row < h; row++) {
                for (int col = 0; col < w; col++) {
                    int mx = tx + col, my = ty + row;
                    if (mx < 0 || my < 0 || mx >= Cart.MAP_WIDTH || my >= Cart.MAP_HEIGHT) continue;
                    int n = console.map[my * Cart.MAP_WIDTH + mx] & 255;
                    if (n != 0 && (layers == 0 || (console.flags[n] & layers) != 0)) drawSprite(console, s, n, sx + col * 8, sy + row * 8);
                }
            }
            return null;
        });
        seven.defineNative("mget", 2, 2, a -> {
            int x = flr(a, 0), y = flr(a, 1);
            return x < 0 || y < 0 || x >= Cart.MAP_WIDTH || y >= Cart.MAP_HEIGHT ? 0.0 : (double) (console.map[y * Cart.MAP_WIDTH + x] & 255);
        });
        seven.defineNative("mset", 3, 3, a -> {
            int x = flr(a, 0), y = flr(a, 1);
            if (x >= 0 && y >= 0 && x < Cart.MAP_WIDTH && y < Cart.MAP_HEIGHT) console.map[y * Cart.MAP_WIDTH + x] = (byte) (flr(a, 2) & 255);
            return null;
        });

        // sound: sfx(n) plays sound n (from the SFX editor) on a free channel, sfx(n, ch) on that one, sfx(-1) stops
        // sound effects; music(n) plays the song from pattern n, music(-1) stops it
        seven.defineNative("sfx", 1, 2, a -> {
            console.audio.sfx(flr(a, 0), a.has(1) ? flr(a, 1) : -1);
            return null;
        });
        seven.defineNative("music", 1, 1, a -> {
            console.audio.music(flr(a, 0));
            return null;
        });

        // input
        // btn(b, [player]) / btnp(b, [player]): player 0 is the keyboard and the first controller, 1-3 more controllers
        seven.defineNative("btn", 1, 2, a -> {
            int b = a.integer(0), p = a.has(1) ? a.integer(1) : 0;
            return (p == 0 && console.input.button(b, false)) || console.gamepads.held(p, b);
        });
        seven.defineNative("btnp", 1, 2, a -> {
            int b = a.integer(0), p = a.has(1) ? a.integer(1) : 0;
            return (p == 0 && console.input.button(b, true)) || console.gamepads.pressed(p, b);
        });
        seven.defineNative("controllers", 0, 0, a -> (double) console.gamepads.connected());
        seven.defineNative("key", 1, 1, a -> console.input.held(keyCode(a)));
        seven.defineNative("keyp", 1, 1, a -> console.input.pressed(keyCode(a)));
        seven.defineNative("mousex", 0, 0, a -> (double) console.input.mouseX);
        seven.defineNative("mousey", 0, 0, a -> (double) console.input.mouseY);
        seven.defineNative("mousedown", 0, 0, a -> console.input.mouseHeld());
        seven.defineNative("click", 0, 0, a -> console.input.mouseClicked());
        seven.defineNative("cursor", 1, 1, a -> {
            String name = a.string(0);
            if (!name.equals("arrow") && !name.equals("hand") && !name.equals("none")) {
                throw a.error("cursor() takes \"arrow\", \"hand\" or \"none\"");
            }
            console.cursor = name;
            return null;
        });

        // handy numbers
        seven.defineNative("rnd", 0, 1, a -> random.nextDouble() * (a.has(0) ? a.number(0) : 1));
        seven.defineNative("flr", 1, 1, a -> Math.floor(a.number(0)));
        seven.defineNative("t", 0, 0, a -> console.time());
        seven.defineNative("log", 0, -1, a -> {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < a.size(); i++) line.append(i > 0 ? " " : "").append(sev7n.lang.runtime.Values.stringify(a.get(i)));
            System.out.println(line);
            return null;
        });
    }

    /** One 8 x 8 sprite at (x, y), colour 0 see-through. */
    private static void drawSprite(Console console, Screen s, int n, int x, int y) {
        int sx = n % 16 * 8, sy = n / 16 * 8;
        for (int py = 0; py < 8; py++) {
            for (int px = 0; px < 8; px++) {
                int c = console.sheet[(sy + py) * Cart.SHEET + sx + px];
                if (!s.pen.transparent[c]) s.pset(x + px, y + py, c);
            }
        }
    }

    private static int flr(Args a, int i) {
        return (int) Math.floor(a.number(i));
    }

    private static int color(Args a, int i, Screen s) {
        if (!a.has(i)) return s.color;
        int c = (int) Math.floor(a.number(i)) & 15;
        s.color = c;
        return c;
    }

    private static int keyCode(Args a) {
        Integer code = Input.code(a.string(0));
        if (code == null) throw a.error("there's no key called '" + a.string(0) + "'. Try \"a\", \"space\", \"enter\", \"up\", \"shift\" or \"f1\"");
        return code;
    }
}
