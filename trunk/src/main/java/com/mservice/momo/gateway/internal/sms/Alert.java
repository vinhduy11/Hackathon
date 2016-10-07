/**
 * Alert.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.sms;

public interface Alert extends java.rmi.Remote {
    public void start() throws java.rmi.RemoteException;
    public void stop() throws java.rmi.RemoteException;
    public void send(String dest, String sms) throws java.rmi.RemoteException;
}
