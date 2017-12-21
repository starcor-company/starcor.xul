package com.starcor.xulapp.cache.cachedomain;

import com.starcor.xulapp.cache.XulCacheModel;
import com.starcor.xulapp.message.XulMessageCenter;
import com.starcor.xulapp.message.XulSubscriber;
import com.starcor.xulapp.message.XulThreadMode;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ZFB on 2015/10/21.
 */
public class XulPropertyCacheDomain extends XulWriteBackCacheDomain {

    private static final int INTERVAL_TO_SAVE_DATA = 200;
    private static final int MSG_TAG_SAVE_DATA_TO_FILE = 0x0001;

    private final AtomicBoolean _isUpdated = new AtomicBoolean(false);
    private final XulMessageCenter _propertyMsgCenter;

    public XulPropertyCacheDomain(long maxSize, int maxCount,
                                  File cacheDir, long maxFileSize, int maxFileCount) {
        super(maxSize, maxCount, cacheDir, maxFileSize, maxFileCount);

        _propertyMsgCenter = new XulMessageCenter(TAG);
        _propertyMsgCenter.register(this);
        _propertyMsgCenter.post(MSG_TAG_SAVE_DATA_TO_FILE, null,
                                INTERVAL_TO_SAVE_DATA, Integer.MAX_VALUE, INTERVAL_TO_SAVE_DATA);
    }


    @XulSubscriber(tag = MSG_TAG_SAVE_DATA_TO_FILE, mode = XulThreadMode.ASYNC)
    public void onSaveTime(Object data) {
        if (_isUpdated.getAndSet(false)) {
            // 更新过数据，需要写入文件缓存
            for (XulCacheModel cache : _memoryCache.getAllCaches()) {
                if (null ==_fileCache.getCache(cache.getKey(), false)) {
                    _fileCache.putCache(cache);
                }
            }
        }
    }

    @Override
    public void close() {
        super.close();
        _propertyMsgCenter.unregister(this);
    }

    @Override
    protected boolean putCache(XulCacheModel cacheData) {
        boolean isUpdated = super.putCache(cacheData);
        if (isUpdated) {
            _isUpdated.set(true);
        }
        return isUpdated;
    }

    @Override
    protected XulCacheModel removeCache(String md5Key) {
        XulCacheModel cache = super.removeCache(md5Key);
        if (cache != null) {
            _isUpdated.set(true);
        }
        return cache;
    }

    @Override
    protected XulCacheModel removeNextCache() {
        XulCacheModel cache = super.removeNextCache();
        if (cache != null) {
            _isUpdated.set(true);
        }
        return cache;
    }

    public boolean getIsUpdated() {
        return _isUpdated.get();
    }
}
