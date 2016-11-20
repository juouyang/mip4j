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
    Side side = Side.UNKNOWN;
    Biopsy biopsyType = Biopsy.UNKNOWN;
    String diagnosisText = "";
    CancerType cancerType = CancerType.UNKNOWN;
    Pathology pathologyLink = null;
    BMR bmrLink = null;

    Diagnosis(Region l) {
        region = l;
    }

    @Override
    public String toString() {
        return this.pathologyLink.toString(this);
    }
    
}
