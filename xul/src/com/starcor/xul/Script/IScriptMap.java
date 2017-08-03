package com.starcor.xul.Script;

/**
 * Created by hy on 2014/6/26.
 */
public interface IScriptMap {
	Object mapGet(String name);
	void mapPut(String name, Object val);
}
