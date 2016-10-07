package com.mservice.momo.vertx.models.wc;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 6/8/14.
 */
public class DuDoan extends MongoModel {

//key is the phoneNumber.

    private String matchId;
    private Integer result;
    private Integer a;
    private Integer b;
    private Long time;

    private Long money;
    private Long sMoney;
    private Integer tranError;
    private Long zaloTime;
    private Long zaloId;

    private Boolean sentZaloMessage;

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public Integer getA() {
        return a;
    }

    public void setA(Integer a) {
        this.a = a;
    }

    public Integer getB() {
        return b;
    }

    public void setB(Integer b) {
        this.b = b;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Long getMoney() {
        return money;
    }

    public void setMoney(Long money) {
        this.money = money;
    }

    public Integer getTranError() {
        return tranError;
    }

    public void setTranError(Integer tranError) {
        this.tranError = tranError;
    }

    public Long getsMoney() {
        return sMoney;
    }

    public void setsMoney(Long sMoney) {
        this.sMoney = sMoney;
    }

    public Long getZaloTime() {
        return zaloTime;
    }

    public void setZaloTime(Long zaloTime) {
        this.zaloTime = zaloTime;
    }

    public Long getZaloId() {
        return zaloId;
    }

    public void setZaloId(Long zaloId) {
        this.zaloId = zaloId;
    }

    public Boolean getSentZaloMessage() {
        return sentZaloMessage;
    }

    public void setSentZaloMessage(Boolean sentZaloMessage) {
        this.sentZaloMessage = sentZaloMessage;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
//        if (matchId != null)
//            json.putString("matchId", matchId);
        if (result != null)
            json.putNumber("result", result);
        if (a != null)
            json.putNumber("a", a);
        if (b != null)
            json.putNumber("b", b);
        if (time != null)
            json.putNumber("time", time);
        if (money != null)
            json.putNumber("money", money);
        if (tranError != null)
            json.putNumber("tranError", tranError);
        if (sMoney != null)
            json.putNumber("sentMoney", sMoney);
        if (zaloTime != null)
            json.putNumber("zaloTime", zaloTime);
        if (zaloId != null)
            json.putNumber("zaloId", zaloId);
        if (sentZaloMessage != null)
            json.putBoolean("sentZaloMessage", sentZaloMessage);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        matchId = savedObject.getString("matchId");
        result = savedObject.getInteger("result");
        a = savedObject.getInteger("a");
        b = savedObject.getInteger("b");
        money = savedObject.getLong("money");
        time = savedObject.getLong("time");
        tranError = savedObject.getInteger("tranError");
        sMoney = savedObject.getLong("sentMoney");
        zaloTime = savedObject.getLong("zaloTime");
        zaloId = savedObject.getLong("zaloId");
        sentZaloMessage = savedObject.getBoolean("sentZaloMessage");
    }

    /**
     *
     * @param dt
     * @param tran
     * @param kq
     * @param ts
     * @return Never null
     */
    public static DuDoan buildFromWebServiceRequest(String dt, String tran, String kq, String ts, String zaloTime, String inputZaloId) {
        if (dt == null) {
            throw new IllegalArgumentException("dt param is missing.");
        }
        if (tran == null) {
            throw new IllegalArgumentException("tran param is missing.");
        }
        if (kq == null) {
            throw new IllegalArgumentException("kq param is missing.");
        }
        if (ts == null) {
            throw new IllegalArgumentException("ts param is missing.");
        }
        if (zaloTime == null) {
            throw new IllegalArgumentException("time param is missing.");
        }

        dt.replace("+84", "");
        if(dt.startsWith("84")) {
            dt = dt.substring(2, dt.length());
        }

        DuDoan duDoan = new DuDoan();

        int temp;

        // lay so phone
        try {
            temp = Integer.parseInt(dt);
            duDoan.setModelId(String.valueOf(temp));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("dt param has invalid value.");
        }

        // ma tran dau
        duDoan.setMatchId(tran.trim());


        //ket qua tran dau
        if (kq.equalsIgnoreCase("T")) {
            duDoan.result = 1;
        } else if (kq.equalsIgnoreCase("H")) {
            duDoan.result = 0;
        } else if (kq.equalsIgnoreCase("B")) {
            duDoan.result = -1;
        } else {
            throw new IllegalArgumentException("kq param has invalid value.");
        }

        // Ti so
        String[] score = ts.split("-");

        if (score.length < 2) {
            throw new IllegalArgumentException("ts param has invalid value.");
        }

        try {
            temp = Integer.parseInt(score[0]);
            duDoan.a = temp;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ts param has invalid value.");
        }

        try {
            temp = Integer.parseInt(score[1]);
            duDoan.b = temp;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ts param has invalid value.");
        }

        long finalZaloTime = 0;
        try {
            finalZaloTime = Long.parseLong(zaloTime);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("time param has invalid value.");
        }

        long zaloId = 0;
        try {
            zaloId = Long.parseLong(inputZaloId);
        } catch (NumberFormatException e) {
            //Đảm bảo code zalo cũ vẫn chạy đc.
            zaloId = finalZaloTime;
        }

        duDoan.time = System.currentTimeMillis();
        duDoan.money = 0L;
        duDoan.tranError = -1;

        duDoan.zaloTime = finalZaloTime;
        duDoan.zaloId = zaloId;
        duDoan.setSentZaloMessage(false);

        return duDoan;
    }
}
