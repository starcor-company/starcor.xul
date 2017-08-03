package com.starcor.xulapp.message.matchpolicy;


import com.starcor.xulapp.message.XulMessage;

import java.util.List;

public interface XulMatchPolicy {

    List<XulMessage> findMatchMessageTypes(XulMessage message);
}
