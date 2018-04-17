/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Menus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.StackStatistics;
import java.awt.Color;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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

    private static final Logger LOG = LogUtils.LOGGER;
    private final boolean EXIT_WHEN_WINDOW_CLOSED = false;

    private double RAPID_ENHANCE = 0.32;
    private double DELAY_WASHOUT = -0.10;
    private double DELAY_PERSIST = 0.10;
    private boolean IS_SHOW_WASHOUT = true;
    private boolean IS_SHOW_PLATEAU = true;
    private boolean IS_SHOW_PERSIST = true;
    private boolean IS_SHOW_EDEMA = false;
    private boolean IS_SHOW_FLUID = false;
    private boolean IS_SHOW_GLANDULAR = false;
    private boolean IS_SHOW_NOISE = false;
    private boolean IS_SHOW_UNMAPPED = false;
    private Range<Double> PLATEAU = Range.between(DELAY_WASHOUT, DELAY_PERSIST);
    private double GLANDULAR_NOISE_RATIO = 1.33;

    public static void main(String args[]) {
        File studyRoot = new File(BMRStudy.SBMR);
        BMRStudy bmr = new BMRStudy(studyRoot.toPath());
        Kinetic k = new Kinetic(bmr, true);
        k.show();
    }

    public final BMRStudy bmrStudy;
    public final int width;
    public final int height;
    public final int size;
    public int glandular;
    public int noiseFloor;
    public boolean finished = false;
    public boolean mriBackground = true;

    public Kinetic(BMRStudy bmr) {
        this(bmr, false);
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
                noiseFloor = 0;
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
        IJUtils.openImageJ(true);
        {
            MenuItem item = new MenuItem("Breast MRI Study");
            Menus.getImageJMenu("File>Import").insert(item, 0);
            Menus.getImageJMenu("File>Import").insertSeparator(1);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DirectoryChooser dc = new DirectoryChooser("Breast MRI Study");
                    if (dc.getDirectory() == null) {
                        return;
                    }
                    File studyRoot = new File(dc.getDirectory());
                    BMRStudy bmr = new BMRStudy(studyRoot.toPath());
                    Kinetic k = new Kinetic(bmr, true);
                    k.show();
                }
            });
        }
        {
            MenuItem item = new MenuItem("Kinetic");
            Menus.getImageJMenu("Plugins").addSeparator();
            Menus.getImageJMenu("Plugins").add(item);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    GenericDialog gd = new GenericDialog("Color Mapping");
                    gd.addCheckbox("Use MRI as background", mriBackground);
                    gd.addNumericField("Rapid Enhance: ", RAPID_ENHANCE, 2);
                    gd.addNumericField("Delay Washout: ", DELAY_WASHOUT, 2);
                    gd.addNumericField("Delay Persist: ", DELAY_PERSIST, 2);
                    gd.addNumericField("Glandular to noise ratio: ", GLANDULAR_NOISE_RATIO, 2);
                    gd.addCheckbox("Show washout", IS_SHOW_WASHOUT);
                    gd.addCheckbox("Show plateau", IS_SHOW_PLATEAU);
                    gd.addCheckbox("Show persistent", IS_SHOW_PERSIST);
                    gd.addCheckbox("Show edema", IS_SHOW_EDEMA);
                    gd.addCheckbox("Show fluid", IS_SHOW_FLUID);
                    gd.addCheckbox("Show high enhancement", IS_SHOW_GLANDULAR);
                    gd.addCheckbox("Show noise", IS_SHOW_NOISE);
                    gd.addCheckbox("Show unmapped", IS_SHOW_UNMAPPED);
                    gd.showDialog();
                    if (gd.wasCanceled()) {
                        return;
                    }
                    mriBackground = gd.getNextBoolean();
                    RAPID_ENHANCE = gd.getNextNumber();
                    DELAY_WASHOUT = gd.getNextNumber();
                    DELAY_PERSIST = gd.getNextNumber();
                    GLANDULAR_NOISE_RATIO = gd.getNextNumber();

                    PLATEAU = Range.between(DELAY_WASHOUT, DELAY_PERSIST);
                    glandular = (int) (noiseFloor * GLANDULAR_NOISE_RATIO);

                    IS_SHOW_WASHOUT = gd.getNextBoolean();
                    IS_SHOW_PLATEAU = gd.getNextBoolean();
                    IS_SHOW_PERSIST = gd.getNextBoolean();
                    IS_SHOW_EDEMA = gd.getNextBoolean();
                    IS_SHOW_FLUID = gd.getNextBoolean();
                    IS_SHOW_GLANDULAR = gd.getNextBoolean();
                    IS_SHOW_NOISE = gd.getNextBoolean();
                    IS_SHOW_UNMAPPED = gd.getNextBoolean();

                    display(colorMapping(null));
                }
            });
        }
    }

    public ImagePlus colorMapping(BitVolume bv) {
        int vWashout = 0;
        int vPlateau = 0;
        int vPersist = 0;
        KineticType.FLUID.color = IS_SHOW_FLUID ? KineticType.FLUID.getDefaultColor() : null;
        KineticType.EDEMA.color = IS_SHOW_EDEMA ? KineticType.EDEMA.getDefaultColor() : null;
        KineticType.PLATEAU.color = IS_SHOW_PLATEAU ? KineticType.PLATEAU.getDefaultColor() : null;
        KineticType.WASHOUT.color = IS_SHOW_WASHOUT ? KineticType.WASHOUT.getDefaultColor() : null;
        KineticType.PERSIST.color = IS_SHOW_PERSIST ? KineticType.PERSIST.getDefaultColor() : null;
        KineticType.GLAND.color = IS_SHOW_GLANDULAR ? KineticType.GLAND.getDefaultColor() : null;
        KineticType.NOISE.color = IS_SHOW_NOISE ? KineticType.NOISE.getDefaultColor() : null;
        KineticType.UNMAPPED.color = IS_SHOW_UNMAPPED ? KineticType.UNMAPPED.getDefaultColor() : null;

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

        String title = getTitle();
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

    public String getTitle() {
        final StringBuffer sb = new StringBuffer();

        sb.append(bmrStudy.getStudyID());
        sb.append("_");
        sb.append(RAPID_ENHANCE);
        sb.append("_");
        sb.append(DELAY_WASHOUT);
        sb.append("_");
        sb.append(DELAY_PERSIST);
        sb.append("_");
        sb.append(GLANDULAR_NOISE_RATIO);

        return sb.toString();
    }

    public String toString(int x, int y, int z) {
        final String sid = bmrStudy.getStudyID();
        final short i = bmrStudy.getPixel(x, y, z, 0);
        final short p = bmrStudy.getPixel(x, y, z, 1);
        final short d = bmrStudy.getPixel(x, y, z, 2);
        final String s = mapping(i, p, d).toString();
        return String.format("SID=[ %5s ] @ (%3d,%3d,%3d) : t0=[ %04d ], t1=[ %04d ], t2=[ %04d ] %8s", sid, x, y, z, i, p, d, s);
    }

    public void show() {
        display(colorMapping(null));
    }

    private void display(ImagePlus i) {
        i.show();
        i.setPosition(size / 2);

        final ImageWindow iw = i.getWindow();
        iw.setAlwaysOnTop(true);
        iw.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent we) {
                if (EXIT_WHEN_WINDOW_CLOSED) {
                    System.err.println("...");
                    System.exit(0);
                }
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
                IJ.showStatus(Kinetic.this.toString(X, Y, Z - 1));
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
                    i.setSlice(i.getCurrentSlice());
                    e.consume();
                } else {
                    final int X = ic.getCursorLoc().x;
                    final int Y = ic.getCursorLoc().y;
                    int Z = i.getCurrentSlice();
                    Z += e.getWheelRotation() > 0 ? 1 : -1;
                    Z = Z > i.getNSlices() ? i.getNSlices() : Z < 1 ? 1 : Z;
                    IJ.showStatus(Kinetic.this.toString(X, Y, Z - 1));
                }
                super.mouseWheelMoved(e);
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
                        boolean old = mriBackground;
                        mriBackground = false;
                        ImagePlus imp = colorMapping(voi);
                        mriBackground = old;
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
                                    + desc + "_" + imp.getTitle()
                                    + String.format(".seed");

                            ROIUtils.saveVOI(rois, roiFile);
                            ROIUtils.showROI(roiFile, EXIT_WHEN_WINDOW_CLOSED);

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
        KineticType ret = null;
        if (initial >= glandular) {
            if (R1 < -0.4) {
                if (PLATEAU.contains(R2)) {
                    ret = KineticType.FLUID;
                } else {
                    ret = KineticType.UNMAPPED;
                }
            } else if (R1 < -0.2) {
                if (PLATEAU.contains(R2)) {
                    ret = KineticType.EDEMA;
                } else {
                    ret = KineticType.UNMAPPED;
                }
            } else if (R1 > RAPID_ENHANCE) {
                if (PLATEAU.contains(R2)) {
                    ret = KineticType.PLATEAU;
                } else if (R2 < PLATEAU.getMinimum()) {
                    ret = KineticType.WASHOUT;
                } else if (R2 > PLATEAU.getMaximum()) {
                    ret = KineticType.PERSIST;
                } else {
                    throw new UnknownError();
                }
            } else {
                ret = KineticType.GLAND;
            }
        } else {
            ret = KineticType.NOISE;
        }

        return ret;
    }
}
