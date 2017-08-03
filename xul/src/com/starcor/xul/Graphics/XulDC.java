package com.starcor.xul.Graphics;

import android.graphics.*;
import com.starcor.xul.Utils.XulPriorityQueue;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Created by hy on 2014/5/23.
 */
public class XulDC {
	private static final long RENDER_THREADHOLD = 8 * 1000;    // us
	static String TAG = XulDC.class.getSimpleName();
	XulRenderContext _render;
	Canvas _canvas;
	int _drawIndex = 0;
	long _beginTime;
	boolean _renderTimeout = false;

	public static Rect _tmpRc0 = new Rect();
	public static Rect _tmpRc1 = new Rect();

	public static RectF _tmpFRc0 = new RectF();
	public static RectF _tmpFRc1 = new RectF();

	static class PostDrawInfo {
		IXulDrawable _drawable;
		int _xBase;
		int _yBase;
		Rect _rect; // clip rect
		int _zIndex = 0;


		PostDrawInfo(IXulDrawable view, Rect rect, int xBase, int yBase) {
			init(view, rect, xBase, yBase, 0);
		}

		PostDrawInfo(IXulDrawable view, Rect rect, int xBase, int yBase, int zIndex) {
			init(view, rect, xBase, yBase, zIndex);
		}

		private void init(IXulDrawable view, Rect rect, int xBase, int yBase, int zIndex) {
			_drawable = view;
			_rect = rect;
			_xBase = xBase;
			_yBase = yBase;
			_zIndex = zIndex;
		}

		void recycle() {
			_drawable = null;
			_rect = null;
		}
	}

	XulPriorityQueue<PostDrawInfo> _postDrawQueue;
	Iterator<PostDrawInfo> _postDrawQueueIterator;

	private static ArrayList<PostDrawInfo> _recycledPostDraw = new ArrayList<PostDrawInfo>();

	private void _initPostDrawQueue() {
		if (_postDrawQueue != null) {
			return;
		}
		_postDrawQueue = new XulPriorityQueue<PostDrawInfo>(10, new Comparator<PostDrawInfo>() {
			@Override
			public int compare(PostDrawInfo lhs, PostDrawInfo rhs) {
				return lhs._zIndex - rhs._zIndex;
			}
		});
		_postDrawQueueIterator = _postDrawQueue.iterator();
	}

	public XulDC(XulRenderContext render) {
		_render = render;
	}

	public boolean isRenderTimeout() {
		if (_renderTimeout) {
			return _renderTimeout;
		}
		_renderTimeout = XulUtils.timestamp_us() - _beginTime > RENDER_THREADHOLD;
		return false;
	}

	public Canvas setCanvas(Canvas canvas) {
		Canvas old = _canvas;
		_canvas = canvas;
		return old;
	}

	public Canvas getCanvas() {
		return _canvas;
	}

	public void postDraw(IXulDrawable drawable, Rect rect, int xBase, int yBase, int zIndex) {
		_initPostDrawQueue();
		if (zIndex == 0) {
			zIndex = _drawIndex;
		}
		PostDrawInfo drawInfo = createPostDrawInfo(drawable, rect, xBase, yBase, zIndex);
		_postDrawQueue.add(drawInfo);
	}

	private PostDrawInfo createPostDrawInfo(IXulDrawable drawable, Rect rect, int xBase, int yBase, int zIndex) {
		if (_recycledPostDraw.isEmpty()) {
			return new PostDrawInfo(drawable, rect, xBase, yBase, zIndex);
		}
		PostDrawInfo postDrawInfo = _recycledPostDraw.remove(_recycledPostDraw.size() - 1);
		postDrawInfo.init(drawable, rect, xBase, yBase, zIndex);
		return postDrawInfo;
	}

	private void recyclePostDrawInfo(PostDrawInfo drawInfo) {
		drawInfo.recycle();
		_recycledPostDraw.add(drawInfo);
	}

	public void save() {
		XulUtils.saveCanvas(this);
	}

	public void save(int flags) {
		XulUtils.saveCanvas(this, flags);
	}

	public void restore() {
		XulUtils.restoreCanvas(_canvas);
	}

	public void translate(float x, float y) {
		_canvas.translate(x, y);
	}

	public void setMatrix(Matrix m) {
		_canvas.setMatrix(m);
	}

	public void scale(float sx, float sy, float px, float py) {
		_canvas.scale(sx, sy, px, py);
	}

	public void scale(float sx, float sy) {
		_canvas.scale(sx, sy);
	}

	public void rotate(float degree, float px, float py) {
		_canvas.rotate(degree, px, py);
	}

	public void rotate(float degree) {
		_canvas.rotate(degree);
	}

	public void drawBitmap(XulDrawable bmp, Rect src, Rect dst, Paint paint) {
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawBitmap(XulDrawable bmp, Rect dst, Paint paint) {
		int width = bmp.getWidth();
		int height = bmp.getHeight();

		Rect src = _tmpRc0;
		src.set(0, 0, width, height);
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawBitmap(XulDrawable bmp, RectF dst, Paint paint) {
		int width = bmp.getWidth();
		int height = bmp.getHeight();

		Rect src = _tmpRc0;
		src.set(0, 0, width, height);
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawBitmap(XulDrawable bmp, int x, int y, int width, int height, Paint paint) {
		Rect src = _tmpRc0;
		src.set(0, 0, bmp.getWidth(), bmp.getHeight());

		Rect dst = _tmpRc1;
		dst.set(x, y, x + width, y + height);
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawBitmap(XulDrawable bmp, float x, float y, float cx, float cy, Paint paint) {
		Rect src = _tmpRc0;
		src.set(0, 0, bmp.getWidth(), bmp.getHeight());

		RectF dst = _tmpFRc0;
		dst.set(x, y, x + cx, y + cy);
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawBitmap(XulDrawable bmp, int x1, int y1, int width1, int height1, int x2, int y2, int width2, int height2, Paint paint) {
		Rect src = _tmpRc0;
		src.set(x1, y1, x1 + width1, y1 + height1);

		Rect dst = _tmpRc1;
		dst.set(x2, y2, x2 + width2, y2 + height2);
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawBitmap(XulDrawable bmp, int x1, int y1, int width1, int height1, float x2, float y2, float width2, float height2, Paint paint) {
		Rect src = _tmpRc0;
		src.set(x1, y1, x1 + width1, y1 + height1);

		RectF dst = _tmpFRc0;
		dst.set(x2, y2, x2 + width2, y2 + height2);
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawBitmap(XulDrawable bmp, int dstX, int dstY, Paint paint) {
		int width = bmp.getWidth();
		int height = bmp.getHeight();

		Rect src = _tmpRc0;
		src.set(0, 0, width, height);

		Rect dst = _tmpRc1;
		dst.set(dstX, dstY, dstX + width, dstY + height);
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawBitmap(Bitmap bmp, int dstX, int dstY, Paint paint) {
		int width = bmp.getWidth();
		int height = bmp.getHeight();

		Rect src = _tmpRc0;
		src.set(0, 0, width, height);

		Rect dst = _tmpRc1;
		dst.set(dstX, dstY, dstX + width, dstY + height);
		_canvas.drawBitmap(bmp, src, dst, paint);
	}

	public void drawBitmap(Bitmap bmp, float dstX, float dstY, Paint paint) {
		int width = bmp.getWidth();
		int height = bmp.getHeight();

		Rect src = _tmpRc0;
		src.set(0, 0, width, height);

		RectF dst = _tmpFRc0;
		dst.set(dstX, dstY, dstX + width, dstY + height);
		_canvas.drawBitmap(bmp, src, dst, paint);
	}

	public void drawBitmap(XulDrawable bmp, int x, int y, int width, int height, Rect dst, Paint paint) {
		Rect src = _tmpRc0;
		src.set(x, y, x + width, y + height);
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawBitmap(XulDrawable bmp, Rect src, RectF dst, Paint paint) {
		bmp.draw(_canvas, src, dst, paint);
	}

	public void drawText(CharSequence text, int start, int end, float xPos, float yPos, Paint paint) {
		_canvas.drawText(text, start, end, xPos, yPos, paint);
	}

	public void clipRect(Rect rect) {
		_canvas.clipRect(rect);
	}

	public void clipRect(RectF rect) {
		_canvas.clipRect(rect);
	}

	public void clipRect(int x, int y, int cx, int cy) {
		_canvas.clipRect(x, y, x + cx, y + cy);
	}

	public void clipRect(float x, float y, float cx, float cy) {
		_canvas.clipRect(x, y, x + cx, y + cy);
	}

	public void drawCircle(float x, float y, float radius, Paint paint) {
		_canvas.drawCircle(x, y, radius, paint);
	}

	public void drawRoundRect(RectF rect, float radiusX, float radiusY, Paint paint) {
		_canvas.drawRoundRect(rect, radiusX, radiusY, paint);
	}

	public void drawRoundRect(float x, float y, float width, float height, float radiusX, float radiusY, Paint paint) {
		RectF dst = _tmpFRc0;
		dst.set(x, y, x + width, y + height);
		_canvas.drawRoundRect(dst, radiusX, radiusY, paint);
	}

	public void drawRoundRect(Rect rect, float radiusX, float radiusY, Paint paint) {
		RectF dst = _tmpFRc0;
		dst.set(rect);
		_canvas.drawRoundRect(dst, radiusX, radiusY, paint);
	}

	public void drawRect(Rect rect, Paint paint) {
		_canvas.drawRect(rect, paint);
	}

	public void drawRect(float left, float top, float right, float bottom, Paint paint) {
		_canvas.drawRect(left, top, right, bottom, paint);
	}

	public void drawRect(RectF rect, Paint paint) {
		_canvas.drawRect(rect, paint);
	}

	public void beginDraw(Canvas canvas) {
		_canvas = canvas;
		_drawIndex = 0;
		_beginTime = XulUtils.timestamp_us();
		_renderTimeout = false;
	}

	public void doPostDraw(int zIndex, XulArea parent) {
		if (_postDrawQueue == null) {
			return;
		}
		_postDrawQueue.resetIterator(_postDrawQueueIterator);
		for (Iterator<PostDrawInfo> iter = _postDrawQueueIterator; iter.hasNext(); ) {
			PostDrawInfo drawInfo = iter.next();
			if (drawInfo._zIndex > zIndex) {
				return;
			}
			if (drawInfo._drawable instanceof XulView && parent.hasChild((XulView) drawInfo._drawable)) {
				iter.remove();
				drawInfo._drawable.draw(this, drawInfo._rect, drawInfo._xBase, drawInfo._yBase);
				recyclePostDrawInfo(drawInfo);
			}
		}
	}

	public void doPostDraw(int zIndex) {
		if (_postDrawQueue == null) {
			return;
		}
		while (!_postDrawQueue.isEmpty()) {
			PostDrawInfo drawInfo = _postDrawQueue.peek();
			if (drawInfo._zIndex > zIndex) {
				return;
			}
			_postDrawQueue.remove(drawInfo);
			drawInfo._drawable.draw(this, drawInfo._rect, drawInfo._xBase, drawInfo._yBase);
			recyclePostDrawInfo(drawInfo);
		}
	}

	public void endDraw() {
		if (_postDrawQueue != null) {
			while (!_postDrawQueue.isEmpty()) {
				PostDrawInfo drawInfo = _postDrawQueue.poll();
				drawInfo._drawable.draw(this, drawInfo._rect, drawInfo._xBase, drawInfo._yBase);
				recyclePostDrawInfo(drawInfo);
			}
		}

		this._canvas = null;
	}
}
