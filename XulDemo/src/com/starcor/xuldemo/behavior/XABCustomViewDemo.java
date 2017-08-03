package com.starcor.xuldemo.behavior;

import android.widget.RelativeLayout;

import com.starcor.xul.IXulExternalView;
import com.starcor.xul.XulView;
import com.starcor.xuldemo.widget.XulExt_ExternalEditBox;

/**
 * Created by hy on 2015/8/7.
 */
public class XABCustomViewDemo extends XulActivityBehavior {

    public static void register() {
        XulActivityBehavior.registerBehavior("behavior_custom_view_demo", new IBehaviorFactory() {
            @Override
            public XulActivityBehavior create() {
                return new XABCustomViewDemo();
            }
        });
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
