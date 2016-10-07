package com.mservice.momo.web.internal.webadmin.handler;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by ntunam on 4/8/14.
 */
public class ActionContent {
    private String path;
    private Method method;
    private Set<Role> roles;
    private Object instance;

    public ActionContent() {
    }

    public ActionContent(String path, Method method, Set<Role> roles, Object instance) {
        this.path = path;
        this.method = method;
        this.roles = roles;
        this.instance = instance;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }
}
