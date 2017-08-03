package com.starcor.xulapp.cache;

import android.text.TextUtils;

import java.io.File;

/**
 * Created by ZFB on 2015/9/25.
 */
public class XulCacheModel {
	private XulCacheDomain _owner;

	/**
	 * 缓存数据键值
	 */
	private String _key;

	/**
	 * 缓存数据
	 */
	private Object _data;

	/**
	 * 缓存数据最后访问时间
	 */
	private long _lastAccessTime;

	public XulCacheModel() {
		_lastAccessTime = System.currentTimeMillis();
	}

	public XulCacheModel(String key, Object data) {
		this(key, data, System.currentTimeMillis());
	}

	public XulCacheModel(String key, Object data, long lastAccessTime) {
		_key = key;
		_data = data;
		_lastAccessTime = lastAccessTime;
	}

	public XulCacheModel(XulCacheModel other) {
		_key = other._key;
		_data = other._data;
		_lastAccessTime = other._lastAccessTime;
	}

	/**
	 * 判断cache数据是否有效
	 */
	public static boolean isValid(XulCacheModel cacheData) {
		return (cacheData != null) && !TextUtils.isEmpty(cacheData.getKey())
			&& (cacheData.getData() != null);
	}

	/**
	 * 以byte为单位返回缓存数据大小
	 */
	public long size() {
		// 根据缓存数据实际类型计算其大小
		if (_data instanceof String) {
			return ((String) _data).getBytes().length;
		} else if (_data instanceof byte[]) {
			return ((byte[]) _data).length;
		} else if (_data instanceof File) {
			return ((File) _data).length();
		} else {
			// 无法计算或，默认返回0
			return 0;
		}
	}

	public String getKey() {
		return _key;
	}

	public void setKey(String key) {
		_key = key;
	}

	public Object getData() {
		return _data;
	}

	public void setData(Object data) {
		_data = data;
	}

	public long getLastAccessTime() {
		return _lastAccessTime;
	}

	public void setLastAccessTime(long lastAccessTime) {
		_lastAccessTime = lastAccessTime;
	}

	public void updateLastAccessTime() {
		_lastAccessTime = System.currentTimeMillis();
	}

	public void setOwner(XulCacheDomain owner) {
		_owner = owner;
	}

	public XulCacheDomain getOwner() {
		return _owner;
	}

	@Override
	public int hashCode() {
		return _key.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return _key.equals(((XulCacheModel) o)._key);
	}
}
