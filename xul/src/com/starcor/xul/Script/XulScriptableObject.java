package com.starcor.xul.Script;

/**
* Created by hy on 2014/6/25.
*/
public abstract class XulScriptableObject {
	public abstract IScriptableClass getScriptClass();
	public abstract Object getUnwrappedObject();
	public abstract Object getProperty(IScriptContext ctx, String name);
	public abstract Object getProperty(IScriptContext ctx, int idx);
	public abstract Object putProperty(IScriptContext ctx, String name, Object newVal);
	public abstract Object putProperty(IScriptContext ctx, int idx, Object newVal);
	public abstract XulScriptMethodInvoker createMethodInvoker(String name);
	public abstract XulScriptMethodInvoker createMethodInvoker(int idx);
	public static abstract class XulScriptMethodInvoker {
		public abstract boolean invoke(XulScriptableObject thisObj, IScriptContext ctx, IScriptArguments args);
	}
}
