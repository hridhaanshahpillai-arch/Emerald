package emerald;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * The code editor: the cart's Seven code, 31 characters wide, with colours for keywords, numbers,
 * strings and comments. Arrows / Home / End / Page Up / Page Down move, Shift selects, Ctrl+C / X / V
 * copy, cut and paste (the whole line when nothing is selected), Ctrl+Z / Y undo and redo,
 * Ctrl+A selects everything. Click to place the cursor, the wheel scrolls.
 */
final class CodeEditor {

    static final int TOP = 9, BOTTOM = 120;                    // the text area, between the bars
    static final int COLUMNS = 56, ROWS = (BOTTOM - TOP) / PixelFont.LINE;
    private static final int LEFT = 2;
    private static final Set<String> KEYWORDS = Set.of("func", "var", "if", "else", "while", "for", "in", "return", "break",
            "continue", "and", "or", "not", "true", "false", "none", "behaviour", "style");

    private final List<StringBuilder> lines = new ArrayList<>();
    private int row, col;                                       // the cursor
    private int anchorRow = -1, anchorCol;                     // the other end of the selection (-1: none)
    private int scrollRow, scrollCol;
    private int wantCol;                                       // the column up/down try to keep
    private boolean dirty;
    private final Deque<String[]> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private int blink;

    void setText(String text) {
        lines.clear();
        for (String line : text.replace("\r\n", "\n").replace("\t", "  ").split("\n", -1)) lines.add(new StringBuilder(line));
        if (lines.isEmpty()) lines.add(new StringBuilder());
        row = col = scrollRow = scrollCol = 0;
        anchorRow = -1;
        undo.clear();
        redo.clear();
        dirty = false;
    }

    String text() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) out.append('\n');
            out.append(lines.get(i));
        }
        return out.toString();
    }

    boolean dirty() {
        return dirty;
    }

    void saved() {
        dirty = false;
    }

    int line() {
        return row + 1;
    }

    int lineCount() {
        return lines.size();
    }

    /** Puts the cursor on a line (an error's line, say). */
    void goTo(int line) {
        row = Math.max(0, Math.min(lines.size() - 1, line - 1));
        col = 0;
        anchorRow = -1;
        reveal();
    }

    // ---------------------------------------------------------------- editing

    void update(Input input) {
        blink++;
        for (Input.KeyPress press : input.presses()) key(press);
        String typed = input.typed();
        if (!typed.isEmpty() && !ctrlHeld(input)) {
            for (char c : typed.toCharArray()) insert(String.valueOf(c));
        }
        if (input.wheel() != 0) scrollRow = Math.max(0, Math.min(Math.max(0, lines.size() - ROWS), scrollRow + input.wheel() * 3));
        // click to put the cursor there (drag to select)
        int mx = input.mouseX, my = input.mouseY;
        if (my >= TOP && my < BOTTOM && mx >= 0 && (input.mouseClicked() || input.mouseHeld())) {
            int r = Math.min(lines.size() - 1, scrollRow + (my - TOP) / PixelFont.LINE);
            int c = Math.min(lines.get(r).length(), scrollCol + Math.max(0, (mx - LEFT + 2) / PixelFont.ADVANCE));
            if (input.mouseClicked()) {
                anchorRow = r;
                anchorCol = c;
            }
            row = r;
            col = c;
            wantCol = col;
            blink = 0;
        }
        if (!input.mouseHeld() && anchorRow == row && anchorCol == col) anchorRow = -1;
    }

    private static boolean ctrlHeld(Input input) {
        return input.held(KeyEvent.VK_CONTROL);
    }

    private void key(Input.KeyPress k) {
        blink = 0;
        boolean shift = k.shift(), ctrl = k.ctrl();
        switch (k.code()) {
            case KeyEvent.VK_LEFT -> move(shift, () -> {
                if (col > 0) col--;
                else if (row > 0) col = lines.get(--row).length();
            });
            case KeyEvent.VK_RIGHT -> move(shift, () -> {
                if (col < lines.get(row).length()) col++;
                else if (row < lines.size() - 1) {
                    row++;
                    col = 0;
                }
            });
            case KeyEvent.VK_UP -> moveVertically(shift, -1);
            case KeyEvent.VK_DOWN -> moveVertically(shift, 1);
            case KeyEvent.VK_PAGE_UP -> moveVertically(shift, -ROWS);
            case KeyEvent.VK_PAGE_DOWN -> moveVertically(shift, ROWS);
            case KeyEvent.VK_HOME -> move(shift, () -> {
                int indent = indent(lines.get(row));
                col = col == indent ? 0 : indent; // first to the code, then to the very start
            });
            case KeyEvent.VK_END -> move(shift, () -> col = lines.get(row).length());
            case KeyEvent.VK_BACK_SPACE -> {
                if (!deleteSelection()) {
                    if (col > 0 || row > 0) {
                        remember();
                        if (col > 0) {
                            // back over an indent two spaces at a time
                            int n = col >= 2 && lines.get(row).substring(0, col).isBlank() && col % 2 == 0 ? 2 : 1;
                            lines.get(row).delete(col - n, col);
                            col -= n;
                        } else {
                            col = lines.get(row - 1).length();
                            lines.get(row - 1).append(lines.remove(row));
                            row--;
                        }
                        dirty = true;
                    }
                }
            }
            case KeyEvent.VK_DELETE -> {
                if (!deleteSelection()) {
                    remember();
                    if (col < lines.get(row).length()) lines.get(row).deleteCharAt(col);
                    else if (row < lines.size() - 1) lines.get(row).append(lines.remove(row + 1));
                    dirty = true;
                }
            }
            case KeyEvent.VK_ENTER -> newline();
            case KeyEvent.VK_TAB -> insert("  ");
            case KeyEvent.VK_A -> {
                if (ctrl) {
                    anchorRow = 0;
                    anchorCol = 0;
                    row = lines.size() - 1;
                    col = lines.get(row).length();
                }
            }
            case KeyEvent.VK_C -> {
                if (ctrl) copy(false);
            }
            case KeyEvent.VK_X -> {
                if (ctrl) copy(true);
            }
            case KeyEvent.VK_V -> {
                if (ctrl) paste();
            }
            case KeyEvent.VK_Z -> {
                if (ctrl) undoRedo(shift ? redo : undo, shift ? undo : redo);
            }
            case KeyEvent.VK_Y -> {
                if (ctrl) undoRedo(redo, undo);
            }
            default -> { }
        }
        reveal();
    }

    private void move(boolean select, Runnable step) {
        if (select && anchorRow < 0) {
            anchorRow = row;
            anchorCol = col;
        } else if (!select) {
            anchorRow = -1;
        }
        step.run();
        wantCol = col;
    }

    private void moveVertically(boolean select, int by) {
        int keep = wantCol;
        move(select, () -> {
            row = Math.max(0, Math.min(lines.size() - 1, row + by));
            col = Math.min(keep, lines.get(row).length());
        });
        wantCol = keep;
    }

    private void newline() {
        remember();
        deleteSelectionQuietly();
        StringBuilder current = lines.get(row);
        String rest = current.substring(col);
        current.setLength(col);
        // keep the indent, and one step more after an opening brace
        String before = current.toString().stripTrailing();
        int indent = indent(current) + (before.endsWith("{") ? 2 : 0);
        String restTrimmed = rest.stripLeading();
        if (restTrimmed.startsWith("}") && before.endsWith("{")) {
            // between { and }: the } goes on its own line below
            lines.add(row + 1, new StringBuilder(" ".repeat(Math.max(0, indent - 2)) + restTrimmed));
            lines.add(row + 1, new StringBuilder(" ".repeat(indent)));
        } else {
            if (restTrimmed.startsWith("}")) indent = Math.max(0, indent - 2);
            lines.add(row + 1, new StringBuilder(" ".repeat(indent) + restTrimmed));
        }
        row++;
        col = indent;
        wantCol = col;
        dirty = true;
    }

    private void insert(String text) {
        remember();
        deleteSelectionQuietly();
        String[] parts = text.replace("\r\n", "\n").replace("\t", "  ").split("\n", -1);
        StringBuilder current = lines.get(row);
        String rest = current.substring(col);
        current.setLength(col);
        current.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            lines.add(++row, new StringBuilder(parts[i]));
        }
        col = lines.get(row).length();
        lines.get(row).append(rest);
        // typing a } on an otherwise empty line: line it up with its {
        if (text.equals("}") && lines.get(row).toString().trim().equals("}") && col >= 3) {
            String trimmed = lines.get(row).toString().trim();
            int indent = Math.max(0, indent(lines.get(row)) - 2);
            lines.set(row, new StringBuilder(" ".repeat(indent) + trimmed));
            col = indent + 1;
        }
        wantCol = col;
        dirty = true;
    }

    private static int indent(CharSequence line) {
        int n = 0;
        while (n < line.length() && line.charAt(n) == ' ') n++;
        return n;
    }

    // ---------------------------------------------------------------- selection, clipboard, undo

    private boolean hasSelection() {
        return anchorRow >= 0 && (anchorRow != row || anchorCol != col);
    }

    /** The selection as {startRow, startCol, endRow, endCol}, start first. */
    private int[] selection() {
        boolean anchorFirst = anchorRow < row || (anchorRow == row && anchorCol < col);
        return anchorFirst ? new int[] {anchorRow, anchorCol, row, col} : new int[] {row, col, anchorRow, anchorCol};
    }

    private String selectedText() {
        int[] s = selection();
        if (s[0] == s[2]) return lines.get(s[0]).substring(s[1], s[3]);
        StringBuilder out = new StringBuilder(lines.get(s[0]).substring(s[1]));
        for (int r = s[0] + 1; r < s[2]; r++) out.append('\n').append(lines.get(r));
        return out.append('\n').append(lines.get(s[2]), 0, s[3]).toString();
    }

    private boolean deleteSelection() {
        if (!hasSelection()) return false;
        remember();
        deleteSelectionQuietly();
        return true;
    }

    private void deleteSelectionQuietly() {
        if (!hasSelection()) {
            anchorRow = -1;
            return;
        }
        int[] s = selection();
        String tail = lines.get(s[2]).substring(s[3]);
        lines.get(s[0]).setLength(s[1]);
        lines.get(s[0]).append(tail);
        for (int r = s[2]; r > s[0]; r--) lines.remove(r);
        row = s[0];
        col = s[1];
        anchorRow = -1;
        dirty = true;
    }

    private void copy(boolean cut) {
        String text;
        if (hasSelection()) {
            text = selectedText();
            if (cut) deleteSelection();
        } else {
            text = lines.get(row) + "\n"; // nothing selected: the whole line
            if (cut) {
                remember();
                if (lines.size() > 1) lines.remove(row);
                else lines.get(0).setLength(0);
                row = Math.min(row, lines.size() - 1);
                col = Math.min(col, lines.get(row).length());
                dirty = true;
            }
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (IllegalStateException ignored) {
            // the clipboard is busy: nothing copied this time
        }
    }

    private void paste() {
        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data instanceof String text && !text.isEmpty()) insert(text);
        } catch (Exception ignored) {
            // nothing usable on the clipboard
        }
    }

    /** Remembers the text before a change, for Ctrl+Z. */
    private void remember() {
        undo.push(new String[] {text(), row + ":" + col});
        if (undo.size() > 200) undo.removeLast();
        redo.clear();
    }

    private void undoRedo(Deque<String[]> from, Deque<String[]> to) {
        if (from.isEmpty()) return;
        to.push(new String[] {text(), row + ":" + col});
        String[] step = from.pop();
        lines.clear();
        for (String line : step[0].split("\n", -1)) lines.add(new StringBuilder(line));
        String[] at = step[1].split(":");
        row = Math.min(Integer.parseInt(at[0]), lines.size() - 1);
        col = Math.min(Integer.parseInt(at[1]), lines.get(row).length());
        anchorRow = -1;
        dirty = true;
    }

    /** Scrolls so the cursor can be seen. */
    private void reveal() {
        if (row < scrollRow) scrollRow = row;
        if (row >= scrollRow + ROWS) scrollRow = row - ROWS + 1;
        if (col < scrollCol) scrollCol = Math.max(0, col - 4);
        if (col >= scrollCol + COLUMNS) scrollCol = col - COLUMNS + 4;
    }

    // ---------------------------------------------------------------- drawing

    void draw(Screen screen, PixelFont font) {
        screen.rectfill(0, TOP - 1, Screen.WIDE - 1, BOTTOM, 1);
        int[] sel = hasSelection() ? selection() : null;
        for (int r = 0; r < ROWS && scrollRow + r < lines.size(); r++) {
            int lineIndex = scrollRow + r;
            String line = lines.get(lineIndex).toString();
            int y = TOP + r * PixelFont.LINE;
            int[] colours = colours(line);
            for (int c = scrollCol; c < Math.min(line.length(), scrollCol + COLUMNS + 1); c++) {
                int x = LEFT + (c - scrollCol) * PixelFont.ADVANCE;
                if (sel != null && inside(sel, lineIndex, c)) screen.rectfill(x - 1, y - 1, x + 3, y + 4, 2);
                font.drawCode(screen, line.charAt(c), x, y, colours[c]);
            }
            if (sel != null && inside(sel, lineIndex, line.length()) && lineIndex < sel[2]) {
                int x = LEFT + (line.length() - scrollCol) * PixelFont.ADVANCE;
                if (x >= 0) screen.rectfill(x - 1, y - 1, x + 1, y + 4, 2); // the line break is selected too
            }
        }
        if (blink / 20 % 2 == 0) {
            int x = LEFT + (col - scrollCol) * PixelFont.ADVANCE - 1, y = TOP + (row - scrollRow) * PixelFont.LINE - 1;
            screen.rectfill(x, y, x, y + 5, 11);
        }
    }

    private static boolean inside(int[] s, int r, int c) {
        if (r < s[0] || r > s[2]) return false;
        if (r == s[0] && c < s[1]) return false;
        return !(r == s[2] && c >= s[3]);
    }

    /** A colour for every character: keywords, numbers, strings, comments, the rest. */
    private static int[] colours(String line) {
        int[] out = new int[line.length()];
        java.util.Arrays.fill(out, 7);
        int i = 0;
        while (i < line.length()) {
            char ch = line.charAt(i);
            if (ch == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                for (int j = i; j < line.length(); j++) out[j] = 5;
                break;
            }
            if (ch == '"') {
                int j = i + 1;
                while (j < line.length() && line.charAt(j) != '"') j += line.charAt(j) == '\\' ? 2 : 1;
                for (int k = i; k <= Math.min(j, line.length() - 1); k++) out[k] = 10;
                i = j + 1;
                continue;
            }
            if (Character.isDigit(ch)) {
                int j = i;
                while (j < line.length() && (Character.isLetterOrDigit(line.charAt(j)) || line.charAt(j) == '.')) j++;
                for (int k = i; k < j; k++) out[k] = 9;
                i = j;
                continue;
            }
            if (Character.isLetter(ch) || ch == '_') {
                int j = i;
                while (j < line.length() && (Character.isLetterOrDigit(line.charAt(j)) || line.charAt(j) == '_')) j++;
                String word = line.substring(i, j);
                int colour = KEYWORDS.contains(word) ? 11 : j < line.length() && line.charAt(j) == '(' ? 12 : 7;
                for (int k = i; k < j; k++) out[k] = colour;
                i = j;
                continue;
            }
            if ("{}()[]".indexOf(ch) >= 0) out[i] = 6;
            else if ("+-*/%=<>!?:".indexOf(ch) >= 0) out[i] = 15;
            i++;
        }
        return out;
    }
}
