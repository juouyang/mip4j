package mip.util;

import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.RoiDecoder;
import ij.process.ImageProcessor;
import java.awt.Polygon;
import java.awt.geom.GeneralPath;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ROIUtils {

    private ROIUtils() { // singleton
    }

    public static List<Roi> uncompressROI(String zipFile) {
        List<Roi> rois = new ArrayList<>();

        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile))) {
            while (true) {
                ZipEntry entry = zin.getNextEntry();
                if (entry == null) {
                    break;
                }

                String name = entry.getName();

                if (!name.endsWith(".roi")) {
                    continue;
                }

                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = zin.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();

                    Roi roi = new RoiDecoder(out.toByteArray(), name).getRoi();

                    if (roi != null) {
                        rois.add(roi);
                    }
                }

            }

            zin.close();
        } catch (IOException ignore) {
        }

        return rois;
    }

    public static ArrayList<Roi> filterROI(List<Roi> rois, int z_position) {
        ArrayList<Roi> ret = new ArrayList<>();
        if (rois != null) {
            for (Roi r : rois) {
                if (r.getPosition() == z_position) {
                    ret.add(r);
                }
            }
        }

        return ret;
    }

    public static boolean withinROI(List<Roi> rois, int x, int y) {
        for (Roi r : rois) {
            if (r.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private static boolean selected(ImageProcessor ip, int x, int y, float min, float max) {
        float v = ip.getf(x, y);
        return v >= min && v <= max;
    }

    /*
     * This class implements a Cartesian polygon in progress.
     * The edges are supposed to be of unit length, and parallel to one axis.
     * It is implemented as a deque to be able to add points to both sides.
     * The points should be added such that for each pair of consecutive points, the inner part is on the left.
     */
    static class Outline {

        int[] x, y;
        int first, last, reserved;
        final int GROW = 10;

        public Outline() {
            reserved = GROW;
            x = new int[reserved];
            y = new int[reserved];
            first = last = GROW / 2;
        }

        private void needs(int newCount, int offset) {
            if (newCount > reserved || (offset > first)) {
                if (newCount < reserved + GROW + 1) {
                    newCount = reserved + GROW + 1;
                }
                int[] newX = new int[newCount];
                int[] newY = new int[newCount];
                System.arraycopy(x, 0, newX, offset, last);
                System.arraycopy(y, 0, newY, offset, last);
                x = newX;
                y = newY;
                first += offset;
                last += offset;
                reserved = newCount;
            }
        }

        public void push(int x, int y) {
            needs(last + 1, 0);
            this.x[last] = x;
            this.y[last] = y;
            last++;
        }

        public void shift(int x, int y) {
            needs(last + 1, GROW);
            first--;
            this.x[first] = x;
            this.y[first] = y;
        }

        public void push(Outline o) {
            int count = o.last - o.first;
            needs(last + count, 0);
            System.arraycopy(o.x, o.first, x, last, count);
            System.arraycopy(o.y, o.first, y, last, count);
            last += count;
        }

        public void shift(Outline o) {
            int count = o.last - o.first;
            needs(last + count + GROW, count + GROW);
            first -= count;
            System.arraycopy(o.x, o.first, x, first, count);
            System.arraycopy(o.y, o.first, y, first, count);
        }

        public Polygon getPolygon() {
            // optimize out long straight lines
            int i, j = first + 1;
            for (i = first + 1; i + 1 < last; j++) {
                int x1 = x[j] - x[j - 1];
                int y1 = y[j] - y[j - 1];
                int x2 = x[j + 1] - x[j];
                int y2 = y[j + 1] - y[j];
                if (x1 * y2 == x2 * y1) {
                    // merge i + 1 into i
                    last--;
                    continue;
                }
                if (i != j) {
                    x[i] = x[j];
                    y[i] = y[j];
                }
                i++;
            }
            // wraparound
            int x1 = x[j] - x[j - 1];
            int y1 = y[j] - y[j - 1];
            int x2 = x[first] - x[j];
            int y2 = y[first] - y[j];
            if (x1 * y2 == x2 * y1) {
                last--;
            } else {
                x[i] = x[j];
                y[i] = y[j];
            }
            int count = last - first;
            int[] xNew = new int[count];
            int[] yNew = new int[count];
            System.arraycopy(x, first, xNew, 0, count);
            System.arraycopy(y, first, yNew, 0, count);
            return new Polygon(xNew, yNew, count);
        }
    }

    /*
     * Construct all outlines simultaneously by traversing the rows from top to bottom.
     * prevRow[x + 1] indicates if the pixel at (x, y - 1) is selected.
     * outline[x] is the outline which is currently unclosed at the lower right corner of the previous row.
     */
    public static Roi createSelectionFromThreshold(ImageProcessor ip, float min, float max) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        boolean[] prevRow, thisRow;
        ArrayList polygons = new ArrayList();
        Outline[] outline;
        int progressInc = Math.max(height / 50, 1);

        prevRow = new boolean[width + 2];
        thisRow = new boolean[width + 2];
        outline = new Outline[width + 1];

        for (int y = 0; y <= height; y++) {
            boolean[] b = prevRow;
            prevRow = thisRow;
            thisRow = b;
            for (int x = 0; x <= width; x++) {
                if (y < height && x < width) {
                    thisRow[x + 1] = selected(ip, x, y, min, max);
                } else {
                    thisRow[x + 1] = false;
                }
                if (thisRow[x + 1]) {
                    if (!prevRow[x + 1]) {
                        // upper edge:
                        // - left and right are null: new outline
                        // - left null: push
                        // - right null: shift
                        // - left == right: close
                        // - left != right: merge
                        if (outline[x] == null) {
                            if (outline[x + 1] == null) {
                                outline[x + 1] = outline[x] = new Outline();
                                outline[x].push(x + 1, y);
                                outline[x].push(x, y);
                            } else {
                                outline[x] = outline[x + 1];
                                outline[x + 1] = null;
                                outline[x].push(x, y);
                            }
                        } else {
                            if (outline[x + 1] == null) {
                                outline[x + 1] = outline[x];
                                outline[x] = null;
                                outline[x + 1].shift(x + 1, y);
                            } else if (outline[x + 1] == outline[x]) {
                                //System.err.println("subtract " + outline[x]);
                                polygons.add(outline[x].getPolygon()); // MINUS
                                outline[x] = outline[x + 1] = null;
                            } else {
                                outline[x].shift(outline[x + 1]);
                                for (int x1 = 0; x1 <= width; x1++) {
                                    if (x1 != x + 1 && outline[x1] == outline[x + 1]) {
                                        outline[x1] = outline[x];
                                        outline[x] = outline[x + 1] = null;
                                        break;
                                    }
                                }
                                if (outline[x] != null) {
                                    throw new RuntimeException("assertion failed");
                                }
                            }
                        }
                    }
                    if (!thisRow[x]) {
                        // left edge
                        if (outline[x] == null) {
                            throw new RuntimeException("assertion failed!");
                        }
                        outline[x].push(x, y + 1);
                    }
                } else {
                    if (prevRow[x + 1]) {
                        // lower edge
                        // - bot null: new outline
                        // - left == null: shift
                        // - right == null: push
                        // - right == left: close
                        // - right != left: push
                        if (outline[x] == null) {
                            if (outline[x + 1] == null) {
                                outline[x] = outline[x + 1] = new Outline();
                                outline[x].push(x, y);
                                outline[x].push(x + 1, y);
                            } else {
                                outline[x] = outline[x + 1];
                                outline[x + 1] = null;
                                outline[x].shift(x, y);
                            }
                        } else if (outline[x + 1] == null) {
                            outline[x + 1] = outline[x];
                            outline[x] = null;
                            outline[x + 1].push(x + 1, y);
                        } else if (outline[x + 1] == outline[x]) {
                            //System.err.println("add " + outline[x]);
                            polygons.add(outline[x].getPolygon()); // PLUS
                            outline[x] = outline[x + 1] = null;
                        } else {
                            outline[x].push(outline[x + 1]);
                            for (int x1 = 0; x1 <= width; x1++) {
                                if (x1 != x + 1 && outline[x1] == outline[x + 1]) {
                                    outline[x1] = outline[x];
                                    outline[x] = outline[x + 1] = null;
                                    break;
                                }
                            }
                            if (outline[x] != null) {
                                throw new RuntimeException("assertion failed");
                            }
                        }
                    }
                    if (thisRow[x]) {
                        // right edge
                        if (outline[x] == null) {
                            throw new RuntimeException("assertion failed");
                        }
                        outline[x].shift(x, y + 1);
                    }
                }
            }
            if (y % progressInc == 0) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
            }
        }

        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
        //progressInc = Math.max(polygons.size() / 10, 1);
        for (Object polygon : polygons) {
            path.append((Polygon) polygon, false);
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
        }

        ShapeRoi shape = new ShapeRoi(path);
        Roi roi = shape != null ? shape.shapeToRoi() : null; // try to convert to non-composite ROI
        if (roi != null) {
            return roi;
        } else {
            return shape;
        }
    }
}
