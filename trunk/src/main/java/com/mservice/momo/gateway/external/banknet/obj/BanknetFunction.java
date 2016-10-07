package com.mservice.momo.gateway.external.banknet.obj;

import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import com.mservice.momo.gateway.external.banknet.webservice.DirectPaymentPortType;
import com.mservice.momo.gateway.external.banknet.webservice.DirectPaymentPortTypeProxy;
import com.mservice.momo.data.SettingsDb;
import com.mservice.momo.util.BanknetErrors;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.processor.Common;

import banknetvn.md5.checkMD5;


/**
 * Created by User on 3/11/14.
 */
public class BanknetFunction {

    private static DirectPaymentPortType portType;
    //default parameters
    String  merchant_code = "";
    String  country_code = "";
    String  good_code = "";
    String  ship_fee = "";
    String  tax = "";
    String  merchant_trans_key = "";
    String  otp_type = "";
    Logger logger;
    checkMD5 md5 = new checkMD5();
    SettingsDb settingsDb;

    public BanknetFunction(EventBus eventBus, JsonObject banknetCfg,Logger logger){

        settingsDb = new SettingsDb(eventBus,logger);

        try{
            logger.info("Load default parameters...........");
            this.logger =logger;

            //default parameter
            merchant_code 	    = banknetCfg.getObject("partner").getString("merchan.code");
            country_code	    = banknetCfg.getObject("partner").getString("country.code");
            good_code		    = banknetCfg.getObject("partner").getString("good.code");
            ship_fee		    = banknetCfg.getObject("partner").getString("ship.fee");
            tax				    = banknetCfg.getObject("partner").getString("tax");
            merchant_trans_key  = banknetCfg.getObject("partner").getString("merchant.trans.key");
            otp_type		    = banknetCfg.getObject("partner").getString("otp.type");

            String spec	= banknetCfg.getObject("partner").getString("ws.url"
                                    ,"http://sandbox.bndebit.vn/pg2705/services/DirectPayment.DirectPaymentHttpSoap12Endpoint");
            
            portType = new DirectPaymentPortTypeProxy(spec);
            logger.info("     merchant_code >> " + merchant_code);
            logger.info("      country_code >> " + country_code);
            logger.info("         good_code >> " + good_code);
            logger.info("          ship_fee >> " + ship_fee);
            logger.info("               tax >> " + tax);
            logger.info("merchant_trans_key >> " + merchant_trans_key);
            logger.info("            WS URL >> " + spec);
            logger.info("");
        }catch(Exception e){
            logger.info(e.getMessage());
        }
    }

    /*public static void main(String[] args) {
        try{
            String phonenumber_ = "0917244824";
            String amount_		= "100000";
            String bankID_		= "161087"; // saigonbank
            String card_holder_name_ 	= "Ly Dinh Quang";
            String card_holder_number_ 	= "123456789";
            String card_holder_month_	= "12";
            String card_holder_year_	= "09";

            BanknetFunction function = new BanknetFunction();
            String str_responose = function.doBankNet(phonenumber_, amount_, bankID_,
                    card_holder_name_, card_holder_number_, card_holder_month_,
                    card_holder_year_);
            logger.info("%%% str_responose >>> " + str_responose);
        }catch(Exception e){
            e.printStackTrace();
        }
    }*/

    public void doBankNet(final String amount
                            ,final String bankID
                            ,final String card_holder_name
                            ,final String card_holder_number
                            ,final String card_holder_month
                            ,final String card_holder_year
                            ,final Common.BuildLog log
                            ,final Handler<BanknetResponse> callback)
    {

        settingsDb.incAndGetLong("BANKNET_SEED",1,new Handler<Long>() {
            @Override
            public void handle(Long aLong) {

                int seed = DataUtil.strToInt(String.valueOf(aLong));

                BanknetRequest bnReq        = new BanknetRequest();
                long ms_transid				= System.currentTimeMillis();
                String merchant_trans_id	= getMerchantTransId(seed,log); // this.getMerchant_trans_id();

                bnReq.ms_transid= ms_transid;

                //SendGodd_ext
                bnReq.merchant_trans_id = merchant_trans_id;
                bnReq.merchant_code = merchant_code;
                bnReq.country_code=country_code;
                bnReq.good_code=good_code;
                bnReq.xml_description="";
                bnReq.net_cost=amount;
                bnReq.ship_fee = ship_fee;
                bnReq.tax=tax;
                bnReq.merchant_trans_key =merchant_trans_key;
                bnReq.trans_datetime=getCurrentTime();
                bnReq.selected_bank =bankID;

                //CheckCardHolder
                bnReq.card_holder_name=card_holder_name;
                bnReq.card_holder_number=card_holder_number;
                bnReq.card_holder_month=card_holder_month;
                bnReq.card_holder_year =card_holder_year;
                bnReq.otpGetType=otp_type;

                log.add("Merchant_trans_id",bnReq.merchant_trans_id);
                log.add("ms_transid",bnReq.ms_transid);
                log.add("Merchant_code",bnReq.merchant_code);
                log.add("Merchant_trans_key",bnReq.merchant_trans_key);
                log.add("Trans_datetime",bnReq.trans_datetime);
                log.add("BankID",bnReq.selected_bank);
                log.add("card_holder_name",card_holder_name);
                log.add("Card_holder_number",card_holder_number);
                log.add("Card_holder_month",card_holder_month);
                log.add("Card_holder_year",card_holder_year);
                log.add("amount", bnReq.net_cost);
                log.add("Otp_type",otp_type);

                //for sendGoodInfo_Ext
                /*md5(merchant_trans_id
                                +merchant_code
                                +good_code
                                +net_cost
                                +ship_fee
                                +tax
                                +merchant_trans_key)*/

                String str_trans_secure_code = 	bnReq.merchant_trans_id
                                                + bnReq.merchant_code
                                                + bnReq.good_code
                                                + bnReq.net_cost
                                                + bnReq.ship_fee
                                                + bnReq.tax
                                                + bnReq.merchant_trans_key;


                log.add("before MD5",str_trans_secure_code);

                String trans_secure_code="";
                BanknetResponse bnRes = new BanknetResponse();

                try {
                    trans_secure_code = md5.getMD5Hash(str_trans_secure_code);

                    log.add("after MD5", trans_secure_code);

                    //set trans secure code for request command
                    bnReq.trans_secure_code = trans_secure_code;

                    log.add("begin SendGoodInfo_Ext", "");
                    bnRes = SendGoodInfo_Ext(bnReq, log);

                    log.add("response code", bnRes.reponsecode);
                    log.add("errdesc", BanknetErrors.getDesc(bnRes.reponsecode));
                    log.add("trans_id", bnRes.trans_id);
                    log.add("command_code", bnRes.command_code);
                    log.add("trans_secure_code", bnRes.trans_secure_code);
                    log.add("end SendGoodInfo_Ext", "");

                    //lay tran_id from bank-net not successed
                    if (bnRes.reponsecode != 0) {
                        callback.handle(bnRes);
                        return;
                    }

                }catch (UnsupportedEncodingException e){

                    log.add("exception SendGoodInfo_Ext","UnsupportedEncodingException " + e.getMessage());
                    callback.handle(bnRes);
                    return;
                }catch (NoSuchAlgorithmException en){

                    log.add("exception","SendGoodInfo_Ext NoSuchAlgorithmException " + en.getMessage());
                    callback.handle(bnRes);

                    return;
                }catch (ConnectException se){
                    bnRes.reponsecode = 8; // connection timeout
                    log.add("response code", bnRes.reponsecode);
                    log.add("errdesc", BanknetErrors.getDesc(bnRes.reponsecode));
                    log.add("exception","SendGoodInfo_Ext ConnectException " + se.getMessage());
                    callback.handle(bnRes);
                    return;
                }catch (RemoteException se){
                    bnRes.reponsecode = 8; // connection timeout
                    log.add("response code", bnRes.reponsecode);
                    log.add("errdesc", BanknetErrors.getDesc(bnRes.reponsecode));
                    log.add("exception","SendGoodInfo_Ext ConnectException " + se.getMessage());
                    callback.handle(bnRes);
                    return;
                }catch (Exception ex){
                    bnRes.reponsecode = -1; // connection timeout
                    log.add("response code", bnRes.reponsecode);
                    log.add("errdesc", BanknetErrors.getDesc(bnRes.reponsecode));
                    log.add("exception","SendGoodInfo_Ext ConnectException " + ex.getMessage());
                    callback.handle(bnRes);
                }

                //for CheckCardHolder
                /*md5(merchant_trans_id
                    +merchant_code
                    +merchant_trans_key
                    +trans_id
                    +card_holder_number
                    +card_holder_year)*/

                str_trans_secure_code = bnReq.merchant_trans_id
                                        + bnReq.merchant_code
                                        + bnReq.merchant_trans_key
                                        + bnRes.trans_id
                                        + bnReq.card_holder_number
                                        + bnReq.card_holder_year;


                try {
                    trans_secure_code = md5.getMD5Hash(str_trans_secure_code);
                    bnReq.trans_secure_code=trans_secure_code;

                    bnReq.trans_id =bnRes.trans_id;
                    log.add("begin CheckCardholder","");

                    log.add("before MD5", str_trans_secure_code);
                    log.add("after MD5", bnReq.trans_secure_code);

                    bnRes = CheckCardholder(bnReq,log);

                    log.add("reponsecode", bnRes.reponsecode);
                    log.add("errdesc", BanknetErrors.getDesc(bnRes.reponsecode));
                    log.add("trans_id", bnRes.trans_id);
                    log.add("command_code", bnRes.command_code);
                    log.add("trans_secure_code", bnRes.trans_secure_code);
                    log.add("end CheckCardholder","");

                }
				catch (ConnectException se){
                    bnRes.reponsecode = 8; // connection timeout
                    log.add("response code", bnRes.reponsecode);
                    log.add("errdesc", BanknetErrors.getDesc(bnRes.reponsecode));
                    log.add("exception","CheckCardholder ConnectException " + se.getMessage());
                    callback.handle(bnRes);
                    return;
                }
                catch (RemoteException se){
                    bnRes.reponsecode = 8; // connection timeout
                    log.add("response code", bnRes.reponsecode);
                    log.add("errdesc", BanknetErrors.getDesc(bnRes.reponsecode));
                    log.add("exception","CheckCardholder ConnectException " + se.getMessage());
                    callback.handle(bnRes);
                    return;
                }
                catch (Exception ex){
                    bnRes.reponsecode = -1; // connection timeout
                    log.add("response code", bnRes.reponsecode);
                    log.add("errdesc", BanknetErrors.getDesc(bnRes.reponsecode));
                    log.add("exception","CheckCardholder ConnectException " + ex.getMessage());
                    callback.handle(bnRes);
                    return;
                }

                callback.handle(bnRes);
            }
        });
    }

    private BanknetResponse mapResponseCode( String str_response, Common.BuildLog log){
        BanknetResponse bankResponse = new BanknetResponse();
        try{
            //xx|merchant_trans_id|trans_id|command_code|trans_secure_code
            String []paras = str_response.split("\\|");
            log.add("response param len",paras.length);
            if(paras.length==5){
                bankResponse.reponsecode= Integer.valueOf(paras[0].trim()) ;
                //bankResponse.merchant_trans_id=paras[1].trim();
                bankResponse.trans_id=paras[2].trim();
                bankResponse.command_code=paras[3].trim();
                bankResponse.trans_secure_code = paras[4].trim();

            }else if(paras.length==3){

                bankResponse.reponsecode=Integer.valueOf(paras[0].trim()) ;
                bankResponse.bill_status=paras[1].trim();
                bankResponse.trans_secure_code=paras[2].trim();
            }

        }catch(Exception e){
            e.printStackTrace();
            log.add("map response code exception", e.getMessage());
        }

        return bankResponse;
    }

    public String getCurrentTime(){
        try{
            String currentTime = "";
            long time 	= System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            currentTime	= sdf.format(new Date(time));
            return currentTime;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public BanknetResponse SendGoodInfo_Ext(BanknetRequest bnReq, Common.BuildLog log) throws ConnectException, RemoteException
    {
        BanknetResponse bnRes;
        String str_response = portType.sendGoodInfo_Ext(bnReq.merchant_trans_id
                , bnReq.merchant_code
                , bnReq.country_code
                , bnReq.good_code
                , bnReq.xml_description
                , bnReq.net_cost
                , bnReq.ship_fee
                , bnReq.tax
                , bnReq.trans_datetime
                , bnReq.trans_secure_code
                , bnReq.selected_bank);



        //MAP STR_BANKNET RESPONSE
        bnRes = mapResponseCode(str_response,log);
        //UPDATE DATE THONG TIN TRANSACTION XUONG DB
        //02|000001||010|2b539501aebe1fc840f07afbd0a5f800

        return bnRes;
    }

    public BanknetResponse CheckCardholder(BanknetRequest bnReq, Common.BuildLog log) throws ConnectException, RemoteException {

        BanknetResponse bnRes;
        String str_response = portType.checkCardHolder(bnReq.merchant_trans_id
                , bnReq.merchant_code
                , bnReq.trans_id
                , bnReq.card_holder_number
                , bnReq.card_holder_name
                , bnReq.card_holder_month
                , bnReq.card_holder_year
                , bnReq.otpGetType
                , bnReq.trans_secure_code);
        //MAP STR_BANKNET RESPONSE
        bnRes = mapResponseCode(str_response,log);
        bnRes.merchant_trans_id = bnReq.merchant_trans_id;

        //UPDATE DATE THONG TIN TRANSACTION XUONG DB
        //02|000001||010|2b539501aebe1fc840f07afbd0a5f800

        // luc nay thang bank-net thong bao ket qua da chuyen tien ve thanh cong/that bai.
        //core thuc hien 1 lenh chuyen tien tiep cho khach hang tai day dung ham transfer
        return bnRes;
    }

    public BanknetResponse VerifyOTP(String otpCode, String merchant_trans_id, String trans_id, Common.BuildLog log){
        log.add("function","VerifyOTP");
        log.add("Merchant_trans_id",merchant_trans_id);
        log.add("Merchant_code",merchant_code);
        log.add("trans_id",trans_id);
        log.add("OTP code",otpCode);
        log.add("otp_type",otp_type);

        BanknetResponse bankResponse = new BanknetResponse();
        String str_trans_secure_code = merchant_trans_id
                                        + merchant_code
                                        + merchant_trans_key
                                        + otpCode;
        checkMD5 md5 = new checkMD5();
        String trans_secure_code;

        try{
            trans_secure_code = md5.getMD5Hash(str_trans_secure_code);

            log.add("str_trans_secure_code",str_trans_secure_code);
            log.add("trans_secure_code",trans_secure_code);
            log.add("begin verifyOTP","");
            String str_response = portType.verifyOTP(merchant_trans_id
                                                    , merchant_code
                                                    , trans_id
                                                    , otpCode
                                                    , trans_secure_code
                                                    , otp_type);

            log.add("verify OTP response",str_response);
            bankResponse = mapResponseCode(str_response,log);
            log.add("reponsecode",bankResponse.reponsecode);
            log.add("errdesc", BanknetErrors.getDesc(bankResponse.reponsecode));
            log.add("merchant_trans_id",bankResponse.merchant_trans_id);
            log.add("trans_id",bankResponse.trans_id);
            log.add("command_code",bankResponse.command_code);
            log.add("trans_secure_code",bankResponse.trans_secure_code);

        }
        catch (RemoteException re){

            bankResponse.reponsecode = 8; // connection timeout
            log.add("response code", bankResponse.reponsecode);
            log.add("errdesc", BanknetErrors.getDesc(bankResponse.reponsecode));
            log.add("exception","VerifyOTP ConnectException " + re.getMessage());
            return bankResponse;
        }

        catch(Exception e){
            bankResponse.reponsecode = -1;
            log.add("errdesc", BanknetErrors.getDesc(-1));
            log.add("exception","VerifyOTP " + e.getMessage());
        }

        return bankResponse;
    }

    public BanknetResponse QueryBillStatus(BanknetRequest bnReq, Common.BuildLog log){
        BanknetResponse bnRes = null;
        try{
            logger.info("<<<---------------- Start QueryBillStatus Request ---------------->>>");
            logger.info("    Merchant_trans_id >> " + bnReq.merchant_trans_id);
            logger.info("             Trans_id >> " + bnReq.trans_id);
            logger.info("        Merchant_code >> " + bnReq.merchant_code);
            logger.info("    Trans_secure_code >> " + bnReq.trans_secure_code);

            DirectPaymentPortType portType = new DirectPaymentPortTypeProxy();

            String str_response = portType.querryBillStatus(bnReq.merchant_trans_id
                                                            , bnReq.trans_id
                                                            , bnReq.merchant_code
                                                            , bnReq.trans_secure_code);

            //MAP STR_BANKNET RESPONSE
            bnRes = mapResponseCode(str_response,log);
            //UPDATE DATE THONG TIN TRANSACTION XUONG DB
            //02|000001||010|2b539501aebe1fc840f07afbd0a5f800

            logger.info("<<<---------------- End QueryBillStatus Request ---------------->>>");

        }catch(Exception e){
            e.printStackTrace();
            bnRes = new BanknetResponse();
            bnRes.reponsecode=2;
            bnRes.merchant_trans_id=bnReq.merchant_trans_id;
            bnRes.trans_id="";
            bnRes.command_code ="";
            bnRes.trans_secure_code="2b539501aebe1fc840f07afbd0a5f800";
        }
        return bnRes;
    }

    public String getMerchantTransId(int seed, Common.BuildLog log){
        int org_number = seed%1000000;// max 999999;
        String number = "000000" + org_number;
        number = number.substring(number.length() - 6, number.length());
        log.add("seed", number);

        return number;
    }

    public BanknetResponse ConfirmTransactionResult(int phoneNumber
                                                        ,String merchant_trans_id
                                                        ,String trans_id
                                                        ,int adjustResult){
        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber("0" + phoneNumber);
        log.add("merchant_trans_id",merchant_trans_id);
        log.add("trans_id",trans_id);
        log.add("adjustResult",adjustResult);
        BanknetResponse bankResponse = new BanknetResponse();

        try{
            String str_trans_secure_code = merchant_trans_id
                                            + trans_id
                                            + merchant_code
                                            + adjustResult
                                            + merchant_trans_key ;
            checkMD5 md5 = new checkMD5();
            String trans_secure_code = md5.getMD5Hash(str_trans_secure_code);

            String str_response = portType.confirmTransactionResult(merchant_trans_id
                                                                    ,trans_id
                                                                    ,merchant_code
                                                                    ,adjustResult + ""
                                                                    ,trans_secure_code);
            //MAP STR_BANKNET RESPONSE
            bankResponse = mapResponseCode(str_response,log);
            log.add("reponsecode",bankResponse.reponsecode);
            log.add("errdesc",BanknetErrors.getDesc(bankResponse.reponsecode));
            log.writeLog();

            return bankResponse;
        }catch (RemoteException re){
            bankResponse.reponsecode = 8;
            bankResponse.merchant_trans_id = merchant_trans_id;
            bankResponse.trans_id = "";
            bankResponse.command_code ="";
            bankResponse.trans_secure_code ="";

            log.add("loi","ConfirmTransactionResult loi remote qua banknet");
            log.add("errdesc", BanknetErrors.getDesc(bankResponse.reponsecode));
            log.writeLog();
            return bankResponse;
        }
        catch(Exception e){
            bankResponse.reponsecode = -1;
            bankResponse.merchant_trans_id = merchant_trans_id;
            bankResponse.trans_id = "";
            bankResponse.command_code ="";
            bankResponse.trans_secure_code ="";
            log.add("loi","ConfirmTransactionResult loi");
            log.add("errdesc", BanknetErrors.getDesc(bankResponse.reponsecode));

            log.writeLog();
            return bankResponse;
        }
    }
}
