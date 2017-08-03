package com.starcor.xul.ScriptWrappr;

import com.starcor.xul.Script.IScriptArguments;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptGetter;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptMethod;
import com.starcor.xul.Wrapper.XulPageSliderAreaWrapper;
import com.starcor.xul.XulArea;

/**
 * Created by hy on 2014/6/27.
 */
public class XulPageSliderScriptableObject extends XulAreaScriptableObject {
	XulPageSliderAreaWrapper _wrapper;

	public XulPageSliderScriptableObject(XulArea item) {
		super(item);
	}

	private boolean _initWrapper() {
		if (_wrapper != null) {
			return true;
		}
		_wrapper = XulPageSliderAreaWrapper.fromXulView(_xulItem);
		return _wrapper != null;
	}

	@ScriptGetter("pageCount")
	public int _script_getter_pageCount(IScriptContext ctx) {
		if (!_initWrapper()) {
			return -1;
		}
		return _wrapper.getPageCount();
	}

	@ScriptGetter("currentPage")
	public int _script_getter_currentPage(IScriptContext ctx) {
		if (!_initWrapper()) {
			return -1;
		}
		return _wrapper.getCurrentPage();
	}

	@ScriptMethod("setCurrentPage")
	public Boolean _script_setCurrentPage(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		if (args.size() != 1) {
			return Boolean.FALSE;
		}
		int page = args.getInteger(0);
		return Boolean.valueOf(_wrapper.setCurrentPage(page));
	}

	@ScriptMethod("invokeImageGC")
	public Boolean _script_invokeImageGC(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		if (args.size() != 1) {
			return Boolean.FALSE;
		}
		int level = args.getInteger(0);
		_wrapper.invokeImageGC(level);
		return Boolean.TRUE;
	}

	@ScriptMethod("syncPages")
	public Boolean _script_syncPages(IScriptContext ctx) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		_wrapper.syncPages();
		return Boolean.TRUE;
	}

	@ScriptMethod("slideNext")
	public Boolean _script_slideNext(IScriptContext ctx) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		_wrapper.slideNext();
		return Boolean.TRUE;
	}

	@ScriptMethod("slidePrev")
	public Boolean _script_slidePrev(IScriptContext ctx) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		_wrapper.slidePrev();
		return true;
	}

	@ScriptMethod("slideLeft")
	public Boolean _script_slideLeft(IScriptContext ctx) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		_wrapper.slideLeft();
		return Boolean.TRUE;
	}

	@ScriptMethod("slideRight")
	public Boolean _script_slideRight(IScriptContext ctx) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		_wrapper.slideRight();
		return Boolean.TRUE;
	}

	@ScriptMethod("slideUp")
	public Boolean _script_slideUp(IScriptContext ctx) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		_wrapper.slideUp();
		return Boolean.TRUE;
	}

	@ScriptMethod("slideDown")
	public Boolean _script_slideDown(IScriptContext ctx) {
		if (!_initWrapper()) {
			return Boolean.FALSE;
		}
		_wrapper.slideDown();
		return Boolean.TRUE;
	}
}
