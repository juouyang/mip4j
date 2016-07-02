package mip.view.swing;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.BitSet;
import javax.swing.JFrame;
import mip.data.image.BitImage;
import static mip.view.swing.AbstractImagePanel.TOKEN;

public class BitImageFrame extends JFrame {

    BitImagePanel imgPanel = null;

    public BitImageFrame(BitImage bi) {
        imgPanel = new BitImagePanel(bi);
        add(imgPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setResizable(false);
    }

    public static void main(String args[]) {
        BitImage bi = new BitImage(512, 512);
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                bi.setPixel(x, y, x > y);
            }
        }

        new BitImageFrame(bi).setVisible(true);
    }
}

class BitImagePanel extends AbstractImagePanel<BitImage> {

    public BitImagePanel(BitImage bi) {
        this();
        setImage(bi);
    }

    public BitImagePanel() {
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

                setTitle(" (" + x + ", " + y + ") " + img.getPixel(x, y));
            }
        });
    }

    @Override
    protected final void convertImageToBufferedImage() {
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        toGrayBytes(data, img.getPixelArray(TOKEN), 0, 255);
    }

    private void toGrayBytes(byte[] grayBytes, BitSet bitBuffer, int imgMin, int imgMax) {
        int w = img.getWidth();
        int h = img.getHeight();

        for (int i = 0; i < (w * h); ++i) {
            if (bitBuffer.get(i)) {
                grayBytes[i] = (byte) imgMax;
            } else {
                grayBytes[i] = (byte) imgMin;
            }
        }
    }

    @Override
    protected BufferedImage newBufferedImage(int w, int h) {
        if ((bi == null) || (bi.getWidth() != w) || (bi.getHeight() != h)) {
            return new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        }
        return bi;
    }
}
