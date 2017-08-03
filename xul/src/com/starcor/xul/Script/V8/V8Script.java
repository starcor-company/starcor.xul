package com.starcor.xul.Script.V8;

import android.util.Log;
import com.starcor.xul.Script.IScript;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.XulManager;

/**
 * Created by hy on 2015/6/17.
 */
public class V8Script implements IScript {
	public static final String TAG = V8Script.class.getSimpleName();
	V8ScriptContext _ctx;
	long _nativeId;

	@Override
	protected void finalize() throws Throwable {
		if (XulManager.DEBUG_V8_ENGINE) {
			Log.d(TAG, String.format("finalize ctx:%x, id:%x", _ctx._nativeId, _nativeId));
		}
		V8Engine.v8DestroyScript(_ctx._nativeId, _nativeId);
		super.finalize();
	}

	V8Script(V8ScriptContext ctx, long nativeScriptId) {
		this._ctx = ctx;
		this._nativeId = nativeScriptId;
	}

	public static V8Script wrapNativeScript(V8ScriptContext ctx, long scriptId) {
		return new V8Script(ctx, scriptId);
	}

	public void run() {
		V8Engine.v8RunScript(_ctx._nativeId, _nativeId);
	}

	@Override
	public String getScriptType() {
		return V8ScriptContext.DEFAULT_SCRIPT_TYPE;
	}

	@Override
	public Object run(IScriptContext ctx, IScriptableObject ctxObj) {
		this.run();
		return null;
	}

	@Override
	public Object run(IScriptContext ctx, IScriptableObject ctxObj, Object[] args) {
		this.run();
		return null;
	}
}
