package com.starcor.xul.Utils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/5/23.
 */
public class XulAreaChildrenRender extends XulArea.XulViewIterator {
	private static final String TAG = XulAreaChildrenRender.class.getSimpleName();
	XulDC dc;
	Canvas canvas;
	Rect rect;
	int xBase;
	int yBase;

	private void drawItem(XulView view) {
		XulViewRender render = view.getRender();
		if (render == null) {
			return;
		}
		if (!render.isVisible()) {
			render.setDrawingSkipped(true);
			return;
		}
		if (render.getDrawingRect() == null) {
			Log.w(TAG, "invalid drawing state!!");
			render.setUpdateLayout();
			render.setDrawingSkipped(true);
			return;
		}
		RectF updateRc = render.getUpdateRect();
		updateRc.left += xBase;
		updateRc.top += yBase;
		updateRc.right += xBase;
		updateRc.bottom += yBase;
		if (canvas.quickReject(updateRc.left, updateRc.top, updateRc.right, updateRc.bottom, Canvas.EdgeType.AA)) {
			render.setDrawingSkipped(true);
			return;
		}
		render.setDrawingSkipped(false);
		if (render.needPostDraw()) {
			dc.postDraw(view, rect, xBase, yBase, render.getZIndex());
		} else {
			view.draw(dc, rect, xBase, yBase);
		}
	}

	@Override
	public boolean onXulView(int pos, XulView view) {
		drawItem(view);
		return true;
	}

	public void init(XulDC dc, Rect rect, int xBase, int yBase) {
		this.dc = dc;
		this.canvas = dc.getCanvas();
		this.rect = rect;
		this.xBase = xBase;
		this.yBase = yBase;
	}
}
