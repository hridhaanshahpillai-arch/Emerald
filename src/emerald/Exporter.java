package emerald;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * EXPORT: turns the cart into a game anyone can play, in exports/NAME/:
 *
 *   NAME/NAME.exe   an app with Java inside (made with jpackage, when Emerald runs on a full JDK)
 *   play.bat        the same game for PCs that have Java (always made)
 *
 * Exported games only play: no command line, no editor, and Esc closes them.
 */
final class Exporter {

    private Exporter() { }

    /** Exports in the background; 'say' gets progress messages (on the exporting thread). */
    static void export(Cart cart, String name, Path exports, Consumer<String> say) {
        Thread worker = new Thread(() -> {
            try {
                Path folder = exports.resolve(name);
                Path files = folder.resolve("files");
                Files.createDirectories(files);
                Path jar = Path.of(Exporter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (!Files.isRegularFile(jar)) throw new IOException("Emerald isn't running from emerald.jar (run build.ps1 first)");
                Files.copy(jar, files.resolve("emerald.jar"), StandardCopyOption.REPLACE_EXISTING);
                cart.write(files.resolve(name + ".emerald"));
                Files.writeString(folder.resolve("play.bat"), "@echo off\r\ncd /d \"%~dp0files\"\r\nstart \"\" javaw -jar emerald.jar " + name + ".emerald --player\r\n");
                say.accept("MADE PLAY.BAT (NEEDS JAVA)");

                Path jpackage = Path.of(System.getProperty("java.home"), "bin", windows() ? "jpackage.exe" : "jpackage");
                if (!Files.isRegularFile(jpackage)) {
                    say.accept("NO JPACKAGE HERE: ONLY PLAY.BAT");
                    return;
                }
                say.accept("MAKING THE APP (ABOUT A MINUTE)...");
                Path app = folder.resolve(name);
                if (Files.exists(app)) deleteTree(app);
                List<String> command = new ArrayList<>(List.of(jpackage.toString(), "--type", "app-image", "--name", name,
                        "--input", files.toString(), "--main-jar", "emerald.jar", "--main-class", "emerald.Emerald",
                        "--arguments", name + ".emerald", "--arguments", "--player",
                        "--add-modules", "java.base,java.desktop,jdk.unsupported", "--jlink-options", "--strip-debug --no-man-pages --no-header-files",
                        "--dest", folder.toString()));
                Path icon = jar.getParent() == null ? null : jar.getParent().getParent().resolve("emerald.ico");
                if (windows() && icon != null && Files.isRegularFile(icon)) command.addAll(List.of("--icon", icon.toString()));
                Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                String output = new String(process.getInputStream().readAllBytes());
                if (process.waitFor() != 0) {
                    System.out.println(output);
                    say.accept("THE APP DIDN'T WORK OUT: USE PLAY.BAT");
                    return;
                }
                say.accept("DONE! EXPORTS/" + name.toUpperCase() + "/" + name.toUpperCase() + (windows() ? "/" + name.toUpperCase() + ".EXE" : ""));
            } catch (Exception e) {
                System.out.println("Export failed: " + e);
                say.accept("EXPORT FAILED: " + String.valueOf(e.getMessage()).toUpperCase());
            }
        }, "Emerald export");
        worker.setDaemon(false);
        worker.start();
    }

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void deleteTree(Path folder) throws IOException {
        try (var walk = Files.walk(folder)) {
            for (Path p : walk.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
