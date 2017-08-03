package com.starcor.xul.Render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.KeyEvent;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Prop.XulStyle;
import com.starcor.xul.Render.Components.BaseScrollBar;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.Drawer.XulDrawer;
import com.starcor.xul.Render.Transform.ITransformer;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.Utils.XulRenderDrawableItem;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulLayout;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;
import com.starcor.xul.XulWorker;

import java.util.Map;

/**
 * Created by hy on 2014/5/13.
 */
public class XulSpannedLabelRender extends XulViewRender {
	private static final String TAG = XulSpannedLabelRender.class.getSimpleName();

	public static void register() {
		XulRenderFactory.registerBuilder("item", "spanned_label", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulItem;
				return new XulSpannedLabelRender(ctx, view);
			}
		});
	}

	float _fontSize = 12;
	int _fontColor = Color.BLACK;
	float _fontWeight = 0.0f;
	boolean _fontItalic = false;
	boolean _fontUnderline = false;
	boolean _fontStrikeThrough = false;

	float _fontShadowX = 0;
	float _fontShadowY = 0;
	float _fontShadowSize = 0;
	int _fontShadowColor = 0;
	float _fontAlignY = 0.0f;
	float _fontAlignX = 0.0f;
	float _fontScaleX = 1.0f;
	boolean _drawEllipsis = false; // …
	String _fontFace = null;

	boolean _multiLine = true;

	class TextMarqueeAnimation implements IXulAnimation {
		XulViewRender _render;
		int _marqueeSpeed = 0;
		int _marqueeDelay = 500;
		int _marqueeInterval = 500;
		int _marqueeSpace = 60;

		long _beginTime;
		long _intervalBeginTime;
		boolean _running = false;

		public TextMarqueeAnimation(XulViewRender render, ITransformer aniTransformer) {
			_render = render;
		}

		public TextMarqueeAnimation(XulViewRender render) {
			_render = render;
		}


		@Override
		public boolean updateAnimation(long timestamp) {
			if (_marqueeSpeed <= 0 || !_running) {
				_marqueePosition = -1;
				return false;
			}
			long progress = timestamp - _beginTime;
			if (progress <= _marqueeDelay) {
				return true;
			}

			if (_multiLine) {
				return false;
			}

			if (_isInvisible()) {
				return false;
			}

			if (_intervalBeginTime == _beginTime) {
				progress -= _marqueeDelay;
				_marqueePosition = _textHeight / 1.1f * progress / _marqueeSpeed;

				long marqueeTextWidth = _textWidth;
				if (_marqueeSpace >= 0) {
					marqueeTextWidth += _marqueeSpace;
				} else {
					int viewWidth = getWidth();
					if (_padding != null) {
						viewWidth -= _padding.left + _padding.right;
					}
					marqueeTextWidth += -(viewWidth * _marqueeSpace / 100);
				}

				if (_marqueePosition >= marqueeTextWidth) {
					_marqueePosition = -1;
					_intervalBeginTime = timestamp;
				}
			}

			if (_marqueePosition < 0) {
				_intervalBeginTime = _beginTime = timestamp - (_marqueeDelay - _marqueeInterval);
				// check for parent
				XulArea parent = _view.getParent();
				while (true) {
					if (parent instanceof XulLayout) {
						break;
					}
					if (parent == null) {
						return false;
					}
					XulViewRender render = parent.getRender();
					if (render == null) {
						return false;
					}
					if (render._isInvisible()) {
						return false;
					}
					parent = parent.getParent();
				}
			}
			markDirtyView();
			return true;
		}

		public void stop() {
			_running = false;
		}

		public void prepareMarqueeAnimation(XulPropParser.xulParsedAttr_Text_Marquee marquee) {
			_marqueeDelay = marquee.delay;
			_marqueeInterval = marquee.interval;
			_marqueeSpace = marquee.space;
			_marqueeSpeed = marquee.speed;
			_marqueeDirection = marquee.direction;
			_running = true;
			if (marquee.speed != _marqueeSpeed || _marqueePosition < 0) {
				_intervalBeginTime = _beginTime = animationTimestamp();
				_marqueeSpeed = marquee.speed;
				if (_marqueeSpeed == 0) {
					_marqueePosition = -1;
				} else {
					_marqueePosition = 0;
				}
			}
		}
	}

	TextMarqueeAnimation _marqueeAnimation;
	float _marqueePosition = -1;
	int _marqueeDirection = 1;

	int _scrollX = 0;
	int _scrollY = 0;

	int _scrollTargetY = 0;

	private Object _textData;
	private Spanned _spannedText;
	private Layout _textLayout;
	private int _textWidth;
	private int _textHeight;
	private int _textLineHeight;
	private float _lineHeightScalar = 1.0f;
	private XulDC _drawingDC;
	private float _superResample = 1.0f;

	private BaseScrollBar _scrollBar;
	private BaseScrollBar.ScrollBarHelper _scrollBarHelper;

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
			_textData = "";
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
			_xulDrawer = XulDrawer.create(_xulDrawable, _view, getRenderContext());
			_drawable.setBounds(0, 0, bmp.getWidth(), bmp.getHeight());
			setUpdateLayout();
		}
	}

	private XulCachedHashMap<String, SpannedLabelImage> _imageCache;
	private Html.ImageGetter _imageGetter;
	private volatile boolean _imageCacheChanged = false;

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

	public XulSpannedLabelRender(XulRenderContext ctx, XulView view) {
		super(ctx, view);
	}

	@Override
	public XulWorker.DrawableItem collectPendingImageItem() {
		XulWorker.DrawableItem pendingImageItem = super.collectPendingImageItem();
		if (pendingImageItem != null) {
			return pendingImageItem;
		}
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

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		_drawingDC = dc;
		super.draw(dc, rect, xBase, yBase);
		drawText(dc, xBase, yBase);
		_drawingDC = null;

		if (_scrollBar != null) {
			RectF animRect = getAnimRect();
			// TODO: add support for float version
			_scrollBar.draw(dc, rect, XulUtils.roundToInt(xBase + animRect.left), XulUtils.roundToInt(yBase + animRect.top));
		}

		if (_scrollTargetY == _scrollY) {
			int viewWidth = XulUtils.calRectWidth(_rect) - _padding.left - _padding.right;
			int viewHeight = XulUtils.calRectHeight(_rect) - _padding.top - _padding.bottom;
			if (_textHeight <= viewHeight) {
				_scrollTargetY = 0;
			} else if (_scrollY + _textHeight < viewHeight) {
				_scrollTargetY = viewHeight - _textHeight;
			}
		}

		if (_scrollTargetY != _scrollY) {
			int deltaY = _scrollY - _scrollTargetY;
			if (Math.abs(deltaY) > 4) {
				deltaY /= 2;
			} else {
				if (deltaY > 0) {
					deltaY = Math.min(deltaY, 2);
				} else {
					deltaY = Math.max(deltaY, -2);
				}
			}
			_scrollY -= deltaY;
			markDirtyView();
		}
	}

	private void drawText(XulDC dc, int xBase, int yBase) {
		if (_textLayout == null) {
			return;
		}
		dc.save();
		Rect viewRect = _rect;
		int viewRectHeight = XulUtils.calRectHeight(viewRect);
		int viewRectWidth = XulUtils.calRectWidth(viewRect);
		int itemOffsetX = _screenX + xBase + viewRect.left;
		int itemOffsetY = _screenY + yBase + viewRect.top;
		int xBaseOffset = itemOffsetX + _padding.left;
		int yBaseOffset = itemOffsetY + _padding.top;

		if (Math.abs(_scalarX - 1.0f) > 0.001f || Math.abs(_scalarY - 1.0f) > 0.001f) {
			float xAlignment = viewRectWidth * _scalarXAlign;
			float yAlignment = viewRectHeight * _scalarYAlign;
			dc.scale(_scalarX, _scalarY, itemOffsetX + xAlignment, itemOffsetY + yAlignment);
		}
		dc.translate(xBaseOffset, yBaseOffset);

		int viewWidth = viewRectWidth - _padding.left - _padding.right;
		int viewHeight = viewRectHeight - _padding.top - _padding.bottom;

		dc.clipRect(
			0,
			0,
			viewWidth,
			viewHeight
		);

		float resampleScalar = _superResample;
		boolean doResample = Math.abs(resampleScalar - 1.0f) > 0.01f;
		if (!_multiLine) {
			// 计算单行文本的align偏移
			float xOff = (viewWidth - _textWidth) * _fontAlignX;
			float yOff = (viewHeight - _textHeight) * _fontAlignY;

			if (_isRTL()) {
				if (_textWidth >= viewWidth) {
					xOff = viewWidth - _textWidth;
				}
			} else if (_textWidth >= viewWidth) {
				xOff = 0;
			}

			// 转换文本偏移
			dc.translate(xOff, yOff);

			// 处理marquee动画
			if (_textWidth <= viewWidth) {
				_stopMarqueeAnimation();
			} else if (_marqueeAnimation != null && _marqueePosition >= 0) {
				int marqueeSpace = _marqueeAnimation._marqueeSpace;
				int space;
				if (marqueeSpace > 0) {
					space = marqueeSpace;
				} else {
					space = -viewWidth * marqueeSpace / 100;
				}

				// 处理滚动偏移
				xOff = -1 * _marqueePosition * _marqueeDirection;
				if (xOff < viewWidth || xOff + _textWidth > 0) {
					dc.translate(xOff, 0);
					if (doResample) {
						dc.save(Canvas.MATRIX_SAVE_FLAG);
						dc.scale(1.0f / resampleScalar, 1.0f / resampleScalar);
						_textLayout.draw(dc.getCanvas());
						dc.restore();
					} else {
						_textLayout.draw(dc.getCanvas());
					}
				}

				// 处理space偏移
				float drawPos = (_textWidth + space) * _marqueeDirection;
				if (drawPos < viewWidth || drawPos + _textWidth > 0) {
					xOff = drawPos;
					dc.translate(xOff, 0);
				}
			}
		}

		dc.translate(0, _scrollY);
		if (doResample) {
			dc.scale(1.0f / resampleScalar, 1.0f / resampleScalar);
		}
		_textLayout.draw(dc.getCanvas());
		dc.restore();
	}

	static private <T> T _testAndSet(T newVal, T oldVal, boolean[] isChanged) {
		if (newVal == oldVal) {
			return oldVal;
		}
		isChanged[0] = true;
		return newVal;
	}

	static private String _testAndSet(String newVal, String oldVal, boolean[] isChanged) {
		if (newVal == oldVal) {
			return oldVal;
		}
		if (newVal != null && oldVal != null && newVal.equals(oldVal)) {
			return oldVal;
		}
		isChanged[0] = true;
		return newVal;
	}

	private void syncTextInfo() {
		if (!_isViewChanged()) {
			return;
		}

		boolean[] anyStyleChanged = new boolean[]{false};

		XulStyle fontFaceStyle = _view.getStyle(XulPropNameCache.TagId.FONT_FACE);
		XulStyle fontSizeStyle = _view.getStyle(XulPropNameCache.TagId.FONT_SIZE);
		XulStyle fontColorStyle = _view.getStyle(XulPropNameCache.TagId.FONT_COLOR);
		XulStyle fontWeightStyle = _view.getStyle(XulPropNameCache.TagId.FONT_WEIGHT);
		XulStyle fontShadowStyle = _view.getStyle(XulPropNameCache.TagId.FONT_SHADOW);
		XulStyle fontAlignStyle = _view.getStyle(XulPropNameCache.TagId.FONT_ALIGN);
		XulStyle fontUnderlineStyle = _view.getStyle(XulPropNameCache.TagId.FONT_STYLE_UNDERLINE);
		XulStyle fontItalicStyle = _view.getStyle(XulPropNameCache.TagId.FONT_STYLE_ITALIC);
		XulStyle lineHeightStyle = _view.getStyle(XulPropNameCache.TagId.LINE_HEIGHT);
		XulStyle fontScaleXStyle = _view.getStyle(XulPropNameCache.TagId.FONT_SCALE_X);
		XulStyle fontStrikeStyle = _view.getStyle(XulPropNameCache.TagId.FONT_STYLE_STRIKE);
		XulStyle fontResampleStyle = _view.getStyle(XulPropNameCache.TagId.FONT_RESAMPLE);

		if (fontResampleStyle != null) {
			_superResample = ((XulPropParser.xulParsedProp_Float) fontResampleStyle.getParsedValue()).val;
			_superResample = Math.min(Math.max(1.0f, _superResample), 4.0f);
		} else {
			_superResample = 1.0f;
		}

		if (fontScaleXStyle != null) {
			_fontScaleX = _testAndSet(((XulPropParser.xulParsedStyle_FontScaleX) fontScaleXStyle.getParsedValue()).val, _fontScaleX, anyStyleChanged);
		} else {
			_fontScaleX = _testAndSet(1.0f, _fontScaleX, anyStyleChanged);
		}

		if (fontStrikeStyle != null) {
			_fontStrikeThrough = _testAndSet(((XulPropParser.xulParsedStyle_FontStyleStrike) fontStrikeStyle.getParsedValue()).val, _fontStrikeThrough, anyStyleChanged);
		} else {
			_fontStrikeThrough = _testAndSet(false, _fontStrikeThrough, anyStyleChanged);
		}

		if (fontFaceStyle != null) {
			_fontFace = _testAndSet(fontFaceStyle.getStringValue(), _fontFace, anyStyleChanged);
		} else {
			_fontFace = _testAndSet(null, _fontFace, anyStyleChanged);
		}

		if (lineHeightStyle != null) {
			_lineHeightScalar = _testAndSet(((XulPropParser.xulParsedStyle_LineHeight) lineHeightStyle.getParsedValue()).val, _lineHeightScalar, anyStyleChanged);
		} else {
			_lineHeightScalar = _testAndSet(1.0f, _lineHeightScalar, anyStyleChanged);
		}

		if (fontUnderlineStyle != null) {
			_fontUnderline = _testAndSet(((XulPropParser.xulParsedProp_booleanValue) fontUnderlineStyle.getParsedValue()).val, _fontUnderline, anyStyleChanged);
		} else {
			_fontUnderline = _testAndSet(false, _fontUnderline, anyStyleChanged);
		}

		if (fontItalicStyle != null) {
			_fontItalic = _testAndSet(((XulPropParser.xulParsedProp_booleanValue) fontItalicStyle.getParsedValue()).val, _fontItalic, anyStyleChanged);
		} else {
			_fontItalic = _testAndSet(false, _fontItalic, anyStyleChanged);
		}

		double xScalar = _ctx.getXScalar();
		double yScalar = _ctx.getYScalar();

		if (fontSizeStyle != null) {
			XulPropParser.xulParsedStyle_FontSize fontSize = fontSizeStyle.getParsedValue();
			_fontSize = _testAndSet((float) (xScalar * fontSize.val), _fontSize, anyStyleChanged);
		} else {
			_fontSize = _testAndSet((float) (xScalar * 12), _fontSize, anyStyleChanged);
		}

		if (fontColorStyle != null) {
			XulPropParser.xulParsedStyle_FontColor fontColor = fontColorStyle.getParsedValue();
			_fontColor = _testAndSet(fontColor.val, _fontColor, anyStyleChanged);
		} else {
			_fontColor = _testAndSet(Color.BLACK, _fontColor, anyStyleChanged);
		}

		if (fontWeightStyle != null) {
			XulPropParser.xulParsedStyle_FontWeight fontWeight = fontWeightStyle.getParsedValue();
			_fontWeight = _testAndSet((float) (xScalar * fontWeight.val), _fontWeight, anyStyleChanged);
		} else {
			_fontWeight = _testAndSet((float) (xScalar * 1.0), _fontWeight, anyStyleChanged);
		}

		if (fontShadowStyle != null) {
			XulPropParser.xulParsedStyle_FontShadow fontShadow = fontShadowStyle.getParsedValue();
			_fontShadowX = _testAndSet((float) (fontShadow.xOffset * xScalar), _fontShadowX, anyStyleChanged);
			_fontShadowY = _testAndSet((float) (fontShadow.yOffset * yScalar), _fontShadowY, anyStyleChanged);
			_fontShadowSize = _testAndSet((float) (fontShadow.size * xScalar), _fontShadowSize, anyStyleChanged);
			_fontShadowColor = _testAndSet(fontShadow.color, _fontShadowColor, anyStyleChanged);
		} else {
			_fontShadowSize = _testAndSet(0.0f, _fontShadowSize, anyStyleChanged);
			_fontShadowColor = _testAndSet(0, _fontShadowColor, anyStyleChanged);

		}

		if (fontAlignStyle != null) {
			XulPropParser.xulParsedStyle_FontAlign fontAlign = fontAlignStyle.getParsedValue();
			_fontAlignX = _testAndSet(fontAlign.xAlign, _fontAlignX, anyStyleChanged);
			_fontAlignY = _testAndSet(fontAlign.yAlign, _fontAlignY, anyStyleChanged);
		} else {
			_fontAlignX = _testAndSet(0.0f, _fontAlignX, anyStyleChanged);
			_fontAlignY = _testAndSet(0.0f, _fontAlignY, anyStyleChanged);
		}

		XulAttr multiAttr = _view.getAttr(XulPropNameCache.TagId.MULTI_LINE);
		if (multiAttr == null || "true".equals(multiAttr.getStringValue())) {
			_multiLine = _testAndSet(true, _multiLine, anyStyleChanged);
		} else {
			_multiLine = _testAndSet(false, _multiLine, anyStyleChanged);
		}

		if ("true".equals(_view.getAttrString(XulPropNameCache.TagId.ELLIPSIS))) {
			_drawEllipsis = _testAndSet(true, _drawEllipsis, anyStyleChanged);
		} else {
			_drawEllipsis = _testAndSet(false, _drawEllipsis, anyStyleChanged);
		}

		XulAttr text = _view.getAttr(XulPropNameCache.TagId.TEXT);
		if (text != null && text.getValue() != null) {
			Object textData = text.getValue();
			if (textData == null) {
				_spannedText = null;
				_textLayout = null;
			} else {
				while (_textData != textData) {
					_textData = textData;
					String newText;
					if (_textData instanceof String) {
						newText = (String) _textData;
					} else {
						_spannedText = null;
						_textLayout = null;
						break;
					}
					_spannedText = Html.fromHtml(newText, obtainImageGetter(), null);
					_textLayout = null;
					break;
				}
			}
		} else {
			_spannedText = null;
			_textLayout = null;
		}

		Paint.FontMetrics fontMetrics = _getTextPaint().getFontMetrics();
		_textLineHeight = XulUtils.ceilToInt((fontMetrics.bottom - fontMetrics.top) * _lineHeightScalar);
		if (anyStyleChanged[0]) {
			_textLayout = null;
		}

		if (_multiLine || _spannedText == null) {
			_stopMarqueeAnimation();
		} else while (true) { // 创建marquee动画
			XulAttr marqueeAttr = _view.getAttr(XulPropNameCache.TagId.MARQUEE);
			if (marqueeAttr == null) {
				_stopMarqueeAnimation();
				break;
			}
			XulPropParser.xulParsedAttr_Text_Marquee marquee = marqueeAttr.getParsedValue();
			if (marquee.speed <= 0) {
				_stopMarqueeAnimation();
				break;
			}
			if (_marqueeAnimation == null) {
				_marqueeAnimation = new TextMarqueeAnimation(this);
			}
			_marqueeAnimation.prepareMarqueeAnimation(marquee);
			addAnimation(_marqueeAnimation);
			break;
		}

		if (_isRTL()) {
			_fontAlignX = 1.0f - _fontAlignX;
			_marqueeDirection = -_marqueeDirection;
		}
	}

	private void _stopMarqueeAnimation() {
		if (_marqueeAnimation == null) {
			return;
		}
		_marqueeAnimation.stop();
		if (_marqueePosition < 0) {
			return;
		}
		markDirtyView();
		_marqueePosition = -1;
	}

	@Override
	public boolean onKeyEvent(KeyEvent event) {
		int scrollLines = 3;
		// 处理滚动事件
		int viewHeight = XulUtils.calRectHeight(_rect) - _padding.top - _padding.bottom;
		if (_textHeight > viewHeight && event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_DPAD_UP:
				if (_scrollTargetY < 0) {
					_scrollTargetY += _textLineHeight * scrollLines;
					_scrollTargetY -= (_scrollTargetY % _textLineHeight);
					if (_scrollTargetY > 0) {
						_scrollTargetY = 0;
					}
					if (_scrollBar != null) {
						_scrollBar.reset();
					}
					markDirtyView();
					return true;
				}
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				int minScrollYPos = viewHeight - _textHeight;
				if (_scrollTargetY > minScrollYPos) {
					_scrollTargetY -= _textLineHeight * scrollLines;
					if (_scrollTargetY < minScrollYPos) {
						_scrollTargetY = minScrollYPos;
					}
					if (_scrollBar != null) {
						_scrollBar.reset();
					}
					markDirtyView();
					return true;
				}
				break;
			}
		}
		return false;
	}

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		super.syncData();

		XulAttr attrScrollbar = _view.getAttr("scrollbar");
		if (attrScrollbar == null || attrScrollbar.getValue() == null) {
			_scrollBar = XulRenderFactory.buildScrollBar(_scrollBar, "", _getScrollBarHelper(), this);
		} else {
			_scrollBar = XulRenderFactory.buildScrollBar(_scrollBar, attrScrollbar.getStringValue(), _getScrollBarHelper(), this);
		}
	}

	private BaseScrollBar.ScrollBarHelper _getScrollBarHelper() {
		if (_scrollBarHelper == null) {
			_scrollBarHelper = new BaseScrollBar.ScrollBarHelper() {
				@Override
				public boolean isVertical() {
					return true;
				}

				@Override
				public int getScrollPos() {
					return _scrollY;
				}

				@Override
				public int getContentWidth() {
					return _textWidth;
				}

				@Override
				public int getContentHeight() {
					return _textHeight;
				}
			};
		}
		return _scrollBarHelper;
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

		if (!_multiLine) {
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

	private Paint _getTextPaint() {
		return _getTextPaint(1.0f);
	}

	private Paint _getTextPaint(float fontSizeScale) {
		Paint defPaint = _ctx.getTextPaintByName(_fontFace);

		if (!(_fontShadowSize == 0 || (_fontShadowColor & 0xFF000000) == 0)) {
			defPaint = _ctx.getShadowTextPaintByName(_fontFace);
			defPaint.setShadowLayer(_fontShadowSize, _fontShadowX, _fontShadowY, _fontShadowColor);
		}

		defPaint.setColor(_fontColor);
		if (Math.abs(fontSizeScale - 1.0f) > 0.01f) {
			defPaint.setTextSize(_fontSize * fontSizeScale);
		} else {
			defPaint.setTextSize(_fontSize);
		}

		//defPaint.setStrokeWidth(_fontWeight / 2.0f);

		if (_fontWeight > 1.0) {
			defPaint.setFakeBoldText(true);
		} else {
			defPaint.setFakeBoldText(false);
		}
		defPaint.setUnderlineText(_fontUnderline);
		defPaint.setTextSkewX(_fontItalic ? -0.25f : 0);
		defPaint.setTextAlign(Paint.Align.LEFT);
		return defPaint;
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_NOFOCUS;
	}

	protected class LayoutElement extends XulViewRender.LayoutElement {
		@Override
		public int doFinal() {
			super.doFinal();
			if (_isInvisible()) {
				return 0;
			}
			refreshTextLayout();
			return 0;
		}

		private void refreshTextLayout() {
			if (_spannedText != null && _textLayout == null) {
				updateTextLayout(XulUtils.calRectWidth(_rect) - _padding.left - _padding.right);
			}
		}

		@Override
		public int prepare() {
			super.prepare();
			if (_isInvisible()) {
				return 0;
			}
			syncTextInfo();

			switch (getWidth()) {
			case XulManager.SIZE_AUTO:
			case XulManager.SIZE_MATCH_CONTENT:
				updateTextLayout(Integer.MAX_VALUE);
				break;
			case XulManager.SIZE_MATCH_PARENT:
				break;
			default:
				updateTextLayout(XulUtils.calRectWidth(_rect) - _padding.left - _padding.right);
				break;
			}
			return 0;
		}

		@Override
		public int getContentWidth() {
			return _textWidth;
		}

		@Override
		public int getContentHeight() {
			refreshTextLayout();
			return _textHeight;
		}
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutElement();
	}

}
