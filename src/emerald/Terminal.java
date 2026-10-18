package emerald;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Emerald's command line: the screen it boots to. Type a command and press Enter (HELP lists them).
 * Up and Down bring back earlier commands; Ctrl+V pastes.
 */
final class Terminal {

    static final String VERSION = "0.1";
    private static final int ROWS = 20;               // lines that fit under the prompt
    private static final String LOGO = "\u0000logo";  // a line that's the boot logo instead of text

    private record Line(String text, int colour) { }

    private final List<Line> lines = new ArrayList<>();
    private final StringBuilder typing = new StringBuilder();
    private int caret;
    private final List<String> history = new ArrayList<>();
    private int historyAt;
    private int blink;
    private final Consumer<String> commands;

    Terminal(Consumer<String> commands) {
        this.commands = commands;
        boot();
    }

    void boot() {
        lines.clear();
        lines.add(new Line(LOGO, 0));
        say("EMERALD " + VERSION, 6);
        say("A COZY FANTASY CONSOLE", 5);
        say("");
        say("TYPE HELP FOR HELP", 11);
        say("");
    }

    void say(String text) {
        say(text, 6);
    }

    /** Adds text to the screen, wrapped at the edge (56 characters). */
    void say(String text, int colour) {
        for (String part : text.split("\n", -1)) {
            while (part.length() > 56) {
                int cut = part.lastIndexOf(' ', 56);
                if (cut <= 0) cut = 56;
                lines.add(new Line(part.substring(0, cut), colour));
                part = "  " + part.substring(cut).trim(); // the rest goes on, a little indented
            }
            lines.add(new Line(part, colour));
        }
        while (lines.size() > 200) lines.remove(0);
    }

    void clear() {
        lines.clear();
    }

    void update(Input input) {
        blink++;
        for (char c : input.typed().toCharArray()) {
            typing.insert(caret++, c);
            blink = 0;
        }
        for (Input.KeyPress k : input.presses()) {
            blink = 0;
            switch (k.code()) {
                case KeyEvent.VK_ENTER -> {
                    String command = typing.toString().trim();
                    lines.add(new Line("> " + typing, 7));
                    typing.setLength(0);
                    caret = 0;
                    if (!command.isEmpty()) {
                        if (history.isEmpty() || !history.get(history.size() - 1).equals(command)) history.add(command);
                        commands.accept(command);
                    }
                    historyAt = history.size();
                }
                case KeyEvent.VK_BACK_SPACE -> {
                    if (caret > 0) typing.deleteCharAt(--caret);
                }
                case KeyEvent.VK_DELETE -> {
                    if (caret < typing.length()) typing.deleteCharAt(caret);
                }
                case KeyEvent.VK_LEFT -> caret = Math.max(0, caret - 1);
                case KeyEvent.VK_RIGHT -> caret = Math.min(typing.length(), caret + 1);
                case KeyEvent.VK_HOME -> caret = 0;
                case KeyEvent.VK_END -> caret = typing.length();
                case KeyEvent.VK_UP -> recall(-1);
                case KeyEvent.VK_DOWN -> recall(1);
                case KeyEvent.VK_V -> {
                    if (k.ctrl()) paste();
                }
                default -> { }
            }
        }
    }

    private void recall(int by) {
        if (history.isEmpty()) return;
        historyAt = Math.max(0, Math.min(history.size(), historyAt + by));
        typing.setLength(0);
        if (historyAt < history.size()) typing.append(history.get(historyAt));
        caret = typing.length();
    }

    private void paste() {
        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data instanceof String text) {
                String line = text.split("\\R", 2)[0];
                typing.insert(caret, line);
                caret += line.length();
            }
        } catch (Exception ignored) {
            // nothing usable on the clipboard
        }
    }

    void draw(Screen s, PixelFont font) {
        s.cls(0);
        // the newest lines, with the prompt under them; the logo is three lines tall
        int needed = 1; // the prompt
        int first = lines.size();
        while (first > 0) {
            int height = lines.get(first - 1).text().equals(LOGO) ? 4 : 1;
            if (needed + height > ROWS + 1) break;
            needed += height;
            first--;
        }
        int y = 2;
        for (int i = first; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (line.text().equals(LOGO)) {
                drawLogo(s, font, y);
                y += PixelFont.LINE * 4;
            } else {
                font.draw(s, line.text(), 2, y, line.colour());
                y += PixelFont.LINE;
            }
        }
        // the prompt: what's being typed, scrolled so the caret stays in view
        int room = 54;
        int start = Math.max(0, caret - room + 1);
        String shown = typing.substring(start, Math.min(typing.length(), start + room));
        font.draw(s, ">", 2, y, 11);
        font.draw(s, shown, 10, y, 7);
        if (blink / 20 % 2 == 0) s.rectfill(10 + (caret - start) * PixelFont.ADVANCE, y, 12 + (caret - start) * PixelFont.ADVANCE, y + 4, 11);
    }

    /** The icon in palette colours, 16 x 16 (-1 = see-through), for the logo. */
    private int[] icon;

    void setIcon(int[] pixels) {
        icon = pixels;
    }

    /** EMERALD in big green letters, with the gem next to it. */
    private void drawLogo(Screen s, PixelFont font, int y) {
        font.drawBig(s, "EMERALD", 3, y + 2, 3, 3);   // the deep green shadow
        font.drawBig(s, "EMERALD", 2, y + 1, 11, 3);
        if (icon == null) return;
        for (int py = 0; py < 16; py++) {
            for (int px = 0; px < 16; px++) {
                int c = icon[py * 16 + px];
                if (c >= 0) s.pset(88 + px, y + 1 + py, c);
            }
        }
    }
}
