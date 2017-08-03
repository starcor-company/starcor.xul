package com.starcor.xul.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by hy on 2015/7/7.
 */
public class XulMemoryOutputStream extends OutputStream {
	int _writePos = 0;
	int _maxSize = 0;
	byte[] _dataBuf;
	volatile int _inStreamNum = 0;

	public XulMemoryOutputStream() {
		this(0);
	}

	public XulMemoryOutputStream(int initialCapacity) {
		this._maxSize = initialCapacity;
	}

	public void reset(int capacity) {
		assert _inStreamNum == 0;
		_writePos = 0;
		_enlargeBuffer(capacity);
	}

	public void expand(int capacity) {
		_enlargeBuffer(capacity);
	}

	public byte[] getDataBuffer() {
		return _dataBuf;
	}

	public int getDataSize() {
		return _writePos;
	}

	public void setDataSize(int size) {
		_writePos = size;
	}

	private void _enlargeBuffer(int sz) {
		int newMaxSize = (sz + 0x3FFF) & ~0x3FFF;
		if (newMaxSize > _maxSize) {
			_maxSize = newMaxSize;
		} else {
			newMaxSize = _maxSize;
			if (_dataBuf != null) {
				return;
			}
		}
		byte[] newBuf = new byte[newMaxSize];
		if (_dataBuf != null) {
			System.arraycopy(_dataBuf, 0, newBuf, 0, _writePos);
		}
		_dataBuf = newBuf;
	}

	@Override
	public void write(int oneByte) throws IOException {
		int writePos = _writePos;
		int maxSize = _maxSize;
		if (_dataBuf == null || writePos + 1 >= maxSize) {
			_enlargeBuffer(maxSize + 128);
		}
		_dataBuf[writePos] = (byte) oneByte;
		++writePos;
		_writePos = writePos;
	}

	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		int writePos = _writePos;
		int maxSize = _maxSize;
		int bufferLen = count;
		if (_dataBuf == null || writePos + bufferLen >= maxSize) {
			_enlargeBuffer(maxSize + bufferLen + 32);
		}
		System.arraycopy(buffer, offset, _dataBuf, writePos, bufferLen);
		writePos += bufferLen;
		_writePos = writePos;
	}

	public InputStream toInputStream() {
		return toInputStream(0, _writePos);
	}

	public InputStream toInputStream(int len) {
		return toInputStream(0, len);
	}

	public InputStream toInputStream(int offset, int len) {
		if (offset > _writePos) {
			offset = _writePos;
		}
		if (offset + len > _writePos) {
			len = _writePos - offset;
		}
		final int finalOffset = offset;
		final int finalLen = len;
		InputStream inputStream = new InputStream() {
			@Override
			protected void finalize() throws Throwable {
				close();
				super.finalize();
			}

			int _maxSize = finalOffset + finalLen;
			int _readPos = finalOffset;
			byte[] _dataBuf = XulMemoryOutputStream.this._dataBuf;
			int _markPos = finalOffset;

			@Override
			public int read() throws IOException {
				if (_readPos >= _maxSize) {
					return -1;
				}
				return _dataBuf[_readPos++];
			}

			@Override
			public int read(byte[] buffer, int offset, int length) throws IOException {
				int readPos = _readPos;
				int availableBytes = _maxSize - readPos;
				if (availableBytes <= 0) {
					return -1;
				}
				int readBytes = Math.min(availableBytes, length);
				System.arraycopy(_dataBuf, readPos, buffer, offset, readBytes);
				readPos += readBytes;
				_readPos = readPos;
				return readBytes;
			}

			@Override
			public int available() throws IOException {
				return _maxSize - _readPos;
			}

			@Override
			public synchronized void reset() throws IOException {
				_readPos = _markPos;
				_markPos = finalOffset;
			}

			@Override
			public boolean markSupported() {
				return true;
			}

			@Override
			public void mark(int readlimit) {
				_markPos = _readPos;
			}

			@Override
			public long skip(long byteCount) throws IOException {
				int availableBytes = _maxSize - _readPos;
				byteCount = Math.min(availableBytes, byteCount);
				_readPos += byteCount;
				return byteCount;
			}

			@Override
			public void close() throws IOException {
				if (_dataBuf == null) {
					return;
				}
				_dataBuf = null;
				_readPos = 0;
				_maxSize = 0;
				releaseRef();
			}
		};
		addRef();
		return inputStream;
	}

	private synchronized void addRef() {
		++_inStreamNum;
	}

	private synchronized void releaseRef() {
		if (--_inStreamNum == 0) {
			onClose();
		}
	}

	public void onClose() {

	}
}
