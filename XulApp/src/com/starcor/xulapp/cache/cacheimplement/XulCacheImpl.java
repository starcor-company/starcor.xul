package com.starcor.xulapp.cache.cacheimplement;

import com.starcor.xulapp.cache.XulCacheDomain;
import com.starcor.xulapp.cache.XulCacheModel;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ZFB on 2015/9/21.
 */
public abstract class XulCacheImpl extends XulCacheDomain {

    /**
     * 缓存数据map集合
     */
    protected final ConcurrentMap<String, XulCacheModel> _caches =
            new ConcurrentHashMap<String, XulCacheModel>();

    protected final AtomicLong _cacheSize = new AtomicLong(0);
    protected final AtomicInteger _cacheCount = new AtomicInteger(0);
    protected final long _sizeLimit;
    protected final int _countLimit;

    public XulCacheImpl(long maxSize, int maxCount) {
        _sizeLimit = maxSize;
        _countLimit = maxCount;
    }

    @Override
    public boolean putCache(XulCacheModel cacheData) {
        if (!XulCacheModel.isValid(cacheData)) {
            return false;
        }

        long valueSize = cacheData.size();
        int valueCount = 1;
        XulCacheModel oldCache = _caches.get(cacheData.getKey());
        if (oldCache != null) {
            // 已经存在同样的key
            valueSize -= oldCache.size();
            valueCount = 0;
        }

        if (valueSize > _sizeLimit) {
            throw new RuntimeException("Data is too large to put in cache.");
        }

        while (_cacheSize.get() + valueSize > _sizeLimit) {
            if (removeNextCache() == null) {
                // 无法移除，存储失败
                return false;
            }
        }
        _cacheSize.addAndGet(valueSize);

        while (_cacheCount.get() + valueCount > _countLimit) {
            if (removeNextCache() == null) {
                return false;
            }
        }
        _cacheCount.addAndGet(valueCount);

        cacheData.updateLastAccessTime();
        _caches.put(cacheData.getKey(), cacheData);
        cacheData.setOwner(this);
        return true;
    }

    @Override
    public XulCacheModel getCache(String key, boolean update) {
        XulCacheModel data = _caches.get(key);
        if (data == null || isExpired(data)) {
            if (data != null) {
                removeCache(key);
            }
            return null;
        }

        if (update) {
            data.updateLastAccessTime();
        }
        return data;
    }

    @Override
    public XulCacheModel removeCache(String md5Key) {
        XulCacheModel data = _caches.remove(md5Key);
        if (data != null) {
            _cacheSize.addAndGet(-data.size());
            _cacheCount.addAndGet(-1);
        }

        return data;
    }

    @Override
    public void clear() {
        _caches.clear();
        _cacheSize.set(0);
        _cacheCount.set(0);
    }

    @Override
    public XulCacheModel removeNextCache() {
        if (_caches.isEmpty()) {
            return null;
        }

        XulCacheModel cache = _recycler.recycle(_caches);
        if (cache != null) {
            _cacheSize.addAndGet(-cache.size());
            _cacheCount.addAndGet(-1);
        }
        return cache;
    }

    @Override
    public Collection<XulCacheModel> getAllCaches() {
        return _caches.values();
    }

    public void setRecycleStrategy(int strategyFlags) {
        _recycler.clear();
        _recycler.addRecycleStrategy(strategyFlags);
    }

    @Override
    public long size() {
        return _cacheSize.get();
    }

    @Override
    public long sizeCapacity() {
        return _sizeLimit;
    }

    @Override
    public int count() {
        return _cacheCount.get();
    }

    @Override
    public int countCapacity() {
        return _countLimit;
    }
}
