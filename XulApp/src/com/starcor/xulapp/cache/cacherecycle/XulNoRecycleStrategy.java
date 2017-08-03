package com.starcor.xulapp.cache.cacherecycle;

import com.starcor.xulapp.cache.XulCacheModel;

import java.util.Collection;

/**
 * Created by ZFB on 2015/10/23.
 */
class XulNoRecycleStrategy implements XulRecycleStrategy {

    @Override
    public XulCacheModel findRecycledCache(Collection<XulCacheModel> caches) {
        return null;
    }
}
