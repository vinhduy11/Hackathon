package com.mservice.momo.clientform;

import com.mservice.momo.data.BillInfoService;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.form.FieldData;
import com.mservice.momo.vertx.form.FormObj;
import com.mservice.momo.vertx.form.RequestObj;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by concu on 11/11/14.
 */
public class FormInfo {

    private Vertx vertx;
    private Logger logger;

    public FormInfo(Vertx vertx, Logger logger){
        this.vertx = vertx;
        this.logger = logger;
    }

    //refine data for each service with request inform from partner
    public void refineFormInfo(final FormObj formObj
            ,final RequestObj reqObj
            ,final HashMap<String,BillInfoService> hashMapBIS
            ,final Common.BuildLog log){

        BillInfoService bis = hashMapBIS.get(reqObj.serviceid + reqObj.phoneNumber);

        if(bis == null){
            log.add("BillInfoService", "null");
            log.writeLog();
            return;
        }

        switch (reqObj.serviceid){
            case "avg":

                logger.info("avg_json " + hashMapBIS.get(reqObj.serviceid + reqObj.phoneNumber).toJsonObject());
                HashMap<String,BillInfoService.TextValue> hashMapCusInfo = Misc.convertArrayTextValue(bis.customer_info);
                String ht = hashMapCusInfo.containsKey(Const.AppClient.FullName) ? hashMapCusInfo.get(Const.AppClient.FullName).value : "";
                String dc = hashMapCusInfo.containsKey(Const.AppClient.Address) ? hashMapCusInfo.get(Const.AppClient.Address).value : "";
                String sdt =hashMapCusInfo.containsKey(Const.AppClient.Phone) ? hashMapCusInfo.get(Const.AppClient.Phone).value : "";
                String sn =hashMapCusInfo.containsKey(Const.AppClient.ServiceName) ? hashMapCusInfo.get(Const.AppClient.ServiceName).value : "";
                Misc.refineLabelForFieldItem(Const.AppClient.FullName, ht, formObj.fieldItems);
                Misc.refineLabelForFieldItem(Const.AppClient.Address, dc, formObj.fieldItems);
                Misc.refineLabelForFieldItem(Const.AppClient.Phone, sdt, formObj.fieldItems);
                Misc.refineLabelForFieldItem(Const.AppClient.ServiceName, sn, formObj.fieldItems);
                Misc.addDataForFieldItem(Const.AppClient.Amount,bis.array_price, formObj.fieldDatas);
                break;
				
			case "vivoo": case "mytv": case "ongame": case "ongate": case "proxy_confirm": case "taske":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfor = Misc.convertArrayTextValue(bis.customer_info);              
                String html =hashMapCusInfor.containsKey(Const.AppClient.Html) ? hashMapCusInfor.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, html, formObj.fieldItems);               
                Misc.addDataForFieldItem(Const.AppClient.Amount,bis.array_price, formObj.fieldDatas);
                break;
													
				
            case "prudential":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoPrudential = Misc.convertArrayTextValue(bis.customer_info);
				String accFru =hashMapCusInfoPrudential.containsKey("item") ? hashMapCusInfoPrudential.get("item").value : "";
                String nameFru = hashMapCusInfoPrudential.containsKey(Const.AppClient.FullName) ? hashMapCusInfoPrudential.get(Const.AppClient.FullName).value : "";             
                Misc.refineLabelForFieldItem("item", accFru, formObj.fieldItems);
				Misc.refineLabelForFieldItem(Const.AppClient.FullName, nameFru, formObj.fieldItems);                
                Misc.addDataForFieldItem(Const.AppClient.Amount,bis.array_price, formObj.fieldDatas);
                break;
                    
            case "railway":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoRailway = Misc.convertArrayTextValue(bis.customer_info);                                                      
                String moneyRailway =hashMapCusInfoRailway.containsKey("money") ? hashMapCusInfoRailway.get("money").value : "";                    
                String htmlRailway =hashMapCusInfoRailway.containsKey(Const.AppClient.Html) ? hashMapCusInfoRailway.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlRailway, formObj.fieldItems);//Không có dấu chấm ở số tiền trả về
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyRailway)).replaceAll(",", ""), formObj.fieldItems);                                                                  
                break;
                
            case "vtvcab": case "pingsky":  case "homecredit": case "msd": case "scj": case "vexere": case "finstar":case "scb": case "napvxuclipvn":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoVtvcab = Misc.convertArrayTextValue(bis.customer_info);                                                      
                String moneyVtvcab = hashMapCusInfoVtvcab.containsKey("money") ? hashMapCusInfoVtvcab.get("money").value : "";                    
                String htmlInforVtvcab = hashMapCusInfoVtvcab.containsKey(Const.AppClient.Html) ? hashMapCusInfoVtvcab.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforVtvcab, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyVtvcab)).replaceAll(",", "."), formObj.fieldItems);                                                                  
                break;
                
            case "thienhoa":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoThienHoa = Misc.convertArrayTextValue(bis.customer_info);
                String moneyThienHoa = hashMapCusInfoThienHoa.containsKey("money") ? hashMapCusInfoThienHoa.get("money").value : "";  
				String accThienHoa =hashMapCusInfoThienHoa.containsKey("item") ? hashMapCusInfoThienHoa.get("item").value : "";
                String htmlThienHoa = hashMapCusInfoThienHoa.containsKey(Const.AppClient.Html) ? hashMapCusInfoThienHoa.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlThienHoa, formObj.fieldItems);             
                Misc.refineLabelForFieldItem("item", accThienHoa, formObj.fieldItems);               
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyThienHoa)).replaceAll(",", "."), formObj.fieldItems);                              
                break;
                
            case "vtv_extension":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoVtvex = Misc.convertArrayTextValue(bis.customer_info);                                                      
                String moneyVtvex = hashMapCusInfoVtvex.containsKey("money") ? hashMapCusInfoVtvex.get("money").value : "";                    
                String htmlInforVtvex = hashMapCusInfoVtvex.containsKey(Const.AppClient.Html) ? hashMapCusInfoVtvex.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforVtvex, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyVtvex)).replaceAll(",", "."), formObj.fieldItems);                
                if(bis.extra_info != null && !bis.extra_info.isEmpty()){
                        ArrayList <FieldData> listData = new ArrayList<>();
                        for (BillInfoService.ExtraInfo ex : bis.extra_info) {
                        String[] bills = ex.bill_detail_id.split("#");
                        String code = ""; 
                        String value = ""; 
                        String text = "";
                        if(bills.length >=3){
                           code = bills[0];
                           value = bills[1]; 
                           text = bills[2];  
                        }
                        if(!code.equals("") && code.equals("acc")){
                            JsonObject joAcc = new JsonObject();
                            joAcc.putString("id", "");
                            joAcc.putString("linkto", Const.AppClient.Account);
                            joAcc.putString("text", text);
                            joAcc.putString("value", value);
                            joAcc.putString("parentid", "");
                            FieldData fd = new FieldData(joAcc);
                            listData.add(fd); 
                        }
                        if(!code.equals("") && code.equals("ht")){
                           JsonObject joMonth = new JsonObject();
                            joMonth.putString("id", "");
                            joMonth.putString("linkto", Const.AppClient.FullName);
                            joMonth.putString("text", text);
                            joMonth.putString("value", value); 
                            joMonth.putString("parentid", "");
                            FieldData fd1 = new FieldData(joMonth);
                            listData.add(fd1); 
                        }                   
                        }																			
                    formObj.fieldDatas.addAll(listData);  
                    }                                
                break;
            
			case "anvien":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoAnvien = Misc.convertArrayTextValue(bis.customer_info);                                                      
                String moneyAnvien = hashMapCusInfoAnvien.containsKey("money") ? hashMapCusInfoAnvien.get("money").value : "";                    
                String htmlInforAnvien = hashMapCusInfoAnvien.containsKey(Const.AppClient.Html) ? hashMapCusInfoAnvien.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforAnvien, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyAnvien)).replaceAll(",", "."), formObj.fieldItems);                
                if(bis.extra_info != null && !bis.extra_info.isEmpty()){
                        ArrayList <FieldData> listData = new ArrayList<>();
                        for (BillInfoService.ExtraInfo ex : bis.extra_info) {
                        String[] bills = ex.bill_detail_id.split("#");
                        String code = ""; 
                        String value = ""; 
                        String text = "";
                        if(bills.length >=3){
                           code = bills[0]; 
                           value = bills[1]; 
                           text = bills[2];  
                        }
                        if(!code.equals("") && code.equals("amt")){
                            JsonObject joAcc = new JsonObject();
                            joAcc.putString("id", "");
                            joAcc.putString("linkto", Const.AppClient.Amount);
                            joAcc.putString("text", text);
                            joAcc.putString("value", value);
                            joAcc.putString("parentid", "");
                            FieldData fd = new FieldData(joAcc);
                            listData.add(fd); 
                        }              
                        }																			
                    formObj.fieldDatas.addAll(listData);  
                    }                                
                break;
				
			case "fshare":
            case "giahanclipvn":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoFshare = Misc.convertArrayTextValue(bis.customer_info);                                                      
                String moneyFshare = hashMapCusInfoFshare.containsKey("money") ? hashMapCusInfoFshare.get("money").value : "";                    
                String htmlInforFshare = hashMapCusInfoFshare.containsKey(Const.AppClient.Html) ? hashMapCusInfoFshare.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforFshare, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyFshare)).replaceAll(",", "."), formObj.fieldItems);                
                if(bis.extra_info != null && !bis.extra_info.isEmpty()){
                        ArrayList <FieldData> listData = new ArrayList<>();
                        for (BillInfoService.ExtraInfo ex : bis.extra_info) {
							String[] bills = ex.bill_detail_id.split("#");
							String code = ""; 
							String value = ""; 
							String text = "";
							if(bills.length >=3){
							   code = bills[0];
							   value = bills[1]; 
							   text = bills[2];  
							}
							if(!code.equals("") && code.equals("amt")){
								JsonObject joAcc = new JsonObject();
								joAcc.putString("id", "");
								joAcc.putString("linkto", Const.AppClient.Amount);
								joAcc.putString("text", text);
								joAcc.putString("value", value);
								joAcc.putString("parentid", "");
								FieldData fd = new FieldData(joAcc);
								listData.add(fd); 
							}              
                        }																			
                    formObj.fieldDatas.addAll(listData);  
                    }                                
                break;
				
			case "missngoisao":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoMiss = Misc.convertArrayTextValue(bis.customer_info);                                                      
                String moneyMiss = hashMapCusInfoMiss.containsKey("money") ? hashMapCusInfoMiss.get("money").value : "";                    
                String htmlInforMiss = hashMapCusInfoMiss.containsKey(Const.AppClient.Html) ? hashMapCusInfoMiss.get(Const.AppClient.Html).value : "";
                String htMiss = hashMapCusInfoMiss.containsKey(Const.AppClient.FullName) ? hashMapCusInfoMiss.get(Const.AppClient.FullName).value : "";
				String sdtMiss =hashMapCusInfoMiss.containsKey(Const.AppClient.Phone) ? hashMapCusInfoMiss.get(Const.AppClient.Phone).value : "";
				Misc.addValueForFieldItem(Const.AppClient.Phone, sdtMiss, formObj.fieldItems);
				Misc.addValueForFieldItem(Const.AppClient.FullName, htMiss, formObj.fieldItems);
				Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforMiss, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyMiss)).replaceAll(",", "."), formObj.fieldItems);                                                                  
                break;		
            default:
                break;

        }



    }
    //support.end

    public void refineNewFormInfo(final FormObj formObj
            ,final RequestObj reqObj
            ,final HashMap<String,BillInfoService> hashMapBIS
            ,final Common.BuildLog log
            ,final JsonObject joConnectorProxy){

        BillInfoService bis = hashMapBIS.get(reqObj.serviceid + reqObj.phoneNumber);

        if(bis == null){
            log.add("BillInfoService", "null");
            log.writeLog();
            return;
        }
        log.add("joConnectorProxy", joConnectorProxy.toString());
        switch (reqObj.serviceid){
            case "avg":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfo = Misc.convertArrayTextValue(bis.customer_info);
                String ht = hashMapCusInfo.containsKey(Const.AppClient.FullName) ? hashMapCusInfo.get(Const.AppClient.FullName).value : "";
                String dc = hashMapCusInfo.containsKey(Const.AppClient.Address) ? hashMapCusInfo.get(Const.AppClient.Address).value : "";
                String sdt =hashMapCusInfo.containsKey(Const.AppClient.Phone) ? hashMapCusInfo.get(Const.AppClient.Phone).value : "";
                String sn =hashMapCusInfo.containsKey(Const.AppClient.ServiceName) ? hashMapCusInfo.get(Const.AppClient.ServiceName).value : "";
                Misc.refineLabelForFieldItem(Const.AppClient.FullName, ht, formObj.fieldItems);
                Misc.refineLabelForFieldItem(Const.AppClient.Address, dc, formObj.fieldItems);
                Misc.refineLabelForFieldItem(Const.AppClient.Phone, sdt, formObj.fieldItems);
                Misc.refineLabelForFieldItem(Const.AppClient.ServiceName, sn, formObj.fieldItems);
                Misc.addDataForFieldItem(Const.AppClient.Amount,bis.array_price, formObj.fieldDatas);
                break;

            case "vivoo": case "mytv": case "ongame": case "ongate": case "proxy_confirm": case "taske":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfor = Misc.convertArrayTextValue(bis.customer_info);
                String html =hashMapCusInfor.containsKey(Const.AppClient.Html) ? hashMapCusInfor.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, html, formObj.fieldItems);
                Misc.addDataForFieldItem(Const.AppClient.Amount,bis.array_price, formObj.fieldDatas);
                break;


            case "prudential":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoPrudential = Misc.convertArrayTextValue(bis.customer_info);
                String accFru =hashMapCusInfoPrudential.containsKey("item") ? hashMapCusInfoPrudential.get("item").value : "";
                String nameFru = hashMapCusInfoPrudential.containsKey(Const.AppClient.FullName) ? hashMapCusInfoPrudential.get(Const.AppClient.FullName).value : "";
                Misc.refineLabelForFieldItem("item", accFru, formObj.fieldItems);
                Misc.refineLabelForFieldItem(Const.AppClient.FullName, nameFru, formObj.fieldItems);
                Misc.addDataForFieldItem(Const.AppClient.Amount,bis.array_price, formObj.fieldDatas);
                break;

            case "railway":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoRailway = Misc.convertArrayTextValue(bis.customer_info);
                String moneyRailway =hashMapCusInfoRailway.containsKey("money") ? hashMapCusInfoRailway.get("money").value : "";
                String htmlRailway =hashMapCusInfoRailway.containsKey(Const.AppClient.Html) ? hashMapCusInfoRailway.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlRailway, formObj.fieldItems);//Không có dấu chấm ở số tiền trả về
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyRailway)).replaceAll(",", ""), formObj.fieldItems);
                break;

            case "vtvcab": case "pingsky":  case "homecredit": case "msd": case "scj": case "vexere": case "finstar":case "scb": case "napvxuclipvn":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoVtvcab = Misc.convertArrayTextValue(bis.customer_info);
                String moneyVtvcab = hashMapCusInfoVtvcab.containsKey("money") ? hashMapCusInfoVtvcab.get("money").value : "";
                String htmlInforVtvcab = hashMapCusInfoVtvcab.containsKey(Const.AppClient.Html) ? hashMapCusInfoVtvcab.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforVtvcab, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyVtvcab)).replaceAll(",", "."), formObj.fieldItems);
                break;

            case "thienhoa":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoThienHoa = Misc.convertArrayTextValue(bis.customer_info);
                String moneyThienHoa = hashMapCusInfoThienHoa.containsKey("money") ? hashMapCusInfoThienHoa.get("money").value : "";
                String accThienHoa =hashMapCusInfoThienHoa.containsKey("item") ? hashMapCusInfoThienHoa.get("item").value : "";
                String htmlThienHoa = hashMapCusInfoThienHoa.containsKey(Const.AppClient.Html) ? hashMapCusInfoThienHoa.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlThienHoa, formObj.fieldItems);
                Misc.refineLabelForFieldItem("item", accThienHoa, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyThienHoa)).replaceAll(",", "."), formObj.fieldItems);
                break;

            case "vtv_extension":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoVtvex = Misc.convertArrayTextValue(bis.customer_info);
                String moneyVtvex = hashMapCusInfoVtvex.containsKey("money") ? hashMapCusInfoVtvex.get("money").value : "";
                String htmlInforVtvex = hashMapCusInfoVtvex.containsKey(Const.AppClient.Html) ? hashMapCusInfoVtvex.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforVtvex, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyVtvex)).replaceAll(",", "."), formObj.fieldItems);
                if(bis.extra_info != null && !bis.extra_info.isEmpty()){
                    ArrayList <FieldData> listData = new ArrayList<>();
                    for (BillInfoService.ExtraInfo ex : bis.extra_info) {
                        String[] bills = ex.bill_detail_id.split("#");
                        String code = "";
                        String value = "";
                        String text = "";
                        if(bills.length >=3){
                            code = bills[0];
                            value = bills[1];
                            text = bills[2];
                        }
                        if(!code.equals("") && code.equals("acc")){
                            JsonObject joAcc = new JsonObject();
                            joAcc.putString("id", "");
                            joAcc.putString("linkto", Const.AppClient.Account);
                            joAcc.putString("text", text);
                            joAcc.putString("value", value);
                            joAcc.putString("parentid", "");
                            FieldData fd = new FieldData(joAcc);
                            listData.add(fd);
                        }
                        if(!code.equals("") && code.equals("ht")){
                            JsonObject joMonth = new JsonObject();
                            joMonth.putString("id", "");
                            joMonth.putString("linkto", Const.AppClient.FullName);
                            joMonth.putString("text", text);
                            joMonth.putString("value", value);
                            joMonth.putString("parentid", "");
                            FieldData fd1 = new FieldData(joMonth);
                            listData.add(fd1);
                        }
                    }
                    formObj.fieldDatas.addAll(listData);
                }
                break;

            case "anvien":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoAnvien = Misc.convertArrayTextValue(bis.customer_info);
                String moneyAnvien = hashMapCusInfoAnvien.containsKey("money") ? hashMapCusInfoAnvien.get("money").value : "";
                String htmlInforAnvien = hashMapCusInfoAnvien.containsKey(Const.AppClient.Html) ? hashMapCusInfoAnvien.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforAnvien, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyAnvien)).replaceAll(",", "."), formObj.fieldItems);
                if(bis.extra_info != null && !bis.extra_info.isEmpty()){
                    ArrayList <FieldData> listData = new ArrayList<>();
                    for (BillInfoService.ExtraInfo ex : bis.extra_info) {
                        String[] bills = ex.bill_detail_id.split("#");
                        String code = "";
                        String value = "";
                        String text = "";
                        if(bills.length >=3){
                            code = bills[0];
                            value = bills[1];
                            text = bills[2];
                        }
                        if(!code.equals("") && code.equals("amt")){
                            JsonObject joAcc = new JsonObject();
                            joAcc.putString("id", "");
                            joAcc.putString("linkto", Const.AppClient.Amount);
                            joAcc.putString("text", text);
                            joAcc.putString("value", value);
                            joAcc.putString("parentid", "");
                            FieldData fd = new FieldData(joAcc);
                            listData.add(fd);
                        }
                    }
                    formObj.fieldDatas.addAll(listData);
                }
                break;

            case "fshare":
            case "vega":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoFshare = Misc.convertArrayTextValue(bis.customer_info);
                String moneyFshare = hashMapCusInfoFshare.containsKey("money") ? hashMapCusInfoFshare.get("money").value : "";
                String htmlInforFshare = hashMapCusInfoFshare.containsKey(Const.AppClient.Html) ? hashMapCusInfoFshare.get(Const.AppClient.Html).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforFshare, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyFshare)).replaceAll(",", "."), formObj.fieldItems);
                if(bis.extra_info != null && !bis.extra_info.isEmpty()){
                    ArrayList <FieldData> listData = new ArrayList<>();
                    for (BillInfoService.ExtraInfo ex : bis.extra_info) {
                        String[] bills = ex.bill_detail_id.split("#");
                        String code = "";
                        String value = "";
                        String text = "";
                        if(bills.length >=3){
                            code = bills[0];
                            value = bills[1];
                            text = bills[2];
                        }
                        if(!code.equals("") && code.equals("amt")){
                            JsonObject joAcc = new JsonObject();
                            joAcc.putString("id", "");
                            joAcc.putString("linkto", Const.AppClient.Amount);
                            joAcc.putString("text", text);
                            joAcc.putString("value", value);
                            joAcc.putString("parentid", "");
                            FieldData fd = new FieldData(joAcc);
                            listData.add(fd);
                        }
                    }
                    formObj.fieldDatas.addAll(listData);
                }
                break;

            case "missngoisao":
                HashMap<String,BillInfoService.TextValue> hashMapCusInfoMiss = Misc.convertArrayTextValue(bis.customer_info);
                String moneyMiss = hashMapCusInfoMiss.containsKey("money") ? hashMapCusInfoMiss.get("money").value : "";
                String htmlInforMiss = hashMapCusInfoMiss.containsKey(Const.AppClient.Html) ? hashMapCusInfoMiss.get(Const.AppClient.Html).value : "";
                String htMiss = hashMapCusInfoMiss.containsKey(Const.AppClient.FullName) ? hashMapCusInfoMiss.get(Const.AppClient.FullName).value : "";
                String sdtMiss =hashMapCusInfoMiss.containsKey(Const.AppClient.Phone) ? hashMapCusInfoMiss.get(Const.AppClient.Phone).value : "";
                Misc.addValueForFieldItem(Const.AppClient.Phone, sdtMiss, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.FullName, htMiss, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforMiss, formObj.fieldItems);
                Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyMiss)).replaceAll(",", "."), formObj.fieldItems);
                break;
            default:
                refineAutoService(joConnectorProxy, bis, formObj);
                break;

        }



    }
    //support.end
    private void refineAutoService(JsonObject joConnectorProxy, BillInfoService bis, FormObj formObj)
    {
        String connectorType = joConnectorProxy.getString("type", "");
        if("getInfoFromCustomer".equalsIgnoreCase(connectorType))
        {
            //Neu lay thong tin tu khach hang.
            getInfoFromCustomer(bis, formObj);
        }
        else
        {
            getInfoFromLocal(bis, formObj);
        }
//        else if("getInfoFromLocal".equalsIgnoreCase(connectorType))
//        {
//            getInfoFromLocal(bis, formObj);
//        }
    }

    private void getInfoFromCustomer(BillInfoService bis, FormObj formObj)
    {
        HashMap<String,BillInfoService.TextValue> hashMapCusInfoVtvex = Misc.convertArrayTextValue(bis.customer_info);
        String moneyVtvex = hashMapCusInfoVtvex.containsKey("money") ? hashMapCusInfoVtvex.get("money").value : "";
        String htmlInforVtvex = hashMapCusInfoVtvex.containsKey(Const.AppClient.Html) ? hashMapCusInfoVtvex.get(Const.AppClient.Html).value : "";
        Misc.addValueForFieldItem(Const.AppClient.Html, htmlInforVtvex, formObj.fieldItems);
        Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(moneyVtvex)).replaceAll(",", "."), formObj.fieldItems);
        if(bis.extra_info != null && !bis.extra_info.isEmpty()){
            ArrayList <FieldData> listData = new ArrayList<>();
            for (BillInfoService.ExtraInfo ex : bis.extra_info) {
                String[] bills = ex.bill_detail_id.split("#");
                String code = "";
                String value = "";
                String text = "";
                if(bills.length >=3){
                    code = bills[0];
                    value = bills[1];
                    text = bills[2];
                }
                if(!code.equals("") && code.equals("acc")){
                    JsonObject joAcc = new JsonObject();
                    joAcc.putString("id", "");
                    joAcc.putString("linkto", Const.AppClient.Account);
                    joAcc.putString("text", text);
                    joAcc.putString("value", value);
                    joAcc.putString("parentid", "");
                    FieldData fd = new FieldData(joAcc);
                    listData.add(fd);
                }
                else if(!code.equals("") && code.equals("ht")){
                    JsonObject joMonth = new JsonObject();
                    joMonth.putString("id", "");
                    joMonth.putString("linkto", Const.AppClient.FullName);
                    joMonth.putString("text", text);
                    joMonth.putString("value", value);
                    joMonth.putString("parentid", "");
                    FieldData fd1 = new FieldData(joMonth);
                    listData.add(fd1);
                }
                else if(!code.equals("") && code.equals("amt")){
                    JsonObject joAcc = new JsonObject();
                    joAcc.putString("id", "");
                    joAcc.putString("linkto", Const.AppClient.Amount);
                    joAcc.putString("text", text);
                    joAcc.putString("value", value);
                    joAcc.putString("parentid", "");
                    FieldData fd = new FieldData(joAcc);
                    listData.add(fd);
                }
            }
            formObj.fieldDatas.addAll(listData);
        }
    }

    private void getInfoFromLocal(BillInfoService bis, FormObj formObj)
    {
        HashMap<String,BillInfoService.TextValue> hashMapCusInfo = Misc.convertArrayTextValue(bis.customer_info);
        String ht = hashMapCusInfo.containsKey(Const.AppClient.FullName) ? hashMapCusInfo.get(Const.AppClient.FullName).value : "";
        String dc = hashMapCusInfo.containsKey(Const.AppClient.Address) ? hashMapCusInfo.get(Const.AppClient.Address).value : "";
        String sdt =hashMapCusInfo.containsKey(Const.AppClient.Phone) ? hashMapCusInfo.get(Const.AppClient.Phone).value : "";
        String sn =hashMapCusInfo.containsKey(Const.AppClient.ServiceName) ? hashMapCusInfo.get(Const.AppClient.ServiceName).value : "";
        String html =hashMapCusInfo.containsKey(Const.AppClient.Html) ? hashMapCusInfo.get(Const.AppClient.Html).value : "";
        String money = hashMapCusInfo.containsKey("money") ? hashMapCusInfo.get("money").value : "";
        String acc =hashMapCusInfo.containsKey("item") ? hashMapCusInfo.get("item").value : "";
        if(!"".equalsIgnoreCase(ht))
        {
            Misc.refineLabelForFieldItem(Const.AppClient.FullName, ht, formObj.fieldItems);
        }

        if(!"".equalsIgnoreCase(dc))
        {
            Misc.refineLabelForFieldItem(Const.AppClient.Address, dc, formObj.fieldItems);
        }

        if(!"".equalsIgnoreCase(sdt))
        {
            Misc.refineLabelForFieldItem(Const.AppClient.Phone, sdt, formObj.fieldItems);
        }

        if(!"".equalsIgnoreCase(sn))
        {
            Misc.refineLabelForFieldItem(Const.AppClient.ServiceName, sn, formObj.fieldItems);
        }

        if(!"".equalsIgnoreCase(html))
        {
            Misc.addValueForFieldItem(Const.AppClient.Html, html, formObj.fieldItems);
        }

        if(!"".equalsIgnoreCase(acc))
        {
            Misc.refineLabelForFieldItem("item", acc, formObj.fieldItems);
        }

        if(!"".equalsIgnoreCase(money))
        {
            Misc.addValueForFieldItem(Const.AppClient.Amount, Misc.formatAmount(DataUtil.stringToUNumber(money)).replaceAll(",", "."), formObj.fieldItems);
        }

        Misc.addDataForFieldItem(Const.AppClient.Amount,bis.array_price, formObj.fieldDatas);
    }

}
