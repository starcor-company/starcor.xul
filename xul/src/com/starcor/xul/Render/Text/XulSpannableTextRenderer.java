package com.starcor.xul.Render.Text;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.Render.Drawer.XulDrawer;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.Utils.XulRenderDrawableItem;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulWorker;

import java.util.Map;

/**
 * Created by hy on 2015/12/11.
 */
public class XulSpannableTextRenderer extends XulBasicTextRenderer {
	private XulCachedHashMap<String, SpannedLabelImage> _imageCache;
	private Html.ImageGetter _imageGetter;
	private volatile boolean _imageCacheChanged = false;
	private XulDC _drawingDC;
	private Spanned _spannedText;
	private Layout _textLayout;
	private SpannedTextEditor _editor;

	public XulSpannableTextRenderer(XulViewRender render) {
		super(render);
	}

	private Html.ImageGetter obtainImageGetter() {
		if (_imageGetter != null) {
			return _imageGetter;
		}
		_imageGetter = new Html.ImageGetter() {
			@Override
			public Drawable getDrawable(String source) {
				if (_imageCache == null) {
					_imageCache = new XulCachedHashMap<String, SpannedLabelImage>();
				}
				SpannedLabelImage spannedLabelImage = _imageCache.get(source);
				if (spannedLabelImage == null) {
					spannedLabelImage = new SpannedLabelImage();
					spannedLabelImage._source = source;
					_imageCache.put(source, spannedLabelImage);
					_imageCacheChanged = true;
				}
				if (spannedLabelImage._drawable == null) {
					return null;
				}
				return spannedLabelImage._drawable;
			}
		};
		return _imageGetter;
	}

	private void updateTextLayout(int maxWidth) {
		if (_textLayout != null) {
			return;
		}

		if (_spannedText == null) {
			_textWidth = 0;
			_textHeight = 0;
			return;
		}

		if (!isMultiline()) {
			maxWidth = Integer.MAX_VALUE;
		} else {
			maxWidth *= _superResample;
		}

		TextPaint textPaint = new TextPaint(_getTextPaint(_superResample));
		_textLayout = new StaticLayout(_spannedText, textPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, _lineHeightScalar, 0, false);

		float textWidth = 0;
		for (int i = 0; i < _textLayout.getLineCount(); ++i) {
			float lineWidth = _textLayout.getLineWidth(i);
			if (textWidth < lineWidth) {
				textWidth = lineWidth;
			}
		}
		_textWidth = XulUtils.roundToInt(XulUtils.ceilToInt(textWidth) / _superResample);
		_textHeight = XulUtils.roundToInt(_textLayout.getHeight() / _superResample);
	}

	private void refreshTextLayout() {
		if (_textLayout != null) {
			return;
		}
		Rect drawingRect = _render.getDrawingRect();
		if (drawingRect == null) {
			return;
		}
		Rect padding = _render.getPadding();
		updateTextLayout(XulUtils.calRectWidth(drawingRect) - padding.left - padding.right);
	}

	@Override
	public void drawText(XulDC dc, float xBase, float yBase, float clientViewWidth, float clientViewHeight) {
		if (_textLayout == null) {
			return;
		}
		_drawingDC = dc;
		dc.translate(xBase, yBase);
		float resampleScalar = _superResample;
		boolean doResample = Math.abs(resampleScalar - 1.0f) > 0.01f;
		if (!isMultiline()) {
			// 计算单行文本的align偏移
			float xOff = (clientViewWidth - _textWidth) * _fontAlignX;
			float yOff = (clientViewHeight - _textHeight) * _fontAlignY;

			if (_render.isRTL()) {
				if (_textWidth >= clientViewWidth) {
					xOff = clientViewWidth - _textWidth;
				}
			} else if (_textWidth >= clientViewWidth) {
				xOff = 0;
			}

			// 转换文本偏移
			dc.translate(xOff, yOff);
		}

		if (doResample) {
			dc.scale(1.0f / resampleScalar, 1.0f / resampleScalar);
		}
		TextPaint paint = _textLayout.getPaint();
		paint.setColor(_fontColor);
		_textLayout.draw(dc.getCanvas());
		_drawingDC = null;
	}

	@Override
	public void stopAnimation() {}

	@Override
	public XulTextEditor edit() {
		if (_editor == null) {
			_editor = new SpannedTextEditor();
		} else {
			_editor.reset();
		}
		return _editor;
	}

	@Override
	public XulWorker.DrawableItem collectPendingImageItem() {
		if (_imageCache == null || !_imageCacheChanged) {
			return null;
		}
		for (Map.Entry<String, SpannedLabelImage> entry : _imageCache.entrySet()) {
			SpannedLabelImage imageItem = entry.getValue();
			if (imageItem._isLoading) {
				continue;
			}
			if (imageItem._drawable != null) {
				continue;
			}
			imageItem._isLoading = true;

			imageItem.url = imageItem._source;
			imageItem.width = 0;
			imageItem.height = 0;
			return imageItem;
		}
		_imageCacheChanged = false;
		return null;
	}

	private class SpannedLabelImage extends XulRenderDrawableItem {
		String _source;
		Drawable _drawable;
		XulDrawable _xulDrawable;
		XulDrawer _xulDrawer;
		volatile boolean _isLoading = false;

		@Override
		public void onImageReady(XulDrawable bmp) {
			_xulDrawable = bmp;
			_isLoading = false;
			if (_xulDrawable == null) {
				return;
			}
			_text = "";
			_drawable = new Drawable() {
				@Override
				public void draw(Canvas canvas) {
					if (_drawingDC == null) {
						return;
					}
					_xulDrawer.draw(_drawingDC, _xulDrawable, this.getBounds(), XulRenderContext.getDefPicPaint());
				}

				@Override
				public void setAlpha(int alpha) {

				}

				@Override
				public void setColorFilter(ColorFilter cf) {

				}

				@Override
				public int getOpacity() {
					return 0;
				}
			};
			_xulDrawer = XulDrawer.create(_xulDrawable, _render.getView(), _render.getRenderContext());
			_drawable.setBounds(0, 0, bmp.getWidth(), bmp.getHeight());
			_render.setUpdateLayout();
		}
	}

	public class SpannedTextEditor extends BasicTextEditor {
		@Override
		public XulTextEditor setText(String newText) {
			if (TextUtils.isEmpty(newText)) {
				_spannedText = null;
				_textLayout = null;
			} else {
				while (!newText.equals(_text)) {
					super.setText(newText);
					_spannedText = Html.fromHtml(newText, obtainImageGetter(), null);
					_textLayout = null;
					break;
				}
			}
			return this;
		}

		@Override
		public void finish(boolean recalAutoWrap) {
			Paint.FontMetrics fontMetrics = _getTextPaint().getFontMetrics();
			_textLineHeight = XulUtils.ceilToInt((fontMetrics.bottom - fontMetrics.top) * _lineHeightScalar);
			if (_testAndSetAnyChanged) {
				_textLayout = null;
			}
			super.finish(recalAutoWrap);
			refreshTextLayout();
		}

		@Override
		public boolean defMultiline() {
			return true;
		}

		@Override
		public boolean defAutoWrap() {
			return true;
		}
	}
}
