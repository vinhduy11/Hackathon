/**
 * UMarketServiceProvider.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public interface UMarketServiceProvider extends javax.xml.rpc.Service {
    public String getBillingServiceProviderSOAPAddress();

    public BillingService getBillingServiceProviderSOAP() throws javax.xml.rpc.ServiceException;

    public BillingService getBillingServiceProviderSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
    public String getVASServiceProviderSOAPAddress();

    public VASService getVASServiceProviderSOAP() throws javax.xml.rpc.ServiceException;

    public VASService getVASServiceProviderSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
    public String getBankServiceProviderSOAPAddress();

    public BankService getBankServiceProviderSOAP() throws javax.xml.rpc.ServiceException;

    public BankService getBankServiceProviderSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
    public String getNotificationReceiverSOAPAddress();

    public NotificationReceiver getNotificationReceiverSOAP() throws javax.xml.rpc.ServiceException;

    public NotificationReceiver getNotificationReceiverSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
    public String getINServiceProviderSOAPAddress();

    public INService getINServiceProviderSOAP() throws javax.xml.rpc.ServiceException;

    public INService getINServiceProviderSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
