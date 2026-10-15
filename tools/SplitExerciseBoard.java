import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/** Splits a generated 3 by 2 coach board into Android drawable PNGs. */
public final class SplitExerciseBoard {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("image outputDir topNames bottomNames");
        BufferedImage source = ImageIO.read(new File(args[0]));
        File output = new File(args[1]);
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create " + output);
        String[] top = args[2].split(",");
        String[] bottom = args[3].split(",");
        if (top.length != 3 || bottom.length != 3) throw new IllegalArgumentException("Need three top and three bottom names");
        int cellWidth = source.getWidth() / 3;
        int cellHeight = source.getHeight() / 2;
        for (int row = 0; row < 2; row++) for (int col = 0; col < 3; col++) {
            int trim = 5;
            BufferedImage cell = source.getSubimage(col * cellWidth + trim, row * cellHeight + trim,
                    cellWidth - trim * 2, cellHeight - trim * 2);
            BufferedImage scaled = new BufferedImage(360, 240, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(cell, 0, 0, 360, 240, null);
            g.dispose();
            String name = (row == 0 ? "mia_" + top[col] : "leo_" + bottom[col]) + ".png";
            ImageIO.write(scaled, "png", new File(output, name));
        }
    }
}
