/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bmr.run;

import ij.IJ;
import ij.Menus;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import mip.data.image.mr.BMRStudy;
import mip.data.image.mr.Kinetic;
import mip.util.IJUtils;

/**
 *
 * @author ju
 */
public class Foo {

    static String title = "Example";
    static int width = 512, height = 512;

    public static void main(String[] args) {
        IJUtils.openImageJ(true);
        {
            MenuItem item = new MenuItem("New 8-bit Image");
            Menus.getImageJMenu("Plugins").addSeparator();
            Menus.getImageJMenu("Plugins").add(item);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    GenericDialog gd = new GenericDialog("New Image");
                    gd.addStringField("Title: ", title);
                    gd.addNumericField("Width: ", width, 0);
                    gd.addNumericField("Height: ", height, 0);
                    gd.showDialog();
                    if (gd.wasCanceled()) {
                        return;
                    }
                    title = gd.getNextString();
                    width = (int) gd.getNextNumber();
                    height = (int) gd.getNextNumber();
                    IJ.newImage(title, "8-bit", width, height, 1);
                }
            });
        }
        {
            MenuItem item = new MenuItem("Breast MRI Study");
            Menus.getImageJMenu("File>Import").insert(item, 0);
            Menus.getImageJMenu("File>Import").insertSeparator(1);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DirectoryChooser dc = new DirectoryChooser("Breast MRI Study");
                    if (dc.getDirectory() == null) {
                        return;
                    }
                    File studyRoot = new File(dc.getDirectory());
                    BMRStudy bmr = new BMRStudy(studyRoot.toPath());
                    Kinetic k = new Kinetic(bmr, true);
                    k.show();
                }
            });
        }
    }
}
