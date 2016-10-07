package com.mservice.momo.vertx.data;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.bouncycastle.util.encoders.Encoder;
import org.vertx.java.core.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by User on 4/10/14.
 */
public class RSACipher {
    public static RSAKeyParameters publicKey;
    public static RSAPrivateCrtKeyParameters privateKey;
    public static Logger logger;

    private static final Encoder encoder = new Base64Encoder();
    private static byte[] encode(byte[]data)
    {
        int i = (data.length + 2) / 3 << 2;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(i);
        try
        {
            encoder.encode(data, 0, data.length, bOut);
        }
        catch (IOException e)
        {
            throw new RuntimeException("exception: " + e);
        }
        return bOut.toByteArray();
    }

    public static String encrypt (byte [] toEncrypt) throws Exception
    {
        AsymmetricBlockCipher theEngine = new RSAEngine();
        theEngine = new PKCS1Encoding(theEngine);
        theEngine.init(true, publicKey);
        byte[] b = theEngine.processBlock(toEncrypt, 0, toEncrypt.length);
        return new String(encode(b));
    }

    public static String decrypt (byte [] toDecrypt){
        try{
            AsymmetricBlockCipher theEngine = new RSAEngine();
            theEngine = new PKCS1Encoding(theEngine);
            theEngine.init(false, privateKey);
            return new String(theEngine.processBlock(toDecrypt, 0, toDecrypt.length));
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

}
