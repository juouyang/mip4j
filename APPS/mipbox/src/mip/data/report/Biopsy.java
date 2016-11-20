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

    private final String description;
    final String[] keywords;

    private Biopsy(String d, String[] s) {
        description = d;
        keywords = s;
    }

    @Override
    public String toString() {
        return this.description;
    }

    public static Biopsy fromString(String s) {
        if ((s.contains(Biopsy.NEEDLE.keywords[0]) && s.contains(Biopsy.NEEDLE.keywords[1]))
                || s.contains(Biopsy.NEEDLE.keywords[2])
                || s.contains(Biopsy.NEEDLE.keywords[3])) {
            return Biopsy.NEEDLE;
        }
        for (String keyword : Biopsy.EXCISIONAL.keywords) {
            if (s.contains(keyword)) {
                return Biopsy.EXCISIONAL;
            }
        }

        return Biopsy.UNKNOWN;
    }

}
