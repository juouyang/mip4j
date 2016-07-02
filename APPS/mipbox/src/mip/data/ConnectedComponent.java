package mip.data;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectedComponent {

    private final long id;
    private Color c;
    private Rectangle bounding;
    private int areaSize;
    private int maxX;
    private int maxY;
    private int minX;
    private int minY;

    public ConnectedComponent() {
        id = UniqueID.getUniqueID();
        c = getRandomColor();
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;
    }

    public ConnectedComponent(long id, int minX, int minY, int maxX, int maxY) {
        this.id = id;
        this.c = getRandomColor();
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public void setAreaSize(int areaSize) {
        this.areaSize = areaSize;
    }

    public int getAreaSize() {
        return this.areaSize;
    }

    public Rectangle getBoundingRectagle() {
        if (this.bounding == null) {
            this.bounding = new Rectangle(this.minX, this.minY, this.maxX - this.minX + 1, this.maxY - this.minY + 1);
        }

        return this.bounding;
    }

    public void setColor(Color c) {
        this.c = c;
    }

    public Color getColor() {
        return this.c;
    }

    public long getID() {
        return this.id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int a = getBoundingRectagle().width * getBoundingRectagle().height;

        sb.append("[id = ").append(this.id).append("] ");
        sb.append("[size = ").append(getAreaSize()).append("]");
        sb.append("[ratio = ").append((int) ((double) getAreaSize() / a * 100.0)).append("%]");

        return sb.toString();
    }

    private final static Random RANDOM = new Random(System.currentTimeMillis());
    private final static int PALETTESIZE = 64;
    private static Color[] COLORPALETTE = null;

    public static Color getRandomColor() {
        if (COLORPALETTE == null) {
            constructColorPalette();
        }

        return COLORPALETTE[RANDOM.nextInt(PALETTESIZE)];
    }

    private static void constructColorPalette() {
        COLORPALETTE = new Color[PALETTESIZE];

        for (int i = 0; i < COLORPALETTE.length; i++) {
            int r = RANDOM.nextInt(256);
            int g = RANDOM.nextInt(256);
            int b = RANDOM.nextInt(256);
            COLORPALETTE[i] = new Color(r, g, b);
        }
    }
}

class UniqueID {

    private static final AtomicLong CURRENT_ID = new AtomicLong(0);

    private UniqueID() {
        // singleton
    }

    public static long getUniqueID() {
        return CURRENT_ID.getAndIncrement();
    }
}
