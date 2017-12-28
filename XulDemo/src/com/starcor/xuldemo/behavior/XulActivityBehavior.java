package com.starcor.xuldemo.behavior;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;

import com.starcor.xul.IXulExternalView;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulView;
import com.starcor.xul.XulWorker;

import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by hy on 2015/8/7.
 */
public abstract class XulActivityBehavior implements XulRenderContext.IXulRenderHandler {
    @Override
    public InputStream getSdcardData(XulWorker.DownloadItem item, String path) {
        return null;
    }

    protected Activity mXulActivity;
    protected View mLayout;
    protected XulRenderContext mXulRenderContext;

    public void initLayout(Activity xulActivity, View layout) {
        mXulActivity = xulActivity;
        mLayout = layout;
    }

    public void initXulRender(XulRenderContext xulRenderContext) {
        mXulRenderContext = xulRenderContext;
    }

    @Override
    public void invalidate(Rect rect) {

    }

    @Override
    public void uiRun(Runnable runnable) {

    }

    @Override
    public void uiRun(Runnable runnable, int delayMS) {

    }

    @Override
    public void onDoAction(XulView view, String action, String type, String command,
                           Object userdata) {

    }

    @Override
    public IXulExternalView createExternalView(String cls, int x, int y, int width, int height,
                                               XulView view) {
        return null;
    }

    @Override
    public String resolve(XulWorker.DownloadItem item, String path) {
        return null;
    }

    @Override
    public InputStream getAssets(XulWorker.DownloadItem item, String path) {
        return null;
    }

    @Override
    public InputStream getAppData(XulWorker.DownloadItem item, String path) {
        return null;
    }

    @Override
    public void onRenderIsReady() {

    }

    @Override
    public void onRenderEvent(int eventId, int param1, int param2, Object msg) {

    }

    public interface IBehaviorFactory {

        XulActivityBehavior create();
    }

    private static HashMap<String, IBehaviorFactory>
            behaviorMap =
            new HashMap<String, IBehaviorFactory>();

    public static void registerBehavior(String behaviorName, IBehaviorFactory factory) {
        behaviorMap.put(behaviorName, factory);
    }

    public static XulActivityBehavior createBehavior(String behaviorName) {
        IBehaviorFactory iBehaviorFactory = behaviorMap.get(behaviorName);
        if (iBehaviorFactory == null) {
            return null;
        }
        return iBehaviorFactory.create();
    }

}
