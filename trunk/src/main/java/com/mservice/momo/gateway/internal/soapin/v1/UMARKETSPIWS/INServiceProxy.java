package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public class INServiceProxy implements INService {
  private String _endpoint = null;
  private INService iNService = null;
  
  public INServiceProxy() {
    _initINServiceProxy();
  }
  
  public INServiceProxy(String endpoint) {
    _endpoint = endpoint;
    _initINServiceProxy();
  }
  
  private void _initINServiceProxy() {
    try {
      iNService = (new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.UMarketServiceProviderLocator()).getINServiceProviderSOAP();
      if (iNService != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)iNService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)iNService)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (iNService != null)
      ((javax.xml.rpc.Stub)iNService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public INService getINService() {
    if (iNService == null)
      _initINServiceProxy();
    return iNService;
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CreditAirtimeResponse creditAirtime(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CreditAirtimeRequest creditAirtimeRequest) throws java.rmi.RemoteException{
    if (iNService == null)
      _initINServiceProxy();
    return iNService.creditAirtime(creditAirtimeRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.DebitAirtimeResponse debitAirtime(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.DebitAirtimeRequest debitAirtimeRequest) throws java.rmi.RemoteException{
    if (iNService == null)
      _initINServiceProxy();
    return iNService.debitAirtime(debitAirtimeRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsAirtimeAccountValidResponse isAirtimeAccountValid(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsAirtimeAccountValidRequest isAirtimeAccountValidRequest) throws java.rmi.RemoteException{
    if (iNService == null)
      _initINServiceProxy();
    return iNService.isAirtimeAccountValid(isAirtimeAccountValidRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseResponse reverse(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseRequest reverseRequest) throws java.rmi.RemoteException{
    if (iNService == null)
      _initINServiceProxy();
    return iNService.reverse(reverseRequest);
  }
  
  public CommitResponse commit(CommitRequest commitRequest) throws java.rmi.RemoteException{
    if (iNService == null)
      _initINServiceProxy();
    return iNService.commit(commitRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackResponse rollback(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackRequest rollbackRequest) throws java.rmi.RemoteException{
    if (iNService == null)
      _initINServiceProxy();
    return iNService.rollback(rollbackRequest);
  }
  
  
}