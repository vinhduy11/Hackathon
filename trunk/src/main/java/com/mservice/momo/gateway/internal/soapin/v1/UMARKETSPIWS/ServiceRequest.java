/**
 * ServiceRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public abstract class ServiceRequest  implements java.io.Serializable {
    private long transactionId;

    private KeyValuePair[] extraParameters;

    public ServiceRequest() {
    }

    public ServiceRequest(
           long transactionId,
           KeyValuePair[] extraParameters) {
           this.transactionId = transactionId;
           this.extraParameters = extraParameters;
    }


    /**
     * Gets the transactionId value for this ServiceRequest.
     * 
     * @return transactionId
     */
    public long getTransactionId() {
        return transactionId;
    }


    /**
     * Sets the transactionId value for this ServiceRequest.
     * 
     * @param transactionId
     */
    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }


    /**
     * Gets the extraParameters value for this ServiceRequest.
     * 
     * @return extraParameters
     */
    public KeyValuePair[] getExtraParameters() {
        return extraParameters;
    }


    /**
     * Sets the extraParameters value for this ServiceRequest.
     * 
     * @param extraParameters
     */
    public void setExtraParameters(KeyValuePair[] extraParameters) {
        this.extraParameters = extraParameters;
    }

    private Object __equalsCalc = null;
    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof ServiceRequest)) return false;
        ServiceRequest other = (ServiceRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.transactionId == other.getTransactionId() &&
            ((this.extraParameters==null && other.getExtraParameters()==null) || 
             (this.extraParameters!=null &&
              java.util.Arrays.equals(this.extraParameters, other.getExtraParameters())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        _hashCode += new Long(getTransactionId()).hashCode();
        if (getExtraParameters() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getExtraParameters());
                 i++) {
                Object obj = java.lang.reflect.Array.get(getExtraParameters(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ServiceRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "ServiceRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("transactionId");
        elemField.setXmlName(new javax.xml.namespace.QName("", "transactionId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("extraParameters");
        elemField.setXmlName(new javax.xml.namespace.QName("", "extraParameters"));
        elemField.setXmlType(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "KeyValuePair"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("", "keyValuePair"));
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           String mechType,
           Class _javaType,
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           String mechType,
           Class _javaType,
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
