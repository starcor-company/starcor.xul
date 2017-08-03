package com.starcor.xul.Render;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.KeyEvent;
import com.starcor.xul.*;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Prop.XulStyle;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulPropParser;

/**
 * Created by hy on 2014/5/26.
 */
public class XulCustomViewRender extends XulViewRender {
	private static final String TAG = XulCustomViewRender.class.getSimpleName();
	IXulExternalView _extView;
	boolean _isParentVisible = true;
	public static final String TYPE = "custom";

	public static void register() {
		XulRenderFactory.registerBuilder("item", TYPE, new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				return new XulCustomViewRender(ctx, view);
			}
		});
	}

	public XulCustomViewRender(XulRenderContext ctx, XulView view) {
		super(ctx, view);
		init();
	}

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		super.syncData();
		IXulExternalView extView = _extView;
		if (extView != null) {
			extView.extSyncData();
		}
	}

	void init() {
		String className = _view.getAttrString("class");
		_extView = _ctx.createExternalView(className, 0, 0, 0, 0, _view);
		if (_extView == null) {
			Log.e(TAG, "create custom view failed!!");
			Log.e(TAG, "item " + _view);
		} else {
			// if view was invisible then synchronize the visibility
			XulView v = _view;
			while (v != null) {
				XulStyle displayStyle = v.getStyle(XulPropNameCache.TagId.DISPLAY);
				if (displayStyle != null
					&& ((XulPropParser.xulParsedStyle_Display) displayStyle.getParsedValue()).mode == XulPropParser.xulParsedStyle_Display.DisplayMode.None) {
					_extView.extHide();
					break;
				}
				v = v.getParent();
			}
		}
	}

	@Override
	public void onVisibilityChanged(boolean isVisible, XulView eventSource) {
		super.onVisibilityChanged(isVisible, eventSource);
		if (_extView == null) {
			Log.e(TAG, "external view is null! " + this.toString());
			return;
		}
		if (eventSource == _view) {
			_syncExtViewVisibility(isVisible);
			return;
		}
		_isParentVisible = isVisible;
		if (_isVisible()) {
			_syncExtViewVisibility(isVisible);
		}
	}

	@Override
	public void switchState(int state) {
		if (_extView != null) {
			try {
				if (XulView.STATE_FOCUSED == state) {
					_extView.extOnFocus();
				} else {
					_extView.extOnBlur();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		super.switchState(state);
	}

	private void _syncExtViewPosition() {
		if (_extView == null) {
			return;
		}
		if (_rect == null) {
			return;
		}
		if (XulUtils.calRectWidth(_rect) >= XulManager.SIZE_MAX || XulUtils.calRectHeight(_rect) >= XulManager.SIZE_MAX) {
			return;
		}
		RectF focusRc = _view.getFocusRc();
		if (_padding != null) {
			focusRc.left += _padding.left;
			focusRc.top += _padding.top;
			focusRc.right -= _padding.right;
			focusRc.bottom -= _padding.bottom;
		}
		Rect targetRc = XulDC._tmpRc0;
		XulUtils.copyRect(focusRc, targetRc);
		_extView.extMoveTo(targetRc);
	}

	private void _syncExtViewVisibility(boolean isVisible) {
		if (isVisible) {
			_extView.extShow();
			if (_view.isFocused()) {
				_extView.extOnFocus();
			}
		} else {
			_extView.extHide();
		}
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_FOCUSABLE;
	}

	@Override
	public boolean onKeyEvent(KeyEvent event) {
		if (_extView != null) {
			return _extView.extOnKeyEvent(event);
		}
		return false;
	}

	@Override
	public IXulExternalView getExternalView() {
		return _extView;
	}

	@Override
	public void destroy() {
		if (_extView == null) {
			return;
		}
		try {
			_extView.extDestroy();
		} catch (Exception e) {
			e.printStackTrace();
		}
		_extView = null;
	}

	protected class LayoutElement extends XulViewRender.LayoutElement {
		@Override
		public int doFinal() {
			super.doFinal();
			_syncExtViewPosition();
			return 0;
		}

		@Override
		public boolean offsetBase(int dx, int dy) {
			super.offsetBase(dx, dy);
			_syncExtViewPosition();
			return true;
		}

		@Override
		public boolean setBase(int x, int y) {
			super.setBase(x, y);
			_syncExtViewPosition();
			return true;
		}
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutElement();
	}

}
