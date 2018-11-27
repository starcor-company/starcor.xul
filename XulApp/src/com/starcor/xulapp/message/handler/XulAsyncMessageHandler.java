package com.starcor.xulapp.message.handler;

import com.starcor.xulapp.message.XulMessage;
import com.starcor.xulapp.message.XulSubscription;
import com.starcor.xulapp.utils.XulLog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * 消息的异步处理,将消息的处理函数执行在子线程中
 */
public class XulAsyncMessageHandler implements XulMessageHandler {

    /**
     * 消息分发线程
     */
    private final ScheduledExecutorService _dispatcherService =
            Executors.newSingleThreadScheduledExecutor();

    /**
     * 消息处理器
     */
    private XulMessageHandler _messageHandler = new XulDefaultMessageHandler();

    /**
     * 对象锁
     */
    private Object _waitLock = new Object();

    /**
     * 将订阅的函数执行在异步线程中
     */
    @Override
    public void handleMessage(final XulSubscription subscription) {
        XulMessage xulMessage = subscription.getXulMessage();
        if (xulMessage == null) {
            return;
        }

        _dispatcherService.schedule(new Runnable() {
            @Override
            public void run() {
                if (subscription.isInvalid()) {
                    // subscriber has dead or message has been canceled.
                    return;
                }

                XulMessage xulMessage = subscription.getXulMessage();

                if (xulMessage == null) {
                    return;
                }

                int repeat = xulMessage.getRepeat();
                if (repeat <= 0) {
                    // 不需要执行
                    return;
                }

                _messageHandler.handleMessage(subscription);
                if (--repeat > 0) {
                    xulMessage.setRepeat(repeat);
                    _dispatcherService.schedule(
                            this, xulMessage.getInterval(), TimeUnit.MILLISECONDS);
                } else {
                    if (xulMessage.getIsSyncMessage()) {
                        // 消息处理结束，释放对象锁
                        synchronized (_waitLock) {
                            _waitLock.notify();
                        }
                    }
                }
            }
        }, xulMessage.getDelay(), TimeUnit.MILLISECONDS);

        if (xulMessage.getIsSyncMessage()) {
            waitHandleFinished();
        }
    }

    private void waitHandleFinished() {
        synchronized (_waitLock) {
            try {
                _waitLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
