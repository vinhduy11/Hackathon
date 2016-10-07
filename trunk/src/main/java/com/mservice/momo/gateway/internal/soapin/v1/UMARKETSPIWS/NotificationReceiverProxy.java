package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public class NotificationReceiverProxy implements NotificationReceiver {
  private String _endpoint = null;
  private NotificationReceiver notificationReceiver = null;
  
  public NotificationReceiverProxy() {
    _initNotificationReceiverProxy();
  }
  
  public NotificationReceiverProxy(String endpoint) {
    _endpoint = endpoint;
    _initNotificationReceiverProxy();
  }
  
  private void _initNotificationReceiverProxy() {
    try {
      notificationReceiver = (new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.UMarketServiceProviderLocator()).getNotificationReceiverSOAP();
      if (notificationReceiver != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)notificationReceiver)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)notificationReceiver)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (notificationReceiver != null)
      ((javax.xml.rpc.Stub)notificationReceiver)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public NotificationReceiver getNotificationReceiver() {
    if (notificationReceiver == null)
      _initNotificationReceiverProxy();
    return notificationReceiver;
  }
  
  public NotifyResponse notify(NotifyRequest notifyRequest) throws java.rmi.RemoteException{
    if (notificationReceiver == null)
      _initNotificationReceiverProxy();
    return notificationReceiver.notify(notifyRequest);
  }
  
  
}