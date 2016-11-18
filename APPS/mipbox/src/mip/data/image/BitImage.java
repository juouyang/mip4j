package mip.data.image;

import ij.ImagePlus;
import ij.gui.Roi;
import java.io.IOException;
import java.util.BitSet;
import mip.data.image.mr.MR;
import mip.data.image.mr.MROpener;
import mip.util.IJUtils;
import mip.util.ROIUtils;
import mip.view.swing.AbstractImagePanel;
import mip.view.swing.BitImageFrame;

public class BitImage extends AbstractImage {

    public static void main(String[] args) throws IOException {
        MR mr = MROpener.openMR();
        BitImage bi = new BitImage(mr.width, mr.height);
        int i = 0;
        for (short s : mr.pixelArray) {
            int x = i % bi.width;
            int y = i / bi.width;
            bi.setPixel(x, y, s > 1500);
            i++;
        }
        bi.show();
        {
            ImagePlus imp = mr.toImagePlus("");
            imp.setRoi(bi.getRoi());
            imp.show();
            IJUtils.exitWhenWindowClosed(imp.getWindow());
        }
    }

    private final BitSet pixelArray;

    public BitImage(int width, int height) {
        super(width, height);
        pixelArray = new BitSet(width * height);
    }

    @Override
    public void show() {
        new BitImageFrame(this).setVisible(true);
    }

    @Override
    protected ImagePlus toImagePlus(String title) {
        return new ImagePlus(title, IJUtils.toByteProcessor(this));
    }

    public void setPixel(int x, int y, boolean v) {
        pixelArray.set((y * width) + x, v);
    }

    public boolean getPixel(int x, int y) {
        return pixelArray.get((y * width) + x);
    }

    public BitSet getPixelArray(AbstractImagePanel.VIEW_ACCESS_TOKEN token) {
        token.hashCode();
        return pixelArray;
    }

    public Roi getRoi() {
        for (int i = 0; i < pixelArray.length(); i++) {
            if (pixelArray.get(i)) {
                return ROIUtils.createSelectionFromThreshold(IJUtils.toByteProcessor(this), 255, 255);
            }
        }
        return null;
    }

}
