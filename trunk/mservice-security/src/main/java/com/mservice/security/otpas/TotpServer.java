package com.mservice.security.otpas;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;



public class TotpServer {
    private String secret;
    private Clock clock;
    public TotpServer(String secret) {
        this.secret = secret;
        clock = new Clock();
    }
    public TotpServer()
    {
    	this.secret= null;
    	this.clock = new Clock();
    }
    public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public Clock getClock() {
		return clock;
	}
	public void setClock(Clock clock) {
		this.clock = clock;
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
    	if(clock.getMarkedLock() - 1 ==  markedLock || clock.getMarkedLock() + 9 == markedLock)
    	{
    		currentInterval = currentInterval - 1; 
    	}
    	return leftPadding(hash(secret, currentInterval));
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
