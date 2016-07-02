/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.view.swing;

import gnu.trove.map.TLongObjectMap;
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
        addMouseMotionListener(new MouseMotionAdapter() {
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
                }

                setTitle(message);
            }
        });
    }

    public static void main(String[] args) {
        BitImage bi = new BitImage(512, 512);
        bi.show();
        CCImage cci = new CCImage(bi);
        cci.show();
        new CCImageFrame(cci).setVisible(true);
    }

}
