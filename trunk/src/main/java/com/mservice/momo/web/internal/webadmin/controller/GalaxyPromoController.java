package com.mservice.momo.web.internal.webadmin.controller;

import com.mservice.momo.data.CdhhConfigDb;
import com.mservice.momo.data.Promo123PhimGlxDb;
import com.mservice.momo.data.UserWebAdminDb;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.CodeUtil;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.models.CdhhConfig;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import com.mservice.momo.web.HttpRequestContext;
import com.mservice.momo.web.internal.webadmin.handler.Action;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.HashMap;

public class GalaxyPromoController {
    public static HashMap<String, UserWebAdminDb.Obj> listUser = new HashMap<String, UserWebAdminDb.Obj>();
    public static JsonObject glbCfg = null;
    private static long TIME_OUT = 60 * 60 * 1000;
    private Vertx vertx;
    private Logger logger;
    private Promo123PhimGlxDb promo123PhimGlxDb;
    private CodeUtil glxUtil = new CodeUtil(4, 5);
    private CdhhConfigDb cdhhConfigDb;

    public GalaxyPromoController(Vertx vertx, Container container) {
        this.vertx = vertx;
        logger = container.logger();
        glbCfg = container.config();

        promo123PhimGlxDb = new Promo123PhimGlxDb(vertx.eventBus(), logger);

        JsonObject glxVC = glbCfg.getObject("galaxyVerticle", null);
        if (glxVC == null) {
            logger.info("Khong cau hinh chay web service tren server nay");
            return;
        }

        JsonArray array = glxVC.getArray("account", null);

        if (array == null) {
            logger.info("Khong cau hinh danh sasch galaxy user tren server nay");
            return;
        }

        if (array != null && array.size() > 0) {
            for (int i = 0; i < array.size(); i++) {
                JsonObject o = array.get(i);
                UserWebAdminDb.Obj ob = new UserWebAdminDb.Obj();

                ob.username = o.getString("user");
                ob.password = o.getString("pass");
                ob.lastActTime = System.currentTimeMillis();
                listUser.put(ob.username, ob);
            }
        }

        cdhhConfigDb = new CdhhConfigDb(vertx, logger);

    }

    @Action(path = "/phim123/upGalaxy")
    public void upGalaxy(HttpRequestContext context, final Handler<JsonObject> callback) {

        MultiMap params = context.getRequest().params();
        String username = params.get("username") == null ? "" : params.get("username").trim();
        String password = params.get("password") == null ? "" : params.get("password").trim();

        boolean ok = checkSession(username, password);
        if (!ok) {
            callback.handle(new JsonObject()
                    .putString("error", "-1")
                    .putString("desc", "Must login"));
            return;
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "/phim123/upGalaxy");
        log.add("username", username);

        int id = Integer.parseInt(params.get("id_code").trim());
        String status = params.get("id_status").trim();
        final JsonObject result = new JsonObject();

        log.add("id", id);
        log.add("status", status);

        JsonObject joUpdate = new JsonObject();
        joUpdate.putNumber(colName.Phim123PromoGlxCols.UPDATE_TIME, System.currentTimeMillis());
        joUpdate.putString(colName.Phim123PromoGlxCols.UPDATE_BY, username);
        joUpdate.putString(colName.Phim123PromoGlxCols.STATUS, status);

        if (status.equals(Promo123PhimGlxDb.STATUS_COMPLETED))
            joUpdate.putString(colName.Phim123PromoGlxCols.DESC, Promo123PhimGlxDb.DESC_COMPLETED);
        else if (status.equals(Promo123PhimGlxDb.STATUS_INVALID))
            joUpdate.putString(colName.Phim123PromoGlxCols.DESC, "Combo không hợp lệ");

        promo123PhimGlxDb.update(id, joUpdate, new Handler<Boolean>() {
            @Override
            public void handle(Boolean flag) {

                if (flag == false) {
                    result.putNumber("error", 1);
                    result.putString("desc", "upsert failed");
                } else {
                    result.putNumber("error", 0);
                    result.putString("desc", "");
                }

                log.add("flag", flag);
                log.writeLog();

                callback.handle(result);
                return;
            }
        });
    }

    @Action(path = "/phim123/getGalaxy")
    public void getGalaxy(HttpRequestContext context, final Handler<JsonObject> callback) {

        MultiMap params = context.getRequest().params();

        String username = params.get("username") == null ? "" : params.get("username").trim();
        String password = params.get("password") == null ? "" : params.get("password").trim();

        String promo_code = params.get("promo_code") == null ? "" : params.get("promo_code").trim();

        promo_code = "MOMO" + promo_code.toUpperCase();

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "/phim123/getGalaxy");
        log.add("username", username);

        boolean ok = checkSession(username, password);
        if (!ok) {
            callback.handle(new JsonObject()
                    .putString("error", "-1")
                    .putString("desc", "Must login"));
            return;
        }
        //valid code
        if ("".equalsIgnoreCase(promo_code) || promo_code.length() != 9) {
            callback.handle(new JsonObject()
                    .putString("error", "-2")
                    .putString("desc", "Combo không hợp lệ."));
            return;
        }

        boolean valid = glxUtil.isCodeValid(promo_code, "MOMO");

        if (!valid) {
            callback.handle(new JsonObject()
                    .putString("error", "-2")
                    .putString("desc", "Combo không hợp lệ."));
            return;
        }

        log.add("code", promo_code);

        promo123PhimGlxDb.get(promo_code, "", new Handler<Promo123PhimGlxDb.Obj>() {
            @Override
            public void handle(final Promo123PhimGlxDb.Obj obj) {
                JsonObject reObj = obj == null ? new Promo123PhimGlxDb.Obj().toJson() : obj.toJson();
                reObj.putString("amount", obj == null ? "" : Misc.formatAmount(obj.AMOUNT).replace(",", "."));
                reObj.putString("update_time", obj == null ? "" : Misc.dateVNFormatWithTime(obj.UPDATE_TIME));

                log.add("desc", obj == null ? "Not Exists" : obj.DESC);
                log.writeLog();
                callback.handle(reObj);
            }
        });
    }

    private boolean checkSession(String username, String password) {

        UserWebAdminDb.Obj obj = listUser.get(username);
        if (obj == null) return false;

        if (!username.equalsIgnoreCase(obj.username)) {
            return false;
        }
        if (obj.lastActTime + TIME_OUT < System.currentTimeMillis()) {
            return false;
        }
        obj.lastActTime = System.currentTimeMillis();

        return true;
    }

    @Action(path = "/phim123/login")
    public void login(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String username = "";
        String password = "";

        if (params.get("username") != null) {
            username = params.get("username");
        }
        if (params.get("password") != null) {
            password = params.get("password");
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "/phim123/login");
        log.add("username", username);

        if ("".equalsIgnoreCase(username) || "".equalsIgnoreCase(password)) {
            callback.handle(new JsonObject()
                            .putNumber("error", -1)
                            .putString("desc", "Login failed")
            );
            return;
        }

        UserWebAdminDb.Obj oblogin = listUser.get(username);

        if (oblogin == null) {
            callback.handle(new JsonObject()
                            .putNumber("error", -1)
                            .putString("desc", "Login failed")
            );
            return;
        }

        if (!username.equalsIgnoreCase(oblogin.username)) {
            callback.handle(new JsonObject()
                            .putNumber("error", -1)
                            .putString("desc", "User name khong hop le")
            );
            return;
        }

        oblogin.lastActTime = System.currentTimeMillis();
        log.add("login", 0);
        log.writeLog();
        callback.handle(new JsonObject()
                        .putNumber("error", 0)
                        .putString("desc", "Login successful")
        );
    }

    @Action(path = "/votingreport/login")
    public void loginCatTienSa(HttpRequestContext context, final Handler<JsonObject> callback) {
        MultiMap params = context.getRequest().params();
        String username = "";
        String password = "";

        if (params.get("username") != null) {
            username = params.get("username");
        }
        if (params.get("password") != null) {
            password = params.get("password");
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "/votingreport/login");
        log.add("username", username);

        if ("".equalsIgnoreCase(username) || "".equalsIgnoreCase(password)) {
            callback.handle(new JsonObject()
                            .putNumber("error", -1)
                            .putString("desc", "Login failed")
            );
            return;
        }

        UserWebAdminDb.Obj oblogin = listUser.get(username);

        if (oblogin == null) {
            callback.handle(new JsonObject()
                            .putNumber("error", -1)
                            .putString("desc", "Login failed")
            );
            return;
        }

        if (!username.equalsIgnoreCase(oblogin.username)) {
            callback.handle(new JsonObject()
                            .putNumber("error", -1)
                            .putString("desc", "User name khong hop le")
            );
            return;
        }

        oblogin.lastActTime = System.currentTimeMillis();
        log.add("login", 0);
        log.writeLog();
        callback.handle(new JsonObject()
                        .putNumber("error", 0)
                        .putString("desc", "Login successful")
        );
    }


    @Action(path = "/votingreport/getByServiceId")
    public void getByServiceId(final HttpRequestContext context, final Handler<JsonArray> callback) {
        MultiMap params = context.getRequest().params();
        String username = "";
        String password = "";

        if (params.get("uname") != null) {
            username = params.get("uname");
        }
        if (params.get("pass") != null) {
            password = params.get("pass");
        }

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "/votingreport/getByServiceId");
        log.add("username", username);

        final String programCode = params.get("code");

        final String collection = params.get("collection");

        final JsonArray array = new JsonArray();

        /*?code=" + code
                + "&uname=" + username
                + "&pass=" + password;*/

        /*UserWebAdminDb.Obj oblogin = listUser.get(username);

        if(oblogin == null){
            JsonObject jo = new JsonObject()
                    .putNumber("error", -1)
                    .putString("desc", "Login failed");
            array.add(jo);
            callback.handle(array);
            return;
        }*/

        /*boolean ok = checkSession(username,password);
        if(ok == false){
            JsonObject jo = new JsonObject()
                    .putNumber("error", -1)
                    .putString("desc", "Khong hop le");
            array.add(jo);
            callback.handle(array);
            return;
        }

        oblogin.lastActTime = System.currentTimeMillis();*/
        log.writeLog();

        cdhhConfigDb.getActiveCollection(programCode, collection, new Handler<CdhhConfig>() {
            @Override
            public void handle(final CdhhConfig cdhhConfig) {
                if (cdhhConfig == null) {
                    callback.handle(array);
                    return;
                }

                cdhhConfigDb.getVoteResult(cdhhConfig.collName, programCode, new Handler<ArrayList<CdhhConfigDb.VotedObj>>() {
                    @Override
                    public void handle(ArrayList<CdhhConfigDb.VotedObj> arrayObj) {

                        JsonArray data = new JsonArray();
                        JsonArray sum = new JsonArray();
                        JsonArray sumByNumber = new JsonArray();

                        if (arrayObj != null && arrayObj.size() > 0) {

                            HashMap<String, Integer> hmSum = new HashMap<String, Integer>();
                            HashMap<String, ObjByNumber> hmSumByNumber = new HashMap<String, ObjByNumber>();

                            ArrayList<Integer> arrSumUser = new ArrayList<Integer>();

                            for (int i = 0; i < arrayObj.size(); i++) {

                                CdhhConfigDb.VotedObj vo = arrayObj.get(i);

                                //tong so luong vi bau chon
                                if (!arrSumUser.contains(vo.number)) {
                                    arrSumUser.add(vo.number);
                                }

                                //tong so luong bau chon theo cap
                                String vCode = (DataUtil.strToInt(vo.code) < 10 ? "0" + DataUtil.strToInt(vo.code) : vo.code + "");
                                Integer count = hmSum.get(vCode);
                                count = (count == null ? vo.votedAmount : (count + vo.votedAmount));
                                hmSum.put(vCode, count);

                                //tong so luong bau chon theo tung dien thoai
                                ObjByNumber objByNumber = hmSumByNumber.get("0" + vo.number);
                                if (objByNumber == null) {
                                    String name = ("".equalsIgnoreCase(vo.name) ? "noname" : vo.name);
                                    objByNumber = new ObjByNumber("0" + vo.number
                                            , name
                                            , vo.votedAmount
                                    );
                                } else {
                                    objByNumber.voteAmount += vo.votedAmount;
                                }
                                hmSumByNumber.put("0" + vo.number, objByNumber);


                                JsonObject jo = vo.toJson();
                                if (DataUtil.strToInt(vo.code) < 10) {
                                    jo.putString("code", "0" + DataUtil.strToInt(vo.code));
                                } else {
                                    jo.putString("code", "" + DataUtil.strToInt(vo.code));
                                }
                                jo.putString("number", "0" + vo.number);
                                if ("".equalsIgnoreCase(vo.name)) {
                                    jo.putString("name", "noname");
                                }

                                data.add(jo);
                            }

                            //sort tong so luong bau chon
                            ArrayList<Obj> arrayList = new ArrayList<Obj>();

                            for (int i = cdhhConfig.minCode; i <= cdhhConfig.maxCode; i++) {
                                String trueCode = i < 10 ? "0" + i : i + "";

                                Integer tAmount = hmSum.get(trueCode);
                                tAmount = tAmount == null ? 0 : tAmount;
                                arrayList.add(new Obj(trueCode, tAmount));
                            }

                            //tong tin nhan
                            int total = 0;
                            for (int i = 0; i < arrayList.size(); i++) {
                                Obj o = arrayList.get(i);
                                JsonObject j = new JsonObject();
                                j.putString("code", o.code);
                                j.putNumber("amount", o.count);
                                total += o.count;
                                sum.add(j);
                            }

                            JsonObject user = new JsonObject();
                            user.putString("code", "<b>Tổng tin nhắn</b>");
                            user.putString("amount", "<b>" + total + "</b>");
                            sum.add(user);

                            //tong so luong khach hang
                            JsonObject sumuser = new JsonObject();
                            sumuser.putString("code", "<b>Tổng khách hàng</b>");
                            sumuser.putString("amount", "<b>" + arrSumUser.size() + "</b>");
                            sum.add(sumuser);

                            //tong so vi binh chon va so luong binh chon
                            for (ObjByNumber o : hmSumByNumber.values()) {
                                JsonObject obn = new JsonObject();
                                obn.putString("num", o.number);
                                obn.putString("name", o.name);
                                obn.putString("vote", o.voteAmount + "");
                                sumByNumber.add(obn);

                            }
                        }

                        JsonArray jsonArray = new JsonArray();

                        JsonObject jdata = new JsonObject();
                        jdata.putArray("data", data);
                        jsonArray.add(jdata);


                        JsonObject jsum = new JsonObject();
                        jsum.putArray("sum", sum);
                        jsonArray.add(jsum);

                        JsonObject sbnObj = new JsonObject();
                        sbnObj.putArray("sumbn", sumByNumber);

                        jsonArray.add(sbnObj);


                        callback.handle(jsonArray);
                    }
                });
            }
        });
    }

    @Action(path = "/phim123/csv")
    public void getExport(final HttpRequestContext context, final Handler<JsonObject> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "/phim123/csv");

        String user = WebAdminController.checkSession(context, callback);
        if ("".equalsIgnoreCase(user)) {
            return;
        }

        promo123PhimGlxDb.export(new Handler<ArrayList<Promo123PhimGlxDb.Obj>>() {
            @Override
            public void handle(final ArrayList<Promo123PhimGlxDb.Obj> obj) {

                String content = "Số điện thoại," +
                        "Mã Combo," +
                        "Trạng thái," +
                        "Người trả combo," +
                        "Thời gian trả," +
                        "Số lần nhận khuyến mãi\n";
                if (obj != null && obj.size() > 0) {
                    for (Promo123PhimGlxDb.Obj ob : obj) {
                        content += "'0"
                                + ob.ID + ","
                                + ob.PROMO_CODE + ","
                                + ob.DESC + ","
                                + ob.UPDATE_BY + ","
                                + (ob.UPDATE_TIME == 0 ? "" : Misc.dateVNFormatWithTime(ob.UPDATE_TIME)) + ","
                                + ob.PROMO_COUNT + "\n";
                    }
                }
                log.add("obj", obj == null ? 0 : obj.size());
                log.writeLog();
                callback.handle(new JsonObject()
                                .putNumber("error", 0)
                                .putString("desc", content)
                );
            }
        });

    }

    @Action(path = "/phim123/report")
    public void getReport(final HttpRequestContext context, final Handler<Object> callback) {

        final Common.BuildLog log = new Common.BuildLog(logger);
        log.add("function", "/phim123/report");

        MultiMap params = context.getRequest().params();

        String username = params.get("username") == null ? "" : params.get("username").trim();
        String password = params.get("password") == null ? "" : params.get("password").trim();

        String fromdate = params.get("fromdate") == null ? "" : params.get("fromdate").trim();
        String todate = params.get("todate") == null ? "" : params.get("todate").trim();


        String[] tempFromdate = fromdate.split(",");
        String[] tempTodate = todate.split(",");

        String strFdate = tempFromdate.length > 1 ? tempFromdate[0].trim() + " " + tempFromdate[1].trim()
                : tempFromdate[0].trim() + " 00:00:00";

        String strTdate = tempTodate.length > 1 ? tempTodate[0].trim() + " " + tempTodate[1].trim()
                : tempTodate[0].trim() + " 00:00:00";

        long fdate = Misc.getDateAsLong(strFdate, "dd/MM/yyyy HH:mm:ss", logger, "");

        long tdate = Misc.getDateAsLong(strTdate, "dd/MM/yyyy HH:mm:ss", logger, "");


        boolean ok = checkSession(username, password);
        if (!ok) {
            callback.handle(new JsonObject()
                    .putString("error", "-1")
                    .putString("desc", "Must login"));
            return;
        }

        promo123PhimGlxDb.report(username, fdate, tdate, new Handler<ArrayList<Promo123PhimGlxDb.Obj>>() {
            @Override
            public void handle(final ArrayList<Promo123PhimGlxDb.Obj> obj) {

                String content = "Số điện thoại," +
                        "Thời gian chiếu," +
                        "Số hóa đơn," +
                        "Ticket," +
                        "Rạp," +
                        "Tên phim," +
                        "Mã Combo," +
                        "Trạng thái," +
                        "Người trả Combo," +
                        "Thời gian trả \n";
                if (obj != null && obj.size() > 0) {
                    for (Promo123PhimGlxDb.Obj ob : obj) {
                        content += "'0"
                                + ob.ID + ","
                                + ob.DISPLAY_TIME + ","
                                + ob.INVOICE_NO + ","
                                + ob.TICKET_CODE + ","
                                + ob.RAP + ","
                                + ob.FILM_NAME + ","
                                + ob.PROMO_CODE + ","
                                + ob.DESC + ","
                                + ob.UPDATE_BY + ","
                                + Misc.dateVNFormatWithTime(ob.UPDATE_TIME) + "\n";
                    }
                }
                log.add("obj", obj == null ? 0 : obj.size());
                log.writeLog();
                callback.handle(new JsonObject()
                                .putNumber("error", 0)
                                .putString("desc", content)
                );
            }
        });

    }

    public static class Obj {
        public String code;
        public int count = 0;

        public Obj(String code, int count) {
            this.code = code;
            this.count = count;
        }
    }

    public static class ObjByNumber {
        public String number;
        public String name;
        public int voteAmount;

        public ObjByNumber(String number, String name, int voteAmount) {
            this.number = number;
            this.name = name;
            this.voteAmount = voteAmount;
        }
    }

}