package mip.data.image;

import ij.ImagePlus;
import java.io.File;
import mip.data.image.mr.BMRStudy;
import mip.data.image.mr.MR;
import mip.data.image.mr.MROpener;
import mip.util.IJUtils;
import mip.view.swing.AbstractImagePanel;
import mip.view.swing.ShortImageFrame;

public class ShortImage extends AbstractImage {

    public static void main(String[] args) throws Throwable {
        File f = new File(BMRStudy.SBMR + "2/080.dcm");
        MR mr = MROpener.openMR(f.toPath());
        mr.show();
        ShortImage si = new ShortImage(512, 512, new short[512 * 512]);
        for (int y = 0; y < si.getHeight(); y++) {
            for (int x = 0; x < si.getWidth(); x++) {
                si.setPixel(x, y, 4000 - mr.getPixel(x, y));
            }
        }
        si.show();
    }

    protected final short[] pixelArray;
    protected short max = Short.MIN_VALUE;
    protected short min = Short.MAX_VALUE;
    protected int windowCenter = Integer.MIN_VALUE;
    protected int windowWidth = Integer.MIN_VALUE;

    public ShortImage(int w, int h, short[] pixels) {
        super(w, h);
        pixelArray = pixels;
    }

    @Override
    public void show() {
        new ShortImageFrame(this).setVisible(true);
    }

    @Override
    protected ImagePlus toImagePlus(String title) {
        return new ImagePlus(title, IJUtils.toShortProcessor(this));
    }

    protected void setPixel(int x, int y, int v) {
        pixelArray[(y * width) + x] = (short) v;

        if (v > max) {
            max = (short) v;
        }
        if (v < min) {
            min = (short) v;
        }
    }

    public short getPixel(int x, int y) {
        if (x >= width || x < 0 || y >= height || y < 0) {
            return Short.MIN_VALUE;
        }

        return pixelArray[(y * width) + x];
    }

    public short[] getPixelArray(AbstractImagePanel.VIEW_ACCESS_TOKEN token) {
        token.hashCode();
        return pixelArray;
    }

    public short getMax() {
        if (max == Short.MIN_VALUE) {
            for (short v : pixelArray) {
                if (v > max) {
                    max = v;
                }
            }
        }
        return max;
    }

    public short getMin() {
        if (min == Short.MAX_VALUE) {
            for (short v : pixelArray) {
                if (v < min) {
                    min = v;
                }
            }
        }
        return min;
    }

    public int getWindowCenter() {
        if (windowCenter == Integer.MIN_VALUE) {
            windowCenter = (getMax() - getMin()) / 2 + getMin();
        }
        return windowCenter;
    }

    public int getWindowWidth() {
        if (windowWidth == Integer.MIN_VALUE) {
            windowWidth = getMax() - getMin();
        }
        return windowWidth;
    }

}
