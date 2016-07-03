package mip.data.image.mr;

import gdcm.ImageReader;
import gdcm.StringFilter;
import java.io.IOException;
import java.nio.file.Path;
import mip.data.image.ShortImage;
import mip.util.IOUtils;
import org.apache.commons.lang.ArrayUtils;

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

            readGDCMPixels(reader, filter, "MR");
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

    public static void main(String[] args) {
        try {
            new MR(IOUtils.getFileFromResources("resources/bmr/2/080.dcm").toPath()).show();
        } catch (IOException ignore) {
        }
    }

}
