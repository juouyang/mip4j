/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bmr.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import mip.model.data.bmr.BMRStudy;
import mip.util.IOUtils;

/**
 *
 * @author ju
 */
public class BMRValidator {

    public static void main(String[] args) {
        
        final HashSet<Path> studies = new HashSet<>();

        for (Path fn : IOUtils.listFiles(args[0])) { // TODO commons-cli
            try {
                Path studyRoot = fn.getParent().getParent(); // STUDY > SERIES > IMAGE
                if (!studies.contains(studyRoot)) {
                    studies.add(studyRoot);
                    System.out.println(studyRoot); // TODO log4j
                    BMRStudy mrs = new BMRStudy(studyRoot);
                }
            } catch (Throwable ex) {
                System.err.println(ex); // TODO log4j
            }
        }
    }
}
