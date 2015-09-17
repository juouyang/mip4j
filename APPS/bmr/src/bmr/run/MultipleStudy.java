package bmr.run;

import mip.model.data.bmr.BMRStudy;
import mip.model.data.bmr.ColorMapping;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import mip.util.IOUtils;
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
        final String dcmRoot = args[0];
        final String roiRoot = args[1];
        File siList = new File(args[2]);
        List<String> lines = FileUtils.readLines(siList, "UTF-8");

        StringBuilder multiResult = new StringBuilder();
        multiResult.append("PID\\SID\tWashout\tPlateau\tPersistent\tEnhanced\tRoi\n");

        for (String s : lines) {
            System.out.println(s);
            String[] split = s.split("\t");
            if (split.length != 2) {
                System.err.println("hospital-SI list: string split error");
                continue;
            }

            String hospital;
            String study_id;
            if (StringUtils.isNumeric(split[1])) {
                hospital = split[0];
                study_id = split[1];
            } else {
                System.err.println("hospital-SI list: SI isNumeric error");
                continue;
            }

            final Path studyRoot = Paths.get(dcmRoot + "\\" + hospital + "\\" + study_id);
            final String roiFile = roiRoot + "\\" + hospital + "\\" + study_id + ".zip";
            if (!IOUtils.fileExisted(roiFile)) {
                continue;
            }

            final BMRStudy mrStudy = new BMRStudy(studyRoot);
            final ColorMapping cm = new ColorMapping(mrStudy, roiFile);

            if (cm.hasROI()) {
                multiResult.append(mrStudy.patientID).append("\\");
                multiResult.append(mrStudy.studyID).append("\t");
                multiResult.append(cm.washoutTotal).append("\t");
                multiResult.append(cm.plateauTotal).append("\t");
                multiResult.append(cm.persistentTotal).append("\t");
                multiResult.append(cm.enhancedTotal).append("\t");
                multiResult.append(cm.roiTotal).append("\n");
            }
        }

        System.out.println(multiResult.toString());
        FileUtils.writeStringToFile(new File(dcmRoot + "/multi_result.txt"), multiResult.toString());
        System.out.println("Press Any Key To Continue...");
        new java.util.Scanner(System.in).nextLine();
    }
}
