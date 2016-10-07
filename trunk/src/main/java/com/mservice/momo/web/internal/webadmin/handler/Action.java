package com.mservice.momo.web.internal.webadmin.handler;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {
	String path();
    Role[] roles() default {};
}
