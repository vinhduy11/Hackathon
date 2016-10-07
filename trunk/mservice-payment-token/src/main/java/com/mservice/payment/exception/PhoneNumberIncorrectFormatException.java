package com.mservice.payment.exception;

public class PhoneNumberIncorrectFormatException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4824596033963419240L;
	public PhoneNumberIncorrectFormatException(String msg)
	{
		super(msg);
	}
	public PhoneNumberIncorrectFormatException(Exception e)
	{
		super(e);
	}
}
