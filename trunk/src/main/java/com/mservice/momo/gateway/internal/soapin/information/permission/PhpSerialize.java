package com.mservice.momo.gateway.internal.soapin.information.permission;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by khoivu on 05/11/2015.
 */
public class PhpSerialize {

    /**
     * Converts a Java integer to PHP serialized notation.
     * @param javaInt
     * @return
     */
    public static String serialize(Integer javaInt) {
        if (javaInt == null) {
            return "N;";
        }
        return "i:" + javaInt.toString() + ";";
    }

    /**
     * Converts a java double to PHP serialized notation.
     * @param javaDouble
     * @return
     */
    public static String serialize(Double javaDouble) {
        if (javaDouble == null) {
            return "N;";
        }
        return "d:" + javaDouble.toString() + ";";
    }

    public static String serialize(Boolean javaBoolean) {
        if (javaBoolean == null) {
            return "N;";
        }
        return "b:" + (javaBoolean.equals(Boolean.TRUE) ? 1 : 0) + ";";
    }

    /**
     * Converts a Java string into a PHP serialized notation.
     * @param javaString
     * @return
     */
    public static String serialize(String javaString) {
        if ( javaString == null) {
            return "N;";
        }
        return "s:" + javaString.length() + ":\"" + javaString + "\";";
    }

    /**
     * Converts a Java list into a PHP serialized notation
     * @param aList
     * @return
     */
    public static String serialize(List<Object> aList) {
        if (aList == null) {
            return "N;";
        }
        StringBuffer buf = new StringBuffer();
        buf.append("a:").append(aList.size()).append(":{");
        int offset = 0;
        for (Iterator<Object> it = aList.iterator(); it.hasNext();) {
            buf.append(serialize(new Integer(offset ++)));
            Object value = it.next();
            buf.append(serialize(value));
        }
        buf.append("};");
        return buf.toString();
    }

    public static String serializeMap(Map<Object,Object> aMap) {
        if (aMap == null) {
            return "N;";
        }
        StringBuffer buf = new StringBuffer();
        buf.append("a:").append(aMap.size()).append(":{");
        for (Iterator<Object> it = aMap.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            buf.append(serialize(key));
            Object value = aMap.get(key);
            buf.append(serialize(value));
        }
        buf.append("};");
        return buf.toString();
    }


    public static String serialize(Object value) {
        if (value == null) {
            return "N;";
        }
        if (value instanceof Integer) {
            return serialize((Integer) value);
        }

        if (value instanceof Double) {
            return serialize((Double) value);
        }

        if (value instanceof Boolean) {
            return serialize((Boolean) value);
        }

        if (value instanceof List<?>) {
            return serialize((List<?>) value);
        }

        if (value instanceof Map<?,?>) {
            return serialize((Map<?,?>) value);
        }

        return serialize((String) value);

    }

}