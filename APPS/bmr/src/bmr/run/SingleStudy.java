package bmr.run;

import mip.model.data.bmr.ColorMapping;
import mip.model.data.bmr.BMRStudy;
import mip.util.IOUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 *
 * @author ju
 */
public class SingleStudy {

    /**
     *
     * @param args
     * @throws java.io.IOException args[0] / result.txt
     */
    public static void main(String[] args) throws IOException {
        final Path studyRoot = Paths.get(args[0]);
        final String studyID = studyRoot.getFileName().toString();
        final String roiRoot = args.length == 2 ? args[1] : args[0];
        final String roiFile = get_roi_file(studyID, roiRoot);

        final BMRStudy mrStudy = new BMRStudy(studyRoot);
        final ColorMapping cm = new ColorMapping(mrStudy, roiFile);
        cm.show();
        cm.save();

        System.out.println(cm.result.toString());
    }

    private static String get_roi_file(String studyID, String roiRoot) {
        ArrayList<Path> allFileNames = IOUtils.listFiles(roiRoot);

        for (Path fn : allFileNames) {
            if (fn.getFileName().toString().equalsIgnoreCase(studyID + ".zip")) {
                return fn.toString();
            }
        }

        return "";
    }
}
