package com.mservice.momo.util.pos.otpad;
public class KeyException extends EncryptionException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6772916996214128701L;

	public KeyException(String message) {
		super(message);
	}

	public KeyException(Exception e) {
		super(e);
	}
}
