import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.AlphaComposite;
import java.awt.Color;

/** Converts a generated 3-by-4 motion sheet into four looping Android GIF resources. */
public final class BuildExerciseGifs {
    private static final int FRAME_W = 360, FRAME_H = 480, TRIM = 4;

    public static void main(String[] args) throws Exception {
        if (args.length == 1) { smoothExisting(new File(args[0])); return; }
        if (args.length != 4 && args.length != 5) {
            throw new IllegalArgumentException("sheet outputDir exerciseA exerciseB [framesPerExercise]");
        }
        BufferedImage sheet = ImageIO.read(new File(args[0]));
        File output = new File(args[1]);
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create " + output);
        int framesPerExercise = args.length == 5 ? Integer.parseInt(args[4]) : 3;
        if (framesPerExercise < 3) throw new IllegalArgumentException("framesPerExercise must be at least 3");
        int cellW = sheet.getWidth() / framesPerExercise, cellH = sheet.getHeight() / 4;
        write(sheet, cellW, cellH, framesPerExercise, 0, new File(output, "mia_" + args[2] + ".gif"));
        write(sheet, cellW, cellH, framesPerExercise, 1, new File(output, "mia_" + args[3] + ".gif"));
        write(sheet, cellW, cellH, framesPerExercise, 2, new File(output, "leo_" + args[2] + ".gif"));
        write(sheet, cellW, cellH, framesPerExercise, 3, new File(output, "leo_" + args[3] + ".gif"));
    }

    /**
     * Re-encodes the offline coach animations as a short ping-pong loop with
     * blended in-between frames. This eliminates the abrupt last-frame-to-first-frame jump
     * while preserving the existing generated trainer imagery.
     */
    private static void smoothExisting(File directory) throws Exception {
        File[] files = directory.listFiles((dir, name) -> name.matches("(mia|leo)_.*\\.gif"));
        if (files == null || files.length == 0) throw new IllegalArgumentException("No coach GIFs found in " + directory);
        for (File file : files) {
            ArrayList<BufferedImage> frames = readFrames(file);
            if (frames.size() < 2) continue;
            ArrayList<BufferedImage> loop = new ArrayList<>();
            for (int i = 0; i < frames.size() - 1; i++) { loop.add(frames.get(i)); loop.add(blend(frames.get(i), frames.get(i + 1), .25f)); loop.add(blend(frames.get(i), frames.get(i + 1), .5f)); loop.add(blend(frames.get(i), frames.get(i + 1), .75f)); }
            loop.add(frames.get(frames.size() - 1));
            for (int i = frames.size() - 1; i > 0; i--) { loop.add(blend(frames.get(i), frames.get(i - 1), .75f));loop.add(blend(frames.get(i), frames.get(i - 1), .5f));loop.add(blend(frames.get(i), frames.get(i - 1), .25f)); }
            File temporary = new File(directory, file.getName() + ".smooth");
            try (ImageOutputStream stream = new FileImageOutputStream(temporary); GifWriter gif = new GifWriter(stream, 70, true)) { for (BufferedImage frame : loop) gif.write(frame); }
            if (!file.delete() || !temporary.renameTo(file)) throw new IllegalStateException("Cannot replace " + file);
        }
    }

    private static ArrayList<BufferedImage> readFrames(File file) throws Exception {
        Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix("gif");
        ImageReader reader = readers.next(); ArrayList<BufferedImage> result = new ArrayList<>();
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) { reader.setInput(input); for (int i = 0; i < reader.getNumImages(true); i++) result.add(reader.read(i)); }
        finally { reader.dispose(); }
        return result;
    }
    private static BufferedImage blend(BufferedImage from, BufferedImage to, float amount) {
        int width = Math.max(from.getWidth(), to.getWidth()), height = Math.max(from.getHeight(), to.getHeight());
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); Graphics2D graphics = output.createGraphics(); graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, width, height);
        graphics.setComposite(AlphaComposite.SrcOver.derive(1f - amount)); graphics.drawImage(from, 0, 0, null); graphics.setComposite(AlphaComposite.SrcOver.derive(amount)); graphics.drawImage(to, 0, 0, null); graphics.dispose(); return output;
    }

    private static void write(BufferedImage sheet, int cellW, int cellH, int framesPerExercise, int row, File out) throws Exception {
        try (ImageOutputStream stream = new FileImageOutputStream(out); GifWriter gif = new GifWriter(stream, 180, true)) {
            for (int col = 0; col < framesPerExercise; col++) gif.write(frame(sheet, cellW, cellH, row, col));
            for (int col = framesPerExercise - 2; col > 0; col--) gif.write(frame(sheet, cellW, cellH, row, col));
        }
    }

    private static BufferedImage frame(BufferedImage sheet, int cellW, int cellH, int row, int column) {
        BufferedImage cell = sheet.getSubimage(column * cellW + TRIM, row * cellH + TRIM, cellW - TRIM * 2, cellH - TRIM * 2);
        BufferedImage frame = new BufferedImage(FRAME_W, FRAME_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = frame.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(cell, 0, 0, FRAME_W, FRAME_H, null);
        graphics.dispose();
        return frame;
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
