package mip.model.data.bmr;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortStatistics;

import java.text.DecimalFormat;
import java.util.ArrayList;

import mip.model.data.image.MR;
import mip.util.ImageJUtils;
import mip.util.ROIUtils;
import mip.util.Timer;
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

    public double washoutTotal = 0;
    public double plateauTotal = 0;
    public double persistentTotal = 0;
    public double edemaTotal = 0;
    public double fluidTotal = 0;
    public double enhancedTotal = 0;
    public double roiTotal = 0;
    public StringBuffer result = new StringBuffer();
    public StringBuffer result_all = new StringBuffer();

    public BMRStudy mrStudy;
    public ImagePlus imp;

    public ColorMapping(BMRStudy mrs) {
        Timer t = new Timer();
        MR fms = mrs.mrs2.getImageArrayXY()[mrs.mrs2.getLength() / 2]; // first middle slice
        ImageStatistics is = new ShortStatistics(ImageJUtils.getShortProcessorFromShortImage(fms));
        NOISE_FLOOR = (int) Math.ceil(is.stdDev * 2.0);
        Glandular_Noise_Ratio = (NOISE_FLOOR > 1000) ? 1.47 : 1.33;
        Glandular = Glandular_Noise_Ratio * NOISE_FLOOR;
        System.out.println("NOISE_FLOOR: " + NOISE_FLOOR); // TODO log4j
        System.out.println("Glandular_Noise_Ratio: " + Glandular_Noise_Ratio); // TODO log4j
        System.out.println("Glandular: " + Glandular); // TODO log4j

        mrStudy = mrs;
        ColorProcessor[] cps = new ColorProcessor[mrs.mrs2.getLength()];

        result.append("Slice\tRed\tPink\tYellow\tGreen\tBlue\tRoiA\tRoiM\n");
        result.append("---------------------------------------------------------------\n");
        for (int i = 0; i < cps.length; i++) { // per slice
            MR mr2 = mrs.mrs2.getImageArrayXY()[i];
            MR mr3 = mrs.mrs3.getImageArrayXY()[i];
            MR mr4 = mrs.mrs4.getImageArrayXY()[i];
            ColorProcessor cp = new ColorProcessor(mr3.getWidth(), mr3.getHeight());
            ByteProcessor bp = ImageJUtils.getByteProcessorFromShortImage(mr3, mr3.getWindowCenter(), mr3.getWindowWidth());
            ArrayList<Roi> roi = ROIUtils.filterROI(mrs.roi, i + 1);
            int z = i + 1;
            double enhancedArea = 0;
            double roiArea = 0;
            double redArea = 0;
            double magentaArea = 0;
            double yellowArea = 0;
            double blueArea = 0;
            double greenArea = 0;
            boolean hasROI = (!roi.isEmpty());

            for (int y = 0; y < cp.getHeight(); y++) {
                for (int x = 0; x < cp.getWidth(); x++) { // per pixel
                    boolean pixelInROI = false;
                    if (ROIUtils.withinROI(roi, x, y)) {
                        roiArea++;
                        pixelInROI = true;
                    }

                    int initial = getSignalIntensity(mr2, x, y);
                    int peak = getSignalIntensity(mr3, x, y);
                    int delay = getSignalIntensity(mr4, x, y);

                    int r = bp.getPixel(x, y);
                    int g = r;
                    int b = r;
                    cp.putPixel(x, y, new int[]{r, g, b});

                    final int c = mapping(initial, peak, delay);
                    switch (c) {
                        case 1:
                            if (pixelInROI) {
                                redArea++;
                            }
                            r = 255;
                            g = 0;
                            b = 0;
                            break;
                        case 2:
                            if (pixelInROI) {
                                magentaArea++;
                            }
                            r = 255;
                            g = 102;
                            b = 255;
                            break;
                        case 3:
                            if (pixelInROI) {
                                yellowArea++;
                            }
                            r = 255;
                            g = 255;
                            b = 0;
                            break;
                        case 4:
                            if (pixelInROI) {
                                blueArea++;
                            }
                            r = 0;
                            g = 0;
                            b = 255;
                            break;
                        case 5:
                            if (pixelInROI) {
                                greenArea++;
                            }
                            r = 0;
                            g = 255;
                            b = 0;
                            break;
                        default:
                            continue;
                    }

                    if (pixelInROI && (c >= 0 && c <= 3)) {
                        enhancedArea++;
                    }
                    cp.putPixel(x, y, new int[]{r, g, b});
                }
            }// per pixel

            redArea *= 0.49;
            magentaArea *= 0.49;
            yellowArea *= 0.49;
            greenArea *= 0.49;
            blueArea *= 0.49;
            enhancedArea *= 0.49;
            roiArea *= 0.49;
            if (hasROI) {
                result.append(i + 1).append("\t").append(df.format(redArea)).append("\t").append(df.format(magentaArea)).append("\t").append(df.format(yellowArea)).append("\t").append(df.format(greenArea)).append("\t").append(df.format(blueArea)).append("\t").append(df.format(enhancedArea)).append("\t").append(df.format(roiArea)).append("\n");
                result_all.append(mrs.patientID).append("_").append(mrs.patientID).append("\t");
                result_all.append(i + 1).append("\t").append(df.format(redArea)).append("\t").append(df.format(magentaArea)).append("\t").append(df.format(yellowArea)).append("\t").append(df.format(greenArea)).append("\t").append(df.format(blueArea)).append("\t").append(df.format(enhancedArea)).append("\t").append(df.format(roiArea)).append("\n");
            }

            washoutTotal += redArea * 1.125;
            plateauTotal += magentaArea * 1.125;
            persistentTotal += yellowArea * 1.125;
            edemaTotal += greenArea * 1.125;
            fluidTotal += blueArea * 1.125;
            enhancedTotal += enhancedArea * 1.125;
            roiTotal += roiArea * 1.125;

            cps[i] = cp;
        }

        result.append("Total\t").append(df.format(washoutTotal)).append("\t").append(df.format(plateauTotal)).append("\t").append(df.format(persistentTotal)).append("\t").append(df.format(edemaTotal)).append("\t").append(df.format(fluidTotal)).append("\t").append(df.format(enhancedTotal)).append("\t").append(df.format(roiTotal)).append("\n");
        result.append("---------------------------------------------------------------\n");
        result.append("RatioA\t").append(df.format(washoutTotal / enhancedTotal)).append("\t").append(df.format(plateauTotal / enhancedTotal)).append("\t").append(df.format(persistentTotal / enhancedTotal)).append("\t").append(df.format(edemaTotal / enhancedTotal)).append("\t").append(df.format(fluidTotal / enhancedTotal)).append("\t").append(df.format(enhancedTotal / enhancedTotal)).append("\t").append(df.format(roiTotal / enhancedTotal)).append("\n");
        result.append("RatioM\t").append(df.format(washoutTotal / roiTotal)).append("\t").append(df.format(plateauTotal / roiTotal)).append("\t").append(df.format(persistentTotal / roiTotal)).append("\t").append(df.format(edemaTotal / roiTotal)).append("\t").append(df.format(fluidTotal / roiTotal)).append("\t").append(df.format(enhancedTotal / roiTotal)).append("\t").append(df.format(roiTotal / roiTotal)).append("\n");

        ImageStack ims = new ImageStack(512, 512);
        for (ColorProcessor cp : cps) {
            ims.addSlice(cp);
        }
        imp = new ImagePlus("", ims);
        t.printElapsedTime("ColorMapping");
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

    public String colorMappingInfo(int x, int y, int z) {
        final short initial = getSignalIntensity(mrStudy.mrs2.getImageArrayXY()[z], x, y);
        final short peak = getSignalIntensity(mrStudy.mrs3.getImageArrayXY()[z], x, y);
        final short delay = getSignalIntensity(mrStudy.mrs4.getImageArrayXY()[z], x, y);
        final double R1 = ColorMapping.initialPhase(initial, peak);
        final double R2 = ColorMapping.delayPhase(initial, delay) - R1;
        return String.format("(%03d,%03d,%03d) = %04d -> %04d -> %04d, R1=%s, R2=%s, %s", x, y, z + 1, initial, peak, delay, df.format(R1), df.format(R2), mappingDesc(initial, peak, delay));
    }

    private static double initialPhase(int initial, int peak) {
        return (double) (peak - initial) / (double) initial;
    }

    private static double delayPhase(int initial, int delay) {
        return (double) (delay - initial) / (double) initial;
    }
}
