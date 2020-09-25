package bmr.run;

import mip.data.image.mr.MR;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import mip.data.image.mr.MROpener;
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
        System.out.println(args[0]);

        for (Path fn : IOUtils.listFiles(args[0])) {
            Path studyRoot = fn.getParent().getParent(); // STUDY > SERIES > IMAGE
            if (!studies.contains(studyRoot)) {
                studies.add(studyRoot);
                MR mr = MROpener.openMR(fn);
                Path hospital = studyRoot.getParent().getFileName();
                table.put(hospital + "\t" + mr.getStudyID() + "\t" + mr.getStudyDate(), mr.getPatientID());

            }
        }

        try {
            StringBuilder sb = new StringBuilder();
            for (String key : table.keySet()) {
                sb.append(String.format("%6s", key)).append("\t").append(String.format("%10s", table.get(key))).append("\n");
            }
            FileUtils.writeStringToFile(new File(args[0] + "/pList.txt"), sb.toString());
        } catch (Throwable ex) {
            System.err.println(ex);
        }
    }
}
