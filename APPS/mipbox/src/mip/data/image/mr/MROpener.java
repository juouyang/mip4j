/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import gdcm.Image;
import gdcm.ImageReader;
import gdcm.PixelFormat;
import gdcm.StringFilter;
import gdcm.Tag;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import mip.util.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author ju
 */
public class MROpener {

    private static final String D = "yyyyMMdd";
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern(D);
    private static final Tag T_PID = new gdcm.Tag(0x0010, 0x0020);
    private static final Tag T_DATE = new gdcm.Tag(0x0008, 0x0020);
    private static final Tag T_STUDY_ID = new gdcm.Tag(0x0020, 0x0010);
    private static final Tag T_SERIES_ID = new gdcm.Tag(0x0020, 0x0011);
    private static final Tag T_IMAGE_ID = new gdcm.Tag(0x0020, 0x0013);
    private static final Tag T_WC = new gdcm.Tag(0x0028, 0x1050);
    private static final Tag T_WW = new gdcm.Tag(0x0028, 0x1051);
    private static final String MODALITY = "MR";
    private static final Tag T_MODALITY = new gdcm.Tag(0x0008, 0x0060);

    public static MR openMR() {
        File f = new File(BMRStudy.SBMR + "2/080.dcm");
        return MROpener.openMR(f.toPath());
    }

    public static MRSeries openMRSeries() throws InterruptedException {
        File seriesRoot = new File(BMRStudy.SBMR + "2");
        return new MRSeries(IOUtils.listFiles(seriesRoot.getPath()));
    }

    public static MR openMR(Path dcmFile) {
        MROpener data = new MROpener(dcmFile);
        return new MR(data);
    }

    public int width;
    public int height;
    public short[] pixels;
    public int windowCenter;
    public int windowWidth;
    public String studyID;
    public LocalDate studyDate;
    public String seriesNumber;
    public String instanceNumber;
    public String patientID;

    public MROpener(Path dcmFile) {
        ImageReader reader = new ImageReader();
        StringFilter filter = new StringFilter();
        reader.SetFileName(dcmFile.toString());
        filter.SetFile(reader.GetFile());

        try {
            if (!reader.Read()) {
                throw new IOException("unable to read " + dcmFile);
            }

            readPixels(reader, filter);

            try {
                studyID = filter.ToString(T_STUDY_ID).trim();
                seriesNumber = filter.ToString(T_SERIES_ID).trim();
                instanceNumber = filter.ToString(T_IMAGE_ID).trim();
                windowCenter = Integer.parseInt(filter.ToString(T_WC).trim());
                windowWidth = Integer.parseInt(filter.ToString(T_WW).trim());
                patientID = filter.ToString(T_PID).trim();
                studyDate = LocalDate.parse(filter.ToString(T_DATE).trim(), DF);
            } catch (NumberFormatException ignore) {
            }
        } catch (IOException | IllegalArgumentException ignore) {
        } finally {
            reader.delete();
            filter.delete();
        }
    }

    private void readPixels(ImageReader reader, StringFilter filter) {

        // check MODALITY
        if (!filter.ToString(T_MODALITY).contains(MODALITY)) {
            throw new IllegalArgumentException("not " + MODALITY);
        }

        Image gImg = reader.GetImage();

        // check dimension
        if (gImg.GetNumberOfDimensions() != 2) {
            throw new IllegalArgumentException("dimension is not 2");
        }
        width = (int) gImg.GetDimension(0);
        height = (int) gImg.GetDimension(1);
        pixels = new short[width * height];

        // check pixel type
        PixelFormat pixeltype = gImg.GetPixelFormat();
        if ((pixeltype.GetScalarType() != PixelFormat.ScalarType.UINT16)
                && (pixeltype.GetScalarType() != PixelFormat.ScalarType.INT16)) {
            throw new IllegalArgumentException("neither INT16 nor UINT16 pixel");
        }

        // load pixel
        if (!gImg.GetArray(pixels)) {
            throw new IllegalArgumentException("unable to load pixel array");
        }

        flipVertically(pixels);
    }

    private void flipVertically(short[] array) {
        ArrayUtils.reverse(array);

        for (int y = 0; y < height; y++) {
            final int sIdxEx = y * width;
            final int eInxEx = (y + 1) * width;
            short[] sub = ArrayUtils.subarray(array, sIdxEx, eInxEx);
            ArrayUtils.reverse(sub);
            System.arraycopy(sub, 0, pixels, y * width, width);
        }
    }

}
