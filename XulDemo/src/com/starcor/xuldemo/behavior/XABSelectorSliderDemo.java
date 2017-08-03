package com.starcor.xuldemo.behavior;

import android.text.TextUtils;
import android.widget.RelativeLayout;

import com.starcor.xul.IXulExternalView;
import com.starcor.xul.Render.XulSliderAreaRender;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulPage;
import com.starcor.xul.XulView;
import com.starcor.xuldemo.utils.log.LogUtil;
import com.starcor.xuldemo.widget.XulExt_ExternalEditBox;

/**
 * Created by hy on 2015/8/7.
 */
public class XABSelectorSliderDemo extends XulActivityBehavior {

    private static final String CITIES_BINDING = "cities";
    private static final String CITIES_DATA_DIR = "file:///.assets/provinces/";

    public static void register() {
        XulActivityBehavior
                .registerBehavior("behavior_selector_slider_demo", new IBehaviorFactory() {
                    @Override
                    public XulActivityBehavior create() {
                        return new XABSelectorSliderDemo();
                    }
                });
    }

    @Override
    public void onDoAction(XulView view, String action, String type, String command,
                           Object userdata) {
        if ("onDecide".equals(command)) {
            // TODO: 用户选定省份和城市，请在代码中处理业务逻辑
        } else if ("onProvinceChecked".equals(command)) {
            String citiesData = (String) userdata;
            if (TextUtils.isEmpty(citiesData)) {
                LogUtil.e("Cities data is empty, please check it!");
                return;
            }

            // 请使用InputStream实现
            XulPage page = mXulRenderContext.getPage();
            page.refreshBinding(CITIES_BINDING, CITIES_DATA_DIR + citiesData);
        } else if ("onCityItemUpdated".equals(command)) {
            // 还原city slider状态, 必须在layout后执行
            mXulRenderContext.scheduleLayoutFinishedTask(new Runnable() {
                @Override
                public void run() {
                    XulPage page = mXulRenderContext.getPage();
                    XulArea cityArea = (XulArea) page.findItemById("city_area");
                    XulSliderAreaRender sliderRender = (XulSliderAreaRender) page.findItemById(
                            cityArea, "slider_area").getRender();
                    // 若slider没有被显示过，则不需要重置
                    if (sliderRender.getDrawingRect() != null) {
                        sliderRender.scrollTo(0, false);
                    }
                }
            });
        }
    }

    @Override
    public IXulExternalView createExternalView(String cls, int x, int y, int width, int height,
                                               XulView view) {
        if ("EditBox".equals(cls)) {
            XulExt_ExternalEditBox editBox = new XulExt_ExternalEditBox(mXulActivity, view);
            ((RelativeLayout) mLayout).addView(editBox, width, height);
            return editBox;
        }

        return null;
    }
}
