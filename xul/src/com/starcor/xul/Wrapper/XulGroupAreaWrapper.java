package com.starcor.xul.Wrapper;

import com.starcor.xul.Render.XulGroupRender;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulView;

import java.util.ArrayList;

/**
 * Created by hy on 2014/6/3.
 */
public class XulGroupAreaWrapper extends XulViewWrapper {
	public static XulGroupAreaWrapper fromXulView(XulView view) {
		if (view == null) {
			return null;
		}
		if (!(view.getRender() instanceof XulGroupRender)) {
			return null;
		}
		return new XulGroupAreaWrapper((XulArea) view);
	}

	XulArea _area;

	XulGroupAreaWrapper(XulArea area) {
		super(area);
		_area = area;
	}

	public void setAllUnchecked() {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return;
		}
		render.setAllUnchecked();
	}

	// CHECK模式：设置所有项为选中状态
	// RADIO模式：设置第一项为选中状态
	public void setAllChecked() {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return;
		}
		render.setAllChecked();
	}

	public void setUnchecked(XulView view) {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return;
		}
		render.setUnchecked(view);
	}

	public void setChecked(XulView view) {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return;
		}
		render.setChecked(view);
	}

	public boolean isChecked(XulView view) {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return false;
		}
		return render.isChecked(view);
	}

	public ArrayList<XulView> getAllGroupItems() {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return null;
		}
		return render.getAllGroupItems();
	}

	public ArrayList<ArrayList<XulView>> getAllGroups() {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return null;
		}
		return render.getAllGroups();
	}

	public ArrayList<XulView> getGroupByItem(XulView view) {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return null;
		}
		return render.getGroupByItem(view);
	}

	public ArrayList<XulView> getAllCheckedItems() {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return null;
		}
		return render.getAllCheckedItems();
	}

	public ArrayList<ArrayList<XulView>> getAllCheckedGroups() {
		XulGroupRender render = (XulGroupRender) _area.getRender();
		if (render == null) {
			return null;
		}
		return render.getAllCheckedGroups();
	}

}
