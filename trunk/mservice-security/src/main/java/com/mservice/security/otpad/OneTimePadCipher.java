package com.mservice.security.otpad;
import java.io.UnsupportedEncodingException;

public class OneTimePadCipher {
	private static String CHARSET = "UTF-8";
	public static byte[] encrypt(String keyStore, String input) throws KeyException, CipherException {
		if (input == null) {
			throw new CipherException("Cannot encrypt null plain text");
		}
		if (keyStore == null) {
			throw new CipherException("Cannot encrypt with a null key name");
		}
		byte[] inputBytes = null;
		try {
			inputBytes = input.getBytes(CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new CipherException(e);
		}
		byte[] key = null;
		try{
			key = keyStore.getBytes(CHARSET);
		}catch (UnsupportedEncodingException e)
		{
			throw new KeyException(e);
		}
		byte[] encrypted = encrypt(inputBytes, key);
		return encrypted;
	}
	public static String encryptToString(String keyStore, String input) throws KeyException, CipherException, UnsupportedEncodingException {
		byte[] cipher = encrypt(keyStore, input);
		return new String(cipher,CHARSET);
	}
	public static String decrypt(String keyStore, byte[] input) throws KeyException, CipherException {
		byte[] key = null;
		try 
		{
			key = keyStore.getBytes(CHARSET);
		}catch (UnsupportedEncodingException e){
			throw new KeyException(e);
		}
		
		byte[] decrypted = decrypt(input, key); 
		String result = null;
		try {
			result = new String(decrypted, CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new CipherException(e);
		}
		return result;
	}
	public static byte[] encryptString(String input, byte[] key) throws CipherException {
		if (input == null) {
			throw new CipherException("Cannot encrypt null plain text");
		}
		if (key == null) {
			throw new CipherException("Cannot encrypt with a null key");
		}
		byte[] b = null;
		try {
			b = input.getBytes(CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new CipherException(e);
		}
		byte[] encrypted = encrypt(b, key);
		
		return encrypted;
	}
	public static String decryptToString(String keyStore, String input) throws CipherException, UnsupportedEncodingException, KeyException  {
		byte []key = null;
		byte []cipher = null;
		try 
		{
			key = keyStore.getBytes(CHARSET);
		}catch (UnsupportedEncodingException e){
			throw new KeyException(e);
		}
		try 
		{
			cipher = input.getBytes(CHARSET);
		}catch (UnsupportedEncodingException e){
			throw new CipherException(e);
		}
		String s = null;
		byte[] b = decrypt(cipher, key);
		try {
			s = new String(b, CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new CipherException(e);
		}
		return s;
		}
	public static String decryptToString(byte[] input, byte[] key) throws CipherException {
		if (input == null) {
			throw new CipherException("Cannot decrypt null cipher text");
		}
		if (key == null) {
			throw new CipherException("Cannot decrypt with a null key");
		}
		String s = null;
		byte[] b = decrypt(input, key);
		try {
			s = new String(b, CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new CipherException(e);
		}
		return s;
	}
	
	public static byte[] encrypt(byte[] plaintext, byte[] key) throws CipherException {
		if (plaintext == null) {
			throw new CipherException("Cannot encrypt null plain text");
		}
		if (key == null) {
			throw new CipherException("Cannot encrypt with a null key");
		}
		if (plaintext.length != key.length) {
			throw new CipherException("Cannot encrypt, input byte length [" + plaintext.length + "] is not the same as the key length [" + key.length + "]");
		}
		byte[] result = new byte[plaintext.length];
		for (int i = 0; i < key.length; i++) {
			result[i] = (byte) (plaintext[i] ^ key[i]);
		}
		return result;
	}

	public static byte[] decrypt(byte[] ciphertext, byte[] key) throws CipherException {
		if (ciphertext == null) {
			throw new CipherException("Cannot decrypt null cipher text");
		}
		if (key == null) {
			throw new CipherException("Cannot decrypt with a null key");
		}
		if (ciphertext.length != key.length) {
			throw new CipherException("Cannot decrypt, input byte length [" + ciphertext.length + "] is not the same as the key length [" + key.length + "]");
		}
		byte[] result = new byte[ciphertext.length];
		for (int i = 0; i < key.length; i++) {
			result[i] = (byte) (ciphertext[i] ^ key[i]);
		}
		return result;
	}
}
