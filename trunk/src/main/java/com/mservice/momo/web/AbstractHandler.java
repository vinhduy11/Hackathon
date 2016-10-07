package com.mservice.momo.web;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

/**
 * Created by ntunam on 4/7/14.
 */
public abstract class AbstractHandler {
    protected Vertx vertx;
    protected Container container;
    protected Logger logger;

    private AbstractHandler nextHandler;

    protected AbstractHandler(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;
        this.logger = container.logger();
    }

    public AbstractHandler getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(AbstractHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void fireNextHandler(HttpRequestContext context) {
        if (nextHandler == null)
            return;
        nextHandler.handle(context);
    }



    public abstract void handle(final HttpRequestContext context);
}
