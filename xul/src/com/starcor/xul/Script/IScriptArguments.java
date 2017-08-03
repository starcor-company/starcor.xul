package com.starcor.xul.Script;

import com.starcor.xul.Utils.XulArrayList;

import java.util.Collection;

/**
 * Created by hy on 2015/6/19.
 */
public interface IScriptArguments {
	void setResult(int val);

	void setResult(long val);

	void setResult(String val);

	void setResult(float val);

	void setResult(double val);

	void setResult(boolean val);

	void setResult(Collection objects);

	void setResult(XulArrayList objects);

	void setResult(Object[] objects);

	void setResult(IScriptableObject val);

	void setResult(IScriptArray val);

	int getInteger(int idx);

	long getLong(int idx);

	float getFloat(int idx);

	double getDouble(int idx);

	String getString(int idx);

	int getStringId(int idx);

	boolean getBoolean(int idx);

	IScriptableObject getScriptableObject(int idx);

	int size();
}
