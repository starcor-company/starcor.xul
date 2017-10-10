package com.starcor.xulapp.cache;

import android.content.Context;
import android.text.TextUtils;

import com.starcor.xul.XulUtils;
import com.starcor.xulapp.XulApplication;
import com.starcor.xulapp.cache.cachedomain.XulFileCacheDomain;
import com.starcor.xulapp.cache.cachedomain.XulMemoryCacheDomain;
import com.starcor.xulapp.cache.cachedomain.XulPropertyCacheDomain;
import com.starcor.xulapp.cache.cachedomain.XulWriteBackCacheDomain;
import com.starcor.xulapp.cache.cachedomain.XulWriteThroughCacheDomain;
import com.starcor.xulapp.debug.IXulDebugCommandHandler;
import com.starcor.xulapp.debug.IXulDebuggableObject;
import com.starcor.xulapp.debug.XulDebugMonitor;
import com.starcor.xulapp.debug.XulDebugServer;
import com.starcor.xulapp.http.XulHttpServer;
import com.starcor.xulapp.utils.XulLog;
import com.starcor.xulapp.utils.XulSystemUtil;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ZFB on 2015/9/21.
 */
public class XulCacheCenter {

    private static final String TAG = XulCacheCenter.class.getSimpleName();

    // =======================================
    // ============ Cache Flags ==============
    // =======================================
    /**
     * Cache is valid until revision(versionCode) changed
     */
    public static final int CACHE_FLAG_REVISION_LOCAL = 0x00000;
    /**
     * Cache is valid until app's version type changed
     */
    public static final int CACHE_FLAG_VERSION_LOCAL = 0x00001;
    /**
     * Cache will always valid
     */
    public static final int CACHE_FLAG_GLOBAL = 0x00002;

    /**
     * Cache will never be recycled automatically
     */
    public static final int CACHE_FLAG_PERSISTENT = 0x00010;
    /**
     * Cache domain optimized for property storing
     */
    public static final int CACHE_FLAG_PROPERTY = 0x10000;

    /**
     * Cache in memory only
     */
    public static final int CACHE_FLAG_MEMORY = 0x20000;
    /**
     * Cache in file only
     */
    public static final int CACHE_FLAG_FILE = 0x40000;
    /**
     * Cache in memory and file
     */
    public static final int CACHE_FLAG_WRITE_BACK = CACHE_FLAG_MEMORY | CACHE_FLAG_FILE;
    /**
     * Cache in memory and file at the same time
     */
    public static final int CACHE_FLAG_WRITE_THROUGH = 0x80000 | CACHE_FLAG_WRITE_BACK;

    /**
     * 默认缓存大小
     */
    public static final long DEFAULT_MAX_MEMORY_SIZE = 1024 * 1024 * 32; // 32 mb
    public static final int DEFAULT_MAX_MEMORY_COUNT = Integer.MAX_VALUE; // 默认不使用
    public static final long DEFAULT_MAX_FILE_SIZE = 1024 * 1024 * 128; // 128 mb
    public static final int DEFAULT_MAX_FILE_COUNT = Integer.MAX_VALUE; // 默认不使用

    private static final ConcurrentMap<Integer, XulCacheDomain> _cacheDomains =
            new ConcurrentHashMap<Integer, XulCacheDomain>();

    private static XulDebugMonitor _dbgMonitor;

    private static int _revision = 0;
    private static String _version = "all";

    /**
     * 获取指定的cache domain
     *
     * @param domainId cache domain的标识id
     * @return 若存在，返回对应的cache domain，否则返回null
     */
    public static XulCacheDomain getCacheDomain(int domainId) {
        return _cacheDomains.get(domainId);
    }

    /**
     * 关闭cache center，一般在程序结束时调用。
     */
    public static void close() {
        for (XulCacheDomain domain : _cacheDomains.values()) {
            domain.close();
        }
    }

    /**
     * 清理缓存中心的所有cache数据，具有PERSISTENT标志的缓存域除外。
     */
    public static void clear() {
        if (_cacheDomains.values() != null && _cacheDomains.size() != 0) {
            for (XulCacheDomain domain : _cacheDomains.values()) {
                // 具有PERSISTENT标志的缓存域不需要清空
                if ((domain.getDomainFlags() & 0xF0) != XulCacheCenter.CACHE_FLAG_PERSISTENT) {
                    domain.clear();
                }
            }
        }
    }

    /**
     * 创建cache domain
     *
     * @param domainId cache domain标识id
     * @return 若创建成功或已存在相同domain则返回domain， 否则返回空（比如id相同但flag不同或无法创建缓存目录等原因造成）
     */
    public static CacheDomainBuilder buildCacheDomain(int domainId) {
        registerDebugHelper();

        Context context = XulApplication.getAppContext();
        return CacheDomainBuilder.obtainBuilder(context, domainId);
    }

    private static void registerDebugHelper() {
        if (_dbgMonitor != null) {
            return;
        }
        _dbgMonitor = XulDebugServer.getMonitor();
        if (_dbgMonitor == null) {
            return;
        }
        _dbgMonitor.registerDebuggableObject(new IXulDebuggableObject() {
            @Override
            public String name() {
                return "CacheCenter";
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public boolean runInMainThread() {
                return false;
            }

            @Override
            public boolean buildBriefInfo(XulHttpServer.XulHttpServerRequest request,
                                          XmlSerializer infoWriter) {
                recordCacheDomainNum(infoWriter);
                return true;
            }

            @Override
            public boolean buildDetailInfo(XulHttpServer.XulHttpServerRequest request,
                                           XmlSerializer infoWriter) {
                dumpCacheDomains(infoWriter);
                return true;
            }

            @Override
            public XulHttpServer.XulHttpServerResponse execCommand(String command,
                                                                   XulHttpServer.XulHttpServerRequest request,
                                                                   XulHttpServer.XulHttpServerHandler serverHandler) {
                return null;
            }

            protected void dumpCacheDomains(XmlSerializer infoWriter) {
                recordCacheDomainNum(infoWriter);
                for (XulCacheDomain domain : _cacheDomains.values()) {
                    try {
                        infoWriter.startTag(null, "domain");
                        infoWriter.attribute(null, "domain-id",
                                             String.valueOf(domain.getDomainId()));
                        infoWriter.attribute(null, "domain-flag",
                                             parseDomainFlags(domain.getDomainFlags()));
                        infoWriter.attribute(null, "life-time",
                                             parseDomainLifeTime(domain.getLifeTime()));

                        infoWriter.startTag(null, "info");
                        infoWriter.attribute(null, "size", parseDomainSize(domain.size()));
                        infoWriter.attribute(null, "sizeCapacity",
                                             parseDomainSize(domain.sizeCapacity()));
                        infoWriter.attribute(null, "count", parseDomainCount(domain.count()));
                        infoWriter.attribute(null, "countCapacity",
                                             parseDomainCount(domain.countCapacity()));
                        infoWriter.endTag(null, "info");

                        infoWriter.endTag(null, "domain");
                    } catch (IOException e) {
                        XulLog.e(TAG, e);
                    }
                }
            }

            private void recordCacheDomainNum(XmlSerializer infoWriter) {
                final int size = _cacheDomains.size();
                try {
                    infoWriter.attribute(null, "cacheDomainNum", String.valueOf(size));
                } catch (IOException e) {
                    XulLog.e(TAG, e);
                }
            }

            private String parseDomainFlags(int domainFlags) {
                String cacheDirFlag;
                switch (domainFlags & 0xF) {
                    case CACHE_FLAG_REVISION_LOCAL:
                        cacheDirFlag = "REVISION_LOCAL";
                        break;
                    case CACHE_FLAG_VERSION_LOCAL:
                        cacheDirFlag = "VERSION_LOCAL";
                        break;
                    case CACHE_FLAG_GLOBAL:
                        cacheDirFlag = "GLOBAL";
                        break;
                    default:
                        cacheDirFlag = "GLOBAL";
                        break;
                }

                String domainKindFlag;
                switch (domainFlags & ~0xFF) {
                    case CACHE_FLAG_PROPERTY:
                        domainKindFlag = "PROPERTY";
                        break;
                    case CACHE_FLAG_MEMORY:
                        domainKindFlag = "MEMORY";
                        break;
                    case CACHE_FLAG_FILE:
                        domainKindFlag = "FILE";
                        break;
                    case CACHE_FLAG_WRITE_BACK:
                        domainKindFlag = "WRITE_BACK";
                        break;
                    case CACHE_FLAG_WRITE_THROUGH:
                        domainKindFlag = "WRITE_THROUGH";
                        break;
                    default:
                        domainKindFlag = "FILE";
                        break;
                }

                final String FLAG_SEPARATOR = " | ";
                StringBuilder flagBuilder = new StringBuilder();
                flagBuilder.append(cacheDirFlag).append(FLAG_SEPARATOR).append(domainKindFlag);
                if ((domainFlags & 0xF0) == XulCacheCenter.CACHE_FLAG_PERSISTENT) {
                    flagBuilder.append(FLAG_SEPARATOR).append("PERSISTENT");
                }

                return flagBuilder.toString();
            }

            private String parseDomainLifeTime(long lifeTime) {
                return lifeTime == 0 ? "NO_TIME_LIMIT" : XulSystemUtil.formatDuring(lifeTime);
            }

            private String parseDomainCount(int count) {
                return count == Integer.MAX_VALUE ? "NO_COUNT_LIMIT" : Integer.toString(count);
            }

            private String parseDomainSize(long size) {
                return XulSystemUtil.formatSize(size);
            }
        });

        XulDebugServer.registerCommandHandler(new IXulDebugCommandHandler() {

            XulHttpServer.XulHttpServerHandler _serverHandler;

            @Override
            public XulHttpServer.XulHttpServerResponse execCommand(String url,
                                                                   XulHttpServer.XulHttpServerHandler serverHandler,
                                                                   XulHttpServer.XulHttpServerRequest request) {
                _serverHandler = serverHandler;
                boolean successful;
                if (url.startsWith("/api/clear-all-caches")) {
                    successful = clearAllCaches();
                } else if (url.startsWith("/api/clear-caches/")) {
                    successful = clearCaches(url.substring(18));
                } else {
                    return null;
                }

                XulHttpServer.XulHttpServerResponse response = _serverHandler.getResponse(request);
                if (successful) {
                    response.addHeader("Content-Type", "text/xml")
                            .writeBody("<result status=\"OK\"/>");
                } else {
                    response.setStatus(404).cleanBody();
                }
                return response;
            }

            private boolean clearCaches(String domainId) {
                XulCacheDomain domain = _cacheDomains.get(XulUtils.tryParseInt(domainId));
                if (domain != null) {
                    domain.clear();
                    return true;
                }

                return false;
            }

            private boolean clearAllCaches() {
                for (XulCacheDomain domain : _cacheDomains.values()) {
                    domain.clear();
                }

                return !_cacheDomains.isEmpty();
            }
        });
    }

    public static int getRevision() {
        return _revision;
    }

    public static void setRevision(int revision) {
        _revision = revision;
    }

    public static String getVersion() {
        return _version;
    }

    public static void setVersion(String version) {
        _version = version;
    }

    public static final class CacheDomainBuilder {

        public static final int DEFAULT_DOMAIN_LIFE_TIME = 0;   // 默认为长期有效
        public static final int DEFAULT_DOMAIN_FLAGS = CACHE_FLAG_WRITE_BACK;

        public static final String PREFIX_REVISION = "revision-";
        public static final String PREFIX_VERSION = "version-";
        public static final String PREFIX_GLOBAL = "global";

        /**
         * DomainId-DomainFlags-DomainLifeTime
         */
        private static final String CACHE_DOMAIN_DIR_FORMAT = "%d-%d-%d";

        private int _domainId;
        private int _domainFlags;
        private Context _context;
        private long _maxMemorySize;
        private int _maxMemoryCount;
        private long _maxFileSize;
        private int _maxFileCount;
        private long _lifeTime;

        private CacheDomainBuilder(Context context, int domainId) {
            _context = context;
            initBuilder(domainId);
        }

        public static CacheDomainBuilder obtainBuilder(Context context, int domainId) {
            return new CacheDomainBuilder(context, domainId);
        }

        public CacheDomainBuilder initBuilder(int domainId) {
            _domainId = domainId;
            _domainFlags = DEFAULT_DOMAIN_FLAGS;
            _lifeTime = DEFAULT_DOMAIN_LIFE_TIME;
            _maxMemorySize = DEFAULT_MAX_MEMORY_SIZE;
            _maxMemoryCount = DEFAULT_MAX_MEMORY_COUNT;
            _maxFileSize = DEFAULT_MAX_FILE_SIZE;
            _maxFileCount = DEFAULT_MAX_FILE_COUNT;
            return this;
        }

        public CacheDomainBuilder setDomainFlags(int domainFlag) {
            _domainFlags = domainFlag;
            return this;
        }

        public CacheDomainBuilder setLifeTime(long ms) {
            _lifeTime = ms;
            return this;
        }

        public CacheDomainBuilder setMaxMemorySize(long maxSize) {
            _maxMemorySize = maxSize;
            return this;
        }

        public CacheDomainBuilder setMaxMemoryCount(int maxCount) {
            _maxMemoryCount = maxCount;
            return this;
        }

        public CacheDomainBuilder setMaxFileSize(long maxFileSize) {
            _maxFileSize = maxFileSize;
            return this;
        }

        public CacheDomainBuilder setMaxFileCount(int maxFileCount) {
            _maxFileCount = maxFileCount;
            return this;
        }

        public synchronized XulCacheDomain build() {
            XulCacheDomain cachedDomain = _cacheDomains.get(_domainId);
            if (cachedDomain != null) {
                if (_domainFlags == cachedDomain.getDomainFlags()
                    && _lifeTime == cachedDomain.getLifeTime()) {
                    // 同一个domain，直接返回已存在的实例
                    return cachedDomain;
                } else {
                    // 不允许创建id相同但flag或者lifetime不同的cache domain
                    return null;
                }
            }

            File cacheDir = getCacheDir();
            if (cacheDir == null) {
                XulLog.e(TAG, "Cannot get cache directory.");
                return null;
            }

            XulCacheDomain domain;
            switch (_domainFlags & ~0xFF) {
                case CACHE_FLAG_PROPERTY:
                    domain = new XulPropertyCacheDomain(
                            _maxMemorySize, _maxMemoryCount, cacheDir, _maxFileSize, _maxFileCount);
                    break;
                case CACHE_FLAG_MEMORY:
                    domain = new XulMemoryCacheDomain(_maxMemorySize, _maxMemoryCount);
                    break;
                case CACHE_FLAG_FILE:
                    domain = new XulFileCacheDomain(cacheDir, _maxFileSize, _maxFileCount);
                    break;
                case CACHE_FLAG_WRITE_BACK:
                    domain = new XulWriteBackCacheDomain(
                            _maxMemorySize, _maxMemoryCount, cacheDir, _maxFileSize, _maxFileCount);
                    break;
                case CACHE_FLAG_WRITE_THROUGH:
                    domain = new XulWriteThroughCacheDomain(
                            _maxMemorySize, _maxMemoryCount, cacheDir, _maxFileSize, _maxFileCount);
                    break;
                default:
                    domain = new XulMemoryCacheDomain(_maxMemorySize, _maxMemoryCount);
                    break;
            }

            domain.setDomainId(_domainId);
            domain.setDomainFlags(_domainFlags);
            domain.setLifeTime(_lifeTime);
            _cacheDomains.put(_domainId, domain);
            return domain;
        }

        private File getCacheDir() {
            String rootCacheDir = XulSystemUtil.getDiskCacheDir(_context);
            if (TextUtils.isEmpty(rootCacheDir)) {
                XulLog.e(TAG, "Cannot get root cache directory.");
                return null;
            }

            File cacheDir;
            String cacheDirName;
            switch (_domainFlags & 0xF) {
                case CACHE_FLAG_REVISION_LOCAL:
                    cacheDirName = PREFIX_REVISION + getRevision();
                    cacheDir = new File(rootCacheDir, cacheDirName);
                    if (!cacheDir.exists()) {
                        // 目录不存在，可能存在旧的缓存，清除无效缓存
                        clearInvalidCache(rootCacheDir, PREFIX_REVISION, cacheDirName);
                    }
                    break;
                case CACHE_FLAG_VERSION_LOCAL:
                    cacheDirName = PREFIX_VERSION + getVersion();
                    cacheDir = new File(rootCacheDir, cacheDirName);
                    if (!cacheDir.exists()) {
                        clearInvalidCache(rootCacheDir, PREFIX_VERSION, cacheDirName);
                    }
                    break;
                case CACHE_FLAG_GLOBAL:
                    cacheDir = new File(rootCacheDir, PREFIX_GLOBAL);
                    break;
                default:
                    cacheDir = new File(rootCacheDir, PREFIX_GLOBAL);
                    break;
            }

            String domainName = String.format(CACHE_DOMAIN_DIR_FORMAT,
                                              _domainId, _domainFlags, _lifeTime);

            if (cacheDir.exists()) {
                for (File file : cacheDir.listFiles()) {
                    String fileName = file.getName();
                    if (fileName.startsWith(_domainId + "-") && !fileName.equals(domainName)) {
                        // 不允许创建id相同但flag或者lifetime不同的cache domain
                        return null;
                    }
                }
            }

            cacheDir = new File(cacheDir, domainName);
            return cacheDir;
        }

        private void clearInvalidCache(final String parent, final String prefix, final  String curFile) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File root = new File(parent);
                    File[] files;
                    if (root == null || (files = root.listFiles()) == null) {
                        return;
                    }
                    for (File file : files) {
                        if (file.getName().equals(curFile)) { //不删除当前缓存文件
                            continue;
                        }
                        if (file.getName().startsWith(prefix)) {
                            XulSystemUtil.deleteDir(file);
                        }
                    }
                }
            }).start();
        }
    }
}
