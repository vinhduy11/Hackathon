package com.mservice.momo.util.pos.otpas;

public class EncodingException extends Exception {
	public EncodingException(String message) {
		super(message);
	}

	public EncodingException(Exception e) {
		super(e);
	}
}
