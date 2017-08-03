package com.starcor.xulapp.message;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息接收函数的注解类,运用在函数上
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface XulSubscriber {

    /**
     * 消息的tag,类似于BroadcastReceiver中的Action,消息的标识符
     */
    int tag() default XulMessage.DEFAULT_TAG;
    
    /**
     * 消息执行的线程,默认为主线程
     */
    XulThreadMode mode() default XulThreadMode.MAIN;
}
