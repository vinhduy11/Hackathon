package com.mservice.momo.data;

import org.vertx.java.core.Handler;

import java.util.ArrayList;

/**
 * Created by concu on 5/20/14.
 */
public class FeeCollection {
    private static FeeDb feeDbStatic;
    private static ArrayList<FeeDb.Obj> feeList = new ArrayList<>();
    private static FeeCollection instance = new FeeCollection();
    public static void setFeeDb(FeeDb feeDb){
        feeDbStatic = feeDb;
    }
    private FeeCollection(){
    }
    public void initData(){
        loadFees();
    }
    private void loadFees(){
        if(feeList.size() == 0){
            feeDbStatic.getAll(new Handler<ArrayList<FeeDb.Obj>>() {
                @Override
                public void handle(ArrayList<FeeDb.Obj> ar) {
                    if(ar != null)
                        for (FeeDb.Obj o : ar) {
                            feeList.add(o);
                        }
                }
            });
        }
    }
    public ArrayList<FeeDb.Obj> getAll(){
        if(feeList.size() == 0){
            loadFees();
        }
        return feeList;
    }
    public static FeeCollection getInstance(){
        return instance;
    }

    public FeeDb.Obj findFeeBy(final String bankid, final int channel, final int trantype,final int inout_city){
        FeeDb.Obj feeObj = null;

        for (int i =0; i<feeList.size(); i++){
            FeeDb.Obj o = feeList.get(i);
            if(o.BANKID.equalsIgnoreCase(bankid)
                    && o.CHANNEL == channel
                    && o.TRANTYPE == trantype
                    && o.INOUT_CITY == inout_city){

                feeObj = o;
                break;
            }
        }
        if(feeObj == null) {
            feeObj = new FeeDb.Obj();
            feeObj.DYNAMIC_FEE = 0;
            feeObj.STATIC_FEE =0;
        }
        return feeObj;
    }

}
