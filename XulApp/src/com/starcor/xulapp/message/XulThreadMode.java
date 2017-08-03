package com.starcor.xulapp.message;

/**
 * 消息发布的线程模式枚举
 */
public enum XulThreadMode {
    /**
     * 在UI线程执行
     */
    MAIN,
    /**
     * 在发布线程执行
     */
    POST,
    /**
     * 在一个子线程中执行
     */
    ASYNC
}
