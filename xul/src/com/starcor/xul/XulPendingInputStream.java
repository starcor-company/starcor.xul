package com.starcor.xul;

import java.io.IOException;
import java.io.InputStream;

/**
* Created by hy on 2014/5/28.
*/
public class XulPendingInputStream extends InputStream {
	volatile InputStream _baseStream = null;
	volatile boolean _isCancelled = false;

	public boolean checkPending() {
		if (_baseStream == null && !_isCancelled) {
			synchronized (this) {
				try {
					this.wait(10 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return _baseStream == null;
		}
		return false;
	}

	public void cancel() {
		_isCancelled = true;
		_baseStream = null;
		synchronized (this) {
			this.notifyAll();
		}
	}

	public void setBaseStream(InputStream stream) {
		_isCancelled = false;
		_baseStream = stream;
		synchronized (this) {
			this.notifyAll();
		}
	}

	public void reload() {
		_isCancelled = false;
		_baseStream = null;
		synchronized (this) {
			this.notifyAll();
		}
	}

	public boolean isReady() {
		return _baseStream != null;
	}

	@Override
	public int available() throws IOException {
		return _baseStream.available();
	}

	@Override
	public void close() throws IOException {
		_baseStream.close();
	}

	@Override
	public void mark(int readlimit) {
		_baseStream.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return _baseStream.markSupported();
	}

	@Override
	public int read() throws IOException {
		return _baseStream.read();
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		return _baseStream.read(buffer);
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		return _baseStream.read(buffer, offset, length);
	}

	@Override
	public synchronized void reset() throws IOException {
		_baseStream.reset();
	}

	@Override
	public long skip(long byteCount) throws IOException {
		return _baseStream.skip(byteCount);
	}
}
