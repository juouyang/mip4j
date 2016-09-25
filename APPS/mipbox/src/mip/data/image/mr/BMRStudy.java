package mip.data.image.mr;

import java.io.File;
import java.util.ArrayList;
import mip.util.IOUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import mip.util.Timer;

public class BMRStudy {

    final String studyRoot;
    private final String patientID;
    private final String studyID;

    MRSeries T0;
    MRSeries T1;
    MRSeries T2;

    public BMRStudy(Path studyRoot) {
        Timer t = new Timer();
        this.studyRoot = studyRoot.toString();
        read_dicom_files(studyRoot);
        patientID = T0.getImageArrayXY()[0].getPatientID();
        studyID = T0.getImageArrayXY()[0].getStudyID();
        t.printElapsedTime("BMRStudy");
    }

    public static void main(String[] args) {
        File studyRoot = new File(Kinetic.class.getClassLoader().getResource("resources/bmr/").getFile());
        BMRStudy mrs = new BMRStudy(studyRoot.toPath());
        System.out.println(mrs.getStudyID());
        System.out.println(mrs.getPatientID());
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

    private void read_dicom_files(Path studyRoot) {
        final Path p2 = studyRoot.resolve("2");
        final Path p3 = studyRoot.resolve("3");
        final Path p4 = studyRoot.resolve("4");
        if (Files.notExists(p2) || Files.notExists(p3) || Files.notExists(p4)) {
            throw new IllegalArgumentException("Missing series");
        }

        ArrayList<Path> t0 = new ArrayList<>();
        ArrayList<Path> t1 = new ArrayList<>();
        ArrayList<Path> t2 = new ArrayList<>();
        ArrayList<Path> s3 = new ArrayList<>();
        ArrayList<Path> s4 = new ArrayList<>();
        ArrayList<Path> s5 = new ArrayList<>();

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
        }

        if (s3.size() == t0.size() && s4.size() == t0.size()) {
            t1.addAll(s3);
            t2.addAll(s4);
        } else if (s3.size() == t0.size() && s5.size() == t0.size()) {
            t1.addAll(s3);
            t2.addAll(s5);
        } else if (s4.size() == t0.size() && s5.size() == t0.size()) {
            t1.addAll(s4);
            t2.addAll(s5);
        } else {
            throw new IllegalArgumentException("Unmatched frame-count of series");
        }

        try {
            T0 = new MRSeries(t0);
            T1 = new MRSeries(t1);
            T2 = new MRSeries(t2);
        } catch (InterruptedException ignore) {
        }
    }

}
