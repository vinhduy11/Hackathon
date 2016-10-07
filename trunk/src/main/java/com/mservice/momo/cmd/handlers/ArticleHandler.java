package com.mservice.momo.cmd.handlers;

import com.mservice.momo.cmd.CommandContext;
import com.mservice.momo.cmd.CommandHandler;
import com.mservice.momo.cmd.MainDb;
import com.mservice.momo.msg.CmdModels;
import com.mservice.momo.msg.MomoCommand;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;

/**
 * Created by ntunam on 3/19/14.
 */
public class ArticleHandler extends CommandHandler {
    public ArticleHandler(MainDb mainDb, Vertx vertx, Container container, JsonObject config) {
        super(mainDb, vertx, container, config);
    }

    public void modifyArticle(final CommandContext context) {
        CmdModels.ModifyArticle cmd = (CmdModels.ModifyArticle) context.getCommand().getBody();
        final CmdModels.Article article = cmd.getArticle();
        //validation
        if (article != null) {
            //article validation;
        }

        //business
        switch (cmd.getCommand()) {
            case ADD:
                mainDb.articleDb.save(cmd.getArticle().getActive(), cmd.getArticle().getPostDate(), cmd.getArticle().getTitle(), cmd.getArticle().getSummary(), cmd.getArticle().getDetail(), new Handler<String>() {
                    @Override
                    public void handle(String objId) {
                        CmdModels.ModifyArticleReply replyBody = CmdModels.ModifyArticleReply.newBuilder()
                                .setResult(CmdModels.ModifyArticleReply.ResultCode.SUCCESS)
                                .setArticle(CmdModels.Article.newBuilder().setId(objId).build())
                                .build();
                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.MODIFY_ARTICLE_REPLY, replyBody);
                        context.reply(replyCommand);
                    }
                });
                break;
            case UPDATE:
                mainDb.articleDb.update(article.getId(), article.getActive(), article.getPostDate(), article.getTitle(), article.getSummary(), article.getDetail(), new Handler<String>() {
                    @Override
                    public void handle(String objId) {
                        CmdModels.ModifyArticleReply replyBody = CmdModels.ModifyArticleReply.newBuilder()
                                .setResult(CmdModels.ModifyArticleReply.ResultCode.SUCCESS)
                                .setArticle(CmdModels.Article.newBuilder().setId(objId).build())
                                .build();
                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.MODIFY_ARTICLE_REPLY, replyBody);
                        context.reply(replyCommand);
                    }
                });
                break;
            case DELETE:
                mainDb.articleDb.delete(article.getId(), new Handler<String>() {
                    @Override
                    public void handle(String objId) {
                        CmdModels.ModifyArticleReply replyBody = CmdModels.ModifyArticleReply.newBuilder()
                                .setResult(CmdModels.ModifyArticleReply.ResultCode.SUCCESS)
                                .build();
                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.MODIFY_ARTICLE_REPLY, replyBody);
                        context.reply(replyCommand);
                    }
                });
                break;
            default:
                throw new IllegalAccessError("Unknown command type");
        }
    }

    public void getArticlePage(final CommandContext context) {
        final CmdModels.GetArticlePage cmd = (CmdModels.GetArticlePage) context.getCommand().getBody();

        //validation
        if (cmd.getPageNumber() < 1) {
            context.replyError(-1, "Page number must be greater than 0.");
            return;
        }
        if (cmd.getPageSize() < 1 || cmd.getPageSize() > 50) {
            context.replyError(-1, "Page size must be in the range of [1->50].");
            return;
        }

        //config
        mainDb.articleDb.getCount(new Handler<Integer>() {
            @Override
            public void handle(final Integer count) {
                if (count == 0) {
                    CmdModels.GetArticlePageReply.Builder builder = CmdModels.GetArticlePageReply.newBuilder();
                    builder.setPageCount(0);
                    MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_ARTICLE_PAGE_REPLY, builder.build());
                    context.reply(replyCommand);
                    return;
                }

                mainDb.articleDb.getPage(cmd.getPageSize(), cmd.getPageNumber(), new Handler<ArrayList<CmdModels.Article>>() {
                    @Override
                    public void handle(ArrayList<CmdModels.Article> result) {
                        CmdModels.GetArticlePageReply.Builder builder = CmdModels.GetArticlePageReply.newBuilder();

                        int s = count / cmd.getPageSize();
                        if (count % cmd.getPageSize() > 0)
                            s++;
                        builder.setPageCount(s);
                        if (result != null) {
                            for (int i = 0; i < result.size(); i++) {
                                builder.addArticles(result.get(i));
                            }
                        }

                        MomoCommand replyCommand = new MomoCommand(CmdModels.CommandType.GET_ARTICLE_PAGE_REPLY, builder.build());
                        context.reply(replyCommand);
                    }
                });
            }
        });
    }
}
