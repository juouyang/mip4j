package mip.util;

import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.util.List;

import de.lmu.ifi.dbs.jfeaturelib.Descriptor.Supports;
import de.lmu.ifi.dbs.jfeaturelib.edgeDetector.Canny;
import de.lmu.ifi.dbs.jfeaturelib.edgeDetector.DroG;
import de.lmu.ifi.dbs.jfeaturelib.edgeDetector.Kernel;
import de.lmu.ifi.dbs.jfeaturelib.edgeDetector.Roberts;
import de.lmu.ifi.dbs.jfeaturelib.features.AutoColorCorrelogram;
import de.lmu.ifi.dbs.jfeaturelib.features.ColorHistogram;
import de.lmu.ifi.dbs.jfeaturelib.features.Haralick;
import de.lmu.ifi.dbs.jfeaturelib.features.Moments;
import de.lmu.ifi.dbs.jfeaturelib.features.PHOG;
import de.lmu.ifi.dbs.jfeaturelib.features.ReferenceColorSimilarity;
import de.lmu.ifi.dbs.jfeaturelib.pointDetector.FASTCornerDetector;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.AdaptiveGridResolution;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.CentroidBoundaryDistance;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.CentroidFeature;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.Compactness;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.Eccentricity;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.ExtremalPoints;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.PolygonEvolution;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.Profiles;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.SquareModelShapeMatrix;

public class JFeatureLibUtils {

    private JFeatureLibUtils() { // singleton
    }

    public static void main(String[] args) {
        System.out.println(new AutoColorCorrelogram().supports().contains(Supports.DOES_16));
        System.out.println(new ColorHistogram().supports().contains(Supports.DOES_16));
        System.out.println(new Moments().supports().contains(Supports.DOES_16));
        System.out.println(new PHOG().supports().contains(Supports.DOES_16));
        System.out.println(new ReferenceColorSimilarity().supports().contains(Supports.DOES_16));

        System.out.println(new Canny().supports().contains(Supports.DOES_16));
        System.out.println(new DroG().supports().contains(Supports.DOES_16));
        System.out.println(new Kernel().supports().contains(Supports.DOES_16));
        System.out.println(new Roberts().supports().contains(Supports.DOES_16));

        System.out.println(new FASTCornerDetector().supports().contains(Supports.DOES_16));

        System.out.println(new AdaptiveGridResolution(1).supports().contains(Supports.DOES_16));
        System.out.println(new CentroidBoundaryDistance().supports().contains(Supports.DOES_16));
        System.out.println(new CentroidFeature().supports().contains(Supports.DOES_16));
        System.out.println(new Compactness().supports().contains(Supports.DOES_16));
        System.out.println(new Eccentricity().supports().contains(Supports.DOES_16));
        System.out.println(new ExtremalPoints().supports().contains(Supports.DOES_16));
        System.out.println(new PolygonEvolution().supports().contains(Supports.DOES_16));
        System.out.println(new Profiles().supports().contains(Supports.DOES_16));
        System.out.println(new SquareModelShapeMatrix().supports().contains(Supports.DOES_16));
    }

    public static double[] getPHOG(ImageProcessor ip, int bins, int recursions, Roi roi) {
        PHOG phog = new PHOG();

        phog.setBins(bins);
        phog.setRecursions(recursions);

        ip.setRoi(roi);
        phog.run(ip.crop());
        List<double[]> fs = phog.getFeatures();

        if (fs.size() > 1) {
            throw new AssertionError();
        }

        return fs.get(0);
    }

    public static double[] getGLCM(ImageProcessor ip, Roi roi) {
        Haralick glcm = new Haralick();

        ip.setRoi(roi);
        glcm.run(ip.crop());
        List<double[]> fs = glcm.getFeatures();

        if (fs.size() > 1) {
            throw new AssertionError();
        }

        return fs.get(0);
    }
}
