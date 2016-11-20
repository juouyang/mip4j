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
public enum Region {
    UNKNOWN("unknown", null),
    SKIN("skin", new String[]{"skin"}),
    LN("lymph node", new String[]{"lymph node"}),
    BREAST("breast", new String[]{"breast", "nipple"});

    private final String description;
    final String[] keywords;

    private Region(String s, String[] k) {
        description = s;
        keywords = k;
    }

    @Override
    public String toString() {
        return this.description;
    }

    public static Region fromString(String s) {
        if (StringUtils.startsWithIgnoreCase(s, Region.BREAST.keywords[0])
                || StringUtils.containsIgnoreCase(s, Region.BREAST.keywords[1])) {
            return Region.BREAST;
        } else if (StringUtils.containsIgnoreCase(s, Region.LN.keywords[0])) {
            return Region.LN;
        } else if (StringUtils.containsIgnoreCase(s, Region.SKIN.keywords[0])) {
            return Region.SKIN;
        } else {
            return Region.UNKNOWN;
        }
    }

}
