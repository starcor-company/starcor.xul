package com.starcor.xulapp.cache.cachedomain;

import com.starcor.xulapp.cache.XulCacheDomain;
import com.starcor.xulapp.cache.XulCacheModel;
import com.starcor.xulapp.cache.cacheimplement.XulCacheImpl;
import com.starcor.xulapp.cache.cacheimplement.XulFileCache;
import com.starcor.xulapp.cache.cacheimplement.XulMemoryCache;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by ZFB on 2015/9/29.
 */
public class XulWriteBackCacheDomain extends XulCacheDomain {

    protected final XulCacheImpl _memoryCache;
    protected final XulCacheImpl _fileCache;

    public XulWriteBackCacheDomain(long maxSize, int maxCount,
                                   File cacheDir, long maxFileSize, int maxFileCount) {
        _memoryCache = new XulMemoryCache(maxSize, maxCount);
        _fileCache = new XulFileCache(cacheDir, maxFileSize, maxFileCount);
    }

    @Override
    public void setDomainFlags(int domainFlags) {
        super.setDomainFlags(domainFlags);
        _memoryCache.setDomainFlags(domainFlags);
        _fileCache.setDomainFlags(domainFlags);
    }

    @Override
    protected boolean putCache(XulCacheModel cacheData) {
        _fileCache.removeCache(cacheData.getKey());//当有更新时，删除filecache的内容，使文件能更新为最新
        return _memoryCache.putCache(cacheData);
    }

    @Override
    protected XulCacheModel getCache(String key, boolean update) {
        XulCacheModel data = _memoryCache.getCache(key, update);
        if (data == null) {
            data = _fileCache.getCache(key, update);
        }
        return data;
    }

    @Override
    protected XulCacheModel removeCache(String md5Key) {
        XulCacheModel cacheModel = _memoryCache.removeCache(md5Key);
        XulCacheModel fileCacheModel = _fileCache.removeCache(md5Key);
        return cacheModel == null ? fileCacheModel : cacheModel;
    }

    @Override
    public void clear() {
        // 同时清除一级缓存和二级缓存
        _memoryCache.clear();
        _fileCache.clear();
    }

    @Override
    protected XulCacheModel removeNextCache() {
        XulCacheModel cacheModel = _memoryCache.removeNextCache();
        if (cacheModel == null) {
            // 一级缓存未命中，从二级缓存删除
            cacheModel = _fileCache.removeNextCache();
        } else {
            // 一级缓存命中，存入二级缓存
            _fileCache.putCache(cacheModel);
        }

        return cacheModel;
    }

    @Override
    public void close() {
        for (XulCacheModel cache : _memoryCache.getAllCaches()) {
            _fileCache.putCache(cache);
        }
        _memoryCache.clear();
    }

    @Override
    public long size() {
        return _memoryCache.size() + _fileCache.size();
    }

    @Override
    public long sizeCapacity() {
        return _memoryCache.sizeCapacity() + _fileCache.sizeCapacity();
    }

    @Override
    public int count() {
        return _memoryCache.count() + _fileCache.count();
    }

    @Override
    public int countCapacity() {
        int count = _memoryCache.countCapacity() + _fileCache.countCapacity();
        return count > 0 ? count : Integer.MAX_VALUE;
    }

    @Override
    public Collection<XulCacheModel> getAllCaches() {
        HashSet<XulCacheModel> caches = new HashSet<XulCacheModel>(count());
        caches.addAll(_memoryCache.getAllCaches());
        caches.addAll(_fileCache.getAllCaches());
        return caches;
    }
}
