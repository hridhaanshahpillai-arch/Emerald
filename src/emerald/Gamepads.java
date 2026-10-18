package emerald;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

/**
 * Controllers (Xbox, PlayStation, Switch Pro and most others), up to 4 players, through GLFW.
 * Each one works like the 6 buttons:
 *
 *   0 left  1 right  2 up  3 down   the d-pad or the left stick
 *   4 O     A / Cross (or X / Square)
 *   5 X     B / Circle (or Y / Triangle)
 *
 * Player 0 is the first controller and the keyboard together. If controllers can't be used on a
 * computer (no GLFW there), games just get the keyboard.
 */
final class Gamepads {

    static final int PLAYERS = 4;
    private static final float STICK = 0.4f;    // how far the stick goes before it counts as a press

    private boolean ready;
    private final boolean[][] held = new boolean[PLAYERS][6];
    private final boolean[][] before = new boolean[PLAYERS][6];
    private GLFWGamepadState state;
    private int connected;

    /** Starts GLFW for controllers. Call on the main thread. Quietly does nothing where it can't work. */
    void start() {
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) return; // GLFW and the Mac's windows don't mix
        try {
            if (!GLFW.glfwInit()) return;
            state = GLFWGamepadState.create();
            ready = true;
        } catch (Throwable e) {
            ready = false; // no native GLFW for this computer: keyboard only
        }
    }

    /** Reads every controller (once a frame). */
    void poll() {
        for (int p = 0; p < PLAYERS; p++) {
            System.arraycopy(held[p], 0, before[p], 0, 6);
            java.util.Arrays.fill(held[p], false);
        }
        connected = 0;
        if (!ready) return;
        int player = 0;
        for (int jid = GLFW.GLFW_JOYSTICK_1; jid <= GLFW.GLFW_JOYSTICK_LAST && player < PLAYERS; jid++) {
            if (!GLFW.glfwJoystickIsGamepad(jid) || !GLFW.glfwGetGamepadState(jid, state)) continue;
            boolean[] b = held[player];
            float x = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X), y = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
            b[0] = down(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT) || x < -STICK;
            b[1] = down(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT) || x > STICK;
            b[2] = down(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP) || y < -STICK;
            b[3] = down(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN) || y > STICK;
            b[4] = down(GLFW.GLFW_GAMEPAD_BUTTON_A) || down(GLFW.GLFW_GAMEPAD_BUTTON_X);
            b[5] = down(GLFW.GLFW_GAMEPAD_BUTTON_B) || down(GLFW.GLFW_GAMEPAD_BUTTON_Y);
            player++;
        }
        connected = player;
    }

    private boolean down(int button) {
        return state.buttons(button) == GLFW.GLFW_PRESS;
    }

    boolean held(int player, int button) {
        return player >= 0 && player < PLAYERS && button >= 0 && button < 6 && held[player][button];
    }

    boolean pressed(int player, int button) {
        return held(player, button) && !before[player][button];
    }

    /** How many controllers are plugged in. */
    int connected() {
        return connected;
    }
}
