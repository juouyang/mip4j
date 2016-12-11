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
    UNKNOWN("Unknown", null),
    BREAST("Breast", new String[]{"breast", "nipple"}),
    SKIN("Skin", new String[]{"skin"}),
    LYMPH("Lymph", new String[]{"lymph node", "axilla"}),
    UTERUS("Uterus", new String[]{"uterus", "endometrium", "fallopian tube", "ovary"}),
    STOMACH("Stomach", new String[]{"stomach"}),
    ANUS("Anus", new String[]{"anus"}),
    ORAL("Oral", new String[]{"oral"}),
    INTESTINE("Intestine", new String[]{"intestine"}),
    ESOPHAGUS("Esophagus", new String[]{"esophagus"}),
    GALLBLADDER("Gallbladder", new String[]{"gallbladder"}),
    DUODENUM("Duodenum", new String[]{"duodenum"}),
    HAND("Hand", new String[]{"thumb", "wrist"}),;

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
        for (Region r : values()) {
            if (r.keywords != null) {
                for (String k : r.keywords) {
                    if (StringUtils.containsIgnoreCase(s, k)) {
                        return r;
                    }
                }
            }
        }
        return UNKNOWN;
//        if (StringUtils.startsWithIgnoreCase(s, Region.BREAST.keywords[0])
//                || StringUtils.containsIgnoreCase(s, Region.BREAST.keywords[1])) {
//            return Region.BREAST;
//        } else if (StringUtils.containsIgnoreCase(s, Region.LN.keywords[0])) {
//            return Region.LN;
//        } else if (StringUtils.containsIgnoreCase(s, Region.SKIN.keywords[0])) {
//            return Region.SKIN;
//        } else {
//            return Region.UNKNOWN;
//        }
    }

}
