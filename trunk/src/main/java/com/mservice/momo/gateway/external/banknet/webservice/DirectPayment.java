/**
 * DirectPayment.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.external.banknet.webservice;

public interface DirectPayment extends javax.xml.rpc.Service {
    public String getDirectPaymentHttpSoap12EndpointAddress();

    public DirectPaymentPortType getDirectPaymentHttpSoap12Endpoint() throws javax.xml.rpc.ServiceException;

    public DirectPaymentPortType getDirectPaymentHttpSoap12Endpoint(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
    public String getDirectPaymentHttpSoap11EndpointAddress();

    public DirectPaymentPortType getDirectPaymentHttpSoap11Endpoint() throws javax.xml.rpc.ServiceException;

    public DirectPaymentPortType getDirectPaymentHttpSoap11Endpoint(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
