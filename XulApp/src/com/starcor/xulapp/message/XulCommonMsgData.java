package com.starcor.xulapp.message;

/**
 * Created by ZFB on 2015/10/14.
 *
 * 通用消息数据，可自行拓展
 */
public class XulCommonMsgData {

    private boolean _boolValue = false;

    private int _intValue1 = 0;

    private int _intValue2 = 0;

    private String _stringValue = "";
    

    public boolean getBoolValue() {
        return _boolValue;
    }

    public void setBoolValue(boolean boolValue) {
        _boolValue = boolValue;
    }

    public int getIntValue1() {
        return _intValue1;
    }

    public void setIntValue1(int intValue1) {
        _intValue1 = intValue1;
    }

    public int getIntValue2() {
        return _intValue2;
    }

    public void setIntValue2(int intValue2) {
        _intValue2 = intValue2;
    }

    public String getStringValue() {
        return _stringValue;
    }

    public void setStringValue(String stringValue) {
        _stringValue = stringValue;
    }
}
