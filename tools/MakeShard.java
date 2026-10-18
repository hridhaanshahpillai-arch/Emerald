package emerald;

import java.nio.file.Path;
import java.util.Random;

/** Writes carts/pip.emerald: PIP AND THE EMERALD HEART. args: a cart to take the sounds from (shard.emerald itself), output */
public class MakePip {

    static Cart cart;

    // ---------- sprites ----------

    static void put(int n, String[] rows) {          // an 8x8 sprite from 8 strings ('.' = see-through)
        int sx = (n % 16) * 8, sy = (n / 16) * 8;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            char c = rows[y].charAt(x);
            cart.sheet[(sy + y) * 128 + sx + x] = (byte) (c == '.' ? 0 : Character.digit(c, 16));
        }
    }

    static void put16(int n, String[] rows) {        // a 16x16 sprite at n, n+1, n+16, n+17
        int sx = (n % 16) * 8, sy = (n / 16) * 8;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            char c = rows[y].charAt(x);
            cart.sheet[(sy + y) * 128 + sx + x] = (byte) (c == '.' ? 0 : Character.digit(c, 16));
        }
    }

    static String[] rotate(String[] r) {             // 90 degrees clockwise
        String[] out = new String[8];
        for (int y = 0; y < 8; y++) {
            StringBuilder b = new StringBuilder();
            for (int x = 0; x < 8; x++) b.append(r[7 - x].charAt(y));
            out[y] = b.toString();
        }
        return out;
    }

    static String[] flipV(String[] r) {
        String[] out = new String[8];
        for (int y = 0; y < 8; y++) out[y] = r[7 - y];
        return out;
    }

    static String[] flipH(String[] r) {
        String[] out = new String[r.length];
        for (int y = 0; y < r.length; y++) out[y] = new StringBuilder(r[y]).reverse().toString();
        return out;
    }

    // Pip, 16x16, facing right. Rows 0-13 are the body, 14-15 the legs.
    static final String[] BODY = {
            "................",
            "................",
            ".....111111.....",
            "....1bbbbbb1....",
            "...1b3bbbbbb1...",
            "...1bbbbbbbbb11.",
            "...14ffffff111..",
            "...144ff1f1f1...",
            "...14ffffff91...",
            ".....1ffff1.....",
            "...113bbbbb311..",
            "..1f13bbbbb31f1.",
            "...1144a4441....",
            "....13b33b31....",
    };
    static final String[] ARMS_UP = {
            "..f113bbbbb311f.",
            "...13bbbbbbb1...",
    };

    static String[] pip(String[] body, String leg1, String leg2) {
        String[] out = new String[16];
        System.arraycopy(body, 0, out, 0, 14);
        out[14] = leg1;
        out[15] = leg2;
        return out;
    }

    static String[] shiftUp(String[] body) {          // the body one pixel higher (a bob)
        String[] out = new String[14];
        for (int y = 0; y < 13; y++) out[y] = body[y + 1];
        out[13] = body[13];
        return out;
    }

    static String[] shiftDown(String[] body) {        // one pixel lower (breathing out)
        String[] out = new String[14];
        out[0] = "................";
        for (int y = 1; y < 14; y++) out[y] = body[y - 1];
        return out;
    }

    static String[] armsUp(String[] body) {
        String[] out = body.clone();
        out[10] = ARMS_UP[0];
        out[11] = ARMS_UP[1];
        return out;
    }

    static String[] lean(String[] body) {             // leaning forward for the dash
        String[] out = new String[14];
        for (int y = 0; y < 14; y++) out[y] = y < 12 ? "." + body[y].substring(0, 15) : body[y];
        return out;
    }

    static void drawPip() {
        String[] up = armsUp(BODY);
        put16(0, pip(BODY, "....111..111....", "...1441..1441..."));                       // 0 idle
        put16(2, pip(shiftDown(BODY), "....111..111....", "...1441..1441..."));            // 1 idle, breathing
        put16(4, pip(BODY, "...111....111...", "..1441.....1441."));                       // 2 run: stride
        put16(6, pip(shiftUp(BODY), ".....1111.......", ".....14441......"));              // 3 run: passing (up)
        put16(8, pip(BODY, "....11...111....", "...144...1441..."));                       // 4 run: other stride
        put16(10, pip(up, "....1441.1441...", "................"));                       // 5 jump
        put16(12, pip(up, "...11.....11....", "..144.....441..."));                        // 6 fall
        put16(14, pip(lean(BODY), "..11111.........", ".14411441......."));                // 7 dash
        put16(32, pip(up, "....111.111.....", "....1441441....."));                        // 8 wall slide
        String[] raise = BODY.clone();                                                      // 9 swing: dagger raised behind
        raise[3] = "..7.1bbbbbb1....";
        raise[4] = "..61b3bbbbbb1...";
        raise[5] = "..41bbbbbbbbb11.";
        raise[6] = "..f14ffffff111..";
        raise[10] = "...113bbbbb311..";
        raise[11] = "...113bbbbb31f1.";
        put16(34, pip(raise, "....111..111....", "...1441..1441..."));
        String[] slash = BODY.clone();                                                      // 10 swing: dagger out in front
        slash[10] = "...113bbbbb31f67";
        slash[11] = "..1f13bbbbb31...";
        put16(38, new String[]{"................", "................", ".....111111.....", "....19999991....", "...1999999991...", "...19ffffff91...", "...19f1ff1f91...", "...19ffffff91...", "...199ffff991...", "...1991ff1991...", "...19e1ee1e91...", "..1f1eeeeee1f1..", "....1eeeeee1....", "...1eeeeeeee1...", ".....11..11.....", "....144..441...."});   // Brae
        put16(36, pip(lean(slash), "...111...111....", "..1441...1441..."));
    }

    // terrain: 16 tiles per set, chosen by which sides are open (1 up, 2 down, 4 left, 8 right)
    static void drawTerrain(int set) {
        for (int mask = 0; mask < 16; mask++) {
            String[] rows = new String[8];
            for (int y = 0; y < 8; y++) {
                StringBuilder b = new StringBuilder();
                for (int x = 0; x < 8; x++) b.append(terrainPixel(set, mask, x, y));
                rows[y] = b.toString();
            }
            put(base(set) + mask, rows);
        }
    }

    static char terrainPixel(int set, int mask, int x, int y) {
        boolean up = (mask & 1) != 0, down = (mask & 2) != 0, left = (mask & 4) != 0, right = (mask & 8) != 0;
        if (up && left && x == 0 && y == 0) return '.';      // rounded corners
        if (up && right && x == 7 && y == 0) return '.';
        if (down && left && x == 0 && y == 7) return '.';
        if (down && right && x == 7 && y == 7) return '.';
        if (set == 0) {                                     // grass and dirt
            if (up) {
                if (y == 0) return 'b';
                if (y == 1) return x % 3 == 1 ? '3' : 'b';
                if (y == 2 && (x == 1 || x == 4 || x == 6)) return '3';
            }
            if (left && x == 0) return up && y < 2 ? '3' : '1';
            if (right && x == 7) return up && y < 2 ? '3' : '1';
            if (down && y == 7) return '1';
            if (down && y == 6) return '2';
            int h = (x * 7 + y * 13) % 17;
            if (h == 0) return '9';
            if (h == 5) return '5';
            if (h == 11) return '2';
            return '4';
        }
        if (set == 2) {                                     // slippery ice: pale blue with shine lines
            if (left && x == 0) return '1';
            if (right && x == 7) return '1';
            if (down && y == 7) return '1';
            if (up && y == 0) return '7';
            if (up && y == 1) return x % 4 == 1 ? 'c' : '7';
            if ((x + y) % 7 == 0) return '7';
            if ((x - y + 16) % 9 == 0) return 'd';
            return 'c';
        }
        // stone bricks with snow
        if (up) {
            if (y == 0) return '7';
            if (y == 1) return x % 3 == 2 ? '6' : '7';
            if (y == 2 && (x == 0 || x == 3 || x == 5)) return '6';
        }
        if (left && x == 0) return '1';
        if (right && x == 7) return '1';
        if (down && y == 7) return '1';
        if (y % 4 == 3) return '1';                                 // mortar
        if (x == ((y / 4) % 2 == 0 ? 3 : 7)) return '1';
        if (y % 4 == 0) return 'd';                                 // light top edge of each brick
        return (x + y) % 5 == 0 ? '6' : '5';
    }

    static int base(int set) { return set == 2 ? 128 : 64 + set * 16; }

    static void drawTiles() {
        put(144, new String[]{"1777777.", ".17cc71.", "..1c71..", "..1c7...", "...1c1..", "...1c...", "....1...", "........"}); // falling icicle
        String[] spikes = {"........", "........", ".7...7..", ".6...6..", "676.676.", "66666666", "55555555", "11111111"};
        put(96, spikes);                    // pointing up
        put(97, flipV(spikes));             // down
        put(98, rotate(spikes));            // right
        put(99, flipH(rotate(spikes)));     // left
        put(100, new String[]{"........", "........", "........", ".888888.", ".8eeee8.", "..6..6..", "...66...", ".115511."});  // spring
        put(101, new String[]{".888888.", ".8eeee8.", "..6..6..", "...66...", "..6..6..", "...66...", "..6..6..", ".115511."}); // sprung
        put(102, new String[]{"99999994", "9a999494", "99449994", "94999949", "99949994", "9a499994", "99999494", "44444444"}); // crumble
        put(103, new String[]{"9.999.94", "9a9..494", ".9449.94", "94.999.9", "9.949994", "9a49.9.4", "99.9949.", "4444.444"}); // crumbling
        put(104, new String[]{"14444444", "19999999", "14444444", ".1......", "..1.....", "........", "........", "........"}); // ledge
        put(105, new String[]{"44444444", "99999999", "44444444", "..1..1..", "........", "........", "........", "........"});
        put(106, new String[]{"44444441", "99999991", "44444441", "......1.", ".....1..", "........", "........", "........"});
        put(107, new String[]{"...9....", "..9a9...", ".9aa79..", ".9a77a9.", "9aa77aa9", "9a7777a9", ".9aaaa9.", "..9999.."}); // ember
        put(108, new String[]{"........", "........", "...4....", "..4.4...", ".4...4..", ".4...4..", "..444...", "........"}); // used ember
        put(109, new String[]{"..3bb3..", ".3b77b3.", "3bb7bbb3", "3bbbbbb3", ".3bbbb3.", "..3bb3..", "...33...", "........"}); // shard
        put(110, new String[]{"..1111..", ".1aaaa1.", "1a7aa9a1", "1a7a9aa1", "1aa79aa1", "1aa9aa91", ".19999 1".replace(' ', '1'), "..1111.."}); // coin
        put(112, new String[]{"........", "........", "...8....", "..8a8...", "...8....", "...3....", "..b3....", "...3...."}); // flowers
        put(113, new String[]{"........", "........", "........", "...a....", "..a9a...", "...a....", "...3....", "..3b3..."});
        put(114, new String[]{"........", "........", "........", "........", "........", "...b....", ".b.b.b..", "..bbb..."}); // tuft
        put(115, new String[]{"........", "........", "..888...", ".87888..", ".888878.", "...77...", "...77...", "..6776.."}); // mushroom
        put(116, new String[]{"........", "........", "........", "........", "........", "........", "...56...", "..5556.."}); // pebble
        put(117, new String[]{"........", "........", "....333.", "..33bbb3", ".3bbbbbb", "3bb3bbbb", "3bbbbb3b", "33333333"}); // bush
        put(118, new String[]{"........", "........", ".333....", "3bbb33..", "bbbbbb3.", "bbbb3bb3", "b3bbbbb3", "33333333"});
        String[] trunk = {"......44", "......41", "......44", "......44", "......14", "......44", "......41", "......44"};
        put(119, trunk);
        put(123, flipH(trunk));
        String[] canopyTop = {"....3333", "..33bbbb", ".3bbbbbb", ".3bb3bbb", "3bbbbbbb", "3bbbbb3b", "3b3bbbbb", "3bbbbbbb"};
        String[] canopyBottom = {"3bbbbbbb", "3bbb3bbb", ".3bbbbbb", ".33bbbbb", "...333bb", "......34", "......44", "......44"};
        put(120, canopyTop);
        put(121, flipH(canopyTop));
        put(122, canopyBottom);
        put(124, flipH(canopyBottom));
        put(125, new String[]{"........", "........", "....d...", "...dc...", ".d.dc.d.", ".dcdccd.", "ddcdccdd", "1d1dd1d1"}); // ice crystals
        put(126, new String[]{"........", "........", "........", "........", "........", "...77...", ".777766.", "77776666"}); // snow pile
        put(127, new String[]{".777777.", "..6776..", "..677...", "...67...", "...6....", "...6....", "........", "........"}); // icicle
        put(111, new String[]{"...3....", "...b....", "..3.....", "..b.....", "...3....", "...b3...", "....b...", "...3...."}); // vine
    }

    static void flags() {
        for (int n = 64; n < 96; n++) cart.flags[n] = 1;            // solid
        for (int n = 128; n < 144; n++) cart.flags[n] = 1;          // solid (slippery ice)
        cart.flags[144] = 2;                                         // icicle: hurts
        cart.flags[102] = cart.flags[103] = (byte) (1 | 128);        // solid, crumbles
        for (int n = 96; n <= 99; n++) cart.flags[n] = 2;            // spikes
        cart.flags[110] = 4;                                         // berry
        cart.flags[109] = 8;                                         // shard
        cart.flags[100] = cart.flags[101] = 16;                      // spring
        cart.flags[107] = 32;                                        // crystal
        for (int n = 104; n <= 106; n++) cart.flags[n] = 64;         // one-way ledge
    }

    // ---------- levels ----------

    static final int W = 48, H = 16;
    static char[][] g;

    static void R(int x, int y, int w, int h) {
        for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) if (i >= 0 && i < W && j >= 0 && j < H) g[j][i] = '#';
    }

    static void at(int x, int y, char c) { g[y][x] = c; }

    static void row(int x0, int x1, int y, char c) { for (int x = x0; x <= x1; x++) g[y][x] = c; }

    static void spikesOver(int x0, int x1, int y) {           // spikes on every empty cell of a row
        for (int x = x0; x <= x1; x++) if (g[y][x] == '.') g[y][x] = '^';
    }

    static int berryCount;

    static void level(int n) {
        g = new char[H][W];
        for (char[] r : g) java.util.Arrays.fill(r, '.');
        switch (n) {
            case 0 -> {   // THE MEADOW: running and jumping
                R(0, 13, 10, 3); R(13, 13, 6, 3); R(19, 12, 4, 4); R(23, 10, 5, 6); R(31, 13, 8, 3); R(39, 11, 9, 5);
                row(14, 17, 10, '='); at(15, 9, 'b');
                at(11, 10, 'b');
                row(28, 30, 8, '='); at(29, 7, 'b');
                at(34, 12, '^'); at(35, 12, '^');
                at(45, 10, 'G');
            }
            case 1 -> {   // WINDY RIDGE: the dash and crystals
                R(0, 13, 8, 3); R(15, 13, 6, 3); R(21, 10, 9, 6);
                at(17, 12, '^'); at(18, 12, '^');
                at(11, 9, 'b');
                row(31, 33, 8, 'C');
                R(35, 6, 4, 10); at(37, 2, 'b');
                at(41, 4, 'D');
                R(44, 4, 4, 12);
                at(46, 3, 'G');
            }
            case 2 -> {   // SPRING WOODS: springs, ledges, crumbling blocks
                R(0, 13, 8, 3); at(7, 12, 'S');
                R(8, 15, 6, 1); spikesOver(8, 13, 14);
                row(9, 12, 5, '=');
                R(14, 5, 3, 11);
                row(18, 19, 7, 'C');
                R(21, 8, 6, 8); at(25, 7, 'S'); at(25, 1, 'b');
                R(28, 4, 2, 12);
                R(30, 13, 18, 3);
                at(34, 12, '^'); at(35, 12, '^'); at(36, 12, '^');
                row(38, 41, 10, '='); at(40, 9, 'b');
                at(45, 12, 'G');
            }
            case 3 -> {   // FROZEN CAVES: pillars over spikes, crumbling, crystal chains
                R(0, 0, 48, 2);
                R(0, 12, 7, 4);
                R(7, 15, 30, 1);
                R(11, 11, 2, 4); R(16, 9, 2, 6); R(21, 11, 2, 4);
                spikesOver(7, 36, 14);
                at(17, 5, 'b');
                row(25, 27, 9, 'C');
                at(31, 8, 'D'); at(34, 6, 'D');
                row(14, 19, 2, 'v'); row(28, 33, 2, 'v');
                R(37, 8, 11, 8);
                at(42, 4, 'b');
                at(45, 7, 'G');
            }
            case 4 -> {   // ICE FALLS: slippery floors, icicles that drop when you pass under them
                R(0, 0, 48, 2);
                R(0, 13, 12, 3); at(6, 2, 'I'); at(9, 2, 'I');
                at(13, 9, 'b');
                R(15, 12, 8, 4); at(17, 2, 'I'); at(19, 2, 'I'); at(21, 2, 'I');
                R(23, 15, 4, 1); spikesOver(23, 26, 14); at(25, 8, 'b');
                R(27, 11, 6, 5); at(29, 2, 'I'); at(31, 2, 'I');
                row(34, 35, 9, 'C');
                R(37, 9, 3, 7); at(38, 2, 'I');
                R(40, 11, 8, 5); at(40, 6, 'b'); at(43, 2, 'I');
                at(45, 10, 'G');
            }
            case 5 -> {   // CRYSTAL HOLLOW: no floor, only spikes; dash from crystal to crystal
                R(0, 0, 48, 2);
                R(0, 11, 6, 5);
                R(6, 15, 36, 1);
                R(17, 11, 2, 4); R(28, 11, 2, 4);
                spikesOver(6, 41, 14);
                row(8, 40, 2, 'v');
                at(11, 8, 'D'); at(23, 8, 'D'); at(34, 8, 'D'); at(38, 8, 'D');
                at(17, 9, 'b'); at(29, 5, 'b');
                R(42, 10, 6, 6);
                at(45, 9, 'G');
            }
            case 6 -> {   // THE GLACIER: long slides, a spring, a crumbling bridge
                R(0, 13, 18, 3); at(17, 12, '^'); at(12, 9, 'b');
                R(20, 11, 5, 5); at(24, 10, 'S');
                row(27, 30, 4, '='); at(29, 2, 'b');
                at(33, 6, 'C'); at(34, 6, 'C'); at(36, 7, 'C'); at(37, 7, 'C'); at(39, 8, 'C');
                at(35, 3, 'b');
                R(41, 10, 7, 6);
                at(45, 9, 'G');
            }
            case 8 -> {   // THE YETI: a foggy arena, one screen and a bit
                R(0, 0, 32, 1);
                R(0, 13, 32, 3);
                row(3, 6, 9, '=');
                row(25, 28, 9, '=');
            }
            case 7 -> {   // THE SUMMIT: everything at once
                R(0, 15, 48, 1);
                R(0, 13, 5, 2); at(4, 12, 'S');
                row(6, 9, 6, '='); at(8, 3, 'b');
                R(11, 4, 2, 11);
                at(16, 3, 'D');
                row(20, 22, 4, 'C');
                R(24, 6, 3, 9);
                R(29, 9, 3, 6); at(30, 8, 'S');
                R(33, 2, 4, 1); at(35, 0, 'b');
                row(38, 39, 4, 'C'); row(42, 43, 5, 'C');
                R(45, 6, 3, 9);
                spikesOver(5, 47, 14);
                at(46, 5, 'G');
            }
        }
    }

    static int LW = W;   // this level's width (the arena is narrower)

    static boolean solid(int x, int y) {
        if (x < 0 || x >= LW) return true;        // the level's sides count as walls
        if (y < 0) return false;
        if (y >= H) return true;
        return g[y][x] == '#';
    }

    static void bake(int n, int ox, int oy, Random rnd) {
        int set = new int[]{0, 0, 0, 1, 2, 1, 2, 1, 1}[n];
        int[][] t = new int[H][W];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            char c = g[y][x];
            t[y][x] = switch (c) {
                case '#' -> {
                    int mask = 0;
                    if (!solid(x, y - 1)) mask |= 1;
                    if (!solid(x, y + 1)) mask |= 2;
                    if (!solid(x - 1, y)) mask |= 4;
                    if (!solid(x + 1, y)) mask |= 8;
                    yield base(set) + mask;
                }
                case 'I' -> 144;
                case '^' -> 96;
                case 'v' -> 97;
                case 'S' -> 100;
                case 'C' -> 102;
                case '=' -> (x > 0 && g[y][x - 1] == '=') ? ((x < W - 1 && g[y][x + 1] == '=') ? 105 : 106) : 104;
                case 'D' -> 107;
                case 'G' -> 109;
                case 'b' -> { berryCount++; yield 110; }
                default -> 0;
            };
        }
        // decoration, never on top of anything
        for (int y = 1; y < H; y++) for (int x = 0; x < W; x++) {
            boolean top = g[y][x] == '#' && g[y - 1][x] == '.' && t[y - 1][x] == 0;
            if (!top) continue;
            int r = rnd.nextInt(10);
            if (set == 0) {
                if (n == 2 && r < 2 && x + 1 < W && y >= 4 && x % 7 == 3 && treeFits(t, x, y)) {   // a tree in the woods
                    t[y - 1][x] = 119; t[y - 1][x + 1] = 123;
                    t[y - 2][x] = 119; t[y - 2][x + 1] = 123;
                    t[y - 3][x] = 122; t[y - 3][x + 1] = 124;
                    t[y - 4][x] = 120; t[y - 4][x + 1] = 121;
                    continue;
                }
                if (r == 0 && x + 1 < W && g[y][x + 1] == '#' && g[y - 1][x + 1] == '.' && t[y - 1][x + 1] == 0) {
                    t[y - 1][x] = 117; t[y - 1][x + 1] = 118;
                } else if (r == 1) t[y - 1][x] = 112;
                else if (r == 2) t[y - 1][x] = 113;
                else if (r == 3 || r == 4) t[y - 1][x] = 114;
                else if (r == 5 && n == 2) t[y - 1][x] = 115;
                else if (r == 6) t[y - 1][x] = 116;
            } else {
                if (r < 3) t[y - 1][x] = 126;
                else if (r == 3 && n != 7) t[y - 1][x] = 125;
                else if (r == 4) t[y - 1][x] = 116;
            }
        }
        // icicles and vines under ceilings
        for (int y = 0; y < H - 1; y++) for (int x = 0; x < W; x++) {
            if (g[y][x] != '#' || g[y + 1][x] != '.' || t[y + 1][x] != 0) continue;
            if (set == 1 && rnd.nextInt(3) == 0) t[y + 1][x] = 127;   // (ice levels: only the real, falling icicles)
            if (n == 2 && rnd.nextInt(4) == 0) t[y + 1][x] = 111;
        }
        for (int y = 0; y < H; y++) for (int x = 0; x < LW; x++) cart.map[(oy + y) * Cart.MAP_WIDTH + ox + x] = (byte) t[y][x];
    }

    static boolean treeFits(int[][] t, int x, int y) {
        for (int j = y - 4; j < y; j++) for (int i = x; i <= x + 1; i++) if (g[j][i] != '.' || t[j][i] != 0) return false;
        return g[y][x + 1] == '#';
    }

    // ---------- music: a gentle mountain loop in A minor (Am F C G), sounds 8-19, patterns 0-3 ----------

    static void note(Sound.Sfx s, int step, int pitch, int wave, int vol, int len) {
        for (int i = 0; i < len && step + i < 32; i++) {
            s.pitch[step + i] = pitch;
            s.wave[step + i] = wave;
            s.volume[step + i] = Math.max(1, vol - i);
        }
    }

    static void music() {
        int[][] melody = {
                {33, 36, 40, 36, 33, 35, 36, -1},
                {29, 33, 36, 33, 29, 31, 33, -1},
                {28, 31, 36, 31, 28, 31, 33, 35},
                {31, 35, 38, 35, 31, 33, 35, -1}};
        int[] roots = {9, 5, 0, 7};                 // A, F, C, G (octave 2)
        for (int bar = 0; bar < 4; bar++) {
            Sound.Sfx lead = new Sound.Sfx(), bass = new Sound.Sfx(), sparkle = new Sound.Sfx();
            lead.speed = bass.speed = sparkle.speed = 18;
            for (int k = 0; k < 8; k++) if (melody[bar][k] >= 0) note(lead, k * 4, melody[bar][k], 0, 4, 3);
            for (int k = 0; k < 4; k++) {
                note(bass, k * 8, roots[bar] + 12, 1, 4, 4);
                note(bass, k * 8 + 4, roots[bar] + 19, 1, 3, 3);   // the fifth
            }
            for (int k = 0; k < 4; k++) note(sparkle, k * 8 + 6, melody[bar][(k * 2) % 8 == 7 ? 0 : (k * 2)] + 12, 0, 2, 1);
            cart.sfx[8 + bar] = lead;
            cart.sfx[12 + bar] = bass;
            cart.sfx[16 + bar] = sparkle;
            Sound.Pattern p = new Sound.Pattern();
            p.channels[0] = 8 + bar;
            p.channels[1] = 12 + bar;
            p.channels[2] = 16 + bar;
            p.loopStart = bar == 0;
            p.loopEnd = bar == 3;
            cart.patterns[bar] = p;
        }
    }

    public static void main(String[] args) throws Exception {
        Cart climb = Cart.read(Path.of(args[0]));
        cart = climb;                                           // keeps CLIMB's sounds
        java.util.Arrays.fill(cart.sheet, (byte) 0);
        java.util.Arrays.fill(cart.map, (byte) 0);
        java.util.Arrays.fill(cart.flags, (byte) 0);
        drawPip();
        drawTerrain(0);
        drawTerrain(1);
        drawTerrain(2);
        drawTiles();
        flags();
        music();
        Random rnd = new Random(11);
        for (int n = 0; n < 9; n++) {
            LW = n == 8 ? 32 : W;
            level(n);
            if (n == 8) bake(n, 96, 0, rnd);
            else bake(n, (n % 2) * 48, (n / 2) * 16, rnd);
        }
        cart.code = CODE.replace("BERRY_TOTAL", String.valueOf(berryCount));
        cart.write(Path.of(args[1]));
        System.out.println("berries: " + berryCount);
    }

    static final String CODE = """
// SHARD: PIP AND THE EMERALD HEART
// A / D: run. W: jump (hold for higher; jump at a wall to kick off it).
// Space: dash, aimed with W A S D. Left click: swing (the yeti at the end needs it). Embers give the dash back. R: restart the level.

var LEVELS = [
    {"name": "1. THE MEADOW", "x": 0, "y": 0, "sx": 2, "sy": 13, "sky": 12},
    {"name": "2. WINDY RIDGE", "x": 48, "y": 0, "sx": 2, "sy": 13, "sky": 12},
    {"name": "3. SPRING WOODS", "x": 0, "y": 16, "sx": 2, "sy": 13, "sky": 9},
    {"name": "4. FROZEN CAVES", "x": 48, "y": 16, "sx": 2, "sy": 12, "sky": 1},
    {"name": "5. ICE FALLS", "x": 0, "y": 32, "sx": 2, "sy": 13, "sky": 1},
    {"name": "6. CRYSTAL HOLLOW", "x": 48, "y": 32, "sx": 2, "sy": 11, "sky": 2},
    {"name": "7. THE GLACIER", "x": 0, "y": 48, "sx": 2, "sy": 13, "sky": 1},
    {"name": "8. THE SUMMIT", "x": 48, "y": 48, "sx": 1, "sy": 13, "sky": 13},
    {"name": "9. THE YETI", "x": 96, "y": 0, "sx": 3, "sy": 13, "sky": 1, "w": 32, "boss": 1}
]
var STORY = [
    ["LONG AGO THE EMERALD HEART KEPT THE MOUNTAIN GREEN AND WARM.",
     "THE VALLEY FOLK SAID IT BEAT ONCE A YEAR. AND EVERY TIME IT DID. SPRING CAME.",
     "BUT THIS YEAR SPRING NEVER CAME. THE RIVERS FROZE. THE FIELDS WENT GREY.",
     "BRAM: PIP! THE HEART HAS CRACKED. I SAW THE PIECES FLY UP THE MOUNTAIN.",
     "BRAM: NINE GREEN LIGHTS. LIKE SHOOTING STARS GOING THE WRONG WAY.",
     "BRAM: MY KNEES ARE TOO OLD FOR CLIMBING. BUT YOURS ARE NOT.",
     "BRAM: TAKE MY RED SCARF. IT WILL KEEP YOU WARM. NOW GO!",
     "A AND D TO RUN. W TO JUMP. HOLD IT TO JUMP HIGHER."],
    ["THE FIRST SHARD IS WARM IN PIPS HANDS. EIGHT MORE TO GO.",
     "ON THE RIDGE THE WIND HOWLS. THE GAPS ARE FAR TOO WIDE TO JUMP.",
     "BRAM (FAR BELOW): USE THE SCARF! IT REMEMBERS THE HEART. IT WILL CARRY YOU!",
     "PRESS Q TO DASH. AIM WITH W A S D.",
     "ONE DASH IN THE AIR. LAND OR TOUCH AN EMBER TO GET IT BACK."],
    ["THE WOODS ARE QUIET. TOO QUIET. EVEN THE BIRDS HAVE GONE.",
     "SOMEONE HAS LEFT RED SPRINGS ALL OVER THE PATH. WHO WOULD DO THAT?",
     "RED SPRINGS THROW YOU HIGH. JUMP AT A WALL IN THE AIR TO KICK OFF IT.",
     "SANDY BLOCKS CRUMBLE WHEN YOU STAND ON THEM. DO NOT WAIT AROUND."],
    ["THE PATH DIVES UNDER THE ICE. IT IS SO COLD THAT PIPS BREATH HANGS IN THE AIR.",
     "A VOICE ECHOES IN THE DARK: WHO GOES THERE?",
     "A SNOWY OWL DROPS DOWN. WREN: A CHILD? UP HERE? YOU MUST BE AFTER THE SHARDS.",
     "WREN: FOUR OF THEM FELL DEEP INTO THE ICE. I WILL SHOW YOU THE WAY.",
     "WREN: MIND THE CEILING. THE ICE HAS TEETH."],
    ["WREN: THESE ARE THE ICE FALLS. THE FLOOR IS AS SLICK AS GLASS.",
     "WREN: YOU WILL SLIDE. START RUNNING EARLY. STOP EARLY.",
     "WREN: AND LISTEN FOR CRACKING ABOVE YOU.",
     "WREN: ICICLES FALL WHEN SOMETHING WARM WALKS UNDER THEM.",
     "PIP: SOMETHING WARM?",
     "WREN: YOU. KEEP MOVING."],
    ["WREN: THE CRYSTAL HOLLOW. THE HEARTS WARMTH STILL FLOATS HERE AS EMBERS.",
     "WREN: THERE IS NO FLOOR HERE. ONLY SPIKES.",
     "WREN: YOU WILL HAVE TO FLY FROM EMBER TO EMBER.",
     "PIP: I CAN NOT FLY.",
     "WREN: YOU CAN DASH. AND EVERY EMBER GIVES IT BACK. THAT IS CLOSE ENOUGH.",
     "WREN: DO NOT LOOK DOWN."],
    ["THE HOLLOW OPENS ONTO A GLACIER. FAR ABOVE. THE SUMMIT GLOWS GREEN.",
     "WREN: PIP. I HAVE TO TELL YOU SOMETHING. THE HEART DID NOT CRACK BY ITSELF.",
     "WREN: THE MOUNTAIN WAS LONELY. EVERY YEAR THE VALLEY TOOK ITS SPRING...",
     "WREN: ...AND NOBODY EVER CLIMBED UP TO SAY THANK YOU.",
     "WREN: SO IT BROKE ITS OWN HEART...",
     "WREN: ...AND HID THE PIECES WHERE ONLY SOMEONE WHO CARED WOULD GO.",
     "PIP: THEN I WILL GO ALL THE WAY UP. AND I WILL SAY IT.",
     "WREN: THEN HURRY. THE GLACIER IS CRACKING."],
    ["THE SUMMIT. THE WIND IS LOUD AND THE SNOW IS DEEP. ANOTHER SHARD SHINES ABOVE.",
     "WREN: I CAN NOT FLY IN THIS WIND. FROM HERE YOU GO ALONE.",
     "BRAM (VERY FAR BELOW): GO ON. PIP!",
     "ONE MORE CLIMB."],
    ["PIP REACHES THE TOP. THE LAST SHARD SHOULD BE HERE. BUT THE SNOW IS EMPTY.",
     "THEN THE FOG MOVES. TWO RED EYES OPEN IN THE WHITE.",
     "A YETI AS BIG AS A HOUSE. AND IN ITS FOREHEAD... THE LAST SHARD. GLOWING.",
     "WREN (FAR AWAY): PIP! THE SHARD HAS MADE IT WILD!",
     "WREN: FREE THE SHARD AND YOU FREE THE YETI!",
     "WHEN IT FALLS OR CRASHES INTO A WALL IT IS DIZZY. THAT IS WHEN YOU STRIKE.",
     "LEFT CLICK TO SWING AT THE SHARD. JUMP OVER THE SHOCKWAVES."]
]
var ENDING = [
    "THE YETI BLINKS. ITS EYES ARE GENTLE NOW. IT LOOKS CONFUSED. AND VERY FLUFFY.",
    "THE NINE SHARDS CLICK TOGETHER IN PIPS HANDS. THE HEART BEATS ONCE.",
    "PIP: THANK YOU. MOUNTAIN. FOR EVERY SPRING.",
    "THE HEART BEATS AGAIN. LOUDER. THE SNOW AROUND PIP MELTS INTO WATER.",
    "WARMTH ROLLS DOWN THE SLOPES. THE GLACIER SIGHS. THE RIVERS START TO RUN.",
    "FAR BELOW. BRAM FEELS THE SUN ON HIS FACE. AND LAUGHS.",
    "WREN: YOU DID IT. AND THE MOUNTAIN HEARD YOU.",
    "FROM THAT YEAR ON. THE VALLEY CLIMBED THE MOUNTAIN EVERY SPRING. TO SAY THANK YOU.",
    "THE END."
]
var RUN = [2, 3, 4, 3]
var BERRIES = BERRY_TOTAL
var W = 48
var H = 16
var SW = 228          // the screen is wide (widescreen): 228 x 128
var OX = 50           // how far to shift things drawn for a 128-wide screen to centre them

var mode = "title"
var level = 0
var lines = []
var freeze = 0
var levelTime = 0
var px = 0
var py = 0
var vx = 0
var vy = 0
var facing = 1
var grounded = false
var wall = 0
var coyote = 0
var buffer = 0
var canDash = true
var dashTime = 0
var wallLock = 0
var lastJump = -1
var steps = 0
var dead = 0
var won = 0
var shards = 0
var berries = 0
var deaths = 0
var startTime = 0
var finishTime = 0
var broken = []
var crystals = []
var springs = []
var icicles = []
var scarf = []
var snow = []

func init() {
    widescreen(true)
    cartdata("pip")
    loadSettings()
    startMusic()
    var i = 0
    while i < 5 {
        push(scarf, {"x": 0, "y": 0})
        i += 1
    }
    i = 0
    while i < 40 {
        push(snow, {"x": rnd(228), "y": rnd(128), "s": 0.3 + rnd(0.7)})
        i += 1
    }
}

// ---------- the map ----------

func tileAt(x, y) {
    var tx = flr(x / 8)
    var ty = flr(y / 8)
    if tx < 0 or tx >= W { return 64 }
    if ty < 0 or ty >= H { return 0 }
    var L = LEVELS[level]
    return mget(L["x"] + tx, L["y"] + ty)
}
func flagAt(x, y, f) { return fget(tileAt(x, y), f) }
func solidAt(x, y) { return fget(tileAt(x, y), 0) }
func setTile(tx, ty, n) {
    var L = LEVELS[level]
    mset(L["x"] + tx, L["y"] + ty, n)
}

// Pip's body is a box 6 wide and 12 tall, from (px, py)
func hitsSolid(x, y) {
    return solidAt(x, y) or solidAt(x + 5, y) or solidAt(x, y + 6) or solidAt(x + 5, y + 6) or solidAt(x, y + 11) or solidAt(x + 5, y + 11)
}
// a tile with this flag that Pip touches (a little inside the box, so spikes are fair), or none
func touched(f) {
    for c in [[1, 2], [4, 2], [1, 10], [4, 10], [2, 6]] {
        var x = px + c[0]
        var y = py + c[1]
        if flagAt(x, y, f) { return {"x": flr(x / 8), "y": flr(y / 8)} }
    }
    return none
}

// ---------- levels ----------

func startGame() {
    shards = 0
    berries = 0
    deaths = 0
    level = 0
    mode = "play"
    startTime = t()
    startLevel()
    lines = copy(STORY[0])
}

// puts back the crumbled blocks, used crystals and springs of this level
func resetTiles() {
    for b in broken { setTile(b["x"], b["y"], 102) }
    for c in crystals { setTile(c["x"], c["y"], 107) }
    for s in springs { setTile(s["x"], s["y"], 100) }
    for i in icicles { setTile(i["x"], i["y"], 144) }
    broken = []
    crystals = []
    springs = []
    icicles = []
}

func startLevel() {
    resetTiles()
    var L = LEVELS[level]
    W = 48
    if has(L, "w") { W = L["w"] }
    if isBoss() { initBoss() }
    // find the icicles that can fall
    var ty = 0
    while ty < H {
        var tx = 0
        while tx < W {
            if mget(L["x"] + tx, L["y"] + ty) == 144 { push(icicles, {"x": tx, "y": ty, "state": 0, "t": 0, "fy": ty * 8, "v": 0}) }
            tx += 1
        }
        ty += 1
    }
    px = L["sx"] * 8 + 1
    py = L["sy"] * 8 - 12
    vx = 0
    vy = 0
    facing = 1
    canDash = true
    dashTime = 0
    wallLock = 0
    dead = 0
    levelTime = 0
    for s in scarf {
        s["x"] = px
        s["y"] = py + 5
    }
}

func nextLevel() {
    resetTiles()
    level += 1
    if level >= len(LEVELS) {
        level = len(LEVELS) - 1
        mode = "epilogue"
        page = 0
        pageTime = 0
        finishTime = t() - startTime
        if dget(0) == 0 or finishTime < dget(0) { dset(0, finishTime) }
        lines = copy(ENDING)
        return
    }
    startLevel()
    lines = copy(STORY[level])
}

func die() {
    if dead > 0 { return }
    if false {
        // never die: bounce back out of pits instead
        if py > H * 8 - 16 {
            py = H * 8 - 24
            vy = -6
            canDash = true
        }
        return
    }
    burst(px + 3, py + 6, 24, 8, 1.6)
    burst(px + 3, py + 6, 10, 7, 1)
    shake(5)
    play(4)
    deaths += 1
    dead = 30
}

// ---------- every frame ----------

func update() {
    if mode == "settings" {
        updateSettings()
        return
    }
    if mode == "title" {
        if keyp("p") {
            openSettings("title")
            return
        }
        updateTitle()
        return
    }
    // story boxes, one at a time; the game waits while one is open
    if saying() { return }
    if len(lines) > 0 {
        say(remove(lines, 0))
        freeze = 8
        return
    }
    if mode == "epilogue" {
        updateEpilogue()
        return
    }
    if mode == "end" {
        if freeze > 0 {
            freeze -= 1
            return
        }
        if btnp(4) or keyp("space") { mode = "title" }
        return
    }
    if freeze > 0 {
        freeze -= 1
        return
    }
    if flash > 0 { flash -= 1 }
    if hitstop > 0 {
        hitstop -= 1
        return
    }
    levelTime += 1
    updateTiles()
    if won > 0 {
        won -= 1
        if won == 0 { nextLevel() }
        return
    }
    if dead > 0 {
        dead -= 1
        if dead == 0 { startLevel() }
        return
    }
    if keyp("r") {
        die()
        return
    }
    if keyp("p") {
        openSettings("play")
        return
    }
    updatePip()
    if isBoss() { updateBoss() }
}

func updateTiles() {
    for b in broken {
        if b["t"] > 0 {
            b["t"] -= 1
            if b["t"] == 0 {
                setTile(b["x"], b["y"], 0)
                burst(b["x"] * 8 + 4, b["y"] * 8 + 4, 8, 9, 0.8)
                b["back"] = 140
            }
        } else if b["back"] > 0 {
            b["back"] -= 1
            if b["back"] == 0 {
                // grow back, unless Pip is standing in the way
                if abs(b["x"] * 8 + 4 - (px + 3)) < 8 and abs(b["y"] * 8 + 4 - (py + 6)) < 11 {
                    b["back"] = 10
                } else {
                    setTile(b["x"], b["y"], 102)
                }
            }
        }
    }
    for c in crystals {
        if c["t"] > 0 {
            c["t"] -= 1
            if c["t"] == 0 {
                setTile(c["x"], c["y"], 107)
                burst(c["x"] * 8 + 4, c["y"] * 8 + 3, 6, 9, 0.5)
            }
        }
    }
    updateIcicles()
    for s in springs {
        if s["t"] > 0 {
            s["t"] -= 1
            if s["t"] == 0 { setTile(s["x"], s["y"], 100) }
        }
    }
}

// icicles: shake when Pip passes under, then fall; they shatter on the ground
func updateIcicles() {
    for i in icicles {
        var ix = i["x"] * 8
        if i["state"] == 0 and dead == 0 and px + 6 > ix - 6 and px < ix + 14 and py > i["y"] * 8 {
            i["state"] = 1
            i["t"] = 14
        } else if i["state"] == 1 {
            i["t"] -= 1
            if i["t"] == 0 {
                i["state"] = 2
                setTile(i["x"], i["y"], 0)
            }
        } else if i["state"] == 2 {
            i["v"] = min(i["v"] + 0.3, 5)
            i["fy"] += i["v"]
            if dead == 0 and abs(ix + 4 - (px + 3)) < 6 and i["fy"] + 7 > py and i["fy"] < py + 12 { die() }
            if solidAt(ix + 4, i["fy"] + 8) or i["fy"] > H * 8 {
                i["state"] = 3
                burst(ix + 4, i["fy"] + 6, 10, 7, 1)
                burst(ix + 4, i["fy"] + 6, 6, 12, 0.8)
                play(4)
            }
        }
    }
}

func drawIcicles() {
    for i in icicles {
        if i["state"] == 1 { spr(144, i["x"] * 8 + rnd(2) - 1, i["y"] * 8) }
        if i["state"] == 2 { spr(144, i["x"] * 8, i["fy"]) }
    }
}

func crumble(tx, ty) {
    if tileAt(tx * 8, ty * 8) != 102 { return }
    setTile(tx, ty, 103)
    push(broken, {"x": tx, "y": ty, "t": 18, "back": 0})
}

func onLedge() {
    var feet = py + 12
    if feet - flr(feet / 8) * 8 > 1 { return false }
    return flagAt(px, feet, 6) or flagAt(px + 5, feet, 6)
}

func updatePip() {
    var dx = 0
    if btn(0) or held("left") { dx = -1 }
    if btn(1) or held("right") { dx = 1 }
    var jumpHeld = btn(4) or btn(2) or held("jump")
    var dashPressed = btnp(5) or hit("dash")
    if btnp(4) or btnp(2) or hit("jump") { buffer = 6 }
    // left click: swing
    if atkCool > 0 { atkCool -= 1 }
    if atkTime > 0 { atkTime -= 1 }
    if hit("swing") and atkCool == 0 {
        atkTime = 8
        atkCool = 14
        play(2)
        burst(px + 3 + facing * 9, py + 6, 4, 7, 0.8)
        if isBoss() { strike() }
    }

    grounded = vy >= 0 and (solidAt(px, py + 12) or solidAt(px + 5, py + 12) or onLedge())
    if grounded {
        coyote = 6
        if dashTime == 0 { canDash = true }
    }
    if grounded {
        if flagAt(px + 1, py + 12, 7) { crumble(flr((px + 1) / 8), flr((py + 12) / 8)) }
        if flagAt(px + 4, py + 12, 7) { crumble(flr((px + 4) / 8), flr((py + 12) / 8)) }
    } else if coyote > 0 {
        coyote -= 1
    }
    wall = 0
    if not grounded {
        if hitsSolid(px + 1, py) {
            wall = 1
        } else if hitsSolid(px - 1, py) {
            wall = -1
        }
    }

    // jumping: off the ground (a moment after leaving it still counts), or off a wall
    if buffer > 0 and dashTime == 0 {
        if coyote > 0 {
            vy = -3.2
            buffer = 0
            coyote = 0
            play(0)
            burst(px + 3, py + 12, 5, 6, 0.6)
        } else if wall != 0 {
            vy = -3.0
            vx = -wall * 2.2
            facing = -wall
            wallLock = 10
            buffer = 0
            play(0)
            burst(px + 3 + wall * 3, py + 8, 5, 6, 0.6)
        }
    }
    if buffer > 0 { buffer -= 1 }

    // dashing
    if dashPressed and canDash {
        var ddx = dx
        var ddy = 0
        if btn(2) or held("jump") { ddy = -1 }
        if btn(3) or held("down") { ddy = 1 }
        if ddx == 0 and ddy == 0 { ddx = facing }
        var speed = 3.4
        if ddx != 0 and ddy != 0 { speed = 2.6 }
        vx = ddx * speed
        vy = ddy * speed
        if ddx != 0 { facing = ddx }
        dashTime = 9
        canDash = false
        wallLock = 0
        shake(2)
        play(1)
        burst(px + 3, py + 6, 8, 9, 1)
    }

    if dashTime > 0 {
        dashTime -= 1
        if dashTime % 2 == 0 { burst(px + 3, py + 6, 2, pick([9, 10]), 0.3) }
        if dashTime == 0 {
            vx = vx * 0.5
            vy = vy * 0.5
        }
    } else {
        if dx != 0 and wallLock == 0 { facing = dx }
        if wallLock > 0 {
            wallLock -= 1
        } else {
            var grip = 0.18
            if grounded { grip = 0.35 }
            var under = tileAt(px + 3, py + 12)
            if grounded and under >= 128 and under < 144 { grip = 0.035 }   // ice: slow to start, slow to stop
            vx += (dx * 1.4 - vx) * grip
        }
        var g = 0.2
        if abs(vy) < 0.6 and jumpHeld { g = 0.1 }
        if vy < 0 and not jumpHeld { g = 0.4 }
        vy += g
        if wall != 0 and dx == wall and vy > 0.8 { vy = 0.8 }
        vy = min(vy, 3.6)
    }

    if moveX(vx) { vx = 0 }
    moveY(vy)

    if grounded and abs(vx) > 0.3 { steps += 1 } else { steps = 0 }

    // what Pip touches
    if touched(1) != none or py > H * 8 {
        die()
        return
    }
    var s = touched(4)
    if s != none and vy >= 0 {
        vy = -5.4
        dashTime = 0
        canDash = true
        coyote = 0
        setTile(s["x"], s["y"], 101)
        push(springs, {"x": s["x"], "y": s["y"], "t": 10})
        play(0)
        burst(s["x"] * 8 + 4, s["y"] * 8 + 4, 6, 8, 0.8)
        shake(1)
    }
    var c = touched(5)
    if c != none and not canDash {
        canDash = true
        setTile(c["x"], c["y"], 108)
        push(crystals, {"x": c["x"], "y": c["y"], "t": 150})
        play(3)
        burst(c["x"] * 8 + 4, c["y"] * 8 + 3, 10, 10, 1)
    }
    var b = touched(2)
    if b != none {
        setTile(b["x"], b["y"], 0)
        berries += 1
        play(3)
        burst(b["x"] * 8 + 4, b["y"] * 8 + 4, 10, 10, 1)
    }
    var gem = touched(3)
    if gem != none {
        setTile(gem["x"], gem["y"], 0)
        shards += 1
        play(5)
        shake(3)
        burst(gem["x"] * 8 + 4, gem["y"] * 8 + 4, 30, 11, 1.5)
        burst(gem["x"] * 8 + 4, gem["y"] * 8 + 4, 15, 7, 1)
        won = 60
    }

    // the scarf: each piece follows the one before, blowing back
    var hx = px + 3 - facing * 2
    var hy = py + 6
    for p in scarf {
        p["x"] += (hx - p["x"]) * 0.45
        p["y"] += (hy - p["y"]) * 0.45
        hx = p["x"] - facing * 1.2
        hy = p["y"] + 0.4 + sin(t() * 9 + hx) * 0.3
    }
}

func moveX(amount) {
    var nx = px + amount
    if not hitsSolid(nx, py) {
        px = nx
        return false
    }
    var s = sign(amount)
    var guard = 0
    while not hitsSolid(px + s, py) and guard < 8 {
        px += s
        guard += 1
    }
    return true
}

func moveY(amount) {
    // ledges hold Pip only when coming down onto them (hold down to drop through)
    if amount > 0 and not (btn(3) or held("down")) {
        var feet = py + 12
        var after = feet + amount
        var top = flr(after / 8) * 8
        if feet <= top and after >= top and (flagAt(px, after, 6) or flagAt(px + 5, after, 6)) {
            py = top - 12
            vy = 0
            return
        }
    }
    var ny = py + amount
    if not hitsSolid(px, ny) {
        py = ny
        return
    }
    var s = sign(amount)
    var guard = 0
    while not hitsSolid(px, py + s) and guard < 8 {
        py += s
        guard += 1
    }
    if s > 0 { py = flr((py + 13) / 8) * 8 - 12 }
    vy = 0
}

// ---------- settings ----------

var KEYS = ["a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "space", "enter", "shift", "up", "down", "left", "right", "click"]
var ACTIONS = [["LEFT", "left"], ["RIGHT", "right"], ["JUMP", "jump"], ["DOWN", "down"], ["DASH", "dash"], ["SWING", "swing"]]
var binds = {"left": "a", "right": "d", "jump": "w", "down": "s", "dash": "q", "swing": "click"}
var musicOn = true
var sfxOn = true
var menu = 0
var waiting = false
var backTo = "title"

// cartdata slots: 0 best time, 1-6 keys, 7 music off, 8 sound off
func loadSettings() {
    var i = 0
    while i < len(ACTIONS) {
        var v = dget(i + 1)
        if v > 0 and v <= len(KEYS) { binds[ACTIONS[i][1]] = KEYS[v - 1] }
        i += 1
    }
    musicOn = dget(7) == 0
    sfxOn = dget(8) == 0
}

func saveSettings() {
    var i = 0
    while i < len(ACTIONS) {
        dset(i + 1, indexOf(KEYS, binds[ACTIONS[i][1]]) + 1)
        i += 1
    }
    dset(7, musicOn ? 0 : 1)
    dset(8, sfxOn ? 0 : 1)
}

func play(n) {
    if sfxOn { sfx(n) }
}

func startMusic() {
    if musicOn { music(0) } else { music(-1) }
}

// the player's keys (a key name, or "click" for the left mouse button)
func held(a) {
    var k = binds[a]
    if k == "click" { return mousedown() }
    return key(k)
}
func hit(a) {
    var k = binds[a]
    if k == "click" { return click() }
    return keyp(k)
}

func openSettings(from) {
    backTo = from
    mode = "settings"
    menu = 0
    waiting = false
}

func updateSettings() {
    if waiting {
        // the next key (or click) pressed becomes the new key
        for k in KEYS {
            if k == "click" {
                if click() {
                    bindTo(k)
                    return
                }
            } else if keyp(k) {
                bindTo(k)
                return
            }
        }
        return
    }
    if btnp(2) { menu = (menu + 8) % 9 }
    if btnp(3) { menu = (menu + 1) % 9 }
    var my = mousey()
    var onRow = mousex() - OX > 8 and mousex() - OX < 120 and my >= 22 and my < 22 + 9 * 10
    if onRow and click() {
        menu = flr((my - 22) / 10)
        choose()
        return
    }
    if keyp("enter") or btnp(4) { choose() }
    if keyp("p") {
        mode = backTo
        freeze = 6
    }
}

func choose() {
    if menu < 6 {
        waiting = true
    } else if menu == 6 {
        musicOn = not musicOn
        startMusic()
        saveSettings()
    } else if menu == 7 {
        sfxOn = not sfxOn
        saveSettings()
        play(3)
    } else {
        mode = backTo
        freeze = 6
    }
}

func bindTo(k) {
    binds[ACTIONS[menu][1]] = k
    waiting = false
    saveSettings()
    play(3)
}

func drawSettings() {
    cls(1)
    menuCard("SETTINGS")
    var i = 0
    while i < 9 {
        var y = 24 + i * 9
        if i >= 6 { y += 6 }
        if i == 8 { y += 4 }
        var label = "BACK"
        var value = ""
        if i < 6 {
            label = ACTIONS[i][0]
            value = upper(binds[ACTIONS[i][1]])
            if waiting and menu == i { value = "PRESS..." }
        } else if i == 6 {
            label = "MUSIC"
            value = musicOn ? "ON" : "OFF"
        } else if i == 7 {
            label = "SOUND"
            value = sfxOn ? "ON" : "OFF"
        }
        menuRow(i == menu, label, value, y)
        i += 1
    }
    print("ENTER / CLICK    P: BACK", 18, 116, 5)
}

// ---------- the epilogue: storybook pages in a gold frame ----------

var EPILOGUE = [
    "AND LATER...",
    "SPRING CAME BACK. THE FIELDS WENT GREEN AND THE RIVERS RAN LOUD.",
    "PIP AND HIS FRIEND BRAE SPENT THE WARM DAYS BY THE WATERFALL.",
    "BRAM TOLD THE STORY TO ANYONE WHO WOULD SIT STILL LONG ENOUGH.",
    "THE YETI MOVED IN WITH WREN. TERRIBLE COOK. GREAT HUGGER.",
    "UNTIL ONE EVENING... PIP AND BRAE LOOKED UP AT THE MOUNTAIN.",
    "SOMETHING WAS LOOKING BACK.",
    "THE END?"
]
var page = 0
var pageTime = 0

// splits text into lines of at most n letters
func wrap(text, n) {
    var out = []
    var line = ""
    for word in split(text, " ") {
        if len(line) > 0 and len(line) + 1 + len(word) > n {
            push(out, line)
            line = word
        } else if len(line) > 0 {
            line = line + " " + word
        } else {
            line = word
        }
    }
    if len(line) > 0 { push(out, line) }
    return out
}

func updateEpilogue() {
    pageTime += 1
    var full = len(EPILOGUE[page])
    var next = btnp(4) or btnp(5) or keyp("enter") or hit("jump") or hit("dash") or click()
    if pageTime > 10 and next {
        if pageTime * 0.7 < full {
            pageTime = flr(full / 0.7) + 1
        } else {
            page += 1
            pageTime = 0
            play(3)
            if page >= len(EPILOGUE) {
                mode = "end"
                freeze = 10
            }
        }
    }
}

func drawFrame() {
    rectfill(4, 4, 123, 123, 1)
    rect(4, 4, 123, 123, 9)
    rect(6, 6, 121, 121, 10)
    rect(8, 8, 119, 119, 4)
    // vines of little dots along the edges
    var i = 14
    while i < 114 {
        var w = sin(i * 0.3 + t()) * 1.5
        pset(i, 11 + w, 3)
        pset(i, 116 + w, 3)
        pset(11 + w, i, 3)
        pset(116 + w, i, 3)
        if i % 12 == 2 {
            pset(i, 11 + w, 11)
            pset(i, 116 + w, 11)
            pset(11 + w, i, 11)
            pset(116 + w, i, 11)
        }
        i += 3
    }
    // shards in the corners, gently bobbing
    var b = sin(t() * 2) * 1
    spr(109, 2, 2 + b)
    spr(109, 118, 2 - b)
    spr(109, 2, 118 - b)
    spr(109, 118, 118 + b)
}

func drawEpilogue() {
    cls(0)
    var last = len(EPILOGUE) - 1
    drawFrame()
    var text = EPILOGUE[page]
    var shown = flr(pageTime * 0.7)
    var rows = wrap(text, 24)
    var y = 64 - len(rows) * 4
    var count = 0
    for r in rows {
        var part = r
        if count + len(r) > shown { part = slice(r, 0, max(0, shown - count)) }
        if count < shown { print(part, 64 - len(r) * 2, y, page == 0 or page == last ? 10 : 7) }
        count += len(r) + 1
        y += 7
    }
}

// a filled circle kept inside the picture frame
func blob(cx, cy, r, c) {
    var dy = -r
    while dy <= r {
        var y = cy + dy
        if y >= 9 and y <= 118 {
            var hw = sqrt(r * r - dy * dy)
            line(max(9, cx - hw), y, min(118, cx + hw), y, c)
        }
        dy += 1
    }
}

// Pip and Brae by the waterfall
func drawWaterfall() {
    rectfill(9, 9, 118, 118, 12)
    circfill(30, 30, 8, 10)
    blob(40, 96, 40, 3)
    // cliff and the waterfall
    rectfill(76, 16, 118, 78, 5)
    rectfill(76, 16, 118, 18, 11)
    var x = 84
    while x <= 96 {
        line(x, 18, x, 76, 12)
        var y = (t() * 60 + x * 7) % 12
        while y < 58 {
            line(x, 18 + y, x, 21 + y, 7)
            y += 12
        }
        x += 1
    }
    rectfill(66, 76, 118, 82, 12)
    var f = 0
    while f < 8 {
        pset(82 + rnd(18), 75 + rnd(4), 7)
        f += 1
    }
    // grass
    rectfill(9, 80, 66, 118, 3)
    rectfill(9, 80, 66, 81, 11)
    rectfill(66, 82, 118, 118, 3)
    var g = 10
    while g < 118 {
        var top = g < 66 ? 80 : 82
        line(g, top, g + sin(t() * 2 + g) * 1.5, top - 3, 11)
        if g % 9 == 0 { pset(g, top - 4, g % 2 == 0 ? 8 : 10) }
        g += 3
    }
    // the two of them
    spr(flr(t() * 2) % 2 * 2, 30, 64, 2, 2)
    spr(38, 46, 64, 2, 2, true)
}

// they look up: the mountain, and something on top of it
func drawMountain() {
    var last = len(EPILOGUE) - 1
    rectfill(9, 9, 118, 118, 2)
    var s = 0
    while s < 20 {
        pset(12 + (s * 37) % 104, 12 + (s * 23) % 40, 7)
        s += 1
    }
    var my = 50
    while my <= 118 {
        var half = (my - 50) * 0.9
        line(max(9, 64 - half), my, min(118, 64 + half), my, my < 62 ? 7 : 5)
        my += 1
    }
    // the figure on the peak, red eyes; bigger on the last page before the end
    var big = page == last - 1
    var fr = big ? 6 : 3
    var fy = 50 - fr * 2 - 1
    circfill(64, fy, fr, 0)
    rectfill(64 - fr, fy, 64 + fr, fy + fr * 2, 0)
    if flr(t() * 3) % 9 != 0 {
        pset(62 - (big ? 1 : 0), fy, 8)
        pset(66 + (big ? 1 : 0), fy, 8)
    }
    // Pip and Brae, from behind, looking up
    spr(10, 18, 74, 2, 2)
    spr(38, 94, 74, 2, 2, true)
    // the last page fades to black
    if big and pageTime > 60 {
        var amount = min(1, (pageTime - 60) / 60)
        var y = 9
        while y < 119 {
            if (y % 4) / 4 < amount { line(9, y, 118, y, 0) }
            y += 1
        }
        if amount >= 1 { rectfill(9, 9, 118, 118, 0) }
    }
}

// ---------- the yeti ----------

var FLOOR = 104
var bx = 0
var by = 0
var bvx = 0
var bvy = 0
var bhp = 0
var bstate = "intro"
var btimer = 0
var bface = -1
var bflash = 0
var banger = 0
var bstep = 0
var hitstop = 0
var flash = 0
var atkTime = 0
var atkCool = 0
var balls = []
var waves = []
var rings = []
var drops = []
var fog = []
var streaks = []
var shardDrop = none

// one item from a list, at random
func pick(list) { return list[flr(rnd(len(list)))] }

func isBoss() { return has(LEVELS[level], "boss") }

func initBoss() {
    bx = 112
    by = FLOOR
    bvx = 0
    bvy = 0
    bhp = 4
    banger = 0
    bstate = "intro"
    btimer = 100
    bface = -1
    bflash = 0
    balls = []
    waves = []
    rings = []
    drops = []
    shardDrop = none
    if len(fog) == 0 {
        var i = 0
        while i < 10 {
            push(fog, {"x": rnd(320), "y": 16 + rnd(100), "r": 10 + rnd(14), "s": 0.2 + rnd(0.5)})
            i += 1
        }
        i = 0
        while i < 34 {
            push(streaks, {"x": rnd(228), "y": rnd(128), "s": 2 + rnd(3)})
            i += 1
        }
    }
}

// where the shard in his head is: high up, or low and forward when he is dizzy
func headX() {
    if bstate == "stunned" { return bx + bface * 12 }
    return bx + bface * 3
}
func headY() {
    if bstate == "stunned" { return by - 22 }
    return by - 46
}

func hitPip(x, y, r) {
    return dead == 0 and x + r > px and x - r < px + 6 and y + r > py and y - r < py + 12
}

func dropIcicles(n) {
    var i = 0
    while i < n {
        push(drops, {"x": 16 + rnd(224), "y": 8, "v": 0, "wait": 20 + i * 10})
        i += 1
    }
}

func pickAttack() {
    var r = rnd(3)
    if r < 1 {
        bstate = "crouch"
        btimer = 38 - banger * 4
    } else if r < 2 {
        bstate = "windup"
        btimer = 38 - banger * 4
    } else {
        bstate = "rev"
        bface = px + 3 < bx ? -1 : 1
        btimer = 48 - banger * 5
    }
}

func land() {
    shake(10)
    play(4)
    flash = 2
    push(waves, {"x": bx - 16, "d": -1, "life": 90})
    push(waves, {"x": bx + 16, "d": 1, "life": 90})
    push(rings, {"x": bx, "y": by, "r": 6})
    burst(bx, by - 2, 25, 7, 2)
    dropIcicles(1 + banger)
    bstate = "stunned"
    btimer = 160 - banger * 15
}

// Pip's swing: it only hurts the yeti when he is dizzy and the swing reaches the shard
func strike() {
    if bstate == "dying" or bstate == "dead" or bstate == "intro" { return }
    var sx = px + 3 + facing * 10
    var sy = py + 6
    if abs(sx - headX()) < 20 and abs(sy - headY()) < 20 {
        if bstate == "stunned" {
            bhp -= 1
            hitstop = 6
            flash = 3
            bflash = 12
            shake(7)
            play(5)
            burst(headX(), headY(), 25, 11, 2)
            burst(headX(), headY(), 15, 7, 1.5)
            push(rings, {"x": headX(), "y": headY(), "r": 3})
            banger = 0
            if bhp <= 2 { banger = 1 }
            if bhp <= 1 { banger = 2 }
            if bhp <= 0 {
                bstate = "dying"
                btimer = 120
            } else {
                bstate = "hurt"
                btimer = 30
                bvx = facing * 3
            }
        } else {
            burst(sx, sy, 6, 10, 1)
            play(2)
            vx = -facing * 2
        }
    }
}

func updateBoss() {
    if bflash > 0 { bflash -= 1 }
    for b in balls {
        b["x"] += b["vx"]
        b["y"] += b["vy"]
        b["vy"] += 0.15
        if hitPip(b["x"], b["y"], 4) { die() }
        if b["y"] > FLOOR - 3 or b["x"] < 8 or b["x"] > 248 {
            b["gone"] = true
            burst(b["x"], b["y"], 12, 7, 1.2)
            push(rings, {"x": b["x"], "y": b["y"], "r": 2})
        }
    }
    balls = filter(balls, func(b) { return not has(b, "gone") })
    for w in waves {
        w["x"] += w["d"] * 1.8
        w["life"] -= 1
        if w["life"] % 3 == 0 { burst(w["x"], FLOOR - 2, 2, 7, 0.6) }
        if abs(w["x"] - (px + 3)) < 5 and py + 12 > FLOOR - 7 { die() }
    }
    waves = filter(waves, func(w) { return w["life"] > 0 and w["x"] > 8 and w["x"] < 248 })
    for r in rings { r["r"] += 2 }
    rings = filter(rings, func(r) { return r["r"] < 40 })
    for d in drops {
        if d["wait"] > 0 {
            d["wait"] -= 1
        } else {
            d["v"] = min(d["v"] + 0.35, 6)
            d["y"] += d["v"]
            if hitPip(d["x"], d["y"] + 4, 3) { die() }
            if d["y"] > FLOOR - 8 {
                d["gone"] = true
                burst(d["x"], FLOOR - 2, 10, 12, 1)
            }
        }
    }
    drops = filter(drops, func(d) { return not has(d, "gone") })

    if shardDrop != none {
        shardDrop["vy"] = min(shardDrop["vy"] + 0.2, 3)
        shardDrop["y"] = min(shardDrop["y"] + shardDrop["vy"], FLOOR - 8)
        if hitPip(shardDrop["x"] + 4, shardDrop["y"] + 4, 6) {
            shards += 1
            play(5)
            flash = 8
            shake(6)
            burst(shardDrop["x"] + 4, shardDrop["y"] + 4, 50, 11, 2)
            burst(shardDrop["x"] + 4, shardDrop["y"] + 4, 30, 7, 1.5)
            shardDrop = none
            won = 90
        }
        return
    }
    if bstate == "dead" { return }

    var target = px + 3
    btimer -= 1
    if bstate == "intro" {
        if btimer == 60 {
            shake(12)
            play(4)
            push(rings, {"x": headX(), "y": headY(), "r": 4})
            push(rings, {"x": headX(), "y": headY(), "r": 14})
        }
        if btimer <= 0 {
            bstate = "idle"
            btimer = 75
        }
    } else if bstate == "idle" {
        bface = target < bx ? -1 : 1
        bx += bface * (0.4 + banger * 0.2)
        bstep += 1
        if btimer <= 0 { pickAttack() }
    } else if bstate == "crouch" {
        if btimer <= 0 {
            bstate = "air"
            bvy = -6
            bvx = clamp((target - bx) / 40, -3, 3)
            play(1)
        }
    } else if bstate == "air" {
        bx += bvx
        bvy += 0.3
        by += bvy
        if by >= FLOOR and bvy > 0 {
            by = FLOOR
            land()
        }
    } else if bstate == "windup" {
        bface = target < bx ? -1 : 1
        if btimer <= 0 {
            var n = 1 + flr(banger / 2)
            var i = 0
            while i < n {
                push(balls, {"x": bx + bface * 16, "y": by - 44, "vx": (target - bx) / 45 + (i - (n - 1) / 2) * 0.7, "vy": -3.2 - i * 0.3})
                i += 1
            }
            play(2)
            shake(2)
            bstate = "idle"
            btimer = 75 - banger * 8
        }
    } else if bstate == "rev" {
        if btimer % 6 == 0 { burst(bx - bface * 10, by - 2, 3, 6, 0.8) }
        if btimer <= 0 {
            bstate = "charge"
            bvx = bface * (2.2 + banger * 0.4)
            play(1)
        }
    } else if bstate == "charge" {
        bx += bvx
        if btimer % 3 == 0 { burst(bx - bface * 12, by - 2, 2, 7, 0.6) }
        if bx < 32 or bx > 224 {
            bx = clamp(bx, 32, 224)
            shake(9)
            play(4)
            flash = 2
            push(rings, {"x": bx + bface * 18, "y": by - 24, "r": 4})
            dropIcicles(1 + banger)
            bstate = "stunned"
            btimer = 170 - banger * 15
        }
    } else if bstate == "stunned" {
        if btimer <= 0 {
            bstate = "idle"
            btimer = 40
        }
    } else if bstate == "hurt" {
        bx += bvx
        bvx *= 0.85
        if btimer <= 0 {
            bstate = "crouch"
            btimer = 18
        }
    } else if bstate == "dying" {
        if btimer % 5 == 0 {
            burst(bx + rnd(40) - 20, by - rnd(50), 15, pick([7, 6, 12, 11]), 1.5)
            shake(4)
            play(4)
        }
        if btimer <= 0 {
            bstate = "dead"
            flash = 12
            shake(12)
            burst(bx, by - 30, 60, 7, 2.5)
            burst(bx, by - 30, 40, 11, 2)
            shardDrop = {"x": bx - 4, "y": by - 54, "vy": -3}
        }
    }
    bx = clamp(bx, 30, 226)

    // touching the yeti hurts, unless he is down
    if bstate == "charge" or bstate == "air" {
        if px + 6 > bx - 15 and px < bx + 15 and py + 12 > by - 40 and py < by { die() }
    }
}

func drawYeti() {
    if bstate == "dead" { return }
    var sq = 0
    if bstate == "crouch" { sq = 4 + rnd(2) }
    if bstate == "stunned" { sq = 14 }
    var sx = 0
    if bstate == "crouch" or bstate == "rev" or bstate == "dying" { sx = rnd(3) - 1.5 }
    var x = bx + sx
    var y = by
    var bob = 0
    if bstate == "idle" { bob = abs(sin(bstep * 0.08)) * 2 }
    if bflash > 0 and bflash % 4 < 2 {
        pal(7, 8)
        pal(6, 8)
        pal(13, 2)
    }
    // big furry feet, stepping as he walks
    var stride = sin(bstep * 0.15) * 2
    if bstate == "charge" { stride = sin(t() * 25) * 3 }
    drawFoot(x - 9, y - max(0, stride))
    drawFoot(x + 9, y - max(0, -stride))
    // body: outline, fur, shaded belly, tufts
    var cy = y - 24 + sq * 0.6 - bob
    circfill(x, cy, 19, 1)
    circfill(x, cy, 18, 7)
    circfill(x + bface * 4, cy + 4, 11, 6)
    var i = 0
    while i < 9 {
        var a = i * 0.7 + 0.3
        pset(x + cos(a) * 15, cy + sin(a) * 15, 6)
        pset(x + cos(a + 0.4) * 10, cy + sin(a + 0.4) * 12, 13)
        i += 1
    }
    // arms: swing when walking, up to throw, up when leaping
    var armY = cy + 2 + sin(bstep * 0.15) * 3
    if bstate == "windup" { armY = cy - 16 }
    if bstate == "air" { armY = cy - 10 }
    if bstate == "stunned" { armY = cy + 8 }
    circfill(x - 20, armY, 8, 1)
    circfill(x + 20, armY, 8, 1)
    circfill(x - 20, armY, 7, 7)
    circfill(x + 20, armY, 7, 7)
    circfill(x - 21, armY + 4, 4, 6)
    circfill(x + 21, armY + 4, 4, 6)
    if bstate == "windup" {
        circfill(x + bface * 20, armY - 9, 6, 1)
        circfill(x + bface * 20, armY - 9, 5, 7)
    }
    // head and horns
    var hx = headX() + sx
    var hy = headY() - bob
    if bstate == "crouch" { hy += sq }
    circfill(hx, hy, 12, 1)
    circfill(hx, hy, 11, 7)
    line(hx - 9, hy - 6, hx - 14, hy - 14, 6)
    line(hx - 8, hy - 7, hx - 13, hy - 15, 6)
    line(hx + 9, hy - 6, hx + 14, hy - 14, 6)
    line(hx + 8, hy - 7, hx + 13, hy - 15, 6)
    // face
    // face: a blue-grey muzzle under a fringe of fur
    var fx = hx + bface * 2
    circfill(fx, hy + 2, 7, 5)
    circfill(fx, hy + 2, 6, 13)
    circfill(fx, hy + 5, 4, 6)
    var j = -6
    while j <= 6 {
        line(fx + j, hy - 6, fx + j + 1, hy - 2, 7)
        j += 2
    }
    drawFace(fx, hy)
    // the shard in his forehead, glowing
    var glow = 3 + sin(t() * 6) * 1.5
    if bstate == "stunned" { glow += 3 }
    circ(hx, hy - 11, glow + 4, 3)
    circ(hx, hy - 11, glow + 2, 11)
    spr(109, hx - 4, hy - 15)
    pal()
    if bstate == "stunned" {
        var s = 0
        while s < 3 {
            var a = t() * 4 + s * 2.1
            rectfill(hx + cos(a) * 14, hy - 18 + sin(a) * 4, hx + cos(a) * 14 + 1, hy - 17 + sin(a) * 4, 10)
            s += 1
        }
    }
}

// his face changes with what he is doing
func drawFace(fx, hy) {
    var s = bstate
    rectfill(fx - 1, hy + 3, fx + 1, hy + 4, 1)
    // eyes
    if s == "stunned" {
        line(fx - 4, hy - 1, fx - 2, hy + 1, 1)
        line(fx - 4, hy + 1, fx - 2, hy - 1, 1)
        line(fx + 2, hy - 1, fx + 4, hy + 1, 1)
        line(fx + 2, hy + 1, fx + 4, hy - 1, 1)
    } else if s == "hurt" or s == "dying" {
        line(fx - 4, hy - 1, fx - 2, hy, 1)
        line(fx - 4, hy + 1, fx - 2, hy, 1)
        line(fx + 4, hy - 1, fx + 2, hy, 1)
        line(fx + 4, hy + 1, fx + 2, hy, 1)
    } else {
        var r = 2
        if s == "air" { r = 3 }
        // glowing eyes (no dark circles round them)
        var c = 8
        if s == "charge" or s == "rev" { c = 10 }
        var e = r - 1
        if s == "windup" {
            line(fx - bface * 3 - 2, hy, fx - bface * 3 + 2, hy, 1)
            rectfill(fx + bface * 3 - e, hy - e + 1, fx + bface * 3 + e - 1, hy + e - 1, c)
        } else {
            rectfill(fx - 3 - e, hy - e + 1, fx - 3 + e - 1, hy + e - 1, c)
            rectfill(fx + 3 - e + 1, hy - e + 1, fx + 3 + e, hy + e - 1, c)
        }
    }
    // brows
    if s == "crouch" or s == "rev" or s == "charge" or s == "intro" {
        line(fx - 6, hy - 5, fx - 1, hy - 2, 1)
        line(fx + 6, hy - 5, fx + 1, hy - 2, 1)
    } else if s == "dying" or s == "hurt" {
        line(fx - 6, hy - 2, fx - 1, hy - 4, 1)
        line(fx + 6, hy - 2, fx + 1, hy - 4, 1)
    } else if s == "windup" {
        line(fx - bface * 6, hy - 3, fx - bface * 1, hy - 3, 1)
        line(fx + bface * 1, hy - 3, fx + bface * 6, hy - 5, 1)
    } else if s == "air" {
        line(fx - 6, hy - 5, fx - 1, hy - 5, 1)
        line(fx + 6, hy - 5, fx + 1, hy - 5, 1)
    }
    // mouth
    if s == "intro" or s == "charge" or s == "rev" {
        circfill(fx, hy + 7, 3, 1)
        circfill(fx, hy + 8, 1, 8)
        line(fx - 3, hy + 5, fx - 2, hy + 8, 7)
        line(fx + 3, hy + 5, fx + 2, hy + 8, 7)
    } else if s == "crouch" {
        rectfill(fx - 4, hy + 6, fx + 4, hy + 8, 1)
        line(fx - 3, hy + 7, fx + 3, hy + 7, 7)
        pset(fx - 1, hy + 7, 1)
        pset(fx + 1, hy + 7, 1)
    } else if s == "air" {
        circfill(fx, hy + 7, 2, 1)
    } else if s == "windup" {
        line(fx - bface * 3, hy + 7, fx + bface * 3, hy + 5, 1)
        pset(fx + bface * 2, hy + 6, 7)
    } else if s == "stunned" {
        line(fx - 3, hy + 7, fx - 1, hy + 6, 1)
        line(fx - 1, hy + 6, fx + 1, hy + 7, 1)
        line(fx + 1, hy + 7, fx + 3, hy + 6, 1)
        rectfill(fx + 1, hy + 7, fx + 2, hy + 9, 8)
    } else if s == "hurt" or s == "dying" {
        circfill(fx, hy + 7, 2, 1)
        line(fx - 2, hy + 6, fx + 2, hy + 6, 7)
        if s == "dying" {
            pset(fx - 4, hy + 2 + (t() * 12) % 5, 12)
            pset(fx + 4, hy + 2 + (t() * 12 + 2) % 5, 12)
        }
    } else {
        line(fx - 3, hy + 7, fx + 3, hy + 7, 1)
        pset(fx - 3, hy + 8, 1)
        pset(fx + 3, hy + 8, 1)
        pset(fx - 2, hy + 6, 7)
        pset(fx + 2, hy + 6, 7)
    }
    // angry: red streaks off the eyes; charging: steam from his nose
    if banger > 0 and s != "stunned" and s != "hurt" and s != "dying" {
        pset(fx - 3 - bface * (2 + rnd(3)), hy - rnd(2), 8)
        pset(fx + 3 - bface * (2 + rnd(3)), hy - rnd(2), 8)
    }

    if (s == "rev" or s == "charge") and rnd(1) < 0.3 { burst(fx + bface * 2, hy + 4, 1, 6, 0.4) }
}

func drawFoot(fx, fy) {
    circfill(fx, fy - 6, 7, 1)
    circfill(fx, fy - 6, 6, 7)
    circfill(fx + 1, fy - 4, 4, 6)
    rectfill(fx - 7, fy - 3, fx + 7, fy, 1)
    rectfill(fx - 6, fy - 3, fx + 6, fy - 1, 7)
    pset(fx - 4, fy, 5)
    pset(fx, fy, 5)
    pset(fx + 4, fy, 5)
}

func drawHazards() {
    for r in rings {
        circ(r["x"], r["y"], r["r"], 7)
        if r["r"] > 4 { circ(r["x"], r["y"], r["r"] - 3, 12) }
    }
    for d in drops {
        if d["wait"] > 0 and d["wait"] % 4 < 2 {
            var yy = 12
            while yy < FLOOR {
                pset(d["x"], yy, 8)
                yy += 6
            }
        }
        spr(144, d["x"] - 4, d["y"])
    }
    for w in waves {
        var h = 6 + sin(w["life"] * 0.6) * 2
        rectfill(w["x"] - 2, FLOOR - h, w["x"] + 2, FLOOR - 1, 7)
        rectfill(w["x"] - 1, FLOOR - h - 3, w["x"] + 1, FLOOR - h, 12)
    }
    for b in balls {
        circfill(b["x"], b["y"], 5, 1)
        circfill(b["x"], b["y"], 4, 7)
        circfill(b["x"] + 1, b["y"] + 1, 2, 6)
    }
    if shardDrop != none {
        circ(shardDrop["x"] + 4, shardDrop["y"] + 4, 6 + sin(t() * 8) * 2, 11)
        circ(shardDrop["x"] + 4, shardDrop["y"] + 4, 10 + sin(t() * 8) * 2, 3)
        spr(109, shardDrop["x"], shardDrop["y"])
    }
}

// fog: drifting banks of mist (every other line, flickering) and a blizzard
func drawFog(camx) {
    var phase = flr(t() * 12) % 3
    for f in fog {
        f["x"] += f["s"]
        var cx = (f["x"] - camx * 0.7) % 320 - 60
        if cx < -60 { cx += 320 }
        var r = f["r"]
        var dy = -r + phase
        while dy <= r {
            var hw = sqrt(max(0, r * r - dy * dy))
            line(cx - hw, f["y"] + dy, cx + hw, f["y"] + dy, 6)
            dy += 3
        }
    }
    for s in streaks {
        s["x"] -= s["s"]
        s["y"] += s["s"] * 0.3
        if s["x"] < -6 {
            s["x"] = SW + 6
            s["y"] = rnd(128)
        }
        if s["y"] > 128 { s["y"] = 0 }
        line(s["x"], s["y"], s["x"] + 4, s["y"] - 1, 7)
    }
}

func drawBossBar() {
    if bstate == "dead" or bstate == "intro" { return }
    rectfill(13, 118, 114, 125, 1)
    print("YETI", 15, 119, 7)
    rectfill(34, 120, 112, 123, 2)
    if bhp > 0 { rectfill(34, 120, 34 + bhp * 19 + 1, 123, 8) }
}

// ---------- drawing ----------

func draw() {
    camera(-OX, 0)   // menus and story screens: 128 wide, in the middle (the level sets its own camera below)
    if mode == "settings" {
        drawSettings()
        return
    }
    if mode == "title" {
        drawTitle()
        return
    }
    if mode == "epilogue" {
        if len(lines) > 0 or saying() {
            drawHeart()
        } else {
            drawEpilogue()
        }
        return
    }
    if mode == "end" {
        drawEnd()
        return
    }
    var L = LEVELS[level]
    var camx = clamp(px + 3 - SW / 2, 0, W * 8 - SW)
    cls(L["sky"])
    drawBackdrop(camx)
    camera(camx, 0)
    map(L["x"], L["y"], 0, 0, W, H)
    drawIcicles()
    if isBoss() {
        drawHazards()
        drawYeti()
    }
    if dead == 0 { drawPip() }
    camera()
    if isBoss() { drawFog(camx) }
    drawHud()
    camera(-OX, 0)
    if isBoss() { drawBossBar() }
    if levelTime < 90 and len(lines) == 0 {
        rectfill(0, 54, 127, 70, 1)
        print(L["name"], 64 - len(L["name"]) * 2, 60, 7)
    }
    if flash > 0 {
        if flash > 2 {
            rectfill(-OX, 0, SW, 127, 7)
        } else {
            var fy = flr(t() * 30) % 2
            while fy < 128 {
                line(-OX, fy, SW, fy, 7)
                fy += 2
            }
        }
    }
    camera(camx, 0)   // so the particles line up with the world
}

func drawBackdrop(camx) {
    var sky = LEVELS[level]["sky"]
    if level == 3 or level == 4 or level == 5 or level == 8 {
        // cave: far rock and glints
        var i = 0
        while i < 14 {
            var x = i * 24 - (camx * 0.3) % 24
            circfill(x, 0, 14, 2)
            circfill(x + 12, 128, 16, 2)
            pset(x + 5, 40 + (i * 37) % 50, 13)
            i += 1
        }
        return
    }
    var far = 6
    var near = 3
    if level == 2 {
        far = 14
        near = 2
    }
    if level == 6 {
        far = 7
        near = 13
    }
    if level == 7 {
        far = 7
        near = 6
    }
    // clouds
    var j = 0
    while j < 6 {
        var cx = j * 50 - (camx * 0.1) % 50
        var cy = 14 + (j * 23) % 30
        circfill(cx, cy, 6, 7)
        circfill(cx + 7, cy + 2, 5, 7)
        circfill(cx - 7, cy + 2, 4, 7)
        j += 1
    }
    // far mountains, then near hills, moving slower than the world
    var k = 0
    while k < 8 {
        var mx = k * 40 - (camx * 0.2) % 40
        circfill(mx, 150, 46, far)
        k += 1
    }
    k = 0
    while k < 10 {
        var hx = k * 26 - (camx * 0.45) % 26
        circfill(hx, 140, 24, near)
        k += 1
    }
    if level >= 6 {
        for f in snow {
            f["y"] += f["s"]
            f["x"] -= f["s"] * 0.6
            if f["y"] > 128 { f["y"] = 0 }
            if f["x"] < 0 { f["x"] = SW }
            pset(f["x"], f["y"], 7)
        }
    }
}

func drawPip() {
    var f = flr(t() * 2) % 2
    if dashTime > 0 {
        f = 7
    } else if not grounded and wall != 0 and vy > 0 {
        f = 8
    } else if not grounded {
        f = vy < 0 ? 5 : 6
    } else if abs(vx) > 0.3 {
        f = RUN[flr(steps / 5) % 4]
    }
    if atkTime > 0 and dashTime == 0 { f = atkTime > 5 ? 9 : 10 }
    var n = f * 2
    if f == 8 { n = 32 }
    if f == 9 { n = 34 }
    if f == 10 { n = 36 }
    var flip = facing < 0
    if f == 8 { flip = wall < 0 }
    // scarf behind him: red, blue when the dash is used up
    var col = 8
    var size = 2
    for p in scarf {
        circfill(p["x"], p["y"], size, col)
        size = max(1, size - 0.3)
    }
    spr(n, px - 5, py - 4, 2, 2, flip)
    pal()
    // the ember: a little warm spark on the scarf while the dash is ready
    if canDash and dashTime == 0 {
        var tip = scarf[len(scarf) - 1]
        pset(tip["x"] + rnd(2) - 1, tip["y"] - 2 - rnd(2), pick([9, 10, 10, 7]))
        pset(tip["x"], tip["y"] - 1, 10)
    }
    if atkTime > 0 and atkTime <= 5 {
        var ax = px + 3 + facing * 9
        var k = (5 - atkTime) * 0.8
        line(ax, py - 2 + k, ax + facing * 6, py + 6, 7)
        line(ax + facing * 6, py + 6, ax, py + 14 - k, 7)
        line(ax - facing, py, ax + facing * 4, py + 6, 12)
        line(ax + facing * 4, py + 6, ax - facing, py + 12, 12)
    }
}

func drawHud() {
    // small and out of the way: shards and coins, with a shadow so they read on any sky
    spr(109, 2, 2)
    print(shards + "/9", 12, 5, 1)
    print(shards + "/9", 11, 4, 7)
    spr(110, 30, 2)
    print("" + berries, 40, 5, 1)
    print("" + berries, 39, 4, 7)
}

var LOGO = [
    ["11111", "10000", "11111", "00001", "11111"],
    ["10001", "10001", "11111", "10001", "10001"],
    ["01110", "10001", "11111", "10001", "10001"],
    ["11110", "10001", "11110", "10100", "10011"],
    ["11110", "10001", "10001", "10001", "11110"]
]
var tmenu = 0

// the big SHARD letters, drawn from little pixel maps
func drawLogo(x, y, s) {
    var k = 0
    while k < 5 {
        var row = 0
        while row < 5 {
            var col = 0
            while col < 5 {
                if slice(LOGO[k][row], col, col + 1) == "1" {
                    var px0 = x + k * (s * 5 + s) + col * s
                    var py0 = y + row * s
                    rectfill(px0 + 1, py0 + 2, px0 + s, py0 + s + 1, 3)
                    rectfill(px0, py0, px0 + s - 1, py0 + s - 1, row == 0 ? 7 : 11)
                }
                col += 1
            }
            row += 1
        }
        k += 1
    }
}

func updateTitle() {
    if btnp(2) or btnp(3) or keyp("w") or keyp("s") { tmenu = 1 - tmenu }
    var my = mousey()
    var picked = false
    if click() and mousex() - OX > 40 and mousex() - OX < 88 {
        if my >= 64 and my < 73 {
            tmenu = 0
            picked = true
        }
        if my >= 74 and my < 83 {
            tmenu = 1
            picked = true
        }
    }
    if picked or keyp("enter") or btnp(4) or hit("jump") or keyp("space") {
        if tmenu == 0 {
            startGame()
        } else {
            openSettings("title")
        }
    }
}

func drawTitle() {
    cls(1)
    // stars
    var i = 0
    while i < 40 {
        var c = (i + flr(t() * 2)) % 9 == 0 ? 7 : 13
        pset((i * 53) % SW - OX, (i * 29) % 70, c)
        i += 1
    }
    // mountains: a far range, then the big peak with the shard above it
    var y = 60
    while y < 128 {
        var a = (y - 60) * 1.4
        line(20 - a, y, 20 + a, y, 2)
        line(-40 - a, y, -40 + a, y, 2)
        line(168 - a, y, 168 + a, y, 2)
        line(108 - a, y, 108 + a, y, 2)
        y += 1
    }
    y = 52
    while y < 128 {
        var half = (y - 52) * 1.1
        line(64 - half, y, 64 + half, y, y < 61 ? 7 : 5)
        y += 1
    }
    var glow = 2 + sin(t() * 3) * 2
    circ(64, 44 + sin(t() * 2), 6 + glow, 3)
    spr(109, 60, 40 + sin(t() * 2))
    // snowy ground and Pip
    rectfill(-OX, 104, SW, 127, 7)
    rectfill(-OX, 104, SW, 105, 6)
    var s = 0
    while s < 5 {
        circfill(18 - s * 3, 95 + sin(t() * 6 + s) * 1.5, max(1, 2 - s * 0.3), 8)
        s += 1
    }
    spr(flr(t() * 2) % 2 * 2, 18, 88, 2, 2)
    // falling snow
    for f in snow {
        f["y"] += f["s"]
        f["x"] -= f["s"] * 0.5
        if f["y"] > 128 { f["y"] = 0 }
        if f["x"] < 0 { f["x"] = SW }
        pset(f["x"] - OX, f["y"], 7)
    }
    drawLogo(20, 8, 3)
    print("PIP AND THE EMERALD HEART", 14, 26, 6)
    // the menu
    var names = ["START", "SETTINGS"]
    var k = 0
    while k < 2 {
        var ry = 66 + k * 10
        if k == tmenu {
            rectfill(40, ry - 2, 87, ry + 6, 3)
            print(">", 42, ry, 11)
        }
        print(names[k], 64 - len(names[k]) * 2, ry, k == tmenu ? 7 : 6)
        k += 1
    }
    if dget(0) > 0 { print("BEST " + fixed(dget(0), 1) + "S", 84, 120, 5) }
}
func drawEnd() {
    cls(1)
    menuCard("THE END")
    var glow = 2 + sin(t() * 3) * 2
    circ(64, 38, 12 + glow, 3)
    sspr(104, 48, 8, 8, 52, 26, 24, 24)
    print("THE HEART IS WHOLE", 28, 58, 11)
    print("COINS  " + berries + " / " + BERRIES, 64 - (10 + len("" + berries)) * 2, 72, 7)
    print("FALLS  " + deaths, 64 - (7 + len("" + deaths)) * 2, 81, 7)
    var tm = fixed(finishTime, 1) + "S"
    print("TIME  " + tm, 64 - (6 + len(tm)) * 2, 90, 10)
    if flr(t() * 2) % 2 == 0 { print("PRESS SPACE", 42, 116, 6) }
}

// while the last story boxes play: the shards come together
func drawHeart() {
    cls(1)
    var k = 0
    while k < 9 {
        var a = k * 0.7 + t()
        var r = max(0, 40 - t() * 4 % 40)
        line(64, 50, 64 + cos(a) * 60, 50 + sin(a) * 60, 2)
        k += 1
    }
    var glow = 3 + sin(t() * 4) * 2
    circfill(64, 50, 14 + glow, 3)
    circ(64, 50, 18 + glow, 11)
    sspr(104, 48, 8, 8, 48, 34, 32, 32)
}
""";
}