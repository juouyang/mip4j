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
public enum CancerType {
    UNKNOWN("unknown", null),
    IDC("IDC", new String[]{
        " invasive carcinoma",
        "mucinous carcinoma",
        "invasive tubular carcinoma",
        "invasive lobular carcinoma",
        "carcinoma with invasion",
        "invasive ductal and lobular carcinoma",
        "infiltrating ductal carcinoma",
        "invasive ductal carcinoma",
        "IDC"}),
    DCIS("DCIS", new String[]{"ductal carcinoma in situ"}),
    BENIGN("benign", new String[]{
        "benign",
        "fibroadipose",
        "no carcinoma",
        "no metastasis",
        "no residual carcinoma",
        "no tumor involve",
        "negative for carcinoma",
        "fibroadenoma",
        "fibrocystic change",
        "fat necrosis"}),
    MIXED("mixed", null);

    private final String description;
    final String[] keywords;

    private CancerType(String d, String[] s) {
        description = d;
        keywords = s;
    }

    @Override
    public String toString() {
        return this.description;
    }

    public static CancerType fromString(String s) {
        boolean hasIDC = false;
        boolean hasDCIS = false;
        boolean hasBenign = false;
        for (String searchString : CancerType.IDC.keywords) {
            if (StringUtils.containsIgnoreCase(s, "no residual " + searchString)
                    || StringUtils.containsIgnoreCase(s, searchString + ", removed")
                    || StringUtils.containsIgnoreCase(s, searchString + " removed")) {
                hasBenign = true;
                continue;
            }
            assert (!StringUtils.containsIgnoreCase(s, "no residual " + searchString) && !StringUtils.containsIgnoreCase(s, searchString + ", removed")
                    || StringUtils.containsIgnoreCase(s, searchString + " removed"));
            if (StringUtils.containsIgnoreCase(s, searchString)) {
                hasIDC = true;
                break;
            }
        }
        for (String searchString : CancerType.DCIS.keywords) {
            if (StringUtils.containsIgnoreCase(s, "no residual " + searchString)
                    || StringUtils.containsIgnoreCase(s, searchString + ", removed")
                    || StringUtils.containsIgnoreCase(s, searchString + " removed")) {
                hasBenign = true;
                continue;
            }
            assert (!StringUtils.containsIgnoreCase(s, "no residual " + searchString) && !StringUtils.containsIgnoreCase(s, searchString + ", removed")
                    || StringUtils.containsIgnoreCase(s, searchString + " removed"));
            if (StringUtils.containsIgnoreCase(s, searchString)) {
                hasDCIS = true;
                break;
            }
        }
        hasBenign = hasBenign ? !hasIDC && !hasDCIS : hasBenign;
        for (String searchString : CancerType.BENIGN.keywords) {
            if (StringUtils.containsIgnoreCase(s, searchString)) {
                hasBenign = true;
                break;
            }
        }
        int diagnosisCount = 0;
        diagnosisCount += hasIDC ? 1 : 0;
        diagnosisCount += hasDCIS ? 1 : 0;
        diagnosisCount += hasBenign ? 1 : 0;

        CancerType ret = (diagnosisCount == 0)
                ? CancerType.UNKNOWN
                : ((diagnosisCount == 1)
                        ? (hasIDC
                                ? CancerType.IDC
                                : (hasDCIS
                                        ? CancerType.DCIS
                                        : (hasBenign
                                                ? CancerType.BENIGN
                                                : null)))
                        : CancerType.MIXED);
        assert (ret != null);
        return ret;
    }
}
