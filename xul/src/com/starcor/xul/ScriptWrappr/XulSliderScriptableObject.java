package com.starcor.xul.ScriptWrappr;

import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Script.IScriptArguments;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptGetter;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptMethod;
import com.starcor.xul.Wrapper.XulSliderAreaWrapper;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/27.
 */
public class XulSliderScriptableObject extends XulAreaScriptableObject {
	XulSliderAreaWrapper _wrapper;

	public XulSliderScriptableObject(XulArea item) {
		super(item);
	}

	private boolean _initWrapper() {
		if (_wrapper != null) {
			return true;
		}
		_wrapper = XulSliderAreaWrapper.fromXulView(_xulItem);
		return _wrapper != null;
	}

	@ScriptGetter("scrollPos")
	public int _script_getter_scrollPos(IScriptContext ctx) {
		if (!_initWrapper()) {
			return -1;
		}
		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return 0;
		}
		boolean vertical = _wrapper.isVertical();
		float scalar = (float) (vertical ? render.getYScalar() : render.getXScalar());
		return (int) (_wrapper.getScrollPos() / scalar);
	}

	@ScriptGetter("scrollDelta")
	public int _script_getter_scrollDelta(IScriptContext ctx) {
		if (!_initWrapper()) {
			return -1;
		}
		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return 0;
		}
		boolean vertical = _wrapper.isVertical();
		float scalar = (float) (vertical ? render.getYScalar() : render.getXScalar());
		return (int) (_wrapper.getScrollDelta() / scalar);
	}

	@ScriptGetter("scrollRange")
	public int _script_getter_scrollRange(IScriptContext ctx) {
		if (!_initWrapper()) {
			return -1;
		}
		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return 0;
		}
		boolean vertical = _wrapper.isVertical();
		float scalar = (float) (vertical ? render.getYScalar() : render.getXScalar());
		return (int) (_wrapper.getScrollRange() / scalar);
	}

	@ScriptMethod("scrollTo")
	public Boolean _script_scrollTo(IScriptContext ctx, IScriptArguments args) {
		int argNum = args.size();
		if (!_initWrapper() || argNum < 1 || argNum > 2) {
			return Boolean.FALSE;
		}
		int pos = args.getInteger(0);
		if (pos < 0) {
			return Boolean.FALSE;
		}
		boolean withAnimation = true;
		if (argNum == 2) {
			withAnimation = args.getBoolean(1);
		}
		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return Boolean.FALSE;
		}
		boolean vertical = _wrapper.isVertical();
		float scalar = (float) (vertical ? render.getYScalar() : render.getXScalar());
		_wrapper.scrollTo((int) (pos * scalar), withAnimation);
		return Boolean.TRUE;
	}

	@ScriptMethod("scrollByPage")
	public Boolean _script_scrollByPage(IScriptContext ctx, IScriptArguments args) {
		int argNum = args.size();
		if (!_initWrapper() || argNum < 1 || argNum > 2) {
			return Boolean.FALSE;
		}
		int pages = args.getInteger(0);
		if (pages == 0) {
			return Boolean.FALSE;
		}
		boolean withAnimation = true;
		if (argNum == 2) {
			withAnimation = args.getBoolean(1);
		}
		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return Boolean.FALSE;
		}
		_wrapper.scrollByPage(pages, withAnimation);
		return Boolean.TRUE;
	}

	@ScriptMethod("activateScrollBar")
	public Boolean _script_activateScrollBar(IScriptContext ctx) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		_wrapper.activateScrollBar();
		return Boolean.TRUE;
	}

	@ScriptMethod("makeChildVisible")
	public Boolean _script_makeChildVisible(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		IScriptableObject scriptableObject = args.getScriptableObject(0);
		if (scriptableObject == null) {
			return Boolean.FALSE;
		}

		switch (args.size()) {
		case 1:
			return Boolean.valueOf(_wrapper.makeChildVisible((XulView) scriptableObject.getObjectValue().getUnwrappedObject()));
		case 2: {
			String lastParam = args.getString(1);
			if ("true".equals(lastParam)) {
				return Boolean.valueOf(_wrapper.makeChildVisible((XulView) scriptableObject.getObjectValue().getUnwrappedObject(), true));
			} else if ("false".equals(lastParam)) {
				return Boolean.valueOf(_wrapper.makeChildVisible((XulView) scriptableObject.getObjectValue().getUnwrappedObject(), false));
			}
			return Boolean.valueOf(_wrapper.makeChildVisible((XulView) scriptableObject.getObjectValue().getUnwrappedObject(), args.getFloat(1)));
		}
		case 3: {
			String lastParam = args.getString(2);
			if ("true".equals(lastParam)) {
				return Boolean.valueOf(_wrapper.makeChildVisible((XulView) scriptableObject.getObjectValue().getUnwrappedObject(), args.getFloat(1), true));
			} else if ("false".equals(lastParam)) {
				return Boolean.valueOf(_wrapper.makeChildVisible((XulView) scriptableObject.getObjectValue().getUnwrappedObject(), args.getFloat(1), false));
			}
			return Boolean.valueOf(_wrapper.makeChildVisible((XulView) scriptableObject.getObjectValue().getUnwrappedObject(), args.getFloat(1), args.getFloat(2)));
		}
		case 4:
			return Boolean.valueOf(_wrapper.makeChildVisible((XulView) scriptableObject.getObjectValue().getUnwrappedObject(), args.getFloat(1), args.getFloat(2), args.getBoolean(3)));
		}
		return Boolean.FALSE;
	}
}
