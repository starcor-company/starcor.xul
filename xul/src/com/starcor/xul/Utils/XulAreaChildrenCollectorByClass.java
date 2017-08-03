package com.starcor.xul.Utils;

import com.starcor.xul.XulArea;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulView;

import java.util.ArrayList;

public class XulAreaChildrenCollectorByClass extends XulArea.XulAreaIterator {
	XulArrayList<XulView> _result = new XulArrayList<XulView>();
	ArrayList<String> _classSet = new ArrayList<String>();
	boolean _isAny = true;

	public void begin(String clsName) {
		begin(true, clsName);
	}

	public void begin(String... clsNames) {
		begin(true, clsNames);
	}

	public void begin(boolean isAny, String... clsNames) {
		_classSet.clear();
		_isAny = isAny;
		int length = clsNames.length;
		for (int i = 0; i < length; i++) {
			String clsName = clsNames[i];
			_classSet.add(clsName);
		}
		_result.clear();
	}

	public XulArrayList<XulView> end() {
		return _result;
	}

	public void clear() {
		_result.clear();
	}

	private boolean testAny(XulView view) {
		for (int i = 0; i < _classSet.size(); i++) {
			String clsName = _classSet.get(i);
			if (view.hasClass(clsName)) {
				return true;
			}
		}
		return false;
	}

	private boolean testAll(XulView view) {
		for (int i = 0; i < _classSet.size(); i++) {
			String clsName = _classSet.get(i);
			if (!view.hasClass(clsName)) {
				return false;
			}
		}
		return true;
	}

	private void doCollect(XulView view) {
		if (_isAny) {
			if (testAny(view)) {
				_result.add(view);
			}

		} else {
			if (testAll(view)) {
				_result.add(view);
			}
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
