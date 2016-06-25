package mip.view.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import mip.data.image.ShortImage;
import mip.view.swing.base.AbstractImagePanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class ShortImagePanel extends AbstractImagePanel<ShortImage> {

    public static class VIEW_ACCESS_TOKEN {

        private VIEW_ACCESS_TOKEN() {
        }
    }
    private static final VIEW_ACCESS_TOKEN TOKEN = new VIEW_ACCESS_TOKEN();
    private int preX = 0;
    private int preY = 0;
    private int winCenter = 0;
    private int winWidth = 0;
    private short pixelMax = Short.MIN_VALUE;
    private short pixelMin = Short.MAX_VALUE;
    private int scale = 0;

    @Override
    public final void setImage(ShortImage si) {
        winCenter = si.getWindowCenter();
        winWidth = si.getWindowWidth();
        pixelMax = si.getMax();
        pixelMin = si.getMin();
        scale = (int) ((pixelMax - pixelMin) * 0.08);
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
                int wc = (e.getX() > preX) ? winCenter - scale : winCenter + scale;
                int ww = (e.getY() > preY) ? winWidth - scale : winWidth + scale;
                setWinCenterWidth(wc, ww);
                preX = e.getX();
                preY = e.getY();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                final int x = e.getX();
                final int y = e.getY();
                setTitle(String.format("(%04d,%04d)=%04d", x, y, img.getPixel(x, y)));
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

    private void setTitle(String s) {
        final JFrame frame = (JFrame) SwingUtilities.getRoot(ShortImagePanel.this);
        frame.setTitle(s);
    }
}
