package com.mservice.momo.vertx;

import com.mservice.momo.data.*;
import com.mservice.momo.data.m2mpromotion.MerchantPromosDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.io.*;
import java.util.*;

/**
 * Created by admin on 2/11/14.
 */
public class MainVerticle_bk extends Verticle {
    //load config first
    public static org.vertx.java.core.logging.Logger logger;
    public static JsonObject globalCfg = null;


    //support function
    private void getPhonesInfo() {
        final PhonesDb phonesDb = new PhonesDb(vertx.eventBus(), logger);

        phonesDb.getPhoneByMonth("2014-09-04 25:21:11", "yyyy-MM-dd HH:mm:ss", new Handler<ArrayList<PhonesDb.Obj>>() {
            @Override
            public void handle(ArrayList<PhonesDb.Obj> objs) {
                if (objs != null && objs.size() > 0) {

                    String str = "";
                    for (int i = 0; i < objs.size(); i++) {
                        str += "0" + objs.get(i).number + "$" +
                                objs.get(i).name + "$" +
                                "0" + objs.get(i).referenceNumber + "$" +
                                objs.get(i).lastImei + "$" +
                                objs.get(i).phoneOs + "$" +
                                objs.get(i).pushToken + "$" +
                                Misc.dateVNFormatWithTime(objs.get(i).createdDate) + "$" +
                                "\n";
                    }

                    BufferedWriter writer = null;
                    try {

                        //create a temporary file
                        java.io.File logFile = new java.io.File("/home/concu/T9.txt");
                        writer = new BufferedWriter(new FileWriter(logFile));
                        writer.write(str);
                        writer.flush();

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            // Close the writer regardless of what happens...
                            writer.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        });
    }

    private void updatePersonalId() {
        final VcbCmndRecs vcbCmndRecs = new VcbCmndRecs(vertx.eventBus(), logger);
        vcbCmndRecs.findAll(new Handler<ArrayList<VcbCmndRecs.Obj>>() {
            @Override
            public void handle(final ArrayList<VcbCmndRecs.Obj> objs) {

                if (objs != null && objs.size() > 0) {

                    final PhonesDb phonesDb = new PhonesDb(vertx.eventBus(), logger);

                    final int size = objs.size();
                    final JsonObject joMax = new JsonObject();
                    joMax.putNumber("pos", 0);

                    vertx.setPeriodic(400, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            final int pos = joMax.getInteger("pos");
                            if (pos < size) {
                                VcbCmndRecs.Obj o = objs.get(pos);
                                JsonObject up = new JsonObject();
                                up.putString(colName.PhoneDBCols.BANK_PERSONAL_ID, o.cardid);

                                phonesDb.updatePartialNoReturnObj(DataUtil.strToInt(o.number), up, new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {
                                        logger.info("result " + aBoolean);
                                        joMax.putNumber("pos", (pos + 1));
                                    }
                                });
                            } else {
                                vertx.cancelTimer(aLong);
                            }
                        }
                    });

                }
            }
        });
    }


    private void updateIsAgent() {

        AgentsDb agentsDb = new AgentsDb(vertx.eventBus(), logger);

        agentsDb.getMomoPhone(new Handler<ArrayList<Integer>>() {
            @Override
            public void handle(final ArrayList<Integer> pgCodes) {
                if (pgCodes != null && pgCodes.size() > 0) {

                    final int size = pgCodes.size();

                    final JsonObject joMax = new JsonObject();
                    joMax.putNumber("pos", 0);

                    final PhonesDb phonesDb = new PhonesDb(vertx.eventBus(), logger);

                    vertx.setPeriodic(300, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            final int pos = joMax.getInteger("pos");
                            if (pos < size) {

                                final int phone = pgCodes.get(pos);

                                JsonObject up = new JsonObject();
                                up.putNumber(colName.PhoneDBCols.NUMBER, phone);
                                up.putBoolean(colName.PhoneDBCols.IS_AGENT, true);

                                phonesDb.updatePartial(phone, up, new Handler<PhonesDb.Obj>() {
                                    @Override
                                    public void handle(final PhonesDb.Obj o) {
                                        joMax.putNumber("pos", (pos + 1));
                                        logger.info("PHONE : " + phone + " , pos " + pos);
                                    }
                                });
                            } else {
                                vertx.cancelTimer(aLong);
                            }
                        }
                    });

                }
            }
        });

        /*{
            "_id" : ObjectId("5456de5c326dcd32fa1f82fd"),
                "number" : 6022,
                "push_token" : "893baaf7-8423-45d5-8217-85fb98cc781a",
                "imei_key" : "893baaf7-8423-45d5-8217-85fb98cc781a",
                "name" : "PG_Cafe22",
                "os" : "ANDROID",
                "isinviter" : true,
                "create_date" : NumberLong(1414980293459),
                "is_actived" : true,
                "is_reged" : true,
                "is_named" : true,
                "is_setup" : true,
                "del" : false,
                "login_cnt" : 9,
                "pin" : "MTExMTEx",
                "last_imei" : "893baaf7-8423-45d5-8217-85fb98cc781a"
        }*/

    }

    private void getPhoneLessThanTime(long lessThanTime) {
        final PhonesDb phonesDb = new PhonesDb(vertx.eventBus(), logger);

        phonesDb.getPhoneLessThanTime(lessThanTime, new Handler<ArrayList<PhonesDb.Obj>>() {
            @Override
            public void handle(ArrayList<PhonesDb.Obj> objs) {
                if (objs != null && objs.size() > 0) {

                    String str = "";
                    for (int i = 0; i < objs.size(); i++) {
                        str += "0" + objs.get(i).number + "$" +
                                //objs.get(i).name + "$" +
                                //"0" +objs.get(i).referenceNumber + "$" +
                                //objs.get(i).lastImei + "$" +
                                objs.get(i).phoneOs + "$" +
                                //objs.get(i).pushToken + "$" +
                                //Misc.dateVNFormatWithTime(objs.get(i).createdDate) + "$" +
                                "\n";
                    }

                    BufferedWriter writer = null;
                    try {

                        //create a temporary file
                        java.io.File logFile = new java.io.File("/home/concu/lt3t.txt");
                        writer = new BufferedWriter(new FileWriter(logFile));
                        writer.write(str);
                        writer.flush();

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            // Close the writer regardless of what happens...
                            writer.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        });
    }

    private void getVotedList(String collName, String serviceid) {
        final CDHH cdhh = new CDHH(vertx, logger);
        cdhh.findAll(collName, serviceid, new Handler<ArrayList<CDHH.Obj>>() {
            @Override
            public void handle(ArrayList<CDHH.Obj> objs) {

                if (objs != null && objs.size() > 0) {

                    for (int i = 0; i < objs.size(); i++) {

                        CDHH.Obj ob = objs.get(i);

                        cdhh.save(ob, "bnhv2", new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                logger.info("A");
                            }
                        });
                    }

                }
            }
        });
    }


    private void getVotedListWithFiller(String collName, JsonObject matcher) {
        final CDHH cdhh = new CDHH(vertx, logger);

        cdhh.findAllWithFilter(collName, matcher, new Handler<ArrayList<CDHH.Obj>>() {
            @Override
            public void handle(final ArrayList<CDHH.Obj> objs) {

                final JsonObject jo = new JsonObject();
                jo.putNumber("pos", 0);

                if (objs != null && objs.size() > 0) {

                    final int total = objs.size();

                    vertx.setPeriodic(1000, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {

                            final int i = jo.getInteger("pos");

                            logger.info("cur pos " + i);

                            if (i <= (total - 1)) {
                                CDHH.Obj ob = objs.get(i);
                                ob.serviceid = "capdoihoanhao";
                                cdhh.save(ob, "t8", new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {

                                        logger.info("save result " + aBoolean);
                                        jo.putNumber("pos", i + 1);
                                    }
                                });
                            } else {
                                logger.info("cancel timer");
                                vertx.cancelTimer(aLong);
                            }
                        }
                    });
                }
            }
        });
    }


    private void getPhoneHasReferal(long greaterOrEqualTime) {
        final PhonesDb phonesDb = new PhonesDb(vertx.eventBus(), logger);

        phonesDb.getPhoneHasReferal(greaterOrEqualTime, new Handler<ArrayList<PhonesDb.Obj>>() {
            @Override
            public void handle(ArrayList<PhonesDb.Obj> objs) {
                if (objs != null && objs.size() > 0) {

                    String str = "";
                    for (int i = 0; i < objs.size(); i++) {
                        str += "0" + objs.get(i).number + "$" +
                                //objs.get(i).name + "$" +
                                //"0" +objs.get(i).referenceNumber + "$" +
                                //objs.get(i).lastImei + "$" +
                                objs.get(i).referenceNumber + "$" +
                                objs.get(i).createdDate + "$" +
                                Misc.dateVNFormatWithTime(objs.get(i).createdDate) + "$" +
                                "\n";
                    }

                    BufferedWriter writer = null;
                    try {

                        //create a temporary file
                        java.io.File logFile = new java.io.File("/home/concu/referal_02.txt");
                        writer = new BufferedWriter(new FileWriter(logFile));
                        writer.write(str);
                        writer.flush();

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            // Close the writer regardless of what happens...
                            writer.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        });
    }


    private void getMerchantCoffee() {
        final MerchantPromosDb merchantPromosDb = new MerchantPromosDb(vertx.eventBus(), logger);
        merchantPromosDb.getAllMerchant(new Handler<List<MerchantPromosDb.Obj>>() {
            @Override
            public void handle(List<MerchantPromosDb.Obj> objs) {
                if (objs != null && objs.size() > 0) {

                    for (int i = 0; i < objs.size(); i++) {

                        MerchantPromosDb.Obj o = objs.get(i);
                        if (!o.numList.contains(o.number)) {
                            o.numList.add(o.number);
                        }
                        o.program = "coffee";
                        o.group = "hcm";

                        merchantPromosDb.upsertMerchant(o, new Handler<Boolean>() {
                            @Override
                            public void handle(Boolean aBoolean) {
                                logger.info(aBoolean);
                            }
                        });
                    }

                    /*String str ="";
                    for(int i = 0; i < objs.size(); i++){
                        str +=  "0" + objs.get(i).number + "$" +
                                objs.get(i).name + "$" +
                                "0" +objs.get(i).totalCode + "$" +
                                objs.get(i).maxCode + "$" +
                                objs.get(i).usedCode + "$" +

                                objs.get(i).totalVal + "$" +
                                objs.get(i).maxVal + "$" +
                                objs.get(i).maxTranPerDay + "$" +
                                objs.get(i).threshold + "$" +
                                "\n";
                    }

                    BufferedWriter writer = null;
                    try {

                        //create a temporary file
                        java.io.File logFile = new java.io.File("/home/concu/Coffee.txt");
                        writer = new BufferedWriter(new FileWriter(logFile));
                        writer.write(str);
                        writer.flush();

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            // Close the writer regardless of what happens...
                            writer.close();
                        } catch (Exception e) {
                        }
                    }*/
                }
            }
        });

    }

    private void getActivedStoresForClient() {

        Track123PayNotifyDb track123PayNotifyDb = new Track123PayNotifyDb(vertx.eventBus(), logger);
        track123PayNotifyDb.findAlll(new Handler<ArrayList<String>>() {
            @Override
            public void handle(ArrayList<String> objs) {
                if (objs.size() > 0) {
                    String str = "";
                    for (int i = 0; i < objs.size(); i++) {
                        str += objs.get(i) + "\n";
                    }

                    BufferedWriter writer = null;
                    try {

                        //create a temporary file
                        java.io.File logFile = new java.io.File("/home/concu/123pcc.txt");
                        writer = new BufferedWriter(new FileWriter(logFile));
                        writer.write(str);
                        writer.flush();

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            // Close the writer regardless of what happens...
                            writer.close();
                        } catch (Exception e) {
                        }
                    }

                }
            }
        });


        /*final AgentsDb agentsDb = new AgentsDb(vertx.eventBus(),logger);
        agentsDb.getActivedLocs(new Handler<ArrayList<AgentsDb.StoreInfo>>() {
            @Override
            public void handle(ArrayList<AgentsDb.StoreInfo> objs) {
                if(objs !=null && objs.size() > 0){

                    String str ="";
                    for(int i = 0; i < objs.size(); i++){
                        str +=  buildStoreOneRow(objs.get(i));
                    }

                    BufferedWriter writer = null;
                    try {

                        //create a temporary file
                        java.io.File logFile = new java.io.File("/home/concu/clientStore.txt");
                        writer = new BufferedWriter(new FileWriter(logFile));
                        writer.write(str);
                        writer.flush();

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            // Close the writer regardless of what happens...
                            writer.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        });*/
    }

    private void readFile(String filePath) {

        final ArrayList<String> refList = new ArrayList<>();

        try {
            // Open the file that is the first
            // command line parameter
            FileInputStream fstream = new FileInputStream(filePath);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                refList.add("0" + strLine.trim());
            }
            //Close the input stream
            in.close();
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        if (refList == null || refList.size() == 0) {
            return;
        }

        CDHHPayBack cdhhPayBack = new CDHHPayBack(vertx.eventBus(), logger);
        cdhhPayBack.getRecByList(refList, "remix", new Handler<ArrayList<CDHHPayBack.Obj>>() {
            @Override
            public void handle(ArrayList<CDHHPayBack.Obj> objs) {
                if (objs == null || objs.size() == 0) {
                    return;
                }
                //SDT - tin nhan
                final String tmp = "%s - %s";
                String s = "";
                for (int i = 0; i < objs.size(); i++) {
                    CDHHPayBack.Obj o = objs.get(i);
                    s += String.format(tmp, o.number, o.voteAmount) + "\n";
                }

                BufferedWriter writer = null;
                try {

                    //create a temporary file
                    java.io.File logFile = new java.io.File("/home/concu/remix_20150330.txt");
                    writer = new BufferedWriter(new FileWriter(logFile));
                    writer.write(s);
                    writer.flush();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        // Close the writer regardless of what happens...
                        writer.close();
                    } catch (Exception e) {
                    }
                }

            }
        });



        /*phonesDb.getByNumList(refList,new Handler<ArrayList<PhonesDb.Obj>>() {
            @Override
            public void handle(final ArrayList<PhonesDb.Obj> objs) {

                logger.info("total phones will be updated " + refList.size());

                if(objs == null && objs.size() == 0){
                    return;
                }

                final  JsonObject pos = new JsonObject();
                final int max = refList.size();
                pos.putNumber("p",0);

                final String tmp ="SDT: %s - INVITER: %s - TIME: %s : RESULT: %s";

                vertx.setPeriodic(500,new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {

                        final int fi=pos.getInteger("p");
                        logger.info("pos " + fi);
                        if(fi <max){

                            PhonesDb.Obj pObj = objs.get(fi);

                            JsonObject joUp = new JsonObject();

                            if(pObj.referenceNumber >= 6100 && pObj.referenceNumber <= 6200){
                                joUp.putString(colName.PhoneDBCols.INVITER, pObj.referenceNumber + "");
                            }else{
                                joUp.putString(colName.PhoneDBCols.INVITER, "0" + pObj.referenceNumber );
                            }

                            joUp.putNumber(colName.PhoneDBCols.INVITE_TIME, pObj.createdDate);

                            if(pObj.referenceNumber > 0 && pObj.createdDate > 0){

                                phonesDb.updatePartial(pObj.number, joUp, new Handler<PhonesDb.Obj>() {
                                    @Override
                                    public void handle(PhonesDb.Obj obj) {
                                        logger.info("UPDATED NUMBER : " + obj.number);
                                        pos.putNumber("p",fi + 1);
                                    }
                                });
                            }

                        }else{
                            vertx.cancelTimer(aLong);
                        }
                    }
                });


            }
        });*/

        logger.info("total phones will be updated " + refList.size());


    }

    public void updateVCBCMND() {

        final VcbCmndRecs vcbCmndRecs = new VcbCmndRecs(vertx.eventBus(), logger);

        final ArrayList<String> cardidList = new ArrayList<>();
        try {
            FileInputStream fstream = new FileInputStream("/home/concu/Desktop/all-remix.txt");
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                cardidList.add(strLine.trim());

            }
            //Close the input stream
            in.close();
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        final HashMap<String, CdhhPB> hashMapOrg = new HashMap<>();

        ArrayList<String> phoneList = new ArrayList<>();

        for (String s : cardidList) {
            String[] arr = s.split(";");
            String number = arr[0].trim();
            int count = (DataUtil.strToInt(arr[1].trim()) / 5000);
            hashMapOrg.put(number, new CdhhPB(number, count));
            phoneList.add(number);
        }

        final CDHHPayBack cdhhPayBack = new CDHHPayBack(vertx.eventBus(), logger);

        cdhhPayBack.getRecByList(phoneList, "remix", new Handler<ArrayList<CDHHPayBack.Obj>>() {
            @Override
            public void handle(ArrayList<CDHHPayBack.Obj> objs) {

                if (objs != null && objs.size() > 0) {
                    for (CDHHPayBack.Obj o : objs) {
                        CdhhPB ch = hashMapOrg.get(o.number);
                        //lay phan giao
                        ch.count = ch.count + DataUtil.strToInt(o.voteAmount + "");
                    }

                    final Queue<CdhhPB> queue = new ArrayDeque<CdhhPB>();

                    //todo
                    for (String s : hashMapOrg.keySet()) {
                        queue.add(hashMapOrg.get(s));
                    }

                    updateRemix(queue, cdhhPayBack);

                    /*vertx.setPeriodic(250,new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            final int pos = jo.getInteger("p");

                            CDHHPayBack.Obj o = queue.get(pos);

                            if(pos < queue.size()){

                                //cap nhat len vote

                                cdhhPayBack.incVotedAmount(o.number
                                        ,"remix"
                                        ,DataUtil.strToInt(o.voteAmount+"")
                                        ,new Handler<Boolean>() {
                                    @Override
                                    public void handle(Boolean aBoolean) {
                                        jo.putNumber("p", pos + 1);
                                    }
                                });
                            }else{

                                vertx.cancelTimer(aLong);
                            }

                        }
                    });*/

                }
            }
        });
    }

    private void updateRemix(final Queue<CdhhPB> queue, final CDHHPayBack cdhhPayBack) {
        if (queue.size() == 0) {
            logger.info("DONE");
            return;
        }
        final CdhhPB ch = queue.poll();
        if (ch == null) {
            updateRemix(queue, cdhhPayBack);

        } else {
            cdhhPayBack.incVotedAmount(ch.number
                    , "remix"
                    , ch.count
                    , new Handler<Boolean>() {
                @Override
                public void handle(Boolean aBoolean) {
                    String tmp = "Number: %s - Count: %s";
                    logger.info(String.format(tmp, ch.number, ch.count));
                    updateRemix(queue, cdhhPayBack);
                }
            });
        }
    }

    public void getPhonesByMatcher(JsonObject matcher, JsonObject fields) {

        final PhonesDb phonesDb = new PhonesDb(vertx.eventBus(), logger);
        phonesDb.getPhonesByMatcher(matcher, fields, new Handler<ArrayList<PhonesDb.Obj>>() {
            @Override
            public void handle(final ArrayList<PhonesDb.Obj> objs) {

                if (objs == null || objs.size() == 0) {
                    return;
                }

                final JsonObject pos = new JsonObject();
                final int max = objs.size();
                pos.putNumber("p", 0);

                vertx.setPeriodic(250, new Handler<Long>() {
                    @Override
                    public void handle(Long aLong) {

                        final int fi = pos.getInteger("p");
                        logger.info("pos " + fi);
                        if (fi < max) {

                            PhonesDb.Obj pObj = objs.get(fi);

                            JsonObject joUp = new JsonObject();

                            joUp.putNumber(colName.PhoneDBCols.INVITE_TIME, pObj.createdDate);

                            if (pObj.createdDate > 0) {

                                phonesDb.updatePartial(pObj.number, joUp, new Handler<PhonesDb.Obj>() {
                                    @Override
                                    public void handle(PhonesDb.Obj obj) {
                                        logger.info("UPDATED NUMBER : " + obj.number);
                                        pos.putNumber("p", fi + 1);
                                    }
                                });
                            } else {
                                pos.putNumber("p", fi + 1);
                            }

                        } else {
                            vertx.cancelTimer(aLong);
                        }
                    }
                });
            }
        });
    }

    public void updatePayBackCDHH2014() {
        CDHH cdhh = new CDHH(vertx, logger);
        final ArrayList<CC> hashMap = new ArrayList<>();

        final HashMap<String, Long> hashMap1 = new HashMap<>();

        try {
            FileInputStream fstream = new FileInputStream("/home/concu/Desktop/payback");
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {

                String[] arr = strLine.split(";");
                String phone = "0" + DataUtil.strToInt(arr[0].trim());
                long amt = DataUtil.strToInt(arr[1].trim()) / 5000;

                Long l = hashMap1.get(phone);
                if (l == null) {
                    hashMap1.put(phone, amt);
                } else {
                    l += amt;
                    hashMap1.put(phone, l);
                }
            }
            //Close the input stream
            in.close();
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        for (String s : hashMap1.keySet()) {
            hashMap.add(new CC(s, hashMap1.get(s)));
        }

        final CDHHPayBack cdhhPayBack = new CDHHPayBack(vertx.eventBus(), logger);

        final JsonObject jo = new JsonObject();
        jo.putNumber("p", 0);

        vertx.setPeriodic(250, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                final int pos = jo.getInteger("p");
                CC c = hashMap.get(pos);
                final String s = c.acc;
                final long paiedBackAmt = c.amt;
                if (pos < hashMap.size()) {
                    cdhhPayBack.updatePaidBackTickets(s, paiedBackAmt, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            logger.info("Result : " + aBoolean + "phone " + s + " amount : " + paiedBackAmt);
                            jo.putNumber("p", pos + 1);
                        }
                    });
                } else {

                    vertx.cancelTimer(aLong);
                }

            }
        });
    }

    private String buildStoreOneRow(AgentsDb.StoreInfo s) {

        String row = "\"INSERT INTO tSTORES(OWNER"
                + ",PHONE"
                + ",ADDRESS"
                + ",STREET"
                + ",STREET_NORM"
                + ",WARD"
                + ",WARD_NORM"
                + ",NAME"
                + ",NAME_NORM"
                + ",LNG"
                + ",LAT"
                + ",CITY_ID"
                + ",DISTRICT_ID"
                + ",AREA_ID"
                + ",CID"
                + ",LAST_UPDATE"
                + ",MOMO_PHONE"
                + ",DELETED) VALUES ('" + s.name + "'"
                + "," + s.phone + ""
                + ",'" + s.address + "'"
                + ",'" + s.street + "'"
                + ",'" + Misc.removeAccent(s.street).toLowerCase() + "'"
                + ",'" + s.ward + "'"
                + ",'" + s.ward + "'"
                + ",'" + s.storeName + "'"
                + ",'" + Misc.removeAccent(s.storeName).toLowerCase() + "'"
                + "," + s.loc.Lng
                + "," + s.loc.Lat
                + "," + s.cityId
                + "," + s.districtId
                + "," + s.areaId
                + "," + s.rowCoreId
                + "," + s.last_update_time
                + ",'" + s.momoNumber + "'"
                + ",0);\"," + "\n";
        return row;
    }

    public void createAccountPGCafe() {
        final PhonesDb phonesDb = new PhonesDb(vertx.eventBus(), logger);
        int base = 6000;
        final ArrayList<Integer> phoneList = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            int tmp = base + i;
            phoneList.add(tmp);
        }
        final JsonObject pos = new JsonObject();
        final int max = phoneList.size();
        pos.putNumber("p", 0);

        vertx.setPeriodic(300, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {

                final int fi = pos.getInteger("p");
                logger.info("pos " + fi);
                if (fi < max) {

                    String guid = UUID.randomUUID().toString();
                    JsonObject jo = new JsonObject();
                    jo.putNumber(colName.PhoneDBCols.NUMBER, phoneList.get(fi));
                    jo.putString(colName.PhoneDBCols.PUSH_TOKEN, guid);
                    jo.putString(colName.PhoneDBCols.IMEI_KEY, guid);
                    jo.putString(colName.PhoneDBCols.NAME, "PG_Cafe" + fi);
                    jo.putString(colName.PhoneDBCols.PHONE_OS, "ANDROID");
                    jo.putBoolean(colName.PhoneDBCols.IS_INVITER, true);
                    jo.putNumber(colName.PhoneDBCols.CREATED_DATE, System.currentTimeMillis());
                    jo.putBoolean(colName.PhoneDBCols.IS_ACTIVED, true);
                    jo.putBoolean(colName.PhoneDBCols.IS_REGED, true);
                    jo.putBoolean(colName.PhoneDBCols.IS_NAMED, true);
                    jo.putBoolean(colName.PhoneDBCols.IS_SETUP, true);
                    jo.putBoolean(colName.PhoneDBCols.DELETED, false);
                    jo.putNumber(colName.PhoneDBCols.MAX_LOGIN_COUNT, 9);
                    jo.putString(colName.PhoneDBCols.PIN, DataUtil.encode("111111"));
                    jo.putString(colName.PhoneDBCols.LAST_IMEI, guid);

                    phonesDb.updatePartialNoReturnObj(phoneList.get(fi), jo, new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean aBoolean) {
                            logger.info(phoneList.get(fi) + " - " + aBoolean);

                            pos.putNumber("p", fi + 1);
                        }
                    });
                } else {
                    vertx.cancelTimer(aLong);
                }
            }
        });

    }

    @Override
    public void start() {

        /*String[] sFullSet = new String[]{
                "MCPG49XIR65AFTBLZ3K2DY7NVHE8S",
                "CA4BVRZDPEI9M2T7L6HG8YX5NFS3K",
                "H3ZMA7KDFG84I5ES6TV2CN9YRXLBP",
                "BAT5P9MKXH7YNDZFV2L48CSR6GE3I",
                "KZ5RAHGMYTX4P6792NFCLID3S8VEB",
                "8RKDMVSFB4PZ5E96XNIT7GH3C2LAY",
                "5LCRP76NY2VID9AZBXEG8KH3FTM4S",
            "01-2015" : 20
        },
        {
            "02-2015" : 10
        },
        {
            "01-2016" : 10
        },
        {
                "S794CIDKFNBX2TVHYGELMZ5863RPA",
                "KCLS6XY239EDM8GBT7NHVA5F4ZPIR"
        };
        //set 2
        //set 3
        //set 4
        //set 6
        //set 7
        //set 8
        //set 9
        //set 10

        Set<String> uniqueSet = new HashSet<>();

        String sGen = "MCPG49XIR65AFTBLZ3K2DY7NVHE8S";
        String sHash = "KCLS6XY239EDM8GBT7NHVA5F4ZPIR";

        String str = "";
        for(int i = 0;i<100000;i++){

            String random = RandomStringUtils.random(5, sGen);
            *//*while(uniqueSet.contains(random)){
                random = RandomStringUtils.random(5,sGen);
                System.out.println(random + CoreCommon.getHash(random,sHash));

            }*//*


            random = RandomStringUtils.random(5,sGen);
            str +=  random + CoreCommon.getHash(random,sHash) + "\n";

            //container.logger().info(random + CoreCommon.getHash(random,sHash));

        }


        BufferedWriter writer = null;
        try {

            //create a temporary file
            java.io.File logFile = new java.io.File("/home/concu/code.txt");
            writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(str);
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
        }*/

        logger = container.logger();
        globalCfg = container.config();//mongo notification.start


        //we first init our cached for imei+private_key map
        //deploy the data module


        JsonObject mongo_config = new JsonObject();
        mongo_config.putString("bus_address", AppConstant.MongoVerticle_ADDRESS);
        mongo_config.putString("host", "localhost");
        mongo_config.putNumber("port", 27017);
        mongo_config.putString("username", null);
        mongo_config.putString("password", null);
        mongo_config.putNumber("pool_size", 20);
        mongo_config.putBoolean("auto_connect_retry", true);
        mongo_config.putNumber("socket_timeout", 20);
        mongo_config.putBoolean("use_ssl", false);
        mongo_config.putString("bus_address", "com.mservice.momo.database");
        mongo_config = globalCfg.getObject("mongo", mongo_config);

        container.deployModule("mservice~mongo~1.0", mongo_config, 1, new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> ar) {
                if (ar.succeeded()) {


                    logger.info("mservice~mongo~1.0 started ok" + ar.result());

                    JsonObject mongo_noti = new JsonObject();
                    mongo_noti.putString("host", "172.16.14.34");
                    mongo_noti.putString("bus_address", AppConstant.MongoVerticle_NOTIFICATION_ADDRESS);
                    mongo_noti.putNumber("port", 27017);
                    mongo_noti.putString("username", null);
                    mongo_noti.putString("password", null);
                    mongo_noti.putNumber("pool_size", 20);
                    mongo_noti.putBoolean("auto_connect_retry", true);
                    mongo_noti.putNumber("socket_timeout", 20);
                    mongo_noti.putBoolean("use_ssl", false);
                    mongo_noti.putString("bus_address", "com.mservice.momo.database_notification");
                    mongo_noti = globalCfg.getObject("mongo_noti", mongo_noti);

                    container.deployModule("mservice~mongo_noti~1.0", mongo_noti, 1, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {

                                ArrayList<String> arrayList = new ArrayList<String>();
                                arrayList.add("APA91bGD8ObhIPDJiJxry2_JoqAdPphzFKwgLYwFQFPAQSEhzBpk7QsW4YFLKierATt0CgJF7wnkychCmFrFW4lArwZIq_pmii-DWrL2eNl8cA1ogAVNdHcA9h4Q7Ahcjnrpq9mOyzqc1-5GJfqFDhzocAb06N3UAg");
                                Notification noti = new Notification();
                                noti.receiverNumber = 942979584;
                                noti.category = 0;
                                noti.caption = "test MMT";
                                noti.body = "AAAAAAAAAAAAA";
                                noti.token = "APA91bGD8ObhIPDJiJxry2_JoqAdPphzFKwgLYwFQFPAQSEhzBpk7QsW4YFLKierATt0CgJF7wnkychCmFrFW4lArwZIq_pmii-DWrL2eNl8cA1ogAVNdHcA9h4Q7Ahcjnrpq9mOyzqc1-5GJfqFDhzocAb06N3UAg";
                                noti.bodyIOS = noti.body;
                                noti.status = Notification.STATUS_DETAIL;
                                noti.btnStatus = 0;
                                noti.cmdId = System.currentTimeMillis();
                                noti.type = MomoProto.NotificationType.NOTI_DETAIL_VALUE;
                                noti.extra = "";
                                noti.priority = 2;

                                //start tool verticle here
                                container.deployWorkerVerticle("com.mservice.momo.vertx.ToolsVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                                    public void handle(AsyncResult<String> ar) {
                                        if (ar.succeeded()) {

                                            logger.info("com.mservice.momo.vertx.ToolsVerticle started ok");
                                        } else {
                                            logger.info("com.mservice.momo.vertx.ToolsVerticle start fail");
                                            ar.cause().printStackTrace();
                                        }
                                    }
                                });

                                logger.info("mservice~mongo_noti~1.0 started ok");
                            } else {
                                logger.info("mservice~mongo_noti~1.0 start fail");
                                logger.info(ar.cause().getMessage());
                                ar.cause().printStackTrace();
                            }
                        }
                    });


                    JsonObject m2number_cfg = container.config().getObject("sms_for_m2number");
                    container.deployWorkerVerticle("com.mservice.momo.vertx.m2number.M2NumberVerticle", m2number_cfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("com.mservice.momo.vertx.m2number.M2NumberVerticle started ok");
                            } else {
                                logger.info("com.mservice.momo.vertx.m2number.M2NumberVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    container.deployWorkerVerticle("com.mservice.momo.vertx.PromotionVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("com.mservice.momo.vertx.PromotionVerticle ok");

                            } else {
                                logger.info("com.mservice.momo.vertx.PromotionVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    container.deployWorkerVerticle("com.mservice.momo.gateway.internal.db.oracle.LStandbyOracleVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("LStandbyOracleVerticle started ok");

                            } else {
                                logger.info("LStandbyOracleVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    JsonObject umarket_database = globalCfg.getObject("umarket_database");
                    JsonObject umarket_database_cfg = new JsonObject();
                    umarket_database_cfg.putObject("umarket_database", umarket_database);

                    container.deployWorkerVerticle("com.mservice.momo.gateway.internal.db.oracle.UMarketOracleVerticle", umarket_database_cfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
//                                String ADDRESS ="UMarketOracleVerticle";
//                                JsonObject cfg = getContainer().config().getObject("umarket_database");
//                                String Driver  = cfg.getString("driver");
//                                String Url = cfg.getString("url");
//                                String Username = cfg.getString("username");
//                                String Password = cfg.getString("password");
//                                DBProcess dbProcess = new DBProcess(Driver,Url,Username,Password,ADDRESS,ADDRESS,logger);
//                                Common.BuildLog log = new Common.BuildLog(logger);
//                                dbProcess.checkC2cRule("","","", 500000, log);
                                /*JsonObject joReq = new JsonObject();
                                joReq.putNumber("type", UMarketOracleVerticle.GET_C2C_INFO);
                                joReq.putString("mtcn","5991803947");
                                joReq.putNumber("number",974541221);

                                vertx.eventBus().send(UMarketOracleVerticle.ADDRESS, joReq, new Handler<CoreMessage<JsonArray>>() {
                                    @Override
                                    public void handle(CoreMessage<JsonArray> arrResult) {

                                        JsonArray arrInfo = arrResult.body();

                                    }
                                });*/

                                logger.info("UMarketOracleVerticle started ok");
                            } else {
                                logger.info("UMarketOracleVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    JsonObject connector_default = new JsonObject();
                    connector_default.putString("server_ip", "0.0.0.0");
                    connector_default.putNumber("server_port", 6969);
                    connector_default.putString("initiator_acc", "confirm");
                    connector_default.putString("initiator_pin", "000000");
                    connector_default.putString("core_client_id", "backend");
                    connector_default.putString("core_client_pass", "696969");

                    JsonObject core_connector = new JsonObject();
                    core_connector.putObject("core_connector", connector_default);

                    final JsonObject connector_cfg = globalCfg.getObject("core_connector", connector_default);
                    container.deployWorkerVerticle("com.mservice.momo.gateway.internal.core.CoreConnectorVerticle", connector_cfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("CoreConnectorVerticle started ok");

                                JsonObject mobiweb_cfg = new JsonObject();
                                mobiweb_cfg.putNumber("mobiweb_port", container.config().getNumber("mobiweb_port"));

                                container.deployVerticle("com.mservice.momo.vertx.mobiweb.MobiWebVerticle", mobiweb_cfg, 1, new AsyncResultHandler<String>() {
                                    public void handle(AsyncResult<String> ar) {
                                        if (ar.succeeded()) {
                                            logger.info("MobiWebVerticle started ok");

                                        } else {
                                            logger.info("MobiWebVerticle start fail");
                                            ar.cause().printStackTrace();
                                        }
                                    }
                                });

                            } else {
                                logger.info("CoreConnectorVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });
//                    Common.BuildLog log = new Common.BuildLog(logger);
//                    JsonObject object = Misc.readJsonObjectFile("verticle.json", log);
//                    System.out.println("");
                    //submit form.start
                    boolean allow_submit_form_verticle = globalCfg.getBoolean("allow_submit_form_verticle", false);
                    if (allow_submit_form_verticle) {

                        container.deployWorkerVerticle("com.mservice.momo.vertx.form.SubmitFormVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                            public void handle(AsyncResult<String> ar) {
                                if (ar.succeeded()) {
                                    logger.info("SubmitFormVerticle started ok");

                                } else {
                                    logger.info("SubmitFormVerticle start fail");
                                    ar.cause().printStackTrace();
                                }
                            }
                        });
                    }
                    //submit form.end

                    //sybersource.start
                    container.deployVerticle("com.mservice.momo.gateway.internal.visamaster.VMNotifyVerticle"
                            , globalCfg
                            , 1
                            , new Handler<AsyncResult<String>>() {

                        @Override
                        public void handle(AsyncResult<String> asyncResult) {
                            if (asyncResult.succeeded()) {
                                logger.info("start VisaMaster NotifyVerticle ok");
                            } else {
                                logger.info("start VisaMaster NotifyVerticle failed");
                            }
                        }
                    });
                    //sybersource.end

                    //vcb.start
                    container.deployWorkerVerticle("com.mservice.momo.vertx.vcb.VcbVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("VcbVerticle started ok");

                            } else {
                                logger.info("VcbVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    //vcb.end


                    //customer care.start

                    container.deployWorkerVerticle("com.mservice.momo.vertx.customercare.CustomerCareVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("CustomerCareVerticle started ok");

                            } else {
                                logger.info("CustomerCareVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    //customer care.end

                    //BEGIN 0000000004 billpay promo
                    container.deployWorkerVerticle("com.mservice.momo.vertx.billpaypromo.BillPayPromoVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("BillPayPromoVerticle started ok");

                            } else {
                                logger.info("BillPayPromoVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });
                    //END 0000000004


//        //BEGIN 0000000015 BEGIN VISAMPOINTPROMOVERTICLE
                    container.deployVerticle("com.mservice.momo.vertx.visampointpromo.VisaMpointPromoVerticle", globalCfg, 1, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("VisaMpointPromoVerticle started ok");

                            } else {
                                logger.info("VisaMpointPromoVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });
//        //END 0000000015

                    //retailer fee verticle.start
                    boolean allow_dgd_fee_verticle = globalCfg.getBoolean("allow_dgd_fee_verticle", false);

                    if (allow_dgd_fee_verticle) {
                        container.deployWorkerVerticle("com.mservice.momo.vertx.retailer.RetailerFeeVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                            public void handle(AsyncResult<String> ar) {
                                if (ar.succeeded()) {
                                    logger.info("RetailerFeeVerticle started ok");

                                } else {
                                    logger.info("RetailerFeeVerticle start fail");
                                    ar.cause().printStackTrace();
                                }
                            }
                        });
                    }
                    //retailer fee.end

                    //vote connector.start

                    JsonObject vote_default = new JsonObject();
                    vote_default.putString("server_ip", "0.0.0.0");
                    vote_default.putNumber("server_port", 6969);
                    vote_default.putString("initiator_acc", "confirm");
                    vote_default.putString("initiator_pin", "000000");
                    vote_default.putString("core_client_id", "backend");
                    vote_default.putString("core_client_pass", "696969");

                    JsonObject vote_connector = new JsonObject();
                    vote_connector.putObject("vote_connector", connector_default);

                    JsonObject vote_cfg = globalCfg.getObject("vote_connector", connector_default);

                    if (globalCfg.getObject("vote_connector").getBoolean("allow", true)) {
                        container.deployWorkerVerticle("com.mservice.momo.gateway.internal.core.CoreConnectorCDHHVerticle", vote_cfg, 1, true, new AsyncResultHandler<String>() {
                            public void handle(AsyncResult<String> ar) {
                                if (ar.succeeded()) {
                                    logger.info("CoreConnectorCDHHVerticle started ok");

                                } else {
                                    logger.info("CoreConnectorVerticle start fail");
                                    ar.cause().printStackTrace();
                                }
                            }
                        });
                    }

                    container.deployWorkerVerticle("com.mservice.momo.vertx.PayBackCDHHVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("PayBackCDHHVerticle started ok");

                            } else {
                                logger.info("PayBackCDHHVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    //vote connector.end

                    container.deployVerticle("com.mservice.momo.vertx.ConfigVerticle", globalCfg, 1, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("ConfigVerticle started ok");
                            } else {
                                logger.info("ConfigVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.entry.ServerVerticle", globalCfg, 1, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("ServerVerticle started ok");
                            } else {
                                logger.info("ServerVerticle started fail" + ar.cause().toString());
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    JsonObject sms_config = new JsonObject();
                    sms_config.putString("sys.sms.api", "http://172.16.18.50:18080/ClientMark/services/Alert");
                    JsonObject sms_cfg = globalCfg.getObject("sms", sms_config);

                    container.deployWorkerVerticle("com.mservice.momo.gateway.internal.sms.SmsVerticle", sms_cfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("SmsVerticle started ok");
                            } else {
                                logger.info("SmsVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    container.deployWorkerVerticle("com.mservice.momo.gateway.internal.soapin.SoapVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {

                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
//                                SoapInProcess process = new SoapInProcess(logger, globalCfg);
//
//                                process.loginTrust();
                                /*
                                final Buffer buffer = MomoMessage.buildBuffer(SoapProto.MsgType.CHECK_USER_STATUS_VALUE
                                        ,0
                                        ,974540385
                                        ,"".getBytes());
                                vertx.eventBus().send(AppConstant.SoapVerticle_ADDRESS, buffer, new Handler<CoreMessage<Buffer>>() {
                                    @Override
                                    public void handle(final CoreMessage<Buffer> response) {
                                        MomoMessage momo = MomoMessage.fromBuffer(response.body());

                                        boolean isRegistered = false;
                                        boolean isActived = true;
                                        MomoProto.RegStatus status = null;
                                        try {
                                            status =  MomoProto.RegStatus.parseFrom(momo.cmdBody);
                                            isRegistered = status.getIsReged();
                                            isActived = status.getIsActive();
                                        } catch (InvalidProtocolBufferException e) {

                                        }

                                        logger.info("A");
                                    }
                                });*/

                                logger.info("SoapVerticle started ok");

                            } else {
                                logger.info("SoapVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    JsonObject bank_net = globalCfg.getObject("bank_net");
                    container.deployWorkerVerticle("com.mservice.momo.gateway.external.banknet.BanknetVerticle", bank_net, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("BanknetVerticle started ok");

                            } else {
                                logger.info("BanknetVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.vertx.Partner123muaRequestVerticle", globalCfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("Partner123muaRequestVerticle has been deployed successfully!");
                            } else {
                                logger.error("Partner123muaRequestVerticle deployed fail!", event.cause());
                            }
                        }
                    });


                    JsonObject store_location_cfg = new JsonObject();
                    container.deployVerticle("com.mservice.momo.vertx.LocationVerticle", store_location_cfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("LocationVerticle has been deployed successfully!");
                            } else {
                                logger.error("LocationVerticle deployed fail!", event.cause());
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.vertx.SmscTcpVerticle", globalCfg, new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("SmscTcpVerticle is ACTIVE.");

                            } else {
                                logger.error("SmscTcpVerticle is inactive!", event.cause());
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.vertx.CommandBridgeVerticle", globalCfg, new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("CommandBridgeVerticle is ACTIVE.");

                            } else {
                                logger.error("CommandBridgeVerticle is inactive!", event.cause());
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.avatar.UserResourceVerticle", globalCfg, new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("UserResourceVerticle is ACTIVE.");

                            } else {
                                logger.error("UserResourceVerticle is inactive!", event.cause());
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.vertx.StatisticVerticle", globalCfg, new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("StatisticVerticle has been deployed successfully!");

                            } else {
                                logger.error("StatisticVerticle has been deployed failed!", event.cause());
                            }
                        }
                    });

                    container.deployWorkerVerticle("com.mservice.momo.avatar.ImageProcessVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("ImageProcessVerticle started ok");
                            } else {
                                logger.info("ImageProcessVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    container.deployWorkerVerticle("com.mservice.momo.avatar.HttpFileDownloadVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("HttpFileDownloadVerticle started ok");

                            } else {
                                logger.info("HttpFileDownloadVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });


                    container.deployVerticle("com.mservice.momo.notification.NotificationVerticle", globalCfg, new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("NotificationVerticle has been deployed successfully!");
                            } else {
                                logger.error("NotificationVerticle has been deployed failed!", event.cause());
                            }
                        }
                    });

//                    container.deployVerticle("com.mservice.momo.vertx.WcVerticle", globalCfg, new Handler<AsyncResult<String>>() {
//                        @Override
//                        public void handle(AsyncResult<String> event) {
//                            if (event.succeeded()) {
//                                logger.info("WcVerticle has been deployed successfully!");
//
//                            } else {
//                                logger.error("WcVerticle has been deployed failed!", event.cause());
//                            }
//                        }
//                    });

                    container.deployVerticle("com.mservice.momo.vertx.CommandVerticle", globalCfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("CommandVerticle has been deployed successfully!");
                            } else {
                                logger.error("CommandVerticle deployed fail!", event.cause());
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.web.internal.webadmin.verticle.WebAdminVerticle", globalCfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("WebAdminVerticle has been deployed successfully!");
                            } else {
                                logger.error("WebAdminVerticle deployed fail!", event.cause());
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.vertx.GalaxyVerticle", globalCfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("GalaxyVerticle has been deployed successfully!");
                            } else {
                                logger.error("GalaxyVerticle deployed fail!", event.cause());
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.vertx.WebServiceVerticle", globalCfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("WebServiceVerticle has been deployed successfully!");
                            } else {
                                logger.error("WebServiceVerticle deployed fail!", event.cause());
                            }
                        }
                    });

                    container.deployWorkerVerticle("com.mservice.momo.vertx.MongoExportVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("MongoExportVerticle started ok");

                            } else {
                                logger.info("MongoExportVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    container.deployWorkerVerticle("com.mservice.momo.cloud.CloudNotifyVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("CloudNotifyVerticle started ok");/*

                                container.deployWorkerVerticle("com.mservice.momo.vertx.MyTestVerticle", globalCfg, 1, true, new AsyncResultHandler<String>() {
                                    public void handle(AsyncResult<String> ar) {
                                        if (ar.succeeded()) {
                                            logger.info("MyTestVerticle started ok");
                                        } else {
                                            logger.info("MyTestVerticle start fail");
                                            ar.cause().printStackTrace();
                                        }
                                    }
                                });*/
                            } else {
                                logger.info("CloudNotifyVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.vertx.SmartLinkVerticle", globalCfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("SmartLinkVerticle has been deployed successfully!");
                            } else {
                                logger.error("SmartLinkVerticle deployed fail!", event.cause());
                            }
                        }
                    });

                    container.deployWorkerVerticle("com.mservice.momo.vertx.ServiceConfVerticle"
                            , globalCfg
                            , 1
                            , true, new AsyncResultHandler<String>() {
                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("ServiceConfVerticle started ok");


//                                ConnectProcess connectProcess = new ConnectProcess(vertx, logger, globalCfg);
//                                Common.BuildLog log = new Common.BuildLog(logger);
//                                int phone = 934777999;
//                                byte[]body = new byte[3];
//                                MomoMessage msg = new MomoMessage(7, 1, phone, body );
//                                connectProcess.checkWalletMappingFromBank(msg, log);
//                                JsonObject db_cfg =  globalCfg.getObject("lstandby_database");
//                                String Driver  = db_cfg.getString("driver");
//                                String Url = db_cfg.getString("url");
//                                String Username = db_cfg.getString("username");
//                                String Password = db_cfg.getString("password");
//                                Common.BuildLog log = new Common.BuildLog(logger);
//                                DBProcess dbProcess = new DBProcess(Driver
//                                        ,Url
//                                        ,Username
//                                        ,Password
//                                        ,AppConstant.LStandbyOracleVerticle_ADDRESS
//                                        ,AppConstant.LStandbyOracleVerticle_ADDRESS
//                                        ,logger);
//                                dbProcess.checkC2cRule("0979754034", "5795", "voucherin", 50000, log);

                            } else {
                                logger.info("ServiceConfVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });

/*
                    container.deployVerticle("com.mservice.momo.vertx.event.TestEventVerticle", globalCfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("TestEventVerticle has been deployed successfully!");
                            } else {
                                logger.error("TestEventVerticle deployed fail!", event.cause());
                            }
                        }
                    });
*/
                } else {
                    logger.info("mongo-verticle start fail");
                    logger.info(ar.cause().getMessage());
                    ar.cause().printStackTrace();
                }
            }
        });

        container.deployVerticle("com.mservice.momo.gateway.external.vng.CinemaVerticle", globalCfg, 1, new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> ar) {
                if (ar.succeeded()) {

                    logger.info("CinemaVerticle started ok");


                } else {
                    logger.info("CinemaVerticle start fail");
                    ar.cause().printStackTrace();
                }
            }
        });


    }

    public static class CdhhPB {
        public String number = "";
        public int count = 0;

        public CdhhPB(String number, int count) {
            this.number = number;
            this.count = count;
        }
    }


    /*public void Export(String collName){

        String header = "<?xml version=\"1.0\" standalone=\"yes\"?>";
            header += "<NewDataSet>\n";

        String tpm = "<Table>\n" +
                "<ID>%s</ID>\n" +
                "<SMS>$s</SMS>\n" +
                "</Table>\n";

    }*/

    public static class CC {
        public String acc = "";
        public long amt = 0;

        public CC(String acc, long amt) {
            this.acc = acc;
            this.amt = amt;
        }
    }

}
