package com.starcor.xul.ScriptWrappr;

import com.starcor.xul.Wrapper.XulLayerAreaWrapper;
import com.starcor.xul.XulArea;

/**
 * Created by hy on 2014/6/27.
 */
public class XulLayerScriptableObject extends XulAreaScriptableObject {
	XulLayerAreaWrapper _wrapper;

	public XulLayerScriptableObject(XulArea item) {
		super(item);
	}

	private boolean _initWrapper() {
		if (_wrapper != null) {
			return true;
		}
		_wrapper = XulLayerAreaWrapper.fromXulView(_xulItem);
		return _wrapper != null;
	}
}
