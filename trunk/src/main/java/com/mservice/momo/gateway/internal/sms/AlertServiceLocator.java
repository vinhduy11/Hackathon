/**
 * AlertServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.sms;

public class AlertServiceLocator extends org.apache.axis.client.Service implements com.mservice.momo.gateway.internal.sms.AlertService {

    public AlertServiceLocator() {
    }


    public AlertServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public AlertServiceLocator(String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for Alert
    private String Alert_address = "http://172.16.18.50:18080/Client/services/Alert";

    public String getAlertAddress() {
        return Alert_address;
    }

    // The WSDD service name defaults to the port name.
    private String AlertWSDDServiceName = "Alert";

    public String getAlertWSDDServiceName() {
        return AlertWSDDServiceName;
    }

    public void setAlertWSDDServiceName(String name) {
        AlertWSDDServiceName = name;
    }

    public com.mservice.momo.gateway.internal.sms.Alert getAlert() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(Alert_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getAlert(endpoint);
    }

    public com.mservice.momo.gateway.internal.sms.Alert getAlert(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.mservice.momo.gateway.internal.sms.AlertSoapBindingStub _stub = new com.mservice.momo.gateway.internal.sms.AlertSoapBindingStub(portAddress, this);
            _stub.setPortName(getAlertWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setAlertEndpointAddress(String address) {
        Alert_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.mservice.momo.gateway.internal.sms.Alert.class.isAssignableFrom(serviceEndpointInterface)) {
                com.mservice.momo.gateway.internal.sms.AlertSoapBindingStub _stub = new com.mservice.momo.gateway.internal.sms.AlertSoapBindingStub(new java.net.URL(Alert_address), this);
                _stub.setPortName(getAlertWSDDServiceName());
                return _stub;
            }
        }
        catch (Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        String inputPortName = portName.getLocalPart();
        if ("Alert".equals(inputPortName)) {
            return getAlert();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://smpp", "AlertService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://smpp", "Alert"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(String portName, String address) throws javax.xml.rpc.ServiceException {

if ("Alert".equals(portName)) {
            setAlertEndpointAddress(address);
        }
        else
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
