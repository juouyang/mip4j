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

    public abstract void show();

    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    protected abstract ImagePlus convertImageToImagePlus(String title);

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public final ImagePlus getImagePlus(String title) {
        if (ips == null) {
            ips = convertImageToImagePlus(title);
        }
        return ips;
    }
    //</editor-fold>

}
