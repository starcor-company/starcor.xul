package com.starcor.xul;

import android.graphics.RectF;
import android.util.Log;

import com.starcor.xul.Events.XulActionEvent;
import com.starcor.xul.Events.XulStateChangeEvent;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Prop.*;
import com.starcor.xul.Render.XulRenderFactory;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Script.XulScriptableObject;
import com.starcor.xul.ScriptWrappr.*;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulSimpleArray;
import com.starcor.xul.Utils.XulSimpleStack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hy on 2014/5/4.
 */
public class XulArea extends XulView {
	private static final String TAG = XulArea.class.getSimpleName();
	public static final XulAreaIterator CLEAN_CHILDREN_BINDING_ITERATOR = new XulAreaIterator() {
		@Override
		public boolean onXulArea(int pos, XulArea area) {
			area.cleanBindingCtx();
			return true;
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			item.cleanBindingCtx();
			return true;
		}

		@Override
		public boolean onXulTemplate(int pos, XulTemplate template) {
			template.cleanBindingCtx();
			return true;
		}
	};

	static class XulElementArray extends XulSimpleArray<XulElement> {

		@Override
		protected XulElement[] allocArrayBuf(int size) {
			return new XulElement[size];
		}
	}

	XulElementArray _children = new XulElementArray();
	WeakReference<XulView> _lastFocusedChild;

	public void onChildDoActionEvent(XulActionEvent event) {
		if (_render == null) {
			return;
		}
		_render.onChildDoActionEvent(event);
	}

	public boolean hasChild(XulView view) {
		if (view == null) {
			return false;
		}

		XulArea viewParent = view._parent;
		while (viewParent != null) {
			if (viewParent == this) {
				return true;
			}
			viewParent = viewParent._parent;
		}
		return false;
	}

	public void removeChild(XulView view) {
		_children.remove(view);
		resetRender();
	}

	public void removeChildren(ArrayList<XulView> items) {
		_children.removeAll(items);
		resetRender();
	}

	public void removeChildren(int first, int last) {
		removeChildren(first, last, null);
	}

	public void removeChildren(int first, int last, List<Object> removedItems) {
		if (first < 0) {
			first = 0;
		}
		if (last > _children.size()) {
			last = _children.size();
		}
		if (removedItems != null) {
			while (last-- > first) {
				Object item = _children.remove(last);
				removedItems.add(item);
			}

		} else {
			while (last-- > first) {
				_children.remove(last);
			}
		}
		resetRender();
	}

	public void removeAllChildren() {
		_children.clear();
	}

	public void removeAllChildrenUpdateSelector() {
		if (_children.isEmpty()) {
			return;
		}
		final XulPage ownerPage = getOwnerPage();
		eachView(new XulViewIterator() {
			@Override
			public boolean onXulView(int pos, XulView view) {
				ownerPage.removeSelectorTarget(view, view.getSelectKeys());
				if (view instanceof XulArea) {
					((XulArea) view).eachView(this);
				}
				view.internalDestroy();
				view._parent = null;
				return true;
			}
		});
		_children.clear();
	}

	@Override
	public void removeSelf() {
		final XulPage ownerPage = getOwnerPage();
		if (this.hasFocus()) {
			ownerPage.getLayout().requestFocus(null);
		}
		if (_parent == null) {
			return;
		}
		eachView(new XulViewIterator() {
			@Override
			public boolean onXulView(int pos, XulView view) {
				ownerPage.removeSelectorTarget(view, view.getSelectKeys());
				if (view instanceof XulArea) {
					((XulArea) view).eachView(this);
				}
				view.internalDestroy();
				view._parent = null;
				return true;
			}
		});
		ownerPage.removeSelectorTarget(this, getSelectKeys());
		this.internalDestroy();
		_parent.removeChild(this);
		_parent = null;
	}

	public XulView getDynamicFocus() {
		WeakReference<XulView> lastFocusedChild = _lastFocusedChild;
		if (lastFocusedChild == null) {
			return null;
		}
		XulView xulView = lastFocusedChild.get();
		if (xulView == null) {
			_lastFocusedChild = null;
			return null;
		}
		if (xulView.isVisible() && xulView.isParentVisible()) {
			return xulView;
		}
		return null;
	}

	public int getChildNum() {
		return _children.size();
	}

	public XulView getChild(int idx) {
		XulElement o = _children.get(idx);
		if (o instanceof XulView) {
			return (XulView) o;
		}
		return null;
	}

	public final XulLayoutHelper.ILayoutElement getVisibleChildLayoutElement(int idx) {
		XulElement xulElement = _children.get(idx);
		if (!(xulElement instanceof XulView)) {
			return null;
		}
		XulViewRender render = ((XulView) xulElement)._render;
		if (render == null) {
			Log.w(TAG, "Element not initialized!! NULL render. " + xulElement);
			return null;
		}
		if (!render.isVisible()) {
			return null;
		}
		return render.getLayoutElement();
	}

	public final int getAllVisibleChildLayoutElement(XulSimpleArray<XulLayoutHelper.ILayoutElement> array) {
		int count = 0;
		for (int i = 0, childrenNum = _children.size(); i < childrenNum; ++i) {
			XulElement xulElement = _children.get(i);
			if (!(xulElement instanceof XulView)) {
				continue;
			}
			XulViewRender render = ((XulView) xulElement)._render;
			if (render == null) {
				Log.w(TAG, "Element not initialized!! NULL render. " + xulElement);
				continue;
			}
			if (!render.isVisible()) {
				continue;
			}
			++count;
			array.add(render.getLayoutElement());
		}
		return count;
	}

	public final XulLayoutHelper.ILayoutElement getChildLayoutElement(int idx) {
		XulElement xulElement = _children.get(idx);
		if (!(xulElement instanceof XulView)) {
			return null;
		}
		XulViewRender render = ((XulView) xulElement)._render;
		if (render == null) {
			Log.w(TAG, "Element not initialized!! NULL render. " + xulElement);
			return null;
		}
		return render.getLayoutElement();
	}

	public static class XulAreaIterator {
		public boolean onXulArea(int pos, XulArea area) {
			return true;
		}

		public boolean onXulItem(int pos, XulItem item) {
			return true;
		}

		public boolean onXulTemplate(int pos, XulTemplate template) {
			return true;
		}
	}

	public static class XulViewIterator {
		public boolean onXulView(int pos, XulView view) {
			return true;
		}
	}

	public abstract static class focusDirectionFilter {
		XulView _src;
		RectF _srcRc;

		focusDirectionFilter(XulView src, RectF srcRc) {
			_src = src;
			_srcRc = srcRc;
		}

		abstract boolean test(XulView view, RectF rc);

		abstract float distance(RectF rc);
	}

	public static class leftFocusFilter extends focusDirectionFilter {

		public leftFocusFilter(XulView src, RectF srcRc) {
			super(src, srcRc);
		}

		@Override
		boolean test(XulView view, RectF rc) {
			if (_src == view) {
				return false;
			}

			float srcVCenter = (_srcRc.top + _srcRc.bottom) / 2;
			float targetVCenter = (rc.top + rc.bottom) / 2;

			if ((targetVCenter < _srcRc.top || targetVCenter > _srcRc.bottom) && (srcVCenter < rc.top || srcVCenter > rc.bottom)) {
				// 纵向差距过大
				return false;
			}

			if ((rc.left + rc.right) / 2 >= _srcRc.left) {
				return false;
			}

			return true;
		}

		@Override
		float distance(RectF rc) {
			return _srcRc.left - rc.right;
		}
	}

	public static class rightFocusFilter extends focusDirectionFilter {

		public rightFocusFilter(XulView src, RectF srcRc) {
			super(src, srcRc);
		}

		@Override
		boolean test(XulView view, RectF rc) {
			if (_src == view) {
				return false;
			}

			float srcVCenter = (_srcRc.top + _srcRc.bottom) / 2;
			float targetVCenter = (rc.top + rc.bottom) / 2;

			if ((targetVCenter < _srcRc.top || targetVCenter > _srcRc.bottom) && (srcVCenter < rc.top || srcVCenter > rc.bottom)) {
				// 纵向差距过大
				return false;
			}

			if ((rc.left + rc.right) / 2 <= _srcRc.right) {
				return false;
			}

			return true;
		}

		@Override
		float distance(RectF rc) {
			return rc.left - _srcRc.right;
		}

	}

	public static class topFocusFilter extends focusDirectionFilter {

		public topFocusFilter(XulView src, RectF srcRc) {
			super(src, srcRc);
		}

		@Override
		boolean test(XulView view, RectF rc) {
			if (_src == view) {
				return false;
			}

			float srcHCenter = (_srcRc.left + _srcRc.right) / 2;
			float targetHCenter = (rc.left + rc.right) / 2;

			if ((targetHCenter < _srcRc.left || targetHCenter > _srcRc.right) && (srcHCenter < rc.left || srcHCenter > rc.right)) {
				// 横向差距过大
				return false;
			}

			if ((rc.top + rc.bottom) / 2 >= _srcRc.top) {
				return false;
			}

			return true;
		}

		@Override
		float distance(RectF rc) {
			return _srcRc.top - rc.bottom;
		}

	}

	public static class bottomFocusFilter extends focusDirectionFilter {

		public bottomFocusFilter(XulView src, RectF srcRc) {
			super(src, srcRc);
		}

		@Override
		boolean test(XulView view, RectF rc) {
			if (_src == view) {
				return false;
			}

			float srcHCenter = (_srcRc.left + _srcRc.right) / 2;
			float targetHCenter = (rc.left + rc.right) / 2;

			if ((targetHCenter < _srcRc.left || targetHCenter > _srcRc.right) && (srcHCenter < rc.left || srcHCenter > rc.right)) {
				// 横向差距过大
				return false;
			}

			if ((rc.top + rc.bottom) / 2 <= _srcRc.bottom) {
				return false;
			}

			return true;
		}

		@Override
		float distance(RectF rc) {
			return rc.top - _srcRc.bottom;
		}

	}

	public static class leftFocusFilterEx extends leftFocusFilter {

		public leftFocusFilterEx(XulView src, RectF srcRc) {
			super(src, srcRc);
		}

		@Override
		boolean test(XulView view, RectF rc) {
			if (_src == view) {
				return false;
			}

			if ((rc.left + rc.right) / 2 >= _srcRc.left) {
				return false;
			}

			return true;
		}

		@Override
		float distance(RectF rc) {
			float srcVCenter = (_srcRc.top + _srcRc.bottom) / 2;
			float targetVCenter = (rc.top + rc.bottom) / 2;
			float dx = _srcRc.left - rc.right;
			float dy = srcVCenter - targetVCenter;
			return dx * dx + dy * dy;
		}
	}

	public static class rightFocusFilterEx extends rightFocusFilter {

		public rightFocusFilterEx(XulView src, RectF srcRc) {
			super(src, srcRc);
		}

		@Override
		boolean test(XulView view, RectF rc) {
			if (_src == view) {
				return false;
			}

			if ((rc.left + rc.right) / 2 <= _srcRc.right) {
				return false;
			}

			return true;
		}

		@Override
		float distance(RectF rc) {
			float srcVCenter = (_srcRc.top + _srcRc.bottom) / 2;
			float targetVCenter = (rc.top + rc.bottom) / 2;
			float dx = rc.left - _srcRc.right;
			float dy = srcVCenter * targetVCenter;
			return dx * dx + dy * dy;
		}

	}

	public static class topFocusFilterEx extends topFocusFilter {

		public topFocusFilterEx(XulView src, RectF srcRc) {
			super(src, srcRc);
		}

		@Override
		boolean test(XulView view, RectF rc) {
			if (_src == view) {
				return false;
			}

			if ((rc.top + rc.bottom) / 2 >= _srcRc.top) {
				return false;
			}

			return true;
		}

		@Override
		float distance(RectF rc) {
			float srcHCenter = (_srcRc.left + _srcRc.right) / 2;
			float targetHCenter = (rc.left + rc.right) / 2;
			float dx = srcHCenter - targetHCenter;
			float dy = _srcRc.top - rc.bottom;
			return dx * dx + dy * dy;
		}

	}

	public static class bottomFocusFilterEx extends bottomFocusFilter {

		public bottomFocusFilterEx(XulView src, RectF srcRc) {
			super(src, srcRc);
		}

		@Override
		boolean test(XulView view, RectF rc) {
			if (_src == view) {
				return false;
			}

			if ((rc.top + rc.bottom) / 2 <= _srcRc.bottom) {
				return false;
			}

			return true;
		}

		@Override
		float distance(RectF rc) {
			float srcHCenter = (_srcRc.left + _srcRc.right) / 2;
			float targetHCenter = (rc.left + rc.right) / 2;
			float dx = srcHCenter - targetHCenter;
			float dy = rc.top - _srcRc.bottom;
			return dx * dx + dy * dy;
		}

	}

	private static class _FocusFilterIterator extends XulAreaIterator {
		private focusDirectionFilter _filter;

		static class _FocusItem {
			XulView view;
			RectF rect;

			public _FocusItem(XulView view, RectF rect) {
				this.view = view;
				this.rect = rect;
			}
		}

		static class _FocusItemList extends XulSimpleArray<_FocusItem> {

			public _FocusItemList(int sz) {
				super(sz);
			}

			@Override
			protected _FocusItem[] allocArrayBuf(int size) {
				return new _FocusItem[size];
			}


			int _itemNum = 0;

			public void add(XulView view, RectF rect) {
				if (_itemNum >= size()) {
					add(new _FocusItem(view, rect));
					++_itemNum;
				} else {
					_FocusItem focusItem = get(_itemNum);
					focusItem.view = view;
					focusItem.rect = rect;
					++_itemNum;
				}
			}

			public void clear() {
				if (_itemNum == 0) {
					return;
				}
				for (int i = 0; i < _itemNum; i++) {
					_FocusItem item = get(i);
					item.view = null;
					item.rect = null;
				}
				_itemNum = 0;
			}

			public int itemNum() {
				return _itemNum;
			}
		}

		_FocusItemList _targets = new _FocusItemList(128);
		RectF _srcRc;
		XulView _src;

		@Override
		public boolean onXulArea(int pos, XulArea area) {
			if (!area.isEnabled() || !area.isVisible()) {
				return true;
			}
			XulViewRender render = area.getRender();
			if (render == null || render.getDrawingRect() == null) {
				return false;
			}
			RectF focusRc = area.getFocusRc();
			if (_filter.test(area, focusRc)) {
				_targets.add(area, focusRc);
			}
			return true;
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			if (!item.isEnabled() || !item.isVisible() || !item.focusable()) {
				return true;
			}
			XulViewRender render = item.getRender();
			if (render == null || render.getDrawingRect() == null) {
				return false;
			}
			RectF focusRc = item.getFocusRc();
			if (_filter.test(item, focusRc)) {
				_targets.add(item, focusRc);
			}
			return true;
		}

		public void begin(focusDirectionFilter filter, RectF srcRc, XulView src) {
			_filter = filter;
			_srcRc = srcRc;
			_src = src;
			_targets.clear();
		}

		public XulView end() {
			XulView result = null;
			float distance = Float.MAX_VALUE;
			int size = _targets.itemNum();
			for (int i = 0; i < size; i++) {
				_FocusItem item = _targets.get(i);
				if (item.view.focusable()) {
					float d = _filter.distance(item.rect);
					if (d < distance) {
						result = item.view;
						distance = d;
					}
				} else {
					XulView view = ((XulArea) item.view).findSubFocus(_filter, _srcRc, _src);
					if (view != null) {
						float d = _filter.distance(view.getFocusRc());
						if (d < distance) {
							result = view;
							distance = d;
						}
					}
				}
			}
			_filter = null;
			_srcRc = null;
			_src = null;
			_targets.clear();
			return result;
		}
	}

	private static _FocusFilterIterator _focusFilterIterator = new _FocusFilterIterator();

	public XulView findFocus(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		if (!isEnabled()) {
			return null;
		}
		XulView result = null;
		while (result == null) {
			if (_render != null) {
				result = _render.preFindFocus(direction, srcRc, src);
				if (result == XulItem.KEEP_FOCUS) {
					result = getRootLayout().getFocus();
					break;
				}
			}

			if (result != XulItem.NULL_FOCUS) {
				if (result != null) {
					break;
				}
				focusDirectionFilter directionFilter = createFocusDirectionFilter(direction, srcRc, src);

				_focusFilterIterator.begin(directionFilter, srcRc, src);
				eachChild(_focusFilterIterator);
				result = _focusFilterIterator.end();
				if (result != null) {
					break;
				}
			}
			// TODO: 通过focus绑定关系查找新焦点元素
			if (_render != null) {
				result = _render.postFindFocus(direction, srcRc, src);
				if (result == XulItem.KEEP_FOCUS) {
					result = getRootLayout().getFocus();
					break;
				}
				if (result != null) {
					break;
				}
			}
			XulArea parent = _parent;
			while (parent != null &&
				parent.getChildNum() == 1 &&
				(!parent.testFocusModeBits(XulFocus.MODE_DYNAMIC) && parent.getDynamicFocus() != null)
				) {
				parent = parent._parent;
			}
			if (parent != null) {
				result = parent.findFocus(direction, srcRc, this);
			}
			break;
		}
		return result;
	}

	private focusDirectionFilter createFocusDirectionFilter(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		focusDirectionFilter directionFilter = null;
		boolean extendNearby = testFocusModeBits(XulFocus.MODE_EXT_NEARBY);
		switch (direction) {
		case MOVE_LEFT:
			directionFilter = extendNearby ? new leftFocusFilterEx(src, srcRc) : new leftFocusFilter(src, srcRc);
			break;
		case MOVE_RIGHT:
			directionFilter = extendNearby ? new rightFocusFilterEx(src, srcRc) : new rightFocusFilter(src, srcRc);
			break;
		case MOVE_UP:
			directionFilter = extendNearby ? new topFocusFilterEx(src, srcRc) : new topFocusFilter(src, srcRc);
			break;
		case MOVE_DOWN:
			directionFilter = extendNearby ? new bottomFocusFilterEx(src, srcRc) : new bottomFocusFilter(src, srcRc);
			break;
		}
		return directionFilter;
	}

	static private boolean testBits(int val, int bits) {
		return (val & bits) == bits;
	}

	public XulView findSubFocus(focusDirectionFilter filter, RectF srcRc, XulView src) {
		if (!isEnabled()) {
			return null;
		}

		XulView item = null;
		int focusMode = this.getFocusMode();
		XulView dynamicFocusItem = this._lastFocusedChild != null ? this._lastFocusedChild.get() : null;
		switch (focusMode & XulFocus.MODE_SEARCH_ORDER_MASK) {
		case XulFocus.MODE_SEARCH_ORDER_DPN:
		default:
			if (dynamicFocusItem != null && testBits(focusMode, XulFocus.MODE_DYNAMIC)
				&& dynamicFocusItem.isVisible() && dynamicFocusItem.isParentVisible()) {
				item = dynamicFocusItem;
				break;
			}
			if (testBits(focusMode, XulFocus.MODE_PRIORITY)) {
				item = findSubFocusByPriority(this, 0);
				if (item != null) {
					break;
				}
			}
			if (testBits(focusMode, XulFocus.MODE_NEARBY)) {
				item = findSubFocusByFilter(this, filter);
			}
			break;
		case XulFocus.MODE_SEARCH_ORDER_DNP:
			if (dynamicFocusItem != null && testBits(focusMode, XulFocus.MODE_DYNAMIC)
				&& dynamicFocusItem.isVisible() && dynamicFocusItem.isParentVisible()) {
				item = dynamicFocusItem;
				break;
			}
			if (testBits(focusMode, XulFocus.MODE_NEARBY)) {
				item = findSubFocusByFilter(this, filter);
				if (item != null) {
					break;
				}
			}
			if (testBits(focusMode, XulFocus.MODE_PRIORITY)) {
				item = findSubFocusByPriority(this, 0);
			}
			break;
		case XulFocus.MODE_SEARCH_ORDER_NDP:
			if (testBits(focusMode, XulFocus.MODE_NEARBY)) {
				item = findSubFocusByFilter(this, filter);
				if (item != null) {
					break;
				}
			}
			if (dynamicFocusItem != null && testBits(focusMode, XulFocus.MODE_DYNAMIC)
				&& dynamicFocusItem.isVisible() && dynamicFocusItem.isParentVisible()) {
				item = dynamicFocusItem;
				break;
			}
			if (testBits(focusMode, XulFocus.MODE_PRIORITY)) {
				item = findSubFocusByPriority(this, 0);
			}
			break;
		case XulFocus.MODE_SEARCH_ORDER_NPD:
			if (testBits(focusMode, XulFocus.MODE_NEARBY)) {
				item = findSubFocusByFilter(this, filter);
				if (item != null) {
					break;
				}
			}
			if (testBits(focusMode, XulFocus.MODE_PRIORITY)) {
				item = findSubFocusByPriority(this, 0);
				if (item != null) {
					break;
				}
			}
			if (dynamicFocusItem != null && testBits(focusMode, XulFocus.MODE_DYNAMIC)
				&& dynamicFocusItem.isVisible() && dynamicFocusItem.isParentVisible()) {
				item = dynamicFocusItem;
				break;
			}
			break;
		case XulFocus.MODE_SEARCH_ORDER_PDN:
			if (testBits(focusMode, XulFocus.MODE_PRIORITY)) {
				item = findSubFocusByPriority(this, 0);
				if (item != null) {
					break;
				}
			}
			if (dynamicFocusItem != null && testBits(focusMode, XulFocus.MODE_DYNAMIC)
				&& dynamicFocusItem.isVisible() && dynamicFocusItem.isParentVisible()) {
				item = dynamicFocusItem;
				break;
			}
			if (testBits(focusMode, XulFocus.MODE_NEARBY)) {
				item = findSubFocusByFilter(this, filter);
			}
			break;
		case XulFocus.MODE_SEARCH_ORDER_PND:
			if (testBits(focusMode, XulFocus.MODE_PRIORITY)) {
				item = findSubFocusByPriority(this, 0);
				if (item != null) {
					break;
				}
			}
			if (testBits(focusMode, XulFocus.MODE_NEARBY)) {
				item = findSubFocusByFilter(this, filter);
				if (item != null) {
					break;
				}
			}
			if (dynamicFocusItem != null && testBits(focusMode, XulFocus.MODE_DYNAMIC)
				&& dynamicFocusItem.isVisible() && dynamicFocusItem.isParentVisible()) {
				item = dynamicFocusItem;
				break;
			}
			break;
		}

		return item;
	}

	public XulView findSubFocus(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		if (!isEnabled()) {
			return null;
		}
		focusDirectionFilter directionFilter = createFocusDirectionFilter(direction, srcRc, src);
		return findSubFocus(directionFilter, srcRc, src);
	}

	public XulView findSubFocusByDirection(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		if (!isEnabled()) {
			return null;
		}
		focusDirectionFilter directionFilter = createFocusDirectionFilter(direction, srcRc, src);
		return findSubFocusByFilter(this, directionFilter);
	}

	private static class _FindSubFocusByFilter extends XulAreaIterator {
		private ArrayList<XulView> _result = new ArrayList<XulView>();
		private focusDirectionFilter _filter;

		void begin(focusDirectionFilter filter) {
			_filter = filter;
			_result.clear();
		}

		XulView end() {
			XulView target = null;
			float distance = Float.MAX_VALUE;
			int resultSize = _result.size();
			for (int i = 0; i < resultSize; i++) {
				XulView view = _result.get(i);
				RectF rc = view.getFocusRc();
				float d = _filter.distance(rc);
				if (d < distance) {
					target = view;
					distance = d;
				}
			}
			_result.clear();
			_filter = null;
			return target;
		}

		private void _addResult(XulView view) {
			_result.add(view);
		}

		@Override
		public boolean onXulArea(int pos, XulArea area) {
			if (!area.isEnabled() || !area.isVisible()) {
				return true;
			}
			if (area.focusable()) {
				if (_filter.test(area, area.getFocusRc())) {
					_addResult(area);
				}
			} else if (area.isEnabled()) {
				area.eachChild(this);
			}
			return true;
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			if (!item.isEnabled() || !item.isVisible() || !item.focusable()) {
				return true;
			}
			if (_filter.test(item, item.getFocusRc())) {
				_addResult(item);
			}
			return true;
		}
	}

	private static _FindSubFocusByFilter _findSubFocusByFilter = new _FindSubFocusByFilter();

	public static XulView findSubFocusByFilter(XulArea parentArea, focusDirectionFilter filter) {
		if (!parentArea.isEnabled()) {
			return null;
		}
		_findSubFocusByFilter.begin(filter);
		parentArea.eachChild(_findSubFocusByFilter);
		return _findSubFocusByFilter.end();
	}

	@Override
	public int getDefaultFocusMode() {
		if (_parent != null) {
			// 不继承父元素的focusable,dynamic属性
			return _parent.getFocusMode() & ~(XulFocus.MODE_FOCUSABLE | XulFocus.MODE_DYNAMIC);
		}
		return super.getDefaultFocusMode();
	}

	private static class _FindSubFocusByPriorityIterator extends XulAreaIterator {
		int _maxPriority;
		XulView _result;
		XulArea _parentArea;
		ArrayList<Object> _findStack = new ArrayList<Object>();

		@Override
		public boolean onXulArea(int pos, XulArea area) {
			if (!area.isEnabled() || !area.isVisible()) {
				return true;
			}
			if (area.focusable()) {
				int focusPriority = area.getFocusPriority();
				if (focusPriority > _maxPriority) {
					_maxPriority = focusPriority;
					_result = area;
				}
			} else {
				XulView view = findSubFocusByPriority(area, _maxPriority);
				if (view != null) {
					XulArea parent = view._parent;
					int basePriority = 0;
					while (parent != null && parent != _parentArea) {
						int parentFocusPriority = parent.getFocusPriority();
						if (parentFocusPriority > 0) {
							basePriority += parentFocusPriority;
						}
						parent = parent._parent;
					}
					int viewFocusPriority = view.getFocusPriority();
					if (viewFocusPriority < 0) {
						viewFocusPriority = 0;
					}
					_maxPriority = basePriority + viewFocusPriority;
					_result = view;
				}
			}
			return true;
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			if (!item.isEnabled() || !item.isVisible()) {
				return true;
			}
			if (item.focusable()) {
				int focusPriority = item.getFocusPriority();
				if (focusPriority > _maxPriority) {
					_maxPriority = focusPriority;
					_result = item;
				}
			}
			return true;
		}

		public void begin(XulArea parentArea, int priorityLimit) {
			_findStack.add(_result);
			_findStack.add(_parentArea);
			_findStack.add(_maxPriority);

			_result = null;
			_parentArea = parentArea;
			_maxPriority = priorityLimit;
		}

		public XulView end() {
			XulView result = _result;
			_maxPriority = (Integer) _findStack.remove(_findStack.size() - 1);
			_parentArea = (XulArea) _findStack.remove(_findStack.size() - 1);
			_result = (XulView) _findStack.remove(_findStack.size() - 1);
			return result;
		}
	}

	static _FindSubFocusByPriorityIterator _findSubFocusByPriorityIterator = new _FindSubFocusByPriorityIterator();

	public static XulView findSubFocusByPriority(XulArea parentArea, int priorityLimit) {
		if (!parentArea.isEnabled()) {
			return null;
		}
		int focusMode = parentArea.getFocusMode();
		XulView item;

		XulView dynamicFocusItem = parentArea._lastFocusedChild != null ? parentArea._lastFocusedChild.get() : null;
		switch (focusMode & XulFocus.MODE_SEARCH_ORDER_MASK) {
		case XulFocus.MODE_SEARCH_ORDER_DPN:
		case XulFocus.MODE_SEARCH_ORDER_DNP:
		case XulFocus.MODE_SEARCH_ORDER_NDP:
		default:
			if (dynamicFocusItem != null && testBits(focusMode, XulFocus.MODE_DYNAMIC)
				&& dynamicFocusItem.isVisible() && dynamicFocusItem.isParentVisible()) {
				item = dynamicFocusItem;
				break;
			}

			if (parentArea.getFocusPriority() > 0) {
				priorityLimit -= parentArea.getFocusPriority();
			}
			_findSubFocusByPriorityIterator.begin(parentArea, priorityLimit);
			parentArea.eachChild(_findSubFocusByPriorityIterator);
			item = _findSubFocusByPriorityIterator.end();
			break;

		case XulFocus.MODE_SEARCH_ORDER_NPD:
		case XulFocus.MODE_SEARCH_ORDER_PDN:
		case XulFocus.MODE_SEARCH_ORDER_PND:
			if (parentArea.getFocusPriority() > 0) {
				priorityLimit -= parentArea.getFocusPriority();
			}
			_findSubFocusByPriorityIterator.begin(parentArea, priorityLimit);
			parentArea.eachChild(_findSubFocusByPriorityIterator);
			item = _findSubFocusByPriorityIterator.end();
			if (item != null) {
				break;
			}
			if (dynamicFocusItem != null && testBits(focusMode, XulFocus.MODE_DYNAMIC)
				&& dynamicFocusItem.isVisible() && dynamicFocusItem.isParentVisible()) {
				item = dynamicFocusItem;
				break;
			}
			break;
		}
		return item;
	}

	@Override
	public void prepareRender(XulRenderContext ctx, boolean preload) {
		if (_render != null) {
			return;
		}

		_render = XulRenderFactory.createRender("area", _type, ctx, this, preload);
		_render.onRenderCreated();
		prepareChildrenRender(ctx);
	}

	public void prepareChildrenRender(XulRenderContext ctx) {
		for (int i = 0; i < _children.size(); i++) {
			Object child = _children.get(i);
			if (child instanceof XulItem) {
				XulItem xulItem = (XulItem) child;
				xulItem.prepareRender(ctx);
			} else if (child instanceof XulArea) {
				XulArea xulArea = (XulArea) child;
				xulArea.prepareRender(ctx);
			}
		}
	}

	public XulArea() {
	}

	public XulArea(XulArea parentArea, int pos) {
		super(parentArea._root, parentArea);
		parentArea.addChild(pos, this);
	}

	public XulArea(XulArea parentArea, int pos, String type) {
		super(parentArea._root, parentArea);
		_type = type;
		parentArea.addChild(pos, this);
	}

	public XulArea(XulArea parent) {
		super(parent);
		parent.addChild(this);
	}

	public XulArea(XulArea parent, boolean prepend) {
		super(parent);
		parent.addChild(this, prepend);
	}

	public XulArea(XulLayout root) {
		super(root);
		root.addChild(this);
	}

	public XulArea(XulLayout root, XulArea parent) {
		super(root, parent);
		parent.addChild(this);
	}

	public XulArea makeClone(XulArea parent) {
		return makeClone(parent, -1);
	}

	public XulArea makeClone(XulArea parent, int pos) {
		XulArea xulArea = new XulArea(parent, pos);
		xulArea.copyContent(this);
		int childrenSize = _children.size();
		for (int i = 0; i < childrenSize; i++) {
			Object obj = _children.get(i);
			if (obj instanceof XulArea) {
				XulArea area = (XulArea) obj;
				area.makeClone(xulArea);
			} else if (obj instanceof XulTemplate) {
				XulTemplate template = (XulTemplate) obj;
				template.makeClone(xulArea);
			} else if (obj instanceof XulItem) {
				XulItem item = (XulItem) obj;
				item.makeClone(xulArea);
			} else {
				Log.d(TAG, "unsupported children type!!! - " + obj.getClass().getSimpleName());
			}
		}
		return xulArea;
	}

	public int findChildPos(XulElement obj) {
		return _children.indexOf(obj);
	}

	public XulView findItemById(String id) {
		XulLayout root = _root;
		if (root == null) {
			return null;
		}
		XulPage page = root._page;
		if (page == null) {
			return null;
		}
		return page.findItemById(this, id);
	}

	public XulArrayList<XulView> findItemsByClass(String cls) {
		XulLayout root = _root;
		if (root == null) {
			return null;
		}
		XulPage page = root._page;
		if (page == null) {
			return null;
		}
		return page.findItemsByClass(this, cls);
	}

	public XulView findCustomItemByExtView(IXulExternalView extView) {
		XulLayout root = _root;
		if (root == null) {
			return null;
		}
		XulPage page = root._page;
		if (page == null) {
			return null;
		}
		XulView customItemByExtView = page.findCustomItemByExtView(extView);
		if (customItemByExtView.isChildOf(this)) {
			return customItemByExtView;
		}
		return null;
	}

	public boolean eachChild(XulAreaIterator iterator) {
		final int childrenSize = _children.size();
		if (childrenSize == 0) {
			return true;
		}
		final XulElement[] array = _children.getArray();
		for (int i = 0; i < childrenSize; ++i) {
			XulElement obj = array[i];
			if (obj instanceof XulArea) {
				if (iterator.onXulArea(i, (XulArea) obj) != true) {
					return false;
				}
			} else if (obj instanceof XulItem) {
				if (iterator.onXulItem(i, (XulItem) obj) != true) {
					return false;
				}
			} else if (obj instanceof XulTemplate) {
				if (iterator.onXulTemplate(i, (XulTemplate) obj) != true) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean eachView(XulViewIterator iter) {
		final int childrenSize = _children.size();
		if (childrenSize == 0) {
			return true;
		}
		final XulElement[] array = _children.getArray();
		for (int i = 0; i < childrenSize; ++i) {
			XulElement element = array[i];
			if (element.elementType == XulElement.VIEW_TYPE &&
				iter.onXulView(i, (XulView) element) != true) {
				return false;
			}
		}
		return true;
	}

	public <T extends XulElement> void addChild(T child) {
		_children.add(child);
	}

	public <T extends XulElement> void addChild(int pos, T obj) {
		if (pos < 0) {
			pos = _children.size();
		}
		_children.add(pos, obj);
	}

	public <T extends XulElement> void addChild(T obj, boolean prepend) {
		_children.add(prepend ? 0 : _children.size(), obj);
	}

	public void onChildStateChanged(XulStateChangeEvent event) {
		if (_render != null) {
			_render.onChildStateChanged(event);
		}

		if ("blur".equals(event.event) && XulView.STATE_FOCUSED == event.oldState && testFocusModeBits(XulFocus.MODE_DYNAMIC)) {
			// 动态focus模式，记录上一次的焦点元素
			_lastFocusedChild = event.eventSource != null ? event.eventSource.getWeakReference() : null;
		}

		if (_parent != null) {
			event.notifySource = this;
			_parent.onChildStateChanged(event);
		}
	}

	private static class _UpdateChildrenFocusState extends XulAreaIterator {
		int state;

		public void init(int state) {
			this.state = state;
		}

		@Override
		public boolean onXulArea(int pos, XulArea area) {
			area.updateFocusState(state);
			return true;
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			item.updateFocusState(state);
			return true;
		}
	}

	private static _UpdateChildrenFocusState _updateChildrenFocusState = new _UpdateChildrenFocusState();

	void updateFocusState(int state) {
		super.updateFocusState(state);
		_updateChildrenFocusState.init(state);
		eachChild(_updateChildrenFocusState);
	}

	public static class _Builder extends ItemBuilder {
		XulArea _area;
		XulTemplate _ownerTemplate;

		private void init(XulLayout layout) {
			_area = createXulArea(layout._root, layout);
		}

		private void init(XulArea area) {
			_area = createXulArea(area._root, area);
		}

		private void init(XulTemplate template) {
			_area = createXulArea();
			_ownerTemplate = template;
		}

		XulArea createXulArea(XulLayout layout, XulArea parent) {
			return new XulArea(layout, parent);
		}

		XulArea createXulArea() {
			return new XulArea();
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_area._id = attrs.getValue("id");
			_area.setClass(attrs.getValue("class"));
			_area._type = XulUtils.getCachedString(attrs.getValue("type"));
			_area._binding = XulUtils.getCachedString(attrs.getValue("binding"));
			_area._desc = attrs.getValue("desc");
			if (_ownerTemplate != null) {
				_ownerTemplate.addChild(_area, attrs.getValue("filter"));
			}
			return true;
		}

		@Override
		public Object finalItem() {
			XulArea area = _area;
			this.recycle();
			return area;
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			if ("area".equals(name)) {
				_Builder builder = _Builder.create(_area);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("item".equals(name)) {
				XulItem._Builder builder = XulItem._Builder.create(_area);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("template".equals(name)) {
				XulTemplate._Builder builder = XulTemplate._Builder.create(_area);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("action".equals(name)) {
				XulAction._Builder builder = XulAction._Builder.create(_area);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("data".equals(name)) {
				XulData._Builder builder = XulData._Builder.create(_area);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("attr".equals(name)) {
				XulAttr._Builder builder = XulAttr._Builder.create(_area);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("style".equals(name)) {
				XulStyle._Builder builder = XulStyle._Builder.create(_area);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("focus".equals(name)) {
				XulFocus._Builder builder = XulFocus._Builder.create(_area);
				builder.initialize(name, attrs);
				return builder;
			}

			return XulManager.CommonDummyBuilder;
		}

		public void recycle() {
			_Builder.recycle(this);
		}

		static public _Builder create(XulLayout select) {
			_Builder builder = create();
			builder.init(select);
			return builder;
		}

		public static _Builder create(XulArea view) {
			_Builder builder = create();
			builder.init(view);
			return builder;
		}

		public static _Builder create(XulTemplate view) {
			_Builder builder = create();
			builder.init(view);
			return builder;
		}

		private static _Builder create() {
			_Builder builder = _recycled_builder.pop();
			if (builder == null) {
				builder = new _Builder();
			}
			return builder;
		}

		private static XulSimpleStack<_Builder> _recycled_builder = new XulSimpleStack<_Builder>(256);

		private static void recycle(_Builder builder) {
			_recycled_builder.push(builder);
			builder._area = null;
			builder._ownerTemplate = null;
		}
	}


	private static class _CleanImageItems extends XulAreaIterator {
		@Override
		public boolean onXulArea(int pos, XulArea area) {
			area.cleanImageItems();
			return true;
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			item.cleanImageItems();
			return true;
		}

	}

	private static _CleanImageItems _cleanImageItems = new _CleanImageItems();

	public void cleanImageItems() {
		if (_render == null) {
			return;
		}

		_render.cleanImageItems();
		eachChild(_cleanImageItems);
	}

	private static XulAreaIterator destroyAllChildren = new XulAreaIterator() {

		@Override
		public boolean onXulArea(int pos, XulArea area) {
			area.destroy();
			return true;
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			item.destroy();
			return true;
		}
	};

	public boolean setDynamicFocus(XulView focus) {
		if (focus == null) {
			_lastFocusedChild = null;
			return true;
		}
		if (!this.hasChild(focus)) {
			return false;
		}
		_lastFocusedChild = focus.getWeakReference();
		return true;
	}

	@Override
	public void destroy() {
		eachChild(destroyAllChildren);
		super.destroy();
	}

	@Override
	protected XulScriptableObject createScriptableObject() {
		if ("layer".equals(_type)) {
			return new XulLayerScriptableObject(this);
		}
		if ("page_slider".equals(_type)) {
			return new XulPageSliderScriptableObject(this);
		}
		if ("slider".equals(_type)) {
			return new XulSliderScriptableObject(this);
		}
		if ("massive".equals(_type)) {
			return new XulMassiveScriptableObject(this);
		}
		return new XulAreaScriptableObject(this);
	}

	public XulView getLastChild() {
		if (_children == null || _children.isEmpty()) {
			return null;
		}
		for (int i = _children.size() - 1; i >= 0; i--) {
			Object child = _children.get(i);
			if (child instanceof XulTemplate) {
				continue;
			}
			return (XulView) child;
		}
		return null;
	}

	public XulView getFirstChild() {
		if (_children == null || _children.isEmpty()) {
			return null;
		}
		for (int i = 0, childrenNum = _children.size(); i < childrenNum; i++) {
			Object child = _children.get(i);
			if (child instanceof XulTemplate) {
				continue;
			}
			return (XulView) child;
		}
		return null;
	}

	public XulView getLastFocusableChild() {
		if (_children == null || _children.isEmpty()) {
			return null;
		}
		for (int i = _children.size() - 1; i >= 0; i--) {
			Object child = _children.get(i);
			if (child instanceof XulTemplate) {
				continue;
			}
			XulView view = (XulView) child;
			if (view.focusable()) {
				return view;
			}
			if (view instanceof XulArea && ((XulArea) view).hasFocusableChild()) {
				return view;
			}
		}
		return null;
	}

	public XulView getFirstFocusableChild() {
		if (_children == null || _children.isEmpty()) {
			return null;
		}
		for (int i = 0, childrenNum = _children.size(); i < childrenNum; i++) {
			Object child = _children.get(i);
			if (child instanceof XulTemplate) {
				continue;
			}
			XulView view = (XulView) child;
			if (view.focusable()) {
				return view;
			}
			if (view instanceof XulArea && ((XulArea) view).hasFocusableChild()) {
				return view;
			}
		}
		return null;
	}

	private boolean hasFocusableChild() {
		if (!isEnabled() || !isVisible()) {
			return false;
		}

		if (_children == null || _children.isEmpty()) {
			return false;
		}
		for (int i = _children.size() - 1; i >= 0; i--) {
			Object child = _children.get(i);
			if (child instanceof XulTemplate) {
				continue;
			}
			XulView view = (XulView) child;
			if (view.focusable()) {
				return true;
			}
			if (view instanceof XulArea && ((XulArea) view).hasFocusableChild()) {
				return true;
			}
		}
		return false;
	}

	public boolean hasFocus() {
		if (_root == null) {
			return false;
		}
		XulView focusedView = _root.getFocus();
		if (focusedView == null) {
			return false;
		}
		if (focusedView == this) {
			return true;
		}
		return focusedView.isChildOf(this);
	}

	@Override
	public void cleanBindingCtx() {
		super.cleanBindingCtx();
		eachChild(CLEAN_CHILDREN_BINDING_ITERATOR);
	}
}
