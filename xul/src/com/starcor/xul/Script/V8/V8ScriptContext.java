package com.starcor.xul.Script.V8;

import android.util.Log;
import com.starcor.xul.Script.*;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.XulManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hy on 2015/6/17.
 */
public class V8ScriptContext implements IScriptContext {
	public static final String TAG = V8ScriptContext.class.getSimpleName();
	private static ArrayList<String> _supportedScriptTypes;
	public static final String DEFAULT_SCRIPT_TYPE = "javascript";

	static {
		_supportedScriptTypes = new ArrayList<String>();
		_supportedScriptTypes.add("javascript");
	}

	public static void register() {
		XulScriptFactory.registerFactory(new XulScriptFactory.IScriptFactory() {
			@Override
			public List<String> getSupportScriptTypes() {
				return _supportedScriptTypes;
			}

			@Override
			public IScriptContext createContext() {
				return new V8ScriptContext();
			}
		});
	}

	long _nativeId;

	@Override
	protected void finalize() throws Throwable {
		if (XulManager.DEBUG_V8_ENGINE) {
			Log.d(TAG, String.format("finalize id:%x", _nativeId));
		}
		V8Engine.v8DestroyScriptContext(_nativeId);
		super.finalize();
	}

	V8ScriptArray createArray(long arrayId) {
		return V8ScriptArray.wrapNativeArray(this, arrayId);
	}

	V8ScriptContext() {
		_nativeId = V8Engine.v8CreateScriptContext(this);
	}

	@Override
	public IScript compileScript(InputStream scriptText, String sourceName, int sourceLine) {
		return v8CompileScript(readStreamText(scriptText), sourceName, sourceLine);
	}

	@Override
	public IScript compileFunction(InputStream funcBodyText, String sourceName, int sourceLine) {
		return v8CompileFunction(readStreamText(funcBodyText), sourceName, sourceLine);
	}

	static String readStreamText(InputStream stream) {
		StringBuilder source = new StringBuilder();
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
			do {
				String s = bufferedReader.readLine();
				if (s == null) {
					break;
				}
				source.append(s);
				source.append("\n");
			} while (true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return source.toString();
	}

	@Override
	public IScript compileFunction(String funcBodyText, String sourceName, int sourceLine) {
		return v8CompileFunction(funcBodyText, sourceName, sourceLine);
	}

	@Override
	public IScript compileScript(String scriptText, String sourceName, int sourceLine) {
		return v8CompileScript(scriptText, sourceName, sourceLine);
	}

	@Override
	public IScript getFunction(Object scriptableObject, String funcName) {
		return getFunction(funcName);
	}

	@Override
	public IScriptableObject createScriptObject(XulScriptableObject obj) {
		V8ScriptObject scriptableClass = getScriptableClass(this, obj);

		V8ScriptObject v8ScriptObject = new V8ScriptObject(scriptableClass);
		v8ScriptObject._extScriptableObject = obj;
		return v8ScriptObject;
	}

	@Override
	public IScriptMap createScriptMap() {
		return null;
	}

	@Override
	public IScriptArray createScriptArray() {
		return new V8ScriptArray(this);
	}

	@Override
	public void destroy() {

	}

	@Override
	public boolean addIndexedString(String str, int id) {
		return V8Engine.v8ContextAddIndexedString(_nativeId, str, id);
	}

	@Override
	public boolean delIndexedString(String str) {
		return V8Engine.v8ContextRemoveIndexedString(_nativeId, str);
	}

	@Override
	public void init() {

	}

	@Override
	public String getScriptType() {
		return DEFAULT_SCRIPT_TYPE;
	}

	public V8Script v8CompileScript(String script, String sourceFile, int lineOffset) {
		long scriptId = V8Engine.v8Compile(_nativeId, script, sourceFile, lineOffset - 1);
		return V8Script.wrapNativeScript(this, scriptId);
	}

	public V8ScriptFunction v8CompileFunction(String functionBody, String sourceFile, int lineOffset) {
		long funcId = V8Engine.v8CompileFunction(_nativeId, functionBody+"\n", sourceFile, lineOffset - 1);
		return V8ScriptFunction.wrapNativeFunction(this, funcId);
	}

	public V8ScriptFunction getFunction(String name) {
		long funcId = V8Engine.v8GetFunction(_nativeId, name);
		return V8ScriptFunction.wrapNativeFunction(this, funcId);
	}

	public static V8ScriptContext obtainScriptContext() {
		return new V8ScriptContext();
	}

	private static XulCachedHashMap<String, V8ScriptObject> scriptableClassCache = new XulCachedHashMap<String, V8ScriptObject>();

	static V8ScriptObject getScriptableClass(V8ScriptContext ctx, final XulScriptableObject obj) {
		final IScriptableClass cls = obj.getScriptClass();
		String className = cls.getClassName();
		V8ScriptObject scriptableObject = scriptableClassCache.get(className);

		if (scriptableObject != null) {
			return scriptableObject;
		}

		scriptableObject = new V8ScriptObject(ctx);
		scriptableClassCache.put(className, scriptableObject);

		String[] methods = cls.getMethods();

		for (int i = 0; i < methods.length; i++) {
			final String method = methods[i];
			final XulScriptableObject.XulScriptMethodInvoker methodInvoker = obj.createMethodInvoker(method);

			scriptableObject.addMethod(method, new V8MethodCallback() {
				XulScriptableObject.XulScriptMethodInvoker _methodInvoker = methodInvoker;

				@Override
				public boolean invoke(V8ScriptObject thisObject, V8Arguments args) {
					return _methodInvoker.invoke(thisObject.getObjectValue(), thisObject.getContext(), args);
				}
			});
		}


		String[] properties = cls.getProperties();

		for (int i = 0; i < properties.length; i++) {
			final String property = properties[i];

			V8MethodCallback propertyCallback = new V8MethodCallback() {

				@Override
				public boolean set(V8ScriptObject thisObject, V8Arguments args) {
					V8ScriptObject scriptObject = args.getScriptObject(0);
					if (scriptObject != null) {
						thisObject.getObjectValue().putProperty(thisObject.getContext(), property, scriptObject);
						return true;
					}
					thisObject.getObjectValue().putProperty(thisObject.getContext(), property, args.getString(0));
					return true;
				}

				@Override
				public boolean get(V8ScriptObject thisObject, V8Arguments args) {
					Object val = thisObject.getObjectValue().getProperty(thisObject.getContext(), property);

					if (val == null) {
						return true;
					}
					if (val instanceof V8ScriptObject) {
						args.setResult((V8ScriptObject) val);
						return true;
					}
					if (val instanceof Integer) {
						args.setResult((Integer) val);
						return true;
					}
					if (val instanceof String) {
						args.setResult((String) val);
						return true;
					}
					if (val instanceof Boolean) {
						args.setResult((Boolean) val);
						return true;
					}
					if (val instanceof Double) {
						args.setResult((Double) val);
						return true;
					}
					if (val instanceof Float) {
						args.setResult((Float) val);
						return true;
					}
					if (val instanceof Long) {
						args.setResult((Long) val);
						return true;
					}
					if (val instanceof XulArrayList) {
						args.setResult((XulArrayList) val);
						return true;
					}
					if (val instanceof Object[]) {
						args.setResult((Object[]) val);
						return true;
					}
					if (val instanceof ArrayList) {
						args.setResult((ArrayList) val);
						return true;
					}
					new Exception("unsupported result type! " + val.getClass().getSimpleName()).printStackTrace();
					return false;
				}
			};
			scriptableObject.addProperty(property, propertyCallback);
		}

		return scriptableObject;
	}
}
