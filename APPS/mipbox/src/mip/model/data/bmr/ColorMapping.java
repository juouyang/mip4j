package mip.model.data.bmr;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import mip.model.data.image.MR;
import mip.util.IOUtils;
import mip.util.ImageJUtils;
import mip.util.ROIUtils;
import mip.util.Timer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.DoubleRange;

public class ColorMapping {

    private static final int CONTRAST = 2692;
    private static int NOISE_FLOOR;
    private static double Glandular_Noise_Ratio;
    private static double Glandular;
    private static final double Initial_Strong_Enhancement = 0.32;
    private static final double DELAYED_Washout = -0.05;
    private static final double DELAYED_Plateau = 0.05;

    private static final DoubleRange PLATEAU_RANGE = new DoubleRange(DELAYED_Washout, DELAYED_Plateau);
    private static final DecimalFormat df = new DecimalFormat(" 00.00;-00.00");

    private final BMRStudy mrStudy;
    public ImagePlus imp;

    private final List<Roi> rois;
    private final String roiFile;
    public double washoutTotal = 0;
    public double plateauTotal = 0;
    public double persistentTotal = 0;
    public double edemaTotal = 0;
    public double fluidTotal = 0;
    public double enhancedTotal = 0;
    public double roiTotal = 0;
    public StringBuffer result = new StringBuffer();

    public ColorMapping(BMRStudy mrs, String roiFile) {
        mrStudy = mrs;
        rois = IOUtils.fileExisted(roiFile) ? ROIUtils.uncompressROI(roiFile) : new ArrayList<Roi>();
        this.roiFile = roiFile;
        colorMapping();
    }

    public boolean hasROI() {
        return !rois.isEmpty();
    }

    private void colorMapping() {
        Timer t = new Timer();

        MR fms = mrStudy.mrs2.getImageArrayXY()[mrStudy.mrs2.getLength() / 2]; // first middle slice
        ImageStatistics is = new ShortStatistics(ImageJUtils.getShortProcessorFromShortImage(fms));
        NOISE_FLOOR = (int) Math.ceil(is.stdDev * 2.0);
        Glandular_Noise_Ratio = (NOISE_FLOOR > 1000) ? 1.47 : 1.33;
        Glandular = Glandular_Noise_Ratio * NOISE_FLOOR;
//        System.out.println("NOISE_FLOOR: " + NOISE_FLOOR);
//        System.out.println("Glandular_Noise_Ratio: " + Glandular_Noise_Ratio);
//        System.out.println("Glandular: " + Glandular);

        ColorProcessor[] cps = new ColorProcessor[mrStudy.mrs2.getLength()];

        if (hasROI()) {
            result.append("Slice\tRed\tPink\tYellow\tGreen\tBlue\tRoiA\tRoiM\n");
            result.append("---------------------------------------------------------------\n");
        }

        for (int i = 0; i < cps.length; i++) {
            MR t0 = mrStudy.mrs2.getImageArrayXY()[i];
            MR t1 = mrStudy.mrs3.getImageArrayXY()[i];
            MR t2 = mrStudy.mrs4.getImageArrayXY()[i];
            final int width = t1.getWidth();
            final int height = t1.getHeight();
            ColorProcessor cp = new ColorProcessor(width, height);
            ByteProcessor bp = ImageJUtils.getByteProcessorFromShortImage(t1, t1.getWindowCenter(), t1.getWindowWidth());

            final int z = i + 1;
            double enhancedArea = 0;
            double roiArea = 0;
            double redArea = 0;
            double magentaArea = 0;
            double yellowArea = 0;
            double blueArea = 0;
            double greenArea = 0;

            List<Roi> roi = ROIUtils.filterROI(rois, z);
            boolean hasROI = (!roi.isEmpty());

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean pixelInROI = false;
                    if (hasROI && ROIUtils.withinROI(roi, x, y)) {
                        roiArea++;
                        pixelInROI = true;
                    }

                    int initial = getSignalIntensity(t0, x, y);
                    int peak = getSignalIntensity(t1, x, y);
                    int delay = getSignalIntensity(t2, x, y);

                    int r = bp.getPixel(x, y);
                    int g = r;
                    int b = r;
                    cp.putPixel(x, y, new int[]{r, g, b});

                    final int c = mapping(initial, peak, delay);
                    switch (c) {
                        case 1:
                            redArea += pixelInROI ? 1 : 0;
                            r = 255;
                            g = 0;
                            b = 0;
                            break;
                        case 2:
                            magentaArea += pixelInROI ? 1 : 0;
                            r = 255;
                            g = 102;
                            b = 255;
                            break;
                        case 3:
                            yellowArea += pixelInROI ? 1 : 0;
                            r = 255;
                            g = 255;
                            b = 0;
                            break;
                        case 4:
                            blueArea += pixelInROI ? 1 : 0;
                            r = 0;
                            g = 0;
                            b = 255;
                            break;
                        case 5:
                            greenArea += pixelInROI ? 1 : 0;
                            r = 0;
                            g = 255;
                            b = 0;
                            break;
                        default:
                            continue;
                    }
                    enhancedArea += (pixelInROI && (c >= 0 && c <= 3)) ? 1 : 0;
                    cp.putPixel(x, y, new int[]{r, g, b});
                }
            }// each pixel

            if (hasROI) {
                // TODO change 0.49
                redArea *= 0.49;
                magentaArea *= 0.49;
                yellowArea *= 0.49;
                greenArea *= 0.49;
                blueArea *= 0.49;
                enhancedArea *= 0.49;
                roiArea *= 0.49;

                result.append(z).append("\t");
                result.append(df.format(redArea)).append("\t");
                result.append(df.format(magentaArea)).append("\t");
                result.append(df.format(yellowArea)).append("\t");
                result.append(df.format(greenArea)).append("\t");
                result.append(df.format(blueArea)).append("\t");
                result.append(df.format(enhancedArea)).append("\t");
                result.append(df.format(roiArea)).append("\n");

                // TODO change 1.125
                washoutTotal += redArea * 1.125;
                plateauTotal += magentaArea * 1.125;
                persistentTotal += yellowArea * 1.125;
                edemaTotal += greenArea * 1.125;
                fluidTotal += blueArea * 1.125;
                enhancedTotal += enhancedArea * 1.125;
                roiTotal += roiArea * 1.125;
            }

            cps[i] = cp;
        } // each slice

        ImageStack ims = new ImageStack(512, 512);
        for (ColorProcessor cp : cps) {
            ims.addSlice(cp);
        }
        imp = new ImagePlus("", ims);

        if (hasROI()) {
            result.append("Total\t").append(df.format(washoutTotal)).append("\t");
            result.append(df.format(plateauTotal)).append("\t");
            result.append(df.format(persistentTotal)).append("\t");
            result.append(df.format(edemaTotal)).append("\t");
            result.append(df.format(fluidTotal)).append("\t");
            result.append(df.format(enhancedTotal)).append("\t");
            result.append(df.format(roiTotal)).append("\n");
            result.append("---------------------------------------------------------------\n");
            result.append("RatioA\t").append(df.format(washoutTotal / enhancedTotal)).append("\t");
            result.append(df.format(plateauTotal / enhancedTotal)).append("\t");
            result.append(df.format(persistentTotal / enhancedTotal)).append("\t");
            result.append(df.format(edemaTotal / enhancedTotal)).append("\t");
            result.append(df.format(fluidTotal / enhancedTotal)).append("\t");
            result.append(df.format(enhancedTotal / enhancedTotal)).append("\t");
            result.append(df.format(roiTotal / enhancedTotal)).append("\n");
            result.append("RatioM\t").append(df.format(washoutTotal / roiTotal)).append("\t");
            result.append(df.format(plateauTotal / roiTotal)).append("\t");
            result.append(df.format(persistentTotal / roiTotal)).append("\t");
            result.append(df.format(edemaTotal / roiTotal)).append("\t");
            result.append(df.format(fluidTotal / roiTotal)).append("\t");
            result.append(df.format(enhancedTotal / roiTotal)).append("\t");
            result.append(df.format(roiTotal / roiTotal)).append("\n");
        }

        t.printElapsedTime("ColorMapping");
    }

    public void save() throws IOException {
        FileSaver fs = new FileSaver(imp);
        fs.saveAsTiffStack(mrStudy.studyRoot + "/cm.tif");
        FileUtils.writeStringToFile(new File(mrStudy.studyRoot + "/result.txt"), result.toString());
    }

    public void show() {
        ImageJ ij = new ImageJ();
        ij.exitWhenQuitting(true);

        new Opener().openZip(roiFile);
        imp.show();
        imp.getCanvas().addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {

                final int Z = imp.getCurrentSlice() - 1;
                final int X = imp.getCanvas().getCursorLoc().x;
                final int Y = imp.getCanvas().getCursorLoc().y;

                imp.setTitle(colorMappingInfo(X, Y, Z));
                super.mouseMoved(e);
            }

        });
        imp.getCanvas().addMouseWheelListener(new MouseAdapter() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                final int Z = imp.getCurrentSlice() - 1;
                final int X = imp.getCanvas().getCursorLoc().x;
                final int Y = imp.getCanvas().getCursorLoc().y;

                imp.setTitle(colorMappingInfo(X, Y, Z));
                super.mouseWheelMoved(e);
            }
        });
        imp.setPosition(mrStudy.numberOfFrames / 2);
    }

    private String colorMappingInfo(int x, int y, int z) {
        final short initial = getSignalIntensity(mrStudy.mrs2.getImageArrayXY()[z], x, y);
        final short peak = getSignalIntensity(mrStudy.mrs3.getImageArrayXY()[z], x, y);
        final short delay = getSignalIntensity(mrStudy.mrs4.getImageArrayXY()[z], x, y);
        final double R1 = ColorMapping.initialPhase(initial, peak);
        final double R2 = ColorMapping.delayPhase(initial, delay) - R1;
        return String.format("(%03d,%03d,%03d) = %04d -> %04d -> %04d, R1=%s, R2=%s, %s", x, y, z + 1, initial, peak, delay, df.format(R1), df.format(R2), mappingDesc(initial, peak, delay));
    }

    private static short getSignalIntensity(MR mr, int x, int y) {
        return mr.getPixel(x, y);
    }

    private static int mapping(int initial, int peak, int delay) {
        if (initial > Glandular/* || peak > CONTRAST*/) {
            double R1 = initialPhase(initial, peak);
            double R2 = delayPhase(initial, delay) - R1;
            if (PLATEAU_RANGE.containsDouble(R2)) {
                if (R1 < -0.4) {
                    return 4; // Fluid
                } else if (R1 < -0.2) {
                    return 5; // Edema
                }
            }

            if (R1 > Initial_Strong_Enhancement) {
                if (R2 < DELAYED_Washout) {
                    return 1; // Washout
                } else if (R2 < DELAYED_Plateau) {
                    return 2; // Plateau
                } else {
                    return 3; // Persistent
                }
            }

            return 0; // Enhanced
        }

        return 6; // Unmapped (Noise)
    }

    private static String mappingDesc(int initial, int peak, int delay) {
        switch (ColorMapping.mapping(initial, peak, delay)) {
            case 0:
                return "Enhanced";
            case 1:
                return "Washout";
            case 2:
                return "Plateau";
            case 3:
                return "Persistent";
            case 4:
                return "Fluid";
            case 5:
                return "Edema";
            default:
                return "Unmapped";
        }
    }

    private static double initialPhase(int initial, int peak) {
        return (double) (peak - initial) / (double) initial;
    }

    private static double delayPhase(int initial, int delay) {
        return (double) (delay - initial) / (double) initial;
    }
}
