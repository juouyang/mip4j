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
import java.time.temporal.ChronoUnit;
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

        StringBuilder pT = new StringBuilder(4096); // pathology
        StringBuilder dG = new StringBuilder(4096); // Diagnosis
        StringBuilder iM = new StringBuilder(4096); // Immuno
        for (String pid : pList.keySet()) {

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
            LocalDate her2Date = LocalDate.MAX;
            boolean her2Conflict = false;
            String ki67 = "-";
            LocalDate ki67Date = LocalDate.MAX;
            boolean ki67Conflict = false;
            String er = "-";
            LocalDate erDate = LocalDate.MAX;
            boolean erConflict = false;
            String pr = "-";
            LocalDate prDate = LocalDate.MAX;
            boolean prConflict = false;

            for (Pathology p : pathologyList.get(pid)) {
                if (p.hasBreast) {
                    hasBreast = true;
                }

                if (p.immuno.hasValue()) {
                    cnt_immuno++;
                    String cur_her2 = p.immuno.getHER2();

                    if (!"-".equals(her2) && !"-".equals(cur_her2) && !her2.equals(cur_her2)) {
                        if ((her2.equals("2") || her2.equals("3")) && (cur_her2.equals("2") || cur_her2.equals("3"))) {
                            LOG.log(Level.FINEST, "HER2!!\t{0}\t{1}\t{2}\t{3}",
                                    new Object[]{
                                        pList.get(pid),
                                        pid,
                                        her2,
                                        cur_her2});
                            her2Conflict = true;
                        }
                    }

                    her2 = (!her2Conflict) ? cur_her2 : "2*";
                    her2Date = "-".equals(her2) ? LocalDate.MAX : her2Conflict ? "2".equals(cur_her2) ? p.pathologyDate : her2Date : p.pathologyDate;

                    final String cur_ki67 = p.immuno.getKi67();
                    String pad = "";

                    if (!"-".equals(ki67) && !"-".equals(cur_ki67)) {
                        if (!ki67.equals(cur_ki67)) {
                            if ((Double.parseDouble(ki67) >= 14 && Double.parseDouble(cur_ki67) < 14)
                                    || (Double.parseDouble(ki67) < 14 && Double.parseDouble(cur_ki67) >= 14)) {
                                LOG.log(Level.FINEST, "Ki67!!\t{0}\t{1}\t{2}\t{3}",
                                        new Object[]{
                                            pList.get(pid),
                                            pid,
                                            ki67,
                                            cur_ki67});
                                ki67Conflict = true;
                            }
                        }
                        pad += "*";
                    }

                    ki67 = cur_ki67 + pad;
                    ki67Date = "-".equals(ki67) ? LocalDate.MAX : ki67Conflict ? LocalDate.MIN : p.pathologyDate;

                    final String cur_er = p.immuno.getER();
                    pad = "";

                    if (!"-".equals(er) && !"-".equals(cur_er) && !er.equals(cur_er)) {
                        if ((Integer.parseInt(er) >= 1 && Integer.parseInt(cur_er) == 0)
                                || (Integer.parseInt(er) == 0 && Integer.parseInt(cur_er) >= 1)) {
                            LOG.log(Level.FINEST, "ER!!!!\t{0}\t{1}\t{2}\t{3}",
                                    new Object[]{
                                        pList.get(pid),
                                        pid,
                                        er,
                                        cur_er});
                            erConflict = true;
                        }
                        pad += "*";
                    }

                    er = p.immuno.getER() + pad;
                    erDate = "-".equals(er) ? LocalDate.MAX : erConflict ? LocalDate.MIN : p.pathologyDate;

                    final String cur_pr = p.immuno.getPR();
                    pad = "";

                    if (!"-".equals(pr) && !"-".equals(cur_pr) && !pr.equals(cur_pr)) {
                        if ((Integer.parseInt(pr) >= 1 && Integer.parseInt(cur_pr) == 0)
                                || (Integer.parseInt(pr) == 0 && Integer.parseInt(cur_pr) >= 1)) {
                            LOG.log(Level.FINEST, "ER!!!!\t{0}\t{1}\t{2}\t{3}",
                                    new Object[]{
                                        pList.get(pid),
                                        pid,
                                        pr,
                                        cur_pr});
                            prConflict = true;
                        }
                        pad += "*";
                    }

                    pr = p.immuno.getPR() + pad;
                    prDate = "-".equals(pr) ? LocalDate.MAX : prConflict ? LocalDate.MIN : p.pathologyDate;
                }

                for (Diagnosis d : p.diagnosisList) {
                    if (d.text.equals("-")) {
                        continue;
                    }

                    if (d.region == Region.UNKNOWN || d.side == Side.UNKNOWN || d.cancerType == CancerType.UNKNOWN) {
                        LOG.log(Level.FINEST, "UNKNOWN\t{0}\t{1}\t{2}\t{3}\t{4}\t{5}",
                                new Object[]{
                                    pList.get(pid),
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
                                        //case IDC_:
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
                                        //case IDC_:
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
                    left = CancerType.IDC/*_*/;
                } else if (l_dcis == 0 && l_idc == 0) {
                    if (l_benign != 0 && l_malignant != 0) {
//                        LOG.log(Level.INFO, "良加惡\t{0}\t{1}",
//                                new Object[]{
//                                    pList.get(pid),
//                                    pid
//                                });
                    } else if (l_benign == 0 && l_malignant == 0) {
                        left = CancerType.IGNORED;
                    } else {
                        left = l_benign != 0 ? CancerType.BENIGN : CancerType.MALIGNANT;
                    }
                } else {
                    left = l_dcis != 0 ? CancerType.DCIS : CancerType.IDC;
                }

                if (r_dcis != 0 && r_idc != 0) {
                    right = CancerType.IDC/*_*/;
                } else if (r_dcis == 0 && r_idc == 0) {
                    if (r_benign != 0 && r_malignant != 0) {
//                        LOG.log(Level.INFO, "良加惡\t{0}\t{1}",
//                                new Object[]{
//                                    pList.get(pid),
//                                    pid
//                                });
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

            LocalDate l_date = null;
            LocalDate r_date = null;

            if (bothIgnored) {
            } else {
                for (Pathology p : pathologyList.get(pid)) {
                    for (Diagnosis d : p.diagnosisList) {
                        final LocalDate pathologyDate = d.pathologyLink.pathologyDate;
                        if (null != d.side) {
                            switch (d.side) {
                                case LEFT:
                                    if (left == CancerType.TBD) {
                                        continue;
                                    }

                                    if (l_date == null) {
                                        l_date = (d.cancerType == left) ? pathologyDate : null;
                                    } else {
                                        if (!l_date.isEqual(pathologyDate)) {
                                            if (ChronoUnit.DAYS.between(l_date, pathologyDate) > 30) {
//                                                LOG.log(Level.INFO, "L_DATE\t{0}\t{1}\t{2}\t{3}",
//                                                        new Object[]{
//                                                            pList.get(pid),
//                                                            pid,
//                                                            l_date, pathologyDate});
                                            } else {
                                                l_date = l_date.isAfter(pathologyDate) ? l_date : pathologyDate;
                                            }
                                        }
                                    }
                                    break;
                                case RIGHT:
                                    if (right == CancerType.TBD) {
                                        continue;
                                    }
                                    if (r_date == null) {
                                        r_date = (d.cancerType == right) ? pathologyDate : null;
                                    } else {
                                        if (!r_date.isEqual(pathologyDate)) {
                                            if (ChronoUnit.DAYS.between(r_date, pathologyDate) > 30) {
//                                                LOG.log(Level.INFO, "R_DATE\t{0}\t{1}\t{2}\t{3}",
//                                                        new Object[]{
//                                                            pList.get(pid),
//                                                            pid,
//                                                            r_date, pathologyDate});
                                            } else {
                                                r_date = r_date.isAfter(pathologyDate) ? r_date : pathologyDate;
                                            }
                                        }
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }

            if (cnt_immuno == 1) {
            } else if (cnt_immuno > 1) {
                //LOG.log(Level.INFO, "IMMUNO\t{0}\t{1}\t{2}",new Object[]{pList.get(pid),pid,cnt_immuno});
            }

            {
                pT.append(pList.get(pid)).append(",");     // hospital
                pT.append("'").append(pid).append(",");    // patient ID

                final LinkedHashSet<Pathology> ps = pathologyList.get(pid);
                if (ps != null) {
                    // pathology source text
                    pT.append("\"");
                    for (Pathology p : ps) {
                        if (p.text.equals("-")) {
                            continue;
                        }
                        pT.append(p.text);
                        pT.append("\n==========================================\n");
                    }
                    pT.append("\"");

                } else {
                }

                pT.append("\n");
            }

            {
                dG.append(pList.get(pid)).append(",");      // hospital
                dG.append("'").append(pid).append(",");     // patient ID

                final LinkedHashSet<Pathology> ps = pathologyList.get(pid);
                if (ps != null) {
                    dG.append(left).append(",");
                    dG.append(right).append(",");
                    dG.append(l_date == null ? "-" : l_date).append(",");
                    dG.append(r_date == null ? "-" : r_date).append(",");
                    dG.append(bothIgnored).append(",");

                    // diagnosis
                    {
                        dG.append("\"");
                        for (Pathology p : ps) {
                            for (Diagnosis d : p.diagnosisList) {
                                if (d.text.equals("-")) {
                                    continue;
                                }

                                if (d.cancerType == CancerType.IGNORED || d.region == Region.IGNORED || d.side == Side.IGNORED) {
                                    continue;
                                }

                                dG.append("[").append(p.pathologyID).append("],");
                                dG.append(d.region).append(",");
                                dG.append(d.side).append(",");
                                dG.append(d.cancerType);
                                dG.append("\n");
                            }
                        }
                        dG.append("\"").append(",");
                    }
                    // diagnosis source text
                    {
                        dG.append("\"");
                        for (Pathology p : ps) {
                            for (Diagnosis d : p.diagnosisList) {
                                if (d.text.equals("-")) {
                                    continue;
                                }
                                dG.append("[").append(p.pathologyID).append("]\t");
                                dG.append(d.text);
                                dG.append("\n");
                            }
                        }
                        dG.append("\"");
                    }
                } else {
                }

                dG.append("\n");
            }

            {
                iM.append(pList.get(pid)).append(",");     // hospital
                iM.append("'").append(pid).append(",");    // patient ID

                final LinkedHashSet<Pathology> ps = pathologyList.get(pid);
                if (ps != null) {
                    iM.append(her2).append(",");
                    iM.append(ki67).append(",");
                    iM.append(er).append(",");
                    iM.append(pr).append(",");
                    boolean allChecked = !"-".equals(her2)
                            && !"-".equals(ki67)
                            && !"-".equals(er)
                            && !"-".equals(pr);
                    iM.append(!allChecked).append(",");
                    iM.append(her2Date == LocalDate.MAX ? "-" : her2Date == LocalDate.MIN ? "X" : her2Date).append(",");
                    iM.append(ki67Date == LocalDate.MAX ? "-" : ki67Date == LocalDate.MIN ? "X" : ki67Date).append(",");
                    iM.append(erDate == LocalDate.MAX ? "-" : erDate == LocalDate.MIN ? "X" : erDate).append(",");
                    iM.append(prDate == LocalDate.MAX ? "-" : prDate == LocalDate.MIN ? "X" : prDate).append(",");
                    iM.append(cnt_immuno).append(",");

                    // immunohistochemical source text
                    iM.append("\"");
                    for (Pathology p : ps) {
                        if (p.immuno.text.equals("-")) {
                            continue;
                        }
                        iM.append("[").append(p.pathologyID).append("]\n");
                        iM.append(p.immuno.text);
                        iM.append("\n");
                    }
                    iM.append("\"");
                } else {
                }

                iM.append("\n");
            }
        }

        FileUtils.writeStringToFile(new File(DATA_ROOT + "PATHOLOGY.csv"), pT.toString());
        FileUtils.writeStringToFile(new File(DATA_ROOT + "DIAGNOSIS.csv"), dG.toString());
        FileUtils.writeStringToFile(new File(DATA_ROOT + "IMMUNOCHL.csv"), iM.toString());
    }

}
