package com.starcor.xul.Graphics;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;

import com.starcor.xul.Utils.XulBufferedInputStream;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.graphics.Bitmap.Config.*;

/**
 * 关于bitmap的处理，至少需要提供从路径到文件等函数
 *
 * @author hy
 *         2013/6/23
 */
public class BitmapTools {
	private static final String TAG = "BitmapTools";
	private static final Config defaultConfig = ARGB_8888;
	public static final int BITMAP_LIFETIME = 5000;
	public static final String TAG_RECYCLER = "Xul BMP Recycler";
	private static ThreadLocal<byte[]> _local_buf;
	private static ThreadLocal<XulBufferedInputStream> _localBufferedInputStream;

	static {
		_local_buf = new ThreadLocal<byte[]>();
		_localBufferedInputStream = new ThreadLocal<XulBufferedInputStream>();
	}

	private static final ArrayList<Pair<Long, Bitmap>> _recycledBitmapQueue = new ArrayList<Pair<Long, Bitmap>>();
	private static final ArrayList<Pair<Long, WeakReference<Bitmap>>> _weakGCBitmapQueue = new ArrayList<Pair<Long, WeakReference<Bitmap>>>();

	public static class ReuseStatisticInfo {
		public final int width;
		public final int height;
		public final Bitmap.Config config;
		long dropped;
		long reused;
		long recycled;

		public ReuseStatisticInfo(int r, int w, int h, Config cfg) {
			recycled = r;
			width = w;
			height = h;
			config = cfg;
		}

		public float reusePercent() {
			return 100.0f * reused / recycled;
		}

		public float dropPercent() {
			return 100.0f * dropped / recycled;
		}

		public long recycled() {
			return recycled;
		}
	}

	private static final HashMap<Long, ReuseStatisticInfo> _reuseStatistic = new HashMap<Long, ReuseStatisticInfo>();

	private static int _totalBitmapCacheSize = 0;
	private static int _maxBitmapCacheSize = 24 * 1024 * 1024;
	private static int _longLIfeBitmapCacheSize = 8 * 1024 * 1024;
	private static final int _maximumSameDimension = 64;

	private static Method getAllocationByteCountMethod = null;
	private static Method reconfigMethod = null;

	private static volatile boolean _reuseBitmap = true;
	private static volatile long _recycleCount = 0;
	private static volatile long _reuseCount = 0;
	private static volatile long _newCount = 0;
	private static volatile long _reuseGCCount = 0;

	public static final int MINIMUM_API_LEVEL = 19;

	static {
		if (Build.VERSION.SDK_INT >= MINIMUM_API_LEVEL) {
			try {
				getAllocationByteCountMethod = Bitmap.class.getMethod("getAllocationByteCount");
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				reconfigMethod = Bitmap.class.getMethod("reconfigure", int.class, int.class, Config.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void recordReuseSuccess(int width, int height, Config config) {
		Long key = Long.valueOf(width * 0x8000 + height * 0x20 + config.ordinal());
		ReuseStatisticInfo record = _reuseStatistic.get(key);
		if (record == null) {
			return;
		}
		++record.reused;
	}

	private static void recordRecycled(int width, int height, Config config) {
		Long key = Long.valueOf(width * 0x8000 + height * 0x20 + config.ordinal());
		ReuseStatisticInfo record = _reuseStatistic.get(key);
		if (record == null) {
			_reuseStatistic.put(key, new ReuseStatisticInfo(1, width, height, config));
		} else {
			++record.recycled;
		}
	}

	private static void recordDropCacheItem(int width, int height, Config config) {
		Long key = Long.valueOf(width * 0x8000 + height * 0x20 + config.ordinal());
		ReuseStatisticInfo record = _reuseStatistic.get(key);
		if (record == null) {
			return;
		}
		++record.dropped;
	}


	/**
	 * 根据丢弃记录修正同尺寸元素的重用队列长度
	 * 如果重用比例越小, 队列长度越小
	 */
	private static int fixMaximumSameDimension(Bitmap bmp, int maximumSameDimension) {
		Long key = Long.valueOf(bmp.getWidth() * 0x8000 + bmp.getHeight() * 0x20 + bmp.getConfig().ordinal());
		ReuseStatisticInfo record = _reuseStatistic.get(key);
		if (record == null || record.recycled < 5) {
			return maximumSameDimension;
		}

		if (record.dropPercent() < 5) {
			maximumSameDimension *= 1.2;
		} else if (record.dropPercent() > 20) {
			maximumSameDimension *= 0.8;
		} else if (record.dropPercent() > 50) {
			maximumSameDimension *= 0.5;
		} else if (record.dropPercent() > 80) {
			maximumSameDimension *= 0.2;
		} else if (record.dropPercent() > 90) {
			maximumSameDimension *= 0.1;
		} else if (record.dropPercent() > 95) {
			maximumSameDimension = 1;
		}
		maximumSameDimension *= (record.reusePercent() + 0.1);
		if (maximumSameDimension < 1) {
			maximumSameDimension = 1;
		}
		return maximumSameDimension;
	}


	public static void setBitmapReuse(boolean reuse) {
		_reuseBitmap = reuse;
	}

	public static boolean hasBitmapReuse() {
		return _reuseBitmap;
	}

	/**
	 * 设置最大图片缓存大小(单位:pixel)
	 */
	public static void setMaxCacheSize(int size) {
		_maxBitmapCacheSize = size;
	}

	/**
	 * 设置long life图片缓存大小(单位:pixel)
	 */
	public static void setLongLifeCacheSize(int size) {
		_longLIfeBitmapCacheSize = size;
	}

	public static void cleanCache() {
		synchronized (_recycledBitmapQueue) {
			for (int i = _recycledBitmapQueue.size() - 1; i >= 0; --i) {
				Pair<Long, Bitmap> item = _recycledBitmapQueue.get(i);
				Bitmap bmp = item.second;
				if (XulManager.DEBUG) {
					int byteCount = calBitmapByteSize(bmp);
					if (byteCount <= 0) {
						Log.e(TAG_RECYCLER, "invalid bitmap size(clean phase)!!! " + byteCount);
					}
				}
				Pair<Long, Bitmap> removedCache = _recycledBitmapQueue.remove(i);
				_weakGCBitmapQueue.add(Pair.create(removedCache.first, new WeakReference<Bitmap>(removedCache.second)));
			}
			_totalBitmapCacheSize = 0;
		}
	}

	public static int calBitmapByteSize(Bitmap bmp) {
		if (bmp == null) {
			return 0;
		}
		if (getAllocationByteCountMethod != null) {
			try {
				return (Integer) getAllocationByteCountMethod.invoke(bmp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return bmp.getByteCount();
	}

	public static int calBitmapPixelsCount(Bitmap bmp) {
		if (bmp == null) {
			return 0;
		}
		return bmp.getWidth() * bmp.getHeight();
	}

	private static int calBitmapPixelSize(Bitmap bmp) {
		return getPixelSize(bmp.getConfig());
	}

	public static void cleanRecycledBitmaps(long timestamp) {
		int longLIfeBitmapCacheSize = _longLIfeBitmapCacheSize;
		if (_reuseCount == 0 && _recycleCount > 300) {
			// 长时间没有重用命中, 把long life bitmap cache 大小减小为10%
			longLIfeBitmapCacheSize *= 0.1f;
		} else {
			// 根据重用比例设置当前重用缓存大小, 范围20%~120%
			float reusePercent = (float) (_reuseCount + _reuseGCCount) / _recycleCount;
			if (reusePercent < 0.20) {
				longLIfeBitmapCacheSize *= 0.2f;
			} else if (reusePercent < 0.50) {
				longLIfeBitmapCacheSize *= 0.5f;
			} else if (reusePercent < 0.8) {
				longLIfeBitmapCacheSize *= 1f;
			} else if (_reuseCount + _reuseGCCount > _newCount) {
				longLIfeBitmapCacheSize *= 1.2f;
			}
		}
		if (_totalBitmapCacheSize <= longLIfeBitmapCacheSize) {
			return;
		}
		synchronized (_recycledBitmapQueue) {
			for (int i = _recycledBitmapQueue.size() - 1; i >= 0 && _totalBitmapCacheSize > longLIfeBitmapCacheSize; --i) {
				Pair<Long, Bitmap> item = _recycledBitmapQueue.get(i);
				if (item.first <= timestamp) {
					Bitmap bmp = item.second;
					int byteCount = calBitmapByteSize(bmp);
					if (XulManager.DEBUG && byteCount <= 0) {
						Log.e(TAG_RECYCLER, "invalid bitmap size(clean phase)!!! " + byteCount);
					}

					int pixelCount = calBitmapPixelsCount(bmp);
					_totalBitmapCacheSize -= pixelCount;
					_recycledBitmapQueue.remove(i);
					_weakGCBitmapQueue.add(Pair.create(item.first, new WeakReference<Bitmap>(bmp)));
					recordDropCacheItem(bmp.getWidth(), bmp.getWidth(), bmp.getConfig());
				}
			}
		}
	}

	public static Bitmap createBitmapFromRecycledBitmaps(int width, int height) {
		return createBitmapFromRecycledBitmaps(width, height, XulManager.DEF_PIXEL_FMT);
	}

	public static Bitmap createBitmapFromRecycledBitmaps(int width, int height, Config config) {
		Bitmap gcBitmap = createBitmapFromGCBitmaps(width, height, config);
		if (gcBitmap != null) {
			return gcBitmap;
		}
		synchronized (_recycledBitmapQueue) {
			int requestBmpSize = width * height * getPixelSize(config);
			int diffSize = Integer.MAX_VALUE;
			int selectedCacheItem = -1;
			for (int i = 0, recycledBitmapQueueSize = _recycledBitmapQueue.size(); i < recycledBitmapQueueSize; i++) {
				Pair<Long, Bitmap> item = _recycledBitmapQueue.get(i);
				Bitmap bmp = item.second;

				if (!bmp.isMutable() || bmp.isRecycled()) {
					if (XulManager.DEBUG) {
						if (!bmp.isMutable()) {
							Log.e(TAG_RECYCLER, "invalid bitmap immutable(reuse phase)!!!");
						} else {
							Log.e(TAG_RECYCLER, "invalid bitmap recycled(reuse phase)!!!");
						}
					}
					_recycledBitmapQueue.remove(i);
					--i;
					--recycledBitmapQueueSize;
					_totalBitmapCacheSize -= calBitmapPixelsCount(bmp);
					continue;
				}

				if (Build.VERSION.SDK_INT >= MINIMUM_API_LEVEL) {
					int bmpSize = calBitmapByteSize(bmp);
					int minimumOutSize = (int) (1.1f * requestBmpSize + 1024);
					if (requestBmpSize <= bmpSize && bmpSize <= minimumOutSize) {
						int newDiffSize = bmpSize - requestBmpSize;
						if (newDiffSize <= diffSize) {
							diffSize = newDiffSize;
							selectedCacheItem = i;
						}
					}
				} else if (bmp.getWidth() == width && bmp.getHeight() == height && config.equals(bmp.getConfig())) {
					selectedCacheItem = i;
					break;
				}
			}

			if (selectedCacheItem >= 0) {
				Pair<Long, Bitmap> item = _recycledBitmapQueue.remove(selectedCacheItem);
				Bitmap bmp = item.second;
				recordReuseSuccess(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
				int pixelCount = calBitmapPixelsCount(bmp);
				int byteCount = calBitmapByteSize(bmp);
				if (XulManager.DEBUG && byteCount <= 0) {
					Log.e(TAG_RECYCLER, "invalid bitmap size(reuse phase)!!! " + byteCount);
				}
				_totalBitmapCacheSize -= pixelCount;
				if (XulManager.DEBUG) {
					Log.d(TAG_RECYCLER, "reuse bitmap!!! " + bmp.getWidth() + "x" + bmp.getHeight());
				}
				bmp.eraseColor(Color.TRANSPARENT);
				if (reconfigMethod != null) {
					try {
						reconfigMethod.invoke(bmp, width, height, config);
					} catch (Exception e) {
						Log.e(TAG_RECYCLER, "reconfigure bitmap failed(reuse phase)!!! " + byteCount);
					}
				}
				++_reuseCount;
				return bmp;
			}
		}
		return createBitmap(width, height, config);
	}

	private static int getPixelSize(Config config) {
		switch (config) {
		case ALPHA_8:
			return 1;
		case ARGB_4444:
			return 2;
		case ARGB_8888:
			return 4;
		case RGB_565:
			return 2;
		}
		return 4;
	}

	public static Bitmap createBitmapFromGCBitmaps(int width, int height, Config config) {
		synchronized (_weakGCBitmapQueue) {
			int requestBmpSize = width * height * getPixelSize(config);
			int diffSize = Integer.MAX_VALUE;
			int selectedCacheItem = -1;
			Bitmap selectedBitmap = null;
			for (int i = 0, gcBitmapQueueSize = _weakGCBitmapQueue.size(); i < gcBitmapQueueSize; i++) {
				Pair<Long, WeakReference<Bitmap>> item = _weakGCBitmapQueue.get(i);
				Bitmap bmp = item.second.get();

				if (bmp == null || !bmp.isMutable() || bmp.isRecycled()) {
					if (XulManager.DEBUG) {
						if (bmp == null) {
						} else if (!bmp.isMutable()) {
							Log.e(TAG_RECYCLER, "invalid bitmap immutable(gc reuse phase)!!!");
						} else {
							Log.e(TAG_RECYCLER, "invalid bitmap recycled(gc reuse phase)!!!");
						}
					}
					_weakGCBitmapQueue.remove(i);
					--i;
					--gcBitmapQueueSize;
					continue;
				}

				if (Build.VERSION.SDK_INT >= MINIMUM_API_LEVEL) {
					int bmpSize = calBitmapByteSize(bmp);
					int minimumOutSize = (int) (1.1f * requestBmpSize + 1024);
					if (requestBmpSize <= bmpSize && bmpSize <= minimumOutSize) {
						int newDiffSize = bmpSize - requestBmpSize;
						if (newDiffSize <= diffSize) {
							diffSize = newDiffSize;
							selectedCacheItem = i;
							selectedBitmap = bmp;
						}
					}
				} else if (bmp.getWidth() == width && bmp.getHeight() == height && config.equals(bmp.getConfig())) {
					selectedCacheItem = i;
					selectedBitmap = bmp;
					break;
				}
			}

			if (selectedCacheItem >= 0) {
				_weakGCBitmapQueue.remove(selectedCacheItem);
				Bitmap bmp = selectedBitmap;
				int byteCount = calBitmapByteSize(bmp);
				if (XulManager.DEBUG && byteCount <= 0) {
					Log.e(TAG_RECYCLER, "invalid bitmap size(gc reuse phase)!!! " + byteCount);
				}
				if (XulManager.DEBUG) {
					Log.d(TAG_RECYCLER, "gc reuse bitmap!!! " + bmp.getWidth() + "x" + bmp.getHeight());
				}
				bmp.eraseColor(Color.TRANSPARENT);
				if (reconfigMethod != null) {
					try {
						reconfigMethod.invoke(bmp, width, height, config);
					} catch (Exception e) {
						Log.e(TAG_RECYCLER, "reconfigure bitmap failed(gc reuse phase)!!! " + byteCount);
					}
				}
				++_reuseGCCount;
				return bmp;
			}
		}
		return null;
	}

	public static Bitmap createBitmap(int width, int height, Config config) {
		if (XulManager.DEBUG) {
			Log.d(TAG_RECYCLER, "new bitmap!!! " + width + "x" + height);
		}
		++_newCount;
		return Bitmap.createBitmap(width, height, config);
	}

	public static Bitmap createBitmap(Bitmap bmp) {
		if (XulManager.DEBUG) {
			Log.d(TAG_RECYCLER, "new bitmap!!! " + bmp);
		}
		if (bmp == null) {
			return null;
		}
		++_newCount;
		return Bitmap.createBitmap(bmp);
	}

	public static void recycleBitmap(Bitmap bmp) {
		if (!_reuseBitmap) {
			try {
				if (!bmp.isMutable()) {
					bmp.recycle();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		if (bmp == null) {
			return;
		}
		if (!bmp.isMutable()) {
			if (XulManager.DEBUG) {
				Log.e(TAG_RECYCLER, "bitmap immutable!!!");
			}
			return;
		}
		if (bmp.isRecycled()) {
			if (XulManager.DEBUG) {
				Log.e(TAG_RECYCLER, "bitmap recycled!!!");
			}
			return;
		}

		++_recycleCount;
		int pixelCount = calBitmapPixelsCount(bmp);
		if (_totalBitmapCacheSize + pixelCount >= _maxBitmapCacheSize) {
			// exceed the cache limits
			if (XulManager.DEBUG) {
				Log.d(TAG_RECYCLER, "exceed the cache limits");
			}
			return;
		}

		int byteCount = calBitmapByteSize(bmp);
		if (byteCount <= 0) {
			if (XulManager.DEBUG) {
				Log.e(TAG_RECYCLER, "invalid bitmap bytecount! " + byteCount);
			}
			return;
		}

		synchronized (_recycledBitmapQueue) {
			if (XulManager.DEBUG) {
				for (int i = 0, recycledBitmapQueueSize = _recycledBitmapQueue.size(); i < recycledBitmapQueueSize; i++) {
					Pair<Long, Bitmap> item = _recycledBitmapQueue.get(i);
					Bitmap recycledBmp = item.second;
					if (recycledBmp.equals(bmp)) {
						Log.e(TAG_RECYCLER, "duplicate bitmap! " + byteCount);
						return;
					}
				}
			}

			int sameSizeBmpCount = 0;
			int maximumSameDimension = _maximumSameDimension;

			maximumSameDimension = fixMaximumSameDimension(bmp, maximumSameDimension);

			for (int i = 0, recycledBitmapQueueSize = _recycledBitmapQueue.size(); i < recycledBitmapQueueSize && sameSizeBmpCount < maximumSameDimension; i++) {
				Pair<Long, Bitmap> item = _recycledBitmapQueue.get(i);
				Bitmap recycledBmp = item.second;
				if (pixelCount == calBitmapPixelsCount(recycledBmp)) {
					++sameSizeBmpCount;
				}
			}

			if (sameSizeBmpCount >= maximumSameDimension) {
				if (XulManager.DEBUG) {
					Log.d(TAG_RECYCLER, "too many recycled bitmap with same dimension");
				}
				return;
			}

			recordRecycled(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
			_recycledBitmapQueue.add(Pair.create(XulUtils.timestamp() + BITMAP_LIFETIME, bmp));
			_totalBitmapCacheSize += pixelCount;
		}
	}

	/**
	 * 解码图片,并转换图片像素格式及尺寸
	 * 进行缩放时, 会保持原图片的宽高比.
	 * 当水平方向与竖直方向上的缩放比例不同时.会选择其中较大的比例进行缩放.
	 * 保持输出尺寸大于或等于目标尺寸.
	 *
	 * @param path        图片路径
	 * @param pixelFormat 解码图片像素格式, null表示使用默认像素格式
	 * @param outWidth    解码图片的目标宽度, 0表示使用默认宽度
	 * @param outHeight   解码图片的目标高度, 0表示使用默认高度
	 * @return
	 */
	public static Bitmap decodeFile(String path, Config pixelFormat, int outWidth, int outHeight) {
		try {
			return decodeStream(new FileInputStream(path), pixelFormat, outWidth, outHeight, 0, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Bitmap decodeStream(InputStream is, Config pixelFormat, int outWidth, int outHeight, int maxWidth, int maxHeight) {
		byte[] decode_buf = _local_buf.get();
		if (decode_buf == null) {
			decode_buf = new byte[64 * 1024];
			_local_buf.set(decode_buf);
		}

		try {
			if (!is.markSupported()) {
				XulBufferedInputStream xulBufferedInputStream = _localBufferedInputStream.get();
				if (xulBufferedInputStream == null) {
					xulBufferedInputStream = new XulBufferedInputStream(is, 64 * 1024);
				} else {
					xulBufferedInputStream.resetInputStream(is);
				}
				is = xulBufferedInputStream;
			}

			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inTempStorage = decode_buf;
			opts.inPreferredConfig = pixelFormat;

			opts.inJustDecodeBounds = true;
			is.mark(64 * 1024);
			BitmapFactory.decodeStream(is, null, opts);
			try {
				is.reset();
			} catch (IOException e) {
				e.printStackTrace();
				is.close();
				return null;
			}
			if ((outWidth > 0 && outHeight >= 0) || (outWidth >= 0 && outHeight > 0) ||
				(maxWidth > 0 && maxHeight >= 0) || (maxWidth >= 0 && maxHeight > 0)) {
				opts.inScaled = true;
				opts.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
				int scale_x;
				int scale_y;
				if (maxWidth > 0 || maxHeight > 0) {
					if (outHeight == 0 && outWidth == 0) {
						outWidth = Math.min(opts.outWidth, maxWidth);
						outHeight = Math.min(opts.outHeight, maxHeight);
					} else {
						if (outWidth > 0 && maxWidth > 0) {
							outWidth = Math.min(outWidth, maxWidth);
						}
						if (outHeight > 0 && maxHeight > 0) {
							outHeight = Math.min(outHeight, maxHeight);
						}
					}
				}
				if (outWidth == 0) {
					scale_x = scale_y = opts.inTargetDensity * opts.outHeight / outHeight;
				} else if (outHeight == 0) {
					scale_x = scale_y = opts.inTargetDensity * opts.outWidth / outWidth;
				} else {
					scale_x = opts.inTargetDensity * opts.outWidth / outWidth;
					scale_y = opts.inTargetDensity * opts.outHeight / outHeight;
				}
				opts.inDensity = Math.min(scale_x, scale_y);
				if (opts.inDensity == opts.inTargetDensity) {
					opts.inScaled = false;
					outWidth = opts.outWidth;
					outHeight = opts.outHeight;
				} else {
					outWidth = XulUtils.roundToInt((float) opts.outWidth * opts.inTargetDensity / opts.inDensity);
					outHeight = XulUtils.roundToInt((float) opts.outHeight * opts.inTargetDensity / opts.inDensity);
				}
			} else {
				outWidth = opts.outWidth;
				outHeight = opts.outHeight;
			}

			if (XulManager.DEBUG) {
				Log.i(TAG, " width " + opts.outWidth + " height " + opts.outHeight + "  ==> width " + outWidth + " height " + outHeight);
			}

			opts.inJustDecodeBounds = false;
			opts.inPurgeable = false;
			opts.inMutable = true;
			if (!opts.inScaled && _reuseBitmap) {
				opts.inBitmap = createBitmapFromRecycledBitmaps(opts.outWidth, opts.outHeight, pixelFormat);
			} else {
				++_newCount;
			}
			opts.inSampleSize = 1;
			Bitmap bm = BitmapFactory.decodeStream(is, null, opts);
			return bm;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Bitmap decodeStream(InputStream is, Config pixelFormat, int outWidth, int outHeight) {
		return decodeStream(is, pixelFormat, outWidth, outHeight, 0, 0);
	}

	public static int countRecycledBitmap() {
		return _recycledBitmapQueue.size();
	}

	public static int countRecycledPixel() {
		return _totalBitmapCacheSize;
	}

	public static int countGCBitmap() {
		synchronized (_weakGCBitmapQueue) {
			for (int i = 0, gcBitmapQueueSize = _weakGCBitmapQueue.size(); i < gcBitmapQueueSize; i++) {
				Pair<Long, WeakReference<Bitmap>> item = _weakGCBitmapQueue.get(i);
				Bitmap bmp = item.second.get();

				if (bmp == null || !bmp.isMutable() || bmp.isRecycled()) {
					if (XulManager.DEBUG) {
						if (bmp == null) {
						} else if (!bmp.isMutable()) {
							Log.e(TAG_RECYCLER, "invalid bitmap immutable(gc reuse phase)!!!");
						} else {
							Log.e(TAG_RECYCLER, "invalid bitmap recycled(gc reuse phase)!!!");
						}
					}
					_weakGCBitmapQueue.remove(i);
					--i;
					--gcBitmapQueueSize;
					continue;
				}
			}
		}
		return _weakGCBitmapQueue.size();
	}

	public interface ICacheEnumerator {
		boolean onItem(Bitmap bmp);
	}

	public static void eachRecycledBitmaps(ICacheEnumerator enumerator) {
		synchronized (_recycledBitmapQueue) {
			for (Pair<Long, Bitmap> cache : _recycledBitmapQueue) {
				if (!enumerator.onItem(cache.second)) {
					break;
				}
			}
		}
	}

	public static void eachGCBitmaps(ICacheEnumerator enumerator) {
		synchronized (_recycledBitmapQueue) {
			for (Pair<Long, WeakReference<Bitmap>> cache : _weakGCBitmapQueue) {
				Bitmap bmp = cache.second.get();
				if (bmp == null) {
					continue;
				}
				if (!enumerator.onItem(bmp)) {
					break;
				}
			}
		}
	}

	public interface IReuseStatisticEnumerator {
		boolean onItem(ReuseStatisticInfo info);
	}

	public static void eachReuseStatistic(IReuseStatisticEnumerator enumerator) {
		synchronized (_recycledBitmapQueue) {
			Iterator<Map.Entry<Long, ReuseStatisticInfo>> iterator = _reuseStatistic.entrySet().iterator();
			while (iterator.hasNext()) {
				ReuseStatisticInfo bmp = iterator.next().getValue();
				if (bmp == null) {
					continue;
				}
				if (!enumerator.onItem(bmp)) {
					break;
				}
			}
		}
	}

	public static long getRecycleCount() {
		return _recycleCount;
	}

	public static long getReuseCount() {
		return _reuseCount;
	}

	public static long getNewCount() {
		return _newCount;
	}

	public static long getReuseGCCount() {
		return _reuseGCCount;
	}

}
