package com.mservice.payment.exception;

public class OTPIncorrectFormatException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4824596033963419240L;
	public OTPIncorrectFormatException(String msg)
	{
		super(msg);
	}
	public OTPIncorrectFormatException(Exception e)
	{
		super(e);
	}
}
