/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.report;

/**
 *
 * @author ju
 */
public class Diagnosis {

    final Region region;
    final Side side;
    final Biopsy biopsyType;
    final CancerType cancerType;
    final String diagnosisText;
    Pathology pathologyLink = null;
    BMR bmrLink = null;

    Diagnosis(String s) {
        region = Region.fromString(s);
        side = Side.fromString(s);
        biopsyType = Biopsy.fromString(s);
        cancerType = CancerType.fromString(s);
        diagnosisText = s;
    }

    public String toCSVString() {
        if (!pathologyLink.diagnosisList.contains(this)) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder(64);

        sb.append("\"'").append(pathologyLink.patientID).append("\"").append(",");
        sb.append(pathologyLink.biopsyDate).append(",");
        sb.append(region).append(",");
        sb.append(side).append(",");
        sb.append(cancerType).append(",");

        sb.append((bmrLink != null) ? bmrLink.hospital : "-").append(",");
        sb.append((bmrLink != null) ? bmrLink.studyID : "-").append(",");
        sb.append((bmrLink != null) ? bmrLink.scanDate : "-").append(",");

        sb.append(pathologyLink.immuno.toCSVString());

        return sb.toString();
    }

}
