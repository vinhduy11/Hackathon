package com.mservice.momo.clientform;

import com.mservice.momo.data.BillInfoService;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.data.model.colName;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.form.FieldItem;
import com.mservice.momo.vertx.form.FormObj;
import com.mservice.momo.vertx.form.RequestObj;
import com.mservice.momo.vertx.gift.GiftManager;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.*;

/**
 * Created by concu on 11/11/14.
 */
public class TranslateForm {

    private static final String BILLID_KEY = "billid";
    private static final String NAME_ACCOUNT_KEY = "nameAccount";
    private static final String CAPTION_CONFIRM = "Xác nhận thanh toán";
    private static final String BUTTON_CONFIRM = "Đồng ý";
    private static final String AMOUNT_CONFIRM = "Số tiền";
    private static final String PRICE_CONFIRM = "Mệnh giá thẻ";
    private static final String QUANTITY_CONFIRM = "Số lượng thẻ";
    private static final String ACCOUNT_CONFIRM = "Tài khoản";
    private static final String KEY_KEY = "key";
    private static final String VALUE_KEY = "value";
    private final Logger logger;
    private final Vertx vertx;
    private final JsonObject glbCfg;
    private GiftManager giftManager;

    public TranslateForm(Logger logger, Vertx vertx, JsonObject glbCfg) {
        this.logger = logger;
        this.vertx = vertx;
        this.glbCfg = glbCfg;
    }

    public void translateKaspersky(Message message, RequestObj reqObj) {
        //todo

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            String pCode = hm.get(Const.AppClient.BillId);
            String pName = "";
            switch (pCode) {
                case "KAV1U":
                    pName = "Kaspersky Anti-Virus 2015 - 1PC/năm";
                    break;
                case "KAV3U":
                    pName = "Kaspersky Anti-Virus 2015 - 3PC/năm";
                    break;
                case "KIS1U":
                    pName = "Kaspersky Internet Security 2015 - 1PC/năm";
                    break;
                case "KIS3U":
                    pName = "Kaspersky Internet Security 2015 - 3PC/năm";
                    break;
                case "KISMAC":
                    pName = "Kaspersky Anti-Virus 2015 for Mac - 1PC/năm";
                    break;
                case "KISMD":
                    pName = "Kaspersky Internet Security - Multi Devices";
                    break;
                case "KISANDROID":
                    pName = "Kaspersky Internet Security for Android  1 năm/máy";
                    break;
                default:
                    break;
            }

            arrayList.add(Misc.buildTextValue("Tên sản phẩm", pName));
        }
        if (hm.containsKey(Const.AppClient.BillId)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmFimplus(Message message, RequestObj reqObj) {
        //todo

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            String pCode = hm.get(Const.AppClient.BillId);
            arrayList.add(Misc.buildTextValue("Tài khoản", pCode.replaceAll("\\.", "")));
        }
        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmCDHH(Message message, RequestObj reqObj) {
        //todo
        HashMap hm = reqObj.hashMap;
        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận bình chọn"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã cặp đôi", hm.get(Const.AppClient.BillId).toString()));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount).toString());
            long smsAmt = (amt / 5000);
            arrayList.add(Misc.buildTextValue("Số tin nhắn", String.valueOf(smsAmt)));
        }

        message.reply(Misc.getJsonArray(arrayList));

    }

    public void translateSubmitFrmVNIDOL(Message message, RequestObj reqObj) {
        //todo
        HashMap hm = reqObj.hashMap;
        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
        logger.info("translateSubmitFrmVNIDOL-------------->" + hm);
        arrayList.add(Misc.getCaptionConfirm("Xác nhận bình chọn"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            String name = hm.get(Const.AppClient.BillId).toString().substring(3);
            arrayList.add(Misc.buildTextValue("Tên thí sinh", name));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount).toString());
            long smsAmt = (amt / 3000);
            arrayList.add(Misc.buildTextValue("Số tin nhắn", String.valueOf(smsAmt)));
        }
        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmTHEVOICE(Message message, RequestObj reqObj) {
        //todo
        HashMap hm = reqObj.hashMap;
        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận bình chọn"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            String name = hm.get(Const.AppClient.BillId).toString().substring(3);
            arrayList.add(Misc.buildTextValue("MSBC (Tên thí sinh)", name));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount).toString());
            long smsAmt = (amt / 3000);
            arrayList.add(Misc.buildTextValue("Số tin nhắn", String.valueOf(smsAmt)));
        }
        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmEvent(final Message message, final RequestObj reqObj) {
        //todo
        final HashMap hm = reqObj.hashMap;
        final ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        Common.ServiceReq serviceReq = new Common.ServiceReq();
        serviceReq.ServiceId = reqObj.serviceid;
        serviceReq.Command = Common.ServiceReq.COMMAND.GET_SERVICE_DETAIL_BY_SERVICE_ID;
        Misc.getServiceInfo(vertx, serviceReq, new Handler<JsonArray>() {
            @Override
            public void handle(JsonArray jsonArray) {

                String captionTitle = "Xác nhận bình chọn";
                String buttonTitle = "Đồng ý";
                String billIdTitle = "Tên thí sinh";
                String amountTitle = "Số tin nhắn";

                if (jsonArray != null && jsonArray.size() > 0) {
                    for (Object serviceDetailObj : jsonArray) {
                        String type = ((JsonObject) serviceDetailObj).getString(colName.ServiceDetailCols.FIELD_TYPE, "");
                        if (type.equalsIgnoreCase("caption")) // Lay caption
                        {
                            captionTitle = ((JsonObject) serviceDetailObj).getString(colName.ServiceDetailCols.FIELD_LABEL, captionTitle);
                        } else if (type.equalsIgnoreCase("button")) // lay ten button
                        {
                            buttonTitle = ((JsonObject) serviceDetailObj).getString(colName.ServiceDetailCols.FIELD_LABEL, buttonTitle);
                        } else if (type.equalsIgnoreCase("text")) // lay thong tin tieu de (ten thi sinh)
                        {
                            billIdTitle = ((JsonObject) serviceDetailObj).getString(colName.ServiceDetailCols.FIELD_LABEL, billIdTitle);
                        } else if (type.equalsIgnoreCase("number"))  // Lay tieu de thong tin tin nhan.
                        {
                            amountTitle = ((JsonObject) serviceDetailObj).getString(colName.ServiceDetailCols.FIELD_LABEL, amountTitle);
                        }
                    }
                }

                arrayList.add(Misc.getCaptionConfirm(captionTitle));
                arrayList.add(Misc.getButtonConfirm(buttonTitle));

                if (hm.containsKey(Const.AppClient.BillId)) {
                    String name = hm.get(Const.AppClient.BillId).toString().substring(3);
                    arrayList.add(Misc.buildTextValue(billIdTitle, name));
                }

                if (hm.containsKey(Const.AppClient.Amount)) {
                    long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount).toString());
                    long smsAmt = (amt / 3000);
                    arrayList.add(Misc.buildTextValue(amountTitle, String.valueOf(smsAmt)));
                }
                message.reply(Misc.getJsonArray(arrayList));
            }
        });
    }


    public void translateSubmitFrmBNHV(Message message, RequestObj reqObj) {
        //todo
        HashMap hm = reqObj.hashMap;
        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận bình chọn"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã cặp đôi", hm.get(Const.AppClient.BillId).toString()));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount).toString());
            long smsAmt = (amt / 4000);
            arrayList.add(Misc.buildTextValue("Số tin nhắn", String.valueOf(smsAmt)));
        }
        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmREMIX(Message message, RequestObj reqObj) {
        //todo
        HashMap hm = reqObj.hashMap;
        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận bình chọn"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã nhóm", hm.get(Const.AppClient.BillId).toString()));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount).toString());
            long smsAmt = (amt / 5000);
            arrayList.add(Misc.buildTextValue("Số tin nhắn", String.valueOf(smsAmt)));
        }
        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmZingxu(Message message, RequestObj reqObj) {
        //todo

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Tên tài khoản", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }
        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmJetStar(Message message, RequestObj reqObj) {
        //todo
        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã vé", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));

    }

    public void translateSubmitFrmBac(Message message, RequestObj reqObj) {
        //todo

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Tên tài khoản", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));

    }

    public void translateSubmitFrmOnc(Message message, RequestObj reqObj) {
        //todo
        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Tên tài khoản", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmTrendmicro(Message message, RequestObj reqObj) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.Amount)) {
            String pCode = hm.get(Const.AppClient.Amount);
            String pName = "";
            switch (pCode) {
                case "260000":
                    pName = "Internet Security 2015 (1Năm/1PC)";
                    break;
                case "360000":
                    pName = "Maximum Security 2015 (1Năm/1PC)";
                    break;
                case "200000":
                    pName = "Mobile Security 2015 (1Năm)";
                    break;
                default:
                    break;
            }

            arrayList.add(Misc.buildTextValue("Tên sản phẩm", pName));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmMcc(Message message, RequestObj reqObj) {
        //todo

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.Amount)) {
            long uPrice = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Mệnh giá thẻ", Misc.formatAmount(uPrice).replaceAll(",", ".") + ""));
            arrayList.add(Misc.buildTextValue("Số lượng thẻ", hm.get(Const.AppClient.Quantity)));
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount)) * DataUtil.stringToUNumber(hm.get(Const.AppClient.Quantity));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmEpay(Message message, RequestObj reqObj) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.Amount)) {
            long uPrice = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Mệnh giá thẻ", Misc.formatAmount(uPrice).replaceAll(",", ".") + ""));
            arrayList.add(Misc.buildTextValue("Số lượng thẻ", hm.get(Const.AppClient.Quantity)));
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount)) * DataUtil.stringToUNumber(hm.get(Const.AppClient.Quantity));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmLacviet(Message message, RequestObj reqObj) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            String pCode = hm.get(Const.AppClient.BillId);
            String pName = "";
            switch (pCode) {
                case "MTDEVAW8_PC":
                    pName = "Từ điển Anh-Việt – 1năm";
                    break;
                case "MTDFVPW8_PC":
                    pName = "Từ điển Pháp-Việt – 1năm";
                    break;
                case "MTDCVH10_PC":
                    pName = "Từ điển Trung-Việt – 1năm";
                    break;
                case "MTDJVNW8_PC":
                    pName = "Từ điển Nhật-Việt – Vĩnh viễn";
                    break;
                case "MTDKVHW8_PC":
                    pName = "Từ điển Hàn-Việt – 1năm";
                    break;
                case "MTDEVKANDROID":
                    pName = "Từ điển Anh-Hàn-Việt for Android – 1năm";
                    break;
                case "MTDEVCANDROID":
                    pName = "Từ điển Anh-Trung-Việt for Android – 1năm";
                    break;
                case "MTDEVFANDROID":
                    pName = "Từ điển Anh-Pháp-Việt for Android – 1năm";
                    break;
                case "ENGLISHPRACTICE":
                    pName = "Trắc nghiệm Tiếng Anh for Android";
                    break;
                case "GRAMMAR":
                    pName = "Ngữ pháp Tiếng Anh for Android";
                    break;
                default:
                    break;
            }

            arrayList.add(Misc.buildTextValue("Tên sản phẩm", pName));
        }

        long amt = hm.containsKey(Const.AppClient.Amount) ? DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount)) : 0;
        arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));

        message.reply(Misc.getJsonArray(arrayList));
    }


    public void translateSubmitFrmAvg(Message message, RequestObj reqObj, HashMap<String, BillInfoService> hasMap) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã khách hàng", hm.get(Const.AppClient.BillId)));
        }

        BillInfoService bis = hasMap.get(reqObj.serviceid + reqObj.phoneNumber);
        if (bis != null) {
            HashMap<String, BillInfoService.TextValue> hash = Misc.convertArrayTextValue(bis.customer_info);

            String ht = hash.containsKey(Const.AppClient.FullName) ? hash.get(Const.AppClient.FullName).value : "";
            String dc = hash.containsKey(Const.AppClient.Address) ? hash.get(Const.AppClient.Address).value : "";
            String sdt = hash.containsKey(Const.AppClient.Phone) ? hash.get(Const.AppClient.Phone).value : "";
            String sn = hash.containsKey(Const.AppClient.ServiceName) ? hash.get(Const.AppClient.ServiceName).value : "";
            if (!"".equalsIgnoreCase(ht)) {
                arrayList.add(Misc.buildTextValue("Họ tên", ht));
            }
            if (!"".equalsIgnoreCase(dc)) {
                arrayList.add(Misc.buildTextValue("Địa chỉ", dc));
            }
            if (!"".equalsIgnoreCase(sdt)) {
                arrayList.add(Misc.buildTextValue("Số điện thoại", sdt));
            }

            if (!"".equalsIgnoreCase(sn)) {
                arrayList.add(Misc.buildTextValue("Gói dịch vụ", sn));
            }
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmVivoo(Message message, RequestObj reqObj) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã khách hàng", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmMytv(Message message, RequestObj reqObj) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã khách hàng", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmFshare(Message message, RequestObj reqObj) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Tài khoản", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            String pCode = hm.get(Const.AppClient.Amount);
            String pName = "";
            switch (pCode) {
                case "60000":
                    pName = "Gói VIP 30 ngày";
                    break;
                case "160000":
                    pName = "Gói VIP 90 ngày";
                    break;
                case "300000":
                    pName = "Gói VIP 180 ngày";
                    break;
                case "500000":
                    pName = "Gói VIP 365 ngày";
                    break;
                default:
                    break;
            }

            arrayList.add(Misc.buildTextValue("Tên sản phẩm", pName));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }
	
	public void translateSubmitFrmMissNgoiSao(Message message, RequestObj reqObj) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));
		
		if (hm.containsKey(Const.AppClient.FullName)) {
            arrayList.add(Misc.buildTextValue("Tên thí sinh", hm.get(Const.AppClient.FullName)));
        }
        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã thí sinh", hm.get(Const.AppClient.BillId)));
        }
        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
			if(amt  == 2000){
				arrayList.add(Misc.buildTextValue("Số tin nhắn", "1"));
			}else if(amt == 15000){
				arrayList.add(Misc.buildTextValue("Số tin nhắn", "10"));
			}else if(amt == 25000){
				arrayList.add(Misc.buildTextValue("Số tin nhắn", "20"));
			}else if(amt == 60000){
				arrayList.add(Misc.buildTextValue("Số tin nhắn", "50"));
			}else{
				arrayList.add(Misc.buildTextValue("Số tin nhắn", "-"));
			}
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmPrudential(Message message, RequestObj reqObj, HashMap<String, BillInfoService> hasMap) {
        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Số", hm.get(Const.AppClient.BillId)));
        }

        BillInfoService bis = hasMap.get(reqObj.serviceid + reqObj.phoneNumber);
        if (bis != null) {
            HashMap<String, BillInfoService.TextValue> hash = Misc.convertArrayTextValue(bis.customer_info);
            String fullName = hash.containsKey(Const.AppClient.FullName) ? hash.get(Const.AppClient.FullName).value : "";
            if (!"".equalsIgnoreCase(fullName)) {
                arrayList.add(Misc.buildTextValue("Chủ hợp đồng", fullName));
            }
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmThienHoa(Message message, RequestObj reqObj, HashMap<String, BillInfoService> hasMap) {
        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã đơn hàng", hm.get(Const.AppClient.BillId)));
        }
        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmRailway(Message message, RequestObj reqObj, HashMap<String, BillInfoService> hasMap) {
        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã thanh toán", hm.get(Const.AppClient.BillId)));
        }

        BillInfoService bis = hasMap.get(reqObj.serviceid + reqObj.phoneNumber);
        if (bis != null) {
            HashMap<String, BillInfoService.TextValue> hash = Misc.convertArrayTextValue(bis.customer_info);
            String bookCode = hash.containsKey(Const.AppClient.Address) ? hash.get(Const.AppClient.Address).value : "";
            String fullName = hash.containsKey(Const.AppClient.FullName) ? hash.get(Const.AppClient.FullName).value : "";
            if (!"".equalsIgnoreCase(bookCode)) {
                arrayList.add(Misc.buildTextValue("Mã đặt chỗ", bookCode));
            }
            if (!"".equalsIgnoreCase(fullName)) {
                arrayList.add(Misc.buildTextValue("Họ và tên", fullName));
            }
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmVtvcab(Message message, RequestObj reqObj) {
        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Mã thanh toán", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmGiatotviet(Message message, RequestObj reqObj) {
        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Số điện thoại liên hệ của bạn", hm.get(Const.AppClient.Phone)));
            arrayList.add(Misc.buildTextValue("Họ tên", hm.get(Const.AppClient.FullName)));
            arrayList.add(Misc.buildTextValue("Tên sản phẩm ", "GTVCard"));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long uPrice = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Mệnh giá", Misc.formatAmount(uPrice).replaceAll(",", ".") + "đ"));
            arrayList.add(Misc.buildTextValue("Số lượng", hm.get(Const.AppClient.Quantity)));
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount)) * DataUtil.stringToUNumber(hm.get(Const.AppClient.Quantity));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmQpal(Message message, RequestObj reqObj) {
        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.Amount)) {
            long uPrice = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Mệnh giá thẻ", Misc.formatAmount(uPrice).replaceAll(",", ".") + ""));
            arrayList.add(Misc.buildTextValue("Số lượng thẻ", hm.get(Const.AppClient.Quantity)));
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount)) * DataUtil.stringToUNumber(hm.get(Const.AppClient.Quantity));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmBkav(Message message, RequestObj reqObj) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            String pCode = hm.get(Const.AppClient.BillId);
            String pName = "";
            switch (pCode) {
                case "2":
                    pName = "Bkav Mobile Security PRO 1 năm";
                    break;
                case "21":
                    pName = "Bkav PRO 1 năm";
                    break;
                default:
                    break;
            }

            arrayList.add(Misc.buildTextValue("Tên sản phẩm", pName));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long uPrice = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Mệnh giá thẻ", Misc.formatAmount(uPrice).replaceAll(",", ".") + ""));
            arrayList.add(Misc.buildTextValue("Số lượng thẻ", hm.get(Const.AppClient.Quantity)));
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount)) * DataUtil.stringToUNumber(hm.get(Const.AppClient.Quantity));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmPingsky(Message message, RequestObj reqObj, HashMap<String, BillInfoService> hasMap) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Tài khoản", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmOnGame(Message message, RequestObj reqObj, HashMap<String, BillInfoService> hasMap) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Tài khoản", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmHomeCredit(Message message, RequestObj reqObj, HashMap<String, BillInfoService> hasMap) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Số hợp đồng", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }
        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmMSD(Message message, RequestObj reqObj, HashMap<String, BillInfoService> hasMap) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận thanh toán"));
        arrayList.add(Misc.getButtonConfirm("Đồng ý"));

        if (hm.containsKey(Const.AppClient.BillId)) {
            arrayList.add(Misc.buildTextValue("Số hợp đồng", hm.get(Const.AppClient.BillId)));
        }

        if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue("Số tiền", Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmAllWithMap(Map<String, TranslateObject> translateMaps, Message message, RequestObj reqObj, Map map) {

        logger.info("translateSubmitFrmAllWithMap");
        HashMap<String, String> hm = reqObj.hashMap;
        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
        Set<Map.Entry<String, String>> entrySet = hm.entrySet();

        logger.info("size key set" + map.keySet().size());
        logger.info("map.keySet()" + map.keySet());
        for (Object key : map.keySet()) {
            logger.info(key.toString() + map.get(key).toString());
            arrayList.add(Misc.buildTextValue(key.toString(), map.get(key).toString()));
        }

        if (translateMaps != null && translateMaps.values() != null) {
            Iterator<TranslateObject> translateObjects = translateMaps.values().iterator();
            TranslateObject translateObject = null;
            while (translateObjects.hasNext()) {
                translateObject = translateObjects.next();
                if (!"".equalsIgnoreCase(translateObject.getContent())) {
                    arrayList.add(Misc.buildTextValue(translateObject.getLabel(), translateObject.getContent()));
                }
            }
            for (Map.Entry<String, String> entry : entrySet) {
                if (translateMaps.containsKey(entry.getKey())) {
                    TranslateObject transObj = translateMaps.get(entry.getKey());
                    arrayList.add(Misc.buildTextValue(transObj.getLabel(), transObj.getValueLabel(entry.getValue())));
                }
            }
        }

        arrayList.add(Misc.getCaptionConfirm(CAPTION_CONFIRM));
        arrayList.add(Misc.getButtonConfirm(BUTTON_CONFIRM));

        if (hm.containsKey(Const.AppClient.Quantity) && hm.containsKey(Const.AppClient.Amount)) {
            long uPrice = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue(PRICE_CONFIRM, Misc.formatAmount(uPrice).replaceAll(",", ".") + ""));
            arrayList.add(Misc.buildTextValue(QUANTITY_CONFIRM, hm.get(Const.AppClient.Quantity)));
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount)) * DataUtil.stringToUNumber(hm.get(Const.AppClient.Quantity));
            arrayList.add(Misc.buildTextValue(AMOUNT_CONFIRM, Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        } else if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue(AMOUNT_CONFIRM, Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }
        logger.info("mess reply " + Misc.getJsonArray(arrayList).toString());
        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmAll(Map<String, TranslateObject> translateMaps, Message message, RequestObj reqObj) {
        logger.info("translateSubmitFrmAll");
        HashMap<String, String> hm = reqObj.hashMap;
        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
        Set<Map.Entry<String, String>> entrySet = hm.entrySet();

        if (translateMaps != null && translateMaps.values() != null) {
            Iterator<TranslateObject> translateObjects = translateMaps.values().iterator();
            TranslateObject translateObject = null;
            while (translateObjects.hasNext()) {
                translateObject = translateObjects.next();
                if (!"".equalsIgnoreCase(translateObject.getContent())) {
                    arrayList.add(Misc.buildTextValue(translateObject.getLabel(), translateObject.getContent()));
                }
            }
            for (Map.Entry<String, String> entry : entrySet) {
                if (translateMaps.containsKey(entry.getKey())) {
                    TranslateObject transObj = translateMaps.get(entry.getKey());
                    arrayList.add(Misc.buildTextValue(transObj.getLabel(), transObj.getValueLabel(entry.getValue())));
                }
            }
        }

        arrayList.add(Misc.getCaptionConfirm(CAPTION_CONFIRM));
        arrayList.add(Misc.getButtonConfirm(BUTTON_CONFIRM));

//        if (jTranlate != null) {
//            Set<String> rows = jTranlate.getFieldNames();
//            for (String row : rows) {
//                JsonObject joRow = jTranlate.getObject(row);
//                if (joRow.containsField(VALUE_KEY)) {
//                    JsonObject joValue = joRow.getObject(VALUE_KEY);
//                    String data = hm.containsKey(row) ? hm.get(row) : "";
//                    arrayList.add(Misc.buildTextValue(joRow.getString(KEY_KEY), joValue.getString(data)));
//                } else {
//                    String data = hm.containsKey(row) ? hm.get(row) : "";
//                    arrayList.add(Misc.buildTextValue(joRow.getString(KEY_KEY), data));
//                }
//            }
//        }
        if (hm.containsKey(Const.AppClient.Quantity) && hm.containsKey(Const.AppClient.Amount)) {
            long uPrice = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue(PRICE_CONFIRM, Misc.formatAmount(uPrice).replaceAll(",", ".") + ""));
            arrayList.add(Misc.buildTextValue(QUANTITY_CONFIRM, hm.get(Const.AppClient.Quantity)));
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount)) * DataUtil.stringToUNumber(hm.get(Const.AppClient.Quantity));
            arrayList.add(Misc.buildTextValue(AMOUNT_CONFIRM, Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        } else if (hm.containsKey(Const.AppClient.Amount)) {
            long amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue(AMOUNT_CONFIRM, Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }
        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateSubmitFrmSurvey(Message message, RequestObj reqObj) {

        HashMap<String, String> hm = reqObj.hashMap;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm("Xác nhận"));
        arrayList.add(Misc.getButtonConfirm(BUTTON_CONFIRM));
        if (reqObj.phoneNumber != 0) {
            arrayList.add(Misc.buildTextValue("Số điện thoại", "0" + String.valueOf(reqObj.phoneNumber)));
        }

        message.reply(Misc.getJsonArray(arrayList));
    }

    public void translateDefault(Message message, RequestObj reqObj) {

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();
        message.reply(Misc.getJsonArray(arrayList));
    }

    public void getAutoTranslateForm(Message message, RequestObj requestObj, ArrayList<FormObj> form)
    {
            //formId, caption, fieldItems, fieldlabel, button
        HashMap<String, String> hm = requestObj.hashMap;
        FormObj jsonTranslateForm = new FormObj();
        String formId = "";
        for(FormObj o : form)
        {

            formId = o.formId;
            if("translateForm".equalsIgnoreCase(formId))
            {
                jsonTranslateForm = o;
                break;
            }
        }
        String caption = jsonTranslateForm.caption;
        String button = jsonTranslateForm.button;

        ArrayList<BillInfoService.TextValue> arrayList = new ArrayList<>();

        arrayList.add(Misc.getCaptionConfirm(caption));
        arrayList.add(Misc.getButtonConfirm(button));

        ArrayList<FieldItem> fieldItems = jsonTranslateForm.fieldItems;
        String fieldLabel = "";
        String key = "";
        long uPrice = 0;
        long amt = 0;
        for(FieldItem fieldItem : fieldItems)
        {
            fieldLabel = fieldItem.fieldlabel;
            key = fieldItem.key;
            if((key).contains(Const.AppClient.Amount))
            {
                amt = DataUtil.stringToUNumber(hm.get(key));
                arrayList.add(Misc.buildTextValue(fieldLabel, Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
            }
            else
            {
                arrayList.add(Misc.buildTextValue(fieldLabel, hm.get(key)));
            }
        }

        if (hm.containsKey(Const.AppClient.Quantity) && hm.containsKey(Const.AppClient.Amount)) {
            amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount)) * DataUtil.stringToUNumber(hm.get(Const.AppClient.Quantity));
            arrayList.add(Misc.buildTextValue(AMOUNT_CONFIRM, Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        } else if (hm.containsKey(Const.AppClient.Amount)) {
            amt = DataUtil.stringToUNumber(hm.get(Const.AppClient.Amount));
            arrayList.add(Misc.buildTextValue(AMOUNT_CONFIRM, Misc.formatAmount(amt).replaceAll(",", ".") + "đ"));
        }
        message.reply(Misc.getJsonArray(arrayList));
    }
}
