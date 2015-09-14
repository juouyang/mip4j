package bmr.run;

import mip.model.data.bmr.BMRStudy;
import mip.model.data.bmr.ColorMapping;
import ij.ImagePlus;
import ij.io.FileSaver;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author ju
 */
public class MultipleStudy {

    /**
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final String dcmRoot = args[0]; // TODO commons-cli
        final String roiRoot = args[1];
        File file = new File(args[2]);
        List<String> lines = FileUtils.readLines(file, "UTF-8");

        StringBuilder result = new StringBuilder();
        File resultFile = new File(dcmRoot + "/result.txt");
        result.append("PID\\SID\tWashout\tPlateau\tPersistent\tEnhanced\tRoi\n");

        for (String s : lines) {
            System.out.println(s); // TODO log4j
            String[] split = s.split("\t");
            assert (split.length == 2);

            String hospital = "_";
            String study_id = "_";
            if (StringUtils.isNumeric(split[1])) {
                hospital = split[0];
                study_id = split[1];
            }

            try {
                final String studyRoot = dcmRoot + "\\" + hospital + "\\" + study_id;
                final String roiFile = roiRoot + "\\" + hospital + "\\" + study_id + ".zip";
                final BMRStudy mrStudy = new BMRStudy(studyRoot);
                mrStudy.addROI(roiFile);
                final ColorMapping cm = new ColorMapping(mrStudy);
                final ImagePlus imp = cm.imp;
                FileSaver fs = new FileSaver(imp);
                fs.saveAsTiffStack(studyRoot + "/cm.tif");
                FileUtils.writeStringToFile(new File(studyRoot + "/result.txt"), cm.result.toString());
                result.append(mrStudy.patientID).append("\\").append(mrStudy.studyID).append("\t");
                result.append(cm.washoutTotal).append("\t").append(cm.plateauTotal).append("\t").append(cm.persistentTotal).append("\t").append(cm.enhancedTotal).append("\t").append(cm.roiTotal).append("\n");
                System.out.println(cm.result.toString()); // TODO log4j
            } catch (IOException ex) {
                // TODO log4j
            }
            FileUtils.writeStringToFile(resultFile, result.toString());
            System.out.println(result.toString()); // TODO log4j
        }

        System.out.println("Press Any Key To Continue...");
        new java.util.Scanner(System.in).nextLine();
    }
}
