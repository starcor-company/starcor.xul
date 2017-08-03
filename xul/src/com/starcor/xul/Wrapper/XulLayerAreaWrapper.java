package com.starcor.xul.Wrapper;

import com.starcor.xul.Render.XulLayerRender;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/3.
 */
public class XulLayerAreaWrapper extends XulViewWrapper {
	public static XulLayerAreaWrapper fromXulView(XulView view) {
		if (view == null) {
			return null;
		}
		if (!(view.getRender() instanceof XulLayerRender)) {
			return null;
		}
		return new XulLayerAreaWrapper((XulArea) view);
	}

	XulLayerAreaWrapper(XulArea view) {
		super(view);
	}
}
