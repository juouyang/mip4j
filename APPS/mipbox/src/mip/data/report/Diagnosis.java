/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.report;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ju
 */
public class Diagnosis implements Comparable<Diagnosis> {

    public static void parse(Pathology p, String text) {
        String diagnosisText = StringUtils.replace(
                StringUtils.replace(
                        text,
                        "\"", // for spreadsheet
                        "'"
                ).trim(),
                "病理診斷：", // remove title
                "");

        for (final String s : diagnosisText.split("\n")) {
            Diagnosis d = new Diagnosis(p, s);
            p.diagnosisList.add(d);
        }
    }

    public Region region;
    public Side side;
    public final Biopsy biopsyType;
    public CancerType cancerType;
    public final String text;
    public final Pathology pathologyLink;
    BMR bmrLink = null;

    private Diagnosis(Pathology p, String s) {
        region = Region.fromString(s);
        side = Side.fromString(s);
        if (region == Region.UTERUS || region == Region.STOMACH) {
            side = Side.IGNORED;
        }
        biopsyType = Biopsy.fromString(s);
        cancerType = CancerType.fromString(s);
        text = s;
        pathologyLink = p;
    }

    @Override
    public int compareTo(Diagnosis d) {
        int pid = pathologyLink.patientID.compareTo(d.pathologyLink.patientID);
        int sid = Integer.parseInt(bmrLink.studyID) - Integer.parseInt(d.bmrLink.studyID);
        int s = this.side.compareTo(d.side);
        return (pid != 0) ? pid : (sid != 0) ? sid : s;
    }
}
