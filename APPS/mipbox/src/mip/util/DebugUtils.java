/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.util;

import java.util.Scanner;
import java.util.function.Consumer;

/**
 *
 * @author ju
 */
public class DebugUtils {

    private static final boolean IS_DEBUG = true;
    public static final Consumer<String> DBG = DebugUtils::debug;

    public static void debug(String s) {
        if (IS_DEBUG == true) {
            System.out.print(s);

        }
    }

    public static void pause() {
        Scanner input = new Scanner(System.in);
        System.out.print("Press enter to exit....");
        input.hasNextLine();
    }
}
