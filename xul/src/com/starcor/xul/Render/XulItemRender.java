package com.starcor.xul.Render;

import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/5/13.
 */
public class XulItemRender extends XulViewRender {
	public static void register() {
		XulRenderFactory.registerBuilder("item", "*",  new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulItem;
				return new XulItemRender(ctx, view);
			}
		});
	}

	public XulItemRender(XulRenderContext ctx, XulView view) {
		super(ctx, view);
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_FOCUSABLE;
	}
}
