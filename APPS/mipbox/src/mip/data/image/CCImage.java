package mip.data.image;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

import java.awt.Color;
import java.io.IOException;
import mip.data.ConnectedComponent;
import mip.data.image.mr.MR;
import mip.util.IOUtils;
import mip.view.swing.ColorImageFrame;

/**
 * @author ju Connected Component Labeling
 *
 */
public class CCImage extends AbstractImage {

    private ColorImage ci;

    public ColorImage getColorImage() {
        return ci;
    }

    private long[] pixelArray = new long[1];
    private TLongObjectMap<ConnectedComponent> componentHashTable;
    private ConnectedComponent background;

    public CCImage(BitImage bi) {
        width = bi.getWidth();
        height = bi.getHeight();
        pixelArray = new long[width * height];
        ci = new ColorImage(width, height);

        TLongObjectMap<ConnectedComponent> table = new TLongObjectHashMap<>();

        ConnectedComponent bg = new ConnectedComponent();
        bg.setMinX(0);
        bg.setMinY(0);
        bg.setMaxX(width - 2); // TODO why -2?
        bg.setMaxY(height - 2); // TODO why -2?
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
                        ConnectedComponent c = new ConnectedComponent();
                        int count = 0;

                        while (!(stack.size() == 0)) {
                            int currentIndex = stack.pop();
                            int currentX = currentIndex % width;
                            int currentY = currentIndex / width;

                            pixelArray[(currentY * width) + currentX] = c.getID();
                            ci.setPixel(currentX, currentY, c.getColor());
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
                                        ci.setPixel(x, y, bg.getColor());
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
                                        ci.setPixel(x, y, bg.getColor());
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
                                        ci.setPixel(x, y, bg.getColor());
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
                                        ci.setPixel(x, y, bg.getColor());
                                    }
                                }
                            } catch (ArrayIndexOutOfBoundsException ignore) { // ignore
                            }
                        } // stack empty

                        c.setAreaSize(count);
                        table.put(c.getID(), c);
                    } else {
                        pixelArray[(y * width) + x] = bg.getID();
                        ci.setPixel(x, y, bg.getColor());
                    }
                }
            } // end for
        }

        table.put(bg.getID(), bg);
        setBackgroundComponent(bg);
        setComponentTable(table);
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

    public TLongObjectMap<ConnectedComponent> getComponentTable() {
        return componentHashTable;
    }

    public ConnectedComponent getBackgroundComponent() {
        return background;
    }

    private void setComponentTable(TLongObjectMap<ConnectedComponent> componentTable) {
        componentHashTable = componentTable;
    }

    private void setBackgroundComponent(ConnectedComponent background) {
        this.background = background;
    }

    private boolean validCoordinate(int x, int y) {
        return (x >= 0) && (x < width) && (y >= 0) && (y < height);
    }

    public void show() {
        new ColorImageFrame(ci).setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        MR mr = new MR(IOUtils.getFileFromResources("resources/bmr/2/080.dcm").toPath());
        BitImage bi = new BitImage(mr.width, mr.height);

        int i = 0;
        for (short s : mr.pixelArray) {
            bi.pixelArray.set(i++, s > 1200);
        }

        new CCImage(bi).show();
    }
}
