package com.starcor.xul.Wrapper;

import android.graphics.RectF;
import com.starcor.xul.Render.XulSliderAreaRender;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/3.
 */
public class XulSliderAreaWrapper extends XulViewWrapper {
	public static XulSliderAreaWrapper fromXulView(XulView view) {
		if (view == null) {
			return null;
		}
		if (!(view.getRender() instanceof XulSliderAreaRender)) {
			return null;
		}
		return new XulSliderAreaWrapper((XulArea) view);
	}

	XulArea _area;

	XulSliderAreaWrapper(XulArea area) {
		super(area);
		_area = area;
	}

	public void scrollByPage(int pages, boolean animation) {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}
		render.scrollByPage(pages, animation);
	}

	public void scrollTo(int pos) {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}
		render.scrollTo(pos, true);
	}

	public void scrollTo(int pos, boolean animation) {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}
		render.scrollTo(pos, animation);
	}

	public int getScrollPos() {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return 0;
		}
		return render.getScrollPos();
	}

	public int getScrollDelta() {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return 0;
		}
		return render.getScrollDelta();
	}

	public int getScrollRange() {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return 0;
		}
		return render.getScrollRange();
	}

	public void activateScrollBar() {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}
		render.activateScrollBar();
	}

	public boolean isVertical() {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return false;
		}
		return render.isVertical();
	}

	public boolean makeChildVisible(XulView view) {
		return makeChildVisible(view, true);
	}

	public boolean makeChildVisible(XulView view, float align, float alignPoint) {
		return makeChildVisible(view, align, alignPoint, true);
	}

	public boolean makeChildVisible(XulView view, float align) {
		return makeChildVisible(view, align, Float.NaN);
	}

	public boolean makeChildVisible(XulView view, float align, boolean animation) {
		return makeChildVisible(view, align, Float.NaN, animation);
	}

	public boolean makeChildVisible(XulView view, boolean animation) {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return false;
		}
		return render.makeChildVisible(view, animation);
	}

	public boolean makeChildVisible(XulView view, float align, float alignPoint, boolean animation) {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return false;
		}
		return render.makeChildVisible(view, align, alignPoint, animation);
	}

	public boolean makeRectVisible(RectF rect) {
		return makeRectVisible(rect, true);
	}

	public boolean makeRectVisible(RectF rect, float align, float alignPoint) {
		return makeRectVisible(rect, align, alignPoint, true);
	}

	public boolean makeRectVisible(RectF rect, float align) {
		return makeRectVisible(rect, align, Float.NaN);
	}

	public boolean makeRectVisible(RectF rect, float align, boolean animation) {
		return makeRectVisible(rect, align, Float.NaN, animation);
	}

	public boolean makeRectVisible(RectF rc, boolean animation) {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return false;
		}
		return render.makeRectVisible(rc, animation);
	}

	public boolean makeRectVisible(RectF rc, float align, float alignPoint, boolean animation) {
		XulSliderAreaRender render = (XulSliderAreaRender) _area.getRender();
		if (render == null) {
			return false;
		}
		return render.makeRectVisible(rc, align, alignPoint, animation);
	}
}
