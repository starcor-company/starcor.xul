package com.starcor.xul.Utils;

import android.util.Pair;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulView;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/23.
 */
public class XulAreaChildrenVisibleChangeNotifier extends XulArea.XulAreaIterator {
	ArrayList<Pair<XulArea, Boolean>> notifyStack = new ArrayList<Pair<XulArea, Boolean>>();

	boolean _isVisible = false;
	XulArea _eventSource = null;

	private void notifyItem(XulView view) {
		if (!view.isVisible()) {
			return;
		}
		XulViewRender render = view.getRender();
		if (render != null) {
			render.onVisibilityChanged(_isVisible, _eventSource);
		}
	}

	@Override
	public boolean onXulArea(int pos, XulArea area) {
		notifyItem(area);
		return true;
	}

	@Override
	public boolean onXulItem(int pos, XulItem item) {
		notifyItem(item);
		return true;
	}

	public void begin(boolean isVisible, XulArea eventSource) {
		if (_eventSource != null) {
			notifyStack.add(Pair.create(_eventSource, _isVisible));
		}
		_isVisible = isVisible;
		_eventSource = eventSource;
	}
	public void end() {
		if (notifyStack.isEmpty()) {
			_eventSource = null;
			return;
		}
		Pair<XulArea, Boolean> oldState = notifyStack.remove(notifyStack.size() - 1);
		_isVisible = oldState.second;
		_eventSource = oldState.first;
	}


	private static XulAreaChildrenVisibleChangeNotifier g_notifier;
	public static XulAreaChildrenVisibleChangeNotifier getNotifier() {
		if (g_notifier == null) {
			g_notifier = new XulAreaChildrenVisibleChangeNotifier();
		}
		return g_notifier;
	}
}
