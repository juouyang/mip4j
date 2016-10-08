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

    GLAND("Glandular", new Color(12, 12, 12)), WASHOUT("Washout", Color.RED), PLATEAU("Plateau", Color.MAGENTA), PERSIST("Persistent", Color.YELLOW), EDEMA("Edema", Color.GREEN), FLUID("Fluid", Color.BLUE), UNMAPPED("Unmapped", null);
    private final String description;
    final Color color;

    private KineticType(String s, Color c) {
        description = s;
        color = c;
    }

    @Override
    public String toString() {
        return this.description;
    }

}
