package com.mservice.security.utils;

public class ConverterException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ConverterException(String message) {
		super(message);
	}

	public ConverterException(Exception e) {
		super(e);
	}
}
