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
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.StackStatistics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import mip.data.image.BitVolume;
import mip.data.image.Point3d;
import mip.util.IJUtils;
import mip.util.LogUtils;
import mip.util.ROIUtils;
import org.apache.commons.lang3.Range;

/**
 *
 * @author ju
 */
public class Kinetic {

    private static final double RAPID_ENHANCE = 0.32;
    private static final Range<Double> PLATEAU = Range.between(-0.10, 0.10);
    private static final double GLANDULAR_NOISE_RATIO = 1.33;
    private static final Logger LOG = LogUtils.LOGGER;

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
    public boolean mriBackground = true;

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
                glandular = (int) (noiseFloor * GLANDULAR_NOISE_RATIO);
            }
        }
        EXIT_WHEN_WINDOW_CLOSED = exitOnClosed;
    }

    public ImagePlus colorMapping(BitVolume bv) {
        int vWashout = 0;
        int vPlateau = 0;
        int vPersist = 0;

        ImageStack ims = new ImageStack(width, height);
        {
            final int[] rgb = new int[3];
            for (int i = 0; i < size; i++) {
                ColorProcessor cp = new ColorProcessor(width, height);
                ByteProcessor bp = mriBackground ? IJUtils.toByteProcessor(
                        bmrStudy.T1.imageArrayXY[i],
                        bmrStudy.T1.imageArrayXY[i].getWindowCenter(),
                        bmrStudy.T1.imageArrayXY[i].getWindowWidth()) : null;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int initial = bmrStudy.getPixel(x, y, i, 0);
                        int peak = bmrStudy.getPixel(x, y, i, 1);
                        int delay = bmrStudy.getPixel(x, y, i, 2);
                        KineticType kt = mapping(initial, peak, delay);

                        if (kt.color == null) {
                            if (bp != null) {
                                rgb[0] = Math.max(bp.get(x, y) - 64, 0);
                                rgb[1] = Math.max(bp.get(x, y) - 64, 0);
                                rgb[2] = Math.max(bp.get(x, y) - 64, 0);
                                cp.putPixel(x, y, rgb);
                            }
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
                title = String.format("%2d-%2d-%2d", washout, plateau, persist);
            }
        }

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

    public void show() {
        ImagePlus imp = colorMapping(null);
        display(imp);
    }

    private void display(ImagePlus i) {
        IJUtils.openImageJ(true);
        i.show();
        ImagePlus mip = bmrStudy.T1.mip();
        mip.show();
        i.setPosition(size / 2);

        final ImageWindow iw = i.getWindow();
        //iw.setResizable(false);
        iw.setLocation(10 + 530, 10);
        mip.getWindow().setLocation(10, 10);
        iw.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent we) {
                if (EXIT_WHEN_WINDOW_CLOSED) {
                    System.exit(0);
                }
                mip.getWindow().close();
                System.gc();
                synchronized (Kinetic.this) {
                    finished = true;
                    Kinetic.this.notifyAll();
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

        ic.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.isControlDown() && me.isAltDown()) { // ctrl+alt pressed
                    final int X = ic.getCursorLoc().x;
                    final int Y = ic.getCursorLoc().y;
                    final int Z = i.getCurrentSlice() - 1;
                    final String side = (X < width / 2) ? "R" : "L";

                    Point3d seed = new Point3d(X, Y, Z);
                    BitVolume voi = BitVolume.regionGrowing(Kinetic.this, seed);
                    if (voi != null) {
                        ImagePlus imp = colorMapping(voi);
                        imp.show();
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
                                    + String.format("seed(%d,%d,%d)", X, Y, Z);

                            ROIUtils.saveVOI(rois, roiFile);
                            //ROIUtils.showROI(roiFile, EXIT_WHEN_WINDOW_CLOSED);

                            File p = new File(pn);
                            try {
                                p.createNewFile();
                            } catch (IOException ex) {
                            }
                        }
                        IJUtils.render(imp, 1, 0, 0);
                    }
                }
                super.mouseClicked(me);
            }
        });
    }

    public boolean isRapidInitialRise(int x, int y, int z) {
        int initial = bmrStudy.getPixel(x, y, z, 0);
        int peak = bmrStudy.getPixel(x, y, z, 1);
        double R1 = (peak - initial) / (double) initial;
        return initial >= glandular && R1 > RAPID_ENHANCE;
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
            } else if (R1 > RAPID_ENHANCE) {
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
