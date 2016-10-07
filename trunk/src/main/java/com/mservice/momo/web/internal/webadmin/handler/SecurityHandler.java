package com.mservice.momo.web.internal.webadmin.handler;

import com.mservice.momo.web.AbstractHandler;
import com.mservice.momo.web.HttpRequestContext;
import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

/**
 * Created by ntunam on 4/7/14.
 */
public class SecurityHandler extends AbstractHandler{

    public SecurityHandler(Vertx vertx, Container container) {
        super(vertx, container);
    }

    @Override
    public void handle(HttpRequestContext context) {

    }
}
