package mip.util;

import gdcm.FileMetaInformation;
import gdcm.Image;
import gdcm.ImageChangeTransferSyntax;
import gdcm.ImageReader;
import gdcm.ImageWriter;
import gdcm.TransferSyntax;
import static gdcm.TransferSyntax.TSType.ImplicitVRLittleEndian;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DCMUtils {

    private static final TransferSyntax TS = new TransferSyntax(ImplicitVRLittleEndian);

    public static boolean isDCM(File file) {
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

    public static boolean decompressDCM(String i, String o) throws Exception {
        String input = i;
        String output = o;
        ImageReader reader = new ImageReader();
        reader.SetFileName(input);
        boolean ret = reader.Read();
        if (!ret) {
            reader.delete();
            throw new Exception("Could not read: " + input);
        }

        ImageChangeTransferSyntax change = new ImageChangeTransferSyntax();
        change.SetTransferSyntax(TS);
        change.SetInput(reader.GetImage());
        if (!change.Change()) {
            change.delete();
            reader.delete();
            throw new Exception("Could not change: " + input);
        }

        Image out = change.GetOutput();

        // Set the Source Application Entity Title
        FileMetaInformation.SetSourceApplicationEntityTitle("GDCM");

        ImageWriter writer = new ImageWriter();
        writer.SetFileName(output);
        writer.SetFile(reader.GetFile());
        writer.SetImage(out);
        ret = writer.Write();
        if (!ret) {
            change.delete();
            reader.delete();
            writer.delete();
            throw new Exception("Could not write: " + output);
        }

        change.delete();
        reader.delete();
        writer.delete();
        return ret;
    }

    private DCMUtils() { //
    }
}
