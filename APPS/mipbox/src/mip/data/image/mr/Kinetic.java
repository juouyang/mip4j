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
import java.io.File;
import java.net.URL;
import java.util.List;
import mip.data.Point3d;
import mip.data.image.BitVolume;
import static mip.util.DebugUtils.DBG;
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
        ClassLoader cl = mip.data.image.mr.Kinetic.class.getClassLoader();
        URL url = cl.getResource("resources/bmr/");
        File studyRoot = new File(url.getFile());
        BMRStudy bmr = new BMRStudy(studyRoot.toPath());
        Kinetic k = new Kinetic(bmr);
        k.show(k.colorMapping(null));
    }
    
    public final BMRStudy bmrStudy;
    public final int width;
    public final int height;
    public final int size;
    public final int glandular;
    public final String roiFile;
    
    public Kinetic(BMRStudy bmr) {
        bmrStudy = bmr;
        width = bmr.T0.getWidth();
        height = bmr.T0.getHeight();
        size = bmr.T0.getSize();
        {
            final MR[] imgs = bmrStudy.T0.imageArrayXY;
            {
                ImageStack s = IJUtils.getImageStackFromShortImages(imgs);
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
                
                double glandularNoiseRatio = noiseFloor > 1000 ? 1.47 : 1.33;
                glandular = (int) (noiseFloor * glandularNoiseRatio);
                DBG.accept("glandular = " + glandular);
                DBG.accept(",\tnoiseFloor = " + noiseFloor + "\n");
            }
//            {
//                MR fms = imgs[size / 2];
//               ImageProcessor p = IJUtils.getProcessorFromShortImage(fms);
//                ImageStatistics is = new ShortStatistics(p);
//                int noiseFloor = (int) Math.ceil(is.stdDev * 2.0);
//                double glandularNoiseRatio = noiseFloor > 1000 ? 1.47 : 1.33;
//                int gland = (int) (glandularNoiseRatio * noiseFloor);
//                DBG.accept("old: glandular = " + gland);
//                DBG.accept(",\tnoiseFloor = " + noiseFloor + "\n");
//            }
        }
        
        roiFile = bmrStudy.studyRoot + "//" + bmrStudy.getStudyID() + ".zip";
    }
    
    public ImagePlus colorMapping(BitVolume bv) {
        Timer t = new Timer();
        
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
                        
                        if ((bv != null && !bv.getPixel(x, y, i))) {
                            continue;
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
        
        t.printElapsedTime("colorMapping");
        
        return new ImagePlus("Kinetics", ims);
    }
    
    public String toString(int x, int y, int z) {
        final short i = bmrStudy.getPixel(x, y, z, 0);
        final short p = bmrStudy.getPixel(x, y, z, 1);
        final short d = bmrStudy.getPixel(x, y, z, 2);
        final String s = mapping(i, p, d).toString();
        return String.format("%3d,%3d,%3d=%4d~%4d~%4d %s", x, y, z, i, p, d, s);
    }
    
    public void show(ImagePlus i) {
        i.show();
        i.setPosition(size / 2);
        
        final ImageWindow iw = i.getWindow();
        IJUtils.exitWhenNoWindow(iw);
        iw.setResizable(false);
        
        final ImageCanvas ic = i.getCanvas();
        ic.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                final int Z = i.getCurrentSlice() - 1;
                final int X = ic.getCursorLoc().x;
                final int Y = ic.getCursorLoc().y;
                i.setTitle(Kinetic.this.toString(X, Y, Z));
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
                    
                    Point3d seed = new Point3d(X, Y, Z);
                    BitVolume voi = BitVolume.regionGrowing(Kinetic.this, seed);
                    if (voi != null) {
                        //voi.render();
                        {
                            List<Roi> rois = voi.getROIs();
                            ROIUtils.saveROIs(rois, roiFile);
                            ROIUtils.showROI(roiFile);
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
