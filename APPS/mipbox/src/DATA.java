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
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import static mip.util.DebugUtils.DBG;
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
        try (BufferedReader in = new BufferedReader(new FileReader(DATA_ROOT + "pList.txt"))) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                String[] tokens = line.split("\t");
                MRStudy mrs = new MRStudy();
                mrs.hospital = (tokens.length >= 1) ? tokens[0].trim() : "ERRR";
                mrs.studyID = (tokens.length >= 2) ? tokens[1].trim() : "00000";
                mrs.scanDate = (tokens.length >= 3) ? LocalDate.parse(tokens[2].trim(), DT_FORMATTER) : LocalDate.MIN;
                mrs.patientID = (tokens.length >= 4) ? tokens[3].trim() : "00000000";

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
        try (BufferedReader in = new BufferedReader(new FileReader(DATA_ROOT + HOSPITAL + (HOSPITAL.length() == 0 ? "" : "_") + "DATA"))) {
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
                boolean isPathologyStart = false;
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
                        isPathologyStart = true;
                    }

                    if (s.contains("主治醫師")) {
                        isPathologyStart = false;

                        tokens = s.split("：");
                        doctor = (tokens.length == 2) ? tokens[1] : "UNKNOWN";

                        Pathology p = new Pathology();

                        p.patientID = pid;
                        p.diagnosis = StringUtils.replace(StringUtils.replace(diagnosis, "\"", "'").trim(), "病理診斷：", "");
                        p.isHER2Only = p.diagnosis.startsWith("The HER");
                        p.isHER2Fail = p.isHER2Only && (StringUtils.containsIgnoreCase(p.diagnosis, "Indeterminate") || StringUtils.containsIgnoreCase(p.diagnosis, "NOT amplified"));
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

                        if (!p.hasIDC && !p.hasDCIS && !p.hasILC && !p.hasBenign) {
                            if (StringUtils.containsIgnoreCase(pathology, "invasive") || StringUtils.containsIgnoreCase(pathology, "carcinoma")) {
                                int start = StringUtils.indexOfIgnoreCase(pathology, "microscop");
                                start = start >= 0 ? start : 0;
                                int end = StringUtils.indexOfIgnoreCase(pathology, IMMUNO_KEYWORD);
                                int refIndex = StringUtils.indexOfIgnoreCase(pathology, "Reference");
                                if (refIndex > 0) {
                                    end = refIndex;
                                }
                                end = end >= start ? end : Math.min(pathology.length(), start + 500); // TODO: magic number search for diagnosis
                                String microscopeText = pathology.substring(start, end);
                                p.hasIDC = StringUtils.containsIgnoreCase(microscopeText, BREAST_MALIGNANT_TYPES[0]);
                                p.hasDCIS = StringUtils.containsIgnoreCase(microscopeText, BREAST_MALIGNANT_TYPES[1]);
                                if (StringUtils.containsIgnoreCase(microscopeText, "lobular") || StringUtils.containsIgnoreCase(microscopeText, "tubular")) {
                                    p.hasILC = true;
                                }
                                //DBG.accept(p.pathologyID + "\t" + start + "\t" + end + "\t" + microscopeText + "\n");
                            }
                        }

                        p.doctor = doctor;
                        p.pathology = StringUtils.replace(StringUtils.replace(pathology, "\"", "'").trim(), "組織報告：", "");
                        p.hasPathlogicStaging = StringUtils.containsIgnoreCase(pathology, PATHOLOGY_KEYWORD);

                        p.immunohistochemicalStudyCount = StringUtils.countMatches(pathology, IMMUNO_KEYWORD);
                        p.immunohistochemicalStudy = (p.immunohistochemicalStudyCount > 0) ? StringUtils.replace(immunohistochemicalText, "\"", "'").trim() : "-";
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
                        p.ki67 = ki67;
                        p.ki67Text = ki67Text.equals("") ? "-" : ki67Text.trim();
                        p.her2 = her2;
                        p.her2Text = her2Text.equals("") ? "-" : her2Text.trim();

                        p.studyID = "";
                        p.scanDate = LocalDate.of(1900, 01, 01);
                        if (mrStudyList.containsKey(p.patientID)) {
                            MRStudy mrs = MRStudy.getClosestMRStudy(mrStudyList.get(p.patientID), p.biopsyDate);
                            if (mrs != null) {
                                p.studyID = mrs.studyID;
                                p.scanDate = mrs.scanDate;
                            }

//                            for (MRStudy mrs : mrStudyList.get(p.patientID)) {
//                                p.studyCount++;
//                                //DBG.accept("\t" + mrs);
//
//                                long daysBetween = Math.abs(DAYS.between(p.biopsyDate, mrs.scanDate));
//                                if (daysBetween < p.daysBetween) {
//                                    p.daysBetween = daysBetween;
//                                    p.studyID = mrs.studyID;
//                                    p.scanDate = mrs.scanDate;
//                                }
//                            }
//
//                            if (Math.abs(MONTHS.between(p.scanDate, p.biopsyDate)) > 6) { // TODO: magic number six month
//                                p.studyCount = 0;
//                                p.studyID = "";
//                                p.scanDate = LocalDate.of(1900, 01, 01);
//                                p.daysBetween = Long.MAX_VALUE;
//                            }
                        }
                        //DBG.accept(p.pathologyID + "\t" + p.studyCount + "\t" + p.studyID + "\t" + p.daysBetween + "\n");

                        pathologyList.add(p);
                    }

                    if (StringUtils.contains(s, IMMUNO_KEYWORD) && isPathologyStart) {
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

                        if (!isPathologyStart) {
                            isImmunohistochemicalStart = false;
                        }
                    }

                    assert (!(isImmunohistochemicalStart && !isPathologyStart));

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
                            //int end = her2Text.contains(")") ? her2Text.indexOf(")") + 1 : StringUtils.indexOfIgnoreCase(her2Text, "score") + 9;
                            int end = Math.min(her2Text.contains(")") ? her2Text.indexOf(")") + 1 : her2Text.length(), StringUtils.indexOfIgnoreCase(her2Text, "score") + 9); // "TH1102673"
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

                    if (isPathologyStart) {
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

        if (HOSPITAL.length() != 0) {
            StringBuilder sb = new StringBuilder();
            pathologyList.stream().forEach((p) -> {
                sb.append(p.toString());
            });
            FileUtils.writeStringToFile(new File(DATA_ROOT + HOSPITAL + ".csv"), sb.toString());
        }

        HashMap<MRStudy, StringBuilder> studyDiagnosisList = new HashMap<>();
        mrStudyList.keySet().stream().forEach((pid) -> { // each patientID
            mrStudyList.get(pid).stream().forEach((mrs) -> { // each MRStudy
                if (!studyDiagnosisList.containsKey(mrs)) {
                    studyDiagnosisList.put(mrs, new StringBuilder());
                }

                if (mrs.studyID.equals("1378")) {
                    DBG.accept("\n");
                }

                // diagnosis for MRStudy
                {
                    boolean hasHER2Fail = false;
                    boolean hasIDC = false;
                    boolean hasDCIS = false;
                    boolean hasILC = false;
                    boolean hasBenign = false;
                    boolean noDiagnosis = true;
                    for (Pathology p : pathologyList) {
                        if (p.patientID.equals(mrs.patientID) && p.studyID.equals(mrs.studyID) && !p.getDiagnosisSummary().equals("-")) {
                            noDiagnosis = false;

                            hasHER2Fail = p.isHER2Fail ? p.isHER2Fail : hasHER2Fail;
                            hasIDC = p.hasIDC ? p.hasIDC : hasIDC;
                            hasDCIS = p.hasDCIS ? p.hasDCIS : hasDCIS;
                            hasILC = p.hasILC ? p.hasILC : hasILC;
                            hasBenign = p.hasBenign ? p.hasBenign : hasBenign;
                        }
                    }
                    StringBuilder sb = studyDiagnosisList.get(mrs);
                    if (noDiagnosis) {
                        for (Pathology p : pathologyList) {
                            if (p.patientID.equals(mrs.patientID) && p.biopsyDate.isAfter(mrs.scanDate) && Math.abs(DAYS.between(p.biopsyDate, mrs.scanDate)) < 30 && !p.getDiagnosisSummary().equals("-")) { //TODO: magic number
                                hasHER2Fail = p.isHER2Fail ? p.isHER2Fail : hasHER2Fail;
                                hasIDC = p.hasIDC ? p.hasIDC : hasIDC;
                                hasDCIS = p.hasDCIS ? p.hasDCIS : hasDCIS;
                                hasILC = p.hasILC ? p.hasILC : hasILC;
                                hasBenign = p.hasBenign ? p.hasBenign : hasBenign;
                            }
                        }
                    }
                    sb.append(hasHER2Fail ? (sb.length() != 0 ? " + " : "") + "HER2" : "");
                    sb.append(hasIDC ? (sb.length() != 0 ? " + " : "") + "IDC" : "");
                    sb.append(hasDCIS ? (sb.length() != 0 ? " + " : "") + "DCIS" : "");
                    sb.append(hasILC ? (sb.length() != 0 ? " + " : "") + "ILC" : "");
                    sb.append(hasBenign ? (sb.length() != 0 ? " + " : "") + "benign" : "");
                    sb.insert(0, noDiagnosis && sb.length() != 0 ? "* " : "");
                    sb.append(sb.length() == 0 ? "-" : "");
                    mrs.diagnosisSummary = sb.toString();
                    //DBG.accept(mrs.studyID + "\t" + sb.toString() + "\n");
                }

                // HER2 for MRStudy
                {
                    int her2 = Integer.MIN_VALUE;
                    LocalDate her2Date = LocalDate.MIN;
                    boolean noHER2 = true;
                    for (Pathology p : pathologyList) {
                        if (p.patientID.equals(mrs.patientID) && p.studyID.equals(mrs.studyID) && p.her2 != Integer.MIN_VALUE) {
                            noHER2 = false;
                            //DBG.accept(mrs.studyID + "\t" + p.her2 + "\n");
                            if (her2 != Integer.MIN_VALUE && her2 != p.her2) {

                                boolean yoursIsAfter = p.biopsyDate.isAfter(mrs.scanDate);
                                boolean mineIsAfter = her2Date.isAfter(mrs.scanDate);
                                boolean isSameDate = p.biopsyDate.equals(her2Date);
                                if (yoursIsAfter && !mineIsAfter) { // prefer biopsy after MRStudy scanDate
                                    her2 = p.her2;
                                    her2Date = p.biopsyDate;
                                } else if (isSameDate) {
                                    her2 += p.her2;
                                } else if (Math.abs(DAYS.between(mrs.scanDate, her2Date)) > Math.abs(DAYS.between(mrs.scanDate, p.biopsyDate))) { // find closest
                                    her2 = p.her2;
                                    her2Date = p.biopsyDate;
                                }

                            } else {
                                her2 = p.her2;
                                her2Date = p.biopsyDate;
                            }
                        }
                    }
                    if (noHER2) {
                        for (Pathology p : pathologyList) {
                            if (p.patientID.equals(mrs.patientID) && p.biopsyDate.isAfter(mrs.scanDate) && Math.abs(DAYS.between(p.biopsyDate, mrs.scanDate)) < 30 && p.her2 != Integer.MIN_VALUE) { //TODO: magic number
                                noHER2 = false;
                                //DBG.accept(mrs.studyID + "\t" + p.her2 + "\n");
                                if (her2 != Integer.MIN_VALUE && her2 != p.her2) {

                                    boolean yoursIsAfter = p.biopsyDate.isAfter(mrs.scanDate);
                                    boolean mineIsAfter = her2Date.isAfter(mrs.scanDate);
                                    boolean isSameDate = p.biopsyDate.equals(her2Date);
                                    if (yoursIsAfter && !mineIsAfter) { // find after MRStudy scanDate
                                        her2 = p.her2;
                                        her2Date = p.biopsyDate;
                                    } else if (isSameDate) {
                                        her2 += p.her2;
                                    } else if (Math.abs(DAYS.between(mrs.scanDate, her2Date)) > Math.abs(DAYS.between(mrs.scanDate, p.biopsyDate))) { // find closest
                                        her2 = p.her2;
                                        her2Date = p.biopsyDate;
                                    }

                                } else {
                                    her2 = p.her2;
                                    her2Date = p.biopsyDate;
                                }
                            }
                        }
                    }
                    mrs.her2 = noHER2 ? Integer.MIN_VALUE : her2;
                    //DBG.accept(mrs.studyID + "\t" + mrs.her2 + "\n");
                }

                // Ki-67 for MRStudy
                {
                    double ki67 = Double.MIN_VALUE;
                    LocalDate ki67Date = LocalDate.MIN;
                    boolean noKi67 = true;
                    for (Pathology p : pathologyList) {
                        if (p.patientID.equals(mrs.patientID) && p.studyID.equals(mrs.studyID) && p.ki67 != Double.MIN_VALUE) {
                            noKi67 = false;
                            //DBG.accept(mrs.studyID + "\t" + p.ki67 + "\n");
                            if (ki67 != Double.MIN_VALUE && ki67 != p.ki67) {

                                boolean yoursIsAfter = p.biopsyDate.isAfter(mrs.scanDate);
                                boolean mineIsAfter = ki67Date.isAfter(mrs.scanDate);
                                boolean isSameDate = p.biopsyDate.equals(ki67Date);
                                if (yoursIsAfter && !mineIsAfter) { // prefer biopsy after MRStudy scanDate
                                    ki67 = p.ki67;
                                    ki67Date = p.biopsyDate;
                                } else if (isSameDate) {
                                    ki67 += p.ki67 * 10000;
                                } else if (Math.abs(DAYS.between(mrs.scanDate, ki67Date)) > Math.abs(DAYS.between(mrs.scanDate, p.biopsyDate))) { // find closest
                                    ki67 = p.ki67;
                                    ki67Date = p.biopsyDate;
                                }

                            } else {
                                ki67 = p.ki67;
                                ki67Date = p.biopsyDate;
                            }
                        }
                    }
                    if (noKi67) {
                        for (Pathology p : pathologyList) {
                            if (p.patientID.equals(mrs.patientID) && p.biopsyDate.isAfter(mrs.scanDate) && Math.abs(DAYS.between(p.biopsyDate, mrs.scanDate)) < 30 && p.ki67 != Double.MIN_VALUE) { //TODO: magic number
                                noKi67 = false;
                                //DBG.accept(mrs.studyID + "\t" + p.ki67 + "\n");
                                if (ki67 != Integer.MIN_VALUE && ki67 != p.ki67) {

                                    boolean yoursIsAfter = p.biopsyDate.isAfter(mrs.scanDate);
                                    boolean mineIsAfter = ki67Date.isAfter(mrs.scanDate);
                                    boolean isSameDate = p.biopsyDate.equals(ki67Date);
                                    if (yoursIsAfter && !mineIsAfter) { // prefer biopsy after MRStudy scanDate
                                        ki67 = p.ki67;
                                        ki67Date = p.biopsyDate;
                                    } else if (isSameDate) {
                                        ki67 += p.ki67 * 10000;
                                    } else if (Math.abs(DAYS.between(mrs.scanDate, ki67Date)) > Math.abs(DAYS.between(mrs.scanDate, p.biopsyDate))) { // find closest
                                        ki67 = p.ki67;
                                        ki67Date = p.biopsyDate;
                                    }

                                } else {
                                    ki67 = p.ki67;
                                    ki67Date = p.biopsyDate;
                                }
                            }
                        }
                    }
                    mrs.ki67 = noKi67 ? Double.MIN_VALUE : ki67;
                    //DBG.accept(mrs.studyID + "\t" + mrs.ki67 + "\n");
                }

                // er for MRStudy
                {
                    int er = Integer.MIN_VALUE;
                    LocalDate erDate = LocalDate.MIN;
                    boolean noer = true;
                    for (Pathology p : pathologyList) {
                        if (p.patientID.equals(mrs.patientID) && p.studyID.equals(mrs.studyID) && p.er != Integer.MIN_VALUE) {
                            noer = false;
                            //DBG.accept(mrs.studyID + "\t" + p.er + "\n");
                            if (er != Integer.MIN_VALUE && er != p.er) {

                                boolean yoursIsAfter = p.biopsyDate.isAfter(mrs.scanDate);
                                boolean mineIsAfter = erDate.isAfter(mrs.scanDate);
                                boolean isSameDate = p.biopsyDate.equals(erDate);
                                if (yoursIsAfter && !mineIsAfter) { // prefer biopsy after MRStudy scanDate
                                    er = p.er;
                                    erDate = p.biopsyDate;
                                } else if (isSameDate) {
                                    er += p.er;
                                } else if (Math.abs(DAYS.between(mrs.scanDate, erDate)) > Math.abs(DAYS.between(mrs.scanDate, p.biopsyDate))) { // find closest
                                    er = p.er;
                                    erDate = p.biopsyDate;
                                }

                            } else {
                                er = p.er;
                                erDate = p.biopsyDate;
                            }
                        }
                    }
                    if (noer) {
                        for (Pathology p : pathologyList) {
                            if (p.patientID.equals(mrs.patientID) && p.biopsyDate.isAfter(mrs.scanDate) && Math.abs(DAYS.between(p.biopsyDate, mrs.scanDate)) < 30 && p.er != Integer.MIN_VALUE) { //TODO: magic number
                                noer = false;
                                //DBG.accept(mrs.studyID + "\t" + p.er + "\n");
                                if (er != Integer.MIN_VALUE && er != p.er) {

                                    boolean yoursIsAfter = p.biopsyDate.isAfter(mrs.scanDate);
                                    boolean mineIsAfter = erDate.isAfter(mrs.scanDate);
                                    boolean isSameDate = p.biopsyDate.equals(erDate);
                                    if (yoursIsAfter && !mineIsAfter) { // find after MRStudy scanDate
                                        er = p.er;
                                        erDate = p.biopsyDate;
                                    } else if (isSameDate) {
                                        er += p.er;
                                    } else if (Math.abs(DAYS.between(mrs.scanDate, erDate)) > Math.abs(DAYS.between(mrs.scanDate, p.biopsyDate))) { // find closest
                                        er = p.er;
                                        erDate = p.biopsyDate;
                                    }

                                } else {
                                    er = p.er;
                                    erDate = p.biopsyDate;
                                }
                            }
                        }
                    }
                    mrs.er = noer ? Integer.MIN_VALUE : er;
                    //DBG.accept(mrs.studyID + "\t" + mrs.er + "\n");
                }

                // pr for MRStudy
                {
                    int pr = Integer.MIN_VALUE;
                    LocalDate prDate = LocalDate.MIN;
                    boolean nopr = true;
                    for (Pathology p : pathologyList) {
                        if (p.patientID.equals(mrs.patientID) && p.studyID.equals(mrs.studyID) && p.pr != Integer.MIN_VALUE) {
                            nopr = false;
                            //DBG.accept(mrs.studyID + "\t" + p.pr + "\n");
                            if (pr != Integer.MIN_VALUE && pr != p.pr) {

                                boolean yoursIsAfter = p.biopsyDate.isAfter(mrs.scanDate);
                                boolean mineIsAfter = prDate.isAfter(mrs.scanDate);
                                boolean isSameDate = p.biopsyDate.equals(prDate);
                                if (yoursIsAfter && !mineIsAfter) { // prefer biopsy after MRStudy scanDate
                                    pr = p.pr;
                                    prDate = p.biopsyDate;
                                } else if (isSameDate) {
                                    pr += p.pr;
                                } else if (Math.abs(DAYS.between(mrs.scanDate, prDate)) > Math.abs(DAYS.between(mrs.scanDate, p.biopsyDate))) { // find closest
                                    pr = p.pr;
                                    prDate = p.biopsyDate;
                                }

                            } else {
                                pr = p.pr;
                                prDate = p.biopsyDate;
                            }
                        }
                    }
                    if (nopr) {
                        for (Pathology p : pathologyList) {
                            if (p.patientID.equals(mrs.patientID) && p.biopsyDate.isAfter(mrs.scanDate) && Math.abs(DAYS.between(p.biopsyDate, mrs.scanDate)) < 30 && p.pr != Integer.MIN_VALUE) { //TODO: magic number
                                nopr = false;
                                //DBG.accept(mrs.studyID + "\t" + p.pr + "\n");
                                if (pr != Integer.MIN_VALUE && pr != p.pr) {

                                    boolean yoursIsAfter = p.biopsyDate.isAfter(mrs.scanDate);
                                    boolean mineIsAfter = prDate.isAfter(mrs.scanDate);
                                    boolean isSameDate = p.biopsyDate.equals(prDate);
                                    if (yoursIsAfter && !mineIsAfter) { // find after MRStudy scanDate
                                        pr = p.pr;
                                        prDate = p.biopsyDate;
                                    } else if (isSameDate) {
                                        pr += p.pr;
                                    } else if (Math.abs(DAYS.between(mrs.scanDate, prDate)) > Math.abs(DAYS.between(mrs.scanDate, p.biopsyDate))) { // find closest
                                        pr = p.pr;
                                        prDate = p.biopsyDate;
                                    }

                                } else {
                                    pr = p.pr;
                                    prDate = p.biopsyDate;
                                }
                            }
                        }
                    }
                    mrs.pr = nopr ? Integer.MIN_VALUE : pr;
                    //DBG.accept(mrs.studyID + "\t" + mrs.pr + "\n");
                }
            });
        });
        {
            StringBuilder sb = new StringBuilder();
            studyDiagnosisList.keySet().stream().forEach((mrs) -> {
                sb.append(mrs.patientID).append(",");
                sb.append(mrs.hospital).append(",");
                sb.append(mrs.studyID).append(",");
                sb.append(mrs.scanDate).append(",");
                sb.append(studyDiagnosisList.get(mrs)).append(",");
                sb.append(mrs.er == Integer.MIN_VALUE ? "-" : mrs.er).append(",");
                sb.append(mrs.pr == Integer.MIN_VALUE ? "-" : mrs.pr).append(",");
                sb.append(mrs.her2 == Integer.MIN_VALUE ? "-" : mrs.her2).append(",");
                sb.append(mrs.ki67 == Double.MIN_VALUE ? "-" : mrs.ki67);
                sb.append("\n");
            });
            FileUtils.writeStringToFile(new File(DATA_ROOT + "MR.csv"), sb.toString());
        }
    }

    static final String SMHT = "SMHT";
    static final String TMUH = "TMUH";
    static final String HOSPITAL = "";
    static final String DATA_ROOT = "D:\\Dropbox\\";

    static class Pathology {

        private String pathologyID;
        private String patientID;
        private LocalDate biopsyDate;
        private String diagnosis;
        private boolean isHER2Only;
        private boolean isHER2Fail;
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
        private String studyID = "";
        private LocalDate scanDate;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(pathologyID).append(",");
            sb.append(patientID).append(",");
            sb.append(hasLeft ? hasRight ? "雙" : "左" : hasRight ? "右" : "-").append(",");
            sb.append(biopsyDate).append(",");
            sb.append("\"").append(diagnosis).append("\"").append(",");
            //sb.append(isHER2Only ? "V" : "-").append(",");
            //sb.append(isHER2Fail ? "V" : "-").append(",");
            sb.append(isCoreNeedleBiopsy ? "V" : "-").append(",");
            //sb.append(hasLeft ? "V" : "-").append(",");
            //sb.append(hasRight ? "V" : "-").append(",");
            //sb.append(hasIDC ? "V" : "-").append(",");
            //sb.append(hasDCIS ? "V" : "-").append(",");
            //sb.append(hasILC ? "V" : "-").append(",");
            //sb.append(hasBenign ? "V" : "-").append(",");
            sb.append("\"").append(getDiagnosisSummary()).append("\"").append(",");
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
            sb.append(studyID.equals("") ? "-" : studyID).append(",");
            sb.append(studyID.equals("") ? "-" : scanDate);

            sb.append("\n");
            return sb.toString();
        }

        public String getDiagnosisSummary() {
            StringBuilder sb = new StringBuilder();

            //sb.append(isHER2Only ? (sb.length() != 0 ? " + " : "") + "HER2" : "");
            sb.append(isHER2Fail ? (sb.length() != 0 ? " + " : "") + "HER2" : "");
            sb.append(hasIDC ? (sb.length() != 0 ? " + " : "") + "IDC" : "");
            sb.append(hasDCIS ? (sb.length() != 0 ? " + " : "") + "DCIS" : "");
            sb.append(hasILC ? (sb.length() != 0 ? " + " : "") + "ILC" : "");
            sb.append(hasBenign ? (sb.length() != 0 ? " + " : "") + "benign" : "");
            sb.append(sb.toString().equals("") ? "-" : "");

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
        String diagnosisSummary;
        int her2 = Integer.MIN_VALUE;
        double ki67 = Double.MIN_VALUE;
        int er = Integer.MIN_VALUE;
        int pr = Integer.MAX_VALUE;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(patientID).append(",");
            sb.append(hospital).append(",");
            sb.append(studyID).append(",");
            sb.append(scanDate).append(",");
            sb.append("\n");
            return sb.toString();
        }

        public static MRStudy getClosestMRStudy(List<MRStudy> list, LocalDate targetDate) {

            List<LocalDate> dates = new ArrayList<>();
            list.stream().forEach((mrs) -> {
                dates.add(mrs.scanDate);
            });

            LocalDate before = null;
            long closestBeforeAndEqualDateBetween = Long.MAX_VALUE;
            for (LocalDate d : dates) {
                long monthsBetween = MONTHS.between(targetDate, d);
                long daysBetween = DAYS.between(targetDate, d);
                if (Math.abs(monthsBetween) > 6 || daysBetween > 0) {
                    continue;
                }
                //DBG.accept("before\t" + d + "\t" + monthsBetween + "\t" + daysBetween + "\n");
                if (closestBeforeAndEqualDateBetween == Long.MAX_VALUE) {
                    before = d;
                    closestBeforeAndEqualDateBetween = daysBetween;
                } else if (Math.abs(closestBeforeAndEqualDateBetween) > Math.abs(daysBetween)) {
                    before = d;
                    closestBeforeAndEqualDateBetween = daysBetween;
                }
            }
            //DBG.accept(before + "\t" + closestBeforeAndEqualDateBetween + "\n");

            LocalDate after = null;
            long closestAfterDateBetween = Long.MAX_VALUE;
            for (LocalDate d : dates) {
                long monthsBetween = MONTHS.between(targetDate, d);
                long daysBetween = DAYS.between(targetDate, d);
                if (Math.abs(monthsBetween) > 3 || daysBetween <= 0) {
                    continue;
                }
                //DBG.accept("after\t" + d + "\t" + monthsBetween + "\t" + daysBetween + "\n");
                if (closestAfterDateBetween == Long.MAX_VALUE) {
                    after = d;
                    closestAfterDateBetween = daysBetween;
                } else if (Math.abs(closestAfterDateBetween) > Math.abs(daysBetween)) {
                    after = d;
                    closestAfterDateBetween = daysBetween;
                }
            }
            //DBG.accept(after + "\t" + closestAfterDateBetween + "\n");

            LocalDate finalDate = (before != null)
                    ? Math.abs(closestBeforeAndEqualDateBetween) < Math.abs(closestAfterDateBetween)
                    ? before
                    : Math.abs(closestBeforeAndEqualDateBetween) <= 30 ? before : after
                    : after;
            //DBG.accept(finalDate + "\n");

            for (MRStudy mrs : list) {
                if (mrs.scanDate.equals(finalDate)) {
                    return mrs;
                }
            }

            return null;
        }
    }
}
