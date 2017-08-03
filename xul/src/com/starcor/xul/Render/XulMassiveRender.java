package com.starcor.xul.Render;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.XulAction;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.Utils.XulSimpleArray;
import com.starcor.xul.*;

import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Created by hy on 2015/5/25.
 */
public class XulMassiveRender extends XulAreaRender {
	private static final String TAG = XulMassiveRender.class.getSimpleName();

	public static void register() {
		XulRenderFactory.registerBuilder("area", "massive", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				return new XulMassiveRender(ctx, (XulArea) view);
			}
		});
	}

	private boolean _initBinding = true;
	private float _skipItemOffsetX = 0;
	private float _skipItemOffsetY = 0;

	private float _clipRangeLeft = 0;
	private float _clipRangeTop = 0;
	private float _clipRangeWidth = 0;
	private float _clipRangeHeight = 0;

	private float _itemOffsetX = 0;
	private float _itemOffsetY = 0;
	private float _scrollTargetX = 0;
	private float _scrollTargetY = 0;
	private float _scrollX = 0;
	private float _scrollY = 0;
	private int _itemIdxOffset = 0;
	// visible item range
	private int _firstItemIdxOffset = 0;
	private int _lastItemIdxOffset = 0;

	private int _contentWidth = 0;
	private int _contentHeight = 0;
	private int _minItemNum = 16;
	private float _cachePages = 0.5f;

	private int _arrangement = XulLayoutHelper.MODE_GRID_HORIZONTAL;
	private boolean _fixedItem = false;
	private boolean _updateItemSize = false;
	private int _lastArrangeXMark = 0;
	private int _lastArrangeYMark = 0;

	WeakHashMap<XulView, ArrayList<XulView>> _instanceCache = new WeakHashMap<XulView, ArrayList<XulView>>();

	private void recycleItem(XulView view) {
		final WeakReference<XulView> refView = view.getRefView();
		assert refView != null;
		final XulView refViewInst = refView.get();
		if (refViewInst == null) {
			Log.d(TAG, "recycle failed! invalid ref view.");
			return;
		}
		ArrayList<XulView> xulViews = _instanceCache.get(refViewInst);
		if (xulViews == null) {
			xulViews = new ArrayList<XulView>();
			_instanceCache.put(refViewInst, xulViews);
		}
		xulViews.add(view);
		if (view.hasFocus()) {
			view.getRootLayout().killFocus();
		}
	}

	@Override
	public void cleanImageItems() {
		// 删除所有缓存对象中的图片
		for (ArrayList<XulView> xulViews : _instanceCache.values()) {
			if (xulViews == null || xulViews.isEmpty()) {
				continue;
			}
			for (XulView view : xulViews) {
				view.cleanImageItems();
			}
		}
		super.cleanImageItems();
	}

	@Override
	public boolean doSuspendRecycle(int recycleLevel) {
		// 删除所有缓存对象中的图片
		for (ArrayList<XulView> xulViews : _instanceCache.values()) {
			if (xulViews == null || xulViews.isEmpty()) {
				continue;
			}
			for (XulView view : xulViews) {
				view.cleanImageItems();
			}
		}
		return super.doSuspendRecycle(recycleLevel);
	}

	XulTemplate _itemTemplate = null;

	XulArea.XulAreaIterator _itemRecycler = new XulArea.XulAreaIterator() {
		@Override
		public boolean onXulArea(int pos, XulArea area) {
			recycleItem(area);
			return super.onXulArea(pos, area);
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			recycleItem(item);
			return super.onXulItem(pos, item);
		}
	};

	List<Object> _itemRecycler_list = new AbstractList<Object>() {
		@Override
		public boolean add(Object object) {
			if (object instanceof XulTemplate) {
				return false;
			}
			assert object instanceof XulView;
			recycleItem((XulView) object);
			return true;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public Object get(int location) {
			return null;
		}
	};

	public int getDataItemNum() {
		if (_data == null) {
			return 0;
		}
		return _data.size();
	}

	public boolean updateDataItems(int firstItemIdx, XulDataNode... items) {
		_rebindViews.clear();
		int childNum = _area.getChildNum();
		for (int idx = 0, lastItemIdx = Math.min(_data.size() - firstItemIdx, items.length); idx < lastItemIdx; idx++) {
			_ItemData itemData = _data.get(firstItemIdx + idx);
			itemData.setData(items[idx]);

			int viewIdx = firstItemIdx + idx - _itemIdxOffset;
			if (viewIdx >= 0 && viewIdx < childNum) {
				XulView view = _area.getChild(viewIdx);
				_rebindViews.add(view);
			}
		}

		if (!_rebindViews.isEmpty()) {
			XulPage ownerPage = _area.getOwnerPage();
			ownerPage.rebindViews(_rebindViews.getArray(), _rebindViews.size(), getRenderContext().getDefaultActionCallback());
			_rebindViews.clear();
			this.reset();
			XulAction action = _view.getAction(XulPropNameCache.TagId.ACTION_MASSIVE_UPDATED);
			if (action != null) {
				XulPage.invokeActionNoPopup(_view, action);
			}
		}
		return true;
	}

	public boolean updateDataItems(int firstItemIdx, Iterable<XulDataNode> items) {
		_rebindViews.clear();
		int childNum = _area.getChildNum();
		Iterator<XulDataNode> iterator = items.iterator();
		for (int idx = 0, lastItemIdx = _data.size() - firstItemIdx; idx < lastItemIdx && iterator.hasNext(); idx++) {
			_ItemData itemData = _data.get(firstItemIdx + idx);
			itemData.setData(iterator.next());

			int viewIdx = firstItemIdx + idx - _itemIdxOffset;
			if (viewIdx >= 0 && viewIdx < childNum) {
				XulView view = _area.getChild(viewIdx);
				_rebindViews.add(view);
			}
		}

		if (!_rebindViews.isEmpty()) {
			XulPage ownerPage = _area.getOwnerPage();
			ownerPage.rebindViews(_rebindViews.getArray(), _rebindViews.size(), getRenderContext().getDefaultActionCallback());
			_rebindViews.clear();
			this.reset();
			XulAction action = _view.getAction(XulPropNameCache.TagId.ACTION_MASSIVE_UPDATED);
			if (action != null) {
				XulPage.invokeActionNoPopup(_view, action);
			}
		}
		return true;
	}

	public interface DataItemIterator {
		void onDataItem(int idx, XulDataNode node);
	}

	public void eachDataItem(DataItemIterator iterator) {
		ArrayList<_ItemData> data = _data;
		if (data == null) {
			return;
		}
		for (int i = 0, dataSize = data.size(); i < dataSize; i++) {
			_ItemData itemData = data.get(i);
			iterator.onDataItem(i, itemData.data);
		}
	}

	public XulDataNode getDataItem(int idx) {
		if (_data == null) {
			return null;
		}
		if (idx < 0 || idx >= _data.size()) {
			return null;
		}
		_ItemData itemData = _data.get(idx);
		return itemData.data;
	}

	public void addDataItem(XulDataNode item) {
		if (_data == null) {
			_data = new ArrayList<_ItemData>();
		}
		_updateItemSize = true;
		final _ItemData itemData = new _ItemData();
		itemData.setData(item);
		_data.add(itemData);
	}

	@Override
	public XulView postFindFocus(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		int childNum = _area.getChildNum();
		if (childNum == 0) {
			return null;
		}
		if (_arrangement == XulLayoutHelper.MODE_GRID_HORIZONTAL || _arrangement == XulLayoutHelper.MODE_GRID_INVERSE_HORIZONTAL) {
			if (direction != XulLayout.FocusDirection.MOVE_DOWN) {
				return null;
			}
			if (childNum + _firstItemIdxOffset < _data.size()) {
				// not all child instanced, keep focus unchanged
				return XulItem.KEEP_FOCUS;
			}
			XulView child = _area.getChild(childNum - 1);
			RectF focusRc = child.getFocusRc();
			if (focusRc.centerY() < srcRc.bottom) {
				return null;
			}
			if (child.focusable()) {
				return child;
			}
			if (child instanceof XulArea) {
				XulArea childArea = (XulArea) child;
				srcRc.offsetTo(focusRc.left, srcRc.top);
				return childArea.findSubFocus(direction, srcRc, src);
			}
		} else if (_arrangement == XulLayoutHelper.MODE_GRID_VERTICAL) {
			if (direction != XulLayout.FocusDirection.MOVE_RIGHT) {
				return null;
			}
			if (childNum + _firstItemIdxOffset < _data.size()) {
				// not all child instanced, keep focus unchanged
				return XulItem.KEEP_FOCUS;
			}
			XulView child = _area.getChild(childNum - 1);
			RectF focusRc = child.getFocusRc();
			if (focusRc.centerX() < srcRc.right) {
				return null;
			}
			if (child.focusable()) {
				return child;
			}
			if (child instanceof XulArea) {
				XulArea childArea = (XulArea) child;
				srcRc.offsetTo(srcRc.left, focusRc.top);
				return childArea.findSubFocus(direction, srcRc, src);
			}
		}
		return null;
	}

	public void addDataItem(int idx, XulDataNode item) {
		if (_data == null) {
			_data = new ArrayList<_ItemData>();
		}
		_updateItemSize = true;
		int initialDataSize = _data.size();
		if (idx < 0 || idx > initialDataSize) {
			idx = initialDataSize;
		}
		final _ItemData itemData = new _ItemData();
		itemData.setData(item);
		_data.add(idx, itemData);
		if (idx >= initialDataSize) {
			syncItemInstance();
			return;
		}

		for (int i = idx, dataSize = initialDataSize; i < dataSize; ++i) {
			_ItemData item1 = _data.get(i);
			_ItemData item2 = _data.get(i + 1);
			item1.width = item2.width;
			item1.height = item2.height;
			item1.relativeTop = item2.relativeTop;
			item1.relativeLeft = item2.relativeLeft;
			item1.marginTop = item2.marginTop;
			item1.marginLeft = item2.marginLeft;
			item1.marginRight = item2.marginRight;
			item1.marginBottom = item2.marginBottom;
			item2.reset();
		}

		final XulPage ownerPage = _area.getOwnerPage();
		int childNum = _area.getChildNum();
		if (idx < _itemIdxOffset) {
			++_itemIdxOffset;
			createItemInstance(ownerPage, _itemIdxOffset - 1, _itemIdxOffset - 1 + childNum);
			this.reset();
		} else if (idx >= _itemIdxOffset && idx < _itemIdxOffset + childNum) {
			Object itemTemplate = _itemTemplate.getItemTemplate(item, idx, _data.size());
			if (itemTemplate instanceof XulTemplate) {
				Log.w(TAG, "DO NOT use template as the item-template of massive render!");
				return;
			}

			int itemPos = idx - _itemIdxOffset;
			XulView templView = (XulView) itemTemplate;
			itemData.refView = templView.getWeakReference();

			_rebindViews.clear();
			final ArrayList<XulView> cachedViews = _instanceCache.get(templView);
			if (cachedViews == null || cachedViews.isEmpty()) {
				if (templView instanceof XulItem) {
					final XulItem xulItem = ((XulItem) templView).makeClone(_area, itemPos);
					xulItem.prepareRender(getRenderContext(), true);
					xulItem.setBindingCtx("[" + idx + "]", itemData.dataArray);
					ownerPage.addSelectorTarget(xulItem);
					ownerPage.applySelectors(xulItem);
					_rebindViews.add(xulItem);
				} else if (templView instanceof XulArea) {
					final XulArea xulArea = ((XulArea) templView).makeClone(_area, itemPos);
					xulArea.prepareRender(getRenderContext(), true);
					xulArea.setBindingCtx("[" + idx + "]", itemData.dataArray);
					ownerPage.addSelectorTargets(xulArea);
					ownerPage.applySelectors(xulArea);
					_rebindViews.add(xulArea);
				}
			} else {
				XulView cachedView = cachedViews.remove(cachedViews.size() - 1);
				cachedView.cleanImageItems();
				_area.addChild(itemPos, cachedView);
				cachedView.cleanBindingCtx();
				cachedView.setBindingCtx("[" + idx + "]", itemData.dataArray);
				_rebindViews.add(cachedView);
			}
			ownerPage.rebindViews(_rebindViews.getArray(), _rebindViews.size(), getRenderContext().getDefaultActionCallback());
			_rebindViews.clear();
			XulAction action = _view.getAction(XulPropNameCache.TagId.ACTION_MASSIVE_UPDATED);
			if (action != null) {
				XulPage.invokeActionNoPopup(_view, action);
			}
			this.reset();
		}
	}

	public void syncContentView() {
		syncItemInstance();
	}

	public boolean cleanDataItems() {
		_scrollX = 0;
		_scrollY = 0;
		_scrollTargetX = 0;
		_scrollTargetY = 0;
		_itemIdxOffset = 0;
		_firstItemIdxOffset = 0;
		_lastItemIdxOffset = 0;
		_skipItemOffsetX = 0;
		_skipItemOffsetY = 0;
		if (_data != null && !_data.isEmpty()) {
			_data.clear();
		}
		_area.eachChild(_itemRecycler);
		_area.removeAllChildren();
		return true;
	}

	@Override
	public boolean collectPendingItems(XulTaskCollector xulTaskCollector) {
		if (_isViewChanged() || _rect == null) {
			return true;
		}

		try {
			int childNum = _area.getChildNum();
			int rangeBegin = _firstItemIdxOffset - _itemIdxOffset;
			int rangeEnd = _lastItemIdxOffset - _itemIdxOffset;
			if (rangeBegin < 0) {
				rangeBegin = 0;
			} else if (rangeBegin > childNum) {
				rangeBegin = childNum;
			}
			if (rangeEnd < 0) {
				rangeEnd = 0;
			} else if (rangeEnd > childNum) {
				rangeEnd = childNum;
			}

			if (rangeBegin <= rangeEnd) {
				for (int i = rangeBegin; i < rangeEnd; i++) {
					XulView child = _area.getChild(i);
					xulTaskCollector.addPendingItem(child);
				}
			}
			int beginPos = rangeBegin - 1;
			int endPos = rangeEnd;
			if (endPos < beginPos) {
				endPos = beginPos + 1;
			}
			while (beginPos >= 0 || endPos < childNum) {
				if (endPos < childNum) {
					XulView child = _area.getChild(endPos);
					xulTaskCollector.addPendingItem(child);
				}

				if (beginPos >= 0) {
					XulView child = _area.getChild(beginPos);
					xulTaskCollector.addPendingItem(child);
				}

				--beginPos;
				++endPos;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean fixedItem() {
		return _fixedItem;
	}

	public int getItemIdx(XulView item) {
		int childPos = _area.findChildPos(item);
		if (childPos < 0) {
			return -1;
		}
		return _itemIdxOffset + childPos;
	}

	public XulView getItemView(int idx) {
		if (_data == null || idx >= _data.size() || idx < _itemIdxOffset) {
			return null;
		}
		idx -= _itemIdxOffset;
		if (idx >= _area.getChildNum()) {
			return null;
		}
		return _area.getChild(idx);
	}

	public boolean removeItem(XulView item) {
		int itemIdx = getItemIdx(item);
		return removeDataItem(itemIdx);
	}

	public boolean removeDataItem(int idx) {
		if (idx < 0 || idx >= _data.size()) {
			return false;
		}
		_updateItemSize = true;
		_data.remove(idx);
		for (int i = idx, dataSize = _data.size(); i < dataSize; ++i) {
			_data.get(i).reset();
		}
		int childNum = _area.getChildNum();
		if (idx >= _itemIdxOffset && idx < _itemIdxOffset + childNum) {
			boolean restoreFocus;
			XulLayout layout = _area.getOwnerPage().getLayout();
			{
				XulView view = _area.getChild(idx - _itemIdxOffset);
				restoreFocus = view.hasFocus();
				if (restoreFocus) {
					layout.requestFocus(null);
				}
				_area.removeChildren(idx - _itemIdxOffset, childNum, _itemRecycler_list);

				final XulPage ownerPage = _area.getOwnerPage();
				createItemInstance(ownerPage, _itemIdxOffset, _itemIdxOffset + childNum);
			}
			if (restoreFocus) {
				childNum = _area.getChildNum();
				idx -= _itemIdxOffset;
				while (idx >= childNum) {
					--idx;
				}
				if (idx >= 0) {
					XulView view = _area.getChild(idx);
					getRenderContext().doLayout();
					layout.requestFocus(view);
				}
			}
		} else if (idx < _itemIdxOffset) {
			_area.removeChildren(0, 1, _itemRecycler_list);
		}
		return true;
	}

	public RectF getItemRect(int idx) {
		return getItemRect(idx, new RectF());
	}

	public RectF getItemRect(int idx, RectF rect) {
		if (idx < 0 || idx >= _data.size()) {
			return null;
		}

		_ItemData itemData = _data.get(idx);
		if (!itemData.isInitialized()) {
			return null;
		}
		rect.set(itemData.relativeLeft, itemData.relativeTop,
			itemData.relativeLeft + itemData.width, itemData.relativeTop + itemData.height
		);
		XulUtils.offsetRect(rect, _screenX, _screenY);
		return rect;
	}

	private static class _ItemData {
		float relativeLeft = Float.NaN;
		float relativeTop = Float.NaN;
		float width = 0;
		float height = 0;
		int marginLeft = 0;
		int marginRight = 0;
		int marginTop = 0;
		int marginBottom = 0;
		WeakReference<XulView> refView;
		ArrayList<XulDataNode> dataArray = new ArrayList<XulDataNode>(1);
		XulDataNode data;

		public void setData(XulDataNode data) {
			this.data = data;
			if (dataArray.isEmpty()) {
				dataArray.add(data);
			} else {
				dataArray.set(0, data);
			}
		}

		public boolean isInitialized() {
			return !Float.isNaN(relativeLeft) && !Float.isNaN(relativeTop);
		}

		public void reset() {
			relativeLeft = Float.NaN;
			relativeTop = Float.NaN;
		}
	}

	ArrayList<_ItemData> _data;

	private class _RebindViewList extends XulSimpleArray<XulView> {

		public _RebindViewList(int sz) {
			super(sz);
		}

		@Override
		protected XulView[] allocArrayBuf(int size) {
			return new XulView[size];
		}
	}

	_RebindViewList _rebindViews = new _RebindViewList(64);

	public XulMassiveRender(XulRenderContext ctx, XulArea area) {
		super(ctx, area);
		_area.eachChild(new XulArea.XulAreaIterator() {
			@Override
			public boolean onXulTemplate(int pos, XulTemplate template) {
				if ("@item-template".equals(template.getId())) {
					_itemTemplate = template;
					return false;
				}
				return true;
			}
		});
		_area.removeAllChildren();
	}

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		super.draw(dc, rect, xBase, yBase);
		syncClipRange();
	}

	private class ClipRangeUpdater implements Runnable {
		private boolean _running = false;
		private float _newClipRangeLeft;
		private float _newClipRangeTop;
		private float _newClipRangeWidth;
		private float _newClipRangeHeight;

		private boolean _resetLayout = false;

		@Override
		public void run() {
			if (_resetLayout) {
				_resetLayout = false;
				setUpdateLayout(true);
			}

			_clipRangeLeft = _newClipRangeLeft;
			_clipRangeTop = _newClipRangeTop;
			_clipRangeWidth = _newClipRangeWidth;
			_clipRangeHeight = _newClipRangeHeight;
			syncItemInstance();
			_running = false;
			if (isLayoutChanged()) {
				getRenderContext().internalDoLayout();
			}
		}

		public void doUpdate(float clipRangeLeft, float clipRangeTop, float clipRangeWidth, float clipRangeHeight) {
			boolean isVertical = XulLayoutHelper.isVerticalLayoutMode(_arrangement);
			if (XulLayoutHelper.isGridLayoutMode(_arrangement)) {
				if (clipRangeWidth <= 0 || clipRangeHeight <= 0) {
					return;
				}
			} else {
				if (isVertical && clipRangeHeight <= 0) {
					return;
				}
				if (!isVertical && clipRangeWidth <= 0) {
					return;
				}
			}
			this._newClipRangeLeft = clipRangeLeft;
			this._newClipRangeTop = clipRangeTop;
			this._newClipRangeWidth = clipRangeWidth;
			this._newClipRangeHeight = clipRangeHeight;
			XulRenderContext renderContext = getRenderContext();
			if (renderContext == null || _running) {
				return;
			}
			_running = true;
			renderContext.scheduleLayoutFinishedTask(this);
		}

		public void resetLayout() {
			_resetLayout = true;
			XulRenderContext renderContext = getRenderContext();
			if (renderContext == null || _running) {
				return;
			}
			renderContext.scheduleLayoutFinishedTask(this);
		}
	}

	private ClipRangeUpdater _clipRangeUpdater = new ClipRangeUpdater();

	private void syncClipRange() {
		RectF focusRect = getFocusRect();
		if (focusRect == null) {
			return;
		}
		XulArea parent = _area.getParent();
		float clipLeft = 0;
		float clipTop = 0;
		float clipRight = Float.MAX_VALUE;
		float clipBottom = Float.MAX_VALUE;
		for (; parent != null; parent = parent.getParent()) {
			XulViewRender render = parent.getRender();
			if (render instanceof XulSliderAreaRender) {
				XulSliderAreaRender sliderAreaRender = (XulSliderAreaRender) render;
				if (!sliderAreaRender._clipChildren) {
					continue;
				}
			} else if (parent instanceof XulLayout) {
			} else {
				continue;
			}
			RectF parentFocus = render.getFocusRect();
			clipLeft = Math.max(parentFocus.left, clipLeft);
			clipTop = Math.max(parentFocus.top, clipTop);
			clipRight = Math.min(parentFocus.right, clipRight);
			clipBottom = Math.min(parentFocus.bottom, clipBottom);
		}

		float clipRangeLeft = clipLeft - focusRect.left;
		float clipRangeTop = clipTop - focusRect.top;
		float clipRangeWidth = clipRight - clipLeft;
		float clipRangeHeight = clipBottom - clipTop;

		if (clipRangeLeft != _clipRangeLeft ||
			clipRangeTop != _clipRangeTop ||
			clipRangeWidth != _clipRangeWidth ||
			clipRangeHeight != _clipRangeHeight
			) {
			_clipRangeUpdater.doUpdate(clipRangeLeft, clipRangeTop, clipRangeWidth, clipRangeHeight);
		}
	}

	@Override
	public void resetBinding() {
		_initBinding = true;
		_scrollX = 0;
		_scrollY = 0;
		_scrollTargetX = 0;
		_scrollTargetY = 0;
		_itemIdxOffset = 0;
		_firstItemIdxOffset = 0;
		_lastItemIdxOffset = 0;
		_skipItemOffsetX = 0;
		_skipItemOffsetY = 0;
		reset();
	}

	private void prepareAttr() {
		if (!_isViewChanged()) {
			return;
		}
		final String arrangement = _area.getAttrString(XulPropNameCache.TagId.ARRANGEMENT);
		final XulAttr directionAttr = _area.getAttr(XulPropNameCache.TagId.DIRECTION);
		final XulAttr minimumItemNum = _area.getAttr(XulPropNameCache.TagId.MINIMUM_ITEM);
		final XulAttr cachePages = _area.getAttr(XulPropNameCache.TagId.CACHE_PAGES);
		boolean isVertical = true;
		boolean isReverse = false;
		if (directionAttr != null) {
			XulPropParser.xulParsedAttr_Direction direction = directionAttr.getParsedValue();
			isVertical = direction.vertical;
			isReverse = direction.reverse;
		}
		int arrangementType = XulLayoutHelper.MODE_GRID_HORIZONTAL;
		int MODE_GRID_HORIZONTAL;
		if (isReverse) {
			MODE_GRID_HORIZONTAL = XulLayoutHelper.MODE_GRID_INVERSE_HORIZONTAL;
		} else {
			MODE_GRID_HORIZONTAL = XulLayoutHelper.MODE_GRID_HORIZONTAL;
		}

		int MODE_LINEAR_HORIZONTAL;
		if (isReverse) {
			MODE_LINEAR_HORIZONTAL = XulLayoutHelper.MODE_LINEAR_INVERSE_HORIZONTAL;
		} else {
			MODE_LINEAR_HORIZONTAL = XulLayoutHelper.MODE_LINEAR_HORIZONTAL;
		}

		if ("grid".equals(arrangement)) {
			arrangementType = isVertical ? XulLayoutHelper.MODE_GRID_VERTICAL : MODE_GRID_HORIZONTAL;
			_fixedItem = false;
		} else if ("grid-fixed".equals(arrangement)) {
			arrangementType = isVertical ? XulLayoutHelper.MODE_GRID_VERTICAL : MODE_GRID_HORIZONTAL;
			_fixedItem = true;
		} else if ("linear".equals(arrangement)) {
			arrangementType = isVertical ? XulLayoutHelper.MODE_LINEAR_VERTICAL : MODE_LINEAR_HORIZONTAL;
			_fixedItem = false;
		} else if ("linear-fixed".equals(arrangement)) {
			arrangementType = isVertical ? XulLayoutHelper.MODE_LINEAR_VERTICAL : MODE_LINEAR_HORIZONTAL;
			_fixedItem = true;
		}

		if (_arrangement != arrangementType) {
			_arrangement = arrangementType;
			if (_data != null && !_data.isEmpty()) {
				_updateItemSize = true;
				for (int i = 0, dataSize = _data.size(); i < dataSize; i++) {
					_data.get(i).reset();
				}
			}
		}

		_minItemNum = minimumItemNum == null ? 16 : Math.max(((XulPropParser.xulParsedProp_Integer) minimumItemNum.getParsedValue()).val, 3);
		_cachePages = cachePages == null ? 0.5f : Math.max(((XulPropParser.xulParsedProp_Float) cachePages.getParsedValue()).val, 0.5f);
	}

	private void prepareBinding() {
		if (_itemTemplate == null) {
			return;
		}
		if (!_initBinding) {
			return;
		}
		_initBinding = false;
		_area.eachChild(_itemRecycler);
		_area.removeAllChildren();

		ArrayList<XulDataNode> bindingData = _area.getBindingData();
		if (bindingData == null || bindingData.isEmpty()) {
			return;
		}

		XulDataNode dataRoot = bindingData.get(0);
		if (dataRoot == null) {
			return;
		}
		XulDataNode dataNode = dataRoot.getFirstChild();
		if (dataNode == null) {
			return;
		}

		_data = new ArrayList<_ItemData>(dataNode.size());
		while (dataNode != null) {
			final _ItemData itemData = new _ItemData();
			itemData.setData(dataNode);
			_data.add(itemData);
			dataNode = dataNode.getNext();
		}

		final XulPage ownerPage = _area.getOwnerPage();

		createItemInstance(ownerPage, 0, _minItemNum);
		this.reset();
	}

	private boolean createItemInstance(XulPage ownerPage, int rangeStart, int rangeEnd) {
		return createItemInstance(ownerPage, rangeStart, rangeEnd, false);
	}

	private boolean createItemInstance(XulPage ownerPage, int rangeStart, int rangeEnd, boolean noEvent) {
		boolean anyItemCreated = false;
		final int childNum = _area.getChildNum();
		for (int i = rangeStart, bindingDataSize = Math.min(_data.size(), rangeEnd); i < bindingDataSize; i++) {
			if (i >= _itemIdxOffset && i < _itemIdxOffset + childNum) {
				continue;
			}

			final _ItemData itemData = _data.get(i);
			final ArrayList<XulDataNode> xulDataArray = itemData.dataArray;
			final XulDataNode xulDataNode = itemData.data;
			Object itemTemplate = itemData.refView == null ? null : itemData.refView.get();

			if (itemTemplate == null) {
				itemTemplate = _itemTemplate.getItemTemplate(xulDataNode, i, bindingDataSize);
				if (itemTemplate instanceof XulTemplate) {
					Log.w(TAG, "DO NOT use template as the item-template of massive render!");
					continue;
				}

				if (itemTemplate == null) {
					if (XulManager.DEBUG) {
						Log.e(TAG, "failed to create item instance " + i + "/" + bindingDataSize);
					}
					continue;
				}
			}

			int itemPos = i - rangeStart;
			XulView templView = (XulView) itemTemplate;
			itemData.refView = templView.getWeakReference();

			final ArrayList<XulView> cachedViews = _instanceCache.get(templView);
			anyItemCreated = true;
			if (cachedViews == null || cachedViews.isEmpty()) {
				if (templView instanceof XulItem) {
					final XulItem xulItem = ((XulItem) templView).makeClone(_area, itemPos);
					xulItem.prepareRender(getRenderContext(), true);
					xulItem.setBindingCtx("[" + i + "]", xulDataArray);
					ownerPage.addSelectorTarget(xulItem);
					ownerPage.applySelectors(xulItem);
					_rebindViews.add(xulItem);
				} else if (templView instanceof XulArea) {
					final XulArea xulArea = ((XulArea) templView).makeClone(_area, itemPos);
					xulArea.prepareRender(getRenderContext(), true);
					xulArea.setBindingCtx("[" + i + "]", xulDataArray);
					ownerPage.addSelectorTargets(xulArea);
					ownerPage.applySelectors(xulArea);
					_rebindViews.add(xulArea);
				}
			} else {
				XulView cachedView = cachedViews.remove(cachedViews.size() - 1);
				cachedView.cleanImageItems();
				_area.addChild(itemPos, cachedView);
				cachedView.cleanBindingCtx();
				cachedView.setBindingCtx("[" + i + "]", xulDataArray);
				_rebindViews.add(cachedView);
			}
		}

		_itemIdxOffset = rangeStart;
		if (anyItemCreated) {
			if (XulManager.PERFORMANCE_BENCH) {
				long t = XulUtils.timestamp_us();
				ownerPage.rebindViews(_rebindViews.getArray(), _rebindViews.size(), getRenderContext().getDefaultActionCallback());
				Log.d(TAG, "REBINDING(us): " + (XulUtils.timestamp_us() - t));
			} else {
				ownerPage.rebindViews(_rebindViews.getArray(), _rebindViews.size(), getRenderContext().getDefaultActionCallback());
			}
			XulAction action = _view.getAction(XulPropNameCache.TagId.ACTION_MASSIVE_UPDATED);
			if (action != null && !noEvent) {
				XulPage.invokeActionNoPopup(_view, action);
			}
		}
		_rebindViews.clear();
		return anyItemCreated;
	}

	private class ScrollAnimation implements IXulAnimation {

		long _lastTimestamp = 0;

		@Override
		public boolean updateAnimation(long timestamp) {
			if (_lastTimestamp == 0) {
				_lastTimestamp = timestamp;
			}

			float deltaX = calDelta(_scrollTargetX - _scrollX);
			float deltaY = calDelta(_scrollTargetY - _scrollY);
			if (Math.abs(deltaX) <= 0.001 && Math.abs(deltaY) <= 0.001) {
				reset();
				return false;
			}
			_scrollX += deltaX;
			_scrollY += deltaY;
			syncItemInstance();
			return true;
		}

		private float calDelta(float delta) {
			float ret = delta / 3;
			if (Math.abs(ret) <= 4) {
				ret = delta;
				final float absDelta = Math.abs(ret);
				if (absDelta > 4) {
					ret = ret * 4 / absDelta;
				}
			}
			return ret;
		}

		public void reset() {
			_lastTimestamp = 0;
		}
	}

	static void assert_true(boolean val) {
		if (!val) {
			new Exception().printStackTrace();
		}
	}

	private void syncItemInstance() {
		if (_data == null) {
			return;
		}

		if (XulManager.DEBUG) {
			Log.d(TAG, String.format("_isLayoutChanged:%s", String.valueOf(_isLayoutChanged())));
		}
		if (_isLayoutChanged()) {
			getRenderContext().doLayout();
		}

		final int childNum = _area.getChildNum();
		if (childNum == 0) {
			if (_data.isEmpty()) {
				return;
			}
			boolean isVertical = !(_arrangement == XulLayoutHelper.MODE_GRID_VERTICAL || _arrangement == XulLayoutHelper.MODE_LINEAR_HORIZONTAL);
			if ((isVertical && _clipRangeTop <= 0) || (!isVertical && _clipRangeLeft <= 0)) {
				final XulPage ownerPage = _area.getOwnerPage();
				createItemInstance(ownerPage, 0, _minItemNum);
				this.reset();
				return;
			} else if (_fixedItem) {
				// no exist child, fix item layout
				_updateItemSize = true;
				final XulPage ownerPage = _area.getOwnerPage();
				createItemInstance(ownerPage, 0, 1, true);
				fixItemSize();
				_area.eachChild(_itemRecycler);
				_area.removeAllChildren();
			} else {
				final XulPage ownerPage = _area.getOwnerPage();
				createItemInstance(ownerPage, 0, _minItemNum);
				this.reset();
				Log.w(TAG, "non-fixed massive container must layout from very begining.");
				return;
			}
		}

		int dataSize = _data.size();
		if (dataSize == 0) {
			if (_area.getChildNum() > 0) {
				_initBinding = false;
				_area.eachChild(_itemRecycler);
				_area.removeAllChildren();
				this.reset();
			}
			return;
		}

		final RectF areaRc = _area.getFocusRc();
		float minimumLeft = areaRc.left - _screenX;
		float maximumRight = areaRc.right - _screenX;
		float minimumTop = areaRc.top - _screenY;
		float maximumBottom = areaRc.bottom - _screenY;

		boolean isVertical = true;
		switch (_arrangement) {
		case XulLayoutHelper.MODE_GRID_HORIZONTAL:
		case XulLayoutHelper.MODE_GRID_INVERSE_HORIZONTAL:
		case XulLayoutHelper.MODE_LINEAR_VERTICAL:
		case XulLayoutHelper.MODE_LINEAR_INVERSE_VERTICAL:
			isVertical = true;
			break;
		case XulLayoutHelper.MODE_GRID_VERTICAL:
		case XulLayoutHelper.MODE_LINEAR_HORIZONTAL:
		case XulLayoutHelper.MODE_LINEAR_INVERSE_HORIZONTAL:
			isVertical = false;
			break;
		}

		if (isVertical) {
			if (minimumTop < _clipRangeTop) {
				minimumTop = (int) _clipRangeTop;
			}
			if (maximumBottom > _clipRangeTop + _clipRangeHeight) {
				maximumBottom = (int) (_clipRangeTop + _clipRangeHeight);
			}
		} else {
			if (minimumLeft < _clipRangeLeft) {
				minimumLeft = (int) _clipRangeLeft;
			}
			if (maximumRight > _clipRangeLeft + _clipRangeWidth) {
				maximumRight = (int) (_clipRangeLeft + _clipRangeWidth);
			}
		}

		float visibleRangeLeft = minimumLeft;
		float visibleRangeTop = minimumTop;
		float visibleRangeRight = maximumRight;
		float visibleRangeBottom = maximumBottom;

		final float preLoadRangeWidth = Math.min(_clipRangeWidth, XulUtils.calRectWidth(areaRc)) * _cachePages;
		final float preLoadRangeHeight = Math.min(_clipRangeHeight, XulUtils.calRectHeight(areaRc)) * _cachePages;
		minimumLeft -= preLoadRangeWidth;
		maximumRight += preLoadRangeWidth;
		minimumTop -= preLoadRangeHeight;
		maximumBottom += preLoadRangeHeight;

		int firstVisibleItem = _itemIdxOffset + childNum - 1;
		int lastVisibleItem = _itemIdxOffset;

		for (int i = 0; i < childNum; i++) {
			final _ItemData itemData = _data.get(i + _itemIdxOffset);
			if (!itemData.isInitialized()) {
				final XulView child = _area.getChild(i);
				final RectF focusRc = child.getFocusRc();
				itemData.relativeLeft = focusRc.left - _itemOffsetX + _skipItemOffsetX - _screenX;
				itemData.relativeTop = focusRc.top - _itemOffsetY + _skipItemOffsetY - _screenY;
				itemData.width = XulUtils.calRectWidth(focusRc);
				itemData.height = XulUtils.calRectHeight(focusRc);
			}
			final float curItemLeft = itemData.relativeLeft + _scrollX - _skipItemOffsetX;
			final float curItemRight = itemData.relativeLeft + itemData.width + _scrollX - _skipItemOffsetX;
			final float curItemTop = itemData.relativeTop + _scrollY - _skipItemOffsetY;
			final float curItemBottom = itemData.relativeTop + itemData.height + _scrollY - _skipItemOffsetY;

			if (curItemLeft <= visibleRangeRight &&
				curItemTop < visibleRangeBottom &&
				curItemRight >= visibleRangeLeft &&
				curItemBottom > visibleRangeTop
				) {
				if (firstVisibleItem > i + _itemIdxOffset) {
					firstVisibleItem = i + _itemIdxOffset;
				}
				if (lastVisibleItem < i + _itemIdxOffset) {
					lastVisibleItem = i + _itemIdxOffset;
				}
			}
		}

		firstVisibleItem = Math.min(dataSize - 1, firstVisibleItem);
		if (lastVisibleItem < firstVisibleItem || firstVisibleItem < 0) {
			// no visible items
			if (firstVisibleItem < 0) {
				firstVisibleItem = 0;
			}
			lastVisibleItem = firstVisibleItem;
			for (int i = firstVisibleItem; i < dataSize; i++) {
				final _ItemData itemData = _data.get(i);
				assert_true(itemData.isInitialized());
				final float curItemLeft = itemData.relativeLeft + _scrollX - _skipItemOffsetX;
				final float curItemRight = itemData.relativeLeft + itemData.width + _scrollX - _skipItemOffsetX;
				final float curItemTop = itemData.relativeTop + _scrollY - _skipItemOffsetY;
				final float curItemBottom = itemData.relativeTop + itemData.height + _scrollY - _skipItemOffsetY;

				if (curItemLeft <= visibleRangeRight &&
					curItemTop < visibleRangeBottom &&
					curItemRight >= visibleRangeLeft &&
					curItemBottom > visibleRangeTop
					) {
					firstVisibleItem = i;
					lastVisibleItem = i;
					break;
				}
			}

			for (int i = firstVisibleItem + 1; i < dataSize; i++) {
				final _ItemData itemData = _data.get(i);
				assert_true(itemData.isInitialized());
				final float curItemLeft = itemData.relativeLeft + _scrollX - _skipItemOffsetX;
				final float curItemRight = itemData.relativeLeft + itemData.width + _scrollX - _skipItemOffsetX;
				final float curItemTop = itemData.relativeTop + _scrollY - _skipItemOffsetY;
				final float curItemBottom = itemData.relativeTop + itemData.height + _scrollY - _skipItemOffsetY;

				if (curItemLeft <= visibleRangeRight &&
					curItemTop < visibleRangeBottom &&
					curItemRight >= visibleRangeLeft &&
					curItemBottom > visibleRangeTop
					) {
					lastVisibleItem = i;
				} else {
					break;
				}
			}
		}

		int newItemRangeStart = firstVisibleItem;
		int newItemRangeEnd = lastVisibleItem;

		for (int i = newItemRangeStart; i >= 0; --i) {
			final _ItemData itemData = _data.get(i);
			boolean initialized = itemData.isInitialized();
			if (XulManager.DEBUG && !initialized) {
				Log.d(TAG, String.format("childNum:%d, dataSize:%d, firstVisibleItem:%d, lastVisibleItem:%d, newItemRangeStart%d, i:%d"
					, childNum
					, dataSize
					, firstVisibleItem
					, lastVisibleItem
					, newItemRangeStart
					, i
				));
			}
			assert_true(initialized);
			final float curItemLeft = itemData.relativeLeft + _scrollX - _skipItemOffsetX;
			final float curItemRight = itemData.relativeLeft + itemData.width + _scrollX - _skipItemOffsetX;
			final float curItemTop = itemData.relativeTop + _scrollY - _skipItemOffsetY;
			final float curItemBottom = itemData.relativeTop + itemData.height + _scrollY - _skipItemOffsetY;

			if (curItemLeft <= visibleRangeRight &&
				curItemTop < visibleRangeBottom &&
				curItemRight >= visibleRangeLeft &&
				curItemBottom > visibleRangeTop
				) {
				if (firstVisibleItem > i) {
					firstVisibleItem = i;
				}
			}
			if (curItemRight > minimumLeft &&
				curItemBottom > minimumTop
				) {
				if (newItemRangeStart > i) {
					newItemRangeStart = i;
				}
			} else {
				break;
			}
		}

		for (int i = newItemRangeEnd; i < dataSize; ++i) {
			final _ItemData itemData = _data.get(i);
			if (!itemData.isInitialized()) {
				break;
			}

			final float curItemLeft = itemData.relativeLeft + _scrollX - _skipItemOffsetX;
			final float curItemRight = itemData.relativeLeft + itemData.width + _scrollX - _skipItemOffsetX;
			final float curItemTop = itemData.relativeTop + _scrollY - _skipItemOffsetY;
			final float curItemBottom = itemData.relativeTop + itemData.height + _scrollY - _skipItemOffsetY;

			if (curItemLeft <= visibleRangeRight &&
				curItemTop < visibleRangeBottom &&
				curItemRight >= visibleRangeLeft &&
				curItemBottom > visibleRangeTop
				) {
				if (lastVisibleItem < i) {
					lastVisibleItem = i;
				}
			}

			if (curItemLeft <= maximumRight &&
				curItemTop <= maximumBottom
				) {
				if (newItemRangeEnd < i) {
					newItemRangeEnd = i;
				}
			} else {
				break;
			}
		}

		firstVisibleItem = Math.min(dataSize - 1, firstVisibleItem);
		_firstItemIdxOffset = firstVisibleItem;
		_lastItemIdxOffset = lastVisibleItem;
		if (newItemRangeEnd < firstVisibleItem + _minItemNum) {
			newItemRangeEnd = firstVisibleItem + _minItemNum;
		}

		if (newItemRangeEnd > dataSize) {
			newItemRangeEnd = dataSize;
		}

		if (_itemIdxOffset == newItemRangeStart && newItemRangeEnd - newItemRangeStart == childNum) {
			return;
		}

		boolean anyChildrenChanged = false;

		if (newItemRangeStart != _itemIdxOffset) {
			final _ItemData itemDataNew = _data.get(newItemRangeStart);
			assert_true(itemDataNew.isInitialized());
			final _ItemData itemDataCur = _data.get(_itemIdxOffset);
			float offsetX = itemDataNew.relativeLeft - itemDataCur.relativeLeft;
			float offsetY = itemDataNew.relativeTop - itemDataCur.relativeTop;

			_scrollTargetX += offsetX;
			_scrollX += offsetX;
			_skipItemOffsetX += offsetX;

			_scrollTargetY += offsetY;
			_scrollY += offsetY;
			_skipItemOffsetY += offsetY;

			if (!_fixedItem) {
				_contentWidth -= offsetX;
				_contentHeight -= offsetY;
			}
		}

		if (newItemRangeEnd - _itemIdxOffset < childNum) {
			anyChildrenChanged = true;
			_area.removeChildren(newItemRangeEnd - _itemIdxOffset, childNum, _itemRecycler_list);
		}

		if (newItemRangeStart > _itemIdxOffset) {
			anyChildrenChanged = true;
			_area.removeChildren(0, newItemRangeStart - _itemIdxOffset, _itemRecycler_list);
			_itemIdxOffset = newItemRangeStart;
		}

		final XulPage ownerPage = _area.getOwnerPage();
		if (createItemInstance(ownerPage, newItemRangeStart, newItemRangeEnd + 1)) {
			anyChildrenChanged = true;
		}

		if (anyChildrenChanged) {
			this.reset();
		}
	}

	private ScrollAnimation _scrollAnimation = null;

	private ScrollAnimation getScrollAnimation() {
		if (_scrollAnimation == null) {
			_scrollAnimation = new ScrollAnimation();
		}
		return _scrollAnimation;
	}

	@Override
	public boolean handleScrollEvent(float hScroll, float vScroll) {
		if (true) {
			return false;
		}

		if (_rect == null) {
			return false;
		}
		//_scrollTargetX += hScroll*128;
		_scrollTargetX += vScroll * 128;

		int viewWidth = XulUtils.calRectWidth(_rect);
		int viewHeight = XulUtils.calRectHeight(_rect);
		if (_padding != null) {
			viewWidth -= _padding.left + _padding.right;
			viewHeight -= _padding.top + _padding.bottom;
		}

		if (_scrollTargetX > 0) {
			_scrollTargetX = 0;
		}

		if (_scrollTargetX < viewWidth - _contentWidth) {
			_scrollTargetX = viewWidth - _contentWidth;
		}

		if (Math.abs(_scrollTargetX - _scrollX) > 0.001f ||
			Math.abs(_scrollTargetY - _scrollY) > 0.001f
			) {
			addAnimation(getScrollAnimation());
			return true;
		}
		return false;
	}

	protected class LayoutContainer extends XulAreaRender.LayoutContainer {
		@Override
		public int prepare() {
			if (!_isLayoutChangedByChild()) {
				prepareAttr();
			}
			int ret = super.prepare();
			prepareBinding();
			return ret;
		}

		@Override
		public boolean updateContentSize() {
			if (_fixedItem) {
				fixItemSize();
				return false;
			}
			return true;
		}

		@Override
		public int setContentSize(int w, int h) {
			_contentWidth = w;
			_contentHeight = h;
			return 0;
		}

		@Override
		public boolean setWidth(int w) {
			int width = getWidth();
			if (_fixedItem && (width == XulManager.SIZE_MATCH_CONTENT || width == XulManager.SIZE_AUTO)) {
				return super.setWidth(constrainWidth(_contentWidth));
			}
			return super.setWidth(w);
		}

		@Override
		public boolean setHeight(int h) {
			int height = getHeight();
			if (_fixedItem && (height == XulManager.SIZE_MATCH_CONTENT || height == XulManager.SIZE_AUTO)) {
				return super.setHeight(constrainHeight(_contentHeight));
			}
			return super.setHeight(h);
		}

		@Override
		public boolean setBase(int x, int y) {
			boolean ret = super.setBase(x, y);
			syncClipRange();
			return ret;
		}

		@Override
		public boolean offsetBase(int dx, int dy) {
			boolean ret = super.offsetBase(dx, dy);
			syncClipRange();
			return ret;
		}

		@Override
		public int getLayoutMode() {
			return _arrangement;
		}

		@Override
		public int getOffsetX() {
			_itemOffsetX = (int) _scrollX;
			return (int) _itemOffsetX;
		}

		@Override
		public int getOffsetY() {
			_itemOffsetY = (int) _scrollY;
			return (int) _itemOffsetY;
		}
	}

	private void fixItemSize() {
		if (!_fixedItem) {
			return;
		}
		if (_data == null || _data.isEmpty()) {
			return;
		}
		if (_area.getChildNum() == 0) {
			return;
		}

		Rect containerPadding = _padding;
		int maxX = getWidth() - containerPadding.right;
		int maxY = getHeight() - containerPadding.bottom;

		boolean needSyncItem = false;

		if (XulLayoutHelper.isGridLayoutMode(_arrangement)) {
			int xMark = maxX * 0x400 + _padding.left;
			int yMark = maxY * 0x400 + _padding.top;
			if (XulLayoutHelper.isVerticalLayoutMode(_arrangement)) {
				needSyncItem = _lastArrangeXMark != xMark;
				_updateItemSize = _updateItemSize || needSyncItem;
			} else {
				needSyncItem = _lastArrangeYMark != yMark;
				_updateItemSize = _updateItemSize || _lastArrangeYMark != yMark;
			}

			if (_updateItemSize) {
				Log.d(TAG, "_update item size (grid mode)! " + _updateItemSize + " xMark:" + (_lastArrangeXMark != xMark) + " yMark:" + (_lastArrangeYMark != yMark));
			}
			_lastArrangeXMark = xMark;
			_lastArrangeYMark = yMark;
		}

		if (!_updateItemSize && _firstItemIdxOffset < _data.size()) {
			_ItemData itemData = _data.get(_firstItemIdxOffset);
			if (itemData.isInitialized()) {
				return;
			}
		}
		_updateItemSize = false;

		_contentWidth = 0;
		_contentHeight = 0;

		XulView firstChild = _area.getFirstChild();
		XulViewRender itemRender = firstChild.getRender();
		RectF itemFocusRect = itemRender.getFocusRect();
		XulLayoutHelper.ILayoutElement itemLayoutElement = itemRender.getLayoutElement();
		Rect itemMargin = itemLayoutElement.getMargin();
		int startX = itemMargin.left + containerPadding.left;
		int startY = itemMargin.top + containerPadding.top;
		int extX = itemMargin.right;
		int extY = itemMargin.bottom;

		float itemWidth = XulUtils.calRectWidth(itemFocusRect);
		float itemHeight = XulUtils.calRectHeight(itemFocusRect);
		float stepX = Math.max(itemMargin.left, itemMargin.right) + itemWidth;
		float stepY = Math.max(itemMargin.top, itemMargin.bottom) + itemHeight;

		int curX = startX;
		int curY = startY;
		if (_arrangement == XulLayoutHelper.MODE_GRID_INVERSE_HORIZONTAL ||
			_arrangement == XulLayoutHelper.MODE_LINEAR_INVERSE_HORIZONTAL
			) {
			curX = XulUtils.roundToInt(maxX - itemWidth);
		}

		for (int i = 0, dataSize = _data.size(); i < dataSize; i++) {
			_ItemData data = _data.get(i);
			if (_arrangement == XulLayoutHelper.MODE_GRID_VERTICAL) {
				if (curY + itemHeight > maxY) {
					curY = startY;
					curX += stepX;
				}
			} else if (_arrangement == XulLayoutHelper.MODE_GRID_HORIZONTAL) {
				if (curX + itemWidth > maxX) {
					curX = startX;
					curY += stepY;
				}
			} else if (_arrangement == XulLayoutHelper.MODE_GRID_INVERSE_HORIZONTAL) {
				if (curX < startX) {
					curX = XulUtils.roundToInt(maxX - itemWidth);
					curY += stepY;
				}
			}
			data.marginLeft = itemMargin.left;
			data.marginTop = itemMargin.top;
			data.marginRight = itemMargin.right;
			data.marginBottom = itemMargin.bottom;
			data.relativeTop = curY;
			data.relativeLeft = curX;
			data.width = itemWidth;
			data.height = itemHeight;
			if (_arrangement == XulLayoutHelper.MODE_LINEAR_VERTICAL ||
				_arrangement == XulLayoutHelper.MODE_GRID_VERTICAL
				) {
				curY += stepY;
				int curItemRight = XulUtils.roundToInt(curX + itemWidth);
				if (_contentWidth < curItemRight) {
					_contentWidth = curItemRight;
				}
				if (_contentHeight < curY) {
					_contentHeight = curY;
				}
			} else if (_arrangement == XulLayoutHelper.MODE_LINEAR_HORIZONTAL ||
				_arrangement == XulLayoutHelper.MODE_GRID_HORIZONTAL
				) {
				curX += stepX;
				if (_contentWidth < curX) {
					_contentWidth = curX;
				}
				int curItemBottom = XulUtils.roundToInt(curY + itemHeight);
				if (_contentHeight < curItemBottom) {
					_contentHeight = curItemBottom;
				}
			} else if (_arrangement == XulLayoutHelper.MODE_LINEAR_INVERSE_HORIZONTAL ||
				_arrangement == XulLayoutHelper.MODE_GRID_INVERSE_HORIZONTAL
				) {
				if (_contentWidth < maxX - curX) {
					_contentWidth = maxX - curX;
				}
				curX -= stepX;
				int curItemBottom = XulUtils.roundToInt(curY + itemHeight);
				if (_contentHeight < curItemBottom) {
					_contentHeight = curItemBottom;
				}
			}

		}
		_contentWidth += extX;
		_contentHeight += extY;

		if (needSyncItem) {
			_ItemData firstItemData = _data.get(0);
			_ItemData firstInstancedItemData = _data.get(_itemIdxOffset);

			_scrollX = _scrollTargetX = _skipItemOffsetX = firstInstancedItemData.relativeLeft - firstItemData.relativeLeft;
			_scrollY = _scrollTargetY = _skipItemOffsetY = firstInstancedItemData.relativeTop - firstItemData.relativeTop;

			_clipRangeUpdater.resetLayout();
		}
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutContainer();
	}
}
