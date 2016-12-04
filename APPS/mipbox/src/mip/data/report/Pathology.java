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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final String DATA_ROOT = "/home/ju/Dropbox/";
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final String DF = "yyyy-MM-dd";
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern(DF);
    private static final LocalDate DEFAULT_DATE = LocalDate.of(1900, 01, 01);
    private static final String IMMUNO_KEYWORD = "IMMUNOHISTOCHEMICAL STUDY";
    private static final Logger LOG = LogUtils.LOGGER;

    public static void main(String[] args) throws IOException {
        Map<String, String> pList = new LinkedHashMap<>(341);
        {
            final String fn = DATA_ROOT + "PATIENT";
            try (BufferedReader in = new BufferedReader(new FileReader(fn))) {
                int count = 0;
                while (true) {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    count++;

                    String[] tokens = line.split("\t");
                    assert (tokens.length == 2);
                    String pid = tokens[1].trim();
                    String hospital = tokens[0].trim();
                    pList.put(pid, hospital);
                }
                assert (count == pList.size());
            }
        }

        Map<String, TreeSet<BMR>> bmrList = new LinkedHashMap<>(341);
        {
            final String fn = DATA_ROOT + "MR";
            try (BufferedReader in = new BufferedReader(new FileReader(fn))) {
                while (true) {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }

                    String[] tokens = line.split("\t");
                    assert (tokens.length == 4);
                    String pid = tokens[1].trim();
                    BMR bmr = new BMR(pid);
                    bmr.hospital = tokens[0].trim();
                    bmr.studyID = tokens[2].trim();
                    bmr.scanDate = LocalDate.parse(tokens[3].trim(), DT);
                    if (!bmrList.containsKey(pid)) {
                        bmrList.put(pid, new TreeSet<>());
                    }
                    bmrList.get(pid).add(bmr);
                }
            }
            assert (bmrList.keySet().size() == 763);
        }

        LinkedHashMap<String, LinkedHashSet<Pathology>> pathologyList = new LinkedHashMap<>(341);
        {
            final String fn = DATA_ROOT + "PATHOLOGY";
            try (BufferedReader in = new BufferedReader(new FileReader(fn))) {
                while (true) {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }

                    String[] tokens = line.split("\t");
                    assert (tokens.length >= 3);
                    String pid = tokens[1].trim();
                    int pCount = Integer.parseInt(tokens[2].trim());
                    assert (pCount >= 1);
                    for (int i = 0; i < pCount; i++) {
                        String en = tokens[3 + i].trim();
                        String decodeText = new String(DECODER.decode(en), "UTF-8");
                        Pathology p = new Pathology(pid, decodeText);
                        p.hospital = pList.get(p.patientID);
                        if (!pathologyList.containsKey(pid)) {
                            pathologyList.put(pid, new LinkedHashSet<>(1));
                        }
                        pathologyList.get(pid).add(p);
                    }
                }
            }
        }

        Pathology.verify(pList, pathologyList);

        StringBuilder csv = new StringBuilder(4096);
        for (String pid : pList.keySet()) {
            csv.append(",");
            csv.append(pList.get(pid)).append(",");
            csv.append("'").append(pid).append(",");

            csv.append(",");
            csv.append(",");

            if (pathologyList.get(pid) == null) {
                csv.append(",");
                csv.append(",");
                csv.append(",");
                csv.append(",");
            } else {
                {
                    Set<Pathology> s = pathologyList.get(pid);
                    Set<Integer> v = new HashSet<>(2);
                    for (Pathology p : s) {
                        int her2 = p.immuno.her2;
                        if (her2 == Integer.MIN_VALUE || v.contains(her2)) {
                            continue;
                        }
                        v.add(her2);
                    }
                    int max = Integer.MIN_VALUE;
                    int min = Integer.MAX_VALUE;
                    int sum = 0;
                    for (int her2 : v) {
                        if (her2 > max) {
                            max = her2;
                        }
                        if (her2 < min) {
                            min = her2;
                        }
                        sum += her2;
                    }

                    if (max == 3 && min != 3) {
//                    csv.append("=");int c = 0;
//                    for (double her2 : v) {
//                        csv.append("\"").append(her2).append("\"");c++;
//                        if (c != v.size()) csv.append("&CHAR(10)&");
//                    }
                        csv.append(Math.ceil(sum / (double) v.size()));
                    } else if (max != Integer.MIN_VALUE) {
                        csv.append(max);
                    } else {
                        csv.append("-");
                    }
                }
                csv.append(",");
                {
                    Set<Pathology> s = pathologyList.get(pid);
                    Set<Double> v = new HashSet<>(2);
                    for (Pathology p : s) {
                        double ki67 = p.immuno.ki67;
                        if (ki67 == Double.MIN_VALUE || v.contains(ki67)) {
                            continue;
                        }
                        v.add(ki67);
                    }
                    double max = Double.MIN_VALUE;
                    double min = Double.MAX_VALUE;
                    double sum = 0;
                    for (double ki67 : v) {
                        if (ki67 > max) {
                            max = ki67;
                        }
                        if (ki67 < min) {
                            min = ki67;
                        }
                        sum += ki67;
                    }
                    if (max >= 14 && min <= 14 && max != min) {
//                    csv.append("=");int c = 0;
//                    for (double ki67 : v) {
//                        csv.append("\"").append(ki67).append("\"");c++;
//                        if (c != v.size()) csv.append("&CHAR(10)&");
//                    }
                        csv.append(Math.ceil(sum / v.size()));
                    } else if (max != Double.MIN_VALUE) {
                        csv.append(max);
                    } else {
                        csv.append("-");
                    }
                }
                csv.append(",");
                {
                    Set<Pathology> s = pathologyList.get(pid);
                    Set<Integer> v = new HashSet<>(2);
                    for (Pathology p : s) {
                        int er = p.immuno.er;
                        if (er == Integer.MIN_VALUE || v.contains(er)) {
                            continue;
                        }
                        v.add(er);
                    }
                    int max = Integer.MIN_VALUE;
                    int min = Integer.MAX_VALUE;
                    int sum = 0;
                    for (int er : v) {
                        if (er > max) {
                            max = er;
                        }
                        if (er < min) {
                            min = er;
                        }
                        sum += er;
                    }

                    if (max >= 10 && min <= 10 && max != min) {
//                    csv.append("=");int c = 0;
//                    for (int er : v) {
//                        csv.append("\"").append(er).append("\"");c++;
//                        if (c != v.size()) csv.append("&CHAR(10)&");
//                    }
                        csv.append(Math.ceil(sum / (double) v.size()));
                    } else if (max != Integer.MIN_VALUE) {
                        csv.append(max);
                    } else {
                        csv.append("-");
                    }
                }
                csv.append(",");
                {
                    Set<Pathology> s = pathologyList.get(pid);
                    Set<Integer> v = new HashSet<>(2);
                    for (Pathology p : s) {
                        int pr = p.immuno.pr;
                        if (pr == Integer.MIN_VALUE || v.contains(pr)) {
                            continue;
                        }
                        v.add(pr);
                    }
                    int max = Integer.MIN_VALUE;
                    int min = Integer.MAX_VALUE;
                    int sum = 0;
                    for (int er : v) {
                        if (er > max) {
                            max = er;
                        }
                        if (er < min) {
                            min = er;
                        }
                        sum += er;
                    }
                    if (max >= 10 && min <= 10 && max != min) {
//                    csv.append("=");int c = 0;
//                    for (int pr : v) {
//                        csv.append("\"").append(pr).append("\"");c++;
//                        if (c != v.size()) csv.append("&CHAR(10)&");
//                    }
                        csv.append(Math.ceil(sum / (double) v.size()));
                    } else if (max != Integer.MIN_VALUE) {
                        csv.append(max);
                    } else {
                        csv.append("-");
                    }
                }
            }

//            csv.append(",");
            // immunohistochemical source text
//            csv.append("\"");
//            for (Pathology p : pathologyList.get(pid)) csv.append(p.immuno.text);
//            csv.append("\"").append(",");
            // pathology source text
//            csv.append("\"");
//            for (Pathology p : pathologyList.get(pid)) csv.append(p.text);
//            csv.append("\"").append(",");
            csv.append("\n");
        }
        FileUtils.writeStringToFile(new File(DATA_ROOT + "PATHOLOGY.csv"), csv.toString());

    }

    public static void verify(Map<String, String> pList,
            LinkedHashMap<String, LinkedHashSet<Pathology>> pathologyList) {
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

        for (String pid : pathologyList.keySet()) {
            for (Pathology p : pathologyList.get(pid)) {
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
        }
        long sumSide = sumLeft + sumRight + sumUnknownSide;
        long sumCancerType = sumBenignBreast + sumIDC + sumDCIS
                + sumMixedType
                + sumUnknownType + sumBenignNotBreast;
        long sumBiopsyType = sumNeedle + sumExcision + sumUnknownBiopsy;
        assert (sumSide == sumCancerType && sumSide == sumBiopsyType);
        LOG.log(Level.FINE, "{0} patients", pList.size());
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

    String hospital;
    final String patientID;
    final String pathologyID;
    final LocalDate pathologyDate;
    final List<Diagnosis> diagnosisList = new ArrayList<>(10);
    final Immuno immuno = new Immuno();
    final String text;

    public Pathology(String pid, String text) {
        String[] lines = text.replace("\r", "").split("\n");
        String[] tokens;

        String id = "";
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
        String her2Text = "";
        boolean hasHER2 = false;
        String erText = "";
        String prText = "";
        int er = Integer.MIN_VALUE;
        int pr = Integer.MIN_VALUE;
        boolean isERStart = false;
        boolean isPRStart = false;
        int erSearchWindowSizeinLine = Immuno.ER_SEARCHWIN;
        int prSearchWindowSizeinLine = Immuno.PR_SEARCHWIN;

        for (final String s : lines) {

            if (s.contains("病理切片報告病理號碼")) {
                tokens = s.split("：");
                id = (tokens.length == 2)
                        ? tokens[1]
                        : "ERROR!!!!";
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
                    ki67Text = (ki67Text.indexOf(':') == 0)
                            ? ki67Text.substring(1).trim().toLowerCase()
                            : ki67Text.trim().toLowerCase();
                }

                // HER-2
                if (StringUtils.indexOfAny(s, Immuno.HER2_KW) >= 0) {
                    hasHER2 = true;
                }
                if (hasHER2 && StringUtils.containsIgnoreCase(s, "score")) {
                    int her2Index = StringUtils.indexOfAny(s, Immuno.HER2_KW);
                    int scoreIndex = StringUtils.indexOfIgnoreCase(s, "score");
                    int start = her2Index >= 0 && her2Index < scoreIndex
                            ? her2Index
                            : 0;
                    her2Text = StringUtils.substring(s, start);
                    int end = Math.min(
                            her2Text.contains(")") ? her2Text.indexOf(')') + 1 : her2Text.length(),
                            StringUtils.indexOfIgnoreCase(her2Text, "score") + 9); // TH1102673
                    her2Text = StringUtils.substring(her2Text, 0, end)
                            .replace("-", " - ")
                            .replace(" %", "%");
                    her2Text = (her2Text.indexOf(':') == 0)
                            ? her2Text.substring(1).trim().toLowerCase()
                            : her2Text.trim().toLowerCase();
                }

                // ER
                if (StringUtils.indexOfAny(s, Immuno.ER_KW) >= 0) {
                    isERStart = true;
                    // ER in one line
                    boolean negativeOrPercent = s.contains("%")
                            || StringUtils.containsIgnoreCase(s,
                                    "negative");
                    if (er == Integer.MIN_VALUE && negativeOrPercent) {
                        int start = StringUtils.indexOfAny(s, Immuno.ER_KW);
                        int end = s.contains("%")
                                ? s.indexOf('%') + 1
                                : StringUtils.indexOfIgnoreCase(s, "negative") + 8;
                        try {
                            String erInOneLine = s.substring(start, end)
                                    .trim().replace("-", " - ").replace(" %", "%");
                            er = Immuno.parseER(erInOneLine);
                        } catch (StringIndexOutOfBoundsException e) {
                            LOG.log(Level.SEVERE,
                                    "\t{0} erText\t{1}\n",
                                    new Object[]{id, s});
                        }
                    }
                }
                if (isERStart) {
                    if (StringUtils.indexOfAny(s, Immuno.ER_END_KW) >= 0) {
                        isERStart = false;
                        erSearchWindowSizeinLine = Immuno.ER_SEARCHWIN;
                    }
                    if (erSearchWindowSizeinLine < 0) {
                        isERStart = false;
                        erSearchWindowSizeinLine = Immuno.ER_SEARCHWIN;
                    }
                }
                if (isERStart) {
                    erText += (s.trim().length() != 0) ? (s + "\n") : "";
                    erSearchWindowSizeinLine--;
                }

                // PR
                if (StringUtils.indexOfAny(s, Immuno.PR_KW) >= 0) {
                    isPRStart = true;
                    // PR in one line
                    boolean negativeOrPercent = s.contains("%")
                            || StringUtils.containsIgnoreCase(s,
                                    "negative");

                    if (pr == Integer.MIN_VALUE && negativeOrPercent) {
                        int start = StringUtils.indexOfAny(s, Immuno.PR_KW);
                        String subLine = s.substring(start, s.length());
                        int end = subLine.contains("%")
                                ? subLine.indexOf('%') + 1
                                : StringUtils.indexOfIgnoreCase(subLine, "negative") + 8;
                        try {
                            String prInOneLine = subLine.substring(0, end)
                                    .trim().replace("-", " - ").replace(" %", "%");
                            pr = Immuno.parsePR(prInOneLine);
                        } catch (StringIndexOutOfBoundsException e) {
                            LOG.log(Level.SEVERE,
                                    "\t{0} prText\t{1}\n",
                                    new Object[]{id, s});
                        }
                    }
                }
                if (isPRStart) {
                    if (StringUtils.indexOfAny(s, Immuno.PR_END_KEYWORDS) >= 0) {
                        isPRStart = false;
                        prSearchWindowSizeinLine = Immuno.PR_SEARCHWIN;
                    }

                    if (prSearchWindowSizeinLine < 0) {
                        isPRStart = false;
                        prSearchWindowSizeinLine = Immuno.PR_SEARCHWIN;
                    }
                }
                if (isPRStart) {
                    prText += (s.trim().length() != 0) ? (s + "\n") : "";
                    prSearchWindowSizeinLine--;
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

        assert (!id.equals("ERROR!!!!"));
        assert (!biopsyDate.equals(DEFAULT_DATE));

        this.pathologyID = id;
        this.patientID = pid;
        this.pathologyDate = biopsyDate;
        this.text = text.replace("\"", "'");

        Diagnosis.parse(this, diagnosisText);

        int immunoCount = StringUtils.countMatches(pathologyText, IMMUNO_KEYWORD);
        immuno.setImmunoText((immunoCount > 0)
                ? StringUtils.replace(immunohistochemicalText,
                        "\"", // for spreadsheet
                        "'").trim()
                : "-");
        immuno.setKi67(Immuno.parseKi67(ki67Text));
        immuno.setHER2(Immuno.parseHER2(her2Text));

        // ER in multiple lines
        if (er == Integer.MIN_VALUE && !erText.isEmpty()) {
            erText = "*****\n" + erText.replace("-", " - ").replace(" %", "%");
            boolean negativeOrPercent
                    = StringUtils.containsIgnoreCase(erText, "%")
                    ? true
                    : StringUtils.containsIgnoreCase(
                            erText,
                            "negative");
            if (negativeOrPercent) {
                int end = erText.contains("%")
                        ? erText.indexOf('%') + 1
                        : StringUtils.indexOfIgnoreCase(erText, "negative") + 8;
                er = Immuno.parseER(erText.substring(StringUtils.indexOfAny(erText, Immuno.ER_KW), end));
            } else { // postive without percentage
                LOG.warning(erText.replace("\n", "\t"));
            }

        }
        immuno.setER(er);

        // PR in multiple lines
        if (pr == Integer.MIN_VALUE && !prText.isEmpty()) {
            prText = "*****\n" + prText.replace("-", " - ").replace(" %", "%");

            boolean negativeOrPercent
                    = StringUtils.containsIgnoreCase(prText, "%")
                    ? true
                    : StringUtils.containsIgnoreCase(
                            prText,
                            "negative");
            if (negativeOrPercent) {
                int end = prText.contains("%")
                        ? prText.indexOf('%') + 1
                        : StringUtils.indexOfIgnoreCase(prText, "negative") + 8;
                pr = Immuno.parsePR(prText.substring(StringUtils.indexOfAny(prText, Immuno.PR_KW), end));
            } else { // postive without percentage
                LOG.warning(prText.replace("\n", "\t"));
            }
        }
        immuno.setPR(pr);
    }

    public String toCSVString() {
        StringBuilder sb = new StringBuilder(64);

        sb.append(",");
        sb.append(this.hospital).append(",");
        sb.append("'").append(this.patientID).append(",");
        sb.append(",");
        sb.append(",");
        sb.append(immuno.toCSVString());

        sb.append("\n");

        return sb.toString();
    }
}
