package com.mservice.security.otpas;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TotpClient {
    private String secret;
    private Clock clock;
    public TotpClient(String secret) {
        this.secret = secret;
        clock = new Clock();
    }
    public TotpClient(String secret, Clock clock) {
        this.secret = secret;
        this.clock = clock;
    }
    public String now() {
        return leftPadding(hash(secret, clock.getCurrentInterval()));// + clock.getMarkedLock();
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
    public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
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
	public Clock getClock() {
		return clock;
	}
}
