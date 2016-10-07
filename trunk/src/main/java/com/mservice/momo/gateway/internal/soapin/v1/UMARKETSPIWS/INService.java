/**
 * INService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public interface INService extends java.rmi.Remote {
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CreditAirtimeResponse creditAirtime(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CreditAirtimeRequest creditAirtimeRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.DebitAirtimeResponse debitAirtime(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.DebitAirtimeRequest debitAirtimeRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsAirtimeAccountValidResponse isAirtimeAccountValid(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsAirtimeAccountValidRequest isAirtimeAccountValidRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseResponse reverse(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseRequest reverseRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CommitResponse commit(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CommitRequest commitRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackResponse rollback(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackRequest rollbackRequest) throws java.rmi.RemoteException;
}
