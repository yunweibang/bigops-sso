package com.yunweibang.auth.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @version V1.0
 * @desc AES 加密工具类
 */
public class AESUtil {

    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";//默认的加密算法
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    
    /**
     * AES 加密操作
     *
     * @param content  待加密内容
     * @param password 加密密码
     * @return 返回Base64转码后的加密数据
     */
    public static String encrypt(String content, String password) {
        if (StringUtils.isEmpty(content)) {
            return content;
        }
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);// 创建密码器

            byte[] byteContent = content.getBytes("utf-8");

            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(password));// 初始化为加密模式的密码器

            byte[] result = cipher.doFinal(byteContent);// 加密

            return Base64.encodeBase64String(result);//通过Base64转码返回
        } catch (Exception ex) {
            Logger.getLogger(AESUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * AES 解密操作
     *
     * @param content
     * @param password
     * @return
     */
    public static String decrypt(String content, String password) {
        if (StringUtils.isEmpty(content) || "null".equals(content)) {
            return content;
        }
        try {
            //实例化
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);

            //使用密钥初始化，设置为解密模式
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(password));

            //执行操作
            byte[] result = cipher.doFinal(Base64.decodeBase64(content));

            return new String(result, "utf-8");
        } catch (Exception ex) {
            Logger.getLogger(AESUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * 生成加密秘钥
     *
     * @return
     */
    private static SecretKeySpec getSecretKey(final String password) {
        //返回生成指定算法密钥生成器的 KeyGenerator 对象
        KeyGenerator kg = null;

        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);

            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");

            secureRandom.setSeed(password.getBytes());
            kg.init(128, secureRandom);

            //生成一个密钥
            SecretKey secretKey = kg.generateKey();
            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);// 转换为AES专用密钥
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(AESUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /** 
     * 使用 HMAC-SHA1 签名方法对data进行签名 
     * @param data 被签名的字符串 
     * @param key 密钥      
     * @return  加密后的字符串 
     */  
    public static String genHMAC(String data, String key) {  
        byte[] result = null;  
        try {  
            byte[] bytekey = key.getBytes("UTF-8");
            //根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称    
            SecretKeySpec signinKey = new SecretKeySpec(bytekey, HMAC_SHA1_ALGORITHM);  
            //生成一个指定 Mac 算法 的 Mac 对象    
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);  
            //用给定密钥初始化 Mac 对象    
            mac.init(signinKey);  
            //完成 Mac 操作     
            byte[] rawHmac = mac.doFinal(data.getBytes("UTF-8"));  
            result = Base64.encodeBase64(rawHmac);  

        } catch (NoSuchAlgorithmException e) {  
            System.err.println(e.getMessage());  
        } catch (InvalidKeyException e) {  
            System.err.println(e.getMessage());  
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } 

        if (null != result) {  
            return new String(result);  
        } else {  
            return null;  
        } 
    }
    
    public static void main(String[] args) {
        //String s = "abcdefg";

        //System.out.println("s:" + s);

        //String s1 = AESUtil.encrypt(s, "123456");
        //System.out.println("s1:" + s1);

        System.out.println("s2:" + AESUtil.decrypt("pwH5F3JK/L3OZlewUI317MVzaev1i629jJC2Nq5gtrDzubx1BtH41syKgd57nE5Fhl+L28NLC8hU/7+RrozV4vHgkpVhyk+Mp5drt+kpj+M=", "bigops"));

    }

}