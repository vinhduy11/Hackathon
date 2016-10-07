package com.mservice.momo.web.internal.webadmin.handler;

import com.mservice.momo.web.AbstractHandler;
import com.mservice.momo.web.HttpRequestContext;
import httl.Engine;
import httl.Template;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ntunam on 4/8/14.
 */
public class RenderHandler extends AbstractHandler {

    private Map<String, Template> templates;

    public RenderHandler(Vertx vertx, Container container) {
        super(vertx, container);

        templates = new HashMap<>();
    }

    private Template getTemplate(String name) {
        if (!name.startsWith("/")) {
            name = "/" + name;
        }

        Template template = templates.get(name);
        try {
            if (template==null) {
                template = Engine.getEngine().getTemplate(ControllerMapper.RESOURCE_FOLDER + name);
            }
            templates.put(name, template);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return template;
    }

    @Override
    public void handle(HttpRequestContext context) {
        HttpServerRequest request = context.getRequest();


        if(context.getCookie().isChanged()) {
            request.response().putHeader("Set-cookie",context.getCookie().toString() + "; path=/");
//            request.response().putHeader("Set-cookie", context.getCookie().toString());
        }


        Object obj = context.getResponseContext().get(HttpRequestContext.RESPONSE_CONTEXT_MODEL);
        if (obj == null) {
            request.response().end();
        } else if (obj instanceof JsonObject) {
            JsonObject content = (JsonObject) obj;
            request.response().putHeader("Content-Type", "application/json; charset=UTF-8");
            request.response().putHeader("access-control-allow-origin", "*");
            request.response().end(content.toString());
        } else if (obj instanceof String) {
            request.response().putHeader("Content-Type", "text/html; charset=UTF-8");
            request.response().putHeader("access-control-allow-origin", "*");
            request.response().end(obj.toString());
        } else if (obj instanceof JsonArray) {
            request.response().putHeader("Content-Type", "application/json; charset=UTF-8");
            request.response().putHeader("access-control-allow-origin", "*");
            request.response().end(String.valueOf(obj));
        } else if (obj instanceof Map) {
            request.response().putHeader("Content-Type", "text/html; charset=UTF-8");
            request.response().putHeader("access-control-allow-origin", "*");

            Map<String, Object> map = (Map) obj;

            String cmd = (String) map.get("$cmd");
            if ("redirect".equalsIgnoreCase(cmd)) {
                String target = (String) map.get("$target");
                String html = "<html><head>    <meta http-equiv='refresh' content='0; url=@target' />    <head></html>";
                html = html.replace("@target", target);

                request.response().putHeader("Content-Type", "text/html; charset=UTF-8");
                request.response().putHeader("access-control-allow-origin", "*");
                request.response().end(html);
                return;
            }

            Template template = null;
            try {
                String templateName = (String) map.get("$template");
                template = getTemplate(templateName);
            }catch (ClassCastException e) {
                container.logger().error("input $template is not match.");
            }
            if (template == null) {
                request.response().end(map.entrySet().toString());
                return;
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream(2048);

            try {
                template.render (map, os);
                request.response().end(os.toString("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
                request.response().end();
            } catch (ParseException e) {
                e.printStackTrace();
                request.response().end();
            }
        } else {
            request.response().putHeader("Content-Type", "text/html; charset=UTF-8");
            request.response().end("RenderHandler exception: No supported type " + obj.getClass().getName());
        }

//        fireNextHandler(context);
    }
}
