package emerald;

import sev7n.lang.Interpreter;
import sev7n.lang.Lexer;
import sev7n.lang.SevenError;
import sev7n.lang.runtime.SevenCallable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * The machine. Three screens, and Esc moves between them:
 *
 *   command line  where Emerald starts: LOAD, SAVE, RUN, NEW, DIR...   (Esc: the editor)
 *   editor        code and sprites                                   (Esc: the command line)
 *   game          the cart running at 60 frames a second             (Esc: the command line)
 *
 * A cart is Seven code with three functions: init() once at the start, update() and draw() every
 * frame. Ctrl+R runs the cart from anywhere. A cart saved outside Emerald is picked up straight away.
 */
final class Console {

    /** How much code a cart can have, in tokens (words, numbers and symbols). PICO-8 allows 8,192. */
    static final int TOKEN_LIMIT = 65_536;
    static final int FPS = 60;

    enum Mode { COMMAND, EDITOR, GAME }

    final Screen screen = new Screen();
    final Input input = new Input();
    final PixelFont font;
    final Audio audio = new Audio();
    final Editor editor = new Editor(audio);
    final Terminal terminal;
    int printX, printY;           // where print(text) without a position goes next
    volatile String cursor = "arrow";
    /** The sprite sheet the game draws from (a copy of the cart's: games can change it with sset). */
    final byte[] sheet = new byte[Cart.SHEET * Cart.SHEET];
    /** The map the game draws (a copy of the cart's: games can change it with mset). */
    final byte[] map = new byte[Cart.MAP_WIDTH * Cart.MAP_HEIGHT];
    /** The sprite flags the game reads (a copy of the cart's: games can change them with fset). */
    final byte[] flags = new byte[256];
    /** The game's camera and colour swaps (the editor draws without them). */
    final Screen.Pen gamePen = new Screen.Pen();
    /** The game's saved numbers (cartdata / dset / dget). */
    final SaveData saves = new SaveData();
    /** Screenshots (F6) and GIF clips (F8). */
    final Recorder recorder;
    /** Controllers (started by the window, on the main thread). */
    final Gamepads gamepads = new Gamepads();
    /** Particles, shake, dialogue and timers for games. */
    final Helpers helpers = new Helpers();
    /** Pause, step, rewind and the variables panel. */
    final Debugger debugger = new Debugger();

    private Mode mode = Mode.COMMAND;
    private Path folder;          // where LOAD, SAVE and DIR look
    private final Path home;      // the carts folder Emerald started in
    private Interpreter seven;
    private SevenCallable update, draw;
    private String error;         // shown instead of the game until the cart is fixed
    private int errorLine;
    private long cartTime;
    private long frames;          // game frames (for t())
    private long ticks;           // every frame
    private int tokens;
    private String confirming;    // a command that would lose unsaved work, typed once: type it again to go ahead
    private boolean quit;
    /** An exported game: it only plays (Esc closes it, no command line or editor). */
    boolean playerMode;
    /** The game's screen width (widescreen(true) makes it Screen.WIDE). */
    int gameWidth = Screen.SIZE;
    /** Messages from background work (exporting), shown on the command line at the next frame. */
    private final java.util.Queue<String> later = new java.util.concurrent.ConcurrentLinkedQueue<>();

    Console(Path cartsFolder, PixelFont font) {
        this.font = font;
        this.home = cartsFolder.toAbsolutePath().normalize();
        this.folder = home;
        this.terminal = new Terminal(this::command);
        this.recorder = new Recorder(home.getParent() != null ? home.getParent().resolve("screenshots") : home.resolve("screenshots"));
    }

    Mode mode() {
        return mode;
    }

    boolean quitRequested() {
        return quit;
    }

    /** For the window's title: the cart's name, with a * if it has unsaved changes. */
    String title() {
        return editor.name() + (editor.dirty() ? " *" : "");
    }

    /** The window's title: "EMERALD - name", or just the game's name for an exported game. */
    String windowTitle() {
        return playerMode ? editor.name().toUpperCase() : "EMERALD  -  " + title();
    }

    /** The cursor to show: the game's choice, or the editor's. */
    String cursorNow() {
        return switch (mode) {
            case GAME -> cursor;
            case EDITOR -> editor.cursor;
            case COMMAND -> "arrow";
        };
    }

    double time() {
        return frames / (double) FPS;
    }

    // ---------------------------------------------------------------- frames

    /** One frame of whichever screen is showing. */
    void frame() {
        if (ticks % 15 == 0 && !playerMode) reloadIfSaved();
        ticks++;
        input.newFrame();
        gamepads.poll();
        for (String message; (message = later.poll()) != null; ) terminal.say(message, message.startsWith("DONE") ? 11 : 6);
        boolean ctrlR = false;
        for (Input.KeyPress k : input.presses()) if (k.ctrl() && k.code() == java.awt.event.KeyEvent.VK_R) ctrlR = true;
        if (ctrlR && mode != Mode.EDITOR) run(); // (the editor handles its own Ctrl+R)
        if (!playerMode && input.pressed(java.awt.event.KeyEvent.VK_ESCAPE)) {
            switch (mode) {
                case COMMAND -> openEditor();
                case EDITOR -> mode = Mode.COMMAND;
                case GAME -> {
                    mode = Mode.COMMAND;
                    audio.stopAll();
                    if (error != null) terminal.say("THE GAME STOPPED: ESC FOR THE EDITOR", 9);
                }
            }
            input.newFrame(); // the Esc is used up; nothing else reacts to it
        }
        if (mode == Mode.GAME) {
            screen.use(gamePen);
            screen.width = gameWidth;
        } else {
            screen.usePlain();
            screen.width = Screen.WIDE;
        }
        switch (mode) {
            case COMMAND -> {
                terminal.update(input);
                if (mode == Mode.COMMAND) terminal.draw(screen, font);
            }
            case EDITOR -> {
                if (ticks % 30 == 0) tokens = countTokens(editor.code.text());
                if (editor.update(input, tokens)) run();
                if (mode == Mode.EDITOR) editor.draw(screen, font, tokens);
            }
            case GAME -> gameFrame();
        }
        // F6: a screenshot, F8: start or stop a GIF (of whatever is on screen, the game or the editor)
        if (input.pressed(java.awt.event.KeyEvent.VK_F6)) recorder.screenshot(screen, editor.name());
        if (input.pressed(java.awt.event.KeyEvent.VK_F8)) {
            if (recorder.recording()) recorder.stopGif(editor.name());
            else recorder.startGif();
        }
        recorder.capture(screen);
        if (recorder.messageTime > 0) {
            recorder.messageTime--;
            Screen.Pen pen = screen.pen;
            screen.usePlain(); // the message is drawn over the picture after it's captured, so it's not in the GIF
            int w = recorder.message.length() * PixelFont.ADVANCE + 3;
            screen.rectfill(0, 121, Math.min(127, w), 127, 3);
            font.draw(screen, recorder.message, 2, 122, 7);
            screen.use(pen);
        }
        if (recorder.recording() && ticks / 30 % 2 == 0) { // a blinking red dot while recording
            Screen.Pen pen = screen.pen;
            screen.usePlain();
            screen.circfill(123, 4, 2, 8);
            screen.use(pen);
        }
    }

    private void gameFrame() {
        if (error == null) {
            try {
                // the debugging keys (not in exported games): F5 pause, F10 one frame, F4 rewind, F7 variables
                boolean tools = !playerMode;
                if (tools && input.pressed(java.awt.event.KeyEvent.VK_F5)) debugger.paused = !debugger.paused;
                if (tools && input.pressed(java.awt.event.KeyEvent.VK_F7)) debugger.showVariables = !debugger.showVariables;
                boolean step = tools && debugger.paused && input.pressed(java.awt.event.KeyEvent.VK_F10);
                debugger.rewinding = tools && input.held(java.awt.event.KeyEvent.VK_F4);
                if (debugger.rewinding) {
                    debugger.stepBack(seven, this);
                    frames = Math.max(0, frames - 2);
                } else if (!debugger.paused || step) {
                    helpers.update(seven, input);
                    if (update != null) seven.callFunction(update);
                    if (tools) debugger.record(seven, this);
                }
                helpers.applyShake(gamePen);
                if (draw != null) seven.callFunction(draw);
                helpers.draw(screen, font);
                if (!debugger.rewinding && !debugger.paused) {
                    // the picture on the cart's label, if it's saved as a .png
                    int[] label = new int[Screen.SIZE * Screen.SIZE];
                    int lx = (screen.width - Screen.SIZE) / 2;   // the middle square of a wide screen
                    for (int i = 0; i < label.length; i++) label[i] = screen.shownRgb(lx + i % Screen.SIZE, i / Screen.SIZE);
                    editor.setLabel(label);
                }
                if (tools && (debugger.showVariables || debugger.paused || debugger.rewinding)) {
                    Screen.Pen pen = screen.pen;
                    screen.usePlain();
                    if (debugger.showVariables) debugger.drawVariables(seven, screen, font);
                    String state = debugger.rewinding ? "<< REWIND  " + debugger.secondsKept() + "S LEFT" : debugger.paused ? "PAUSED  F10: STEP  F5: GO" : "";
                    if (!state.isEmpty()) {
                        screen.rectfill(0, 0, screen.width - 1, 6, debugger.rewinding ? 2 : 3);
                        font.draw(screen, state, 2, 1, 7);
                    }
                    screen.use(pen);
                }
            } catch (SevenError e) {
                fail(e);
            } catch (StackOverflowError e) {
                fail("a function keeps calling itself forever");
            } catch (RuntimeException e) {
                fail("Emerald hit a problem: " + e);
            }
        }
        if (error != null) {
            screen.usePlain();
            drawError();
        }
        frames++;
    }

    // ---------------------------------------------------------------- moving between screens

    void openEditor() {
        mode = Mode.EDITOR;
        if (error != null) editor.showCode(errorLine);
        tokens = countTokens(editor.code.text());
    }

    /** Runs the cart as it is in the editor (saving it first if it has a file). */
    void run() {
        if (editor.dirty() && editor.file() != null) editor.save();
        audio.stopAll(); // editor previews stop; the game starts in silence
        Cart cart = editor.cart();
        mode = Mode.GAME;
        start(cart, editor.name());
    }

    /** Opens a cart file and runs it straight away (Emerald started with a cart). */
    void openAndRun(Path file) {
        try {
            editor.open(file);
            folder = file.toAbsolutePath().normalize().getParent();
            cartTime = Files.getLastModifiedTime(file).toMillis();
            run();
        } catch (IOException e) {
            terminal.say("CAN'T OPEN " + file.getFileName(), 9);
        }
    }

    /** Starts a cart afresh: its sprites, its code, then its init(). */
    private void start(Cart cart, String name) {
        error = null;
        errorLine = 0;
        update = null;
        draw = null;
        cursor = "arrow";
        gameWidth = Screen.SIZE;   // square until the cart asks for widescreen
        screen.width = Screen.SIZE;
        screen.cls(0);
        screen.color = 7;
        printX = printY = 0;
        frames = 0;
        System.arraycopy(cart.sheet, 0, sheet, 0, sheet.length);
        System.arraycopy(cart.map, 0, map, 0, map.length);
        System.arraycopy(cart.flags, 0, flags, 0, flags.length);
        gamePen.reset();
        screen.use(gamePen);
        saves.reset();
        try {
            tokens = countTokens(cart.code);
            if (tokens > TOKEN_LIMIT) {
                fail(name + " has " + tokens + " tokens: the most a cart can have is " + TOKEN_LIMIT);
                return;
            }
            seven = new Interpreter(System.out::println);
            Api.install(seven, this);
            helpers.reset();
            helpers.install(seven, this);
            debugger.reset(seven);
            seven.run(cart.code, name + ".emerald");
            update = function("update");
            draw = function("draw");
            SevenCallable init = function("init");
            if (init != null) seven.callFunction(init);
        } catch (SevenError e) {
            fail(e);
        } catch (RuntimeException e) {
            fail("Emerald hit a problem: " + e);
        }
    }

    /** How many tokens (words, numbers and symbols) some code has; comments don't count. */
    static int countTokens(String source) {
        try {
            return new Lexer(source, "cart").tokenize().size() - 1;
        } catch (SevenError e) {
            return -1; // can't even be read yet (an unfinished string, say)
        }
    }

    private SevenCallable function(String name) {
        try {
            return seven.lookupGlobal(name) instanceof SevenCallable f ? f : null;
        } catch (SevenError e) {
            return null; // the cart doesn't have it
        }
    }

    private void reloadIfSaved() {
        Path file = editor.file();
        if (file == null) return;
        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (modified == cartTime) return;
            cartTime = modified;
            if (modified == editor.lastWrite) return; // the editor's own save
            if (editor.dirty()) return;               // unsaved work here wins
            editor.reopen();                          // changed outside Emerald: pick it up
            if (mode == Mode.GAME) start(editor.cart(), editor.name());
        } catch (IOException ignored) {
            // being saved right now: try again in a moment
        }
    }

    // ---------------------------------------------------------------- the command line

    /** Runs a command typed at the command line. */
    private void command(String line) {
        String[] parts = line.trim().split("\\s+", 2);
        String verb = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim().replace("\"", "") : "";
        boolean confirmed = line.equalsIgnoreCase(confirming);
        confirming = null;
        switch (verb) {
            case "help" -> help();
            case "run" -> run();
            case "edit" -> openEditor();
            case "new" -> {
                if (needsConfirming(line, confirmed)) return;
                editor.newCart();
                terminal.say("A NEW CART. ESC TO EDIT IT", 11);
            }
            case "load" -> {
                if (arg.isEmpty()) {
                    terminal.say("LOAD WHICH CART? E.G. LOAD DEMO", 9);
                    return;
                }
                if (arg.equalsIgnoreCase("demo")) arg = "shard";   // LOAD DEMO: the game that comes with Emerald
                Path file = cartFile(arg);
                if (!Files.isRegularFile(file)) {
                    terminal.say("THERE'S NO CART CALLED " + arg.toUpperCase(), 9);
                    return;
                }
                if (needsConfirming(line, confirmed)) return;
                try {
                    editor.open(file);
                    cartTime = Files.getLastModifiedTime(file).toMillis();
                    error = null;
                    terminal.say("LOADED " + editor.name().toUpperCase(), 11);
                    terminal.say("RUN TO PLAY, ESC TO EDIT", 5);
                } catch (IOException e) {
                    terminal.say("CAN'T READ " + arg.toUpperCase(), 9);
                }
            }
            case "save" -> {
                if (arg.isEmpty() && editor.file() == null) {
                    terminal.say("SAVE AS WHAT? E.G. SAVE MYGAME", 9);
                    return;
                }
                Path file = arg.isEmpty() ? editor.file() : cartFile(arg);
                if (editor.saveAs(file)) {
                    cartTime = editor.lastWrite;
                    terminal.say("SAVED " + editor.name().toUpperCase(), 11);
                } else {
                    terminal.say("COULDN'T SAVE " + arg.toUpperCase(), 9);
                }
            }
            case "export" -> {
                if (editor.dirty() && editor.file() != null) editor.save();
                Path exports = home.getParent() != null ? home.getParent().resolve("exports") : home.resolve("exports");
                String name = editor.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
                if (name.isEmpty()) name = "game";
                terminal.say("EXPORTING " + name.toUpperCase() + "...", 11);
                Exporter.export(editor.cart(), name, exports, later::add);
            }
            case "dir", "ls" -> dir();
            case "cd" -> cd(arg);
            case "folder" -> {
                try {
                    java.awt.Desktop.getDesktop().open(folder.toFile());
                    terminal.say("OPENED THE FOLDER ON YOUR PC", 11);
                } catch (Exception e) {
                    terminal.say("CAN'T OPEN THE FOLDER HERE", 9);
                }
            }
            case "cls" -> terminal.clear();
            case "boot", "reboot" -> terminal.boot();
            case "exit", "shutdown", "quit" -> {
                if (needsConfirming(line, confirmed)) return;
                quit = true;
            }
            default -> terminal.say("NO COMMAND CALLED " + verb.toUpperCase() + ". TYPE HELP", 9);
        }
    }

    /** True (and a warning) if the command would lose unsaved changes and hasn't been typed twice yet. */
    private boolean needsConfirming(String line, boolean confirmed) {
        if (!editor.dirty() || confirmed) return false;
        terminal.say("UNSAVED CHANGES IN " + editor.name().toUpperCase() + "!", 9);
        terminal.say("SAVE FIRST, OR TYPE IT AGAIN", 9);
        confirming = line;
        return true;
    }

    private void help() {
        terminal.say("COMMANDS", 11);
        terminal.say("RUN         PLAY THE CART");
        terminal.say("EDIT        THE EDITOR (OR ESC)");
        terminal.say("LOAD NAME   OPEN A CART");
        terminal.say("SAVE NAME   SAVE THE CART");
        terminal.say("NEW         AN EMPTY CART");
        terminal.say("DIR         LIST THE CARTS");
        terminal.say("CD NAME     GO INTO A FOLDER");
        terminal.say("EXPORT      MAKE A GAME TO SHARE");
        terminal.say("FOLDER      OPEN IT ON YOUR PC");
        terminal.say("CLS         CLEAR THIS SCREEN");
        terminal.say("EXIT        TURN EMERALD OFF");
        terminal.say("CTRL+R RUNS FROM ANYWHERE", 3);
    }

    /** "hello" -> the folder's hello.emerald. */
    private Path cartFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        String file = lower.endsWith(".emerald") || lower.endsWith(".png") ? name : name + ".emerald";
        return folder.resolve(file).normalize();
    }

    private void dir() {
        terminal.say(where(), 6);
        try (Stream<Path> entries = Files.list(folder)) {
            List<Path> sorted = entries.sorted().toList();
            int shown = 0;
            for (Path p : sorted) {
                String n = p.getFileName().toString();
                if (Files.isDirectory(p) && !n.startsWith(".")) {
                    terminal.say(n.toUpperCase() + "/", 12);
                    shown++;
                }
            }
            for (Path p : sorted) {
                String n = p.getFileName().toString();
                String lower = n.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".emerald")) {
                    terminal.say(n.substring(0, n.length() - ".emerald".length()).toUpperCase(), 7);
                    shown++;
                } else if (lower.endsWith(".png")) {
                    terminal.say(n.toUpperCase(), 10); // a cart picture (or just a picture)
                    shown++;
                }
            }
            if (shown == 0) terminal.say("(NO CARTS HERE YET)", 5);
        } catch (IOException e) {
            terminal.say("CAN'T LOOK IN THIS FOLDER", 9);
        }
    }

    private void cd(String arg) {
        if (arg.isEmpty() || arg.equals("~")) {
            folder = home;
        } else {
            Path to = folder.resolve(arg).normalize();
            if (!Files.isDirectory(to)) {
                terminal.say("THERE'S NO FOLDER CALLED " + arg.toUpperCase(), 9);
                return;
            }
            folder = to;
        }
        terminal.say(where(), 6);
    }

    /** The folder, as a short path from the carts folder ("/" is the carts folder itself). */
    private String where() {
        if (folder.startsWith(home)) {
            String rest = home.relativize(folder).toString().replace('\\', '/');
            return "/" + rest.toUpperCase();
        }
        return folder.toString().toUpperCase();
    }

    // ---------------------------------------------------------------- errors

    private void fail(SevenError e) {
        errorLine = e.line();
        fail(e.getMessage() + (e.line() > 0 ? " (line " + e.line() + ")" : ""));
    }

    private void fail(String message) {
        error = message;
        System.out.println("ERROR: " + message);
    }

    /** The error, on the screen in capitals, wrapped to fit. */
    private void drawError() {
        screen.width = Screen.WIDE;
        screen.rectfill(0, 0, Screen.WIDE - 1, 127, 1);
        screen.rectfill(0, 0, Screen.WIDE - 1, 8, 3);
        screen.rectfill(0, 9, Screen.WIDE - 1, 9, 11);
        font.draw(screen, "ERROR", 2, 2, 7);
        int y = 14;
        for (String line : wrap(error, 56)) {
            font.draw(screen, line, 2, y, 7);
            y += PixelFont.LINE;
        }
        if (!playerMode) {
            font.draw(screen, "ESC, ESC: FIX IT IN THE EDITOR", 2, 110, 6);
            font.draw(screen, "(IT OPENS AT THE RIGHT LINE)", 2, 117, 5);
        }
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (line.length() + word.length() + (line.isEmpty() ? 0 : 1) > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines.size() > 14 ? lines.subList(0, 14) : lines;
    }
}
