package com.starcor.xul.Utils;

import java.util.ArrayList;

/**
 * Created by hy on 2015/1/20.
 */
public class XulSparseArray<T> implements Cloneable {
	private static final int _CONTAINER_SIZE = 32;
	private static final int _ARRAY_LIMITATION = _CONTAINER_SIZE * _CONTAINER_SIZE;
	private static ArrayList<Object[]> _cachedItem = new ArrayList<Object[]>();

	private static Object[] _allocContainer() {
		if (!_cachedItem.isEmpty()) {
			return _cachedItem.remove(_cachedItem.size() - 1);
		}
		return new Object[_CONTAINER_SIZE];
	}

	private static void _recycleContainer(Object[] c) {
		_cachedItem.add(c);
	}

	private static void _recycleAll(Object[] _obj) {
		if (_obj == null) {
			return;
		}
		for (int i = 0; i < _CONTAINER_SIZE; ++i) {
			Object o = _obj[i];
			if (o == null) {
				continue;
			}
			_obj[i] = null;
			Object[] l2Container = (Object[]) o;
			for (int l2 = 0; l2 < _CONTAINER_SIZE; ++l2) {
				l2Container[l2] = null;
			}
			_recycleContainer(l2Container);

		}
		_recycleContainer(_obj);
	}

	private Object[] _obj;

	public T get(int idx) {
		if (idx < 0 || idx >= _ARRAY_LIMITATION) {
			return null;
		}
		if (_obj == null) {
			return null;
		}
		int l2 = idx % _CONTAINER_SIZE;
		int l1 = idx / _CONTAINER_SIZE;
		Object l2ContainerObj = _obj[l1];
		if (l2ContainerObj == null) {
			return null;
		}
		Object[] l2Container = (Object[]) l2ContainerObj;
		if (l2Container[l2] == null) {
			return null;
		}
		return (T) l2Container[l2];
	}

	public void put(int idx, T v) {
		if (idx < 0 || idx >= _ARRAY_LIMITATION) {
			return;
		}
		if (v == null) {
			remove(idx);
			return;
		}
		int l2 = idx % _CONTAINER_SIZE;
		int l1 = idx / _CONTAINER_SIZE;
		if (_obj == null) {
			_obj = _allocContainer();
		}
		Object l2ContainerObj = _obj[l1];
		if (l2ContainerObj == null) {
			l2ContainerObj = _obj[l1] = _allocContainer();
		}
		Object[] l2Container = (Object[]) l2ContainerObj;
		l2Container[l2] = v;
	}

	public void remove(int idx) {
		if (idx < 0 || idx >= _ARRAY_LIMITATION) {
			return;
		}
		if (_obj == null) {
			return;
		}
		int l2 = idx % _CONTAINER_SIZE;
		int l1 = idx / _CONTAINER_SIZE;

		Object l2ContainerObj = _obj[l1];
		if (l2ContainerObj == null) {
			return;
		}

		Object[] l2Container = (Object[]) l2ContainerObj;
		l2Container[l2] = null;
	}

	/**
	 * 返回下一个非空对象的索引
	 * @param startIdx 开始搜索索引
	 * @return 非空对象的索引
	 */
	public int nextIdx(int startIdx) {
		++startIdx;
		if (startIdx >= _ARRAY_LIMITATION) {
			return -1;
		}
		if (_obj == null) {
			return -1;
		}

		int l2 = startIdx % _CONTAINER_SIZE;
		int l1 = startIdx / _CONTAINER_SIZE;

		Object l2ContainerObj = _obj[l1];
		while (l2ContainerObj == null) {
			++l1;
			if (l1 >= _CONTAINER_SIZE) {
				return -1;
			}
			l2ContainerObj = _obj[l1];
			l2 = 0;
		}

		Object[] l2Container = (Object[]) l2ContainerObj;
		Object obj = l2Container[l2];
		while (obj == null) {
			++l2;
			if (l2 >= _CONTAINER_SIZE) {
				do {
					++l1;
					if (l1 >= _CONTAINER_SIZE) {
						return -1;
					}
					l2ContainerObj = _obj[l1];
					l2 = 0;
				} while (l2ContainerObj == null);
				l2Container = (Object[]) l2ContainerObj;
			}
			obj = l2Container[l2];
		}

		return l1 * _CONTAINER_SIZE + l2;
	}

	public void copy(XulSparseArray<T> src) {
		Object[] srcObj = src._obj;
		if (srcObj == null) {
			return;
		}
		if (_obj == null) {
			_obj = _allocContainer();
		}
		for (int i = 0; i < _CONTAINER_SIZE; ++i) {
			Object o = srcObj[i];
			if (o == null) {
				continue;
			}
			Object[] l2Container = (Object[]) o;
			Object dstL2ContainerObj = _obj[i];
			if (dstL2ContainerObj == null) {
				dstL2ContainerObj = _obj[i] = _allocContainer();
			}
			Object[] dstL2Container = (Object[]) dstL2ContainerObj;
			System.arraycopy(l2Container, 0, dstL2Container, 0, _CONTAINER_SIZE);
		}
	}

	@Override
	public Object clone() {
		XulSparseArray<T> array = new XulSparseArray<T>();
		if (_obj == null) {
			return array;
		}
		array._obj = _allocContainer();
		array.copy(this);
		return array;
	}

	public void clear() {
		_recycleAll(_obj);
		_obj = null;
	}
}
