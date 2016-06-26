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
import org.apache.commons.lang.math.DoubleRange;

public class ColorMapping {

    private static final boolean NONE_BACKGROUND = true;
    private static int NOISE_FLOOR;
    private static double Glandular_Noise_Ratio;
    private static double Glandular;
    private static final double Initial_Strong_Enhancement = 0.32;
    private final double DELAYED_WASHOUT;
    private final double DELAYED_PLATEAU;
    private final DoubleRange PLATEAU_RANGE;

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

    public ColorMapping(BMRStudy mrs) {
        this(mrs, null, -0.05, 0.05);
    }

    public ColorMapping(BMRStudy mrs, String roiFile, double delayedWashout, double delayedPlateau) {
        mrStudy = mrs;
        rois = IOUtils.fileExisted(roiFile) ? ROIUtils.uncompressROI(roiFile) : new ArrayList<Roi>();
        this.roiFile = roiFile;
        DELAYED_WASHOUT = delayedWashout;
        DELAYED_PLATEAU = delayedPlateau;
        PLATEAU_RANGE = new DoubleRange(DELAYED_WASHOUT, DELAYED_PLATEAU);
        doColorMapping();
    }

    private boolean hasROI() {
        return !rois.isEmpty();
    }

    private void doColorMapping() {
        Timer t = new Timer();

        MR fms = mrStudy.mrs2.getImageArrayXY()[mrStudy.mrs2.getSize() / 2]; // first middle slice
        ImageStatistics is = new ShortStatistics(ImageJUtils.getShortProcessorFromShortImage(fms));
        NOISE_FLOOR = (int) Math.ceil(is.stdDev * 2.0);
        Glandular_Noise_Ratio = (NOISE_FLOOR > 1000) ? 1.47 : 1.33;
        Glandular = Glandular_Noise_Ratio * NOISE_FLOOR;
//        System.out.println("NOISE_FLOOR: " + NOISE_FLOOR);
//        System.out.println("Glandular_Noise_Ratio: " + Glandular_Noise_Ratio);
//        System.out.println("Glandular: " + Glandular);

        ColorProcessor[] cps = new ColorProcessor[mrStudy.mrs2.getSize()];

        if (hasROI()) {
            result.append("Slice\tRed\tPink\tYellow\tGreen\tBlue\tRoiA\tRoiM\n");
            result.append("---------------------------------------------------------------\n");
        }

        for (int i = 0; i < cps.length; i++) {
            MR t1 = mrStudy.mrs3.getImageArrayXY()[i];
            final int width = t1.getWidth();
            final int height = t1.getHeight();
            ColorProcessor cp = new ColorProcessor(width, height);
            ByteProcessor bp = NONE_BACKGROUND ? null : ImageJUtils.getByteProcessorFromShortImage(t1, t1.getWindowCenter(), t1.getWindowWidth());

            final int z = i + 1;
            double enhancedArea = 0;
            double roiArea = 0;
            double redArea = 0;
            double magentaArea = 0;
            double yellowArea = 0;
            double blueArea = 0;
            double greenArea = 0;
            List<Roi> roi = ROIUtils.filterROI(rois, z);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    int initial = mrStudy.getPixel(x, y, i, 0);
                    int peak = mrStudy.getPixel(x, y, i, 1);
                    int delay = mrStudy.getPixel(x, y, i, 2);
                    int r, g, b;

                    if (!NONE_BACKGROUND) {
                        r = g = b = (bp != null) ? (t1.getPixel(x, y) < 1000 ? 0 : bp.getPixel(x, y)) : 0;
                        cp.putPixel(x, y, new int[]{r, g, b});
                    }

                    boolean pixelInROI = false;
                    if (hasROI() && ROIUtils.withinROI(roi, x, y)) {
                        roiArea++;
                        pixelInROI = true;
                    }

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
            cps[i] = cp;

            if (hasROI()) {
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
        } // each slice

        ImageStack ims = new ImageStack(mrStudy.mrs3.getImageArrayXY()[0].getWidth(), mrStudy.mrs3.getImageArrayXY()[0].getHeight());
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

    private static double initialPhase(int initial, int peak) {
        return (double) (peak - initial) / (double) initial;
    }

    private static double delayPhase(int initial, int delay) {
        return (double) (delay - initial) / (double) initial;
    }

    private int mapping(int initial, int peak, int delay) {
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
                if (R2 < DELAYED_WASHOUT) {
                    return 1; // Washout
                } else if (R2 < DELAYED_PLATEAU) {
                    return 2; // Plateau
                } else {
                    return 3; // Persistent
                }
            }

            return 0; // Enhanced
        }

        return 6; // Unmapped (Noise)
    }

    private String mappingDesc(int initial, int peak, int delay) {
        switch (mapping(initial, peak, delay)) {
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

    private String colorMappingInfo(int x, int y, int z) {
        final short initial = mrStudy.getPixel(x, y, z, 0);
        final short peak = mrStudy.getPixel(x, y, z, 1);
        final short delay = mrStudy.getPixel(x, y, z, 2);
        final double R1 = ColorMapping.initialPhase(initial, peak);
        final double R2 = ColorMapping.delayPhase(initial, delay) - R1;
        return String.format("(%03d,%03d,%03d) = %04d -> %04d -> %04d, R1=%s, R2=%s, %s", x, y, z + 1, initial, peak, delay, df.format(R1), df.format(R2), mappingDesc(initial, peak, delay));
    }

    public void save() throws IOException {
        FileSaver fs = new FileSaver(imp);
        fs.saveAsTiffStack(mrStudy.studyRoot + "/cm.tif");
        FileUtils.writeStringToFile(new File(mrStudy.studyRoot + "/result.txt"), result.toString());
    }

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
        imp.setPosition(mrStudy.mrs2.getSize() / 2);
    }

    public void render() {
        Image3DUniverse univ = new Image3DUniverse();
        ContentInstant ci = univ.addVoltex(imp, 1).getCurrent();

        if (ci != null) {
            univ.show();
        }
    }

    public int getColorMapping(int x, int y, int z) {
        return mapping(mrStudy.getPixel(x, y, z, 0), mrStudy.getPixel(x, y, z, 1), mrStudy.getPixel(x, y, z, 2));
    }

    public static void main(String[] args) {
        File studyRoot = new File(ColorMapping.class.getClassLoader().getResource("resources/bmr/").getFile());
        final ColorMapping cm = new ColorMapping(new BMRStudy(studyRoot.toPath()));
        cm.show();
        cm.render();
    }
}
