/**
 * UMarketServiceProviderLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public class UMarketServiceProviderLocator extends org.apache.axis.client.Service implements UMarketServiceProvider {

    public UMarketServiceProviderLocator() {
    }


    public UMarketServiceProviderLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public UMarketServiceProviderLocator(String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for BillingServiceProviderSOAP
    private String BillingServiceProviderSOAP_address = "http://172.16.18.50:58080/UMarketSPI/BillingService";

    public String getBillingServiceProviderSOAPAddress() {
        return BillingServiceProviderSOAP_address;
    }

    // The WSDD service name defaults to the port name.
    private String BillingServiceProviderSOAPWSDDServiceName = "BillingServiceProviderSOAP";

    public String getBillingServiceProviderSOAPWSDDServiceName() {
        return BillingServiceProviderSOAPWSDDServiceName;
    }

    public void setBillingServiceProviderSOAPWSDDServiceName(String name) {
        BillingServiceProviderSOAPWSDDServiceName = name;
    }

    public BillingService getBillingServiceProviderSOAP() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(BillingServiceProviderSOAP_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getBillingServiceProviderSOAP(endpoint);
    }

    public BillingService getBillingServiceProviderSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.BillingServiceProviderSOAPStub _stub = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.BillingServiceProviderSOAPStub(portAddress, this);
            _stub.setPortName(getBillingServiceProviderSOAPWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setBillingServiceProviderSOAPEndpointAddress(String address) {
        BillingServiceProviderSOAP_address = address;
    }


    // Use to get a proxy class for VASServiceProviderSOAP
    private String VASServiceProviderSOAP_address = "http://localhost:8080/tesst/services/VASServiceProviderSOAP";

    public String getVASServiceProviderSOAPAddress() {
        return VASServiceProviderSOAP_address;
    }

    // The WSDD service name defaults to the port name.
    private String VASServiceProviderSOAPWSDDServiceName = "VASServiceProviderSOAP";

    public String getVASServiceProviderSOAPWSDDServiceName() {
        return VASServiceProviderSOAPWSDDServiceName;
    }

    public void setVASServiceProviderSOAPWSDDServiceName(String name) {
        VASServiceProviderSOAPWSDDServiceName = name;
    }

    public VASService getVASServiceProviderSOAP() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(VASServiceProviderSOAP_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getVASServiceProviderSOAP(endpoint);
    }

    public VASService getVASServiceProviderSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            VASServiceProviderSOAPStub _stub = new VASServiceProviderSOAPStub(portAddress, this);
            _stub.setPortName(getVASServiceProviderSOAPWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setVASServiceProviderSOAPEndpointAddress(String address) {
        VASServiceProviderSOAP_address = address;
    }


    // Use to get a proxy class for BankServiceProviderSOAP
    private String BankServiceProviderSOAP_address = "http://localhost:8080/tesst/services/BankServiceProviderSOAP";

    public String getBankServiceProviderSOAPAddress() {
        return BankServiceProviderSOAP_address;
    }

    // The WSDD service name defaults to the port name.
    private String BankServiceProviderSOAPWSDDServiceName = "BankServiceProviderSOAP";

    public String getBankServiceProviderSOAPWSDDServiceName() {
        return BankServiceProviderSOAPWSDDServiceName;
    }

    public void setBankServiceProviderSOAPWSDDServiceName(String name) {
        BankServiceProviderSOAPWSDDServiceName = name;
    }

    public BankService getBankServiceProviderSOAP() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(BankServiceProviderSOAP_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getBankServiceProviderSOAP(endpoint);
    }

    public BankService getBankServiceProviderSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            BankServiceProviderSOAPStub _stub = new BankServiceProviderSOAPStub(portAddress, this);
            _stub.setPortName(getBankServiceProviderSOAPWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setBankServiceProviderSOAPEndpointAddress(String address) {
        BankServiceProviderSOAP_address = address;
    }


    // Use to get a proxy class for NotificationReceiverSOAP
    private String NotificationReceiverSOAP_address = "http://localhost:8080/tesst/services/NotificationReceiverSOAP";

    public String getNotificationReceiverSOAPAddress() {
        return NotificationReceiverSOAP_address;
    }

    // The WSDD service name defaults to the port name.
    private String NotificationReceiverSOAPWSDDServiceName = "NotificationReceiverSOAP";

    public String getNotificationReceiverSOAPWSDDServiceName() {
        return NotificationReceiverSOAPWSDDServiceName;
    }

    public void setNotificationReceiverSOAPWSDDServiceName(String name) {
        NotificationReceiverSOAPWSDDServiceName = name;
    }

    public NotificationReceiver getNotificationReceiverSOAP() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(NotificationReceiverSOAP_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getNotificationReceiverSOAP(endpoint);
    }

    public NotificationReceiver getNotificationReceiverSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            NotificationReceiverSOAPStub _stub = new NotificationReceiverSOAPStub(portAddress, this);
            _stub.setPortName(getNotificationReceiverSOAPWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setNotificationReceiverSOAPEndpointAddress(String address) {
        NotificationReceiverSOAP_address = address;
    }


    // Use to get a proxy class for INServiceProviderSOAP
    private String INServiceProviderSOAP_address = "http://localhost:8080/tesst/services/INServiceProviderSOAP";

    public String getINServiceProviderSOAPAddress() {
        return INServiceProviderSOAP_address;
    }

    // The WSDD service name defaults to the port name.
    private String INServiceProviderSOAPWSDDServiceName = "INServiceProviderSOAP";

    public String getINServiceProviderSOAPWSDDServiceName() {
        return INServiceProviderSOAPWSDDServiceName;
    }

    public void setINServiceProviderSOAPWSDDServiceName(String name) {
        INServiceProviderSOAPWSDDServiceName = name;
    }

    public INService getINServiceProviderSOAP() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(INServiceProviderSOAP_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getINServiceProviderSOAP(endpoint);
    }

    public INService getINServiceProviderSOAP(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            INServiceProviderSOAPStub _stub = new INServiceProviderSOAPStub(portAddress, this);
            _stub.setPortName(getINServiceProviderSOAPWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setINServiceProviderSOAPEndpointAddress(String address) {
        INServiceProviderSOAP_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (BillingService.class.isAssignableFrom(serviceEndpointInterface)) {
                com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.BillingServiceProviderSOAPStub _stub = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.BillingServiceProviderSOAPStub(new java.net.URL(BillingServiceProviderSOAP_address), this);
                _stub.setPortName(getBillingServiceProviderSOAPWSDDServiceName());
                return _stub;
            }
            if (VASService.class.isAssignableFrom(serviceEndpointInterface)) {
                VASServiceProviderSOAPStub _stub = new VASServiceProviderSOAPStub(new java.net.URL(VASServiceProviderSOAP_address), this);
                _stub.setPortName(getVASServiceProviderSOAPWSDDServiceName());
                return _stub;
            }
            if (BankService.class.isAssignableFrom(serviceEndpointInterface)) {
                BankServiceProviderSOAPStub _stub = new BankServiceProviderSOAPStub(new java.net.URL(BankServiceProviderSOAP_address), this);
                _stub.setPortName(getBankServiceProviderSOAPWSDDServiceName());
                return _stub;
            }
            if (NotificationReceiver.class.isAssignableFrom(serviceEndpointInterface)) {
                NotificationReceiverSOAPStub _stub = new NotificationReceiverSOAPStub(new java.net.URL(NotificationReceiverSOAP_address), this);
                _stub.setPortName(getNotificationReceiverSOAPWSDDServiceName());
                return _stub;
            }
            if (INService.class.isAssignableFrom(serviceEndpointInterface)) {
                INServiceProviderSOAPStub _stub = new INServiceProviderSOAPStub(new java.net.URL(INServiceProviderSOAP_address), this);
                _stub.setPortName(getINServiceProviderSOAPWSDDServiceName());
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
        if ("BillingServiceProviderSOAP".equals(inputPortName)) {
            return getBillingServiceProviderSOAP();
        }
        else if ("VASServiceProviderSOAP".equals(inputPortName)) {
            return getVASServiceProviderSOAP();
        }
        else if ("BankServiceProviderSOAP".equals(inputPortName)) {
            return getBankServiceProviderSOAP();
        }
        else if ("NotificationReceiverSOAP".equals(inputPortName)) {
            return getNotificationReceiverSOAP();
        }
        else if ("INServiceProviderSOAP".equals(inputPortName)) {
            return getINServiceProviderSOAP();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "UMarketServiceProvider");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "BillingServiceProviderSOAP"));
            ports.add(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "VASServiceProviderSOAP"));
            ports.add(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "BankServiceProviderSOAP"));
            ports.add(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "NotificationReceiverSOAP"));
            ports.add(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "INServiceProviderSOAP"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(String portName, String address) throws javax.xml.rpc.ServiceException {
        
if ("BillingServiceProviderSOAP".equals(portName)) {
            setBillingServiceProviderSOAPEndpointAddress(address);
        }
        else 
if ("VASServiceProviderSOAP".equals(portName)) {
            setVASServiceProviderSOAPEndpointAddress(address);
        }
        else 
if ("BankServiceProviderSOAP".equals(portName)) {
            setBankServiceProviderSOAPEndpointAddress(address);
        }
        else 
if ("NotificationReceiverSOAP".equals(portName)) {
            setNotificationReceiverSOAPEndpointAddress(address);
        }
        else 
if ("INServiceProviderSOAP".equals(portName)) {
            setINServiceProviderSOAPEndpointAddress(address);
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
