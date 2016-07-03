package mip.data.image;

import ij.ImagePlus;

public abstract class AbstractImage {

    protected int width;
    protected int height;
    protected ImagePlus ips;

    public AbstractImage() {
        width = 1;
        height = 1;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    protected abstract ImagePlus _getImagePlus(String title);

    public final ImagePlus getImagePlus(String title) {
        if (ips == null) {
            ips = _getImagePlus(title);
        }
        return ips;
    }
}
