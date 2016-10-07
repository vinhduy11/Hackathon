package com.mservice.payment.exception;

public class PaymentTokenIncorrectFormatException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4824596033963419240L;
	public PaymentTokenIncorrectFormatException(String msg)
	{
		super(msg);
	}
	public PaymentTokenIncorrectFormatException(Exception e)
	{
		super(e);
	}
}
