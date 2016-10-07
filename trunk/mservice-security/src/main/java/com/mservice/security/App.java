package com.mservice.security;

import java.io.UnsupportedEncodingException;

import com.mservice.security.otpad.CipherException;
import com.mservice.security.otpad.KeyException;
import com.mservice.security.otpad.OneTimePadCipher;
import com.mservice.security.otpad.OneTimePadCipherBase10;
import com.mservice.security.otpas.Base32;
import com.mservice.security.otpas.EncodingException;
import com.mservice.security.otpas.TotpClient;
import com.mservice.security.otpas.TotpServer;
import com.mservice.security.utils.ConverterException;
import com.mservice.security.utils.ConverterUtils;


/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws InterruptedException, KeyException, CipherException, UnsupportedEncodingException, EncodingException, ConverterException
    {
    	//Example Base converter
    	/*String base10 = "9999999";
    	
    	String base30 = ConverterUtils.Base10toBase30(base10);
    	System.out.println( "base30:" + base30 );
    	System.out.println( "base10:" + ConverterUtils.Base30toBase10(base30, 10) );
    	*/
        System.out.println( "Hello World!" );
        String secret = Base32.random(); ///geneate shared secret for client and server
        System.out.println("SECRET:" + secret);
        
        TotpClient totp = new TotpClient(secret);
        String otp = totp.now();
        System.out.println("OTP:" + otp);
        System.out.println("MarkedLock:" + totp.getClock().getMarkedLock());
        
        
        int markedLock = totp.getClock().getMarkedLock();
        String pass = "093323";
        System.out.println("Pass:" + pass);
        String cipher = OneTimePadCipherBase10.encrypt(otp, pass);
        //String cipher = OneTimePadCipher.encryptToString(otp, pass);
        System.out.println("Cipher:" + cipher);
        
        ///Add markedLock to cipher to check on Server'OTP
        String base10 = cipher + markedLock;
        
        System.out.println("Encode Base10:" + base10);
        
        ///Convert to Base30
        String base30 = ConverterUtils.Base10toBase30(base10);
        System.out.println("Encode Base30:" + base30);
        
        ///Decode at server to get code
        String base10_de = ConverterUtils.Base30toBase10(base30, 7);
        System.out.println("Base 10 - de:"+base10_de);
        
        //Get encrypted password and server marked lock
        String cipher_server = base10_de.substring(0,base10_de.length()-1);
        String lock_server = base10_de.substring(base10_de.length()-1);
        
        TotpServer totpServer = new TotpServer(secret);
        
        Thread.sleep(20000);
        
        //Server generate OTP based on marked lock
        String gen_opt = totpServer.now(Integer.valueOf(lock_server));
        System.out.println("Server OTP:"+gen_opt );
        
        //Server decrypt password using generated OTP
        String decrypt = OneTimePadCipherBase10.decrypt(gen_opt, cipher_server);
        //String decrypt = OneTimePadCipher.decryptToString(gen_opt, cipher_server);
        System.out.println("Decrypt PASS:"+decrypt );
        
        
        //boolean flag = totpServer.verify(otp);
        //System.out.println("RESULT:" + flag);
        //boolean flag1 = totpServer.verify(otp);
        //System.out.println("RESULT:" + flag1);
        
        /*for(int i = 0; i < 40*2*5*1000; i+= 1000)
        {
        	Thread.sleep(1000);
        	
        	String gen_opt = totpServer.now(totp.getClock().getMarkedLock());
        	//String gen_opt = totpServer.verify();//now(totp.getClock().getMarkedLock());
            System.out.println("gen_opt " + i+":"+ gen_opt);
        }
        
        String gen_opt = totpServer.now();
        System.out.println("gen_opt:" + gen_opt);
        */        
        //String auto_pass = OneTimePadCipher.encryptToString(gen_opt,cipher);
        //System.out.println("Enrypt Pass:" + pass);
        //System.out.println("Decrypt Pass:" + auto_pass);
        
        
    }
}
