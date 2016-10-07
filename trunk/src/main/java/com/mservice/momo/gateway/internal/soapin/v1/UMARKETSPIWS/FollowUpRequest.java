/**
 * FollowUpRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public abstract class FollowUpRequest  extends ServiceRequest  implements java.io.Serializable {
    private String serviceTransactionId;

    public FollowUpRequest() {
    }

    public FollowUpRequest(
           long transactionId,
           KeyValuePair[] extraParameters,
           String serviceTransactionId) {
        super(
            transactionId,
            extraParameters);
        this.serviceTransactionId = serviceTransactionId;
    }


    /**
     * Gets the serviceTransactionId value for this FollowUpRequest.
     * 
     * @return serviceTransactionId
     */
    public String getServiceTransactionId() {
        return serviceTransactionId;
    }


    /**
     * Sets the serviceTransactionId value for this FollowUpRequest.
     * 
     * @param serviceTransactionId
     */
    public void setServiceTransactionId(String serviceTransactionId) {
        this.serviceTransactionId = serviceTransactionId;
    }

    private Object __equalsCalc = null;
    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof FollowUpRequest)) return false;
        FollowUpRequest other = (FollowUpRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.serviceTransactionId==null && other.getServiceTransactionId()==null) || 
             (this.serviceTransactionId!=null &&
              this.serviceTransactionId.equals(other.getServiceTransactionId())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = super.hashCode();
        if (getServiceTransactionId() != null) {
            _hashCode += getServiceTransactionId().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(FollowUpRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "FollowUpRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("serviceTransactionId");
        elemField.setXmlName(new javax.xml.namespace.QName("", "serviceTransactionId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
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
