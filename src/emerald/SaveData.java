package emerald;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saved numbers for high scores, unlocked levels and settings: 64 slots per game that stay when
 * the game is closed. A game picks its name once with cartdata("mygame"), then uses dset(slot, n)
 * and dget(slot). They're kept in your user folder (Emerald\saves\mygame.txt), one number a line.
 */
final class SaveData {

    static final int SLOTS = 64;

    private final double[] values = new double[SLOTS];
    private Path file;               // null until cartdata() is called

    /** A game starting: nothing open yet. */
    void reset() {
        file = null;
        java.util.Arrays.fill(values, 0);
    }

    /** Where every game's saves live. */
    static Path folder() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null ? Path.of(appData) : Path.of(System.getProperty("user.home"), ".config");
        return base.resolve("Emerald").resolve("saves");
    }

    /** Opens (or starts) the saves for this name. Returns false if the name can't be a file name. */
    boolean open(String name) {
        String safe = name.toLowerCase().replaceAll("[^a-z0-9_-]", "");
        if (safe.isEmpty()) return false;
        file = folder().resolve(safe + ".txt");
        java.util.Arrays.fill(values, 0);
        try {
            if (Files.exists(file)) {
                String[] lines = Files.readString(file, StandardCharsets.UTF_8).split("\\R");
                for (int i = 0; i < Math.min(SLOTS, lines.length); i++) {
                    try {
                        values[i] = Double.parseDouble(lines[i].trim());
                    } catch (NumberFormatException ignored) {
                        // a damaged line: 0
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Couldn't read the saves in " + file + ": " + e.getMessage());
        }
        return true;
    }

    boolean isOpen() {
        return file != null;
    }

    double get(int slot) {
        return slot >= 0 && slot < SLOTS ? values[slot] : 0;
    }

    /** Sets a slot and writes the file straight away (so nothing is lost if the game is closed suddenly). */
    void set(int slot, double value) {
        if (slot < 0 || slot >= SLOTS || file == null) return;
        if (values[slot] == value) return;
        values[slot] = value;
        StringBuilder out = new StringBuilder();
        for (double v : values) out.append(v == Math.floor(v) && Math.abs(v) < 1e15 ? String.valueOf((long) v) : String.valueOf(v)).append('\n');
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Couldn't save to " + file + ": " + e.getMessage());
        }
    }
}
