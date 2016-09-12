/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ju
 */
public class DATA {

    static String[] breastSearchStrings = {"breast", "Lymph node", "nipple", "HER-2", "HER2"};
    static String[] malignantTypes = {"invasive ductal carcinoma", "ductal carcinoma in situ"/*, "invasive lobular carcinoma"*/};
    static String[] benignSearchStrings = {"benign", "fibroadenoma", "fibroadipose", "no carcinoma", "no metastasis", "negative for carcinoma"};

    public static void main(String args[]) throws UnsupportedEncodingException, IOException {
        final Base64.Decoder decoder = Base64.getDecoder();
        try (BufferedReader in = new BufferedReader(new FileReader("/home/ju/TMUH_DATA"))) {

            StringBuilder sb = new StringBuilder();
            while (true) {
                String encodedText = in.readLine();
                if (encodedText == null) {
                    break;
                }
                //out.accept(encodedText);
                String decodeText = new String(decoder.decode(encodedText), "UTF-8");
                String[] lines = decodeText.split("\n");
                boolean isDiagnosisStart = false;
                boolean isDiagnosisEnd = true;
                String diagnosis = "";
                String pathologyID = "";
                String biopsyDate = "";
                //int i = 0;
                for (String s : lines) {
                    //out.accept(++i + "\t" + s);
                    if (s.contains("病理切片報告病理號碼")) {
                        String[] tokens = s.split("：");
                        pathologyID = (tokens.length == 2) ? tokens[1] : "ER0000000";
                    }

                    if (s.contains("切片日期")) {
                        String[] tokens = s.split("：");
                        biopsyDate = (tokens.length == 2) ? tokens[1] : "1900-01-01";
                    }

                    if (s.contains("病理診斷")) {
                        isDiagnosisStart = true;
                        isDiagnosisEnd = false;
                    }

                    if (s.contains("組織報告")) {
                        isDiagnosisStart = false;
                        isDiagnosisEnd = true;
                        sb.append(pathologyID).append(",");
                        sb.append(biopsyDate).append(",");
                        diagnosis = StringUtils.replace(diagnosis, "\"", "'");
                        sb.append("\"").append(diagnosis.trim()).append("\"").append(",");
                        sb.append(StringUtils.containsIgnoreCase(diagnosis, "left") ? "V" : "-").append(",");
                        sb.append(StringUtils.containsIgnoreCase(diagnosis, "right") ? "V" : "-").append(",");
                        for (String malignantType : malignantTypes) {
                            sb.append(StringUtils.containsIgnoreCase(diagnosis, malignantType) ? "V" : "-").append(",");
                        }
                        boolean isBenign = false;
                        for (String searchString : benignSearchStrings) {
                            if (StringUtils.containsIgnoreCase(diagnosis, searchString)) {
                                sb.append("V");
                                isBenign = true;
                                break;
                            }
                        }
                        sb.append(isBenign ? "" : "-").append(",");
                        boolean isBreast = false;
                        for (String searchString : breastSearchStrings) {
                            if (StringUtils.containsIgnoreCase(diagnosis, searchString)) {
                                sb.append("V");
                                isBreast = true;
                                break;
                            }
                        }
                        sb.append(isBreast ? "" : "-").append(",");
                        sb.append("\n");
                    }

                    if (isDiagnosisStart && !isDiagnosisEnd) {
                        diagnosis += (s.trim().length() != 0) ? (s + "\n") : "";
                    }
                }

                FileUtils.writeStringToFile(new File("/home/ju/test.csv"), sb.toString());
            }
        }
    }
}
