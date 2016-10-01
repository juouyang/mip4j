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
import java.util.ArrayList;
import java.util.List;

import mip.util.IOUtils;
import mip.util.ImageJUtils;
import mip.util.ROIUtils;
import mip.util.Timer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Range;

public class Kinetic {

    private static final double AURORA_STRONG_ENHANCE = 0.32;
    private static final double AURORA_DELAYED_WASHOUT = -0.05;
    private static final double AURORA_DELAYED_PERSIST = 0.05;
    private static final double STRONG_ENHANCE = AURORA_STRONG_ENHANCE;
    private static final DecimalFormat DF = new DecimalFormat("0.0000;-0.0000");

    private final double DELAYED_WASHOUT;
    private final double DELAYED_PERSIST;
    private final double GLANDULAR;
    private final Range<Double> PLATEAU_RANGE;
    private final boolean NO_BACKGROUND;

    private final BMRStudy mrStudy;
    private final List<Roi> rois;
    private final String roiFile;
    private final double resolution;

    public ImagePlus imp;
    public double summaryWashout = 0;
    public double summaryPlateau = 0;
    public double summaryPersistent = 0;
    public double summaryStrongEnhanced = 0;
    public double summaryROI = 0;
    public StringBuffer detail = new StringBuffer();
    public StringBuffer summary = new StringBuffer();

    public Kinetic(BMRStudy mrs) {
        this(mrs, null, AURORA_DELAYED_WASHOUT, AURORA_DELAYED_PERSIST, true);
    }

    public Kinetic(BMRStudy mrs, String roiFile, double delayedWashout, double delayedPersist, boolean noBackground) {
        mrStudy = mrs;
        rois = IOUtils.fileExisted(roiFile) ? ROIUtils.uncompressROI(roiFile) : new ArrayList<>();
        resolution = mrs.T0.getPixelSpacingX() * mrs.T0.getPixelSpacingY() * mrs.T0.getSliceThickness();
        this.roiFile = roiFile;
        NO_BACKGROUND = noBackground;
        DELAYED_WASHOUT = delayedWashout;
        DELAYED_PERSIST = delayedPersist;
        PLATEAU_RANGE = Range.between(DELAYED_WASHOUT, DELAYED_PERSIST);
        GLANDULAR = getGlandular();
        doColorMapping();
    }

    public void save() throws IOException {
        FileSaver fs = new FileSaver(imp);
        fs.saveAsTiffStack(mrStudy.studyRoot + "/cm.tif");
        FileUtils.writeStringToFile(new File(mrStudy.studyRoot + "/summary.txt"), summary.toString());
    }

    public static void main(String[] args) throws IOException {
        File studyRoot = new File(Kinetic.class.getClassLoader().getResource("resources/bmr/").getFile());
        final Kinetic k = new Kinetic(new BMRStudy(studyRoot.toPath()));
        k.show();
        System.out.println(k.summary.toString());
    }

    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    public void show() {
        ImageJ ij = new ImageJ();
        ij.exitWhenQuitting(true);

        if (roiFile != null) {
            new Opener().openZip(roiFile);
        }
        imp.show();
        imp.getCanvas().addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {

                final int Z = imp.getCurrentSlice() - 1;
                final int X = imp.getCanvas().getCursorLoc().x;
                final int Y = imp.getCanvas().getCursorLoc().y;

                imp.setTitle(getColorMappingInfo(X, Y, Z));
                super.mouseMoved(e);
            }

        });
        imp.getCanvas().addMouseWheelListener(new MouseAdapter() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int Z = imp.getCurrentSlice() + ((e.getWheelRotation() > 0) ? 1 : -1);
                Z = Z > imp.getNSlices() ? imp.getNSlices() : Z < 1 ? 1 : Z;
                final int X = imp.getCanvas().getCursorLoc().x;
                final int Y = imp.getCanvas().getCursorLoc().y;

                imp.setTitle(getColorMappingInfo(X, Y, Z - 1));
                imp.setPosition(Z);
                super.mouseWheelMoved(e);
            }
        });
        imp.setPosition(mrStudy.T0.getSize() / 2);
    }

    public void render() {
        Image3DUniverse univ = new Image3DUniverse();
        ContentInstant ci = univ.addVoltex(imp, 1).getCurrent();

        if (ci != null) {
            univ.show();
        }
    }

    private boolean hasROI() {
        return !rois.isEmpty();
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
    //</editor-fold>

    private void doColorMapping() {
        Timer t = new Timer();

        ColorProcessor[] cps = new ColorProcessor[mrStudy.T0.getSize()];
        {
            detail.append("Slice, Washout, Plateau, Persist, RoiA, RoiM\n");

            for (int i = 0; i < cps.length; i++) {
                MR t1 = mrStudy.T1.getImageArrayXY()[i];
                final int width = t1.getWidth();
                final int height = t1.getHeight();
                ByteProcessor bp = NO_BACKGROUND ? null : ImageJUtils.getByteProcessorFromShortImage(t1, t1.getWindowCenter(), t1.getWindowWidth());
                final int z = i + 1;
                List<Roi> roi = ROIUtils.filterROIbySlice(rois, z);

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
                        if ((hasROI() && ROIUtils.withinROI(roi, x, y)) || !hasROI()) {
                            roiVolume++;
                            pixelInROI = true;
                        }

                        final KineticType kt = mapping(initial, peak, delay);
                        final int v = (!NO_BACKGROUND && bp != null) ? bp.getPixel(x, y) / 3 : 0;
                        final Color c = (kt == KineticType.UNMAPPED || kt == KineticType.GLAND)
                                ? (NO_BACKGROUND || bp == null) ? Color.BLACK : new Color(v, v, v)
                                : kt.color;
                        cp.putPixel(x, y, new int[]{c.getRed(), c.getGreen(), c.getBlue()});

                        if (!pixelInROI) {
                            continue;
                        }

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
                }// each pixel
                cps[i] = cp;

                washoutVolume *= resolution;
                plateauVolume *= resolution;
                persistVolume *= resolution;
                enhancedVolume *= resolution;
                roiVolume *= resolution;
                detail.append(z).append(", ");
                detail.append((int) washoutVolume).append(", ");
                detail.append((int) plateauVolume).append(", ");
                detail.append((int) persistVolume).append(", ");
                detail.append((int) enhancedVolume).append(", ");
                detail.append((int) roiVolume).append("\n");

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
        imp = new ImagePlus("", ims);

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

        t.printElapsedTime("ColorMapping");
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
                ret = PLATEAU_RANGE.contains(R2) ? KineticType.FLUID : ret;
            } else if (R1 < -0.2) {
                ret = PLATEAU_RANGE.contains(R2) ? KineticType.EDEMA : ret;
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

    private double getGlandular() {
        MR firstMiddleSlice = mrStudy.T0.getImageArrayXY()[mrStudy.T0.getSize() / 2];
        ImageStatistics is = new ShortStatistics(ImageJUtils.getShortProcessorFromShortImage(firstMiddleSlice));
        // TODO magic number
        int noiseFloor = (int) Math.ceil(is.stdDev * 2.0);
        double glandularNoiseRatio = (noiseFloor > 1000) ? 1.47 : 1.33;
        return glandularNoiseRatio * noiseFloor;
    }

    public enum KineticType {

        GLAND("Glandular", new Color(12, 12, 12)), WASHOUT("Washout", Color.RED), PLATEAU("Plateau", Color.MAGENTA), PERSIST("Persistent", Color.YELLOW), EDEMA("Edema", Color.GREEN), FLUID("Fluid", Color.BLUE), UNMAPPED("Unmapped", null);
        private final String description;
        private final Color color;

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
