package com.mservice.momo.vertx.models.wc;

import com.mservice.momo.data.model.MongoModel;
import org.vertx.java.core.json.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nam on 6/8/14.
 */
public class Match extends MongoModel implements Comparable{

    private String name;
    private Long startTime;
    private Long endTime;
    private Integer round;

    private Integer result;
    private Integer a;
    private Integer b;


    private Long soNguoiChoi;
    private Long dungKetQua;
    private Long dungTiSo;
    private Long giaiKetQua;
    private Long giaiTiSo;

    private Double phanTramKetQua;
    private Double phanTramTiSo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
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

    public Long getSoNguoiChoi() {
        return soNguoiChoi;
    }

    public void setSoNguoiChoi(Long soNguoiChoi) {
        this.soNguoiChoi = soNguoiChoi;
    }

    public Long getDungKetQua() {
        return dungKetQua;
    }

    public void setDungKetQua(Long dungKetQua) {
        this.dungKetQua = dungKetQua;
    }

    public Long getDungTiSo() {
        return dungTiSo;
    }

    public void setDungTiSo(Long dungTiSo) {
        this.dungTiSo = dungTiSo;
    }

    public Long getGiaiKetQua() {
        return giaiKetQua;
    }

    public void setGiaiKetQua(Long giaiKetQua) {
        this.giaiKetQua = giaiKetQua;
    }

    public Long getGiaiTiSo() {
        return giaiTiSo;
    }

    public void setGiaiTiSo(Long giaiTiSo) {
        this.giaiTiSo = giaiTiSo;
    }

    public Double getPhanTramKetQua() {
        return phanTramKetQua;
    }

    public void setPhanTramKetQua(Double phanTramKetQua) {
        this.phanTramKetQua = phanTramKetQua;
    }

    public Double getPhanTramTiSo() {
        return phanTramTiSo;
    }

    public void setPhanTramTiSo(Double phanTramTiSo) {
        this.phanTramTiSo = phanTramTiSo;
    }

    @Override
    public JsonObject getPersisFields() {
        JsonObject json = new JsonObject();
        if (name != null)
            json.putString("name", name);
        if (startTime != null)
            json.putNumber("startTime", startTime);
        if (endTime != null)
            json.putNumber("endTime", endTime);
        if (round != null)
            json.putNumber("round", round);
        if (result != null) {
            json.putNumber("result", result);
        }
        if (a != null) {
            json.putNumber("a", a);
        }
        if (b != null) {
            json.putNumber("b", b);
        }

        if(soNguoiChoi !=null)
            json.putNumber("soNguoiChoi", soNguoiChoi);
        if(dungKetQua !=null)
            json.putNumber("dungKetQua", dungKetQua);
        if(dungTiSo !=null)
            json.putNumber("dungTiSo", dungTiSo);
        if(giaiKetQua !=null)
            json.putNumber("giaiKetQua", giaiKetQua);
        if(giaiTiSo !=null)
            json.putNumber("giaiTiSo", giaiTiSo);
        if(phanTramKetQua !=null)
            json.putNumber("phanTramKetQua", phanTramKetQua);
        if(phanTramTiSo !=null)
            json.putNumber("phanTramTiSo", phanTramTiSo);
        return json;
    }

    @Override
    public void setValues(JsonObject savedObject) {
        name = savedObject.getString("name");
        startTime = savedObject.getLong("startTime");
        endTime = savedObject.getLong("endTime");
        round = savedObject.getInteger("round");
        result = savedObject.getInteger("result");
        a = savedObject.getInteger("a");
        b = savedObject.getInteger("b");
        soNguoiChoi = savedObject.getLong("soNguoiChoi");
        dungKetQua = savedObject.getLong("dungKetQua");
        dungTiSo = savedObject.getLong("dungTiSo");
        giaiKetQua = savedObject.getLong("giaiKetQua");
        giaiTiSo = savedObject.getLong("giaiTiSo");

        Number temp = savedObject.getNumber("phanTramKetQua");
        if (temp != null)
            phanTramKetQua = (double) temp;

        temp = savedObject.getNumber("phanTramTiSo");
        if (temp != null)
            phanTramTiSo = (double) savedObject.getNumber("phanTramTiSo");
    }

    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
    public String getFormatedDate(Date date) {
        return format.format(date);
    }

    public String toHtmlTableRow() {
        StringBuilder builder =  new StringBuilder();
        builder.append("<tr>");
        builder.append("<td>").append(getModelId() == null ? "--" : getModelId()).append("</td>");
        builder.append("<td>").append(name == null ? "--" : name).append("</td>");
        builder.append("<td>").append(round == null ? "--" : round).append("</td>");
        builder.append("<td>").append(startTime == null ? "--" : getFormatedDate(new Date(startTime))).append("</td>");
        builder.append("<td>").append(endTime == null ? "--" : getFormatedDate(new Date(endTime))).append("</td>");
        builder.append("<td>").append(result == null ? "--" : result).append("</td>");
        builder.append("<td>").append(a == null ? "--" : a).append("</td>");
        builder.append("<td>").append(b == null ? "--" : b).append("</td>");
        builder.append("<td>").append(soNguoiChoi == null ? "--" : soNguoiChoi).append("</td>");
        builder.append("<td>").append(dungKetQua == null ? "--" : dungKetQua).append("</td>");
        builder.append("<td>").append(dungTiSo == null ? "--" : dungTiSo).append("</td>");
        builder.append("<td>").append(giaiKetQua == null ? "--" : giaiKetQua).append("</td>");
        builder.append("<td>").append(giaiTiSo == null ? "--" : giaiTiSo).append("</td>");
        builder.append("<td>").append(phanTramKetQua == null ? "--" : phanTramKetQua).append("</td>");
        builder.append("<td>").append(phanTramTiSo == null ? "--" : phanTramTiSo).append("</td>");
        builder.append("</tr>");
        return builder.toString();
    }
    public static String getHtmlTableHeader() {
        StringBuilder builder =  new StringBuilder();
        builder.append("<tr>");
        builder.append("<th>").append("MatchID").append("</th>");
        builder.append("<th>").append("Name").append("</th>");
        builder.append("<th>").append("Round").append("</th>");
        builder.append("<th>").append("Start time").append("</th>");
        builder.append("<th>").append("End time").append("</th>");
        builder.append("<th>").append("Result").append("</th>");
        builder.append("<th>").append("A").append("</th>");
        builder.append("<th>").append("B").append("</th>");
        builder.append("<th>").append("So nguoi choi").append("</th>");
        builder.append("<th>").append("Dung ket qua").append("</th>");
        builder.append("<th>").append("Dung ti so").append("</th>");
        builder.append("<th>").append("Giai ket qua").append("</th>");
        builder.append("<th>").append("Giai ti so").append("</th>");
        builder.append("<th>").append("% dung ket qua").append("</th>");
        builder.append("<th>").append("% dung ti so").append("</th>");
        builder.append("</tr>");
        return builder.toString();
    }

    @Override
    public int compareTo(Object o) {
        if (o==null)
            return 1;
        if (o instanceof Match){
            Match match = (Match)o;
            if(getModelId() == null)
                return 0;
            if (match.getModelId() ==null) {
                return 1;
            }
            return getModelId().compareTo(match.getModelId());
        }
        return -1;
    }
}
