/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bmr.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import mip.model.data.bmr.BMRReport;
import mip.util.IOUtils;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author ju
 */
public class PDFReader {

    public static void main(String args[]) throws IOException {
        single();
        //runAll();
    }

    private static void single() throws IOException {
        BMRReport.DEBUG = true;
        BMRReport bmrr = new BMRReport("D:\\_BREAST_MRI\\PDF\\09362167_4051_chinese.pdf");
        System.out.println(bmrr);
        System.out.println(bmrr.hasEmptyField());
    }

    private static void runAll() throws IOException {
        String pdfRoot = "D:\\_BREAST_MRI\\PDF\\";
        List<Path> files = IOUtils.listFiles(pdfRoot);
        for (Path f : files) {
            if (f.toString().contains("chinese") && FilenameUtils.isExtension(f.toString(), "pdf")) {
                if (BMRReport.DEBUG) {
                    System.out.println(f);
                }
                BMRReport bmrr = new BMRReport(f.toString());
                if (!bmrr.hospital.equals("ACBC")) {
                    System.out.println(bmrr);
                }
            }
        }
    }
}
