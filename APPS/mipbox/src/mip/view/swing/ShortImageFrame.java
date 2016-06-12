package mip.view.swing;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import mip.model.data.image.ShortImage;

public class ShortImageFrame extends JFrame {

    ShortImagePanel imgPanel = null;

    public ShortImageFrame(ShortImage si) {
        add(new ShortImagePanel(si), BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setResizable(false);
    }

    public static void main(String args[]) throws InterruptedException {
        short[] pixel = new short[512 * 512];
        for (int i = 0; i < pixel.length; i++) {
            pixel[i] = (short) (i % 512);
        }
        new ShortImageFrame(new ShortImage(512, 512, pixel)).setVisible(true);
    }
}
