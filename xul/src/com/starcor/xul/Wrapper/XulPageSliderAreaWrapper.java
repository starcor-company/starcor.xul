package com.starcor.xul.Wrapper;

import com.starcor.xul.Render.XulPageSliderAreaRender;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulView;

import java.util.ArrayList;

/**
 * Created by hy on 2014/6/5.
 */
public class XulPageSliderAreaWrapper extends XulViewWrapper {
	public static XulPageSliderAreaWrapper fromXulView(XulView view) {
		if (view == null) {
			return null;
		}
		if (!(view.getRender() instanceof XulPageSliderAreaRender)) {
			return null;
		}
		return new XulPageSliderAreaWrapper((XulArea) view);
	}

	XulArea _area;

	XulPageSliderAreaWrapper(XulArea area) {
		super(area);
		_area = area;
	}

	public int getPageCount() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return -1;
		}

		return render.getPageCount();
	}

	public int getCurrentPage() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return -1;
		}

		return render.getCurrentPage();
	}

	public XulView getCurrentView() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return null;
		}

		return render.getCurrentView();
	}

	public ArrayList<XulView> getAllChildViews() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return null;
		}

		return render.getAllChildViews();
	}

	public void slideLeft() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}

		render.slideLeft();
		render.markDirtyView();
	}

	public void slideRight() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}

		render.slideRight();
		render.markDirtyView();
	}

	public void slideUp() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}

		render.slideUp();
		render.markDirtyView();
	}

	public void slideDown() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}

		render.slideDown();
		render.markDirtyView();
	}

	public void slidePrev() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}

		render.slidePrev();
		render.markDirtyView();
	}

	public void slideNext() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}

		render.slideNext();
		render.markDirtyView();
	}

	public static final int IMAGE_GC_ALL = 0;      //  释放所有页面海报
	public static final int IMAGE_GC_NORMAL = 1;   //  释放预加载缓存外的海报(除当前页和前后两页预加载内容)
	public static final int IMAGE_GC_STRICT = 2;   //  释放除当前页以外的海报

	public void invokeImageGC(int level) {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}
		switch (level) {
		case IMAGE_GC_ALL:
			render.imageGC(0);
			break;
		case IMAGE_GC_NORMAL:
			render.imageGC(3);
			break;
		case IMAGE_GC_STRICT:
			render.imageGC(1);
			break;
		}
	}

	public boolean setCurrentPage(int page) {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return false;
		}

		boolean ret = render.setCurrentPage(page);
		render.markDirtyView();
		return ret;
	}


	public boolean setCurrentPage(XulView view) {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return false;
		}

		boolean ret = render.setCurrentPage(view);
		render.markDirtyView();
		return ret;
	}

	public void syncPages() {
		XulPageSliderAreaRender render = (XulPageSliderAreaRender) _area.getRender();
		if (render == null) {
			return;
		}

		render.syncPages();
	}
}
