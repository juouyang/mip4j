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

    GLAND("Glandular", null),
    WASHOUT("Washout", Color.RED),
    PLATEAU("Plateau", Color.MAGENTA),
    PERSIST("Persistent", Color.YELLOW),
    EDEMA("Edema", null),
    FLUID("Fluid", null),
    UNMAPPED("Unmapped", null);

    private final String description;
    public final Color color;

    private KineticType(String s, Color c) {
        description = s;
        color = c;
    }

    @Override
    public String toString() {
        return this.description;
    }

}
