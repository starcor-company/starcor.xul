package com.starcor.xul.Script;

import java.io.InputStream;

/**
* Created by hy on 2014/6/25.
*/
public interface IScriptContext {
	void init();
	String getScriptType();
	IScript compileFunction(String funcBodyText, String sourceName, int sourceLine);
	IScript compileFunction(InputStream funcBodyText, String sourceName, int sourceLine);
	IScript compileScript(String scriptText, String sourceName, int sourceLine);
	IScript compileScript(InputStream scriptText, String sourceName, int sourceLine);
	IScript getFunction(Object scriptableObject, String funcName);
	IScriptableObject createScriptObject(XulScriptableObject obj);
	IScriptMap createScriptMap();
	IScriptArray createScriptArray();
	void destroy();
	boolean addIndexedString(String str, int id);
	boolean delIndexedString(String str);
}
