package com.starcor.xulapp.cache.cacherecycle;

import com.starcor.xulapp.cache.XulCacheDomain;
import com.starcor.xulapp.cache.XulCacheModel;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ZFB on 2015/9/28.
 */
public class XulCacheRecycle {

    // 回收策略枚举
    public static final int STRATEGY_NO_RECYCLE = 0x0001;
    public static final int STRATEGY_EXPIRED = 0x0002;
    public static final int STRATEGY_RECENTLY_UNUSED = 0x0003;

    private XulCacheDomain _cacheDomain;
    private ConcurrentLinkedQueue<XulRecycleStrategy> _strategies;

    public XulCacheRecycle(XulCacheDomain cacheDomain) {
        _cacheDomain = cacheDomain;
        _strategies = new ConcurrentLinkedQueue<XulRecycleStrategy>();
    }

    /**
     * 添加回收策略 回收时会按照添加策略的先后顺序依次回收，直到找到下一个回收对象
     */
    public void addRecycleStrategy(int recycleStrategy) {
        XulRecycleStrategy newStrategy = getRecycleStrategy(recycleStrategy);
        if (newStrategy != null && !_strategies.contains(newStrategy)) {
            _strategies.add(newStrategy);
        }
    }

    private XulRecycleStrategy getRecycleStrategy(int recycleStrategyFlag) {
        XulRecycleStrategy strategy = null;
        switch (recycleStrategyFlag) {
            case STRATEGY_EXPIRED:
                strategy = new XulExpireStrategy(_cacheDomain);
                break;
            case STRATEGY_RECENTLY_UNUSED:
                strategy = new XulRecentlyUnusedStrategy();
                break;
            case STRATEGY_NO_RECYCLE:
                strategy = new XulNoRecycleStrategy();
                break;
            default:
                strategy = null;
                break;
        }
        return strategy;
    }

    public void removeRecycleStrategy(int recycleStrategy) {
        _strategies.remove(getRecycleStrategy(recycleStrategy));
    }

    public void clear() {
        _strategies.clear();
    }

    public boolean containsRecycleStrategy(int recycleStrategy) {
        return _strategies.contains(getRecycleStrategy(recycleStrategy));
    }

    public XulCacheModel recycle(ConcurrentMap<String, XulCacheModel> caches) {
        XulCacheModel cache = null;
        for (XulRecycleStrategy strategy : _strategies) {
            cache = strategy.findRecycledCache(caches.values());
            if (cache != null) {
                caches.remove(cache.getKey());
                break;
            }
        }

        return cache;
    }
}

