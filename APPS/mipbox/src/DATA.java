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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ju
 */
public class DATA {

    static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final Base64.Decoder DECODER = Base64.getDecoder();
    static final String[] BREAST_KEYWORDS = {"breast"/*, "lymph node"*/, "nipple", "HER-2", "HER2"};
    static final String[] NEEDLE_BIOPSY_KEYWORDS = {"needle", "biopsy"};
    static final String IMMUNO_KEYWORD = "IMMUNOHISTOCHEMICAL STUDY";
    static final String[] ER_KEYWORDS = new String[]{"Estrogen Receptor", "ESTROGEN RECEPTOR", "(ER)"};
    static final String[] ER_END_KEYWORDS = new String[]{"Progesterone Receptor", "PROGESTERONE RECEPTOR", "(PgR)", "HER2", "HER-2", "HER-2/neu", "Ki-67"};
    static final String[] PR_KEYWORDS = new String[]{"Progesterone Receptor", "PROGESTERONE RECEPTOR", "(PgR)"};
    static final String[] PR_END_KEYWORDS = new String[]{"Estrogen Receptor", "ESTROGEN RECEPTOR", "(ER)", "HER2", "HER-2", "HER-2/neu", "Ki-67"};
    static final String PATHOLOGY_KEYWORD = "(pTNM)";
    static final String[] BREAST_MALIGNANT_TYPES = {"invasive ductal carcinoma", "ductal carcinoma in situ", "invasive lobular carcinoma"};
    static final String[] BREAST_BENIGN_KEYWORDS = {"benign", "fibroadenoma", "fibroadipose", "no carcinoma", "no metastasis", "negative for carcinoma", "fibrocystic change", "fat necrosis"};

    static int countSMHT = 0;
    static int countTMUH = 0;

    public static void main(String args[]) throws UnsupportedEncodingException, IOException, IllegalArgumentException, IllegalAccessException {
        Map<String, List<DATA.MRStudy>> mrStudyList = new HashMap<>();
        try (BufferedReader in = new BufferedReader(new FileReader("/home/ju/pList.txt"))) {
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                String[] tokens = line.split("\t");
                MRStudy mrs = new MRStudy();
                mrs.hospital = (tokens.length >= 1) ? tokens[0] : "ERRR";
                mrs.studyID = (tokens.length >= 2) ? tokens[1] : "00000";
                mrs.scanDate = (tokens.length >= 3) ? LocalDate.parse(tokens[2].trim(), DT_FORMATTER) : LocalDate.MIN;
                mrs.patientID = (tokens.length >= 4) ? tokens[3] : "00000000";

                if (mrStudyList.get(mrs.patientID) != null) {
                    mrStudyList.get(mrs.patientID).add(mrs);
                } else {
                    List<MRStudy> studies = new ArrayList<>();
                    studies.add(mrs);
                    mrStudyList.put(mrs.patientID, studies);
                }
            }
        }

        List<DATA.Pathology> pathologyList = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader("/home/ju/TMUH_DATA"))) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                String[] tokens = line.split("\t");
                String pid = tokens[0];
                String encodedText = tokens[1];
                String decodeText = new String(DECODER.decode(encodedText), "UTF-8").replace("\r", "");
                String[] lines = decodeText.split("\n");
                //DBG.accept(pid + "\t" + decodeText + "\n-------------------\n");

                //<editor-fold defaultstate="collapsed" desc="Pathology Report Analyze">
                // ***********************************************************//
                // from free Text: decodeText lines
                // to struct report: pathology
                // ***********************************************************//
                boolean isDiagnosisStart = false;
                boolean isPathologyReportStart = false;
                boolean isImmunohistochemicalStart = false;
                boolean hasHER2inThisPatholgy = false;
                boolean isERStart = false;
                boolean isPRStart = false;

                String diagnosis = "";
                String pathologyID = "";
                String biopsyDate = "";
                String doctor;
                String pathology = "";
                String immunohistochemicalText = "";
                String ki67Text = "";
                String her2Text = "";
                String erText = "";
                String prText = "";

                double ki67 = Double.MIN_VALUE;
                int her2 = Integer.MIN_VALUE;
                int er = Integer.MIN_VALUE;
                int pr = Integer.MIN_VALUE;

                final int ER_SEARCH_WINDOW_SIZE = 3;
                int erSearchWindowSizeinLine = ER_SEARCH_WINDOW_SIZE;

                final int PR_SEARCH_WINDOW_SIZE = 3;
                int prSearchWindowSizeinLine = PR_SEARCH_WINDOW_SIZE;

                final int IMMUNO_SEARCH_WINDOW_SIZE = 20;
                int immunoSearchWindowSizeinLine = IMMUNO_SEARCH_WINDOW_SIZE;

                int i = 0;
                for (final String s : lines) {
                    //DBG.accept("\t" + i++ + "\t" + s + "\n");

                    if (s.contains("病理切片報告病理號碼")) {
                        tokens = s.split("：");
                        pathologyID = (tokens.length == 2) ? tokens[1] : "ERROR!!!!";
                    }

                    if (s.contains("切片日期")) {
                        tokens = s.split("：");
                        biopsyDate = (tokens.length == 2) ? tokens[1] : "1900-01-01";
                    }

                    if (s.contains("病理診斷")) {
                        isDiagnosisStart = true;
                    }

                    if (s.contains("組織報告")) {
                        isDiagnosisStart = false;
                        isPathologyReportStart = true;
                    }

                    if (s.contains("主治醫師")) {
                        isPathologyReportStart = false;

                        tokens = s.split("：");
                        doctor = (tokens.length == 2) ? tokens[1] : "UNKNOWN";

                        Pathology p = new Pathology();
                        p.patientID = pid;
                        p.diagnosis = StringUtils.replace(StringUtils.replace(diagnosis, "\"", "'").trim(), "病理診斷：", "");
                        p.pathologyID = pathologyID;
                        p.biopsyDate = LocalDate.parse(biopsyDate.trim(), DT_FORMATTER);
                        p.isCoreNeedleBiopsy = true;
                        for (String searchString : NEEDLE_BIOPSY_KEYWORDS) {
                            if (!StringUtils.containsIgnoreCase(diagnosis, searchString)) {
                                p.isCoreNeedleBiopsy = false;
                                break;
                            }
                        }
                        p.hasLeft = StringUtils.containsIgnoreCase(diagnosis, "left");
                        p.hasRight = StringUtils.containsIgnoreCase(diagnosis, "right");
                        p.hasIDC = StringUtils.containsIgnoreCase(diagnosis, BREAST_MALIGNANT_TYPES[0]);
                        p.hasDCIS = StringUtils.containsIgnoreCase(diagnosis, BREAST_MALIGNANT_TYPES[1]);
                        p.hasILC = StringUtils.containsIgnoreCase(diagnosis, BREAST_MALIGNANT_TYPES[2]);
                        p.hasBenign = false;
                        for (String searchString : BREAST_BENIGN_KEYWORDS) {
                            if (StringUtils.containsIgnoreCase(diagnosis, searchString)) {
                                p.hasBenign = true;
                                break;
                            }
                        }
                        p.isBreast = false;
                        for (String searchString : BREAST_KEYWORDS) {
                            if (StringUtils.containsIgnoreCase(diagnosis, searchString)) {
                                p.isBreast = true;
                                break;
                            }
                        }
                        p.doctor = doctor;
                        p.pathology = StringUtils.replace(StringUtils.replace(pathology, "\"", "'").trim(), "組織報告：", "");
                        p.hasPathlogicStaging = StringUtils.containsIgnoreCase(pathology, PATHOLOGY_KEYWORD);
                        p.immunohistochemicalStudyCount = StringUtils.countMatches(pathology, IMMUNO_KEYWORD);
                        p.immunohistochemicalStudy = (p.immunohistochemicalStudyCount > 0) ? StringUtils.replace(immunohistochemicalText, "\"", "'").trim() : "-";
                        p.ki67 = ki67;
                        p.ki67Text = ki67Text.equals("") ? "-" : ki67Text.trim();
                        p.her2 = her2;
                        p.her2Text = her2Text.equals("") ? "-" : her2Text.trim();
                        // ER in multiple lines
                        if (er == Integer.MIN_VALUE && !erText.equals("")) {
                            erText = "*****\n" + erText.replace("-", " - ").replace(" %", "%");

                            assert (StringUtils.containsIgnoreCase(erText, "%") ? true : StringUtils.containsIgnoreCase(erText, "negative"));

                            int end = erText.contains("%") ? erText.indexOf("%") + 1 : StringUtils.indexOfIgnoreCase(erText, "negative") + 8;
                            er = Pathology.parseER(erText.substring(StringUtils.indexOfAny(erText, ER_KEYWORDS), end));
                            //DBG.accept(pathologyID + "\t" + er + "\t[" + erText + "]\n====\n");
                        }
                        p.er = er;
                        p.erText = erText.equals("") ? "-" : erText.trim();
                        // PR in multiple lines
                        if (pr == Integer.MIN_VALUE && !prText.equals("")) {
                            prText = "*****\n" + prText.replace("-", " - ").replace(" %", "%");

                            assert (StringUtils.containsIgnoreCase(prText, "%") ? true : StringUtils.containsIgnoreCase(prText, "negative"));

                            int end = prText.contains("%") ? prText.indexOf("%") + 1 : StringUtils.indexOfIgnoreCase(prText, "negative") + 8;
                            pr = Pathology.parsePR(prText.substring(StringUtils.indexOfAny(prText, PR_KEYWORDS), end));
                            //DBG.accept(pathologyID + "\t" + er + "\t[" + erText + "]\n====\n");
                        }
                        p.pr = pr;
                        p.prText = prText.equals("") ? "-" : prText.trim();

                        pathologyList.add(p);
                    }

                    if (StringUtils.contains(s, IMMUNO_KEYWORD) && isPathologyReportStart) {
                        isImmunohistochemicalStart = true;
                        immunoSearchWindowSizeinLine = IMMUNO_SEARCH_WINDOW_SIZE;
                    }

                    if (isImmunohistochemicalStart) {
                        if (s.contains("METHOD")) {
                            isImmunohistochemicalStart = false;
                        }

                        if (immunoSearchWindowSizeinLine < 0) {
                            isImmunohistochemicalStart = false;
                        }

                        if (!isPathologyReportStart) {
                            isImmunohistochemicalStart = false;
                        }
                    }

                    assert (!(isImmunohistochemicalStart && !isPathologyReportStart));

                    if (isImmunohistochemicalStart) {
                        // Ki-67
                        if (StringUtils.containsIgnoreCase(s, "ki-67") && s.contains("%")) {
                            int start = StringUtils.indexOfIgnoreCase(s, "ki-67") + 5;
                            int end = StringUtils.indexOfIgnoreCase(s, "%") + 1;

                            assert (end > start);

                            ki67Text = StringUtils.substring(s, start, end).replace("-", " - ").replace(" %", "%");
                            ki67Text = (ki67Text.indexOf(":") == 0) ? ki67Text.substring(1).trim().toLowerCase() : ki67Text.trim().toLowerCase();
                            ki67 = Pathology.parseKi67(ki67Text);
                            //DBG.accept(pathologyID + "\t" + ki67 + "%\t[" + ki67Text + "]\n--\n");
                        }

                        // HER-2
                        final String[] her2SeaerchStrings = new String[]{"HER2", "HER-2", "HER-2/neu"};
                        if (StringUtils.indexOfAny(s, her2SeaerchStrings) >= 0) {
                            hasHER2inThisPatholgy = true;
                        }
                        if (hasHER2inThisPatholgy && StringUtils.containsIgnoreCase(s, "score")) {
                            int her2Index = StringUtils.indexOfAny(s, her2SeaerchStrings);
                            int scoreIndex = StringUtils.indexOfIgnoreCase(s, "score");
                            int start = her2Index >= 0 && her2Index < scoreIndex ? her2Index : 0;
                            her2Text = StringUtils.substring(s, start);
                            int end = her2Text.contains(")") ? her2Text.indexOf(")") + 1 : StringUtils.indexOfIgnoreCase(her2Text, "score") + 9;
                            her2Text = StringUtils.substring(her2Text, 0, end).replace("-", " - ").replace(" %", "%");
                            her2Text = (her2Text.indexOf(":") == 0) ? her2Text.substring(1).trim().toLowerCase() : her2Text.trim().toLowerCase();
                            her2 = Pathology.parseHER2(her2Text);
                            //DBG.accept(pathologyID + "\t" + her2 + "\t[" + her2Text + "]\n--\n");
                        }

                        // ER
                        if (StringUtils.indexOfAny(s, ER_KEYWORDS) >= 0) {
                            isERStart = true;

                            // ER in one line
                            if (er == Integer.MIN_VALUE && (s.contains("%") || StringUtils.containsIgnoreCase(s, "negative"))) {
                                int end = s.contains("%") ? s.indexOf("%") + 1 : StringUtils.indexOfIgnoreCase(s, "negative") + 8;
                                String erInOneLine = s.substring(StringUtils.indexOfAny(s, ER_KEYWORDS), end).trim().replace("-", " - ").replace(" %", "%");
                                er = Pathology.parseER(erInOneLine);
                                //DBG.accept(pathologyID + "\t" + er + "\t[" + erInOneLine + "]\n--\n");
                            }
                        }

                        if (isERStart) {

                            if (StringUtils.indexOfAny(s, ER_END_KEYWORDS) >= 0) {
                                isERStart = false;
                                erSearchWindowSizeinLine = ER_SEARCH_WINDOW_SIZE;
                            }

                            if (erSearchWindowSizeinLine < 0) {
                                isERStart = false;
                                erSearchWindowSizeinLine = ER_SEARCH_WINDOW_SIZE;
                            }
                            //DBG.accept(pathologyID + "\t[" + erText + "]\n");
                        }

                        if (isERStart) {
                            erText += (s.trim().length() != 0) ? (s + "\n") : "";
                            erSearchWindowSizeinLine--;
                        }

                        // PR
                        if (StringUtils.indexOfAny(s, PR_KEYWORDS) >= 0) {
                            isPRStart = true;

                            // PR in one line
                            if (pr == Integer.MIN_VALUE && (s.contains("%") || StringUtils.containsIgnoreCase(s, "negative"))) {
                                int end = s.contains("%") ? s.indexOf("%") + 1 : StringUtils.indexOfIgnoreCase(s, "negative") + 8;
                                String prInOneLine = s.substring(StringUtils.indexOfAny(s, PR_KEYWORDS), end).trim().replace("-", " - ").replace(" %", "%");
                                pr = Pathology.parsePR(prInOneLine);
                                //DBG.accept(pathologyID + "\t" + pr + "\t[" + prInOneLine + "]\n--\n");
                            }
                        }

                        if (isPRStart) {
                            if (StringUtils.indexOfAny(s, PR_END_KEYWORDS) >= 0) {
                                isPRStart = false;
                                prSearchWindowSizeinLine = PR_SEARCH_WINDOW_SIZE;
                            }

                            if (prSearchWindowSizeinLine < 0) {
                                isPRStart = false;
                                prSearchWindowSizeinLine = ER_SEARCH_WINDOW_SIZE;
                            }
                            //DBG.accept(pathologyID + "\t[" + prText + "]\n");
                        }

                        if (isPRStart) {
                            prText += (s.trim().length() != 0) ? (s + "\n") : "";
                            prSearchWindowSizeinLine--;
                        }
                    }

                    if (isDiagnosisStart) {
                        diagnosis += (s.trim().length() != 0) ? (s + "\n") : "";
                    }

                    if (isPathologyReportStart) {
                        pathology += (s.trim().length() != 0) ? (s + "\n") : "";
                    }

                    if (isImmunohistochemicalStart) {
                        immunohistochemicalText += s.trim().length() != 0 ? (s + "\n") : "";
                        immunoSearchWindowSizeinLine--;
                    }
                }
                //</editor-fold>
            }
        }

        StringBuilder sb = new StringBuilder();
        pathologyList.stream().forEach((p) -> {
            sb.append(p.toString());
        });
        FileUtils.writeStringToFile(new File("/home/ju/test.csv"), sb.toString());

    }

    static class Pathology {

        private String pathologyID;
        private String patientID;
        private LocalDate biopsyDate;
        private String diagnosis;
        private boolean isCoreNeedleBiopsy;
        private boolean hasLeft;
        private boolean hasRight;
        private boolean hasIDC;
        private boolean hasDCIS;
        private boolean hasILC;
        private boolean hasBenign;
        private boolean isBreast;
        private String doctor;
        private String pathology;
        private boolean hasPathlogicStaging;
        private int immunohistochemicalStudyCount;
        private String immunohistochemicalStudy;
        private double ki67;
        private String ki67Text;
        private int her2;
        private String her2Text;
        private int er;
        private String erText;
        private int pr;
        private String prText;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(pathologyID).append(",");
            sb.append(patientID).append(",");
            sb.append(biopsyDate).append(",");
            sb.append("\"").append(diagnosis).append("\"").append(",");
            sb.append(isCoreNeedleBiopsy ? "V" : "-").append(",");
            sb.append(hasLeft ? "V" : "-").append(",");
            sb.append(hasRight ? "V" : "-").append(",");
            sb.append(hasIDC ? "V" : "-").append(",");
            sb.append(hasDCIS ? "V" : "-").append(",");
            sb.append(hasILC ? "V" : "-").append(",");
            sb.append(hasBenign ? "V" : "-").append(",");
            sb.append(isBreast ? "V" : "-").append(",");
            sb.append(doctor).append(",");
            sb.append("\"").append(pathology).append("\"").append(",");
            sb.append(hasPathlogicStaging ? "V" : "-").append(",");
            sb.append(immunohistochemicalStudyCount).append(",");
            sb.append("\"").append(immunohistochemicalStudy).append("\"").append(",");
            sb.append(ki67 == Double.MIN_VALUE ? "-" : ki67).append(",");
            sb.append("\"").append(ki67Text).append("\"").append(",");
            sb.append(her2 == Integer.MIN_VALUE ? "-" : her2).append(",");
            sb.append("\"").append(her2Text).append("\"").append(",");
            sb.append(er == Integer.MIN_VALUE ? "-" : er).append(",");
            sb.append("\"").append(erText).append("\"").append(",");
            sb.append(pr == Integer.MIN_VALUE ? "-" : pr).append(",");
            sb.append("\"").append(prText).append("\"").append(",");

            sb.append("\n");
            return sb.toString();
        }

        public static int parseER(String erText) {
            int ret = Integer.MIN_VALUE;
            Scanner fi = new Scanner(erText);
            fi.useDelimiter("[^\\p{Alnum},\\.-]");
            while (true) {
                if (fi.hasNextInt()) {
                    ret = fi.nextInt();
                    //DBG.accept("\tInt: " + er + "\n");
                } else if (fi.hasNextDouble()) {
                    ret = (int) fi.nextDouble();
                    //DBG.accept("\tDouble: " + er + "\n");
                } else if (fi.hasNext()) {
                    String tmp = fi.next();
                    if (StringUtils.containsIgnoreCase(tmp, "negative")) {
                        ret = 0;
                    }
                    //DBG.accept("\tWord: " + tmp + "\n");
                } else {
                    break;
                }
            }
            if (ret == 1) {
                //DBG.accept("\t" + erText + "\n----\n");
                if (erText.contains(">=1")) {
                    ret = Integer.MIN_VALUE;
                }
                if (erText.contains("<1")) {
                    ret = 0;
                }
            }

            return ret;
        }

        public static int parsePR(String prText) {
            int ret = Integer.MIN_VALUE;
            Scanner fi = new Scanner(prText);
            fi.useDelimiter("[^\\p{Alnum},\\.-]");
            while (true) {
                if (fi.hasNextInt()) {
                    ret = fi.nextInt();
                    //DBG.accept("\tInt: " + er + "\n");
                } else if (fi.hasNextDouble()) {
                    ret = (int) fi.nextDouble();
                    //DBG.accept("\tDouble: " + er + "\n");
                } else if (fi.hasNext()) {
                    String tmp = fi.next();
                    if (StringUtils.containsIgnoreCase(tmp, "negative")) {
                        ret = 0;
                    }
                    //DBG.accept("\tWord: " + tmp + "\n");
                } else {
                    break;
                }
            }
            if (ret == 1) {
                //DBG.accept("\t" + prText + "\n--\n");
                if (prText.contains(">=1")) {
                    ret = Integer.MIN_VALUE;
                }
                if (prText.contains("<1")) {
                    ret = 0;
                }
            }

            return ret;
        }

        public static int parseHER2(String her2Text) {
            int ret = Integer.MIN_VALUE;
            Scanner fi = new Scanner(her2Text);
            fi.useDelimiter("[^\\p{Alnum},\\.-]");
            while (true) {
                if (fi.hasNextInt()) {
                    ret = fi.nextInt();
                    //DBG.accept("\tInt: " + her2 + "\n");
                } else if (fi.hasNextDouble()) {
                    ret = (int) fi.nextDouble();
                    //DBG.accept("\tDouble: " + her2 + "\n");
                } else if (fi.hasNext()) {
                    fi.next();
                    //DBG.accept("\tWord: " + fi.next() + "\n");
                } else {
                    break;
                }
            }

            return ret;
        }

        public static double parseKi67(String ki67Text) {
            double ret = Double.MIN_VALUE;
            Scanner fi = new Scanner(ki67Text);
            fi.useDelimiter("[^\\p{Alnum},\\.-]");
            while (true) {
                if (fi.hasNextInt()) {
                    ret = fi.nextInt();
                    //DBG.accept("\tInt: " + ki67 + "\n");
                } else if (fi.hasNextDouble()) {
                    ret = fi.nextDouble();
                    //DBG.accept("\tDouble: " + ki67 + "\n");
                } else if (fi.hasNext()) {
                    fi.next();
                    //DBG.accept("\tWord: " + fi.next() + "\n");
                } else {
                    break;
                }
            }

            return ret;
        }

    }

    static class MRStudy {

        String studyID;
        String hospital;
        String patientID;
        LocalDate scanDate;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(studyID).append(",");
            sb.append(hospital).append(",");
            sb.append(patientID).append(",");
            sb.append(scanDate).append(",");
            sb.append("\n");
            return sb.toString();
        }
    }

    static final boolean IS_DEBUG = true;
    static final Consumer<String> DBG = DATA::debug;

    private static void debug(String s) {
        if (IS_DEBUG == true) {
            System.out.print(s);
        }
    }
}
