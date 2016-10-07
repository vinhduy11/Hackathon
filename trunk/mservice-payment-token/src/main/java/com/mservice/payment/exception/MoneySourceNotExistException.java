package com.mservice.payment.exception;

public class MoneySourceNotExistException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4824596033963419240L;
	public MoneySourceNotExistException(String msg)
	{
		super(msg);
	}
	public MoneySourceNotExistException(Exception e)
	{
		super(e);
	}
}
