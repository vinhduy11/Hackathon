package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public class BankServiceProxy implements BankService {
  private String _endpoint = null;
  private BankService bankService = null;
  
  public BankServiceProxy() {
    _initBankServiceProxy();
  }
  
  public BankServiceProxy(String endpoint) {
    _endpoint = endpoint;
    _initBankServiceProxy();
  }
  
  private void _initBankServiceProxy() {
    try {
      bankService = (new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.UMarketServiceProviderLocator()).getBankServiceProviderSOAP();
      if (bankService != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)bankService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)bankService)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (bankService != null)
      ((javax.xml.rpc.Stub)bankService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public BankService getBankService() {
    if (bankService == null)
      _initBankServiceProxy();
    return bankService;
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CreditBankAccountResponse creditBankAccount(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CreditBankAccountRequest creditBankAccountRequest) throws java.rmi.RemoteException{
    if (bankService == null)
      _initBankServiceProxy();
    return bankService.creditBankAccount(creditBankAccountRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.DebitBankAccountResponse debitBankAccount(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.DebitBankAccountRequest debitBankAccountRequest) throws java.rmi.RemoteException{
    if (bankService == null)
      _initBankServiceProxy();
    return bankService.debitBankAccount(debitBankAccountRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsBankAccountValidResponse isBankAccountValid(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsBankAccountValidRequest isBankAccountValidRequest) throws java.rmi.RemoteException{
    if (bankService == null)
      _initBankServiceProxy();
    return bankService.isBankAccountValid(isBankAccountValidRequest);
  }
  
  public ReverseResponse reverse(ReverseRequest reverseRequest) throws java.rmi.RemoteException{
    if (bankService == null)
      _initBankServiceProxy();
    return bankService.reverse(reverseRequest);
  }
  
  public CommitResponse commit(CommitRequest commitRequest) throws java.rmi.RemoteException{
    if (bankService == null)
      _initBankServiceProxy();
    return bankService.commit(commitRequest);
  }
  
  public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackResponse rollback(RollbackRequest rollbackRequest) throws java.rmi.RemoteException{
    if (bankService == null)
      _initBankServiceProxy();
    return bankService.rollback(rollbackRequest);
  }
  
  
}