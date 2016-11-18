package mip.data.descriptor.process3d;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;

import java.util.ArrayList;
import java.util.List;

import weka.core.Attribute;
import weka.core.Instances;

public class HOG {

    private static final int ORIENT_NBINS_H = 6;
    private static final int ORIENT_NBINS_V = 4;
    private static final double ORIENT_DELTA_H = Math.PI / ORIENT_NBINS_H;
    private static final double ORIENT_DELTA_V = Math.PI / ORIENT_NBINS_V;
    public static final int FEATURE_SIZE = 8 * ORIENT_NBINS_H * ORIENT_NBINS_V;
    public static final int RESULT_FEATURE_SIZE = FEATURE_SIZE * 3 + 1;

    public static final int width = 16, halfW = width / 2;
    public static final int height = 16, halfH = height / 2;
    public static final int length = 16, halfL = length / 2;

    public static void main(String[] args) {

    }

    public static Instances getFeatureStructure() {

        ArrayList<Attribute> attributes = new ArrayList<>();

        for (int i = 1; i <= FEATURE_SIZE; i++) {
            attributes.add(new Attribute("T0.Bin" + String.format("%03d", i)));
        }
        for (int i = 1; i <= FEATURE_SIZE; i++) {
            attributes.add(new Attribute("T1.Bin" + String.format("%03d", i)));
        }
        for (int i = 1; i <= FEATURE_SIZE; i++) {
            attributes.add(new Attribute("T2.Bin" + String.format("%03d", i)));
        }

        List<String> classAttribute = new ArrayList<>();

        classAttribute.add("positive");
        classAttribute.add("negative");
        attributes.add(new Attribute("class", classAttribute));

        return new Instances("descriptor", attributes, 0);
    }

    public static double[] extract(ImagePlus magnitude, ImagePlus orientationH, ImagePlus orientationV, int x, int y, int z) {

        double[] ret = new double[FEATURE_SIZE];

        int sub = -1;
        ImageStatistics is0 = new StackStatistics(Crop.performCrop(magnitude, x - halfW, x, y - halfH, y, z - halfL, z, false));
        ImageStatistics is1 = new StackStatistics(Crop.performCrop(magnitude, x, x + halfW, y - halfH, y, z - halfL, z, false));
        ImageStatistics is2 = new StackStatistics(Crop.performCrop(magnitude, x - halfW, x, y, y + halfH, z - halfL, z, false));
        ImageStatistics is3 = new StackStatistics(Crop.performCrop(magnitude, x, x + halfW, y, y + halfH, z - halfL, z, false));

        ImageStatistics is4 = new StackStatistics(Crop.performCrop(magnitude, x - halfW, x, y - halfH, y, z, z + halfL, false));
        ImageStatistics is5 = new StackStatistics(Crop.performCrop(magnitude, x, x + halfW, y - halfH, y, z, z + halfL, false));
        ImageStatistics is6 = new StackStatistics(Crop.performCrop(magnitude, x - halfW, x, y, y + halfH, z, z + halfL, false));
        ImageStatistics is7 = new StackStatistics(Crop.performCrop(magnitude, x, x + halfW, y, y + halfH, z, z + halfL, false));

        assert (is0.min >= 0);
        assert (is1.min >= 0);
        assert (is2.min >= 0);
        assert (is3.min >= 0);
        assert (is4.min >= 0);
        assert (is5.min >= 0);
        assert (is6.min >= 0);
        assert (is7.min >= 0);

        for (int zz = z - halfL; zz < z; zz++) {
            assert (zz >= 1 && zz <= magnitude.getImageStackSize());

            FloatProcessor ipM = (FloatProcessor) magnitude.getImageStack().getProcessor(zz + 1);
            FloatProcessor ipH = (FloatProcessor) orientationH.getImageStack().getProcessor(zz + 1);
            FloatProcessor ipV = (FloatProcessor) orientationV.getImageStack().getProcessor(zz + 1);

            for (int yy = y - halfH; yy < y; yy++) {
                assert (yy >= 0 && zz < magnitude.getHeight());
                sub = 0;
                for (int xx = x - halfW; xx < x; xx++) {
                    assert (xx >= 0 && xx < magnitude.getWidth());

                    float mag = ipM.getf(xx, yy);
                    float oriH = ipH.getf(xx, yy);
                    float oriV = ipV.getf(xx, yy);

                    int binH = binH(oriH);
                    int binV = binV(oriV);

                    ret[sub * ORIENT_NBINS_H * ORIENT_NBINS_V + binH * ORIENT_NBINS_V + binV] += (mag - is0.min) / (is0.max - is0.min);
                }
                sub = 1;
                for (int xx = x; xx < x + halfW; xx++) {
                    assert (xx >= 0 && xx < magnitude.getWidth());

                    float mag = ipM.getf(xx, yy);
                    float oriH = ipH.getf(xx, yy);
                    float oriV = ipV.getf(xx, yy);

                    int binH = binH(oriH);
                    int binV = binV(oriV);

                    ret[sub * ORIENT_NBINS_H * ORIENT_NBINS_V + binH * ORIENT_NBINS_V + binV] += (mag - is1.min) / (is1.max - is1.min);
                }
            }

            for (int yy = y; yy < y + halfH; yy++) {
                assert (yy >= 0 && zz < magnitude.getHeight());
                sub = 2;
                for (int xx = x - halfW; xx < x; xx++) {
                    assert (xx >= 0 && xx < magnitude.getWidth());

                    float mag = ipM.getf(xx, yy);
                    float oriH = ipH.getf(xx, yy);
                    float oriV = ipV.getf(xx, yy);

                    int binH = binH(oriH);
                    int binV = binV(oriV);

                    ret[sub * ORIENT_NBINS_H * ORIENT_NBINS_V + binH * ORIENT_NBINS_V + binV] += (mag - is2.min) / (is2.max - is2.min);
                }
                sub = 3;
                for (int xx = x; xx < x + halfW; xx++) {
                    assert (xx >= 0 && xx < magnitude.getWidth());

                    float mag = ipM.getf(xx, yy);
                    float oriH = ipH.getf(xx, yy);
                    float oriV = ipV.getf(xx, yy);

                    int binH = binH(oriH);
                    int binV = binV(oriV);

                    ret[sub * ORIENT_NBINS_H * ORIENT_NBINS_V + binH * ORIENT_NBINS_V + binV] += (mag - is3.min) / (is3.max - is3.min);
                }
            }

        }

        for (int zz = z; zz < z + halfL; zz++) {
            assert (zz >= 1 && zz <= magnitude.getImageStackSize());

            FloatProcessor ipM = (FloatProcessor) magnitude.getImageStack().getProcessor(zz + 1);
            FloatProcessor ipH = (FloatProcessor) orientationH.getImageStack().getProcessor(zz + 1);
            FloatProcessor ipV = (FloatProcessor) orientationV.getImageStack().getProcessor(zz + 1);

            for (int yy = y - halfH; yy < y; yy++) {
                assert (yy >= 0 && zz < magnitude.getHeight());
                sub = 4;
                for (int xx = x - halfW; xx < x; xx++) {
                    assert (xx >= 0 && xx < magnitude.getWidth());

                    float mag = ipM.getf(xx, yy);
                    float oriH = ipH.getf(xx, yy);
                    float oriV = ipV.getf(xx, yy);

                    int binH = binH(oriH);
                    int binV = binV(oriV);

                    ret[sub * ORIENT_NBINS_H * ORIENT_NBINS_V + binH * ORIENT_NBINS_V + binV] += (mag - is4.min) / (is4.max - is4.min);
                }
                sub = 5;
                for (int xx = x; xx < x + halfW; xx++) {
                    assert (xx >= 0 && xx < magnitude.getWidth());

                    float mag = ipM.getf(xx, yy);
                    float oriH = ipH.getf(xx, yy);
                    float oriV = ipV.getf(xx, yy);

                    int binH = binH(oriH);
                    int binV = binV(oriV);

                    ret[sub * ORIENT_NBINS_H * ORIENT_NBINS_V + binH * ORIENT_NBINS_V + binV] += (mag - is5.min) / (is5.max - is5.min);
                }
            }

            for (int yy = y; yy < y + halfH; yy++) {
                assert (yy >= 0 && zz < magnitude.getHeight());
                sub = 6;
                for (int xx = x - halfW; xx < x; xx++) {
                    assert (xx >= 0 && xx < magnitude.getWidth());

                    float mag = ipM.getf(xx, yy);
                    float oriH = ipH.getf(xx, yy);
                    float oriV = ipV.getf(xx, yy);

                    int binH = binH(oriH);
                    int binV = binV(oriV);

                    ret[sub * ORIENT_NBINS_H * ORIENT_NBINS_V + binH * ORIENT_NBINS_V + binV] += (mag - is6.min) / (is6.max - is6.min);
                }
                sub = 7;
                for (int xx = x; xx < x + halfW; xx++) {
                    assert (xx >= 0 && xx < magnitude.getWidth());

                    float mag = ipM.getf(xx, yy);
                    float oriH = ipH.getf(xx, yy);
                    float oriV = ipV.getf(xx, yy);

                    int binH = binH(oriH);
                    int binV = binV(oriV);

                    ret[sub * ORIENT_NBINS_H * ORIENT_NBINS_V + binH * ORIENT_NBINS_V + binV] += (mag - is7.min) / (is7.max - is7.min);
                }
            }
        }
        return ret;
    }

    private static int binH(float oriH) {
        int binH = (int) (oriH / ORIENT_DELTA_H);
        binH = (binH == ORIENT_NBINS_H) ? ORIENT_NBINS_H - 1 : binH;
        assert (binH >= 0 && binH < ORIENT_NBINS_H);
        return binH;
    }

    private static int binV(float oriV) {
        int binV = (int) (oriV / ORIENT_DELTA_V);
        binV = (binV == ORIENT_NBINS_V) ? ORIENT_NBINS_V - 1 : binV;
        assert (binV >= 0 && binV < ORIENT_NBINS_V);
        return binV;
    }
}
