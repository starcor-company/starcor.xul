package com.starcor.xul.Script;

/**
* Created by hy on 2014/6/25.
*/
public interface IScript {
	String getScriptType();
	Object run(IScriptContext ctx, IScriptableObject ctxObj);
	Object run(IScriptContext ctx, IScriptableObject ctxObj, Object[] args);
}
