package mip.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DCMUtils {

    public static boolean isDICOM(File file) {
        if (file.length() >= 132) {
            try (InputStream in = new FileInputStream(file)) {
                byte[] b = new byte[128 + 4];
                in.read(b, 0, 132);

                for (int i = 0; i < 128; i++) {
                    if (b[i] != 0) {
                        return false;
                    }
                }

                if (b[128] != 68 || b[129] != 73 || b[130] != 67 || b[131] != 77) {
                    return false;
                }
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }

            return true;
        }

        return false;
    }

    private DCMUtils() { //
    }
}
