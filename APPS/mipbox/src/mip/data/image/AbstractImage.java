package mip.data.image;

import ij.ImagePlus;

public abstract class AbstractImage {

    protected final int width;
    protected final int height;

    public AbstractImage(int w, int h) {
        width = w;
        height = h;
    }

    public abstract void show();

    protected abstract ImagePlus toImagePlus(String title);

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
