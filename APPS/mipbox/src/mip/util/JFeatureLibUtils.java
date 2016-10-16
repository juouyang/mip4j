package mip.util;

import de.lmu.ifi.dbs.jfeaturelib.Descriptor;
import de.lmu.ifi.dbs.jfeaturelib.Descriptor.Supports;
import de.lmu.ifi.dbs.jfeaturelib.edgeDetector.Canny;
import de.lmu.ifi.dbs.jfeaturelib.edgeDetector.DroG;
import de.lmu.ifi.dbs.jfeaturelib.edgeDetector.Kernel;
import de.lmu.ifi.dbs.jfeaturelib.edgeDetector.Roberts;
import de.lmu.ifi.dbs.jfeaturelib.features.AutoColorCorrelogram;
import de.lmu.ifi.dbs.jfeaturelib.features.ColorHistogram;
import de.lmu.ifi.dbs.jfeaturelib.features.FeatureDescriptor;
import de.lmu.ifi.dbs.jfeaturelib.features.Haralick;
import de.lmu.ifi.dbs.jfeaturelib.features.Moments;
import de.lmu.ifi.dbs.jfeaturelib.features.PHOG;
import de.lmu.ifi.dbs.jfeaturelib.features.ReferenceColorSimilarity;
import de.lmu.ifi.dbs.jfeaturelib.pointDetector.FASTCornerDetector;
import de.lmu.ifi.dbs.jfeaturelib.pointDetector.PointDetector;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.AdaptiveGridResolution;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.CentroidBoundaryDistance;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.CentroidFeature;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.Compactness;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.Eccentricity;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.ExtremalPoints;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.PolygonEvolution;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.Profiles;
import de.lmu.ifi.dbs.jfeaturelib.shapeFeatures.SquareModelShapeMatrix;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.util.List;
import static mip.util.DGBUtils.DBG;

public class JFeatureLibUtils {

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

    public static void main(String[] args) {
        FeatureDescriptor fd;
        {
            fd = new AutoColorCorrelogram();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new ColorHistogram();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new Moments();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new PHOG();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new ReferenceColorSimilarity();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new AdaptiveGridResolution(1);
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new CentroidBoundaryDistance();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new CentroidFeature();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new Compactness();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new Eccentricity();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new ExtremalPoints();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new PolygonEvolution();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new Profiles();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
            fd = new SquareModelShapeMatrix();
            DBG.accept(fd.supports().contains(Supports.DOES_16) + "\n");
        }
        Descriptor d;
        {
            d = new Canny();
            DBG.accept(d.supports().contains(Supports.DOES_16) + "\n");
            d = new DroG();
            DBG.accept(d.supports().contains(Supports.DOES_16) + "\n");
            d = new Kernel();
            DBG.accept(d.supports().contains(Supports.DOES_16) + "\n");
            d = new Roberts();
            DBG.accept(d.supports().contains(Supports.DOES_16) + "\n");
        }
        PointDetector pd;
        {
            pd = new FASTCornerDetector();
            DBG.accept(pd.supports().contains(Supports.DOES_16) + "\n");
        }
    }

    private JFeatureLibUtils() { // singleton
    }
}
