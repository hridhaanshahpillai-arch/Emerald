package emerald;

/**
 * Emerald's screen: 128 x 128 pixels, each one of 16 colours. Every drawing command ends up
 * here; the window just shows it, scaled up with sharp pixels.
 */
final class Screen {

    static final int SIZE = 128;
    static final int WIDE = 228;   // widescreen (16:9): carts turn it on with widescreen(true)

    /** The Emerald palette: soft and sweet, with emerald greens, and enough darks and brights for any game. */
    static final int[] PALETTE = {
            0x1D1E26, // 0  ink
            0x2B2D42, // 1  night
            0x6B3E8C, // 2  plum
            0x008751, // 3  deep emerald
            0x8A5A44, // 4  cocoa
            0x5A5D70, // 5  stone
            0xB8BCCB, // 6  mist
            0xFFF6F0, // 7  cream
            0xFF5E7E, // 8  strawberry
            0xFFA46B, // 9  peach
            0xFFE27A, // 10 lemon
            0x00E756, // 11 emerald
            0x7AC7FF, // 12 sky
            0xA99CFF, // 13 lavender
            0xFF8FC7, // 14 pink
            0xFFD6C2, // 15 sakura
    };

    /**
     * How things are drawn: the camera (everything is drawn shifted by -camX, -camY), colour swaps
     * (draw: a colour drawn as another; show: a colour shown as another on screen) and which colours
     * sprites leave see-through. Games change theirs; the editor always draws with a plain one.
     */
    static final class Pen {
        int camX, camY;
        int shakeX, shakeY;   // screen shake, on top of the camera (shake() in games)
        final int[] draw = new int[16];
        final int[] show = new int[16];
        final boolean[] transparent = new boolean[16];

        Pen() {
            reset();
        }

        void reset() {
            camX = camY = 0;
            shakeX = shakeY = 0;
            resetColours();
        }

        void resetColours() {
            for (int i = 0; i < 16; i++) {
                draw[i] = i;
                show[i] = i;
                transparent[i] = i == 0;
            }
        }
    }

    /** Colour numbers, row by row. */
    final byte[] pixels = new byte[WIDE * SIZE];
    /** How wide the screen is right now: 128, or 228 in widescreen. Always 128 tall. */
    int width = SIZE;
    int color = 7;
    private final Pen plain = new Pen();
    Pen pen = plain;

    /** The game's pen (its camera and colour swaps). */
    void use(Pen p) {
        pen = p;
    }

    /** The plain pen: no camera, no swaps (the editor and command line). */
    void usePlain() {
        pen = plain;
    }

    void cls(int c) {
        java.util.Arrays.fill(pixels, (byte) (c & 15));
    }

    void pset(int x, int y, int c) {
        x -= pen.camX - pen.shakeX;
        y -= pen.camY - pen.shakeY;
        if (x < 0 || y < 0 || x >= width || y >= SIZE) return;
        pixels[y * width + x] = (byte) pen.draw[c & 15];
    }

    int pget(int x, int y) {
        x -= pen.camX;
        y -= pen.camY;
        if (x < 0 || y < 0 || x >= width || y >= SIZE) return 0;
        return pixels[y * width + x];
    }

    void line(int x0, int y0, int x1, int y1, int c) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        for (int guard = 0; guard < 4 * SIZE * SIZE; guard++) {
            pset(x0, y0, c);
            if (x0 == x1 && y0 == y1) return;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    void rect(int x0, int y0, int x1, int y1, int c) {
        line(x0, y0, x1, y0, c);
        line(x0, y1, x1, y1, c);
        line(x0, y0, x0, y1, c);
        line(x1, y0, x1, y1, c);
    }

    void rectfill(int x0, int y0, int x1, int y1, int c) {
        x0 -= pen.camX - pen.shakeX;
        x1 -= pen.camX - pen.shakeX;
        y0 -= pen.camY - pen.shakeY;
        y1 -= pen.camY - pen.shakeY;
        int left = Math.max(0, Math.min(x0, x1)), right = Math.min(width - 1, Math.max(x0, x1));
        int top = Math.max(0, Math.min(y0, y1)), bottom = Math.min(SIZE - 1, Math.max(y0, y1));
        byte colour = (byte) pen.draw[c & 15];
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) pixels[y * width + x] = colour;
        }
    }

    void circ(int cx, int cy, int r, int c) {
        if (r < 0) return;
        int x = r, y = 0, err = 1 - r;
        while (x >= y) {
            pset(cx + x, cy + y, c); pset(cx + y, cy + x, c); pset(cx - y, cy + x, c); pset(cx - x, cy + y, c);
            pset(cx - x, cy - y, c); pset(cx - y, cy - x, c); pset(cx + y, cy - x, c); pset(cx + x, cy - y, c);
            y++;
            if (err < 0) err += 2 * y + 1;
            else {
                x--;
                err += 2 * (y - x) + 1;
            }
        }
    }

    void circfill(int cx, int cy, int r, int c) {
        if (r < 0) return;
        for (int y = -r; y <= r; y++) {
            int half = (int) Math.floor(Math.sqrt(r * r + r - y * y)); // a touch rounder than plain r*r
            rectfill(cx - half, cy + y, cx + half, cy + y, c);
        }
    }

    /** Copies the colour numbers into RGB pixels for the window (through the game's screen colour swaps). */
    void toRgb(int[] rgb) {
        int[] show = pen.show;
        for (int i = 0; i < width * SIZE; i++) rgb[i] = PALETTE[show[pixels[i] & 15]];
    }

    /** The colour actually shown at a pixel (for screenshots, GIFs and cart labels). */
    int shownRgb(int x, int y) {
        return PALETTE[pen.show[pixels[y * width + x] & 15]];
    }
}
