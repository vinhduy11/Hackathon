package com.mservice.momo.vertx.data;

import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;

/**
 * Created by User on 4/10/14.
 */
public class DESCipher {
    public static PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESedeEngine()));
    public static byte[] keyByteArray = null;

    private static String a = "922bf287aab6fa61d14bec20c14ed4f8f003c9e20d6fb9f4d6ee8c4b9ba320bf6c016d369887df4ca864476c0fc8f6e1b34c9b5d406702df522dc2e1b5951db01979df29aa7878d21406e09117f349623813ce2736e52669a809372c6ddc406e23d92e89cc359323213249dbc4c4922dc7704786e3b2b0";
    private static String b = "4d6ee8a2";
    public static String getDESKey() {
        return (b + a).substring(0, 32);
    }

    public static String encript(String inputText) throws Exception {

        cipher.reset();
        cipher.init(true, new KeyParameter(keyByteArray));
        byte[] inpBytes = inputText.getBytes();
        byte[] cipherText = new byte[cipher.getOutputSize(inpBytes.length)];
        int outputLength = cipher.processBytes(inpBytes, 0, inpBytes.length, cipherText, 0);
        cipher.doFinal(cipherText, outputLength);
        inpBytes = Hex.encode(cipherText, 0, cipherText.length);
        return new String(inpBytes);

    }

    public static String decript(String inputText) throws Exception {
        cipher.reset();
        cipher.init(false, new KeyParameter(keyByteArray));
        byte[] inpBytes = Hex.decode(inputText.getBytes());
        byte[] cipherText = new byte[cipher.getOutputSize(inpBytes.length)];
        int outputLength = cipher.processBytes(inpBytes, 0, inpBytes.length, cipherText, 0);
        int finalLength = cipher.doFinal(cipherText, outputLength);
        return new String(cipherText, 0, outputLength + finalLength);
    }
}
