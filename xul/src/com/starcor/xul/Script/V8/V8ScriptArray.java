package com.starcor.xul.Script.V8;

import android.util.Log;
import com.starcor.xul.Script.IScriptArray;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

import java.util.Collection;

/**
 * Created by hy on 2015/6/19.
 */
public class V8ScriptArray implements IScriptArray {
	public static final String TAG = V8ScriptArray.class.getSimpleName();
	V8ScriptContext _ctx;
	long _nativeId;

	V8ScriptArray(V8ScriptContext ctx, long arrayId) {
		_ctx = ctx;
		_nativeId = arrayId;
	}

	public V8ScriptArray(V8ScriptContext ctx) {
		_ctx = ctx;
		_nativeId = V8Engine.v8CreateArray(ctx._nativeId, this);
	}

	public void add(int val) {
		V8Engine.v8ArraySetInt(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void add(long val) {
		V8Engine.v8ArraySetLong(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void add(String val) {
		V8Engine.v8ArraySetString(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void add(float val) {
		V8Engine.v8ArraySetFloat(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void add(double val) {
		V8Engine.v8ArraySetDouble(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	public void add(boolean val) {
		V8Engine.v8ArraySetBoolean(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val);
	}

	@Override
	public void add(IScriptableObject val) {
		add((V8ScriptObject) val);
	}

	@Override
	public void add(IScriptArray val) {
		add((V8ScriptArray) val);
	}

	public void add(V8ScriptObject val) {
		V8Engine.v8ArraySetScriptObject(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val._nativeId);
	}

	public void add(V8ScriptArray val) {
		V8Engine.v8ArraySetArray(_ctx._nativeId, _nativeId, V8Engine.ArgumentIndexLast, val._nativeId);
	}

	public int getInteger(int idx) {
		return V8Engine.v8ArrayGetInt(_ctx._nativeId, _nativeId, idx);
	}

	public long getLong(int idx) {
		return V8Engine.v8ArrayGetLong(_ctx._nativeId, _nativeId, idx);
	}

	public float getFloat(int idx) {
		return V8Engine.v8ArrayGetFloat(_ctx._nativeId, _nativeId, idx);
	}

	public double getDouble(int idx) {
		return V8Engine.v8ArrayGetDouble(_ctx._nativeId, _nativeId, idx);
	}

	public String getString(int idx) {
		return V8Engine.v8ArrayGetString(_ctx._nativeId, _nativeId, idx);
	}

	@Override
	public int getStringId(int idx) {
		return V8Engine.v8ArrayGetStringId(_ctx._nativeId, _nativeId, idx);
	}

	public boolean getBoolean(int idx) {
		return V8Engine.v8ArrayGetBoolean(_ctx._nativeId, _nativeId, idx);
	}

	public V8ScriptObject getScriptObject(int idx) {
		return V8Engine.v8ArrayGetScriptObject(_ctx._nativeId, _nativeId, idx);
	}

	public V8ScriptArray getArray(int idx) {
		return V8Engine.v8ArrayGetArray(_ctx._nativeId, _nativeId, idx);
	}

	public int size() {
		return V8Engine.v8ArrayLength(_ctx._nativeId, _nativeId);
	}


	@Override
	protected void finalize() throws Throwable {
		if (XulManager.DEBUG_V8_ENGINE) {
			Log.d(TAG, String.format("finalize ctx:%x, id:%x", _ctx._nativeId, _nativeId));
		}
		V8Engine.v8DestroyArray(_ctx._nativeId, _nativeId);
		super.finalize();
	}

	public static V8ScriptArray wrapNativeArray(V8ScriptContext ctx, long arrayId) {
		return new V8ScriptArray(ctx, arrayId);
	}

	public void addAll(Collection val) {
		if (val instanceof XulArrayList) {
			addAll((XulArrayList) val);
			return;
		}
		for (Object v : val) {
			addObject(val, v);
		}
	}

	public void addAll(XulArrayList val) {
		Object[] arrayBuf = val.getArray();
		for (int i = 0, valSize = val.size(); i < valSize; i++) {
			addObject(val, arrayBuf[i]);
		}
	}

	public void addAll(Object[] val) {
		for (int i = 0, valLength = val.length; i < valLength; i++) {
			Object v = val[i];
			addObject(val, v);
		}
	}

	private void addObject(Object container, Object v) {
		if (v instanceof V8ScriptObject) {
			add((V8ScriptObject) v);
		} else if (v instanceof XulView) {
			add((V8ScriptObject) ((XulView) v).getScriptableObject(V8ScriptContext.DEFAULT_SCRIPT_TYPE));
		} else if (v instanceof String) {
			add((String) v);
		} else if (v instanceof Integer) {
			add((Integer) v);
		} else if (v instanceof Boolean) {
			add((Boolean) v);
		} else if (v instanceof Float) {
			add((Float) v);
		} else if (v instanceof V8ScriptArray) {
			add((V8ScriptArray) v);
		} else if (v instanceof Object[]) {
			if (v.equals(container)) {
				add(this);
			} else {
				V8ScriptArray array = new V8ScriptArray(_ctx);
				array.addAll((Object[]) v);
				add(array);
			}
		} else if (v instanceof Collection) {
			if (v.equals(container)) {
				add(this);
			} else {
				V8ScriptArray array = new V8ScriptArray(_ctx);
				array.addAll((Collection) v);
				add(array);
			}
		} else if (v instanceof Double) {
			add((Double) v);
		} else if (v instanceof Long) {
			add((Long) v);
		} else {
			add(String.valueOf(v));
		}
	}

	private static V8ScriptArray EMPTY_SCRIPT_ARRAY;

	private static V8ScriptArray getEmptyArray(V8ScriptContext ctx) {
		if (EMPTY_SCRIPT_ARRAY == null) {
			EMPTY_SCRIPT_ARRAY = new V8ScriptArray(ctx);
		}
		return EMPTY_SCRIPT_ARRAY;
	}

	public static V8ScriptArray from(V8ScriptContext ctx, Collection objs) {
		if (objs == null || objs.isEmpty()) {
			return getEmptyArray(ctx);
		}
		V8ScriptArray array = new V8ScriptArray(ctx);
		array.addAll(objs);
		return array;
	}

	public static V8ScriptArray fromArrayList(V8ScriptContext ctx, XulArrayList objs) {
		if (objs == null || objs.isEmpty()) {
			return getEmptyArray(ctx);
		}
		long t1 = XulUtils.timestamp_us();
		V8ScriptArray array = new V8ScriptArray(ctx);
		long t2 = XulUtils.timestamp_us();
		array.addAll(objs);
		long t3 = XulUtils.timestamp_us();
		Log.d("BENCH!!!", "setResult " + (t2 - t1) + " " + (t3 - t2));
		return array;
	}

	public static V8ScriptArray from(V8ScriptContext ctx, Object[] objs) {
		if (objs == null || objs.length == 0) {
			return getEmptyArray(ctx);
		}
		V8ScriptArray array = new V8ScriptArray(ctx);
		array.addAll(objs);
		return array;
	}
}
