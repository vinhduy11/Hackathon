package com.mservice.momo.gateway.internal.soapin.information;

import com.mservice.momo.data.BillInfoService;
import com.mservice.momo.data.model.Const;
import com.mservice.momo.gateway.internal.soapin.information.obj.AgentInfo;
import com.mservice.momo.gateway.internal.soapin.information.obj.LoginTrustRespond;
import com.mservice.momo.gateway.internal.soapin.information.obj.RegisterInfo;
import com.mservice.momo.gateway.internal.soapin.information.obj.StandardMSResponse;
import com.mservice.momo.gateway.internal.soapin.information.permission.Cryption;
import com.mservice.momo.gateway.internal.soapin.information.session.SessionInfo;
import com.mservice.momo.gateway.internal.soapin.information.session.SessionManagerPlus;
import com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.BillingService;
import com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.PayBillRequest;
import com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.PayBillResponse;
import com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.UMarketServiceProviderLocator;
import com.mservice.momo.msg.SoapProto;
import com.mservice.momo.util.DataUtil;
import com.mservice.momo.vertx.processor.Common;
import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import umarketscws.*;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import static com.mservice.momo.util.DataUtil.getSHA;

/**
 * Created by concu on 9/25/15.
 */
public class SoapInProcessPlus {


        private Logger logger;
        private UMarketSC stub = null;

        private String wsdl = null;
        private JsonObject biller_cfg;
        private JsonObject pay_cfg;
        private JsonObject bank_direct_cfg;
        private String soap_un = "";
        private String soap_pw ="";
        private JsonObject fi_cfg;
        private JsonObject remain_referal;

        public SoapInProcessPlus(Logger logger, JsonObject glbCfg) {
            this.logger = logger;

            //for soapin
            this.wsdl = glbCfg.getObject("soap_2").getString("soap_url_2","http://172.16.18.50:8280/services/umarketsc?wsdl");
            this.biller_cfg =glbCfg.getObject("soap").getObject("billchk_cfg");
            this.pay_cfg =glbCfg.getObject("soap").getObject("billpay_cfg");
            this.bank_direct_cfg = glbCfg.getObject("soap").getObject("bank_direct");
            this.soap_un =glbCfg.getObject("soap_2").getString("soap.un_2");
            this.soap_pw = glbCfg.getObject("soap_2").getString("soap.pw_2");
            this.fi_cfg = glbCfg.getObject("fi");
            this.remain_referal = glbCfg.getObject("remain_for_referal",null);
            callSoap();
        }

        private void callSoap() {
            URL url = null;
            try {
                url = new URL(wsdl);
            } catch (MalformedURLException e) {
                logger.error("Can not connect to SOAPIN URL", e);
            }

            UMarketSC_Service soap;
            try {
                soap = new UMarketSC_Service(url);
                stub = soap.getUMarketSC();
            } catch (Exception e) {
                logger.error("", e);
            }
        }

        public AgentInfo getAgentInfo(String agentReference) {
            if (stub == null)
                callSoap();

            Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber(agentReference);
            log.add("function","getAgentInfo");

            CreatesessionResponseType csrt = SessionManagerPlus.getCsrt(log.getTime());
            if(csrt==null){
                log.add("csrt","null");
            }

            AgentInfo agentInfo = null;
            GetAgentByReferenceRequest agentByReferenceRequest = new GetAgentByReferenceRequest();

            agentByReferenceRequest.setReference(agentReference);
            agentByReferenceRequest.setSessionid(csrt.getSessionid());
            AgentResponseType agentResponseType = stub.getAgentByReference(agentByReferenceRequest);

            log.add("AgentResponseType result",agentResponseType.getResult());

            if (agentResponseType.getResult() == 0) {
                MarketAgent marketAgent = agentResponseType.getAgent();
                agentInfo = new AgentInfo();
                agentInfo.setAgentReference(marketAgent.getSalt());
                agentInfo.setFullName(marketAgent.getName() == null ? "" : marketAgent.getName());
                agentInfo.setAddress(marketAgent.getAddress() == null ? "" : marketAgent.getAddress());
                agentInfo.setAgentid(marketAgent.getAgentID());

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.MINUTE, marketAgent.getCreatedDate().getMinute());
                cal.set(Calendar.SECOND, marketAgent.getCreatedDate().getSecond());
                cal.set(Calendar.HOUR_OF_DAY, marketAgent.getCreatedDate().getHour());
                cal.set(Calendar.DAY_OF_MONTH, marketAgent.getCreatedDate().getDay());
                cal.set(Calendar.MONTH, marketAgent.getCreatedDate().getMonth() - 1); // Month Field start from zero
                cal.set(Calendar.YEAR, marketAgent.getCreatedDate().getYear());

                agentInfo.setCreatedDate(cal.getTime());


                //get agent group
                GetAgentGroupByAgentIDRequest getAgentGroupByAgentIDRequest = new GetAgentGroupByAgentIDRequest();
                getAgentGroupByAgentIDRequest.setAgentID(marketAgent.getAgentID());
                getAgentGroupByAgentIDRequest.setSessionid(csrt.getSessionid());
                AgentGroupsResponseType agentGroupsResponseType = stub.getAgentGroupByAgentID(getAgentGroupByAgentIDRequest);

                //lay thong tin dinh danh.
                boolean isNamed = true;
                //khong nan trong nhom nao -->chac chan la dinh danh
                if(agentGroupsResponseType.getAgentGroups().size() > 0){

                    for(int i =0; i< agentGroupsResponseType.getAgentGroups().size();i++){
                        log.add("groupID:", agentGroupsResponseType.getAgentGroups().get(i).getID());
                        log.add("groupName:", agentGroupsResponseType.getAgentGroups().get(i).getName().trim());

                        if(agentGroupsResponseType.getAgentGroups().get(i).getName() !=null
                                &&  "noname".equalsIgnoreCase(agentGroupsResponseType.getAgentGroups().get(i).getName().trim())){
                            isNamed=false;
                            break;
                        }
                    }
                }

                log.add("isnamed",isNamed);
                agentInfo.setIsNamed(isNamed);

                agentInfo.setEmail(marketAgent.getEmailAddress());
                agentInfo.setSmsAddress(marketAgent.getSMSAddress());
                agentInfo.setRefid(marketAgent.getID());

                List<com.utiba.delirium.ws.misc.KeyValuePair> agentData = marketAgent.getAgentData();
                int length = agentData.size();
                for (int i = 0; i < length; i++) {
                    com.utiba.delirium.ws.misc.KeyValuePair currAgentData = agentData.get(i);
                    if (currAgentData.getKey().equalsIgnoreCase("bank_name"))
                        agentInfo.setBank_name(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("bank_code"))
                        agentInfo.setBank_code(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("bank_acc_no"))
                        agentInfo.setBank_acc_no(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("alt_id_type"))
                        agentInfo.setBusiness_name(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("alt_id_type_add"))
                        agentInfo.setBusiness_address(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("contact_no"))
                        agentInfo.setContact_no(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("date_of_birth"))
                        agentInfo.setDateOfBirth(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("address"))
                        agentInfo.setAddress(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("ward_code"))
                        agentInfo.setWard_code(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("province_code"))
                        agentInfo.setProvince_code(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("score"))
                        agentInfo.setScore(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("dist_code"))
                        agentInfo.setDist_code(currAgentData.getValue());
                    else if (currAgentData.getKey().equalsIgnoreCase("personal_id")) {//Split ~
                        String[] personalData = currAgentData.getValue().split("~");
                        if (personalData.length == 3) {
                            agentInfo.setPersonalID(personalData[0]);
                            agentInfo.setDate_of_persionalid(personalData[1]);
                            agentInfo.setPlace_of_persionalid(personalData[2]);
                        } else {
                            agentInfo.setPersonalID(currAgentData.getValue());
                        }
                    }
                }
                if (agentInfo.getBank_name() == null)
                    agentInfo.setBank_name("");
                if (agentInfo.getBank_acc_no() == null)
                    agentInfo.setBank_acc_no("");
                if (agentInfo.getBusiness_name() == null)
                    agentInfo.setBusiness_name("");
                if (agentInfo.getBusiness_address() == null)
                    agentInfo.setBusiness_address("");
                if (agentInfo.getContact_no() == null)
                    agentInfo.setContact_no("");
                if (agentInfo.getDateOfBirth() == null)
                    agentInfo.setDateOfBirth("");
                if (agentInfo.getAddress() == null)
                    agentInfo.setAddress("");
                if (agentInfo.getPersonalID() == null)
                    agentInfo.setPersonalID("");
                if (agentInfo.getDate_of_persionalid() == null)
                    agentInfo.setDate_of_persionalid("");
                if (agentInfo.getPlace_of_persionalid() == null)
                    agentInfo.setPlace_of_persionalid("");
                if (agentInfo.getEmail() == null)
                    agentInfo.setEmail("");
                if (agentInfo.getScore() == null)
                    agentInfo.setScore("0");
                agentInfo.setStatus(marketAgent.getStatus());
            }
            log.writeLog();
            return agentInfo;
        }

        public boolean unMapNoNameGroup(String sessionId, String agent,long groupIdNoname,Common.BuildLog log){

            log.add("groupIdNoname", groupIdNoname);
            UnmapAgentRequest unmapAgentRequest = new UnmapAgentRequest();
            UnmapAgentRequestType unmapAgentRequestType = new UnmapAgentRequestType();

        /*String agent = ""; // mong muon dinh danh cho agent nay
        long groupIdNoname = 124512;*/
            unmapAgentRequestType.setAgent(agent);
            unmapAgentRequestType.setAgid(groupIdNoname);

            unmapAgentRequestType.setSessionid(sessionId);

            unmapAgentRequest.setUnmapAgentRequestType(unmapAgentRequestType);

            UnmapAgentResponse response = stub.unmapAgent(unmapAgentRequest);
            if(response  == null){
                log.add("UnmapAgentResponse" , "null");
                return false;
            }
            StandardBizResponse bizResponse = response.getUnmapAgentReturn();
            if(bizResponse == null){
                log.add("StandardBizResponse" , "null");
                return  false;
            }

            log.add("unmap group result", bizResponse.getResult());
            int eCode = bizResponse.getResult();
            log.add("desc",SoapError.getDesc(eCode));
            return (eCode == 0);
        }

        public StandardMSResponse topupAirtime(Vertx _vertx, String agentReference
                ,String mpin
                ,String phoneNumber
                ,long amount
                ,int walletType
                ,String channel
                ,List<SoapProto.keyValuePair> listKeyValuePair
                ,Common.BuildLog log) {
            if (stub == null)
                callSoap();

            phoneNumber ="0" + DataUtil.strToInt(phoneNumber);
            log.add("func", "soap.topupAirtime");
            log.add("to", phoneNumber);
            log.add("pinlen",mpin.length());
            log.add("amount", amount);
            log.add("walletType", walletType);
            log.add("channel", channel);

            SessionInfo sessionInfo = SessionManagerPlus.getCsrt(_vertx
                    ,agentReference
                    ,mpin
                    ,log.getTime());

            if(sessionInfo == null){
                log.add("SessionManagerPlus.getCsrt", "null");
                return null;
            }

            int loginResultCode = sessionInfo.getLoginResult();

            CreatesessionResponseType csrt;
            StandardMSResponse response = new StandardMSResponse();
            if (loginResultCode == 0) {
                log.add("login result",loginResultCode);
                csrt = sessionInfo.getCsrt();
            } else {
                response = new StandardMSResponse();
                response.setResultCode(ErrorMapping.getResultCode(loginResultCode));
                response.setDescription(ErrorMapping.getDescriptionError(loginResultCode));

                log.add("login result", loginResultCode);
                return response;
            }

            if (csrt == null) {
                log.add("csrt","null");

                return null;
            }

            StandardBizResponse standardBizResponse = null;
            String target = TopupMapping.getTargetTopup(phoneNumber, agentReference,log);

            log.add("TopupMapping.getTargetTopup result", target);

            if (!"".equalsIgnoreCase(target))//phoneNumber hop le
            {
                com.utiba.delirium.ws.misc.KeyValuePair client = new com.utiba.delirium.ws.misc.KeyValuePair();
                client.setKey("client");
                client.setValue("backend");

                com.utiba.delirium.ws.misc.KeyValuePair chanel = new com.utiba.delirium.ws.misc.KeyValuePair();
                chanel.setKey("chanel");
                chanel.setValue(channel);

                com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
                kvIsSMS.setKey("issms");
                kvIsSMS.setValue("no");

                com.utiba.delirium.ws.misc.KeyValuePairMap kvpm = new com.utiba.delirium.ws.misc.KeyValuePairMap();


                //set them keys
                setKeyValuePairForCommand(kvpm,listKeyValuePair,log);

                kvpm.getKeyValuePairs().add(client);
                kvpm.getKeyValuePairs().add(chanel);
                kvpm.getKeyValuePairs().add(kvIsSMS);

                BuyRequestType buyRequestType = new BuyRequestType();
                buyRequestType.setExtraTransData(kvpm);
                buyRequestType.setSessionid(csrt.getSessionid());
                buyRequestType.setAmount(new BigDecimal(amount));
                buyRequestType.setTarget(target);
                buyRequestType.setType(walletType == 2 ? 2 : 1);
                buyRequestType.setRecipient(phoneNumber);
                standardBizResponse = stub.buy(buyRequestType);

                int tranId = -100;
                int rcode = -100;
                String desc = "";

                if(standardBizResponse != null && standardBizResponse.getTransid() != null){
                    tranId = standardBizResponse.getTransid();
                    rcode = standardBizResponse.getResult();
                }else{
                    if(standardBizResponse == null){
                        desc ="standardBizResponse == null";
                    }else if(standardBizResponse.getTransid() == null){
                        desc = "standardBizResponse.getTransid() == null";
                    }else{
                        desc="Khong xac dinh duoc ket qua tra ve tu core";
                    }
                }

                log.add("tranid", tranId);
                log.add("rcode", rcode);
                log.add("desc", desc);
                log.add("errorDesc", SoapError.getDesc(rcode));

                if (standardBizResponse != null && standardBizResponse.getTransid() != null) {

                    response.setTransID(standardBizResponse.getTransid().toString());
                    response.setResultCode(standardBizResponse.getResult());

                } else if (standardBizResponse != null && standardBizResponse.getTransid() == null) {
                    return null;
                } else {
                    return null;
                }

            } else {
                response.setTransID(System.currentTimeMillis() / 100000 + new Random().nextInt(100000) + "");
                response.setResultCode(47);
            }

            return response;
        }

        public StandardMSResponse transfer(Vertx _vertx,String agentReference
                ,String mpin
                ,String phoneNumber
                ,long amount
                ,String notice
                ,String channel
                ,String m2mType
                ,int isSMS
                ,List<SoapProto.keyValuePair> kvps
                ,Common.BuildLog log) {

            if (stub == null){
                callSoap();
            }

            StandardMSResponse response = new StandardMSResponse();
            log.setPhoneNumber(agentReference);
            phoneNumber = "0" + DataUtil.strToInt(phoneNumber);

            log.add("func", "transfer");
            log.add("to", phoneNumber);
            log.add("pinlen",mpin.length());
            log.add("amount", amount);
            log.add("notice",notice);
            log.add("channel",channel);
            log.add("hasSMS", isSMS);
            log.add("transfertype", m2mType);

            SessionInfo sessionInfo = SessionManagerPlus.getCsrt(_vertx
                    ,agentReference
                    ,mpin
                    ,log.getTime());
            if(sessionInfo == null){
                log.add("get SessionInfo","null");
                return null;
            }

            int loginResultCode = sessionInfo.getLoginResult();

            CreatesessionResponseType csrt = null;
            if (loginResultCode == 0) {
                log.add("login result",loginResultCode);
                csrt = sessionInfo.getCsrt();
            } else {
                response = new StandardMSResponse();
                response.setResultCode(ErrorMapping.getResultCode(loginResultCode));
                response.setDescription(ErrorMapping.getDescriptionError(loginResultCode));

                log.add("login result",loginResultCode);
                log.writeLog();
                return response;
            }

            if (csrt == null) {
                log.add("csrt", "null");
                return null;
            }

            com.utiba.delirium.ws.misc.KeyValuePair message = new com.utiba.delirium.ws.misc.KeyValuePair();
            message.setKey("message");
            message.setValue(notice);

            com.utiba.delirium.ws.misc.KeyValuePair client = new com.utiba.delirium.ws.misc.KeyValuePair();
            client.setKey("client");
            client.setValue("backend");

            com.utiba.delirium.ws.misc.KeyValuePair chanel = new com.utiba.delirium.ws.misc.KeyValuePair();
            chanel.setKey("chanel");
            chanel.setValue(channel);

            com.utiba.delirium.ws.misc.KeyValuePair m2mTypeKp = new com.utiba.delirium.ws.misc.KeyValuePair();
            m2mTypeKp.setKey("m2mtype");
            m2mTypeKp.setValue(m2mType);

            com.utiba.delirium.ws.misc.KeyValuePair isSmsKp = new com.utiba.delirium.ws.misc.KeyValuePair();
            isSmsKp.setKey("issms");

            //all transfer througth issms = no, no use isSMS flag at this time
            String sms = "no";

        /*if(isSMS==0){
            sms ="no";
        }*/

            isSmsKp.setValue(sms);

            com.utiba.delirium.ws.misc.KeyValuePairMap kvpm = new com.utiba.delirium.ws.misc.KeyValuePairMap();

            setKeyValuePairForCommand(kvpm,kvps,log);

            kvpm.getKeyValuePairs().add(message);
            kvpm.getKeyValuePairs().add(client);
            kvpm.getKeyValuePairs().add(chanel);

            kvpm.getKeyValuePairs().add(isSmsKp);
            kvpm.getKeyValuePairs().add(m2mTypeKp);

            TransferRequestType transferRequestType = new TransferRequestType();

            transferRequestType.setSessionid(csrt.getSessionid());
            transferRequestType.setAmount(new BigDecimal(amount));
            transferRequestType.setTo(phoneNumber);
            transferRequestType.setType(1);

            transferRequestType.setExtraTransData(kvpm);

            StandardBizResponse standardBizResponse = stub.transfer(transferRequestType);

            int tranId = -100;
            int rcode  = -100;
            String desc = "";
            if(standardBizResponse != null && standardBizResponse.getTransid() != null){
                tranId = standardBizResponse.getTransid();
                rcode = standardBizResponse.getResult();
            }else{
                if(standardBizResponse == null){
                    desc ="standardBizResponse == null";
                }else if(standardBizResponse.getTransid() == null){
                    desc = "standardBizResponse.getTransid() == null";
                }else{
                    desc="Khong xac dinh duoc ket qua tra ve tu core";
                }
            }

            log.add("tranid", tranId);
            log.add("rcode", rcode);
            log.add("desc", desc);
            log.add("errorDesc", SoapError.getDesc(rcode));

            if (standardBizResponse != null && standardBizResponse.getTransid() != null) {

                response.setTransID(standardBizResponse.getTransid().toString());
                response.setResultCode(standardBizResponse.getResult());
            } else if (standardBizResponse != null && standardBizResponse.getTransid() == null) {

                return null;
            } else {

                return null;
            }
            return response;
        }

    /*public StandardBizResponse transferM2c(String agentReference, String mpin, MsTransfer transfer, String channel) {
        String tarGet = m_sys_c2c_in_account;
        logger.info("source: " + agentReference + " target: " + tarGet + " amount: " + transfer.getAmount() + " walletType: " + 1);
        if (stub == null)
            callSoap();
        SessionInfo sessionInfo = SessionManager.getCsrt(agentReference, mpin, true);

        TransferRequestType transferRequestType = new TransferRequestType();

        com.utiba.delirium.ws.misc.KeyValuePair recipient = new com.utiba.delirium.ws.misc.KeyValuePair();
        recipient.setKey("recipient");
        recipient.setValue("" + transfer.getTarget());

        com.utiba.delirium.ws.misc.KeyValuePair recipient_name = new com.utiba.delirium.ws.misc.KeyValuePair();
        recipient_name.setKey("recipientname");
        recipient_name.setValue("" + transfer.getTargetname());

        com.utiba.delirium.ws.misc.KeyValuePair recipient_id = new com.utiba.delirium.ws.misc.KeyValuePair();
        recipient_id.setKey("recipientid");
        recipient_id.setValue("" + transfer.getTargetid().replace(" ", ""));

        com.utiba.delirium.ws.misc.KeyValuePair message = new com.utiba.delirium.ws.misc.KeyValuePair();
        message.setKey("message");
        message.setValue("" + transfer.getMessage());

        KeyValuePair client = new KeyValuePair();
        client.setKey("client");
        client.setValue("backend");

        KeyValuePair chanel = new KeyValuePair();
        chanel.setKey("chanel");
        chanel.setValue(channel);

        KeyValuePair isSMS = new KeyValuePair();
        chanel.setKey("issms");
        chanel.setValue("0");

        com.utiba.delirium.ws.misc.KeyValuePairMap kvpm = new com.utiba.delirium.ws.misc.KeyValuePairMap();
        kvpm.getKeyValuePairs().add(recipient);
        kvpm.getKeyValuePairs().add(recipient_name);
        kvpm.getKeyValuePairs().add(client);
        kvpm.getKeyValuePairs().add(chanel);
        kvpm.getKeyValuePairs().add(isSMS);

        if (!transfer.getMessage().equals(""))
            kvpm.getKeyValuePairs().add(message);
        kvpm.getKeyValuePairs().add(recipient_id);

        com.utiba.delirium.ws.misc.KeyValuePair transType = new com.utiba.delirium.ws.misc.KeyValuePair();
        transType.setKey("c2ctype");
        transType.setValue("m2c");
        kvpm.getKeyValuePairs().add(transType);

        transferRequestType.setExtraTransData(kvpm);
        transferRequestType.setSessionid(sessionInfo.getCsrt().getSessionid());
        transferRequestType.setAmount(transfer.getAmount());
        transferRequestType.setTo(tarGet);
        transferRequestType.setType(1);

        //transferRequestType.setType(walletType);
        StandardBizResponse standardBizResponse = stub.transfer(transferRequestType);

        logger.debug("---transferM2c response");
        logger.debug("---transid: " + standardBizResponse.getTransid());
        logger.debug("---resultcode: " + standardBizResponse.getResult());
        return standardBizResponse;
    }*/

        public StandardMSResponse billpay(Vertx _vertx,String agentReference
                ,String mpin
                ,String providerId
                ,String billId
                ,long amount
                ,int walletType
                ,String channel
                ,List<SoapProto.keyValuePair> listKeyValuePairs
                ,Common.BuildLog log) {
            if(stub == null)
                callSoap();

            log.add("billpay","");
            log.add("pinlen",mpin.length());
            log.add("providerid",providerId);
            log.add("billid",billId);
            log.add("amount",amount);
            log.add("wallettype",walletType);
            log.add("channel",channel);

            StandardMSResponse response = new StandardMSResponse();
            SessionInfo sessionInfo = SessionManagerPlus.getCsrt(_vertx,agentReference, mpin,log.getTime());
            int loginResultCode = sessionInfo.getLoginResult();

            log.add("login result",loginResultCode);

            CreatesessionResponseType csrt = null;
            if(loginResultCode == 0){
                csrt = sessionInfo.getCsrt();
            }else{
                response.setResultCode(ErrorMapping.getResultCode(loginResultCode));
                response.setDescription(ErrorMapping.getDescriptionError(loginResultCode));
                return response;
            }

            if(csrt == null) {
                log.add("csrt","null");
                return null;
            }

            StandardBizResponse standardBizResponse = null;

            //Thanh toan voi so tien tuong ung
            BillpayRequestType billpayRequestType = new BillpayRequestType();
            billpayRequestType.setSessionid(csrt.getSessionid());
            if(amount == 30335568)
                billpayRequestType.setAmount(new BigDecimal(30335569));
            else
                billpayRequestType.setAmount(new BigDecimal(amount));

            billpayRequestType.setType(walletType);//1 for e-wallet, 2 for stock

            String targetAgent = pay_cfg.getString("billerid." + providerId.toLowerCase().trim() + ".target", "");

            if(targetAgent.equalsIgnoreCase("")){
                targetAgent = providerId;
            }

            if(targetAgent.equals(""))
                targetAgent = providerId;
            billpayRequestType.setTarget(targetAgent);

            log.add("targetAgent",targetAgent);

            billpayRequestType.setReference1("09179999");
            billpayRequestType.setReference2("09189999");

            com.utiba.delirium.ws.misc.KeyValuePair account = new com.utiba.delirium.ws.misc.KeyValuePair();
            account.setKey("account");
            account.setValue(billId);

            com.utiba.delirium.ws.misc.KeyValuePair biller = new com.utiba.delirium.ws.misc.KeyValuePair();
            biller.setKey("billerid");
            biller.setValue(providerId);

            com.utiba.delirium.ws.misc.KeyValuePair _target = new com.utiba.delirium.ws.misc.KeyValuePair();
            _target.setKey("__target");
            _target.setValue(providerId);

            com.utiba.delirium.ws.misc.KeyValuePair client = new com.utiba.delirium.ws.misc.KeyValuePair();
            client.setKey("client");
            client.setValue("backend");

            com.utiba.delirium.ws.misc.KeyValuePair chanel = new com.utiba.delirium.ws.misc.KeyValuePair();
            chanel.setKey("chanel");
            chanel.setValue(channel);

            com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
            kvIsSMS.setKey("issms");
            //kvIsSMS.setValue("0");
            kvIsSMS.setValue("no");

            com.utiba.delirium.ws.misc.KeyValuePairMap value = new com.utiba.delirium.ws.misc.KeyValuePairMap();

            setKeyValuePairForCommand(value,listKeyValuePairs,log);

            value.getKeyValuePairs().add(account);
            value.getKeyValuePairs().add(biller);
            value.getKeyValuePairs().add(_target);
            value.getKeyValuePairs().add(client);
            value.getKeyValuePairs().add(chanel);
            value.getKeyValuePairs().add(kvIsSMS);

            billpayRequestType.setExtraTransData(value);

            standardBizResponse = stub.billpay(billpayRequestType);

            int tranId = -100;
            int rcode = -100;
            String desc = "";
            if(standardBizResponse != null && standardBizResponse.getTransid() != null){
                tranId = standardBizResponse.getTransid();
                rcode = standardBizResponse.getResult();
            }else{
                if(standardBizResponse == null){
                    desc ="standardBizResponse == null";
                }else if(standardBizResponse.getTransid() == null){
                    desc = "standardBizResponse.getTransid() == null";
                }else{
                    desc="Khong xac dinh duoc ket qua tra ve tu core";
                }
            }

            log.add("tranid",tranId);
            log.add("rcode",rcode);
            log.add("desc", desc);
            log.add("errordesc", SoapError.getDesc(rcode));

            if(standardBizResponse != null && standardBizResponse.getTransid() != null) {
                response.setTransID(standardBizResponse.getTransid().toString());
                response.setResultCode(standardBizResponse.getResult());
                response.setDebitAmount(0);

            }else if(standardBizResponse != null && standardBizResponse.getTransid() == null) {
                return null;
            }else{
                return null;
            }

            return response;
        }

        public StandardMSResponse getBillInfo(String number, String providerId, String billId) {
            if (stub == null)
                callSoap();

            Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber(number);
            log.add("function", "getBillInfo");
            log.add("providerid", providerId);
            log.add("billid", billId);

            StandardMSResponse response = new StandardMSResponse();

            String urlToCheck = biller_cfg.getString("billerid." + providerId.toLowerCase().trim() + ".checkdebit.url", "");
            log.add("urlToCheck","".equalsIgnoreCase(urlToCheck) ? "urlToCheck not found" : urlToCheck);

            if (urlToCheck == null || urlToCheck.equals(""))//Ma dich vu tuong ung ko cho phep check debit amount
            {
                //todo quy dinh lai ma loi cho dung
                response.setResultCode(24);//invalid amount
                response.setDebitAmount(0);
                response.setTransID(System.currentTimeMillis() / 100000 + new Random().nextInt(100000) + "");
            } else //Cho phep check so tien no cua hoa don nay
            {
                //Call webservice on SOAP 26.12, 26.20
                UMarketServiceProviderLocator Ulocator = new UMarketServiceProviderLocator();
                URL portAddress;
                PayBillRequest pbRequest = new PayBillRequest();
                PayBillResponse pbResponse = null;
                try {
                    //set values
                    pbRequest.setAmount(new BigDecimal(1001));
                    long transid = System.currentTimeMillis() / 100000 + new Random().nextInt(100000);
                    log.add("check debit, MS-Transid", transid);
                    pbRequest.setTransactionId(transid);
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvpRequest[] = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair[8];

                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp0 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp1 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp2 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp3 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp4 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp5 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp6 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp7 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();

                    kvp0.setKey("type");
                    kvp0.setValue("billpay");
                    kvpRequest[0] = kvp0;

                    kvp1.setKey("account");
                    kvp1.setValue(billId.trim());
                    kvpRequest[1] = kvp1;

                    kvp2.setKey("notify");
                    kvp2.setValue("notify");
                    kvpRequest[2] = kvp2;

                    kvp3.setKey("initiator");
                    kvp3.setValue(number);
                    kvpRequest[3] = kvp3;

                    kvp4.setKey("debitamount");
                    kvp4.setValue("0");
                    kvpRequest[4] = kvp4;

                /*kvp5.setKey("billerid");
                kvp5.setValue(providerId.trim());
                kvpRequest[5] = kvp5;*/

                    //theo yeu cau cua core connector
                    kvp5.setKey("client");
                    kvp5.setValue("backend");
                    kvpRequest[5] = kvp5;


                    //don't remove kpv6, be careful
                    kvp6.setKey("billerid");
                    kvp6.setValue(providerId.trim());
                    kvpRequest[6] = kvp6;

                    kvp7.setKey("billerid");
                    kvp7.setValue(providerId.trim());
                    kvpRequest[7] = kvp7;

                    pbRequest.setExtraParameters(kvpRequest);

                    //send request
                    log.add("begin call WS",urlToCheck);

                    portAddress = new URL(urlToCheck);
                    BillingService services = Ulocator.getBillingServiceProviderSOAP(portAddress);
                    pbResponse = services.payBill(pbRequest);
                    pbResponse.setServiceTransactionId(transid + "");

                    log.add("end call WS",urlToCheck);

                    if(pbResponse.getExtraParameters() == null){
                        return null;
                    }

                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair[] kvpResponse = pbResponse.getExtraParameters();

                    log.add("result code",pbResponse.getResultCode());

                    if(pbResponse.getResultCode() == com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ResultCode.AccountNotFound){
                        response.setResultCode(13);
                        log.add("coreresult","account not found");
                    }else if(pbResponse.getResultCode() == com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ResultCode.BillpayAccountInvalid){
                        log.add("coreresult","billpay account invalid");
                        response.setResultCode(0);
                    }else{
                        response.setResultCode(1006);
                    }

                    log.add("kvp[4]",kvpResponse[4].getValue());
                    log.add("kvpResponse len",kvpResponse.length);
                    for (int i=0; i<kvpResponse.length; i++)
                        log.add(kvpResponse[i].getKey(),kvpResponse[i].getValue());

                    long debitAmount = DataUtil.stringToUNumber(kvpResponse[4].getValue());
                    response.setDebitAmount(debitAmount);
                    String billInfo = "";

                    //neu adsl viettel / -- > lay o debitamout
                    if("aviettel".equalsIgnoreCase(providerId)){

                        //viettel gives us only  total amount when we call check debit for an account
                        billInfo = ",," + "~"
                                +billId +"," + kvpResponse[4].getValue() +",,~";

                        //nuoc cho lon / anvien --> lam theo kieu moi
                    }else if("nuochcm".equalsIgnoreCase(providerId) || "avg".equalsIgnoreCase(providerId) ){

                        Map<String,String> mapResponse = DataUtil.convertKVPArrToMap(kvpResponse);

                        billInfo = "";

                        if (mapResponse.containsKey("cusName")) {
                            billInfo += mapResponse.get("cusName");
                        }

                        billInfo += ",," + "~";

                        int numBill = 0;
                        if (mapResponse.get("cusBillCount") != null && !mapResponse.get("cusBillCount").isEmpty()) {
                            numBill = DataUtil.strToInt(mapResponse.get("cusBillCount"));
                        }

                        for (int i=0; i<numBill; i++){
                            if (mapResponse.containsKey("cusBill"+(i+1))) {
                                String temp = mapResponse.get("cusBill" + (i + 1));
                                String[] listTemp = temp.split("#");
                                billInfo += listTemp[0] + ",";
                                billInfo += listTemp[1] + ",";
                                if (listTemp[2] != null && !"".equalsIgnoreCase(listTemp[2])){
                                    billInfo += listTemp[2] + ",";
                                    billInfo += listTemp[2];
                                } else {
                                    billInfo += listTemp[3] + ",";
                                    billInfo += listTemp[4];
                                }
                                if (i < (numBill - 1)){
                                    billInfo += "#";
                                }
                            }
                        }

                        billInfo += "~";
                        if (mapResponse.containsKey("cusInfo"))
                            billInfo += mapResponse.get("cusInfo");

                        //chay theo cac luong cu dang chay binh thuong
                    }else {

                        //danh cho cac bill lay thong tin tu description
                        if(kvpResponse.length>=8
                                && kvpResponse[7].getValue()!= null
                                && !kvpResponse[7].getValue().equalsIgnoreCase("")){
                            billInfo = kvpResponse[7].getValue();

                            //eo biet tai sao lai co billpay|, truoc day thi khong co ---??
                            if(billInfo.startsWith("billpay|")){
                                billInfo = billInfo.substring("billpay|".length());
                            }

                            if(billInfo.startsWith("paybill|")){
                                billInfo = billInfo.substring("paybill|".length());
                            }

                        }else{
                            //cac bill con lai lay tu kvp 4
                            billInfo = ",," + "~"
                                    +billId +"," + kvpResponse[4].getValue() +",,~";

                    /*
                    result=thông tin khách hàng~thong tin hóa đơn 1#thông tin hóa đơn 2~thong tin thêm.
                    3 trường phân cách nhau bằng dấu ~
                    Nếu kh có nhiều hóa đơn thì sẽ phân cách bằng dấu #.
                    thông tin khách hàng=họ tên,địa chỉ,số đt
                    thông tin hóa đơn=mã hóa đơn,số tiền,từ ngày,đến ngày.
                    thông tin thêm=hiện tại là rỗng.
                    */
                        }
                    }

                    log.add("billInfo",billInfo);
                    response.setResultName(billInfo);
                    log.add("response transid", pbResponse.getServiceTransactionId());
                    response.setTransID(pbResponse.getServiceTransactionId());

                }catch (RemoteException re){
                    log.add("loi", "goi " + urlToCheck + " khong truy xuat duoc dich vu check cuoc");
                }
                catch (Exception e) {
                    log.add("loi", "goi " + urlToCheck + " check cuoc khong thanh cong vi loi");
                }
            }

            log.writeLog();
            return response;
        }

        /***
         * lay thong tin bill theo huong dich vu
         * @param number
         * @param providerId
         * @param billId
         * @return
         */

        public JsonObject getBillInfoByService(String number
                ,String providerId
                ,String billId)
        {
            if (stub == null)
                callSoap();

            Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber(number);
            log.add("function", "getBillInfoByService");
            log.add("providerid", providerId);
            log.add("billid", billId);

            StandardMSResponse response = new StandardMSResponse();

            String urlToCheck = biller_cfg.getString("billerid." + providerId.toLowerCase().trim() + ".checkdebit.url", "");
            log.add("urlToCheck","".equalsIgnoreCase(urlToCheck) ? "urlToCheck not found" : urlToCheck);

            BillInfoService billInfoService = new BillInfoService();

            if (urlToCheck == null || "".equalsIgnoreCase(urlToCheck))//Ma dich vu tuong ung ko cho phep check debit amount
            {
                //todo quy dinh lai ma loi cho dung
                response.setResultCode(24);//invalid amount
                response.setDebitAmount(0);
                response.setTransID(System.currentTimeMillis() / 100000 + new Random().nextInt(100000) + "");
            } else //Cho phep check so tien no cua hoa don nay
            {
                //Call webservice on SOAP 26.12, 26.20
                UMarketServiceProviderLocator Ulocator = new UMarketServiceProviderLocator();
                URL portAddress;
                PayBillRequest pbRequest = new PayBillRequest();
                PayBillResponse pbResponse = null;
                try {
                    //set values
                    pbRequest.setAmount(new BigDecimal(1001));
                    long transid = System.currentTimeMillis() / 100000 + new Random().nextInt(100000);
                    log.add("check debit, MS-Transid", transid);
                    pbRequest.setTransactionId(transid);
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvpRequest[] = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair[8];

                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp0 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp1 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp2 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp3 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp4 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp5 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp6 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();
                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair kvp7 = new com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair();

                    kvp0.setKey("type");
                    kvp0.setValue("billpay");
                    kvpRequest[0] = kvp0;

                    kvp1.setKey("account");
                    kvp1.setValue(billId.trim());
                    kvpRequest[1] = kvp1;

                    kvp2.setKey("notify");
                    kvp2.setValue("notify");
                    kvpRequest[2] = kvp2;

                    kvp3.setKey("initiator");
                    kvp3.setValue(number);
                    kvpRequest[3] = kvp3;

                    kvp4.setKey("debitamount");
                    kvp4.setValue("0");
                    kvpRequest[4] = kvp4;

                /*kvp5.setKey("billerid");
                kvp5.setValue(providerId.trim());
                kvpRequest[5] = kvp5;*/

                    //theo yeu cau cua core connector
                    kvp5.setKey("client");
                    kvp5.setValue("backend");
                    kvpRequest[5] = kvp5;

                    //don't remove kpv6, be careful
                    kvp6.setKey("billerid");
                    kvp6.setValue(providerId.trim());
                    kvpRequest[6] = kvp6;

                    kvp7.setKey("billerid");
                    kvp7.setValue(providerId.trim());
                    kvpRequest[7] = kvp7;

                    pbRequest.setExtraParameters(kvpRequest);

                    //send request
                    log.add("begin call WS",urlToCheck);

                    portAddress = new URL(urlToCheck);
                    BillingService services = Ulocator.getBillingServiceProviderSOAP(portAddress);
                    pbResponse = services.payBill(pbRequest);
                    pbResponse.setServiceTransactionId(transid + "");

                    log.add("end call WS",urlToCheck);

                    log.add("resultCode", (pbResponse !=null &&  pbResponse.getResultCode() !=null) ?  pbResponse.getResultCode().getValue() : "null");

                    if(pbResponse.getExtraParameters() == null){
                        log.add("pbResponse.getExtraParameters()", "null");
                        log.writeLog();
                        return null;
                    }

                    com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.KeyValuePair[] kvpResponse = pbResponse.getExtraParameters();

                    log.add("result code",pbResponse.getResultCode());

                    if(pbResponse.getResultCode() == com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ResultCode.AccountNotFound){
                        log.add("coreresult", "account not found");
                        response.setResultCode(13);
                    }else if(pbResponse.getResultCode() == com.mservice.momo.gateway.internal.soapin.v1.UMARKETSPIWS.ResultCode.BillpayAccountInvalid){
                        log.add("coreresult", "billpay account invalid");
                        response.setResultCode(0);
                    }else{
                        response.setResultCode(1006);
                    }

                    log.add("kvpResponse len",kvpResponse.length);
                    long debitAmount = 0;

                    // parse new format
                    //log response

                    log.add("keyvaluepair from core connector -----------------------","");
                    for (int i=0; i<kvpResponse.length; i++){
                        log.add(kvpResponse[i].getKey(),kvpResponse[i].getValue());
                    }

                    log.add("---------------------------------------------------------","");
                    //todo backend lam tu cho nay tro di
                    //billinfo -> Nguyen Ngoc Tu,,~UHNA0002719,187000,31/07/2014,31/07/2014~
                    Map<String,String> mapRes = new HashMap<>();
                    Map<String,String> tempMapRes = DataUtil.convertKVPArrToMap(kvpResponse);

                    //theo chuan moi

                /*debitAmount: dùng khi kiểm tra số dư
                serviceCode:  lưu thông tin của đối tác
                partnerResCode: mã trả về của đối tác
                partnerTransId: transaction của đối tác
                */

                /*
                Phan Lap Phuong: 12124787-7-67045#$67045 mot ky
                [16:06:10] Phan Lap Phuong: khi thanh toan 12124787#$67045
                [16:06:41] Phan Lap Phuong: khong co no ,,~0*/

                /*billpay|Tran Thi Huong Huong,bao nhi đong so 12
                ho xuan huong - Nguyen Du - Hai Ba Trung - Ha Noi,0915433268~,0,082014,082014~*/


                    if ("vinahcm".equalsIgnoreCase(kvp6.getValue())) {
                        String value = kvpResponse[7].getValue();
                        log.add("kvp7", value);
                        value = value.replace("billpay|","");
                        //{"agentName":"Tran Thi Huong Huong",
                        // "address":"bao nhi đong so 12 ho xuan huong - Nguyen Du - Hai Ba Trung - Ha Noi",
                        // "phoneNumber":"0915433268",
                        // "bills":[{"billId":"","amount":0,"startDate":"082014","endDate":"082014"}]}

                        JsonObject json = billInfoToJsonObject(value);

                        mapRes.put("cusName", json.getString("agentName", ""));
                        //dinh dang cusInfo : SO DIEN THOAI#SERVICE NAME#DIA CHI
                        mapRes.put("cusInfo", json.getString("phoneNumber", "") + "##" + json.getString("address", ""));
                        mapRes.put("debitAmount", tempMapRes.get("debitamount"));
                    }

                    // core tra ra ket qua theo cach cu
                    if ("nuochcm".equalsIgnoreCase(providerId)
                            && tempMapRes.containsKey("billInfo")
                            && tempMapRes.get("billInfo") != null
                            && tempMapRes.get("billInfo").indexOf(",,~0")<0
                            ){
                        log.add("core connector tra ve nuochcm theo chuan tao lao", "");
                        //12124787-7-67045#$67045
                        String[] arr = tempMapRes.get("billInfo").split("$");
                        if(arr.length >1){
                            mapRes.put("debitAmount",arr[arr.length -1].trim());
                        }
                        log.add("co debitamout", mapRes.get("debitAmount"));
                        //chi tiet tung ky hoa don
                        String[] billInfos = ((arr.length > 0) ? arr[0].split("#") : new String[0]);

                        int count = 0;

                        for (int i=0; i<billInfos.length; i++){
                            String[] billInfo = billInfos[i].split("-");
                            if (billInfo.length>2) {
                                mapRes.put("cusBill" + (i + 1), billInfo[0] + "#"
                                        + billInfo[2] + "#"
                                        + billInfo[1] + "##");
                                count++;
                            }
                        }

                        mapRes.put("cusBillCount",count+"");
                        log.add("co cusBillCount", mapRes.get("cusBillCount"));

                    }else if ("avg".equalsIgnoreCase(providerId) && tempMapRes.containsKey("billInfo") ){

                        //DTH Như ý@27/4/2022@198000_93~396000_186~792000_372~1188000_558~1584000_744~
                        if (tempMapRes.containsKey("billInfo")) {
                            String[] infos = tempMapRes.get("billInfo").split("@");
                            if (infos.length>0) {
                                String tempSN = "#" + infos[0] + "#";
                                mapRes.put("cusInfo", tempSN);
                            }
                            if (infos.length>2){
                                String[] billInfos = infos[2].split("~");

                                int count = 0;

                                for (int i=0; i<billInfos.length; i++){
                                    String[] billInfo = billInfos[i].split("_");
                                    if (billInfo.length>1) {
                                        mapRes.put("cusBill" + (i + 1), billInfo[1] + "#" + billInfo[0] + "###");
                                        count++;
                                    }
                                }

                                mapRes.put("cusBillCount",count+"");
                            }
                        }
                        log.add("core connector tra ve theo chuan tao lao", "");
                    } else if (tempMapRes.containsKey("serviceCode")
                            && tempMapRes.containsKey("partnerResCode")
                            && tempMapRes.containsKey("partnerTransId")){

                        mapRes = tempMapRes;
                        log.add("core connector tra ve theo chuan moi", "");

                    } else {

                        log.add("core connector tra ve theo chuan cu", "");

                        if (tempMapRes.containsKey("debitamount")){
                            mapRes.put("debitAmount",tempMapRes.get("debitamount"));
                        }
                        log.add("debitamout", mapRes.get("debitAmount"));

                        //cac loai hoa don con lai -->
                        //TRAN HUU KY,5 AP 4 PHAM HUU LAU,-1~~
                        //tien xu ly truoc, connector luc tra ve billinfo luc billInfo, haizzzzz
                        if (tempMapRes.get("billinfo") != null
                                && !tempMapRes.get("billinfo").isEmpty()){
                            tempMapRes.put("billInfo",tempMapRes.get("billinfo"));
                        }


                        if (tempMapRes.get("billInfo") != null
                                && !tempMapRes.get("billInfo").isEmpty()){
                            String[] infos = tempMapRes.get("billInfo").split("~");

                            if (infos.length>0) {
                                String[] cusInfo = infos[0].split(",");
                                if (cusInfo.length>0 && !cusInfo[0].isEmpty()) {
                                    mapRes.put("cusName", cusInfo[0]);
                                }
                                String temp = "";
                                if (cusInfo.length>2 && !cusInfo[2].isEmpty()) {
                                    temp += cusInfo[2];
                                }
                                temp += "##";

                                if (cusInfo.length>1 && !cusInfo[1].isEmpty()) {

                                    temp += (cusInfo[1].indexOf("-1")>=0 ? "" : cusInfo[1]);
                                }
                                mapRes.put("cusInfo", temp);
                            }

                            String[] billInfos = (infos.length >=2 ? infos[1].split("#"): new String[0]) ;

                            int count = 0;

                            for (int i=0; i<billInfos.length; i++){
                                String[] billInfo = billInfos[i].split(",");

                                if (billInfo.length>3) {
                                    mapRes.put("cusBill" + (i + 1), billInfo[0]
                                                    + "#"
                                                    + billInfo[1]
                                                    + "##"
                                                    + billInfo[2]
                                                    + "#"
                                                    + billInfo[3]
                                    );
                                    count++;
                                }
                            }

                            mapRes.put("cusBillCount",count+"");
                            log.add("cusBillCount", mapRes.get("cusBillCount"));
                        }
                    }

                    log.add("keyvaluepair after refined by backend -----------------------","");
                    for (Map.Entry<String, String> entry : mapRes.entrySet()) {
                        log.add(entry.getKey(), entry.getValue());
                    }

                    log.add("--------------------------------------------------------------","");

                    if (mapRes.containsKey("debitAmount")){
                        debitAmount = DataUtil.stringToUNumber(mapRes.get("debitAmount"));
                    }else if(mapRes.containsKey("debitamount")){
                        debitAmount = DataUtil.stringToUNumber(mapRes.get("debitamount"));
                    }

                    response.setDebitAmount(debitAmount);
                    billInfoService.total_amount = (debitAmount <=0 ? 0 : debitAmount);

                    BillInfoService.TextValue cusInfo;

                    if (mapRes.containsKey("cusName")) {
                        cusInfo = new BillInfoService.TextValue();
                        cusInfo.text = "ht";
                        cusInfo.value = mapRes.get("cusName");
                        if (cusInfo.value!=null && !"".equalsIgnoreCase(cusInfo.value))
                            billInfoService.addCustomInfo(cusInfo);
                    }

                    //dinh dang cusInfo : SO DIEN THOAI#SERVICE NAME#DIA CHI

                    if (mapRes.containsKey("cusInfo")) {
                        String tempCusInfo = mapRes.get("cusInfo");
                        String[] listTempCusInfo = tempCusInfo.split("#");

                        //lay so dien thoai
                        if(listTempCusInfo != null
                                && (listTempCusInfo.length >=1)
                                && (!listTempCusInfo[0].equalsIgnoreCase(""))){

                            //neu co so dien thoai
                            if(DataUtil.strToInt(listTempCusInfo[0]) > 0){
                                cusInfo = new BillInfoService.TextValue();
                                cusInfo.text = "sdt";
                                cusInfo.value = listTempCusInfo[0];
                                billInfoService.addCustomInfo(cusInfo);
                            }

                        }

                        //lay goi dia chi
                        if((listTempCusInfo != null)
                                && (listTempCusInfo.length>=3)
                                && (!listTempCusInfo[2].equalsIgnoreCase(""))){
                            cusInfo = new BillInfoService.TextValue();
                            cusInfo.text = "dc";
                            cusInfo.value = listTempCusInfo[2];
                            billInfoService.addCustomInfo(cusInfo);
                        }

                        //lay goi dich vu
                        if((listTempCusInfo != null)
                                && (listTempCusInfo.length>=2)
                                && (!listTempCusInfo[1].equalsIgnoreCase(""))){
                            cusInfo = new BillInfoService.TextValue();
                            cusInfo.text = "sn";
                            cusInfo.value = listTempCusInfo[1];
                            billInfoService.addCustomInfo(cusInfo);
                        }
                    }

                    //lay danh sach thong tin phu neu co
                    //vi du danh sach bill chi tiet
                    BillInfoService.ExtraInfo extraInfo;
                    BillInfoService.TextValue textValue;

                    if (mapRes.containsKey("cusBillCount")){

                        int numBill = DataUtil.strToInt(mapRes.get("cusBillCount"));

                        for (int i=0; i<numBill; i++){

                            if (mapRes.containsKey("cusBill"+(i+1))) {

                                String temp = mapRes.get("cusBill" + (i + 1));
                                String[] listTemp = temp.split("#");

                                String agvFormat = "%s tháng - trị giá %sđ";

                                if ("avg".equalsIgnoreCase(providerId)){
                                    textValue = new BillInfoService.TextValue();

                                    String tmp =listTemp[0];
                                    String[] arTmp = tmp.split("\\.");
                                    tmp = arTmp[0].trim();

                                    textValue.text = String.format(agvFormat
                                            ,DataUtil.strToInt(tmp) /30
                                            , Misc.formatAmount(DataUtil.stringToUNumber(listTemp[1])).replace(",","."))  ;
                                    textValue.value = listTemp[1];

                                    billInfoService.addPrice(textValue);
                                } else {
                                    extraInfo = new BillInfoService.ExtraInfo();
                                    extraInfo.bill_detail_id = (listTemp.length >= 1 ? listTemp[0] : "");
                                    extraInfo.amount = (listTemp.length >= 2 ? listTemp[1] : "");

                                    //lay theo ky thanh toan
                                    if (listTemp[2] != null && !"".equalsIgnoreCase(listTemp[2])) {
                                        extraInfo.from_date = "Kỳ " + listTemp[2];
                                        extraInfo.to_date = "Kỳ " + listTemp[2];
                                    } else {

                                        //lay theo ngay thanh toan
                                        extraInfo.from_date = listTemp[3];
                                        extraInfo.to_date = listTemp[4];
                                    }

                                    billInfoService.addExtraInfo(extraInfo);
                                }
                            }
                        }
                    }

                    log.add("billInfo",billInfoService.toJsonObject());

                    response.setResultName(billInfoService.toJsonObject().toString());
                    log.add("response transid", pbResponse.getServiceTransactionId());

                    response.setTransID(pbResponse.getServiceTransactionId());

                }catch (RemoteException re){
                    log.add("loi", "goi " + urlToCheck + " khong the truy cap check cuoc");
                }

                catch (Exception e) {
                    log.add("loi","goi " + urlToCheck + " check cuoc gap loi");
                }
            }

            log.writeLog();

            JsonObject jo = new JsonObject();
            jo.putNumber("rcode", response.getResultCode());
            jo.putObject("json_result",billInfoService.toJsonObject());

            return  jo;
        }

        /**
         * result json fields:
         * {
         * agentName : "",
         * address : "",
         * phoneNumber : "",
         * bills : [
         * {billId : "",
         * amount : "",
         * startDate : "",
         * endDate : "",
         * }, ..
         * ]
         * <p/>
         * }
         *
         * @param info
         * @return
         */
        private static JsonObject billInfoToJsonObject(String info) {
            //NGUYEN MINH KHUE,TO 42 KP2 Phuong Tan Chanh Hiep Quan 12,-1~~
            //TRAN THI KHANH NINH,411/4 LE DUC THO,0989033099~141010,1125889,2014-08-22 2014-09-21#~
            JsonObject obj = new JsonObject();
            JsonArray bills = new JsonArray();
            int i, j;

            try {

                String part[] = info.split("~");

                i = part[0].indexOf(",");
                String agentName = part[0].substring(0, i);

                j = part[0].indexOf(",", i + 1);
                String address = part[0].substring(i + 1, j);
                i = j;

                String phoneNumber = part[0].substring(i + 1, part[0].length());

                if (part.length > 1) {

                    String billParts[] = part[1].split("#");
                    for (String bill : billParts) {

                        JsonObject billJson = new JsonObject();

                        i = bill.indexOf(",");
                        String billId = bill.substring(0, i);

                        j = bill.indexOf(",", i + 1);
                        long amount = 0;
                        try {
                            amount = Long.parseLong(bill.substring(i + 1, j));
                        } catch (NumberFormatException e) {
                        }
                        i = j;

                        billJson.putNumber("amount", amount).putString("billId", billId);

                        try {

                            j = bill.indexOf(",", i + 1);
                            String startDate = "";
                            startDate = bill.substring(i + 1, j);
                            i = j;

                            String endDay = "";
                            endDay = bill.substring(i + 1, bill.length());

                            JsonObject b = new JsonObject();
                            billJson.putString("startDate", startDate).putString("endDate", endDay);

                        } catch (Exception e) {

                        }
                        bills.add(billJson);
                    }
                }

                obj.putString("agentName", agentName);
                obj.putString("address", address);
                obj.putString("phoneNumber", phoneNumber);
                obj.putArray("bills", bills);
//        System.out.println(agentName);
//        System.out.println(address);
//        System.out.println(phoneNumber);
            } catch (Exception e) {

            }
            return obj;
        }

        public AgentResponseType getAgentStatus(String agentReference) {
            if (stub == null)
                callSoap();

            Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber(agentReference);
            log.add("function","getAgentStatus");

            CreatesessionResponseType csrt = SessionManagerPlus.getCsrt(log.getTime());
            if(csrt == null){
                log.add("csrt","null");
            }
            log.writeLog();

            GetAgentByReferenceRequest agentByReferenceRequest = new GetAgentByReferenceRequest();
            agentByReferenceRequest.setReference(agentReference);//Phone number
            agentByReferenceRequest.setSessionid(csrt.getSessionid());
            AgentResponseType agentResponseType = stub.getAgentByReference(agentByReferenceRequest);

            return  agentResponseType;
        }

        public void setGroupCapset(String agent, String[] arr_group, String[] arr_capset, long upperLimit){

            //map group >> MoMo:
            StandardBizResponse groupResponse = new StandardBizResponse();
            for (int i = 0; i < arr_group.length; i++) {
                long groupId = 19112;
                groupId = DataUtil.stringToUNumber(arr_group[i]);
                groupResponse = mapAgentToGroup("88888888"
                        ,agent
                        ,groupId);

                logger.info(agent + " MAP GROUP AT FIRST TIME RESULT " + groupResponse.getResult());
            }

            //map capset
            StandardBizResponse capsetResponse = new StandardBizResponse();
            for (int i = 0; i < arr_capset.length; i++) {
                long capsetId = 6;
                capsetId = DataUtil.stringToUNumber(arr_capset[i]);
                StandardBizResponse mapAgentResp = mapAgentToCapset("88888888"
                        ,agent
                        ,capsetId
                        ,1
                        ,upperLimit);
                logger.info(agent + " MAP CAPSET AT FIRST TIME RESULT " + mapAgentResp.getResult());
            }
        }

        //todo whats if one of function return error ??????????????????????????????
        public boolean doRegisterMS(RegisterInfo obj
                ,String[] arr_group
                ,String[] arr_capset
                ,String upperLimit
                ,String desc
                ,String channel
                ,HashMap<String,String> kvps
                ,Common.BuildLog log) {


            log.add("function","doRegisterMS");
            log.add("is register new", obj.isRegisterNew());

            StandardBizResponse bizResponse;

            //lam theo luong cu
            if (obj.isRegisterNew()) {

                bizResponse = registerNewAgent(obj.getPhone()
                        , obj.getNewPin()
                        , obj.getName()
                        , obj.getDateOfBirth()
                        , obj.getProvince()
                        , obj.getPersional_id()
                        , ""
                        , obj.getEmail()
                        , channel
                        , kvps
                        , log);
            } else {
                bizResponse = modifyAgentInfo(obj.getPhone()
                        ,true
                        ,true
                        ,obj.getName()
                        ,obj.getDateOfBirth()
                        ,obj.getProvince()
                        ,obj.getPersional_id()
                        ,""
                        ,obj.getEmail()
                        ,channel,null,log);
            }

            if (bizResponse != null) {
                logger.info(bizResponse.getTransid() + ", Register result: " + bizResponse.getResult());
                log.add("result",bizResponse.getResult());

                //neu la dang ky moi --> thi map group lai cho no
                if(obj.isRegisterNew() ){
                    if(upperLimit == null){
                        upperLimit = "5000000";
                    }
                    setGroupCapset(obj.getPhone(),arr_group,arr_capset,DataUtil.stringToUNumber(upperLimit));
                }

                StandardBizResponse resetPinResp = resetPIN(obj.getPhone(), obj.getNewPin());
                logger.debug(obj.getPhone() + " RESET PIN AT FIRST TIME RESULT " + resetPinResp.getResult());
                log.add("reset pin result",resetPinResp.getResult());
            }else{
                log.add("StandardBizResponse","null");
            }

            boolean result = true;
            if(bizResponse ==null){
                result =false;
            }
            return result;
        }

        public StandardBizResponse registerNewAgent(String agentReference
                ,String pin
                ,String agentName
                ,String dateOfBirth
                ,String address
                ,String personalId
                ,String contact
                ,String email
                ,String channel
                ,HashMap<String,String> kvps
                ,Common.BuildLog log) {

            if (stub == null)
                callSoap();
            log.add("function","registerNewAgent");
            log.add("function","modifyAgentInfo");
            log.add("name",agentName);
            log.add("dateOfBirth",dateOfBirth);
            log.add("address",address);
            log.add("personalId",personalId);
            log.add("contact",contact);
            log.add("email",email);
            log.add("channel",channel);


            CreatesessionResponseType csrt = SessionManagerPlus.getCsrt(log.getTime());

            if(csrt == null){
                log.add("csrt","null");
            }

            StandardBizResponse biz_response_modify = null;
            umarketscws.RegisterRequestType reg_request = new RegisterRequestType();
            KeyValuePairMap kvpm = new KeyValuePairMap();

            //them cac key value pair

            if(kvps != null && kvps.size()>0){
                Set<Map.Entry<String,String>> set = kvps.entrySet();
                for(Map.Entry<String,String> entry : set){
                    log.add("-----------------------------","");
                    log.add("key",entry.getKey());
                    log.add("value",entry.getValue());

                    umarketscws.KeyValuePair kvpItem = new umarketscws.KeyValuePair();
                    kvpItem.setKey(entry.getKey());
                    kvpItem.setValue(entry.getValue());

                    kvpm.getKeyValuePair().add(kvpItem);
                }
            }

            reg_request.setSessionid(csrt.getSessionid());
            reg_request.setAgent(agentReference);
            reg_request.setName(agentName);
            reg_request.setNewPin("123456");
            reg_request.setEmailAddress(email);

            // truong momo
            umarketscws.KeyValuePair momo = new umarketscws.KeyValuePair();
            momo.setKey("MOMO");
            momo.setValue("ON");

            kvpm.getKeyValuePair().add(momo);

            umarketscws.KeyValuePair client= new umarketscws.KeyValuePair();
            client.setKey("client");
            client.setValue("backend");

            umarketscws.KeyValuePair kvIsSMS = new umarketscws.KeyValuePair();
            kvIsSMS.setKey("issms");
            kvIsSMS.setValue("no");


            umarketscws.KeyValuePair chanel = new umarketscws.KeyValuePair();
            chanel.setKey("chanel");
            chanel.setValue(channel);

            kvpm.getKeyValuePair().add(client);
            kvpm.getKeyValuePair().add(chanel);
            kvpm.getKeyValuePair().add(kvIsSMS);

            umarketscws.KeyValuePair time = new umarketscws.KeyValuePair();
            time.setKey("time");
            time.setValue(System.currentTimeMillis() + "");
            kvpm.getKeyValuePair().add(time);

            //	date_of_birth DD/MM/YYYY
            if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                umarketscws.KeyValuePair kvpdateOfBirth = new umarketscws.KeyValuePair();
                kvpdateOfBirth.setKey("date_of_birth");
                kvpdateOfBirth.setValue(dateOfBirth);
                kvpm.getKeyValuePair().add(kvpdateOfBirth);
            }

            // truong address
            if (address != null && !address.isEmpty()) {
                umarketscws.KeyValuePair kvpAddress = new umarketscws.KeyValuePair();
                kvpAddress.setKey("address");
                kvpAddress.setValue(address);
                kvpm.getKeyValuePair().add(kvpAddress);
            }

            // personal
            if (personalId != null && !personalId.isEmpty()) {
                umarketscws.KeyValuePair kvpPersonalId = new umarketscws.KeyValuePair();
                kvpPersonalId.setKey("personal_id");
                kvpPersonalId.setValue(personalId);
                kvpm.getKeyValuePair().add(kvpPersonalId);
            }
            // contact_no
            if (personalId != null && !personalId.isEmpty()) {
                umarketscws.KeyValuePair kvpContact = new umarketscws.KeyValuePair();
                kvpContact.setKey("contact_no");
                kvpContact.setValue(contact);
                kvpm.getKeyValuePair().add(kvpContact);
            }

            if (kvpm.getKeyValuePair().size() > 0)
                reg_request.setExtraParams(kvpm);

            biz_response_modify = stub.register(reg_request);

            if(biz_response_modify ==null){
                log.add("register result","null");
            }else{
                log.add("register result",biz_response_modify.getResult());
            }

            return biz_response_modify;
        }

        public StandardBizResponse modifyAgentInfo(String agentReference
                ,boolean isRegistered
                ,boolean isActived
                ,String agentName
                ,String dateOfBirth
                ,String address
                ,String personalId
                ,String contact
                ,String email
                ,String channel
                ,List<SoapProto.keyValuePair> keyValuePairList
                ,Common.BuildLog log) {

            /************** modifyAgentInfo AGENTS********************/

            log.add("function","modifyAgentInfo");
            log.add("isReged",isRegistered);
            log.add("isActived",isActived);
            log.add("name",agentName);
            log.add("dateOfBirth",dateOfBirth);
            log.add("address",address);
            log.add("personalId",personalId);
            log.add("contact",contact);
            log.add("email",email);
            log.add("channel",channel);

            StandardBizResponse biz_response_modify = null;
            if (agentReference == null || agentReference.isEmpty()){
                log.add("agentReference","null");
                return null;
            }

            if (stub == null)
                callSoap();

            CreatesessionResponseType csrt = SessionManagerPlus.getCsrt(log.getTime());

            if(csrt == null){
                log.add("csrt","null");
                log.add("login unsuccessed, don't modify angent any more","");
                return null;
            }

            try {
                umarketscws.ModifyRequestType modi_request = new umarketscws.ModifyRequestType();
                modi_request.setSessionid(csrt.getSessionid());
                modi_request.setAgent(agentReference);

                umarketscws.ModifyRequestType.Status status = new umarketscws.ModifyRequestType.Status();
                status.setActive(new Boolean(isActived));
                status.setRegistered(isRegistered);
                modi_request.setStatus(status);
                modi_request.setName(agentName);
                modi_request.setEmailAddress(email);

                KeyValuePairMap kvpm = new KeyValuePairMap();

                setUmarketKeyValuePairForCommand(kvpm,keyValuePairList,log);

                // truong momo
                umarketscws.KeyValuePair momo = new umarketscws.KeyValuePair();
                momo.setKey("MOMO");
                momo.setValue("ON");
                kvpm.getKeyValuePair().add(momo);

                umarketscws.KeyValuePair kvIsSMS = new umarketscws.KeyValuePair();
                kvIsSMS.setKey("issms");
                kvIsSMS.setValue("no");
                kvpm.getKeyValuePair().add(kvIsSMS);

                umarketscws.KeyValuePair client= new umarketscws.KeyValuePair();
                client.setKey("client");
                client.setValue("backend");
                kvpm.getKeyValuePair().add(client);

                umarketscws.KeyValuePair chanel = new umarketscws.KeyValuePair();
                chanel.setKey("chanel");
                chanel.setValue(channel);
                kvpm.getKeyValuePair().add(chanel);

                umarketscws.KeyValuePair time = new umarketscws.KeyValuePair();
                time.setKey("time");
                time.setValue(System.currentTimeMillis() + "");
                kvpm.getKeyValuePair().add(time);

                //	date_of_birth DD/MM/YYYY
                if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                    umarketscws.KeyValuePair kvpdateOfBirth = new umarketscws.KeyValuePair();
                    kvpdateOfBirth.setKey("date_of_birth");
                    kvpdateOfBirth.setValue(dateOfBirth);
                    kvpm.getKeyValuePair().add(kvpdateOfBirth);
                }
                // truong address
                if (address != null && !address.isEmpty()) {
                    umarketscws.KeyValuePair kvpAddress = new umarketscws.KeyValuePair();
                    kvpAddress.setKey("address");
                    kvpAddress.setValue(address);
                    kvpm.getKeyValuePair().add(kvpAddress);
                }

                // personal
                if (personalId != null && !personalId.isEmpty()) {
                    umarketscws.KeyValuePair kvpPersonalId = new umarketscws.KeyValuePair();
                    kvpPersonalId.setKey("personal_id");
                    kvpPersonalId.setValue(personalId);
                    kvpm.getKeyValuePair().add(kvpPersonalId);
                }
                // contact_no
                if (contact != null && !contact.isEmpty()) {
                    umarketscws.KeyValuePair kvpContact = new umarketscws.KeyValuePair();
                    kvpContact.setKey("contact_no");
                    kvpContact.setValue(contact);
                    kvpm.getKeyValuePair().add(kvpContact);
                }

                if (kvpm.getKeyValuePair().size() > 0)
                    modi_request.setExtraParams(kvpm);


                biz_response_modify = stub.modify(modi_request);

                if (biz_response_modify != null) {
                    int result = biz_response_modify.getResult();
                    log.add("Namespace",biz_response_modify.getResultNamespace());

                } else {
                    log.add("function","modifyAgentInfo response modify is null");
                }
            } catch (Exception e) {
                log.add("error",e.getMessage());
            }

            return biz_response_modify;
        }

        public StandardBizResponse modifyAgentInfoExtra(String agentReference
                ,List<SoapProto.keyValuePair> keyValuePairList
                ,Common.BuildLog log) {

            StandardBizResponse biz_response_modify = null;
            if (agentReference == null || agentReference.isEmpty()){
                log.add("agentReference","null");
                return null;
            }

            if (stub == null)
                callSoap();

            CreatesessionResponseType csrt = SessionManagerPlus.getCsrt(log.getTime());

            if(csrt == null){
                log.add("csrt","null");
                log.add("login unsuccessed, don't modify angent any more","");
                return null;
            }

            try {
                umarketscws.ModifyRequestType modi_request = new umarketscws.ModifyRequestType();
                modi_request.setSessionid(csrt.getSessionid());
                modi_request.setAgent(agentReference);

                umarketscws.ModifyRequestType.Status status = new umarketscws.ModifyRequestType.Status();
                status.setActive(new Boolean(true));
                status.setRegistered(true);
                modi_request.setStatus(status);

                KeyValuePairMap kvpm = new KeyValuePairMap();

                setUmarketKeyValuePairForCommand(kvpm,keyValuePairList,log);

                umarketscws.KeyValuePair kvIsSMS = new umarketscws.KeyValuePair();
                kvIsSMS.setKey("issms");
                kvIsSMS.setValue("no");
                kvpm.getKeyValuePair().add(kvIsSMS);

                umarketscws.KeyValuePair client= new umarketscws.KeyValuePair();
                client.setKey("client");
                client.setValue("backend");
                kvpm.getKeyValuePair().add(client);

                umarketscws.KeyValuePair time = new umarketscws.KeyValuePair();
                time.setKey("time");
                time.setValue(System.currentTimeMillis() + "");
                kvpm.getKeyValuePair().add(time);

                if (kvpm.getKeyValuePair().size() > 0){
                    modi_request.setExtraParams(kvpm);
                }

                biz_response_modify = stub.modify(modi_request);

                if (biz_response_modify != null) {
                    log.add("Namespace",biz_response_modify.getResultNamespace());
                } else {
                    log.add("function","modifyAgentInfo response modify is null");
                }
            } catch (Exception e) {
                log.add("error",e.getMessage());
            }

            return biz_response_modify;
        }

        public StandardBizResponse resetPIN(String agentReference, String newPin) {
            if (stub == null)
                callSoap();

            Common.BuildLog log = new Common.BuildLog(logger);
            log.setPhoneNumber(agentReference);
            CreatesessionResponseType csrt = SessionManagerPlus.getCsrt(log.getTime());
            ResetPinRequest ResetPin_ReqType = new ResetPinRequest();
            ResetPinRequestType resetPinRequestType = new ResetPinRequestType();
            resetPinRequestType.setSessionid(csrt.getSessionid());
            resetPinRequestType.setAgent(agentReference);
            resetPinRequestType.setNewPin(newPin);
            resetPinRequestType.setSuppressPinExpiry(true);
            ResetPin_ReqType.setResetPinRequestType(resetPinRequestType);

            //client-backend
            com.utiba.delirium.ws.misc.KeyValuePair client= new com.utiba.delirium.ws.misc.KeyValuePair();
            client.setKey("client");
            client.setValue("backend");

            //kvpm
            com.utiba.delirium.ws.misc.KeyValuePairMap kvpm = new com.utiba.delirium.ws.misc.KeyValuePairMap();
            kvpm.getKeyValuePairs().add(client);

            com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
            kvIsSMS.setKey("issms");
            kvIsSMS.setValue("no");
            kvpm.getKeyValuePairs().add(kvIsSMS);

            //set
            resetPinRequestType.setExtraTransData(kvpm);

            StandardBizResponse bizResponse = stub.resetPin(ResetPin_ReqType).getResetPinReturn();

            log.add("resetPIN response","");
            log.add("transid",bizResponse.getTransid());
            log.add("resultcode",bizResponse.getResult());

            log.writeLog();

            return bizResponse;
        }

    public StandardBizResponse resetPINWithSms(String agentReference, String newPin) {
        if (stub == null)
            callSoap();

        Common.BuildLog log = new Common.BuildLog(logger);
        log.setPhoneNumber(agentReference);
        CreatesessionResponseType csrt = SessionManagerPlus.getCsrt(log.getTime());
        ResetPinRequest ResetPin_ReqType = new ResetPinRequest();
        ResetPinRequestType resetPinRequestType = new ResetPinRequestType();
        resetPinRequestType.setSessionid(csrt.getSessionid());
        resetPinRequestType.setAgent(agentReference);
        resetPinRequestType.setNewPin(newPin);
        resetPinRequestType.setSuppressPinExpiry(true);
        ResetPin_ReqType.setResetPinRequestType(resetPinRequestType);

        //client-backend
        com.utiba.delirium.ws.misc.KeyValuePair client= new com.utiba.delirium.ws.misc.KeyValuePair();
        client.setKey("client");
        client.setValue("backend");

        //kvpm
        com.utiba.delirium.ws.misc.KeyValuePairMap kvpm = new com.utiba.delirium.ws.misc.KeyValuePairMap();
        kvpm.getKeyValuePairs().add(client);

        com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
        kvIsSMS.setKey("issms");
        kvIsSMS.setValue("no");
        kvpm.getKeyValuePairs().add(kvIsSMS);

        //set
        resetPinRequestType.setExtraTransData(kvpm);

        StandardBizResponse bizResponse = stub.resetPin(ResetPin_ReqType).getResetPinReturn();

        log.add("resetPIN response","");
        log.add("transid",bizResponse.getTransid());
        log.add("resultcode",bizResponse.getResult());

        log.writeLog();

        return bizResponse;
    }

        public StandardBizResponse mapAgentToGroup(String TransID, String MSISDN, long GroupID) {
            /************** MAP AGENT TO GROUP ********************/
            if (stub == null)
                callSoap();

            Common.BuildLog log = new Common.BuildLog(logger);
            CreatesessionResponseType csrt = SessionManagerPlus.getCsrt(log.getTime());

            MapAgentRequest MapAgent_Req = new MapAgentRequest();

            MapAgentRequestType MapAgent_ReqType = new MapAgentRequestType();
            MapAgent_ReqType.setSessionid(csrt.getSessionid());
            MapAgent_ReqType.setAgid(GroupID);
            MapAgent_ReqType.setAgent(MSISDN);

            MapAgent_Req.setMapAgentRequestType(MapAgent_ReqType);

            StandardBizResponse biz_response_modify = stub.mapAgent(MapAgent_Req).getMapAgentReturn();

            log.add("tranid",TransID);
            log.add("group",(biz_response_modify.getResult() == 0 ? "SUCCESS" : "FAIL"));
            log.add("group result tranid",biz_response_modify.getTransid());
            log.add("group result code",biz_response_modify.getResult());
            log.writeLog();

            return biz_response_modify;
        }

        public StandardBizResponse mapAgentToCapset(String TransID, String MSISDN, long CapSetID, int WalletType, long UpperLimit) {
            if (stub == null)
                callSoap();

            Common.BuildLog log = new Common.BuildLog(logger);

            CreatesessionResponseType csrt = SessionManagerPlus.getCsrt(log.getTime());
            ModifyAccountRequest ModifyAccount_Req = new ModifyAccountRequest();

            ModifyAccountRequestType ModifyAccount_ReqType = new ModifyAccountRequestType();
            ModifyAccount_ReqType.setSessionid(csrt.getSessionid());
            ModifyAccount_ReqType.setCapSet(CapSetID);
            ModifyAccount_ReqType.setAgent(MSISDN);
            ModifyAccount_ReqType.setType(WalletType);
            ModifyAccount_ReqType.setUpperLimit(new BigDecimal(UpperLimit));

            ModifyAccount_Req.setModifyAccountRequestType(ModifyAccount_ReqType);

            StandardBizResponse biz_response_modify = stub.modifyAccount(ModifyAccount_Req).getModifyAccountReturn();

            log.add("TransID",TransID);
            log.add("capset",CapSetID + (biz_response_modify.getResult() == 0 ? "SUCCESS" : "FAIL"));
            log.add("capset result tranid",biz_response_modify.getTransid());
            log.add("capset result code",biz_response_modify.getResult());
            log.add("capset upper limit",UpperLimit);
            log.writeLog();

            return biz_response_modify;
        }

        public StandardBizResponse changePIN(String phone, String oldPin, String newPin) {

            if (stub == null)
                callSoap();
            Common.BuildLog log = new Common.BuildLog(logger);

            String hashPin = "";
            CreatesessionResponseType ccsrt = stub.createsession();
            try {
                hashPin = DataUtil.getSHA(ccsrt.getSessionid() + DataUtil.getSHA(phone + oldPin).toLowerCase()).toUpperCase();
            } catch (Exception e) {
                logger.info("change pin "  + phone + " " + e.getMessage());
            }
            LoginRequestType login_req = new LoginRequestType();
            login_req.setSessionid(ccsrt.getSessionid());
            login_req.setInitiator(phone);
            login_req.setPin(hashPin);
            StandardBizResponse biz_response_login = stub.login(login_req);

            if (biz_response_login.getResult() == 0 || biz_response_login.getResult() == 1013) {
                log.add("login result", "ok");
                PinRequestType type = new PinRequestType();
                type.setSessionid(ccsrt.getSessionid());
                type.setInitiator(phone);
                type.setPin(hashPin);
                type.setNewPin(newPin);
                biz_response_login = stub.pin(type);
            }

            log.add("tranid",biz_response_login.getTransid());
            log.add("result",biz_response_login.getResult());
            log.writeLog();

            return biz_response_login;
        }

        public StandardBizResponse bankin(Vertx _vertx,String agent
                ,String bank_code
                ,String mpin
                ,BigDecimal amount
                ,String channel, Common.BuildLog log) {


            if (stub == null){
                callSoap();
            }

            log.add("func", "soapin.bankin");
            log.add("pinlen",mpin.length());
            log.add("amount",amount);
            log.add("bankcode",bank_code);
            log.add("channel",channel);

            SessionInfo csrt = SessionManagerPlus.getCsrt(_vertx,agent, mpin,log.getTime());

            int loginResult;
            if(csrt== null){
                //khong login vao duoc soapin
                log.add("csrt","null");
                log.add("desc", "can not get session from SessionManagerPlus");

                StandardBizResponse standardRes = new StandardBizResponse();
                standardRes.setResult(-100);
                standardRes.setTransid(-100);
                return  standardRes;

            }else {
                loginResult= csrt.getLoginResult();
                log.add("login result",loginResult);
            }

            BankcashinRequestType bankcashinRequestType = new BankcashinRequestType();
            if(csrt.getCsrt() !=null && csrt.getCsrt().getSessionid()!=null){
                bankcashinRequestType.setSessionid(csrt.getCsrt().getSessionid());
            }

            bankcashinRequestType.setAmount(amount);

            String bank_source = bank_direct_cfg.getString("bank." + bank_code+ ".src","");
            log.add("bank_source",bank_source);

            bankcashinRequestType.setDetails(bank_code);//Bank Account Number -->vietcombank bank cod vietinbank: 102/vtb.bank
            bankcashinRequestType.setSource(bank_source);//.bank account which this agent mapping to

            com.utiba.delirium.ws.misc.KeyValuePair client = new com.utiba.delirium.ws.misc.KeyValuePair();
            client.setKey("client");
            client.setValue("backend");

            com.utiba.delirium.ws.misc.KeyValuePair chanel = new com.utiba.delirium.ws.misc.KeyValuePair();
            chanel.setKey("chanel");
            chanel.setValue(channel);

            com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
            kvIsSMS.setKey("issms");
            kvIsSMS.setValue("no");

            com.utiba.delirium.ws.misc.KeyValuePairMap kvpm = new com.utiba.delirium.ws.misc.KeyValuePairMap();
            kvpm.getKeyValuePairs().add(client);
            kvpm.getKeyValuePairs().add(chanel);
            kvpm.getKeyValuePairs().add(kvIsSMS);

            bankcashinRequestType.setExtraTransData(kvpm);

            StandardBizResponse standardBizResponse = stub.bankcashin(bankcashinRequestType);

            int tranId = -100;
            int rcode = -100;
            String desc = "";
            if(standardBizResponse != null && standardBizResponse.getTransid() != null){
                tranId = standardBizResponse.getTransid();
                rcode = standardBizResponse.getResult();
            }else{
                if(standardBizResponse == null){
                    desc ="standardBizResponse == null";
                }else if(standardBizResponse.getTransid() == null){
                    desc = "standardBizResponse.getTransid() == null";
                }else{
                    desc="Khong xac dinh duoc ket qua tra ve tu core";
                }
            }

            log.add("tranid", tranId);
            log.add("rcode", rcode);
            log.add("desc", desc);
            log.add("errorDesc", SoapError.getDesc(rcode));

            return standardBizResponse;
        }

        public StandardBizResponse bankout(Vertx _vertx,String agent
                ,String bank_code
                ,String mpin
                ,BigDecimal amount
                ,String channel, Common.BuildLog log) {

            if (stub == null){
                callSoap();
            }

            log.add("func", "soapin.bankout");
            log.add("pinlen",mpin.length());
            log.add("amount",amount);
            log.add("bankcode",bank_code);
            log.add("channel",channel);

            SessionInfo sessionInfo = SessionManagerPlus.getCsrt(_vertx, agent, mpin,log.getTime());

            BankcashoutRequestType bankcashoutRequestType = new BankcashoutRequestType();

            if(sessionInfo.getCsrt() != null && sessionInfo.getCsrt().getSessionid() != null){
                bankcashoutRequestType.setSessionid(sessionInfo.getCsrt().getSessionid());
            }

            bankcashoutRequestType.setAmount(amount);

            int loginResult;
            if(sessionInfo == null){
                log.add("sessionInfo","null");

                //khong login vao duoc soapin
                log.add("csrt","null");
                log.add("desc", "can not get session from SessionManagerPlus");

                StandardBizResponse standardRes = new StandardBizResponse();
                standardRes.setResult(-100);
                standardRes.setTransid(-100);
                return  standardRes;

            }else{

                loginResult = sessionInfo.getLoginResult();
                log.add("login result",loginResult);
            }

            String bank_source = bank_direct_cfg.getString("bank." + bank_code+ ".src","");

            log.add("bank_source",bank_source);

            bankcashoutRequestType.setDetails(bank_code);//Bank Account Number
            bankcashoutRequestType.setTarget(bank_source);//.bank account which this agent mapping to


            com.utiba.delirium.ws.misc.KeyValuePair client = new com.utiba.delirium.ws.misc.KeyValuePair();
            client.setKey("client");
            client.setValue("backend");

            com.utiba.delirium.ws.misc.KeyValuePair chanel = new com.utiba.delirium.ws.misc.KeyValuePair();
            chanel.setKey("chanel");
            chanel.setValue(channel);

            com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
            kvIsSMS.setKey("issms");
            kvIsSMS.setValue("no");

            com.utiba.delirium.ws.misc.KeyValuePairMap kvpm = new com.utiba.delirium.ws.misc.KeyValuePairMap();
            kvpm.getKeyValuePairs().add(client);
            kvpm.getKeyValuePairs().add(chanel);
            kvpm.getKeyValuePairs().add(kvIsSMS);

            bankcashoutRequestType.setExtraTransData(kvpm);

            StandardBizResponse standardBizResponse = stub.bankcashout(bankcashoutRequestType);

            int tranId = -100;
            int rcode = -100;
            String desc = "";
            if(standardBizResponse != null && standardBizResponse.getTransid() != null){
                tranId = standardBizResponse.getTransid();
                rcode = standardBizResponse.getResult();
            }else{
                if(standardBizResponse == null){
                    desc ="standardBizResponse == null";
                }else if(standardBizResponse.getTransid() == null){
                    desc = "standardBizResponse.getTransid() == null";
                }else{
                    desc="Khong xac dinh duoc ket qua tra ve tu core";
                }
            }

            log.add("tranid", tranId);
            log.add("rcode", rcode);
            log.add("desc", desc);

            return standardBizResponse;
        }

        public static void main(String[] args) {
            String a = "TRAN THI KHANH NINH,411/4 LE DUC THO,0989033099~141010,1125889,2014-08-22 2014-09-21#~";
            System.out.println(billInfoToJsonObject(a));
        }

        //admin.start
        public LoginTrustRespond loginTrust(){

            String susername = soap_un;
            String spassword = soap_pw;
            String username = "";
            String password = "";
            try {
                Cryption cryption = new Cryption(fi_cfg, logger);
                String key = cryption.getPrivateKey("");
                password = new String(cryption.decrypt(spassword, key));
                username = new String(cryption.decrypt(susername, key));
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Login Soap - Cannot Hash MPIN. " + e.getMessage());
            }
            LoginTrustRespond trustRespond = new LoginTrustRespond();
            String MPin = "";
            CreatesessionResponseType csrt = stub.createsession();
            try {
                MPin = getSHA(csrt.getSessionid() + getSHA(username + password).toLowerCase()).toUpperCase();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Login Soap - Cannot Hash MPIN. ".concat(e.getMessage()));
            }
            LoginRequestType login_req = new LoginRequestType();
            login_req.setSessionid(csrt.getSessionid());
            login_req.setInitiator(username);
            login_req.setPin(MPin);
            StandardBizResponse bizResponse = stub.login(login_req);
            trustRespond.setStub(stub);
            trustRespond.setCsrt(csrt);
            trustRespond.setBizResponse(bizResponse);
            if(bizResponse.getResult() != 0)
                logger.info("LoginAgent fail, Soap result >> " + bizResponse.getResult());
            return trustRespond;
        }

        public AdjustWalletResponse adjustment(String source
                ,String target
                ,BigDecimal amount
                ,int walletType
                ,String description
                ,List<SoapProto.keyValuePair> keyValuePairs
                ,Common.BuildLog log)
        {

            //tai khoan bi tru tien (bank-net): source
            // tai khoan duoc cong tien(bank-net) : target
            //paymentType : 1 Thanh toán ngay; 2: Thanh toán tạm giữ

            com.utiba.delirium.ws.misc.KeyValuePairMap kvpm = new com.utiba.delirium.ws.misc.KeyValuePairMap();

            //set them key
            setKeyValuePairForCommand(kvpm,keyValuePairs,log);

            com.utiba.delirium.ws.misc.KeyValuePair client = new com.utiba.delirium.ws.misc.KeyValuePair();
            client.setKey("client");
            client.setValue("backend");

            com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
            kvIsSMS.setKey("issms");

            kvIsSMS.setValue("no");

            com.utiba.delirium.ws.misc.KeyValuePair chanel = new com.utiba.delirium.ws.misc.KeyValuePair();
            chanel.setKey("chanel");
            chanel.setValue("mobi");

            kvpm.getKeyValuePairs().add(client);
            kvpm.getKeyValuePairs().add(chanel);
            kvpm.getKeyValuePairs().add(kvIsSMS);

            CreatesessionResponseType info = SessionManagerPlus.getCsrt(log.getTime());

            AdjustWalletRequestType adjustWallet_ReqType = new AdjustWalletRequestType();
            adjustWallet_ReqType.setSessionid(info.getSessionid());
            adjustWallet_ReqType.setAmount(amount);
            adjustWallet_ReqType.setType(walletType);//Adjustment MoMo
            adjustWallet_ReqType.setSource(source);
            adjustWallet_ReqType.setTarget(target);
            adjustWallet_ReqType.setSuppressConfirm(true);
            adjustWallet_ReqType.setExtDetails(description);

            if (kvpm.getKeyValuePairs().size() > 0) {

                adjustWallet_ReqType.setExtraTransData(kvpm);
            }

            AdjustWalletRequest adjustWallet_Req = new AdjustWalletRequest();
            adjustWallet_Req.setAdjustWalletRequestType(adjustWallet_ReqType);

            AdjustWalletResponse adjustWalletResponse = stub.adjustWallet(adjustWallet_Req);

            StandardBizResponse standardBizResponse = adjustWalletResponse.getAdjustWalletReturn();

            int tranId = -100;
            int rcode = -100;
            String desc = "";
            if(standardBizResponse != null && standardBizResponse.getTransid() != null){
                tranId = standardBizResponse.getTransid();
                rcode = standardBizResponse.getResult();
            }else{
                if(standardBizResponse == null){
                    desc ="standardBizResponse == null";
                }else if(standardBizResponse.getTransid() == null){
                    desc = "standardBizResponse.getTransid() == null";
                }else{
                    desc="Khong xac dinh duoc ket qua tra ve tu core";
                }
            }

            log.add("function", "adjustment");
            log.add("tranid", tranId);
            log.add("source", source);
            log.add("target", target);
            log.add("amount", amount);
            log.add("wallettype", walletType);
            log.add("rcode", rcode);
            log.add("desc", desc);
            log.add("errdesc",SoapError.getDesc(rcode));

            return adjustWalletResponse;
        }

        //admin.end
        private LoginTrustRespond loginAgent(String username, String password,long time){
            //writeLog.writeLog("Login Agent for payment: agent > " + username + " ******");
            LoginTrustRespond trustRespond = new LoginTrustRespond();
            //UMarketSC stub = this.stub;
            String MPin = "";
            CreatesessionResponseType csrt = stub.createsession();
            try {
                MPin = getSHA(csrt.getSessionid() + getSHA(username + password).toLowerCase()).toUpperCase();
            } catch (Exception e) {
                logger.info(time + " " + username + " LoginTrustRespond loginAgent");
                logger.info(time + " " + username + " " +  e.getMessage());

            }
            LoginRequestType login_req = new LoginRequestType();
            login_req.setSessionid(csrt.getSessionid());
            login_req.setInitiator(username);
            login_req.setPin(MPin);
            StandardBizResponse bizResponse = stub.login(login_req);
            trustRespond.setStub(stub);
            trustRespond.setCsrt(csrt);
            trustRespond.setBizResponse(bizResponse);

            return trustRespond;
        }

        public StandardBizResponse billPayChien(String phoneNumber
                ,String mpin
                ,String providerId  //billpayid -- >ma nhan cung cap
                ,String accountID   // ma hoa don can thanh toan
                ,long amt
                ,int walletType
                ,String channel,List<SoapProto.keyValuePair> keyValuePairList, Common.BuildLog log) {

            log.add("func","billPayChien");
            log.add("pinlen",mpin.length());
            log.add("agent",phoneNumber);
            log.add("providerid",providerId);
            log.add("billid",accountID);
            log.add("amount",amt);
            log.add("wallettype",walletType);
            log.add("channel",channel);

            BigDecimal amount =  new BigDecimal(amt);
            LoginTrustRespond trustRespond = loginAgent(phoneNumber, mpin, log.getTime());
            CreatesessionResponseType csrt = trustRespond.getCsrt();
            UMarketSC stub = trustRespond.getStub();
            BillpayRequestType billpayRequestType = new BillpayRequestType();
            billpayRequestType.setSessionid(csrt.getSessionid());
            billpayRequestType.setAmount(amount);
            billpayRequestType.setType(walletType);//1 for e-wallet, 2 for stock

            int loginResult;

            if(csrt == null){
                log.add("csrt","null");
            }else{
                loginResult = csrt.getResult();
                log.add("login result",loginResult);
            }

            String targetAgent = pay_cfg.getString("billerid." + providerId.toLowerCase().trim() + ".target", "");

            if(targetAgent.equalsIgnoreCase("")){
                targetAgent = providerId;
            }
            log.add("targetAgent",targetAgent);

            billpayRequestType.setTarget(targetAgent);

            //rebuild list of key value pair
            List<SoapProto.keyValuePair> valuePairList = buildKeyValuePairList(keyValuePairList,providerId,amt,log);

            com.utiba.delirium.ws.misc.KeyValuePairMap value = new com.utiba.delirium.ws.misc.KeyValuePairMap();

            //append key value pair for command
            setKeyValuePairForCommand(value,valuePairList,log);

            com.utiba.delirium.ws.misc.KeyValuePair type = new com.utiba.delirium.ws.misc.KeyValuePair();
            type.setKey("__target");
            type.setValue(providerId);
            value.getKeyValuePairs().add(type);

            com.utiba.delirium.ws.misc.KeyValuePair account = new com.utiba.delirium.ws.misc.KeyValuePair();
            account.setKey("account");
            account.setValue(accountID);
            value.getKeyValuePairs().add(account);

            com.utiba.delirium.ws.misc.KeyValuePair client = new com.utiba.delirium.ws.misc.KeyValuePair();
            client.setKey("client");
            client.setValue("backend");
            value.getKeyValuePairs().add(client);

            com.utiba.delirium.ws.misc.KeyValuePair chanel = new com.utiba.delirium.ws.misc.KeyValuePair();
            chanel.setKey("chanel");
            chanel.setValue(channel);
            value.getKeyValuePairs().add(chanel);

            com.utiba.delirium.ws.misc.KeyValuePair kvIsSMS = new com.utiba.delirium.ws.misc.KeyValuePair();
            kvIsSMS.setKey("issms");
            kvIsSMS.setValue("no");
            value.getKeyValuePairs().add(kvIsSMS);

            billpayRequestType.setExtraTransData(value);

            //gia lap thanh toan thanh cong
            //gia lap.start
        /*StandardBizResponse standard = new StandardBizResponse();
        standard.setResult(0);
        standard.setTransid(12345678);
        return  standard;*/
            //gia lap.end

            StandardBizResponse standardBizResponse = stub.billpay(billpayRequestType);

            long tranId = -100;
            int rcode = -100;
            String desc = "";
            if(standardBizResponse != null && standardBizResponse.getTransid() != null){
                tranId = (standardBizResponse.getTransid() == null ? System.currentTimeMillis() : standardBizResponse.getTransid());
                rcode = standardBizResponse.getResult();
            }else{
                if(standardBizResponse == null){
                    desc ="standardBizResponse == null";
                }else if(standardBizResponse.getTransid() == null){
                    desc = "standardBizResponse.getTransid() == null";
                }else{
                    desc="Khong xac dinh duoc ket qua tra ve tu core";
                }
            }

            log.add("tranid",tranId);
            log.add("rcode",rcode);
            log.add("desc",desc);
            log.add("errdesc",SoapError.getDesc(rcode));

            return standardBizResponse;
        }

        private void setKeyValuePairForCommand(com.utiba.delirium.ws.misc.KeyValuePairMap kvpm
                ,List<SoapProto.keyValuePair> listKeyValuePairs
                ,Common.BuildLog log){
            if (listKeyValuePairs != null && listKeyValuePairs.size() > 0){
                for (int i=0; i<listKeyValuePairs.size(); i++) {

                    com.utiba.delirium.ws.misc.KeyValuePair item = new com.utiba.delirium.ws.misc.KeyValuePair();

                    item.setKey(listKeyValuePairs.get(i).getKey());
                    item.setValue(listKeyValuePairs.get(i).getValue());

                    kvpm.getKeyValuePairs().add(item);

                    log.add(item.getKey(),item.getValue());

                }
            }
        }

        private void setUmarketKeyValuePairForCommand(umarketscws.KeyValuePairMap kvpm
                ,List<SoapProto.keyValuePair> listKeyValuePairs
                ,Common.BuildLog log){
            if (listKeyValuePairs != null && listKeyValuePairs.size() > 0){
                for (int i=0; i<listKeyValuePairs.size(); i++) {

                    umarketscws.KeyValuePair item = new umarketscws.KeyValuePair();

                    item.setKey(listKeyValuePairs.get(i).getKey());
                    item.setValue(listKeyValuePairs.get(i).getValue());

                    kvpm.getKeyValuePair().add(item);

                    log.add(item.getKey(),item.getValue());

                }
            }
        }

        private long calBonusAmount(String providerid,long tranAmount, Common.BuildLog log){
            JsonObject jo = remain_referal.getObject(providerid,null);
            log.add("function","getBonusAmount");
            log.add("service id", providerid);
            if(jo == null){
                log.add("json result", "null");
                return 0;
            }

            int percent = jo.getInteger("percent",0);
            long staticValue = jo.getLong("static",0);
            log.add("percent", percent);
            log.add("static value", staticValue);

            long bonusValue = (long)Math.ceil((tranAmount*percent)/100) + staticValue;
            return bonusValue;
        /*"remain_for_referal":{
            "day":365,
                    "date_affected":"2014-10-01 00:00:00",
                    "topup":{
                "max_value_per_month":300000,
                        "percent":3,
                        "static":0
            },
            "vinahcm":{
                "percent":0,
                        "static":5000
            },
            "vinahcm":{
                "percent":0,
                        "static":5000
            },
            "jetstar":{
                "percent":0,
                        "static":5000
            },
            "dien":{
                "percent":0,
                        "static":5000
            },
            "avg":{
                "percent":0,
                        "static":5000
            },
            "ifpt":{
                "percent":0,
                        "static":5000
            },
            "cdhcm":{
                "percent":0,
                        "static":5000
            }
        }*/

        }

        private List<SoapProto.keyValuePair> buildKeyValuePairList(List<SoapProto.keyValuePair> listKeyPairs
                ,String providerId
                ,long tranAmount
                ,Common.BuildLog log){
            ArrayList<SoapProto.keyValuePair> keyValuePairs = null;

            log.add("function","buildKeyValuePairList");


            if(listKeyPairs != null || listKeyPairs.size() == 0){
                log.add("Khong co key referal","");
                return keyValuePairs;
            }
            for(int i = 0;i<listKeyPairs.size();i++){

                log.add(listKeyPairs.get(i).getKey(), listKeyPairs.get(i).getValue());

                //co chua key referal
                if(Const.REFERAL.equalsIgnoreCase(listKeyPairs.get(i).getKey())){
                    long bonusAmt = calBonusAmount(providerId,tranAmount,log);
                    keyValuePairs = new ArrayList<>();

                    //add key referal
                    keyValuePairs.add( listKeyPairs.get(i));

                    //add key bonus value
                    keyValuePairs.add(SoapProto.keyValuePair.newBuilder()
                                    .setKey(Const.BONUSFORREFERAL)
                                    .setValue(String.valueOf(bonusAmt))
                                    .build()
                    );
                    break;
                }
            }
            return keyValuePairs;
        }



        public JsonObject returnInfoDecrypt(String susername, String spassword){

            JsonObject jsonResult = new JsonObject();
            String username = "";
            String password = "";
            try {
                Cryption cryption = new Cryption(fi_cfg, logger);
                String key = cryption.getPrivateKey("");
                password = new String(cryption.decrypt(spassword, key));
                username = new String(cryption.decrypt(susername, key));
                jsonResult.putString("username_encrypted", username);
                jsonResult.putString("password_encrypted", password);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Login Soap - Cannot Hash MPIN. " + e.getMessage());
            }

            return jsonResult;
        }

        public JsonObject returnInfoEncrypt(String susername, String spassword){

            JsonObject jsonResult = new JsonObject();
            String username = "";
            String password = "";
            try {
                Cryption cryption = new Cryption(fi_cfg, logger);
                String key = cryption.getPublicKey("");
                password = new String(cryption.encrypt(spassword.getBytes(), key));
                username = new String(cryption.encrypt(susername.getBytes(), key));
                jsonResult.putString("username_encrypted", username);
                jsonResult.putString("password_encrypted", password);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Login Soap - Cannot Hash MPIN. " + e.getMessage());
            }

            return jsonResult;
        }
    }

