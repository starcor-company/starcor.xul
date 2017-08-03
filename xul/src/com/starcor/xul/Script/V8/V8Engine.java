package com.starcor.xul.Script.V8;

import android.util.Log;

/**
 * Created by hy on 2015/6/17.
 */
public class V8Engine {
	static {
		System.loadLibrary("starcor_xul-v8");
	}

	public static native long v8CreateScriptContext(Object object);

	public static native long v8CreateScriptObject(long ctxId, Object object);

	public static native long v8CreateArguments(long ctxId);

	public static native void v8DestroyScriptContext(long ctxId);

	public static native void v8DestroyScriptObject(long ctxId, long objectId);

	public static native void v8DestroyArguments(long ctxId, long argsId);

	public static native void v8DestroyFunction(long ctxId, long funcId);

	public static native void v8DestroyScript(long ctxId, long scriptId);

	public static native int v8ObjectAddMethod(long ctxId, long objectId, String methodName);

	public static native int v8ObjectAddProperty(long ctxId, long objectId, String attrName, boolean enableGetter, boolean enableSetter);

	public static native void v8ObjectSetPrototype(long ctxId, long objectId, long prototypeId);

	public static native long v8ObjectGetFunction(long ctxId, long objectId, String funcName);

	public static native long v8Compile(long ctxIdx, String script, String srcFile, int lineOffset);

	public static native long v8CompileFunction(long ctxIdx, String funcBody, String srcFile, int lineOffset);

	public static native void v8RunScript(long ctxId, long scriptId);

	public static native long v8GetFunction(long ctxId, String funcName);

	public static native boolean v8CallFunction(long ctxId, long funcId, long thisObjId, long argsId);

	public static final int ArgumentIndexResult = -2;
	public static final int ArgumentIndexLast = -1;

	public static native int v8ArgumentsClear(long ctxId, long argsId);
	public static native int v8ArgumentsLength(long ctxId, long argsId);

	public static native boolean v8ArgumentsGetBoolean(long ctxId, long argsId, int idx);
	public static native int v8ArgumentsSetBoolean(long ctxId, long argsId, int idx, boolean val);

	public static native int v8ArgumentsSetInt(long ctxId, long argsId, int idx, int val);
	public static native int v8ArgumentsGetInt(long ctxId, long argsId, int idx);

	public static native int v8ArgumentsSetLong(long ctxId, long argsId, int idx, long val);
	public static native long v8ArgumentsGetLong(long ctxId, long argsId, int idx);

	public static native int v8ArgumentsSetString(long ctxId, long argsId, int idx, String val);
	public static native String v8ArgumentsGetString(long ctxId, long argsId, int idx);
	public static native int v8ArgumentsGetStringId(long ctxId, long argsId, int idx);

	public static native int v8ArgumentsSetFloat(long ctxId, long argsId, int idx, float val);
	public static native float v8ArgumentsGetFloat(long ctxId, long argsId, int idx);

	public static native int v8ArgumentsSetDouble(long ctxId, long argsId, int idx, double val);
	public static native double v8ArgumentsGetDouble(long ctxId, long argsId, int idx);

	public static native int v8ArgumentsSetScriptObject(long ctxId, long argsId, int idx, long objectId);
	public static native V8ScriptObject v8ArgumentsGetScriptObject(long ctxId, long argsId, int idx);

	public static native int v8ArgumentsSetArray(long ctxId, long argsId, int idx, long arrayId);
	public static native V8ScriptArray v8ArgumentsGetArray(long ctxId, long argsId, int idx);

	public static native long v8CreateArray(long ctxId, Object object);
	public static native void v8DestroyArray(long ctxId, long arrayId);
	public static native int v8ArrayLength(long ctxId, long arrayId);

	public static native int v8ArraySetBoolean(long ctxId, long argsId, int idx, boolean val);
	public static native boolean v8ArrayGetBoolean(long ctxId, long argsId, int idx);

	public static native int v8ArraySetInt(long ctxId, long argsId, int idx, int val);
	public static native int v8ArrayGetInt(long ctxId, long argsId, int idx);

	public static native int v8ArraySetLong(long ctxId, long argsId, int idx, long val);
	public static native long v8ArrayGetLong(long ctxId, long argsId, int idx);

	public static native int v8ArraySetFloat(long ctxId, long argsId, int idx, float val);
	public static native float v8ArrayGetFloat(long ctxId, long argsId, int idx);

	public static native int v8ArraySetDouble(long ctxId, long argsId, int idx, double val);
	public static native double v8ArrayGetDouble(long ctxId, long argsId, int idx);

	public static native int v8ArraySetString(long ctxId, long argsId, int idx, String val);
	public static native String v8ArrayGetString(long ctxId, long argsId, int idx);
	public static native int v8ArrayGetStringId(long ctxId, long argsId, int idx);

	public static native int v8ArraySetScriptObject(long ctxId, long argsId, int idx, long objectId);
	public static native V8ScriptObject v8ArrayGetScriptObject(long ctxId, long argsId, int idx);

	public static native int v8ArraySetArray(long ctxId, long argsId, int idx, long arrayId);
	public static native V8ScriptArray v8ArrayGetArray(long ctxId, long argsId, int idx);

	public static native boolean v8ContextAddIndexedString(long ctxId, String str, int strId);
	public static native boolean v8ContextRemoveIndexedString(long ctxId, String str);

	public static native void v8ContextOnIdle(long ctxId, int idleMs);
	public static native int v8EngineStatistics(int typeId);

	public static final int V8TypeIdScriptContext = 1;
	public static final int V8TypeIdScriptObject = 2;
	public static final int V8TypeIdScriptArray = 3;
	public static final int V8TypeIdScriptFunction = 4;
	public static final int V8TypeIdScriptProgram = 5;
	public static final int V8TypeIdScriptArguments = 6;

	public static void getV8Statistics() {
		Log.d("V8Engine/Statistics", String.format("ctx:%d, obj:%d, array:%d, func:%d, prog:%d, args:%d",
			v8EngineStatistics(V8TypeIdScriptContext),
			v8EngineStatistics(V8TypeIdScriptObject),
			v8EngineStatistics(V8TypeIdScriptArray),
			v8EngineStatistics(V8TypeIdScriptFunction),
			v8EngineStatistics(V8TypeIdScriptProgram),
			v8EngineStatistics(V8TypeIdScriptArguments)
		));
	}
}
