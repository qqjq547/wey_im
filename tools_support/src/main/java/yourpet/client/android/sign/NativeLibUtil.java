package yourpet.client.android.sign;

import android.content.Context;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import framework.telegram.support.tools.MD5;

public class NativeLibUtil {

    private NativeLibUtil() {
    }

    private static class SingletonHolder {
        public static final NativeLibUtil INSTANCE = new NativeLibUtil();
    }

    public static NativeLibUtil getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * HTTP\SOCKET 加密
     *
     * @param context
     * @param testServer
     * @param data
     * @param mode       1:加密 2:解密
     * @return
     */
    public byte[] sign1(Context context, boolean testServer, byte[] data, int mode) {
        synchronized (this) {
            try {
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                SecretKeySpec secretKeySpec;
                if (testServer) {
                    secretKeySpec = new SecretKeySpec("1234560000000000".getBytes(), "AES");
                } else {
                    secretKeySpec = new SecretKeySpec("5I4SGU42ETFI4TQG".getBytes(), "AES");
                }
                cipher.init(mode, secretKeySpec);
                return cipher.doFinal(data);
            } catch (Exception e) {
                return new byte[0];
            }
        }
    }

    /**
     * 用户密码
     *
     * @param context
     * @param testServer
     * @param data
     * @return
     */
    public String sign2(Context context, boolean testServer, String data) {
        synchronized (this) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                if (testServer) {
                    messageDigest.update((data + "!@#b%^&*9").getBytes());
                } else {
                    messageDigest.update((data + "!@#b%^&*9").getBytes());
                }
                return new String(MD5.encodeHex(messageDigest.digest()));
            } catch (Exception e) {
                return "";
            }
        }
    }
}
