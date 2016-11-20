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
public enum Side {
    UNKNOWN("Unknown"), LEFT("Left"), RIGHT("Right"), MIXED("Mixed");
    private final String description;

    private Side(String s) {
        description = s;
    }

    @Override
    public String toString() {
        return this.description;
    }

    public static Side fromString(String s) {
        if (StringUtils.containsIgnoreCase(s, "left") && StringUtils.containsIgnoreCase(s, "right")) {
            return Side.MIXED;
        }
        return StringUtils.containsIgnoreCase(s, "left")
                ? Side.LEFT
                : StringUtils.containsIgnoreCase(s, "right")
                ? Side.RIGHT
                : Side.UNKNOWN;
    }
}
