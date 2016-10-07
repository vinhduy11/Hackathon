package com.mservice.momo.vertx.processor;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by duyhuynh on 19/04/2016.
 */
public class DataCrypt {

    private static byte[] secret;

    static {
        secret = new byte[]{118,
                96,
                -122,
                -122,
                -121,
                8,
                119,
                104,
                118,
                118,
                119,
                103,
                103,
                96,
                119,
                -121,
                118,
                96,
                -122,
                -122,
                -121,
                8,
                119,
                104
        };
    }

    public static String encrypt(String type, String salt, String key, String value) {
        if (value == null) {
            return null;
        } else {
            byte[][] ciphertext = new byte[2][];

            try {
                SecretKeyFactory ct = SecretKeyFactory.getInstance("DESede");
                if (ct == null) {
                    throw new GeneralSecurityException("No DESede factory");
                }

                byte[] result = secret;
                SecretKey enc = ct.generateSecret(new DESedeKeySpec(result));
                if (enc == null) {
                    throw new GeneralSecurityException("No secret key");
                }

                Cipher plaintext = Cipher.getInstance("DESede/CBC/PKCS5Padding");
                byte[] iv = generateIV(type, salt, key);
                plaintext.init(1, enc, new IvParameterSpec(iv));
                ciphertext[0] = plaintext.update(stringToByteArray(value));
                ciphertext[1] = plaintext.doFinal(generateMAC(type, salt, key, value));
            } catch (GeneralSecurityException var11) {
                return null;
            }

            if (ciphertext[0] == null) {
                ciphertext[0] = new byte[0];
            }

            if (ciphertext[1] == null) {
                ciphertext[1] = new byte[0];
            }

            byte[] ct1 = new byte[ciphertext[0].length + ciphertext[1].length];
            System.arraycopy(ciphertext[0], 0, ct1, 0, ciphertext[0].length);
            System.arraycopy(ciphertext[1], 0, ct1, ciphertext[0].length, ciphertext[1].length);
            StringBuffer result1 = new StringBuffer(2 * ct1.length + 1);
            result1.append("b");
            result1.append(new Base64().encode(ct1));
            String plaintext1 = getSafePlaintext(type, key, value);
            if (plaintext1 != null) {
                result1.append(':').append(plaintext1);
            }

            return result1.toString();
        }
    }

    public static String getSafePlaintext(String type, String key, String value) {
        return null;
    }

    private static byte[] generateIV(String type, String salt, String key) {
        try {
            MessageDigest ex = MessageDigest.getInstance("SHA-1");
            if (type != null) {
                ex.update(stringToByteArray(type));
            }

            if (salt != null) {
                ex.update(stringToByteArray(salt));
            }

            if (key != null) {
                ex.update(stringToByteArray(key));
            }

            byte[] sha = ex.digest();
            byte[] iv = new byte[8];

            for (int scan = 0; scan < sha.length; ++scan) {
                iv[scan % 8] ^= sha[scan];
            }

            return iv;
        } catch (NoSuchAlgorithmException var8) {
            throw new IllegalStateException(var8.getMessage());
        }
    }

    private static byte[] generateMAC(String type, String salt, String key, String value) {
        try {
            MessageDigest ex = MessageDigest.getInstance("SHA-1");
            if (type != null) {
                ex.update(stringToByteArray(type));
            }

            if (salt != null) {
                ex.update(stringToByteArray(salt));
            }

            if (key != null) {
                ex.update(stringToByteArray(key));
            }

            if (value != null) {
                ex.update(stringToByteArray(value));
            }

            byte[] sha = ex.digest();
            byte[] mac = new byte[4];

            for (int scan = 0; scan < sha.length; ++scan) {
                mac[scan % 4] ^= sha[scan];
            }

            return mac;
        } catch (NoSuchAlgorithmException var9) {
            throw new IllegalStateException(var9.getMessage());
        }
    }

    private static byte[] stringToByteArray(String aString) {
        try {
            return aString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException var3) {
            var3.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println(encrypt("auth_token", "0907486886", "pin", "111111"));
    }
}
