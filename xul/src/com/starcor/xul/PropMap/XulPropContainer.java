package com.starcor.xul.PropMap;

import com.starcor.xul.Prop.XulProp;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Utils.XulSparseArray;
import com.starcor.xul.XulView;

public class XulPropContainer<T extends XulProp> extends XulSparseArray<T> {
	XulPriorityPropMap<T> _priorityProp;
	int _state;
	boolean _enabled = true;

	public XulPropContainer() {
		_priorityProp = new XulPriorityPropMap<T>();
	}

	public void switchState(int state) {
		_state = state;
	}

	public void switchEnabled(boolean enable) {
		_enabled = enable;
	}

	/**
	 * 添加自身属性
	 */
	public void put(T prop) {
		int key = prop.getNameId();
		super.put(key, prop);
	}

	public T getOwnProp(String key) {
		return getOwnProp(XulPropNameCache.name2Id(key));
	}

	public T getOwnProp(int key) {
		return super.get(key);
	}

	/**
	 * 删除自身属性，不影响继承属性
	 */
	public void removeOwnProp(String key) {
		removeOwnProp(XulPropNameCache.name2Id(key));
	}

	public void removeOwnProp(int key) {
		super.remove(key);
	}

	public void removeOwnProp(T prop) {
		for (int i = super.nextIdx(-1); i >= 0; i = super.nextIdx(i)) {
			if (prop == super.get(i)) {
				super.remove(i);
				break;
			}
		}
	}

	public void add(T prop) {
		int key = prop.getNameId();
		_priorityProp.add(key, prop);
	}

	public void add(T prop, int state) {
		int key = prop.getNameId();
		_priorityProp.add(key, prop, state);
	}

	public <T extends XulProp> void each(IXulPropIterator<T> iterator) {
		eachInlineProp(iterator);
		_priorityProp.each(iterator);
	}

	public <T extends XulProp> void eachInlineProp(IXulPropIterator<T> iterator) {
		for (int i = super.nextIdx(-1); i >= 0; i = super.nextIdx(i)) {
			iterator.onProp((T) super.get(i), -1);
		}
	}

	public void remove(T prop) {
		removeOwnProp(prop);
		_priorityProp.remove(prop);
	}

	public void remove(T prop, int state) {
		removeOwnProp(prop);
		_priorityProp.remove(prop, state);
	}

	public static <T extends XulProp> XulPropContainer<T> makeClone(XulPropContainer<T> src) {
		if (src == null) {
			return null;
		}
		XulPropContainer<T> newObj = new XulPropContainer<T>();
		newObj._state = src._state;
		XulPriorityPropMap<T> priorityProp = newObj._priorityProp;
		priorityProp.copy(src._priorityProp);
		for (int i = src.nextIdx(-1); i >= 0; i = src.nextIdx(i)) {
			newObj.put(i, (T) src.get(i).makeClone());
		}
		for (int i = priorityProp.nextIdx(-1); i >= 0; i = priorityProp.nextIdx(i)) {
			final XulPriorityPropMap.MapEntry<T> propEntries = priorityProp.get(i);
			priorityProp.put(i, propEntries.cloneEntry(true));
		}
		return newObj;
	}

	public T get(String key) {
		return get(XulPropNameCache.name2Id(key));
	}

	public T get(int key) {
		return getWithState(key, _state);
	}

	public T getWithState(int key, int state) {
		T t = super.get(key);
		if (t != null) {
			return t;
		}
		if (_enabled) {
			return _priorityProp.getVal(key, state );
		}
		return _priorityProp.getValEx(key, XulView.STATE_DISABLED, state);
	}

	public void destroy() {
		super.clear();
		_priorityProp.clear();
	}
}
