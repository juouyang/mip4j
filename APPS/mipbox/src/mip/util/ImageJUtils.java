package mip.util;

import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import mip.model.data.image.ShortImage;

public class ImageJUtils {

    private ImageJUtils() { // singleton
    }

    public static ShortProcessor getShortProcessorFromShortImage(ShortImage si) {
        ShortProcessor sp = new ShortProcessor(si.getWidth(), si.getHeight());

        short imgMin = si.getMin();
        short imgMax = si.getMax();
        int offset = 0;

        if (imgMin < 0) {
            offset = imgMax - imgMin;
        }

        for (int y = 0; y < sp.getHeight(); y++) {
            for (int x = 0; x < sp.getWidth(); x++) {
                sp.putPixel(x, y, si.getPixel(x, y) + offset);
            }
        }

        return sp;
    }

    public static ByteProcessor getByteProcessorFromShortImage(ShortImage si, int windowCenter, int windowWidth) {
        ByteProcessor bp = new ByteProcessor(si.getWidth(), si.getHeight());

        int imgMin = windowCenter - (windowWidth / 2);
        int imgMax = windowCenter + (windowWidth / 2);
        int displayMin = 0;
        int displayMax = 255;
        float displayRatio = (float) (displayMax - displayMin) / (imgMax - imgMin);

        for (int y = 0; y < bp.getHeight(); y++) {
            for (int x = 0; x < bp.getWidth(); x++) {
                int v = si.getPixel(x, y);

                if (v < imgMin) {
                    v = displayMin;
                } else if (v > imgMax) {
                    v = displayMax;
                } else {
                    v = ((int) ((v - imgMin) * displayRatio));
                }

                bp.putPixel(x, y, v);
            }
        }

        return bp;
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
}
