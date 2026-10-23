package emerald;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A cart: one .emerald file holding a game's code, sprites, sounds and music, so a whole game is
 * one file to share. After the code come sections of comment lines (Seven skips them):
 *
 *   ...your code...
 *   //@sprites
 *   //0000ee000...      a row of the 128 x 128 sheet per line, a hex digit per pixel
 *   //@flags
 *   //0103000...        8 on/off flags for each of the 256 sprites, 2 hex digits each ("solid", "spikes"...)
 *   //@map
 *   //00010203...       a row of the 256 x 128 map per line, 2 hex digits (a sprite number) per tile
 *   //@sfx
 *   //10000000...       a sound per line (see Sound.Sfx), "--" for an empty one
 *   //@music
 *   //0ff01ffff         a song pattern per line (see Sound.Pattern)
 *
 * The sheet holds 256 sprites of 8 x 8, numbered left to right, top to bottom. Empty things at the
 * end of a section aren't written, so a cart only gets as long as what's in it.
 */
final class Cart {

    static final String SPRITES = "//@sprites", FLAGS = "//@flags", MAP = "//@map", SFX = "//@sfx", MUSIC = "//@music";
    static final int SHEET = 128;
    /** The map: 256 x 128 tiles, each a sprite number (0 = empty). Every 16 x 16 tiles is one screen. */
    static final int MAP_WIDTH = 256, MAP_HEIGHT = 128;

    String code = "";
    final byte[] sheet = new byte[SHEET * SHEET];
    final byte[] map = new byte[MAP_WIDTH * MAP_HEIGHT];
    /** 8 flags (bits) for each sprite, for games to mark tiles: solid, deadly, a ladder... */
    final byte[] flags = new byte[256];
    final Sound.Sfx[] sfx = new Sound.Sfx[Sound.SFX_COUNT];
    final Sound.Pattern[] patterns = new Sound.Pattern[Sound.PATTERNS];

    Cart() {
        for (int i = 0; i < sfx.length; i++) sfx[i] = new Sound.Sfx();
        for (int i = 0; i < patterns.length; i++) patterns[i] = new Sound.Pattern();
    }

    /** The last picture of the game (128 x 128 RGB), the label on .png carts. Not saved in .emerald files. */
    int[] label;

    /** Reads a cart: a .emerald file, or a .png cart picture. */
    static Cart read(Path file) throws IOException {
        if (file.getFileName().toString().toLowerCase().endsWith(".png")) return PngCart.read(file);
        return fromText(Files.readString(file, StandardCharsets.UTF_8));
    }

    static Cart fromText(String text) {
        Cart cart = new Cart();
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        StringBuilder code = new StringBuilder();
        String section = null;
        int row = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equals(SPRITES) || trimmed.equals(FLAGS) || trimmed.equals(MAP) || trimmed.equals(SFX) || trimmed.equals(MUSIC)) {
                section = trimmed;
                row = 0;
                continue;
            }
            if (section == null) {
                code.append(line).append('\n');
                continue;
            }
            String data = trimmed.startsWith("//") ? trimmed.substring(2) : trimmed;
            switch (section) {
                case SPRITES -> {
                    if (row < SHEET) {
                        for (int x = 0; x < Math.min(SHEET, data.length()); x++) {
                            int c = Character.digit(data.charAt(x), 16);
                            if (c >= 0) cart.sheet[row * SHEET + x] = (byte) c;
                        }
                    }
                }
                case FLAGS -> {
                    for (int i = 0; i < Math.min(256, data.length() / 2); i++) {
                        try {
                            cart.flags[i] = (byte) Integer.parseInt(data.substring(i * 2, i * 2 + 2), 16);
                        } catch (NumberFormatException ignored) {
                            // a damaged flag stays off
                        }
                    }
                }
                case MAP -> {
                    if (row < MAP_HEIGHT) {
                        for (int x = 0; x < Math.min(MAP_WIDTH, data.length() / 2); x++) {
                            try {
                                cart.map[row * MAP_WIDTH + x] = (byte) Integer.parseInt(data.substring(x * 2, x * 2 + 2), 16);
                            } catch (NumberFormatException ignored) {
                                // a damaged tile stays empty
                            }
                        }
                    }
                }
                case SFX -> {
                    if (row < Sound.SFX_COUNT && !data.startsWith("-") && !data.isEmpty()) cart.sfx[row] = Sound.Sfx.decode(data);
                }
                default -> {
                    if (row < Sound.PATTERNS && !data.isEmpty()) cart.patterns[row] = Sound.Pattern.decode(data);
                }
            }
            row++;
        }
        // without sections, the last line added a newline the file didn't have; with them, the code ends where they start
        cart.code = section == null && code.length() > 0 ? code.substring(0, code.length() - 1) : code.toString();
        return cart;
    }

    /** Writes the cart: a .emerald file, or (named .png) a cart picture. */
    void write(Path file) throws IOException {
        if (file.getFileName().toString().toLowerCase().endsWith(".png")) PngCart.write(this, file);
        else Files.writeString(file, text(), StandardCharsets.UTF_8);
    }

    /** The whole cart as the text of a .emerald file. */
    String text() {
        StringBuilder out = new StringBuilder(code);
        List<String> rows = new ArrayList<>();
        // sprites: rows up to the last one with something in it
        int lastRow = -1;
        for (int y = 0; y < SHEET; y++) for (int x = 0; x < SHEET; x++) if (sheet[y * SHEET + x] != 0) lastRow = y;
        for (int y = 0; y <= lastRow; y++) {
            StringBuilder r = new StringBuilder();
            for (int x = 0; x < SHEET; x++) r.append(Character.forDigit(sheet[y * SHEET + x] & 15, 16));
            rows.add(r.toString());
        }
        section(out, SPRITES, rows);
        int lastFlag = -1;
        for (int i = 0; i < 256; i++) if (flags[i] != 0) lastFlag = i;
        if (lastFlag >= 0) {
            StringBuilder r = new StringBuilder();
            for (int i = 0; i <= lastFlag; i++) r.append(String.format("%02x", flags[i] & 255));
            section(out, FLAGS, List.of(r.toString()));
        }
        rows = new ArrayList<>();
        int lastMapRow = -1;
        for (int y = 0; y < MAP_HEIGHT; y++) for (int x = 0; x < MAP_WIDTH; x++) if (map[y * MAP_WIDTH + x] != 0) lastMapRow = y;
        for (int y = 0; y <= lastMapRow; y++) {
            StringBuilder r = new StringBuilder();
            for (int x = 0; x < MAP_WIDTH; x++) r.append(String.format("%02x", map[y * MAP_WIDTH + x] & 255));
            rows.add(r.toString());
        }
        section(out, MAP, rows);
        rows = new ArrayList<>();
        int lastSfx = -1;
        for (int i = 0; i < sfx.length; i++) if (!sfx[i].empty() || sfx[i].speed != 16 || sfx[i].loops()) lastSfx = i;
        for (int i = 0; i <= lastSfx; i++) rows.add(sfx[i].empty() && sfx[i].speed == 16 && !sfx[i].loops() ? "--" : sfx[i].encode());
        section(out, SFX, rows);
        rows = new ArrayList<>();
        int lastPattern = -1;
        for (int i = 0; i < patterns.length; i++) if (!patterns[i].empty() || patterns[i].loopStart || patterns[i].loopEnd || patterns[i].stop) lastPattern = i;
        for (int i = 0; i <= lastPattern; i++) rows.add(patterns[i].encode());
        section(out, MUSIC, rows);
        return out.toString();
    }

    private static void section(StringBuilder out, String marker, List<String> rows) {
        if (rows.isEmpty()) return;
        if (!out.isEmpty() && out.charAt(out.length() - 1) != '\n') out.append('\n');
        out.append(marker).append('\n');
        for (String r : rows) out.append("//").append(r).append('\n');
    }
}
