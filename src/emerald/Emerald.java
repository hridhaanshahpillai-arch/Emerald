package emerald;

import javax.imageio.ImageIO;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFrame;

/**
 * EMERALD 🍡 a cozy fantasy console. A 128 x 128 screen, 16 colours, every key on the keyboard,
 * a big code limit, and your games written in Seven.
 *
 *   java -jar emerald.jar [cart.emerald]      (no cart: the command line)
 *
 * The window can be any size: the screen grows in whole steps so pixels stay sharp.
 */
public final class Emerald {

    private Emerald() { }

    public static void main(String[] args) throws Exception {
        // for automated checks: --screenshot file.png frames (saves the screen after that many frames, then quits)
        java.util.List<String> all = java.util.Arrays.asList(args);
        int shotAt = all.indexOf("--screenshot");
        Path shot = shotAt >= 0 ? Path.of(args[shotAt + 1]) : null;
        int shotFrames = shotAt >= 0 && shotAt + 2 < args.length ? Integer.parseInt(args[shotAt + 2]) : 30;
        Path cart = args.length > 0 && !args[0].startsWith("--") ? Path.of(args[0]) : null;
        if (cart != null && !Files.isRegularFile(cart)) {
            // an exported app starts wherever it's opened from: its cart sits next to emerald.jar
            Path beside = Path.of(Emerald.class.getProtectionDomain().getCodeSource().getLocation().toURI()).resolveSibling(cart.toString());
            if (Files.isRegularFile(beside)) cart = beside;
        }
        if (cart != null && !Files.isRegularFile(cart)) {
            System.out.println("There's no cart at " + cart.toAbsolutePath());
            System.exit(1);
        }
        PixelFont font = font();
        BufferedImage arrow, hand;
        try (InputStream a = Emerald.class.getResourceAsStream("/emerald/cursor.png");
             InputStream h = Emerald.class.getResourceAsStream("/emerald/cursor_hand.png")) {
            arrow = ImageIO.read(a);
            hand = ImageIO.read(h);
        }
        boolean player = all.contains("--player") && cart != null; // an exported game: it only plays
        Console console = new Console(player ? cart.toAbsolutePath().getParent() : cartsFolder(), font);
        console.playerMode = player;
        int[] gem = paletteIcon();
        console.terminal.setIcon(gem);
        if (shot == null) console.audio.start(); // (screenshot runs stay silent)
        if (cart != null) console.openAndRun(cart); // with a cart: play it; without: the command line
        int editorAt = all.indexOf("--editor"); // (tests) start in the editor: --editor code | sprites | sfx | music
        if (editorAt >= 0) {
            console.openEditor();
            if (editorAt + 1 < args.length) console.editor.show(args[editorAt + 1].toUpperCase());
        }
        int typeAt = all.indexOf("--type"); // (tests) type commands at the command line: --type "help;dir"
        if (typeAt >= 0) {
            for (String command : args[typeAt + 1].split(";")) {
                for (char c : command.toCharArray()) console.input.typed(c);
                console.input.keyPressed(java.awt.event.KeyEvent.VK_ENTER);
                console.frame();
                console.input.keyReleased(java.awt.event.KeyEvent.VK_ENTER);
            }
        }
        if (shot != null) {
            for (int i = 0; i < shotFrames; i++) console.frame();
            BufferedImage picture = new BufferedImage(console.screen.width * 4, Screen.SIZE * 4, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < picture.getHeight(); y++)
                for (int x = 0; x < picture.getWidth(); x++)
                    picture.setRGB(x, y, console.screen.shownRgb(x / 4, y / 4));
            ImageIO.write(picture, "png", shot.toFile());
            System.exit(0);
        }
        console.gamepads.start(); // controllers (GLFW wants the main thread)
        new Emerald.Window(console, arrow, hand).run();
    }

    private static PixelFont font;

    /** The pixel font (read once from inside the jar), or null if it can't be read. */
    static synchronized PixelFont font() {
        if (font == null) {
            try (InputStream ttf = Emerald.class.getResourceAsStream("/emerald/font.ttf")) {
                font = new PixelFont(ttf);
            } catch (Exception e) {
                System.out.println("Couldn't read the font: " + e);
            }
        }
        return font;
    }

    /** The icon as palette colours (each pixel the nearest of the 16), 16 x 16, for the boot logo. */
    private static int[] paletteIcon() throws java.io.IOException {
        try (InputStream in = Emerald.class.getResourceAsStream("/emerald/icon.png")) {
            if (in == null) return null;
            BufferedImage icon = ImageIO.read(in);
            int[] out = new int[16 * 16];
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int p = icon.getRGB(x * icon.getWidth() / 16, y * icon.getHeight() / 16);
                    if ((p >>> 24) < 128) {
                        out[y * 16 + x] = -1;
                        continue;
                    }
                    int best = 0, bestDistance = Integer.MAX_VALUE;
                    for (int c = 0; c < 16; c++) {
                        int q = Screen.PALETTE[c];
                        int dr = (p >> 16 & 255) - (q >> 16 & 255), dg = (p >> 8 & 255) - (q >> 8 & 255), db = (p & 255) - (q & 255);
                        int distance = dr * dr * 3 + dg * dg * 4 + db * db * 2;
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = c;
                        }
                    }
                    out[y * 16 + x] = best;
                }
            }
            return out;
        }
    }

    /** Emerald's carts folder: "carts" next to the build folder the jar is in (or in the current folder). Made if needed. */
    private static Path cartsFolder() throws java.io.IOException {
        Path folder = Path.of("carts");
        try {
            Path jar = Path.of(Emerald.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path beside = jar.getParent() == null || jar.getParent().getParent() == null ? null : jar.getParent().getParent().resolve("carts");
            if (beside != null && Files.isDirectory(beside)) folder = beside;
        } catch (java.net.URISyntaxException | RuntimeException ignored) {
            // running some other way: the current folder's carts
        }
        Files.createDirectories(folder);
        return folder;
    }

    /** The window: shows the screen as big as fits, sends it keys and the mouse, draws the cursor. */
    private static final class Window {
        private final Console console;
        private final BufferedImage arrow, hand;
        private final Point arrowTip, handTip;
        private final Canvas canvas = new Canvas();
        private final BufferedImage square = new BufferedImage(Screen.SIZE, Screen.SIZE, BufferedImage.TYPE_INT_RGB);
        private final BufferedImage wide = new BufferedImage(Screen.WIDE, Screen.SIZE, BufferedImage.TYPE_INT_RGB);
        private int shownWidth = Screen.SIZE;   // the screen's width when it was last shown (for the mouse)
        private volatile int windowMouseX = -1, windowMouseY = -1;
        private int scale = 1, offsetX, offsetY, side = Screen.SIZE, sideX = Screen.SIZE;

        Window(Console console, BufferedImage arrow, BufferedImage hand) {
            this.console = console;
            this.arrow = arrow;
            this.hand = hand;
            arrowTip = tip(arrow);
            handTip = tip(hand);
        }

        void run() throws InterruptedException {
            JFrame window = new JFrame("EMERALD");
            window.setIconImages(icons()); // the emerald, crisp at every size Windows asks for
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            canvas.setPreferredSize(new Dimension(Screen.WIDE * 5, Screen.SIZE * 5));
            canvas.setBackground(new Color(Screen.PALETTE[0]));
            canvas.setFocusTraversalKeysEnabled(false); // Tab is a key games can use
            canvas.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
                    new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), "none")); // Emerald draws its own
            window.add(canvas);
            window.pack();
            window.setLocationRelativeTo(null);
            this.window = window;
            setFullscreen(true);   // fullscreen from the start (sharp, never stretched); F11 for a window

            canvas.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_F11) {
                        toggleFullscreen = true;
                        return;
                    }
                    console.input.keyPressed(e.getKeyCode(), e.isControlDown(), e.isShiftDown());
                }
                @Override public void keyReleased(KeyEvent e) { console.input.keyReleased(e.getKeyCode()); }
                @Override public void keyTyped(KeyEvent e) { if (!e.isControlDown()) console.input.typed(e.getKeyChar()); }
            });
            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { moved(e.getX(), e.getY()); }
                @Override public void mouseDragged(MouseEvent e) { moved(e.getX(), e.getY()); }
                @Override public void mouseExited(MouseEvent e) { moved(-1, -1); }
                @Override public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) console.input.rightButton(true);
                    else console.input.mouseButton(true);
                    canvas.requestFocus();
                }
                @Override public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) console.input.rightButton(false);
                    else console.input.mouseButton(false);
                }
                @Override public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) { console.input.wheel(e.getWheelRotation()); }
            };
            canvas.addMouseListener(mouse);
            canvas.addMouseMotionListener(mouse);
            canvas.addMouseWheelListener(mouse);

            long frameTime = 1_000_000_000L / Console.FPS;
            long next = System.nanoTime();
            String shownTitle = "";
            while (window.isDisplayable() && !console.quitRequested()) {
                if (toggleFullscreen) {
                    toggleFullscreen = false;
                    setFullscreen(!fullscreen);
                }
                console.frame();
                show();
                String title = console.windowTitle();
                if (!title.equals(shownTitle)) {
                    window.setTitle(title);
                    shownTitle = title;
                }
                next += frameTime;
                long wait = next - System.nanoTime();
                if (wait > 0) Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
                else if (wait < -frameTime * 5) next = System.nanoTime(); // fell far behind (window dragged): don't rush to catch up
            }
            System.exit(0); // EXIT typed, or the window closed
        }

        private JFrame window;
        private boolean fullscreen;
        private volatile boolean toggleFullscreen;   // asked for by F11, done between frames

        /** The whole screen (no title bar, over the taskbar), or a normal window. */
        private void setFullscreen(boolean on) {
            fullscreen = on;
            java.awt.GraphicsDevice device = window.getGraphicsConfiguration().getDevice();
            window.dispose();                        // a window has to be closed to add or remove its title bar
            window.setUndecorated(on);
            if (on) {
                device.setFullScreenWindow(window);
            } else {
                device.setFullScreenWindow(null);
                window.pack();
                window.setLocationRelativeTo(null);
            }
            window.setVisible(true);
            canvas.createBufferStrategy(2);
            canvas.requestFocus();
        }

        private void moved(int x, int y) {
            windowMouseX = x;
            windowMouseY = y;
            boolean inside = x >= offsetX && y >= offsetY && x < offsetX + sideX && y < offsetY + side;
            console.input.mouseX = inside ? (x - offsetX) * shownWidth / sideX : -1;
            console.input.mouseY = inside ? (y - offsetY) * Screen.SIZE / side : -1;
        }

        private void show() {
            BufferStrategy buffers = canvas.getBufferStrategy();
            if (buffers == null) return;
            int width = canvas.getWidth(), height = canvas.getHeight();
            int sw = console.screen.width;
            shownWidth = sw;
            scale = Math.max(1, Math.min(width / sw, height / Screen.SIZE)); // whole steps: every pixel the same size
            side = Screen.SIZE * scale;
            sideX = sw * scale;
            offsetX = (width - sideX) / 2;
            offsetY = (height - side) / 2;
            BufferedImage frame = sw == Screen.WIDE ? wide : square;
            console.screen.toRgb(((DataBufferInt) frame.getRaster().getDataBuffer()).getData());
            Graphics2D g = (Graphics2D) buffers.getDrawGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setColor(new Color(0x111116));
            g.fillRect(0, 0, width, height);
            g.drawImage(frame, offsetX, offsetY, sideX, side, null);
            String cursor = console.cursorNow();
            if (windowMouseX >= 0 && !cursor.equals("none")) {
                BufferedImage picture = cursor.equals("hand") ? hand : arrow;
                Point tip = cursor.equals("hand") ? handTip : arrowTip;
                int cursorScale = Math.max(2, scale / 2); // crisp, and a comfortable size at any window size
                g.drawImage(picture, windowMouseX - tip.x * cursorScale, windowMouseY - tip.y * cursorScale,
                        picture.getWidth() * cursorScale, picture.getHeight() * cursorScale, null);
            }
            g.dispose();
            buffers.show();
            Toolkit.getDefaultToolkit().sync();
        }

        /** Emerald's icon (assets/icon.png) blown up with sharp pixels to the sizes windows and taskbars use. */
        private static java.util.List<java.awt.Image> icons() {
            java.util.List<java.awt.Image> sizes = new java.util.ArrayList<>();
            try (InputStream in = Emerald.class.getResourceAsStream("/emerald/icon.png")) {
                if (in == null) return sizes;
                BufferedImage icon = ImageIO.read(in);
                for (int size : new int[] {16, 32, 48, 64, 128, 256}) {
                    BufferedImage big = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < size; y++)
                        for (int x = 0; x < size; x++)
                            big.setRGB(x, y, icon.getRGB(x * icon.getWidth() / size, y * icon.getHeight() / size));
                    sizes.add(big);
                }
            } catch (java.io.IOException ignored) {
                // no icon: Java's own
            }
            return sizes;
        }

        /** The first pixel from the top that isn't see-through: where the arrow (or finger) points. */
        private static Point tip(BufferedImage image) {
            for (int y = 0; y < image.getHeight(); y++)
                for (int x = 0; x < image.getWidth(); x++)
                    if ((image.getRGB(x, y) >>> 24) > 0) return new Point(x, y);
            return new Point();
        }
    }
}
