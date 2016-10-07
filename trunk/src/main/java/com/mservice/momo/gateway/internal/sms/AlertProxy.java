package com.mservice.momo.gateway.internal.sms;

public class AlertProxy implements com.mservice.momo.gateway.internal.sms.Alert {
  private String _endpoint = null;
  private com.mservice.momo.gateway.internal.sms.Alert alert = null;
  
  public AlertProxy() {
    _initAlertProxy();
  }
  
  public AlertProxy(String endpoint) {
    _endpoint = endpoint;
    _initAlertProxy();
  }
  
  private void _initAlertProxy() {
    try {
      alert = (new com.mservice.momo.gateway.internal.sms.AlertServiceLocator()).getAlert();
      if (alert != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)alert)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)alert)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (alert != null)
      ((javax.xml.rpc.Stub)alert)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public com.mservice.momo.gateway.internal.sms.Alert getAlert() {
    if (alert == null)
      _initAlertProxy();
    return alert;
  }
  
  public void start() throws java.rmi.RemoteException{
    if (alert == null)
      _initAlertProxy();
    alert.start();
  }
  
  public void stop() throws java.rmi.RemoteException{
    if (alert == null)
      _initAlertProxy();
    alert.stop();
  }
  
  public void send(String dest, String sms) throws java.rmi.RemoteException{
    if (alert == null)
      _initAlertProxy();
    alert.send(dest, sms);
  }
  
  
}