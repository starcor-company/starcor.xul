package com.starcor.xulapp.message;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * the subscriber method hunter, find all of the subscriber's methods which annotated with
 *
 * @Subcriber.
 */
public class XulSubscriberMethodHunter {

    /**
     * the message bus's subscriber's map
     */
    private final Map<XulMessage, CopyOnWriteArrayList<XulSubscription>> _subscriberMap;

    private final ReferenceQueue<Object> _subscriberReferenceQueue;

    /**
     * @param subscriberMap
     */
    public XulSubscriberMethodHunter(
            Map<XulMessage, CopyOnWriteArrayList<XulSubscription>> subscriberMap,
            ReferenceQueue<Object> subscriberReferenceQueue) {
        _subscriberMap = subscriberMap;
        _subscriberReferenceQueue = subscriberReferenceQueue;
    }

    /**
     * 查找订阅对象中的所有订阅函数,订阅函数的参数只能有一个.找到订阅函数之后构建Subscription存储到Map中
     *
     * @param subscriber 订阅对象
     */
    public void findSubscribeMethods(Object subscriber) {
        if (_subscriberMap == null) {
            throw new NullPointerException("the _subscriberMap is null. ");
        }
        Reference subscriberReference =
                new WeakReference<Object>(subscriber, _subscriberReferenceQueue);

        // 查找类中符合要求的注册方法,直到Object类
        Class<?> clazz = subscriber.getClass();
        while (clazz != null && !isSystemClass(clazz.getName())) {
            final Method[] allMethods = clazz.getDeclaredMethods();
            for (int i = 0; i < allMethods.length; i++) {
                Method method = allMethods[i];
                // 根据注解来解析函数
                XulSubscriber annotation = method.getAnnotation(XulSubscriber.class);
                if (annotation != null) {
                    // 获取方法参数
                    Class<?>[] paramsTypeClass = method.getParameterTypes();
                    // 订阅函数只支持一个参数
                    if (paramsTypeClass != null && paramsTypeClass.length == 1) {
                        Class<?> paramType = convertType(paramsTypeClass[0]);
                        XulMessage xulMessage = new XulMessage(annotation.tag(), paramType);
                        XulSubscription subscription = new XulSubscription(
                                xulMessage, subscriberReference, method, annotation.mode());
                        subscribe(xulMessage, subscription);
                    }
                }
            } // end for
            // 获取父类,以继续查找父类中符合要求的方法
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 按照MessageType存储订阅者列表,这里的MessageType就是消息类型,一个消息对应0到多个订阅者.
     *
     * @param xulMessage   消息
     * @param subscription 订阅者对象
     */
    private void subscribe(XulMessage xulMessage, XulSubscription subscription) {
        CopyOnWriteArrayList<XulSubscription> xulSubscriptionLists = _subscriberMap.get(xulMessage);
        if (xulSubscriptionLists == null) {
            xulSubscriptionLists = new CopyOnWriteArrayList<XulSubscription>();
        }

        if (xulSubscriptionLists.contains(subscription)) {
            return;
        }

        xulSubscriptionLists.add(subscription);
        // 将消息类型key和订阅者信息存储到map中
        _subscriberMap.put(xulMessage, xulSubscriptionLists);
    }

    /**
     * remove subscriber methods from map
     */
    public void removeMethodsFromMap(Object subscriber) {
        Iterator<CopyOnWriteArrayList<XulSubscription>> iterator =
                _subscriberMap.values().iterator();
        while (iterator.hasNext()) {
            CopyOnWriteArrayList<XulSubscription> subscriptions = iterator.next();
            if (subscriptions != null) {
                List<XulSubscription> foundSubscriptions = new LinkedList<XulSubscription>();
                Iterator<XulSubscription> subIterator = subscriptions.iterator();
                while (subIterator.hasNext()) {
                    XulSubscription xulSubscription = subIterator.next();
                    // 获取引用
                    Object cacheObject = xulSubscription.getSubscriber();
                    if ((cacheObject == null)
                        || cacheObject.equals(subscriber)) {
                        xulSubscription.clearXulMessages();
                        foundSubscriptions.add(xulSubscription);
                    }
                }

                // 移除该subscriber的相关的Subscription
                subscriptions.removeAll(foundSubscriptions);
            }

            // 如果针对某个Msg的订阅者数量为空了,那么需要从map中清除
            if (subscriptions == null || subscriptions.size() == 0) {
                iterator.remove();
            }
        }
    }

    /**
     * 清除已经失效的订阅者
     */
    public void cleanupInvalidMethods() {
        removeMethodsFromMap(null);
    }

    /**
     * if the subscriber method's type is primitive, convert it to corresponding Object type. for
     * example, int to Integer.
     *
     * @param aClass origin class Type
     */
    private Class<?> convertType(Class<?> aClass) {
        Class<?> returnClass = aClass;
        if (aClass.equals(boolean.class)) {
            returnClass = Boolean.class;
        } else if (aClass.equals(int.class)) {
            returnClass = Integer.class;
        } else if (aClass.equals(float.class)) {
            returnClass = Float.class;
        } else if (aClass.equals(double.class)) {
            returnClass = Double.class;
        }

        return returnClass;
    }

    private boolean isSystemClass(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.");
    }
}
