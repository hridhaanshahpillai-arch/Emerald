package emerald;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Screenshots (F6) and GIF clips (F8 to start, F8 again to stop, 20 seconds at most), saved
 * 4 times bigger with sharp pixels in the screenshots folder, ready to share.
 */
final class Recorder {

    static final int SCALE = 4;
    private static final int GIF_EVERY = 2;            // every other frame: 30 a second
    private static final int GIF_MAX = 20 * 60 / GIF_EVERY;

    private final Path folder;
    private List<byte[]> gif;                          // the clip's frames (colours as shown), null when not recording
    private int[] gifPalette;
    private int frameCount;
    String message = "";                               // what to show for a moment ("SAVED ...")
    int messageTime;

    Recorder(Path folder) {
        this.folder = folder;
    }

    boolean recording() {
        return gif != null;
    }

    /** Saves the screen as it looks now. Returns the file's name. */
    String screenshot(Screen screen, String name) {
        BufferedImage image = new BufferedImage(screen.width * SCALE, Screen.SIZE * SCALE, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                image.setRGB(x, y, screen.shownRgb(x / SCALE, y / SCALE));
        try {
            Path file = nextFile(name, "png");
            ImageIO.write(image, "png", file.toFile());
            say("SCREENSHOT SAVED: " + file.getFileName().toString().toUpperCase());
            return file.getFileName().toString();
        } catch (IOException e) {
            say("COULDN'T SAVE THE SCREENSHOT");
            return null;
        }
    }

    void startGif() {
        gif = new ArrayList<>();
        frameCount = 0;
        say("RECORDING... F8 TO STOP");
    }

    /** Called every frame while recording: keeps every other frame. */
    void capture(Screen screen) {
        if (gif == null) return;
        if (frameCount++ % GIF_EVERY != 0) return;
        byte[] frame = new byte[screen.width * Screen.SIZE];
        gifPalette = new int[16];
        for (int c = 0; c < 16; c++) gifPalette[c] = Screen.PALETTE[screen.pen.show[c]];
        System.arraycopy(screen.pixels, 0, frame, 0, frame.length);
        gif.add(frame);
        if (gif.size() >= GIF_MAX) stopGif(null);
    }

    /** Stops recording and writes the GIF (in the background, so the game doesn't stutter). */
    void stopGif(String name) {
        if (gif == null) return;
        List<byte[]> frames = gif;
        int[] palette = gifPalette;
        gif = null;
        if (frames.isEmpty()) return;
        Path file;
        try {
            file = nextFile(name == null ? "clip" : name, "gif");
        } catch (IOException e) {
            say("COULDN'T SAVE THE GIF");
            return;
        }
        say("SAVING GIF...");
        Thread writer = new Thread(() -> {
            try {
                writeGif(frames, palette, file);
                say("GIF SAVED: " + file.getFileName().toString().toUpperCase());
            } catch (IOException e) {
                say("COULDN'T SAVE THE GIF");
            }
        }, "GIF writer");
        writer.setDaemon(false); // finish writing even if Emerald is closing
        writer.start();
    }

    private void say(String text) {
        message = text;
        messageTime = 120;
    }

    private Path nextFile(String name, String extension) throws IOException {
        Files.createDirectories(folder);
        String base = name.toLowerCase().replaceAll("[^a-z0-9_-]", "");
        if (base.isEmpty()) base = "emerald";
        for (int n = 1; ; n++) {
            Path file = folder.resolve(base + "-" + n + "." + extension);
            if (!Files.exists(file)) return file;
        }
    }

    /** An animated GIF that loops, 30 frames a second, each pixel 4 x 4. */
    private static void writeGif(List<byte[]> frames, int[] palette, Path file) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        byte[] r = new byte[16], g = new byte[16], b = new byte[16];
        for (int i = 0; i < 16; i++) {
            r[i] = (byte) (palette[i] >> 16);
            g[i] = (byte) (palette[i] >> 8);
            b[i] = (byte) palette[i];
        }
        java.awt.image.IndexColorModel colours = new java.awt.image.IndexColorModel(4, 16, r, g, b);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file.toFile())) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            int size = Screen.SIZE * SCALE;
            for (int i = 0; i < frames.size(); i++) {
                byte[] frame = frames.get(i);
                int w = frame.length / Screen.SIZE, sizeX = w * SCALE;
                BufferedImage image = new BufferedImage(sizeX, size, BufferedImage.TYPE_BYTE_INDEXED, colours);
                java.awt.image.WritableRaster raster = image.getRaster();
                int[] row = new int[sizeX];
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < sizeX; x++) row[x] = frame[(y / SCALE) * w + x / SCALE] & 15;
                    raster.setPixels(0, y, sizeX, 1, row);
                }
                ImageWriteParam param = writer.getDefaultWriteParam();
                IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), param);
                String format = meta.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);
                IIOMetadataNode control = child(root, "GraphicControlExtension");
                control.setAttribute("disposalMethod", "none");
                control.setAttribute("userInputFlag", "FALSE");
                control.setAttribute("transparentColorFlag", "FALSE");
                control.setAttribute("delayTime", i % 3 == 2 ? "4" : "3"); // 3, 3, 4 hundredths: 30 a second on average
                control.setAttribute("transparentColorIndex", "0");
                if (i == 0) {
                    IIOMetadataNode apps = child(root, "ApplicationExtensions");
                    IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID", "NETSCAPE");
                    loop.setAttribute("authenticationCode", "2.0");
                    loop.setUserObject(new byte[] {1, 0, 0}); // loop forever
                    apps.appendChild(loop);
                }
                meta.setFromTree(format, root);
                writer.writeToSequence(new IIOImage(image, null, meta), param);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) return (IIOMetadataNode) root.item(i);
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
