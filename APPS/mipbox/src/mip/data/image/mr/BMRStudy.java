package mip.data.image.mr;

import java.io.File;
import java.util.ArrayList;
import mip.util.IOUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import static mip.util.DebugUtils.DBG;
import mip.util.Timer;

public class BMRStudy {

    private final String patientID;
    private final String studyID;

    public final String studyRoot;
    public final MRSeries T0;
    public final MRSeries T1;
    public final MRSeries T2;

    public BMRStudy(Path studyRoot) {
        Timer t = new Timer();
        this.studyRoot = studyRoot.toString();
        MRSeries[] ret = read_dicom_files(studyRoot);
        T0 = ret[0];
        T1 = ret[1];
        T2 = ret[2];
        patientID = T0.imageArrayXY[0].getPatientID();
        studyID = T0.imageArrayXY[0].getStudyID();
        t.printElapsedTime("BMRStudy");
    }

    public static void main(String[] args) {
        File studyRoot = new File(BMRStudy.class.getClassLoader().getResource("resources/bmr/").getFile());
        BMRStudy mrs = new BMRStudy(studyRoot.toPath());
        DBG.accept(mrs.getStudyID() + "\n");
        DBG.accept(mrs.getPatientID() + "\n");
    }

    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    public String getPatientID() {
        return patientID;
    }

    public String getStudyID() {
        return studyID;
    }

    public short getPixel(int x, int y, int z, int t) {
        switch (t) {
            case 0:
                return T0.getPixel(x, y, z);
            case 1:
                return T1.getPixel(x, y, z);
            case 2:
                return T2.getPixel(x, y, z);
            default:
                throw new IllegalArgumentException();
        }
    }

    public String getStudyRoot() {
        return studyRoot;
    }
    //</editor-fold>

    private MRSeries[] read_dicom_files(Path studyRoot) {
        DBG.accept(studyRoot + "\n");
        final Path p2 = studyRoot.resolve("2");
        final Path p3 = studyRoot.resolve("3");
        final Path p4 = studyRoot.resolve("4");
        if (Files.notExists(p2) || Files.notExists(p3) || Files.notExists(p4)) {
            throw new IllegalArgumentException("Missing series");
        }

        ArrayList<Path> t0 = new ArrayList<>();
        ArrayList<Path> t1;
        ArrayList<Path> t2;
        ArrayList<Path> s3 = new ArrayList<>();
        ArrayList<Path> s4 = new ArrayList<>();
        ArrayList<Path> s5 = new ArrayList<>();
        ArrayList<Path> s6 = new ArrayList<>();

        for (Path fn : IOUtils.listFiles(studyRoot.toString())) {
            if (fn.getParent().endsWith("2")) {
                t0.add(fn);
            }
            if (fn.getParent().endsWith("3")) {
                s3.add(fn);
            }
            if (fn.getParent().endsWith("4")) {
                s4.add(fn);
            }
            if (fn.getParent().endsWith("5")) {
                s5.add(fn);
            }
            if (fn.getParent().endsWith("6")) {
                s6.add(fn);
            }
        }

        if (s3.size() == t0.size() && s4.size() == t0.size()) {
            t1 = s3;
            t2 = s4;
        } else if (s3.size() == t0.size() && s5.size() == t0.size()) {
            t1 = s3;
            t2 = s5;
        } else if (s4.size() == t0.size() && s5.size() == t0.size()) {
            t1 = s4;
            t2 = s5;
        } else if (s3.size() == s4.size() && s4.size() == s5.size() && s3.size() != t0.size()) {
            t0 = s4;
            t1 = s5;
            t2 = s6;
        } else {
            throw new IllegalArgumentException("Unmatched frame-count of series " + studyRoot);
        }

        MRSeries[] ret = new MRSeries[3];
        try {
            ret[0] = new MRSeries(t0);
            ret[1] = new MRSeries(t1);
            ret[2] = new MRSeries(t2);
        } catch (InterruptedException ignore) {
        }

        return ret;
    }

}
