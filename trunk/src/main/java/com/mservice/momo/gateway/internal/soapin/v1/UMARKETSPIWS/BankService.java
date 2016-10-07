/**
 * BankService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public interface BankService extends java.rmi.Remote {
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CreditBankAccountResponse creditBankAccount(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CreditBankAccountRequest creditBankAccountRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.DebitBankAccountResponse debitBankAccount(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.DebitBankAccountRequest debitBankAccountRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsBankAccountValidResponse isBankAccountValid(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.IsBankAccountValidRequest isBankAccountValidRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseResponse reverse(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ReverseRequest reverseRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CommitResponse commit(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.CommitRequest commitRequest) throws java.rmi.RemoteException;
    public com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackResponse rollback(com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.RollbackRequest rollbackRequest) throws java.rmi.RemoteException;
}
