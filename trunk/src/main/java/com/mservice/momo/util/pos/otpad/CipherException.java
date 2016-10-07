package com.mservice.momo.util.pos.otpad;

public class CipherException extends EncryptionException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4842995171858227424L;

	public CipherException(String message) {
		super(message);
	}

	public CipherException(Exception e) {
		super(e);
	}
}
