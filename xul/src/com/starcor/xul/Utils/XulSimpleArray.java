package com.starcor.xul.Utils;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by hy on 2015/6/12.
 */

public abstract class XulSimpleArray<T> {
	T[] _buf;
	int _maximumSize;
	int _num;

	public XulSimpleArray(int size) {
		_buf = allocArrayBuf(size);
		_maximumSize = size;
		_num = 0;
	}

	public XulSimpleArray() {
		_buf = null;
		_maximumSize = 0;
		_num = 0;
	}

	protected abstract T[] allocArrayBuf(int size);

	public void push(T val) {
		if (_num < _maximumSize) {
			_buf[_num] = val;
			++_num;
			return;
		}

		_enlargeBuf(getDelta(1));
		_buf[_num] = val;
		++_num;
	}

	public void add(T val) {
		if (_num < _maximumSize) {
			_buf[_num] = val;
			++_num;
			return;
		}

		_enlargeBuf(getDelta(1));
		_buf[_num] = val;
		++_num;
	}

	public void put(int pos, T val) {
		_buf[pos] = val;
	}

	public void add(int pos, T val) {
		if (pos >= _num || pos < 0) {
			push(val);
			return;
		}
		if (_num < _maximumSize) {
			for (int i = _num; i > pos; --i) {
				_buf[i] = _buf[i - 1];
			}
			_buf[pos] = val;
			++_num;
			return;
		}

		_maximumSize += getDelta(1);
		T[] newBuf = allocArrayBuf(_maximumSize);
		if (_buf != null) {
			System.arraycopy(_buf, 0, newBuf, 0, pos);
			System.arraycopy(_buf, pos, newBuf, pos + 1, _num - pos);
		}
		_buf = newBuf;
		_buf[pos] = val;
		++_num;
	}

	private void _enlargeBuf(int delta) {
		_maximumSize += delta;
		T[] newBuf = allocArrayBuf(_maximumSize);
		if (_buf != null) {
			System.arraycopy(_buf, 0, newBuf, 0, _num);
		}
		_buf = newBuf;
	}

	public T get(int idx) {
		return _buf[idx];
	}

	public int size() {
		return _num;
	}

	public void resize(int sz) {
		if (sz > _maximumSize) {
			_enlargeBuf(getDelta(sz - _maximumSize));
		}
		if (sz < _num) {
			Arrays.fill(_buf, sz, _num, null);
		} else if (sz > _num) {
			Arrays.fill(_buf, _num, sz, null);
		}
		_num = sz;
	}

	public boolean isEmpty() {
		return _num <= 0;
	}

	public void clear() {
		if (_num == 0) {
			return;
		}
		Arrays.fill(_buf, 0, _num, null);
		_num = 0;
	}

	public T pop() {
		if (_num <= 0) {
			return null;
		}
		--_num;
		T obj = _buf[_num];
		_buf[_num] = null;
		return obj;
	}

	public void addAll(XulSimpleArray<T> baseSeq) {
		if (_num + baseSeq._num >= _maximumSize) {
			_enlargeBuf(getDelta((_num + baseSeq._num) - _maximumSize));
		}
		System.arraycopy(baseSeq._buf, 0, _buf, _num, baseSeq._num);
		_num += baseSeq._num;
	}

	private static int getDelta(int delta) {
		return (delta + 31) & ~0x1F;
	}

	public boolean contains(T obj) {
		for (int i = 0; i < _num; i++) {
			T t = _buf[i];
			if (t == obj) {
				return true;
			}
			if (t == null) {
				continue;
			}
			if (t.equals(obj)) {
				return true;
			}
		}
		return false;
	}

	public int indexOf(T obj) {
		for (int i = 0; i < _num; i++) {
			T t = _buf[i];
			if (t.equals(obj)) {
				return i;
			}
		}
		return -1;
	}

	public int lastIndexOf(T obj) {
		for (int i = _num - 1; i >= 0; --i) {
			T t = _buf[i];
			if (t.equals(obj)) {
				return i;
			}
		}
		return -1;
	}

	public T remove(int index) {
		int s = _num;
		if (index >= s) {
			return null;
		}
		T[] a = _buf;
		T result = a[index];
		System.arraycopy(a, index + 1, a, index, --s - index);
		a[s] = null;  // Prevent memory leak
		_num = s;
		return result;
	}

	public void remove(int begin, int end) {
		int s = _num;
		if (begin >= s) {
			return;
		}
		if (end >= s) {
			end = s;
		}

		T[] a = _buf;
		int removedItemNum = end - begin;
		System.arraycopy(a, end, a, begin, s - end);
		Arrays.fill(a, s - removedItemNum, s, null); // Prevent memory leak
		_num = s - removedItemNum;
		return;
	}

	public boolean remove(T object) {
		int s = _num;
		T[] a = _buf;
		if (object != null) {
			for (int i = 0; i < s; i++) {
				if (object.equals(a[i])) {
					System.arraycopy(a, i + 1, a, i, --s - i);
					a[s] = null;  // Prevent memory leak
					_num = s;
					return true;
				}
			}
		} else {
			for (int i = 0; i < s; i++) {
				if (a[i] == null) {
					System.arraycopy(a, i + 1, a, i, --s - i);
					a[s] = null;  // Prevent memory leak
					_num = s;
					return true;
				}
			}
		}
		return false;
	}

	public T[] getArray() {
		return _buf;
	}

	public void removeAll(Collection<? extends T> objs) {
		for (T obj : objs) {
			remove(obj);
		}
	}
}
