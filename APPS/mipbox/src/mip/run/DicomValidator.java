/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.run;

import java.io.IOException;
import java.nio.file.Path;
import mip.model.data.image.MR;
import mip.util.IOUtils;

/**
 *
 * @author ju
 */
public class DicomValidator {

    public static void main(String[] args) {
        for (Path fn : IOUtils.listFiles(args[0])) { // TODO commons-cli
            try {
                MR mr = new MR(fn.toString());
                mr.getPatientID();
            } catch (IOException ex) {
                System.err.println(fn); // TODO log4j
            }
        }
    }
}
