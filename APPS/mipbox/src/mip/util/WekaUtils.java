package mip.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.unsupervised.attribute.Remove;

public class WekaUtils {

    private WekaUtils() { // singleton
    }

    public static void saveARFF(Instances is, File f) throws IOException {
        ArffSaver saver = new ArffSaver();
        saver.setInstances(is);
        saver.setFile(f);
        saver.writeBatch();
        saver.resetOptions();
    }

    public static Instances loadARFF(File f) throws IOException {
        ArffLoader loader = new ArffLoader();
        loader.setFile(f);
        Instances ret = loader.getDataSet();
        loader.reset();
        return ret;
    }

    public static Instances loadStructure(File f) throws IOException {
        ArffLoader loader = new ArffLoader();
        loader.setFile(f);
        Instances ret = loader.getStructure();
        loader.reset();
        return ret;
    }

    public static void saveModel(Object model, File f) throws FileNotFoundException, IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(model);
            oos.flush();
            oos.close();
        }
    }

    public static Object loadModel(File f) throws FileNotFoundException, IOException, ClassNotFoundException {
        Object ret = null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            ret = ois.readObject();
            ois.close();
        }
        return ret;
    }

    public static Instances resample(Instances data, int newSize) throws Exception {
        final int sampleSize = data.size();
        final int halfNewSize = newSize / 2;
        final int positive = data.attributeStats(data.numAttributes() - 1).nominalCounts[0];
        final int resamplePositiveSize = (positive > halfNewSize) ? halfNewSize : positive;
        final int resampleSize = resamplePositiveSize * 2;
        final double resampleSizePercent = ((double) resampleSize / (double) sampleSize) * 100.0;

        Resample r = new Resample();

        r.setBiasToUniformClass(1);
        r.setSampleSizePercent(resampleSizePercent);
        r.setInputFormat(data);

        Instances ret = Filter.useFilter(data, r);
        ret.setClassIndex(data.classIndex());

        return ret;
    }

    public static Instances removeClassAttribute(Instances data) throws Exception {
        Remove filter = new Remove();
        filter.setAttributeIndices("" + (data.classIndex() + 1));
        filter.setInputFormat(data);
        return Filter.useFilter(data, filter);
    }

}
