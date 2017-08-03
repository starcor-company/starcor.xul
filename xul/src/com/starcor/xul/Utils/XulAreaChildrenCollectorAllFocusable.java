package com.starcor.xul.Utils;

import com.starcor.xul.XulArea;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulView;

public class XulAreaChildrenCollectorAllFocusable extends XulArea.XulAreaIterator {
	XulArrayList<XulView> _result = new XulArrayList<XulView>();

	public void begin() {
		_result.clear();
	}

	public XulArrayList<XulView> end() {
		return _result;
	}

	public void clear() {
		_result.clear();
	}

	private void doCollect(XulView view) {
		if ( view.isEnabled() && view.isVisible() && view.focusable() ) {
			_result.add(view);
		}
	}

	@Override
	public boolean onXulArea(int pos, XulArea area) {
		doCollect(area);
		area.eachChild(this);
		return true;
	}

	@Override
	public boolean onXulItem(int pos, XulItem item) {
		doCollect(item);
		return true;
	}
}
