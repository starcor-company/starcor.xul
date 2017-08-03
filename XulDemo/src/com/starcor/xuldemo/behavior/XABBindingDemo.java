package com.starcor.xuldemo.behavior;

import com.starcor.xul.XulView;
import com.starcor.xuldemo.utils.log.LogUtil;

/**
 * Created by hy on 2015/8/7.
 */
public class XABBindingDemo extends XulActivityBehavior {

    public static void register() {
        XulActivityBehavior.registerBehavior("behavior_binding_demo", new IBehaviorFactory() {
            @Override
            public XulActivityBehavior create() {
                return new XABBindingDemo();
            }
        });
    }

    @Override
    public void onDoAction(XulView view, String action, String type, String command,
                           Object userdata) {
        final String TAG_BINDING_EVENT = "BindingEvent";
        LogUtil.i(TAG_BINDING_EVENT, action + " " + type + " " + command);

        if ("load".equals(action)) {

        } else if ("ready".equals(action)) {

        } else if ("bindingFinished".equals(action)) {

        } else if ("bindingUpdated".equals(action)) {

        } else if ("incrementalBindingUpdate".equals(action)) {
            LogUtil.e(
                    "Should not print this log! incrementalBindingUpdate has been handled by js!");
        } else if ("incrementalBindingFinished".equals(action)) {
            LogUtil.e(
                    "Should not print this log! incrementalBindingFinished has been handled by js!");
        } else if ("bindingReady".equals(action)) {

        } else if ("bindingError".equals(action)) {

        } else if ("click".equals(action)) {
            // 刷新网络绑定源
            mXulRenderContext.getPage().refreshBinding("network_source");
        } else {
            LogUtil.e("Should not print this log! No other event!");
        }
    }
}
