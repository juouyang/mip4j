
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import mip.data.image.mr.BMRStudy;
import mip.data.image.mr.Kinetic;
import static mip.util.DebugUtils.DBG;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
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

    private static final String DATA_ROOT = "/home/ju/Dropbox/";
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final LocalDate DEFAULT_DATE = LocalDate.of(1900, 01, 01);
    private static final String IMMUNO_KEYWORD = "IMMUNOHISTOCHEMICAL STUDY";

    private final String patientID;
    private final String patientName;
    private final String pathologyID;
    private final LocalDate biopsyDate;
    private final List<Diagnosis> diagnosisList = new ArrayList<>();

    public Pathology(String pid, String pName, String pathID, LocalDate date) {
        patientID = pid;
        patientName = pName;
        pathologyID = pathID;
        biopsyDate = date;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Map<String, TreeSet<BMR>> bmrList = new TreeMap<>();
        //<editor-fold defaultstate="collapsed" desc="Breast MRI">
        try (BufferedReader in = new BufferedReader(new FileReader(DATA_ROOT + "pList.txt"))) {
            int summaryBMRCount = 0;
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                summaryBMRCount++;

                //<editor-fold defaultstate="collapsed" desc="Construct BMR">
                String[] tokens = line.split("\t");
                assert (tokens.length == 4);
                BMR bmr = new BMR();
                bmr.hospital = tokens[0].trim();
                bmr.studyID = tokens[1].trim();
                bmr.scanDate = LocalDate.parse(tokens[2].trim(), DT_FORMATTER);
                bmr.patientID = tokens[3].trim();
                if (!bmrList.containsKey(bmr.patientID)) {
                    bmrList.put(bmr.patientID, new TreeSet<>());
                }
                bmrList.get(bmr.patientID).add(bmr);
                //</editor-fold>
            }

            //<editor-fold defaultstate="collapsed" desc="Summary">
            DBG.accept(summaryBMRCount + " MR studies are processed.\n");
            //</editor-fold>
        }
        //</editor-fold>

        TreeMap<String, Pathology> pathologyList = new TreeMap<>();
        Set<BMR> bmrHasDiagnosisSet = new TreeSet<>();
        //<editor-fold defaultstate="collapsed" desc="Pathology">
        try (BufferedReader in = new BufferedReader(new FileReader(DATA_ROOT + "ALL_DATA"))) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                //<editor-fold defaultstate="collapsed" desc="Decode Base64 Text">
                String[] tokens = line.split("\t");
                String pid = tokens[0];
                String encodedText = tokens[1];
                String decodeText = new String(DECODER.decode(encodedText), "UTF-8").replace("\r", "");
                String[] lines = decodeText.split("\n");
                //</editor-fold>

                //<editor-fold defaultstate="collapsed" desc="Parse a Pathology">
                String patientName = "";
                String pathologyID = "";
                LocalDate biopsyDate = LocalDate.MIN;
                LocalDate receiveDate = LocalDate.MIN;
                boolean isDiagnosisStart = false;
                String diagnosisText = "";
                boolean isPathologyStart = false;
                String pathologyText = "";
                boolean isImmunohistochemicalStart = false;
                String immunohistochemicalText = "";
                final int IMMUNO_SEARCH_WINDOW_SIZE = 20;
                int immunoSearchWindowSizeinLine = IMMUNO_SEARCH_WINDOW_SIZE;
                for (final String s : lines) {

                    if (s.contains("病理切片報告病理號碼")) {
                        tokens = s.split("：");
                        pathologyID = (tokens.length == 2) ? tokens[1] : "ERROR!!!!";
                    }

                    if (s.contains("名字")) {
                        tokens = s.split("：");
                        patientName = (tokens.length == 2) ? tokens[1] : "無名氏";
                    }

                    if (s.contains("切片日期")) {
                        tokens = s.split("：");
                        try {
                            biopsyDate = (tokens.length == 2) ? LocalDate.parse(tokens[1].trim(), DT_FORMATTER) : DEFAULT_DATE;
                        } catch (DateTimeParseException e) {
                            biopsyDate = DEFAULT_DATE;
                        }
                    }

                    if (s.contains("收件日期")) {
                        tokens = s.split("：");
                        receiveDate = (tokens.length == 2) ? LocalDate.parse(tokens[1].trim(), DT_FORMATTER) : DEFAULT_DATE;
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
                    }

                    if (s.contains(IMMUNO_KEYWORD) && isPathologyStart) {
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

                    if (isDiagnosisStart) {
                        diagnosisText += (s.trim().length() != 0) ? (s + "\n") : "";
                    }

                    if (isPathologyStart) {
                        pathologyText += (s.trim().length() != 0) ? (s + "\n") : "";
                    }

                    if (isImmunohistochemicalStart) {
                        immunohistochemicalText += s.trim().length() != 0 ? (s + "\n") : "";
                        immunoSearchWindowSizeinLine--;
                    }
                }
                //</editor-fold>

                //<editor-fold defaultstate="collapsed" desc="Construct Pathology">
                biopsyDate = biopsyDate.equals(DEFAULT_DATE) ? (!receiveDate.equals(DEFAULT_DATE) ? receiveDate : DEFAULT_DATE) : biopsyDate;
                diagnosisText = StringUtils.replace(StringUtils.replace(diagnosisText, "\"", "'").trim(), "病理診斷：", "");

                assert (!pathologyID.equals("ERROR!!!!"));  // must have pathology ID
                assert (!biopsyDate.equals(DEFAULT_DATE)); // must have biopsy date

                lines = diagnosisText.split("\n");
                for (final String s : lines) {
                    Pathology p;
                    {
                        if (pathologyList.get(pathologyID) == null) {
                            pathologyList.put(pathologyID, new Pathology(pid, patientName, pathologyID, biopsyDate));
                        }
                        p = pathologyList.get(pathologyID);
                    }

                    Location l;
                    {
                        if (StringUtils.startsWithIgnoreCase(s, Location.BREAST.keywords[0]) || StringUtils.containsIgnoreCase(s, Location.BREAST.keywords[1])) {
                            l = Location.BREAST;
                        } else if (StringUtils.containsIgnoreCase(s, Location.LN.keywords[0])) {
                            l = Location.LN;
                        } else if (StringUtils.containsIgnoreCase(s, Location.SKIN.keywords[0])) {
                            l = Location.SKIN;
                        } else {
                            l = Location.UNKNOWN;
                        }
                    }

                    Diagnosis d = new Diagnosis(l);
                    {
                        Side side;
                        {
                            if (StringUtils.containsIgnoreCase(s, "left") && StringUtils.containsIgnoreCase(s, "right")) {
                                continue;
                            }
                            assert (!(StringUtils.containsIgnoreCase(s, "left") && StringUtils.containsIgnoreCase(s, "right"))); // right and left in same line

                            side = StringUtils.containsIgnoreCase(s, "left") ? Side.LEFT : StringUtils.containsIgnoreCase(s, "right") ? Side.RIGHT : Side.UNKNOWN;
                        }
                        d.side = side;

                        Biopsy b;
                        {
                            b = Biopsy.UNKNOWN;
                            if ((s.contains(Biopsy.NEEDLE.keywords[0]) && s.contains(Biopsy.NEEDLE.keywords[1])) || s.contains(Biopsy.NEEDLE.keywords[2]) || s.contains(Biopsy.NEEDLE.keywords[3])) {
                                b = Biopsy.NEEDLE;
                            }
                            for (String keyword : Biopsy.EXCISIONAL.keywords) {
                                if (s.contains(keyword)) {
                                    b = Biopsy.EXCISIONAL;
                                    break;
                                }
                            }
                        }
                        d.biopsy = b;

                        BreastCancer t;
                        {
                            boolean hasIDC = false;
                            boolean hasDCIS = false;
                            boolean hasBenign = false;
                            for (String searchString : BreastCancer.IDC.keywords) {
                                if (StringUtils.containsIgnoreCase(s, "no residual " + searchString) || StringUtils.containsIgnoreCase(s, searchString + ", removed") || StringUtils.containsIgnoreCase(s, searchString + " removed")) {
                                    hasBenign = true;
                                    continue;
                                }
                                assert (!StringUtils.containsIgnoreCase(s, "no residual " + searchString) && !StringUtils.containsIgnoreCase(s, searchString + ", removed") || StringUtils.containsIgnoreCase(s, searchString + " removed"));
                                if (StringUtils.containsIgnoreCase(s, searchString)) {
                                    hasIDC = true;
                                    break;
                                }
                            }
                            for (String searchString : BreastCancer.DCIS.keywords) {
                                if (StringUtils.containsIgnoreCase(s, "no residual " + searchString) || StringUtils.containsIgnoreCase(s, searchString + ", removed") || StringUtils.containsIgnoreCase(s, searchString + " removed")) {
                                    hasBenign = true;
                                    continue;
                                }
                                assert (!StringUtils.containsIgnoreCase(s, "no residual " + searchString) && !StringUtils.containsIgnoreCase(s, searchString + ", removed") || StringUtils.containsIgnoreCase(s, searchString + " removed"));
                                if (StringUtils.containsIgnoreCase(s, searchString)) {
                                    hasDCIS = true;
                                    break;
                                }
                            }
                            hasBenign = hasBenign ? !hasIDC && !hasDCIS : hasBenign;
                            for (String searchString : BreastCancer.BENIGN.keywords) {
                                if (StringUtils.containsIgnoreCase(s, searchString)) {
                                    hasBenign = true;
                                    break;
                                }
                            }
                            int diagnosisCount = 0;
                            diagnosisCount += hasIDC ? 1 : 0;
                            diagnosisCount += hasDCIS ? 1 : 0;
                            diagnosisCount += hasBenign ? 1 : 0;
                            t = (diagnosisCount == 0)
                                    ? BreastCancer.NOT
                                    : ((diagnosisCount == 1)
                                            ? (hasIDC
                                                    ? BreastCancer.IDC
                                                    : (hasDCIS
                                                            ? BreastCancer.DCIS
                                                            : (hasBenign
                                                                    ? BreastCancer.BENIGN
                                                                    : null)))
                                            : BreastCancer.MIX);
                            assert (t != null);
                        }
                        d.cancerType = t;

                        d.diagnosisText = s;
                        d.pathology = p;
                        d.bmr = BMR.getClosestBMRBeforeBiopsyDate(d, bmrList.get(pid), biopsyDate);
                        p.diagnosisList.add(d);
                    }

                    BMR bmr = d.bmr;
                    {
                        if (bmr == null) {
                            continue;
                        }

                        assert (d.side != Side.UNKNOWN && d.cancerType != BreastCancer.NOT && d.location != Location.UNKNOWN);

                        switch (d.side) {
                            case LEFT:
                                if (d.cancerType == BreastCancer.MIX) {
                                    bmr.leftMixedDiagnosisList.add(d);
                                } else {
                                    bmr.leftDiagnosisList.add(d);
                                }
                                break;
                            case RIGHT:
                                if (d.cancerType == BreastCancer.MIX) {
                                    bmr.rightMixedDiagnosisList.add(d);
                                } else {
                                    bmr.rightDiagnosisList.add(d);
                                }
                                break;
                        }

                        bmrHasDiagnosisSet.add(bmr);
                    }
                } // for each diagnosis
                //</editor-fold>
            }

            //<editor-fold defaultstate="collapsed" desc="Merge diagnoses">]
            for (BMR bmr : bmrHasDiagnosisSet) {
                if (bmr.leftDiagnosisList.size() > 1) {
                    Diagnosis keepMalignant = null;
                    Diagnosis keepBenign = null;
                    for (Diagnosis d : bmr.leftDiagnosisList) {
                        if (d.cancerType != BreastCancer.BENIGN) {
                            keepMalignant = d;
                        }

                        if (d.cancerType == BreastCancer.BENIGN) {
                            keepBenign = d;
                        }
                    }

                    assert (keepMalignant != null || keepBenign != null);

                    if (keepMalignant != null && keepBenign != null) {
                        BMR.merge(keepMalignant, bmr, true);
                    } else {
                        BMR.merge(keepMalignant != null ? keepMalignant : keepBenign, bmr, false);
                    }
                }

                if (bmr.rightDiagnosisList.size() > 1) {
                    Diagnosis keepMalignant = null;
                    Diagnosis keepBenign = null;
                    for (Diagnosis d : bmr.rightDiagnosisList) {
                        if (d.cancerType != BreastCancer.BENIGN) {
                            keepMalignant = d;
                        }

                        if (d.cancerType == BreastCancer.BENIGN) {
                            keepBenign = d;
                        }
                    }

                    assert (keepMalignant != null || keepBenign != null);

                    if (keepMalignant != null && keepBenign != null) {
                        BMR.merge(keepMalignant, bmr, true);
                    } else {
                        BMR.merge(keepMalignant != null ? keepMalignant : keepBenign, bmr, false);
                    }
                }
            }

            //</editor-fold>
            // 
            //<editor-fold defaultstate="collapsed" desc="Summary">
            {
                int summaryLeftCount = 0;
                int summaryRightCount = 0;
                int summaryUnknownSideCount = 0;
                int summaryBenignBreastCancerCount = 0;
                int summaryIDCCount = 0;
                int summaryDCISCount = 0;
                int summaryMixedBreastCancerCount = 0;
                int summaryNotBreastCancerCount = 0;
                int summaryBenignNotBreastCancerCount = 0;
                int summaryNeedleCount = 0;
                int summaryExcisionCount = 0;
                int summaryUnknownBiopsyCount = 0;

                StringBuilder allPathology = new StringBuilder();
                for (Pathology p : pathologyList.values()) {
                    for (Diagnosis d : p.diagnosisList) {
                        summaryLeftCount += d.side == Side.LEFT ? 1 : 0;
                        summaryRightCount += d.side == Side.RIGHT ? 1 : 0;
                        summaryUnknownSideCount += d.side == Side.UNKNOWN ? 1 : 0;
                        //
                        summaryBenignBreastCancerCount += d.cancerType == BreastCancer.BENIGN && d.location == Location.BREAST ? 1 : 0;
                        summaryBenignNotBreastCancerCount += d.cancerType == BreastCancer.BENIGN && d.location != Location.BREAST ? 1 : 0;
                        summaryIDCCount += d.cancerType == BreastCancer.IDC ? 1 : 0;
                        summaryDCISCount += d.cancerType == BreastCancer.DCIS ? 1 : 0;
                        summaryMixedBreastCancerCount += d.cancerType == BreastCancer.MIX ? 1 : 0;
                        summaryNotBreastCancerCount += d.cancerType == BreastCancer.NOT ? 1 : 0;
                        //
                        summaryNeedleCount += d.biopsy == Biopsy.NEEDLE ? 1 : 0;
                        summaryExcisionCount += d.biopsy == Biopsy.EXCISIONAL ? 1 : 0;
                        summaryUnknownBiopsyCount += d.biopsy == Biopsy.UNKNOWN ? 1 : 0;
                    }

                    allPathology.append(p);
                }
                FileUtils.writeStringToFile(new File(DATA_ROOT + "PATHOLOGY_ALL.csv"), allPathology.toString());

                assert (summaryLeftCount + summaryRightCount + summaryUnknownSideCount - summaryBenignBreastCancerCount - summaryBenignNotBreastCancerCount - summaryIDCCount - summaryDCISCount - summaryNotBreastCancerCount - summaryMixedBreastCancerCount == 0);
                assert (summaryLeftCount + summaryRightCount + summaryUnknownSideCount - summaryNeedleCount - summaryExcisionCount - summaryUnknownBiopsyCount == 0);

                DBG.accept("--\n");
                DBG.accept("\t" + summaryLeftCount + " left diagnoses\n"
                        + "\t" + summaryRightCount + " right diagnoses\n"
                        + "\t" + summaryUnknownSideCount + " diagnoses with unknown side\n");
                DBG.accept("--\n");
                DBG.accept("\t" + summaryBenignBreastCancerCount + " benign\n"
                        + "\t" + summaryIDCCount + " IDC\n"
                        + "\t" + summaryDCISCount + " DCIS\n"
                        + "\t" + summaryMixedBreastCancerCount + " mixed type breast lesions\n"
                        + "\t" + (summaryNotBreastCancerCount + summaryBenignNotBreastCancerCount) + " are not located in breast\n");
            }

            {
                StringBuilder mrOneSideSingleDiagnosis = new StringBuilder();
                for (BMR bmr : bmrHasDiagnosisSet) {

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
            //</editor-fold>

        } // close file
        //</editor-fold>

        int diagnosesCount = 0;
        int diagnosesMergedCount = 0;
        int mrWithMixedDiagnosisCount = 0;
        Set<BMR> bmrOnlyLeftSideSingleDiagnosisList = new TreeSet<>();
        Set<BMR> bmrOnlyRightSideSingleDiagnosisList = new TreeSet<>();
        Set<BMR> bmrBothSideSingleDiagnosisList = new TreeSet<>();
        Set<BMR> bmrHasSeveralDifferentDiagnosesList = new TreeSet<>();
        for (BMR bmr : bmrHasDiagnosisSet) {

            if (!bmr.leftMixedDiagnosisList.isEmpty() || !bmr.rightMixedDiagnosisList.isEmpty()) {
                mrWithMixedDiagnosisCount++;
            } else {
                assert (bmr.leftMixedDiagnosisList.isEmpty());
                assert (bmr.rightMixedDiagnosisList.isEmpty());

                if (bmr.leftDiagnosisList.size() == 1 && bmr.rightDiagnosisList.size() == 1) {
                    bmrBothSideSingleDiagnosisList.add(bmr);
                } else if (bmr.leftDiagnosisList.size() == 1 && bmr.rightDiagnosisList.isEmpty()) {
                    bmrOnlyLeftSideSingleDiagnosisList.add(bmr);
                } else if (bmr.rightDiagnosisList.size() == 1 && bmr.leftDiagnosisList.isEmpty()) {
                    bmrOnlyRightSideSingleDiagnosisList.add(bmr);
                } else {
                    bmrHasSeveralDifferentDiagnosesList.add(bmr);
                }
            }

            diagnosesMergedCount += bmr.mergedDiagnosisList.size();
            diagnosesCount += bmr.getTotalDiagnosesCount();
        }

        int daignosesHasBMRCount = 0;
        for (Pathology p : pathologyList.values()) {
            daignosesHasBMRCount = p.diagnosisList.stream().map((d) -> d.bmr != null ? 1 : 0).reduce(daignosesHasBMRCount, Integer::sum);
        }
        assert (diagnosesCount + diagnosesMergedCount == daignosesHasBMRCount);
        assert (bmrHasDiagnosisSet.size() == bmrOnlyLeftSideSingleDiagnosisList.size() + bmrOnlyRightSideSingleDiagnosisList.size() + bmrBothSideSingleDiagnosisList.size() + mrWithMixedDiagnosisCount + bmrHasSeveralDifferentDiagnosesList.size());

        for (BMR bmr : bmrOnlyLeftSideSingleDiagnosisList) {
            Path p = Paths.get(BMR.MR_ROOT + bmr.hospital + "/" + bmr.studyID);
            assert (p.toFile().exists());

            DBG.accept(p.toString() + "\n");
            Kinetic k = new Kinetic(new BMRStudy(p));
            DBG.accept(k.getSummary().toString() + "\n--\n");
            break;
        }
    }

    //<editor-fold defaultstate="collapsed" desc="toString">
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Diagnosis d : diagnosisList) {
            sb.append("\"'").append(patientID).append("\"").append(",");
            sb.append("\"'").append(patientName).append("\"").append(",");
            sb.append(biopsyDate).append(",");
            sb.append(d.location).append(",");
            sb.append(d.side).append(",");
            sb.append(d.biopsy).append(",");
            sb.append(d.cancerType).append(",");
            sb.append("\"").append(d.diagnosisText).append("\"").append(",");
            sb.append(pathologyID).append(",");

            sb.append((d.bmr != null) ? d.bmr.hospital : "-").append(",");
            sb.append((d.bmr != null) ? d.bmr.studyID : "-").append(",");
            sb.append((d.bmr != null) ? d.bmr.scanDate : "-");

            sb.append("\n");
        }
        return sb.toString();
    }

    private String toString(Diagnosis d) {
        if (!diagnosisList.contains(d)) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder();

        sb.append("\"'").append(String.format("%8s", patientID)).append("\"").append(",");
        sb.append(String.format("%10s", patientName)).append(",");
        sb.append(biopsyDate).append(",");
        sb.append(d.location).append(",");
        sb.append(d.side).append(",");
        sb.append(d.biopsy).append(",");
        sb.append(d.cancerType).append(",");
        sb.append("\"").append(d.diagnosisText).append("\"").append(",");
        sb.append(pathologyID).append(",");

        sb.append((d.bmr != null) ? d.bmr.hospital : "-").append(",");
        sb.append((d.bmr != null) ? d.bmr.studyID : "-").append(",");
        sb.append((d.bmr != null) ? d.bmr.scanDate : "-");

        sb.append("\n");
        return sb.toString();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="enum">
    public enum Location {

        UNKNOWN("unknown", null), SKIN("skin", new String[]{"skin"}), LN("lymph node", new String[]{"lymph node"}), BREAST("breast", new String[]{"breast", "nipple"});
        public final String[] keywords;
        private final String description;

        private Location(String s, String[] k) {
            description = s;
            keywords = k;
        }

        @Override
        public String toString() {
            return this.description;
        }
    }

    public enum Side {

        UNKNOWN("Unknown"), LEFT("Left"), RIGHT("Right");
        private final String description;

        private Side(String s) {
            description = s;
        }

        @Override
        public String toString() {
            return this.description;
        }
    }

    public enum Biopsy {

        UNKNOWN("Unknwon", null),
        EXCISIONAL("Excisional",
                new String[]{"excision", "mastectomy", "operative biopsy", "specimen"}
        ),
        NEEDLE("Needle",
                new String[]{"needle", "biopsy", ", biopsy", "incisional biopsy"}
        );

        public final String[] keywords;
        private final String description;

        private Biopsy(String d, String[] s) {
            description = d;
            keywords = s;
        }

        @Override
        public String toString() {
            return this.description;
        }
    }

    public enum BreastCancer {

        NOT("NOT", null),
        IDC("IDC",
                new String[]{" invasive carcinoma", "mucinous carcinoma", "invasive tubular carcinoma", "invasive lobular carcinoma", "carcinoma with invasion", "invasive ductal and lobular carcinoma", "infiltrating ductal carcinoma", "invasive ductal carcinoma", "IDC"}
        ),
        DCIS("DCIS",
                new String[]{"ductal carcinoma in situ"}
        ),
        BENIGN("benign",
                new String[]{"benign", "fibroadipose", "no carcinoma", "no metastasis", "no residual carcinoma", "no tumor involve", "negative for carcinoma", "fibroadenoma", "fibrocystic change", "fat necrosis"}
        ),
        MIX("mixed", null);

        public final String[] keywords;
        private final String description;

        private BreastCancer(String d, String[] s) {
            description = d;
            keywords = s;
        }

        @Override
        public String toString() {
            return this.description;
        }
    }
    //</editor-fold>

    private static class BMR implements Comparable<BMR> {

        public static final String MR_ROOT = "/home/ju/workspace/_BREAST_MRI/";

        String hospital;
        String studyID;
        LocalDate scanDate;
        String patientID;
        List<Diagnosis> leftDiagnosisList = new ArrayList<>();
        List<Diagnosis> rightDiagnosisList = new ArrayList<>();
        List<Diagnosis> leftMixedDiagnosisList = new ArrayList<>();
        List<Diagnosis> rightMixedDiagnosisList = new ArrayList<>();
        List<Diagnosis> mergedDiagnosisList = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(hospital).append("\t").append(studyID).append("\n");
            sb.append(ArrayUtils.toString(leftDiagnosisList.toArray())).append("\n");
            sb.append(ArrayUtils.toString(rightDiagnosisList.toArray())).append("\n");
            sb.append(ArrayUtils.toString(leftMixedDiagnosisList.toArray())).append("\n");
            sb.append(ArrayUtils.toString(rightMixedDiagnosisList.toArray())).append("\n");

            return sb.toString();
        }

        public int getTotalDiagnosesCount() {
            return leftDiagnosisList.size() + rightDiagnosisList.size() + leftMixedDiagnosisList.size() + rightMixedDiagnosisList.size();
        }

        public static BMR getClosestBMRBeforeBiopsyDate(Diagnosis d, Set<BMR> set, LocalDate targetDate) {
            if (set == null || d.cancerType == BreastCancer.NOT || d.location == Location.UNKNOWN || d.side == Side.UNKNOWN) { // not breast cancer, no link to BMR
                return null;
            }

            List<LocalDate> dates = new ArrayList<>();
            set.stream().forEach((mrs) -> {
                dates.add(mrs.scanDate);
            });

            LocalDate beforeAndEqual = null;
            long closestBeforeAndEqualDateBetween = Long.MAX_VALUE;
            for (LocalDate date : dates) {
                long monthsBetween = MONTHS.between(targetDate, date);
                long daysBetween = DAYS.between(targetDate, date);
                if (Math.abs(monthsBetween) > 6 || daysBetween > 0) {
                    continue;
                }
                if (closestBeforeAndEqualDateBetween == Long.MAX_VALUE || Math.abs(closestBeforeAndEqualDateBetween) > Math.abs(daysBetween)) {
                    beforeAndEqual = date;
                    closestBeforeAndEqualDateBetween = daysBetween;
                }
            }

            for (BMR mrs : set) {
                if (mrs.scanDate.equals(beforeAndEqual)) {
                    return mrs;
                }
            }

            return null;
        }

        private static void merge(Diagnosis keep, BMR mrs, boolean ignoredBenign) {
            assert (keep != null && mrs != null && keep.side != Side.UNKNOWN);
            boolean sameDiagnosis = true;
            List<Diagnosis> list = (keep.side == Side.LEFT) ? mrs.leftDiagnosisList : mrs.rightDiagnosisList;
            for (Diagnosis d : list) {
                if (ignoredBenign && d.cancerType == BreastCancer.BENIGN) {
                    continue;
                }
                if (d != keep && d.cancerType != keep.cancerType) {
                    sameDiagnosis = false;
                    break;
                }
            }
            if (sameDiagnosis) {
                for (Diagnosis d : list) {
                    if (ignoredBenign && d.cancerType == BreastCancer.BENIGN) {
                        continue;
                    }
                    keep = (Math.abs(DAYS.between(mrs.scanDate, d.pathology.biopsyDate)) > Math.abs(DAYS.between(mrs.scanDate, keep.pathology.biopsyDate))) ? d : keep;
                }

                for (Diagnosis d : list) {
                    if (d != keep) {
                        mrs.mergedDiagnosisList.add(d);
                    }
                }

                mrs.mergedDiagnosisList.stream().forEach((d) -> {
                    list.remove(d);
                });
            }
        }

        @Override
        public int compareTo(BMR mrs) {
            int hos = hospital.compareTo(mrs.hospital);
            int id = Integer.parseInt(studyID) - Integer.parseInt(mrs.studyID);
            return (hos != 0) ? hos : id;
        }
    }

    private static class Immuno {

        public Immuno() {
        }
    }

    private static class Diagnosis {

        private final Location location;
        private Side side = Side.UNKNOWN;
        private Biopsy biopsy = Biopsy.UNKNOWN;
        private String diagnosisText = "";
        private BreastCancer cancerType = BreastCancer.NOT;
        private Pathology pathology = null;
        private BMR bmr = null;

        public Diagnosis(Location l) {
            location = l;
        }

        @Override
        public String toString() {
            return this.pathology.toString(this);
        }
    }
}
