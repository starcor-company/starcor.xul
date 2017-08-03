package com.starcor.xul.Script.V8;

import android.util.Log;
import com.starcor.xul.Script.IScript;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.XulManager;

/**
 * Created by hy on 2015/6/17.
 */
public class V8ScriptFunction implements IScript {
	public static final String TAG = V8ScriptFunction.class.getSimpleName();
	V8ScriptContext _ctx;
	long _nativeId;

	@Override
	protected void finalize() throws Throwable {
		if (XulManager.DEBUG_V8_ENGINE) {
			Log.d(TAG, String.format("finalize ctx:%x, id:%x", _ctx._nativeId, _nativeId));
		}
		V8Engine.v8DestroyFunction(_ctx._nativeId, _nativeId);
		super.finalize();
	}

	private V8ScriptFunction(V8ScriptContext ctx, long funcId) {
		_ctx = ctx;
		_nativeId = funcId;
	}

	public boolean call(V8ScriptObject thisObject, V8Arguments args) {
		long ctxNativeId = _ctx._nativeId;
		long funcId = _nativeId;
		long thisObjId = thisObject == null ? 0 : thisObject._nativeId;
		long argsId = args == null ? 0 : args._nativeId;
		if (XulManager.DEBUG_V8_ENGINE) {
			Log.d(TAG, String.format("call ctx:%x, func:%x, this:%x, args:%x", ctxNativeId, funcId, thisObjId, argsId));
		}
		return V8Engine.v8CallFunction(ctxNativeId, funcId
			, thisObjId
			, argsId);
	}

	static V8ScriptFunction wrapNativeFunction(V8ScriptContext ctx, long funcId) {
		if (funcId == 0) {
			return null;
		}
		return new V8ScriptFunction(ctx, funcId);
	}

	@Override
	public String getScriptType() {
		return V8ScriptContext.DEFAULT_SCRIPT_TYPE;
	}

	@Override
	public Object run(IScriptContext ctx, IScriptableObject ctxObj) {
		this.call((V8ScriptObject) ctxObj, null);
		return null;
	}

	private static V8Arguments cachedArgument;

	@Override
	public Object run(IScriptContext ctx, IScriptableObject ctxObj, Object[] args) {
		if (cachedArgument == null) {
			cachedArgument = V8Arguments.from((V8ScriptContext) ctx, args);
			this.call((V8ScriptObject) ctxObj, cachedArgument);
			if (cachedArgument != null) {
				cachedArgument.reset();
			}
		} else if (args != null && args.length > 0) {
			cachedArgument.reset();
			cachedArgument.addAll(args);
			this.call((V8ScriptObject) ctxObj, cachedArgument);
			cachedArgument.reset();
		} else {
			this.call((V8ScriptObject) ctxObj, null);
		}
		return null;
	}
}
