package com.starcor.xul.Wrapper;

import com.starcor.xul.Render.XulLabelRender;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/3.
 */
public class XulLabelItemWrapper extends XulViewWrapper {
	public static XulLabelItemWrapper fromXulView(XulView view) {
		if (view == null) {
			return null;
		}
		if (!(view.getRender() instanceof XulLabelRender)) {
			return null;
		}
		return new XulLabelItemWrapper(view);
	}

	XulLabelItemWrapper(XulView view) {
		super(view);
	}
}
