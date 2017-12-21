package com.starcor.xul;

import android.graphics.RectF;
import android.util.Log;
import android.view.KeyEvent;

import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Prop.*;
import com.starcor.xul.Render.XulViewRender;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/4.
 */
public class XulLayout extends XulArea {
	private static final String TAG = XulLayout.class.getSimpleName();
	XulPage _page;
	XulView _focus;

	private XulLayout(XulPage page) {
		super();
		_root = this;
		_page = page;
		_page.addLayout(this);
	}

	public XulLayout makeClone(XulPage page) {
		XulLayout layout = new XulLayout(page);

		layout.copyContent(this);
		int childrenSize = _children.size();
		for (int i = 0; i < childrenSize; i++) {
			Object obj = _children.get(i);
			if (obj instanceof XulArea) {
				XulArea area = (XulArea) obj;
				area.makeClone(layout);
			} else if (obj instanceof XulTemplate) {
				XulTemplate template = (XulTemplate) obj;
				template.makeClone(layout);
			} else if (obj instanceof XulItem) {
				XulItem item = (XulItem) obj;
				item.makeClone(layout);
			} else {
				Log.d(TAG, "unsupported children type!!! - " + obj.getClass().getSimpleName());
			}
		}
		return layout;
	}

	public int getHeight() {
		XulAttr val = getAttr("height");
		if (val == null || val.getValue() == null) {
			return _page.getDesignPageHeight();
		}
		String valHeight = val.getStringValue();
		if ("match_parent".equals(valHeight)) {
			return _page.getDesignPageHeight();
		}
		return XulUtils.tryParseInt(valHeight, _page.getDesignPageHeight());
	}

	public int getWidth() {
		XulAttr val = getAttr("width");
		if (val == null || val.getValue() == null) {
			return _page.getDesignPageWidth();
		}
		String valWidth = val.getStringValue();
		if ("match_parent".equals(valWidth)) {
			return _page.getDesignPageWidth();
		}
		return XulUtils.tryParseInt(valWidth, _page.getDesignPageWidth());
	}

	@Override
	public boolean onKeyEvent(KeyEvent event) {
		if (_focus == null) {
			return false;
		}
		XulViewRender render = _focus.getRender();
		if (render == null || render.getDrawingRect() == null) {
			return false;
		}
		return _focus.onKeyEvent(event);
	}

	public boolean isFocused(XulView view) {
		return _focus == view;
	}

	public void requestFocus(XulView view) {
		if (_focus == view) {
			return;
		}
		XulView oldFocus = _focus;
		if (view != null && !view.focusable()) {
			// 目标不可设置为焦点, 从目标子元素中找焦点
			if (!(view instanceof XulArea)) {
				return;
			}
			if (oldFocus != null && oldFocus.isChildOf(view)) {
				return;
			}
			XulArea area = (XulArea) view;
			view = area.getDynamicFocus();
			if (view == null) {
				area.eachChild(_findAnyFocusIterator);
				view = _findAnyFocusIterator.getFocus();
			}
			if (view == null) {
				return;
			}
		}
		XulView newFocus = _focus = view;
		if (oldFocus != null) {
			oldFocus.onBlur();
			oldFocus.resetRender();
		}
		if (newFocus != null) {
			newFocus.onFocus();
			newFocus.resetRender();
		}
		if (oldFocus != null) {
			XulPage.invokeActionNoPopup(oldFocus, "blur");
		}
		if (newFocus != null) {
			XulPage.invokeActionNoPopup(newFocus, "focus");
		}
	}

	public void killFocus() {
		requestFocus(null);
	}

	public XulView getFocus() {
		return _focus;
	}

	boolean applySelectors(XulView xulView, ArrayList<String> clsKeys) {
		return _page.applySelectors(xulView, clsKeys);
	}

	boolean unApplySelectors(XulView xulView, ArrayList<String> clsKeys) {
		return _page.unApplySelectors(xulView, clsKeys);
	}

	void addSelectorTargets(XulArea area) {
		_page.addSelectorTargets(area);
	}

	void addSelectorTarget(XulView item) {
		if (item instanceof XulArea) {
			_page.addSelectorTargets((XulArea) item);
		} else {
			_page.addSelectorTarget(item, item.getSelectKeys());
		}
	}

	void addSelectorTarget(XulView view, ArrayList<String> clsKeys) {
		_page.addSelectorTarget(view, clsKeys);
	}

	void removeSelectorTarget(XulView view, ArrayList<String> clsKeys) {
		_page.removeSelectorTarget(view, clsKeys);
	}

	public void applySelectors(XulArea area) {
		_page.applySelectors(area);
	}

	public void applySelectors(XulView item) {
		if (item instanceof XulArea) {
			_page.applySelectors((XulArea) item);
		} else {
			_page.applySelectors((XulItem) item);
		}
	}

	public enum FocusDirection {
		MOVE_LEFT,
		MOVE_RIGHT,
		MOVE_UP,
		MOVE_DOWN,
	}

	public boolean moveFocus(FocusDirection direction) {
		XulView curFocus = _focus;
		if (curFocus == null) {
			this.eachChild(_findAnyFocusIterator);
			curFocus = _findAnyFocusIterator.getFocus();
			requestFocus(curFocus);
			return _focus != null;
		}

		RectF focusRc = curFocus.getFocusRc();
		XulView focus = curFocus._parent.findFocus(direction, focusRc, curFocus);
		if (focus == null) {
			//
			return false;
		}

		XulArea focusContainer = focus.getParent();
		RectF targetRect = focus.getFocusRc();
		while (focusContainer != null) {
			XulViewRender focusContainerRender = focusContainer.getRender();
			if (focusContainerRender != null && focusContainerRender.keepFocusVisible()) {
				RectF focusContainerFocusRc = focusContainer.getFocusRc();
				if (!XulUtils.intersects(focusContainerFocusRc, targetRect)) {
					return false;
				}
			}
			focusContainer = focusContainer.getParent();
		}
		requestFocus(focus);
		return true;
	}

	public boolean doClick(XulPage.IActionCallback actionCallback) {
		if (_focus == null) {
			return false;
		}
		return XulPage.invokeAction(_focus, "click", actionCallback);
	}

	public void initFocus() {
		if (_focus != null) {
			return;
		}

		_findDefaultFocus(this);
	}

	public XulPage getOwnerPage() {
		return _page;
	}

	private static class FindAnyFocusIterator extends XulAreaIterator {
		private XulView _newFocus = null;

		public XulView getFocus() {
			XulView newFocus = _newFocus;
			_newFocus = null;
			return newFocus;
		}

		@Override
		public boolean onXulArea(int pos, XulArea area) {
			if (_newFocus != null) {
				return false;
			}
			if (!area.isEnabled() || !area.isVisible()) {
				return true;
			}
			if (area.focusable()) {
				_newFocus = area;
			} else {
				XulView dynamicFocus = area.getDynamicFocus();
				if (dynamicFocus != null) {
					_newFocus = dynamicFocus;
					return false;
				}
				area.eachChild(this);
			}
			return true;
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			if (_newFocus != null) {
				return false;
			}
			if (!item.isEnabled() || !item.isVisible()) {
				return true;
			}
			if (item.focusable()) {
				_newFocus = item;
			}
			return true;
		}
	}

	private static FindAnyFocusIterator _findAnyFocusIterator = new FindAnyFocusIterator();

	private void _findDefaultFocus(XulArea area) {
		if (!area.isEnabled()) {
			return;
		}
		area.eachChild(new XulAreaIterator() {
			               @Override
			               public boolean onXulArea(int pos, XulArea area) {
				               if (_focus != null) {
					               return false;
				               }
				               if (!area.isEnabled() || !area.isVisible()) {
					               return true;
				               }
				               if (area.focusable() && area.isDefaultFocus()) {
					               requestFocus(area);
				               } else {
					               _findDefaultFocus(area);
				               }
				               return true;
			               }

			               @Override
			               public boolean onXulItem(int pos, XulItem item) {
				               if (_focus != null) {
					               return false;
				               }
				               if (!item.isEnabled() || !item.isVisible()) {
					               return true;
				               }
				               if (item.focusable() && item.isDefaultFocus()) {
					               requestFocus(item);
				               }
				               return true;
			               }

			               @Override
			               public boolean onXulTemplate(int pos, XulTemplate template) {
				               return true;
			               }
		               }
		);
	}

	public static class _Builder extends ItemBuilder {
		XulLayout _layout;

		private void init(XulPage page) {
			_layout = new XulLayout(page);
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_layout._id = attrs.getValue("id");
			_layout.setClass(attrs.getValue("class"));
			_layout._binding = attrs.getValue("binding");
			_layout._desc = attrs.getValue("desc");
			return true;
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			if (name.equals("area")) {
				XulArea._Builder builder = XulArea._Builder.create(_layout);
				builder.initialize(name, attrs);
				return builder;
			}
			if (name.equals("item")) {
				XulItem._Builder builder = XulItem._Builder.create(_layout);
				builder.initialize(name, attrs);
				return builder;
			}
			if (name.equals("template")) {
				XulTemplate._Builder builder = XulTemplate._Builder.create(_layout);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("action".equals(name)) {
				XulAction._Builder builder = XulAction._Builder.create(_layout);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("data".equals(name)) {
				XulData._Builder builder = XulData._Builder.create(_layout);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("attr".equals(name)) {
				XulAttr._Builder builder = XulAttr._Builder.create(_layout);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("style".equals(name)) {
				XulStyle._Builder builder = XulStyle._Builder.create(_layout);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("focus".equals(name)) {
				XulFocus._Builder builder = XulFocus._Builder.create(_layout);
				builder.initialize(name, attrs);
				return builder;
			}
			return XulManager.CommonDummyBuilder;
		}

		@Override
		public Object finalItem() {
			XulLayout layout = _layout;
			_Builder.recycle(this);
			return layout;
		}

		static public _Builder create(XulPage page) {
			_Builder builder = create();
			builder.init(page);
			return builder;

		}

		private static _Builder create() {
			_Builder builder = _recycled_builder;
			if (builder == null) {
				builder = new _Builder();
			} else {
				_recycled_builder = null;
			}
			return builder;
		}

		private static _Builder _recycled_builder;

		private static void recycle(_Builder builder) {
			_recycled_builder = builder;
			_recycled_builder._layout = null;
		}
	}

	@Override
	public boolean isDiscarded() {
		return false;
	}

	@Override
	public boolean hasFocus() {
		return _focus != null;
	}
}
