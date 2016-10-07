package com.mservice.momo.vertx;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Created by admin on 2/11/14.
 */
public class NamVerticle extends Verticle {
    //load config first
    public static org.vertx.java.core.logging.Logger logger;
    public static JsonObject globalCfg = null;

    @Override
    public void start() {

        logger = container.logger();
        globalCfg =  container.config();//mongo notification.start
        JsonObject mongo_noti = new JsonObject();
        mongo_noti.putString("host", "172.16.14.34");
        mongo_noti.putNumber("port", 27017);
        mongo_noti.putString("username",null);
        mongo_noti.putString("password",null);
        mongo_noti.putNumber("pool_size", 20);
        mongo_noti.putBoolean("auto_connect_retry", true);
        mongo_noti.putNumber("socket_timeout", 20);
        mongo_noti.putBoolean("use_ssl", false);
        mongo_noti = globalCfg.getObject("mongo_noti", mongo_noti);

        container.deployModule("mservice~mongo_noti~1.0", mongo_noti, 1, new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> ar) {
                if (ar.succeeded()) {
                    logger.info("mservice~mongo_noti~1.0 started ok");

                } else {
                    logger.info("mservice~mongo_noti~1.0 start fail");
                    logger.info(ar.cause().getMessage());
                    ar.cause().printStackTrace();
                }
            }
        });


        //we first init our cached for imei+private_key map
        //deploy the data module
        JsonObject mongo_config = new JsonObject();
        mongo_config.putString("address","com.mservice.momo.database");
        mongo_config.putString("host", "localhost");
        mongo_config.putNumber("port", 27017);
        mongo_config.putString("username",null);
        mongo_config.putString("password", null);
        mongo_config.putNumber("pool_size", 20);
        mongo_config.putBoolean("auto_connect_retry", true);
        mongo_config.putNumber("socket_timeout",20);
        mongo_config.putBoolean("use_ssl", false);
        mongo_config = globalCfg.getObject("mongo", mongo_config);

        container.deployModule("mservice~mongo~1.0", mongo_config, 1, new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> ar) {
                if (ar.succeeded()) {
                    logger.info("mongo-verticle started ok");
                    JsonObject soap_default = new JsonObject();
                    soap_default.putString("soap_url", "http://172.16.18.50:8280/services/umarketsc?wsdl");
                    soap_default.putString("soap_user_name", "admin_soapin");
                    soap_default.putString("soap_password", "soap#access");
                    soap_default.putNumber("soap_session_expire", 6);

                    soap_default.putString("sys_c2c_in_account", "workingaccount");
                    JsonObject bill_cfg = new JsonObject();
                    bill_cfg.putString("billerid.mlevn.checkdebit.url", "http://172.16.18.50:38080/MockService/services/BillingServiceProviderSOAP");

                    soap_default.putObject("biller_cfg", bill_cfg);

                    JsonObject soap_config = globalCfg.getObject("soap", soap_default);

                    JsonObject df_tran = new JsonObject();

                    df_tran.putNumber("bank_in",1000);
                    df_tran.putNumber("bank_out",1000);
                    df_tran.putNumber("m2c",1000);
                    df_tran.putNumber("m2m",1000);
                    df_tran.putNumber("top_up",1000);
                    df_tran.putNumber("top_up_game",1000);
                    df_tran.putNumber("pay_one_bill",1000);
                    df_tran.putNumber("quick_deposit",1000);
                    df_tran.putNumber("quick_payment",1000);
                    df_tran.putNumber("pay_one_bill_other",1000);
                    df_tran.putNumber("transfer_money_2_place",1000);

                    df_tran.putNumber("bill_pay_telephone",1000);
                    df_tran.putNumber("bill_pay_ticket_airline",1000);
                    df_tran.putNumber("bill_pay_ticket_train",1000);
                    df_tran.putNumber("bill_pay_insurance",1000);
                    df_tran.putNumber("bill_pay_internet",1000);
                    df_tran.putNumber("bill_pay_other",1000);

                    df_tran.putNumber("deposit_cash_other",1000);
                    df_tran.putNumber("buy_mobility_card",1000);
                    df_tran.putNumber("buy_game_card",1000);
                    df_tran.putNumber("buy_other",1000);

                    JsonObject tran_has_point = globalCfg.getObject("tran_has_point", df_tran);

                    soap_config.putObject("tran_has_point", tran_has_point);
                    JsonObject fi_cfg = globalCfg.getObject("fi");
                    soap_config.putObject("fi", fi_cfg);

                    container.deployWorkerVerticle("com.mservice.momo.gateway.internal.soapin.SoapVerticle", soap_config, 1, true, new AsyncResultHandler<String>() {

                        public void handle(AsyncResult<String> ar) {
                            if (ar.succeeded()) {
                                logger.info("SoapVerticle started ok");
                            } else {
                                logger.info("SoapVerticle start fail");
                                ar.cause().printStackTrace();
                            }
                        }
                    });



                    container.deployVerticle("com.mservice.momo.notification.NotificationVerticle", globalCfg, new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("NotificationVerticle has been deployed successfully!");

                            } else {
                                logger.error("NotificationVerticle has been deployed failed!", event.cause());
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.vertx.WcVerticle", globalCfg, new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("WcVerticle has been deployed successfully!");

                            } else {
                                logger.error("WcVerticle has been deployed failed!", event.cause());
                            }
                        }
                    });


                    container.deployVerticle("com.mservice.momo.web.internal.webadmin.verticle.WebAdminVerticle", globalCfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("WebAdminVerticle has been deployed successfully!");
                            } else {
                                logger.error("WebAdminVerticle deployed fail!", event.cause());
                            }
                        }
                    });

                    container.deployVerticle("com.mservice.momo.vertx.WebServiceVerticle", globalCfg, new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> event) {
                            if (event.succeeded()) {
                                logger.info("WebServiceVerticle has been deployed successfully!");
                            } else {
                                logger.error("WebServiceVerticle deployed fail!", event.cause());
                            }
                        }
                    });


                }
            }
        });
    }
}
