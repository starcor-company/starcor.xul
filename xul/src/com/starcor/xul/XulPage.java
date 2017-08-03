package com.starcor.xul;

import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.starcor.xul.Events.XulActionEvent;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Factory.XulParserData;
import com.starcor.xul.Factory.XulParserDataNoStoreSupported;
import com.starcor.xul.Factory.XulParserDataStoreSupported;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.*;
import com.starcor.xul.PropMap.IXulPropIterator;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Script.IScript;
import com.starcor.xul.Script.XulScriptableObject;
import com.starcor.xul.ScriptWrappr.XulPageScriptableObject;
import com.starcor.xul.Utils.*;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.starcor.xul.Utils.XulBindingSelector.IXulDataSelectContext;

/**
 * Created by hy on 2014/5/5.
 */
public class XulPage extends XulView {
	private static final String TAG = XulPage.class.getSimpleName();

	private static final XulArrayList<XulView> EMPTY_ITEMS_BY_CLASS = new XulArrayList<XulView>();
	private final String _ownerId;

	public boolean isBindingFinished() {
		return _bindingMgr != null && _bindingMgr.isBindingFinished();
	}

	// for XulSelectorCtx
	private static WeakReference<XulView> getWeakReference(XulView view) {
		return view.getWeakReference();
	}

	static class SortItem {
		XulIntArray seq;
		XulView view;

		SortItem() {
			this.seq = new XulIntArray(32);
		}
	}

	static class XulSortArray extends XulSimpleArray<SortItem> {
		int _dataNum;
		XulArrayList<XulView> _seq;
		Comparator<SortItem> _comparator = new Comparator<SortItem>() {
			@Override
			public int compare(SortItem lhs, SortItem rhs) {
				XulIntArray seqBufA = lhs.seq;
				XulIntArray seqBufB = rhs.seq;
				int aPos = seqBufA.size() - 1;
				int bPos = seqBufB.size() - 1;

				while (aPos >= 0 && bPos >= 0) {
					int aVal = seqBufA.get(aPos);
					int bVal = seqBufB.get(bPos);
					int delta = aVal - bVal;
					if (delta != 0) {
						return delta;
					}
					--aPos;
					--bPos;
				}
				if (aPos == bPos) {
					return 0;
				}
				if (aPos < 0) {
					return -1;
				}
				return 1;
			}
		};

		@Override
		protected SortItem[] allocArrayBuf(int size) {
			return new SortItem[size];
		}

		public void setViews(XulArrayList<XulView> seq, XulView baseView) {
			_seq = seq;
			_dataNum = seq.size();
			for (int i = 0, sortBufSize = this.size(); i < _dataNum; ++i) {
				SortItem item;
				if (i < sortBufSize) {
					item = this.get(i);
				} else {
					item = new SortItem();
					this.add(item);
				}
				XulView xulView = seq.get(i);
				item.view = xulView;
				calViewSequence(item.seq, xulView, baseView);
			}
		}

		private XulIntArray calViewSequence(XulIntArray seqBuf, XulView view, XulView baseView) {
			seqBuf.clear();
			XulArea parent = view.getParent();
			while (parent != null) {
				int childPos = parent.findChildPos(view);
				seqBuf.add(childPos);
				if (parent == baseView) {
					break;
				}
				view = parent;
				parent = view.getParent();
			}
			return seqBuf;
		}

		public void sort() {
			Arrays.sort(getArray(), 0, _dataNum, _comparator);
		}

		public void clearViews() {
			for (int i = 0, itemSize = size(); i < itemSize; ++i) {
				get(i).view = null;
			}
			_seq = null;
		}

		public void restoreViews() {
			for (int i = 0; i < _dataNum; i++) {
				_seq.set(i, this.get(i).view);
			}
		}
	}

	static XulSortArray _sortBuffer = new XulSortArray();

	static void sortByViewSequence(XulArrayList<XulView> seq, XulView baseView) {
		int dataNum = seq.size();
		if (dataNum < 2) {
			return;
		}
		XulUtils.ticketMarker tm = null;
		if (XulManager.PERFORMANCE_BENCH) {
			tm = new XulUtils.ticketMarker("sortByViewSequence ", false);
			tm.mark();
		}
		_sortBuffer.setViews(seq, baseView);
		if (tm != null) {
			tm.mark("setViews");
		}
		_sortBuffer.sort();
		if (tm != null) {
			tm.mark("sort");
		}
		_sortBuffer.restoreViews();
		if (tm != null) {
			tm.mark("restore");
		}
		_sortBuffer.clearViews();
		if (tm != null) {
			tm.mark("clear");
			Log.d("BENCH!!!", tm.toString());
		}
	}

	static class XulSelectList extends XulSimpleArray<XulSelect> {

		@Override
		protected XulSelect[] allocArrayBuf(int size) {
			return new XulSelect[size];
		}
	}

	static class XulWeakXulViewList extends XulSimpleArray<WeakReference<XulView>> {
		@Override
		protected WeakReference<XulView>[] allocArrayBuf(int size) {
			return new WeakReference[size];
		}
	}

	class XulSelectorCtx {
		XulCachedHashMap<String, XulWeakXulViewList> _itemMap = new XulCachedHashMap<String, XulWeakXulViewList>();
		XulCachedHashMap<String, XulSelectList> _selectMap = new XulCachedHashMap<String, XulSelectList>();

		XulCachedHashMap<String, WeakHashMap<XulView, XulArrayList<XulView>>> _findItemByClassCache = new XulCachedHashMap<String, WeakHashMap<XulView, XulArrayList<XulView>>>();

		private void updateFindItemCache(String classKey, XulView owner, XulArrayList<XulView> result) {
			WeakHashMap<XulView, XulArrayList<XulView>> xulAreaArrayListXulCachedHashMap = _findItemByClassCache.get(classKey);
			if (xulAreaArrayListXulCachedHashMap == null) {
				if (result == null) {
					return;
				}
				xulAreaArrayListXulCachedHashMap = new WeakHashMap<XulView, XulArrayList<XulView>>();
				_findItemByClassCache.put(classKey, xulAreaArrayListXulCachedHashMap);
			}
			if (result == null) {
				if (owner == null) {
					_findItemByClassCache.remove(classKey);
				} else {
					xulAreaArrayListXulCachedHashMap.remove(owner);
				}
			} else {
				xulAreaArrayListXulCachedHashMap.put(owner, result);
			}
		}

		private void removeItemFromFindCache(String classKey, XulView item) {
			WeakHashMap<XulView, XulArrayList<XulView>> xulAreaArrayListXulCachedHashMap = _findItemByClassCache.get(classKey);
			if (xulAreaArrayListXulCachedHashMap == null) {
				return;
			}
			for (Map.Entry<XulView, XulArrayList<XulView>> entry : xulAreaArrayListXulCachedHashMap.entrySet()) {
				if (item.isChildOf(entry.getKey())) {
					entry.getValue().remove(item);
				}
			}
		}

		private XulArrayList<XulView> findItemsByCache(String classKey, XulArea owner) {
			WeakHashMap<XulView, XulArrayList<XulView>> xulAreaArrayListXulCachedHashMap = _findItemByClassCache.get(classKey);
			if (xulAreaArrayListXulCachedHashMap == null) {
				return null;
			}
			return xulAreaArrayListXulCachedHashMap.get(owner);
		}

		XulSelectorCtx(XulPage page) {
			addItem(page);

			XulLayout layout = page.getLayout();
			if (layout == null) {
				Log.e(TAG, "page is empty!! [" + page.getId() + "]");
				return;
			}
			addItem(layout);
		}

		class _XulSelectorAddChildrenIterator extends XulArea.XulAreaIterator {
			@Override
			public boolean onXulArea(int pos, XulArea area) {
				addItem(area);
				return true;
			}

			@Override
			public boolean onXulItem(int pos, XulItem item) {
				addItem(item);
				return true;
			}
		}

		_XulSelectorAddChildrenIterator _addChildrenItem = new _XulSelectorAddChildrenIterator();

		void addItem(XulArea area) {
			addItem((XulView) area);
			area.eachChild(_addChildrenItem);
		}

		private void addItemByKeys(XulView item, ArrayList<String> keys) {
			if (keys == null) {
				return;
			}

			for (int i = 0, keySize = keys.size(); i < keySize; i++) {
				String key = keys.get(i);
				XulWeakXulViewList itemSet = _itemMap.get(key);
				WeakReference<XulView> weakReference = getWeakReference(item);

				if (key.startsWith(".")) {
					updateFindItemCache(key, null, null);
				}

				if (itemSet == null) {
					itemSet = new XulWeakXulViewList();
					_itemMap.put(key, itemSet);
				}

				if (itemSet.contains(weakReference)) {
					continue;
				}

				itemSet.add(weakReference);
			}
		}

		void addItem(XulView item) {
			addItemByKeys(item, item.getSelectKeys());
		}

		void removeItemByKeys(XulView item, ArrayList<String> keys) {
			if (keys == null) {
				return;
			}

			for (int i = 0, keySize = keys.size(); i < keySize; i++) {
				String key = keys.get(i);
				XulWeakXulViewList itemSet = _itemMap.get(key);

				if (itemSet == null) {
					continue;
				}
				if (key.startsWith(".")) {
					// remove cached keys
					removeItemFromFindCache(key, item);
				}
				WeakReference<XulView> weakReference = getWeakReference(item);
				itemSet.remove(weakReference);
			}
		}

		void addSelect(XulSelect select) {
			String key = select.getSelectKey();
			XulSelectList xulSelects = _selectMap.get(key);
			if (xulSelects == null) {
				xulSelects = new XulSelectList();
				_selectMap.put(key, xulSelects);
			}
			if (xulSelects.contains(select)) {
				return;
			}
			xulSelects.add(select);
		}

		void apply(XulSelect select) {
			// FIXME: 仅支持一级选择器
			String selectKey = select.getSelectKey();

			XulWeakXulViewList itemSet = _itemMap.get(selectKey);
			if (itemSet == null) {
				return;
			}

			for (int i = 0, itemSetSize = itemSet.size(); i < itemSetSize; ) {
				WeakReference<XulView> xulViewRef = itemSet.get(i);
				XulView xulView = xulViewRef.get();
				if (xulView == null) {
					itemSet.remove(i);
					--itemSetSize;
					continue;
				}
				++i;
				select.apply(xulView);
			}
		}

		void apply(XulArea ctx, XulSelect select) {
			// FIXME: 仅支持一级选择器
			String selectKey = select.getSelectKey();

			XulWeakXulViewList itemSet = _itemMap.get(selectKey);
			if (itemSet == null) {
				return;
			}

			for (int i = 0, itemSetSize = itemSet.size(); i < itemSetSize; ) {
				WeakReference<XulView> xulViewRef = itemSet.get(i);
				XulView xulView = xulViewRef.get();
				if (xulView == null) {
					itemSet.remove(i);
					--itemSetSize;
					continue;
				}
				++i;
				if (!ctx.hasChild(xulView)) {
					continue;
				}
				select.apply(xulView);
			}
		}

		boolean applySelectors(XulView view, ArrayList<String> keys) {
			if (keys == null) {
				return false;
			}
			boolean needUpdate = false;
			for (int i = 0, keysSize = keys.size(); i < keysSize; i++) {
				String key = keys.get(i);
				XulSelectList xulSelects = _selectMap.get(key);
				if (xulSelects == null) {
					continue;
				}

				for (int j = 0, xulSelectsSize = xulSelects.size(); j < xulSelectsSize; j++) {
					XulSelect select = xulSelects.get(j);
					select.apply(view);
					needUpdate = true;
				}
			}
			return needUpdate;
		}

		boolean unApplySelectors(XulView view, ArrayList<String> keys) {
			if (keys == null) {
				return false;
			}
			boolean needUpdate = false;
			for (int i = 0, keysSize = keys.size(); i < keysSize; i++) {
				String key = keys.get(i);
				XulSelectList xulSelects = _selectMap.get(key);
				if (xulSelects == null) {
					continue;
				}

				for (int j = 0, xulSelectsSize = xulSelects.size(); j < xulSelectsSize; j++) {
					XulSelect select = xulSelects.get(j);
					select.unApply(view);
					needUpdate = true;
				}
			}
			return needUpdate;
		}

		void applySelectors() {
			for (XulSelectList xulSelects : _selectMap.values()) {
				for (int i = 0, xulSelectsSize = xulSelects.size(); i < xulSelectsSize; i++) {
					XulSelect select = xulSelects.get(i);
					apply(select);
				}
			}
		}

		XulView findItemById(String id) {
			if (_itemMap == null) {
				return null;
			}
			XulWeakXulViewList xulViews = _itemMap.get("#" + id);
			if (xulViews == null) {
				return null;
			}
			for (int i = 0, xulViewsSize = xulViews.size(); i < xulViewsSize; ) {
				XulView xulView = xulViews.get(i).get();
				if (xulView == null || !_currentLayout.hasChild(xulView)) {
					xulViews.remove(i);
					--xulViewsSize;
					continue;
				}
				return xulView;
			}
			return null;
		}

		XulView findCustomItemByExtView(IXulExternalView extView) {
			if (_itemMap == null) {
				return null;
			}
			XulWeakXulViewList xulViews = _itemMap.get("@custom");
			if (xulViews == null) {
				return null;
			}
			for (int i = 0, xulViewsSize = xulViews.size(); i < xulViewsSize; ) {
				XulView xulView = xulViews.get(i).get();
				if (xulView == null) {
					xulViews.remove(i);
					--xulViewsSize;
					continue;
				}
				if (xulView.getExternalView() == extView) {
					return xulView;
				}
				++i;
			}
			return null;
		}

		public void applySelectors(XulArea area) {
			applySelectors(area, area.getSelectKeys());
			for (XulSelectList xulSelects : _selectMap.values()) {
				for (int i = 0, xulSelectsSize = xulSelects.size(); i < xulSelectsSize; i++) {
					XulSelect select = xulSelects.get(i);
					apply(area, select);
				}
			}
		}

		public void applySelectors(XulItem item) {
			applySelectors(item, item.getSelectKeys());
		}

		public XulView findItemById(XulArea owner, String id) {
			if (_itemMap == null) {
				return null;
			}
			XulWeakXulViewList xulViews = _itemMap.get("#" + id);
			if (xulViews == null) {
				return null;
			}
			for (int i = 0, xulViewsSize = xulViews.size(); i < xulViewsSize; ) {
				XulView xulView = xulViews.get(i).get();
				if (xulView == null) {
					xulViews.remove(i);
					--xulViewsSize;
					continue;
				}
				if (owner.hasChild(xulView)) {
					return xulView;
				}
				++i;
			}
			return null;
		}

		public XulArrayList<XulView> findItemsByClass(XulArea owner, String cls) {
			if (_itemMap == null) {
				return EMPTY_ITEMS_BY_CLASS;
			}
			String classKey = "." + cls;

			XulArrayList<XulView> cachedResult = findItemsByCache(classKey, owner);
			if (cachedResult != null) {
				return cachedResult;
			}

			XulWeakXulViewList xulViews = _itemMap.get(classKey);
			if (xulViews == null) {
				return EMPTY_ITEMS_BY_CLASS;
			}
			XulArrayList<XulView> result = new XulArrayList<XulView>();
			for (int i = 0, xulViewsSize = xulViews.size(); i < xulViewsSize; ) {
				XulView xulView = xulViews.get(i).get();
				if (xulView == null) {
					xulViews.remove(i);
					--xulViewsSize;
					continue;
				}
				if (owner.hasChild(xulView)) {
					result.add(xulView);
				}
				++i;
			}

			sortByViewSequence(result, owner);
			updateFindItemCache(classKey, owner, result);
			return result;
		}

		public XulView findFirstItemByClass(XulArea owner, String cls) {
			return null;
		}
	}

	class XulBindingManager {
		public void preloadBinding(XulWorker.IXulWorkItemSource workItemSource) {
			int bindingSize = _bindingList.size();
			for (int i = 0; i < bindingSize; i++) {
				XulBindingDownloadItem item = _bindingList.get(i);
				if (item.isReadyToLoad() && item.isPreloadBinding()) {
					String bindingURL = item.getBindingURL();
					InputStream inputStream = XulWorker.loadData(bindingURL, workItemSource);
					if (inputStream != null) {
						item.finish(inputStream);
						if (XulManager.DEBUG) {
							Log.d(TAG, "preloadBinding success: " + bindingURL);
						}
					} else {
						if (XulManager.DEBUG) {
							Log.d(TAG, "preloadBinding failed: " + bindingURL);
						}
					}
				}
			}
		}

		private class XulBindingCtx {
			XulView owner;
			ArrayList<Object> pendingBindings = new ArrayList<Object>();

			void onBinding(XulBinding xulBinding, Object obj, Object value) throws Exception {
				XulProp prop = (XulProp) obj;

				XulBinding bindingSource = prop.getBindingSource();
				if (bindingSource != null && !bindingSource.isUpdated()) {
					return;
				}
				prop.setBindingReady();
				prop.setValue(value);
				prop.setBindingSource(xulBinding);
				owner.resetRender();
				this.pendingBindings.remove(obj);
			}

			// 返回元素上绑定的数据
			public Object getObjectBindingData(Object obj) {
				XulProp prop = (XulProp) obj;
				return prop.getValue();
			}

			String getDataSelector(Object obj) {
				XulProp prop = (XulProp) obj;
				return prop.getBinding();
			}

			boolean isDiscarded() {
				return owner.isDiscarded();
			}

			boolean isEmpty() {
				return pendingBindings.isEmpty();
			}

			void add(Object obj) {
				pendingBindings.add(obj);
			}

			public XulView getOwner() {
				return owner;
			}

			public XulView getDataContext() {
				XulView view = owner;
				while (view != null && !view.hasBindingCtx()) {
					view = view._parent;
				}
				if (view != null && view.hasBindingCtx()) {
					return view;
				}
				return null;
			}
		}

		class XulBindingDownloadItem extends XulWorker.DownloadItem {
			XulBinding _binding;
			volatile boolean _isLoading = false;
			long _lastFailTime = 0;

			boolean isReadyToLoad() {
				if (_binding == null) {
					return false;
				}
				if (_isLoading) {
					return false;
				}
				return isPending();
			}

			boolean isPending() {
				return _binding.isRefreshing() || (_binding.isRemoteData() && !_binding.isDataReady() && !_binding.isUpdated());
			}

			void finish(InputStream data) {
				_isLoading = false;
				if (data == null) {
					_binding.setEmptyData();
					Log.e(XulRenderContext.TAG, "download data failed!!!!");
					invokeActionNoPopupWithArgs(XulPage.this, "bindingError", _binding.getId(), "DOWNLOAD-FAILED");
					return;
				}
				_binding.setData(data);
				invokeActionNoPopupWithArgs(XulPage.this, "bindingReady", _binding.getId());
			}

			public boolean isPreloadBinding() {
				return _binding.isPreloadBinding();
			}

			public String getBindingURL() {
				return _binding.getDataUrl();
			}
		}

		ArrayList<XulBindingDownloadItem> _bindingList;
		XulCachedHashMap<String, XulBinding> _bindingMap = new XulCachedHashMap<String, XulBinding>();
		ArrayList<XulBindingCtx> _pendingCtx = new ArrayList<XulBindingCtx>();
		ArrayList<ArrayList<XulBindingCtx>> _rebindViewCtxStack = new ArrayList<ArrayList<XulBindingCtx>>();
		int _rebindViewCtxStackPointer = -1;
		boolean _bindingFinished = false;

		XulBindingManager() {
			_bindingList = new ArrayList<XulBindingDownloadItem>();
			ArrayList<XulBinding> bindings = _bindings;
			if (_globalBindings != null) {
				for (int i = 0; i < _globalBindings.size(); i++) {
					XulBinding binding = _globalBindings.get(i);
					XulBindingDownloadItem xulBindingDownloadItem = new XulBindingDownloadItem();
					xulBindingDownloadItem._binding = binding;
					_bindingList.add(xulBindingDownloadItem);
				}
			}

			for (int i = 0; i < bindings.size(); i++) {
				XulBinding binding = bindings.get(i);
				XulBindingDownloadItem xulBindingDownloadItem = new XulBindingDownloadItem();
				xulBindingDownloadItem._binding = binding;
				_bindingList.add(xulBindingDownloadItem);
			}
			if (_bindingList.isEmpty()) {
				return;
			}
			XulBindingCtx bindingCtx = new XulBindingCtx() {
				@Override
				void onBinding(XulBinding xulBinding, Object obj, Object value) throws Exception {
					super.onBinding(xulBinding, obj, value);
					XulBinding binding = (XulBinding) obj;
					if (value instanceof String) {
						binding.setDataUrl((String) value);
						binding.setBindingSource(xulBinding);
					} else if (value instanceof XulDataNode) {
						binding.setData((XulDataNode) value);
						binding.setBindingSource(xulBinding);
					} else {
						throw new Exception("unsupported binding _targets");
					}
				}

				// 返回元素上绑定的数据
				@Override
				public Object getObjectBindingData(Object obj) {
					XulBinding binding = (XulBinding) obj;
					return binding.getData();
				}
			};
			bindingCtx.owner = XulPage.this;
			for (int i = 0; i < _bindingList.size(); i++) {
				XulBindingDownloadItem item = _bindingList.get(i);
				XulBinding binding = item._binding;
				_bindingMap.put(binding.getId(), binding);
//			Logger.i(TAG, "xulpage_hxf binding="+binding.getName()+"binding.getId()="+binding.getId());
				if (binding.isBindingPending()) {
					bindingCtx.add(binding);
				}
			}

			if (!bindingCtx.isEmpty()) {
				_pendingCtx.add(bindingCtx);
			}

			collectPendingCtx(XulPage.this, _pendingCtx);
			collectPendingCtx(getLayout(), _pendingCtx);
		}

		public synchronized void addBinding(XulBinding binding) {
			XulBindingDownloadItem xulBindingDownloadItem = new XulBindingDownloadItem();
			xulBindingDownloadItem._binding = binding;
			_bindingList.add(xulBindingDownloadItem);
			_bindingMap.put(binding.getId(), binding);
		}

		private class _ChildrenCollector extends XulArea.XulAreaIterator {
			class _ChildrenCollectorSavedLevel {
				XulBindingCtx areaCtx;
				XulBindingCtx templateCtx;
				XulArea area;
				ArrayList<XulBindingCtx> pendingCtx;

				public _ChildrenCollectorSavedLevel() {
					pushValue();
				}

				void pushValue() {
					this.areaCtx = _ChildrenCollector.this.areaCtx;
					this.templateCtx = _ChildrenCollector.this.templateCtx;
					this.area = _ChildrenCollector.this.area;
					this.pendingCtx = _ChildrenCollector.this.pendingCtx;
				}

				void popValue() {
					_ChildrenCollector.this.areaCtx = this.areaCtx;
					_ChildrenCollector.this.templateCtx = this.templateCtx;
					_ChildrenCollector.this.area = this.area;
					_ChildrenCollector.this.pendingCtx = this.pendingCtx;

					this.areaCtx = null;
					this.templateCtx = null;
					this.area = null;
					this.pendingCtx = null;
				}
			}

			class _ChildrenCollectStack extends XulSimpleArray<_ChildrenCollectorSavedLevel> {

				int _curPos;

				public _ChildrenCollectStack(int sz) {
					super(sz);
				}

				@Override
				protected _ChildrenCollectorSavedLevel[] allocArrayBuf(int size) {
					return new _ChildrenCollectorSavedLevel[size];
				}

				public void pushState() {
					if (_curPos >= size()) {
						add(new _ChildrenCollectorSavedLevel());
					} else {
						this.get(_curPos).pushValue();
					}
					++_curPos;
				}

				public void popState() {
					--_curPos;
					get(_curPos).popValue();
				}
			}

			private _ChildrenCollectStack _collectCtx = new _ChildrenCollectStack(64);
			XulBindingCtx areaCtx;
			XulBindingCtx templateCtx;
			XulArea area;
			ArrayList<XulBindingCtx> pendingCtx;

			public void begin(XulArea area, ArrayList<XulBindingCtx> pendingCtx) {
				_collectCtx.pushState();

				this.areaCtx = null;
				this.templateCtx = null;
				this.area = area;
				this.pendingCtx = pendingCtx;
			}

			public void end() {
				_collectCtx.popState();
			}

			private void initAreaCtx() {
				if (areaCtx != null) {
					return;
				}
				areaCtx = new XulBindingCtx() {
					@Override
					String getDataSelector(Object obj) {
						XulView view = (XulView) obj;
						return view.getBinding();
					}

					@Override
					void onBinding(XulBinding xulBinding, Object obj, Object value) throws Exception {
						XulView view = (XulView) obj;
						XulBinding bindingSource = view.getBindingSource();
						if (bindingSource != null && !bindingSource.isUpdated()) {
							return;
						}
						ArrayList<XulDataNode> dataNodes = (ArrayList<XulDataNode>) value;
						view.setBindingSource(xulBinding);
						if (dataNodes.size() > 0) {
							view.setBindingCtx(dataNodes);
						} else {
							if (XulManager.DEBUG_BINDING) {
								Log.w(XulRenderContext.TAG, "binding targets not found!!!");
							}
						}
						this.pendingBindings.remove(obj);
					}

					// 返回元素上绑定的数据
					@Override
					public Object getObjectBindingData(Object obj) {
						XulView view = (XulView) obj;
						return view.getBindingSource();
					}
				};
				areaCtx.owner = area;
				pendingCtx.add(areaCtx);
			}

			@Override
			public boolean onXulArea(int pos, XulArea area) {
				if (area.hasBindingCtx() && !area.isBindingCtxReady()) {
					initAreaCtx();
					areaCtx.add(area);
				}
				collectPendingCtx(area, pendingCtx);
				return true;
			}

			@Override
			public boolean onXulItem(int pos, XulItem item) {
				if (item.hasBindingCtx() && !item.isBindingCtxReady()) {
					initAreaCtx();
					areaCtx.add(item);
				}
				collectPendingCtx(item, pendingCtx);
				return true;
			}

			@Override
			public boolean onXulTemplate(final int pos, final XulTemplate template) {
				if (TextUtils.isEmpty(template.getBinding())) {
					// no binding target
					return true;
				}
				if (templateCtx == null) {
					templateCtx =  new XulBindingCtx() {

						ArrayList<XulBindingCtx> pendingCtx = _ChildrenCollector.this.pendingCtx;

						@Override
						String getDataSelector(Object obj) {
							Pair<Integer, XulTemplate> pair = (Pair<Integer, XulTemplate>) obj;
							return pair.second.getBinding();
						}

						@Override
						void onBinding(XulBinding xulBinding, Object obj, Object value) throws Exception {
							Pair<Integer, XulTemplate> pair = (Pair<Integer, XulTemplate>) obj;
							XulTemplate view = pair.second;
							XulBinding bindingSource = view.getBindingSource();
							if (bindingSource != null && !bindingSource.isUpdated()) {
								return;
							}
							ArrayList<XulDataNode> dataNodes = (ArrayList<XulDataNode>) value;
							view.setBindingSource(xulBinding);
							view.bindingData(dataNodes);
							this.pendingBindings.remove(obj);

							collectPendingCtx(view, pendingCtx);
						}

						// 返回元素上绑定的数据
						@Override
						public Object getObjectBindingData(Object obj) {
							Pair<Integer, XulTemplate> pair = (Pair<Integer, XulTemplate>) obj;
							XulTemplate view = pair.second;
							return view.getBindingSource();
						}
					};
					templateCtx.owner = area;
					pendingCtx.add(templateCtx);
				}
				templateCtx.add(Pair.create(pos, template));
				return true;
			}
		}

		private _ChildrenCollector _childrenCollector = new _ChildrenCollector();

		private void collectPendingCtx(XulArea area, ArrayList<XulBindingCtx> pendingCtx) {
			if (area == null) {
				Log.e(XulRenderContext.TAG, "collectPendingCtx failed!! invalid area(null) object");
				return;
			}
			collectPendingCtx((XulView) area, pendingCtx);
			_childrenCollector.begin(area, pendingCtx);
			area.eachChild(_childrenCollector);
			_childrenCollector.end();
		}

		private void collectPendingCtx(XulTemplate template, ArrayList<XulBindingCtx> pendingCtx) {
			if (template == null) {
				Log.e(XulRenderContext.TAG, "collectPendingCtx failed!! invalid template(null) object");
				return;
			}
			if (template._instance == null) {
				Log.w(XulRenderContext.TAG, "collectPendingCtx failed!! template not instanced " + template);
				return;
			}
			XulTemplate.XulViewArrayList instance = template._instance;
			int size = instance.size();
			XulView[] instanceArray = instance.getArray();
			for (int i = 0; i < size; i++) {
				XulView item = instanceArray[i];
				if (item instanceof XulArea) {
					collectPendingCtx((XulArea) item, pendingCtx);
				} else {
					collectPendingCtx(item, pendingCtx);
				}
			}
		}

		private class _PropertyCollector implements IXulPropIterator<XulProp> {
			XulBindingCtx ctx;
			XulView view;
			private ArrayList<XulBindingCtx> pendingCtx;

			public void begin(XulView view, ArrayList<XulBindingCtx> pendingCtx) {
				this.view = view;
				this.ctx = null;
				this.pendingCtx = pendingCtx;
			}

			@Override
			public void onProp(XulProp prop, int state) {
				if (prop.isBinding() && prop.isBindingPending()) {
					initBindingCtx();
					ctx.add(prop);
				}
			}

			private void initBindingCtx() {
				if (ctx != null) {
					return;
				}
				ctx = new XulBindingCtx();
				ctx.owner = view;
				pendingCtx.add(ctx);
			}

			public void end() {
				ctx = null;
				view = null;
				pendingCtx = null;
			}
		}

		private _PropertyCollector _propCollector = new _PropertyCollector();

		private void collectPendingCtx(XulView view, ArrayList<XulBindingCtx> pendingCtx) {
			if (view == null) {
				Log.e(XulRenderContext.TAG, "collectPendingCtx failed!! invalid view(null) object");
				return;
			}
			_propCollector.begin(view, pendingCtx);
			view.eachInlineProp(_propCollector);
			_propCollector.end();
		}

		synchronized XulWorker.DownloadItem getPendingItem() {
			int bindingSize = _bindingList.size();
			for (int i = 0; i < bindingSize; i++) {
				XulBindingDownloadItem item = _bindingList.get(i);
				if (item.isReadyToLoad()) {
					if (XulManager.DEBUG) {
						Log.d(TAG, "getPendingItem " + item._binding.getId() + " " + item._binding.getDataUrl());
					}
					item._isLoading = true;
					item.url = item._binding.getDataUrl();
					return item;
				}
			}
			return null;
		}

		synchronized boolean setPendingItemData(XulWorker.DownloadItem item, InputStream data, IActionCallback actionCallback) {
			if (!(item instanceof XulBindingDownloadItem)) {
				return false;
			}
			XulBindingDownloadItem downloadItem = (XulBindingDownloadItem) item;
			downloadItem.finish(data);
			return applyBinding(actionCallback);
		}

		boolean applyBinding(IActionCallback actionCallback) {
			if (_bindingList.isEmpty()) {
				return true;
			}
			if (_bindingFinished) {
				return updateBinding(actionCallback, _pendingCtx);
			}
			for (int i = 0, bindingSize = _bindingList.size(); i < bindingSize; i++) {
				XulBindingDownloadItem item = _bindingList.get(i);
				if (item.isPending()) {
					// 远程数据未准备完成
					return false;
				}
			}

			_doBinding(actionCallback, _pendingCtx);
			_bindingFinished = _pendingCtx.isEmpty();
			return _bindingFinished;
		}

		private void _doBinding(IActionCallback actionCallback, ArrayList<XulBindingCtx> pendingCtx) {
			long begin = XulUtils.timestamp();
			_selectData = 0;

			for (int i = 0; i < pendingCtx.size(); ++i) {
				XulBindingCtx ctx = pendingCtx.get(i);
				for (Object obj : ctx.pendingBindings.toArray()) {
					String dataSelector = ctx.getDataSelector(obj);
					XulView bindingCtxView = ctx.getDataContext();
					if (bindingCtxView != null && bindingCtxView._bindingData == null && !dataSelector.startsWith("#")) {
						// FIXME: assignment selector will fail
						if (XulManager.DEBUG_BINDING) {
							Log.w(TAG, "binding context not ready!! " + dataSelector);
						}
						ctx.pendingBindings.remove(obj);
						continue;
					}
					ArrayList<XulDataNode> dataContext = bindingCtxView != null ? bindingCtxView._bindingData : null;
					ArrayList<XulDataNode> xulDataNodes = selectData(dataSelector, dataContext);
					if (xulDataNodes != null) {
						try {
							XulBinding xulBinding = null;
							XulDataNode dataNode = null;
							if (!xulDataNodes.isEmpty()) {
								dataNode = xulDataNodes.get(0);
								if (dataNode == null) {
									if (XulManager.DEBUG_BINDING) {
										Log.e(TAG, "fatal binding error!! " + dataSelector);
									}
								} else {
									xulBinding = dataNode.getOwnerBinding();
								}
							}
							if (obj instanceof XulProp) {
								Object data = "";
								if (dataNode != null) {
									if (dataNode.hasChild()) {
										data = dataNode;
									} else {
										data = dataNode.getValue();
									}
								}
								ctx.onBinding(xulBinding, obj, data);
							} else {
								ctx.onBinding(xulBinding, obj, xulDataNodes);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						// 绑定失败
						if (XulManager.DEBUG_BINDING) {
							Log.e(XulRenderContext.TAG, "数据绑定失败!! " + dataSelector + " - " + obj.toString());
						}
						ctx.pendingBindings.remove(obj);
					}
				}
			}

			applySelectors();
			notifyBindingEvent(actionCallback, "bindingFinished", pendingCtx);

			long end = XulUtils.timestamp();
			if (XulManager.PERFORMANCE_BENCH) {
				Log.d("BENCH!!!", "applyBinding/" + pendingCtx.isEmpty() + " " + (end - begin) + " select:" + (_selectData / 1000.0f));
			}
		}

		IXulDataSelectContext selectContext = new IXulDataSelectContext() {
			@Override
			public boolean isEmpty() {
				return _bindingList == null || _bindingList.isEmpty();
			}

			@Override
			public XulBinding getDefaultBinding() {
				return _bindingList.get(0)._binding;
			}

			@Override
			public XulBinding getBindingById(String id) {
				return _bindingMap.get(id);
			}
		};

		long _selectData = 0;

		private ArrayList<XulDataNode> selectData(String dataSelector, ArrayList<XulDataNode> ctx) {
			long begin = XulUtils.timestamp_us();
			ArrayList<XulDataNode> xulDataNodes = XulBindingSelector.selectData(selectContext, dataSelector, ctx);
			_selectData += XulUtils.timestamp_us() - begin;
			return xulDataNodes;
		}

		boolean isBindingFinished() {
			return _bindingFinished;
		}

		boolean updateBinding(IActionCallback actionCallback, ArrayList<XulBindingCtx> pendingCtx) {
			boolean anyBindingUpdated = false;
			for (int i = 0, bindingListSize = _bindingList.size(); i < bindingListSize; i++) {
				XulBindingDownloadItem item = _bindingList.get(i);
				XulBinding binding = item._binding;
				if (binding.isUpdated()) {
					anyBindingUpdated = true;
					continue;
				}
			}

			if (!anyBindingUpdated) {
				// 没有更新的binding
				return false;
			}

			boolean anyBindingRefreshing = false;

			for (int i = 0; i < _bindingList.size(); i++) {
				XulBindingDownloadItem item = _bindingList.get(i);
				XulBinding binding = item._binding;
				if (binding.isRefreshing()) {
					// 有binding处于刷新状态
					anyBindingRefreshing = true;
					continue;
				}
				if (binding.isUpdated() || !binding.isBinding()) {
					continue;
				}
				XulDataNode dataNode;
				if (binding.getData() != null) {
					XulBinding ownerBinding = binding.getData().getOwnerBinding();
					if (!ownerBinding.isUpdated()) {
						// 依赖的目标数据未更新
						continue;
					}
					binding.refreshBinding();
					String bindingSelector = binding.getBinding();
					ArrayList<XulDataNode> result = selectData(bindingSelector, null);
					if (result == null || result.isEmpty()) {
						binding.setEmptyData();
						continue;
					}
					dataNode = result.get(0);
				} else {
					// 数据处于未绑定状态，则通过数据选择器进行绑定更新判定
					String bindingSelector = binding.getBinding();
					ArrayList<XulDataNode> result = selectData(bindingSelector, null);
					if (result == null || result.isEmpty()) {
//		//				Logger.i(TAG, "result.isEmpty()  true ");
						continue;
					}
					dataNode = result.get(0);
					if (dataNode.getOwnerBinding() == null || !dataNode.getOwnerBinding().isUpdated()) {
						continue;
					}
					binding.refreshBinding();
				}

				if (dataNode.hasChild()) {
					binding.setData(dataNode);
				} else {
					binding.setDataUrl(dataNode.getValue());
				}

				if (binding.isRefreshing()) {
					// 有binding处于刷新状态
					anyBindingRefreshing = true;
				}
			}
			if (anyBindingRefreshing) {
				// 有binding处于刷新状态, 必须等待所有数据源都更新完成
				return false;
			}

			collectPendingCtx(getLayout(), pendingCtx);

			for (int i = 0; i < pendingCtx.size(); ++i) {
				XulBindingCtx ctx = pendingCtx.get(i);
				if (ctx.isDiscarded()) {
					if (XulManager.DEBUG_BINDING) {
						Log.w(TAG, "_update binding: pending ctx discarded!");
					}
					pendingCtx.remove(i);
					--i;
					continue;
				}
				int pendingItems = ctx.pendingBindings.size();
				int updateItems = 0;
				for (Object obj : ctx.pendingBindings.toArray()) {
					String dataSelector = ctx.getDataSelector(obj);
					XulView bindingCtxView = ctx.getDataContext();
					if (bindingCtxView != null && bindingCtxView._bindingData == null) {
						if (XulManager.DEBUG_BINDING) {
							Log.d(TAG, "binding context not ready!! " + dataSelector);
						}
						ctx.pendingBindings.clear();
						continue;
					}
					ArrayList<XulDataNode> dataContext = bindingCtxView != null ? bindingCtxView._bindingData : null;
					ArrayList<XulDataNode> xulDataNodes = selectData(dataSelector, dataContext);
					if (xulDataNodes != null) {
						try {
							XulBinding xulBinding = null;
							XulDataNode dataNode = null;
							if (!xulDataNodes.isEmpty()) {
								dataNode = xulDataNodes.get(0);
								xulBinding = dataNode.getOwnerBinding();
							}
							boolean isDataReadyBefore = ctx.getObjectBindingData(obj) != null;
							boolean isDataReadyCurrently = dataNode != null;
							if (isDataReadyCurrently || isDataReadyBefore) {
								// 当前绑定了新数据，或当前绑定数据为空但之前绑定过数据
								// 表示数据绑定有更新
								++updateItems;
							} else {
								if (XulManager.DEBUG_BINDING) {
									Log.d(TAG, "binding item not updated!! " + dataSelector + " " + isDataReadyCurrently + " / " + isDataReadyBefore);
								}
							}

							if (obj instanceof XulProp) {
								Object data = "";
								if (dataNode != null) {
									if (dataNode.hasChild()) {
										data = dataNode;
									} else {
										data = dataNode.getValue();
									}
								}
								ctx.onBinding(xulBinding, obj, data);
							} else {
								ctx.onBinding(xulBinding, obj, xulDataNodes);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						// 绑定失败
						if (XulManager.DEBUG_BINDING) {
							Log.e(XulRenderContext.TAG, "数据绑定失败!! " + dataSelector + " - " + obj.toString());
						}
						ctx.pendingBindings.remove(obj);
					}
				}

				if (updateItems == 0) {
					if (XulManager.DEBUG_BINDING) {
						Log.w(TAG, "_update binding: no item updated!");
					}
					pendingCtx.remove(i);
					--i;
					continue;
				}
			}

			applySelectors();
			notifyBindingEvent(actionCallback, "bindingUpdated", pendingCtx);

			pendingCtx.clear();
			for (int i = 0, bindingListSize = _bindingList.size(); i < bindingListSize; i++) {
				XulBindingDownloadItem item = _bindingList.get(i);
				XulBinding binding = item._binding;
				binding.markReady();
			}

			return true;
		}

		private void notifyBindingEvent(IActionCallback actionCallback, String bindingUpdated, ArrayList<XulBindingCtx> pendingCtx) {
			final HashMap<XulView, XulAction> notifiedTargets = new LinkedHashMap<XulView, XulAction>();
			for (int i = pendingCtx.size() - 1; i >= 0; --i) {
				if (pendingCtx.get(i).isEmpty()) {
					XulBindingCtx bindingCtx = pendingCtx.remove(i);
					XulView ctxOwner = bindingCtx.getOwner();
					XulAction bindingUpdatedAction = ctxOwner.getAction(bindingUpdated);
					if (bindingUpdatedAction == null) {
						continue;
					}
					if (notifiedTargets.containsKey(ctxOwner)) {
						continue;
					}
					notifiedTargets.put(ctxOwner, bindingUpdatedAction);
				}
			}
			asyncInvokeAction(notifiedTargets, actionCallback);
		}

		public void refreshBinding(String bindingId) {
			XulBinding binding = _bindingMap.get(bindingId);
			if (binding == null) {
				return;
			}
			binding.refreshBinding();
		}

		public void refreshBinding(String bindingId, String url) {
			XulBinding binding = _bindingMap.get(bindingId);
			if (binding == null) {
				return;
			}
			if (url == null) {
				binding.refreshBinding();
			} else {
				binding.refreshBinding(url);
			}
		}

		public void refreshBinding(String bindingId, XulDataNode dataNode) {
			XulBinding binding = _bindingMap.get(bindingId);
			if (binding == null) {
				return;
			}
			binding.refreshBinding(dataNode);
		}

		private XulView _singleRebindView[] = new XulView[1];
		public <T extends XulView> boolean rebindView(T xulView, IActionCallback actionCallback) {
			_singleRebindView[0] = xulView;
			return rebindViews(_singleRebindView, 1, actionCallback);
		}

		public <T extends XulView> boolean rebindViews(T[] xulViews, int viewNum, IActionCallback actionCallback) {
			if (!_bindingList.isEmpty() && !_bindingFinished) {
				return false;
			}
			for (int i = 0, bindingListSize = _bindingList.size(); i < bindingListSize; i++) {
				XulBindingDownloadItem item = _bindingList.get(i);
				if (item.isPending()) {
					// 远程数据未准备完成
					return false;
				}
			}
			++_rebindViewCtxStackPointer;
			ArrayList<XulBindingCtx> pendingCtx;
			if (_rebindViewCtxStackPointer >= _rebindViewCtxStack.size()) {
				pendingCtx = new ArrayList<XulBindingCtx>();
				_rebindViewCtxStack.add(pendingCtx);
			} else {
				pendingCtx = _rebindViewCtxStack.get(_rebindViewCtxStackPointer);
			}
			try {
				for (int i = 0, xulViewLength = viewNum; i < xulViewLength; i++) {
					T xulView = xulViews[i];
					if (xulView instanceof XulArea) {
						collectPendingCtx((XulArea) xulView, pendingCtx);
					} else {
						collectPendingCtx(xulView, pendingCtx);
					}
				}
				_doBinding(actionCallback, pendingCtx);
			} catch (Exception e) {
				e.printStackTrace();
			}
			pendingCtx.clear();
			--_rebindViewCtxStackPointer;
			return true;
		}
	}

	private void asyncInvokeAction(final HashMap<XulView, XulAction> notifiedTargets, final IActionCallback actionCallback) {
		if (notifiedTargets == null || notifiedTargets.isEmpty()) {
			return;
		}
		getRenderContext().scheduleLayoutFinishedTask(new Runnable() {
			@Override
			public void run() {
				for (Map.Entry<XulView, XulAction> entry : notifiedTargets.entrySet()) {
					XulPage.invokeAction(entry.getKey(), entry.getValue(), true, actionCallback, null);
				}
			}
		});
	}

	XulParserData _xulParserData;

	private XulManager _mgr;
	private XulLayout _currentLayout;
	private ArrayList<XulLayout> _layouts = new ArrayList<XulLayout>();
	private ArrayList<XulSelect> _selectors;
	private ArrayList<XulBinding> _bindings = new ArrayList<XulBinding>();
	private ArrayList<XulBinding> _globalBindings;
	private XulRenderContext _renderCtx;
	private HashSet<WeakReference<XulView>> _refComponents;

	// 页面显示尺寸
	private int _pageWidth = XulManager.getPageWidth();
	private int _pageHeight = XulManager.getPageHeight();

	// 页面设计尺寸
	private int _designPageWidth;
	private int _designPageHeight;
	private float _xScalar = 1.0f;
	private float _yScalar = 1.0f;

	private XulSelectorCtx _selectContext;

	private XulBindingManager _bindingMgr;

	// for package use only
	void setGlobalBindings(ArrayList<XulBinding> globalBindings) {
		if (getAttr(XulPropNameCache.TagId.DISABLE_GLOBAL_BINDING) == null) {
			_globalBindings = globalBindings;
		}
	}

	void preloadBinding(XulWorker.IXulWorkItemSource workItemSource) {
		if (_bindingMgr == null) {
			_bindingMgr = new XulBindingManager();
		}
		_bindingMgr.preloadBinding(workItemSource);
	}

	boolean applyBinding(IActionCallback actionCallback) {
		if (_bindingMgr == null) {
			_bindingMgr = new XulBindingManager();
		}
		return _bindingMgr.applyBinding(actionCallback);
	}

	XulWorker.DownloadItem getPendingItem() {
		if (_bindingMgr == null) {
			return null;
		}
		return _bindingMgr.getPendingItem();
	}

	boolean setPendingItemData(XulWorker.DownloadItem item, InputStream data, IActionCallback actionCallback) {
		if (_bindingMgr == null) {
			return false;
		}
		return _bindingMgr.setPendingItemData(item, data, actionCallback);
	}

	boolean applySelectors(XulView xulView, ArrayList<String> clsKeys) {
		if (_selectContext == null || clsKeys == null) {
			return false;
		}
		return _selectContext.applySelectors(xulView, clsKeys);
	}

	boolean unApplySelectors(XulView xulView, ArrayList<String> clsKeys) {
		if (_selectContext == null || clsKeys == null) {
			return false;
		}
		return _selectContext.unApplySelectors(xulView, clsKeys);
	}

	public void addSelectors(XulComponent component) {
		if (component == null) {
			return;
		}
		ArrayList<XulSelect> selectors = component.getSelectors();
		if (selectors == null) {
			return;
		}
		if (_refComponents == null) {
			_refComponents = new HashSet<WeakReference<XulView>>();
		}
		WeakReference<XulView> componentRef = component.getWeakReference();
		if (_refComponents.contains(componentRef)) {
			return;
		}
		_refComponents.add(componentRef);
		for (int i = 0, selectorsSize = selectors.size(); i < selectorsSize; i++) {
			XulSelect selector = selectors.get(i);
			_selectContext.addSelect(selector);
		}
	}

	public void addSelectorTargets(XulArea area) {
		if (_selectContext == null) {
			return;
		}
		_selectContext.addItem(area);
	}

	public void addSelectorTarget(XulItem view) {
		if (_selectContext == null) {
			return;
		}
		_selectContext.addItemByKeys(view, view.getSelectKeys());
	}

	public void updateBindingContext(XulView addedView) {
		if (_bindingMgr == null) {
			return;
		}
		if (addedView instanceof XulArea) {
			_bindingMgr.collectPendingCtx((XulArea) addedView, _bindingMgr._pendingCtx);
		} else {
			_bindingMgr.collectPendingCtx(addedView, _bindingMgr._pendingCtx);
		}
	}

	public void addSelectorTarget(XulView view, ArrayList<String> clsKeys) {
		if (_selectContext == null) {
			return;
		}
		_selectContext.addItemByKeys(view, clsKeys);
	}

	public void removeSelectorTarget(XulView view, ArrayList<String> clsKeys) {
		if (_selectContext == null) {
			return;
		}
		_selectContext.removeItemByKeys(view, clsKeys);
	}

	public void applySelectors(XulArea area) {
		if (_selectContext == null) {
			return;
		}
		_selectContext.applySelectors(area);

	}

	public void applySelectors(XulItem item) {
		if (_selectContext == null) {
			return;
		}
		_selectContext.applySelectors(item);
	}

	void updateSelectorPriorityLevel() {
		if (_selectors == null) {
			return;
		}
		for (int i = 0; i < _selectors.size(); ++i) {
			XulSelect selector = _selectors.get(i);
			selector.setPriorityLevel(i + 1, 0x8000000);
		}
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_NEARBY | XulFocus.MODE_PRIORITY;
	}

	//////////////////////////////////////////////////////////////////////////////////
	// for private use only

	private XulPage(XulRenderContext renderCtx, XulPage orgPage) {
		this(renderCtx, orgPage, orgPage._pageWidth, orgPage._pageHeight);
	}

	public XulPage(XulRenderContext renderCtx, XulPage orgPage, int pageWidth, int pageHeight) {
		_ownerId = orgPage._ownerId;
		_renderCtx = renderCtx;
		_currentLayout = orgPage._currentLayout;
		copyContent(orgPage);
		_mgr = orgPage._mgr;
		_selectors = orgPage._selectors;

		if (pageWidth == 0) {
			pageWidth = orgPage._pageWidth;
		}

		if (pageHeight == 0) {
			pageHeight = orgPage._pageHeight;
		}

		_pageWidth = pageWidth;
		_pageHeight = pageHeight;
		_designPageWidth = orgPage._designPageWidth;
		_designPageHeight = orgPage._designPageHeight;
		if (_designPageWidth != 0) {
			_xScalar = ((float) pageWidth) / _designPageWidth;
		} else {
			_xScalar = 1.0f;
		}
		if (_designPageHeight != 0) {
			_yScalar = ((float) pageHeight) / _designPageHeight;
		} else {
			_yScalar = 1.0f;
		}

		for (XulBinding binding : orgPage._bindings) {
			_bindings.add(binding.makeClone());
		}

		_layouts = new ArrayList<XulLayout>();
		for (XulLayout layout : orgPage._layouts) {
			XulLayout newLayout = layout.makeClone(this);
			if (layout == _currentLayout) {
				_currentLayout = newLayout;
			}
		}
	}


	private XulPage(XulManager mgr, String ownerId) {
		_selectors = new ArrayList<XulSelect>();
		_mgr = mgr;
		_ownerId = ownerId;
	}

	private void initId(String id) {
		_id = id;
		_mgr.addPage(this);
	}

	//////////////////////////////////////////////////////////////////////////////////

	static class _XulFindItemByIdIterator extends XulArea.XulViewIterator {
		XulView _view;
		XulArea _owner;
		String _id;

		public void begin(String id) {
			_id = id;
		}

		public void begin(XulArea owner, String id) {
			_owner = owner;
			_id = id;
		}

		public XulView end() {
			XulView view = _view;
			_view = null;
			_owner = null;
			return view;
		}

		@Override
		public boolean onXulView(int pos, XulView view) {
			if (_owner != null && !_owner.hasChild(view)) {
				return true;
			}
			if (view.getId().equals(_id)) {
				_view = view;
				return false;
			}
			if (view instanceof XulArea) {
				((XulArea) view).eachView(this);
			}
			return true;
		}
	}

	static _XulFindItemByIdIterator _findItemByIdIterator = new _XulFindItemByIdIterator();

	private XulView findItemByIdRecursively(String id) {
		if (_currentLayout == null) {
			return null;
		}
		_findItemByIdIterator.begin(id);
		_currentLayout.eachView(_findItemByIdIterator);
		return _findItemByIdIterator.end();
	}

	private XulView findItemByIdRecursively(XulArea owner, String id) {
		if (_currentLayout == null) {
			return null;
		}
		_findItemByIdIterator.begin(owner, id);
		_currentLayout.eachView(_findItemByIdIterator);
		return _findItemByIdIterator.end();
	}

	public XulView findItemById(String id) {
		if (_selectContext == null) {
			return findItemByIdRecursively(id);
		}
		return _selectContext.findItemById(id);
	}

	public XulView findItemById(XulArea owner, String id) {
		if (_selectContext == null) {
			return findItemByIdRecursively(owner, id);
		}
		return _selectContext.findItemById(owner, id);
	}

	public XulView findCustomItemByExtView(IXulExternalView extView) {
		if (_selectContext == null) {
			return null;
		}
		return _selectContext.findCustomItemByExtView(extView);
	}

	public static XulView findFirstItemByClass(XulArea owner, String cls) {
		XulArrayList<XulView> itemsByClass = findItemsByClass(owner, cls);
		return (itemsByClass == null || itemsByClass.isEmpty()) ? null : itemsByClass.get(0);
	}

	public static XulView findFirstItemByClass(XulArea owner, String... cls) {
		XulArrayList<XulView> itemsByClass = findItemsByClass(owner, cls);
		return (itemsByClass == null || itemsByClass.isEmpty()) ? null : itemsByClass.get(0);
	}

	public XulArrayList<XulView> findItemsByClass(String cls) {
		return findItemsByClass(_currentLayout, cls);
	}

	public XulArrayList<XulView> findItemsByClass(String... cls) {
		return findItemsByClass(_currentLayout, cls);
	}

	public static XulArrayList<XulView> findItemsByClass(XulArea parent, String cls) {
		XulPage ownerPage = parent.getOwnerPage();
		XulSelectorCtx selectContext;
		if (ownerPage == null || (selectContext = ownerPage._selectContext) == null) {
			XulAreaChildrenCollectorByClass childrenCollectorByClass = new XulAreaChildrenCollectorByClass();
			childrenCollectorByClass.begin(false, cls);
			parent.eachChild(childrenCollectorByClass);
			return childrenCollectorByClass.end();
		}
		return selectContext.findItemsByClass(parent, cls);
	}

	public static XulArrayList<XulView> findItemsByClass(XulArea parent, String... cls) {
		if (cls.length == 1) {
			return findItemsByClass(parent, cls[0]);
		}
		XulAreaChildrenCollectorByClass childrenCollectorByClass = new XulAreaChildrenCollectorByClass();
		childrenCollectorByClass.begin(false, cls);
		parent.eachChild(childrenCollectorByClass);
		return childrenCollectorByClass.end();
	}

	public ArrayList<XulDataNode> queryBindingData(String selector) {
		if (_bindingMgr == null) {
			return null;
		}
		ArrayList<XulDataNode> result = new ArrayList<XulDataNode>();
		result = _bindingMgr.selectData(selector, result);
		return result;
	}

	public <T extends XulView> boolean rebindView(T xulView, IActionCallback actionCallback) {
		if (_bindingMgr == null) {
			return false;
		}
		return  _bindingMgr.rebindView(xulView, actionCallback);
	}

	public <T extends XulView> boolean rebindViews(T[] xulView, int viewNum, IActionCallback actionCallback) {
		if (_bindingMgr == null) {
			return false;
		}
		return _bindingMgr.rebindViews(xulView, viewNum, actionCallback);
	}

	public void applySelectors() {
		if (_selectContext == null) {
			_selectContext = new XulSelectorCtx(this);

			{
				if (_selectors != null) {
					for (int i = 0, selectorsSize = _selectors.size(); i < selectorsSize; i++) {
						XulSelect selector = _selectors.get(i);
						_selectContext.addSelect(selector);
					}
				}
			}
			{
				ArrayList<XulSelect> selectors = XulManager.getSelectors();
				if (selectors != null) {
					for (int i = 0, selectorsSize = selectors.size(); i < selectorsSize; i++) {
						XulSelect selector = selectors.get(i);
						_selectContext.addSelect(selector);
					}
				}
			}

			_selectContext.applySelectors();
		}
	}

	public void addSelector(XulSelect select) {
		_selectors.add(select);
	}

	public void addLayout(XulLayout xulLayout) {
		_layouts.add(xulLayout);
		if (_currentLayout == null) {
			_currentLayout = xulLayout;
		}
	}

	public void addBinding(XulBinding binding) {
		_bindings.add(binding);
		if (_bindingMgr != null) {
			_bindingMgr.addBinding(binding);
		}
	}

	public void addBinding(String bindingId) {
		XulBinding binding = XulBinding.createBinding(bindingId);
		addBinding(binding);
	}

	public XulLayout getLayout() {
		return _currentLayout;
	}

	public String getId() {
		return _id;
	}

	public void refreshBinding(String bindingId) {
		if (_bindingMgr == null) {
			return;
		}

		_bindingMgr.refreshBinding(bindingId);
	}

	public void refreshBinding(String bindingId, String url) {
		if (_bindingMgr == null) {
			return;
		}

		_bindingMgr.refreshBinding(bindingId, url);
	}

	public void refreshBinding(String bindingId, XulDataNode dataNode) {
		if (_bindingMgr == null) {
			return;
		}

		_bindingMgr.refreshBinding(bindingId, dataNode);
	}

	@Override
	public void destroy() {
		for (XulLayout layout : _layouts) {
			layout.destroy();
		}
		super.destroy();
	}

	public void doLayout(int offsetX, int offsetY) {
		if (_currentLayout == null) {
			return;
		}
		XulLayoutHelper.updateLayout((XulLayoutHelper.ILayoutContainer) _currentLayout.getRender().getLayoutElement(), offsetX, offsetY, getPageWidth(), getPageWidth());
	}

	@Override
	public void prepareRender(XulRenderContext ctx, boolean preload) {
		if (_currentLayout == null) {
			return;
		}
		_currentLayout.prepareRender(ctx, preload);
	}

	@Override
	public boolean draw(XulDC dc, Rect updateRc, int xBase, int yBase) {
		if (_currentLayout == null) {
			return false;
		}
		return _currentLayout.draw(dc, updateRc, xBase, yBase);
	}

	public void setPageSize(int width, int height) {
		_pageWidth = width;
		_pageHeight = height;

		if (_designPageWidth != 0) {
			_xScalar = ((float) _pageWidth) / _designPageWidth;
		} else {
			_xScalar = 1.0f;
		}
		if (_designPageHeight != 0) {
			_yScalar = ((float) _pageHeight) / _designPageHeight;
		} else {
			_yScalar = 1.0f;
		}
	}

	public int getPageHeight() {
		return _pageHeight;
	}

	public int getPageWidth() {
		return _pageWidth;
	}

	public void setDesignPageSize(int designPageWidth, int designPageHeight) {
		_designPageWidth = designPageWidth;
		_designPageHeight = designPageHeight;

		if (_designPageWidth != 0) {
			_xScalar = ((float) _pageWidth) / _designPageWidth;
		} else {
			_xScalar = 1.0f;
		}
		if (_designPageHeight != 0) {
			_yScalar = ((float) _pageHeight) / _designPageHeight;
		} else {
			_yScalar = 1.0f;
		}
	}

	private static class _PageState {
		private static class _ViewState {
			XulView _view;
			boolean _oldIsFocused = false;
			Boolean _oldIsEnabled;
			ArrayList<String> _removedClass;
			ArrayList<String> _addedClass;
			ArrayList<String> _blinkClass;
			ArrayList<Pair<String, String>> _oldAttrs;
			ArrayList<Pair<String, String>> _oldStyles;

			void removeClass(String cls) {
				if (!_view.hasClass(cls)) {
					return;
				}
				if (_removedClass == null) {
					_removedClass = new ArrayList<String>();
				}
				_removedClass.add(cls);
				_view.removeClass(cls);
			}

			void addClass(String cls) {
				if (_view.hasClass(cls)) {
					return;
				}
				if (_addedClass == null) {
					_addedClass = new ArrayList<String>();
				}
				_addedClass.add(cls);
				_view.addClass(cls);
			}

			void blinkClass(String cls) {
				if (_blinkClass == null) {
					_blinkClass = new ArrayList<String>();
				}
				_blinkClass.add(cls);
				final XulViewRender render = _view.getRender();
				if (render != null) {
					render.blinkClass(cls);
				}
			}

			void setAttr(String key, String val) {
				XulAttr attr = _view.getAttr(key);
				String oldVal = attr == null ? null : attr.getStringValue();

				if (val == null && oldVal == null) {
					return;
				}

				if (val != null && val.equals(oldVal)) {
					return;
				}

				if (_oldAttrs == null) {
					_oldAttrs = new ArrayList<Pair<String, String>>();
				}

				_oldAttrs.add(Pair.create(key, oldVal));
				_view.setAttr(key, val);
			}

			void setStyle(String key, String val) {
				XulStyle style = _view.getStyle(key);
				String oldVal = style == null ? null : style.getStringValue();

				if (val == null && oldVal == null) {
					return;
				}

				if (val != null && val.equals(oldVal)) {
					return;
				}

				if (_oldStyles == null) {
					_oldStyles = new ArrayList<Pair<String, String>>();
				}

				_oldStyles.add(Pair.create(key, oldVal));
				_view.setStyle(key, val);
			}

			void setEnabled(boolean enable) {
				if (_oldIsEnabled == null) {
					_oldIsEnabled = Boolean.valueOf(_view.isEnabled());
				}
				_view.setEnabled(enable);
			}

		}

		ArrayList<ArrayList<_ViewState>> _savedViews = new ArrayList<ArrayList<_ViewState>>();

		boolean pop(boolean discard) {
			if (_savedViews.isEmpty()) {
				return false;
			}
			ArrayList<_ViewState> savedViews = _savedViews.remove(_savedViews.size() - 1);

			if (discard) {
				return true;
			}

			XulView focusView = null;
			for (int i = 0; i < savedViews.size(); i++) {
				_ViewState savedView = savedViews.get(i);
				XulView view = savedView._view;
				if (savedView._oldIsFocused) {
					focusView = view;
				}

				if (savedView._oldIsEnabled != null) {
					view.setEnabled(savedView._oldIsEnabled);
				}

				ArrayList<Pair<String, String>> oldAttrs = savedView._oldAttrs;
				for (int idx = 0; oldAttrs != null && idx < oldAttrs.size(); idx++) {
					Pair<String, String> oldAttr = oldAttrs.get(idx);
					if (TextUtils.isEmpty(oldAttr.second)) {
						view.setAttr(oldAttr.first, null);
					} else {
						view.setAttr(oldAttr.first, oldAttr.second);
					}
				}

				ArrayList<Pair<String, String>> oldStyles = savedView._oldStyles;
				for (int idx = 0; oldStyles != null && idx < oldStyles.size(); idx++) {
					Pair<String, String> oldStyle = oldStyles.get(idx);
					if (TextUtils.isEmpty(oldStyle.second)) {
						view.setStyle(oldStyle.first, null);
					} else {
						view.setStyle(oldStyle.first, oldStyle.second);
					}
				}

				ArrayList<String> addedClasses = savedView._addedClass;
				for (int idx = 0; addedClasses != null && idx < addedClasses.size(); idx++) {
					String addedClass = addedClasses.get(idx);
					view.removeClass(addedClass);
				}

				ArrayList<String> removedClasses = savedView._removedClass;
				for (int idx = 0; removedClasses != null && idx < removedClasses.size(); idx++) {
					String removedClass = removedClasses.get(idx);
					view.addClass(removedClass);
				}

				ArrayList<String> blinkClasses = savedView._blinkClass;
				for (int idx = 0; blinkClasses != null && idx < blinkClasses.size(); idx++) {
					String blinkClass = blinkClasses.get(idx);
					final XulViewRender render = view.getRender();
					if (render != null) {
						render.blinkClass(blinkClass);
					}
				}

				view.resetRender();
			}

			if (focusView != null) {
				XulLayout rootLayout = focusView.getRootLayout();
				if (rootLayout != null) {
					rootLayout.requestFocus(focusView);
				}
			}
			return true;
		}

		void push(Object[] args) {
			ArrayList<_ViewState> savedViews = new ArrayList<_ViewState>();
			_ViewState lastViewState = null;
			_ViewState newFocusState = null;
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				if (arg instanceof XulView) {
					lastViewState = new _ViewState();
					lastViewState._view = (XulView) arg;
					savedViews.add(lastViewState);
				}

				if (lastViewState == null) {
					continue;
				}
				String cmd = String.valueOf(arg);
				if ("addClass".equals(cmd)) {
					++i;
					if (i >= args.length || args[i] instanceof XulView) {
						--i;
						continue;
					}
					String className = String.valueOf(args[i]);
					lastViewState.addClass(className);
				} else if ("removeClass".equals(cmd)) {
					++i;
					if (i >= args.length || args[i] instanceof XulView) {
						--i;
						continue;
					}
					String className = String.valueOf(args[i]);
					lastViewState.removeClass(className);
				} else if ("blinkClass".equals(cmd)) {
					++i;
					if (i >= args.length || args[i] instanceof XulView) {
						--i;
						continue;
					}
					String className = String.valueOf(args[i]);
					lastViewState.blinkClass(className);
				} else if ("setStyle".equals(cmd)) {
					++i;
					if (i >= args.length || args[i] instanceof XulView) {
						--i;
						continue;
					}
					String styleName = String.valueOf(args[i]);

					++i;
					if (i >= args.length || args[i] instanceof XulView) {
						--i;
						continue;
					}
					String styleVal = String.valueOf(args[i]);
					lastViewState.setStyle(styleName, styleVal);
				} else if ("removeStyle".equals(cmd)) {
					++i;
					if (i >= args.length || args[i] instanceof XulView) {
						--i;
						continue;
					}
					String styleName = String.valueOf(args[i]);
					lastViewState.setStyle(styleName, null);
				} else if ("setAttr".equals(cmd)) {
					++i;
					if (i >= args.length || args[i] instanceof XulView) {
						--i;
						continue;
					}
					String attrName = String.valueOf(args[i]);
					++i;
					if (i >= args.length || args[i] instanceof XulView) {
						--i;
						continue;
					}
					String attrVal = String.valueOf(args[i]);

					lastViewState.setAttr(attrName, attrVal);
				} else if ("removeAttr".equals(cmd)) {
					++i;
					if (i >= args.length || args[i] instanceof XulView) {
						--i;
						continue;
					}
					String attrName = String.valueOf(args[i]);
					lastViewState.setAttr(attrName, null);
				} else if ("setEnabled".equals(cmd)) {
					lastViewState.setEnabled(true);
				} else if ("setDisabled".equals(cmd)) {
					lastViewState.setEnabled(false);
				} else if ("requestFocus".equals(cmd)) {
					newFocusState = lastViewState;
				} else if ("killFocus".equals(cmd)) {
					newFocusState = new _ViewState();
					newFocusState._view = null;
				}
			}

			XulView newFocusedView = null;

			while (newFocusState != null) {
				XulView view = newFocusState._view;
				XulLayout rootLayout = lastViewState._view.getRootLayout();
				if (rootLayout == null) {
					break;
				}
				XulView oldFocus = rootLayout.getFocus();
				newFocusedView = view;
				if (oldFocus == null) {
					break;
				}

				if (newFocusedView == null) {
					rootLayout.requestFocus(null);
				}

				if (oldFocus != view) {
					lastViewState = new _ViewState();
					lastViewState._view = oldFocus;
					savedViews.add(lastViewState);
				} else {
					lastViewState = newFocusState;
				}
				lastViewState._oldIsFocused = true;
				break;
			}
			if (!savedViews.isEmpty()) {
				for (int i = 0; i < savedViews.size(); i++) {
					_ViewState savedView = savedViews.get(i);
					savedView._view.resetRender();
				}
				_savedViews.add(savedViews);

				if (newFocusedView != null) {
					newFocusedView.getOwnerPage().doLayout(0, 0);
					newFocusedView.getRootLayout().requestFocus(newFocusedView);
				}
			}
		}
	}

	private _PageState _savedPageStates = new _PageState();

	public boolean pushStates(Object[] args) {
		_savedPageStates.push(args);
		return true;
	}

	public boolean popStates() {
		return _savedPageStates.pop(false);
	}

	public boolean popStates(boolean discard) {
		return _savedPageStates.pop(discard);
	}

	public boolean popAllStates(boolean discard) {
		while (_savedPageStates.pop(discard)) {
		}
		return true;
	}

	public float getXScalar() {
		return _xScalar;
	}

	public float getYScalar() {
		return _yScalar;
	}

	public int getDesignPageWidth() {
		return _designPageWidth;
	}

	public int getDesignPageHeight() {
		return _designPageHeight;
	}

	private void _initPage() {
		if (_xulParserData != null) {
			XulUtils.ticketMarker marker = new XulUtils.ticketMarker("BENCH!! ", true);
			marker.mark();
			_xulParserData.buildItem(new _PageBuilder(this));
			_xulParserData = null;
			this.updateSelectorPriorityLevel();
			marker.mark("_initPage");
			Log.d(TAG, marker.toString());
		}
	}

	public XulPage makeClone(XulRenderContext renderCtx, int pageWidth, int pageHeight) {
		_initPage();
		return new XulPage(renderCtx, this, pageWidth, pageHeight);
	}

	public XulPage makeClone(XulRenderContext renderCtx) {
		_initPage();
		return new XulPage(renderCtx, this);
	}

	public void initPage() {
		_initPage();
	}

	public XulPage getOwnerPage() {
		return this;
	}

	public String getOwnerId() {
		return _ownerId;
	}

	public XulRenderContext getRenderContext() {
		return _renderCtx;
	}

	static class _PageBuilder extends ItemBuilder {
		XulPage _page;

		public _PageBuilder(XulPage page) {
			_page = page;
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_page._id = attrs.getValue("id");
			return super.initialize(name, attrs);
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			if (name.equals("layout")) {
				XulLayout._Builder builder = XulLayout._Builder.create(_page);
				builder.initialize(name, attrs);
				return builder;
			}

			if (name.equals("attr")) {
				XulAttr._Builder builder = XulAttr._Builder.create(_page);
				builder.initialize(name, attrs);
				return builder;
			}

			if (name.equals("action")) {
				XulAction._Builder builder = XulAction._Builder.create(_page);
				builder.initialize(name, attrs);
				return builder;
			}

			if (name.equals("script")) {
				ItemBuilder itemBuilder = XulManager.getScriptBuilder();
				itemBuilder.initialize(name, attrs);
				return itemBuilder;
			}

			if (name.equals("selector")) {
				return new ItemBuilder() {
					@Override
					public boolean initialize(String name, Attributes attrs) {
						return super.initialize(name, attrs);
					}

					@Override
					public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
						if (name.equals("select")) {
							XulSelect._Builder builder = XulSelect._Builder.create(_page);
							builder.initialize(name, attrs);
							return builder;
						}

						return XulManager.CommonDummyBuilder;
					}

					@Override
					public boolean pushText(String path, XulFactory.IPullParser parser) {
						return super.pushText(path, parser);
					}

					@Override
					public Object finalItem() {
						return super.finalItem();
					}
				};
			}

			if ("binding".equals(name)) {
				XulBinding._Builder builder = XulBinding._Builder.create(_page);
				builder.initialize(name, attrs);
				return builder;
			}
			return XulManager.CommonDummyBuilder;
		}

		@Override
		public Object finalItem() {
			return _page;
		}
	}

	static class _Builder extends ItemBuilder {

		XulPage _page;
		XulParserData _xulParserData;

		public _Builder(XulFactory.ResultBuilderContext ctx, XulManager mgr, int pageDesignWidth, int pageDesignHeight) {
			_page = new XulPage(mgr, ctx.getName());
			_page.setDesignPageSize(pageDesignWidth, pageDesignHeight);
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_page.initId(attrs.getValue("id"));
			String screen = attrs.getValue("screen");
			if (!TextUtils.isEmpty(screen)) {
				Pattern screenPat = Pattern.compile("(\\d+)x(\\d+)");
				Matcher matcher = screenPat.matcher(screen);
				if (matcher.matches()) {
					_page.setDesignPageSize(XulUtils.tryParseInt(matcher.group(1)), XulUtils.tryParseInt(matcher.group(2)));
				}
			}
			return super.initialize(name, attrs);
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			if (parser != null) {
				Object storePos = parser.storeParserPos();
				if (storePos != null) {
					_page._xulParserData = _xulParserData = new XulParserDataStoreSupported(parser, storePos);
					return null;
				}
			}
			XulParserDataNoStoreSupported parserData;
			if (_xulParserData == null) {
				parserData = new XulParserDataNoStoreSupported();
				_page._xulParserData = _xulParserData = parserData;
			} else {
				parserData = (XulParserDataNoStoreSupported) _xulParserData;
			}
			return parserData.pushSubItem(path, name, attrs);
		}

		@Override
		public Object finalItem() {
			return _page;
		}
	}

	@Override
	protected XulScriptableObject createScriptableObject() {
		return new XulPageScriptableObject(this);
	}

	public interface IActionCallback {
		void doAction(XulView view, String action, String type, String command, Object userdata);
	}

	private static XulActionEvent _actionEvent = new XulActionEvent();

	private static XulUtils.ticketMarker _TM;

	static {
		if (XulManager.PERFORMANCE_BENCH) {
			_TM = new XulUtils.ticketMarker("BENCH!!! invokeAction ", false);
		}
	}

	private static void _benchMark(String mark) {
		if (!XulManager.PERFORMANCE_BENCH) {
			return;
		}
		_TM.mark(mark);
	}

	private static void _benchLog() {
		if (!XulManager.PERFORMANCE_BENCH) {
			return;
		}
		Log.d(TAG, _TM.toString());
		_TM.reset();
	}

	public static boolean invokeActionNoPopup(XulView view, String action) {
		XulPage ownerPage = view.getOwnerPage();
		if (ownerPage._renderCtx != null) {
			return invokeActionNoPopup(view, action, ownerPage._renderCtx.getDefaultActionCallback());
		}
		return invokeActionNoPopup(view, action, null);
	}

	public static boolean invokeActionNoPopupWithArgs(XulView view, String action, Object... args) {
		XulPage ownerPage = view.getOwnerPage();
		if (ownerPage._renderCtx != null) {
			return invokeAction(view, action, true, ownerPage._renderCtx.getDefaultActionCallback(), args);
		}
		return invokeAction(view, action, true, null, args);
	}

	public static boolean invokeActionNoPopupWithArgs(XulView view, XulAction action, Object... args) {
		XulPage ownerPage = view.getOwnerPage();
		if (ownerPage._renderCtx != null) {
			return invokeAction(view, action, true, ownerPage._renderCtx.getDefaultActionCallback(), args);
		}
		return invokeAction(view, action, true, null, args);
	}

	public static boolean invokeAction(XulView view, String action) {
		XulPage ownerPage = view.getOwnerPage();
		if (ownerPage._renderCtx != null) {
			return invokeAction(view, action, ownerPage._renderCtx.getDefaultActionCallback());
		}
		return invokeAction(view, action, null);
	}

	public static boolean invokeActionWithArgs(XulView view, String action, Object[] args) {
		XulPage ownerPage = view.getOwnerPage();
		if (ownerPage._renderCtx != null) {
			return invokeActionWithArgs(view, action, ownerPage._renderCtx.getDefaultActionCallback(), args);
		}
		return invokeActionWithArgs(view, action, null, args);
	}

	public static boolean invokeActionNoPopup(XulView view, XulAction action) {
		XulPage ownerPage = view.getOwnerPage();
		if (ownerPage._renderCtx != null) {
			return invokeAction(view, action, ownerPage._renderCtx.getDefaultActionCallback(), null);
		}
		return invokeAction(view, action, null, null);
	}

	public static boolean invokeActionNoPopup(XulView view, String action, IActionCallback actionCallback) {
		return invokeAction(view, action, true, actionCallback, null);
	}

	public static boolean invokeActionWithArgs(XulView view, String action, IActionCallback actionCallback, Object[] args) {
		return invokeAction(view, action, false, actionCallback, args);
	}

	public static boolean invokeAction(XulView view, String action, IActionCallback actionCallback) {
		return invokeAction(view, action, false, actionCallback, null);
	}

	public static boolean invokeAction(XulView view, String action, boolean noPopup, IActionCallback actionCallback, Object[] args) {
		if (!noPopup) {
			popupEvent(view, action);
		}
		return invokeAction(view, action, actionCallback, args);
	}

	public static boolean invokeAction(XulView view, XulAction action, boolean noPopup, IActionCallback actionCallback, Object[] args) {
		if (!noPopup) {
			popupEvent(view, action.getName());
		}
		return invokeAction(view, action, actionCallback, args);
	}

	private static void popupEvent(XulView view, String action) {
		long tm;
		if (XulManager.PERFORMANCE_BENCH) {
			tm = XulUtils.timestamp_us();
		}

		_actionEvent.action = action;
		_actionEvent.eventSource = view;
		_actionEvent.notifySource = view;
		_actionEvent.noPopup = false;

		XulArea viewParent = view.getParent();
		while (viewParent != null) {
			if (_actionEvent.noPopup) {
				break;
			}
			viewParent.onChildDoActionEvent(_actionEvent);
			_actionEvent.notifySource = viewParent;
			viewParent = viewParent.getParent();
		}

		_actionEvent.eventSource = null;
		_actionEvent.notifySource = null;

		if (XulManager.PERFORMANCE_BENCH) {
			tm = XulUtils.timestamp_us() - tm;
			Log.d(TAG, String.format("BENCH!!! popup event %d\n", tm));
		}
	}

	private static boolean invokeAction(XulView view, String action, IActionCallback actionCallback, Object[] args) {
		XulAction act = view.getAction(action);
		if (act == null) {
			return false;
		}
		return invokeAction(view, act, actionCallback, args);
	}

	private static boolean invokeAction(XulView view, XulAction action, IActionCallback actionCallback, Object[] args) {
		_benchMark("");
		String type = action.getType();
		String command = action.getStringValue();
		XulData userdata = view.getData("userdata");
		if (XulManager.DEBUG) {
			Log.d(TAG, "invokeAction " + action.getName() + " type:" + type + " cmd:" + command);
			if (userdata == null) {
				Log.d(TAG, "userdata is null!!!");
			} else {
				Log.d(TAG, "userdata " + userdata.getStringValue());
			}
		}

		_benchMark("prepareInvoke");
		IScript script = action.getScript();
		_benchMark("getScript");
		if (script != null) {
			String scriptType = script.getScriptType();
			if (args != null) {
				for (int i = 0, argsLength = args.length; i < argsLength; i++) {
					Object arg = args[i];
					if (arg instanceof XulView) {
						args[i] = ((XulView) arg).getScriptableObject(scriptType);
					}
				}
			}
			script.run(XulManager.getScriptContext(scriptType), view.getScriptableObject(scriptType), args);
			_benchMark("execScript");
			_benchLog();
			return true;
		}

		dispatchAction(view, action.getName(), type, command, userdata, actionCallback);
		_benchMark("dispAct");
		_benchLog();
		return true;
	}

	public boolean dispatchAction(XulView view, String action, String type, String command) {
		XulData userdata = view.getData("userdata");
		return dispatchAction(view, action, type, command, userdata);
	}

	public boolean dispatchAction(XulView view, String action, String type, String command, XulData userdata) {
		dispatchAction(view, action, type, command, userdata, _renderCtx.getDefaultActionCallback());
		return true;
	}

	public static boolean dispatchAction(XulView view, String action, String type, String command, XulData userdata, IActionCallback actionCallback) {
		if (actionCallback == null) {
			return false;
		}
		actionCallback.doAction(view, action, type, command, userdata == null ? null : userdata.getValue());
		return true;
	}

	@Override
	public boolean hasFocus() {
		return true;
	}
}
