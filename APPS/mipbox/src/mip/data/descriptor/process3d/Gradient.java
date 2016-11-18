package mip.data.descriptor.process3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

public class Gradient {

    public static ImagePlus[] calculateGrad(ImagePlus imp) {
        float[] H_x = new float[]{-1, 0, 1};
        ImagePlus g_x = Convolve.convolveX(imp, H_x);

        float[] H_y = new float[]{-1, 0, 1};
        ImagePlus g_y = Convolve.convolveY(imp, H_y);

        float[] H_z = new float[]{-1, 0, 1};
        ImagePlus g_z = Convolve.convolveZ(imp, H_z);

        int w = imp.getWidth(), h = imp.getHeight();
        int d = imp.getStackSize();
        ImageStack grad = new ImageStack(w, h);
        ImageStack oriH = new ImageStack(w, h);
        ImageStack oriV = new ImageStack(w, h);

        for (int z = 0; z < d; z++) {
            FloatProcessor gradP = new FloatProcessor(w, h);
            grad.addSlice("", gradP);
            FloatProcessor oriHP = new FloatProcessor(w, h);
            oriH.addSlice("", oriHP);
            FloatProcessor oriVP = new FloatProcessor(w, h);
            oriV.addSlice("", oriVP);

            float[] valuesG = (float[]) gradP.getPixels();
            float[] valuesH = (float[]) oriHP.getPixels();
            float[] valuesV = (float[]) oriVP.getPixels();
            float[] x_ = (float[]) g_x.getStack().getProcessor(z + 1).getPixels();
            float[] y_ = (float[]) g_y.getStack().getProcessor(z + 1).getPixels();
            float[] z_ = (float[]) g_z.getStack().getProcessor(z + 1).getPixels();
            for (int i = 0; i < w * h; i++) {
                valuesG[i] = (float) Math.sqrt(x_[i] * x_[i] + y_[i] * y_[i] + z_[i] * z_[i]);
                valuesH[i] = (float) (Math.atan(y_[i] / x_[i]) + (Math.PI / 2));
                valuesV[i] = (float) (Math.atan(z_[i] / Math.sqrt(x_[i] * x_[i] + y_[i] * y_[i])) + (Math.PI / 2));
            }
        }
        ImagePlus retG = new ImagePlus("Magnitude", grad);
        ImagePlus retH = new ImagePlus("OrientationH", oriH);
        ImagePlus retV = new ImagePlus("OrientationV", oriV);
        return new ImagePlus[]{retG, retH, retV};
    }
}
