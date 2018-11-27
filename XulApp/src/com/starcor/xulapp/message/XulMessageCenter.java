package com.starcor.xulapp.message;

import com.starcor.xulapp.message.handler.XulAsyncMessageHandler;
import com.starcor.xulapp.message.handler.XulDefaultMessageHandler;
import com.starcor.xulapp.message.handler.XulMessageHandler;
import com.starcor.xulapp.message.handler.XulUiThreadMessageHandler;
import com.starcor.xulapp.message.matchpolicy.XulDefaultMatchPolicy;
import com.starcor.xulapp.message.matchpolicy.XulMatchPolicy;
import com.starcor.xulapp.utils.XulLog;

import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <p> MessageCenter是此框架的核心类,也是用户的入口类.它存储了用户注册的订阅者信息和方法, 消息类型和该消息对应的tag标识一个种类的消息{@see
 * XulMessage},每一种消息对应有一个或者多个订阅者{@see XulSubscription} ,订阅者中的订阅函数通过{@see
 * Subcriber}注解来标识tag和线程模型,这样使得用户体检较为友好,代码也更加整洁. <p> 用户需要在发布消息前通过@{@see
 * #register(Object)}方法将订阅者注册到MessageCenter中,MessageCenter会解析该订阅者中使用了 {@see
 * Subcriber}标识的函数,并且将它们以{@see XulMessage}为key,以{@see XulSubscription} 列表为value存储在map中.
 * 当用户post一个消息时通过消息到map中找到对应的订阅者,然后按照订阅函数的线程模型将函数执行在对应的线程中. <p> 最后在不在需要订阅消息时,应该调用{@see
 * #unregister(Object)}函数注销该对象,避免内存泄露! 例如在Activity或者Fragment的onDestory函数中注销对Activity或者Fragment的订阅.
 * <p> 注意 : 如果发布的消息的参数类型是订阅的消息参数的子类,订阅函数默认也会被执行。例如你在订阅函数中订阅的是List<String>类型的消息,
 * 但是在发布时发布的是ArrayList<String>的消息, 因此List<String>是一个泛型抽象,而ArrayList<String>才是具体的实现
 * ,因此这种情况下订阅函数也会被执行。如果你需要订阅函数能够接收到的消息类型必须严格匹配 ,你可以构造一个MessageCenterConfig对象,
 * 然后设置MatchPolicy然后在使用消息总线之前使用该MessageCenterConfig来初始化消息总线. <code> MessageCenterConfig config = new
 * MessageCenterConfig(); config.setMatchPolicy(new XulStrictMatchPolicy());
 * XulMessageCenter.getDefault().initWithConfig(config); </code>
 */
public final class XulMessageCenter {

    private static final String TAG = XulMessageCenter.class.getSimpleName();

    /**
     * default descriptor
     */
    private static final String DESCRIPTOR = TAG;

    /**
     * The Default XulMessageCenter instance
     */
    private static volatile XulMessageCenter _defaultCenter;

    /**
     * XulMessage-Subcriptions map
     */
    private final Map<XulMessage, CopyOnWriteArrayList<XulSubscription>> _subscriberMap =
            new ConcurrentHashMap<XulMessage, CopyOnWriteArrayList<XulSubscription>>();

    /**
     * 订阅者引用队列，用于自动注销无用的订阅者
     */
    private final ReferenceQueue<Object> _subscriberReferenceQueue = new ReferenceQueue<Object>();

    /**
     * 订阅者自动清理服务
     */
    private final ScheduledExecutorService _cleanService =
            Executors.newSingleThreadScheduledExecutor();
    private static final int CLEAN_UP_INTERVAL = 1; // 单位为分钟

    /**
     * the thread local message queue, every single thread has it's own queue.
     */
    private final ThreadLocal<Queue<XulMessage>> _localMessages =
            new ThreadLocal<Queue<XulMessage>>() {
                protected Queue<XulMessage> initialValue() {
                    return new ConcurrentLinkedQueue<XulMessage>();
                }
            };

    /**
     * the message dispatcher
     */
    private final MessageDispatcher _dispatcher = new MessageDispatcher();

    /**
     * the subscriber method hunter, find all of the subscriber's methods annotated with @Subcriber
     */
    private final XulSubscriberMethodHunter _methodHunter = new XulSubscriberMethodHunter(
            _subscriberMap, _subscriberReferenceQueue);

    /**
     * 消息总线描述符
     */
    private String _desc = DESCRIPTOR;

    /**
     * 粘性消息容器
     */
    private final List<XulMessage> _stickyMessages = Collections.synchronizedList(
            new LinkedList<XulMessage>());

    /**
     * private Constructor
     */
    private XulMessageCenter() {
        this(DESCRIPTOR);
    }

    /**
     * constructor with desc
     *
     * @param desc the descriptor of XulMessageCenter
     */
    public XulMessageCenter(String desc) {
        _desc = desc;

        prepareAutoCleanUp();
    }

    private void prepareAutoCleanUp() {
        _cleanService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    // 等待并清空所有已入队列的引用
                    while (_subscriberReferenceQueue.poll() != null) {
                    }

                    // 执行本次清理操作
                    synchronized (this) {
                        _methodHunter.cleanupInvalidMethods();
                    }
                    XulLog.i(TAG, "Auto cleanup finished");
                } catch (Exception e)  {
                    XulLog.e(TAG, "Cleanup exception!!!", e);
                }
            }
        }, CLEAN_UP_INTERVAL, CLEAN_UP_INTERVAL, TimeUnit.MINUTES);
    }

    /**
     * 返回默认的 message center
     */
    public static XulMessageCenter getDefault() {
        if (_defaultCenter == null) {
            synchronized (XulMessageCenter.class) {
                if (_defaultCenter == null) {
                    _defaultCenter = new XulMessageCenter();
                }
            }
        }
        return _defaultCenter;
    }

    /**
     * 关闭 Message center，调用此方法后若要再次使用对应的 Message Center则需要重新创建。
     */
    public void close() {
        _cleanService.shutdownNow();
        _localMessages.get().clear();
        _stickyMessages.clear();

        for (XulMessage xulMessage : _subscriberMap.keySet()) {
            cancel(xulMessage);
        }
        _subscriberMap.clear();
    }

    /**
     * register a subscriber into the _subscriberMap, the key is subscriber's method's name and tag
     * which annotated with {@see Subcriber}, the value is a list of XulSubscription.
     *
     * @param subscriber the target subscriber
     */
    public void register(Object subscriber) {
        if (subscriber == null) {
            return;
        }

        synchronized (this) {
            _methodHunter.findSubscribeMethods(subscriber);
        }

        // 处理sticky消息
        _dispatcher.dispatchStickyMessages(subscriber);
    }

    /**
     * @param subscriber
     */
    public void unregister(Object subscriber) {
        if (subscriber == null) {
            return;
        }
        synchronized (this) {
            _methodHunter.removeMethodsFromMap(subscriber);
        }
    }

    /**
     * post a message
     */
    public void post(XulMessage xulMessage) {
        _localMessages.get().offer(xulMessage);
        _dispatcher.dispatchMessages();
    }

    /**
     * post a null message
     */
    public void post() {
        post(XulMessage.DEFAULT_TAG, XulMessage.DEFAULT_DATA, XulMessage.DEFAULT_DELAY,
             XulMessage.DEFAULT_REPEAT, XulMessage.DEFAULT_INTERVAL);
    }

    /**
     * post a message with tag
     */
    public void post(int tag) {
        post(tag, XulMessage.DEFAULT_DATA, XulMessage.DEFAULT_DELAY,
             XulMessage.DEFAULT_REPEAT, XulMessage.DEFAULT_INTERVAL);
    }

    /**
     * post a message with data
     */
    public void post(Object message) {
        post(XulMessage.DEFAULT_TAG, message, XulMessage.DEFAULT_DELAY,
             XulMessage.DEFAULT_REPEAT, XulMessage.DEFAULT_INTERVAL);
    }

    /**
     * 发布消息
     *
     * @param message 要发布的消息
     * @param tag     消息的tag, 类似于BroadcastReceiver的action
     */
    public void post(int tag, Object message) {
        post(tag, message, XulMessage.DEFAULT_DELAY, XulMessage.DEFAULT_REPEAT,
             XulMessage.DEFAULT_INTERVAL);
    }

    public void post(int tag, Object message, long delay) {
        post(tag, message, delay, XulMessage.DEFAULT_REPEAT, XulMessage.DEFAULT_INTERVAL);
    }

    public void post(int tag, Object message, int repeat, long interval) {
        post(tag, message, XulMessage.DEFAULT_DELAY, repeat, interval);
    }

    public void post(int tag, Object message, long delay, int repeat, long interval) {
        XulMessage xulMessage = new XulMessage(tag, message, delay, repeat, interval);
        post(xulMessage);
    }

    /**
     * 取消发送消息
     */
    public void cancel(int tag, Object message) {
        Class<?> msgClass = (message == null ? Object.class : message.getClass());
        _dispatcher.cancelMessage(new XulMessage(tag, msgClass));
    }

    /**
     * 取消发送给定消息
     */
    public void cancel(XulMessage xulMessage) {
        _dispatcher.cancelMessage(xulMessage);
    }

    /**
     * 同步发送一条消息
     */
    public void send(XulMessage xulMessage) {
        xulMessage.setIsSyncMessage(true);
        post(xulMessage);
    }

    public void send(int tag, Object message, long delay, int repeat, long interval) {
        XulMessage xulMessage = new XulMessage(tag, message, delay, repeat, interval);
        send(xulMessage);
    }

    /**
     * 发布Sticky消息
     */
    public void postSticky(XulMessage xulMessage) {
        // 避免重复添加stick事件
        if (!_stickyMessages.contains(xulMessage)) {
            _stickyMessages.add(xulMessage);
        }
        post(xulMessage);
    }

    /**
     * 发布Sticky空消息，id为MessageType.DEFAULT_TAG，message为MessageType.DEFAULT_DATA
     */
    public void postSticky() {
        postSticky(XulMessage.DEFAULT_TAG, XulMessage.DEFAULT_DATA);
    }

    /**
     * 发布Sticky消息,id为MessageType.DEFAULT_TAG
     */
    public void postSticky(Object message) {
        postSticky(XulMessage.DEFAULT_TAG, message);
    }

    /**
     * 发布含有tag的Sticky消息
     *
     * @param message 消息
     * @param tag     消息tag
     */
    public void postSticky(int tag, Object message) {
        if (message == null) {
            XulLog.i(TAG, "The message object is null");
            message = new Object();
        }

        XulMessage xulMessage = XulMessage.obtainMessage();
        xulMessage.setTag(tag);
        xulMessage.setData(message);

        postSticky(xulMessage);
    }

    public void removeStickyMessage(Class<?> messageClass) {
        removeStickyMessage(XulMessage.DEFAULT_TAG, messageClass);
    }

    /**
     * 移除Sticky消息
     */
    public void removeStickyMessage(int tag, Class<?> messageClass) {
        Iterator<XulMessage> iterator = _stickyMessages.iterator();
        while (iterator.hasNext()) {
            XulMessage xulMessage = iterator.next();
            if ((xulMessage.getTag() == tag) && xulMessage.getParamClass().equals(messageClass)) {
                iterator.remove();
            }
        }
    }

    /**
     * 移除Sticky消息
     */
    public void removeStickyMessage(XulMessage msg) {
        _stickyMessages.remove(msg);
    }

    public List<XulMessage> getStickyMessages() {
        return _stickyMessages;
    }

    /**
     * 设置订阅函数匹配策略
     *
     * @param policy 匹配策略
     */
    public void setMatchPolicy(XulMatchPolicy policy) {
        _dispatcher._matchPolicy = policy;
    }

    /**
     * 设置执行在UI线程的消息处理器
     */
    public void setUIThreadEventHandler(XulMessageHandler handler) {
        _dispatcher._uiThreadHandler = handler;
    }

    /**
     * 设置执行在post线程的消息处理器
     */
    public void setPostThreadHandler(XulMessageHandler handler) {
        _dispatcher._postThreadHandler = handler;
    }

    /**
     * 设置执行在异步线程的消息处理器
     */
    public void setAsyncEventHandler(XulMessageHandler handler) {
        _dispatcher._asyncThreadHandler = handler;
    }

    /**
     * 返回订阅map
     */
    public Map<XulMessage, CopyOnWriteArrayList<XulSubscription>> getSubscriberMap() {
        return _subscriberMap;
    }

    /**
     * 获取等待处理的消息队列
     */
    public Queue<XulMessage> getEventQueue() {
        return _localMessages.get();
    }

    /**
     * clear the messages and subcribers map
     */
    public synchronized void clear() {
        _localMessages.get().clear();
        _subscriberMap.clear();
        _stickyMessages.clear();
    }

    /**
     * get the descriptor of XulMessageCenter
     *
     * @return the descriptor of XulMessageCenter
     */
    public String getDescriptor() {
        return _desc;
    }

    public MessageDispatcher getDispatcher() {
        return _dispatcher;
    }

    /**
     * 消息分发器
     */
    private class MessageDispatcher {

        /**
         * 将接收方法执行在UI线程
         */
        XulMessageHandler _uiThreadHandler = new XulUiThreadMessageHandler();

        /**
         * 哪个线程执行的post,接收方法就执行在哪个线程
         */
        XulMessageHandler _postThreadHandler = new XulDefaultMessageHandler();

        /**
         * 异步线程中执行订阅方法
         */
        XulMessageHandler _asyncThreadHandler = new XulAsyncMessageHandler();

        /**
         * 消息匹配策略,根据策略来查找对应的MessageType集合
         */
        XulMatchPolicy _matchPolicy = new XulDefaultMatchPolicy();

        /**
         * 缓存一个消息类型对应的可MessageType列表
         */
        private Map<XulMessage, List<XulMessage>>
                _messageTypeCaches = new ConcurrentHashMap<XulMessage, List<XulMessage>>();

        void dispatchMessages() {
            Queue<XulMessage> messagesQueue = _localMessages.get();
            while (messagesQueue.size() > 0) {
                deliveryMessage(messagesQueue.poll());
            }
        }

        void cancelMessage(XulMessage message) {
            List<XulMessage> xulMessages = getMatchedMessageTypes(message);
            for (XulMessage xulMessage : xulMessages) {
                List<XulSubscription> xulSubscriptions = _subscriberMap.get(xulMessage);
                if (xulSubscriptions == null) {
                    continue;
                }
                for (XulSubscription xulSubscription : xulSubscriptions) {
                    xulSubscription.clearXulMessages();
                }
            }
        }

        /**
         * 根据message查找到所有匹配的集合,然后处理消息
         */
        private void deliveryMessage(XulMessage message) {
            if (messageFilter != null && messageFilter.filter(message)) {
                return;
            }
            // 如果有缓存则直接从缓存中取
            List<XulMessage> xulMessages = getMatchedMessageTypes(message);
            // 迭代所有匹配的消息并且分发给订阅者
            for (XulMessage xulMessage : xulMessages) {
                handleMessage(xulMessage);
            }
        }

        /**
         * 处理单个消息
         */
        private void handleMessage(XulMessage xulMessage) {
            List<XulSubscription> xulSubscriptions = _subscriberMap.get(xulMessage);
            if (xulSubscriptions == null) {
                return;
            }

            for (XulSubscription xulSubscription : xulSubscriptions) {
                final XulThreadMode mode = xulSubscription.getXulThreadMode();
                if (xulMessage.getIsSyncMessage() && (mode == XulThreadMode.MAIN)) {
                    // 目前只支持同步消息必须后台线程中执行, 忽略必须在主线程执行的接收方法
                    continue;
                }

                // 处理消息
                XulMessageHandler xulMessageHandler = getMessageHandler(mode);
                xulSubscription.addXulMessage(xulMessage);
                xulMessageHandler.handleMessage(xulSubscription);
            }
        }

        private List<XulMessage> getMatchedMessageTypes(XulMessage message) {
            List<XulMessage> messages;
            // 如果有缓存则直接从缓存中取
            if (_messageTypeCaches.containsKey(message)) {
                messages = _messageTypeCaches.get(message);

                // 更新缓存中的msg数据
                for (XulMessage matchMsg : messages) {
                    matchMsg.copyData(message);
                }
            } else {
                messages = _matchPolicy.findMatchMessageTypes(message);
                _messageTypeCaches.put(message, messages);
            }

            return messages;
        }

        void dispatchStickyMessages(Object subscriber) {
            for (XulMessage xulMessage : _stickyMessages) {
                handleStickyMessage(xulMessage, subscriber);
            }
        }

        /**
         * 处理单个Sticky消息
         */
        private void handleStickyMessage(XulMessage xulMessage, Object subscriber) {
            List<XulMessage> xulMessages = getMatchedMessageTypes(xulMessage);
            // 消息
            Object message = xulMessage.getData();
            for (XulMessage foundXulMessage : xulMessages) {
                XulLog.i(TAG, "### find message type : "
                              + foundXulMessage.getParamClass().getSimpleName()
                              + ", message class : " + message.getClass().getSimpleName());
                final List<XulSubscription> xulSubscriptions = _subscriberMap.get(foundXulMessage);
                if (xulSubscriptions == null) {
                    continue;
                }
                for (XulSubscription subItem : xulSubscriptions) {
                    final XulThreadMode mode = subItem.getXulThreadMode();
                    XulMessageHandler xulMessageHandler = getMessageHandler(mode);
                    // 如果订阅者为空,那么该sticky消息分发给所有订阅者.否则只分发给该订阅者
                    if (isTarget(subItem, subscriber)
                        && (subItem.getMessageType().equals(foundXulMessage)
                            || subItem.getMessageType().getParamClass()
                                    .isAssignableFrom(foundXulMessage.getParamClass()))) {
                        // 处理消息
                        subItem.addXulMessage(foundXulMessage);
                        xulMessageHandler.handleMessage(subItem);
                    }
                }
            }
        }

        /**
         * 如果传递进来的订阅者不为空,那么该Sticky消息只传递给该订阅者(注册时),否则所有订阅者都传递(发布时).
         */
        private boolean isTarget(XulSubscription item, Object subscriber) {
            Object cacheObject = item.getSubscriber();
            return (subscriber == null)
                   || (subscriber != null && cacheObject != null && cacheObject.equals(subscriber));
        }

        private XulMessageHandler getMessageHandler(XulThreadMode mode) {
            if (mode == XulThreadMode.ASYNC) {
                return _asyncThreadHandler;
            }
            if (mode == XulThreadMode.POST) {
                return _postThreadHandler;
            }
            return _uiThreadHandler;
        }
    } // end of MessageDispatcher

    /**
     * 构建message对象，使用指定的message center发送
     *
     * @param msgCenter 指定的 message center
     */
    public static MessageHelper buildMessage(XulMessageCenter msgCenter) {
        return new MessageHelper(msgCenter);
    }

    /**
     * 构建message对象，使用默认的message center发送
     */
    public static MessageHelper buildMessage() {
        return buildMessage(getDefault());
    }

    /**
     * Message发送辅助类
     */
    public static final class MessageHelper {

        private XulMessageCenter _messageCenter;
        private XulMessage _message;

        MessageHelper(XulMessageCenter msgCenter) {
            _message = XulMessage.obtainMessage();
            _messageCenter = msgCenter;
            if (_messageCenter == null) {
                _messageCenter = XulMessageCenter.getDefault();
            }
        }

        public MessageHelper setTag(int tag) {
            _message.setTag(tag);
            return this;
        }

        public MessageHelper setData(Object data) {
            _message.setData(data);
            return this;
        }

        public MessageHelper setRepeat(int repeat) {
            _message.setRepeat(repeat);
            return this;
        }

        public MessageHelper setDelay(long delay) {
            _message.setDelay(delay);
            return this;
        }

        public MessageHelper setInterval(long interval) {
            _message.setInterval(interval);
            return this;
        }

        public void post() {
            _messageCenter.post(_message);
        }

        public void postSticky() {
            _messageCenter.postSticky(_message);
        }

        public void send() {
            _messageCenter.send(_message);
        }

        public void cancel() {
            _messageCenter.cancel(_message);
            _messageCenter.removeStickyMessage(_message);
        }
    }

    private XulMessageFilter messageFilter;

    public void setMessageFilter(XulMessageFilter messageFilter) {
        this.messageFilter = messageFilter;
    }

    public interface XulMessageFilter {
        boolean filter(XulMessage message);
    }
}
