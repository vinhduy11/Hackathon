package com.mservice.momo.data.web;

import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.vertx.AppConstant;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;

/**
 * Created by ntunam on 3/20/14.
 */
public class BankAccountDb {
    public static final String COLLECTION_NAME = "web_bank_account";

    private EventBus eventBus;

    public BankAccountDb(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void save(int phoneNumber, String accountId, String bankName, String ownerName, long createdDate, String bankId, final Handler<String> handler) {
        JsonObject model = new JsonObject()
                .putNumber("phoneNumber", phoneNumber)
                .putString("accountId", accountId)
                .putString("bankName", bankName)
                .putString("ownerName", ownerName)
                .putNumber("createdDate", createdDate)
                .putString("bankId", bankId);

        JsonObject query = new JsonObject()
                .putString("action", "save")
                .putString("collection", COLLECTION_NAME)
                .putObject("document", model);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(event.body().getString("_id"));
            }
        });
    }

    public void update(int phoneNumber, String accountId, String bankName, String ownerName, long createdDate, String bankId, final Handler<String> handler) {
        JsonObject model = new JsonObject()
                .putNumber("phoneNumber", phoneNumber)
                .putString("accountId", accountId)
                .putString("bankName", bankName)
                .putString("ownerName", ownerName)
                .putNumber("createdDate", createdDate)
                .putString("bankId", bankId);

        JsonObject criteria = new JsonObject()
                .putNumber("phoneNumber", phoneNumber)
                .putString("accountId", accountId);

        JsonObject query = new JsonObject()
                .putString("action", "update")
                .putBoolean("upsert", true)
                .putString("collection", COLLECTION_NAME)
                .putObject("criteria", criteria)
                .putObject("objNew", model);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(event.body().getString("_id"));
            }
        });
    }

    public void delete(int phoneNumber, String accountId, final Handler<String> handler) {
        JsonObject criteria = new JsonObject()
                .putNumber("phoneNumber", phoneNumber)
                .putString("accountId", accountId);

        JsonObject query = new JsonObject()
                .putString("action", "delete")
                .putString("collection", COLLECTION_NAME)
                .putObject("matcher", criteria);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(event.body().getString("_id"));
            }
        });
    }

    public void getUserBankAccounts(int phoneNumber, final Handler<ArrayList<CmdModels.BankAccount>> callback) {
        JsonObject criteria = new JsonObject()
                .putNumber("phoneNumber", phoneNumber);

        JsonObject query = new JsonObject();
        query.putString("action", "find");
        query.putString("collection", COLLECTION_NAME);
        query.putObject("matcher", criteria);


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject obj = event.body();

                ArrayList<CmdModels.BankAccount> finalResult = new ArrayList<CmdModels.BankAccount>();

                if (obj.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    if (results != null && results.size() > 0) {

                        for (int i = 0; i < results.size(); i++) {
                            JsonObject model = (JsonObject) results.get(i);
                            finalResult.add(
                                    CmdModels.BankAccount.newBuilder()
                                            .setPhoneNumber(model.getInteger("phoneNumber", 0))
                                            .setAccountId(model.getString("accountId", ""))
                                            .setBankName(model.getString("bankName", ""))
                                            .setOwnerName(model.getString("ownerName", ""))
                                            .setCreatedDate(model.getLong("createdDate", 0L))
                                            .setBankId(model.getString("bankId", ""))
                                            .build()
                            );
                        }
                    } else {
                        callback.handle(null);
                    }

                    callback.handle(finalResult);
                } else {
                    callback.handle(null);
                }
            }
        });
    }

}
