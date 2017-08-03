package com.starcor.xulapp.utils;

import com.starcor.xul.Wrapper.XulMassiveAreaWrapper;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulDataNode;
import com.starcor.xul.XulLayout;
import com.starcor.xul.XulView;
import com.starcor.xulapp.XulApplication;
import com.starcor.xulapp.behavior.XulUiBehavior;
import com.starcor.xulapp.model.XulDataCallback;
import com.starcor.xulapp.model.XulDataService;
import com.starcor.xulapp.model.XulPullDataCollection;

import java.util.Iterator;

/**
 * Created by hy on 2016/9/19.
 */
public class XulMassiveHelper {

	/**
	 * synchronize data nodes with massive container automatically
	 *
	 * @param behavior     ui behavior
	 * @param helper       data helper for retrieving each data node from result sets
	 * @param massiveView  massive container which will be synchronized automatically
	 * @param pullDS       pull data collection object returned by XulDataService
	 * @param data         initial result set returned by XulDataService.pull function
	 * @param prefetchSize forward prefetch item count
	 * @return a cancelable object for cancel the synchronization
	 */

	public static XulCancelable syncContent(final XulUiBehavior behavior, XulDataNodeHelper helper, XulView massiveView, XulPullDataCollection pullDS, XulDataNode data, int prefetchSize) {
		if (massiveView == null || pullDS == null || data == null) {
			return null;
		}
		class MassiveSyncHelper extends XulDataCallback implements XulCancelable, Runnable, XulUiBehavior.XulActionFilter {
			private final XulView _massiveView;
			private final XulMassiveAreaWrapper _massiveAreaWrapper;
			private boolean _cancelFlag = false;
			private XulDataNodeHelper _dataHelper;
			private XulPullDataCollection _dataSource;
			private int _prefetchSize;

			public MassiveSyncHelper(XulView view, XulPullDataCollection pullDS, XulDataNode data, XulDataNodeHelper helper, int prefetchSize) {
				_dataHelper = helper;
				_massiveView = view;
				_dataSource = pullDS;
				_prefetchSize = prefetchSize;
				_massiveAreaWrapper = XulMassiveAreaWrapper.fromXulView(_massiveView);
				behavior.xulAddActionFilter(this);
				doSyncData(data);
			}

			@Override
			public void cancel() {
				if (_cancelFlag) {
					return;
				}
				_cancelFlag = true;
				_dataSource.cancel();
				behavior.xulDelActionFilter(this);
			}

			@Override
			public void onResult(XulDataService.Clause clause, int code, XulDataNode data) {
				doSyncData(data);
			}

			private void doSyncData(XulDataNode data) {
				if (_cancelFlag) {
					return;
				}
				if (_dataHelper != null) {
					data = _dataHelper.getData(data);
				}
				appendData(data, _massiveAreaWrapper);

				int itemIdx = 0;
				XulLayout rootLayout = _massiveView.getRootLayout();
				if (rootLayout != null) {
					XulView focus = rootLayout.getFocus();
					while (focus != null) {
						if (focus.getParent() == _massiveView) {
							itemIdx = _massiveAreaWrapper.getItemIdx(focus);
							break;
						}
						focus = focus.getParent();
					}
				}
				if (_massiveAreaWrapper.itemNum() - itemIdx < _prefetchSize) {
					XulApplication.getAppInstance().postDelayToMainLooper(MassiveSyncHelper.this, 300);
				}
			}

			@Override
			public boolean accept(XulView view, String action, String type, String command, Object userdata) {
				if (_cancelFlag) {
					return false;
				}
				if (!"focus".equals(action)) {
					return false;
				}
				if (!view.isChildOf(_massiveView)) {
					return false;
				}
				XulView directChild = view;
				XulArea parent = view.getParent();
				while (parent != _massiveView) {
					directChild = parent;
					parent = parent.getParent();
				}
				int itemIdx = _massiveAreaWrapper.getItemIdx(directChild);
				if (itemIdx + _prefetchSize >= _massiveAreaWrapper.itemNum()) {
					run();
				}
				return false;
			}

			@Override
			public void run() {
				if (!_dataSource.pull(this)) {
					cancel();
				}
			}
		}

		return new MassiveSyncHelper(massiveView, pullDS, data, helper, prefetchSize);
	}

	/**
	 * synchronize data nodes with massive container automatically
	 * this function will add placeholders into massive container and synchronize the specified page(s) of content
	 *
	 * @param behavior     ui behavior
	 * @param helper       data helper for retrieving each data node from result sets, implement user specified features
	 * @param massiveView  massive container which will be synchronized automatically
	 * @param pullDS       pull data collection object returned by XulDataService
	 * @param data         initial result set returned by XulDataService.pull function
	 * @param prefetchSize forward/backward prefetch item count
	 * @return a cancelable object for cancel the synchronization
	 */
	public static XulCancelable syncContentEx(final XulUiBehavior behavior, final XulDataNodeHelperEx helper, XulView massiveView, XulPullDataCollection pullDS, XulDataNode data, int prefetchSize) {

		if (massiveView == null || pullDS == null || data == null || helper == null) {
			return null;
		}
		class MassiveSyncHelper extends XulDataCallback implements XulCancelable, Runnable, XulUiBehavior.XulActionFilter {
			private final XulView _massiveView;
			private final XulMassiveAreaWrapper _massiveAreaWrapper;
			private boolean _cancelFlag = false;
			private XulDataNodeHelperEx _dataHelper;
			private XulPullDataCollection _dataSource;
			private int _totalItemNum;
			private int _prefetchSize;
			private boolean _running = false;

			public MassiveSyncHelper(XulView view, XulPullDataCollection pullDS, XulDataNode data, XulDataNodeHelperEx helper, int prefetchSize) {
				_dataHelper = helper;
				_massiveView = view;
				_dataSource = pullDS;
				_prefetchSize = prefetchSize;
				_totalItemNum = helper.getDataCollectionSize(pullDS, data);
				_massiveAreaWrapper = XulMassiveAreaWrapper.fromXulView(_massiveView);
				behavior.xulAddActionFilter(this);

				data = helper.getData(data);
				appendData(data, _massiveAreaWrapper);
				for (int i = _massiveAreaWrapper.itemNum(); i < _totalItemNum; i++) {
					XulDataNode dataNode = helper.getPlaceholder(i);
					_massiveAreaWrapper.addItem(dataNode);
				}
				doSyncData(0, null);
			}

			@Override
			public void cancel() {
				if (_cancelFlag) {
					return;
				}
				_cancelFlag = true;
				_dataSource.cancel();
				behavior.xulDelActionFilter(this);
				_running = false;
			}

			@Override
			public void onResult(XulDataService.Clause clause, int code, XulDataNode data) {
				_running = false;
				int pageIdx = _dataSource.currentPage() - 1;
				doSyncData(pageIdx * _dataSource.pageSize(), data);
			}

			private void doSyncData(int pos, XulDataNode data) {
				if (_cancelFlag) {
					return;
				}

				if (data != null) {
					data = _dataHelper.getData(data);
					updateData(data, pos, _massiveAreaWrapper);
				}

				int focusedItemIdx = 0;
				XulLayout rootLayout = _massiveView.getRootLayout();
				if (rootLayout != null) {
					XulView focus = rootLayout.getFocus();
					while (focus != null) {
						if (focus.getParent() == _massiveView) {
							focusedItemIdx = _massiveAreaWrapper.getItemIdx(focus);
							break;
						}
						focus = focus.getParent();
					}
				}

				trySyncData(focusedItemIdx, 300);
			}

			private void trySyncData(int focusedItemIdx, int delay) {
				int pageSize = _dataSource.pageSize();
				int curPage = focusedItemIdx / pageSize;
				int prefetchPageNum = (_prefetchSize + pageSize - 1) / pageSize;
				for (int pageOffset = 0; pageOffset < prefetchPageNum; pageOffset++) {
					int fwItemIdx = (curPage + pageOffset) * pageSize;
					int bwItemIdx = (curPage - pageOffset - 1) * pageSize;
					XulDataNode dataNode = _massiveAreaWrapper.getItem(fwItemIdx);
					if (_dataHelper.isPlaceHolder(fwItemIdx, dataNode)) {
						_dataSource.reset(curPage + pageOffset);
						_running = true;
						XulApplication.getAppInstance().postDelayToMainLooper(MassiveSyncHelper.this, delay);
						break;
					}

					if (bwItemIdx >= 0) {
						dataNode = _massiveAreaWrapper.getItem(bwItemIdx);
						if (_dataHelper.isPlaceHolder(fwItemIdx, dataNode)) {
							_dataSource.reset(curPage - pageOffset - 1);
							_running = true;
							XulApplication.getAppInstance().postDelayToMainLooper(MassiveSyncHelper.this, delay);
							break;
						}
					}
				}
			}

			@Override
			public boolean accept(XulView view, String action, String type, String command, Object userdata) {
				if (_cancelFlag || _running) {
					return false;
				}
				if (!"focus".equals(action)) {
					return false;
				}
				if (!view.isChildOf(_massiveView)) {
					return false;
				}
				XulView directChild = view;
				XulArea parent = view.getParent();
				while (parent != _massiveView) {
					directChild = parent;
					parent = parent.getParent();
				}
				int itemIdx = _massiveAreaWrapper.getItemIdx(directChild);

				trySyncData(itemIdx, 0);
				return false;
			}

			@Override
			public void run() {
				if (!_dataSource.pull(this)) {
					cancel();
				}
			}
		}

		return new MassiveSyncHelper(massiveView, pullDS, data, helper, prefetchSize);

	}

	public static void appendData(XulDataNode data, XulMassiveAreaWrapper massiveAreaWrapper) {
		if (data == null) {
			return;
		}
		XulDataNode dataNode = data.getFirstChild();
		while (dataNode != null) {
			massiveAreaWrapper.addItem(dataNode);
			dataNode = dataNode.getNext();
		}
		massiveAreaWrapper.syncContentView();
	}

	public static void updateData(XulDataNode data, int index, XulMassiveAreaWrapper massiveAreaWrapper) {
		updateData(data, 0, index, massiveAreaWrapper);
	}

	public static void updateData(final XulDataNode data, final int skipData, int index, XulMassiveAreaWrapper massiveAreaWrapper) {
		if (data == null) {
			return;
		}
		massiveAreaWrapper.updateItems(index, new Iterable<XulDataNode>() {
			@Override
			public Iterator<XulDataNode> iterator() {
				return new Iterator<XulDataNode>() {
					int _skipData = skipData;
					XulDataNode _dataNode = data.getFirstChild();

					@Override
					public boolean hasNext() {
						_doSkip();
						return _dataNode != null;
					}

					private void _doSkip() {
						while (_skipData > 0 && _dataNode != null) {
							--_skipData;
							_dataNode = _dataNode.getNext();
						}
						_skipData = 0;
					}

					@Override
					public XulDataNode next() {
						_doSkip();
						XulDataNode dataNode = _dataNode;
						if (_dataNode != null) {
							_dataNode = _dataNode.getNext();
						}
						return dataNode;
					}

					@Override
					public void remove() {

					}
				};
			}
		});
	}

	public interface XulDataNodeHelper {
		XulDataNode getData(XulDataNode data);
	}

	public interface XulDataNodeHelperEx extends XulDataNodeHelper {
		int getDataCollectionSize(XulPullDataCollection pullDS, XulDataNode data);

		XulDataNode getPlaceholder(int idx);

		boolean isPlaceHolder(int idx, XulDataNode dataNode);
	}
}
