package com.mservice.security.otpas;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
public class Base32 {
	private static String CHARSET = "UTF-8";
    private static final int SECRET_SIZE = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base32 INSTANCE =
            new Base32("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"); // RFC 4648/3548
    static Base32 getInstance() {
        return INSTANCE;
    }
    private String ALPHABET;
    private char[] DIGITS;
    private int MASK;
    private int SHIFT;
    private HashMap<Character, Integer> CHAR_MAP;
    static final String SEPARATOR = "-";
    protected Base32(String alphabet) {
        this.ALPHABET = alphabet;
        DIGITS = ALPHABET.toCharArray();
        MASK = DIGITS.length - 1;
        SHIFT = Integer.numberOfTrailingZeros(DIGITS.length);
        CHAR_MAP = new HashMap<Character, Integer>();
        for (int i = 0; i < DIGITS.length; i++) {
            CHAR_MAP.put(DIGITS[i], i);
        }
    }

    public static byte[] decode(String encoded) throws DecodingException {
        return getInstance().decodeInternal(encoded);
    }

    protected byte[] decodeInternal(String encoded) throws DecodingException {
        encoded = encoded.trim().replaceAll(SEPARATOR, "").replaceAll(" ", "");
        encoded = encoded.replaceFirst("[=]*$", "");
        encoded = encoded.toUpperCase(Locale.US);
        if (encoded.length() == 0) {
            return new byte[0];
        }
        int encodedLength = encoded.length();
        int outLength = encodedLength * SHIFT / 8;
        byte[] result = new byte[outLength];
        int buffer = 0;
        int next = 0;
        int bitsLeft = 0;
        for (char c : encoded.toCharArray()) {
            if (!CHAR_MAP.containsKey(c)) {
                throw new DecodingException("Illegal character: " + c);
            }
            buffer <<= SHIFT;
            buffer |= CHAR_MAP.get(c) & MASK;
            bitsLeft += SHIFT;
            if (bitsLeft >= 8) {
                result[next++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return result;
    }

    public static String encode(byte[] data) {
        return getInstance().encodeInternal(data);
    }
    public static String encode(String input) throws EncodingException
    {
    	byte[] inputBytes = null;
		try {
			inputBytes = input.getBytes(CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new EncodingException(e);
		}
		return encode(inputBytes);
    }
    protected String encodeInternal(byte[] data) {
        if (data.length == 0) {
            return "";
        }
        if (data.length >= (1 << 28)) {
            throw new IllegalArgumentException();
        }

        int outputLength = (data.length * 8 + SHIFT - 1) / SHIFT;
        StringBuilder result = new StringBuilder(outputLength);

        int buffer = data[0];
        int next = 1;
        int bitsLeft = 8;
        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < SHIFT) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= (data[next++] & 0xff);
                    bitsLeft += 8;
                } else {
                    int pad = SHIFT - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            int index = MASK & (buffer >> (bitsLeft - SHIFT));
            bitsLeft -= SHIFT;
            result.append(DIGITS[index]);
        }
        return result.toString();
    }

    public static class DecodingException extends Exception {
        public DecodingException(String message) {
            super(message);
        }
    }

    public static String random() {
        byte[] buffer = new byte[SECRET_SIZE];
        RANDOM.nextBytes(buffer);
        byte[] secretKey = Arrays.copyOf(buffer, SECRET_SIZE);
        return encode(secretKey);
    }
}
