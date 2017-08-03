package com.starcor.xul.Script.V8;

/**
 * Created by hy on 2015/6/18.
 */
public abstract class V8MethodCallback {
	public boolean invoke(V8ScriptObject thisObject, V8Arguments args) {
		return false;
	}

	public boolean set(V8ScriptObject thisObject, V8Arguments args) {
		return false;
	}

	public boolean get(V8ScriptObject thisObject, V8Arguments args) {
		return false;
	}
}
