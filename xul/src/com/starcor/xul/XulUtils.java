package com.starcor.xul;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;

import com.starcor.xul.Graphics.XulDC;

import java.nio.Buffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * Created by hy on 2014/5/23.
 */
public class XulUtils {
	static {
		System.loadLibrary("starcor_xul");
	}

	public static final String STR_EMPTY = "";

	private static native boolean _native_do_blur(Buffer buf, int width, int height, int pass);

	private static native boolean _native_do_fast_blur(Buffer buf, int width, int height, int radius);

	public static boolean doBlurOnBuffer(Buffer buf, int width, int height, int pass) {
		return _native_do_fast_blur(buf, width, height, pass * 2);
	}

	public static String getCachedString(String str) {
		return str;
	}

	public static int tryParseInt(String val) {
		return tryParseInt(val, 0);
	}

	public static long tryParseLong(String val) {
		return tryParseLong(val, 0);
	}

	public static int tryParseInt(String val, int defVal) {
		if (TextUtils.isEmpty(val)) {
			return defVal;
		}
		try {
			return Integer.parseInt(val);
		} catch (Exception e) {
		}
		return defVal;
	}

	public static long tryParseLong(String val, long defVal) {
		if (TextUtils.isEmpty(val)) {
			return defVal;
		}
		try {
			return Long.parseLong(val);
		} catch (Exception e) {
		}
		return defVal;
	}

	public static long tryParseHex(String val, long defVal) {
		if (TextUtils.isEmpty(val)) {
			return defVal;
		}
		try {
			return Long.parseLong(val, 16);
		} catch (Exception e) {
		}
		return defVal;
	}

	public static double tryParseDouble(String val, double defVal) {
		if (TextUtils.isEmpty(val)) {
			return defVal;
		}
		try {
			return Double.parseDouble(val);
		} catch (Exception e) {
		}
		return defVal;
	}

	public static float tryParseFloat(String val, float defVal) {
		if (TextUtils.isEmpty(val)) {
			return defVal;
		}
		try {
			return Float.parseFloat(val);
		} catch (Exception e) {
		}
		return defVal;
	}

	public static int roundToInt(double val) {
		if (val >= 0) {
			return (int) (val + 0.5);
		} else {
			return (int) (val - 0.5);
		}
	}

	public static int roundToInt(float val) {
		if (val >= 0) {
			return (int) (val + 0.5f);
		} else {
			return (int) (val - 0.5f);
		}
	}

	public static void copyRect(Rect src, Rect dst) {
		dst.left = src.left;
		dst.top = src.top;
		dst.right = src.right;
		dst.bottom = src.bottom;
	}

	public static void copyRect(Rect src, RectF dst) {
		dst.left = src.left;
		dst.top = src.top;
		dst.right = src.right;
		dst.bottom = src.bottom;
	}

	public static void copyRect(RectF src, RectF dst) {
		dst.left = src.left;
		dst.top = src.top;
		dst.right = src.right;
		dst.bottom = src.bottom;
	}

	public static void copyRect(RectF src, Rect dst) {
		dst.left = roundToInt(src.left);
		dst.top = roundToInt(src.top);
		dst.right = roundToInt(src.right);
		dst.bottom = roundToInt(src.bottom);
	}

	public static void offsetRect(Rect rc, int offX, int offY) {
		rc.left += offX;
		rc.right += offX;
		rc.top += offY;
		rc.bottom += offY;
	}

	public static void offsetRect(RectF rc, float offX, float offY) {
		rc.left += offX;
		rc.right += offX;
		rc.top += offY;
		rc.bottom += offY;
	}

	public static boolean intersects(Rect a, Rect b) {
		return a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom;
	}

	public static boolean intersects(RectF a, RectF b) {
		return a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom;
	}

	public static int calRectWidth(Rect rc) {
		return rc.right - rc.left;
	}

	public static float calRectWidth(RectF rc) {
		return rc.right - rc.left;
	}

	public static int calRectHeight(Rect rc) {
		return rc.bottom - rc.top;
	}

	public static float calRectHeight(RectF rc) {
		return rc.bottom - rc.top;
	}

	public static int ceilToInt(float v) {
		return (int) Math.ceil(v);
	}

	public static long roundToLong(double val) {
		return Math.round(val);
	}

	public static long timestamp() {
		return System.nanoTime() / 1000000;
	}

	public static long timestamp_us() {
		return System.nanoTime() / 1000;
	}

	public static String calMD5(String imageKey) {
		String localCacheKey = calMD5(imageKey.getBytes());
		if (localCacheKey == null) {
			return imageKey;
		}
		return localCacheKey;
	}

	private static char hexCharMap[] = new char[]{
		'0', '1', '2', '3',
		'4', '5', '6', '7',
		'8', '9', 'A', 'B',
		'C', 'D', 'E', 'F',
	};

	public static boolean isEqual(float v1, float v2) {
		return Math.abs(v1 - v2) < 0.00001f;
	}

	public static void resizeRect(Rect rect, int newWidth, int newHeight) {
		rect.right = rect.left + newWidth;
		rect.bottom = rect.top + newHeight;
	}

	public static String join(String sep, String... vals) {
		return TextUtils.join(sep, vals);
	}

	public static String join(String sep, Iterable vals) {
		return TextUtils.join(sep, vals);
	}

	private static class MessageDigestCtx {
		MessageDigest digest;
		char[] digestStr = new char[32];

		public MessageDigestCtx(MessageDigest digest) {
			this.digest = digest;
		}

		public void reset() {
			digest.reset();
		}

		public char[] digest(byte[] data) {
			byte[] digestVal = digest.digest(data);
			for (int i = 0; i < 16; ++i) {
				int b = digestVal[i] & 0xFF;
				digestStr[i * 2 + 0] = hexCharMap[b / 16];
				digestStr[i * 2 + 1] = hexCharMap[b % 16];
			}
			return digestStr;
		}
	}

	private static final WeakHashMap<Thread, MessageDigestCtx> _threadHashMap = new WeakHashMap<Thread, MessageDigestCtx>();

	private static MessageDigestCtx getMD5() {
		synchronized (_threadHashMap) {
			Thread thread = Thread.currentThread();
			MessageDigestCtx messageDigest = _threadHashMap.get(thread);
			if (messageDigest == null) {
				try {
					MessageDigest md5 = MessageDigest.getInstance("md5");
					MessageDigestCtx digestCtx = new MessageDigestCtx(md5);
					_threadHashMap.put(thread, digestCtx);
					return digestCtx;
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
					return null;
				}
			}
			messageDigest.reset();
			return messageDigest;
		}
	}

	public static String calMD5(byte[] data) {
		MessageDigestCtx md5 = getMD5();
		return String.valueOf(md5.digest(data));
	}

	public static class ticketMarker {
		static final int _MarkersLimitation = 64;
		long[] _t = new long[_MarkersLimitation];
		String[] _n = new String[_MarkersLimitation];
		String _infoHdr;
		int _pos = 0;
		int _precision = 1000;

		public ticketMarker() {
			_infoHdr = "(ms) ";
		}

		public ticketMarker(String infoHdr, boolean ms) {
			if (infoHdr == null) {
				infoHdr = "";
			}
			if (ms) {
				_infoHdr = infoHdr + "(ms) ";
				_precision = 1000;
			} else {
				_infoHdr = infoHdr + "(us) ";
				_precision = 1;
			}
		}

		public void reset() {
			_pos = 0;
		}

		public void mark() {
			mark(null);
		}

		public void mark(String name) {
			if (name == null) {
				name = String.valueOf(_pos);
			}
			_n[_pos] = name;
			_t[_pos] = timestamp_us();
			++_pos;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_infoHdr);
			sb.append((_t[_pos - 1] - _t[0]) / _precision);
			sb.append(" - ");
			for (int i = 1; i < _pos; i++) {
				if (i > 1) {
					sb.append(", ");
				}
				sb.append(_n[i]);
				sb.append(":");
				sb.append((_t[i] - _t[i - 1]) / _precision);
			}
			return sb.toString();
		}
	}


	static class CanvasSaveInfo {
		Exception stackInfo;
		Exception restoreInfo;
		Canvas canvas;
		XulDC dc;

		public CanvasSaveInfo(Canvas canvas, XulDC dc) {
			this.canvas = canvas;
			this.dc = dc;
			this.stackInfo = new Exception();
		}
	}

	static WeakHashMap<Canvas, ArrayList<CanvasSaveInfo>> _saveCanvasStack;
	static WeakHashMap<Canvas, ArrayList<CanvasSaveInfo>> _saveCanvasStackFull;

	private static void _saveInfo(Canvas canvas) {
		CanvasSaveInfo canvasSaveInfo = new CanvasSaveInfo(canvas, null);
		_doSaveInfo(_saveCanvasStack, canvasSaveInfo);
		_doSaveInfo(_saveCanvasStackFull, canvasSaveInfo);
	}

	private static void _saveInfo(Canvas canvas, XulDC dc) {
		CanvasSaveInfo canvasSaveInfo = new CanvasSaveInfo(canvas, dc);
		_doSaveInfo(_saveCanvasStack, canvasSaveInfo);
		_doSaveInfo(_saveCanvasStackFull, canvasSaveInfo);
	}

	private static void _doSaveInfo(WeakHashMap<Canvas, ArrayList<CanvasSaveInfo>> stack, CanvasSaveInfo info) {
		if (stack != null) {
			ArrayList<CanvasSaveInfo> canvasSaveInfos = stack.get(info.canvas);
			if (canvasSaveInfos == null) {
				canvasSaveInfos = new ArrayList<CanvasSaveInfo>();
				stack.put(info.canvas, canvasSaveInfos);
			}
			canvasSaveInfos.add(info);
		}
	}

	private static void _dumpFullCanvasStack(Canvas canvas) {
		ArrayList<CanvasSaveInfo> canvasSaveInfos = _saveCanvasStackFull.get(canvas);
		for (CanvasSaveInfo canvasSaveInfo : canvasSaveInfos) {
			{
				StackTraceElement[] stackTrace = canvasSaveInfo.stackInfo.getStackTrace();
				Log.e("canvas", "save at: " + canvasSaveInfo.canvas);
				for (StackTraceElement stackTraceElement : stackTrace) {
					Log.e("canvas", String.format(">  %s", stackTraceElement.toString()));
				}
			}
			if (canvasSaveInfo.restoreInfo != null) {
				StackTraceElement[] stackTrace = canvasSaveInfo.restoreInfo.getStackTrace();
				Log.e("canvas", "restore at: " + canvasSaveInfo.canvas);
				for (StackTraceElement stackTraceElement : stackTrace) {
					Log.e("canvas", String.format(">    %s", stackTraceElement.toString()));
				}
			}
			Log.e("canvas", "--------------------------------------");
		}
		canvasSaveInfos.clear();
	}

	static {
		if (XulManager.DEBUG_CANVAS_SAVE_RESTORE) {
			_saveCanvasStack = new WeakHashMap<Canvas, ArrayList<CanvasSaveInfo>>();
			_saveCanvasStackFull = new WeakHashMap<Canvas, ArrayList<CanvasSaveInfo>>();
		}
	}

	public static void saveCanvas(Canvas canvas) {
		if (_saveCanvasStack != null) {
			_saveInfo(canvas);
		}
		canvas.save();
	}

	public static void saveCanvas(XulDC dc) {
		Canvas canvas = dc.getCanvas();
		if (_saveCanvasStack != null) {
			_saveInfo(canvas, dc);
		}
		canvas.save();
	}

	public static void saveCanvas(Canvas canvas, int flags) {
		if (_saveCanvasStack != null) {
			_saveInfo(canvas);
		}
		canvas.save(flags);
	}

	public static void saveCanvasLayer(Canvas canvas, RectF bounds, Paint paint, int flags) {
		if (_saveCanvasStack != null) {
			_saveInfo(canvas);
		}
		canvas.saveLayer(bounds, paint, flags);
	}

	public static void saveCanvasLayer(Canvas canvas, int x, int y, int width, int height, Paint paint, int flags) {
		if (_saveCanvasStack != null) {
			_saveInfo(canvas);
		}
		canvas.saveLayer(x, y, width, height, paint, flags);
	}

	public static void saveCanvas(XulDC dc, int flags) {
		Canvas canvas = dc.getCanvas();
		if (_saveCanvasStack != null) {
			_saveInfo(canvas, dc);
		}
		canvas.save(flags);
	}

	public static void restoreCanvas(Canvas canvas) {
		if (_saveCanvasStack != null) {
			ArrayList<CanvasSaveInfo> canvasSaveInfos = _saveCanvasStack.get(canvas);
			if (canvasSaveInfos == null || canvasSaveInfos.size() == 0) {
				Log.e("canvas", "restore canvas error 0 !!!");
				Log.e("canvas", "restore-----------------------------------");
				new Exception().printStackTrace();
				Log.e("canvas", "end---------------------------------------");
				_dumpFullCanvasStack(canvas);
				return;
			}
			int lastIdx = canvasSaveInfos.size() - 1;
			CanvasSaveInfo info = canvasSaveInfos.remove(lastIdx);
			info.restoreInfo = new Exception();
			if (!info.canvas.equals(canvas)) {
				Log.e("canvas", "restore canvas error 1 !!!");
				Log.e("canvas", "save-----------------------------------");
				info.stackInfo.printStackTrace();
				Log.e("canvas", "restore-----------------------------------");
				new Exception().printStackTrace();
				Log.e("canvas", "end---------------------------------------");
				_dumpFullCanvasStack(canvas);
			}
			try {
				canvas.restore();
			} catch (Exception e) {
				Log.e("canvas", "restore canvas error 2 !!!");
				Log.e("canvas", "save-----------------------------------");
				info.stackInfo.printStackTrace();
				Log.e("canvas", "restore-----------------------------------");
				e.printStackTrace();
				Log.e("canvas", "end---------------------------------------");

				_dumpFullCanvasStack(canvas);
			}
			if (canvasSaveInfos.isEmpty()) {
				_saveCanvasStackFull.get(canvas).clear();
			}
		} else {
			canvas.restore();
		}
	}
}
