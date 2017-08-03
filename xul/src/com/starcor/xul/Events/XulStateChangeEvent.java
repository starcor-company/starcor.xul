package com.starcor.xul.Events;

import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/5/31.
 */
public class XulStateChangeEvent {
	public String event;
	public int oldState;
	public int state;
	public XulView eventSource; // 发生事件的view
	public XulView alteredEventSource;  // 事件处理链变更后的目标view
	public XulView notifySource;        // 本次通知的发送者view
	public boolean adjustFocusView = true;

	public XulView getEventSource() {
		return alteredEventSource == null ? eventSource : alteredEventSource;
	}
}
