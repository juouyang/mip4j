package mip.model.data.bmr;

import ij.gui.Roi;
import java.util.ArrayList;
import java.util.Collections;
import mip.model.data.series.MRSeries;
import mip.util.AlphanumComparator;
import mip.util.IOUtils;
import mip.util.ROIUtils;
import ij.io.Opener;
import java.io.File;
import java.nio.file.Path;
import mip.util.Timer;

public class BMRStudy {

    public final String patientID;
    public final String studyID;
    public final int numberOfFrames;
    MRSeries mrs2;
    MRSeries mrs3;
    MRSeries mrs4;
    ArrayList<Roi> roi;

    public BMRStudy(String studyRoot) {
        Timer t = new Timer();
        read_dicom_files(studyRoot);

        patientID = mrs2.getImageArrayXY()[0].getPatientID();
        studyID = mrs2.getImageArrayXY()[0].getStudyID();
        numberOfFrames = mrs2.getLength();
        t.printElapsedTime("BMRStudy");
    }

    private boolean read_dicom_files(String studyRoot) {
        ArrayList<String> t0 = new ArrayList<>();
        ArrayList<String> t1 = new ArrayList<>();
        ArrayList<String> t2 = new ArrayList<>();

        ArrayList<Path> allFileNames = IOUtils.listFiles(studyRoot);

        for (Path fn : allFileNames) {
            if (fn.getParent().endsWith("2")) {
                t0.add(fn.toString());
            }
        }

        Collections.sort(t0, new AlphanumComparator());

        ArrayList<String> s3 = new ArrayList<>();
        ArrayList<String> s4 = new ArrayList<>();
        ArrayList<String> s5 = new ArrayList<>();
        for (Path fn : allFileNames) {
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
            throw new IllegalArgumentException("Wrong number of files in Series 3, 4, 5");
        }

        Collections.sort(t1, new AlphanumComparator());
        Collections.sort(t2, new AlphanumComparator());

        try {
            mrs2 = new MRSeries(t0.toArray(new String[t0.size()]));
            mrs3 = new MRSeries(t1.toArray(new String[t1.size()]));
            mrs4 = new MRSeries(t2.toArray(new String[t2.size()]));
        } catch (InterruptedException ex) {
            System.err.println(ex); // TODO log4j
            System.exit(-1);
        }

        return true;
    }

    public boolean addROI(String roiFile) {
        if (roi == null) {
            roi = new ArrayList<>();
        }
        new Opener().openZip(roiFile);
        return roi.addAll(ROIUtils.uncompressROI(new File(roiFile)));
    }
}
