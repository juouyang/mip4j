package mip.view.swing;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JFrame;

import mip.data.image.ShortImage;
import static mip.view.swing.AbstractImagePanel.TOKEN;

public class ShortImageFrame extends JFrame {

    ShortImagePanel imgPanel = null;

    public ShortImageFrame(ShortImage si) {
        imgPanel = new ShortImagePanel(si);
        add(imgPanel, BorderLayout.CENTER);
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

class ShortImagePanel extends AbstractImagePanel<ShortImage> {

    private int preX = 0;
    private int preY = 0;
    private int winCenter = 0;
    private int winWidth = 0;
    private short pixelMax = Short.MIN_VALUE;
    private short pixelMin = Short.MAX_VALUE;

    @Override
    public final void setImage(ShortImage si) {
        winCenter = si.getWindowCenter();
        winWidth = si.getWindowWidth();
        pixelMax = si.getMax();
        pixelMin = si.getMin();
        super.setImage(si);
    }

    public ShortImagePanel(ShortImage si) {
        this();
        setImage(si);
    }

    private ShortImagePanel() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int diff_x = Math.abs(e.getX() - preX);
                int diff_y = Math.abs(e.getY() - preY);
                int wc = (e.getX() > preX) ? winCenter - diff_x : winCenter + diff_x;
                int ww = (e.getY() > preY) ? winWidth - diff_y : winWidth + diff_y;
                setWinCenterWidth(wc, ww);
                preX = e.getX();
                preY = e.getY();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                final int x = e.getX();
                final int y = e.getY();
                setTitle(String.format("[%04d:%04d] (%04d,%04d)=%04d", winCenter, winWidth, x, y, img.getPixel(x, y)));
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 /*&& !e.isConsumed()*/) { //e.consume();
                    setWinCenterWidth(img.getWindowCenter(), img.getWindowWidth());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                preX = e.getX();
                preY = e.getY();
            }
        });
    }

    @Override
    protected BufferedImage newBufferedImage(int w, int h) {
        if ((bi == null) || (bi.getWidth() != w) || (bi.getHeight() != h)) {
            return new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        }
        return bi;
    }

    @Override
    protected void convertImageToBufferedImage() {
        final short[] pixels = img.getPixelArray(TOKEN);
        final int w = img.getWidth();
        final int h = img.getHeight();

        if (pixels.length != (w * h)) {
            throw new IllegalArgumentException();
        }

        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        convertShortsToBytes(pixels, winCenter - (winWidth / 2), winCenter + (winWidth / 2), data);
    }

    private void convertShortsToBytes(short[] inShortBuf, int windowMin, int windowMax, byte[] outByteBuf) {
        final int displayMin = 0;
        final int displayMax = 255;
        final int imgMin = (windowMin >= pixelMin) ? windowMin : pixelMin;
        final int imgMax = (windowMax <= pixelMax) ? windowMax : pixelMax;
        float displayRatio = (float) (displayMax - displayMin) / (imgMax - imgMin);

        for (int i = 0; i < inShortBuf.length; ++i) {
            int in = inShortBuf[i];
            int out;

            if (in < imgMin) {
                out = displayMin;
            } else if (in > imgMax) {
                out = displayMax;
            } else {
                out = (int) ((in - imgMin) * displayRatio);
            }

            outByteBuf[i] = (byte) out;
        }
    }

    private void setWinCenterWidth(int wc, int ww) {
        final int max_ww = (pixelMax - pixelMin + 1) * 2;
        winWidth = ww < 3 ? 3 : ww > max_ww ? max_ww : ww;
        winCenter = wc < pixelMin ? pixelMin : wc > pixelMax ? pixelMax : wc;
        update();
        setTitle(String.format("[%04d:%04d]", winCenter, winWidth));
    }
}
