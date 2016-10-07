/**
 * BillingService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public interface BillingService extends java.rmi.Remote {
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.PayBillResponse payBill(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.PayBillRequest payBillRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsBillAccountValidResponse isBillAccountValid(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsBillAccountValidRequest isBillAccountValidRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseResponse reverse(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseRequest reverseRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CommitResponse commit(CommitRequest commitRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackResponse rollback(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackRequest rollbackRequest) throws java.rmi.RemoteException;
}
