package com.starcor.xulapp.message;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 订阅者对象,包含订阅者和目标方法
 */
public class XulSubscription {

    /**
     * 订阅者对象
     */
    private Reference<Object> _subscriber;
    /**
     * 接受者的方法
     */
    private Method _targetMethod;
    /**
     * 执行事件的线程模型
     */
    private XulThreadMode _threadMode;
    /**
     * 消息类型
     */
    private XulMessage _xulMessage;

    private ConcurrentLinkedQueue<XulMessage> _messageQueue;

    /**
     * 订阅者对象,包含订阅者和目标方法
     */
    public XulSubscription(XulMessage xulMessage, Reference<Object> subscriber,
                           Method method, XulThreadMode threadMode) {
        _subscriber = subscriber;
        _targetMethod = method;
        _threadMode = threadMode;
        _xulMessage = xulMessage;
        _messageQueue = new ConcurrentLinkedQueue<XulMessage>();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_subscriber == null) ? 0 : _subscriber.hashCode());
        result = prime * result + ((_targetMethod == null) ? 0 : _targetMethod.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        XulSubscription other = (XulSubscription) obj;
        if (_subscriber.get() == null) {
            if (other._subscriber.get() != null) {
                return false;
            }
        } else if (!_subscriber.get().equals(other._subscriber.get())) {
            return false;
        }
        if (_targetMethod == null) {
            if (other._targetMethod != null) {
                return false;
            }
        } else if (!_targetMethod.equals(other._targetMethod)) {
            return false;
        }
        return true;
    }


    public Object getSubscriber() {
        return _subscriber.get();
    }

    public void setSubscriber(Reference<Object> subscriber) {
        _subscriber = subscriber;
    }

    public Method getTargetMethod() {
        return _targetMethod;
    }

    public void setTargetMethod(Method targetMethod) {
        _targetMethod = targetMethod;
    }

    public XulThreadMode getXulThreadMode() {
        return _threadMode;
    }

    public void setXulThreadMode(XulThreadMode xulThreadMode) {
        _threadMode = xulThreadMode;
    }

    public XulMessage getMessageType() {
        return _xulMessage;
    }

    public void addXulMessage(XulMessage xulMessage) {
        if (xulMessage != null) {
            _messageQueue.add(new XulMessage(xulMessage));
        }
    }

    public XulMessage getXulMessage() {
        return _messageQueue.peek();
    }

    public void clearXulMessages() {
        _messageQueue.clear();
    }

    public boolean isInvalid() {
        return _subscriber.get() == null || _messageQueue.isEmpty();
    }

    /**
     * 在特定线程中处理消息
     */
    public void handleMessage() {
        if (isInvalid()) {
            return;
        }

        try {
            // 执行
            _targetMethod.setAccessible(true);
            XulMessage xulMessage = getXulMessage();
            Object o = _subscriber.get();
            if (o != null) {
                _targetMethod.invoke(o, xulMessage.getData());
            }
            if (xulMessage.getRepeat() <= 1) {
                _messageQueue.poll();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
