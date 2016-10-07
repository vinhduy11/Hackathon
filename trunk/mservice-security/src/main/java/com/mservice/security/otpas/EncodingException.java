package com.mservice.security.otpas;

public class EncodingException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4021414095285601939L;

	public EncodingException(String message) {
		super(message);
	}

	public EncodingException(Exception e) {
		super(e);
	}
}
