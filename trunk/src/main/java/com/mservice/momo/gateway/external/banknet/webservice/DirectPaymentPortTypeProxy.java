package com.mservice.momo.gateway.external.banknet.webservice;

public class DirectPaymentPortTypeProxy implements DirectPaymentPortType {
  private String _endpoint = null;
  private DirectPaymentPortType directPaymentPortType = null;
  
  public DirectPaymentPortTypeProxy() {
    _initDirectPaymentPortTypeProxy();
  }
  
  public DirectPaymentPortTypeProxy(String endpoint) {
    _endpoint = endpoint;
    _initDirectPaymentPortTypeProxy();
  }
  
  private void _initDirectPaymentPortTypeProxy() {
    try {
      directPaymentPortType = (new DirectPaymentLocator()).getDirectPaymentHttpSoap11Endpoint();
      if (directPaymentPortType != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)directPaymentPortType)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)directPaymentPortType)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (directPaymentPortType != null)
      ((javax.xml.rpc.Stub)directPaymentPortType)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public DirectPaymentPortType getDirectPaymentPortType() {
    if (directPaymentPortType == null)
      _initDirectPaymentPortTypeProxy();
    return directPaymentPortType;
  }
  
  public String querryBillStatus(String merchant_trans_id, String trans_id, String merchant_code, String trans_secure_code) throws java.rmi.RemoteException{
    if (directPaymentPortType == null)
      _initDirectPaymentPortTypeProxy();
    return directPaymentPortType.querryBillStatus(merchant_trans_id, trans_id, merchant_code, trans_secure_code);
  }
  
  public String confirmTransactionResult(String merchant_trans_id, String trans_id, String merchant_code, String trans_result, String trans_secure_code) throws java.rmi.RemoteException{
    if (directPaymentPortType == null)
      _initDirectPaymentPortTypeProxy();
    return directPaymentPortType.confirmTransactionResult(merchant_trans_id, trans_id, merchant_code, trans_result, trans_secure_code);
  }
  
  public String checkCardHolder(String merchant_trans_id, String merchant_code, String trans_id, String card_holder_number, String card_holder_name, String card_holder_month, String card_holder_year, String otpGetType, String trans_secure_code) throws java.rmi.RemoteException{
    if (directPaymentPortType == null)
      _initDirectPaymentPortTypeProxy();
    return directPaymentPortType.checkCardHolder(merchant_trans_id, merchant_code, trans_id, card_holder_number, card_holder_name, card_holder_month, card_holder_year, otpGetType, trans_secure_code);
  }
  
  public String sendGoodInfo_Ext(String merchant_trans_id, String merchant_code, String country_code, String good_code, String xml_description, String net_cost, String ship_fee, String tax, String trans_datetime, String trans_secure_code, String selected_bank) throws java.rmi.RemoteException{
    if (directPaymentPortType == null)
      _initDirectPaymentPortTypeProxy();
    return directPaymentPortType.sendGoodInfo_Ext(merchant_trans_id, merchant_code, country_code, good_code, xml_description, net_cost, ship_fee, tax, trans_datetime, trans_secure_code, selected_bank);
  }
  
  public String verifyOTP(String merchant_trans_id, String merchant_code, String trans_id, String otp_code, String trans_secure_code, String otpGetType) throws java.rmi.RemoteException{
    if (directPaymentPortType == null)
      _initDirectPaymentPortTypeProxy();
    return directPaymentPortType.verifyOTP(merchant_trans_id, merchant_code, trans_id, otp_code, trans_secure_code, otpGetType);
  }
  
  
}