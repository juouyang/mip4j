package bmr.run;

import mip.model.data.image.MR;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import mip.util.IOUtils;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author ju
 */
public class PatientList {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final HashMap<String, String> table = new HashMap<>();
        final HashSet<Path> studies = new HashSet<>();

        for (Path fn : IOUtils.listFiles(args[0])) { // TODO commons-cli
            try {
                Path studyRoot = fn.getParent().getParent(); // STUDY > SERIES > IMAGE
                if (!studies.contains(studyRoot)) {
                    studies.add(studyRoot);
                    MR mr = new MR(fn.toString());
                    table.put(mr.getStudyID(), mr.getPatientID());
                    // TODO log4j
                }
            } catch (IOException ex) {
                System.err.println(ex); // TODO log4j
            }
        }

        try {
            StringBuilder sb = new StringBuilder();
            for (String key : table.keySet()) {
                sb.append(String.format("%6s", key)).append("\t").append(String.format("%10s", table.get(key))).append("\n");
            }
            FileUtils.writeStringToFile(new File(args[0] + "/pList.txt"), sb.toString());
        } catch (Throwable ex) {
            System.err.println(ex); // TODO log4j
        }
    }
}
