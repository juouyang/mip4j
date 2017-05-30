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

    //GLAND("Glandular", new Color(10, 10, 10)),
    GLAND("Glandular", Color.DARK_GRAY),
    //GLAND("Glandular", null),
    //WASHOUT("Washout", Color.RED),
    WASHOUT("Washout", new Color(226, 35, 26)),
    PLATEAU("Plateau", new Color(180 - 50, 105 - 50, 198 - 50)),
    //PLATEAU("Plateau", Color.MAGENTA),
    PERSIST("Persistent", new Color(255 - 100, 221 - 100, 0)),
    //EDEMA("Edema", Color.GREEN),
    //FLUID("Fluid", Color.BLUE),
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
