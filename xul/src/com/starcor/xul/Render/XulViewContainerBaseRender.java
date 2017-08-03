package com.starcor.xul.Render;

import com.starcor.xul.Events.XulStateChangeEvent;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulSimpleArray;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2015/2/7.
 */
public abstract class XulViewContainerBaseRender extends XulViewRender {

	protected XulArea _area;

	public XulViewContainerBaseRender(XulRenderContext ctx, XulArea area) {
		super(ctx, area);
		_area = area;
	}

	@Override
	public boolean onChildStateChanged(XulStateChangeEvent event) {
		if (_area.getStyle(XulPropNameCache.TagId.LOCK_FOCUS_TARGET) != null) {
			event.alteredEventSource = _area;
		}
		return super.onChildStateChanged(event);
	}

	protected class LayoutContainer extends LayoutElement implements XulLayoutHelper.ILayoutContainer {
		@Override
		public int getContentOffsetX() {
			return 0;
		}

		@Override
		public int getContentOffsetY() {
			return 0;
		}

		@Override
		public boolean updateContentSize() {
			return false;
		}

		@Override
		public int setContentSize(int w, int h) {
			return 0;
		}

		@Override
		public int getAlignmentOffsetX() {
			return 0;
		}

		@Override
		public int getAlignmentOffsetY() {
			return 0;
		}

		@Override
		public int setAlignmentOffset(int x, int y) {
			return 0;
		}

		@Override
		public float getAlignmentX() {
			return Float.NaN;
		}

		@Override
		public float getAlignmentY() {
			return Float.NaN;
		}

		@Override
		public int getOffsetX() {
			return 0;
		}

		@Override
		public int getOffsetY() {
			return 0;
		}

		@Override
		public int getLayoutMode() {
			return 0;
		}

		@Override
		public int getChildNum() {
			return _area.getChildNum();
		}

		@Override
		public XulLayoutHelper.ILayoutElement getChild(int idx) {
			return _area.getChildLayoutElement(idx);
		}

		@Override
		public XulLayoutHelper.ILayoutElement getVisibleChild(int idx) {
			return _area.getVisibleChildLayoutElement(idx);
		}

		@Override
		public int getAllVisibleChildren(XulSimpleArray<XulLayoutHelper.ILayoutElement> array) {
			return _area.getAllVisibleChildLayoutElement(array);
		}

	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutContainer();
	}

	@Override
	public XulLayoutHelper.ILayoutContainer getLayoutElement() {
		return (XulLayoutHelper.ILayoutContainer) super.getLayoutElement();
	}

	public boolean setDrawingSkipped(final boolean skipped) {
		if (super.setDrawingSkipped(skipped)) {
			_area.eachView(SkipDrawingUpdater.obtainUpdater(skipped));
		}
		return false;
	}

	static class SkipDrawingUpdater extends XulArea.XulViewIterator {
		private boolean _skipped;
		static SkipDrawingUpdater _updater;

		static SkipDrawingUpdater obtainUpdater(boolean skipped) {
			if (_updater == null) {
				_updater = new SkipDrawingUpdater();
			}
			_updater._skipped = skipped;
			return _updater;
		}

		private SkipDrawingUpdater() {}

		@Override
		public boolean onXulView(int pos, XulView view) {
			XulViewRender render = view.getRender();
			if (render != null) {
				render.setDrawingSkipped(_skipped);
			}
			return true;
		}
	}
}
