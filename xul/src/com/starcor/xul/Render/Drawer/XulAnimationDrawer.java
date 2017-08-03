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
public class XulAnimationDrawer extends XulDrawer implements IXulAnimation {
	XulRenderContext _renderCtx;
	XulView _ownerView;
	XulAnimationDrawable _drawable;
	XulAnimationDrawable.AnimationDrawingContext _aniCtx;
	boolean _terminated = true;

	public static XulAnimationDrawer create(XulDrawable drawable, XulView owner, XulRenderContext render) {
		if (owner == null || drawable == null) {
			return null;
		}
		XulAnimationDrawer drawer = new XulAnimationDrawer();
		drawer._renderCtx = render;
		drawer._ownerView = owner;
		drawer._drawable = (XulAnimationDrawable) drawable;
		drawer._aniCtx = drawer._drawable.createDrawingCtx();
		return drawer;
	}

	@Override
	public void reset() {
		_terminated = true;
	}

	@Override
	public void draw(XulDC dc, XulDrawable drawable, Rect src, Rect dst, Paint paint) {
		if (_terminated) {
			_terminated = false;
			_aniCtx.reset();
			_renderCtx.addAnimation(this);
		}
		_drawable.drawAnimation(_aniCtx, dc, dst, paint);
	}

	@Override
	public void draw(XulDC dc, XulDrawable drawable, Rect src, RectF dst, Paint paint) {
		if (_terminated) {
			_terminated = false;
			_aniCtx.reset();
			_renderCtx.addAnimation(this);
		}
		_drawable.drawAnimation(_aniCtx, dc, dst, paint);
	}

	@Override
	public void draw(XulDC dc, XulDrawable drawable, Rect dst, Paint paint) {
		if (_terminated) {
			_terminated = false;
			_aniCtx.reset();
			_renderCtx.addAnimation(this);
		}
		_drawable.drawAnimation(_aniCtx, dc, dst, paint);
	}

	@Override
	public void draw(XulDC dc, XulDrawable drawable, RectF dst, Paint paint) {
		if (_terminated) {
			_terminated = false;
			_aniCtx.reset();
			_renderCtx.addAnimation(this);
		}
		_drawable.drawAnimation(_aniCtx, dc, dst, paint);
	}

	@Override
	public boolean updateAnimation(long timestamp) {
		if (_terminated) {
			return false;
		}
		boolean animationFinished = _aniCtx.isAnimationFinished();
		if (_aniCtx.updateAnimation(timestamp)) {
			// 动画已经更新
			_ownerView.markDirtyView();
		}
		return !animationFinished;
	}
}
