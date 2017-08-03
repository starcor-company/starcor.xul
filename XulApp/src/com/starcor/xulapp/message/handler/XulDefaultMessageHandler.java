package com.starcor.xulapp.message.handler;


import com.starcor.xulapp.message.XulSubscription;

/**
 * 消息在哪个线程post,消息的接收就在哪个线程
 */
public class XulDefaultMessageHandler implements XulMessageHandler {

    /**
     * handle the message
     */
    @Override
    public void handleMessage(XulSubscription subscription) {
        if (subscription != null) {
            subscription.handleMessage();
        }
    }
}
