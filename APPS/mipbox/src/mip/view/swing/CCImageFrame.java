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
import mip.data.ConnectedComponent;
import mip.data.image.BitImage;
import mip.data.image.CCImage;

/**
 *
 * @author ju
 */
public class CCImageFrame extends ColorImageFrame {

    public CCImageFrame(final CCImage cci) {
        super(cci.getColorImage());
        super.imgPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (imgPanel.img == null) {
                    return;
                }

                int x = e.getX();
                int y = e.getY();

                if ((x < 0) || (x >= imgPanel.img.getWidth()) || (y < 0) || (y >= imgPanel.img.getHeight())) {
                    return;
                }

                String message = " (" + x + ", " + y + ") ";

                TLongObjectMap<ConnectedComponent> componentTable = cci.getComponentTable();
                long id = cci.getPixel(x, y);
                ConnectedComponent c = componentTable.get(id);

                if (c != null) {
                    message += c.toString();
                    c.setColor(Color.WHITE); // TODO change ColorImage
                }

                setTitle(message);
            }
        });
    }

    public static void main(String[] args) {
        BitImage bi = new BitImage(512, 512);
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                bi.setPixel(x, y, (x + y) % 64 > 32);
            }
        }
        new CCImageFrame(new CCImage(bi)).setVisible(true);
    }

}
