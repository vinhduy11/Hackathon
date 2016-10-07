package com.mservice.momo.util.pos.otpas;

public class ConverterUtils {
	public static String Base10toBase30(String val) throws ConverterException
	{
		int val10 = Integer.MAX_VALUE;
		String rs = "N/A";
		try{
			val10 = Integer.valueOf(val,10);
			rs = Integer.toString(val10, 30);
		}catch(Exception e)
		{
			throw new ConverterException(e);
		}
		return rs;
	}
	public static String Base30toBase10(String val, int len) throws ConverterException
	{
		int val30 = Integer.MAX_VALUE;
		String rs = "N/A";
		String format = "%0" + len+"d";
		try{
			val30 = Integer.valueOf(val,30);
			rs = String.format(format,val30);
		}catch(Exception e)
		{
			throw new ConverterException(e);
		}
		return rs;
	}
	
}
