package emerald;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Carts as pictures: a little green cartridge with a screenshot of the game as its label and the
 * cart's name underneath, and the whole cart (code, sprites, map, sounds, music) hidden inside the
 * PNG as compressed text. Share it anywhere a picture goes; LOAD NAME.PNG plays it.
 */
final class PngCart {

    private PngCart() { }

    static final String KEY = "emerald-cart";
    private static final int WIDTH = 160, HEIGHT = 196, SCALE = 2;

    static void write(Cart cart, Path file) throws IOException {
        BufferedImage image = picture(cart, file);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file.toFile())) {
            writer.setOutput(out);
            IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);
            IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
            IIOMetadataNode zText = new IIOMetadataNode("zTXt");
            IIOMetadataNode entry = new IIOMetadataNode("zTXtEntry");
            entry.setAttribute("keyword", KEY);
            entry.setAttribute("compressionMethod", "deflate");
            entry.setAttribute("text", cart.text());
            zText.appendChild(entry);
            root.appendChild(zText);
            meta.mergeTree("javax_imageio_png_1.0", root);
            writer.write(null, new IIOImage(image, null, meta), null);
        } finally {
            writer.dispose();
        }
    }

    static Cart read(Path file) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(file.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) throw new IOException("it isn't a picture");
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                IIOMetadata meta = reader.getImageMetadata(0);
                String text = find(meta.getAsTree("javax_imageio_png_1.0"));
                if (text == null) throw new IOException("it's a picture, but not an Emerald cart");
                return Cart.fromText(text);
            } finally {
                reader.dispose();
            }
        }
    }

    /** The cart text inside the PNG's zTXt (or tEXt / iTXt) chunks, or null. */
    private static String find(Node node) {
        String name = node.getNodeName();
        if (name.equals("zTXtEntry") || name.equals("tEXtEntry") || name.equals("iTXtEntry")) {
            NamedNodeMap attributes = node.getAttributes();
            Node keyword = attributes.getNamedItem("keyword");
            Node value = attributes.getNamedItem(name.equals("iTXtEntry") ? "text" : name.equals("zTXtEntry") ? "text" : "value");
            if (keyword != null && KEY.equals(keyword.getNodeValue()) && value != null) return value.getNodeValue();
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            String found = find(child);
            if (found != null) return found;
        }
        return null;
    }

    /** The cartridge: deep green with a lighter edge, the label (the game's picture), and its name. */
    private static BufferedImage picture(Cart cart, Path file) {
        // drawn on a small Emerald-sized canvas first, then doubled
        int w = WIDTH / SCALE, h = HEIGHT / SCALE;
        int[] px = new int[w * h];
        int deep = Screen.PALETTE[3], bright = Screen.PALETTE[11], ink = Screen.PALETTE[0];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean corner = (x < 2 || x >= w - 2) && (y < 2 || y >= h - 2); // rounded corners: see-through
                boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1 || (x < 2 || x >= w - 2) && (y < 2 || y >= h - 2);
                px[y * w + x] = corner && edge && (x == 0 || x == w - 1) && (y == 0 || y == h - 1) ? 0 : edge ? bright : deep;
            }
        }
        // the label: the game's last picture, 64 x 64 (every other pixel), framed in ink
        int lx = (w - 64) / 2, ly = 8;
        for (int y = -1; y <= 64; y++) for (int x = -1; x <= 64; x++) px[(ly + y) * w + lx + x] = ink;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                px[(ly + y) * w + lx + x] = cart.label != null ? cart.label[(y * 2) * Screen.SIZE + x * 2] : (((x / 8 + y / 8) % 2 == 0) ? Screen.PALETTE[1] : ink);
            }
        }
        // the name, in the pixel font, under the label
        String name = file.getFileName().toString();
        name = name.substring(0, name.length() - 4).toUpperCase();
        if (name.length() > 18) name = name.substring(0, 18);
        Screen text = new Screen();
        text.cls(0);
        PixelFont font = Emerald.font();
        if (font != null) {
            font.draw(text, name, 0, 0, 7);
            font.draw(text, "EMERALD", 0, 8, 11);
        }
        int nameWidth = name.length() * PixelFont.ADVANCE - 1, brandWidth = 7 * PixelFont.ADVANCE - 1;
        for (int y = 0; y < 14; y++) {
            for (int x = 0; x < Math.max(nameWidth, brandWidth); x++) {
                int c = text.pget(x, y);
                if (c == 0) continue;
                int at = y < 8 ? (w - nameWidth) / 2 : (w - brandWidth) / 2;
                px[(ly + 68 + y) * w + at + x] = Screen.PALETTE[c];
            }
        }
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int c = px[(y / SCALE) * w + x / SCALE];
                image.setRGB(x, y, c == 0 ? 0 : 0xFF000000 | c);
            }
        }
        return image;
    }
}
