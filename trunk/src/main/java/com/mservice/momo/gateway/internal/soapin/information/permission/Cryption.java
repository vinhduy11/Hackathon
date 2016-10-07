package com.mservice.momo.gateway.internal.soapin.information.permission;

import org.apache.commons.codec.binary.Base64;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class Cryption {

    public static String publicKey_tmp = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCFLBM/aAkFnTVWV3bD4BCbS2t6uDfsSFgkQgwD" +
            "p5VS5ro3PI1zIG2+BbAm/qvFM0YgwwjwE2ky0CoQSA60Bbm3BiglTBqni5AGU2x/8pwaAjjuEDiJ" +
            "MDsRucg8tZ+Mlu11Q2g7W2AY1t2lWpNZSyBQh0rlvOJxQNFQYGbPioZnYwIDAQAB";

    public static String privateKey_tmp = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIUsEz9oCQWdNVZXdsPgEJtLa3q4" +
            "N+xIWCRCDAOnlVLmujc8jXMgbb4FsCb+q8UzRiDDCPATaTLQKhBIDrQFubcGKCVMGqeLkAZTbH/y" +
            "nBoCOO4QOIkwOxG5yDy1n4yW7XVDaDtbYBjW3aVak1lLIFCHSuW84nFA0VBgZs+KhmdjAgMBAAEC" +
            "gYBa1ctzLmUo/ShKcnJB/O2W34S6Ojl644WrhZloTMCfwV03HAhnhLEWMg8LKh7D7cthwWaxSqww" +
            "yqhGXsAYrucTIh6L5B1W0FnHWBxzgdXifkUMBxeIILSb5oJ5SqmwhLEVqub8+SoqPGl6/U3V0kGZ" +
            "Mie487+/j7uDCYGSHs1PUQJBALzh19BkdacTSx90sRRxvqFYWmUITpdW7pyjotyEA8+uXzbavJgi" +
            "oLK6rmiS+Wgi2BhpO1KSP+CLq84GubsfxqcCQQC0fmzlkg2qnsASSLf7Wk+3voMVQ1CqwS1esJ9o" +
            "dJ0kAvm2fIWH52A2ExfklaSqSVqWHcp3LuDTX/V5b/NbAyzlAkEAuUkObRXvOAs8GUpeX4DJEPtc" +
            "CVohxmH3xl7bZ1h1aKhULLpcYR0u6MUqS9lJofb0ZtXr5K8kzEAXNA7y6XZdZwJAYZOvf3S0Grd0" +
            "Eu5pOGOckTNXXllj9Mw2oOhPZYMVqPBK25L6BXzakKFF23fi64R4iotUd5ZKSTupU5toLp3K2QJA" +
            "cPXgEpiP02xlMy3/kJ1oKGMnPJbWVgmxIt4oy5Is6V9xskoNDG6Bw2aYO0sDCIoMaUu16GFKtBGr" +
            "5yrASS7bwA==";

    static final String KEY_GOTIT = "EVBjZweGARcDAVbp1k0vKvMe1M5Z27k7";
    static final String HMAC_SHA256 = "HmacSHA256";
    static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    static final String CRYPTION = "AES/CBC/PKCS5PADDING";
    public static Map<String, String> RSAKey = new HashMap<String, String>();
    private static KeyPair keyPair;
    JsonObject fi_cfg;
    Logger logger;

    public Cryption(JsonObject fi_cfg, Logger logger) {
        this.fi_cfg = fi_cfg;
        this.logger = logger;
    }

    public Cryption()
    {

    }

    static public String Hash(String input) throws NoSuchAlgorithmException {
        String result = "";
        MessageDigest sha = MessageDigest.getInstance("SHA");
        sha.update(input.getBytes());
        BigInteger dis = new BigInteger(1, sha.digest());
        result = dis.toString(16);
        return result.toUpperCase();
    }

    public static String convertPublicKeyToString(PublicKey pk) {
        String plk = "";
        BASE64Encoder base64Encoder = new BASE64Encoder();
        plk = base64Encoder.encode(pk.getEncoded());
        return plk;
    }

    public static String convertPrivateKeyToString(PrivateKey prk) {
        String prkey = "";
        BASE64Encoder base64Encoder = new BASE64Encoder();
        prkey = base64Encoder.encode(prk.getEncoded());
        return prkey;
    }

    public static PublicKey convertStringToPublicKey(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        PublicKey publicKey1 = null;

        BASE64Decoder decoder = new BASE64Decoder();
        BASE64Encoder encoder = new BASE64Encoder();
        byte[] publicKeyBytes = decoder.decodeBuffer(publicKey);
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        publicKey1 = keyFactory.generatePublic(publicKeySpec);

        return publicKey1;
    }

    public static PrivateKey convertStringToPrivateKey(String privateKeyS) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        PrivateKey privateKey = null;

        BASE64Decoder decoder = new BASE64Decoder();
        BASE64Encoder encoder = new BASE64Encoder();
        byte[] privateKeyBytes = decoder.decodeBuffer(privateKeyS);

        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        privateKey = keyFactory.generatePrivate(privateKeySpec);
        privateKey = keyFactory.generatePrivate(privateKeySpec);

        return privateKey;
    }

    public String initKeyPair(int keySize) {
        JsonObject jo = new JsonObject();
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(keySize);
            keyPair = keyGen.genKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            jo.putString("publicKey", convertPublicKeyToString(publicKey));
            jo.putString("privateKey", convertPrivateKeyToString(privateKey));

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Algorithm not supported! " + e.getMessage() + "!");
        }
        return jo.toString();
    }

    public String decrypt(String enc, String prk) throws InvalidKeyException,
            IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        PrivateKey prvk;
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] privateKeyBytes = decoder.decodeBuffer(prk);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        prvk = keyFactory.generatePrivate(privateKeySpec);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, prvk);
        return new String(cipher.doFinal(decoder.decodeBuffer(enc)));
    }

    public String decryptIOS(String enc, String prk) throws InvalidKeyException,
            IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        PrivateKey prvk;
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] privateKeyBytes = decoder.decodeBuffer(prk);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        prvk = keyFactory.generatePrivate(privateKeySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, prvk);
        return new String(cipher.doFinal(decoder.decodeBuffer(enc)));
    }

    public String encrypt(byte[] inpBytes, String plk) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        PublicKey pubk;
        BASE64Decoder decoder = new BASE64Decoder();
        BASE64Encoder encoder = new BASE64Encoder();
        byte[] publicKeyBytes = decoder.decodeBuffer(plk);
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        pubk = keyFactory.generatePublic(publicKeySpec);
        Cipher cipher = Cipher.getInstance("RSA");
        System.out.println("\n" + cipher.getProvider().getInfo());
        cipher.init(Cipher.ENCRYPT_MODE, pubk);
        return new String(encoder.encode(cipher.doFinal(inpBytes)).replace("\r", ""));
    }

    public String getPublicKey(String username) throws FileNotFoundException, IOException {
        String filePath;
        if (username.isEmpty())
            filePath = fi_cfg.getString("fi.pub");
        else
            filePath = fi_cfg.getString("fi.pub") + "." + username;
        String keyContent = RSAKey.get(filePath);
        StringBuffer strContent = new StringBuffer("");
        if (keyContent == null) {

            try {
                File file = new File(filePath);
                FileInputStream fin = new FileInputStream(file);
                int ch;
                while ((ch = fin.read()) != -1)
                    strContent.append((char) ch);
                fin.close();
                keyContent = strContent.toString();
                RSAKey.put(filePath, keyContent);
                logger.info("Put public key to map ");
            } catch (FileNotFoundException e) {
                logger.info("READ PUBLIC KEY FILE - FILE NOT FOUND: " + filePath);
                logger.info("FileNotFoundException" + e.getMessage());
            } catch (IOException e) {
                logger.info("READ PUBLIC KEY FILE - IO EXCEPTION");
                logger.info("IOException" + e.getMessage());
            }
        } else
            logger.info("Load public key from map");
        return keyContent;
    }

    public String getPrivateKey(String username) {
        String filePath;
        if (username.isEmpty())
            filePath = fi_cfg.getString("fi.pri");
        else
            filePath = fi_cfg.getString("fi.pri") + "." + username;
        StringBuffer strContent = new StringBuffer("");
        String keyContent = RSAKey.get(filePath);
        if (keyContent == null) {
            try {
                logger.info("Load private key for system");
                File file = new File(filePath);
                FileInputStream fin = new FileInputStream(file);
                int ch;
                while ((ch = fin.read()) != -1)
                    strContent.append((char) ch);
                fin.close();
                keyContent = strContent.toString();
                RSAKey.put(filePath, keyContent);
                logger.info("Put private key to map");
            } catch (FileNotFoundException e) {
                logger.info("READ PRIVATE KEY FILE - FILE NOT FOUND: " + filePath);
                logger.error("FileNotFoundException, " + e.getMessage());
            } catch (IOException e) {
                logger.info("READ PRIVATE KEY FILE - IO EXCEPTION");
                logger.error("IOException, " + e.getMessage());
            }
        } else
            logger.info("Load private key from map");
        return keyContent;
    }

    public byte[] signData(byte[] toBeSigned, String prk) {
        PrivateKey prvk;
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] privateKeyBytes = decoder.decodeBuffer(prk);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            prvk = keyFactory.generatePrivate(privateKeySpec);
            Signature rsa = Signature.getInstance("SHA1withRSA");
            rsa.initSign(prvk);
            rsa.update(toBeSigned);
            return rsa.sign();
        } catch (NoSuchAlgorithmException e) {
            logger.info("SIGN DATA - NoSuchAlgorithmException");
            logger.error("NoSuchAlgorithmException, " + e.getMessage());
        } catch (InvalidKeyException e) {
            logger.info("SIGN DATA - InvalidKeyException");
            logger.error("InvalidKeyException, " + e.getMessage());
        } catch (SignatureException e) {
            logger.info("SIGN DATA - SignatureException");
            logger.error("SignatureException, " + e.getMessage());
        } catch (IOException e) {
            logger.info("SIGN DATA - IOException");
            logger.error("IOException, " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            logger.info("SIGN DATA - InvalidKeySpecException");
            logger.error("InvalidKeySpecException, " + e.getMessage());
        }
        return null;
    }

    public String signDataBase64(String toBeSigned, String prk) {
        PrivateKey prvk;
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            BASE64Encoder encoder = new BASE64Encoder();
            byte[] privateKeyBytes = decoder.decodeBuffer(prk);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            prvk = keyFactory.generatePrivate(privateKeySpec);
            Signature rsa = Signature.getInstance("SHA1withRSA");
            rsa.initSign(prvk);
            rsa.update(toBeSigned.getBytes());
            return encoder.encode(rsa.sign()).replace("\r", "");
        } catch (NoSuchAlgorithmException e) {
            logger.info("SIGN DATA - NoSuchAlgorithmException");
            logger.error("NoSuchAlgorithmException, " + e.getMessage());
        } catch (InvalidKeyException e) {
            logger.info("SIGN DATA - InvalidKeyException");
            logger.error("InvalidKeyException, " + e.getMessage());
        } catch (SignatureException e) {
            logger.info("SIGN DATA - SignatureException");
            logger.error("SignatureException, " + e.getMessage());
        } catch (IOException e) {
            logger.info("SIGN DATA - IOException");
            logger.error("IOException, " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            logger.info("SIGN DATA - InvalidKeySpecException");
            logger.error("InvalidKeySpecException, " + e.getMessage());
        }
        return null;
    }

    public boolean verifySignature(byte[] signature, byte[] toBeSign, String plk) {
        try {
            PublicKey pubk;
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] publicKeyBytes = decoder.decodeBuffer(plk);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubk = keyFactory.generatePublic(publicKeySpec);
            Signature sign = Signature.getInstance("SHA1withRSA");
            sign.initVerify(pubk);
            sign.update(toBeSign);
            return sign.verify(signature);
        } catch (NoSuchAlgorithmException e) {
            logger.info("VERIFY SIGNATURE DATA - NoSuchAlgorithmException");
            logger.error("NoSuchAlgorithmException, " + e.getMessage());
        } catch (InvalidKeyException e) {
            logger.info("VERIFY SIGNATURE DATA - InvalidKeyException");
            logger.error("InvalidKeyException," + e.getMessage());
        } catch (SignatureException e) {
            logger.info("VERIFY SIGNATURE DATA - SignatureException");
            logger.error("SignatureException, " + e.getMessage());
        } catch (IOException e) {
            logger.info("VERIFY SIGNATURE DATA - IOException");
            logger.error("IOException");
        } catch (InvalidKeySpecException e) {
            logger.info("VERIFY SIGNATURE DATA - InvalidKeySpecException");
            logger.error("InvalidKeySpecException, " + e.getMessage());
        }
        return false;
    }

    public boolean verifySignatureBase64(String signature, String toBeSign, String plk) {
        try {
            PublicKey pubk;
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] publicKeyBytes = decoder.decodeBuffer(plk);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubk = keyFactory.generatePublic(publicKeySpec);
            Signature sign = Signature.getInstance("SHA1withRSA");
            byte[] ssignature = decoder.decodeBuffer(signature);
            sign.initVerify(pubk);
            sign.update(toBeSign.getBytes());
            return sign.verify(ssignature);
        } catch (NoSuchAlgorithmException e) {
            logger.info("VERIFY DATA - NoSuchAlgorithmException");
            logger.error("NoSuchAlgorithmException, " + e.getMessage());
        } catch (InvalidKeyException e) {
            logger.info("VERIFY DATA - InvalidKeyException");
            logger.error("InvalidKeyException, " + e.getMessage());
        } catch (SignatureException e) {
            logger.info("VERIFY DATA - SignatureException");
            logger.error("SignatureException, " + e.getMessage());
        } catch (IOException e) {
            logger.info("VERIFY DATA - IOException");
            logger.error("IOException, " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            logger.info("VERIFY DATA - InvalidKeySpecException");
            logger.error("InvalidKeySpecException, " + e.getMessage());
        }
        return false;
    }

    public String getPublicKeyByPath(String filePath) throws FileNotFoundException, IOException {

        String keyContent = RSAKey.get(filePath);
        StringBuffer strContent = new StringBuffer("");
        if (keyContent == null) {
            logger.info("Load public key of system ");
            try {
                File file = new File(filePath);
                FileInputStream fin = new FileInputStream(file);
                int ch;
                while ((ch = fin.read()) != -1)
                    strContent.append((char) ch);
                fin.close();
                keyContent = strContent.toString();
                RSAKey.put(filePath, keyContent);
                logger.info("Put public key to map ");
            } catch (FileNotFoundException e) {
                logger.info("READ PUBLIC KEY FILE - FILE NOT FOUND: " + filePath);
                logger.error("FileNotFoundException, " + e.getMessage());
            } catch (IOException e) {
                logger.info("READ PUBLIC KEY FILE - IO EXCEPTION");
                logger.error("IOException, " + e.getMessage());
            }
        } else
            logger.info("Load public key from map");
        return keyContent;
    }

    public String getPrivateKeyByPath(String filePath) {

        StringBuffer strContent = new StringBuffer("");
        String keyContent = RSAKey.get(filePath);
        if (keyContent == null) {
            try {
                logger.info("Load private key for system");
                File file = new File(filePath);
                FileInputStream fin = new FileInputStream(file);
                int ch;
                while ((ch = fin.read()) != -1)
                    strContent.append((char) ch);
                fin.close();
                keyContent = strContent.toString();
                RSAKey.put(filePath, keyContent);
                logger.info("Put private key to map");
            } catch (FileNotFoundException e) {
                logger.info("READ PRIVATE KEY FILE - FILE NOT FOUND: " + filePath);
                logger.error("FileNotFoundException, " + e.getMessage());
            } catch (IOException e) {
                logger.info("READ PRIVATE KEY FILE - IO EXCEPTION");
                logger.error("IOException, " + e.getMessage());
            }
        } else
            logger.info("Load private key from map");
        return keyContent;
    }

    public boolean resultVerifyData(String en, String publicKey, String privateKey) throws Exception {
        boolean result = false;

//        PublicKey publicKey1 = convertStringToPublicKey(publicKey);
//        PrivateKey privateKey1 = convertStringToPrivateKey(privateKey);
        if (!publicKey.equalsIgnoreCase("") && !privateKey.equalsIgnoreCase("")) {
            byte[] digitalSignature = signData(en.getBytes(), privateKey);
            boolean verified;

            verified = verifySignature(digitalSignature, en.getBytes(), publicKey);
            result = verified;
        } else {
            result = false;
        }

        return result;

    }

    public String decryptSessionKeyData(final String encrypted,String keyString) throws IOException, InvalidAlgorithmParameterException {
        //final SecretKeySpec skeySpec=new SecretKeySpec(new BigInteger(keyString,24).toByteArray(),"AES");
        final SecretKeySpec skeySpec=new SecretKeySpec(keyString.getBytes(),"AES");
        byte[] iv = new byte[16];
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        try {
            //final Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");\
            final Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivspec);
            //byte[] decordedValue = DatatypeConverter.parseBase64Binary(encrypted);
            BASE64Decoder base64Decoder = new BASE64Decoder();
            byte[] decodedValue = base64Decoder.decodeBuffer(encrypted);

            return new String(cipher.doFinal(decodedValue));
        }
        catch (  final InvalidKeyException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
        catch (  final IllegalBlockSizeException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
        catch (  final BadPaddingException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
        catch (  final NoSuchAlgorithmException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
        catch (  final NoSuchPaddingException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
    }

    public String encrypSessionKeyData(final String encryptData,String keyString) throws IOException, InvalidAlgorithmParameterException {
        //final SecretKeySpec skeySpec=new SecretKeySpec(new BigInteger(keyString,24).toByteArray(),"AES");
        final SecretKeySpec skeySpec=new SecretKeySpec(keyString.getBytes(),"AES");
        byte[] iv = new byte[16];
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        try {
            //final Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");\
            final Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivspec);
            //byte[] decordedValue = DatatypeConverter.parseBase64Binary(encrypted);
            byte[] results = cipher.doFinal(encryptData.getBytes("UTF-8")); // Finish
            BASE64Encoder encoder = new BASE64Encoder();
            return new String (encoder.encode(results));
        }
        catch (  final InvalidKeyException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
        catch (  final IllegalBlockSizeException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
        catch (  final BadPaddingException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
        catch (  final NoSuchAlgorithmException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
        catch (  final NoSuchPaddingException e) {
//            throw new UnsupportedOperationException(e);
            return "";
        }
    }



    public static String decryptIPOS(String key, String input)
            throws UnsupportedEncodingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] keyBytes = digest.digest(key.getBytes("UTF-8"));
        SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] encrypted = cipher.doFinal(decoder.decodeBuffer(input));

        return new String(encrypted, "UTF-8");
    }

    public String encryptGotIt(String value) throws Exception {

        init();
        byte[] iv = randomIv();
        String valueSerialize = PhpSerialize.serialize(value);
        String valueEncrypt = encryptGt(KEY_GOTIT, iv, valueSerialize);
        String ivBase64 = Base64.encodeBase64String(iv);
        String mac = hash(valueEncrypt, ivBase64);
        JsonObject object = new JsonObject();
        object.putString("iv", ivBase64);
        object.putString("value", valueEncrypt);
        object.putString("mac", mac);
        String encrypt = Base64.encodeBase64String(object.encode().getBytes());
        return encrypt;
    }
    private String encryptGt(String key, byte[] initVector, String value) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(initVector);
        SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

        Cipher cipher = Cipher.getInstance(CRYPTION);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

        byte[] encrypted = cipher.doFinal(value.getBytes());
        return Base64.encodeBase64String(encrypted);
    }
    private void init() throws Exception {
        Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
        field.setAccessible(true);
        field.set(null, java.lang.Boolean.FALSE);
    }

    public String hash(String value, String iv) throws Exception {
        String ivValue = iv + value;
        String hashSha256 = hmacSha256(ivValue, KEY_GOTIT);
        return hashSha256;
    }
    public String hmacSha256(String value, String key) throws Exception {
        try {
            Mac sha256_HMAC = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), HMAC_SHA256);
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(value.getBytes());
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public byte[] randomIv() {
        byte[] randomByteArray = new byte[getIvSize()];
        new Random().nextBytes(randomByteArray);
        return randomByteArray;
    }

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    int getIvSize() {
        return 16;
    }

}
