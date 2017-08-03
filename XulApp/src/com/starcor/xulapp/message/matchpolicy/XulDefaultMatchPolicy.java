package com.starcor.xulapp.message.matchpolicy;


import com.starcor.xulapp.message.XulMessage;

import java.util.LinkedList;
import java.util.List;

public class XulDefaultMatchPolicy implements XulMatchPolicy {

    @Override
    public List<XulMessage> findMatchMessageTypes(XulMessage message) {
        Class<?> messageClass = message.getParamClass();
        List<XulMessage> result = new LinkedList<XulMessage>();
        while (messageClass != null) {
            XulMessage newMessage = new XulMessage(message);
            newMessage.setParamClass(messageClass);
            result.add(newMessage);
            addInterfaces(result, newMessage);
            messageClass = messageClass.getSuperclass();
        }

        return result;
    }


    /**
     * 获取该消息的所有接口类型
     */
    private void addInterfaces(List<XulMessage> messages, XulMessage message) {
        Class<?>[] interfacesClasses = message.getParamClass().getInterfaces();
        for (Class<?> interfaceClass : interfacesClasses) {
            if (!messages.contains(interfaceClass)) {
                XulMessage newMessage = new XulMessage(message);
                newMessage.setParamClass(interfaceClass);
                messages.add(newMessage);
                addInterfaces(messages, newMessage);
            }
        }
    }

}
