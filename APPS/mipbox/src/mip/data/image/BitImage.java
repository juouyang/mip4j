package mip.data.image;

import ij.ImagePlus;
import ij.gui.Roi;
import java.io.IOException;
import java.util.BitSet;
import mip.data.image.mr.MR;
import mip.util.IOUtils;
import mip.util.ImageJUtils;
import mip.util.ROIUtils;
import mip.view.swing.AbstractImagePanel;
import mip.view.swing.BitImageFrame;

public class BitImage extends AbstractImage {

    BitSet pixelArray;

    public BitImage() {
        pixelArray = new BitSet(1);
    }

    public BitImage(int width, int height) {
        this.width = width;
        this.height = height;
        pixelArray = new BitSet(width * height);
    }

    @Override
    public void show() {
        new BitImageFrame(this).setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        MR mr = new MR(IOUtils.getFileFromResources("resources/bmr/2/080.dcm").toPath());
        BitImage bi = new BitImage(mr.width, mr.height);

        int i = 0;
        for (short s : mr.pixelArray) {
            bi.pixelArray.set(i++, s > 1500);
        }

        bi.show();
        bi.getImagePlus("").show();

        ImagePlus mrips = mr.getImagePlus("mr");
        mrips.setRoi(bi.getRoi());
        mrips.show();
    }

    //<editor-fold defaultstate="collapsed" desc="getters & setters">
    @Override
    protected ImagePlus convertImageToImagePlus(String title) {
        return new ImagePlus(title, ImageJUtils.getByteProcessorFromBitImage(this));
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
                return ROIUtils.createSelectionFromThreshold(getImagePlus("").getProcessor(), 255, 255);
            }
        }
        return null;
    }
    //</editor-fold>

}
