package com.starcor.xul.Utils;

import java.util.Arrays;

/**
 * Created by hy on 2015/6/12.
 */

public class XulIntArray {
	int[] _buf;
	int _maximumSize;
	int _num;

	public XulIntArray(int size) {
		this._buf = new int[size];
		_maximumSize = size;
		_num = 0;
	}

	public void push(int val) {
		add(val);
	}

	public void add(int val) {
		if (_num >= _maximumSize) {
			_enlargeBuf(128);
		}

		_buf[_num] = val;
		++_num;
	}

	private void _enlargeBuf(int delta) {
		_maximumSize += delta;
		int[] newStack = new int[_maximumSize];
		System.arraycopy(_buf, 0, newStack, 0, _num);
		_buf = newStack;
	}

	public int get(int idx) {
		if (idx >= _num) {
			return -1;
		}
		return _buf[idx];
	}

	public int size() {
		return _num;
	}

	public void clear() {
		if (_num == 0) {
			return;
		}
		Arrays.fill(_buf, 0, _num, -1);
		_num = 0;
	}

	public int pop() {
		if (_num <= 0) {
			return -1;
		}
		--_num;
		int obj = _buf[_num];
		_buf[_num] = -1;
		return obj;
	}

	public void addAll(XulIntArray baseSeq) {
		if (_num + baseSeq._num >= _maximumSize) {
			_enlargeBuf(((_num + baseSeq._num) - _maximumSize + 31) & ~0x1F);
		}
		System.arraycopy(baseSeq._buf, 0, _buf, _num, baseSeq._num);
		_num += baseSeq._num;
	}
}
