package com.starcor.xul.Script.V8;

import android.util.Log;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.Script.XulScriptableObject;
import com.starcor.xul.XulManager;

/**
 * Created by hy on 2015/6/17.
 */
public class V8ScriptObject implements IScriptableObject {
	public static final String TAG = V8ScriptObject.class.getSimpleName();
	V8ScriptContext _ctx;
	V8ScriptObject _prototype;
	long _nativeId;
	V8MethodCallback[] _methodSlots;

	@Override
	protected void finalize() throws Throwable {
		if (XulManager.DEBUG_V8_ENGINE) {
			Log.d(TAG, String.format("finalize ctx:%x, id:%x", _ctx._nativeId, _nativeId));
		}
		V8Engine.v8DestroyScriptObject(_ctx._nativeId, _nativeId);
		super.finalize();
	}

	static boolean invokeMethod(V8ScriptObject classObject, V8ScriptObject thisObject, int methodId, long argsId) {
		if (classObject._methodSlots == null) {
			return false;
		}
		V8MethodCallback method = classObject._methodSlots[methodId];
		V8Arguments args = V8Arguments.wrapTempNativeArguments(classObject._ctx, argsId);
		try {
			return method.invoke(thisObject, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	static boolean setProperty(V8ScriptObject classObject, V8ScriptObject thisObject, int attrId, long argsId) {
		if (classObject._methodSlots == null) {
			return false;
		}
		V8MethodCallback method = classObject._methodSlots[attrId];
		V8Arguments args = V8Arguments.wrapTempNativeArguments(classObject._ctx, argsId);
		try {
			return method.set(thisObject, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	static boolean getProperty(V8ScriptObject classObject, V8ScriptObject thisObject, int attrId, long argsId) {
		if (classObject._methodSlots == null) {
			return false;
		}
		V8MethodCallback method = classObject._methodSlots[attrId];
		V8Arguments args = V8Arguments.wrapTempNativeArguments(classObject._ctx, argsId);
		try {
			return method.get(thisObject, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public V8ScriptObject(V8ScriptContext ctx) {
		_ctx = ctx;
		_nativeId = V8Engine.v8CreateScriptObject(_ctx._nativeId, this);
	}

	public V8ScriptObject(V8ScriptObject prototype) {
		this(prototype._ctx);
		_prototype = prototype;
		V8Engine.v8ObjectSetPrototype(_ctx._nativeId, _nativeId, _prototype._nativeId);
	}

	public V8ScriptFunction getMethod(String name) {
		long funcId = V8Engine.v8ObjectGetFunction(_ctx._nativeId, _nativeId, name);
		return V8ScriptFunction.wrapNativeFunction(_ctx, funcId);
	}

	public boolean addMethod(String name, V8MethodCallback callback) {
		int methodId = V8Engine.v8ObjectAddMethod(_ctx._nativeId, _nativeId, name);
		writeMethodSlot(callback, methodId);
		return true;
	}

	private boolean internalAddPropertyGetter(String name, V8MethodCallback callback, boolean enableGetter, boolean enableSetter) {
		int methodId = V8Engine.v8ObjectAddProperty(_ctx._nativeId, _nativeId, name, enableGetter, enableSetter);
		writeMethodSlot(callback, methodId);
		return true;
	}

	private void writeMethodSlot(V8MethodCallback callback, int methodId) {
		if (_methodSlots == null) {
			_methodSlots = new V8MethodCallback[(methodId + 32) & ~0x1F];
		} else if (_methodSlots.length <= methodId) {
			V8MethodCallback[] newSlots = new V8MethodCallback[(methodId + 32) & ~0x1F];
			System.arraycopy(_methodSlots, 0, newSlots, 0, _methodSlots.length);
			_methodSlots = newSlots;
		}
		_methodSlots[methodId] = callback;
	}

	public boolean addProperty(String name, V8MethodCallback callback) {
		return internalAddPropertyGetter(name, callback, true, true);
	}

	public boolean addPropertyGetter(String name, V8MethodCallback callback) {
		return internalAddPropertyGetter(name, callback, true, false);
	}

	public boolean addPropertySetter(String name, V8MethodCallback callback) {
		return internalAddPropertyGetter(name, callback, false, true);
	}

	public V8ScriptContext getContext() {
		return _ctx;
	}

	XulScriptableObject _extScriptableObject;

	@Override
	public XulScriptableObject getObjectValue() {
		return _extScriptableObject;
	}
}
