package com.mservice.payment;

import com.mservice.payment.exception.GiftNotExistException;
import com.mservice.payment.exception.MoneySourceNotExistException;
import com.mservice.payment.exception.NotExistPrefixMapCodeException;
import com.mservice.payment.exception.OTPIncorrectFormatException;
import com.mservice.payment.exception.PaymentTokenIncorrectFormatException;
import com.mservice.payment.exception.PhoneNumberIncorrectFormatException;
import com.mservice.payment.token.OfflinePaymentToken;
import com.mservice.security.otpad.CipherException;
import com.mservice.security.otpad.OneTimePadCipherBase10;
import com.mservice.security.otpas.Base32;
import com.mservice.security.otpas.TotpClient;
import com.mservice.security.otpas.TotpServer;
import com.mservice.security.utils.ConverterException;
import com.mservice.security.utils.ConverterUtils;

public class App {

	public static void main(String[] args) throws PaymentTokenIncorrectFormatException, CipherException, ConverterException, InterruptedException, PhoneNumberIncorrectFormatException, OTPIncorrectFormatException, MoneySourceNotExistException, GiftNotExistException, NotExistPrefixMapCodeException {
		// TODO Auto-generated method stub
		//Payment data
		String phone = "0933242572";
		String pass = "000000";
		int momoSource = 9; // From 1 --> 9
		int gift = 9; // From 1 --> 9
		
		//Create secret key
		String secret = Base32.random(); ///geneate shared secret for client and server
        System.out.println("SECRET:" + secret);
        
        
        //Payment token is encoded by client
        TotpClient totp = new TotpClient(secret);  
        String token = OfflinePaymentToken.encodeBase30(totp,phone,pass,momoSource, gift);
        System.out.println("Token Base30:" + OfflinePaymentToken.format(token.toUpperCase(),4));
        
        TotpServer totpServer = new TotpServer(secret);
      
        Thread.sleep(20000);
        
        //Payment data is decoded by server
        String[] de = OfflinePaymentToken.decodeBase30(totpServer,token);
		System.out.println("Phone Number: " + de[0]);
		System.out.println("OTP+PASS: " + de[1]);
		System.out.println("Money Source: " + de[2]);
		System.out.println("GIFT: " + de[3]);

	}

}
