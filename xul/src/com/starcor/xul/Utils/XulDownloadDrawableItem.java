package com.starcor.xul.Utils;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.Render.Drawer.XulDrawer;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulWorker;

/**
 * Created by john on 2017/12/27.
 */

public class XulDownloadDrawableItem extends XulRenderDrawableItem {
	private final XulViewRender _ownerRender;
	protected XulDrawable _drawable;
	protected XulDrawer _drawer;
	protected String _url;
	protected int _width;
	protected int _height;

	private volatile boolean _loading;
	private volatile long _lastLoadFailedTime;
	static private Rect tmpRect1 = new Rect();
	static private Rect tmpRect2 = new Rect();
	static private RectF tmpRectF1 = new RectF();
	private boolean _reload = false;

	@Override
	public void onImageReady(XulDrawable bmp) {
		if (bmp == null) {
			_lastLoadFailedTime = XulUtils.timestamp();
		}
		_drawable = bmp;
		_loading = false;
		if (bmp != null) {
			_ownerRender.markDirtyView();
		}
		onImageLoaded(bmp != null);
	}

	public XulDownloadDrawableItem(String url, int w, int h, XulViewRender ownerRender) {
		_url = url;
		_width = w;
		_height = h;
		_ownerRender = ownerRender;
	}

	public XulWorker.DrawableItem collectPendingImageItem(XulWorker.DrawableItem pendingItem) {
		if (pendingItem != null) {
			return pendingItem;
		}
		if (_loading || TextUtils.isEmpty(_url)) {
			return null;
		}

		if (_drawable != null && !_reload) {
			return null;
		}

		if (XulUtils.timestamp() - _lastLoadFailedTime < 2 * 1000) {
			return null;
		}

		this.url = _url;
		this.width = _width;
		this.height = _height;
		prepareDownloadParameters();
		pendingItem = this;
		_loading = true;
		return pendingItem;
	}

	public void prepareDownloadParameters() {
	}

	public void onImageLoaded(boolean success) {

	}


	public void draw(XulDC dc, int x, int y, int w, int h, Paint paint) {
		if (_width <= 0 || _height <= 0 || w <= 0 || h <= 0) {
			return;
		}
		XulDrawable drawable = prepareDrawable();
		if (drawable == null) {
			return;
		}

		XulDrawer drawer = prepareDrawer();

		tmpRect1.set(x, y, x + w, y + h);
		drawer.draw(dc, drawable, tmpRect1, paint);
	}

	public void draw(XulDC dc, int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2, Paint paint) {
		if (_width <= 0 || _height <= 0 || w1 <= 0 || h1 <= 0 || x1 >= _width || y1 >= _height
			|| w2 <= 0 || h2 <= 0
			) {
			return;
		}

		XulDrawable drawable = prepareDrawable();
		if (drawable == null) {
			return;
		}

		XulDrawer drawer = prepareDrawer();

		tmpRect1.set(x1, y1, x1 + w1, y1 + h1);
		tmpRect2.set(x2, y2, x2 + w2, y2 + h2);
		drawer.draw(dc, drawable, tmpRect1, tmpRect2, paint);

	}

	public void draw(XulDC dc, int x, int y, float w, float h, Paint paint) {
		if (_width <= 0 || _height <= 0 || w <= 0 || h <= 0) {
			return;
		}

		XulDrawable drawable = prepareDrawable();
		if (drawable == null) {
			return;
		}

		XulDrawer drawer = prepareDrawer();

		tmpRectF1.set(x, y, x + w, y + h);
		drawer.draw(dc, drawable, tmpRectF1, paint);

	}

	public void draw(XulDC dc, int x1, int y1, int w1, int h1, float x2, float y2, float w2, float h2, Paint paint) {
		if (_width <= 0 || _height <= 0 || w1 <= 0 || h1 <= 0 || x1 >= _width || y1 >= _height
			|| w2 <= 0 || h2 <= 0
			) {
			return;
		}

		XulDrawable drawable = prepareDrawable();
		if (drawable == null) {
			return;
		}
		XulDrawer drawer = prepareDrawer();
		tmpRect1.set(x1, y1, x1 + w1, y1 + h1);
		tmpRectF1.set(x2, y2, x2 + w2, y2 + h2);
		drawer.draw(dc, drawable, tmpRect1, tmpRectF1, paint);
	}

	public void reset() {
		_drawer.reset();
	}

	private XulDrawer prepareDrawer() {
		if (_drawer != null) {
			return _drawer;
		}
		if (_drawable == null) {
			return null;
		}
		_drawer = XulDrawer.create(_drawable, _ownerRender.getView(), _ownerRender.getRenderContext());
		return _drawer;
	}

	private XulDrawable prepareDrawable() {
		if (_drawable == null && !TextUtils.isEmpty(_url)) {
			_drawable = XulWorker.loadDrawableFromCache(_url);
		}
		return _drawable;
	}

	public void update(String url, int w, int h) {

	}
}
