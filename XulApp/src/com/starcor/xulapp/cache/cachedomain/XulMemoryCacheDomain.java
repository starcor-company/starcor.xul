package com.starcor.xulapp.cache.cachedomain;

import com.starcor.xulapp.cache.cacheimplement.XulMemoryCache;

/**
 * Created by ZFB on 2015/9/29.
 */
public class XulMemoryCacheDomain extends XulMemoryCache {

    public XulMemoryCacheDomain(long maxSize, int maxCount) {
        super(maxSize, maxCount);
    }
}
