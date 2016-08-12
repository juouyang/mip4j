package mip.data.image.mr;

import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.ZProjector;
import ij.process.StackConverter;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mip.util.AlphanumComparator;
import mip.util.IOUtils;

import mip.util.ImageJUtils;

public class MRSeries {

    private final static int CORES = Runtime.getRuntime().availableProcessors();
    private final MR[] imageArrayXY;
    private String seriesNumber;

    public MRSeries(final ArrayList<Path> dcmFiles) throws InterruptedException {
        Collections.sort(dcmFiles, new AlphanumComparator());       
        imageArrayXY = new MR[dcmFiles.size()];

        CountDownLatch latch = new CountDownLatch(dcmFiles.size());
        ExecutorService e = Executors.newFixedThreadPool(CORES);

        for (int i = 0; i < dcmFiles.size(); i++) {
            e.execute(new ReadMR(latch, dcmFiles.get(i), i, imageArrayXY));
        }

        latch.await();
        e.shutdown();

        checkSeriesNumber();
    }

    public ImagePlus toImagePlus(String title) {
        return new ImagePlus(title, ImageJUtils.getShortImageStackFromShortImageArray(imageArrayXY));
    }

    public void mip() {
        ImagePlus seriesImage = new ImagePlus(seriesNumber, ImageJUtils.getShortImageStackFromShortImageArray(imageArrayXY));
        ZProjector z = new ZProjector(seriesImage);
        z.setMethod(1);
        z.doProjection();
        z.getProjection().show();
    }

    public void show(int p) {
        ImagePlus seriesImage = new ImagePlus(seriesNumber, ImageJUtils.getShortImageStackFromShortImageArray(imageArrayXY));
        seriesImage.show();
        seriesImage.setPosition(p);
    }

    public void render() {
        ImageJ ij = new ImageJ();
        ij.exitWhenQuitting(true);

        Image3DUniverse univ = new Image3DUniverse();
        ImagePlus imp = new ImagePlus("", ImageJUtils.getByteImageStackFromShortImageArray(this.imageArrayXY, this.getImageArrayXY()[0].getWindowCenter(), this.getImageArrayXY()[0].getWindowWidth()));

        new StackConverter(imp).convertToGray8();

        ContentInstant ci = univ.addVoltex(imp, 1).getCurrent();

        if (ci != null) {
            ci.setTransparency(70 / 100f);
            ci.setThreshold(25);
            univ.show();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MRSeries mrs = new MRSeries(IOUtils.listFiles(IOUtils.getFileFromResources("resources/bmr/2/").getPath()));
        mrs.show(mrs.getSize() / 2);
        mrs.render();
    }

    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    public MR[] getImageArrayXY() {
        return imageArrayXY;
    }

    public int getWidth() {
        return imageArrayXY[0].getWidth();
    }

    public int getHeight() {
        return imageArrayXY[0].getHeight();
    }

    public int getSize() {
        return imageArrayXY.length;
    }

    public short getPixel(int x, int y, int z) {
        return imageArrayXY[z].getPixel(x, y);
    }
    //</editor-fold>

    private static final class ReadMR implements Runnable {

        private final CountDownLatch doneSignal;
        private final Path inputFile;
        private final int outputNumber;
        private final MR[] outputArray;

        ReadMR(CountDownLatch signal, Path dcmFile, int num, MR[] imgArray) {
            doneSignal = signal;
            inputFile = dcmFile;
            outputNumber = num;
            outputArray = imgArray;
        }

        @Override
        public void run() {
            doWork();
            doneSignal.countDown();
        }

        private void doWork() {
            try {
                outputArray[outputNumber] = new MR(inputFile);
            } catch (IOException ignore) {
            }
        }
    }

    private void checkSeriesNumber() {
        seriesNumber = imageArrayXY[0].getSeriesNumber();
        for (int i = 1; i < imageArrayXY.length; i++) {
            try {
                if (!imageArrayXY[i].getSeriesNumber().equalsIgnoreCase(seriesNumber)) {
                    throw new IllegalArgumentException("The input dicom files contain more than one series.\n\t");
                }
            } catch (NullPointerException ignore) {
            }
        }
    }
}
