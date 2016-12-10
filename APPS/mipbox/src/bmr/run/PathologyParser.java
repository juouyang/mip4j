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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import mip.data.report.BMR;
import mip.data.report.Pathology;
import static mip.data.report.Pathology.DT;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author ju
 */
public class PathologyParser {

    private static final String DATA_ROOT = "/home/ju/Dropbox/";
    private static final Base64.Decoder DECODER = Base64.getDecoder();

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

}
