package emerald;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * The PixelPico font as tiny pixel pictures: every letter is 3 x 5 pixels, 4 apart, and lines
 * are 6 apart. Text is always shown in capitals (the font's lowercase is too small to read).
 */
final class PixelFont {

    static final int ADVANCE = 4;
    static final int LINE = 6;
    private static final int CELL = 8;      // each glyph is cut from an 8 x 8 cell
    private static final int BASELINE = 6;  // where the letters sit in their cell

    /** For each character code (0..255): its pixels as bits, row by row, CELL wide. */
    private final long[] glyphs = new long[256];

    PixelFont(InputStream ttf) throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, ttf).deriveFont(8f); // at 8, one design pixel is one pixel
        BufferedImage cell = new BufferedImage(CELL, CELL, BufferedImage.TYPE_INT_ARGB);
        for (int ch = 32; ch < 256; ch++) {
            Graphics2D g = cell.createGraphics();
            g.setComposite(java.awt.AlphaComposite.Clear);
            g.fillRect(0, 0, CELL, CELL);
            g.setComposite(java.awt.AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setFont(font);
            g.setColor(java.awt.Color.WHITE);
            g.drawString(String.valueOf((char) ch), 0, BASELINE);
            g.dispose();
            long bits = 0;
            for (int y = 0; y < CELL; y++) {
                for (int x = 0; x < CELL; x++) {
                    if ((cell.getRGB(x, y) >>> 24) > 128) bits |= 1L << (y * CELL + x);
                }
            }
            glyphs[ch] = bits;
        }
    }

    /**
     * One character for the code editor: shown as a capital like everything else, but a real
     * capital letter gets a line under it, so "x" and "X" (different names in code) look different.
     */
    void drawCode(Screen screen, char ch, int x, int y, int color) {
        draw(screen, String.valueOf(ch), x, y, color);
        if (Character.isUpperCase(ch)) screen.line(x, y + 5, x + 2, y + 5, color);
    }

    /** Big text (each font pixel a 'scale' x 'scale' block), for titles and the boot logo. */
    void drawBig(Screen screen, String text, int x, int y, int color, int scale) {
        for (char ch : text.toUpperCase().toCharArray()) {
            long bits = ch < 256 ? glyphs[ch] : glyphs['?'];
            for (int gy = 1; gy < CELL; gy++) {
                for (int gx = 0; gx < CELL; gx++) {
                    if ((bits >>> (gy * CELL + gx) & 1) != 0) {
                        screen.rectfill(x + gx * scale, y + (gy - 1) * scale, x + gx * scale + scale - 1, y + (gy - 1) * scale + scale - 1, color);
                    }
                }
            }
            x += ADVANCE * scale;
        }
    }

    /** Draws text with its top-left at (x, y). Returns the x after the text. Newlines start a new line. */
    int draw(Screen screen, String text, int x, int y, int color) {
        int startX = x;
        for (char ch : text.toUpperCase().toCharArray()) {
            if (ch == '\n') {
                x = startX;
                y += LINE;
                continue;
            }
            long bits = ch < 256 ? glyphs[ch] : glyphs['?'];
            for (int gy = 0; gy < CELL && bits != 0; gy++) {
                for (int gx = 0; gx < CELL; gx++) {
                    if ((bits >>> (gy * CELL + gx) & 1) != 0) screen.pset(x + gx, y + gy - 1, color); // the font has a blank top row
                }
            }
            x += ADVANCE;
        }
        return x;
    }
}
