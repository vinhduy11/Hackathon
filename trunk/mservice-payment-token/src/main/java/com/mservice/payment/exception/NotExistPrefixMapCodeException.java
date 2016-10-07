package com.mservice.payment.exception;

public class NotExistPrefixMapCodeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5192343105628072206L;
	public NotExistPrefixMapCodeException(String message)
	{
		super(message);
	}
	public NotExistPrefixMapCodeException(Exception e)
	{
		super(e);
	}

}
