package mip.data.image;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import java.awt.Color;
import java.io.IOException;
import mip.data.image.mr.MR;
import mip.data.image.mr.MROpener;
import mip.view.swing.ConnectedComponentFrame;

/**
 * @author ju Connected Component Labeling
 *
 */
public class ConnectedComponent extends AbstractImage {

    public static void main(String[] args) throws IOException {
        MR mr = MROpener.openMR();
        BitImage bi = new BitImage(mr.width, mr.height);
        int i = 0;
        for (short s : mr.pixelArray) {
            int x = i % bi.width;
            int y = i / bi.width;
            bi.setPixel(x, y, s > 1200);
            i++;
        }
        ConnectedComponent cc = new ConnectedComponent(bi);
        cc.show();
    }

    private ColorImage ci;
    private final long[] pixelArray;
    private TLongObjectMap<Component> componentHashTable;
    private Component background;

    public ConnectedComponent(BitImage bi) {
        super(bi.width, bi.height);
        pixelArray = new long[width * height];

        connectedComponentLabeling(bi);
        getColorImage(false);
    }

    public final ColorImage getColorImage(boolean renew) {
        if (ci == null || renew) {
            ci = new ColorImage(width, height);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Component c = componentHashTable.get(getPixel(x, y));

                    if (c.getID() == background.getID()) {
                        continue;
                    }

                    ci.setPixel(x, y, c.getColor());
                }
            }
        }
        return ci;
    }

    @Override
    public void show() {
        new ConnectedComponentFrame(this).setVisible(true);
    }

    @Override
    protected ImagePlus toImagePlus(String title) {
        ColorProcessor ip = new ColorProcessor(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Component c = componentHashTable.get(getPixel(x, y));

                if (c.getID() == background.getID()) {
                    continue;
                }

                int r = c.getColor().getRed();
                int g = c.getColor().getGreen();
                int b = c.getColor().getBlue();

                int rgb = ((r << 16) & 0x00FF0000)
                        | ((g << 8) & 0x0000FF00)
                        | (b & 0x000000FF);

                ip.putPixel(x, y, rgb);
            }
        }

        return new ImagePlus(ip.toString(), ip);
    }

    public void setPixel(int x, int y, long v) {
        pixelArray[(y * width) + x] = v;
    }

    public long getPixel(int x, int y) {
        return pixelArray[(y * width) + x];
    }

    public TLongObjectMap<Component> getComponentTable() {
        return componentHashTable;
    }

    public Component getBackgroundComponent() {
        return background;
    }

    private void setComponentTable(TLongObjectMap<Component> componentTable) {
        componentHashTable = componentTable;
    }

    private void setBackgroundComponent(Component background) {
        this.background = background;
    }

    private boolean validPoint(int x, int y) {
        return (x >= 0) && (x < width) && (y >= 0) && (y < height);
    }

    private void connectedComponentLabeling(BitImage bi) {
        TLongObjectMap<Component> table = new TLongObjectHashMap<>();

        Component bg = new Component();
        bg.setColor(Color.BLACK);

        TIntStack stack = new TIntArrayStack(width, height);
        boolean[] visited = new boolean[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelIndex = (y * width) + x;

                if (!visited[pixelIndex]) {
                    visited[pixelIndex] = true;

                    if (bi.getPixel(x, y)) {
                        stack.push(pixelIndex);

                        // new component
                        Component c = new Component();
                        int count = 0;

                        while (!(stack.size() == 0)) {
                            int curIdx = stack.pop();
                            int curX = curIdx % width;
                            int curY = curIdx / width;

                            pixelArray[(curY * width) + curX] = c.getID();
                            count++;

                            if (curX < c.getMinX()) {
                                c.setMinX(curX);
                            }

                            if (curX > c.getMaxX()) {
                                c.setMaxX(curX);
                            }

                            if (curY < c.getMinY()) {
                                c.setMinY(curY);
                            }

                            if (curY > c.getMaxY()) {
                                c.setMaxY(curY);
                            }

                            // up
                            try {
                                int newX = curX;
                                int newY = curY - 1;
                                int newIdx = (newY * width) + newX;

                                if (validPoint(newX, newY) && !visited[newIdx]) {
                                    visited[newIdx] = true;

                                    if (bi.getPixel(newX, newY)) {
                                        stack.push(newIdx);
                                    } else {
                                        pixelArray[(y * width) + x] = bg.getID();
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignore) {
                            }

                            // down
                            try {
                                int newX = curX;
                                int newY = curY + 1;
                                int newIdx = (newY * width) + newX;

                                if (validPoint(newX, newY) && !visited[newIdx]) {
                                    visited[newIdx] = true;

                                    if (bi.getPixel(newX, newY)) {
                                        stack.push(newIdx);
                                    } else {
                                        pixelArray[(y * width) + x] = bg.getID();
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignore) {
                            }

                            // left
                            try {
                                int newX = curX - 1;
                                int newY = curY;
                                int newIdx = (newY * width) + newX;

                                if (validPoint(newX, newY) && !visited[newIdx]) {
                                    visited[newIdx] = true;

                                    if (bi.getPixel(newX, newY)) {
                                        stack.push(newIdx);
                                    } else {
                                        pixelArray[(y * width) + x] = bg.getID();
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignore) {
                            }

                            // right
                            try {
                                int newX = curX + 1;
                                int newY = curY;
                                int newIdx = (newY * width) + newX;

                                if (validPoint(newX, newY) && !visited[newIdx]) {
                                    visited[newIdx] = true;

                                    if (bi.getPixel(newX, newY)) {
                                        stack.push(newIdx);
                                    } else {
                                        pixelArray[(y * width) + x] = bg.getID();
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignore) {
                            }
                        } // stack empty

                        c.setAreaSize(count);
                        table.put(c.getID(), c);
                    } else {
                        pixelArray[(y * width) + x] = bg.getID();
                    }
                }
            } // end for
        }

        table.put(bg.getID(), bg);
        setBackgroundComponent(bg);
        setComponentTable(table);
    }

}
