package com.mservice.payment.exception;

public class GiftNotExistException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4824596033963419240L;
	public GiftNotExistException(String msg)
	{
		super(msg);
	}
	public GiftNotExistException(Exception e)
	{
		super(e);
	}
}
