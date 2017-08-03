package com.starcor.xul.Utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * Created by hy on 2015/6/1.
 */
public class XulCircleQueue<E> implements Queue<E> {
	E[] _data;
	int _begin = 0;
	int _end = 0;

	void _enlargeBuffer(int space) {
		if (_end >= _begin) {
			final int length = _end - _begin;
			if (_data.length - 1 - length > space) {
				return;
			}
			int newSize = _data.length + roundUpToPowerOfTwo(space + 15);
			E[] newBuf = (E[]) new Object[newSize];

			System.arraycopy(_data, _begin, newBuf, 0, length);
			_begin = 0;
			_end = length;
			_data = newBuf;
		} else {
			int length = _data.length - _begin + _end;
			if (_data.length - 1 - length > space) {
				return;
			}
			int newSize = _data.length + roundUpToPowerOfTwo(space + 15);
			E[] newBuf = (E[]) new Object[newSize];

			System.arraycopy(_data, _begin, newBuf, 0, _data.length - _begin);
			System.arraycopy(_data, 0, newBuf, _data.length - _begin, _end);

			_begin = 0;
			_end = length;
			_data = newBuf;
		}

	}

	public XulCircleQueue() {
		this(32);
	}

	public XulCircleQueue(int capacity) {
		_data = (E[]) new Object[capacity];
	}

	private void _internalAdd(E e) {
		_data[_end] = e;
		_end++;
		if (_end >= _data.length) {
			_end = 0;
		}
	}

	@Override
	public boolean add(E e) {
		_enlargeBuffer(1);
		_internalAdd(e);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		_enlargeBuffer(collection.size());
		for (E e : collection) {
			_internalAdd(e);
		}
		return true;
	}

	@Override
	public void clear() {
		Arrays.fill(_data, null);
		_begin = 0;
		_end = 0;
	}

	@Override
	public boolean contains(Object object) {
		for (int i = _begin; i != _end; ) {
			if (testEquals(_data[i], object)) {
				return true;
			}
			++i;
			if (i == _data.length) {
				i = 0;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		for (Object o : collection) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return _end == _begin;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			int _pos = _begin;

			@Override
			public boolean hasNext() {
				return _pos != _end;
			}

			@Override
			public E next() {
				return _data[_pos++];
			}

			@Override
			public void remove() {
				XulCircleQueue.this.remove(_data[_pos - 1]);
			}
		};
	}

	@Override
	public boolean remove(Object object) {
		int beginPos = _begin;
		int endPos = _end;
		int writePos = beginPos;
		E[] dataBuffer = _data;
		int bufferSize = dataBuffer.length;

		for (int readPos = beginPos; readPos != endPos; ) {
			final E e = dataBuffer[readPos];
			if (testEquals(object, e)) {
				++readPos;
				if (readPos == bufferSize) {
					readPos = 0;
				}
				continue;
			}

			if (writePos != readPos) {
				dataBuffer[writePos] = dataBuffer[readPos];
			}

			++readPos;
			if (readPos == bufferSize) {
				readPos = 0;
			}

			++writePos;
			if (writePos == bufferSize) {
				writePos = 0;
			}
		}

		for (int i = writePos; i != endPos; ) {
			dataBuffer[i] = null;
			++i;
			if (i == bufferSize) {
				i = 0;
			}
		}
		_end = writePos;
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		_eliminateCollection(collection, false);
		return true;
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		_eliminateCollection(collection, true);
		return true;
	}

	private void _eliminateCollection(Collection<?> collection, boolean inclusive) {
		int beginPos = _begin;
		int endPos = _end;
		int writePos = beginPos;
		E[] dataBuffer = _data;
		int bufferSize = dataBuffer.length;

		for (int readPos = beginPos; readPos != endPos; ) {
			final E e = dataBuffer[readPos];

			if (collection.contains(e) != inclusive) {
				++readPos;
				if (readPos == bufferSize) {
					readPos = 0;
				}
				continue;
			}

			if (writePos != readPos) {
				dataBuffer[writePos] = dataBuffer[readPos];
			}

			++readPos;
			if (readPos == bufferSize) {
				readPos = 0;
			}
			++writePos;
			if (writePos == bufferSize) {
				writePos = 0;
			}
		}

		for (int i = writePos; i != endPos; ) {
			dataBuffer[i] = null;
			++i;
			if (i == bufferSize) {
				i = 0;
			}
		}
		_end = writePos;
	}

	@Override
	public int size() {
		if (_end >= _begin) {
			return _end - _begin;
		}
		return _data.length - _begin + _end;
	}

	@Override
	public Object[] toArray() {
		final Object[] objects = new Object[size()];
		int bufferSize = _data.length;
		for (int i = _begin, wpos = 0; i < _end; ) {
			objects[wpos] = _data[i];

			++i;
			if (i == bufferSize) {
				i = 0;
			}
			++wpos;
		}
		return objects;
	}

	@Override
	public <T> T[] toArray(T[] array) {
		assert array.length >= size();
		int bufferSize = _data.length;
		for (int i = _begin, wpos = 0; i < _end; ) {
			array[wpos] = (T) _data[i];
			++i;
			if (i == bufferSize) {
				i = 0;
			}
			++wpos;
		}
		return array;
	}

	@Override
	public boolean offer(E e) {
		return false;
	}

	@Override
	public E remove() {
		return poll();
	}

	@Override
	public E poll() {
		if (_end == _begin) {
			return null;
		}
		final E e = _data[_begin];
		_data[_begin] = null;
		++_begin;
		if (_begin >= _data.length) {
			_begin = 0;
		}
		if (_end == _begin) {
			_begin = _end = 0;
		}
		return e;
	}

	@Override
	public E element() {
		return peek();
	}

	@Override
	public E peek() {
		if (_end == _begin) {
			return null;
		}
		return _data[_begin];
	}

	private static <K, V> boolean testEquals(K v1, V v2) {
		return v1 == v2 || (v1 != null && v1.equals(v2));
	}

	private static int roundUpToPowerOfTwo(int v) {
		int newVal = 1;
		while (v > 1) {
			int rm = 2;
			if (v >= 0x1000) {
				rm = 0x1000;
			} else if (v >= 0x10) {
				rm = 0x10;
			}
			newVal *= rm;
			if (v % rm > 0) {
				v /= rm;
				++v;
			} else {
				v /= rm;
			}
		}
		return newVal;
	}
}
