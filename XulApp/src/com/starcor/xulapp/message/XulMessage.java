package com.starcor.xulapp.message;

import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p> 该类是描述一个函数唯一性的对象，参数类型、tag两个条件保证了对象的唯一性.通过该类的对象来查找注册了相应类型和tag的所有订阅者{@see XulSubscription},
 * 并且在接到消息时调用所有订阅者对应的函数.
 */
public class XulMessage {

    protected final String TAG = getClass().getSimpleName();

    /**
     * 默认的消息数据
     */
    public static final Object DEFAULT_DATA = new Object();
    /**
     * 默认的tag
     */
    public static final int DEFAULT_ID = 0;
    /**
     * 默认的tag
     */
    public static final int DEFAULT_TAG = -1;
    /**
     * 默认的延后时间
     */
    public static final long DEFAULT_DELAY = 0;
    /**
     * 默认的发送次数
     */
    public static final int DEFAULT_REPEAT = 1;
    /**
     * 默认的重复发送间隔时间
     */
    public static final long DEFAULT_INTERVAL = 0;

    /**
     * 消息id
     */
    private int _id = DEFAULT_ID;

    /**
     * 消息tag
     */
    private int _tag = DEFAULT_TAG;

    /**
     * 参数数据
     */
    private Object _data = DEFAULT_DATA;

    /**
     * 参数类型
     */
    private Class<?> _paramClass = DEFAULT_DATA.getClass();

    /**
     * 消息同步类型,默认为异步消息
     */
    private boolean _isSyncMessage = false;

    /**
     * 延后发送时间
     */
    private long _delay = 0;

    /**
     * 发送次数
     */
    private int _repeat = 1;

    /**
     * 重复发送间隔时间
     */
    private long _interval = 0;

    private XulMessage() {
        _id = obtainMessageId();
    }

    /**
     * 仅作为message type使用
     */
    public XulMessage(int aTag, Class<?> aClass) {
        _id = obtainMessageId();
        _paramClass = aClass;
        _tag = aTag;
    }

    public XulMessage(int tag, Object data, long delay, int repeat, long interval) {
        _id = obtainMessageId();
        setData(data);
        _tag = tag;
        _delay = delay;
        _repeat = repeat;
        _interval = interval;
    }

    public XulMessage(XulMessage xulMessage) {
        _tag = xulMessage._tag;
        _paramClass = xulMessage._paramClass;
        copyData(xulMessage);
    }

    private static AtomicInteger _messageId = new AtomicInteger(0);

    public static int obtainMessageId() {
        return _messageId.getAndIncrement();
    }

    public static XulMessage obtainMessage() {
        return new XulMessage();
    }

    @Override
    public String toString() {
        return "XulMessage [tag=" + _tag + ", paramClass=" + _paramClass.getName() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_paramClass == null) ? 0 : _paramClass.hashCode());
        result = prime * result + _tag;
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
        XulMessage other = (XulMessage) obj;
        if (_tag != other._tag) {
            return false;
        }

        if (_paramClass == null) {
            if (other._paramClass != null) {
                return false;
            }
        } else if (!_paramClass.equals(other._paramClass)) {
            return false;
        }

        return true;
    }

    public boolean getIsSyncMessage() {
        return _isSyncMessage;
    }

    public void setIsSyncMessage(boolean isSyncMessage) {
        _isSyncMessage = isSyncMessage;
    }

    public int getTag() {
        return _tag;
    }

    public void setTag(int tag) {
        _tag = tag;
    }

    public long getDelay() {
        return _delay;
    }

    public void setDelay(long delay) {
        _delay = delay;
    }

    public int getRepeat() {
        return _repeat;
    }

    public void setRepeat(int repeat) {
        _repeat = repeat;
    }

    public long getInterval() {
        return _interval;
    }

    public void setInterval(long interval) {
        _interval = interval;
    }

    public Class<?> getParamClass() {
        return _paramClass;
    }

    public void setParamClass(Class<?> paramClass) {
        _paramClass = paramClass;
    }

    public Object getData() {
        return _data;
    }

    public void setData(Object data) {
        if (data == null) {
            Log.d(TAG, "The data object is null");
            data = new Object();
        }

        _data = data;
        _paramClass = data.getClass();
    }

    protected void copyData(XulMessage message) {
        _isSyncMessage = message._isSyncMessage;
        _data = message._data;
        _delay = message._delay;
        _repeat = message._repeat;
        _interval = message._interval;
    }
}
