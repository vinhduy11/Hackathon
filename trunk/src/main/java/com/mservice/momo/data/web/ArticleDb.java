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
 * Created by ntunam on 3/19/14.
 */
public class ArticleDb {
    private EventBus eventBus;

    public ArticleDb(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void save(boolean active, long postDate, String title, String summary, String detail, final Handler<String> handler) {
        JsonObject model = new JsonObject()
                .putBoolean("active", active)
                .putNumber("postDate", postDate)
                .putString("title", title)
                .putString("summary", summary)
                .putString("detail", detail);

        JsonObject query = new JsonObject()
                .putString("action", "save")
                .putString("collection", "web_article")
                .putObject("document", model);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(event.body().getString("_id"));
            }
        });
    }

    public void update(String id, boolean active, long postDate, String title, String summary, String detail, final Handler<String> handler) {
        JsonObject model = new JsonObject()
                .putBoolean("active", active)
                .putNumber("postDate", postDate)
                .putString("title", title)
                .putString("summary", summary)
                .putString("detail", detail);

        JsonObject criteria = new JsonObject()
                .putString("_id", id);

        JsonObject query = new JsonObject()
                .putString("action", "update")
                .putString("collection", "web_article")
                .putObject("criteria", criteria)
                .putObject("objNew", model);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(event.body().getString("_id"));
            }
        });
    }

    public void delete(String id, final Handler<String> handler) {
        JsonObject criteria = new JsonObject()
                .putString("_id", id);

        JsonObject query = new JsonObject()
                .putString("action", "delete")
                .putString("collection", "web_article")
                .putObject("matcher", criteria);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(event.body().getString("_id"));
            }
        });
    }

    public void getPage(int pageSize, int pageNumber, final Handler<ArrayList<CmdModels.Article>> callback) {
        JsonObject query = new JsonObject();
        query.putString("action", "find");
        query.putString("collection", "web_article");

        //sort by _id desc
        JsonObject sort = new JsonObject();
        sort.putNumber("postDate", -1);
        query.putObject("sort", sort);

        int skip = (pageNumber - 1) * pageSize;
        int records = pageSize;

        query.putNumber("skip", skip);
        query.putNumber("limit", records);

        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonObject obj = event.body();

                ArrayList<CmdModels.Article> finalResult = new ArrayList<CmdModels.Article>();

                if (obj.getString("status", "ko").equalsIgnoreCase("ok")) {
                    JsonArray results = event.body().getArray("results");

                    if (results != null && results.size() > 0) {

                        for (int i = 0; i < results.size(); i++) {
                            JsonObject model = (JsonObject) results.get(i);
                            finalResult.add(
                                    CmdModels.Article.newBuilder()
                                            .setId(model.getString("_id"))
                                            .setPostDate(model.getLong("postDate"))
                                            .setTitle(model.getString("title"))
                                            .setSummary(model.getString("summary"))
                                            .setDetail(model.getString("detail"))
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

    public void getCount(final Handler<Integer> callback) {
        JsonObject query = new JsonObject();
        query.putString("action", "count");
        query.putString("collection", "web_article");


        eventBus.send(AppConstant.MongoVerticle_ADDRESS, query, new Handler<Message<JsonObject>>(){
            @Override
            public void handle(Message<JsonObject> event) {
                callback.handle(event.body().getInteger("count"));
            }
        });
    }

}
