package com.starcor.xul.Graphics;

import android.graphics.*;
import android.util.Log;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.starcor.xul.XulUtils;

import java.io.InputStream;

/**
 * Created by hy on 2015/2/2.
 */
public class XulSVGDrawable extends XulDrawable {

	private SVG _svg;
	private Bitmap _cachedBmp;

	public static XulDrawable buildSVGDrawable(InputStream stream, String url, String imageKey, int width, int height) {
		if (stream == null) {
			return null;
		}

		XulSVGDrawable drawable = new XulSVGDrawable();

		try {
			drawable._svg = SVG.getFromInputStream(stream);
		} catch (SVGParseException e) {
			e.printStackTrace();
			return null;
		}

		float offsetX = 0;
		float offsetY = 0;

		float xScalar = 1.0f;
		float yScalar = 1.0f;

		RectF documentViewBox = drawable._svg.getDocumentViewBox();
		if (documentViewBox == null) {
			float documentWidth = drawable._svg.getDocumentWidth();
			float documentHeight = drawable._svg.getDocumentHeight();
			if (documentWidth <= 0 && documentHeight <= 0) {
				documentWidth = 256;
				documentHeight = 256;
				drawable._svg.setDocumentWidth(documentWidth);
				drawable._svg.setDocumentHeight(documentHeight);
			}
			drawable._svg.setDocumentViewBox(0, 0, documentWidth, documentHeight);
			documentViewBox = drawable._svg.getDocumentViewBox();
		}

		int docWidth = XulUtils.roundToInt(documentViewBox.width());
		int docHeight = XulUtils.roundToInt(documentViewBox.height());
		if (width == 0 && height == 0) {
			width = docWidth;
			height = docHeight;
		} else if (width == 0) {
			xScalar = yScalar = (float)(height)/docHeight;
			width = XulUtils.roundToInt(docWidth*xScalar);
			docWidth = width;
			docHeight = height;
		} else if (height == 0) {
			xScalar = yScalar = (float)(width)/docWidth;
			height = XulUtils.roundToInt(docHeight*xScalar);
			docWidth = width;
			docHeight = height;
		} else {
			float wScalar = (float) width / docWidth;
			float hScalar = (float) height / docHeight;
			if (wScalar > hScalar) {
				docWidth *= wScalar;
				docHeight *= wScalar;
				xScalar = yScalar = wScalar;
			} else {
				docWidth *= hScalar;
				docHeight *= hScalar;
				xScalar = yScalar = hScalar;
			}
			offsetX = (docWidth - width) / 2.0f;
			offsetY = (docHeight - height) / 2.0f;
		}

		XulUtils.ticketMarker ticketMarker = new XulUtils.ticketMarker("BENCH!!!", true);
		ticketMarker.mark();

		drawable._cachedBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(drawable._cachedBmp);
		c.translate(-offsetX, -offsetY);
		c.scale(xScalar, yScalar);
		drawable._svg.renderToCanvas(c, documentViewBox);

		ticketMarker.mark("svg");
		Log.d("BENCH", ticketMarker.toString());

		drawable._url = url;
		drawable._key = imageKey;
		return drawable;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, Rect dst, Paint paint) {
		canvas.drawBitmap(_cachedBmp, rc, dst, paint);
		return true;
	}

	@Override
	public boolean draw(Canvas canvas, Rect rc, RectF dst, Paint paint) {
		canvas.drawBitmap(_cachedBmp, rc, dst, paint);
		return true;
	}

	@Override
	public int getHeight() {
		return _cachedBmp.getHeight();
	}

	@Override
	public int getWidth() {
		return _cachedBmp.getWidth();
	}
}
