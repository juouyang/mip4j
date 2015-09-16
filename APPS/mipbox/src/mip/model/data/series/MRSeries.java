package mip.model.data.series;

import ij.ImagePlus;
import ij.plugin.ZProjector;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mip.model.data.image.MR;
import mip.util.ImageJUtils;

public class MRSeries {
    private final static int CORES = Runtime.getRuntime().availableProcessors();
    private MR[] imageArrayXY;
    private String seriesNumber;

    public MRSeries(final String[] dcmFiles) throws InterruptedException {
        imageArrayXY = new MR[dcmFiles.length];

        CountDownLatch latch = new CountDownLatch(dcmFiles.length);
        ExecutorService e = Executors.newFixedThreadPool(CORES);

        for (int i = 0; i < dcmFiles.length; i++) {
            e.execute(new ImportRunnable(latch, dcmFiles[i], i, imageArrayXY));
        }

        latch.await();
        e.shutdown();

        seriesNumber = imageArrayXY[0].getSeriesNumber();
        for (int i = 1; i < imageArrayXY.length; i++) {
            try {
                if (!imageArrayXY[i].getSeriesNumber().equalsIgnoreCase(seriesNumber)) {
                    throw new IllegalArgumentException("The input dicom files contain more than one series.\n\t");
                }
            } catch (NullPointerException ex) {
                System.err.println(ex); // TODO log4j
            }
        }
    }

    public MR[] getImageArrayXY() {
        return imageArrayXY;
    }

    public int getWidth() {
        return imageArrayXY[0].getWidth();
    }

    public int getHeight() {
        return imageArrayXY[0].getHeight();
    }

    public int getLength() {
        return imageArrayXY.length;
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
}

final class ImportRunnable implements Runnable {

    private final CountDownLatch doneSignal;
    private final String inputFile;
    private final int outputNumber;
    private final MR[] outputArray;

    ImportRunnable(CountDownLatch signal, String dcmFile, int num, MR[] imgArray) {
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
        } catch (IOException ex) { 
            System.err.println(ex); // TODO log4j
        }
    }
}
