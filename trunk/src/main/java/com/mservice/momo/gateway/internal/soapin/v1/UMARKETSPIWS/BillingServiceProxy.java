package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public class BillingServiceProxy implements BillingService {
  private String _endpoint = null;
  private BillingService billingService = null;
  
  public BillingServiceProxy() {
    _initBillingServiceProxy();
  }
  
  public BillingServiceProxy(String endpoint) {
    _endpoint = endpoint;
    _initBillingServiceProxy();
  }
  
  private void _initBillingServiceProxy() {
    try {
      billingService = (new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.UMarketServiceProviderLocator()).getBillingServiceProviderSOAP();
      if (billingService != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)billingService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)billingService)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (billingService != null)
      ((javax.xml.rpc.Stub)billingService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public BillingService getBillingService() {
    if (billingService == null)
      _initBillingServiceProxy();
    return billingService;
  }
  
  public PayBillResponse payBill(PayBillRequest payBillRequest) throws java.rmi.RemoteException{
    if (billingService == null)
      _initBillingServiceProxy();
    return billingService.payBill(payBillRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsBillAccountValidResponse isBillAccountValid(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsBillAccountValidRequest isBillAccountValidRequest) throws java.rmi.RemoteException{
    if (billingService == null)
      _initBillingServiceProxy();
    return billingService.isBillAccountValid(isBillAccountValidRequest);
  }
  
  public ReverseResponse reverse(ReverseRequest reverseRequest) throws java.rmi.RemoteException{
    if (billingService == null)
      _initBillingServiceProxy();
    return billingService.reverse(reverseRequest);
  }
  
  public CommitResponse commit(CommitRequest commitRequest) throws java.rmi.RemoteException{
    if (billingService == null)
      _initBillingServiceProxy();
    return billingService.commit(commitRequest);
  }
  
  public RollbackResponse rollback(RollbackRequest rollbackRequest) throws java.rmi.RemoteException{
    if (billingService == null)
      _initBillingServiceProxy();
    return billingService.rollback(rollbackRequest);
  }
  
  
}