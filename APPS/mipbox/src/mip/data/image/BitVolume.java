/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image;

import ij.ImagePlus;
import ij.gui.Roi;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import mip.data.image.mr.Kinetic;
import mip.util.IJUtils;

/**
 *
 * @author ju
 */
public class BitVolume {

    public static BitVolume regionGrowing(Kinetic k, Point3d seed) {
        BitVolume selected = new BitVolume(k.width, k.height, k.size);

        /*if (!k.isStrongEnhanced(seed.X, seed.Y, seed.Z)) {
        return null;
        }*/
        Stack<Point3d> s = new Stack<>();
        BitVolume checked = new BitVolume(k.width, k.height, k.size);
        Point3d p = seed;
        s.push(p);
        checked.setPixel(seed.X, seed.Y, seed.Z, true);

        while (!s.isEmpty()) {
            p = s.pop();
            selected.setPixel(p.X, p.Y, p.Z, true);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = p.X + dx;
                        int ny = p.Y + dy;
                        int nz = p.Z + dz;
                        nx = nx < 0 ? 0 : nx >= k.width ? k.width - 1 : nx;
                        ny = ny < 0 ? 0 : ny >= k.height ? k.height - 1 : ny;
                        nz = nz < 0 ? 0 : nz >= k.size ? k.size - 1 : nz;

                        if (checked.getPixel(nx, ny, nz)) {
                            continue;
                        }

                        if (k.isStrongEnhanced(nx, ny, nz)) {
                            Point3d np = new Point3d(nx, ny, nz);
                            s.push(np);
                            checked.setPixel(nx, ny, nz, true);
                        }
                    }
                }
            }
        }

        return selected;
    }

    public static void main(String[] args) {
        BitVolume bv = new BitVolume(512, 512, 256);
        Point3d p = new Point3d(256, 256, 128);
        final Random random = new Random();

        for (int i = 0; i < 1000000; i++) {
            p.X = random.nextBoolean() ? p.X - 1 : p.X + 1;
            p.Y = random.nextBoolean() ? p.Y - 1 : p.Y + 1;
            p.Z = random.nextBoolean() ? p.Z - 1 : p.Z + 1;
            p.X = p.X >= 512 ? 511 : p.X < 0 ? 0 : p.X;
            p.Y = p.Y >= 512 ? 511 : p.Y < 0 ? 0 : p.Y;
            p.Z = p.Z >= 256 ? 255 : p.Z < 0 ? 0 : p.Z;
            bv.setPixel(p.X, p.Y, p.Z, true);
        }

        bv.getROIs();
        bv.show();
        bv.render();
    }

    private final BitImage[] imageArrayXY;

    public BitVolume(int width, int height, int size) {
        imageArrayXY = new BitImage[size];
        for (int i = 0; i < size; i++) {
            imageArrayXY[i] = new BitImage(width, height);
        }
    }

    public void setPixel(int x, int y, int z, boolean v) {
        imageArrayXY[z].setPixel(x, y, v);
    }

    public boolean getPixel(int x, int y, int z) {
        return imageArrayXY[z].getPixel(x, y);
    }

    public void show() {
        ImagePlus imp = new ImagePlus("", IJUtils.toImageStack(imageArrayXY));
        imp.show();
        IJUtils.exitWhenWindowClosed(imp.getWindow());
    }

    public void render() {
        ImagePlus imp = new ImagePlus("", IJUtils.toImageStack(this.imageArrayXY));
        IJUtils.render(imp, 2, 0, 0);
    }

    public List<Roi> getROIs() {
        List<Roi> rois = new ArrayList<>(50);

        int i = 0;
        for (BitImage bi : imageArrayXY) {
            i++;
            final Roi roi = bi.getRoi();
            if (roi == null) {
                continue;
            }
            roi.setPosition(i);
            rois.add(roi);
        }

        return rois;
    }

}
