package com.starcor.xulapp.message.seqmessage;

/**
 * Created by ZFB on 2015/12/8 0008.
 */
public class XulSeqMsgData {

    /**
     * 默认的序列id
     */
    public static final int DEFAULT_SEQ_ID = 0;

    private int _msgId = 0;

    private int _seqId = 0;

    private Object _data = null;

    public XulSeqMsgData(int msgId, int seqId, Object data) {
        _msgId = msgId;
        _seqId = seqId;
        _data = data;
    }

    public int getMsgId() {
        return _msgId;
    }

    public int getSeqId() {
        return _seqId;
    }

    public Object getData() {
        return _data;
    }

    public void setData(Object _data) {
        this._data = _data;
    }
}
