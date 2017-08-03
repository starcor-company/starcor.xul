package com.starcor.xul.Script.V8;

import android.util.Log;
import com.starcor.xul.Script.IScriptArguments;
import com.starcor.xul.Script.IScriptArray;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulView;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by hy on 2015/6/17.
 */
public class V8Arguments implements IScriptArguments {
	public static final String TAG = V8Arguments.class.getSimpleName();
	V8ScriptContext _ctx;
	long _nativeId;
	boolean _autoDestroy = true;

	@Override
	protected void finalize() throws Throwable {
		if (XulManager.DEBUG_V8_ENGINE) {
			Log.d(TAG, String.format("finalize ctx:%x, id:%x, destroy:%s", _ctx._nativeId, _nativeId, String.valueOf(_autoDestroy)));
		}
		if (_autoDestroy) {
			V8Engine.v8DestroyArguments(_ctx._nativeId, _nativeId);
		}
		super.finalize();
	}

	V8Arguments(V8ScriptContext ctx, long argsId) {
		_ctx = ctx;
		_nativeId = argsId;
	}

	public V8Arguments(V8ScriptContext ctx) {
		this(ctx, V8Engine.v8CreateArguments(ctx._nativeId));
	}

	public void setResult(int val) {
		V8Engine.v8ArgumentsSetInt(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexResult, val);
	}

	public void setResult(long val) {
		V8Engine.v8ArgumentsSetLong(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexResult, val);
	}

	public void setResult(String val) {
		V8Engine.v8ArgumentsSetString(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexResult, val);
	}

	public void setResult(float val) {
		V8Engine.v8ArgumentsSetFloat(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexResult, val);
	}

	public void setResult(double val) {
		V8Engine.v8ArgumentsSetDouble(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexResult, val);
	}

	public void setResult(boolean val) {
		V8Engine.v8ArgumentsSetBoolean(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexResult, val);
	}

	@Override
	public void setResult(Collection objects) {
		setResult(V8ScriptArray.from(_ctx, objects));
	}

	@Override
	public void setResult(XulArrayList objects) {
		setResult(V8ScriptArray.fromArrayList(_ctx, objects));
	}

	@Override
	public void setResult(Object[] objects) {
		setResult(V8ScriptArray.from(_ctx, objects));
	}

	@Override
	public void setResult(IScriptableObject val) {
		setResult((V8ScriptObject) val);
	}

	@Override
	public void setResult(IScriptArray val) {
		setResult((V8ScriptArray) val);
	}

	public void setResult(V8ScriptObject val) {
		V8Engine.v8ArgumentsSetScriptObject(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexResult, val._nativeId);
	}

	public void setResult(V8ScriptArray val) {
		V8Engine.v8ArgumentsSetArray(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexResult, val._nativeId);
	}

	public void addArg(int val) {
		V8Engine.v8ArgumentsSetInt(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void addArg(long val) {
		V8Engine.v8ArgumentsSetLong(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void addArg(String val) {
		V8Engine.v8ArgumentsSetString(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void addArg(float val) {
		V8Engine.v8ArgumentsSetFloat(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void addArg(double val) {
		V8Engine.v8ArgumentsSetDouble(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void addArg(boolean val) {
		V8Engine.v8ArgumentsSetBoolean(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void addArg(V8ScriptObject val) {
		V8Engine.v8ArgumentsSetScriptObject(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val._nativeId);
	}

	public void addArg(V8ScriptArray val) {
		V8Engine.v8ArgumentsSetArray(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val._nativeId);
	}

	public int getInteger(int idx) {
		return V8Engine.v8ArgumentsGetInt(_ctx._nativeId, _nativeId, idx);
	}

	public long getLong(int idx) {
		return V8Engine.v8ArgumentsGetLong(_ctx._nativeId, _nativeId, idx);
	}

	public float getFloat(int idx) {
		return V8Engine.v8ArgumentsGetFloat(_ctx._nativeId, _nativeId, idx);
	}

	public double getDouble(int idx) {
		return V8Engine.v8ArgumentsGetDouble(_ctx._nativeId, _nativeId, idx);
	}

	public String getString(int idx) {
		return V8Engine.v8ArgumentsGetString(_ctx._nativeId, _nativeId, idx);
	}

	@Override
	public int getStringId(int idx) {
		return V8Engine.v8ArgumentsGetStringId(_ctx._nativeId, _nativeId, idx);
	}

	public boolean getBoolean(int idx) {
		return V8Engine.v8ArgumentsGetBoolean(_ctx._nativeId, _nativeId, idx);
	}

	@Override
	public IScriptableObject getScriptableObject(int idx) {
		return getScriptObject(idx);
	}

	public V8ScriptObject getScriptObject(int idx) {
		return V8Engine.v8ArgumentsGetScriptObject(_ctx._nativeId, _nativeId, idx);
	}

	public V8ScriptArray getArray(int idx) {
		return V8Engine.v8ArgumentsGetArray(_ctx._nativeId, _nativeId, idx);
	}

	public int size() {
		return V8Engine.v8ArgumentsLength(_ctx._nativeId, _nativeId);
	}

	public void reset() {
		V8Engine.v8ArgumentsClear(_ctx._nativeId, _nativeId);
	}

	private static V8Arguments tempNativeArguments;

	static V8Arguments wrapTempNativeArguments(V8ScriptContext ctx, long argsId) {
		if (argsId == 0) {
			return null;
		}
		if (tempNativeArguments == null) {
			tempNativeArguments = new V8Arguments(ctx, argsId);
			tempNativeArguments._autoDestroy = false;
		} else {
			tempNativeArguments._ctx = ctx;
			tempNativeArguments._nativeId = argsId;
		}
		return tempNativeArguments;
	}

	public static V8Arguments from(V8ScriptContext ctx, Object[] args) {
		if (args == null || args.length == 0) {
			return null;
		}
		V8Arguments v8Arguments = new V8Arguments(ctx);
		v8Arguments.addAll(Arrays.asList(args));
		return v8Arguments;
	}


	private void addObject(Object v) {
		if (v instanceof V8ScriptObject) {
			addArg((V8ScriptObject) v);
		} else if (v instanceof XulView) {
			addArg((V8ScriptObject) ((XulView) v).getScriptableObject(V8ScriptContext.DEFAULT_SCRIPT_TYPE));
		} else if (v instanceof String) {
			addArg((String) v);
		} else if (v instanceof Integer) {
			addArg((Integer) v);
		} else if (v instanceof Boolean) {
			addArg((Boolean) v);
		} else if (v instanceof Float) {
			addArg((Float) v);
		} else if (v instanceof V8ScriptArray) {
			addArg((V8ScriptArray) v);
		} else if (v instanceof Object[]) {
			V8ScriptArray array = new V8ScriptArray(_ctx);
			array.addAll((Object[]) v);
			addArg(array);
		} else if (v instanceof Collection) {
			V8ScriptArray array = new V8ScriptArray(_ctx);
			array.addAll((Collection) v);
			addArg(array);
		} else if (v instanceof Double) {
			addArg((Double) v);
		} else if (v instanceof Long) {
			addArg((Long) v);
		} else {
			addArg(String.valueOf(v));
		}
	}


	public void addAll(Collection val) {
		for (Object v : val) {
			addObject(v);
		}
	}

	public void addAll(Object[] val) {
		for (int i = 0, valLength = val.length; i < valLength; i++) {
			Object v = val[i];
			addObject(v);
		}
	}
}
