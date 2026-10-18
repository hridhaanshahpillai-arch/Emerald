package emerald;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Emerald's editor, with a tab for each part of a cart: CODE, SPRITES, MAP, SFX and MUSIC. It holds
 * the cart being worked on (which may not have a file yet, after NEW).
 *
 *   Ctrl+S  save the cart        Ctrl+R or RUN  save and run it        Esc  the command line
 */
final class Editor {

    private static final String[] TABS = {"CODE", "SPRITES", "MAP", "SFX", "MUSIC"};
    private static final int TAB_WIDTH = 11, TAB_STEP = 12;
    /** A 7 x 5 pixel icon for each tab. */
    private static final String[][] ICONS = {
            {".##.##.", ".#...#.", "#.....#", ".#...#.", ".##.##."},   // CODE: { }
            {"..###..", ".#####.", "##.#.##", "#######", "#.#.#.#"},   // SPRITES: a little ghost
            {"#######", "#..#..#", "#######", "#..#..#", "#######"},   // MAP: a grid of tiles
            {".#.....", "#.#...#", "...#.#.", "....#..", "......."},   // SFX: a sound wave
            {"..#####", "..#...#", "..#...#", ".##..##", "###.###"},   // MUSIC: two notes
    };
    private String hoveredTab = "";

    final CodeEditor code = new CodeEditor();
    final SpriteEditor sprites = new SpriteEditor();
    final MapEditor map = new MapEditor();
    final SfxEditor sfx;
    final MusicEditor music;
    private final Audio audio;
    private Path file;                 // null: a new cart that hasn't been saved yet
    private Cart cart = new Cart();
    private String tab = "CODE";
    private String message = "";
    private int messageTime;
    String cursor = "arrow";
    /** When the editor last saved the file (so Emerald doesn't mistake its own save for a change from outside). */
    long lastWrite;

    Editor(Audio audio) {
        this.audio = audio;
        this.sfx = new SfxEditor(audio);
        this.music = new MusicEditor(audio);
        newCart();
    }

    Path file() {
        return file;
    }

    /** The cart's name for titles and the command line: "hello", or "untitled". */
    String name() {
        if (file == null) return "untitled";
        String n = file.getFileName().toString();
        if (n.endsWith(".emerald")) return n.substring(0, n.length() - ".emerald".length());
        return n.toLowerCase().endsWith(".png") ? n.substring(0, n.length() - 4) : n;
    }

    /** An empty cart with no file yet. */
    void newCart() {
        file = null;
        use(new Cart());
        cart.code = "func init() {\n}\n\nfunc update() {\n}\n\nfunc draw() {\n  cls(1)\n  print(\"HELLO!\", 2, 2, 11)\n}\n";
        code.setText(cart.code);
        tab = "CODE";
    }

    /** Reads a cart from a file. Throws IOException if it can't. */
    void open(Path path) throws IOException {
        Cart read = Cart.read(path);
        file = path;
        use(read);
    }

    private void use(Cart c) {
        cart = c;
        code.setText(cart.code);
        sprites.setSheet(cart.sheet, cart.flags);
        map.setCart(cart);
        sfx.setCart(cart);
        music.setCart(cart);
        audio.setCart(cart);
    }

    /** Reads the cart's file again (it changed outside Emerald). */
    void reopen() {
        if (file == null) return;
        try {
            open(file);
        } catch (IOException e) {
            flash("CAN'T READ THE CART");
        }
    }

    /** The game's last picture: the label if the cart is saved as a .png. */
    void setLabel(int[] label) {
        cart.label = label;
    }

    boolean dirty() {
        return code.dirty() || sprites.dirty() || map.dirty() || sfx.dirty() || music.dirty();
    }

    /** The cart as it is in the editor right now (code, sprites, sounds, music), to run. */
    Cart cart() {
        cart.code = code.text();
        return cart;
    }

    /** Writes the cart to its file. Returns false (and says why) if it couldn't. */
    boolean save() {
        if (file == null) {
            flash("NO NAME YET: ESC, THEN SAVE NAME");
            return false;
        }
        return saveAs(file);
    }

    /** Writes the cart to a file, which becomes its file from now on. */
    boolean saveAs(Path path) {
        cart.code = code.text();
        try {
            if (path.toAbsolutePath().getParent() != null) Files.createDirectories(path.toAbsolutePath().getParent());
            cart.write(path);
            file = path;
            lastWrite = Files.getLastModifiedTime(path).toMillis();
            code.saved();
            sprites.saved();
            map.saved();
            sfx.saved();
            music.saved();
            flash("SAVED " + name().toUpperCase());
            return true;
        } catch (IOException e) {
            flash("COULDN'T SAVE!");
            System.out.println("Couldn't save " + path + ": " + e.getMessage());
            return false;
        }
    }

    /** Opens a tab by name ("CODE", "SPRITES", "SFX", "MUSIC"). */
    void show(String name) {
        tab = name;
    }

    void showSprites() {
        tab = "SPRITES";
    }

    void showCode(int line) {
        tab = "CODE";
        if (line > 0) code.goTo(line);
    }

    private void flash(String text) {
        message = text;
        messageTime = 120;
    }

    /** One frame of editing. Returns true when the player asked to run the cart. */
    boolean update(Input input, int tokens) {
        boolean run = false;
        for (Input.KeyPress k : input.presses()) {
            if (k.ctrl() && k.code() == KeyEvent.VK_S) save();
            if (k.ctrl() && k.code() == KeyEvent.VK_R) run = true;
        }
        // the tabs
        int mx = input.mouseX, my = input.mouseY;
        boolean overTab = false;
        hoveredTab = "";
        for (int i = 0; i < TABS.length; i++) {
            int x = 1 + i * TAB_STEP;
            if (my >= 0 && my < 8 && mx >= x && mx < x + TAB_WIDTH) {
                overTab = true;
                hoveredTab = TABS[i];
                if (input.mouseClicked()) tab = TABS[i];
            }
        }
        boolean overRun = my >= 0 && my < 8 && mx >= RUN_X;
        boolean middle = tab.equals("SPRITES") || tab.equals("SFX") || tab.equals("MUSIC");
        if (middle && input.mouseX >= 0) input.mouseX -= MIDDLE;   // these editors are drawn in the middle
        if (overRun && input.mouseClicked()) run = true;
        boolean overButton = switch (tab) {
            case "CODE" -> {
                code.update(input);
                yield false;
            }
            case "SPRITES" -> {
                sprites.update(input);
                yield sprites.overButton;
            }
            case "MAP" -> {
                map.update(input);
                yield map.overButton;
            }
            case "SFX" -> {
                sfx.update(input);
                yield sfx.overButton;
            }
            default -> {
                music.update(input);
                yield music.overButton;
            }
        };
        if (middle && input.mouseX >= 0) input.mouseX += MIDDLE;
        cursor = overTab || overRun || overButton ? "hand" : "arrow";
        if (messageTime > 0) messageTime--;
        return run;
    }

    void draw(Screen s, PixelFont font, int tokens) {
        boolean middle = tab.equals("SPRITES") || tab.equals("SFX") || tab.equals("MUSIC");
        if (middle) {
            s.rectfill(0, 8, Screen.WIDE - 1, 120, 1);
            s.pen.camX = -MIDDLE;
        }
        switch (tab) {
            case "CODE" -> code.draw(s, font);
            case "SPRITES" -> sprites.draw(s, font);
            case "MAP" -> map.draw(s, font);
            case "SFX" -> sfx.draw(s, font);
            default -> music.draw(s, font);
        }
        s.pen.camX = 0;
        // the top bar: tabs and RUN
        s.rectfill(0, 0, Screen.WIDE - 1, 7, 3);
        for (int i = 0; i < TABS.length; i++) tab(s, 1 + i * TAB_STEP, ICONS[i], tab.equals(TABS[i]));
        font.draw(s, hoveredTab.isEmpty() ? tab : hoveredTab, 1 + TABS.length * TAB_STEP + 2, 2, hoveredTab.isEmpty() ? 11 : 7);
        s.rectfill(RUN_X, 1, RUN_X + 22, 6, 11);
        font.draw(s, "RUN", RUN_X + 5, 2, 1);
        // the bottom bar: what's going on
        s.rectfill(0, 121, Screen.WIDE - 1, 127, 3);
        String status;
        if (messageTime > 0) status = message;
        else status = switch (tab) {
            case "CODE" -> "LINE " + code.line() + "/" + code.lineCount() + "  " + tokens + " TOKENS" + (dirty() ? " *" : "");
            case "SPRITES" -> sprites.hover.isEmpty() ? "SPRITE " + sprites.sprite + "  COLOUR " + sprites.colour : sprites.hover;
            case "MAP" -> map.hover.isEmpty() ? "PAINT TILES  RIGHT: PICK  ARROWS: MOVE" : map.hover;
            case "SFX" -> sfx.hover.isEmpty() ? "PAINT NOTES  RIGHT: ERASE  SPACE: PLAY" : sfx.hover;
            default -> music.hover.isEmpty() ? "SPACE: PLAY THE SONG FROM HERE" : music.hover;
        };
        font.draw(s, status, 2, 122, 7);
    }

    private static final int RUN_X = Screen.WIDE - 24;                  // the RUN button, top right
    private static final int MIDDLE = (Screen.WIDE - Screen.SIZE) / 2;  // where the 128-wide editors sit

    /** A tab: its icon, in green on a dark box when it's the open one. */
    private static void tab(Screen s, int x, String[] icon, boolean on) {
        if (on) s.rectfill(x, 1, x + TAB_WIDTH - 1, 7, 1);
        for (int r = 0; r < icon.length; r++) {
            for (int c = 0; c < icon[r].length(); c++) {
                if (icon[r].charAt(c) == '#') s.pset(x + 2 + c, 2 + r, on ? 11 : 7);
            }
        }
    }
}
