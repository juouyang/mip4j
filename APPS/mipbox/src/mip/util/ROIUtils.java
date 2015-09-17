package mip.util;

import ij.gui.Roi;
import ij.io.RoiDecoder;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ROIUtils {

    private ROIUtils() { // singleton
    }

    public static List<Roi> uncompressROI(String zipFile) {
        List<Roi> rois = new ArrayList<>();

        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile))) {
            while (true) {
                ZipEntry entry = zin.getNextEntry();
                if (entry == null) {
                    break;
                }

                String name = entry.getName();

                if (!name.endsWith(".roi")) {
                    continue;
                }

                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = zin.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();

                    Roi roi = new RoiDecoder(out.toByteArray(), name).getRoi();

                    if (roi != null) {
                        rois.add(roi);
                    }
                }

            }

            zin.close();
        } catch (IOException ignore) {
        }

        return rois;
    }

    public static ArrayList<Roi> filterROI(List<Roi> rois, int z_position) {
        ArrayList<Roi> ret = new ArrayList<>();
        if (rois != null) {
            for (Roi r : rois) {
                if (r.getPosition() == z_position) {
                    ret.add(r);
                }
            }
        }

        return ret;
    }

    public static boolean withinROI(List<Roi> rois, int x, int y) {
        for (Roi r : rois) {
            if (r.contains(x, y)) {
                return true;
            }
        }
        return false;
    }
}
