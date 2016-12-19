/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bmr.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import mip.data.report.BMR;
import mip.data.report.CancerType;
import mip.data.report.Diagnosis;
import mip.data.report.Pathology;
import static mip.data.report.Pathology.DT;
import mip.data.report.Region;
import mip.data.report.Side;
import mip.util.LogUtils;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author ju
 */
public class PathologyParser {

    private static final String DATA_ROOT;
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Logger LOG = LogUtils.LOGGER;

    static {
        if (System.getProperty("os.name").contains("Windows")) {
            DATA_ROOT = "D:/Dropbox/";
        } else {
            DATA_ROOT = "/home/ju/Dropbox/";
        }
    }

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
            boolean hasUnknown = false;

            boolean hasBreast = false;
            int l_dcis = 0;
            int l_idc = 0;
            int l_malignant = 0;
            int l_benign = 0;
            int r_dcis = 0;
            int r_idc = 0;
            int r_malignant = 0;
            int r_benign = 0;

            int cnt_immuno = 0;
            String her2 = "-";
            String ki67 = "-";
            String er = "-";
            String pr = "-";

            for (Pathology p : pathologyList.get(pid)) {
                if (p.hasBreast) {
                    hasBreast = true;
                }

                if (p.immuno.hasValue()) {
                    cnt_immuno++;
                    her2 = p.immuno.getHER2();
                    ki67 = p.immuno.getKi67();
                    er = p.immuno.getER();
                    pr = p.immuno.getPR();
                }

                for (Diagnosis d : p.diagnosisList) {
                    if (d.text.equals("-")) {
                        continue;
                    }

                    if (d.region == Region.UNKNOWN || d.side == Side.UNKNOWN || d.cancerType == CancerType.UNKNOWN) {
                        hasUnknown = true;
                        LOG.log(Level.INFO, "!!!!\t{0}\t{1}\t{2}\t{3}\t{4}",
                                new Object[]{
                                    pid,
                                    String.format("%10s", d.region),
                                    String.format("%10s", d.side),
                                    String.format("%10s", d.cancerType),
                                    d.text
                                });
                    }

                    if (null != d.side) {
                        switch (d.side) {
                            case LEFT:
                                switch (d.cancerType) {
                                    case DCIS:
                                        l_dcis++;
                                        break;
                                    case IDC:
                                    case IDC_:
                                        l_idc++;
                                        break;
                                    case MALIGNANT:
                                        l_malignant++;
                                        break;
                                    case BENIGN:
                                        l_benign++;
                                        break;

                                }
                                break;
                            case RIGHT:
                                switch (d.cancerType) {
                                    case DCIS:
                                        r_dcis++;
                                        break;
                                    case IDC:
                                    case IDC_:
                                        r_idc++;
                                        break;
                                    case MALIGNANT:
                                        r_malignant++;
                                        break;
                                    case BENIGN:
                                        r_benign++;
                                        break;

                                }
                                break;
                            case IGNORED:
                                break;
                            default:
                                break;
                        }
                    }
                }
            }

            CancerType left = CancerType.TBD;
            CancerType right = CancerType.TBD;

            if (!hasBreast) {
                left = CancerType.IGNORED;
                right = CancerType.IGNORED;
            } else {
                if (l_dcis != 0 && l_idc != 0) {
                    left = CancerType.IDC_;
                } else if (l_dcis == 0 && l_idc == 0) {
                    if (l_benign != 0 && l_malignant != 0) {
                        LOG.log(Level.INFO, "BENIGN+MALIGNANT\t{0}\t{1}",
                                new Object[]{
                                    pList.get(pid),
                                    pid
                                });
                    } else if (l_benign == 0 && l_malignant == 0) {
                        left = CancerType.IGNORED;
                    } else {
                        left = l_benign != 0 ? CancerType.BENIGN : CancerType.MALIGNANT;
                    }
                } else {
                    left = l_dcis != 0 ? CancerType.DCIS : CancerType.IDC;
                }

                if (r_dcis != 0 && r_idc != 0) {
                    right = CancerType.IDC_;
                } else if (r_dcis == 0 && r_idc == 0) {
                    if (r_benign != 0 && r_malignant != 0) {
                        LOG.log(Level.INFO, "BENIGN+MALIGNANT\t{0}\t{1}",
                                new Object[]{
                                    pList.get(pid),
                                    pid
                                });
                    } else if (r_benign == 0 && r_malignant == 0) {
                        right = CancerType.IGNORED;
                    } else {
                        right = r_benign != 0 ? CancerType.BENIGN : CancerType.MALIGNANT;
                    }
                } else {
                    right = r_dcis != 0 ? CancerType.DCIS : CancerType.IDC;
                }
            }
            boolean bothIgnored = left == CancerType.IGNORED && right == CancerType.IGNORED;

            if (cnt_immuno == 1) {
            } else if (cnt_immuno > 1) {
                LOG.log(Level.INFO, "IMMUNO\t{0}\t{1}\t{2}",
                        new Object[]{
                            pList.get(pid),
                            pid,
                            cnt_immuno
                        });
            }

            csv.append(pList.get(pid)).append(",");     // hospital
            csv.append("'").append(pid).append(",");    // patient ID

            final LinkedHashSet<Pathology> ps = pathologyList.get(pid);
            if (ps != null) {
                csv.append(left).append(",");
                csv.append(right).append(",");
                csv.append(bothIgnored).append(",");

                // diagnosis
                {
                    csv.append("\"");
                    for (Pathology p : ps) {
                        for (Diagnosis d : p.diagnosisList) {
                            if (d.text.equals("-")) {
                                continue;
                            }

                            if (d.cancerType == CancerType.IGNORED || d.region == Region.IGNORED || d.side == Side.IGNORED) {
                                //continue;
                            }

                            csv.append("[").append(p.pathologyID).append("],");
                            csv.append(d.region).append(",");
                            csv.append(d.side).append(",");
                            csv.append(d.cancerType);
                            csv.append("\n--------------------------------\n");
                        }
                    }
                    csv.append("\"").append(",");
                }

                // diagnosis source text
                {
                    csv.append("\"");
                    for (Pathology p : ps) {
                        for (Diagnosis d : p.diagnosisList) {
                            if (d.text.equals("-")) {
                                continue;
                            }
                            csv.append("[").append(p.pathologyID).append("]\t");
                            csv.append(d.text);
                            csv.append("\n--------------------------------\n");
                        }
                    }
                    csv.append("\"").append(",");
                }

                csv.append(her2).append(",");
                csv.append(ki67).append(",");
                csv.append(er).append(",");
                csv.append(pr).append(",");
                csv.append(cnt_immuno).append(",");

                // immunohistochemical source text
                csv.append("\"");
                for (Pathology p : ps) {
                    if (p.immuno.text.equals("-")) {
                        continue;
                    }
                    csv.append("[").append(p.pathologyID).append("]\n");
                    csv.append(p.immuno.text);
                    csv.append("\n--------------------------------\n");
                }
                csv.append("\"").append(",");

                // pathology source text
                csv.append("\"");
                for (Pathology p : ps) {
                    if (p.text.equals("-")) {
                        continue;
                    }
                    csv.append(p.text);
                    csv.append("\n--------------------------------\n");
                }
                csv.append("\"").append(",");

            } else {
            }

            csv.append("\n");
        }
        FileUtils.writeStringToFile(new File(DATA_ROOT + "PATHOLOGY.csv"), csv.toString());

    }

}
