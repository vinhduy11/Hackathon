/**
 * AlertService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.sms;

public interface AlertService extends javax.xml.rpc.Service {
    public String getAlertAddress();

    public com.mservice.momo.gateway.internal.sms.Alert getAlert() throws javax.xml.rpc.ServiceException;

    public com.mservice.momo.gateway.internal.sms.Alert getAlert(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
