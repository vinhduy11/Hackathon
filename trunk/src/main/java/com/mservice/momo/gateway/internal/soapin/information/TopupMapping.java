package com.mservice.momo.gateway.internal.soapin.information;

import com.mservice.momo.vertx.processor.Common;

public class TopupMapping {
	enum AirtimeRanges {
		_0950,_0951,_0952,_0953,_0954,_0955,_0956,_0957,_0958,_0959,
		
		_0910,_0911,_0912,_0913,_0914,_0915,_0916,_0917,_0918,_0919,
		_0940,_0941,_0942,_0943,_0944,_0945,_0946,_0947,_0948,_0949,
		_0123,_0124,_0125,_0127,_0129,
		
		_0960,_0961,_0962,_0963,_0964,_0965,_0966,_0967,_0968,_0969,
		_0970,_0971,_0972,_0973,_0974,_0975,_0976,_0977,_0978,_0979,
		_0980,_0981,_0982,_0983,_0984,_0985,_0986,_0987,_0988,_0989,
		_0162,_0163,_0164,_0165,_0166,_0167,_0168,_0169,
		
		_0930,_0931,_0932,_0933,_0934,_0935,_0936,_0937,_0938,_0939,
		_0900,_0901,_0902,_0903,_0904,_0905,_0906,_0907,_0908,_0909,
		_0120,_0121,_0122,_0126,_0128,
		
		_0920,_0921,_0922,_0923,_0924,_0925,_0926,_0927,_0928,_0929,
		_0188,_0186,
		
		_0199,
		_0990,_0991,_0992,_0993,_0994,_0995,_0996,_0997,_0998,_0999,
		
		_0420,_0421,_0422,_0423,_0424,_0425,_0426,_0427,_0428,_0429,
		_0820,_0821,_0822,_0823,_0824,_0825,_0826,_0827,_0828,_0829,
		_07802,_0712,_06502,_0642,_0612,
		_0860,_0861,_0862,_0863,_0864,_0865,_0866,_0867,_0868,_0869,
		_0880,_0881,_0882,_0883,_0884,_0885,_0886,_0887,_0888,_0889,
		_0890,_0891,_0892,_0893,_0894,_0895,_0896,_0897,_0898,_0899

		}
	public static String getTargetTopup (String phoneNumber, String agent, Common.BuildLog log){
        log.add("call function","getTargetTopup");
        log.add("number to getTargetTopup", phoneNumber);

        String result = "";
		if(phoneNumber.length() < 10)
			return "unknown.airtime";
		if(!phoneNumber.startsWith("0"))
			return "unknown.airtime";
		if(phoneNumber.startsWith("0780") || phoneNumber.startsWith("0650")){

            phoneNumber = phoneNumber.substring(0,5);
            log.add("phoneNumber after subString(0,5)", phoneNumber);
        }else{
            phoneNumber = phoneNumber.substring(0,4);
            log.add("phoneNumber after subString(0,4)", phoneNumber);
        }
		AirtimeRanges ar = null;
		try{
			ar = AirtimeRanges.valueOf("_" + phoneNumber);
            log.add("AirtimeRanges", ar);
		}
		catch (IllegalArgumentException e) {

            log.add("exception", e.getMessage());
            log.add("desc","getTargetTopup, Can not get target for " + phoneNumber + ". This phone number is not in ranges");
            return "";

		}catch (Exception ex){
            log.add("exception", ex.getMessage());
            log.add("desc","getTargetTopup, Can not get target for " + phoneNumber + ". This phone number is not in ranges");
            return "";
        }

		if(ar==null)
			return "";
		else
			switch (ar) {
			//Sfone
			case _0950: case _0951: case _0952: case _0953: case _0954: case _0955: case _0956: case _0957: case _0958: case _0959: 
				result = "sfonev22.airtime";
				break;
			//Vinaphone
			case _0910: case _0911: case _0912: case _0913: case _0914: case _0915: case _0916: case _0917: case _0918: case _0919:
			case _0940: case _0941: case _0942: case _0943: case _0944: case _0945: case _0946: case _0947: case _0948: case _0949:
			case _0123: case _0124: case _0125: case _0127: case _0129:
				case _0880: case _0881: case _0882: case _0883: case _0884: case _0885: case _0886: case _0887: case _0888: case _0889:
				result = "vinaphone.airtime";
				break;
			//Viettel
			case _0960: case _0961: case _0962: case _0963: case _0964: case _0965: case _0966: case _0967: case _0968: case _0969:
			case _0970: case _0971: case _0972: case _0973: case _0974: case _0975: case _0976: case _0977: case _0978: case _0979:  
			case _0980: case _0981: case _0982: case _0983: case _0984: case _0985: case _0986: case _0987: case _0988: case _0989:
				case _0860: case _0861: case _0862: case _0863: case _0864: case _0865: case _0866: case _0867: case _0868: case _0869:
				result = "viettelv1.airtime";
				break;
			case  _0162: case  _0163: case _0164: case _0165: case _0166: case _0167: case _0168: case _0169: 
				result = "viettelv2.airtime";
				break;
			//Mobifone
			case _0930: case _0931: case _0932: case _0933: case _0934: case _0935: case _0936: case _0937: case _0938: case _0939: 
			case _0900: case _0901: case _0902: case _0903: case _0904: case _0905: case _0906: case _0907: case _0908: case _0909: 
			case _0120: case _0121: case _0122: case _0126: case _0128:
				case _0890: case _0891: case _0892: case _0893: case _0894: case _0895: case _0896: case _0897: case _0898: case _0899:
					result = "vms.airtime";
				break;
			//Vietnam Mobile			
			case  _0920: case _0921: case _0922: case _0923: case _0924: case _0925: case _0926: case _0927: case _0928: case _0929: 
			case _0186:  case _0188:
				result = "vnmobile.airtime";
				break; 
			//Beeline
			case  _0199: 
			case  _0990: case  _0991: case _0992: case  _0993: case  _0994: case _0995: case  _0996: case  _0997: case _0998: case _0999:
				result = "beeline.airtime";
				break; 
			//EVN
			//case _0960: case _0961: case _0962: case _0963: case _0964: case _0965: case _0966: case _0967: case _0968: case _0969:
			case _0420: case _0421: case _0422: case _0423: case _0424: case _0425: case _0426: case _0427: case _0428: case _0429: 
			case _0820: case _0821: case _0822: case _0823: case _0824: case _0825: case _0826: case _0827: case _0828: case _0829: 
			case _07802: case _0712: case _06502: case _0642: case _0612:
				result = "evnmobile.airtime";
				break; 
				
			default:
				result = "unknown.airtime";
				break;
			}

        log.add("result", result);

		return result;
	}
}
