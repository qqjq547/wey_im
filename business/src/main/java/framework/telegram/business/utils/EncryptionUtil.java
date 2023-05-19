package framework.telegram.business.utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by lzh on 19-7-30.
 * INFO: 图片加解密测试用的代码
 */
public  class EncryptionUtil {

    public EncryptionUtil() {
    }

    public static  byte[] getKey() {
        //KeyGenerator，密钥生成器
        KeyGenerator keyGen = null;//算法：DES,DESede,AES
        try {
            keyGen = KeyGenerator.getInstance("DES");
            //初始化密钥生成器
            keyGen.init(56); //各算法密钥长度不同，参见说明
            //生成密钥
            SecretKey secretKey = keyGen.generateKey();
            //生产字节码数据
            byte[] key = secretKey.getEncoded();
            return key;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static  byte[] encryptionByKey(byte[] data,byte[] key,boolean decrypt) {
        try {
        //通过字节码数据key 恢复密钥
        SecretKey secretKey = new SecretKeySpec(key, "DES");
        //Cipher完成加密/解密工作
        Cipher cipher = null;
        cipher = Cipher.getInstance("DES");
        //根据密钥，对Cipher初始化，并选择加密还是解密
        if (decrypt){//解密
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
        }else {//加密
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        }
        return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

}
