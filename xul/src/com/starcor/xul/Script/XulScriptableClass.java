package com.starcor.xul.Script;

import com.starcor.xul.Utils.XulCachedHashMap;

/**
 * Created by hy on 2014/6/25.
 */
public class XulScriptableClass implements IScriptableClass {
	private final String[] _clsMethods;
	private final String[] _clsProps;
	String _clsName;

	@Override
	public String getClassName() {
		return _clsName;
	}

	@Override
	public String[] getProperties() {
		return _clsProps;
	}

	@Override
	public String[] getMethods() {
		return _clsMethods;
	}

	static XulCachedHashMap<String, XulScriptableClass> classCache = new XulCachedHashMap<String, XulScriptableClass>();

	private XulScriptableClass(String name, String[] props, String[] methods) {
		_clsName = name;
		_clsProps = props;
		_clsMethods = methods;
	}

	static public IScriptableClass createScriptableClass(String name, String[] props, String[] methods) {
		XulScriptableClass cls = classCache.get(name);
		if (cls == null) {
			cls = new XulScriptableClass(name, props, methods);
			classCache.put(name, cls);
		}
		return cls;
	}
}
