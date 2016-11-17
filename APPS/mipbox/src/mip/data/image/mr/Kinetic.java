/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.StackStatistics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import mip.data.image.BitVolume;
import mip.data.image.Point3d;
import static mip.util.DGBUtils.DBG;
import mip.util.IJUtils;
import mip.util.ROIUtils;
import mip.util.Timer;
import org.apache.commons.lang3.Range;

/**
 *
 * @author ju
 */
public class Kinetic {

    private static final double STRONG_ENHANCE = 0.32;
    private static final Range<Double> PLATEAU = Range.between(-0.05, 0.05);

    public static void main(String args[]) {
        File studyRoot = new File(BMRStudy.SBMR);
        BMRStudy bmr = new BMRStudy(studyRoot.toPath());
        Kinetic k = new Kinetic(bmr, true);
        k.show();
    }
    private final boolean EXIT_WHEN_WINDOW_CLOSED;

    public final BMRStudy bmrStudy;
    public final int width;
    public final int height;
    public final int size;
    public final int glandular;
    public boolean finished = false;

    public Kinetic(BMRStudy bmr) {
        this(bmr, true);
    }

    public Kinetic(BMRStudy bmr, boolean exitOnClosed) {
        bmrStudy = bmr;
        width = bmr.T0.getWidth();
        height = bmr.T0.getHeight();
        size = bmr.T0.getSize();
        {
            final MR[] imgs = bmrStudy.T0.imageArrayXY;
            {
                ImageStack s = IJUtils.toImageStack(imgs);
                StackStatistics ss = new StackStatistics(new ImagePlus("", s));
                int count = 0;
                int noiseFloor = 0;
                for (int i : ss.histogram16) {
                    count += i;

                    double cdf = count / ss.area;
                    if (cdf >= 0.95) {
                        break;
                    }
                    noiseFloor++;
                }

                double glandularNoiseRatio = noiseFloor < 2000 ? 1.47 : 1.33;
                glandular = (int) (noiseFloor * glandularNoiseRatio);
                DBG.accept("glandular = " + glandular);
                DBG.accept(",\tnoiseFloor = " + noiseFloor + "\n");
            }
        }
        EXIT_WHEN_WINDOW_CLOSED = exitOnClosed;
    }

    public ImagePlus colorMapping(BitVolume bv) {
        Timer t = new Timer();

        int vWashout = 0;
        int vPlateau = 0;
        int vPersist = 0;

        ImageStack ims = new ImageStack(width, height);
        {
            final int[] rgb = new int[3];
            for (int i = 0; i < size; i++) {
                ColorProcessor cp = new ColorProcessor(width, height);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int initial = bmrStudy.getPixel(x, y, i, 0);
                        int peak = bmrStudy.getPixel(x, y, i, 1);
                        int delay = bmrStudy.getPixel(x, y, i, 2);
                        KineticType kt = mapping(initial, peak, delay);

                        if (kt.color == null) {
                            continue;
                        }

                        if (bv != null) {
                            if (!bv.getPixel(x, y, i)) {
                                continue;
                            }

                            // accumulate
                            switch (kt) {
                                case WASHOUT:
                                    vWashout++;
                                    break;
                                case PLATEAU:
                                    vPlateau++;
                                    break;
                                case PERSIST:
                                    vPersist++;
                                    break;
                            }
                        }

                        rgb[0] = kt.color.getRed();
                        rgb[1] = kt.color.getGreen();
                        rgb[2] = kt.color.getBlue();
                        cp.putPixel(x, y, rgb);
                    }
                }
                ims.addSlice(cp);
            }
        }

        String title = "";
        {
            if (bv != null) {
                final int v = vWashout + vPlateau + vPersist;
                final int washout = (int) (vWashout * 100.0 / v);
                final int plateau = (int) (vPlateau * 100.0 / v);
                final int persist = (int) (vPersist * 100.0 / v);
                DBG.accept("WASHOUT: " + washout + "%\n");
                DBG.accept("PLATEAU: " + plateau + "%\n");
                DBG.accept("PERSIST: " + persist + "%\n");
                title = String.format("%2d-%2d-%2d", washout, plateau, persist);
            }
        }

        t.printElapsedTime("colorMapping");

        return new ImagePlus(title, ims);
    }

    public String toString(int x, int y, int z) {
        final String sid = bmrStudy.getStudyID();
        final short i = bmrStudy.getPixel(x, y, z, 0);
        final short p = bmrStudy.getPixel(x, y, z, 1);
        final short d = bmrStudy.getPixel(x, y, z, 2);
        final String s = mapping(i, p, d).toString();
        return String.format("%5s: %d,%d,%d=%d~%d~%d %s", sid, x, y, z, i, p, d, s);
    }

    public ImageWindow show() {
        ImagePlus imp = colorMapping(null);
        show(imp, bmrStudy.T1.mip());
        return imp.getWindow();
    }

    private void render(ImagePlus i) {
        IJUtils.render(i, 1, 0, 0);
    }

    private void show(ImagePlus i, ImagePlus mip) {
        i.show();
        mip.show();
        i.setPosition(size / 2);

        final ImageWindow iw = i.getWindow();
        iw.setResizable(false);
        iw.setLocation(10 + 512, 10);
        mip.getWindow().setLocation(10, 10);
        iw.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent we) {
                mip.getWindow().close();
                synchronized (Kinetic.this) {
                    finished = true;
                    Kinetic.this.notifyAll();
                    System.gc();
                }
                if (EXIT_WHEN_WINDOW_CLOSED) {
                    System.exit(0);
                }
            }
        });

        final ImageCanvas ic = i.getCanvas();
        ic.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                final int Z = i.getCurrentSlice();
                final int X = ic.getCursorLoc().x;
                final int Y = ic.getCursorLoc().y;
                i.setTitle(Kinetic.this.toString(X, Y, Z - 1));
                super.mouseMoved(e);
            }
        });
        ic.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    Point loc = ic.getCursorLoc();
                    if (!ic.cursorOverImage()) {
                        Rectangle srcRect = ic.getSrcRect();
                        loc.x = srcRect.x + srcRect.width / 2;
                        loc.y = srcRect.y + srcRect.height / 2;
                    }
                    final int X = ic.screenX(loc.x);
                    final int Y = ic.screenY(loc.y);
                    if (e.getWheelRotation() > 0) {
                        if (ic.getMagnification() > 1.0) {
                            ic.zoomOut(X, Y);
                        }
                    } else {
                        ic.zoomIn(X, Y);
                    }
                } else {
                    final int X = ic.getCursorLoc().x;
                    final int Y = ic.getCursorLoc().y;
                    int Z = i.getCurrentSlice();
                    Z += e.getWheelRotation() > 0 ? 1 : -1;
                    Z = Z > i.getNSlices() ? i.getNSlices() : Z < 1 ? 1 : Z;
                    i.setPosition(Z);
                    i.setTitle(Kinetic.this.toString(X, Y, Z - 1));
                }
                super.mouseWheelMoved(e);
            }
        });
        ic.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.isControlDown() && me.isAltDown()) {
                    final int X = ic.getCursorLoc().x;
                    final int Y = ic.getCursorLoc().y;
                    final int Z = i.getCurrentSlice() - 1;
                    final String side = (X < width / 2) ? "R" : "L";

                    Point3d seed = new Point3d(X, Y, Z);
                    BitVolume voi = BitVolume.regionGrowing(Kinetic.this, seed);
                    if (voi != null) {
                        ImagePlus imp = colorMapping(voi);
                        {
                            List<Roi> rois = voi.getROIs();
                            String desc = ROIUtils.getDesc(rois);

                            String roiFile = bmrStudy.studyRoot + "/"
                                    + bmrStudy.getStudyID()
                                    + "_" + side + "_"
                                    + desc + "_" + imp.getTitle()
                                    + ".zip";
                            final String pn = bmrStudy.studyRoot + "/"
                                    + bmrStudy.getStudyID()
                                    + "_" + side + "_"
                                    + desc + "_"
                                    + String.format("%d_%d_%d.seed", X, Y, Z);

                            ROIUtils.saveVOI(rois, roiFile);
                            ROIUtils.showROI(roiFile);

                            File p = new File(pn);
                            try {
                                p.createNewFile();
                            } catch (IOException ex) {
                            }
                        }
                    }
                }
                super.mouseClicked(me);
            }
        });
    }

    public boolean isStrongEnhanced(int x, int y, int z) {
        int initial = bmrStudy.getPixel(x, y, z, 0);
        int peak = bmrStudy.getPixel(x, y, z, 1);
        double R1 = (peak - initial) / (double) initial;
        return initial >= glandular && R1 > STRONG_ENHANCE;
    }

    private KineticType mapping(int initial, int peak, int delay) {
        final double R1 = (peak - initial) / (double) initial;
        final double R2 = (delay - initial) / (double) initial - R1;
        KineticType ret = KineticType.UNMAPPED;
        if (initial >= glandular) {
            if (R1 < -0.4) {
                ret = PLATEAU.contains(R2) ? KineticType.FLUID : ret;
            } else if (R1 < -0.2) {
                ret = PLATEAU.contains(R2) ? KineticType.EDEMA : ret;
            } else if (R1 > STRONG_ENHANCE) {
                ret = PLATEAU.contains(R2) ? KineticType.PLATEAU : ret;
                ret = R2 < PLATEAU.getMinimum() ? KineticType.WASHOUT : ret;
                ret = R2 > PLATEAU.getMaximum() ? KineticType.PERSIST : ret;
            } else {
                ret = KineticType.GLAND;
            }
        }

        return ret;
    }
}
