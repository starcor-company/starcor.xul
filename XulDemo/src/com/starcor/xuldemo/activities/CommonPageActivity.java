package com.starcor.xuldemo.activities;

import android.content.Intent;
import android.os.Bundle;

/**
 * Created by ZFB on 2015/8/6 0006.
 */
public class CommonPageActivity extends XulDemoBaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (intent != null) {
            mPageId = intent.getStringExtra(EXTRA_XUL_PAGE_ID);
            mXulFileName = intent.getStringExtra(EXTRA_XUL_FILE_NAME);
            mXulPageBehavior = intent.getStringExtra(EXTRA_XUL_PAGE_BEHAVIOR);
        }
        super.onCreate(savedInstanceState);
    }
}
