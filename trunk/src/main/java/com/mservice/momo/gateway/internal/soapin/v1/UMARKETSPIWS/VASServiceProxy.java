package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public class VASServiceProxy implements VASService {
  private String _endpoint = null;
  private VASService vASService = null;
  
  public VASServiceProxy() {
    _initVASServiceProxy();
  }
  
  public VASServiceProxy(String endpoint) {
    _endpoint = endpoint;
    _initVASServiceProxy();
  }
  
  private void _initVASServiceProxy() {
    try {
      vASService = (new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.UMarketServiceProviderLocator()).getVASServiceProviderSOAP();
      if (vASService != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)vASService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)vASService)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (vASService != null)
      ((javax.xml.rpc.Stub)vASService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public VASService getVASService() {
    if (vASService == null)
      _initVASServiceProxy();
    return vASService;
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.PurchaseVASResponse purchaseVAS(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.PurchaseVASRequest purchaseVASRequest) throws java.rmi.RemoteException{
    if (vASService == null)
      _initVASServiceProxy();
    return vASService.purchaseVAS(purchaseVASRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsVASValidResponse isVASValid(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsVASValidRequest isVASValidRequest) throws java.rmi.RemoteException{
    if (vASService == null)
      _initVASServiceProxy();
    return vASService.isVASValid(isVASValidRequest);
  }
  
  public ReverseResponse reverse(ReverseRequest reverseRequest) throws java.rmi.RemoteException{
    if (vASService == null)
      _initVASServiceProxy();
    return vASService.reverse(reverseRequest);
  }
  
  public CommitResponse commit(CommitRequest commitRequest) throws java.rmi.RemoteException{
    if (vASService == null)
      _initVASServiceProxy();
    return vASService.commit(commitRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackResponse rollback(RollbackRequest rollbackRequest) throws java.rmi.RemoteException{
    if (vASService == null)
      _initVASServiceProxy();
    return vASService.rollback(rollbackRequest);
  }
  
  
}