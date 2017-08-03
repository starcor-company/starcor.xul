package com.starcor.xul.Render;

import android.text.TextUtils;

import com.starcor.xul.Render.Components.BaseScrollBar;
import com.starcor.xul.Render.Components.SimpleScrollBar;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulView;

import java.util.HashMap;

/**
 * Created by hy on 2014/5/27.
 */
public final class XulRenderFactory {
	public static abstract class RenderBuilder {
		protected abstract XulViewRender createRender(XulRenderContext ctx, XulView view);
	}

	public interface ScrollBarBuilder {
		BaseScrollBar create(String desc, String[] descFields, BaseScrollBar.ScrollBarHelper helper, XulViewRender render);
	}

	private static XulCachedHashMap<String, RenderBuilder> _builderMap = new XulCachedHashMap<String, RenderBuilder>();

	public static void registerBuilder(String viewType, String renderType, RenderBuilder builder) {
		String renderKey = viewType + "." + renderType;
		_builderMap.put(renderKey, builder);
	}

	public static XulViewRender createRender(String viewType, String renderType, XulRenderContext ctx, XulView view, boolean preload) {
		if (TextUtils.isEmpty(renderType)) {
			renderType = "*";
		}
		String renderKey = viewType + "." + renderType;
		RenderBuilder builder = _builderMap.get(renderKey);
		if (builder == null) {
			renderKey = viewType + ".*";
			builder = _builderMap.get(renderKey);
		}
		if (builder == null) {
			return null;
		}
		XulViewRender render = builder.createRender(ctx, view);
		render.setPreload(preload, preload);
		render.setUpdateAll();
		render.markDirtyView();
		return render;
	}


	static HashMap<String, ScrollBarBuilder> _scrollBarFactories = new HashMap<String, ScrollBarBuilder>();

	public static BaseScrollBar buildScrollBar(BaseScrollBar oldScrollBar, String desc, BaseScrollBar.ScrollBarHelper helper, XulViewRender render) {
		desc = desc.trim();
		if (TextUtils.isEmpty(desc)) {
			if (oldScrollBar != null) {
				oldScrollBar.recycle();
			}
			return null;
		}
		String[] descFields = desc.split(",");
		String scrollBarType = descFields[0];
		if (oldScrollBar != null) {
			BaseScrollBar newScrollBar = oldScrollBar.update(desc, descFields);
			if (newScrollBar != oldScrollBar) {
				oldScrollBar.recycle();
				oldScrollBar = newScrollBar;
			}
		}

		if (oldScrollBar != null) {
			return oldScrollBar;
		}

		ScrollBarBuilder factory = _scrollBarFactories.get(scrollBarType);
		if (factory == null) {
			return null;
		}

		return factory.create(desc, descFields, helper, render);
	}

	public static boolean registerScrollBarFactory(String scrollBarType, ScrollBarBuilder f) {
		_scrollBarFactories.put(scrollBarType, f);
		return true;
	}

	static {
		SimpleScrollBar.register();

		XulAreaRender.register();
		XulCustomViewRender.register();
		XulGridAreaRender.register();
		XulImageRender.register();
		XulItemRender.register();
		XulLabelRender.register();
		XulSpannedLabelRender.register();
		XulPageSliderAreaRender.register();
		XulSliderAreaRender.register();
		XulGroupRender.register();
		XulLayerRender.register();
		XulComponentRender.register();
		XulMassiveRender.register();
	}
}
