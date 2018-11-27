package com.starcor.xulapp.message.handler;

import android.os.Handler;
import android.os.Looper;

import com.starcor.xulapp.message.XulMessage;
import com.starcor.xulapp.message.XulSubscription;


/**
 * 消息处理在UI线程,通过Handler将消息处理post到UI线程的消息队列
 */
public class XulUiThreadMessageHandler implements XulMessageHandler {

	/**
	 * default handler
	 */
	private XulDefaultMessageHandler _messageHandler = new XulDefaultMessageHandler();

	/**
	 * ui handler
	 */
	private Handler _uiHandler = new Handler(Looper.getMainLooper());

	/**
	 * 将订阅的函数执行在主线程中
	 */
	@Override
	public void handleMessage(final XulSubscription subscription) {
		XulMessage xulMessage = subscription.getXulMessage();
		if (xulMessage == null) {
			return;
		}

		_uiHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (subscription.isInvalid()) {
					// subscriber has dead or message has been canceled.
					return;
				}

				XulMessage xulMessage = subscription.getXulMessage();

				if (xulMessage == null) {
					return;
				}

				int repeat = xulMessage.getRepeat();
				if (repeat <= 0) {
					// 不需要执行
					return;
				}

				_messageHandler.handleMessage(subscription);
				if (--repeat > 0) {
					xulMessage.setRepeat(repeat);
					_uiHandler.postDelayed(this, xulMessage.getInterval());
				}
			}
		}, xulMessage.getDelay());
	}
}
