package com.starcor.xulapp.message.seqmessage;

import com.starcor.xulapp.message.XulMessageCenter;
import com.starcor.xulapp.message.XulSubscriber;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ZFB on 2015/12/8 0008.
 */
public abstract class XulSeqMsgDataDispatcher {

    public XulSeqMsgDataDispatcher(XulMessageCenter msgCenter) {
        msgCenter.register(this);
    }

    public XulSeqMsgDataDispatcher() {
        this(XulMessageCenter.getDefault());
    }

    private ConcurrentMap<Integer, XulSeqMessageHandler> _handlers =
            new ConcurrentHashMap<Integer, XulSeqMessageHandler>();

    @XulSubscriber
    public void onXulSeqMsgData(XulSeqMsgData seqMsgData) {
        if (seqMsgData.getSeqId() == XulSeqMsgData.DEFAULT_SEQ_ID) {
            onNewMessageSeq(seqMsgData);
        }
        dispatch(seqMsgData);
    }

    public void registerSeqHandler(XulSeqMsgData seqMsgData, XulSeqMessageHandler handler) {
        _handlers.put(seqMsgData.getMsgId(), handler);
    }

    public void dispatch(XulSeqMsgData seqMsgData) {
        final XulSeqMessageHandler handler = _handlers.get(seqMsgData.getMsgId());
        if (handler != null) {
            handler.handleSeqMessage(seqMsgData);
        }
    }

    public abstract void onNewMessageSeq(XulSeqMsgData msg);
}

