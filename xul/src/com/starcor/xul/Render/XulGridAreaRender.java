package com.starcor.xul.Render;

import android.graphics.Rect;
import android.graphics.RectF;
import com.starcor.xul.Events.XulStateChangeEvent;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Utils.XulAreaChildrenRender;
import com.starcor.xul.Utils.XulAreaChildrenVisibleChangeNotifier;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.*;

/**
 * Created by hy on 2014/5/12.
 */
public class XulGridAreaRender extends XulViewContainerBaseRender {

	public static void register() {
		XulRenderFactory.registerBuilder("area", "grid", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				return new XulGridAreaRender(ctx, (XulArea) view);
			}
		});
	}

	XulAreaChildrenRender childrenRender = new XulAreaChildrenRender();

	boolean _isVertical = false;

	public XulGridAreaRender(XulRenderContext ctx, XulArea area) {
		super(ctx, area);
	}

	private void syncDirection() {
		if (!_isViewChanged()) {
			return;
		}
		XulAttr directionAttr = _area.getAttr(XulPropNameCache.TagId.DIRECTION);
		if (directionAttr != null) {
			XulPropParser.xulParsedAttr_Direction direction = directionAttr.getParsedValue();
			_isVertical = direction.vertical;
			// _reverseLayout = direction.reverse;
		}
	}

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		super.draw(dc, rect, xBase, yBase);
		childrenRender.init(dc, rect, xBase, yBase);
		_area.eachView(childrenRender);
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_NOFOCUS | XulFocus.MODE_NEARBY | XulFocus.MODE_PRIORITY;
	}

	@Override
	public boolean onChildStateChanged(XulStateChangeEvent event) {
		// if (XulView.STATE_FOCUSED.equals(state)) {
		// 	return makeChildVisible(focusedView);
		// }
		return false;
	}

	private boolean onChildFocused(XulView focusedView) {
		RectF focusRc = focusedView.getFocusRc();
		XulUtils.offsetRect(focusRc, -(_screenX + _padding.left + _rect.left), -(_screenY + _padding.top + _rect.top));
		{
			if (focusRc.left < 0) {
				// scrollContentTo(_scrollPos - focusRc.left, 0);
			} else if (focusRc.right + _padding.left > XulUtils.calRectWidth(_rect) - _padding.right) {
				// scrollContentTo(_scrollPos + (_rect.width() - _padding.right) - (focusRc.right + _padding.left), 0);
			}
		}
		return false;
	}

	@Override
	public void onVisibilityChanged(boolean isVisible, XulView eventSource) {
		super.onVisibilityChanged(isVisible, eventSource);
		XulAreaChildrenVisibleChangeNotifier notifier = XulAreaChildrenVisibleChangeNotifier.getNotifier();
		notifier.begin(isVisible, (XulArea) eventSource);
		_area.eachChild(notifier);
		notifier.end();
	}

	@Override
	public XulView postFindFocus(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		XulView lastChild = _area.getLastChild();
		if (lastChild == null) {
			return null;
		}

		if (_area.testFocusModeBits(XulFocus.MODE_WRAP)) {
			RectF newRc = XulDC._tmpFRc0;
			XulUtils.copyRect(srcRc, newRc);
			RectF areaFocus = _area.getFocusRc();

			if (!_isVertical) {
				if (direction == XulLayout.FocusDirection.MOVE_LEFT) {
					XulUtils.offsetRect(newRc, XulUtils.calRectWidth(areaFocus), -XulUtils.calRectHeight(newRc));
				} else if (direction == XulLayout.FocusDirection.MOVE_RIGHT) {
					XulUtils.offsetRect(newRc, -XulUtils.calRectWidth(areaFocus), XulUtils.calRectHeight(newRc));
				} else {
					newRc = null;
				}
			} else {
				if (direction == XulLayout.FocusDirection.MOVE_UP) {
					XulUtils.offsetRect(newRc, -XulUtils.calRectWidth(newRc), XulUtils.calRectHeight(areaFocus));
				} else if (direction == XulLayout.FocusDirection.MOVE_DOWN) {
					XulUtils.offsetRect(newRc, XulUtils.calRectWidth(newRc), -XulUtils.calRectHeight(areaFocus));
				} else {
					newRc = null;
				}
			}
			if (newRc != null) {
				XulView focusView = _area.findSubFocusByDirection(direction, newRc, src);
				if (focusView != null) {
					return focusView;
				}
			}

		}

		if (_area.testFocusModeBits(XulFocus.MODE_LOOP)) {
			RectF newRc = XulDC._tmpFRc0;
			XulUtils.copyRect(srcRc, newRc);
			RectF areaFocus = _area.getFocusRc();
			XulView firstChild = _area.getFirstChild();
			if (lastChild == src || src.isChildOf(lastChild)) {
				if (!_isVertical && direction == XulLayout.FocusDirection.MOVE_RIGHT) {
					newRc.offsetTo(areaFocus.left - XulUtils.calRectWidth(newRc), areaFocus.top);
				} else if (_isVertical && direction == XulLayout.FocusDirection.MOVE_DOWN) {
					newRc.offsetTo(areaFocus.left, areaFocus.top - XulUtils.calRectHeight(newRc));
				} else {
					newRc = null;
				}
			} else if (firstChild == src || src.isChildOf(firstChild)) {
				RectF lastChildFocusRc = lastChild.getFocusRc();
				if (!_isVertical && direction == XulLayout.FocusDirection.MOVE_LEFT) {
					newRc.offsetTo(lastChildFocusRc.right, lastChildFocusRc.top);
				} else if (_isVertical && direction == XulLayout.FocusDirection.MOVE_UP) {
					newRc.offsetTo(lastChildFocusRc.left, lastChildFocusRc.bottom);
				} else {
					newRc = null;
				}
			} else {
				newRc = null;
			}
			if (newRc != null) {
				return _area.findSubFocusByDirection(direction, newRc, src);
			}
		}

		{
			RectF rc = XulDC._tmpFRc0;
			XulUtils.copyRect(lastChild.getFocusRc(), rc);
			if (_isVertical) {
				if (lastChild == src || src.isChildOf(lastChild)) {
					if (direction == XulLayout.FocusDirection.MOVE_DOWN) {
						rc.offsetTo(rc.left - XulUtils.calRectWidth(rc), rc.top);
					} else {
						return null;
					}
				} else if (direction == XulLayout.FocusDirection.MOVE_RIGHT) {
					rc.offsetTo(srcRc.left, rc.top);
				} else {
					return null;
				}
			} else {
				if (lastChild == src || src.isChildOf(lastChild)) {
					if (direction == XulLayout.FocusDirection.MOVE_RIGHT) {
						rc.offsetTo(rc.left, rc.top - XulUtils.calRectHeight(rc));
					} else {
						return null;
					}
				} else if (direction == XulLayout.FocusDirection.MOVE_DOWN) {
					rc.offsetTo(rc.left, srcRc.top);
				} else {
					return null;
				}
			}
			return _area.findSubFocusByDirection(direction, rc, src);
		}
	}

	protected class LayoutContainer extends XulViewContainerBaseRender.LayoutContainer {
		@Override
		public int prepare() {
			super.prepare();
			syncDirection();
			return 0;
		}

		@Override
		public int getLayoutMode() {
			if (_isRTL() && !_isVertical) {
				return XulLayoutHelper.MODE_GRID_INVERSE_HORIZONTAL;
			}
			if (_isVertical) {
				return XulLayoutHelper.MODE_GRID_VERTICAL;
			}
			return XulLayoutHelper.MODE_GRID_HORIZONTAL;
		}
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutContainer();
	}

}