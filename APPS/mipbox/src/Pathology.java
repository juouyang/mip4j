
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
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

    private static final String DATA_ROOT = "D:\\Dropbox\\";
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final LocalDate DEFAULT_DATE = LocalDate.of(1900, 01, 01);
    private static final String[] IDC_KEYWORDS = {" invasive carcinoma", "mucinous carcinoma", "invasive tubular carcinoma", "invasive lobular carcinoma", "carcinoma with invasion", "invasive ductal and lobular carcinoma", "infiltrating ductal carcinoma", "invasive ductal carcinoma", "IDC"};
    private static final String[] DCIS_KEYWORDS = {"ductal carcinoma in situ"};
    private static final String[] BENIGN_KEYWORDS = {"benign", "fibroadipose", "no carcinoma", "no metastasis", "no residual carcinoma", "no tumor involve", "negative for carcinoma", "fibroadenoma", "fibrocystic change", "fat necrosis"};

    private String patientID = "";
    private String patientName = "";

    private MRStudy mrStudy = null;

    private String pathologyID = "";
    private LocalDate biopsyDate = LocalDate.MIN;
    private Side side = Side.UNKNOWN;
    private Biopsy biopsy = Biopsy.UNKNOWN;
    private String diagnosisText = "";
    private boolean hasIDC = false;
    private boolean hasDCIS = false;
    private boolean hasBenign = false;

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Map<String, List<MRStudy>> mrStudyList = new HashMap<>();
        try (BufferedReader in = new BufferedReader(new FileReader(DATA_ROOT + "pList.txt"))) {
            int summaryMRStudyCount = 0;
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                summaryMRStudyCount++;

                //<editor-fold defaultstate="collapsed" desc="Process Each MR Study">
                String[] tokens = line.split("\t");
                assert (tokens.length == 4);
                MRStudy mrs = new MRStudy();
                mrs.hospital = tokens[0].trim();
                mrs.studyID = tokens[1].trim();
                mrs.scanDate = LocalDate.parse(tokens[2].trim(), DT_FORMATTER);
                mrs.patientID = tokens[3].trim();
                if (!mrStudyList.containsKey(mrs.patientID)) {
                    mrStudyList.put(mrs.patientID, new ArrayList<>());
                }
                mrStudyList.get(mrs.patientID).add(mrs);
                //</editor-fold>
            }

            //<editor-fold defaultstate="collapsed" desc="Summary">
            DBG.accept("Totally " + summaryMRStudyCount + " MR studies are processed.\n");
            //</editor-fold>
        }

        List<Pathology> pathologyList = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(DATA_ROOT + "DATA"))) {
            int summaryPathologyCount = 0;
            int summaryNotBreastCount = 0;
            int summaryNoDiagnosisCount = 0;
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                summaryPathologyCount++;

                //<editor-fold defaultstate="collapsed" desc="Decode Base64 Text">
                String[] tokens = line.split("\t");
                String pid = tokens[0];
                String encodedText = tokens[1];
                String decodeText = new String(DECODER.decode(encodedText), "UTF-8").replace("\r", "");
                String[] lines = decodeText.split("\n");
                //DBG.accept(pid + "\t" + decodeText + "\n-------------------\n");
                //</editor-fold>

                //<editor-fold defaultstate="collapsed" desc="Process Each Pathology Report">
                String patientName = "";
                String pathologyID = "";
                LocalDate biopsyDate = LocalDate.MIN;
                LocalDate receiveDate = LocalDate.MIN;
                boolean isDiagnosisStart = false;
                String diagnosisText = "";
                boolean isPathologyStart = false;
                int i = 0;
                for (final String s : lines) {
                    //DBG.accept("\t" + i++ + "\t" + s + "\n");

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
                        biopsyDate = (tokens.length == 2) ? LocalDate.parse(tokens[1].trim(), DT_FORMATTER) : DEFAULT_DATE;
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

                    if (isDiagnosisStart) {
                        diagnosisText += (s.trim().length() != 0) ? (s + "\n") : "";
                    }
                }
                //</editor-fold>

                //<editor-fold defaultstate="collapsed" desc="Post Process">
                assert (!pathologyID.equals("ERROR!!!!"));  // must have pathology ID
                biopsyDate = biopsyDate.equals(DEFAULT_DATE) ? (!receiveDate.equals(DEFAULT_DATE) ? receiveDate : DEFAULT_DATE) : biopsyDate;
                assert (!biopsyDate.equals(DEFAULT_DATE)); // must have biopsy date
                diagnosisText = StringUtils.replace(StringUtils.replace(diagnosisText, "\"", "'").trim(), "病理診斷：", "");

                i = 0;
                lines = diagnosisText.split("\n");
                for (final String s : lines) {
                    //DBG.accept("\t" + i++ + "\t" + s + "\n");

                    if (!StringUtils.startsWithIgnoreCase(s, "breast")) {
                        //DBG.accept(s + "\n");
                        summaryNotBreastCount++;
                        continue;
                    }

                    boolean hasIDC = false;
                    boolean hasDCIS = false;
                    boolean hasBenign = false;
                    assert (!(StringUtils.containsIgnoreCase(s, "left") && StringUtils.containsIgnoreCase(s, "right"))); // right and left in same line

                    for (String searchString : IDC_KEYWORDS) {
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
                    for (String searchString : DCIS_KEYWORDS) {
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
                    for (String searchString : BENIGN_KEYWORDS) {
                        if (StringUtils.containsIgnoreCase(s, searchString)) {
                            hasBenign = true;
                            break;
                        }
                    }

                    int diagnosisCount = 0;
                    diagnosisCount += hasIDC ? 1 : 0;
                    diagnosisCount += hasDCIS ? 1 : 0;
                    diagnosisCount += hasBenign ? 1 : 0;
                    if (diagnosisCount == 0) {
                        if (MRStudy.getClosestMRStudyBeforeBiopsyDate(mrStudyList.get(pid), biopsyDate) != null) {
                            DBG.accept(s + "\n");
                        }
                        summaryNoDiagnosisCount++;
                        continue;
                    }
                    if (diagnosisCount > 1 && hasBenign) {
                        if (MRStudy.getClosestMRStudyBeforeBiopsyDate(mrStudyList.get(pid), biopsyDate) != null) {
                            //DBG.accept(s + "\n");
                        }
                    }
                    assert (diagnosisCount != 0 && (diagnosisCount > 1 ? !hasBenign : true));

                    Biopsy b = Biopsy.UNKNOWN;
                    for (String keyword : Biopsy.EXCISIONAL.keywords) {
                        if (s.contains(keyword)) {
                            b = Biopsy.EXCISIONAL;
                            break;
                        }
                    }
                    if ((s.contains(Biopsy.NEEDLE.keywords[0]) && s.contains(Biopsy.NEEDLE.keywords[1])) || s.contains(Biopsy.NEEDLE.keywords[2]) || s.contains(Biopsy.NEEDLE.keywords[3])) {
                        b = Biopsy.NEEDLE;
                    }

                    Pathology p = new Pathology();
                    p.pathologyID = pathologyID;
                    p.patientID = pid;
                    p.patientName = patientName;
                    p.biopsyDate = biopsyDate;
                    p.side = StringUtils.containsIgnoreCase(s, "left") ? Side.LEFT : StringUtils.containsIgnoreCase(s, "right") ? Side.RIGHT : Side.UNKNOWN;
                    p.biopsy = b;
                    p.diagnosisText = s;
                    p.hasIDC = hasIDC;
                    p.hasDCIS = hasDCIS;
                    p.hasBenign = hasBenign;
                    pathologyList.add(p);
                }
                //</editor-fold>

            }

            //<editor-fold defaultstate="collapsed" desc="Find Closest MR Study">
            pathologyList.stream().forEach((p) -> {
                p.mrStudy = MRStudy.getClosestMRStudyBeforeBiopsyDate(mrStudyList.get(p.patientID), p.biopsyDate);
            });
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="Summary">
            int summaryLeftCount = 0;
            int summaryRightCount = 0;
            int summaryUnknownSideCount = 0;
            int summaryBenignCount = 0;
            int summaryIDCCount = 0;
            int summaryDCISCount = 0;
            int summaryIDCDCISCount = 0;
            int summaryNeedleCount = 0;
            int summaryExcisionCount = 0;
            int summaryUnknownBiopsyCount = 0;
            int summaryMRStudyLinked = 0;
            int summaryMRStudyNotLinked = 0;
            Set<MRStudy> mrStudyFromPathologyList = new HashSet<>();
            StringBuilder allPathology = new StringBuilder();
            for (Pathology p : pathologyList) {
                summaryLeftCount += p.side == Side.LEFT ? 1 : 0;
                summaryRightCount += p.side == Side.RIGHT ? 1 : 0;
                summaryUnknownSideCount += p.side == Side.UNKNOWN ? 1 : 0;
                summaryBenignCount += p.hasBenign ? 1 : 0;
                summaryIDCCount += p.hasIDC && !p.hasDCIS ? 1 : 0;
                summaryDCISCount += p.hasDCIS && !p.hasIDC ? 1 : 0;
                summaryIDCDCISCount += p.hasIDC && p.hasDCIS ? 1 : 0;
                summaryNeedleCount += p.biopsy == Biopsy.NEEDLE ? 1 : 0;
                summaryExcisionCount += p.biopsy == Biopsy.EXCISIONAL ? 1 : 0;
                summaryUnknownBiopsyCount += p.biopsy == Biopsy.UNKNOWN ? 1 : 0;
                summaryMRStudyLinked += p.mrStudy != null ? 1 : 0;
                summaryMRStudyNotLinked += p.mrStudy == null ? 1 : 0;
                if (p.mrStudy != null) {
                    mrStudyFromPathologyList.add(p.mrStudy);
                }

                allPathology.append(p);
            }
            FileUtils.writeStringToFile(new File(DATA_ROOT + "PATHOLOGY_ALL.csv"), allPathology.toString());
            assert (summaryLeftCount + summaryRightCount + summaryUnknownSideCount - summaryBenignCount - summaryIDCCount - summaryDCISCount - summaryIDCDCISCount == 0);
            assert (summaryLeftCount + summaryRightCount + summaryUnknownSideCount - summaryNeedleCount - summaryExcisionCount - summaryUnknownBiopsyCount == 0);
            int mrOneSideOneDiagnosisCount = 0;
            int mrOneSideMultipleDiagnosisCount = 0;
            int ignoredDiagnosisCount = 0;
            StringBuilder mrSinglePathology = new StringBuilder();
            StringBuilder mrMultiplePathology = new StringBuilder();
            for (MRStudy mrs : mrStudyFromPathologyList) {
                int leftPathologyCount = 0;
                int rightPathologyCount = 0;
                int unknownSidePathologyCount = 0;
                for (Pathology p : pathologyList) {
                    if (p.mrStudy != null && (mrs.hospital.equals(p.mrStudy.hospital) && mrs.studyID.equals(p.mrStudy.studyID))) {
                        switch (p.side) {
                            case LEFT:
                                leftPathologyCount++;
                                mrs.leftPathologyList.add(p);
                                break;
                            case RIGHT:
                                rightPathologyCount++;
                                mrs.rightPathologyList.add(p);
                                break;
                            default:
                                unknownSidePathologyCount++;
                                ignoredDiagnosisCount++;
                        }
                    }
                }
                int pathologyCount = leftPathologyCount + rightPathologyCount + unknownSidePathologyCount;
                assert (pathologyCount != 0);

                //<editor-fold defaultstate="collapsed" desc="Merge pathology reports of the MR study with same diagnosis on the left side">
                if (leftPathologyCount == 2) {
                    Pathology one = mrs.leftPathologyList.get(0);
                    Pathology two = mrs.leftPathologyList.get(1);
                    if (!one.hasIDC ^ two.hasIDC && !one.hasDCIS ^ two.hasDCIS && !one.hasBenign ^ two.hasBenign) {

                        if (one.biopsyDate.equals(two.biopsyDate)) {
                            if (one.biopsy == Biopsy.EXCISIONAL && two.biopsy == Biopsy.NEEDLE) {
                                mrs.leftPathologyList.remove(two);
                            } else {
                                mrs.leftPathologyList.remove(one);
                            }
                        } else {
                            if (one.biopsyDate.isAfter(two.biopsyDate)) {
                                mrs.leftPathologyList.remove(two);
                            } else {
                                mrs.leftPathologyList.remove(one);
                            }
                        }
                        mrs.isLeftMerged = true;
                        leftPathologyCount = 1;
                        ignoredDiagnosisCount++;
                    }
                }
                if (leftPathologyCount == 3) {
                    Pathology one = mrs.leftPathologyList.get(0);
                    Pathology two = mrs.leftPathologyList.get(1);
                    Pathology three = mrs.leftPathologyList.get(2);
                    if ((one.hasIDC == two.hasIDC && two.hasIDC == three.hasIDC) && (one.hasDCIS == two.hasDCIS && two.hasDCIS == three.hasDCIS) && (one.hasBenign == two.hasBenign && two.hasBenign == three.hasBenign)) {

                        if (one.biopsyDate.equals(two.biopsyDate) && two.biopsyDate.equals(three.biopsyDate)) {
                            if (one.biopsy == Biopsy.EXCISIONAL && two.biopsy != Biopsy.EXCISIONAL && three.biopsy != Biopsy.EXCISIONAL) {
                                mrs.leftPathologyList.remove(two);
                                mrs.leftPathologyList.remove(three);
                            } else if (one.biopsy != Biopsy.EXCISIONAL && two.biopsy == Biopsy.EXCISIONAL && three.biopsy != Biopsy.EXCISIONAL) {
                                mrs.leftPathologyList.remove(one);
                                mrs.leftPathologyList.remove(three);
                            } else {
                                mrs.leftPathologyList.remove(one);
                                mrs.leftPathologyList.remove(two);
                            }
                        } else {
                            if (one.biopsyDate.isAfter(two.biopsyDate)) {
                                mrs.leftPathologyList.remove(two);

                                if (one.biopsyDate.isAfter(three.biopsyDate)) {
                                    mrs.leftPathologyList.remove(three);
                                } else {
                                    mrs.leftPathologyList.remove(one);
                                }
                            } else {
                                mrs.leftPathologyList.remove(one);

                                if (two.biopsyDate.isAfter(three.biopsyDate)) {
                                    mrs.leftPathologyList.remove(three);
                                } else {
                                    mrs.leftPathologyList.remove(two);
                                }
                            }
                        }
                        mrs.isLeftMerged = true;
                        leftPathologyCount = 1;
                        ignoredDiagnosisCount += 2;
                    }
                }
                if (leftPathologyCount == 1) {
                    mrOneSideOneDiagnosisCount++;
                    mrSinglePathology.append(mrs.leftPathologyList.get(0));
                }
                if (leftPathologyCount > 1) {
                    mrOneSideMultipleDiagnosisCount += mrs.leftPathologyList.size();
                    for (Pathology p : mrs.leftPathologyList) {
                        mrMultiplePathology.append(p);
                    }
                }
                //</editor-fold>

                //<editor-fold defaultstate="collapsed" desc="Merge pathology reports of the MR study with same diagnosis on the right side">
                if (rightPathologyCount == 2) {
                    Pathology one = mrs.rightPathologyList.get(0);
                    Pathology two = mrs.rightPathologyList.get(1);
                    if (/*Math.abs(DAYS.between(one.biopsyDate, two.biopsyDate)) < 30 && */!one.hasIDC ^ two.hasIDC && !one.hasDCIS ^ two.hasDCIS && !one.hasBenign ^ two.hasBenign) {

                        if (one.biopsyDate.equals(two.biopsyDate)) {
                            if (one.biopsy == Biopsy.EXCISIONAL && two.biopsy == Biopsy.NEEDLE) {
                                mrs.rightPathologyList.remove(two);
                            } else {
                                mrs.rightPathologyList.remove(one);
                            }
                        } else {
                            if (one.biopsyDate.isAfter(two.biopsyDate)) {
                                mrs.rightPathologyList.remove(two);
                            } else {
                                mrs.rightPathologyList.remove(one);
                            }
                        }
                        mrs.isRightMerged = true;
                        rightPathologyCount = 1;
                        ignoredDiagnosisCount++;
                    }
                }
                if (rightPathologyCount == 3) {
                    Pathology one = mrs.rightPathologyList.get(0);
                    Pathology two = mrs.rightPathologyList.get(1);
                    Pathology three = mrs.rightPathologyList.get(2);
                    if ((one.hasIDC == two.hasIDC && two.hasIDC == three.hasIDC) && (one.hasDCIS == two.hasDCIS && two.hasDCIS == three.hasDCIS) && (one.hasBenign == two.hasBenign && two.hasBenign == three.hasBenign)) {

                        if (one.biopsyDate.equals(two.biopsyDate) && two.biopsyDate.equals(three.biopsyDate)) {
                            if (one.biopsy == Biopsy.EXCISIONAL && two.biopsy != Biopsy.EXCISIONAL && three.biopsy != Biopsy.EXCISIONAL) {
                                mrs.rightPathologyList.remove(two);
                                mrs.rightPathologyList.remove(three);
                            } else if (one.biopsy != Biopsy.EXCISIONAL && two.biopsy == Biopsy.EXCISIONAL && three.biopsy != Biopsy.EXCISIONAL) {
                                mrs.rightPathologyList.remove(one);
                                mrs.rightPathologyList.remove(three);
                            } else {
                                mrs.rightPathologyList.remove(one);
                                mrs.rightPathologyList.remove(two);
                            }
                        } else {
                            if (one.biopsyDate.isAfter(two.biopsyDate)) {
                                mrs.rightPathologyList.remove(two);

                                if (one.biopsyDate.isAfter(three.biopsyDate)) {
                                    mrs.rightPathologyList.remove(three);
                                } else {
                                    mrs.rightPathologyList.remove(one);
                                }
                            } else {
                                mrs.rightPathologyList.remove(one);

                                if (two.biopsyDate.isAfter(three.biopsyDate)) {
                                    mrs.rightPathologyList.remove(three);
                                } else {
                                    mrs.rightPathologyList.remove(two);
                                }
                            }
                        }
                        mrs.isRightMerged = true;
                        rightPathologyCount = 1;
                        ignoredDiagnosisCount += 2;
                    }
                }
                if (rightPathologyCount == 1) {
                    mrOneSideOneDiagnosisCount++;
                    mrSinglePathology.append(mrs.rightPathologyList.get(0));
                }

                if (rightPathologyCount > 1) {
                    mrOneSideMultipleDiagnosisCount += mrs.rightPathologyList.size();
                    for (Pathology p : mrs.rightPathologyList) {
                        mrMultiplePathology.append(p);
                    }
                }
                //</editor-fold>
            }
            FileUtils.writeStringToFile(new File(DATA_ROOT + "MR_SINGLE_DIAGNOSIS.csv"), mrSinglePathology.toString());
            FileUtils.writeStringToFile(new File(DATA_ROOT + "MR_MULTIPLE_DIAGNOSIS.csv"), mrMultiplePathology.toString());
            DBG.accept("--\n");
            DBG.accept("Totally " + summaryPathologyCount + " pathology reports are processed.\n");
            DBG.accept("\t" + summaryNotBreastCount + " diagnoses are NOT on the breast.\n");
            DBG.accept("\t" + summaryNoDiagnosisCount + " diagnoses cannot be determined.\n");
            DBG.accept("\t" + summaryLeftCount + " diagnoses are on the left, "
                    + summaryRightCount + " diagnoses are on the right, and "
                    + summaryUnknownSideCount + " diagnoses without side of breast.\n");
            DBG.accept("\t\tTotally " + summaryBenignCount + " diagnoses are benign only.\n");
            DBG.accept("\t\tTotally " + summaryIDCCount + " diagnoses are IDC only.\n");
            DBG.accept("\t\tTotally " + summaryDCISCount + " diagnoses are DCIS only.\n");
            DBG.accept("\t\tTotally " + summaryIDCDCISCount + " diagnoses have both IDC and DCIS.\n\n");
            DBG.accept("\t\tTotally " + summaryNeedleCount + " needle biopsies.\n");
            DBG.accept("\t\tTotally " + summaryExcisionCount + " excisional biopsies.\n");
            DBG.accept("\t\tTotally " + summaryUnknownBiopsyCount + " biopsies with unknown type.\n");
            DBG.accept("--\n");
            DBG.accept("\tTotally " + summaryMRStudyNotLinked + " diagnoses are NOT linked with MRI.\n");
            DBG.accept("\tTotally " + summaryMRStudyLinked + " diagnoses are linked with " + mrStudyFromPathologyList.size() + " MRI.\n");
            DBG.accept("\t\tTotally " + mrOneSideOneDiagnosisCount + " 組 病理為單邊單診斷.\n");
            DBG.accept("\t\tTotally " + mrOneSideMultipleDiagnosisCount + " 組 病理為單邊多診斷.\n");
            DBG.accept("\t\tTotally " + ignoredDiagnosisCount + " 組 病理被忽略或合併.\n");
            //</editor-fold>
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%8s", patientID)).append(",\t");
        sb.append(String.format("%10s", patientName)).append(",\t");
        sb.append(biopsyDate).append(",\t");
        sb.append(side).append(",\t");
        sb.append(biopsy).append(",\t");
        sb.append(hasIDC ? "V" : "-").append(",\t");
        sb.append(hasDCIS ? "V" : "-").append(",\t");
        sb.append(hasBenign ? "V" : "-").append(",\t");
        sb.append(pathologyID).append(",\t");
        sb.append((mrStudy != null) ? mrStudy.hospital : "-").append(",\t");
        sb.append((mrStudy != null) ? mrStudy.studyID : "-").append(",\t");
        sb.append((mrStudy != null) ? mrStudy.scanDate : "-");

        sb.append("\n");
        return sb.toString();
    }

    static final boolean IS_DEBUG = true;
    //<editor-fold defaultstate="collapsed" desc="Debug">
    static final Consumer<String> DBG = Pathology::debug;

    private static void debug(String s) {
        if (IS_DEBUG == true) {
            System.out.print(s);
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="enum">
    public enum Side {

        UNKNOWN("Unknown"), LEFT("Left"), RIGHT("Right");
        private final String description;

        private Side(String s) {
            description = s;
        }

        @Override
        public String toString() {
            return String.format("%7s", this.description);
        }
    }

    public enum Biopsy {

        UNKNOWN("Unknwon", null),
        EXCISIONAL("Excisional",
                new String[]{"excision", "mastectomy", "operative biopsy"}
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
            return String.format("%10s", this.description);
        }
    }
    //</editor-fold>

    static class MRStudy {

        String hospital;
        String studyID;
        LocalDate scanDate;
        String patientID;
        List<Pathology> leftPathologyList = new ArrayList<>();
        List<Pathology> rightPathologyList = new ArrayList<>();
        boolean isLeftMerged = false;
        boolean isRightMerged = false;

        public static MRStudy getClosestMRStudyBeforeBiopsyDate(List<MRStudy> list, LocalDate targetDate) {
            if (list == null) {
                return null;
            }

            List<LocalDate> dates = new ArrayList<>();
            list.stream().forEach((mrs) -> {
                dates.add(mrs.scanDate);
            });

            LocalDate beforeAndEqual = null;
            long closestBeforeAndEqualDateBetween = Long.MAX_VALUE;
            for (LocalDate d : dates) {
                long monthsBetween = MONTHS.between(targetDate, d);
                long daysBetween = DAYS.between(targetDate, d);
                if (Math.abs(monthsBetween) > 6 || daysBetween > 0) {
                    continue;
                }
                if (closestBeforeAndEqualDateBetween == Long.MAX_VALUE || Math.abs(closestBeforeAndEqualDateBetween) > Math.abs(daysBetween)) {
                    beforeAndEqual = d;
                    closestBeforeAndEqualDateBetween = daysBetween;
                }
            }

            for (MRStudy mrs : list) {
                if (mrs.scanDate.equals(beforeAndEqual)) {
                    return mrs;
                }
            }

            return null;
        }
    }
}
