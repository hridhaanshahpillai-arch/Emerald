package emerald;

import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The sprite editor: draw 8 x 8 sprites (or 16 x 16, four at once) on the 128 x 128 sprite sheet.
 *
 *   canvas   left mouse paints, right mouse picks a colour from the drawing
 *   palette  click a colour        tools   PEN, FILL, size 8 / 16, CLEAR
 *   sheet    click a sprite to edit it; the 1 2 3 4 tabs show the four quarters of the sheet
 *   Ctrl+Z / Y undo and redo
 *   ANIM     plays this sprite and the next ones (2, 3 or 4 frames) as an animation, beside it
 */
final class SpriteEditor {

    private static final int CANVAS_X = 4, CANVAS_Y = 12, CANVAS = 64;
    private static final int PALETTE_X = 76, PALETTE_Y = 12, SWATCH = 9;
    private static final int FLAGS_Y = 49, TOOLS_Y = 55;
    /** A colour for each of the 8 flags, so they're easy to tell apart. */
    private static final int[] FLAG_COLOURS = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int TABS_Y = 80, SHEET_Y = 88;

    private byte[] sheet;
    private byte[] flags;
    int sprite;                         // the top-left sprite being edited (0..255)
    int colour = 8;
    private boolean fill;               // FILL tool instead of PEN
    private int size = 1;               // 1 = 8 x 8, 2 = 16 x 16
    private int page;                   // which quarter of the sheet shows (0..3)
    private int frames = 1;             // ANIM: how many sprites the preview plays (1 = off)
    private boolean dirty;
    private boolean stroking;
    private final Deque<byte[]> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    String hover = "";                  // what the mouse is over, for the bottom bar
    boolean overButton;                 // the cursor shows the hand

    void setSheet(byte[] sheet, byte[] flags) {
        this.sheet = sheet;
        this.flags = flags;
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

    // ---------------------------------------------------------------- input

    void update(Input input) {
        int mx = input.mouseX, my = input.mouseY;
        hover = "";
        overButton = false;
        for (Input.KeyPress k : input.presses()) {
            if (k.ctrl() && k.code() == KeyEvent.VK_Z) undoRedo(k.shift() ? redo : undo, k.shift() ? undo : redo);
            if (k.ctrl() && k.code() == KeyEvent.VK_Y) undoRedo(redo, undo);
        }
        if (!input.mouseHeld()) stroking = false;

        // the canvas
        int span = 8 * size, cell = CANVAS / span;
        if (inBox(mx, my, CANVAS_X, CANVAS_Y, CANVAS, CANVAS)) {
            int px = (mx - CANVAS_X) / cell, py = (my - CANVAS_Y) / cell;
            int sx = sheetX() + px, sy = sheetY() + py;
            hover = "X " + px + " Y " + py;
            if (input.rightHeld()) colour = sheet[sy * Cart.SHEET + sx];
            else if (fill && input.mouseClicked()) {
                remember();
                flood(sx, sy, sheet[sy * Cart.SHEET + sx], colour);
            } else if (!fill && input.mouseHeld() && (input.mouseClicked() || stroking)) {
                if (!stroking) remember();
                stroking = true;
                if (sheet[sy * Cart.SHEET + sx] != colour) {
                    sheet[sy * Cart.SHEET + sx] = (byte) colour;
                    dirty = true;
                }
            }
        }
        // the palette
        if (inBox(mx, my, PALETTE_X, PALETTE_Y, SWATCH * 4, SWATCH * 4)) {
            int c = (my - PALETTE_Y) / SWATCH * 4 + (mx - PALETTE_X) / SWATCH;
            hover = "COLOUR " + c;
            overButton = true;
            if (input.mouseClicked()) colour = c;
        }
        // the 8 flags of the sprite being edited
        for (int f = 0; f < 8; f++) {
            int x = PALETTE_X + f * 4 + (f >= 4 ? 2 : 0);
            if (inBox(mx, my, x - 1, FLAGS_Y - 1, 4, 5)) {
                boolean on = (flags[sprite] >> f & 1) != 0;
                hover = "FLAG " + f + (on ? " ON" : " OFF") + ": CLICK TO SWITCH";
                overButton = true;
                if (input.mouseClicked()) {
                    flags[sprite] = (byte) (flags[sprite] ^ (1 << f));
                    dirty = true;
                }
            }
        }
        // the tools
        button(input, mx, my, PALETTE_X, TOOLS_Y, "PEN", () -> fill = false);
        button(input, mx, my, PALETTE_X + 18, TOOLS_Y, "FILL", () -> fill = true);
        button(input, mx, my, PALETTE_X, TOOLS_Y + 9, size == 1 ? "8X8" : "16X16", () -> {
            size = size == 1 ? 2 : 1;
            if (size == 2 && sprite % 16 == 15) sprite--;  // 16 x 16 needs room to the right and below
            if (size == 2 && sprite / 16 == 15) sprite -= 16;
        });
        button(input, mx, my, PALETTE_X, TOOLS_Y + 18, "CLEAR", () -> {
            remember();
            for (int y = 0; y < span; y++) for (int x = 0; x < span; x++) sheet[(sheetY() + y) * Cart.SHEET + sheetX() + x] = 0;
            dirty = true;
        });
        button(input, mx, my, 50, TABS_Y, "ANIM " + frames, () -> frames = frames % 4 + 1);
        if (inBox(mx, my, 50, TABS_Y, 27, 7)) hover = frames == 1 ? "ANIM: PLAY THE NEXT SPRITES AS FRAMES" : "ANIM: " + frames + " FRAMES (CLICK FOR MORE)";
        // the sheet tabs and the sheet
        for (int p = 0; p < 4; p++) {
            final int which = p;
            button(input, mx, my, 2 + p * 10, TABS_Y, String.valueOf(p + 1), () -> page = which);
        }
        if (inBox(mx, my, 0, SHEET_Y, 128, 32)) {
            int n = page * 64 + (my - SHEET_Y) / 8 * 16 + mx / 8;
            hover = "SPRITE " + n;
            overButton = true;
            if (input.mouseClicked()) {
                sprite = n;
                if (size == 2 && sprite % 16 == 15) sprite--;
                if (size == 2 && sprite / 16 == 15) sprite -= 16;
            }
        }
    }

    private int sheetX() {
        return sprite % 16 * 8;
    }

    private int sheetY() {
        return sprite / 16 * 8;
    }

    private void button(Input input, int mx, int my, int x, int y, String label, Runnable action) {
        int w = label.length() * PixelFont.ADVANCE + 3;
        if (inBox(mx, my, x, y, w, 7)) {
            overButton = true;
            if (input.mouseClicked()) action.run();
        }
    }

    private static boolean inBox(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    /** Fills the area of 'from' colour around (x, y) with 'to', staying inside the sprite being edited. */
    private void flood(int x, int y, int from, int to) {
        if (from == to) return;
        int span = 8 * size, left = sheetX(), top = sheetY();
        Deque<int[]> todo = new ArrayDeque<>();
        todo.push(new int[] {x, y});
        while (!todo.isEmpty()) {
            int[] p = todo.pop();
            if (p[0] < left || p[1] < top || p[0] >= left + span || p[1] >= top + span) continue;
            if (sheet[p[1] * Cart.SHEET + p[0]] != from) continue;
            sheet[p[1] * Cart.SHEET + p[0]] = (byte) to;
            todo.push(new int[] {p[0] + 1, p[1]});
            todo.push(new int[] {p[0] - 1, p[1]});
            todo.push(new int[] {p[0], p[1] + 1});
            todo.push(new int[] {p[0], p[1] - 1});
        }
        dirty = true;
    }

    private void remember() {
        undo.push(sheet.clone());
        if (undo.size() > 100) undo.removeLast();
        redo.clear();
    }

    private void undoRedo(Deque<byte[]> from, Deque<byte[]> to) {
        if (from.isEmpty()) return;
        to.push(sheet.clone());
        System.arraycopy(from.pop(), 0, sheet, 0, sheet.length);
        dirty = true;
    }

    // ---------------------------------------------------------------- drawing

    void draw(Screen s, PixelFont font) {
        s.rectfill(0, 8, 127, 120, 5);
        // the canvas, with a frame
        int span = 8 * size, cell = CANVAS / span;
        s.rect(CANVAS_X - 1, CANVAS_Y - 1, CANVAS_X + CANVAS, CANVAS_Y + CANVAS, 0);
        for (int y = 0; y < span; y++) {
            for (int x = 0; x < span; x++) {
                int c = sheet[(sheetY() + y) * Cart.SHEET + sheetX() + x];
                s.rectfill(CANVAS_X + x * cell, CANVAS_Y + y * cell, CANVAS_X + x * cell + cell - 1, CANVAS_Y + y * cell + cell - 1, c);
            }
        }
        // the palette: the chosen colour gets a frame
        for (int c = 0; c < 16; c++) {
            int x = PALETTE_X + c % 4 * SWATCH, y = PALETTE_Y + c / 4 * SWATCH;
            s.rectfill(x, y, x + SWATCH - 1, y + SWATCH - 1, c);
        }
        int cx = PALETTE_X + colour % 4 * SWATCH, cy = PALETTE_Y + colour / 4 * SWATCH;
        s.rect(cx - 1, cy - 1, cx + SWATCH, cy + SWATCH, 7);
        s.rect(cx, cy, cx + SWATCH - 1, cy + SWATCH - 1, 0);
        // the flags: filled when on
        for (int f = 0; f < 8; f++) {
            int x = PALETTE_X + f * 4 + (f >= 4 ? 2 : 0);
            boolean on = (flags[sprite] >> f & 1) != 0;
            if (on) s.rectfill(x, FLAGS_Y, x + 2, FLAGS_Y + 2, FLAG_COLOURS[f]);
            else s.rect(x, FLAGS_Y, x + 2, FLAGS_Y + 2, 1);
        }
        // tools
        label(s, font, PALETTE_X, TOOLS_Y, "PEN", !fill);
        label(s, font, PALETTE_X + 18, TOOLS_Y, "FILL", fill);
        label(s, font, PALETTE_X, TOOLS_Y + 9, size == 1 ? "8X8" : "16X16", false);
        label(s, font, PALETTE_X, TOOLS_Y + 18, "CLEAR", false);
        font.draw(s, "#" + String.format("%03d", sprite), PALETTE_X + 28, TOOLS_Y + 19, 6);
        // the sheet: one quarter at a time
        for (int p = 0; p < 4; p++) label(s, font, 2 + p * 10, TABS_Y, String.valueOf(p + 1), p == page);
        label(s, font, 50, TABS_Y, "ANIM " + frames, frames > 1);
        if (frames > 1) drawPreview(s, 80, TABS_Y - 1);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 128; x++) s.pset(x, SHEET_Y + y, sheet[(page * 32 + y) * Cart.SHEET + x]);
        }
        // the sprite being edited, outlined on the sheet
        int sy = sheetY() - page * 32;
        if (sy >= 0 && sy < 32) {
            s.rect(sheetX() - 1, SHEET_Y + sy - 1, sheetX() + span, SHEET_Y + sy + span, 7);
        }
    }

    /** The animation: this sprite and the next ones in turn, 6 frames a second (16 x 16 shown at half size). */
    private void drawPreview(Screen s, int x, int y) {
        int frame = (int) (System.nanoTime() / 166_000_000L % frames);
        int n = sprite + frame * size;
        int sx = n % 16 * 8, sy = n / 16 * 8, step = size;
        s.rectfill(x - 1, y - 1, x + 8, y + 8, 0);
        for (int py = 0; py < 8; py++)
            for (int px = 0; px < 8; px++) {
                int gx = sx + px * step, gy = sy + py * step;
                if (gx < Cart.SHEET && gy < Cart.SHEET) s.pset(x + px, y + py, sheet[gy * Cart.SHEET + gx]);
            }
    }

    private static void label(Screen s, PixelFont font, int x, int y, String text, boolean on) {
        int w = text.length() * PixelFont.ADVANCE + 3;
        s.rectfill(x, y, x + w - 1, y + 6, on ? 11 : 1);
        font.draw(s, text, x + 2, y + 1, on ? 1 : 6);
    }
}
