
package com.mservice.momo.util.pos.otpad;

public class EncryptionException extends Exception {


	/**
	 * 
	 */
	private static final long serialVersionUID = 8503333069593472499L;

	public EncryptionException(String message) {
		super(message);
	}

	public EncryptionException(Exception e) {
		super(e);
	}
}
