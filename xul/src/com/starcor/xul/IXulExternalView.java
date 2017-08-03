package com.starcor.xul;

import android.graphics.Rect;
import android.view.KeyEvent;

/**
 * Created by hy on 2014/5/27.
 */
public interface IXulExternalView {
	void extMoveTo(int x, int y, int width, int height);
	void extMoveTo(Rect rect);
	boolean extOnKeyEvent(KeyEvent event);
	void extOnFocus();
	void extOnBlur();
	void extShow();
	void extHide();
	void extDestroy();
	String getAttr(String key, String defVal);
	boolean setAttr(String key, String val);
	void extSyncData();

}
