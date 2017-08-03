package com.starcor.xuldemo;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;

import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.XulDataNode;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulWorker;
import com.starcor.xuldemo.behavior.XABBindingDemo;
import com.starcor.xuldemo.behavior.XABCustomViewDemo;
import com.starcor.xuldemo.behavior.XABMassiveDemo;
import com.starcor.xuldemo.behavior.XABSelectorSliderDemo;
import com.starcor.xuldemo.utils.log.LogFilter;
import com.starcor.xuldemo.utils.log.LogUtil;
import com.starcor.xuldemo.widget.XulCustomRender;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ZFB on 2015/7/16 0016.
 */
public class XulDemoApp extends Application {

    private static final String TAG = XulDemoApp.class.getSimpleName();

    private static final String IMAGE_PREPROCESS_SCALE = "scale:";
    private static final String IMAGE_PREPROCESS_EFFECT_PARALLELOGRAM = "effect:parallelogram:";
    private static final String IMAGE_PREPROCESS_EFFECT_HEXAGON = "effect:hexagon:";
    private static final String IMAGE_PREPROCESS_MANUAL_LOAD = "manual_image";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            XulDataNode data = XulDataNode.build(this.getAssets().open("MetaDataIndex.xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        initBehaviors();
        initLogUtil();
        initXul();
    }

    private void initBehaviors() {
        XABMassiveDemo.register();
        XABCustomViewDemo.register();
        XABBindingDemo.register();
        XABSelectorSliderDemo.register();
    }

    private void initLogUtil() {
        LogUtil.setEnabled(true);
        LogUtil.setGlobalTag(TAG);

        //Log文件输出配置
        //LogUtil.setLog2FileEnabled(false); // log默认不会输出到文件
        //LogUtil.setFilePathGenerator(new FilePathGenerator.LimitSizeFilePathGenerator(context));
        //LogUtil.setLogFormatter(new LogFormatter.IDEAFormatter());

        // If debug environment, will log more information;
        LogUtil.LEVEL filterLever =
                XulDemoEnv.DEBUGGABLE ? LogUtil.LEVEL.DEBUG : LogUtil.LEVEL.ERROR;
        LogUtil.addLogFilter(new LogFilter.LevelFilter(filterLever));
    }

    private void initXul() {
        final Context context = getApplicationContext();

        LogUtil.i(TAG, "Init XUL, BEGIN");
        XulWorker.setHandler(new XulWorker.IXulWorkerHandler() {

            @Override
            public InputStream getAssets(String path) {
                LogUtil.i(TAG, "getAssets invoked, path=" + path);
                try {
                    return context.getAssets().open(path);
                } catch (IOException e) {
                    LogUtil.e(TAG, e);
                }
                return null;
            }

            @Override
            public InputStream getAppData(String path) {
                LogUtil.i(TAG, "getAppData invoked, path=" + path);
                return null;
            }

            @Override
            public String resolvePath(XulWorker.DownloadItem downloadItem, String path) {
                LogUtil.i(TAG, "resolvePath invoked, path=" + path);

                // 目前只处理图片
                if (!(downloadItem instanceof XulWorker.DrawableItem)) {
                    return null;
                }

                if (path.equals(IMAGE_PREPROCESS_SCALE)) {
                    return "";
                } else if (path.startsWith(IMAGE_PREPROCESS_SCALE)) {
                    XulWorker.DrawableItem imgItem = (XulWorker.DrawableItem) downloadItem;
                    path = path.substring(IMAGE_PREPROCESS_SCALE.length());
                    if (imgItem.target_width != 0 && imgItem.target_height != 0) {
                        int i = path.lastIndexOf('/');
                        if (i > 0) {
                            path = String.format("%s/%dx%d/%s"
                                    , path.substring(0, i)
                                    , imgItem.target_width, imgItem.target_height
                                    , path.substring(i + 1));
                        }
                    }
                    return path;
                } else if (path.startsWith(IMAGE_PREPROCESS_EFFECT_PARALLELOGRAM)) {
                    return path.substring(IMAGE_PREPROCESS_EFFECT_PARALLELOGRAM.length());
                } else if (path.startsWith(IMAGE_PREPROCESS_EFFECT_HEXAGON)) {
                    return path.substring(IMAGE_PREPROCESS_EFFECT_HEXAGON.length());
                } else if (path.startsWith(IMAGE_PREPROCESS_MANUAL_LOAD)) {
                    ApplicationInfo appInfo = context.getApplicationInfo();
                    Drawable appIcon = appInfo.loadIcon(context.getPackageManager());
                    XulDrawable drawable = XulDrawable.fromDrawable(appIcon, path, path);
                    XulWorker.addDrawableToCache(path, drawable);
                    return path;
                }

                return null;
            }

            @Override
            public InputStream loadCachedData(String path) {
                LogUtil.i(TAG, "loadCachedData invoked, path=" + path);
                return null;
            }

            @Override
            public boolean storeCachedData(String path, InputStream stream) {
                LogUtil.i(TAG, "storeCachedData invoked, path=" + path);
                return false;
            }

            @Override
            public String calCacheKey(String url) {
                LogUtil.i(TAG, "calCacheKey invoked, url=" + url);
                return null;
            }

            @Override
            public Bitmap preprocessImage(XulWorker.DrawableItem drawableItem, Bitmap bitmap) {
                LogUtil.i(TAG, "preprocessImage invoked, drawableItem=" + drawableItem + ", bitmap="
                               + bitmap);
                if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
                    return null;
                }
                if (drawableItem.url.startsWith("effect:hexagon:")) {
                    if (hexagonBitmap == null) {
                        return null;
                    }
                    return getClipBitmap(bitmap, hexagonBitmap, false);
                }
                if (drawableItem.url.startsWith("effect:parallelogram:")) {
                    if (parallelogram == null) {
                        return null;
                    }
                    return getClipBitmap(bitmap, parallelogram, true);
                }
                return null;
            }

            @Override
            public boolean preloadImage(XulWorker.DrawableItem drawableItem, Rect rcOut) {
                LogUtil.i(TAG, "preloadImage invoked, drawableItem=" + drawableItem + ", rcOut="
                               + rcOut);
                return false;
            }

            private Bitmap getClipBitmap(Bitmap bitmap, Bitmap protoBitmap, boolean drawBottom) {
                Bitmap emptyBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                                                         Bitmap.Config.ARGB_8888);
                int id = (int) Thread.currentThread().getId();
                Canvas canvas;
                synchronized (canvasSparseArray) {
                    canvas = canvasSparseArray.get(id);
                    if (canvas == null) {
                        canvas = new Canvas();
                        canvasSparseArray.put(id, canvas);
                    }
                }
                canvas.setBitmap(emptyBitmap);
                canvas.drawBitmap(bitmap, null,
                                  new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), null);
                XulUtils.saveCanvas(canvas);
                canvas.clipRect(0, bitmap.getHeight() * 0.74f, bitmap.getWidth(),
                                bitmap.getHeight());
                canvas.drawColor(0xff914cff);
                XulUtils.restoreCanvas(canvas);
                canvas.drawBitmap(protoBitmap, null,
                                  new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), clipPaint);
                canvas.setBitmap(null);
                return emptyBitmap;
            }

            SparseArray<Canvas> canvasSparseArray = new SparseArray<Canvas>();
            Paint clipPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

            {
                clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            }

            Bitmap hexagonBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hexagon);
            Bitmap parallelogram =
                    BitmapFactory.decodeResource(getResources(), R.drawable.parallelogram);
        });

        XulCustomRender.register();

        XulRenderContext.addTypeFace("scyahei",
                                     Typeface.createFromAsset(getAssets(), "fonts/scyahei.ttf"));
        XulManager.setBaseTempPath(context.getDir("xul", Context.MODE_PRIVATE));
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        XulManager.setPageSize(displayMetrics.widthPixels, displayMetrics.heightPixels);
        loadXulFile(context, XulDemoEnv.XUL_VIEW_DEMO_FILE);
        loadXulFile(context, XulDemoEnv.XUL_PAGE_DEMO_FILE);
        loadXulFile(context, XulDemoEnv.XUL_ANIM_DEMO_FILE);
        loadXulFile(context, XulDemoEnv.XUL_FOCUS_DEMO_FILE);

        LogUtil.i(TAG, "Init XUL, END");
    }

    /**
     * 加载Xul文件，增量加载
     *
     * @param context  当前上下文
     * @param fileName xul文件名
     * @return true，加载成功，false，加载失败
     */
    public static boolean loadXulFile(Context context, String fileName) {
        boolean loadResult = false;
        if (!TextUtils.isEmpty(fileName)) {
            try {
                InputStream input = context.getAssets().open(fileName);
                loadResult = XulManager.loadXul(input, fileName);
                LogUtil.i(TAG, "Xul file is loaded=" + loadResult);
            } catch (IOException e) {
                LogUtil.i(TAG, "Load Xul file failed", e);
            }
        }

        return loadResult;
    }
}
