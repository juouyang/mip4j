/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import java.awt.Color;

/**
 *
 * @author ju
 */
public enum KineticType {
    GLAND("High Enhancement", Color.LIGHT_GRAY, Color.LIGHT_GRAY),
    WASHOUT("Washout", Color.RED, Color.RED),
    PLATEAU("Plateau", Color.MAGENTA, Color.MAGENTA),
    PERSIST("Persist", Color.YELLOW, Color.YELLOW),
    EDEMA("Edema", Color.GREEN, Color.GREEN),
    FLUID("Fluid", Color.BLUE, Color.BLUE),
    NOISE("Noise", Color.DARK_GRAY, Color.DARK_GRAY),
    UNMAPPED("Unmapped", Color.ORANGE, Color.ORANGE);

    private final String description;
    public Color color;
    private final Color defaultColor;

    private KineticType(String s, Color c, Color dc) {
        description = s;
        color = c;
        defaultColor = dc;
    }

    @Override
    public String toString() {
        return this.description;
    }

    public Color getDefaultColor() {
        return this.defaultColor;
    }

}
