package com.starcor.xulapp.behavior.utils;

import android.content.Intent;
import com.starcor.xulapp.behavior.XulUiBehavior;

/**
 * Created by skycnlr on 2018/8/23.
 */
public interface IBehaviorContact {

    boolean xulOpenBehavior(Intent o) ;

    boolean xulCloseBehavior(String id) ;

    IBehaviorOperation getXulBehaviorOperation(XulUiBehavior behavior);

    IBehaviorOperation getXulBehaviorOperation(String id);
}
