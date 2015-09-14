package mip.model.data.image.base;

public abstract class AbstractImage {

    protected int width;
    protected int height;

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
}
