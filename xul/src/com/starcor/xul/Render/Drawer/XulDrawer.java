package com.starcor.xul.Render.Drawer;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.starcor.xul.Graphics.XulAnimationDrawable;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/21.
 */
public abstract class XulDrawer {

	static public XulDrawer create(XulDrawable drawable, XulView owner, XulRenderContext render) {
		if (drawable instanceof XulAnimationDrawable) {
			return XulAnimationDrawer.create(drawable, owner, render);
		}
		return XulBitmapDrawer.create(drawable, owner, render);
	}

	public void reset() {
	}

	public abstract void draw(XulDC dc, XulDrawable drawable, Rect src, Rect dst, Paint paint);

	public abstract void draw(XulDC dc, XulDrawable drawable, Rect src, RectF dst, Paint paint);

	public abstract void draw(XulDC dc, XulDrawable drawable, Rect dst, Paint paint);

	public abstract void draw(XulDC dc, XulDrawable drawable, RectF dst, Paint paint);
}
