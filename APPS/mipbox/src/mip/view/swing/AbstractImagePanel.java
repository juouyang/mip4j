package mip.view.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import mip.data.image.AbstractImage;

public abstract class AbstractImagePanel<T extends AbstractImage> extends JPanel {

    private static final long serialVersionUID = 1L;

    protected static final VIEW_ACCESS_TOKEN TOKEN = new VIEW_ACCESS_TOKEN();

    protected BufferedImage bi;
    protected T img;

    protected abstract BufferedImage newBufferedImage(int w, int h);

    protected abstract void convertImageToBufferedImage();

    public void setImage(T i) {
        if (i == null) {
            return;
        }

        img = i;

        final int w = i.getWidth();
        final int h = i.getHeight();

        if ((bi == null) || (bi.getWidth() != w) || (bi.getHeight() != h)) {
            bi = newBufferedImage(w, h);
        }

        setSize(w, h);
        setPreferredSize(new Dimension(w, h));
        update();
    }

    public void update() {
        SwingUtilities.invokeLater(() -> {
            invalidate();
            repaint();
        });
    }

    @Override
    public void paint(Graphics g) {
        if (img != null) {
            convertImageToBufferedImage();
            g.drawImage(bi, 0, 0, this);
        } else {
            super.paint(g);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (img != null) {
            return new Dimension(img.getWidth(), img.getHeight());
        }

        return new Dimension(1, 1);
    }

    protected void setTitle(String s) {
        final JFrame frame = (JFrame) SwingUtilities.getRoot(this);
        frame.setTitle(s);
    }

    public static class VIEW_ACCESS_TOKEN {

        private VIEW_ACCESS_TOKEN() {
        }
    }
}
