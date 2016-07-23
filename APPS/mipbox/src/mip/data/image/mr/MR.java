package mip.data.image.mr;

import gdcm.Image;
import gdcm.ImageReader;
import gdcm.PixelFormat;
import gdcm.StringFilter;
import java.io.IOException;
import java.nio.file.Path;
import mip.data.image.ShortImage;
import mip.util.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

public class MR extends ShortImage {

    private String studyID;
    private String seriesNumber;
    private String instanceNumber;
    private String patientID;

    public MR(Path dcmFile) throws IOException {
        ImageReader reader = new ImageReader();
        StringFilter filter = new StringFilter();
        reader.SetFileName(dcmFile.toString());
        filter.SetFile(reader.GetFile());

        try {
            if (!reader.Read()) {
                throw new IOException("unable to read " + dcmFile);
            }

            readGDCMPixels(reader, filter);
            flipVertically(pixelArray);

            try {
                studyID = filter.ToString(new gdcm.Tag(0x0020, 0x0010)).trim();
                seriesNumber = filter.ToString(new gdcm.Tag(0x0020, 0x0011)).trim();
                instanceNumber = filter.ToString(new gdcm.Tag(0x0020, 0x0013)).trim();
                windowCenter = Integer.parseInt(filter.ToString(new gdcm.Tag(0x0028, 0x1050)).trim());
                windowWidth = Integer.parseInt(filter.ToString(new gdcm.Tag(0x0028, 0x1051)).trim());
                patientID = filter.ToString(new gdcm.Tag(0x0010, 0x0020)).trim();
            } catch (NumberFormatException ignore) {
            }
        } catch (IOException | IllegalArgumentException ignore) {
        } finally {
            reader.delete();
            filter.delete();
        }
    }

    public static void main(String[] args) throws IOException {
        MR mr = new MR(IOUtils.getFileFromResources("resources/bmr/2/080.dcm").toPath());
        mr.show();
        mr.getImagePlus("").show();
    }

    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    public String getStudyID() {
        return studyID;
    }

    public String getSeriesNumber() {
        return seriesNumber;
    }

    public String getInstanceNumber() {
        return instanceNumber;
    }

    public String getPatientID() {
        return patientID;
    }

    private void flipVertically(short[] array) {
        ArrayUtils.reverse(array);

        for (int y = 0; y < height; y++) {
            short[] sub = ArrayUtils.subarray(array, (y * width), ((y + 1) * width));
            ArrayUtils.reverse(sub);
            System.arraycopy(sub, 0, pixelArray, y * width, width);
        }
    }
    //</editor-fold>

    private void readGDCMPixels(ImageReader reader, StringFilter filter) {
        String modality = "MR";
        // check modality
        if (!filter.ToString(new gdcm.Tag(0x0008, 0x0060)).contains(modality)) {
            throw new IllegalArgumentException("not " + modality);
        }

        // check dimension
        Image gImg = reader.GetImage();

        if (gImg.GetNumberOfDimensions() != 2) {
            throw new IllegalArgumentException("dimension is not 2");
        }

        width = (int) gImg.GetDimension(0);
        height = (int) gImg.GetDimension(1);
        pixelArray = new short[width * height];

        // check pixel type
        PixelFormat pixeltype = gImg.GetPixelFormat();

        if ((pixeltype.GetScalarType() != PixelFormat.ScalarType.INT16) && (pixeltype.GetScalarType() != PixelFormat.ScalarType.UINT16)) {
            throw new IllegalArgumentException("neither INT16 nor UINT16 image type");
        }

        // load pixel
        if (!gImg.GetArray(pixelArray)) {
            throw new IllegalArgumentException("unable to load pixel array");
        }
    }
}
