package com.starcor.xulapp.cache.cachedomain;

import com.starcor.xulapp.cache.XulCacheDomain;
import com.starcor.xulapp.cache.XulCacheModel;
import com.starcor.xulapp.cache.cacheimplement.XulCacheImpl;
import com.starcor.xulapp.cache.cacheimplement.XulFileCache;
import com.starcor.xulapp.cache.cacheimplement.XulMemoryCache;

import java.io.File;
import java.util.Collection;

/**
 * Created by ZFB on 2015/9/29.
 */
public class XulWriteThroughCacheDomain extends XulCacheDomain {

    protected final XulCacheImpl _memoryCache;
    protected final XulCacheImpl _fileCache;

    public XulWriteThroughCacheDomain(long maxSize, int maxCount,
                                      File cacheDir, long maxFileSize, int maxFileCount) {
        // 同时写入内存和文件，取最小的大小限制和数量限制
        long minSize = Math.min(maxSize, maxFileSize);
        int minCount = Math.min(maxCount, maxFileCount);

        _memoryCache = new XulMemoryCache(minSize, minCount);
        _fileCache = new XulFileCache(cacheDir, minSize, minCount);

        // 保证文件缓存和内存保存数据同步
        for (XulCacheModel cacheData : _fileCache.getAllCaches()) {
            _memoryCache.putCache(cacheData);
        }
    }

    @Override
    public void setDomainFlags(int domainFlags) {
        super.setDomainFlags(domainFlags);
        _memoryCache.setDomainFlags(domainFlags);
        _fileCache.setDomainFlags(domainFlags);
    }

    @Override
    protected boolean putCache(XulCacheModel cacheData) {
        boolean putResult = _memoryCache.putCache(cacheData);
        if (putResult) {
            putResult = _fileCache.putCache(cacheData);
        } else {
            // 内存缓存保存失败
            return false;
        }

        if (putResult) {
            return true;
        } else {
            // 文件缓存保存失败，同时删除内存中的缓存
            _memoryCache.removeCache(cacheData.getKey());
            return false;
        }
    }

    @Override
    protected XulCacheModel getCache(String key, boolean update) {
        return _memoryCache.getCache(key, update);
    }

    @Override
    protected XulCacheModel removeCache(String md5Key) {
        XulCacheModel memoryData = _memoryCache.removeCache(md5Key);
        if (memoryData != null) {
            _fileCache.removeCache(md5Key);
        }

        return memoryData;
    }

    @Override
    public void clear() {
        _memoryCache.clear();
        _fileCache.clear();
    }

    @Override
    protected XulCacheModel removeNextCache() {
        XulCacheModel memoryData = _memoryCache.removeNextCache();
        if (memoryData != null) {
            _fileCache.removeCache(memoryData.getKey());
        }

        return memoryData;
    }

    @Override
    public long size() {
        return _memoryCache.size();
    }

    @Override
    public long sizeCapacity() {
        return _memoryCache.sizeCapacity();
    }

    @Override
    public int count() {
        return _memoryCache.count();
    }

    @Override
    public int countCapacity() {
        return _memoryCache.countCapacity();
    }

    @Override
    public Collection<XulCacheModel> getAllCaches() {
        return _memoryCache.getAllCaches();
    }
}
