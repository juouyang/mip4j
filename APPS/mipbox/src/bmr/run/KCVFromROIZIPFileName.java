/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bmr.run;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import mip.util.AlphanumComparator;
import static mip.util.DGBUtils.DBG;
import mip.util.IOUtils;

/**
 *
 * @author ju
 */
public class KCVFromROIZIPFileName {

    private static final String ROOT = "W:/_BREAST_MRI/TMUH";

    public static void main(String[] args) {
        final ArrayList<Path> listFiles = IOUtils.listFiles(ROOT);
        Collections.sort(listFiles, new AlphanumComparator());
        for (Path p : listFiles) {
            if (p.toString().endsWith(".zip")) {
                final String fn = p.getFileName().toString().replace(".zip", "");
                //DBG.accept(fn + "\n");

                String[] tokens = fn.split("_");

                assert (tokens.length == 5);

                final String studyID = tokens[0];
                final String side = tokens[1];
                final String sliceS = tokens[2].split("-")[0];
                final String sliceE = tokens[2].split("-")[1];
                final String width = tokens[3].split("X")[0];
                final String height = tokens[3].split("X")[1];
                final String size = tokens[3].split("X")[2];
                final String washOut = tokens[4].split("-")[0];
                final String plateau = tokens[4].split("-")[1];
                final String persist = tokens[4].split("-")[2];

                DBG.accept(studyID + "\t");
                DBG.accept(side + "\t");
                DBG.accept(sliceS + "\t");
                DBG.accept(sliceE + "\t");
                DBG.accept(width + "\t");
                DBG.accept(height + "\t");
                DBG.accept(size + "\t");
                DBG.accept(washOut + "\t");
                DBG.accept(plateau + "\t");
                DBG.accept(persist + "\n");
            }
        }

    }
}
