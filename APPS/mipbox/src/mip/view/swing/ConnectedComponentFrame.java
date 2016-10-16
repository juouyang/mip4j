/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.view.swing;

import gnu.trove.map.TLongObjectMap;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import mip.data.image.BitImage;
import mip.data.image.Component;
import mip.data.image.ConnectedComponent;

/**
 *
 * @author ju
 */
public class ConnectedComponentFrame extends ColorImageFrame {

    private static final long serialVersionUID = 1L;

    public static void main(String[] args) {
        BitImage bi = new BitImage(512, 512);
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                bi.setPixel(x, y, (x + y) % 64 > 32);
            }
        }
        new ConnectedComponentFrame(new ConnectedComponent(bi)).setVisible(true);
    }

    public ConnectedComponentFrame(final ConnectedComponent cc) {
        super(cc.getColorImage(false));
        imgPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (imgPanel.img == null) {
                    return;
                }

                int x = e.getX();
                int y = e.getY();

                if ((x < 0) || (x >= imgPanel.img.getWidth())
                        || (y < 0) || (y >= imgPanel.img.getHeight())) {
                    return;
                }

                String message = " (" + x + ", " + y + ") ";

                TLongObjectMap<Component> componentTable = cc.getComponentTable();
                long id = cc.getPixel(x, y);
                Component c = componentTable.get(id);

                if (c != null) {
                    message += c.toString();
                    c.setColor(Color.WHITE); // TODO change ColorImage
                }

                setTitle(message);
            }
        });
    }

}
