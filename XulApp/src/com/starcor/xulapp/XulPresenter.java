package com.starcor.xulapp;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.starcor.xul.XulRenderContext;

/**
 * Created by hy on 2015/12/28.
 */
public interface XulPresenter {

	Context xulGetContext();

	String xulGetIntentPageId();

	String xulGetCurPageId();

	String xulGetIntentLayoutFile();

	String xulGetCurLayoutFile();

	String xulGetCurBehaviorName();

	XulRenderContext xulGetRenderContext();

	FrameLayout xulGetRenderContextView();

	void xulLoadLayoutFile(String layoutFile);

	boolean xulDefaultDispatchKeyEvent(KeyEvent event);

	boolean xulDefaultDispatchTouchEvent(MotionEvent event);

	Bundle xulGetBehaviorParams();

	void xulDestroy();

	boolean xulIsAlive();
}
