package bmr.run;

import mip.data.image.mr.BMRStudy;
import mip.data.image.mr.ColorMapping;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
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
        final File siList = new File(args[2]);
        final double delayWashout = (args.length == 5) ? Double.parseDouble(args[3]) : -0.05;
        final double delayPlateau = (args.length == 5) ? Double.parseDouble(args[4]) : 0.05;
        List<String> lines = FileUtils.readLines(siList, "UTF-8");
        final DecimalFormat df = new DecimalFormat(" 00.00;-00.00");

        StringBuilder multiResult = new StringBuilder();
        multiResult.append("Hospital\tPID\tSID\tWashout\tPlateau\tPersistent\tEnhanced\tRoi\n");

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
                System.err.println("ROI zip file not found, please check it is existed.");
                continue;
            }

            final BMRStudy mrStudy = new BMRStudy(studyRoot);
            final ColorMapping cm = new ColorMapping(mrStudy, roiFile, delayWashout, delayPlateau);
            

            if (cm.hasROI()) {
                multiResult.append(hospital).append("\t");
                multiResult.append(mrStudy.patientID).append("\t");
                multiResult.append(mrStudy.studyID).append("\t");
                multiResult.append(df.format(cm.washoutTotal)).append("\t");
                multiResult.append(df.format(cm.plateauTotal)).append("\t");
                multiResult.append(df.format(cm.persistentTotal)).append("\t");
                multiResult.append(df.format(cm.enhancedTotal)).append("\t");
                multiResult.append(df.format(cm.roiTotal)).append("\n");
            }
        }

        System.out.println(multiResult.toString());
        FileUtils.writeStringToFile(new File(dcmRoot + "/multi_result.txt"), multiResult.toString());
        System.out.println("Press Any Key To Continue...");
        new java.util.Scanner(System.in).nextLine();
    }
}
