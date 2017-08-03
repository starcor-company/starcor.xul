package com.starcor.xul.Wrapper;

import android.graphics.RectF;

import com.starcor.xul.Render.XulMassiveRender;
import com.starcor.xul.Render.XulSliderAreaRender;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulDataNode;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/3.
 */
public class XulMassiveAreaWrapper extends XulViewWrapper {
	public static XulMassiveAreaWrapper fromXulView(XulView view) {
		if (view == null) {
			return null;
		}
		if (!(view.getRender() instanceof XulMassiveRender)) {
			return null;
		}
		return new XulMassiveAreaWrapper((XulArea) view);
	}

	XulArea _view;

	XulMassiveAreaWrapper(XulArea view) {
		super(view);
		_view = view;
	}

	public boolean clear() {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		render.cleanDataItems();
		render.reset();
		return true;
	}

	public boolean addItem(XulDataNode item) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		render.addDataItem(item);
		return true;
	}

	public boolean addItem(int idx, XulDataNode item) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		render.addDataItem(idx, item);
		return true;
	}

	public XulDataNode getItem(int idx) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return null;
		}
		return render.getDataItem(idx);
	}

	public boolean updateItems(int firstItemIdx, XulDataNode... items) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.updateDataItems(firstItemIdx, items);
	}

	public boolean updateItems(int firstItemIdx, Iterable<XulDataNode> items) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.updateDataItems(firstItemIdx, items);
	}

	public void eachItem(XulMassiveRender.DataItemIterator iterator) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return;
		}
		render.eachDataItem(iterator);
	}

	public int getItemIdx(XulView item) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return -1;
		}
		return render.getItemIdx(item);
	}

	public RectF getItemRect(XulView view) {
		return getItemRect(view, new RectF());
	}

	public RectF getItemRect(XulView item, RectF rect) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return null;
		}
		return render.getItemRect(render.getItemIdx(item), rect);
	}

	public RectF getItemRect(int idx) {
		return getItemRect(idx, new RectF());
	}

	public RectF getItemRect(int idx, RectF rect) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return null;
		}
		return render.getItemRect(idx, rect);
	}

	public XulView getItemView(int idx) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return null;
		}
		return render.getItemView(idx);
	}

	public boolean fixedItem() {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.fixedItem();
	}

	public boolean setItemData(XulView item, String key, String val) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return true;
	}

	public boolean getItemData(XulView item, String key, String val) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return true;
	}

	public boolean setItemData(int idx, String key, String val) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return true;
	}

	public boolean getItemData(int idx, String key, String val) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return true;
	}

	public String queryItemData(XulView item, String query) {
		return null;
	}

	public String queryItemData(int idx, String query) {
		return null;
	}

	public boolean removeItem(XulView item) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.removeItem(item);
	}

	public boolean removeItem(int idx) {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.removeDataItem(idx);
	}

	public int itemNum() {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return 0;
		}
		return render.getDataItemNum();
	}

	public void syncContentView() {
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return;
		}
		render.syncContentView();
		return;
	}

	public boolean makeChildVisible(XulView ownerSlider, int itemIdx) {
		return makeChildVisible(ownerSlider, itemIdx, Float.NaN, Float.NaN, true);
	}

	public boolean makeChildVisible(XulView ownerSlider, int itemIdx, boolean animation) {
		return makeChildVisible(ownerSlider, itemIdx, Float.NaN, Float.NaN, animation, null);
	}

	public boolean makeChildVisible(XulView ownerSlider, int itemIdx, boolean animation, Runnable onFinished) {
		return makeChildVisible(ownerSlider, itemIdx, Float.NaN, Float.NaN, animation, onFinished);
	}

	public boolean makeChildVisible(XulView ownerSlider, int itemIdx, float align, float alignPoint, boolean animation) {
		return makeChildVisible(ownerSlider, itemIdx, align, alignPoint, animation, null);
	}

	public boolean makeChildVisible(XulView ownerSlider, int itemIdx, float align, boolean animation) {
		return makeChildVisible(ownerSlider, itemIdx, align, Float.NaN, animation);
	}

	public boolean makeChildVisible(XulView ownerSlider, int itemIdx, float align, float alignPoint) {
		return makeChildVisible(ownerSlider, itemIdx, align, alignPoint, true);
	}

	public boolean makeChildVisible(XulView ownerSlider, int itemIdx, float align) {
		return makeChildVisible(ownerSlider, itemIdx, align, Float.NaN, true);
	}

	public boolean makeChildVisible(XulView ownerSlider, int itemIdx, float align, float alignPoint, boolean animation, Runnable onFinished) {
		if (!"slider".equals(ownerSlider.getType())) {
			return false;
		}
		if (!_view.isChildOf(ownerSlider)) {
			return false;
		}
		final XulMassiveRender render = (XulMassiveRender) _view.getRender();
		if (render == null) {
			return false;
		}

		if (!render.fixedItem()) {
			return false;
		}

		if (itemIdx < 0 || itemIdx >= itemNum()) {
			return false;
		}

		XulSliderAreaRender sliderAreaRender = (XulSliderAreaRender) ownerSlider.getRender();
		XulView itemView = render.getItemView(itemIdx);
		if (itemView != null) {
			if (Float.isNaN(align)) {
				sliderAreaRender.makeChildVisible(itemView, animation);
			} else {
				sliderAreaRender.makeChildVisible(itemView, align, alignPoint, animation);
			}
			_internalScheduleOnFinishTask(render, sliderAreaRender, itemIdx, onFinished, render.getRenderContext());
			return true;
		}
		_internalMakeChildVisibleByRect(render, sliderAreaRender, itemIdx, align, alignPoint, animation, onFinished);
		return true;
	}

	private void _internalMakeChildVisibleByRect(final XulMassiveRender render, final XulSliderAreaRender sliderAreaRender, final int itemIdx, final float align, final float alignPoint, final boolean animation, final Runnable onFinished) {
		RectF itemRect = render.getItemRect(itemIdx);
		final XulRenderContext renderContext = render.getRenderContext();
		if (itemRect == null) {
			render.setUpdateLayout(true);
			renderContext.scheduleLayoutFinishedTask(new Runnable() {
				@Override
				public void run() {
					_internalMakeChildVisibleByRect(render, sliderAreaRender, itemIdx, align, alignPoint, animation, onFinished);
				}
			});
			return;
		}
		if (Float.isNaN(align)) {
			sliderAreaRender.makeRectVisible(itemRect, animation);
		} else {
			sliderAreaRender.makeRectVisible(itemRect, align, alignPoint, animation);
		}
		_internalScheduleOnFinishTask(render, sliderAreaRender, itemIdx, onFinished, renderContext);
	}

	private void _internalScheduleOnFinishTask(final XulMassiveRender render, final XulSliderAreaRender sliderAreaRender, final int itemIdx, final Runnable onFinished, final XulRenderContext renderContext) {
		if (onFinished == null) {
			return;
		}
		render.setUpdateLayout(true);
		renderContext.scheduleLayoutFinishedTask(new Runnable() {
			@Override
			public void run() {
				XulView itemView = render.getItemView(itemIdx);
				if (itemView == null) {
					renderContext.scheduleLayoutFinishedTask(this);
					return;
				}
				if (sliderAreaRender.isScrolling()) {
					renderContext.scheduleLayoutFinishedTask(this);
				} else {
					try {
						onFinished.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
}
