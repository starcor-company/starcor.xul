package com.starcor.xul;

import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Utils.XulCircleQueue;

import java.lang.ref.WeakReference;
import java.util.Queue;

/**
 * Created by hy on 2015/6/1.
 */
public class XulSuspendTaskCollector {
	private WeakReference<XulView> _root;
	private Queue<WeakReference<XulView>> _stack = new XulCircleQueue<WeakReference<XulView>>();
	private WeakReference<XulView> _targetView;
	private boolean _finished = false;
	private int _recycleLevel = 0;


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

	public void reset() {
		_finished = false;
	}

	public void doSuspendWork() {
		if (_finished) {
			return;
		}

		final XulView rootView = _root.get();
		if (rootView == null) {
			return;
		}
		long beginTime = XulUtils.timestamp_us();
		if (_stack.isEmpty()) {
			_stack.add(_root);
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
				if (viewRender == null) {
					continue;
				}
			} else {
				firstView = _stack.poll();
				if (firstView == null) {
					// finished
					_finished = true;
					if (XulManager.PERFORMANCE_BENCH) {
						// Log.d("BENCH!!!", "collect finished!!!");
					}
					break;
				}
				final XulView view = firstView.get();
				if (view == null) {
					continue;
				}
				viewRender = view._render;
				if (viewRender == null) {
					continue;
				}
				if (view instanceof XulArea) {
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

			if (!viewRender.doSuspendRecycle(_recycleLevel)) {
				continue;
			}
			_targetView = firstView;
			return;
		}
		if (XulManager.PERFORMANCE_BENCH) {
			// Log.d("BENCH!!!", String.format("collect nothing %d", XulUtils.timestamp_us() - beginTime));
		}
		return;
	}
}
