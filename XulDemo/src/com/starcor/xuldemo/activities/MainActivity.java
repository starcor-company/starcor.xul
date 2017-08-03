package com.starcor.xuldemo.activities;

import android.content.Intent;
import android.text.TextUtils;
import com.starcor.xul.XulView;
import com.starcor.xuldemo.utils.log.LogUtil;

/**
 *
 */
public class MainActivity extends XulDemoBaseActivity {

    private static final String PAGE_MAIN = "page_main";

    public MainActivity() {
        super();
        mPageId = PAGE_MAIN;
    }

    @Override
    public void onDoAction(XulView view, String action, String type, String command,
                           Object userdata) {
        if ("click".equals(action)) {
            String pageId = command;
            if (TextUtils.isEmpty(pageId)) {
                LogUtil.e("XUL demo page id is null!");
                return;
            }
            String pageBehavior = view.getAttrString("behavior");
            String pageLayout = view.getAttrString("layout");

            Intent intent = new Intent(this, CommonPageActivity.class);
            intent.putExtra(XulDemoBaseActivity.EXTRA_XUL_PAGE_ID, pageId);
            intent.putExtra(XulDemoBaseActivity.EXTRA_XUL_PAGE_BEHAVIOR, pageBehavior);
            intent.putExtra(XulDemoBaseActivity.EXTRA_XUL_FILE_NAME, pageLayout);
            startActivity(intent);
        }
    }
}
