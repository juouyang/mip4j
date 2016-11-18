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
public enum Biopsy {
    UNKNOWN("Unknwon", null),
    EXCISIONAL("Excisional", new String[]{
        "excision",
        "mastectomy",
        "operative biopsy",
        "specimen"}),
    NEEDLE("Needle", new String[]{
        "needle",
        "biopsy",
        ", biopsy",
        "incisional biopsy"});

    public final String[] keywords;
    private final String description;

    private Biopsy(String d, String[] s) {
        description = d;
        keywords = s;
    }

    @Override
    public String toString() {
        return this.description;
    }

}
