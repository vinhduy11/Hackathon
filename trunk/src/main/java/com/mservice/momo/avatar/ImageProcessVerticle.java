package com.mservice.momo.avatar;

import com.mservice.momo.vertx.AppConstant;
import org.imgscalr.Scalr;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.File;
import java.io.IOException;

/**
 * Created by nam on 5/13/14.
 */
public class ImageProcessVerticle extends Verticle {

//    public static final String ADDRESS = "imageProcessVerticle";

    public static final String CMD_SCALE = "scale";


    @Override
    public void start() {

//        StoreWarningTypeDb db = new StoreWarningTypeDb(vertx, container.logger());
//
//        StoreWarningType type = new StoreWarningType();
//        type.name = "Điểm giao dịch quá bựa!";
//        type.typeId = 1;
//
//        db.save(type, new Handler<String>() {
//            @Override
//            public void handle(String s) {
//
//            }
//        });
//
//
//        StoreWarningType type2 = new StoreWarningType();
//        type2.name = "Xóa cmn điểm giao dịch này đi";
//        type2.typeId = 2;
//
//        db.save(type2, new Handler<String>() {
//            @Override
//            public void handle(String s) {
//
//            }
//        });

//        final StoreRateManager manager = new StoreRateManager(vertx, container);
//        for (Integer phone : new Integer[]{979946419, 933000560, 908122456}) {
//            StoreComment comment = new StoreComment();
//            comment.storeId = phone;
//            comment.commenter = 987568815;
//            comment.status = StoreComment.STATUS_NEW;
//            comment.content = "Số của Nam Tester.";
//            comment.star = 3;
//            comment.modifyDate = new Date();
//
//            manager.upsertComment(comment, new Handler<JsonObject>() {
//                @Override
//                public void handle(JsonObject jsonObject) {
//                    System.out.println(jsonObject);
//                    container.logger().error(jsonObject);
//                }
//            });
//        }

//        StoreCommentDb db = new StoreCommentDb(vertx, container.logger());
//        db.getCommentPage(10,);

        System.setProperty("java.awt.headless", "true");

        vertx.eventBus().registerLocalHandler(AppConstant.ImageProcessVerticle_ADDRESS, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> requestCommand) {
                JsonObject cmdBody = requestCommand.body();
                String cmd = cmdBody.getString("cmd");
                if (cmd == null) {
                    requestCommand.reply();
                }

                switch (cmd) {
                    case CMD_SCALE:
                        scaleImage(requestCommand);
                        break;
                    default:
                        response(requestCommand, -1, "Unknown command: " + cmd);
                }
            }
        });

    }

    /**
     * {
     *     *fileIn: "/home/sdf/abc.jpg",
     *     fileOut: "/home/sdf/abc.jpg",
     *     fileOutType: "PNG"|"JPG"
     *     width: 256,
     *     height: 256,
     *     replace: false
     * }
     * errors: 1: missing params
     *          2: IOException
     */
    private void scaleImage(final Message<JsonObject> requestCommand) {
        JsonObject cmdBody = requestCommand.body();
        final String fileIn = cmdBody.getString("fileIn");
        final String fileOut = cmdBody.getString("fileOut", fileIn); // replace the old one.
        final String fileOutType = cmdBody.getString("fileOutType", "PNG");
        final int width = cmdBody.getInteger("width", 100);
        final int height = cmdBody.getInteger("height", 100);
        final boolean replace = cmdBody.getBoolean("replace", false);

        if (fileIn == null) {
            response(requestCommand, 1, "Missing params: fileIn.");
            return;
        }

        vertx.fileSystem().exists(fileIn, new Handler<AsyncResult<Boolean>>() {
            @Override
            public void handle(AsyncResult<Boolean> event) {
                if (!event.result()) {
                    response(requestCommand, 2, "File not found.");
                    return;
                }

                try {
                    BufferedImage inputImage = ImageIO.read(new File(fileIn));
                    BufferedImage outputImage = Scalr.resize(inputImage, width, height);
                    ImageIO.write(outputImage, fileOutType, new File(fileOut));
                    response(requestCommand, 0, "Success");

                } catch (IOException e) {
                    response(requestCommand, 2, e.getMessage());
                }catch (ImagingOpException ie){

                    //linh sua them bat exception nay
                    response(requestCommand, 2, ie.getMessage());
                }catch (Exception ex){

                    //linh sua them bat exception nay
                    response(requestCommand, 2, ex.getMessage());
                }
            }
        });
    }

    public static void response(Message requestCommand, int error, String description) {
        JsonObject replyBody = new JsonObject()
                .putNumber("error", error);
        if (description != null) {
            replyBody.putString("description", description);
        }
        requestCommand.reply(replyBody);
    }

}
