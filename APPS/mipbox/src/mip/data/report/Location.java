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
public enum Location {
    UNKNOWN("unknown", null),
    SKIN("skin", new String[]{"skin"}),
    LN("lymph node", new String[]{"lymph node"}),
    BREAST("breast", new String[]{"breast", "nipple"});

    public final String[] keywords;
    private final String description;

    private Location(String s, String[] k) {
        description = s;
        keywords = k;
    }

    @Override
    public String toString() {
        return this.description;
    }

}
