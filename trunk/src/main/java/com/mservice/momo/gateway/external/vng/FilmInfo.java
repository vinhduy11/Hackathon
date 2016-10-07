package com.mservice.momo.gateway.external.vng;

import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.vertx.processor.Misc;

/**
 * Created by concu on 1/9/15.
 */
public class FilmInfo {

    /*final String fTenPhim = ((ar != null && ar.length > 1) ? ar[1].trim() : "");
    final String fSuatChieu = ((ar != null && ar.length > 2) ? ar[2].trim() : "");
    final String fRap = ((ar != null && ar.length > 4) ? ar[4].trim() : "");
    final String fSoGhe = chairInfo;
    final String fNgayMua = ((ar != null && ar.length > 3) ? ar[3].trim() : "");
    final int fChairCount = chairCount;*/

    public String filmName ="";
    public String session ="";
    public String cinemaFullName="";
    public String seatList="";
    public String buyDate="";
    public int seatAmount=0;
    public long amount = 0;
    public String cinemaShortName ="";
    public String invoiceNo ="";
    public String ticketCode= "";
    public String cinemaSubName ="";
    public String displayTime = "";
    public long tranId = 0;
    public long displayTimeAsLong = 0;

    public FilmInfo(){}
    public FilmInfo(String info,String invoiceNo,String ticketCode , long amount){
        this.amount = amount;
        this.invoiceNo = invoiceNo;
        this.ticketCode = ticketCode;
        /*pname : 123Phim
        \1.u0007Ninja Rùa - Teenage Mutant Ninja Turtles - 2D
        \2.u00072014-08-27 19:40:00
        \3.u000716:59 27/08/2014
        \4.u0007BHD Star Cineplex ICON 68
        \5.u00071 ghế. [E05]
        Trường hợp 2 ghế
        "pname" : "123Phim\u0007Sự Khởi Đầu Của Hành Tinh Khỉ - Dawn Of The Planet Of The Apes\u00072014-07-16 20:40:00\u000717:45 16/07/2014\u0007BHD Star Cineplex ICON 68\u00072 ghế. [G15-G14]"
        0. bo qua
        1. Ten Phim
        2. Ngày giờ chiếu
        3. Ngày giờ mua vé
        4. Rạp
        5. Số ghế
        */

        String[] ar = null;
        if (!"".equalsIgnoreCase(info)) {
            ar = info.split(MomoMessage.BELL);
            String soGhe = (ar != null && ar.length > 5 ? ar[5] : "");
            if (!"".equalsIgnoreCase(soGhe)) {
                String[] arghe = soGhe.split("\\.");
                if (arghe != null && arghe.length > 1) {
                    seatList = arghe[1].trim();
                    //android : [A03, A04]
                    //ios : [G15-G14]

                    //calculate chair count
                    String tmpSeatList = seatList.replace(",","-");
                    seatAmount = tmpSeatList.split("-").length;
                }
            }
        }
        filmName = ((ar != null && ar.length > 1) ? ar[1].trim() : "");
        session = ((ar != null && ar.length > 2) ? ar[2].trim() : "");
        cinemaFullName = ((ar != null && ar.length > 4) ? ar[4].trim() : "");
        cinemaShortName = cinemaFullName.toLowerCase().contains("galaxy") ? "glx" : "bhd";

        String lower = cinemaFullName.toLowerCase();

        if(!lower.contains("galaxy")){
            if(lower.contains("68")){
                cinemaSubName = "BHD 68";
            }else {
                cinemaSubName = "BHD 32";
            }
        }else{
            String[] subAr = cinemaFullName.split("-");
            cinemaSubName = subAr.length > 1 ? subAr[1].trim() : subAr[0].trim();
        }

        //2015-01-14 17:00:00
        //2015-01-18 18:00:00
        //2015-03-31 17:45:00
        displayTime = ((ar !=null  && ar.length >2) ? ar[2].trim() : "");
        displayTime = refineDateShow(displayTime);
        displayTimeAsLong = Misc.getDateAsLong(displayTime,"yyyy-MM-dd HH:mm:ss", null,"");

        //15:36 - 14/01/2015
        buyDate = ((ar != null && ar.length > 3) ? ar[3].trim() : "");
    }

    private String refineDateShow(String strDateShow) {

        //redefine ngay gio chieu
        if(!"".equalsIgnoreCase(strDateShow)){
            String[] arTmp = strDateShow.split(" ");
            strDateShow = arTmp[0].trim() + " " + arTmp[arTmp.length-1].trim();
        }

        String[] arrShow = strDateShow.split(" ");

        String strDate = arrShow.length >0 ?  arrShow[0] : "";
        if("".equalsIgnoreCase(strDate)){
            strDate = Misc.getDateWithFormat(System.currentTimeMillis(), "yyyy-MM-dd");
        }else{
            String[] dateArr = strDate.split("-");
            String y = dateArr.length > 0 ? dateArr[0] : Misc.getDateWithFormat(System.currentTimeMillis(),"yyyy");
            String m = dateArr.length > 1 ? dateArr[1].trim() : Misc.getDateWithFormat(System.currentTimeMillis(),"MM");
            String d = dateArr.length > 2 ? dateArr[2].trim() : Misc.getDateWithFormat(System.currentTimeMillis(),"dd");
            m = m.length() == 1 ? "0" + m : m;
            d = d.length() == 1 ? "0" + d : d;
            strDate = y + "-" + m + "-" + d;
        }
        String strTime = arrShow.length > 1 ? arrShow[1] : "";
        if("".equalsIgnoreCase(strTime)){
            strTime = Misc.getDateWithFormat(System.currentTimeMillis(),"HH:mm:ss");
        }else{
            String[] timeArr = strTime.split(":");
            String h = timeArr.length > 0 ? timeArr[0].trim() : Misc.getDateWithFormat(System.currentTimeMillis(),"HH");
            String mm = timeArr.length >1 ? timeArr[1].trim() : Misc.getDateWithFormat(System.currentTimeMillis(),"mm");
            String ss = timeArr.length >2 ? timeArr[2].trim() : Misc.getDateWithFormat(System.currentTimeMillis(),"ss");

            h = h.length() == 1 ? "0" + h : h;
            mm = mm.length() == 1 ? "0" + mm : mm;
            ss = ss.length() == 1 ? "0" + ss : ss;
            strTime = h + ":" + mm + ":" + ss;
        }

        return strDate + " " + strTime;
    }
}
