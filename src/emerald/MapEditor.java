package emerald;

import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The MAP editor: paint levels with your sprites as tiles, on a 128 x 64 tile map (every 16 x 16
 * tiles is one screen; faint lines show where screens meet).
 *
 *   left mouse    paint the chosen tile          right mouse   pick up the tile under it
 *   arrows / middle-drag / wheel   move around   ZOOM   see twice as much
 *   the strip at the bottom: pick a tile (tabs 1-4 show all 256); tile 0 is empty
 *   Ctrl+Z / Y    undo and redo
 *   AUTO (or T)   auto-tiling: a sheet row of 16 tiles is one set, ordered by which sides are open
 *                 (+1 open above, +2 below, +4 left, +8 right); painting any tile of the set picks
 *                 the right one, and fixes the tiles around it
 */
final class MapEditor {

    private static final int VIEW_TOP = 9, VIEW_BOTTOM = 80;     // the map view
    private static final int INFO_Y = 82, PICKER_Y = 88;          // the info row and the tile picker

    private Cart cart;
    int tile = 1;                        // the tile painted
    private int page;                    // which quarter of the sheet the picker shows
    private int scrollX, scrollY;        // the map tile at the view's top-left
    private boolean zoomed = true;       // 4-pixel tiles (the whole first screen in view) instead of 8
    private boolean dirty, stroking;
    private boolean auto;                // auto-tiling on
    private int dragX = -1, dragY, dragScrollX, dragScrollY;
    private final Deque<byte[]> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    String hover = "";
    boolean overButton;

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

    private int cell() {
        return zoomed ? 4 : 8;
    }

    private int viewColumns() {
        return Screen.WIDE / cell();
    }

    private int viewRows() {
        return (VIEW_BOTTOM - VIEW_TOP + 1) / cell();
    }

    void update(Input input) {
        hover = "";
        overButton = false;
        int mx = input.mouseX, my = input.mouseY;
        for (Input.KeyPress k : input.presses()) {
            if (k.ctrl() && k.code() == KeyEvent.VK_Z) undoRedo(k.shift() ? redo : undo, k.shift() ? undo : redo);
            else if (k.ctrl() && k.code() == KeyEvent.VK_Y) undoRedo(redo, undo);
            else if (k.code() == KeyEvent.VK_LEFT) scroll(-1, 0);
            else if (k.code() == KeyEvent.VK_RIGHT) scroll(1, 0);
            else if (k.code() == KeyEvent.VK_UP) scroll(0, -1);
            else if (k.code() == KeyEvent.VK_DOWN) scroll(0, 1);
            else if (k.code() == KeyEvent.VK_T && !k.ctrl()) auto = !auto;
        }
        if (!input.mouseHeld() && !input.rightHeld()) stroking = false;
        boolean inView = mx >= 0 && mx < Screen.WIDE && my >= VIEW_TOP && my <= VIEW_BOTTOM;

        // middle-drag (or Space + drag) moves the map
        boolean panning = input.held(KeyEvent.VK_SPACE) && input.mouseHeld();
        if (panning && inView) {
            if (dragX < 0) {
                dragX = mx;
                dragY = my;
                dragScrollX = scrollX;
                dragScrollY = scrollY;
            }
            scrollX = dragScrollX - (mx - dragX) / cell();
            scrollY = dragScrollY - (my - dragY) / cell();
            clampScroll();
        } else {
            dragX = -1;
        }
        if (inView && input.wheel() != 0) scroll(0, input.wheel() * 2);

        if (inView && !panning) {
            int tx = scrollX + mx / cell(), ty = scrollY + (my - VIEW_TOP) / cell();
            if (tx < Cart.MAP_WIDTH && ty < Cart.MAP_HEIGHT) {
                int here = cart.map[ty * Cart.MAP_WIDTH + tx] & 255;
                hover = "X " + tx + " Y " + ty + "  TILE " + here;
                if (input.rightHeld()) tile = here;
                else if (input.mouseHeld() && (input.mouseClicked() || stroking)) {
                    if (!stroking) remember();
                    stroking = true;
                    if (auto) {
                        if (tile == 0 ? here != 0 : (here == 0 || here / 16 != tile / 16)) {
                            int group = tile == 0 ? here / 16 : tile / 16;
                            cart.map[ty * Cart.MAP_WIDTH + tx] = (byte) (tile == 0 ? 0 : group * 16);
                            fixAround(tx, ty, group);
                            dirty = true;
                        }
                    } else if (here != tile) {
                        cart.map[ty * Cart.MAP_WIDTH + tx] = (byte) tile;
                        dirty = true;
                    }
                }
            }
        }
        // the picker
        if (mx >= 0 && mx < 128 && my >= PICKER_Y && my < PICKER_Y + 32) {
            int n = page * 64 + (my - PICKER_Y) / 8 * 16 + mx / 8;
            hover = n == 0 ? "TILE 0: EMPTY (ERASES)" : "TILE " + n;
            overButton = true;
            if (input.mouseClicked()) tile = n;
        }
        for (int p = 0; p < 4; p++) {
            final int which = p;
            button(input, 88 + p * 10, INFO_Y - 1, String.valueOf(p + 1), () -> page = which);
        }
        button(input, 2, INFO_Y - 1, "AUTO", () -> auto = !auto);
        button(input, 58, INFO_Y - 1, zoomed ? "8PX" : "ZOOM", () -> {
            zoomed = !zoomed;
            clampScroll();
        });
    }

    // ---------------------------------------------------------------- auto-tiling

    /** Picks the right tile of the set for a cell and its four neighbours. */
    private void fixAround(int tx, int ty, int group) {
        fix(tx, ty, group);
        fix(tx, ty - 1, group);
        fix(tx, ty + 1, group);
        fix(tx - 1, ty, group);
        fix(tx + 1, ty, group);
    }

    private void fix(int x, int y, int group) {
        if (x < 0 || y < 0 || x >= Cart.MAP_WIDTH || y >= Cart.MAP_HEIGHT) return;
        int n = cart.map[y * Cart.MAP_WIDTH + x] & 255;
        if (n == 0 || n / 16 != group) return;
        int mask = 0;
        if (!inSet(x, y - 1, group)) mask |= 1;
        if (!inSet(x, y + 1, group)) mask |= 2;
        if (!inSet(x - 1, y, group)) mask |= 4;
        if (!inSet(x + 1, y, group)) mask |= 8;
        cart.map[y * Cart.MAP_WIDTH + x] = (byte) (group * 16 + mask);
    }

    /** Is this cell part of the set? (The map's edges count as yes: no outline there.) */
    private boolean inSet(int x, int y, int group) {
        if (x < 0 || y < 0 || x >= Cart.MAP_WIDTH || y >= Cart.MAP_HEIGHT) return true;
        int n = cart.map[y * Cart.MAP_WIDTH + x] & 255;
        return n != 0 && n / 16 == group;
    }

    private void scroll(int dx, int dy) {
        scrollX += dx;
        scrollY += dy;
        clampScroll();
    }

    private void clampScroll() {
        scrollX = Math.max(0, Math.min(Cart.MAP_WIDTH - viewColumns(), scrollX));
        scrollY = Math.max(0, Math.min(Cart.MAP_HEIGHT - viewRows(), scrollY));
    }

    private void button(Input input, int x, int y, String label, Runnable action) {
        int w = label.length() * PixelFont.ADVANCE + 3;
        if (input.mouseX >= x && input.mouseY >= y && input.mouseX < x + w && input.mouseY < y + 7) {
            overButton = true;
            if (input.mouseClicked()) action.run();
        }
    }

    private void remember() {
        undo.push(cart.map.clone());
        if (undo.size() > 100) undo.removeLast();
        redo.clear();
    }

    private void undoRedo(Deque<byte[]> from, Deque<byte[]> to) {
        if (from.isEmpty()) return;
        to.push(cart.map.clone());
        System.arraycopy(from.pop(), 0, cart.map, 0, cart.map.length);
        dirty = true;
    }

    // ---------------------------------------------------------------- drawing

    void draw(Screen s, PixelFont font) {
        s.rectfill(0, 8, Screen.WIDE - 1, 120, 5);
        // the map, with faint lines where screens (16 x 16 tiles) meet
        s.rectfill(0, VIEW_TOP, Screen.WIDE - 1, VIEW_BOTTOM, 0);
        int cell = cell();
        for (int row = 0; row < viewRows(); row++) {
            for (int col = 0; col < viewColumns(); col++) {
                int tx = scrollX + col, ty = scrollY + row;
                int n = cart.map[ty * Cart.MAP_WIDTH + tx] & 255;
                if (n != 0) drawTile(s, n, col * cell, VIEW_TOP + row * cell, zoomed);
            }
        }
        for (int col = 0; col <= viewColumns(); col++) {
            if ((scrollX + col) % 16 == 0) s.line(col * cell, VIEW_TOP, col * cell, VIEW_BOTTOM, 1);
        }
        for (int row = 0; row <= viewRows(); row++) {
            if ((scrollY + row) % 16 == 0) s.line(0, VIEW_TOP + row * cell, Screen.WIDE - 1, VIEW_TOP + row * cell, 1);
        }
        // the info row: where the view is, zoom, and the picker's pages
        label(s, font, 2, INFO_Y - 1, "AUTO", auto);
        font.draw(s, String.format("#%03d", tile), 34, INFO_Y, 7);
        label(s, font, 58, INFO_Y - 1, zoomed ? "8PX" : "ZOOM", zoomed);
        for (int p = 0; p < 4; p++) label(s, font, 88 + p * 10, INFO_Y - 1, String.valueOf(p + 1), p == page);
        // the picker: a quarter of the sprite sheet, the chosen tile outlined
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 128; x++) s.pset(x, PICKER_Y + y, cart.sheet[(page * 32 + y) * Cart.SHEET + x]);
        }
        if (page == 0) { // tile 0 is the eraser: crossed out
            s.line(1, PICKER_Y + 1, 6, PICKER_Y + 6, 8);
            s.line(6, PICKER_Y + 1, 1, PICKER_Y + 6, 8);
        }
        int ty = tile / 16 * 8 - page * 32;
        if (ty >= 0 && ty < 32) s.rect(tile % 16 * 8 - 1, PICKER_Y + ty - 1, tile % 16 * 8 + 8, PICKER_Y + ty + 8, 7);
    }

    /** A sprite as a map tile: full size, or every other pixel when zoomed out. */
    private void drawTile(Screen s, int n, int x, int y, boolean small) {
        int sx = n % 16 * 8, sy = n / 16 * 8, step = small ? 2 : 1;
        for (int py = 0; py < 8; py += step) {
            for (int px = 0; px < 8; px += step) {
                int c = cart.sheet[(sy + py) * Cart.SHEET + sx + px];
                if (c != 0) s.pset(x + px / step, y + py / step, c);
            }
        }
    }

    private static void label(Screen s, PixelFont font, int x, int y, String text, boolean on) {
        int w = text.length() * PixelFont.ADVANCE + 3;
        s.rectfill(x, y, x + w - 1, y + 6, on ? 11 : 1);
        font.draw(s, text, x + 2, y + 1, on ? 1 : 6);
    }
}
