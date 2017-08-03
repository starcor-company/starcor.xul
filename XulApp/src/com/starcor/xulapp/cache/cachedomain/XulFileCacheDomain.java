package com.starcor.xulapp.cache.cachedomain;

import com.starcor.xulapp.cache.cacheimplement.XulFileCache;

import java.io.File;

/**
 * Created by ZFB on 2015/9/29.
 */
public class XulFileCacheDomain extends XulFileCache {

    public XulFileCacheDomain(File cacheDir, long maxSize, int maxCount) {
        super(cacheDir, maxSize, maxCount);
    }
}
