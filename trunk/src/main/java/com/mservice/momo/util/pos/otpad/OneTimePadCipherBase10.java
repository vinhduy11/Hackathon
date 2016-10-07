package com.mservice.momo.util.pos.otpad;

import java.util.HashMap;
import java.util.Map;

public class OneTimePadCipherBase10 {
	protected static final int BASE = 10; 
	protected static final char chars[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
	protected Map<Character, Integer> keyVals;
	protected Map<Integer, Character> valKeys;
	private static final OneTimePadCipherBase10 INSTANCE = new OneTimePadCipherBase10();
	static OneTimePadCipherBase10 getInstance() {
        return INSTANCE;
    }
	private OneTimePadCipherBase10()
	{
		keyVals = new HashMap<Character, Integer>();
		valKeys = new HashMap<Integer, Character>();
		for(int i = 0;i < chars.length;i++)
		{
			keyVals.put(chars[i], i);
			valKeys.put(i, chars[i]);
		}
	}
	public static String encrypt(String key, String input) throws CipherException
	{
		return getInstance().internalEncrypt(key,input);
	}
	private String internalEncrypt(String key, String input) throws CipherException{
		if (input == null) {
			throw new CipherException("Cannot encrypt null plain text");
		}
		if (key == null) {
			throw new CipherException("Cannot encrypt with a null key");
		}
		if (input.length() != key.length()) {
			throw new CipherException("Cannot encrypt, input byte length [" + input.length() + "] is not the same as the key length [" + key.length() + "]");
		}
		String cipher = "";
		for(int i = 0;i<input.length();i++)
		{
			int msg = keyVals.get(input.charAt(i));
			int k = keyVals.get(key.charAt(i));
			int val = mod(msg + k, BASE);
			char en =  valKeys.get(val);
			cipher += en;
		}
		return cipher;
    }
	public static String decrypt(String key, String input) throws CipherException
	{
		return getInstance().internalDecrypt(key, input);
	}
	private String internalDecrypt(String key, String input) throws CipherException{
		if (input == null) {
			throw new CipherException("Cannot encrypt null plain text");
		}
		if (key == null) {
			throw new CipherException("Cannot encrypt with a null key");
		}
		if (input.length() != key.length()) {
			throw new CipherException("Cannot decrypt, input byte length [" + input.length() + "] is not the same as the key length [" + key.length() + "]");
		}
		String plain = "";
		for(int i = 0;i<input.length();i++)
		{
			int msg = keyVals.get(input.charAt(i));
			int k = keyVals.get(key.charAt(i));
			char de =  valKeys.get(mod(msg - k, BASE));
			plain += de;
		}
		return plain;
    }
	public static int mod(int val, int base)
	{
		int tmp= val;
		while(tmp < 0)
		{
			tmp = tmp+base;
		}
		return tmp%base;
	}
}
