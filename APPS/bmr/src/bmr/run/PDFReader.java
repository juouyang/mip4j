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
        //jsonSingle();
        //jsonAll();
        excelAll();
    }

    private static void jsonSingle() throws IOException {
        BMRReport.DEBUG = true;
        BMRReport bmrr = new BMRReport("D:\\_BREAST_MRI\\PDF\\SMHT_COPY_PDF\\2015.06.25.陽追MR報告\\000250_885_chinese_web.pdf");
        System.out.println(bmrr.toJSONString());
        System.out.println(bmrr.hasMissingData());
    }

    private static void jsonAll() throws IOException {
        String pdfRoot = "D:\\_BREAST_MRI\\PDF\\";
        List<Path> files = IOUtils.listFiles(pdfRoot);

        System.out.println("{ \"Reports\" : [ {}");
        for (Path f : files) {
            if (f.toString().contains("chinese") && FilenameUtils.isExtension(f.toString(), "pdf")) {
                BMRReport bmrr = new BMRReport(f.toString());

                if (!bmrr.hospital.equals("ACBC") && bmrr.hasMissingData()) {
                } else {
                    System.out.println(",");
                    System.out.println(bmrr.toJSONString());
                }
            }
        }
        System.out.println("] }");
    }
    
    private static void excelAll() throws IOException {
        String pdfRoot = "D:\\_BREAST_MRI\\PDF\\";
        List<Path> files = IOUtils.listFiles(pdfRoot);

        for (Path f : files) {
            if (f.toString().contains("chinese") && FilenameUtils.isExtension(f.toString(), "pdf")) {
                BMRReport bmrr = new BMRReport(f.toString());

                if (bmrr.hospital.contains("ACBC") || bmrr.hasMissingData() || bmrr.lesionList.isEmpty()) {
                } else {
                    System.out.println(bmrr.toExcelRow());
                }
            }
        }
    }
}
