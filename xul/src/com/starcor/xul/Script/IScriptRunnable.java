package com.starcor.xul.Script;

/**
* Created by hy on 2014/6/25.
*/
public interface IScriptRunnable {
	Object run(IScriptContext ctx, IScriptableObject ctxObj, Object[] params);
}
