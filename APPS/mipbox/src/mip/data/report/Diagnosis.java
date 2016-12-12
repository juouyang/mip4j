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
        if (region == Region.FEMALE || region == Region.ABDOMEN) { // TODO: ignore not breast 
            side = Side.IGNORED;
        }
        biopsyType = Biopsy.fromString(s);
        cancerType = CancerType.fromString(s);
        text = s;
        pathologyLink = p;

        // NER-2 not amplified
        if (s.contains("HER")) {
            region = Region.BREAST;
            side = Side.IGNORED;

            if ((StringUtils.containsIgnoreCase(s, "not amplified")
                    || StringUtils.containsIgnoreCase(s, "Indeterminate"))) {
                cancerType = CancerType.HER2;
            } else if (StringUtils.containsIgnoreCase(s, "is amplified")
                    || StringUtils.containsIgnoreCase(s, "is equivocally amplified")) {
                cancerType = CancerType.IGNORED;
            }
        }

        // additional or revise report
        if (StringUtils.containsIgnoreCase(s, "report")) {
            if (StringUtils.containsIgnoreCase(s, "additional") || StringUtils.containsIgnoreCase(s, "addtional")
                    || StringUtils.containsIgnoreCase(s, "revise")) {
                region = Region.IGNORED;
                cancerType = CancerType.IGNORED;
                side = Side.IGNORED;
            }
        }
        if (s.contains("IMMUNOHISTOCHEMICAL STUDY") || s.contains("Revise:")) {
            region = Region.IGNORED;
            cancerType = CancerType.IGNORED;
            side = Side.IGNORED;
        }
        if (s.contains("修正")) {
            region = Region.IGNORED;
            cancerType = CancerType.IGNORED;
            side = Side.IGNORED;
        }

        // ignore cancer type for none breast region
        if (region != Region.BREAST && region != Region.LYMPH) {
            cancerType = CancerType.IGNORED;
            side = Side.IGNORED;
        }

        // ignore lymph node without cancer type or side
        if (region == Region.LYMPH && cancerType == CancerType.UNKNOWN) {
            cancerType = CancerType.IGNORED;
            side = Side.IGNORED;
        }
    }

    @Override
    public int compareTo(Diagnosis d) {
        int pid = pathologyLink.patientID.compareTo(pathologyLink.patientID);
        int sid = Integer.parseInt(bmrLink.studyID) - Integer.parseInt(bmrLink.studyID);
        int s = this.side.compareTo(side);
        return (pid != 0) ? pid : (sid != 0) ? sid : s;
    }
}
