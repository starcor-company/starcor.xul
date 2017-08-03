package com.starcor.xul;

import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Utils.XulCircleQueue;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Queue;

/**
 * Created by hy on 2015/6/1.
 */
public class XulTaskCollector {
	private WeakReference<XulView> _root;
	private Queue<WeakReference<XulView>> _stack = new XulCircleQueue<WeakReference<XulView>>();
	private volatile WeakReference<XulView> _targetView;
	private volatile boolean _finished = false;


	public void init(XulView root) {
		WeakReference<XulView> newRoot = root.getWeakReference();
		if (newRoot.equals(_root)) {
			return;
		}
		_root = newRoot;
		_stack.clear();
		_targetView = null;
		_finished = false;
	}

	boolean isFinished() {
		return _finished;
	}

	public synchronized void reset() {
		_stack.clear();
		_targetView = null;
		_finished = false;
	}

	synchronized XulWorker.DrawableItem collectPendingDrawableItem() {
		final XulView rootView = _root.get();
		if (rootView == null) {
			return null;
		}
		long beginTime = XulUtils.timestamp_us();
		if (_stack.isEmpty()) {
			_stack.add(_root);
			_finished = false;
		}
		int counter = 0;
		final int checkCounterLimit = 32;
		final int checkDurationLimit = 400;
		while (true) {
			++counter;
			if (counter % checkCounterLimit == 0 && XulUtils.timestamp_us() - beginTime >= checkDurationLimit) {
				break;
			}
			XulViewRender viewRender;
			WeakReference<XulView> firstView;
			if (_targetView != null) {
				firstView = _targetView;
				_targetView = null;

				final XulView view = firstView.get();
				if (view == null) {
					continue;
				}
				viewRender = view._render;
				if (viewRender == null || viewRender.rejectTest()) {
					continue;
				}
			} else {
				firstView = _stack.poll();
				if (firstView == null) {
					// finished
					if (XulManager.PERFORMANCE_BENCH) {
						// Log.d("BENCH!!!", "collect finished!!!");
					}
					_finished = true;
					break;
				}
				final XulView view = firstView.get();
				if (view == null) {
					continue;
				}
				viewRender = view._render;
				if (viewRender == null || viewRender.rejectTest()) {
					continue;
				}
				if (viewRender.collectPendingItems(this)) {
					// customized collecting behavior
				} else if (view instanceof XulArea) {
					XulArea area = (XulArea) view;
					final XulArea.XulElementArray children = area._children;
					final int childrenSize = children.size();
					final XulElement[] array = children.getArray();
					for (int i = 0; i < childrenSize; i++) {
						XulElement child = array[i];
						if (child instanceof XulArea) {
							_stack.add(((XulView) child).getWeakReference());
						} else if (child instanceof XulView) {
							_stack.add(((XulView) child).getWeakReference());
						}
					}
				}
			}

			final XulWorker.DrawableItem pendingImageItem = viewRender.getPendingImageItem();
			if (pendingImageItem == null) {
				continue;
			}
			_targetView = firstView;
			return pendingImageItem;
		}
		if (XulManager.PERFORMANCE_BENCH) {
			// Log.d("BENCH!!!", String.format("collect nothing %d", XulUtils.timestamp_us() - beginTime));
		}
		return null;
	}

	public void addPendingItem(XulView view) {
		_stack.add(view.getWeakReference());
	}

	public XulVolatileReference addVolatilePendingItem(XulView view) {
		XulVolatileReference ref = new XulVolatileReference(view);
		_stack.add(ref);
		return ref;
	}

	public static class XulVolatileReference<T> extends WeakReference<T> {

		public XulVolatileReference(T r) {
			super(r);
		}

		public XulVolatileReference(T r, ReferenceQueue<? super T> q) {
			super(r, q);
		}

		public void invalidate() {
			this.clear();
		}
	}
}
