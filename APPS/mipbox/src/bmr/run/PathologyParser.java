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
import mip.data.report.BMR;
import mip.data.report.Diagnosis;
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
            csv.append(pList.get(pid)).append(",");     // hospital
            csv.append("'").append(pid).append(",");    // patient ID

            if (pathologyList.get(pid) != null) {
                // diagnosis source text
                csv.append("\"");
                for (Pathology p : pathologyList.get(pid)) {
                    for (Diagnosis d : p.diagnosisList) {
                        csv.append(d.text);
                        csv.append("\n--------------------------------\n");
                    }
                }
                csv.append("\"").append(",");

                // immunohistochemical source text
                csv.append("\"");
                for (Pathology p : pathologyList.get(pid)) {
                    csv.append(p.immuno.text);
                    csv.append("\n--------------------------------\n");
                }
                csv.append("\"").append(",");

                // pathology source text
                csv.append("\"");
                for (Pathology p : pathologyList.get(pid)) {
                    csv.append(p.text);
                    csv.append("\n--------------------------------\n");
                }
                csv.append("\"").append(",");

            } else {
                csv.append(",").append(",").append(",");
            }

            csv.append("\n");
        }
        FileUtils.writeStringToFile(new File(DATA_ROOT + "PATHOLOGY.csv"), csv.toString());

    }

}
