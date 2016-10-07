package com.mservice.momo.avatar;

import com.mservice.momo.vertx.AppConstant;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by nam on 5/14/14.
 */
public class HttpFileDownloadVerticle extends Verticle {
//    public static final String ADDRESS = "HttpFileDownloadVerticle";

    private CloseableHttpClient httpclient = HttpClients.createDefault();

    public static final String CMD_DOWNLOAD_IMAGE = "downloadImage";

    @Override
    public void start() {

        vertx.eventBus().registerLocalHandler(AppConstant.HttpFileDownloadVerticle_ADDRESS, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> requestCommand) {
                container.logger().debug("new cmd: " + requestCommand.body());
                JsonObject cmdBody = requestCommand.body();
                String cmd = cmdBody.getString("cmd");
                if (cmd == null) {
                    requestCommand.reply();
                }

                switch (cmd) {
                    case CMD_DOWNLOAD_IMAGE:
                        downloadImage(requestCommand);
                        break;
                    default:
                        response(requestCommand, -1, "Unknown command: " + cmd);
                }
            }
        });
    }

    /**
     * {
     * *url: "http://www.rebo.com/abc.png"
     * *fileOut; "/tmp/abc.png"
     * maxSize: 1024 (kBs)
     * }
     *
     * @param requestCommand
     */
    private void downloadImage(Message<JsonObject> requestCommand) {
        String url = requestCommand.body().getString("url");
        String fileOut = requestCommand.body().getString("fileOut");

        if (url == null) {
            response(requestCommand, -2, "Missing params: url.");
            return;
        }

        if (fileOut == null) {
            response(requestCommand, -2, "Missing params: fileOut.");
            return;
        }

        int maxSize = requestCommand.body().getInteger("maxSize", 1024); // 1 mB

        HttpGet request = new HttpGet(url);

        try {
            container.logger().info(String.format("Download image(url: %s) > %s", url, fileOut));
            CloseableHttpResponse response = null;
            try {
                response = httpclient.execute(request);
            } catch (IllegalStateException e) {
                container.logger().info(e.getMessage());
            }
            if (response == null) {
                response(requestCommand, 3, "Unknown content type");
                return;
            }

            if (response.getHeaders("Content-Type").length > 0) {
                String contentType = response.getHeaders("Content-Type")[0].getValue();
                container.logger().info(String.format("url(%s) has content-type=", url, contentType));
                if ("image/jpeg".equalsIgnoreCase(contentType) || "image/gif".equalsIgnoreCase(contentType) || "image/png".equalsIgnoreCase(contentType)) {
                    HttpEntity entity = response.getEntity();

                    InputStream is = response.getEntity().getContent();
                    try {
                        OutputStream os = new FileOutputStream(fileOut);
                        try {
                            byte[] buffer = new byte[1024];
                            int currentKbs = 0;
                            for (int n; (n = is.read(buffer)) != -1; ) {
                                currentKbs++;
                                os.write(buffer, 0, n);

                                if (currentKbs > maxSize) {
                                    container.logger().info(String.format("%s is too big.", url));
                                    response(requestCommand, 1, "File is too big.");
                                    break;
                                }
                            }
                            if (currentKbs <= maxSize) {
                                response(requestCommand, 0, "Success");
                            }
                        } finally {
                            os.close();
                        }
                    } finally {
                        is.close();
                    }

                } else {
                    container.logger().info(String.format("%s type is not supported.", url));
                    response(requestCommand, 2, "Unsupported image type");
                }
            } else {
                container.logger().info(String.format("%s is not an image.", url));
                response(requestCommand, 3, "Unknown content type");
            }

            for (Header header : response.getAllHeaders()) {
                container.logger().debug(header);
            }

            System.out.println(response.getStatusLine());
        } catch (ClientProtocolException e) {
            container.logger().error(e.getMessage(), e);
            response(requestCommand, -100, e.getMessage());
        } catch (IOException e) {
            container.logger().error(e.getMessage(), e);
            response(requestCommand, -100, e.getMessage());
        }
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
