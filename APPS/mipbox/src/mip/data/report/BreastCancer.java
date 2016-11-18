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
public enum BreastCancer {
    NOT("NOT", null),
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
    MIX("mixed", null);

    public final String[] keywords;
    private final String description;

    private BreastCancer(String d, String[] s) {
        description = d;
        keywords = s;
    }

    @Override
    public String toString() {
        return this.description;
    }

}
