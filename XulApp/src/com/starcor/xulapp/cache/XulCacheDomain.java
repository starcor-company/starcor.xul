package com.starcor.xulapp.cache;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.starcor.xul.XulUtils;
import com.starcor.xulapp.cache.cacherecycle.XulCacheRecycle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Collection;

/**
 * Created by ZFB on 2015/9/21.
 */
public abstract class XulCacheDomain {

	protected String TAG = getClass().getSimpleName();

	/**
	 * Cache Domain 标识id
	 */
	protected int _domainId = 0;

	/**
	 * flags用于区分cache的添加和移除逻辑
	 */
	protected int _domainFlags = 0;

	/**
	 * Cache的保存时间，以毫秒为单位，0代表不会过期
	 */
	protected long _lifeTime = 0;

	public int getDomainId() {
		return _domainId;
	}

	public void setDomainId(int domainId) {
		_domainId = domainId;
	}

	public int getDomainFlags() {
		return _domainFlags;
	}

	public void setDomainFlags(int domainFlags) {
		_domainFlags = domainFlags;
		resetRecycleStrategy();
	}

	private void resetRecycleStrategy() {
		if ((_domainFlags & 0xF0) == XulCacheCenter.CACHE_FLAG_PERSISTENT) {
			setRecycleStrategy(XulCacheRecycle.STRATEGY_NO_RECYCLE);
		} else {
			setRecycleStrategy(
				XulCacheRecycle.STRATEGY_EXPIRED, XulCacheRecycle.STRATEGY_RECENTLY_UNUSED);
		}
	}

	public long getLifeTime() {
		return _lifeTime;
	}

	public void setLifeTime(long ms) {
		_lifeTime = ms;
	}

	protected final XulCacheRecycle _recycler = new XulCacheRecycle(this);

	// =======================================
	// ============ 通用数据 读写 ==============
	// =======================================

	/**
	 * 保存 通用数据 到 缓存中
	 *
	 * @param key   保存的key
	 * @param value 保存的数据
	 */
	public <T> void put(String key, T value) {
		putCache(new XulCacheModel(XulUtils.calMD5(key), value));
	}

	/**
	 * 读取 Cache数据
	 *
	 * @return cache 数据
	 */
	public Object get(String key) {
		XulCacheModel cache = internalGetCache(key, true);
		if (cache == null) {
			return null;
		}
		return cache.getData();
	}

	/**
	 * 判断是否包含以key为键的缓存
	 *
	 * @param key 带判断缓存的键值
	 * @return 包含返回true，否则返回false
	 */
	public boolean contains(String key) {
		return getCache(XulUtils.calMD5(key), false) != null;
	}

	protected abstract XulCacheModel getCache(String key, boolean update);

	protected XulCacheModel internalGetCache(String key) {
		return internalGetCache(key, true);
	}

	protected XulCacheModel internalGetCache(String key, boolean update) {
		return getCache(XulUtils.calMD5(key), update);
	}

	// =======================================
	// ============ String数据 读写 ==============
	// =======================================
	/**
	 * 读取 String数据
	 *
	 * @param key
	 * @return String 数据
	 */
	public String getAsString(String key) {
		XulCacheModel cacheModel = internalGetCache(key);
		if (cacheModel == null) {
			return null;
		}
		return getAsString(cacheModel);
	}

	public String getAsString(XulCacheModel cacheModel) {
		XulCacheDomain owner = cacheModel.getOwner();
		if (owner == null) {
			return null;
		}
		return owner.getAsString(cacheModel);
	}

	// =======================================
	// ============= JSONObject 数据 读写 ==============
	// =======================================
	/**
	 * 读取JSONObject数据
	 *
	 * @param key
	 * @return JSONObject数据
	 */
	public JSONObject getAsJSONObject(String key) {
		XulCacheModel cacheModel = internalGetCache(key);
		if (cacheModel == null) {
			return null;
		}
		return getAsJSONObject(cacheModel);
	}

	public JSONObject getAsJSONObject(XulCacheModel cacheModel) {
		XulCacheDomain owner = cacheModel.getOwner();
		if (owner == null) {
			return null;
		}
		return owner.getAsJSONObject(cacheModel);
	}

	// =======================================
	// ============ JSONArray 数据 读写 =============
	// =======================================
	/**
	 * 读取JSONArray数据
	 *
	 * @param key
	 * @return JSONArray数据
	 */
	public JSONArray getAsJSONArray(String key) {
		XulCacheModel cacheModel = internalGetCache(key);
		if (cacheModel == null) {
			return null;
		}
		return getAsJSONArray(cacheModel);
	}

	public JSONArray getAsJSONArray(XulCacheModel cacheModel) {
		XulCacheDomain owner = cacheModel.getOwner();
		if (owner == null) {
			return null;
		}
		return owner.getAsJSONArray(cacheModel);
	}

	// =======================================
	// ============== byte 数据 读写 =============
	// =======================================

	/**
	 * 获取 byte 数据
	 *
	 * @param key
	 * @return byte 数据
	 */
	public byte[] getAsBinary(String key) {
		XulCacheModel cacheModel = internalGetCache(key);
		if (cacheModel == null) {
			return null;
		}
		return getAsBinary(cacheModel);
	}

	public byte[] getAsBinary(XulCacheModel cacheModel) {
		XulCacheDomain owner = cacheModel.getOwner();
		if (owner == null) {
			return null;
		}
		return owner.getAsBinary(cacheModel);
	}

	public InputStream getAsStream(String key) {
		XulCacheModel cacheModel = internalGetCache(key);
		if (cacheModel == null) {
			return null;
		}
		return getAsStream(cacheModel);
	}

	public InputStream getAsStream(XulCacheModel cacheModel) {
		XulCacheDomain owner = cacheModel.getOwner();
		if (owner == null) {
			return null;
		}
		return owner.getAsStream(cacheModel);
	}

	// =======================================
	// ============= 序列化 数据 读写 ===============
	// =======================================

	/**
	 * 读取 Serializable数据
	 *
	 * @param key
	 * @return Serializable 数据
	 */
	public Object getAsObject(String key) {
		XulCacheModel cacheModel = internalGetCache(key);
		if (cacheModel == null) {
			return null;
		}
		return getAsObject(cacheModel);
	}

	public Object getAsObject(XulCacheModel cacheModel) {
		XulCacheDomain owner = cacheModel.getOwner();
		if (owner == null) {
			return null;
		}
		return owner.getAsObject(cacheModel);
	}

	/**
	 * 读取 bitmap 数据
	 *
	 * @param key
	 * @return bitmap 数据
	 */
	public Bitmap getAsBitmap(String key) {
		XulCacheModel cacheModel = internalGetCache(key);
		if (cacheModel == null) {
			return null;
		}
		return getAsBitmap(cacheModel);
	}

	public Bitmap getAsBitmap(XulCacheModel cacheModel) {
		XulCacheDomain owner = cacheModel.getOwner();
		if (owner == null) {
			return null;
		}
		return owner.getAsBitmap(cacheModel);
	}

	// =======================================
	// ============= drawable 数据 读写 =============
	// =======================================
	/**
	 * 读取 Drawable 数据
	 *
	 * @param key
	 * @return Drawable 数据
	 */
	public Drawable getAsDrawable(String key) {
		XulCacheModel cacheModel = internalGetCache(key);
		if (cacheModel == null) {
			return null;
		}
		return getAsDrawable(cacheModel);
	}

	public Drawable getAsDrawable(XulCacheModel cacheModel) {
		XulCacheDomain owner = cacheModel.getOwner();
		if (owner == null) {
			return null;
		}
		return owner.getAsDrawable(cacheModel);
	}

	/**
	 * 移除某个key所对应的cache
	 */
	public XulCacheModel remove(String key) {
		XulCacheModel cache = removeCache(XulUtils.calMD5(key));
		return cache;
	}

	/**
	 * 移除指定的cache数据
	 */
	public XulCacheModel remove(XulCacheModel cache) {
		return removeCache(cache.getKey());
	}

	/**
	 * 清除所有数据
	 *
	 * @return 是否清除成功
	 */
	public abstract void clear();

	/**
	 * 移除一个cache数据
	 */
	public Object removeNext() {
		XulCacheModel cache = removeNextCache();
		if (cache != null) {
			return cache.getData();
		}

		return null;
	}

	/**
	 * 关闭cache
	 */
	public void close() {
		clear();
	}

	/**
	 * 获取所有的缓存数据
	 * @return
	 */
	public abstract Collection<XulCacheModel> getAllCaches();

	/**
	 * 获取已存储缓存大小
	 * @return
	 */
	public abstract long size();

	/**
	 * 获取缓存总容量
	 * @return
	 */
	public abstract long sizeCapacity();

	/**
	 * 获取已存储缓存数量
	 * @return
	 */
	public abstract int count();

	/**
	 * 获取缓存总数量
	 * @return
	 */
	public abstract int countCapacity();

	/**
	 * 判断缓存数据是否过期
	 */
	public boolean isExpired(XulCacheModel cache) {
		if ((_lifeTime > 0) && (cache != null)) {
			return System.currentTimeMillis() > _lifeTime + cache.getLastAccessTime();
		}

		return false;
	}

	public void setRecycleStrategy(int... strategies) {
		_recycler.clear();
		for (int strategy : strategies) {
			_recycler.addRecycleStrategy(strategy);
		}
	}

	// =======================================
	// ============= 内部使用api ===============
	// =======================================

	/**
	 * 保存 缓存数据 到 缓存中
	 */
	protected abstract boolean putCache(XulCacheModel cacheData);

	/**
	 * 移除下一个cache数据
	 */
	protected abstract XulCacheModel removeNextCache();

	/**
	 * 移除指定的cache数据
	 */
	protected abstract XulCacheModel removeCache(String md5Key);
}
