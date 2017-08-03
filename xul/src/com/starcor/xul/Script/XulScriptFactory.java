package com.starcor.xul.Script;

import android.text.TextUtils;
import com.starcor.xul.Utils.XulCachedHashMap;

import java.util.List;

/**
 * Created by hy on 2014/6/25.
 */
public class XulScriptFactory {
	public interface IScriptFactory {
		List<String> getSupportScriptTypes();
		IScriptContext createContext();
	}

	private static XulCachedHashMap<String,IScriptFactory> _factories = new XulCachedHashMap<String, IScriptFactory>();

	public static boolean registerFactory(IScriptFactory factory) {
		List<String> supportScriptTypes = factory.getSupportScriptTypes();
		if (supportScriptTypes == null) {
			return false;
		}

		for (int i = 0; i < supportScriptTypes.size(); i++) {
			String scriptType = supportScriptTypes.get(i);
			_factories.put(scriptType, factory);
		}
		return true;
	}

	public static IScriptContext createScriptContext(String type) {
		if (TextUtils.isEmpty(type)) {
			return null;
		}
		IScriptFactory scriptFactory = _factories.get(type);
		if (scriptFactory == null) {
			return null;
		}

		IScriptContext context = scriptFactory.createContext();
		return context;
	}
}
