package com.starcor.xul.Render;

import android.graphics.Rect;
import com.starcor.xul.*;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Utils.XulAreaChildrenRender;
import com.starcor.xul.Utils.XulAreaChildrenVisibleChangeNotifier;
import com.starcor.xul.Utils.XulLayoutHelper;

/**
 * Created by hy on 2014/5/12.
 */
public class XulAreaRender extends XulViewContainerBaseRender {
	public static void register() {
		XulRenderFactory.registerBuilder("area", "*", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				return new XulAreaRender(ctx, (XulArea) view);
			}
		});
	}

	@Override
	public boolean hitTest(int event, float x, float y) {
		return super.hitTest(XulManager.HIT_EVENT_DUMMY, x, y);
	}

	protected XulAreaChildrenRender _childrenRender = createChildrenRender();

	protected XulAreaChildrenRender createChildrenRender() {
		return new XulAreaChildrenRender();
	}

	public XulAreaRender(XulRenderContext ctx, XulArea area) {
		super(ctx, area);
	}

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		super.draw(dc, rect, xBase, yBase);
		_childrenRender.init(dc, rect, xBase, yBase);
		_area.eachView(_childrenRender);
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_NOFOCUS|XulFocus.MODE_NEARBY|XulFocus.MODE_PRIORITY;
	}

	@Override
	public void onVisibilityChanged(boolean isVisible, XulView eventSource) {
		super.onVisibilityChanged(isVisible, eventSource);
		XulAreaChildrenVisibleChangeNotifier notifier = XulAreaChildrenVisibleChangeNotifier.getNotifier();
		notifier.begin(isVisible, (XulArea) eventSource);
		_area.eachChild(notifier);
		notifier.end();
	}

	protected class LayoutContainer extends XulViewContainerBaseRender.LayoutContainer {
		@Override
		public Rect getMargin() {
			if (_margin == null) {
				if (_area.getType() == XulTemplate.TEMPLATE_CONTAINER) {
					if (_area.getChildNum() == 1) {
						XulView child = _area.getChild(0);
						if (child != null) {
							return child.getRender().getLayoutElement().getMargin();
						}
					}
				}
				return super.getMargin();
			}
			return _margin;
		}
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutContainer();
	}
}
