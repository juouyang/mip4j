package bmr.run;

import java.io.File;
import java.io.IOException;
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
        final File studyRoot = (args.length == 1) ? new File(args[0]) : new File(BMRStudy.SBMR);
        final BMRStudy mbr = new BMRStudy(studyRoot.toPath());
        final Kinetic k = new Kinetic(mbr);
        k.show();
    }
}
