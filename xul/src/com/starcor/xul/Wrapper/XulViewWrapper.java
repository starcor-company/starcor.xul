package com.starcor.xul.Wrapper;

import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulLayout;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2015/12/23.
 */
public class XulViewWrapper {
	XulView _view;

	XulViewWrapper(XulView view) {
		_view = view;
	}

	public static XulViewWrapper fromXulView(XulView view) {
		return new XulViewWrapper(view);
	}

	public XulView getAsView() {
		return _view;
	}

	public XulArea getAsArea() {
		if (_view instanceof XulArea) {
			return (XulArea) _view;
		}
		return null;
	}

	public void requestFocus() {
		XulLayout rootLayout = _view.getRootLayout();
		if (rootLayout == null) {
			return;
		}
		rootLayout.requestFocus(_view);
	}

	public boolean blinkClass(String... clsName) {
		XulViewRender render = _view.getRender();
		if (render == null) {
			return false;
		}
		return render.blinkClass(clsName);
	}
}
