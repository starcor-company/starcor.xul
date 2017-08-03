package com.starcor.xulapp.cache.cacherecycle;

import com.starcor.xulapp.cache.XulCacheModel;

import java.util.Collection;

/**
 * Created by ZFB on 2015/10/23.
 */
interface XulRecycleStrategy {

    /**
     * 根据特定的回收算法返回一个可回收的数据对象
     *
     * @param caches 缓存数据集合
     * @return 可回收对象，若未找到，则返回null
     */
    XulCacheModel findRecycledCache(Collection<XulCacheModel> caches);
}
