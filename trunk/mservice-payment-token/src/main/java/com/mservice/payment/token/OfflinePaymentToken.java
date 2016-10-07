package com.mservice.payment.token;

import com.mservice.payment.exception.GiftNotExistException;
import com.mservice.payment.exception.MoneySourceNotExistException;
import com.mservice.payment.exception.NotExistPrefixMapCodeException;
import com.mservice.payment.exception.OTPIncorrectFormatException;
import com.mservice.payment.exception.PaymentTokenIncorrectFormatException;
import com.mservice.payment.exception.PhoneNumberIncorrectFormatException;
import com.mservice.security.otpad.CipherException;
import com.mservice.security.otpad.OneTimePadCipherBase10;
import com.mservice.security.otpas.TotpClient;
import com.mservice.security.otpas.TotpServer;
import com.mservice.security.utils.ConverterException;
import com.mservice.security.utils.ConverterUtils;

public class OfflinePaymentToken {
	/**
	 * 
	 * @param optClient is OTP generator.
	 * @param phone is phone number.
	 * @param pass is user's password
	 * @param momoSource is money source that is used to payment.
	 * @param gift is gift information that is is used to payment.
	 * @return payment token
	 * @throws PhoneNumberIncorrectFormatException
	 * @throws OTPIncorrectFormatException
	 * @throws MoneySourceNotExistException
	 * @throws GiftNotExistException
	 * @throws ConverterException
	 * @throws CipherException
	 */
	public static String encodeBase30(TotpClient optClient,String phone, String pass, int momoSource, int gift) throws PhoneNumberIncorrectFormatException, OTPIncorrectFormatException, MoneySourceNotExistException, GiftNotExistException, ConverterException, CipherException
	{
		if(phone.length() <10 || phone.length() > 11)
		{
			throw new PhoneNumberIncorrectFormatException("Phone number is incorrect in fortmat!!!");
		}
		if(pass.length() != 6)
		{
			throw new OTPIncorrectFormatException("PASS is incorrect in fortmat!!!");
		}
		if(momoSource <0 || momoSource>9)
		{
			throw new MoneySourceNotExistException("Money source is incorrect!!!");
		}
		if(gift <0 || gift>9)
		{
			throw new GiftNotExistException("Money source is incorrect!!!");
		}
		String otp = optClient.now();  
		System.out.println("Client OTP:" + otp);
        int markedLock = optClient.getClock().getMarkedLock();
        System.out.println("Marked Lock:" + markedLock);
        System.out.println("Pass:" + pass);
        String cipher = OneTimePadCipherBase10.encrypt(otp, pass);
        String base10 = cipher + markedLock;
        System.out.println("Pass Base10:" + base10);
        String base30 = ConverterUtils.Base10toBase30(base10,5);
		String tmp = String.format("%s%s",momoSource,gift);
		tmp = ConverterUtils.Base10toBase30(tmp, 2); 
		return String.format("%s%s%s",phone,base30,tmp);
	}
	
	public static String encodeBase30(String phone, String OTPBase30, Integer momoSource, Integer gift) throws PhoneNumberIncorrectFormatException, OTPIncorrectFormatException, MoneySourceNotExistException, GiftNotExistException, NotExistPrefixMapCodeException, ConverterException
	{
		if(phone.length() <10 || phone.length() > 11)
		{
			throw new PhoneNumberIncorrectFormatException("Phone number is incorrect in fortmat!!!");
		}
		if(OTPBase30.length() != 5)
		{
			throw new OTPIncorrectFormatException("OTP is incorrect in fortmat!!!");
		}
		if(momoSource <0 || momoSource>9)
		{
			throw new MoneySourceNotExistException("Money source is not found!!!");
		}
		if(gift <0 || gift>9)
		{
			throw new GiftNotExistException("Money source is not found!!!");
		}
		String tmp = String.format("%s%s",momoSource,gift);
		tmp = ConverterUtils.Base10toBase30(tmp, 2);
		return String.format("%s%s%s",phone,OTPBase30,tmp);
	}
	/**
	 * 
	 * @param server is OPT generator
	 * @param token is payment token which is encrypted by encodeBase30 function.
	 * @return String Array (A[]) include 4 elements. A[0] is phone number, A[1] is pass, A[2] is money source, 
	 * A[3] is gift
	 * @throws PaymentTokenIncorrectFormatException
	 * @throws ConverterException
	 * @throws CipherException
	 */
	public static String[] decodeBase30(TotpServer server, String token)throws PaymentTokenIncorrectFormatException, ConverterException, CipherException
	{
		if(token.length() >18 || token.length() < 17)
		{
			throw new PaymentTokenIncorrectFormatException("Token is incorrect format!!!");
		}
		String[] rs = new String[4];
		rs[0] = token.substring(0, token.length() - 7);
		rs[1] = token.substring(token.length() - 7, token.length() - 2);
		
		String base10_de = ConverterUtils.Base30toBase10(rs[1], 7);
        String cipher_server = base10_de.substring(0,base10_de.length()-1);
        String lock_server = base10_de.substring(base10_de.length()-1);
		
		String base30 = token.substring(token.length() - 2, token.length());
		String base10 = ConverterUtils.Base30toBase10(base30, 2);
		
		String gen_opt = server.now(Integer.valueOf(lock_server));
		
        System.out.println("Server OTP:"+gen_opt );
        System.out.println("Marked Lock:" + server.getClock().getMarkedLock());
        
        String decrypt = OneTimePadCipherBase10.decrypt(gen_opt, cipher_server);
        rs[1] = decrypt;
		rs[2] = ""+base10.charAt(0);
		rs[3] = ""+base10.charAt(1);
		return rs;
	}
	public static String[] decodeBase30(String token) throws PaymentTokenIncorrectFormatException, ConverterException
	{
		if(token.length() >18 || token.length() < 17)
		{
			throw new PaymentTokenIncorrectFormatException("Token is incorrect format!!!");
		}
		String[] rs = new String[4];
		rs[0] = token.substring(0, token.length() - 7);
		rs[1] = token.substring(token.length() - 7, token.length() - 2);
		String base30 = token.substring(token.length() - 2, token.length());
		String base10 = ConverterUtils.Base30toBase10(base30, 2);
		rs[2] = ""+base10.charAt(0);
		rs[3] = ""+base10.charAt(1);
		return rs;
	}
	public static String format(String token, int numchar)
	{
		String rs = "";
		for(int i = 0;i < token.length();i++)
		{
			if(i%numchar == 0)
			{
				rs = rs + " " + token.charAt(i); 
			}else{
				rs = rs + token.charAt(i);
			}
		}
		return rs;
	}
	
}
