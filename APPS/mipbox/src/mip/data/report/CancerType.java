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
    UNKNOWN("Unknown", null),
    IDC("IDC", new String[]{
        " invasive carcinoma",
        "mucinous carcinoma",
        "invasive tubular carcinoma",
        "invasive lobular carcinoma",
        "carcinoma with invasion",
        "invasive ductal and lobular carcinoma",
        "infiltrating ductal carcinoma",
        "invasive ductal carcinoma",
        "IDC"
    }),
    DCIS("DCIS", new String[]{
        "ductal carcinoma in situ",
        "atypical apocrine hyperplasia",
        "atypical ductal hyperplasia"
    }),
    BENIGN("benign", new String[]{
        "benign",
        "fibroadipose",
        "no carcinoma",
        "no metastasis",
        "no residual carcinoma",
        "no residual tumor",
        "no tumor involve",
        "no involvement of malignancy",
        "negative for metastasis",
        "negative for carcinoma",
        "fibroadenoma",
        "fibrocystic change",
        "fat necrosis",
        "intradermal nevus",
        "gynaecomastia",
        "negative for malignancy",
        "chronic inflammation",
        "necrotic adipose tissue",
        "usual ductal hyperplasia",
        "fibrosis",
        "adipose tissue",
        "intraductal papilloma",
        "steatocystoma",
        "accessory nipple"
    }),
    TBD("TBD", null),
    HER2("HER2", null),
    IGNORED("Ignored", new String[]{
        "no lymph node found",
        "no lymph found",
        "medial side",
        "lateral side"
    }),
    IMMUNO("Immuno", null),
    MALIGNANT("Malignant", new String[]{
        "carcinoma involvement",
        "fibroepithelial lesion",
        "paget disease",
        "atypical cell",
        "atypical ductal cell",
        "atypical papillary lesion",
        "carcinoma",
        "metastatic deposit",
        "micrometastases",
        "microcalcification",
        "sclerosing adenosis",
        "phyllodes tumor",
        "papillary lesion",
        "tumor necrosis",
        "malignant",
        "ER(+)",
        "ER(-)",
        "Her2",
        "Ki67"
    });

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
                        : CancerType.TBD);
        assert (ret != null);

        if (ret == CancerType.UNKNOWN) {
            for (String k : CancerType.MALIGNANT.keywords) {
                if (StringUtils.containsIgnoreCase(s, k)) {
                    ret = CancerType.MALIGNANT;
                }
            }

            for (String k : CancerType.IGNORED.keywords) {
                if (StringUtils.containsIgnoreCase(s, k)) {
                    ret = CancerType.IGNORED;
                }
            }
        }
        return ret;
    }
}
