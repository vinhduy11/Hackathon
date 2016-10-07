/**
 * NotifyRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public class NotifyRequest  extends com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ServiceRequest  implements java.io.Serializable {
    private String operationName;

    private int resultCode;

    private String resultCodeNamespace;

    public NotifyRequest() {
    }

    public NotifyRequest(
           long transactionId,
           KeyValuePair[] extraParameters,
           String operationName,
           int resultCode,
           String resultCodeNamespace) {
        super(
            transactionId,
            extraParameters);
        this.operationName = operationName;
        this.resultCode = resultCode;
        this.resultCodeNamespace = resultCodeNamespace;
    }


    /**
     * Gets the operationName value for this NotifyRequest.
     * 
     * @return operationName
     */
    public String getOperationName() {
        return operationName;
    }


    /**
     * Sets the operationName value for this NotifyRequest.
     * 
     * @param operationName
     */
    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }


    /**
     * Gets the resultCode value for this NotifyRequest.
     * 
     * @return resultCode
     */
    public int getResultCode() {
        return resultCode;
    }


    /**
     * Sets the resultCode value for this NotifyRequest.
     * 
     * @param resultCode
     */
    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }


    /**
     * Gets the resultCodeNamespace value for this NotifyRequest.
     * 
     * @return resultCodeNamespace
     */
    public String getResultCodeNamespace() {
        return resultCodeNamespace;
    }


    /**
     * Sets the resultCodeNamespace value for this NotifyRequest.
     * 
     * @param resultCodeNamespace
     */
    public void setResultCodeNamespace(String resultCodeNamespace) {
        this.resultCodeNamespace = resultCodeNamespace;
    }

    private Object __equalsCalc = null;
    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof NotifyRequest)) return false;
        NotifyRequest other = (NotifyRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.operationName==null && other.getOperationName()==null) || 
             (this.operationName!=null &&
              this.operationName.equals(other.getOperationName()))) &&
            this.resultCode == other.getResultCode() &&
            ((this.resultCodeNamespace==null && other.getResultCodeNamespace()==null) || 
             (this.resultCodeNamespace!=null &&
              this.resultCodeNamespace.equals(other.getResultCodeNamespace())));
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
        if (getOperationName() != null) {
            _hashCode += getOperationName().hashCode();
        }
        _hashCode += getResultCode();
        if (getResultCodeNamespace() != null) {
            _hashCode += getResultCodeNamespace().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(NotifyRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "NotifyRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("operationName");
        elemField.setXmlName(new javax.xml.namespace.QName("", "operationName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("resultCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "resultCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("resultCodeNamespace");
        elemField.setXmlName(new javax.xml.namespace.QName("", "resultCodeNamespace"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
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
