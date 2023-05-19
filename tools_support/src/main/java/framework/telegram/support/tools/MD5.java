package framework.telegram.support.tools;

import java.security.MessageDigest;

public class MD5 {
    private static final char[] DIGITS_LOWER = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public MD5() {
    }

    public static byte[] sha1(String str) {
        if (str == null) {
            return null;
        } else {
            try {
                MessageDigest md5 = MessageDigest.getInstance("SHA-1");
                md5.update(str.getBytes());
                byte[] m = md5.digest();
                return m;
            } catch (Exception var3) {
                throw new RuntimeException(var3);
            }
        }
    }

    public static String md5(String str) {
        if (str == null) {
            return null;
        } else {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.update(str.getBytes());
                return new String(encodeHex(messageDigest.digest()));
            } catch (Exception var2) {
                throw new RuntimeException(var2);
            }
        }
    }

    public static String md5(byte[] bytes) {
        if (bytes == null) {
            return null;
        } else {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.update(bytes);
                return new String(encodeHex(messageDigest.digest()));
            } catch (Exception var2) {
                throw new RuntimeException(var2);
            }
        }
    }

    public static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        int i = 0;

        for (int var4 = 0; i < l; ++i) {
            out[var4++] = DIGITS_LOWER[(240 & data[i]) >>> 4];
            out[var4++] = DIGITS_LOWER[15 & data[i]];
        }

        return out;
    }
}
