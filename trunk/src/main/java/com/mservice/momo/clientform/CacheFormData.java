package com.mservice.momo.clientform;

import com.mservice.momo.data.BillInfoService;

import java.util.HashMap;

/**
 * Created by concu on 11/11/14.
 */
public class CacheFormData {
    private static HashMap<String,BillInfoService> data;
    static {
        data = new HashMap<>();
    }
    /*public static void put(String key, BillInfoService bis){
        data.put(key,bis);
    }*/
    public static BillInfoService get(String key){
        return data.get(key);
    }
    public static void remove(String key){
        if(data.containsKey(key)){
            data.remove(key);
        }
    }
    public static boolean contain (String key){
        return data.containsKey(key);
    }
}
