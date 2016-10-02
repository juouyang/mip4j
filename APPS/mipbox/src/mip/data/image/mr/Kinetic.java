package mip.data.image.mr;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortStatistics;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;

import java.text.DecimalFormat;
import java.util.List;
import mip.data.Point3d;
import mip.data.image.BitVolume;
import static mip.util.DebugUtils.DBG;
import static mip.util.DebugUtils.pause;

import mip.util.IOUtils;
import mip.util.ImageJUtils;
import mip.util.ROIUtils;
import mip.util.Timer;
import org.apache.commons.lang3.Range;

public class Kinetic {

    private static final double AURORA_STRONG_ENHANCE = 0.32;
    private static final double AURORA_DELAYED_WASHOUT = -0.05;
    private static final double AURORA_DELAYED_PERSIST = 0.05;
    private static final DecimalFormat DF = new DecimalFormat("0.0000;-0.0000");

    private final double STRONG_ENHANCE = AURORA_STRONG_ENHANCE;
    private final double GLANDULAR;
    private final Range<Double> PLATEAU_RANGE;
    private final boolean NO_BACKGROUND;

    public final BMRStudy mrStudy;

    private final StringBuilder summary = new StringBuilder();
    private String roiFilePath;
    private BitVolume selectedVOI;
    private boolean onlyVOI;
    private ImagePlus imp;
    private double summaryWashout = 0;
    private double summaryPlateau = 0;
    private double summaryPersistent = 0;
    private double summaryStrongEnhanced = 0;
    private double summaryROI = 0;

    public Kinetic(BMRStudy mrs) {
        this(mrs, null, AURORA_DELAYED_WASHOUT, AURORA_DELAYED_PERSIST, false, false, false);
    }

    public Kinetic(BMRStudy mrs, String roiFile, double delayedWashout, double delayedPersist, boolean noBackground, boolean doColorMapping, boolean doOnlyVOI) {
        mrStudy = mrs;
        selectedVOI = initVOI(roiFile);
        NO_BACKGROUND = noBackground;
        PLATEAU_RANGE = Range.between(delayedWashout, delayedPersist);
        GLANDULAR = initGlandular();
        imp = (doColorMapping) ? colorMapping(doOnlyVOI) : null;
        onlyVOI = doOnlyVOI;
    }

    public static void main(String[] args) throws IOException {
        File studyRoot = new File(Kinetic.class.getClassLoader().getResource("resources/bmr/").getFile());
        final Kinetic k = new Kinetic(new BMRStudy(studyRoot.toPath()));
        k.show();
        Point3d seed = new Point3d(376, 267, 71);
        BitVolume selected = BitVolume.regionGrowingByKinetic(k, seed);
        List<Roi> rois = selected.getROIs();
        final String roiFile = "/tmp/foo.zip";
        ROIUtils.saveROIs(rois, roiFile);
        k.setVOI(roiFile);
        DBG.accept(k + "\n");
        k.show();
        k.render();
    }

    public void save() throws IOException {
        FileSaver fs = new FileSaver(getImagePlue());
        fs.saveAsTiffStack(mrStudy.studyRoot + "/cm.tif");
    }

    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    private BitVolume initVOI(String roiFile) {
        roiFilePath = (IOUtils.fileExisted(roiFile)) ? roiFile : null;
        if (roiFilePath == null) {
            return null;
        }

        int w = getWidth();
        int h = getHeight();
        int l = getSize();
        BitVolume bv = new BitVolume(w, h, l);
        List<Roi> rois = ROIUtils.openROIs(roiFilePath);
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

    private double initGlandular() {
        MR firstMiddleSlice = mrStudy.T0.getImageArrayXY()[mrStudy.T0.getSize() / 2];
        ImageStatistics is = new ShortStatistics(ImageJUtils.getShortProcessorFromShortImage(firstMiddleSlice));
        // TODO magic number
        int noiseFloor = (int) Math.ceil(is.stdDev * 2.0);
        double glandularNoiseRatio = (noiseFloor > 1000) ? 1.47 : 1.33;
        return glandularNoiseRatio * noiseFloor;
    }

    public void setVOI(String roiFile) {
        selectedVOI = initVOI(roiFile);
        imp = colorMapping(true);
    }

    public boolean isStrongEnhanced(int x, int y, int z) {
        int initial = mrStudy.getPixel(x, y, z, 0);
        double R1 = initialPhase(initial, mrStudy.getPixel(x, y, z, 1));
        return initial >= GLANDULAR && R1 > STRONG_ENHANCE;
    }

    public int getWidth() {
        return mrStudy.T0.getWidth();
    }

    public int getHeight() {
        return mrStudy.T0.getHeight();
    }

    public int getSize() {
        return mrStudy.T0.getSize();
    }

    @Override
    public String toString() {
        getImagePlue();
        return summary.toString();
    }

    public void show() {
        ImageJ ij = new ImageJ();
        ij.exitWhenQuitting(true);

        if (roiFilePath != null) {
            new Opener().openZip(roiFilePath);
        }
        final ImagePlus i = getImagePlue().duplicate();
        i.show();
        i.getCanvas().addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {

                final int Z = i.getCurrentSlice() - 1;
                final int X = i.getCanvas().getCursorLoc().x;
                final int Y = i.getCanvas().getCursorLoc().y;

                i.setTitle(getColorMappingInfo(X, Y, Z));
                super.mouseMoved(e);
            }

        });
        i.getCanvas().addMouseWheelListener(new MouseAdapter() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int Z = i.getCurrentSlice() + ((e.getWheelRotation() > 0) ? 1 : -1);
                Z = Z > i.getNSlices() ? i.getNSlices() : Z < 1 ? 1 : Z;
                final int X = i.getCanvas().getCursorLoc().x;
                final int Y = i.getCanvas().getCursorLoc().y;

                i.setTitle(getColorMappingInfo(X, Y, Z - 1));
                i.setPosition(Z);
                super.mouseWheelMoved(e);
            }
        });
        i.setPosition(mrStudy.T0.getSize() / 2);
    }

    public void render() {
        Image3DUniverse univ = new Image3DUniverse();
        ContentInstant ci = univ.addVoltex(imp, 1).getCurrent();

        if (ci != null) {
            univ.show();
        }
    }

    private boolean hasROI() {
        return roiFilePath != null;
    }

    public KineticType getKinetic(int x, int y, int z) {
        return mapping(mrStudy.getPixel(x, y, z, 0), mrStudy.getPixel(x, y, z, 1), mrStudy.getPixel(x, y, z, 2));
    }

    private String getColorMappingInfo(int x, int y, int z) {
        final short initial = mrStudy.getPixel(x, y, z, 0);
        final short peak = mrStudy.getPixel(x, y, z, 1);
        final short delay = mrStudy.getPixel(x, y, z, 2);
        final double R1 = Kinetic.initialPhase(initial, peak);
        final double R2 = Kinetic.delayPhase(initial, delay) - R1;
        return String.format("(%03d,%03d,%03d) = %04d -> %04d -> %04d, R1=%s, R2=%s, %s", x, y, z + 1, initial, peak, delay, DF.format(R1), DF.format(R2), getMappingDesc(initial, peak, delay));
    }

    private String getMappingDesc(int initial, int peak, int delay) {
        return mapping(initial, peak, delay).toString();
    }

    private ImagePlus getImagePlue() {
        if (imp == null) {
            imp = colorMapping(onlyVOI);
        }
        return imp;
    }
    //</editor-fold>

    private ImagePlus colorMapping(boolean onlyROI) {
        imp = null;
        summaryWashout = 0;
        summaryPlateau = 0;
        summaryPersistent = 0;
        summaryStrongEnhanced = 0;
        summaryROI = 0;
        summary.setLength(0);

        assert (imp == null && summary.length() == 0 && (summaryWashout + summaryPlateau + summaryPersistent + summaryStrongEnhanced + summaryROI == 0));

        Timer t = new Timer();

        ColorProcessor[] cps = new ColorProcessor[mrStudy.T0.getSize()];
        {
            for (int i = 0; i < cps.length; i++) {
                MR t1 = mrStudy.T1.getImageArrayXY()[i];
                final int width = t1.getWidth();
                final int height = t1.getHeight();
                ByteProcessor bp = NO_BACKGROUND ? null : ImageJUtils.getByteProcessorFromShortImage(t1, t1.getWindowCenter(), t1.getWindowWidth());

                assert (bp != null || NO_BACKGROUND);

                ColorProcessor cp = new ColorProcessor(width, height);
                double enhancedVolume = 0;
                double roiVolume = 0;
                double washoutVolume = 0;
                double plateauVolume = 0;
                double persistVolume = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {

                        int initial = mrStudy.getPixel(x, y, i, 0);
                        int peak = mrStudy.getPixel(x, y, i, 1);
                        int delay = mrStudy.getPixel(x, y, i, 2);

                        boolean pixelInROI = false;
                        if ((hasROI() && selectedVOI.getPixel(x, y, i)) || !hasROI()) {
                            roiVolume++;
                            pixelInROI = true;
                        }

                        if (onlyROI && !pixelInROI) {
                            continue;
                        }

                        final KineticType kt = mapping(initial, peak, delay);

                        Color c;
                        {
                            if (kt == KineticType.UNMAPPED || kt == KineticType.GLAND) {
                                if (NO_BACKGROUND) {
                                    continue;
                                }
                                assert (bp != null);
                                final int v = bp.getPixel(x, y) / 3;
                                c = new Color(v, v, v);
                            } else {
                                c = kt.color;
                            }
                        }
                        cp.putPixel(x, y, new int[]{c.getRed(), c.getGreen(), c.getBlue()});
                        if (pixelInROI) {
                            switch (kt) {
                                case WASHOUT:
                                    washoutVolume++;
                                    enhancedVolume++;
                                    break;
                                case PLATEAU:
                                    plateauVolume++;
                                    enhancedVolume++;
                                    break;
                                case PERSIST:
                                    persistVolume++;
                                    enhancedVolume++;
                                    break;
                                default:
                            }
                        }
                    }
                }// each pixel
                cps[i] = cp;

                summaryWashout += washoutVolume;
                summaryPlateau += plateauVolume;
                summaryPersistent += persistVolume;
                summaryStrongEnhanced += enhancedVolume;
                summaryROI += roiVolume;
            } // each slice
        }
        ImageStack ims = new ImageStack(mrStudy.T1.getImageArrayXY()[0].getWidth(), mrStudy.T1.getImageArrayXY()[0].getHeight());
        for (ColorProcessor cp : cps) {
            ims.addSlice(cp);
        }
        ImagePlus ret = new ImagePlus("", ims);

        //double resolution = mrStudy.T0.pixelSpacingX * mrStudy.T0.pixelSpacingY * mrStudy.T0.sliceThickness;
        summary.append("\tWashout\tPlateau\tPersist\tEnhance\tROI\n");
        summary.append("------------------------------------------------\n");
        summary.append("Total\t");
        summary.append((int) summaryWashout).append("\t");
        summary.append((int) summaryPlateau).append("\t");
        summary.append((int) summaryPersistent).append("\t");
        summary.append((int) summaryStrongEnhanced).append("\t");
        summary.append((int) summaryROI).append("\n");
        summary.append("RatioA\t");
        summary.append(DF.format(summaryWashout / summaryStrongEnhanced)).append("\t");
        summary.append(DF.format(summaryPlateau / summaryStrongEnhanced)).append("\t");
        summary.append(DF.format(summaryPersistent / summaryStrongEnhanced)).append("\t");
        summary.append(DF.format(summaryStrongEnhanced / summaryStrongEnhanced)).append("\t");
        summary.append(DF.format(summaryROI / summaryStrongEnhanced)).append("\n");
        summary.append("RatioM\t");
        summary.append(DF.format(summaryWashout / summaryROI)).append("\t");
        summary.append(DF.format(summaryPlateau / summaryROI)).append("\t");
        summary.append(DF.format(summaryPersistent / summaryROI)).append("\t");
        summary.append(DF.format(summaryStrongEnhanced / summaryROI)).append("\t");
        summary.append(DF.format(summaryROI / summaryROI)).append("\n");

        t.printElapsedTime("colorMapping");

        return ret;
    }

    private static double initialPhase(int initial, int peak) {
        return (double) (peak - initial) / (double) initial;
    }

    private static double delayPhase(int initial, int delay) {
        return (double) (delay - initial) / (double) initial;
    }

    private KineticType mapping(int initial, int peak, int delay) {

        final double R1 = initialPhase(initial, peak);
        final double R2 = delayPhase(initial, delay) - R1;
        KineticType ret = KineticType.UNMAPPED;

        if (initial >= GLANDULAR) {
            if (R1 < -0.4) {
                //ret = PLATEAU_RANGE.contains(R2) ? KineticType.FLUID : ret;
            } else if (R1 < -0.2) {
                //ret = PLATEAU_RANGE.contains(R2) ? KineticType.EDEMA : ret;
            } else if (R1 > STRONG_ENHANCE) {
                ret = PLATEAU_RANGE.contains(R2) ? KineticType.PLATEAU : ret;
                ret = R2 < PLATEAU_RANGE.getMinimum() ? KineticType.WASHOUT : ret;
                ret = R2 > PLATEAU_RANGE.getMaximum() ? KineticType.PERSIST : ret;
            } else {
                ret = KineticType.GLAND;
            }
        }

        return ret;
    }

    public enum KineticType {

        GLAND("Glandular", new Color(12, 12, 12)), WASHOUT("Washout", Color.RED), PLATEAU("Plateau", Color.MAGENTA), PERSIST("Persistent", Color.YELLOW), EDEMA("Edema", Color.GREEN), FLUID("Fluid", Color.BLUE), UNMAPPED("Unmapped", null);
        private final String description;
        final Color color;

        private KineticType(String s, Color c) {
            description = s;
            color = c;
        }

        @Override
        public String toString() {
            return this.description;
        }
    }

}
