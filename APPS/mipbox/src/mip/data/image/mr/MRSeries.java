package mip.data.image.mr;

import gdcm.ImageReader;
import gdcm.StringFilter;
import gdcm.Tag;
import ij.ImagePlus;
import ij.plugin.ZProjector;
import ij.process.StackConverter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mip.util.AlphanumComparator;
import mip.util.DCMUtils;
import static mip.util.DGBUtils.DBG;
import mip.util.IJUtils;
import mip.util.IOUtils;

public class MRSeries {

    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final String JPEG_MPEG = "1.2.840.10008.1.2.4";
    private static final double DEFAULT_PIXEL_SPACING = 0.703125;
    private static final double DEFAULT_SLICE_THICKNESS = 1.125;
    private static final Tag T_TRANSFERSYNTAXUID = new gdcm.Tag(0x0002, 0x0010);
    private static final Tag T_SLICETHICKNESS = new gdcm.Tag(0x0018, 0x0050);
    private static final Tag T_SPATIALRESOLUTION = new gdcm.Tag(0x0018, 0x1050);
    private static final Tag T_PIXELSPACING = new gdcm.Tag(0x0028, 0x0030);

    public static void main(String[] args) throws InterruptedException {
        MRSeries mrs = new MRSeries(IOUtils.listFiles(BMRStudy.SBMR + "3"));
        mrs.display(mrs.getSize() / 2);
        mrs.render(2);
        {
            ImagePlus mip = mrs.mip();
            mip.show();
            IJUtils.exitWhenWindowClosed(mip.getWindow());
        }
    }
    public final MR[] imageArrayXY;
    private String seriesNumber;
    private final ImagePlus imp;
    private final ArrayList<Path> dcmFiles;

    public final double pixelSpacingX;
    public final double pixelSpacingY;
    public final double sliceThickness;
    public final boolean isCompressed;

    public MRSeries(final ArrayList<Path> dcmFiles) throws InterruptedException {
        this.dcmFiles = dcmFiles;
        Collections.sort(dcmFiles, new AlphanumComparator());
        imageArrayXY = new MR[dcmFiles.size()];

        CountDownLatch latch = new CountDownLatch(dcmFiles.size());
        ExecutorService e = Executors.newFixedThreadPool(NCPU);

        for (int i = 0; i < dcmFiles.size(); i++) {
            e.execute(new ReadMR(latch, dcmFiles.get(i), i, imageArrayXY));
        }

        latch.await();
        e.shutdownNow();

        checkSeriesNumber();
        double[] rs = readSpatialResolution(dcmFiles.get(0));
        pixelSpacingX = rs[0];
        pixelSpacingY = rs[1];
        sliceThickness = rs[2];
        isCompressed = rs[3] == 1;
        imp = new ImagePlus(seriesNumber, IJUtils.toImageStack(imageArrayXY));

    }

    public ImagePlus mip() {
        ZProjector z = new ZProjector(imp);
        z.setMethod(1); // "Maximun Intensity Projection
        z.doProjection();
        ImagePlus mip = z.getProjection();
        mip.setTitle("Maximun Intensity Projection");
        return mip;
    }

    public void display(int p) {
        imp.show();
        imp.setPosition(p);
        IJUtils.exitWhenWindowClosed(imp.getWindow());
    }

    public void render(int resample) {
        StackConverter sc = new StackConverter(imp);
        sc.convertToGray8();
        IJUtils.render(imp, resample, 70, 25);
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

    private void checkSeriesNumber() {
        seriesNumber = imageArrayXY[0].getSeriesNumber();
        for (int i = 1; i < imageArrayXY.length; i++) {
            try {
                final String s = imageArrayXY[i].getSeriesNumber();
                if (s.equalsIgnoreCase(seriesNumber)) {
                    continue;
                }
                throw new IllegalArgumentException("series ID error");
            } catch (NullPointerException ignore) {
            }
        }
    }

    private double[] readSpatialResolution(Path dcmFile) {
        ImageReader reader = new ImageReader();
        StringFilter filter = new StringFilter();
        reader.SetFileName(dcmFile.toString());
        filter.SetFile(reader.GetFile());
        boolean ret = reader.Read();
        assert (ret == true);
        String pixelSpacingText = filter.ToString(T_PIXELSPACING).trim();
        String sliceThicknessText = filter.ToString(T_SLICETHICKNESS).trim();
        String spatialResText = filter.ToString(T_SPATIALRESOLUTION).trim();
        double psX = Double.MIN_VALUE;
        double psY = Double.MAX_VALUE;
        {
            try {
                String[] tokens = pixelSpacingText.split("\\\\");
                assert (tokens.length == 2);
                psX = Double.parseDouble(tokens[0]);
                psY = Double.parseDouble(tokens[1]);
            } catch (NumberFormatException ignore) {
            }
            if (psX == Double.MIN_VALUE || psY == Double.MIN_VALUE) {
                try {
                    String[] tokens = spatialResText.split("\\\\");
                    assert (tokens.length == 3);
                    psX = Double.parseDouble(tokens[0]);
                    psY = Double.parseDouble(tokens[1]);
                } catch (NumberFormatException ignore) {
                }
            }
        }
        double st = Double.MIN_VALUE;
        {
            try {
                st = Double.parseDouble(sliceThicknessText);
            } catch (NumberFormatException t) {
            }
            if (st == Double.MIN_VALUE) {
                try {
                    String[] tokens = spatialResText.split("\\\\");
                    assert (tokens.length == 3);
                    st = Double.parseDouble(tokens[2]);
                } catch (NumberFormatException ignore) {
                }
            }
        }

        psX = psX == Double.MIN_VALUE ? DEFAULT_PIXEL_SPACING : psX;
        psY = psY == Double.MIN_VALUE ? DEFAULT_PIXEL_SPACING : psY;
        st = st == Double.MIN_VALUE ? DEFAULT_SLICE_THICKNESS : st;

        String transferSyntaxUID = filter.ToString(T_TRANSFERSYNTAXUID).trim();
        boolean compressed = transferSyntaxUID.startsWith(JPEG_MPEG);

        return new double[]{psX, psY, st, compressed ? 1 : 0};
    }

    void decompress() {
        if (!isCompressed) {
            return;
        }

        dcmFiles.stream().forEach((p) -> {
            try {
                DCMUtils.decompressDCM(p.toString(), p.toString());
            } catch (Exception ex) {
                DBG.accept(ex + "\n");
            }
        });
    }

    private class ReadMR implements Runnable {

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
            outputArray[outputNumber] = MROpener.openMR(inputFile);
            doneSignal.countDown();
        }
    }
}
