package mip.data.image;

import ij.ImagePlus;
import mip.data.image.AbstractImage;
import mip.util.ImageJUtils;
import mip.view.swing.ShortImagePanel;

public class ShortImage extends AbstractImage {

    protected short[] pixelArray;
    protected short max = Short.MIN_VALUE;
    protected short min = Short.MAX_VALUE;
    protected int windowCenter = Integer.MIN_VALUE;
    protected int windowWidth = Integer.MIN_VALUE;

    protected ShortImage() {
        pixelArray = new short[1];
    }

    protected ShortImage(int w, int h) {
        width = w;
        height = h;
        pixelArray = new short[w * h];
    }

    public ShortImage(int w, int h, short[] pixels) {
        width = w;
        height = h;
        pixelArray = pixels;
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

    public short[] getPixelArray(ShortImagePanel.VIEW_ACCESS_TOKEN token) {
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

    public ImagePlus toImagePlus(String title) {
        return new ImagePlus(title, ImageJUtils.getShortProcessorFromShortImage(this));
    }
}
