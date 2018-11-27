package com.starcor.xulapp.behavior.utils;

import android.graphics.Rect;

/**
 * Created by skycnlr on 2018/9/6.
 */
public interface IBehaviorOperation {
    boolean close();
    boolean show();
    boolean hide();
    boolean changeBounds(Rect rect) ;
    boolean enableKeyEvent(boolean enable);
    boolean setBackgroundColor(int color);
    Rect getBounds() ;
}
