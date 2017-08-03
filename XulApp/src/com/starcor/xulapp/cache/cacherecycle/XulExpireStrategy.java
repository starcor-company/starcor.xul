package com.starcor.xulapp.cache.cacherecycle;

import com.starcor.xulapp.cache.XulCacheDomain;
import com.starcor.xulapp.cache.XulCacheModel;

import java.util.Collection;

/**
 * Created by ZFB on 2015/10/23.
 */
class XulExpireStrategy implements XulRecycleStrategy {

    private final XulCacheDomain _cacheDomain;

    public XulExpireStrategy(XulCacheDomain domain) {
        _cacheDomain = domain;
    }

    @Override
    public XulCacheModel findRecycledCache(Collection<XulCacheModel> caches) {
        XulCacheModel cacheModel = null;
        for (XulCacheModel cache : caches) {
            if (cache != null && _cacheDomain.isExpired(cache)) {
                break;
            }
        }

        return cacheModel;
    }
}
