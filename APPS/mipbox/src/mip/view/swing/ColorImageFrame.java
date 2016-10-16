/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.view.swing;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.JFrame;
import mip.data.image.ColorImage;
import mip.data.image.Component;
import mip.data.image.RGB;
import static mip.view.swing.AbstractImagePanel.TOKEN;

/**
 *
 * @author ju
 */
public class ColorImageFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public static void main(String args[]) throws InterruptedException {
        ColorImage ci = new ColorImage(512, 512);
        for (int y = 0; y < ci.getHeight(); y++) {
            for (int x = 0; x < ci.getWidth(); x++) {
                ci.setPixel(x, y, Component.getColorFromPalette((x + y) % Component.PALETTESIZE));
            }
        }
        new ColorImageFrame(ci).setVisible(true);
    }

    protected ColorImagePanel imgPanel = null;

    public ColorImageFrame(ColorImage ci) {
        imgPanel = new ColorImagePanel(ci);
        add(imgPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setResizable(false);
    }

    protected static final class ColorImagePanel extends AbstractImagePanel<ColorImage> {

        private static final long serialVersionUID = 1L;

        ColorImagePanel(ColorImage ci) {
            this();
            setImage(ci);
        }

        ColorImagePanel() {
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (img == null) {
                        return;
                    }

                    int x = e.getX();
                    int y = e.getY();

                    int w = img.getWidth();
                    int h = img.getHeight();

                    if ((x < 0) || (x >= w) || (y < 0) || (y >= h)) {
                        return;
                    }
                    setTitle(String.format("(%04d,%04d)=%s", x, y, img.getPixel(x, y)));
                }
            });
        }

        @Override
        protected BufferedImage newBufferedImage(int w, int h) {
            if ((bi == null) || (bi.getWidth() != w) || (bi.getHeight() != h)) {
                return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            }
            return bi;
        }

        @Override
        protected void convertImageToBufferedImage() {
            int[] data = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
            int i = 0;
            for (RGB v : img.getPixelArray(TOKEN)) {
                data[i++] = ((v.R << 16) & 0x00FF0000)
                        | ((v.G << 8) & 0x0000FF00)
                        | (v.B & 0x000000FF);
            }
        }

    }

}
