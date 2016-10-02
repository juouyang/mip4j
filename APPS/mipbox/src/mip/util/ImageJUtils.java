package mip.util;

import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ShortProcessor;
import mip.data.image.BitImage;
import mip.data.image.ColorImage;
import mip.data.image.ShortImage;

public class ImageJUtils {

    private ImageJUtils() { // singleton
    }

    public static ByteProcessor getByteProcessorFromBitImage(BitImage bi) {
        ByteProcessor ip = new ByteProcessor(bi.getWidth(), bi.getHeight());

        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                int v = 0;

                if (bi.getPixel(x, y)) {
                    v = 255;
                }

                ip.putPixel(x, y, v);
            }
        }

        return ip;
    }

    public static ColorProcessor getColorProcessorFromColorImage(ColorImage ci) {
        ColorProcessor ip = new ColorProcessor(ci.getWidth(), ci.getHeight());

        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                int r = ci.getPixel(x, y).R;
                int g = ci.getPixel(x, y).G;
                int b = ci.getPixel(x, y).B;
                ip.putPixel(x, y, ((r << 16) & 0x00FF0000) | ((g << 8) & 0x0000FF00) | (b
                        & 0x000000FF));
            }
        }

        return ip;
    }

    public static ShortProcessor getShortProcessorFromShortImage(ShortImage si) {
        ShortProcessor ip = new ShortProcessor(si.getWidth(), si.getHeight());

        short imgMin = si.getMin();
        short imgMax = si.getMax();
        int offset = 0;

        if (imgMin < 0) {
            offset = imgMax - imgMin;
        }

        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                ip.putPixel(x, y, si.getPixel(x, y) + offset);
            }
        }

        return ip;
    }

    public static ByteProcessor getByteProcessorFromShortImage(ShortImage si, int windowCenter, int windowWidth) {
        ByteProcessor ip = new ByteProcessor(si.getWidth(), si.getHeight());

        int imgMin = windowCenter - (windowWidth / 2);
        int imgMax = windowCenter + (windowWidth / 2);
        int displayMin = 0;
        int displayMax = 255;
        float displayRatio = (float) (displayMax - displayMin) / (imgMax - imgMin);

        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                int v = si.getPixel(x, y);

                if (v < imgMin) {
                    v = displayMin;
                } else if (v > imgMax) {
                    v = displayMax;
                } else {
                    v = ((int) ((v - imgMin) * displayRatio));
                }

                ip.putPixel(x, y, v);
            }
        }

        return ip;
    }

    public static ImageStack getShortImageStackFromShortImageArray(ShortImage[] imageArray) {
        ImageStack ims = new ImageStack(imageArray[0].getWidth(), imageArray[0].getHeight());

        for (ShortImage si : imageArray) {
            ims.addSlice(ImageJUtils.getShortProcessorFromShortImage(si));
        }

        return ims;
    }

    public static ImageStack getByteImageStackFromShortImageArray(ShortImage[] imageArray) {
        int wc = imageArray[0].getWindowCenter();
        int ww = imageArray[0].getWindowWidth();

        return getByteImageStackFromShortImageArray(imageArray, wc, ww);
    }

    public static ImageStack getByteImageStackFromShortImageArray(ShortImage[] imageArray, int windowCenter, int widowWidth) {
        ImageStack ims = new ImageStack(imageArray[0].getWidth(), imageArray[0].getHeight());

        for (ShortImage si : imageArray) {
            ims.addSlice(ImageJUtils.getByteProcessorFromShortImage(si, windowCenter, widowWidth));
        }

        return ims;
    }

    public static ImageStack getByteImageStackFromBitImageArray(BitImage[] imageArray) {
        ImageStack ims = new ImageStack(imageArray[0].getWidth(), imageArray[0].getHeight());

        for (BitImage bi : imageArray) {
            ims.addSlice(ImageJUtils.getByteProcessorFromBitImage(bi));
        }

        return ims;
    }
}
