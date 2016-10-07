/**
 * VASService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public interface VASService extends java.rmi.Remote {
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.PurchaseVASResponse purchaseVAS(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.PurchaseVASRequest purchaseVASRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsVASValidResponse isVASValid(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsVASValidRequest isVASValidRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseResponse reverse(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseRequest reverseRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CommitResponse commit(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CommitRequest commitRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackResponse rollback(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackRequest rollbackRequest) throws java.rmi.RemoteException;
}
