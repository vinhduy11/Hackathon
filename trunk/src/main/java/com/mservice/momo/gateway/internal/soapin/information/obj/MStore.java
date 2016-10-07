package com.mservice.momo.gateway.internal.soapin.information.obj;

import com.mservice.momo.data.AgentsDb;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duyhv on 12/7/2015.
 */
public class MStore {

    public int ID;
    public String NAME;
    public long OWNER_ID;
    public double LATITUDE;
    public double LONGITUDE;
    public String CONTACT_NUMBER;
    public String MOMO_NUMBER;
    public String ADDRESS;
    public String STREET;
    public int WARD_ID;
    public String WARD;
    public int DISTRICT_ID;
    public String DISTRICT;
    public int CITY_ID;
    public String CITY;
    public int AREA_ID;
    public String AREA;
    public int STATUS;
    public double DISTANCE;

    public MStore() {}

    public static MStore fromStoreInfo(AgentsDb.StoreInfo st) {
        MStore store = new MStore();
        try {
            store.ID = Integer.valueOf(st.rowCoreId);
        } catch (Exception e) {
            store.ID = 0;
        }
        store.NAME = st.storeName;
        store.OWNER_ID = st.rowCoreId;
        store.LATITUDE = st.loc.Lat;
        store.LONGITUDE = st.loc.Lng;
        store.CONTACT_NUMBER = st.phone;
        store.MOMO_NUMBER = st.momoNumber;
        store.ADDRESS = st.address + " " + st.street;
        store.STREET = st.street;
        try {
            store.WARD_ID = Integer.valueOf(st.ward);
        } catch (Exception e) {
            store.WARD_ID = 0;
        }
        store.WARD = st.wardname;
        store.DISTRICT_ID = st.districtId;
        store.DISTRICT = st.districtname;
        store.CITY_ID = st.cityId;
        store.CITY = st.cityname;
        store.AREA_ID = st.areaId;
        store.AREA = st.areaId + "";
        store.STATUS = st.status;
        return store;
    }

    public static List<MStore> fromList(List<AgentsDb.StoreInfo> list) {
        List<MStore> stores = new ArrayList<>();
        for(AgentsDb.StoreInfo i : list) {
            stores.add(fromStoreInfo(i));
        }
        return stores;
    }

    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        float dist = (float) (earthRadius * c);

        return dist;
    }
}
