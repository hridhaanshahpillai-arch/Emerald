package emerald;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Writes carts/shard.emerald. args: a cart to take the sounds from (shard.emerald itself), output, shard.seven.
 *  Levels drawn in the level editor (tools\levels\levelN.txt) replace the ones written below. */
public class MakeShard {

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
            "....1bbbabb1....",
            "...1b7bbbbb31...",
            "...13bbbbbbbb11.",
            "...14ffffff111..",
            "...144ff1f1f1...",
            "...14fffffe91...",
            ".....1ffff1.....",
            "...117bbbbb711..",
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
        put16(0, pip(BODY, "....111..111....", "...1491..1491..."));                       // 0 idle
        put16(2, pip(shiftDown(BODY), "....111..111....", "...1491..1491..."));            // 1 idle, breathing
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
        put16(34, pip(raise, "....111..111....", "...1491..1491..."));
        String[] slash = BODY.clone();                                                      // 10 swing: dagger out in front
        slash[10] = "...113bbbbb31f67";
        slash[11] = "..1f13bbbbb31...";
        // Bram: the old herder with his hat, beard and stick
        put16(40, new String[]{"................", "................", "......1111......", ".....144441.....", "....14444441....", "...1111111111...", "....16ffff61....", "....1f1ff1f1....", "....16f99f61....", "....17777771....", "...1177777711...", "..1f15444451f14.", "...15444451..4..", "...15444451..4..", "....111111...4..", "....14..41...4.."});
        // Wren: the snowy owl
        put16(42, new String[]{"................", "................", "................", "................", "................", "...1771..1771...", "...1777117771...", "...17aa77aa71...", "...17a1771a71...", "...1777997771...", "...1767777671...", "...1777777771...", "...1767676771...", "....17777771....", ".....1a11a1.....", "................"});
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

    // the Complete Edition's new pieces: doors, gates, cracked ice, enemies and the ember bolt
    static void drawWorldTiles() {
        put(149, new String[]{"..5555..", ".566665.", "56444465", "54999945", "54944945", "54999945", "54944945", "54999945"}); // door (top)
        put(150, new String[]{"54999945", "54944a45", "54999945", "54944945", "54999945", "54944945", "54999945", "55555555"}); // door (bottom)
        put(152, new String[]{"c71c71c7", "c71c71c7", "c71c71c7", "dddddddd", "c71c71c7", "c71c71c7", "c71c71c7", "1d11d11d"}); // ice gate
        put(153, new String[]{"dccccccd", "c7c1cc7c", "cc1c71cc", "c1cccc1c", "cc71c1cc", "c1cc1ccc", "cc1c7c1c", "dddddddd"}); // cracked ice
        put(156, new String[]{"..7777..", ".777777.", "77177177", "77777777", "77788777", ".777777.", ".67..76.", "66....66"}); // snow sprite
        put(157, new String[]{"..7777..", ".777777.", "77177177", "77777777", "77788777", ".777777.", "..6776..", "..66.66."});
        put(158, new String[]{"c......c", "cc....cc", "ccc11ccc", ".cc11cc.", "..1881..", "...11...", "........", "........"}); // ice bat
        put(159, new String[]{"........", "........", "...11...", "..1881..", ".cc11cc.", "ccc11ccc", "cc....cc", "c......c"});
        put(160, new String[]{"........", "..99....", ".9aa99..", "9a77aa9.", ".9aa99..", "..99....", "........", "........"}); // ember bolt
    }

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
        put(107, new String[]{"...8....", "..8e8...", ".8ee78..", ".8e77e8.", "8ee77ee8", "8e7777e8", ".8eeee8.", "..8888.."}); // ember (pink)
        put(108, new String[]{"........", "........", "...4....", "..4.4...", ".4...4..", ".4...4..", "..444...", "........"}); // used ember
        put(161, new String[]{"...33...", "..3bb3..", ".3b77b3.", "3bb77bb3", ".3b77b3.", ".3b77b3.", "..3bb3..", "...33..."}); // wall crystal: run up the next wall
        put(162, new String[]{"...55...", "..5..5..", ".5....5.", "5......5", ".5....5.", ".5....5.", "..5..5..", "...55..."}); // used wall crystal
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
        cart.flags[152] = 1;                                         // ice gate: solid until enough shards
        cart.flags[153] = 1;                                         // cracked ice: solid until smashed
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
    static int LH = H;   // this level's height (the mountain is taller)
    static char[][] g;
    static final List<String> doors = new ArrayList<>(), gates = new ArrayList<>();

    static void R(int x, int y, int w, int h) {
        for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) if (i >= 0 && i < LW && j >= 0 && j < LH) g[j][i] = '#';
    }

    static void at(int x, int y, char c) { g[y][x] = c; }

    static void row(int x0, int x1, int y, char c) { for (int x = x0; x <= x1; x++) g[y][x] = c; }

    static void spikesOver(int x0, int x1, int y) {           // spikes on every empty cell of a row
        for (int x = x0; x <= x1; x++) if (g[y][x] == '.') g[y][x] = '^';
    }

    static int berryCount;

    static void level(int n) {
        Path file = levelsDir.resolve("level" + n + ".txt");
        start = startOf(n);
        try {
            if (java.nio.file.Files.exists(file)) {              // drawn in the level editor (any size)
                List<String> rows = new ArrayList<>(java.nio.file.Files.readAllLines(file));
                while (!rows.isEmpty() && rows.get(rows.size() - 1).isBlank()) rows.remove(rows.size() - 1);
                LH = rows.size();
                LW = rows.stream().mapToInt(String::length).max().orElse(0);
                g = new char[LH][LW];
                for (int y = 0; y < LH; y++) for (int x = 0; x < LW; x++) {
                    char c = x < rows.get(y).length() ? rows.get(y).charAt(x) : '.';
                    if (c == 'P') { start[0] = x; start[1] = y + 1; c = '.'; }
                    g[y][x] = c;
                }
                return;
            }
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        g = new char[LH][LW];
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
            case 9 -> {   // SNOWBALL HILLS: snow sprites and the sword combo; cracked ice walls to smash
                R(0, 13, 48, 3);
                R(10, 12, 4, 1); R(26, 11, 5, 2); R(34, 12, 3, 1);
                at(8, 12, 'k'); at(17, 12, 'k'); at(29, 10, 'k'); at(38, 12, 'k');
                for (int y = 9; y <= 12; y++) at(22, y, 'X');
                for (int y = 10; y <= 12; y++) at(41, y, 'X');
                row(12, 15, 9, '='); at(13, 8, 'b');
                row(29, 31, 7, '='); at(30, 6, 'b');
                at(45, 12, 'G');
            }
            case 10 -> {   // BAT CAVERN: ice bats; ember bolts
                R(0, 0, 48, 2);
                R(0, 13, 48, 3);
                row(14, 16, 12, '^'); row(30, 32, 12, '^');
                R(22, 9, 2, 4);
                at(10, 7, 'j'); at(20, 6, 'j'); at(27, 8, 'j'); at(36, 7, 'j'); at(42, 6, 'j');
                at(23, 7, 'b'); at(37, 10, 'b');
                at(45, 12, 'G');
            }
            case 11 -> {   // THE ROOTS: a ground pound breaks the floor into a hidden room
                R(0, 13, 10, 3); at(8, 12, 'S'); at(8, 4, 'b');
                R(10, 10, 10, 6);
                R(20, 13, 28, 3);
                for (int x = 36; x <= 41; x++) for (int y = 14; y <= 15; y++) g[y][x] = '.';
                at(38, 13, 'X'); at(39, 13, 'X');
                at(14, 9, 'k'); at(26, 12, 'k'); at(44, 12, 'k');
                row(24, 27, 9, '='); at(25, 8, 'b');
                at(40, 15, 'G');
            }
            case 12 -> {   // FROST MINES: slippery ice, falling icicles, snow sprites
                R(0, 0, 48, 2);
                R(0, 13, 48, 3);
                for (int x : new int[]{8, 14, 20, 26, 34, 40}) at(x, 2, 'I');
                at(12, 12, 'k'); at(24, 12, 'k'); at(38, 12, 'k');
                for (int y = 10; y <= 12; y++) at(30, y, 'X');
                row(17, 20, 9, '='); at(18, 8, 'b');
                at(45, 12, 'G');
            }
            case 13 -> {   // STORM BRIDGE: a crumbling bridge in the wind, bats overhead
                R(0, 15, 48, 1);
                R(0, 13, 6, 2);
                for (int x = 7; x <= 40; x++) if (x != 15 && x != 16 && x != 25 && x != 26) at(x, 12, 'C');
                spikesOver(6, 40, 14);
                at(12, 8, 'j'); at(20, 7, 'j'); at(30, 8, 'j'); at(37, 7, 'j');
                at(20, 9, 'b'); at(33, 9, 'b');
                R(41, 12, 7, 3);
                at(45, 11, 'G');
            }
            case 14 -> {   // FROZEN TEMPLE: pillars over spikes, bats, and a wall to smash at the end
                R(0, 0, 48, 2);
                R(0, 13, 8, 3);
                R(8, 15, 32, 1);
                R(12, 11, 2, 4); R(18, 9, 2, 6); R(24, 11, 2, 4); R(30, 9, 2, 6); R(36, 11, 2, 4);
                spikesOver(8, 39, 14);
                at(15, 6, 'j'); at(27, 5, 'j'); at(33, 6, 'j');
                at(21, 6, 'D');
                at(19, 6, 'b'); at(31, 6, 'b');
                R(40, 12, 8, 4);
                at(42, 11, 'k');
                for (int y = 8; y <= 11; y++) at(44, y, 'X');
                at(46, 11, 'G');
            }
            case 15 -> {   // THE MOUNTAIN: the open world; doors lead to the levels, ice gates open with shards
                // the foot of the mountain
                R(0, 45, 96, 3);
                at(6, 44, '0'); at(18, 44, '1'); at(30, 44, '9');
                R(40, 40, 8, 1); at(44, 39, '2');
                R(54, 42, 6, 1); R(62, 39, 6, 1); R(70, 36, 6, 1); R(78, 33, 8, 1);
                at(12, 44, 'k'); at(60, 37, 'j'); at(50, 40, 'b'); at(5, 40, 'b');
                // the middle: a ledge across the mountain, reached through the first ice gate
                R(0, 30, 76, 2); R(86, 30, 10, 2);
                for (int x = 76; x <= 85; x++) { at(x, 30, 'x'); at(x, 31, 'x'); }
                at(70, 29, '3'); at(58, 29, '4'); at(46, 29, 'A'); at(34, 29, 'B');
                at(40, 29, 'k'); at(30, 22, 'j'); at(92, 26, 'b');
                R(24, 28, 5, 1); R(17, 26, 5, 1); R(8, 24, 5, 1); R(1, 22, 5, 1); R(8, 20, 4, 1); R(2, 18, 4, 1); at(3, 17, 'S');
                // the top: snow and stone, through the second gate
                R(10, 15, 86, 2);
                for (int x = 0; x <= 9; x++) { at(x, 15, 'y'); at(x, 16, 'y'); }
                at(20, 14, '5'); at(32, 14, '6'); at(44, 14, 'C'); at(68, 14, '7'); at(80, 14, 'D');
                R(72, 13, 5, 1); R(64, 11, 5, 1); R(56, 9, 5, 1); at(58, 8, 'E');
                R(64, 7, 8, 1); R(74, 6, 8, 1);
                for (int y = 0; y <= 5; y++) { at(82, y, 'z'); at(83, y, 'z'); }
                R(84, 6, 12, 1); at(90, 5, 'Y');
                at(26, 14, 'k'); at(48, 8, 'j'); at(40, 10, 'b'); at(94, 2, 'b');
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
        try {                                                    // first build: hand the level to the editor
            java.nio.file.Files.createDirectories(levelsDir);
            StringBuilder s = new StringBuilder();
            for (int y = 0; y < LH; y++) {
                for (int x = 0; x < LW; x++) s.append(x == start[0] && y == start[1] - 1 ? 'P' : g[y][x]);
                s.append('\n');
            }
            java.nio.file.Files.writeString(levelsDir.resolve("level" + n + ".txt"), s);
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    static Path levelsDir;
    static int[] start;

    /** Level n's line in the LEVELS table of the code: {start, end}. */
    static int[] entry(int n) {
        int from = CODE.indexOf("var LEVELS = ["), to = CODE.indexOf("\n]", from);
        var m = java.util.regex.Pattern.compile("(?m)^\\s*\\{\"name\": .*$").matcher(CODE).region(from, to);
        for (int i = 0; i <= n; i++) if (!m.find()) throw new IllegalStateException("level " + n + " is missing from LEVELS");
        return new int[]{m.start(), m.end()};
    }

    static int field(String line, String key) {
        var m = java.util.regex.Pattern.compile("\"" + key + "\": (\\d+)").matcher(line);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** Where Pip starts in level n ({column, row he stands on}), taken from the LEVELS table in the code. */
    static int[] startOf(int n) {
        int[] e = entry(n);
        String line = CODE.substring(e[0], e[1]);
        return new int[]{field(line, "sx"), field(line, "sy")};
    }

    /** Writes where level n sits in the map, its size and Pip's start back into the LEVELS table. */
    static void setEntry(int n, int x, int y, int w, int h, int[] start) {
        int[] e = entry(n);
        String line = CODE.substring(e[0], e[1]);
        int[][] values = {{x}, {y}, {start[0]}, {start[1]}, {w}, {h}};
        String[] keys = {"x", "y", "sx", "sy", "w", "h"};
        for (int k = 0; k < keys.length; k++) {
            String pair = "\"" + keys[k] + "\": " + values[k][0];
            if (field(line, keys[k]) >= 0) line = line.replaceFirst("\"" + keys[k] + "\": \\d+", pair);
            else line = line.replaceFirst("\\}(,?)\\s*$", ", " + pair + "}$1");
        }
        CODE = CODE.substring(0, e[0]) + line + CODE.substring(e[1]);
    }

    /** Finds a free spot in Emerald's 256x128 map for each level, biggest first. */
    static int[][] pack(int[] ws, int[] hs) {
        boolean[][] used = new boolean[Cart.MAP_HEIGHT][Cart.MAP_WIDTH];
        Integer[] order = new Integer[ws.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> ws[b] * hs[b] - ws[a] * hs[a]);
        int[][] spots = new int[ws.length][];
        for (int n : order) {
            search:
            for (int y = 0; y + hs[n] <= Cart.MAP_HEIGHT; y++) {
                spot:
                for (int x = 0; x + ws[n] <= Cart.MAP_WIDTH; x++) {
                    for (int j = y; j < y + hs[n]; j++) for (int i = x; i < x + ws[n]; i++) if (used[j][i]) continue spot;
                    for (int j = y; j < y + hs[n]; j++) for (int i = x; i < x + ws[n]; i++) used[j][i] = true;
                    spots[n] = new int[]{x, y};
                    break search;
                }
            }
            if (spots[n] == null) throw new IllegalStateException("The levels are too big to fit in Emerald's map (256x128 tiles). Make one smaller.");
        }
        return spots;
    }

    static final int LEVEL_COUNT = 9;

    /** A level painted tile by tile in the level editor (tools\levels\levelN.tiles): "START x y", then 2 hex digits a tile. */
    static int[][] paintedTiles(int n, int[] start) throws java.io.IOException {
        Path file = levelsDir.resolve("level" + n + ".tiles");
        if (!java.nio.file.Files.exists(file)) return null;
        List<String> rows = new ArrayList<>();
        for (String line : java.nio.file.Files.readAllLines(file)) {
            line = line.trim();
            if (line.startsWith("START")) {
                String[] p = line.split("\\s+");
                start[0] = Integer.parseInt(p[1]);
                start[1] = Integer.parseInt(p[2]);
            } else if (!line.isEmpty() && !line.startsWith("LEVEL")) rows.add(line);
        }
        int w = rows.stream().mapToInt(String::length).max().orElse(0) / 2;
        int[][] t = new int[rows.size()][w];
        for (int y = 0; y < rows.size(); y++) for (int x = 0; x < w && x * 2 + 1 < rows.get(y).length(); x++)
            t[y][x] = Integer.parseInt(rows.get(y).substring(x * 2, x * 2 + 2), 16);
        return t;
    }

    static int LW = W;   // this level's width (the arena is narrower)

    static boolean solid(int x, int y) {
        if (x < 0 || x >= LW) return true;        // the level's sides count as walls
        if (y < 0) return false;
        if (y >= LH) return true;
        return g[y][x] == '#';
    }

    static void bake(int n, int ox, int oy, Random rnd) {
        int set = new int[]{0, 0, 0, 1, 2, 1, 2, 1, 1, 0, 1, 0, 2, 1, 1, 0}[n];
        int[][] t = new int[LH][LW];
        for (int y = 0; y < LH; y++) for (int x = 0; x < LW; x++) {
            char c = g[y][x];
            int doorLevel = "0123456789ABCDE".indexOf(c);
            if (c == 'Y') doorLevel = 8;
            if (doorLevel >= 0 && n == 15) {             // a door on the mountain (its top goes on the tile above)
                doors.add(String.format("{\"level\": %d, \"x\": %d, \"y\": %d}", doorLevel, x, y));
                t[y][x] = 150;
                continue;
            }
            if (c == 'x' || c == 'y' || c == 'z') {
                gates.add(String.format("{\"x\": %d, \"y\": %d, \"need\": %d}", x, y, c == 'x' ? 3 : c == 'y' ? 8 : 14));
                t[y][x] = 152;
                continue;
            }
            t[y][x] = switch (c) {
                case '#' -> {
                    int mask = 0;
                    if (!solid(x, y - 1)) mask |= 1;
                    if (!solid(x, y + 1)) mask |= 2;
                    if (!solid(x - 1, y)) mask |= 4;
                    if (!solid(x + 1, y)) mask |= 8;
                    yield base(n == 15 && y < 24 ? 1 : set) + mask;   // the mountain turns to snow and stone up high
                }
                case 'I' -> 144;
                case 'X' -> 153;
                case 'k' -> 156;
                case 'j' -> 158;
                case '^' -> 96;
                case 'v' -> 97;
                case 'S' -> 100;
                case 'C' -> 102;
                case '=' -> (x > 0 && g[y][x - 1] == '=') ? ((x < LW - 1 && g[y][x + 1] == '=') ? 105 : 106) : 104;
                case 'D' -> 107;
                case 'G' -> 109;
                case 'b' -> { berryCount++; yield 110; }
                default -> 0;
            };
        }
        for (int y = 1; y < LH; y++) for (int x = 0; x < LW; x++) if (t[y][x] == 150) t[y - 1][x] = 149;   // door tops
        // decoration, never on top of anything
        for (int y = 1; y < LH; y++) for (int x = 0; x < LW; x++) {
            boolean top = g[y][x] == '#' && g[y - 1][x] == '.' && t[y - 1][x] == 0;
            if (!top) continue;
            int r = rnd.nextInt(10);
            if (set == 0 && !(n == 15 && y < 24)) {
                if ((n == 2 || n == 11) && r < 2 && x + 1 < LW && y >= 4 && x % 7 == 3 && treeFits(t, x, y)) {   // a tree in the woods
                    t[y - 1][x] = 119; t[y - 1][x + 1] = 123;
                    t[y - 2][x] = 119; t[y - 2][x + 1] = 123;
                    t[y - 3][x] = 122; t[y - 3][x + 1] = 124;
                    t[y - 4][x] = 120; t[y - 4][x + 1] = 121;
                    continue;
                }
                if (r == 0 && x + 1 < LW && g[y][x + 1] == '#' && g[y - 1][x + 1] == '.' && t[y - 1][x + 1] == 0) {
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
        for (int y = 0; y < LH - 1; y++) for (int x = 0; x < LW; x++) {
            if (g[y][x] != '#' || g[y + 1][x] != '.' || t[y + 1][x] != 0) continue;
            if (set == 1 && rnd.nextInt(3) == 0) t[y + 1][x] = 127;   // (ice levels: only the real, falling icicles)
            if (n == 2 && rnd.nextInt(4) == 0) t[y + 1][x] = 111;
        }
        for (int y = 0; y < LH; y++) for (int x = 0; x < LW; x++) cart.map[(oy + y) * Cart.MAP_WIDTH + ox + x] = (byte) t[y][x];
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
        drawWorldTiles();
        flags();
        music();
        Random rnd = new Random(11);
        Path seven = Path.of(args.length > 2 ? args[2] : "shard.seven").toAbsolutePath();
        CODE = java.nio.file.Files.readString(seven);
        levelsDir = seven.getParent().resolve("levels");
        char[][][] grids = new char[LEVEL_COUNT][][];
        int[] ws = new int[LEVEL_COUNT], hs = new int[LEVEL_COUNT];
        int[][] starts = new int[LEVEL_COUNT][];
        int[][][] painted = new int[LEVEL_COUNT][][];
        for (int n = 0; n < LEVEL_COUNT; n++) {
            LW = n == 8 ? 32 : W;
            LH = H;
            level(n);
            grids[n] = g; ws[n] = LW; hs[n] = LH; starts[n] = start;
            painted[n] = paintedTiles(n, starts[n]);
            if (painted[n] != null) { hs[n] = painted[n].length; ws[n] = painted[n][0].length; }
        }
        int[][] spots = pack(ws, hs);
        for (int n = 0; n < LEVEL_COUNT; n++) {
            g = grids[n]; LW = ws[n]; LH = hs[n];
            setEntry(n, spots[n][0], spots[n][1], LW, LH, starts[n]);
            if (painted[n] == null) { bake(n, spots[n][0], spots[n][1], rnd); continue; }
            for (int y = 0; y < LH; y++) for (int x = 0; x < LW; x++) {
                int t = painted[n][y][x];
                if (t == 110) berryCount++;
                cart.map[(spots[n][1] + y) * Cart.MAP_WIDTH + spots[n][0] + x] = (byte) t;
            }
        }
        cart.code = CODE.replace("BERRY_TOTAL", String.valueOf(berryCount))
                .replace("DOORS_TABLE", String.join(",\n    ", doors)).replace("GATES_TABLE", String.join(",\n    ", gates));
        cart.write(Path.of(args[1]));
        updateEditor(seven.getParent().resolve("shard-level-editor.html"), spots, ws, hs, starts);
        System.out.println("berries: " + berryCount);
    }

    /** Puts the finished levels (every tile, as the game has them) and the sprite sheet into the level editor page. */
    static void updateEditor(Path page, int[][] spots, int[] ws, int[] hs, int[][] starts) throws java.io.IOException {
        if (!java.nio.file.Files.exists(page)) return;
        StringBuilder d = new StringBuilder("const SHEET_ROWS = [");
        for (int y = 0; y < Cart.SHEET; y++) {
            d.append(y == 0 ? "\n  \"" : ",\n  \"");
            for (int x = 0; x < Cart.SHEET; x++) d.append(Character.forDigit(cart.sheet[y * Cart.SHEET + x] & 15, 16));
            d.append('"');
        }
        d.append("];\nconst START = [");
        int[] sets = {0, 0, 0, 1, 2, 1, 2, 1, 1};
        for (int n = 0; n < LEVEL_COUNT; n++) {
            int[] e = entry(n);
            String line = CODE.substring(e[0], e[1]);
            var name = java.util.regex.Pattern.compile("\"name\": \"([^\"]*)\"").matcher(line);
            name.find();
            d.append(n == 0 ? "\n  {" : ",\n  {").append("\"name\": \"").append(name.group(1)).append("\", \"sky\": ").append(field(line, "sky"))
                    .append(", \"set\": ").append(sets[n]).append(", \"sx\": ").append(starts[n][0]).append(", \"sy\": ").append(starts[n][1]).append(", \"rows\": [");
            for (int y = 0; y < hs[n]; y++) {
                d.append(y == 0 ? "\"" : ",\"");
                for (int x = 0; x < ws[n]; x++) d.append(String.format("%02x", cart.map[(spots[n][1] + y) * Cart.MAP_WIDTH + spots[n][0] + x] & 255));
                d.append('"');
            }
            d.append("]}");
        }
        d.append("];\n");
        String html = java.nio.file.Files.readString(page);
        int a = html.indexOf("/*DATA*/"), b = html.indexOf("/*END*/");
        if (a < 0 || b < a) return;
        java.nio.file.Files.writeString(page, html.substring(0, a + 8) + "\n" + d + html.substring(b));
    }

    /** The game's Seven code lives in its own file (shard.seven, next to this program; too big for a Java string). */
    static String CODE;

}