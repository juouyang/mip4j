package mip.data.image;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;
import ij.ImagePlus;
import ij.process.ColorProcessor;

import java.awt.Color;
import java.io.IOException;
import mip.data.Component;
import mip.data.image.mr.MR;
import mip.util.IOUtils;
import mip.view.swing.ConnectedComponentFrame;

/**
 * @author ju Connected Component Labeling
 *
 */
public class ConnectedComponent extends AbstractImage {

    private ColorImage ci;
    private long[] pixelArray = new long[1];
    private TLongObjectMap<Component> componentHashTable;
    private Component background;

    public ConnectedComponent(BitImage bi) {
        width = bi.getWidth();
        height = bi.getHeight();
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

    public static void main(String[] args) throws IOException {
        MR mr = new MR(IOUtils.getFileFromResources("resources/bmr/2/080.dcm").toPath());
        BitImage bi = new BitImage(mr.width, mr.height);

        int i = 0;
        for (short s : mr.pixelArray) {
            bi.pixelArray.set(i++, s > 1200);
        }

        ConnectedComponent cc = new ConnectedComponent(bi);
        cc.show();
        cc.getImagePlus("").show();
    }

    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    @Override
    protected ImagePlus convertImageToImagePlus(String title) {
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

                int rgb = ((r << 16) & 0x00FF0000) | ((g << 8) & 0x0000FF00) | (b
                        & 0x000000FF);

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

    public long[] getPixelArray() {
        return pixelArray;
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
    //</editor-fold>

    private boolean validCoordinate(int x, int y) {
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
                            int currentIndex = stack.pop();
                            int currentX = currentIndex % width;
                            int currentY = currentIndex / width;

                            pixelArray[(currentY * width) + currentX] = c.getID();
                            count++;

                            if (currentX < c.getMinX()) {
                                c.setMinX(currentX);
                            }

                            if (currentX > c.getMaxX()) {
                                c.setMaxX(currentX);
                            }

                            if (currentY < c.getMinY()) {
                                c.setMinY(currentY);
                            }

                            if (currentY > c.getMaxY()) {
                                c.setMaxY(currentY);
                            }

                            // up
                            try {
                                int newX = currentX;
                                int newY = currentY - 1;
                                int newIndex = (newY * width) + newX;

                                if (validCoordinate(newX, newY) && !visited[newIndex]) {
                                    visited[newIndex] = true;

                                    if (bi.getPixel(newX, newY)) {
                                        stack.push(newIndex);
                                    } else {
                                        pixelArray[(y * width) + x] = bg.getID();
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignore) { // ignore
                            }

                            // down
                            try {
                                int newX = currentX;
                                int newY = currentY + 1;
                                int newIndex = (newY * width) + newX;

                                if (validCoordinate(newX, newY) && !visited[newIndex]) {
                                    visited[newIndex] = true;

                                    if (bi.getPixel(newX, newY)) {
                                        stack.push(newIndex);
                                    } else {
                                        pixelArray[(y * width) + x] = bg.getID();
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignore) { // ignore
                            }

                            // left
                            try {
                                int newX = currentX - 1;
                                int newY = currentY;
                                int newIndex = (newY * width) + newX;

                                if (validCoordinate(newX, newY) && !visited[newIndex]) {
                                    visited[newIndex] = true;

                                    if (bi.getPixel(newX, newY)) {
                                        stack.push(newIndex);
                                    } else {
                                        pixelArray[(y * width) + x] = bg.getID();
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignore) { // ignore
                            }

                            // right
                            try {
                                int newX = currentX + 1;
                                int newY = currentY;
                                int newIndex = (newY * width) + newX;

                                if (validCoordinate(newX, newY) && !visited[newIndex]) {
                                    visited[newIndex] = true;

                                    if (bi.getPixel(newX, newY)) {
                                        stack.push(newIndex);
                                    } else {
                                        pixelArray[(y * width) + x] = bg.getID();
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignore) { // ignore
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
