package com.mservice.momo.util.pos.otpas;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


public class TotpServer {
    private static final int DELAY_WINDOW = 1;
    private final String secret;
    private final Clock clock;
    public TotpServer(String secret) {
        this.secret = secret;
        clock = new Clock();
    }
    public TotpServer(String secret, Clock clock) {
        this.secret = secret;
        this.clock = clock;
    }
    public String now() {
        return leftPadding(hash(secret, clock.getCurrentInterval()));
    }
    public String now(int markedLock)
    {

    	long currentInterval = clock.getCurrentInterval();
        //System.out.println("currentInterval: "+ currentInterval);
        if(clock.getMarkedLock() - 1 ==  markedLock || clock.getMarkedLock() + 9 == markedLock)
    	{
    		currentInterval = currentInterval - 1; 
    	}
        //System.out.println("Marked adjust currentInterval: "+ currentInterval);
        return leftPadding(hash(secret, currentInterval));
    }
    public String verify()
    {
    	String str= "";
    	long currentInterval = clock.getCurrentInterval();

        int pastResponse = Math.max(DELAY_WINDOW, 0);

        for (int i = pastResponse; i >= 0; --i) {
            int candidate = generate(this.secret, currentInterval - i);
            str += " - " + leftPadding(candidate);
        }
        return str;

    }
    public boolean verify(String otp) {

        long code = Long.parseLong(otp);
        long currentInterval = clock.getCurrentInterval();

        int pastResponse = Math.max(DELAY_WINDOW, 0);

        for (int i = pastResponse; i >= 0; --i) {
            int candidate = generate(this.secret, currentInterval - i);
            if (candidate == code) {
                return true;
            }
        }
        return false;
    }

    public String generate(String secret) {
    	long currentInterval = clock.getCurrentInterval();
        return leftPadding(hash(secret, currentInterval));
    }
    private int generate(String secret, long interval) {
        return hash(secret, interval);
    }

    private int hash(String secret, long interval) {
        byte[] hash = new byte[0];
        try {
            hash = new Hmac(Hash.SHA1, Base32.decode(secret), interval).digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (Base32.DecodingException e) {
            e.printStackTrace();
        }
        return bytesToInt(hash);
    }

    private int bytesToInt(byte[] hash) {
        int offset = hash[hash.length - 1] & 0xf;
        int binary = ((hash[offset] & 0x7f) << 24) |
                ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) << 8) |
                (hash[offset + 3] & 0xff);

        return binary % Digits.SIX.getValue();
    }
    private String leftPadding(int otp) {
        return String.format("%06d", otp);
    }

}
