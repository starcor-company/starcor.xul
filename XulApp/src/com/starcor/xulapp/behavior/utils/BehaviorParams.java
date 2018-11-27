package com.starcor.xulapp.behavior.utils;

import android.graphics.Rect;
import android.text.TextUtils;
import com.starcor.xul.XulDataNode;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by skycnlr on 2018/11/2.
 */
public class BehaviorParams {
    public static final String XPARAM_X_FLAG = "xul_x_flag";
    public static final String XPARAM_Y_FLAG = "xul_y_flag";
    public static final String XPARAM_W_FLAG = "xul_w_flag";
    public static final String XPARAM_H_FLAG = "xul_h_flag";
    public static final String XPARAM_ID_FLAG = "xul_id_flag";
    public static final String XPARAM_COLOR_FLAG = "xul_color_flag";
    public static final String XPARAM_VISIBLE_FLAG = "xul_visible_flag";
    public static final String XPARAM_ENABLE_KEY_FLAG = "xul_enable_key_flag";
    public static final String XPARAM_HOST_BEHAVIOR_ID = "xul_host_behavior_id";
    public static final String XPARAM_BEHAVIOR_LAUNCH_MODE = "xul_behavior_launch_mode";

    public static class Builder {
        private final HashMap<String, String> param = new HashMap<>();

        public Builder(XulBehaviorUnit.LaunchMode launchMode) {
            setLaunchMode(launchMode);
        }
        public Builder setId(String id) {
            param.put(XPARAM_ID_FLAG, id);
            return this;
        }

        public Builder setVisible(boolean show) {
            param.put(XPARAM_VISIBLE_FLAG, show ? "1" : "0");
            return this;
        }

        public Builder setBounds(Rect rect) {
            param.put(XPARAM_X_FLAG, String.valueOf(rect.left));
            param.put(XPARAM_Y_FLAG, String.valueOf(rect.top));
            param.put(XPARAM_W_FLAG, String.valueOf(rect.width()));
            param.put(XPARAM_H_FLAG, String.valueOf(rect.height()));
            return this;
        }

        public Builder setHostId(String hostId) {
            param.put(XPARAM_HOST_BEHAVIOR_ID, hostId);
            return this;
        }

        public Builder setBackgroundColor(int color) {
            param.put(XPARAM_COLOR_FLAG, String.valueOf(color));
            return this;
        }

        public Builder setEnableKey(boolean enable) {
            param.put(XPARAM_ENABLE_KEY_FLAG, enable? "1" : "0");
            return this;
        }

        public Builder setLaunchMode(XulBehaviorUnit.LaunchMode launchMode) {
            if (launchMode != null) {
                param.put(XPARAM_BEHAVIOR_LAUNCH_MODE, launchMode.name());
            } else {
                param.put(XPARAM_BEHAVIOR_LAUNCH_MODE, XulBehaviorUnit.LaunchMode.FLAG_BEHAVIOR_ATTACH_STACK.name());
            }
            return this;
        }

        public XulDataNode build() {
            XulDataNode behaviorParams = XulDataNode.obtainDataNode("behavior");
            if (param == null) {
                return behaviorParams;
            }
            Iterator iterator = param.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<String, String> entry = (HashMap.Entry) iterator.next();
                String key = entry.getKey();
                String value = entry.getValue();

                if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                    continue;
                }
                behaviorParams.appendChild(key, value);
            }

            return behaviorParams;
        }
    }

}
