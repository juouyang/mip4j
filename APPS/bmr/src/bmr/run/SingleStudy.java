package bmr.run;

import ij.ImageJ;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;

import mip.model.data.bmr.ColorMapping;
import mip.model.data.bmr.BMRStudy;
import mip.util.IOUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author ju
 */
public class SingleStudy {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        final String studyRoot = args[0]; // TODO commons-cli
        final String roiFile = scan_roi_file(studyRoot, args.length == 2 ? args[1] : args[0]); // TODO commons-cli  

        ImageJ ij = new ImageJ();
        ij.exitWhenQuitting(true);

        try {
            final BMRStudy mrStudy = new BMRStudy(Paths.get(studyRoot));
            if (roiFile != null) {
                mrStudy.addROI(roiFile);
            }
            final ColorMapping cm = new ColorMapping(mrStudy);
            final ImagePlus imp = cm.imp;
            imp.show();
            imp.setPosition(mrStudy.numberOfFrames / 2);
            imp.getCanvas().addMouseMotionListener(new MouseAdapter() {

                @Override
                public void mouseMoved(MouseEvent e) {

                    final int Z = imp.getCurrentSlice() - 1;
                    final int X = imp.getCanvas().getCursorLoc().x;
                    final int Y = imp.getCanvas().getCursorLoc().y;

                    imp.setTitle(cm.colorMappingInfo(X, Y, Z));
                    super.mouseMoved(e);
                }

            });
            imp.getCanvas().addMouseWheelListener(new MouseAdapter() {

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    final int Z = imp.getCurrentSlice() - 1;
                    final int X = imp.getCanvas().getCursorLoc().x;
                    final int Y = imp.getCanvas().getCursorLoc().y;

                    imp.setTitle(cm.colorMappingInfo(X, Y, Z));
                    super.mouseWheelMoved(e);
                }
            });

            FileSaver fs = new FileSaver(imp);
            fs.saveAsTiffStack(args[0] + "/cm.tif"); // TODO commons-cli
            FileUtils.writeStringToFile(new File(args[0] + "/result.txt"), cm.result.toString()); // TODO commons-cli

            System.out.println(cm.result.toString()); // TODO log4j
        } catch (IOException ex) {
            // TODO log4j
        }
    }

    private static String scan_roi_file(String studyRoot, String roiRoot) {
        ArrayList<Path> allFileNames = IOUtils.listFiles(roiRoot);

        for (Path fn : allFileNames) {
            if (fn.getFileName().toString().equalsIgnoreCase(new File(studyRoot).getName() + ".zip")) {
                return fn.toString();
            }
        }

        return null;
    }
}
