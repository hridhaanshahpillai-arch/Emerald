package emerald;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Keys and the mouse. Games can use every key on the keyboard by name (key("a"), key("space")),
 * or the 6 classic buttons that also work for people used to fantasy consoles:
 *   0 left  1 right  2 up  3 down   (arrows or WASD)
 *   4 O     (Z, C or N)             5 X  (X, V or M)
 */
final class Input {

    private static final int[][] BUTTONS = {
            {KeyEvent.VK_LEFT, KeyEvent.VK_A}, {KeyEvent.VK_RIGHT, KeyEvent.VK_D},
            {KeyEvent.VK_UP, KeyEvent.VK_W}, {KeyEvent.VK_DOWN, KeyEvent.VK_S},
            {KeyEvent.VK_Z, KeyEvent.VK_C, KeyEvent.VK_N}, {KeyEvent.VK_X, KeyEvent.VK_V, KeyEvent.VK_M},
    };

    /** Key names scripts use, to Java's key codes. */
    static final Map<String, Integer> NAMES = new HashMap<>();

    static {
        for (char c = 'a'; c <= 'z'; c++) NAMES.put(String.valueOf(c), KeyEvent.getExtendedKeyCodeForChar(c));
        for (char c = '0'; c <= '9'; c++) NAMES.put(String.valueOf(c), KeyEvent.VK_0 + (c - '0'));
        for (int f = 1; f <= 12; f++) NAMES.put("f" + f, KeyEvent.VK_F1 + f - 1);
        NAMES.put("space", KeyEvent.VK_SPACE);
        NAMES.put("enter", KeyEvent.VK_ENTER);
        NAMES.put("shift", KeyEvent.VK_SHIFT);
        NAMES.put("ctrl", KeyEvent.VK_CONTROL);
        NAMES.put("alt", KeyEvent.VK_ALT);
        NAMES.put("tab", KeyEvent.VK_TAB);
        NAMES.put("escape", KeyEvent.VK_ESCAPE);
        NAMES.put("backspace", KeyEvent.VK_BACK_SPACE);
        NAMES.put("delete", KeyEvent.VK_DELETE);
        NAMES.put("up", KeyEvent.VK_UP);
        NAMES.put("down", KeyEvent.VK_DOWN);
        NAMES.put("left", KeyEvent.VK_LEFT);
        NAMES.put("right", KeyEvent.VK_RIGHT);
        NAMES.put("-", KeyEvent.VK_MINUS);
        NAMES.put("=", KeyEvent.VK_EQUALS);
        NAMES.put(",", KeyEvent.VK_COMMA);
        NAMES.put(".", KeyEvent.VK_PERIOD);
        NAMES.put("/", KeyEvent.VK_SLASH);
        NAMES.put(";", KeyEvent.VK_SEMICOLON);
    }

    // written by the window's thread, read by the game's: everything goes through these sets under a lock
    private final Set<Integer> down = new HashSet<>();
    private final Set<Integer> pressedSinceFrame = new HashSet<>();
    private Set<Integer> pressed = Set.of();     // this frame
    private Set<Integer> held = Set.of();        // this frame
    private boolean mouseDown, mouseClickedSinceFrame, mouseClicked, mouseHeld;
    volatile int mouseX = -1, mouseY = -1;      // in screen pixels (0..127), -1 when outside

    /** For the editor: every key press (with the OS's key repeat), letters typed, and the wheel. */
    record KeyPress(int code, boolean ctrl, boolean shift) { }

    private final java.util.List<KeyPress> pressesSinceFrame = new java.util.ArrayList<>();
    private final StringBuilder typedSinceFrame = new StringBuilder();
    private java.util.List<KeyPress> presses = java.util.List.of();
    private String typed = "";
    private int wheelSinceFrame, wheel;
    private boolean rightDown, rightClickedSinceFrame, rightClicked, rightHeld;

    synchronized void keyPressed(int code, boolean ctrl, boolean shift) {
        if (down.add(code)) pressedSinceFrame.add(code);
        pressesSinceFrame.add(new KeyPress(code, ctrl, shift));
    }

    synchronized void keyPressed(int code) {
        keyPressed(code, false, false);
    }

    synchronized void typed(char c) {
        if (c >= 32 && c < 127) typedSinceFrame.append(c);
    }

    synchronized void wheel(int clicks) {
        wheelSinceFrame += clicks;
    }

    synchronized void rightButton(boolean isDown) {
        if (isDown && !rightDown) rightClickedSinceFrame = true;
        rightDown = isDown;
    }

    java.util.List<KeyPress> presses() {
        return presses;
    }

    String typed() {
        return typed;
    }

    int wheel() {
        return wheel;
    }

    boolean rightHeld() {
        return rightHeld;
    }

    boolean rightClicked() {
        return rightClicked;
    }

    synchronized void keyReleased(int code) {
        down.remove(code);
    }

    synchronized void mouseButton(boolean isDown) {
        if (isDown && !mouseDown) mouseClickedSinceFrame = true;
        mouseDown = isDown;
    }

    /** Called once at the start of every frame: what's held and what was just pressed stays the same all frame. */
    synchronized void newFrame() {
        pressed = Set.copyOf(pressedSinceFrame);
        pressedSinceFrame.clear();
        held = Set.copyOf(down);
        mouseClicked = mouseClickedSinceFrame;
        mouseClickedSinceFrame = false;
        mouseHeld = mouseDown;
        presses = java.util.List.copyOf(pressesSinceFrame);
        pressesSinceFrame.clear();
        typed = typedSinceFrame.toString();
        typedSinceFrame.setLength(0);
        wheel = wheelSinceFrame;
        wheelSinceFrame = 0;
        rightClicked = rightClickedSinceFrame;
        rightClickedSinceFrame = false;
        rightHeld = rightDown;
    }

    boolean held(int code) {
        return held.contains(code);
    }

    boolean pressed(int code) {
        return pressed.contains(code);
    }

    boolean button(int b, boolean justPressed) {
        if (b < 0 || b >= BUTTONS.length) return false;
        for (int code : BUTTONS[b]) if (justPressed ? pressed(code) : held(code)) return true;
        return false;
    }

    /** A key by name, or null if there's no key with that name. */
    static Integer code(String name) {
        return NAMES.get(name.toLowerCase(Locale.ROOT));
    }

    boolean mouseHeld() {
        return mouseHeld;
    }

    boolean mouseClicked() {
        return mouseClicked;
    }
}
