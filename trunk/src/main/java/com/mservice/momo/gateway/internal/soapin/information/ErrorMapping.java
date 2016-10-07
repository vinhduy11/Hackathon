package com.mservice.momo.gateway.internal.soapin.information;

import vn.com.ms.config.Config;

public class ErrorMapping {
	public static String getBankName(String bankcode) {
		String result = "";
		result = Config.getStrConfig("bank.fromcode." + bankcode, "");
		return result;
	}
	public static int getResultCode(int soapResult){
		int result = 100;
		switch (soapResult) {
		case 0: // success
			result = 0;
			break;
		case 1:
			result = 0;
			break;
		case 1001: // Insufficient�funds
			result = 17; //"Insufficient funds"
			break;
		case 3: //The�Agent�attempting�to�access�the�system�could�not�be�found
			result = 3; //"The account doesn't exist in M_Service system"
			break;
		case 4: //The�Agent�attempting�to�access�the�system�must�be�registered�before�they�can�use�any�functionality
			result = 3; //"The account doesn't exist in M_Service system"
			break;
		case 14: //The�Agent�attempting�to�access�the�system�must�be�registered�before�they�can�use�any�functionality
			result = 3; //"The account doesn't exist in M_Service system"
			break;
		case 7: //Access�is�denied�for�this�function�to�the�requesting�Agent
			result = 1001;
			break;
		case 13: //account not found
			result = 1005;
			break;			
		case 15: //Access�is�denied�for�this�function�to�the�requesting�Agent
			result = 1005;
			break;
		case 17: 
			result = 13; //"Amount is invalid"
			break;
		case 58: //Agent�is�already�stopped
			result = 9; //"The Account was locked"
			break;
		case 38: //Agent�is�already�stopped
			result = 9; //"The Account was locked"
			break;
		case 39: //Agent�is�already�stopped
			result = 9; //"The Account was locked"
			break;
		case 23: //Amount�requested�is�too�small
			result = 13; //"Amount is invalid"
			break;
		case 24: //Amount�requested�is�too�large
			result = 13; //"Amount is invalid"
			break;
		case 1003: //Wallet�Balance�Exceeded
			result = 18; //Wallet�Balance�Exceeded
			break;
		case 1011:
			result = 20; //Password Agent Invalid
			break;
		case 1013: //Password Expired
			result = 21; //Password Agent Expired, Please Change New Password
			break; 
		case 1012: //Password�error�retry�exceeded
			result = 22; //Password�error�retry�exceeded
			break; 
		case 1023: //invalid parameter
			result = 13;
			break;
		///////////////////////////////////////////////////
		case 1006: 
			result = 1006; 
			break;
		case 41: 
			result = 100; 
			break;
		case 42: 
			result = 100; 
			break;
		case 43: 
			result = 100; 
			break;
		case 44: 
			result = 100; 
			break;
		case 45: 
			result = 100; 
			break;
		case 47: 
			result = 1005; 
			break;
		case 66: 
			result = 1004; 
			break;
		case 67: 
			result = 1004; 
			break;
		default:
			result = 100; //Webservice error
			break;
		}
		return result;
	}
	public static String getDescriptionError(int resultCode) {
		String result = "";
		switch(resultCode){
		case 0: 
			result = "Successfully";
			break;
		case 1:
			result = "Invalid Username/Password";
			break;
		case 2:
			result = "Invalid DataSign";
			break;
		case 3:
			result = "The account doesn't exist in M_Service system";
			break;
		case 4:
			result = "The account has been already registered with another bank";
			break;
		case 5:
			result = "The account has been already registered with this bank";
			break;
		case 6:
			result = "The account is not registered with any bank";
			break;
		case 7:
			result = "Password can not be decrypted";
			break;
		case 8:
			result = "The account can not be Registered/Unregistered because MoMo greater than zero";
			break;
		case 9:
			result = "The Account was locked";
			break;
		case 10:
			result = "The SessionID is expired";
			break;
		case 11:
			result = "The transaction is timeout";
			break;
		case 12:
			result = "Over limited transaction per days";
			break;
		case 13:
			result = "Amount is invalid";
			break;
		case 14:
			result = "Duplicate TransID";
			break;
		case 15:
			result = "The service is upgrading";
			break;
		case 16:
			result = "Invalid parameters";
			break;
		case 17:
			result = "Insufficient funds";
			break;
		case 18:
			result = "Wallet Balance Exceeded";
			break;
		case 19:
			result = "Wallet Cap Exceeded";
			break;
		case 20:
			result = "Agent MPin Invalid. Agent will be locked after 3 times invalid MPin";
			break;
		case 21:
			result = "Agent MPin Expired. Please change new MPin";
			break;
		case 22:
			result = "Agent MPin error�retry�exceeded";
			break;
		case 23:
			result = "Transaction ID is not exist on M_Service";
			break;
		case 24:
			result = "Transaction is processing";
			break;
		case 25:
			result = "Database error, can't get Transaction Info";
			break;
		case 26:
			result = "Process error, can't get Transaction Info";
			break;
		case 100:
			result = "Fail, webservice error";
			break;
		case 1001:
			result = "Access is denied for this function to the requesting Agent";
			break;
		case 1004:
			result = "Fail, M_Service Third Party error (error of partner that supplies service to M_Service)";
			break;
		case 1005:
			result = "Fail, invalid customer account";
			break;
		case 1006:
			result = "Fail, M_Service Third Party error or Customer account is invalid";
			break;
		default:
			result = "Fail, indefinite error";
			break;
		}
		return result;
	}
	
	public static int getCode(int res){
		switch (res) {
		case 0:
			return ResultCode.SUCCESS;
		case 1001:
			return ResultCode.INSUFFICIENT_FUNDS;
		case 1180:
			return ResultCode.NOT_HAVE_BANK_ACCOUNT;
		case 13:
			return ResultCode.WRONG_AMOUNT;
		case 1023:
			return ResultCode.BILLPAY_WRONG_ACCOUNTID;
		case 1016:
			return ResultCode.AUTH_PASSWORD_PREV_USED;
			
		default:
			return ResultCode.SYSTEM_ERROR;
		}
	}
}
