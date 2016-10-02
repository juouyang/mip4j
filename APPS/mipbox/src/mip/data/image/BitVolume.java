/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image;

import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.StackConverter;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import mip.data.Point3d;
import mip.data.image.mr.Kinetic;
import mip.util.ImageJUtils;
import mip.util.Timer;

/**
 *
 * @author ju
 */
public class BitVolume {
    
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
        new ImagePlus("", ImageJUtils.getByteImageStackFromBitImageArray(imageArrayXY)).show();
    }
    
    public void render() {
        ImageJ ij = new ImageJ();
        ij.exitWhenQuitting(true);
        
        Image3DUniverse univ = new Image3DUniverse();
        ImagePlus imp = new ImagePlus("", ImageJUtils.getByteImageStackFromBitImageArray(this.imageArrayXY));
        
        new StackConverter(imp).convertToGray8();
        
        ContentInstant ci = univ.addVoltex(imp, 2).getCurrent();
        
        if (ci != null) {
            univ.show();
        }
    }
    
    public static BitVolume regionGrowingByKinetic(Kinetic k, Point3d seed) {
        BitVolume selected = new BitVolume(k.getWidth(), k.getHeight(), k.getSize());
        
        if (!k.isStrongEnhanced(seed.x, seed.y, seed.z)) {
            return null;
        }
        
        Stack<Point3d> s = new Stack<>();
        BitVolume checked = new BitVolume(k.getWidth(), k.getHeight(), k.getSize());
        Point3d p = seed;
        s.push(p);
        checked.setPixel(seed.x, seed.y, seed.z, true);
        
        while (!s.isEmpty()) {
            p = s.pop();
            selected.setPixel(p.x, p.y, p.z, true);
            
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = p.x + dx;
                        int ny = p.y + dy;
                        int nz = p.z + dz;
                        
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
    
    public List<Roi> getROIs() {
        Timer t = new Timer();
        
        List<Roi> rois = new ArrayList<>();
        
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
        
        t.printElapsedTime("getROIs");
        return rois;
    }
    
    public static void main(String[] args) {
        BitVolume bv = new BitVolume(512, 512, 256);
        Point3d p = new Point3d(256, 256, 128);
        
        for (int i = 0; i < 100000; i++) {
            p.x = new Random().nextBoolean() ? p.x - 1 : p.x + 1;
            p.y = new Random().nextBoolean() ? p.y - 1 : p.y + 1;
            p.z = new Random().nextBoolean() ? p.z - 1 : p.z + 1;
            p.x = p.x >= 512 ? 511 : p.x < 0 ? 0 : p.x;
            p.y = p.y >= 512 ? 511 : p.y < 0 ? 0 : p.y;
            p.z = p.z >= 256 ? 255 : p.z < 0 ? 0 : p.z;
            bv.setPixel(p.x, p.y, p.z, true);
        }
        
        bv.getROIs();
        bv.show();
        bv.render();
    }
    
}
