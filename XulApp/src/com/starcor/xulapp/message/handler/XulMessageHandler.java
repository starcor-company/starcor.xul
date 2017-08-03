package com.starcor.xulapp.message.handler;


import com.starcor.xulapp.message.XulSubscription;

/**
 * 消息处理接口
 */
public interface XulMessageHandler {

    /**
     * 处理消息
     *
     * @param subscription 订阅对象
     */
    void handleMessage(XulSubscription subscription);
}
