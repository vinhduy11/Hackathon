/**
 * ServiceResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public abstract class ServiceResponse  implements java.io.Serializable {
    private ResultCode resultCode;

    private String errorMessage;

    private KeyValuePair[] extraParameters;

    public ServiceResponse() {
    }

    public ServiceResponse(
           ResultCode resultCode,
           String errorMessage,
           KeyValuePair[] extraParameters) {
           this.resultCode = resultCode;
           this.errorMessage = errorMessage;
           this.extraParameters = extraParameters;
    }


    /**
     * Gets the resultCode value for this ServiceResponse.
     * 
     * @return resultCode
     */
    public ResultCode getResultCode() {
        return resultCode;
    }


    /**
     * Sets the resultCode value for this ServiceResponse.
     * 
     * @param resultCode
     */
    public void setResultCode(ResultCode resultCode) {
        this.resultCode = resultCode;
    }


    /**
     * Gets the errorMessage value for this ServiceResponse.
     * 
     * @return errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }


    /**
     * Sets the errorMessage value for this ServiceResponse.
     * 
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }


    /**
     * Gets the extraParameters value for this ServiceResponse.
     * 
     * @return extraParameters
     */
    public KeyValuePair[] getExtraParameters() {
        return extraParameters;
    }


    /**
     * Sets the extraParameters value for this ServiceResponse.
     * 
     * @param extraParameters
     */
    public void setExtraParameters(KeyValuePair[] extraParameters) {
        this.extraParameters = extraParameters;
    }

    private Object __equalsCalc = null;
    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof ServiceResponse)) return false;
        ServiceResponse other = (ServiceResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.resultCode==null && other.getResultCode()==null) || 
             (this.resultCode!=null &&
              this.resultCode.equals(other.getResultCode()))) &&
            ((this.errorMessage==null && other.getErrorMessage()==null) || 
             (this.errorMessage!=null &&
              this.errorMessage.equals(other.getErrorMessage()))) &&
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
        if (getResultCode() != null) {
            _hashCode += getResultCode().hashCode();
        }
        if (getErrorMessage() != null) {
            _hashCode += getErrorMessage().hashCode();
        }
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
        new org.apache.axis.description.TypeDesc(ServiceResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "ServiceResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("resultCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "resultCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "ResultCode"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("errorMessage");
        elemField.setXmlName(new javax.xml.namespace.QName("", "errorMessage"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
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
