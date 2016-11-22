package mip.data.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import mip.util.LogUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ju
 */
public class Pathology {

    private static final String DATA_ROOT = "D:/Dropbox/";
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final String DF = "yyyy-MM-dd";
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern(DF);
    private static final LocalDate DEFAULT_DATE = LocalDate.of(1900, 01, 01);
    private static final String IMMUNO_KEYWORD = "IMMUNOHISTOCHEMICAL STUDY";
    private static final Logger LOG = LogUtils.LOGGER;

    public static void main(String[] args) throws IOException {
        Map<String, TreeSet<BMR>> bmrList = new TreeMap<>();
        {
            final String fn = DATA_ROOT + "pList.txt";
            try (BufferedReader in = new BufferedReader(new FileReader(fn))) {
                int count = 0;
                while (true) {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    count++;

                    String[] tokens = line.split("\t");
                    assert (tokens.length == 4);
                    BMR bmr = new BMR();
                    bmr.hospital = tokens[0].trim();
                    bmr.studyID = tokens[1].trim();
                    bmr.scanDate = LocalDate.parse(tokens[2].trim(), DT);
                    String pid = tokens[3].trim();
                    if (!bmrList.containsKey(pid)) {
                        bmrList.put(pid, new TreeSet<>());
                    }
                    bmrList.get(pid).add(bmr);
                }
                LOG.log(Level.FINE, "{0} MR studies", count);
            }
        }

        TreeMap<String, Pathology> pathologyList = new TreeMap<>();
        Set<BMR> bmrWithDiagnosisSet = new TreeSet<>();
        {
            final String fn = DATA_ROOT + "ALL_DATA";
            try (BufferedReader in = new BufferedReader(new FileReader(fn))) {

                int count = 0;
                while (true) { // for each pathology

                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }

                    String[] tokens = line.split("\t");
                    String pid = tokens[0];
                    String en = tokens[1];
                    String decodeText = new String(DECODER.decode(en), "UTF-8");
                    String[] lines = decodeText.replace("\r", "").split("\n");

                    String patientName = "";
                    String pathologyID = "";
                    LocalDate biopsyDate = LocalDate.MIN;
                    LocalDate receiveDate = LocalDate.MIN;
                    boolean flagDiagnosisStart = false;
                    boolean flagPathologyStart = false;
                    boolean flagImmunohistochemicalStart = false;
                    String diagnosisText = "";
                    String pathologyText = "";
                    String immunohistochemicalText = "";
                    final int IMMUNO_SEARCH_WINDOW_SIZE = 20;
                    int immunoSearchWindowSizeinLine = IMMUNO_SEARCH_WINDOW_SIZE;
                    String ki67Text = "";
                    double ki67 = Double.MIN_VALUE;

                    Pathology p;
                    // <editor-fold defaultstate="collapsed" desc="parse a Pathology">
                    for (final String s : lines) {

                        if (s.contains("病理切片報告病理號碼")) {
                            tokens = s.split("：");
                            pathologyID = (tokens.length == 2)
                                    ? tokens[1]
                                    : "ERROR!!!!";
                        }

                        if (s.contains("名字")) {
                            tokens = s.split("：");
                            patientName = (tokens.length == 2)
                                    ? tokens[1]
                                    : "無名氏";
                        }

                        if (s.contains("切片日期")) {
                            tokens = s.split("：");
                            try {
                                biopsyDate = (tokens.length == 2)
                                        ? LocalDate.parse(tokens[1].trim(), DT)
                                        : DEFAULT_DATE;
                            } catch (DateTimeParseException e) {
                                biopsyDate = DEFAULT_DATE;
                            }
                        }

                        if (s.contains("收件日期")) {
                            tokens = s.split("：");
                            receiveDate = (tokens.length == 2)
                                    ? LocalDate.parse(tokens[1].trim(), DT)
                                    : DEFAULT_DATE;
                        }

                        if (s.contains("病理診斷")) {
                            flagDiagnosisStart = true;
                        }

                        if (s.contains("組織報告")) {
                            flagDiagnosisStart = false;
                            flagPathologyStart = true;
                        }

                        if (s.contains("主治醫師")) {
                            flagPathologyStart = false;
                        }

                        if (s.contains(IMMUNO_KEYWORD) && flagPathologyStart) {
                            flagImmunohistochemicalStart = true;
                            immunoSearchWindowSizeinLine = IMMUNO_SEARCH_WINDOW_SIZE;
                        }

                        if (flagImmunohistochemicalStart) {
                            // Ki-67
                            if (StringUtils.containsIgnoreCase(s, "ki-67") && s.contains("%")) {
                                int start = StringUtils.indexOfIgnoreCase(s, "ki-67") + 5;
                                int end = StringUtils.indexOfIgnoreCase(s, "%") + 1;

                                assert (end > start);

                                ki67Text = StringUtils.substring(s, start, end)
                                        .replace("-", " - ")
                                        .replace(" %", "%");
                                ki67Text = (ki67Text.indexOf(":") == 0)
                                        ? ki67Text.substring(1).trim().toLowerCase()
                                        : ki67Text.trim().toLowerCase();
                                ki67 = Pathology.parseKi67(ki67Text);
                            }

                            if (s.contains("METHOD")) {
                                flagImmunohistochemicalStart = false;
                            }

                            if (immunoSearchWindowSizeinLine < 0) {
                                flagImmunohistochemicalStart = false;
                            }

                            if (!flagPathologyStart) {
                                flagImmunohistochemicalStart = false;
                            }
                        }

                        assert (!(flagImmunohistochemicalStart && !flagPathologyStart));

                        if (flagDiagnosisStart) {
                            diagnosisText += s.trim().length() != 0
                                    ? (s + "\n") : "";
                        }

                        if (flagPathologyStart) {
                            pathologyText += s.trim().length() != 0
                                    ? (s + "\n") : "";
                        }

                        if (flagImmunohistochemicalStart) {
                            immunohistochemicalText += s.trim().length() != 0
                                    ? (s + "\n") : "";
                            immunoSearchWindowSizeinLine--;
                        }
                    }

                    biopsyDate = biopsyDate.equals(DEFAULT_DATE)
                            ? (receiveDate.equals(DEFAULT_DATE)
                            ? DEFAULT_DATE
                            : receiveDate)
                            : biopsyDate;

                    assert (!pathologyID.equals("ERROR!!!!"));
                    assert (!biopsyDate.equals(DEFAULT_DATE));
                    assert (!patientName.equals("無名氏"));

                    p = new Pathology(pid, patientName, pathologyID, biopsyDate);
                    pathologyList.put(pathologyID, p);
                    // </editor-fold>

                    // <editor-fold defaultstate="collapsed" desc="analyse diagnoses of the Pathology">
                    diagnosisText = StringUtils.replace(
                            StringUtils.replace(
                                    diagnosisText,
                                    "\"", // for spreadsheet
                                    "'"
                            ).trim(),
                            "病理診斷：", // remove title
                            "");

                    lines = diagnosisText.split("\n");
                    for (final String s : lines) {

                        Diagnosis d = new Diagnosis(s);
                        {
                            if (d.side == Side.MIXED) {
                                continue; // ignore diagnosis with ambiguous Side
                            }
                            d.pathologyLink = p;
                            d.bmrLink = BMR.getBMR(d, bmrList.get(pid), biopsyDate);
                            // a pathology may have many diagnoses
                            p.diagnosisList.add(d);
                        }

                        BMR bmr = d.bmrLink;
                        {
                            if (bmr == null) {
                                continue;
                            }

                            assert (d.cancerType != CancerType.UNKNOWN
                                    && d.region != Region.UNKNOWN
                                    && d.side != Side.UNKNOWN);

                            switch (d.side) {
                                case LEFT:
                                    if (d.cancerType == CancerType.MIXED) {
                                        bmr.leftMixedDiagnosisList.add(d);
                                    } else {
                                        bmr.leftDiagnosisList.add(d);
                                    }
                                    break;
                                case RIGHT:
                                    if (d.cancerType == CancerType.MIXED) {
                                        bmr.rightMixedDiagnosisList.add(d);
                                    } else {
                                        bmr.rightDiagnosisList.add(d);
                                    }
                                    break;
                            }

                            bmrWithDiagnosisSet.add(bmr);
                        }
                    }
                    // </editor-fold>

                    // <editor-fold defaultstate="collapsed" desc="analyse immunohistochemical of the Pathology">
                    int immunoCount = StringUtils.countMatches(pathologyText, IMMUNO_KEYWORD);
                    p.immuno = new Immuno(
                            (immunoCount > 0)
                                    ? StringUtils.replace(immunohistochemicalText,
                                            "\"", // for spreadsheet
                                            "'").trim()
                                    : "-");
                    p.immuno.ki67 = ki67;
                    // </editor-fold>
                } // for each pathology
            }
        }
        // <editor-fold defaultstate="collapsed" desc="output and measurement">
        StringBuilder allPathology = new StringBuilder(4096);
        {
            int sumLeft = 0;
            int sumRight = 0;
            int sumUnknownSide = 0;
            int sumBenignBreast = 0;
            int sumBenignNotBreast = 0;
            int sumIDC = 0;
            int sumDCIS = 0;
            int sumMixedType = 0;
            int sumUnknownType = 0;
            int sumNeedle = 0;
            int sumExcision = 0;
            int sumUnknownBiopsy = 0;

            for (Pathology p : pathologyList.values()) {
                allPathology.append(p);

                for (Diagnosis d : p.diagnosisList) {
                    sumLeft += d.side == Side.LEFT ? 1 : 0;
                    sumRight += d.side == Side.RIGHT ? 1 : 0;
                    sumUnknownSide += d.side == Side.UNKNOWN ? 1 : 0;
                    //
                    sumBenignBreast += d.cancerType == CancerType.BENIGN && d.region == Region.BREAST ? 1 : 0;
                    sumBenignNotBreast += d.cancerType == CancerType.BENIGN && d.region != Region.BREAST ? 1 : 0;
                    sumIDC += d.cancerType == CancerType.IDC ? 1 : 0;
                    sumDCIS += d.cancerType == CancerType.DCIS ? 1 : 0;
                    sumMixedType += d.cancerType == CancerType.MIXED ? 1 : 0;
                    sumUnknownType += d.cancerType == CancerType.UNKNOWN ? 1 : 0;
                    //
                    sumNeedle += d.biopsyType == Biopsy.NEEDLE ? 1 : 0;
                    sumExcision += d.biopsyType == Biopsy.EXCISIONAL ? 1 : 0;
                    sumUnknownBiopsy += d.biopsyType == Biopsy.UNKNOWN ? 1 : 0;
                }
            }
            FileUtils.writeStringToFile(new File(DATA_ROOT + "PATHOLOGY_ALL.csv"), allPathology.toString());

            long sumSide = sumLeft + sumRight + sumUnknownSide;
            long sumCancerType = sumBenignBreast + sumIDC + sumDCIS
                    + sumMixedType
                    + sumUnknownType + sumBenignNotBreast;
            long sumBiopsyType = sumNeedle + sumExcision + sumUnknownBiopsy;
            assert (sumSide == sumCancerType && sumSide == sumBiopsyType);

            LOG.log(Level.FINE, "{0} pathology reports", pathologyList.size());
            LOG.log(Level.FINE, "{0} diagnoses are within these reports.\n", sumSide);
            //
            LOG.log(Level.FINE, "\t{0} left diagnoses", sumLeft);
            LOG.log(Level.FINE, "\t{0} right diagnoses", sumRight);
            LOG.log(Level.FINE, "\t{0} diagnoses with unknown side\n", sumUnknownSide);
            //
            LOG.log(Level.FINE, "\t{0} benign", sumBenignBreast);
            LOG.log(Level.FINE, "\t{0} IDC", sumIDC);
            LOG.log(Level.FINE, "\t{0} DCIS", sumDCIS);
            LOG.log(Level.FINE, "\t{0} multiple type breast lesions", sumMixedType);
            LOG.log(Level.FINE, "\t{0} are ignored\n", (sumUnknownType + sumBenignNotBreast));
            //
            LOG.log(Level.FINE, "\t{0} needle biopsy", sumNeedle);
            LOG.log(Level.FINE, "\t{0} excision", sumExcision);
            LOG.log(Level.FINE, "\t{0} unknown type fo biopsy\n", sumUnknownBiopsy);
        }

        StringBuilder mrOneSideSingleDiagnosis = new StringBuilder(4096);
        {
            bmrWithDiagnosisSet.stream().forEach((bmr) -> {
                bmr.mergeByCancerType();
            });

            for (BMR bmr : bmrWithDiagnosisSet) {

                int ld = bmr.leftDiagnosisList.size();
                int rd = bmr.rightDiagnosisList.size();
                int lmd = bmr.leftMixedDiagnosisList.size();
                int rmd = bmr.rightMixedDiagnosisList.size();

                assert (ld + rd + lmd + rmd != 0);

                if (lmd != 0 || rmd != 0) {
                    continue;
                }

                assert (lmd + rmd == 0);

                if (ld > 1 || rd > 1) {
                    continue;
                }

                assert (ld == 1 || rd == 1);

                if (ld == 1) {
                    mrOneSideSingleDiagnosis.append(bmr.leftDiagnosisList.get(0).toString());
                }

                if (rd == 1) {
                    mrOneSideSingleDiagnosis.append(bmr.rightDiagnosisList.get(0).toString());
                }
            }
            FileUtils.writeStringToFile(new File(DATA_ROOT + "MR_SINGLE_DIAGNOSIS.csv"), mrOneSideSingleDiagnosis.toString());
        }

        {
            int diagnoses = 0;
            int diagnosesMerged = 0;
            int bmrMixedDiagnoses = 0;
            int bmrOnlyLeftSideOneDiagnosis = 0;
            int bmrOnlyRightSideOneDiagnosis = 0;
            int bmrBothSideOneDiagnosis = 0;

            for (BMR bmr : bmrWithDiagnosisSet) {

                diagnosesMerged += bmr.duplicateTypeDiagnosisList.size();
                diagnoses += bmr.getTotalDiagnosesCount();

                if (!bmr.leftMixedDiagnosisList.isEmpty() || !bmr.rightMixedDiagnosisList.isEmpty()) {
                    bmrMixedDiagnoses++;
                } else {
                    assert (bmr.leftMixedDiagnosisList.isEmpty() && bmr.rightMixedDiagnosisList.isEmpty());

                    if (bmr.leftDiagnosisList.size() == 1 && bmr.rightDiagnosisList.size() == 1) {
                        bmrBothSideOneDiagnosis++;
                    } else if (bmr.leftDiagnosisList.size() == 1 && bmr.rightDiagnosisList.isEmpty()) {
                        bmrOnlyLeftSideOneDiagnosis++;
                    } else if (bmr.rightDiagnosisList.size() == 1 && bmr.leftDiagnosisList.isEmpty()) {
                        bmrOnlyRightSideOneDiagnosis++;
                    } else {
                        bmrMixedDiagnoses++;
                    }
                }
            }

            int daignosesHasBMR = 0;
            for (Pathology p : pathologyList.values()) {
                daignosesHasBMR = p.diagnosisList.stream().map((mip.data.report.Diagnosis d) -> d.bmrLink != null ? 1 : 0).reduce(daignosesHasBMR, Integer::sum);
            }
            assert (diagnoses + diagnosesMerged == daignosesHasBMR);
            int sum = bmrOnlyLeftSideOneDiagnosis
                    + bmrOnlyRightSideOneDiagnosis
                    + bmrBothSideOneDiagnosis
                    + bmrMixedDiagnoses;
            assert (bmrWithDiagnosisSet.size() == sum);

            LOG.log(Level.FINE, "{0} MR has diagnosis", bmrWithDiagnosisSet.size());
            LOG.log(Level.FINE, "\t{0} only left diagnosis", bmrOnlyLeftSideOneDiagnosis);
            LOG.log(Level.FINE, "\t{0} only right diagnosis", bmrOnlyRightSideOneDiagnosis);
            LOG.log(Level.FINE, "\t{0} left and right diagnosis", bmrBothSideOneDiagnosis);
            LOG.log(Level.FINE, "\t{0} ignored diagnosis", bmrMixedDiagnoses);
        }
        // </editor-fold>
    }

    public static double parseKi67(String ki67Text) {
        double ret = Double.MIN_VALUE;
        Scanner fi = new Scanner(ki67Text);
        fi.useDelimiter("[^\\p{Alnum},\\.-]");
        while (true) {
            if (fi.hasNextInt()) {
                ret = fi.nextInt();
            } else if (fi.hasNextDouble()) {
                ret = fi.nextDouble();
            } else if (fi.hasNext()) {
                fi.next();
            } else {
                break;
            }
        }

        return ret;
    }

    final String patientID;
    final String patientName;
    final String pathologyID;
    final LocalDate biopsyDate;
    final List<Diagnosis> diagnosisList = new ArrayList<>(10);
    Immuno immuno;

    public Pathology(String pid, String pName, String pathID, LocalDate date) {
        patientID = pid;
        patientName = pName;
        pathologyID = pathID;
        biopsyDate = date;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);

        for (Diagnosis d : diagnosisList) {
            sb.append("\"'").append(patientID).append("\"").append(",");
            sb.append("\"'").append(patientName).append("\"").append(",");
            sb.append(biopsyDate).append(",");
            sb.append(d.region).append(",");
            sb.append(d.side).append(",");
            sb.append(d.biopsyType).append(",");
            sb.append(d.cancerType).append(",");
            sb.append("\"").append(d.diagnosisText).append("\"").append(",");
            sb.append(pathologyID).append(",");

            sb.append((d.bmrLink != null) ? d.bmrLink.hospital : "-").append(",");
            sb.append((d.bmrLink != null) ? d.bmrLink.studyID : "-").append(",");
            sb.append((d.bmrLink != null) ? d.bmrLink.scanDate : "-").append(",");

            sb.append(immuno);

            sb.append("\n");
        }
        return sb.toString();
    }
}
