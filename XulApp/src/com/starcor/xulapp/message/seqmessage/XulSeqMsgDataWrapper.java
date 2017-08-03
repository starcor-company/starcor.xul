package com.starcor.xulapp.message.seqmessage;

import com.starcor.xulapp.message.XulMessage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ZFB on 2015/12/8 0008.
 */
public class XulSeqMsgDataWrapper {

    private final int _messageId;
    private final AtomicInteger _seqId = new AtomicInteger();

    public XulSeqMsgDataWrapper() {
        _messageId = XulMessage.obtainMessageId();
        _seqId.set(0);
    }

    public XulSeqMsgData wrap() {
        return wrap(new Object());
    }

    public XulSeqMsgData wrap(Object data) {
        return new XulSeqMsgData(_messageId, _seqId.getAndIncrement(), data);
    }

}
