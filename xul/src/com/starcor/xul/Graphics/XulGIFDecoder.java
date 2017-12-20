package com.starcor.xul.Graphics;

import android.graphics.AvoidXfermode;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Xfermode;

import com.starcor.xul.XulUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by john on 2017/12/15.
 */

public class XulGIFDecoder {
	public static class GIFAnimationRender {
		Bitmap _frameImage;
		int[] _decodeBuffer;
		XulDrawable _xulDrawable;
		Xfermode _colorKeyXferMode;
		GIFFrame[] _gifFrames;
		boolean _isCurrentFrameDecoded;
		long _decodeTime;
		int _currentFrame;
		boolean _noLoop;

		public int getWidth() {
			return _frameImage.getWidth();
		}

		public int getHeight() {
			return _frameImage.getHeight();
		}

		public boolean decodeFrame() {
			if (_isCurrentFrameDecoded) {
				return false;
			}
			int width = getWidth();
			int height = getHeight();
			if (_decodeBuffer == null) {
				_decodeBuffer = new int[width * height];
			}

			GIFFrame frame = _gifFrames[_currentFrame];
			if (frame._cleanBackground || _currentFrame == 0) {
				Arrays.fill(_decodeBuffer, frame._backgroundColor);
			}
			int dataIdx = 0;
			byte[] frameData = frame._data;
			int[] colorTable = frame._colorTable;
			for (int y = frame._y, maxY = frame._y + frame._height; y < maxY; y++) {
				for (int x = frame._x, maxX = frame._x + frame._width; x < maxX; x++) {
					int colorIdx = frameData[dataIdx] & 0xFF;
					if (colorIdx != frame._transparentColorIdx) {
						int color = colorTable[colorIdx];
						_decodeBuffer[x + y * width] = color;
					}
					dataIdx++;
				}
			}
			_frameImage.setPixels(_decodeBuffer, frame._x + frame._y * width, width, frame._x, frame._y, frame._width, frame._height);
			_isCurrentFrameDecoded = true;
			_decodeTime = XulUtils.timestamp();
			return true;
		}

		public boolean nextFrame(long timestamp, float speed) {
			if (!_isCurrentFrameDecoded) {
				return true;
			}

			GIFFrame frame = _gifFrames[_currentFrame];

			if ((timestamp - _decodeTime) * speed >= frame._delay) {
				_currentFrame++;
				if (_currentFrame >= _gifFrames.length) {
					if (_noLoop) {
						_currentFrame = _gifFrames.length - 1;
						return false;
					}
					_currentFrame = 0;
				}
				_isCurrentFrameDecoded = false;
				return true;
			}
			return false;
		}

		public boolean reset() {
			_currentFrame = 0;
			_isCurrentFrameDecoded = false;
			return true;
		}

		public void draw(XulDC dc, int x1, int y1, int w1, int h1, float x2, float y2, float w2, float h2, Paint p) {
			dc.save();
			Xfermode xfermode = p.setXfermode(_colorKeyXferMode);
			if (_xulDrawable == null) {
				_xulDrawable = XulDrawable.fromBitmap(_frameImage, "", "");
			}
			dc.drawBitmap(_xulDrawable, x1, y1, w1, h1, x2, y2, w2, h2, p);
			p.setXfermode(xfermode);
			dc.restore();
		}
	}

	public static class GIFStaticRender {
		Bitmap _frameImage;

		public int getWidth() {
			return _frameImage.getWidth();
		}

		public int getHeight() {
			return _frameImage.getHeight();
		}

		public boolean decodeFrame(GIFFrame frame) {
			int dataIdx = 0;
			byte[] frameData = frame._data;
			int[] colorTable = frame._colorTable;
			for (int y = frame._y, maxY = frame._y + frame._height; y < maxY; y++) {
				for (int x = frame._x, maxX = frame._x + frame._width; x < maxX; x++) {
					int colorIdx = frameData[dataIdx] & 0xFF;
					if (colorIdx != frame._transparentColorIdx) {
						int color = colorTable[colorIdx];
						_frameImage.setPixel(x, y, color);
					}
					dataIdx++;
				}
			}
			return true;
		}


		public XulDrawable extractDrawable(String url, String key) {
			return XulDrawable.fromBitmap(_frameImage, url, key);
		}
	}

	public static GIFAnimationRender createAnimationRenderer(GIFFrame[] gifFrames, boolean noLoop, boolean noTransBkg) {
		GIFAnimationRender render = new GIFAnimationRender();
		GIFFrame gifFrame0 = gifFrames[0];
		render._frameImage = Bitmap.createBitmap(gifFrame0._screenW, gifFrame0._screenH, Bitmap.Config.ARGB_8888);
		render._gifFrames = gifFrames;
		render._isCurrentFrameDecoded = false;
		render._currentFrame = 0;
		render._noLoop = noLoop;
		if (noTransBkg || gifFrame0._backgroundColor == 0xFFFFFF) {
			render._colorKeyXferMode = null;
		} else {
			render._colorKeyXferMode = new AvoidXfermode(gifFrame0._backgroundColor, 0, AvoidXfermode.Mode.AVOID);
		}
		//render._colorKeyXferMode = new PixelXorXfermode(gifFrame0._backgroundColor);
		return render;
	}

	public static GIFStaticRender createStaticRenderer(GIFFrame[] gifFrames) {
		GIFStaticRender render = new GIFStaticRender();
		GIFFrame gifFrame0 = gifFrames[0];
		render._frameImage = Bitmap.createBitmap(gifFrame0._screenW, gifFrame0._screenH, Bitmap.Config.ARGB_8888);
		render.decodeFrame(gifFrame0);
		return render;
	}

	static class GIFHeader {
		byte[] gif = new byte[3];
		byte[] ver = new byte[3];

		// global params
		int width;
		int height;

		boolean hasGlobalColorTable = false;
		boolean sortFlag = false;
		int colorResolution = 0;
		int globalColorTableSize = 0;
		int backgroundColorIdx = 0;
		float pixelAspectRatio = 0;
		int[] globalColorTable;

		/**
		 * <PRE>
		 * Values:
		 * 0 -   No disposal specified. The decoder is not required to take any action.
		 * 1 -   Do not dispose. The graphic is to be left in place.
		 * 2 -   Restore to background color. The area used by the graphic must be restored to the background color.
		 * 3 -   Restore to previous. The decoder is required to restore the area overwritten by the graphic with what
		 * was there prior to rendering the graphic.
		 * 4-7 - To be defined.
		 * </PRE>
		 */
		int disposalMethod;
		boolean userInputFlag;
		boolean transparentColorFlag;
		int delay;
		int transparentColorIdx;
	}

	public static class GIFFrame {
		int _screenW, _screenH;
		int _x, _y, _width, _height;
		int _delay;
		byte[] _data;
		int[] _colorTable;
		int _backgroundColor;
		int _transparentColorIdx;
		boolean _cleanBackground;

		public GIFFrame(int screenW, int screenH, int x, int y, int width, int height, int[] colorTable, int delay, int backgroundColor, int transparentColorIdx, boolean cleanBackground) {
			_screenW = screenW;
			_screenH = screenH;

			if (x + width > screenW) {
				width = screenW - x;
			}
			if (y + height > screenH) {
				height = screenH - y;
			}

			_x = x;
			_y = y;
			_width = width;
			_height = height;
			_delay = delay;
			_colorTable = colorTable;
			_backgroundColor = backgroundColor;
			_transparentColorIdx = transparentColorIdx;
			_data = new byte[width * height];
			_cleanBackground = cleanBackground;
		}

		public void setPixel(int x, int y, int val) {
			if (x >= _screenW || y >= _screenH) {
				return;
			}
			_data[((x - _x) + (y - _y) * _width)] = (byte) (val & 0xFF);
		}
	}

	private static int _readUShort(InputStream is) throws IOException {
		int l = is.read();
		int h = is.read();
		return (h & 0xFF) * 0x100 + (l & 0xFF);
	}

	private static int _readColor(InputStream is) throws IOException {
		int r = is.read();
		int g = is.read();
		int b = is.read();
		return Color.rgb(r, g, b);
	}

	public synchronized static GIFFrame[] decode(InputStream is, boolean noLoop, boolean noTransBkg) {
		GIFHeader hdr = new GIFHeader();
		try {
			if (is.read(hdr.gif) != hdr.gif.length) {
				return null;
			}
			if (is.read(hdr.ver) != hdr.ver.length) {
				return null;
			}

			hdr.width = _readUShort(is);
			hdr.height = _readUShort(is);
			{
				int flags = is.read();
				hdr.hasGlobalColorTable = (flags & 0x80) != 0;
				hdr.colorResolution = (flags & 0x70) >> 4;
				hdr.sortFlag = (flags & 0x08) != 0;
				hdr.globalColorTableSize = 1 << ((flags & 0x7) + 1);
			}
			hdr.backgroundColorIdx = is.read() & 0xFF;
			{
				int ratio = is.read() & 0xFF;
				if (ratio != 0) {
					hdr.pixelAspectRatio = (ratio + 15.0f) / 64.0f;
				} else {
					hdr.pixelAspectRatio = 1;
				}
			}

			if (hdr.hasGlobalColorTable) {
				hdr.globalColorTable = new int[hdr.globalColorTableSize];
				for (int i = 0; i < hdr.globalColorTableSize; i++) {
					int color = _readColor(is) | 0xFF010101;
					hdr.globalColorTable[i] = color;
				}
				if (!noTransBkg) {
					hdr.globalColorTable[hdr.backgroundColorIdx] = 0;
				}
			}
			ArrayList<GIFFrame> frames = new ArrayList<>();

			while (true) {
				int tag = is.read() & 0xFF;
				switch (tag) {
				case 0x21: //Extension Introducer
					switch (is.read() & 0xFF) {
					case 0x01: // text
						_skipBlocks(is);
						break;
					case 0xFE: // comment
						_skipBlocks(is);
						break;
					case 0xFF: // application label
						_skipBlocks(is);
						break;
					case 0xF9: // Graphic Control Label
						// read data
					{
						int blockSize = is.read() & 0xFF;   // should be 4
						int flag = is.read() & 0xFF;
						hdr.disposalMethod = (flag & 0x1C) >> 2;
						hdr.userInputFlag = (flag & 0x02) == 0x02;
						hdr.transparentColorFlag = (flag & 0x01) == 0x01;
						int delay = _readUShort(is) * 10;
						if (delay != 0) {
							hdr.delay = delay; // in ms
						}
						hdr.transparentColorIdx = is.read() & 0xFF;
					}
					_skipBlocks(is);
					break;
					default:
						// FIXME: invalid label
					}
					break;
				case 0x3B: // tail
					// GIF stream terminated
					if (frames.isEmpty()) {
						return null;
					}
					return frames.toArray(new GIFFrame[frames.size()]);
				case 0x2C:  // Image Separator
					int x = _readUShort(is);
					int y = _readUShort(is);
					int width = _readUShort(is);
					int height = _readUShort(is);
					int flag = is.read() & 0xFF;

					boolean hasLocalColorTable = (flag & 0x80) != 0;
					boolean isImageInterlaced = (flag & 0x40) != 0;
					boolean isColorTableSorted = (flag & 0x20) != 0;
					int colorTableSize = 1 << ((flag & 0x07) + 1);
					int[] colorTable = hdr.globalColorTable;

					if (hasLocalColorTable) {
						int[] localColorTable = new int[colorTableSize];
						for (int i = 0; i < colorTableSize; i++) {
							int color = _readColor(is) | 0xFF010101;
							localColorTable[i] = color;
						}
						colorTable = localColorTable;
					}

					// read lzw data
					int lzwCodeSize = is.read() & 0xFF;

					gifLzwReader.init(is, lzwCodeSize);

					try {
						switch (hdr.disposalMethod) {
						case 1:
						case 3:
							break;
						case 2:
							// clear with background
							break;
						default:
						}

						GIFFrame frame = new GIFFrame(hdr.width, hdr.height, x, y, width, height, colorTable, hdr.delay,
							hdr.hasGlobalColorTable ? hdr.globalColorTable[hdr.backgroundColorIdx] : 0xFFFFFF,
							hdr.transparentColorFlag ? hdr.transparentColorIdx : -1,
							hdr.disposalMethod == 2);

						int data;
						int xPos = x, yPos = y;
						do {
							data = gifLzwReader.read();
							if (data < 0) {
								frames.add(frame);
								break;
							}
							frame.setPixel(xPos, yPos, data);
							xPos++;
							if (xPos >= x + width) {
								yPos++;
								xPos = x;
							}
						} while (data >= 0 && y < y + height);

						_skipBlocks(is);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						gifLzwReader.clear();
					}
					break;
				default:
					// FIXME: invalid label
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static void _skipBlocks(InputStream is) throws IOException {
		while (true) {
			int dataBlockSize = is.read() & 0xFF;
			if (dataBlockSize == 0) {
				break;
			}

			is.skip(dataBlockSize);
		}
	}

	static class GIFLZWReader {
		int _codeSize;
		int _curCodeSize;
		int _maxCode;
		int _buffer;
		int _bufferedBits;

		int _clearCode;
		int _blockSize;

		InputStream _is;

		byte[] _dictData = null;
		int _dictDataLen;
		int[] _dictItems = new int[4096];
		int[] _sortedDictPairs = new int[4096];
		int _dictLen;


		int _outputCacheLen;
		int _outputCacheOffset;

		int _lastCodedData;


		public void init(InputStream is, int codeSize) {
			_is = is;
			_codeSize = codeSize;
			_clearCode = 1 << codeSize;
			_blockSize = -1;
			_buffer = 0;
			_bufferedBits = 0;
			_resetDecoderState();
		}

		public void clear() {
			_is = null;
			_codeSize = 0;
			_clearCode = 0;
			_blockSize = -1;
			_buffer = 0;
			_bufferedBits = 0;
			_resetDecoderState();
		}

		public int read() throws IOException {
			if (_outputCacheLen > 0) {
				_outputCacheLen--;
				return _dictData[_outputCacheOffset++] & 0xFF;
			}

			while (true) {
				int data = _getCodedData();

				if (data == _clearCode) {
					_resetDecoderState();
					continue;
				}
				if (data == _clearCode + 1) {
					// TERMINATE!!!
					_resetDecoderState();
					return -1;
				}

				if (_lastCodedData >= 0) {
					_checkAndAddDictItem(_lastCodedData, data);
				}

				_lastCodedData = data;
				if (data < _clearCode) {
					return data;
				}

				int dictItemIdx = data - _clearCode - 2;
				long dictItem = _dictItems[dictItemIdx];

				int offset = (int) ((dictItem >> 0) & 0xFFFFF);
				int len = (int) ((dictItem >> 20) & 0xFFFFF);

				_outputCacheOffset = offset;
				_outputCacheLen = len;
				return read();
			}
		}

		private void _checkAndAddDictItem(int lastCodedData, int data) {
			if (data > _clearCode) {
				int dataItemIdx = data - _clearCode - 2;

				if (dataItemIdx == _dictLen) {
					if (lastCodedData < _clearCode) {
						data = lastCodedData;
					} else {
						int dictItem = _dictItems[lastCodedData - _clearCode - 2];
						data = _dictData[dictItem & 0xFFFFF] & 0xFF;
					}
				} else if (dataItemIdx > _dictLen) {
					// FIXME: incorrect data!!!!
				} else {
					int dictItem = _dictItems[dataItemIdx];
					data = _dictData[dictItem & 0xFFFFF] & 0xFF;
				}
			}

			// int dictPair = ((lastCodedData & 0xFFF) * 0x1000) + (data & 0xFFF);
			// if (Arrays.binarySearch(_sortedDictPairs, 0, _dictLen, dictPair) > 0) {
			// 	// exists! do nothing
			// 	Log.d("GIF", "Exists code pair!!!");
			// 	return;
			// }

			// not exists add new dictItem
			int dictItemIdx = _dictLen;
			int offset = _dictDataLen;

			int dataLen = _putDictData(lastCodedData);
			dataLen += _putDictData(data);

			_dictLen++;
			_dictItems[dictItemIdx] = (offset & 0xFFFFF) | ((dataLen & 0xFFFFF) << 20);

			// _sortedDictPairs[dictItemIdx] = dictPair;
			// Arrays.sort(_sortedDictPairs, 0, _dictLen);
		}

		private int _putDictData(int data) {
			if (data < _clearCode) {
				if (_dictData == null) {
					_dictData = new byte[16384];
				} else if (_dictData.length <= _dictDataLen + 1) {
					int allocLen = ((_dictDataLen + 1) | 0x3FFF) + 1;
					_dictData = Arrays.copyOf(_dictData, allocLen);
				}
				_dictData[_dictDataLen] = (byte) data;
				_dictDataLen++;
				return 1;
			}

			int dataItemIdx = data - _clearCode - 2;
			int dictItem = _dictItems[dataItemIdx];
			int dictItemOffset = (dictItem >> 0) & 0xFFFFF;
			int dictItemLen = (dictItem >> 20) & 0xFFFFF;

			if (_dictData == null) {
				int allocLen = 16384;
				if (dictItemLen >= allocLen) {
					allocLen = (dictItemLen | 0x3FFF) + 1;
				}
				_dictData = new byte[allocLen];
			} else if (_dictData.length < _dictDataLen + dictItemLen) {
				int allocLen = ((_dictDataLen + dictItemLen) | 0x3FFF) + 1;
				_dictData = Arrays.copyOf(_dictData, allocLen);
			}

			System.arraycopy(_dictData, dictItemOffset, _dictData, _dictDataLen, dictItemLen);

			_dictDataLen += dictItemLen;
			return dictItemLen;
		}

		private void _resetDecoderState() {
			if (_codeSize < 3) {
				_curCodeSize = 3;
			} else {
				_curCodeSize = _codeSize + 1;
			}

			_maxCode = 1 << _curCodeSize;
			_dictDataLen = 0;
			_dictLen = 0;
			_outputCacheLen = 0;
			_outputCacheOffset = 0;
			_lastCodedData = -1;
		}

		private int _getCodedData() throws IOException {
			if (_curCodeSize <= 11 && _dictLen + _clearCode + 2 == _maxCode) {
				_curCodeSize++;
				_maxCode = 1 << _curCodeSize;
			}

			while (_bufferedBits < _curCodeSize) {
				if (_blockSize < 0) {
					_blockSize = _is.read() & 0xFF;
				}
				if (_blockSize == 0) {
					return -1;
				}
				int data = (_is.read() & 0xFF) << _bufferedBits;
				_blockSize--;
				if (_blockSize == 0) {
					_blockSize = -1;
				}
				_buffer = data | _buffer;
				_bufferedBits += 8;
			}

			if (_bufferedBits >= _curCodeSize) {
				int data = _buffer & (_maxCode - 1);
				_buffer = _buffer >> _curCodeSize;
				_bufferedBits -= _curCodeSize;
				return data;
			}
			return -1;
		}
	}

	static GIFLZWReader gifLzwReader = new GIFLZWReader();
}
