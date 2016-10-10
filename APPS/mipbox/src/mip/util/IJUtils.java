package mip.util;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ShortProcessor;
import ij.process.StackConverter;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;
import ij3d.ImageWindow3D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import mip.data.image.BitImage;
import mip.data.image.ColorImage;
import mip.data.image.ShortImage;

public class IJUtils {

    private static final Image3DUniverse UNIV = new Image3DUniverse();

    private IJUtils() { // singleton
    }

    public static ImageJ openImageJ() {
        ImageJ ij = (IJ.getInstance() == null) ? new ImageJ() : IJ.getInstance();
        ij.exitWhenQuitting(true);
        return ij;
    }

    public static void render(ImagePlus i) {
        Timer t = new Timer();

        UNIV.removeAllContents();
        final ImagePlus imp = i.duplicate();
        new StackConverter(imp).convertToGray8();

        ContentInstant ci = UNIV.addVoltex(imp, 1).getCurrent();

        if (ci == null) {
            return;
        }

        if (UNIV.getWindow() == null) {
            UNIV.show();
            final ImageWindow3D iw = UNIV.getWindow();
            iw.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent we) {
                    UNIV.removeAllContents();
                    new Thread(() -> {
                        UNIV.cleanup();
                        System.gc();
                        System.exit(0);
                    }, "Close 3D view thread").start();
                }
            });
        }

        t.printElapsedTime("render");
    }

    public static void exitWhenNoWindow(ImageWindow iw) {
        iw.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent we) {
                System.exit(0);
            }
        });
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

    public static ShortProcessor getProcessorFromShortImage(ShortImage si) {
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

    public static ImageStack getImageStackFromShortImages(ShortImage[] imageArray) {
        ImageStack ims = new ImageStack(imageArray[0].getWidth(), imageArray[0].getHeight());

        for (ShortImage si : imageArray) {
            ims.addSlice(IJUtils.getProcessorFromShortImage(si));
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
            ims.addSlice(IJUtils.getByteProcessorFromShortImage(si, windowCenter, widowWidth));
        }

        return ims;
    }

    public static ImageStack getByteImageStackFromBitImageArray(BitImage[] imageArray) {
        ImageStack ims = new ImageStack(imageArray[0].getWidth(), imageArray[0].getHeight());

        for (BitImage bi : imageArray) {
            ims.addSlice(IJUtils.getByteProcessorFromBitImage(bi));
        }

        return ims;
    }
}
