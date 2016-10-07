package com.mservice.momo.web.internal.webadmin.handler;

import com.mservice.momo.web.AbstractHandler;
import com.mservice.momo.web.HttpRequestContext;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by ntunam on 4/7/14.
 */
public class ControllerMapper extends AbstractHandler {
    public static final String SESSION_ROLES = "roles";
    public static final String COOKIE_SESSION_ID = "sessionId";

    public static final String RESOURCE_FOLDER = "webadmin";

    private Map<String, ActionContent> actions;

    public ControllerMapper(Vertx vertx, Container container) {
        super(vertx, container);
        actions = new HashMap<String, ActionContent>();
    }


    public void addController(Object controller) {
        Method[] methods = controller.getClass().getMethods();
        for (Method method : methods) {
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Action) {
                    Action action = (Action) annotation;
                    String path = action.path();
                    Set<Role> roles = new HashSet<Role>(Arrays.asList(action.roles()));
                    ActionContent content = new ActionContent(path, method, roles, controller);
                    actions.put(path, content);
                }
            }
        }
    }


    private boolean hasAuthority(HttpRequestContext context, ActionContent action) {
        JsonArray roles = context.getSession().getArray(SESSION_ROLES, new JsonArray());
        if (action.getRoles().size() == 0)
            return true;
        for (int i = 0; i < roles.size(); i++) {
            if (action.getRoles().contains(Role.valueOf(roles.get(i).toString()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handle(final HttpRequestContext context) {
        final String requestPath = context.getRequest().path();
        logger.info("[" + context.getRequest().method() + "] " + requestPath);
        ActionContent action = actions.get(requestPath);
        if (action != null) {
            if (hasAuthority(context, action)) {
                try {
                    action.getMethod().invoke(action.getInstance(), context, new Handler<Object>() {
                        @Override
                        public void handle(Object result) {
//                        if (result instanceof JsonObject) {
                            context.getResponseContext().put(HttpRequestContext.RESPONSE_CONTEXT_MODEL, result);
//                        }
                            fireNextHandler(context);
                        }
                    });
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else {
                JsonObject json = new JsonObject()
                        .putNumber("error", -1000)
                        .putString("desc", "Access denied!")
                        .putString("roles", action.getRoles().toString());
                context.getResponseContext().put(HttpRequestContext.RESPONSE_CONTEXT_MODEL, json);
                fireNextHandler(context);
            }
            return;
        }

        final String resourceFile = RESOURCE_FOLDER + requestPath;
        vertx.fileSystem().exists(resourceFile, new Handler<AsyncResult<Boolean>>() {
            @Override
            public void handle(AsyncResult<Boolean> event) {
                if(event.result() == true) {
                    context.getRequest().response().sendFile(resourceFile);
                    return;
                }
                context.getResponseContext().put(HttpRequestContext.RESPONSE_CONTEXT_MODEL, "ERROR 404: Not found!");
                fireNextHandler(context);
            }
        });
    }
}
