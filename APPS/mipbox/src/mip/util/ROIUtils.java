package mip.util;

import ij.IJ;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.Opener;
import ij.io.RoiDecoder;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.awt.Polygon;
import java.awt.geom.GeneralPath;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import mip.data.image.BitVolume;

public class ROIUtils {

    private static final RoiManager SAVER = new RoiManager(true);
    private static final Logger LOG = Logger.getLogger(ROIUtils.class.getName());

    public static void showROI(String zipFile) {
        RoiManager rm = RoiManager.getInstance();
        if (rm != null) {
            rm.runCommand("Select All");
            rm.runCommand("Delete");
            rm.close();
        }
        IJUtils.openImageJ();
        IJ.getImage().setRoi(0, 0, 0, 0);
        IJ.run("Select None");
        if (zipFile != null) {
            new Opener().openZip(zipFile);
        }
    }

    public static void saveROIs(List<Roi> rois, String zipFile) {
        new File(zipFile).delete();
        if (SAVER.getCount() != 0) {
            SAVER.runCommand("Select All");
            SAVER.runCommand("Delete");
        }
        rois.stream().forEach((roi) -> {
            SAVER.addRoi(roi);
        });
        SAVER.runCommand("save", zipFile);
    }

    public static BitVolume openROIs(String zipFile, int w, int h, int l) {
        if (!IOUtils.fileExisted(zipFile)) {
            return null;
        }

        BitVolume bv = new BitVolume(w, h, l);
        List<Roi> rois = ROIUtils.openROIs(zipFile);
        rois.stream().forEach((roi) -> {
            int z = roi.getPosition() - 1;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (roi.contains(x, y)) {
                        bv.setPixel(x, y, z, true);
                    }
                }
            }
        });

        return bv;
    }

    public static List<Roi> openROIs(String zipFile) {
        List<Roi> rois = new ArrayList<>(10);

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

    private static boolean selected(ImageProcessor ip, int x, int y, float min, float max) {
        float v = ip.getf(x, y);
        return v >= min && v <= max;
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
                        } else if (outline[x + 1] == null) {
                            outline[x + 1] = outline[x];
                            outline[x] = null;
                            outline[x + 1].shift(x + 1, y);
                        } else if (outline[x + 1] == outline[x]) {
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

    private ROIUtils() {
    }

    /*
     * This class implements a Cartesian polygon in progress.
     * The edges are supposed to be of unit length, and parallel to one axis.
     * It is implemented as a deque to be able to add points to both sides.
     * The points should be added such that for each pair of consecutive points, the inner part is on the left.
     */
    private static class Outline {

        int[] x;
        int[] y;
        int first;
        int last;
        int reserved;
        final int GROW = 10;

        Outline() {
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
}
