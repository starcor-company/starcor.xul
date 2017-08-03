package com.starcor.xul.ScriptWrappr;

import android.util.Pair;
import com.starcor.xul.Script.*;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptGetter;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptMethod;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptSetter;
import com.starcor.xul.Utils.XulCachedHashMap;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by hy on 2014/6/25.
 */
public abstract class XulScriptableObjectWrapper<T> extends XulScriptableObject {
	static class ScriptableClassDesc {
		String[] _scriptMethodNames;
		String[] _scriptPropertyNames;
		Pair<Method, Method>[] _scriptProperties;
		Method[] _scriptMethods;
		XulCachedHashMap<String, Method> _scriptMethodMap;
		XulCachedHashMap<String, Pair<Method, Method>> _scriptPropertyMap;
		Class<?> _cls;
		String _clsName;

		ScriptableClassDesc(Class<?> cls) {
			_cls = cls;
			_clsName = _cls.getSimpleName();
			ArrayList<Method> scriptMethods = new ArrayList<Method>();
			_scriptPropertyMap = new XulCachedHashMap<String, Pair<Method, Method>>();
			Method[] methods = cls.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method m = methods[i];
				m.setAccessible(true);
				ScriptMethod method = m.getAnnotation(ScriptMethod.class);
				if (method != null) {
					scriptMethods.add(m);
					continue;
				}

				ScriptGetter getter = m.getAnnotation(ScriptGetter.class);
				if (getter != null) {
					String propName = getter.value();
					Pair<Method, Method> methodPair = _scriptPropertyMap.get(propName);
					if (methodPair == null) {
						methodPair = Pair.create(m, (Method) null);
					} else {
						methodPair = Pair.create(m, methodPair.second);
					}
					_scriptPropertyMap.put(propName, methodPair);
					continue;
				}

				ScriptSetter setter = m.getAnnotation(ScriptSetter.class);
				if (setter != null) {
					String propName = setter.value();
					Pair<Method, Method> methodPair = _scriptPropertyMap.get(propName);
					if (methodPair == null) {
						methodPair = Pair.create((Method) null, m);
					} else {
						methodPair = Pair.create(methodPair.first, m);
					}
					_scriptPropertyMap.put(propName, methodPair);
					continue;
				}
			}
			_scriptMethods = new Method[scriptMethods.size()];
			_scriptMethodNames = new String[scriptMethods.size()];
			_scriptMethodMap = new XulCachedHashMap<String, Method>();
			scriptMethods.toArray(_scriptMethods);
			for (int i = 0, scriptMethodsLength = _scriptMethods.length; i < scriptMethodsLength; i++) {
				Method m = _scriptMethods[i];
				ScriptMethod annotation = m.getAnnotation(ScriptMethod.class);
				String methodName = annotation.value();
				_scriptMethodNames[i] = methodName;
				_scriptMethodMap.put(methodName, m);
			}

			_scriptPropertyNames = new String[_scriptPropertyMap.size()];
			_scriptPropertyMap.keySet().toArray(_scriptPropertyNames);
			_scriptProperties = new Pair[_scriptPropertyMap.size()];
			for (int i = 0; i < _scriptPropertyNames.length; i++) {
				String propertyName = _scriptPropertyNames[i];
				_scriptProperties[i] = _scriptPropertyMap.get(propertyName);
			}
		}
	}

	static XulCachedHashMap<String, ScriptableClassDesc> scriptableClassMap = new XulCachedHashMap<String, ScriptableClassDesc>();

	ScriptableClassDesc _clsDesc;
	T _xulItem;

	private void _initialize() {
		if (_clsDesc != null) {
			return;
		}
		String clsName = this.getClass().getSimpleName();
		_clsDesc = scriptableClassMap.get(clsName);
		if (_clsDesc == null) {
			_clsDesc = new ScriptableClassDesc(this.getClass());
			scriptableClassMap.put(clsName, _clsDesc);
		}
	}

	public Object getUnwrappedObject() {
		return _xulItem;
	}

	public XulScriptableObjectWrapper(T item) {
		_xulItem = initUnwrappedObject(item);
		_initialize();
	}

	protected T initUnwrappedObject(T item) {
		return item;
	}

	@Override
	public IScriptableClass getScriptClass() {
		return XulScriptableClass.createScriptableClass(_clsDesc._clsName, _clsDesc._scriptPropertyNames, _clsDesc._scriptMethodNames);
	}

	@Override
	public Object getProperty(IScriptContext ctx, String name) {
		Pair<Method, Method> methodMethodPair = _clsDesc._scriptPropertyMap.get(name);
		if (methodMethodPair == null || methodMethodPair.first == null) {
			return null;
		}
		try {
			params1[0] = ctx;
			return methodMethodPair.first.invoke(this, params1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			params1[0] = null;
		}
		return null;
	}

	@Override
	public Object getProperty(IScriptContext ctx, int idx) {
		Pair<Method, Method> methodMethodPair = _clsDesc._scriptProperties[idx];
		if (methodMethodPair == null || methodMethodPair.first == null) {
			return null;
		}
		try {
			params1[0] = ctx;
			return methodMethodPair.first.invoke(this, params1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			params1[0] = null;
		}
		return null;
	}

	@Override
	public Object putProperty(IScriptContext ctx, String name, Object newVal) {
		Pair<Method, Method> methodMethodPair = _clsDesc._scriptPropertyMap.get(name);
		if (methodMethodPair == null || methodMethodPair.second == null) {
			return null;
		}
		try {
			params2[0] = ctx;
			params2[1] = newVal;
			return methodMethodPair.second.invoke(this, params2);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			params2[0] = null;
			params2[1] = null;
		}
		return null;
	}

	@Override
	public Object putProperty(IScriptContext ctx, int idx, Object newVal) {
		Pair<Method, Method> methodMethodPair = _clsDesc._scriptProperties[idx];
		if (methodMethodPair == null || methodMethodPair.second == null) {
			return null;
		}
		try {
			params2[0] = ctx;
			params2[1] = newVal;
			return methodMethodPair.second.invoke(this, params2);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			params2[0] = null;
			params2[1] = null;
		}
		return null;
	}

	@Override
	public XulScriptMethodInvoker createMethodInvoker(String name) {
		Method method = _clsDesc._scriptMethodMap.get(name);
		return createMethodInvoker(method);
	}

	@Override
	public XulScriptMethodInvoker createMethodInvoker(int idx) {
		Method method = _clsDesc._scriptMethods[idx];
		return createMethodInvoker(method);
	}

	private static void setScriptResult(Object result, IScriptArguments args) {
		if (result instanceof IScriptableObject) {
			args.setResult((IScriptableObject) result);
		} else if (result instanceof Collection) {
			args.setResult((Collection) result);
		} else if (result instanceof Object[]) {
			args.setResult((Object[]) result);
		} else if (result instanceof Boolean) {
			args.setResult((Boolean) result);
		} else if (result instanceof String) {
			args.setResult((String) result);
		} else if (result instanceof Integer) {
			args.setResult((Integer) result);
		} else if (result instanceof Float) {
			args.setResult((Float) result);
		} else if (result instanceof Double) {
			args.setResult((Double) result);
		} else if (result instanceof Long) {
			args.setResult((Long) result);
		}
	}

	static Object[] params1 = new Object[1];
	static Object[] params2 = new Object[2];

	private static XulScriptMethodInvoker createMethodInvoker(final Method method) {
		if (method == null) {
			return null;
		}
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes == null || parameterTypes.length == 0) {
			return new XulScriptMethodInvoker() {
				@Override
				public boolean invoke(XulScriptableObject thisObj, IScriptContext ctx, IScriptArguments args) {
					Object result = null;
					try {
						result = method.invoke(thisObj);
					} catch (Exception e) {
						e.printStackTrace();
					}
					setScriptResult(result, args);
					return true;
				}
			};
		} else switch (parameterTypes.length) {
		case 1:
			return new XulScriptMethodInvoker() {
				@Override
				public boolean invoke(XulScriptableObject thisObj, IScriptContext ctx, IScriptArguments args) {
					Object result = null;
					try {
						params1[0] = ctx;
						result = method.invoke(thisObj, params1);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						params1[0] = null;
					}
					setScriptResult(result, args);
					return true;
				}
			};
		default:
			return new XulScriptMethodInvoker() {
				@Override
				public boolean invoke(XulScriptableObject thisObj, IScriptContext ctx, IScriptArguments args) {
					try {
						params2[0] = ctx;
						params2[1] = args;
						method.invoke(thisObj, params2);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						params2[0] = null;
						params2[1] = null;
					}
					return true;
				}
			};
		}
	}
}
