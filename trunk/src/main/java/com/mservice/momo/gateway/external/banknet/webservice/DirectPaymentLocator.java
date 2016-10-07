/**
 * DirectPaymentLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.external.banknet.webservice;

public class DirectPaymentLocator extends org.apache.axis.client.Service implements DirectPayment {

    public DirectPaymentLocator() {
    }


    public DirectPaymentLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public DirectPaymentLocator(String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for DirectPaymentHttpSoap12Endpoint
    private String DirectPaymentHttpSoap12Endpoint_address = "http://sandbox.bndebit.vn/pg2705/services/DirectPayment.DirectPaymentHttpSoap12Endpoint";

    public String getDirectPaymentHttpSoap12EndpointAddress() {
        return DirectPaymentHttpSoap12Endpoint_address;
    }

    // The WSDD service name defaults to the port name.
    private String DirectPaymentHttpSoap12EndpointWSDDServiceName = "DirectPaymentHttpSoap12Endpoint";

    public String getDirectPaymentHttpSoap12EndpointWSDDServiceName() {
        return DirectPaymentHttpSoap12EndpointWSDDServiceName;
    }

    public void setDirectPaymentHttpSoap12EndpointWSDDServiceName(String name) {
        DirectPaymentHttpSoap12EndpointWSDDServiceName = name;
    }

    public DirectPaymentPortType getDirectPaymentHttpSoap12Endpoint() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(DirectPaymentHttpSoap12Endpoint_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getDirectPaymentHttpSoap12Endpoint(endpoint);
    }

    public DirectPaymentPortType getDirectPaymentHttpSoap12Endpoint(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            DirectPaymentSoap12BindingStub _stub = new DirectPaymentSoap12BindingStub(portAddress, this);
            _stub.setPortName(getDirectPaymentHttpSoap12EndpointWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setDirectPaymentHttpSoap12EndpointEndpointAddress(String address) {
        DirectPaymentHttpSoap12Endpoint_address = address;
    }


    // Use to get a proxy class for DirectPaymentHttpSoap11Endpoint
    private String DirectPaymentHttpSoap11Endpoint_address = "http://sandbox.bndebit.vn/pg2705/services/DirectPayment.DirectPaymentHttpSoap11Endpoint";

    public String getDirectPaymentHttpSoap11EndpointAddress() {
        return DirectPaymentHttpSoap11Endpoint_address;
    }

    // The WSDD service name defaults to the port name.
    private String DirectPaymentHttpSoap11EndpointWSDDServiceName = "DirectPaymentHttpSoap11Endpoint";

    public String getDirectPaymentHttpSoap11EndpointWSDDServiceName() {
        return DirectPaymentHttpSoap11EndpointWSDDServiceName;
    }

    public void setDirectPaymentHttpSoap11EndpointWSDDServiceName(String name) {
        DirectPaymentHttpSoap11EndpointWSDDServiceName = name;
    }

    public DirectPaymentPortType getDirectPaymentHttpSoap11Endpoint() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(DirectPaymentHttpSoap11Endpoint_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getDirectPaymentHttpSoap11Endpoint(endpoint);
    }

    public DirectPaymentPortType getDirectPaymentHttpSoap11Endpoint(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            DirectPaymentSoap11BindingStub _stub = new DirectPaymentSoap11BindingStub(portAddress, this);
            _stub.setPortName(getDirectPaymentHttpSoap11EndpointWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setDirectPaymentHttpSoap11EndpointEndpointAddress(String address) {
        DirectPaymentHttpSoap11Endpoint_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     * This service has multiple ports for a given interface;
     * the proxy implementation returned may be indeterminate.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (DirectPaymentPortType.class.isAssignableFrom(serviceEndpointInterface)) {
                DirectPaymentSoap12BindingStub _stub = new DirectPaymentSoap12BindingStub(new java.net.URL(DirectPaymentHttpSoap12Endpoint_address), this);
                _stub.setPortName(getDirectPaymentHttpSoap12EndpointWSDDServiceName());
                return _stub;
            }
            if (DirectPaymentPortType.class.isAssignableFrom(serviceEndpointInterface)) {
                DirectPaymentSoap11BindingStub _stub = new DirectPaymentSoap11BindingStub(new java.net.URL(DirectPaymentHttpSoap11Endpoint_address), this);
                _stub.setPortName(getDirectPaymentHttpSoap11EndpointWSDDServiceName());
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
        if ("DirectPaymentHttpSoap12Endpoint".equals(inputPortName)) {
            return getDirectPaymentHttpSoap12Endpoint();
        }
        else if ("DirectPaymentHttpSoap11Endpoint".equals(inputPortName)) {
            return getDirectPaymentHttpSoap11Endpoint();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://pgWebservices", "DirectPayment");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://pgWebservices", "DirectPaymentHttpSoap12Endpoint"));
            ports.add(new javax.xml.namespace.QName("http://pgWebservices", "DirectPaymentHttpSoap11Endpoint"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(String portName, String address) throws javax.xml.rpc.ServiceException {
        
if ("DirectPaymentHttpSoap12Endpoint".equals(portName)) {
            setDirectPaymentHttpSoap12EndpointEndpointAddress(address);
        }
        else 
if ("DirectPaymentHttpSoap11Endpoint".equals(portName)) {
            setDirectPaymentHttpSoap11EndpointEndpointAddress(address);
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
