package bmr.run;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import mip.data.image.mr.BMRStudy;
import mip.data.image.mr.Kinetic;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ju
 */
public class SingleMR {

    public static void main(String[] args) throws IOException {
        final File studyRoot;
        if (args.length == 1) {
            studyRoot = new File(args[0]);
        } else {
            ClassLoader cl = mip.data.image.mr.Kinetic.class.getClassLoader();
            URL url = cl.getResource("resources/bmr/");
            studyRoot = new File(url.getFile());
//            final String dir = "/home/ju/workspace/_BREAST_MRI/SMHT/717/";
//            studyRoot = new File(dir);
        }
        final Kinetic k = new Kinetic(new BMRStudy(studyRoot.toPath()));
        k.show(k.colorMapping(null));
    }
}
