package com.starcor.xulapp.behavior.utils;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.starcor.xulapp.behavior.XulUiBehavior;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by skycnlr on 2018/9/26.
 */

public class BehaviorProcessor {
    private XulBehaviorUnit temp;
    private List<List<XulBehaviorUnit>> linkedList = new LinkedList<>();

    public boolean dispatchKeyEvent(KeyEvent event) {
         if (isValidStack()) {
             List<XulBehaviorUnit> top = linkedList.get(linkedList.size() - 1);
             int index = top.size() - 1;
             for (; index > -1; index--) {
                 temp = top.get(index);
                 if (temp != null && temp.responseKeyEvent && temp.xulOnDispatchKeyEvent(event)) {
                     return true;
                 }
             }
         }
        return false;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isValidStack()) {
            List<XulBehaviorUnit> top = linkedList.get(linkedList.size() - 1);
            int index = top.size() - 1;
            for (; index > -1; index--) {
                temp = top.get(index);
                if (temp != null && temp.responseKeyEvent &&temp.xulOnDispatchTouchEvent(event)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean xulOnBackPressed() {
        if (isValidStack()) {
            List<XulBehaviorUnit> top = linkedList.get(linkedList.size() - 1);
            int index = top.size() - 1;
            for (; index > -1; index--) {
                temp = top.get(index);
                if (temp != null && temp.responseKeyEvent && temp.xulOnBackPressed()) {
                    return true;
                }
            }

            if (linkedList.size() == 1) {
                return false;
            }
            changeToState(top, XulBehaviorUnit.STATE_DESTROY);
            linkedList.remove(top);
            changeToState(linkedList.get(linkedList.size() - 1), XulBehaviorUnit.STATE_RESTART);
        }
        return true;
    }

    public boolean xulOnPause() {
        List<XulBehaviorUnit> top = linkedList.get(linkedList.size() - 1);
        if (top != null) {
            changeToState(top, XulBehaviorUnit.STATE_PAUSE);
        }
        return true;
    }

    public boolean xulOnNewIntent(Intent intent) {
        return true;
    }

    public boolean xulOnSaveInstanceState(Bundle outState) {
        return true;
    }

    public boolean xulOnRestoreInstanceState(Bundle savedInstanceState) {
        return true;
    }

    public boolean xulOnStop() {
        List<XulBehaviorUnit> top = linkedList.get(linkedList.size() - 1);
        if (top != null) {
            changeToState(top, XulBehaviorUnit.STATE_STOP);
        }
        return true;
    }

    public boolean xulOnStart() {
        List<XulBehaviorUnit> top = linkedList.get(linkedList.size() - 1);
        if (top != null) {
            changeToState(top, XulBehaviorUnit.STATE_START);
        }
        return true;
    }

    public boolean xulOnResume() {
        List<XulBehaviorUnit> top = linkedList.get(linkedList.size() - 1);
        if (top != null) {
            changeToState(top, XulBehaviorUnit.STATE_RESUME);
        }
        return true;
    }

    public boolean xulOnRestart() {
        List<XulBehaviorUnit> top = linkedList.get(linkedList.size() - 1);
        if (top != null) {
            changeToState(top, XulBehaviorUnit.STATE_RESTART);
        }
        return true;
    }

    public boolean xulOnDestroy() {
        if (isValidStack()) {
            List<XulBehaviorUnit> top = linkedList.get(linkedList.size() - 1);
            changeToState(top, XulBehaviorUnit.STATE_DESTROY);
        }
        return false;
    }

    public boolean closeBehavior(String id) {
        List<List<XulBehaviorUnit>> units = linkedList;
        if (units == null || units.size() == 0) {
            return false;
        }
        XulBehaviorUnit info;
        List<XulBehaviorUnit> pre = null;
        for (List<XulBehaviorUnit> unit : units) {
            info = has(unit, id);
            if (info != null) {
                info.changeToState(XulBehaviorUnit.STATE_DESTROY);
                unit.remove(info);
                if (unit.size() == 0) {
                    changeToState(pre, XulBehaviorUnit.STATE_SHOW);
                }
                break;
            }
            pre = unit;
        }
        return true;
    }

    public boolean add(XulBehaviorUnit model) {
        if (model == null || linkedList == null) {
            return false;
        }
        if (linkedList.size() == 0) {
            ArrayList<XulBehaviorUnit> list = new ArrayList<>();
            linkedList.add(list);
        }
        List<XulBehaviorUnit> pre = linkedList.get(linkedList.size() - 1);
        if (model.getLaunchMode() == XulBehaviorUnit.LaunchMode.FLAG_BEHAVIOR_ATTACH_STACK) {
            pre.add(model);
            model.changeToState(XulBehaviorUnit.STATE_CREATE);
            model.changeToState(XulBehaviorUnit.STATE_RESUME);
        } else {
            ArrayList<XulBehaviorUnit> cur = new ArrayList<>();
            cur.add(model);
            linkedList.add(cur);

            changeToState(pre, XulBehaviorUnit.STATE_PAUSE);
            model.changeToState(XulBehaviorUnit.STATE_CREATE);
            model.changeToState(XulBehaviorUnit.STATE_RESUME);
            changeToState(pre, XulBehaviorUnit.STATE_STOP);
        }
        return true;
    }

    public IBehaviorOperation getXulBehaviorOperation(String id) {
        List<List<XulBehaviorUnit>> units = linkedList;
        if (units == null || units.size() == 0) {
            return null;
        }
        XulBehaviorUnit info;
        for (List<XulBehaviorUnit> unit : units) {
            info = has(unit, id);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    public IBehaviorOperation getXulBehaviorOperation(XulUiBehavior behavior) {
        List<List<XulBehaviorUnit>> units = linkedList;
        if (units == null || units.size() == 0) {
            return null;
        }
        XulBehaviorUnit info;
        for (List<XulBehaviorUnit> unit : units) {
            info = has(unit, behavior);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    private XulBehaviorUnit has(List<XulBehaviorUnit> list, String id) {
        for (XulBehaviorUnit unit: list) {
            if (unit.id.equalsIgnoreCase(id)) {
                return unit;
            }
        }
        return null;
    }

    private XulBehaviorUnit has(List<XulBehaviorUnit> list, XulUiBehavior behavior) {
        for (XulBehaviorUnit unit: list) {
            if (unit.uiBehavior == behavior) {
                return unit;
            }
        }
        return null;
    }

    private boolean changeToState(List<XulBehaviorUnit> lists, int state) {
        if (lists == null || lists.size() == 0) {
            return false;
        }
        for (XulBehaviorUnit info : lists) {
            info.changeToState(state);
        }
        return true;
    }

    private boolean isValidStack() {
        return linkedList != null && linkedList.size() > 0;
    }
}
