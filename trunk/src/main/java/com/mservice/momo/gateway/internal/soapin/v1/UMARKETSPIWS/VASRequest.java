/**
 * VASRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS;

public abstract class VASRequest  extends com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ServiceRequest  implements java.io.Serializable {
    private String product;

    private String reference1;

    private String reference2;

    public VASRequest() {
    }

    public VASRequest(
           long transactionId,
           com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair[] extraParameters,
           String product,
           String reference1,
           String reference2) {
        super(
            transactionId,
            extraParameters);
        this.product = product;
        this.reference1 = reference1;
        this.reference2 = reference2;
    }


    /**
     * Gets the product value for this VASRequest.
     * 
     * @return product
     */
    public String getProduct() {
        return product;
    }


    /**
     * Sets the product value for this VASRequest.
     * 
     * @param product
     */
    public void setProduct(String product) {
        this.product = product;
    }


    /**
     * Gets the reference1 value for this VASRequest.
     * 
     * @return reference1
     */
    public String getReference1() {
        return reference1;
    }


    /**
     * Sets the reference1 value for this VASRequest.
     * 
     * @param reference1
     */
    public void setReference1(String reference1) {
        this.reference1 = reference1;
    }


    /**
     * Gets the reference2 value for this VASRequest.
     * 
     * @return reference2
     */
    public String getReference2() {
        return reference2;
    }


    /**
     * Sets the reference2 value for this VASRequest.
     * 
     * @param reference2
     */
    public void setReference2(String reference2) {
        this.reference2 = reference2;
    }

    private Object __equalsCalc = null;
    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof VASRequest)) return false;
        VASRequest other = (VASRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.product==null && other.getProduct()==null) || 
             (this.product!=null &&
              this.product.equals(other.getProduct()))) &&
            ((this.reference1==null && other.getReference1()==null) || 
             (this.reference1!=null &&
              this.reference1.equals(other.getReference1()))) &&
            ((this.reference2==null && other.getReference2()==null) || 
             (this.reference2!=null &&
              this.reference2.equals(other.getReference2())));
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
        if (getProduct() != null) {
            _hashCode += getProduct().hashCode();
        }
        if (getReference1() != null) {
            _hashCode += getReference1().hashCode();
        }
        if (getReference2() != null) {
            _hashCode += getReference2().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(VASRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("urn:UMARKETSPIWS:com.mservice.momo.gateway.internal.soapin.v1", "VASRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("product");
        elemField.setXmlName(new javax.xml.namespace.QName("", "product"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("reference1");
        elemField.setXmlName(new javax.xml.namespace.QName("", "reference1"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("reference2");
        elemField.setXmlName(new javax.xml.namespace.QName("", "reference2"));
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
