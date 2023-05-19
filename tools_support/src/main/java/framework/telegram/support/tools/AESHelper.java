package framework.telegram.support.tools;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import de.greenrobot.common.io.FileUtils;

/**
 * AES加解密类
 */
public class AESHelper {

    private AESHelper() {
    }

    public static String encrypt(byte[] messageBytes, String key) throws NullPointerException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return encrypt("AES/ECB/PKCS7Padding", messageBytes, key);
    }

    public static String decrypt(byte[] messageBytes, String key) throws NullPointerException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return decrypt("AES/ECB/PKCS7Padding", messageBytes, key);
    }

    public static byte[] decryptToBytes(byte[] messageBytes, String key) throws NullPointerException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return decryptToBytes("AES/ECB/PKCS7Padding", messageBytes, key);
    }

    public static String encrypt(String algorithm, byte[] messageBytes, String key) throws NullPointerException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (messageBytes == null) {
            throw new NullPointerException();
        }

        if (TextUtils.isEmpty(key) || key.length() < 16) {
            throw new BadPaddingException();
        }

        final String ALGORITHM = "AES";
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.substring(0, 16).getBytes(), ALGORITHM));
        return HexString.bufferToHex(cipher.doFinal(messageBytes));
    }

    public static String decrypt(String algorithm, byte[] messageBytes, String key) throws NullPointerException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (messageBytes == null) {
            throw new NullPointerException();
        }

        if (TextUtils.isEmpty(key) || key.length() < 16) {
            throw new BadPaddingException();
        }

        final String ALGORITHM = "AES";
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.substring(0, 16).getBytes(), ALGORITHM));
        return new String(cipher.doFinal(messageBytes));
    }

    public static byte[] decryptToBytes(String algorithm, byte[] messageBytes, String key) throws NullPointerException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (messageBytes == null) {
            throw new NullPointerException();
        }

        if (TextUtils.isEmpty(key) || key.length() < 16) {
            throw new BadPaddingException();
        }

        final String ALGORITHM = "AES";
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.substring(0, 16).getBytes(), ALGORITHM));
        return cipher.doFinal(messageBytes);
    }

    /**
     * 初始化 AES Cipher
     *
     * @param key
     * @param cipherMode
     * @return
     */
    private static Cipher initAESCipher(byte[] key, int cipherMode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        final String ALGORITHM = "AES";
        final String ALGORITHM_STR = "AES/ECB/PKCS7Padding";
        Cipher cipher = Cipher.getInstance(ALGORITHM_STR);
        cipher.init(cipherMode, new SecretKeySpec(key, ALGORITHM));
        return cipher;
    }

    /**
     * 对文件进行AES加密
     *
     * @param key
     * @param sourceFilePath
     * @param destFilePath
     * @return
     */
    public static boolean encryptFile(String key, String sourceFilePath, String destFilePath, CryptProgressListener listener) throws NullPointerException,
            BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        if (TextUtils.isEmpty(key) || key.length() < 16) {
            FileUtils.copyFile(sourceFilePath, destFilePath);
            return true;
        }

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            File sourceFile = new File(sourceFilePath);
            File destFile = new File(destFilePath);
            if (sourceFile.exists() && sourceFile.isFile()) {
                if (!destFile.getParentFile().exists()) {
                    destFile.getParentFile().mkdirs();
                }
                destFile.createNewFile();
                in = new FileInputStream(sourceFile);
                out = new FileOutputStream(destFile);

                Cipher cipher = initAESCipher(key.substring(0, 16).getBytes(), Cipher.ENCRYPT_MODE);
                BufferedInputStream bis = new BufferedInputStream(in);
                byte[] buffer = new byte[1024 * 100];
                int length;
                long total = sourceFile.length();
                long current = 0;
                while ((length = bis.read(buffer)) != -1) {
                    byte[] data = cipher.doFinal(buffer, 0, length);
                    out.write(data, 0, data.length);
                    out.flush();

                    current += length;
                    if (listener != null) {
                        if (!listener.progress(current, total)) {
                            return false;
                        }
                    }
                }
                bis.close();
                return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return false;
    }

    public static boolean decryptFile(String key, String sourceFilePath, String destFilePath, CryptProgressListener listener) throws NullPointerException, BadPaddingException,
            IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (TextUtils.isEmpty(key) || key.length() < 16) {
            throw new BadPaddingException();
        }

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            File sourceFile = new File(sourceFilePath);
            File destFile = new File(destFilePath);
            if (sourceFile.exists() && sourceFile.isFile()) {
                if (!destFile.getParentFile().exists()) {
                    destFile.getParentFile().mkdirs();
                }
                destFile.createNewFile();
                in = new FileInputStream(sourceFile);
                out = new FileOutputStream(destFile);

                Cipher cipher = initAESCipher(key.substring(0, 16).getBytes(), Cipher.DECRYPT_MODE);
                BufferedOutputStream cipherOutputStream = new BufferedOutputStream(out);
                byte[] buffer = new byte[1024 * 100 + 16];
                int length;
                long total = sourceFile.length();
                long current = 0;
                while ((length = in.read(buffer)) >= 0) {
                    byte[] data = cipher.doFinal(buffer, 0, length);
                    cipherOutputStream.write(data, 0, data.length);

                    current += length;
                    if (listener != null) {
                        if (!listener.progress(current, total)) {
                            return false;
                        }
                    }
                }
                cipherOutputStream.close();
                return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return false;
    }

    public static byte[] encryptBytes(String key, byte[] sourceBytes) throws NullPointerException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (TextUtils.isEmpty(key) || key.length() < 16) {
            throw new BadPaddingException();
        }

        ByteArrayInputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            in = new ByteArrayInputStream(sourceBytes);
            out = new ByteArrayOutputStream();

            Cipher cipher = initAESCipher(key.substring(0, 16).getBytes(), Cipher.ENCRYPT_MODE);
            byte[] buffer = new byte[1024 * 100];
            int length;
            while ((length = in.read(buffer)) != -1) {
                byte[] data = cipher.doFinal(buffer, 0, length);
                out.write(data, 0, data.length);
            }
            byte[] data = out.toByteArray();
            return data;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return null;
    }

    public static byte[] decryptBytes(String key, byte[] sourceBytes) throws NullPointerException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (TextUtils.isEmpty(key) || key.length() < 16) {
            throw new BadPaddingException();
        }

        ByteArrayInputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            in = new ByteArrayInputStream(sourceBytes);
            out = new ByteArrayOutputStream();

            Cipher cipher = initAESCipher(key.substring(0, 16).getBytes(), Cipher.DECRYPT_MODE);

            byte[] buffer = new byte[1024 * 100 + 16];
            int length;
            while ((length = in.read(buffer)) != -1) {
                byte[] data = cipher.doFinal(buffer, 0, length);
                out.write(data, 0, data.length);
            }
            byte[] data = out.toByteArray();
            return data;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return null;
    }

    /**
     * 随机生成密码
     *
     * @param length 密码的长度
     * @return 最终生成的密码
     */
    public static String generatePassword(int length) {
        // 最终生成的密码
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            // 随机生成0或1，用来确定是当前使用数字还是字母 (0则输出数字，1则输出字母)
            int charOrNum = random.nextInt();
            if (charOrNum == 1) {
                // 随机生成0或1，用来判断是大写字母还是小写字母 (0则输出小写字母，1则输出大写字母)
                password.append(random.nextInt(26) + random.nextInt(2) == 1 ? 65 : 97);
            } else {
                // 生成随机数字
                password.append(random.nextInt(10));
            }
        }
        return password.toString();
    }

    public interface CryptProgressListener {
        boolean progress(long currentPosition, long totalPosition);
    }
}
