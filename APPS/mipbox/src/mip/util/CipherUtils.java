package mip.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import static mip.util.DGBUtils.DBG;
import org.apache.commons.codec.binary.Base64;

public class CipherUtils {

    private final static byte[] KEY = "1234567890123456".getBytes(); // 16 char

    public static String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            final SecretKeySpec secretKey = new SecretKeySpec(KEY, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            final String encryptedString = Base64.encodeBase64String(
                    cipher.doFinal(strToEncrypt.getBytes())
            );
            return encryptedString;
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException |
                InvalidKeyException |
                IllegalBlockSizeException |
                BadPaddingException e) {
        }
        return null;

    }

    public static String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            final SecretKeySpec secretKey = new SecretKeySpec(KEY, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            final String decryptedString = new String(
                    cipher.doFinal(Base64.decodeBase64(strToDecrypt))
            );
            return decryptedString;
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException |
                InvalidKeyException |
                IllegalBlockSizeException |
                BadPaddingException e) {
        }
        return null;
    }

    public static void main(String args[]) {

        String strToEncrypt = "foo";
        final String encryptedStr = CipherUtils.encrypt(strToEncrypt.trim());
        DBG.accept("String to Encrypt : " + strToEncrypt + "\n");
        DBG.accept("Encrypted : " + encryptedStr + "\n");

        final String strToDecrypt = encryptedStr;
        final String decryptedStr = CipherUtils.decrypt(strToDecrypt.trim());
        DBG.accept("String To Decrypt : " + strToDecrypt + "\n");
        DBG.accept("Decrypted : " + decryptedStr + "\n");

    }
}
