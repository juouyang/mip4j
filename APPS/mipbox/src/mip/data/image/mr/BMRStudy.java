package mip.data.image.mr;

import java.util.ArrayList;
import java.util.Collections;
import mip.util.AlphanumComparator;
import mip.util.IOUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import mip.util.Timer;

public class BMRStudy {

    public final String studyRoot;
    public final String patientID;
    public final String studyID;
    public final int numberOfFrames;
    MRSeries mrs2;
    MRSeries mrs3;
    MRSeries mrs4;

    public BMRStudy(Path studyRoot) {
        Timer t = new Timer();
        this.studyRoot = studyRoot.toString();
        read_dicom_files(studyRoot);
        patientID = mrs2.getImageArrayXY()[0].getPatientID();
        studyID = mrs2.getImageArrayXY()[0].getStudyID();
        numberOfFrames = mrs2.getSize();
        t.printElapsedTime("BMRStudy");
    }

    private void read_dicom_files(Path studyRoot) {
        final Path p2 = studyRoot.resolve("2");
        final Path p3 = studyRoot.resolve("3");
        final Path p4 = studyRoot.resolve("4");
        if (Files.notExists(p2) || Files.notExists(p3) || Files.notExists(p4)) {
            throw new IllegalArgumentException("Missing series");
        }

        ArrayList<String> t0 = new ArrayList<>();
        ArrayList<String> t1 = new ArrayList<>();
        ArrayList<String> t2 = new ArrayList<>();
        ArrayList<String> s3 = new ArrayList<>();
        ArrayList<String> s4 = new ArrayList<>();
        ArrayList<String> s5 = new ArrayList<>();

        for (Path fn : IOUtils.listFiles(studyRoot.toString())) {
            if (fn.getParent().endsWith("2")) {
                t0.add(fn.toString());
            }
            if (fn.getParent().endsWith("3")) {
                s3.add(fn.toString());
            }
            if (fn.getParent().endsWith("4")) {
                s4.add(fn.toString());
            }
            if (fn.getParent().endsWith("5")) {
                s5.add(fn.toString());
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

        Collections.sort(t0, new AlphanumComparator());
        Collections.sort(t1, new AlphanumComparator());
        Collections.sort(t2, new AlphanumComparator());

        try {
            mrs2 = new MRSeries(t0.toArray(new String[t0.size()]));
            mrs3 = new MRSeries(t1.toArray(new String[t1.size()]));
            mrs4 = new MRSeries(t2.toArray(new String[t2.size()]));
        } catch (InterruptedException ignore) {
        }
    }
}
