package mip.data.image;

import ij.ImagePlus;
import java.awt.Color;
import mip.data.Component;
import mip.util.ImageJUtils;
import mip.view.swing.AbstractImagePanel;
import mip.view.swing.ColorImageFrame;

public class ColorImage extends AbstractImage {

    public static class RGB {

        public short R;
        public short G;
        public short B;

        @Override
        public String toString() {
            return String.format("(%04d,%04d,%04d)", R, G, B);
        }
    }

    RGB[] pixelArray = new RGB[1];

    public ColorImage(int width, int height) {
        this.width = width;
        this.height = height;
        pixelArray = new RGB[width * height];

        for (int i = 0; i < pixelArray.length; i++) {
            pixelArray[i] = new RGB();
        }
    }

    public ColorImage(int width, int height, RGB[] pixels) {
        this.width = width;
        this.height = height;
        pixelArray = pixels;
    }

    @Override
    public void show() {
        new ColorImageFrame(this).setVisible(true);
    }

    public static void main(String[] args) {
        ColorImage ci = new ColorImage(512, 512);
        for (int y = 0; y < ci.getHeight(); y++) {
            for (int x = 0; x < ci.getWidth(); x++) {
                ci.setPixel(x, y, Component.getRandomColor());
            }
        }
        ci.show();
        ci.getImagePlus("").show();
    }

    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    @Override
    protected ImagePlus convertImageToImagePlus(String title) {
        return new ImagePlus(title, ImageJUtils.getColorProcessorFromColorImage(this));
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
    //</editor-fold>

}
