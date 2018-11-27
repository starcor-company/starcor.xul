package com.starcor.xulapp;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.view.Display;
import android.view.WindowManager;

import com.starcor.xul.XulManager;
import com.starcor.xul.XulWorker;
import com.starcor.xulapp.behavior.XulBehaviorManager;
import com.starcor.xulapp.cache.XulCacheCenter;
import com.starcor.xulapp.cache.XulCacheDomain;
import com.starcor.xulapp.model.XulDataServiceImpl;
import com.starcor.xulapp.service.XulServiceManager;
import com.starcor.xulapp.utils.XulResPrefetchManager;
import com.starcor.xulapp.utils.XulSystemUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by hy on 2015/8/31.
 */
public class XulApplication extends Application {

	protected final String TAG = getClass().getSimpleName();

	private static XulApplication _appInst;

	private static Handler _appMainHandler;

	public static XulApplication getAppInstance() {
		return _appInst;
	}

	public static Context getAppContext() {
		return _appInst.getApplicationContext();
	}

	public XulApplication() {
		super();
		_appInst = this;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		initCacheCenter();
		XulResPrefetchManager.init();

		_appMainHandler = new Handler(getMainLooper());

		XulBehaviorManager.initBehaviorManager();
		onRegisterXulBehaviors();

		XulServiceManager.initServiceManager();
		onRegisterXulServices();

		onInitXul();
	}

	protected void initCacheCenter() {
		XulCacheCenter.setRevision(XulSystemUtil.getAppVersion(getAppContext()));
		XulCacheCenter.setVersion(XulSystemUtil.getCurrentVersion(getAppContext()));

		_xulCacheDomain = XulCacheCenter.buildCacheDomain(CACHE_DOMAIN_ID_APP)
				.setDomainFlags(XulCacheCenter.CACHE_FLAG_FILE
								| XulCacheCenter.CACHE_FLAG_REVISION_LOCAL)
				.setLifeTime(CACHE_LIFETIME)
				.setMaxFileSize(CACHE_MAX_SIZE)
				.build();
	}

	public void postToMainLooper(Runnable runnable) {
		_appMainHandler.post(runnable);
	}

	public void postDelayToMainLooper(Runnable runnable, long ms) {
		_appMainHandler.postDelayed(runnable, ms);
	}

	public void removeMainLooperCallBack(Runnable runnable) {
		_appMainHandler.removeCallbacks(runnable);
	}

	public void onRegisterXulServices() {
		XulDataServiceImpl.register();
	}

	@Override
	public void onTerminate() {
		XulServiceManager.shutdownServiceManager();
		XulBehaviorManager.shutdownBehaviorManager();
		XulCacheCenter.close();
		super.onTerminate();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
	}

	public void onInitXul() {
		{
			// initialize global xul page size
			WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point displaySize = new Point();
			display.getSize(displaySize);
			int pageWidth = displaySize.x;
			int pageHeight = displaySize.y;

			// fix some device with incompatible resolution
			if (pageHeight > 665 && pageHeight < 720) {
				pageHeight = 720;
			} else if (pageHeight > 1000 && pageHeight < 1080) {
				pageHeight = 1080;
			}

			XulManager.setPageSize(pageWidth, pageHeight);
		}
		onInitXulWorker();
		onLoadXul();
	}

	public void onRegisterXulBehaviors() {
	}

	public void onLoadXul() {
	}

	public void onInitXulWorker() {
		XulWorker.setHandler(new XulWorker.IXulWorkerHandler() {
			@Override
			public InputStream getAssets(String path) {
				return XulApplication.this.xulGetAssets(path);
			}

			@Override
			public InputStream getAppData(String path) {
				return XulApplication.this.xulGetAppData(path);
			}

			@Override
			public InputStream getSdcardData(String path) {
				return XulApplication.this.xulGetSdcardData(path);
			}

			@Override
			public String resolvePath(XulWorker.DownloadItem downloadItem, String path) {
				return XulApplication.this.xulResolvePath(downloadItem, path);
			}

			@Override
			public InputStream loadCachedData(String path) {
				return XulApplication.this.xulLoadCachedData(path);
			}

			@Override
			public boolean storeCachedData(String path, InputStream stream) {
				return XulApplication.this.xulStoreCachedData(path, stream);
			}

			@Override
			public String calCacheKey(String url) {
				return XulApplication.this.xulCalCacheKey(url);
			}

			@Override
			public boolean preloadImage(XulWorker.DrawableItem drawableItem, Rect rcOut) {
				return XulApplication.this.xulPreloadImage(drawableItem, rcOut);
			}

			@Override
			public Bitmap preprocessImage(XulWorker.DrawableItem drawableItem, Bitmap bitmap) {
				return XulApplication.this.xulPreprocessImage(drawableItem, bitmap);
			}
		});
	}

	public Bitmap xulPreprocessImage(XulWorker.DrawableItem drawableItem, Bitmap bitmap) {
		return null;
	}

	public boolean xulPreloadImage(XulWorker.DrawableItem drawableItem, Rect rcOut) {
		return false;
	}

	public String xulCalCacheKey(String url) {
		if (url == null) {
			return "";
		}
		int idx = url.indexOf("http://");
		if (idx < 0) {
			return url;
		}
		idx = url.indexOf('/', idx + 7);
		if (idx < 0) {
			return url;
		}
		return url.substring(idx);
	}

	protected static final int CACHE_DOMAIN_ID_APP = 0x1001;
	protected static final int CACHE_MAX_SIZE = 128 * 1024 * 1024;	// 最大保存128M
	protected static final long CACHE_LIFETIME = TimeUnit.DAYS.toMillis(7);	// 最多保存7天
	protected XulCacheDomain _xulCacheDomain;

	public boolean xulStoreCachedData(String path, InputStream stream) {
		_xulCacheDomain.put(path, stream);
		return true;
	}

	public InputStream xulLoadCachedData(String path) {
		return _xulCacheDomain.getAsStream(path);
	}

	public String xulResolvePath(XulWorker.DownloadItem downloadItem, String path) {
		return null;
	}

	public InputStream xulGetAppData(String path) {
		return null;
	}

    public InputStream xulGetSdcardData(String path) {
        try {
            return new FileInputStream(Environment.getExternalStorageDirectory().getPath() + File.separator + path);
        } catch (IOException e) {
        }

        return null;
    }

	public InputStream xulGetAssets(String path) {
		try {
			return getAssets().open(path, MODE_PRIVATE);
		} catch (IOException e) {
//			XulLog.e(TAG, e);
		}
		return null;
	}

	public void xulLoadLayouts(String xulLayout) {
		InputStream xulMainXml = xulGetAssets(xulLayout);
		if (xulMainXml == null) {
			return;
		}
		XulManager.loadXul(xulMainXml, xulLayout);
	}

}
