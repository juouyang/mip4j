package mip.data.image.mr;

import ij.ImagePlus;
import java.io.IOException;
import java.time.LocalDate;
import mip.data.image.ShortImage;
import mip.util.IJUtils;

public class MR extends ShortImage {

    public static void main(String[] args) throws IOException {
        MR mr = MROpener.openMR();
        mr.show();
        {
            IJUtils.openImageJ(true);
            ImagePlus imp = mr.toImagePlus("");
            imp.show();
            IJUtils.exitWhenWindowClosed(imp.getWindow());
        }
    }
    private final String studyID;
    private final LocalDate studyDate;
    private final String seriesNumber;
    private final String instanceNumber;
    private final String patientID;

    MR(MROpener data) {
        super(data.width, data.height, data.pixels);
        studyID = data.studyID;
        seriesNumber = data.seriesNumber;
        instanceNumber = data.instanceNumber;
        windowCenter = data.windowCenter;
        windowWidth = data.windowWidth;
        patientID = data.patientID;
        studyDate = data.studyDate;
    }

    public String getStudyID() {
        return studyID;
    }

    public LocalDate getStudyDate() {
        return studyDate;
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

}
