/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bmr.run;

import mip.model.data.bmr.BMRReport;

/**
 *
 * @author ju
 */
public class PDFReader {

    public static void main(String args[]) {
       BMRReport bmrr = new BMRReport(args[0]);
       System.out.println(bmrr);
    }
}
