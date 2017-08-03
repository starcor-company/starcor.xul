package com.starcor.xulapp.cache.cacherecycle;

import com.starcor.xulapp.cache.XulCacheModel;

import java.util.Collection;

/**
 * Created by ZFB on 2015/10/23.
 */
class XulRecentlyUnusedStrategy implements XulRecycleStrategy {

    @Override
    public XulCacheModel findRecycledCache(Collection<XulCacheModel> caches) {
        XulCacheModel oldestData = null;
        for (XulCacheModel cacheData : caches) {
            if (oldestData == null) {
                oldestData = cacheData;
            } else {
                if (cacheData.getLastAccessTime() < oldestData.getLastAccessTime()) {
                    oldestData = cacheData;
                }
            }
        }

        return oldestData;
    }
}
