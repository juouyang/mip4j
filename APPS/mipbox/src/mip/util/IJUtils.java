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

    private static Image3DUniverse UNIV;

    private static Image3DUniverse get3DUniv() {
        if (UNIV == null) {
            UNIV = new Image3DUniverse();
        }
        return UNIV;
    }

    public static ImageJ openImageJ() {
        ImageJ ij = IJ.getInstance() == null ? new ImageJ() : IJ.getInstance();
        ij.exitWhenQuitting(true);
        return ij;
    }

    public static void render(ImagePlus i) {
        render(i, 2, 0, 0);
    }

    public static void render(ImagePlus i, int resample, int trans, int threshold) {
        Timer t = new Timer();

        Image3DUniverse univ = get3DUniv();
        univ.removeAllContents();
        final ImagePlus imp = i.duplicate();
        new StackConverter(imp).convertToGray8();

        ContentInstant ci = univ.addVoltex(imp, resample).getCurrent();

        if (ci == null) {
            return;
        }

        assert (trans >= 0 && trans <= 100);
        assert (threshold >= 0 && threshold <= 255);

        ci.setTransparency(trans / 100f);
        ci.setThreshold(threshold);

        if (univ.getWindow() == null) {
            univ.show();
            final ImageWindow3D iw = univ.getWindow();
            iw.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent we) {
                    univ.removeAllContents();
                    new Thread(() -> {
                        univ.cleanup();
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

    public static ByteProcessor toByteProcessor(BitImage bi) {
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

    public static ByteProcessor toByteProcessor(ShortImage si, int wc, int ww) {
        ByteProcessor ip = new ByteProcessor(si.getWidth(), si.getHeight());

        int srcMin = wc - (ww / 2);
        int srcMax = wc + (ww / 2);
        int dstMin = 0;
        int dstMax = 255;
        float ratio = (float) (dstMax - dstMin) / (srcMax - srcMin);

        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                int v = si.getPixel(x, y);

                if (v < srcMin) {
                    v = dstMin;
                } else if (v > srcMax) {
                    v = dstMax;
                } else {
                    v = ((int) ((v - srcMin) * ratio));
                }

                ip.putPixel(x, y, v);
            }
        }

        return ip;
    }

    public static ColorProcessor toColorProcessor(ColorImage ci) {
        ColorProcessor ip = new ColorProcessor(ci.getWidth(), ci.getHeight());

        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                int r = ci.getPixel(x, y).R;
                int g = ci.getPixel(x, y).G;
                int b = ci.getPixel(x, y).B;
                ip.putPixel(x, y,
                        ((r << 16) & 0x00FF0000)
                        | ((g << 8) & 0x0000FF00)
                        | (b & 0x000000FF)
                );
            }
        }

        return ip;
    }

    public static ShortProcessor toShortProcessor(ShortImage si) {
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

    public static ImageStack toImageStack(ShortImage[] imgs) {
        final int w = imgs[0].getWidth();
        final int h = imgs[0].getHeight();
        ImageStack ims = new ImageStack(w, h);

        for (ShortImage si : imgs) {
            ims.addSlice(IJUtils.toShortProcessor(si));
        }

        return ims;
    }

    public static ImageStack toImageStack(ShortImage[] imgs, int wc, int ww) {
        final int w = imgs[0].getWidth();
        final int h = imgs[0].getHeight();
        ImageStack ims = new ImageStack(w, h);

        for (ShortImage si : imgs) {
            ims.addSlice(IJUtils.toByteProcessor(si, wc, ww));
        }

        return ims;
    }

    public static ImageStack toImageStack(BitImage[] imgs) {
        final int w = imgs[0].getWidth();
        final int h = imgs[0].getHeight();
        ImageStack ims = new ImageStack(w, h);

        for (BitImage bi : imgs) {
            ims.addSlice(IJUtils.toByteProcessor(bi));
        }

        return ims;
    }

    private IJUtils() { // singleton
    }
}
