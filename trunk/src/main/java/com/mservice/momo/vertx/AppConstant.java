package com.mservice.momo.vertx;

import com.mservice.momo.vertx.processor.Misc;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by nam on 6/10/14.
 */

public class AppConstant {

    //todo do we still need this ???
    public static String prefixServer = "";

    public static String hostServer = "";
    public static int portServer = 0;
    public static final String PREFIX = getPrefix();
    public static final String HOST_SERVER = getHostServer();
    public static final int PORT_SERVER = getPortServer();
    public static String AppSmsVerticle_ADDRESS = PREFIX + "com.mservice.momo.vertx.app.sms";
    public static String BanknetVerticle_ADDRESS = PREFIX + "BANKNET_WEBSERVICE";
    public static String CommandVerticle_ADDRESS = PREFIX + "momo.CommandVerticle";
    public static String HttpFileDownloadVerticle_ADDRESS = PREFIX + "HttpFileDownloadVerticle";
    public static String ImageProcessVerticle_ADDRESS = PREFIX + "imageProcessVerticle";
    public static String PayBackCDHHVerticle_ADDRESS = PREFIX + "PayBackCDHHVerticle";
    public static String LocationVerticle_ADDRESS = PREFIX + "com.mservice.momo.location";
    public static String MongoVerticle_ADDRESS = PREFIX + "com.mservice.momo.database";
    public static String MongoVerticle_NOTIFICATION_ADDRESS = PREFIX + "com.mservice.momo.database_notification";
    public static String NotificationVerticle_ADDRESS_SEND_NOTIFICATION = PREFIX + "notificationVerticle.sendNotification";
    public static String NotificationVerticle_CloudNotifyVerticleUpdate_UpdateListcache = PREFIX + "NotificationVerticle_CloudNotifyVerticleUpdate_UpdateListcache";
    public static String NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_SDK_SERVER = PREFIX + "notificationVerticle.sendNotificationFromSDKServer";
    public static String NotificationVerticle_ADDRESS_SEND_SMS = PREFIX + "notificationVerticle.sendSMS";
    public static String NotificationVerticle_REMIND_SEND_NOTI = PREFIX + "notificationVerticle.remindSendNoti";
    public static String NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL = PREFIX + "notificationVerticle.sendNotificationFromTool";
    public static String NotificationVerticle_ADDRESS_SEND_NOTIFICATION_FROM_TOOL_WITH_REDIS = PREFIX + "notificationVerticle.sendNotificationFromToolViaRedis";
    public static String RedisVerticle_NOTIFICATION_FROM_TOOL = PREFIX + "redisVerticle_NotificationFromTool";
    public static String NotificationVerticle_ADDRESS_SEND_PROMOTION_POPUP_NOTIFICATION = PREFIX + "notificationVerticle.sendPromotionPopupNotification";
    public static String NotificationVerticle_VISA_ADDRESS_SEND_NOTIFICATION = PREFIX + "notificationVerticle.sendVisaNotification";
    public static String NotificationVerticle_ADDRESS_SEND_NOTIFICATION_VIA_CLOUD = PREFIX + "notificationVerticle.sendNotificationViaCloud";
    public static String NotificationVerticle_SYNC_ADDRESS_SEND_NOTIFICATION = PREFIX + "notificationVerticle.sendSyncNotification";
    public static String ReportAdminOracleVerticle_ADDRESS = PREFIX + "REPORT_ADMIN.OracleVerticle";
    public static String SmsVerticle_ADDRESS = PREFIX + "com.mservimomoce.momo.gateway.internal.sms";
    public static String SoapVerticle_ADDRESS = PREFIX + "com.mservice.momo.soapin";
    public static String StatisticVerticle_ADDRESS_ACTION = PREFIX + "StatisticVerticle.action";
    public static String StatisticVerticle_ADDRESS_GET_NUMBER = PREFIX + "StatisticVerticle.getNumber";
    public static String WcVerticle_ADDRESS_REQUEST_CMD = PREFIX + "WcVerticle";
    public static String WebAdminVerticle_WEB_ADMIN_SESSION_MAP = PREFIX + "WebAdminVerticle.maps.session";
    public static String LStandbyOracleVerticle_ADDRESS = PREFIX + "LStandbyOracleVerticle";
    public static String HTPPOracleVerticle_ADDRESS = PREFIX + "HTPPOracleVerticle";
    public static String BankHelperVerticle_ADDRESS = PREFIX + "BankHelperVerticle";
    public static String MongoExportVerticle = PREFIX + "MongoExportVerticle";
    public static String VinaGameCinemaVerticle_ADDRESS = PREFIX + "VinaGameCinemaVerticle";
    public static String WcTransfer = PREFIX + "WcTransferVerticle";
    public static String CoreConnectorVerticle_ADDRESS = PREFIX + "CoreConnectorVerticle";
    public static String Plus_CoreConnectorVerticle_ADDRESS = PREFIX + "PlusCoreConnectorVerticle";
    public static String CoreConnectorCDHHVerticle_ADDRESS = PREFIX + "CoreConnectorCDHHVerticle";
    public static String SmartLinkVerticle = PREFIX + "SmartLinkVerticle";
    public static String ConfigVerticle = PREFIX + "ConfigVerticle";
    public static String ConfigVerticleService = PREFIX + "ServiceConfVerticle.Service";
    public static String Promotion_ADDRESS = PREFIX + "PromotionVerticle";
    public static String VietCombak_Address = PREFIX + "VietCombak_Address";
    public static String BILL_PAY_BUSS_ADDRESS = PREFIX + "BILL_PAY_BUSS_ADDRESS";
    public static String IRON_MAN_PROMO_BUSS_ADDRESS = PREFIX + "IRON_MAN_PROMO_BUSS_ADDRESS";
    public static String OCTOBER_PROMO_BUSS_ADDRESS = PREFIX + "OCTOBER_PROMO_BUSS_ADDRESS";
    public static String ROLLBACK_50PERCENT_PROMO_BUSS_ADDRESS = PREFIX + "ROLLBACK_50PERCENT_PROMO_BUSS_ADDRESS";
    public static String PRE_IRON_MAN_PROMO_BUSS_ADDRESS = PREFIX + "PRE_IRON_MAN_PROMO_BUSS_ADDRESS";
    public static String VISA_MPOINT_BUSS_ADDRESS = PREFIX + "VISA_MPOINT_BUSS_ADDRESS";
    //bus dung chung
    public static String NotificationVerticle_ADDRESS_SEND_PACKET_FAIL = "notificationVerticle.failSendingNotification"; // on master
    public static String NotificationVerticle_ADDRESS_SEND_PACKET_SUCCESS = "notificationVerticle.successSendingNotification"; // on master
    public static String CloundNotifyVerticle = "CloudNotifyVerticle.Send"; // on master
    public static String CloundNotifyVerticleUpdate = "CloudNotifyVerticle.Update"; // on master
    public static String ConfigVerticlePublish = "ConfigVerticlePublish"; // request updating service status. // on master
    public static String ConfigVerticleService_Update = "ServiceConfVerticle.Update"; // reload by web admin
    public static String ConfigVerticleService_PrefixUpdate = PREFIX + "ServiceConfVerticle.PrefixUpdate";
    public static String SubmitForm_Address = "SubmitForm_Address"; // on master
    public static String DGD_SubmitForm_Address = "DGD_SubmitForm_Address"; // on master
    public static String Retailer_Fee_Address = "Retailer_Fee_Address"; // on master
    public static String Promotion_ADDRESS_UPDATE = "PromotionVerticle.Update"; // reload by web admin
    public static String VMNotifyVerticle = "VisaMaster_NotifyVerticle";
    public static String VMNotifyVerticle_Backup = "VisaMaster_NotifyVerticle_Backup";
    public static String DOLLAR_HEART_CUSTOMER_CARE_GROUP = PREFIX + "DOLLAR_HEART_CUSTOMER_CARE_GROUP";
    public static String ZALO_PROMOTION_BUSS_ADDRESS = PREFIX + "ZALO_PROMOTION_BUSS_ADDRESS";
    public static String HTTP_POST_CONNECTOR_BUS_ADDRESS = PREFIX + "HTTP_POST_CONNECTOR_BUS_ADDRESS";
    public static String HTTP_POST_BUS_ADDRESS = PREFIX + "HTTP_POST_BUS_ADDRESS";
    public static String CLAIMED_CODE_PROMOTION_BUSS_ADDRESS = PREFIX + "CLAIMED_CODE_PROMOTION_BUSS_ADDRESS";
    public static String LOAD_CLAIMED_CODE_PROMOTION_BUSS_ADDRESS = PREFIX + "LOAD_CLAIMED_CODE_PROMOTION_BUSS_ADDRESS";
    public static String UPDATE_CLAIMED_CODE_PROMOTION_BUSS_ADDRESS = "UPDATE_CLAIMED_CODE_PROMOTION_BUSS_ADDRESS";
    public static String WOMAN_NATIONAL_2016_PROMOTION_BUS_ADDRESS = PREFIX + "WOMAN_NATIONAL_2016_PROMOTION_BUS_ADDRESS";
    public static String OPEN_NEW_STORE_PROMOTION_BUS_ADDRESS = PREFIX + "OPEN_NEW_STORE_PROMOTION_BUS_ADDRESS";
    public static String REFERRAL_PROMOTION_BUS_ADDRESS       = PREFIX + "REFERRAL_PROMOTION_BUS_ADDRESS";
    public static String CASH_BACK_PROMOTION_BUS_ADDRESS      = PREFIX + "CASH_BACK_PROMOTION_BUS_ADDRESS";

    public static String BINHTAN_PROMOTION_BUS_ADDRESS      = PREFIX + "BINHTAN_PROMOTION_BUS_ADDRESS";
    public static String BILL_PAY_REDIS_BUS_ADDRESS      = PREFIX + "BILL_PAY_REDIS_BUS_ADDRESS";
    public static String PROMOTION_CLIENT_BUS_ADDRESS    = PREFIX + "PROMOTION_CLIENT_BUS_ADDRESS";
    public static String CHECK_CHEATING_PROMOTION_BUS_ADDRESS    = PREFIX + "CHECK_CHEATING_PROMOTION_BUS_ADDRESS";
    public static String NOTI_DB_BUS = PREFIX + "com.mservice.db.mysql.Noti";
    public static String TRAN_DB_BUS = PREFIX + "com.mservice.db.mysql.Tran";
    //address for promotion
    public static String LOTTE_PROMOTION_BUS_ADDRESS      = "LOTTE_PROMOTION_BUS_ADDRESS";
    public static String VIC_PROMOTION_BUS_ADDRESS      = "VIC_PROMOTION_BUS_ADDRESS";
    public static String GRAB_PROMOTION_BUS_ADDRESS      = "GRAB_PROMOTION_BUS_ADDRESS";
    public static String BINHTAN_EXTEND_PROMOTION_BUS_ADDRESS      = "BINHTAN_EXTEND_PROMOTION_BUS_ADDRESS";
    public static String BINHTAN_RETAIN_PROMOTION_BUS_ADDRESS      = "BINHTAN_RETAIN_PROMOTION_BUS_ADDRESS";

    public static String getPrefix() {
        if ("".equalsIgnoreCase(prefixServer)) {
            //load file
            JsonObject jsonConfigServer = Misc.readJsonObjectFile("config_server.json");
            prefixServer = jsonConfigServer.getString("prefix", "xxx");
            hostServer = jsonConfigServer.getString("host", "");
            portServer = jsonConfigServer.getInteger("port", 0);
        }

        return prefixServer;
    }

    public static String getHostServer() {
        if ("".equalsIgnoreCase(hostServer)) {
            //load file
            JsonObject jsonConfigServer = Misc.readJsonObjectFile("config_server.json");
            prefixServer = jsonConfigServer.getString("prefix", "xxx");
            hostServer = jsonConfigServer.getString("host", "");
            portServer = jsonConfigServer.getInteger("port", 0);
        }

        return hostServer;
    }

    public static int getPortServer() {
        if (0 == (portServer)) {
            //load file
            JsonObject jsonConfigServer = Misc.readJsonObjectFile("config_server.json");
            prefixServer = jsonConfigServer.getString("prefix", "xxx");
            hostServer = jsonConfigServer.getString("host", "");
            portServer = jsonConfigServer.getInteger("port", 0);
        }

        return portServer;
    }
}
