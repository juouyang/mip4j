package mip.data.image;

import ij.ImagePlus;
import java.awt.Color;
import java.io.IOException;
import mip.data.image.mr.MR;
import mip.data.image.mr.MROpener;
import mip.util.IJUtils;
import mip.view.swing.AbstractImagePanel;
import mip.view.swing.ColorImageFrame;

public class ColorImage extends AbstractImage {

    public static void main(String[] args) throws IOException {
        MR mr = MROpener.openMR();
        ColorImage ci = new ColorImage(512, 512);
        int i = 0;
        for (Short s : mr.pixelArray) {
            ci.pixelArray[i++].G = (short) ((s > 1500) ? 255 : 0);
        }
        ci.show();
    }

    private final RGB[] pixelArray;

    public ColorImage(int width, int height) {
        super(width, height);
        pixelArray = new RGB[width * height];

        for (int i = 0; i < pixelArray.length; i++) {
            pixelArray[i] = new RGB();
        }
    }

    public ColorImage(int width, int height, RGB[] pixels) {
        super(width, height);
        pixelArray = pixels;
    }

    @Override
    public void show() {
        new ColorImageFrame(this).setVisible(true);
    }

    @Override
    protected ImagePlus toImagePlus(String title) {
        return new ImagePlus(title, IJUtils.toColorProcessor(this));
    }

    public void setPixel(int x, int y, int r, int g, int b) {
        int i = (y * width) + x;

        pixelArray[i].R = (short) r;
        pixelArray[i].G = (short) g;
        pixelArray[i].B = (short) b;
    }

    public void setPixel(int x, int y, Color v) {
        int i = (y * width) + x;

        pixelArray[i].R = (short) v.getRed();
        pixelArray[i].G = (short) v.getGreen();
        pixelArray[i].B = (short) v.getBlue();
    }

    public RGB getPixel(int x, int y) {
        return pixelArray[(y * width) + x];
    }

    public RGB[] getPixelArray(AbstractImagePanel.VIEW_ACCESS_TOKEN token) {
        token.hashCode();
        return pixelArray;
    }

}
