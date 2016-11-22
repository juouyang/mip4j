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

    @Override
    public String toString() {
        if (!pathologyLink.diagnosisList.contains(this)) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder(64);

        sb.append("\"'").append(String.format("%8s", pathologyLink.patientID)).append("\"").append(",");
        sb.append(String.format("%10s", pathologyLink.patientName)).append(",");
        sb.append(pathologyLink.biopsyDate).append(",");
        sb.append(region).append(",");
        sb.append(side).append(",");
        sb.append(biopsyType).append(",");
        sb.append(cancerType).append(",");
        sb.append("\"").append(diagnosisText).append("\"").append(",");
        sb.append(pathologyLink.pathologyID).append(",");

        sb.append((bmrLink != null) ? bmrLink.hospital : "-").append(",");
        sb.append((bmrLink != null) ? bmrLink.studyID : "-").append(",");
        sb.append((bmrLink != null) ? bmrLink.scanDate : "-").append(",");

        sb.append(pathologyLink.immuno);

        sb.append("\n");
        return sb.toString();
    }

}
