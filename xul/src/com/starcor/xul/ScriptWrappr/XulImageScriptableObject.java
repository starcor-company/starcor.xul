package com.starcor.xul.ScriptWrappr;

import com.starcor.xul.Script.IScriptArguments;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptMethod;
import com.starcor.xul.Wrapper.XulImageItemWrapper;
import com.starcor.xul.XulItem;

/**
 * Created by hy on 2014/6/27.
 */
public class XulImageScriptableObject extends XulItemScriptableObject {
	XulImageItemWrapper _wrapper;

	public XulImageScriptableObject(XulItem item) {
		super(item);
	}

	private boolean _initWrapper() {
		if (_wrapper != null) {
			return true;
		}
		_wrapper = XulImageItemWrapper.fromXulView(_xulItem);
		return _wrapper != null;
	}

	@ScriptMethod("hasImageLayer")
	public Boolean _script_hasImageLayer(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper() || args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int layerIdx = args.getInteger(0);
		args.setResult(_wrapper.hasImageLayer(layerIdx));
		return Boolean.TRUE;
	}


	@ScriptMethod("getImageWidth")
	public Boolean _script_getImageWidth(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper() || args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int layerIdx = args.getInteger(0);
		args.setResult(_wrapper.getImageWidth(layerIdx));
		return Boolean.TRUE;
	}

	@ScriptMethod("getImageHeight")
	public Boolean _script_getImageHeight(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper() || args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int layerIdx = args.getInteger(0);
		args.setResult(_wrapper.getImageHeight(layerIdx));
		return Boolean.TRUE;
	}

	@ScriptMethod("resetAnimation")
	public Boolean _script_resetAnimation(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper() || args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int layerIdx = args.getInteger(0);
		args.setResult(_wrapper.resetAnimation(layerIdx));
		return Boolean.TRUE;
	}


	@ScriptMethod("isImageLoaded")
	public Boolean _script_isImageLoaded(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper() || args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int layerIdx = args.getInteger(0);
		args.setResult(_wrapper.isImageLoaded(layerIdx));
		return Boolean.TRUE;
	}

	@ScriptMethod("isImageVisible")
	public Boolean _script_isImageVisible(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper() || args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int layerIdx = args.getInteger(0);
		args.setResult(_wrapper.isImageVisible(layerIdx));
		return Boolean.TRUE;
	}

	@ScriptMethod("getImageOpacity")
	public Boolean _script_getImageOpacity(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper() || args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int layerIdx = args.getInteger(0);
		args.setResult(_wrapper.getImageOpacity(layerIdx));
		return Boolean.TRUE;
	}

	@ScriptMethod("getImageUrl")
	public Boolean _script_getImageUrl(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper() || args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int layerIdx = args.getInteger(0);
		args.setResult(_wrapper.getImageUrl(layerIdx));
		return Boolean.TRUE;
	}

	@ScriptMethod("getImageResolvedUrl")
	public Boolean _script_getImageResolvedUrl(IScriptContext ctx, IScriptArguments args) {
		if (!_initWrapper() || args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int layerIdx = args.getInteger(0);
		args.setResult(_wrapper.getImageResolvedUrl(layerIdx));
		return Boolean.TRUE;
	}
}
