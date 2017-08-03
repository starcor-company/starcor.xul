package com.starcor.xul.Script;

import com.starcor.xul.Utils.XulArrayList;

import java.util.Collection;

/**
 * Created by hy on 2015/6/21.
 */
public interface IScriptArray {
	void add(int val);

	void add(long val);

	void add(String val);

	void add(float val);

	void add(double val);

	void add(boolean val);

	void add(IScriptableObject val);

	void add(IScriptArray val);

	int getInteger(int idx);

	long getLong(int idx);

	float getFloat(int idx);

	double getDouble(int idx);

	String getString(int idx);

	int getStringId(int idx);

	boolean getBoolean(int idx);

	void addAll(Collection val);

	void addAll(XulArrayList val);

	void addAll(Object[] val);

	int size();
}
