package emerald;

import sev7n.lang.Interpreter;
import sev7n.lang.runtime.SevenCallable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tools for finding out what a game is doing (while making it; exported games don't have them):
 *
 *   F5   pause / carry on          F10  one frame forward (while paused)
 *   F4   hold to rewind (up to 10 seconds back)
 *   F7   show the game's variables, live
 *
 * Rewinding puts back the cart's variables, map, sprites and flags as they were each frame.
 */
final class Debugger {

    private static final int HISTORY = 600; // 10 seconds of frames

    private record Snapshot(Map<String, Object> globals, byte[] map, byte[] sheet, byte[] flags) { }

    private final Deque<Snapshot> history = new ArrayDeque<>();
    private Set<String> builtins = Set.of();
    boolean paused, showVariables, rewinding;

    /** A cart starting: no history; everything defined so far belongs to Emerald, not the cart. */
    void reset(Interpreter seven) {
        history.clear();
        paused = false;
        rewinding = false;
        builtins = Set.copyOf(seven.globalNames());
    }

    /** The cart's own variables (not its functions, not Emerald's), sorted by name. */
    private List<String> variables(Interpreter seven) {
        List<String> names = new ArrayList<>();
        for (String name : new TreeSet<>(seven.globalNames())) {
            if (builtins.contains(name)) continue;
            if (seven.lookupGlobal(name) instanceof SevenCallable) continue;
            names.add(name);
        }
        return names;
    }

    /** Remembers this frame (after update), for rewinding. */
    void record(Interpreter seven, Console console) {
        Map<String, Object> globals = new LinkedHashMap<>();
        for (String name : variables(seven)) globals.put(name, copy(seven.lookupGlobal(name)));
        history.addLast(new Snapshot(globals, console.map.clone(), console.sheet.clone(), console.flags.clone()));
        while (history.size() > HISTORY) history.removeFirst();
    }

    /** Goes back one frame. Returns false when there's nothing older left. */
    boolean stepBack(Interpreter seven, Console console) {
        if (history.size() <= 1) return false;
        history.removeLast();
        Snapshot s = history.peekLast();
        for (Map.Entry<String, Object> e : s.globals().entrySet()) seven.defineGlobal(e.getKey(), copy(e.getValue()));
        System.arraycopy(s.map(), 0, console.map, 0, console.map.length);
        System.arraycopy(s.sheet(), 0, console.sheet, 0, console.sheet.length);
        System.arraycopy(s.flags(), 0, console.flags, 0, console.flags.length);
        return true;
    }

    int secondsKept() {
        return history.size() / Console.FPS;
    }

    /** A value copied deeply enough that changing the game's lists and maps later doesn't change the copy. */
    @SuppressWarnings("unchecked")
    private static Object copy(Object value) {
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) out.add(copy(item));
            return out;
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) out.put(e.getKey(), copy(e.getValue()));
            return out;
        }
        if (value instanceof sev7n.math.Vec3 v) return v.copy();
        return value; // numbers, text, colours, true/false, none: they never change
    }

    /** The F7 panel: the cart's variables and their values, over the game. */
    void drawVariables(Interpreter seven, Screen s, PixelFont font) {
        List<String> names = variables(seven);
        int rows = Math.min(names.size(), 17);
        int cols = (s.width - 4) / PixelFont.ADVANCE;
        s.rectfill(0, 8, s.width - 1, 12 + rows * PixelFont.LINE, 0);
        int y = 10;
        for (int i = 0; i < rows; i++) {
            String name = names.get(i);
            String value = sev7n.lang.runtime.Values.stringify(seven.lookupGlobal(name));
            Object raw = seven.lookupGlobal(name);
            if (raw instanceof Double d && d != Math.floor(d)) value = String.format("%.2f", d);
            if (raw instanceof List<?> l) value = "LIST OF " + l.size();
            String line = name + " = " + value;
            font.draw(s, line.length() > cols ? line.substring(0, cols) : line, 2, y, 7);
            y += PixelFont.LINE;
        }
        if (names.size() > rows) font.draw(s, "+" + (names.size() - rows) + " MORE", 2, y, 5);
        if (names.isEmpty()) font.draw(s, "NO VARIABLES YET", 2, 10, 5);
    }
}
