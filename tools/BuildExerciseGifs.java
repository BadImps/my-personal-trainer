import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/** Converts a generated 3-by-4 motion sheet into four looping Android GIF resources. */
public final class BuildExerciseGifs {
    private static final int FRAME_W = 180, FRAME_H = 240, TRIM = 4;

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("sheet outputDir exerciseA exerciseB");
        BufferedImage sheet = ImageIO.read(new File(args[0]));
        File output = new File(args[1]);
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create " + output);
        int cellW = sheet.getWidth() / 3, cellH = sheet.getHeight() / 4;
        write(sheet, cellW, cellH, 0, new File(output, "mia_" + args[2] + ".gif"));
        write(sheet, cellW, cellH, 1, new File(output, "mia_" + args[3] + ".gif"));
        write(sheet, cellW, cellH, 2, new File(output, "leo_" + args[2] + ".gif"));
        write(sheet, cellW, cellH, 3, new File(output, "leo_" + args[3] + ".gif"));
    }

    private static void write(BufferedImage sheet, int cellW, int cellH, int row, File out) throws Exception {
        try (ImageOutputStream stream = new FileImageOutputStream(out); GifWriter gif = new GifWriter(stream, 320, true)) {
            for (int col = 0; col < 3; col++) {
                BufferedImage cell = sheet.getSubimage(col * cellW + TRIM, row * cellH + TRIM, cellW - TRIM * 2, cellH - TRIM * 2);
                BufferedImage frame = new BufferedImage(FRAME_W, FRAME_H, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = frame.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(cell, 0, 0, FRAME_W, FRAME_H, null);
                graphics.dispose();
                gif.write(frame);
            }
        }
    }

    private static final class GifWriter implements AutoCloseable {
        private final ImageWriter writer;
        private final ImageWriteParam params;
        private final IIOMetadata metadata;

        GifWriter(ImageOutputStream stream, int delayMs, boolean loop) throws Exception {
            writer = ImageIO.getImageWritersBySuffix("gif").next();
            params = writer.getDefaultWriteParam();
            metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB), params);
            String format = metadata.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            IIOMetadataNode control = child(root, "GraphicControlExtension");
            control.setAttribute("disposalMethod", "none"); control.setAttribute("userInputFlag", "FALSE");
            control.setAttribute("transparentColorFlag", "FALSE"); control.setAttribute("delayTime", Integer.toString(delayMs / 10)); control.setAttribute("transparentColorIndex", "0");
            if (loop) { IIOMetadataNode apps = child(root, "ApplicationExtensions"); IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension"); app.setAttribute("applicationID", "NETSCAPE"); app.setAttribute("authenticationCode", "2.0"); app.setUserObject(new byte[]{1, 0, 0}); apps.appendChild(app); }
            metadata.setFromTree(format, root); writer.setOutput(stream); writer.prepareWriteSequence(null);
        }

        void write(BufferedImage image) throws Exception { writer.writeToSequence(new IIOImage(image, null, metadata), params); }
        private static IIOMetadataNode child(IIOMetadataNode root, String name) { for (int i = 0; i < root.getLength(); i++) if (root.item(i).getNodeName().equals(name)) return (IIOMetadataNode) root.item(i); IIOMetadataNode node = new IIOMetadataNode(name); root.appendChild(node); return node; }
        @Override public void close() throws Exception { writer.endWriteSequence(); writer.dispose(); }
    }
}
