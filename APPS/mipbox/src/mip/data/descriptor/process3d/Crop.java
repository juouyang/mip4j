package mip.data.descriptor.process3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;

import java.awt.image.ColorModel;

public class Crop {

    static public ImagePlus performCrop(ImagePlus imp, int min_x, int max_x, int min_y, int max_y, int min_z, int max_z, boolean adjust_origin) {

        int original_width = imp.getWidth();

        int new_width = (max_x - min_x) + 1;
        int new_height = (max_y - min_y) + 1;

        int first_slice = min_z + 1;
        int last_slice = max_z + 1;

        ImageStack stack = imp.getStack();
        ImageStack new_stack = new ImageStack(new_width, new_height);

        int type = imp.getType();

        ColorModel cm = null;
        if (ImagePlus.COLOR_256 == type) {
            cm = stack.getColorModel();
        }

        switch (type) {

            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:

                for (int slice = first_slice; slice <= last_slice; slice++) {

                    byte[] slice_bytes = (byte[]) stack.getPixels(slice);

                    byte[] new_slice = new byte[new_width * new_height];
                    for (int y = min_y; y <= max_y; ++y) {
                        System.arraycopy(slice_bytes, y * original_width + min_x, new_slice, (y - min_y) * new_width, new_width);
                    }

                    ByteProcessor bp = new ByteProcessor(new_width, new_height);

                    bp.setPixels(new_slice);

                    new_stack.addSlice(null, bp);

                    IJ.showProgress((slice - first_slice) / ((last_slice - first_slice) + 1));
                }
                break;

            case ImagePlus.GRAY16:

                for (int slice = first_slice; slice <= last_slice; slice++) {

                    short[] slice_shorts = (short[]) stack.getPixels(slice);

                    short[] new_slice = new short[new_width * new_height];
                    for (int y = min_y; y <= max_y; ++y) {
                        System.arraycopy(slice_shorts, y * original_width + min_x, new_slice, (y - min_y) * new_width, new_width);
                    }

                    ShortProcessor sp = new ShortProcessor(new_width, new_height);

                    sp.setPixels(new_slice);

                    new_stack.addSlice(null, sp);

                    IJ.showProgress((slice - first_slice) / ((last_slice - first_slice) + 1));
                }
                break;

            case ImagePlus.COLOR_RGB:

                for (int slice = first_slice; slice <= last_slice; slice++) {

                    int[] slice_ints = (int[]) stack.getPixels(slice);

                    int[] new_slice = new int[new_width * new_height];
                    for (int y = min_y; y <= max_y; ++y) {
                        System.arraycopy(slice_ints, y * original_width + min_x, new_slice, (y - min_y) * new_width, new_width);
                    }

                    ColorProcessor cp = new ColorProcessor(new_width, new_height);

                    cp.setPixels(new_slice);

                    new_stack.addSlice(null, cp);

                    IJ.showProgress((slice - first_slice) / ((last_slice - first_slice) + 1));
                }
                break;

            case ImagePlus.GRAY32:

                for (int slice = first_slice; slice <= last_slice; slice++) {

                    float[] slice_floats = (float[]) stack.getPixels(slice);

                    float[] new_slice = new float[new_width * new_height];
                    for (int y = min_y; y <= max_y; ++y) {
                        System.arraycopy(slice_floats, y * original_width + min_x, new_slice, (y - min_y) * new_width, new_width);
                    }

                    FloatProcessor fp = new FloatProcessor(new_width, new_height);

                    fp.setPixels(new_slice);

                    new_stack.addSlice(null, fp);

                    IJ.showProgress((slice - first_slice) / ((last_slice - first_slice) + 1));
                }
                break;
            default:
                break;

        }

        if (ImagePlus.COLOR_256 == type) {
            if (cm != null) {
                new_stack.setColorModel(cm);
            }
        }

        IJ.showProgress(1);

        ImagePlus imagePlus = new ImagePlus("cropped " + imp.getTitle(), new_stack);

        /*
         * Should we adjust the origin according to the crop rather than just copying the Calibration? I'm not doing so by default, since the ImageJ built-in Crop command doesn't. However, you can select that option in the interface.
         */
        Calibration oldCalibration = imp.getCalibration();

        if (oldCalibration != null) {

            Calibration newCalibration = (Calibration) oldCalibration.clone();

            if (adjust_origin) {
                newCalibration.xOrigin -= min_x;
                newCalibration.yOrigin -= min_y;
                newCalibration.zOrigin -= min_z;
            }

            if (newCalibration != null) {
                imagePlus.setCalibration(newCalibration);
            }

        }

        if (imp.getProperty("Info") != null) {
            imagePlus.setProperty("Info", imp.getProperty("Info"));
        }

        imagePlus.setFileInfo(imp.getOriginalFileInfo());

        return imagePlus;
    }
}
