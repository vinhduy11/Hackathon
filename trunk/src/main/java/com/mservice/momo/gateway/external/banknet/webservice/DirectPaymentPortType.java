/**
 * DirectPaymentPortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.external.banknet.webservice;

public interface DirectPaymentPortType extends java.rmi.Remote {
    public String querryBillStatus(String merchant_trans_id, String trans_id, String merchant_code, String trans_secure_code) throws java.rmi.RemoteException;
    public String confirmTransactionResult(String merchant_trans_id, String trans_id, String merchant_code, String trans_result, String trans_secure_code) throws java.rmi.RemoteException;
    public String checkCardHolder(String merchant_trans_id, String merchant_code, String trans_id, String card_holder_number, String card_holder_name, String card_holder_month, String card_holder_year, String otpGetType, String trans_secure_code) throws java.rmi.RemoteException;
    public String sendGoodInfo_Ext(String merchant_trans_id, String merchant_code, String country_code, String good_code, String xml_description, String net_cost, String ship_fee, String tax, String trans_datetime, String trans_secure_code, String selected_bank) throws java.rmi.RemoteException;
    public String verifyOTP(String merchant_trans_id, String merchant_code, String trans_id, String otp_code, String trans_secure_code, String otpGetType) throws java.rmi.RemoteException;
}
