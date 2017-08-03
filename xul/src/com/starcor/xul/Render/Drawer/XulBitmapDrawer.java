package com.starcor.xul.Render.Drawer;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/21.
 */
public class XulBitmapDrawer extends XulDrawer {

	static XulBitmapDrawer _instance = new XulBitmapDrawer();

	private XulBitmapDrawer() {

	}

	static public XulBitmapDrawer create(XulDrawable drawable, XulView owner, XulRenderContext render) {
		return _instance;
	}

	@Override
	public void draw(XulDC dc, XulDrawable drawable, Rect src, Rect dst, Paint paint) {
		dc.drawBitmap(drawable, src, dst, paint);
	}

	@Override
	public void draw(XulDC dc, XulDrawable drawable, Rect src, RectF dst, Paint paint) {
		dc.drawBitmap(drawable, src, dst, paint);
	}

	@Override
	public void draw(XulDC dc, XulDrawable drawable, Rect dst, Paint paint) {
		dc.drawBitmap(drawable, dst, paint);
	}

	@Override
	public void draw(XulDC dc, XulDrawable drawable, RectF dst, Paint paint) {
		dc.drawBitmap(drawable, dst, paint);
	}
}
