package com.mservice.momo.util;

import com.mservice.momo.data.model.Const;
import com.mservice.momo.gateway.internal.visamaster.VMConst;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;
import com.mservice.momo.msg.TranTypeExt;
import com.mservice.momo.vertx.models.Notification;
import com.mservice.momo.vertx.models.TranObj;
import com.mservice.momo.vertx.processor.Misc;
import org.apache.commons.lang3.StringUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nam on 5/17/14.
 */
public class NotificationUtils {

    public static final int TYPE_TRAN = 1;
    public static final int TYPE_MONEY_REQUEST = 2;
    //dgd.end
    public static HashMap<String, Retailer> hashMapNotiRetailer = new HashMap<>();
    private static Document errorDocs;
    private static HashMap<String, String> hashMapSmsForCustomer = new HashMap<>();
    private static HashMap<String, HashMap<String, String>> hashMapReactiveInfo = new HashMap<>();
    private static HashMap<String, String> hashMapSmsForRetailer = new HashMap<>();
    //  TOP_UP_SUCCESS_TITLE > ""
    //  TOP_UP_SUCCESS_CONTENT >
    private static Map<String, String> tranMessages = new HashMap<>();

    static {
        init();
    }

    /**
     * MAPPING PROVIDER NAME FROM PROVIDER ID
     */

    private static Map<String, String> providerName;

    static {
        providerName = new HashMap<>();
        providerName.put("jetstar", "Jetstar");
        providerName.put("amr", "Mekong Airline");
        providerName.put("dien", "EVN");
        providerName.put("vco", "VTC");
        providerName.put("bac", "FPT");
        providerName.put("zxu", "VNG");
        providerName.put("onc", "VDC");
        providerName.put("vinahcm", "Vinaphone");
        providerName.put("cdhcm", "VNPT");
        providerName.put("ifpt", "FPT");
        providerName.put("ivettel", "Viettel");
        providerName.put("aviettel", "ADSL Viettel");
        providerName.put("nuochcm", "Nuoc Chợ Lớn");
        providerName.put("avg", "Truyền Hình An Viên");
    }

    private static void readErrorMessage(String fileName) {
        File fXmlFile = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            errorDocs = dBuilder.parse(fXmlFile);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readSmsMessage(String fileName) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                String fullContent = sb.toString();

                JsonArray array = new JsonArray(fullContent);

                for (int i = 0; i < array.size(); i++) {
                    JsonObject jo = array.get(i);
                    String key = "";
                    String val = "";
                    for (String s : jo.getFieldNames()) {
                        if ("".equalsIgnoreCase(key)) {
                            key = jo.getString(s);
                        } else {
                            val = jo.getString(s);
                        }
                    }
                    hashMapSmsForCustomer.put(key, val);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void readNotiRetailerMessage(String fileName) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line = null;
            line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            String fullContent = sb.toString();

            JsonArray array = new JsonArray(fullContent);

            for (int i = 0; i < array.size(); i++) {
                JsonObject jo = array.get(i);
                String key = "";
                RetailerNoti failObj = null;
                RetailerNoti successObj = null;

                for (String s : jo.getFieldNames()) {
                    key = s;
                    JsonObject subObj = jo.getObject(s);

                    JsonArray failArr = subObj.getArray("fail");
                    JsonArray successArr = subObj.getArray("success");

                    failObj = new RetailerNoti();
                    failObj.caption = ((JsonObject) failArr.get(0)).getString("cap", "");
                    failObj.body = ((JsonObject) failArr.get(1)).getString("body", "");
                    failObj.sms = ((JsonObject) failArr.get(2)).getString("sms", "");

                    successObj = new RetailerNoti();
                    successObj.caption = ((JsonObject) successArr.get(0)).getString("cap", "");
                    successObj.body = ((JsonObject) successArr.get(1)).getString("body", "");
                    successObj.sms = ((JsonObject) successArr.get(2)).getString("sms", "");
                }

                Retailer retailer = new Retailer();
                retailer.Fail = failObj;
                retailer.Success = successObj;

                hashMapNotiRetailer.put(key, retailer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void readSMSRetailerMessage(String fileName) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            // Read all file content
            br = new BufferedReader(new FileReader(fileName));
            line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            String fullContent = sb.toString();

            // Convert to json
            JsonArray array = new JsonArray(fullContent);

            for (int i = 0; i < array.size(); i++) {
                JsonObject jo = array.get(i);

                // Get key, sms
                String key = jo.getString("key");
                String sms = jo.getString("sms");
                // Put to hashMapSmsForRetailer
                hashMapSmsForRetailer.put(key, sms);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static String getSmsFormat(String key) {
        String content = hashMapSmsForCustomer.get(key);
        content = content == null ? "" : content;
        return content;
    }

    public static String getSmsFormatRetailer(String key) {
        String content = hashMapSmsForRetailer.get(key);
        content = content == null ? "" : content;
        return content;
    }

    public static Retailer getNotiFormatRetailer(String key) {
        return hashMapNotiRetailer.get(key);
    }

    private static void readTranMessage(String fileName) {
        File fXmlFile = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            NodeList nodeList = doc.getElementsByTagName("message");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node messageTag = nodeList.item(i);
                String tranType = messageTag.getAttributes().getNamedItem("name").getTextContent();
                NodeList notiTags = messageTag.getChildNodes();
                for (int j = 0; j < notiTags.getLength(); j++) {
                    Node notiTag = notiTags.item(j);
                    if (notiTag.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                    String successOrFail = notiTag.getAttributes().getNamedItem("name").getTextContent();
                    NodeList titleOrContentTags = notiTag.getChildNodes();
                    for (int k = 0; k < titleOrContentTags.getLength(); k++) {
                        Node titleOrContentTag = titleOrContentTags.item(k);
                        if (titleOrContentTag.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        String key = "";
                        if ("title".equalsIgnoreCase(titleOrContentTag.getNodeName())) {
                            key = tranType + "_" + successOrFail + "_" + "TITLE";
                        } else if ("sms".equalsIgnoreCase(titleOrContentTag.getNodeName())) {
                            key = tranType + "_" + successOrFail + "_" + "SMS";
                        } else if ("content".equalsIgnoreCase(titleOrContentTag.getNodeName())) {
                            key = tranType + "_" + successOrFail + "_" + "CONTENT";
                        }
                        tranMessages.put(key, titleOrContentTag.getTextContent());
//                        System.out.println(key + ">" + titleOrContentTag.getTextContent());
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //BEGIN 0000000001 Send SMS to Customer.
    private static void readCustomerMessage(String fileName) {
        File fXmlFile = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            NodeList nodeList = doc.getElementsByTagName("message");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node messageTag = nodeList.item(i);
                String tranType = messageTag.getAttributes().getNamedItem("name").getTextContent();
                NodeList notiTags = messageTag.getChildNodes();
                for (int j = 0; j < notiTags.getLength(); j++) {
                    Node notiTag = notiTags.item(j);
                    if (notiTag.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                    String key = "";
                    key = tranType;
                    hashMapSmsForCustomer.put(key, notiTag.getTextContent());
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //END 0000000001 Send SMS to Customer.

    //BEGIN Reactive Info.
    public static void readReactiveInfoMessage(String fileName) {
        File fXmlFile = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            NodeList nodeList = doc.getElementsByTagName("group");
            HashMap<String, String> hashContent;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node messageTag = nodeList.item(i);
                String tranType = messageTag.getAttributes().getNamedItem("name").getTextContent();
                NodeList notiTags = messageTag.getChildNodes();
                hashContent = new HashMap<>();
                for (int j = 0; j < notiTags.getLength(); j++) {
                    Node notiTag = notiTags.item(j);
                    if (notiTag.getNodeType() != Node.ELEMENT_NODE)
                        continue;
//                    String key = "";
//                    key = tranType;
                    hashContent.put(notiTag.getNodeName(), notiTag.getTextContent());

                }
                hashMapReactiveInfo.put(tranType, hashContent);
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //END Reactive Info .
    public static void init() {
        readErrorMessage("notification/core-error-message.vi.xml");
        readTranMessage("notification/tran-message.vi.xml");
        readCustomerMessage("notification/sms-message.vi.xml");
        readReactiveInfoMessage("notification/reactive-info.vi.xml");
        //BEGIN 0000000001 Send SMS to Customer.
//        readCustomerMessage("notification/sms-message.vi.xml");
        //END 0000000001 Send SMS to Customer.

        // Read SMS template for retailer
//        readSMSRetailerMessage("retailter/sms.json");

        // Read noti message for retailer
//        readNotiRetailerMessage("retailter/moti.json");
    }

    public static String getMessageByErrorCode(int id) {
        if (errorDocs == null)
            return "";
        Element element = errorDocs.getElementById(String.valueOf("id"));
        if (element == null)
            return "";
        return element.getTextContent();
    }

    public static String getTitleTranNotification(TranObj tran) {

        //dgd.start
        String key = "";
        String status;
        String result = null;

        if (tran.isRetailer == 1) {
            status = tran.error == 0 ? Retailer.success : Retailer.fail;

            if (tran.tranType == MomoProto.TranHisV1.TranType.M2M_VALUE) {
                key = Retailer.keyM2m;
            }
            result = getNotiCaptionInRetailer(key, status);
            return result;
        }
        //dgd.end

        String subfix;
        if (tran.error == 0) {
            subfix = "_SUCCESS_TITLE";
        } else {
            subfix = "_FAIL_TITLE";
        }

        switch (tran.tranType) {

            case TranTypeExt.Escape:
                result = tran.partnerRef;
                break;
            case TranTypeExt.CapDoiHoanHao:
                result = tran.partnerRef;
                break;
            case MomoProto.TranHisV1.TranType.M2M_VALUE:
                if (tran.io < 0) {
                    result = tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_SEND" + subfix);
                } else {
                    result = tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_RECEIVE" + subfix);
                }
                break;
            case MomoProto.TranHisV1.TranType.M2C_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.M2C.name() + subfix);
                break;
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.TOP_UP.name() + subfix);
                break;
            case MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.TOP_UP_GAME.name() + subfix);
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE.name() + subfix);
                break;
            case MomoProto.TranHisV1.TranType.BANK_IN_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.BANK_IN.name() + subfix);
                break;
            case MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN.name() + subfix);
                break;
            case MomoProto.TranHisV1.TranType.BANK_OUT_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.BANK_OUT.name() + subfix);
                break;
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.PAY_ONE_BILL.name() + subfix);
                break;

            //anh linh : chia thanh toan bill == thanh toan hoa don + dich vu
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE.name() + subfix);
                break;

            case MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME_VALUE:
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME.name() + subfix));
                break;
            case MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME_VALUE:
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME.name() + subfix));
                break;
            case MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL_VALUE:
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL.name() + subfix));
                break;
            case MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE:
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO.name() + subfix));
                break;
            case MomoProto.TranHisV1.TranType.BONUS_VALUE:
                JsonArray jsonArray = tran.share;
                boolean checkBonus300 = false;
                if (jsonArray != null && jsonArray.size() > 0) {
                    for (Object o : jsonArray) {
                        if (((JsonObject) o).containsField(StringConstUtil.KEY)) {
                            checkBonus300 = ((JsonObject) o).getBoolean(StringConstUtil.KEY, false);
                            break;
                        }

                    }
                }
                if (checkBonus300) {
                    result = StringConstUtil.BONUS_300_HEADER;
                    break;
                }
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BONUS.name() + subfix));
                break;
            case MomoProto.TranHisV1.TranType.BONUS_DGD_VALUE:
                result = String.format(tranMessages.get(StringConstUtil.BONUS_DGD + subfix));
                break;
//            case MomoProto.TranHisV1.TranType.BONUS_300_VALUE:
//                //<content>Quý khách đã nhận được %sđ tiền hoa hồng của của giao dịch nạp tiền điện thoại.</content>
//                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BONUS_300.name() + subfix));
//                break;
            case MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE:
                if (tran.io < 0)
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE.name() + "_SEND" + subfix));
                else
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE.name() + "_RECEIVE" + subfix));
                break;
            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PHIM123.name() + subfix));
                break;
            case MomoProto.TranHisV1.TranType.BUY_GIFT_VALUE:
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BUY_GIFT.name() + subfix));
                break;
            case MomoProto.TranHisV1.TranType.SEND_GIFT_VALUE:
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.SEND_GIFT.name() + subfix));
                break;
            case MomoProto.TranHisV1.TranType.GIFT_TO_MPOINT_VALUE:
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.GIFT_TO_MPOINT.name() + subfix));
                break;
            case MomoProto.TranHisV1.TranType.FEE_VALUE:
                try{
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.FEE.name() + subfix));
                }
                catch (Exception e)
                {

                }
                break;
            case MomoProto.TranHisV1.TranType.TRANSFER_ONE_BILL_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_SEND" + subfix);
                break;
        }
        if (result == null) {
            return String.valueOf("tranType = " + tran.tranType);
        }
        return result;
    }

    public static String getAmount(long amount) {
        return String.format("%,d", amount).replace(",", ".");
    }

    public static String getDate(long time) {
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm dd/MM/yyyy");
        return formater.format(new Date(time));
    }

    public static String getContentTranNotification(TranObj tran) {
        String result = null;
        // /dgd.start
        /*String key = "";
        String status;

        if(tran.isRetailer == 1){
            status = tran.error == 0 ? Retailer.success : Retailer.fail;

            if(tran.tranType == MomoProto.TranHisV1.TranType.M2M_VALUE){
                key=Retailer.keyM2m;
            }
            result = getNotiBodyInRetailer(key,status);
            return result;
        }*/

        //dgd.end

        String subfix;
        if (tran.error == 0) {
            subfix = "_SUCCESS_CONTENT";
        } else {
            subfix = "_FAIL_CONTENT";
        }
        if (tran.error == 0) {
            switch (tran.tranType) {
//                case MomoProto.TranHisV1. TranType.M2C_VALUE:
//                    return String.format(tranMessages.get(MomoProto.TranHisV1. TranType.M2C.name() + subfix), tran.partnerName);
            }
        }

        // fail transaction & default message
        switch (tran.tranType) {

            case TranTypeExt.CapDoiHoanHao:

                if (tran.error == 0) {
                    String[] arr = tran.billId.split(MomoMessage.BELL);
                    //String str = "Bạn đã bình chọn cho cặp " + arr[0] + ".  " + arr[1] + ". Số lượng bình chọn: " + arr[2];
                    String tmp = "Bạn đã bình chọn thành công cho SBD%s với số lượng: %s tin nhắn. Bình chọn bằng Ví MoMo bạn có cơ hội nhận được giải thưởng là 10 triệu cho đêm liveshow, 5 triệu cho tuần do Ví MoMo tặng. LH: 0839917199";
                    result = String.format(tmp, "0" + DataUtil.strToInt(arr[0]), arr[2] + "");

                } else {
                    result = tran.comment;
                }
                break;

            //su dung cho cac trantype ben ngoai
            case TranTypeExt.Escape:
                String serviceName = tran.billId;
                String[] ar = serviceName.split(MomoMessage.BELL);
                //Bạn đã đặt mua thành công [tên của gói]. Mã đặt vé: MOEH(6 số). Bộ phận bán vé của Escape Halloween sẽ liên hệ với bạn để xác nhận thời gian giao vé sớm nhất. Gọi hotline 0909404490 để biết thêm chi tiết.
                String tpl = "Bạn đã đặt mua thành công vé %s. Mã đặt vé: MOEH%s. Bộ phận bán vé của Escape Halloween sẽ liên hệ với bạn để xác nhận thời gian giao vé sớm nhất. Gọi hotline 0909404490 để biết thêm chi tiết.";
                result = String.format(tpl, ar[0], ar[1]);

                break;

            case MomoProto.TranHisV1.TranType.M2M_VALUE:
                long fee = 0;
                //Insert fee
                if (tran != null) {
                    JsonObject jsonKvp = tran.kvp;
                    fee = jsonKvp.getInteger(Const.AppClient.Fee);
                }
                tran.amount = tran.amount - fee;
                JsonArray array = tran.share;
                JsonObject jsonOldAmount = null;
                long oldAmount = 0;
                if (array != null && array.size() > 0) {
                    for (int i = 0; i < array.size(); i++) {
                        jsonOldAmount = array.get(i);
                        if (jsonOldAmount.containsField(Const.AppClient.OldAmount)) {
                            oldAmount = jsonOldAmount.getLong(Const.AppClient.OldAmount, 0);
                            break;
                        }
                    }
                }
                //kiem tra neu la so phone --> tra dung dinh dang so phone
                int phoneNumber = DataUtil.strToInt(tran.partnerId);
                if (phoneNumber > 0) {
                    tran.partnerId = "0" + phoneNumber;
                }

                if (tran.io < 0) {
                    //<content>Quý khách đã chuyển thành công số tiền %sđ cho %s(%s).</content>
                    if (oldAmount == 0) {
                        result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_SEND" + subfix),
                                getAmount(tran.amount), tran.partnerName, tran.partnerId);
                    } else {
                        result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_SEND" + subfix),
                                getAmount(oldAmount), tran.partnerName, tran.partnerId);
                    }
                } else {
                    //<content>Quý khách đã nhận thành công số tiền %sđ từ %s(%s).</content>
                    if ("0".equals(tran.partnerId)) {
                        if (oldAmount == 0) {
                            result = String.format("Quý khách đã nhận thành công số tiền %sđ từ %s.",
                                    getAmount(tran.amount), tran.partnerName, tran.partnerId);
                        } else {
                            result = String.format("Quý khách đã nhận thành công số tiền %sđ từ %s.",
                                    getAmount(oldAmount), tran.partnerName, tran.partnerId);
                        }
                        if (tran.comment != null && !tran.comment.isEmpty()) {
                            result += " Nội dung: \"" + tran.comment + "\".";
                        }
                    } else {
                        result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_RECEIVE" + subfix),
                                getAmount(tran.amount), tran.partnerName, tran.partnerId);
                        if (tran.comment != null && !tran.comment.isEmpty()) {
                            result += " Nội dung: \"" + tran.comment + "\".";
                        }
                    }
                }
//                result = String.format(tranMessages.get(MomoProto.TranHisV1. TranType.M2M.name() + subfix), "0" + tran.partnerId, getAmount(tran));
                break;
            case MomoProto.TranHisV1.TranType.M2C_VALUE:
                //<content>Quý khách đã chuyển thành công số tiền %sđ tới số điện thoại %s.</content>
                if (tran.error == 0)
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2C.name() + subfix),
                            getAmount(tran.amount), tran.partnerName);
                else
                    //<content>Giao dịch chuyển số tiền %sđ cho số điện thoại %s của quý khách không thành công. Số tiền hoàn trả: %sđ.</content>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2C.name() + subfix),
                            getAmount(tran.amount), tran.partnerId, getAmount(tran.amount));
                break;
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
                //<content>Quý khách đã nạp thành công số tiền %sđ cho số điện thoại %s.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TOP_UP.name() + subfix),
                        getAmount(tran.amount), tran.parterCode);
                break;
            case MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE:
                //<content>Quý khách đã nạp thành công số tiền %sđ cho tài khoản %s, nhà cung cấp %s.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TOP_UP_GAME.name() + subfix),
                        getAmount(tran.amount), tran.parterCode, getProviderNameFromProviderId(tran.partnerId));
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE:
                //<content>Quý khách đã thanh toán thành công số tiền %sđ cho nhà cung cấp %s, mã đặt chỗ %s.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE.name() + subfix),
                        getAmount(tran.amount), tran.partnerName, tran.billId);
                break;
            case MomoProto.TranHisV1.TranType.BANK_IN_VALUE:
                //<content>Quý khách đã nạp thành công số tiền %sđ qua %s.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BANK_IN.name() + subfix),
                        getAmount(tran.amount), tran.partnerName);
                break;
            case MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE:
                //Quý khách đã nạp thành công số tiền 100.000đ từ thẻ Visa – 1234

                String cardLabel = "";
                for (Object o : tran.share) {
                    JsonObject jo = (JsonObject) o;
                    if (jo.containsField(VMConst.cardLabel)) {
                        cardLabel = jo.getString(VMConst.cardLabel);
                    }
                }

                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN.name() + subfix),
                        getAmount(tran.amount), cardLabel);
                break;
            case MomoProto.TranHisV1.TranType.BANK_OUT_VALUE:
                //<content>Quý khách đã rút thành công số tiền %sđ qua %s.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BANK_OUT.name() + subfix),
                        getAmount(tran.amount), tran.partnerName);
                break;
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:

                //<content>Quý khách đã thanh toán thành công hóa đơn %s của nhà cung cấp %s, số tiền %sđ.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PAY_ONE_BILL.name() + subfix),
                        tran.billId, tran.partnerName, getAmount(tran.amount));
                break;

            //linh : chia thanh toan = thanh toan hoa don va dich vu
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE_VALUE:
                //<content>Quý khách đã thanh toán thành công %sđ cho dịch vụ %s. Mã thanh toán: %s.</content>
//                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE.name() + subfix),
//                        getAmount(tran.amount),tran.partnerName,tran.billId);
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE.name() + subfix),
                        getAmount(tran.amount), tran.partnerName);
                break;

            case MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME_VALUE:
                //<content>Quý khách đã nạp thành công số tiền %sđ tại %s.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME.name() + subfix),
                        getAmount(tran.amount), tran.partnerRef);
                break;
            case MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME_VALUE:
                //<content>Quý khách đã rút thành công số tiền %sđ tại %s.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME.name() + subfix),
                        getAmount(tran.amount), tran.partnerRef);
                break;
            case MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL_VALUE:
                //<content>Quý khách đã rút thành công số tiền %sđ về ngân hàng %s.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL.name() + subfix),
                        getAmount(tran.amount), tran.partnerRef);
                break;
            case MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE:
                //<content>Quý khách đã nạp thành công số tiền %sđ từ ngân hàng %s.</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO.name() + subfix),
                        getAmount(tran.amount), tran.partnerName);
                break;
            case MomoProto.TranHisV1.TranType.BONUS_VALUE:
                //<content>Quý khách đã nhận được %sđ tiền hoa hồng của của giao dịch nạp tiền điện thoại.</content>
                JsonArray jsonArray = tran.share;
                boolean checkBonus300 = false;
                if (jsonArray != null && jsonArray.size() > 0) {
                    for (Object o : jsonArray) {
                        if (((JsonObject) o).containsField(StringConstUtil.KEY)) {
                            checkBonus300 = ((JsonObject) o).getBoolean(StringConstUtil.KEY, false);
                            break;
                        }

                    }
                }
                if (checkBonus300) {
                    result = StringConstUtil.BONUS_300_CONTENT;
                    break;
                }

                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BONUS.name() + subfix),
                        getAmount(tran.amount));

                break;
            case MomoProto.TranHisV1.TranType.BONUS_DGD_VALUE:
                result = String.format(tranMessages.get(StringConstUtil.BONUS_DGD + subfix),
                        getAmount(tran.amount));
                break;
            case MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE:
                //<content>Quý khách đã rút thành công số tiền %sđ.</content>
                if (tran.io < 0)
                    //<content>Quý khách đã rút thành công số tiền %sđ.</content>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE.name() + "_SEND" + subfix),
                            getAmount(tran.amount));
                else
                    //<content>Quý khách nhận thành công số tiền %sđ từ %s(%s).</content>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE.name() + "_RECEIVE" + subfix),
                            getAmount(tran.amount), tran.partnerName, tran.partnerId);

                break;
            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
                //<content>Quý khách đã đặt vé xem phim thành công. Tên phim: %s, %s</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PHIM123.name() + subfix),
                        getFilmName(tran), getChairId(tran));
                break;
            case MomoProto.TranHisV1.TranType.BUY_GIFT_VALUE:
                //<content>Quý khách đã đặt vé xem phim thành công. Tên phim: %s, %s</content>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BUY_GIFT.name() + subfix),
                        getFilmName(tran), getChairId(tran));
                break;
            case MomoProto.TranHisV1.TranType.SEND_GIFT_VALUE:
                //<content>Quý khách đã đặt vé xem phim thành công. Tên phim: %s, %s</content>
                result = tranMessages.get(MomoProto.TranHisV1.TranType.SEND_GIFT.name() + subfix);
                break;
            case MomoProto.TranHisV1.TranType.GIFT_TO_MPOINT_VALUE:
                result = tranMessages.get(MomoProto.TranHisV1.TranType.GIFT_TO_MPOINT.name() + subfix);
                break;
            case MomoProto.TranHisV1.TranType.FEE_VALUE:
//                result = DataUtil.stringFormat(tranMessages.get(MomoProto.TranHisV1.TranType.FEE.name() + subfix))
//                        .put("fee", tran.amount)
//                        .toString();
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.FEE.name() + subfix), getAmount(tran.amount) + "");
                break;
            case MomoProto.TranHisV1.TranType.TRANSFER_ONE_BILL_VALUE:
                result = tran.comment;
                break;
        }

        if (result == null) {
            return String.valueOf("tranType = " + tran.tranType);
        }
        return result;
    }

    private static String getNotiBodyInRetailer(String key, String status) {

        String result = "";
        Retailer retailer = (Retailer) hashMapNotiRetailer.get(key);
        if (retailer == null) {
            result = "No body for this noti";
        } else {
            if (status.equalsIgnoreCase(Retailer.success)) {
                result = retailer.Success.body;
            } else {
                result = retailer.Fail.body;
            }
        }
        return result;
    }

    private static String getNotiCaptionInRetailer(String key, String status) {

        String result = "";
        Retailer retailer = hashMapNotiRetailer.get(key);
        if (retailer == null) {
            result = "No caption for this noti";
        } else {
            if (status.equalsIgnoreCase(Retailer.success)) {
                result = retailer.Success.caption;
            } else {
                result = retailer.Fail.caption;
            }
        }
        return result;
    }

    private static String getFilmName(TranObj tran) {
        String[] arr = tran.partnerName.split(MomoMessage.BELL);
        if (arr.length >= 2)
            return arr[1];
        return "UNKNOWN";
    }

    public static String getChairId(TranObj tran) {
        return tran.comment.substring(21);
    }

    public static String getSmsContent(TranObj tran) {
        String subfix;
        if (tran.error == 0) {
            subfix = "_SUCCESS_SMS";
        } else {
            subfix = "_FAIL_SMS";
        }
        if (tran.error == 0) {
            switch (tran.tranType) {
//                case MomoProto.TranHisV1. TranType.M2C_VALUE:
//                    return String.format(tranMessages.get(MomoProto.TranHisV1. TranType.M2C.name() + subfix), tran.partnerName);
            }
        }
        String result = null;
        // fail transaction & default message
        switch (tran.tranType) {

            case TranTypeExt.Escape:
                result = "";
                break;
            case TranTypeExt.CapDoiHoanHao:
                result = "";
                break;

            case MomoProto.TranHisV1.TranType.M2M_VALUE:
                if (!tran.partnerId.startsWith("0")) {
                    try {
                        int phone = Integer.parseInt(tran.partnerId);
                        tran.partnerId = "0" + tran.partnerId;
                    } catch (NumberFormatException e) {
                    }
                }
                if (tran.io < 0) {
                    if (tran.error == 0)
                        //<sms>Chuc mung quy khach da chuyen thanh cong so tien %sd den %s (%s) luc: %s. TID:%s. Xin cam on.</sms>
                        result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_SEND" + subfix),
                                getAmount(tran.amount), tran.partnerName, tran.partnerId, getDate(tran.finishTime), String.valueOf(tran.tranId));
                    else
                        //<sms>Xin loi quy khach, giao dich chuyen tien toi %s(%s) khong thanh cong. TID: %s. Xin vui long thu lai.</sms>
                        result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_SEND" + subfix),
                                tran.partnerName, tran.partnerId, String.valueOf(tran.tranId));
                } else {
                    if ("0".equals(tran.partnerId)) {
                        if (tran.error == 0)
                        {
                            //<sms>Chuc mung quy khach da nhan duoc %sd tu %s(%s) luc %s. TID: %s. Xin cam on.</sms>
//                           QK da nhan <so_tien_GD>d tu <ten_nguoi_gui>. Ma GD: <ma_giao_dich>. Loi nhan: <15 ki tu dau tien trong tin nhan của nguoi gui ke ca khoang trang>... Vui long dang nhap Vi MoMo de xem chi tiet.
                            String content = tran.comment.length() > 15 ? Misc.removeAccentWithoutUpper(Normalizer.normalize(tran.comment.substring(0, 15), Normalizer.Form.NFC)) : Misc.removeAccentWithoutUpper((Normalizer.normalize(tran.comment, Normalizer.Form.NFC)));
                            try{
                                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_RECEIVE" + subfix),
                                        getAmount(tran.amount), tran.partnerName, String.valueOf(tran.tranId), content);
                            }
                            catch (Exception ex)
                            {
                                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_RECEIVE" + subfix),
                                        getAmount(tran.amount), tran.partnerName, getDate(tran.finishTime), String.valueOf(tran.tranId));
                            }

                        }
                        else
                            //<sms>Xin loi quy khach, giao dich nhan so tien %sd tu %s(%s) khong thanh cong. TID: %s. Xin vui long thu lai.</sms>
                            result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_RECEIVE" + subfix),
                                    getAmount(tran.amount), tran.partnerName, tran.partnerId, String.valueOf(tran.tranId));

                    }
                    else {
                        if (tran.error == 0)
                        {

                            //<sms>Chuc mung quy khach da nhan duoc %sd tu %s(%s) luc %s. TID: %s. Xin cam on.</sms>
                            try{
                                String content = tran.comment.length() > 15 ? Misc.removeAccentWithoutUpper(Normalizer.normalize(tran.comment.substring(0, 15), Normalizer.Form.NFC)) : Misc.removeAccentWithoutUpper((Normalizer.normalize(tran.comment, Normalizer.Form.NFC)));
                                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_RECEIVE" + subfix),
                                        getAmount(tran.amount), tran.partnerName, String.valueOf(tran.tranId), content);
                            }
                            catch (Exception ex)
                            {
                                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_RECEIVE" + subfix),
                                        getAmount(tran.amount), tran.partnerName, getDate(tran.finishTime), String.valueOf(tran.tranId));
                            }
                        }
                        else
                            //<sms>Xin loi quy khach, giao dich nhan so tien %sd tu %s(%s) khong thanh cong. TID: %s. Xin vui long thu lai.</sms>
                            result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2M.name() + "_RECEIVE" + subfix),
                                    getAmount(tran.amount), tran.partnerName, tran.partnerId, String.valueOf(tran.tranId));
                    }
                }
//                result = String.format(tranMessages.get(MomoProto.TranHisV1. TranType.M2M.name() + subfix), "0" + tran.partnerId, getAmount(tran));
                break;
            case MomoProto.TranHisV1.TranType.M2C_VALUE:
                if (tran.error == 0)
                    //<sms>Chuc mung quy khach, giao dich chuyen so tien %sd den so %s da thanh cong luc %s. TID: %s. Xin cam on.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2C.name() + subfix),
                            getAmount(tran.amount), tran.partnerId, getDate(tran.finishTime), String.valueOf(tran.tranId));
                else
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.M2C.name() + subfix),
                            getAmount(tran.amount), tran.partnerId, String.valueOf(tran.tranId));
                break;
            case MomoProto.TranHisV1.TranType.TOP_UP_VALUE:
                if (tran.error == 0)
                    //<sms>Chuc mung quy khach da nap thanh cong %sd cho so dien thoai %s luc %s. TID: %s. Xin cam on.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TOP_UP.name() + subfix),
                            getAmount(tran.amount), tran.parterCode, getDate(tran.finishTime), String.valueOf(tran.tranId));
                else
                    //<sms>Xin loi quy khach, giao dich nap tien cho so dien thoai %s khong thanh cong. TID: %s. Xin vui long thu lai.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TOP_UP.name() + subfix),
                            tran.partnerId, String.valueOf(tran.tranId));
                break;
            case MomoProto.TranHisV1.TranType.TOP_UP_GAME_VALUE:
                if (tran.error == 0)
                    //<sms>Chuc mung quy khach da nap thanh cong %sd cho tai khoan %s, nha cung cap %s, luc %s. TID: %s. Xin cam on.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TOP_UP_GAME.name() + subfix),
                            getAmount(tran.amount), tran.parterCode, getProviderNameFromProviderId(tran.partnerId), getDate(tran.finishTime), String.valueOf(tran.tranId));
                else
                    //<sms>Xin loi quy khach, giao dich nap tien cho tai khoan %s, nha cung cap %s, khong thanh cong. TID: %s. Xin vui long thu lai.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TOP_UP_GAME.name() + subfix),
                            tran.parterCode, getProviderNameFromProviderId(tran.partnerId), String.valueOf(tran.tranId));
                break;
            case MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE_VALUE:
                if (tran.error == 0)
                    //<sms>Chuc mung quy khach da thanh toan thanh cong so tien %sd cho ve may bay %s. TID: %s. Xin cam on!</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE.name() + subfix),
                            getAmount(tran.amount), tran.billId, String.valueOf(tran.tranId));
                else
                    //<sms>Xin loi quy khach, giao dich thanh toan ve may bay %s khong thanh cong. TID: %s. Xin vui long thu lai.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BILL_PAY_TICKET_AIRLINE.name() + subfix),
                            tran.billId, String.valueOf(tran.tranId));
                break;
            case MomoProto.TranHisV1.TranType.BANK_IN_VALUE:
                //<sms>Chuc mung quy khach da nap thanh cong %sd tu tai khoan ngan hang %s vao tai khoan MoMo luc %s. TID: %s. Xin cam on.</sms>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BANK_IN.name() + subfix),
                        getAmount(tran.amount), tran.partnerName, getDate(tran.finishTime), String.valueOf(tran.tranId));
                break;

            case MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN_VALUE:
                //<sms>Chuc mung quy khach da nap thanh cong %sd tu tai khoan ngan hang %s vao tai khoan MoMo luc %s. TID: %s. Xin cam on.</sms>
                String cardLabel = "";
                for (Object o : tran.share) {
                    JsonObject jo = (JsonObject) o;
                    if (jo.containsField(VMConst.cardLabel)) {
                        cardLabel = jo.getString(VMConst.cardLabel);
                    }
                }

                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.VM_PROCESS_CASH_IN.name() + subfix),
                        getAmount(tran.amount), cardLabel, getDate(tran.finishTime), String.valueOf(tran.tranId));
                break;

            case MomoProto.TranHisV1.TranType.BANK_OUT_VALUE:
                if (tran.error == 0)
                    //<sms>Chuc mung quy khach da rut thanh cong %sd tu tai khoan MoMo sang tai khoan ngan hang %s luc %s. TID: %s. Xin cam on.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BANK_OUT.name() + subfix),
                            getAmount(tran.amount), tran.partnerName, getDate(tran.finishTime), String.valueOf(tran.tranId));
                else
                    //<sms>Xin loi quy khach, giao dich rut tien tu tai khoan MoMo sang tai khoan ngan hang %s chua thanh cong. TID: %s. Xin vui long thu lai.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BANK_OUT.name() + subfix),
                            tran.partnerName, getDate(tran.finishTime), String.valueOf(tran.tranId));
                break;
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_VALUE:
                if (tran.error == 0)
                    //<sms>Chuc mung quy khach da thanh toan thanh cong hoa don %s cua nha cung cap %s, so tien %sd. TID: %s. Xin cam on</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PAY_ONE_BILL.name() + subfix),
                            tran.billId, tran.partnerId, getAmount(tran.amount), String.valueOf(tran.tranId)); // Sua parnetName -> tran.partnerId
                else
                    //<sms>Xin loi quy khach, giao dich thanh toan hoa don %s voi so tien %sd cua dich vu %s khong thanh cong. TID: %s. Xin vui long thu lai.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PAY_ONE_BILL.name() + subfix),
                            tran.billId, getAmount(tran.amount), tran.partnerId, String.valueOf(tran.tranId));
                break;

            //linh : chia thanh toan hoa don = thanh toan hoa don + dich vu
            case MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE_VALUE:
                if (tran.error == 0)
                    //<sms>Chuc mung quy khach da thanh toan thanh cong %sd cho dich vu %s. Ma thanh toan: %s. TID: %s. Xin cam on!</sms>
//                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE.name() + subfix),
//                            getAmount(tran.amount),tran.billId, tran.partnerName, tran.billId, String.valueOf(tran.tranId));
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PAY_ONE_BILL_SERVICE.name() + subfix),
                            getAmount(tran.amount), tran.billId, tran.partnerName, String.valueOf(tran.tranId));
                else
                    //<sms></sms>
                    result = "";
                break;

            case MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME_VALUE:
                //<sms>Chuc mung quy khach da nap so tien %sd tai %s thanh cong. TID: %s. Xin cam on.</sms>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.DEPOSIT_AT_HOME.name() + subfix),
                        getAmount(tran.amount), tran.partnerRef, String.valueOf(tran.tranId));
                break;
            case MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME_VALUE:
                //<sms>Chuc mung quy khach da rut thanh cong so tien %sd tai %s. TID: %s. Xin cam on.</sms>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.WITHDRAW_AT_HOME.name() + subfix),
                        getAmount(tran.amount), tran.partnerRef, String.valueOf(tran.tranId));
                break;
            case MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL_VALUE:
                if (tran.error == 0)
                    //<sms>Chuc mung quy khach da rut thanh cong so tien %sd tu tai khoan MoMo ve tai khoan ngan hang %s luc %s. TID: %s. Xin cam on.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL.name() + subfix),
                            getAmount(tran.amount), tran.partnerName, getDate(tran.finishTime), String.valueOf(tran.tranId));
                else
                    //<sms>Xin loi quy khach, giao dich rut so tien %sd tu tai khoan MoMo ve tai khoan ngan hang %s cua quy khach chua thanh cong. TID: %s. Xin vui long thu lai.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.MOMO_TO_BANK_MANUAL.name() + subfix),
                            getAmount(tran.amount), tran.partnerName, String.valueOf(tran.tranId));
                break;
            case MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO_VALUE:
                //
                if (tran.error == 0)
                    //<sms>Chuc mung quy khach da nap thanh cong so tien %sd tu tai khoan ngan hang %s vao tai khoan MoMo luc %s. TID: %s. Xin cam on.</sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO.name() + subfix),
                            getAmount(tran.amount), tran.partnerName, getDate(tran.finishTime), String.valueOf(tran.tranId));
                else
                    //<sms>Xin loi quy khach, giao dich nap so tien %sd tu tai khoan ngan hang %s vao tai khoan MoMo cua quy khach khong thanh cong. TID: %s. Xin vui long thu lai. </sms>
                    result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BANK_NET_TO_MOMO.name() + subfix),
                            getAmount(tran.amount), tran.partnerName, String.valueOf(tran.tranId));
                break;
            case MomoProto.TranHisV1.TranType.BONUS_VALUE:
                //<sms>Chuc mung quy khach vua nhan duoc %sd tien hoa hong cua dich vu. So du MoMo: %sd. Xin cam on.</sms>
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BONUS.name() + subfix),
                        getAmount(tran.amount));


                break;
            case MomoProto.TranHisV1.TranType.BONUS_DGD_VALUE:
                result = String.format(tranMessages.get(StringConstUtil.BONUS_DGD + subfix),
                        getAmount(tran.amount));
                break;
//            case MomoProto.TranHisV1.TranType.BONUS_300_VALUE:
//                //<content>Quý khách đã nhận được %sđ tiền hoa hồng của của giao dịch nạp tiền điện thoại.</content>
//                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.BONUS_300.name() + subfix));
//                break;
            case MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE_VALUE:
                if (tran.io < 0) {
                    if (tran.error == 0)
                        //<sms>Chuc mung quy khach da chuyen thanh cong so tien %sd den %s(%s) luc %s. TID: %s. Xin cam on.</sms>
                        result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE.name() + "_SEND" + subfix),
                                getAmount(tran.amount), tran.partnerName, tran.partnerId, getDate(tran.finishTime), String.valueOf(tran.tranId));
                    else
                        //<sms>Giao dich chuyen tien den %s khong thanh cong. TID: %s. Xin cam on.</sms>
                        result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE.name() + "_SEND" + subfix),
                                tran.partnerId, String.valueOf(tran.tranId));
                } else {
                    if (tran.error == 0)
                        //<sms>Chuc mung quy khach da nhan thanh cong so tien %sd tu %s(%s) luc %s. TID: %s. Xin cam on.</sms>
                        result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE.name() + "_RECEIVE" + subfix),
                                getAmount(tran.amount), tran.partnerName, tran.partnerId, getDate(tran.finishTime), String.valueOf(tran.tranId));
                    else
                        //<sms>Giao dich nhan tien tu %s khong thanh cong. TID: %s. Xin cam on.</sms>
                        result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.TRANSFER_MONEY_TO_PLACE.name() + "_RECEIVE" + subfix),
                                tran.partnerId, String.valueOf(tran.tranId));
                }
                break;
            case MomoProto.TranHisV1.TranType.PHIM123_VALUE:
                result = String.format(tranMessages.get(MomoProto.TranHisV1.TranType.PHIM123.name() + subfix),
                        getFilmName(tran), getChairId(tran), String.valueOf(tran.tranId));
                break;
        }

        if (result == null) {
            return String.valueOf("tranType = " + tran.tranType);
        }
        return result;
    }

    public static Notification build(JsonObject json) {
        int type = json.getInteger("type", -1);
        switch (type) {
            case TYPE_TRAN:
                return createNotificationTran(json);
            case TYPE_MONEY_REQUEST:
                return createMoneyRequestNotification(json);
            default:
                break;
        }
        return null;
    }

    private static Notification createNotificationTran(JsonObject json) {
        if (json == null)
            return null;
        Notification notification = new Notification();
        TranObj tran = new TranObj(json.getObject("trans"));


        //Skip this tran:
        if (tran.tranType == MomoProto.TranHisV1.TranType.SEND_GIFT_VALUE
                || tran.tranType == MomoProto.TranHisV1.TranType.BUY_GIFT_VALUE)
            return notification;

        //linh lam
        if (tran.tranType == TranTypeExt.CapDoiHoanHao)
            return notification;

        if (tran.tranType == TranTypeExt.Fsc2014)
            return notification;


//        notification.id = ""; //id will be set by mongoDb
        notification.priority = json.getInteger("priority", 2);
        notification.type = MomoProto.NotificationType.NOTI_TRANSACTION_VALUE;
        notification.caption = getTitleTranNotification(tran);
        notification.body = getContentTranNotification(tran);
        notification.sms = getSmsContent(tran);
        notification.tranId = tran.tranId;
        notification.cmdId = tran.cmdId;
        notification.time = new Date().getTime();
        notification.receiverNumber = json.getInteger("phoneNumber");
        return notification;
    }

    private static Notification createMoneyRequestNotification(JsonObject json) {
        if (json == null)
            return null;
        Notification notification = new Notification();
//        notification.id = ""; //id will be set by mongoDb
        notification.priority = json.getInteger("priority", 2);
        notification.type = MomoProto.NotificationType.NOTI_MONEY_REQUEST_VALUE;
        notification.caption = "Yêu cầu mượn tiền" + MomoMessage.BELL + json.getString("fromUserName", "") + MomoMessage.BELL + json.getLong("amount", 0) + MomoMessage.BELL + json.getString("comment", "");
        notification.body = String.format(json.getString("fromUserName", "") + "(%s) yêu cầu mượn tiền từ bạn. Số tiền: %s. Nội dung \"%s\".",
                "0" + String.valueOf(json.getNumber("fromPhone")),
                //json.getString("fromUserName", "") +
                getAmount(json.getLong("amount", 0)),
                json.getString("comment", ""));

        notification.sms = StringUtils.stripAccents(notification.body).replace("đ", "d");
        notification.tranId = 0L;
        notification.sender = json.getInteger("fromPhone", 0);
        notification.cmdId = json.getLong("cmdId", 0L);
        notification.time = new Date().getTime();
        notification.receiverNumber = json.getInteger("phoneNumber");

        return notification;
    }

    public static HashMap<String, String> getReactiveInfo(String group)
    {
        HashMap<String, String> data;
        data = hashMapReactiveInfo.containsKey(group) ? hashMapReactiveInfo.get(group) : new HashMap<String, String>();
        return data;
    }
    public static JsonObject createRequestPushNotification(int phoneNumber
            , int priority
            , TranObj tran) {
        if (tran == null) {
            throw new IllegalArgumentException("TranObj can't be null.");
        }
        JsonObject json = new JsonObject();
        json.putNumber("type", TYPE_TRAN);
        json.putNumber("phoneNumber", phoneNumber);
        json.putNumber("priority", priority);

        json.putObject("trans", tran.getJSON());

        return build(json).toFullJsonObject();
    }

    public static void main(String args[]) {
        init();
        TranObj tran = new TranObj();
        tran.tranType = MomoProto.TranHisV1.TranType.TOP_UP_VALUE;
        tran.amount = 23123L;
        tran.parterCode = "asdfasdf";
        System.out.println(getContentTranNotification(tran));
    }

    private static String getProviderNameFromProviderId(String id) {
        String name = providerName.get(id);
        return name == null ? "" : name;
    }

    //dgd.start
    public static class RetailerNoti {
        public String caption = "";
        public String body = "";
        public String sms = "";
    }

    public static class Retailer {
        public static final String fail = "fail";
        public static final String success = "success";
        public static final String keyM2m = "m2m";

        public RetailerNoti Success;
        public RetailerNoti Fail;
    }
}
