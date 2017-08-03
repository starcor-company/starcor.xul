package com.starcor.xul.Render;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Prop.XulStyle;
import com.starcor.xul.Render.Components.BaseScrollBar;
import com.starcor.xul.Render.Text.XulMarqueeTextRenderer;
import com.starcor.xul.Render.Text.XulSimpleTextRenderer;
import com.starcor.xul.Render.Text.XulSpannableTextRenderer;
import com.starcor.xul.Render.Text.XulTextRenderer;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/5/13.
 */
public class XulLabelRender extends XulViewRender {
	private static final String TAG = XulLabelRender.class.getSimpleName();
	protected XulTextRenderer _baseTextRenderer;
	protected XulMarqueeTextRenderer _textRenderer;
	int _scrollX = 0;
	int _scrollY = 0;
	int _scrollTargetY = 0;
	private BaseScrollBar _scrollBar;
	private BaseScrollBar.ScrollBarHelper _scrollBarHelper;

	public XulLabelRender(XulRenderContext ctx, XulView view) {
		super(ctx, view);
	}

	public static void register() {
		XulRenderFactory.registerBuilder("item", "label", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulItem;
				return new XulLabelRender(ctx, view);
			}
		});
	}

	@Override
	public void onVisibilityChanged(boolean isVisible, XulView eventSource) {
		if (!isVisible && _textRenderer != null) {
			_textRenderer.stopAnimation();
		}
		super.onVisibilityChanged(isVisible, eventSource);
	}

	@Override
	public void destroy() {
		XulMarqueeTextRenderer textRenderer = _textRenderer;
		_baseTextRenderer = null;
		_textRenderer = null;
		if (textRenderer != null) {
			textRenderer.stopAnimation();
		}
		super.destroy();
	}

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		super.drawBackground(dc, rect, xBase, yBase);
		drawText(dc, rect, xBase, yBase);
		super.drawBorder(dc, rect, xBase, yBase);
	}

	public void drawText(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		XulMarqueeTextRenderer textRenderer = _textRenderer;
		if (textRenderer == null || textRenderer.isEmpty()) {
			return;
		}
		Rect viewRect = _rect;
		int viewRectHeight = XulUtils.calRectHeight(viewRect);
		int viewRectWidth = XulUtils.calRectWidth(viewRect);

		int clientViewWidth = viewRectWidth - _padding.left - _padding.right;
		int clientViewHeight = viewRectHeight - _padding.top - _padding.bottom;

		if (clientViewWidth <= 0 || clientViewHeight <= 0) {
			return;
		}

		int itemOffsetX = _screenX + xBase + viewRect.left;
		int itemOffsetY = _screenY + yBase + viewRect.top;
		int xBaseOffset = itemOffsetX + _padding.left;
		int yBaseOffset = itemOffsetY + _padding.top;


		dc.save();
		if (Math.abs(_scalarX - 1.0f) > 0.001f || Math.abs(_scalarY - 1.0f) > 0.001f) {
			float xAlignment = viewRectWidth * _scalarXAlign;
			float yAlignment = viewRectHeight * _scalarYAlign;
			dc.scale(_scalarX, _scalarY, itemOffsetX + xAlignment, itemOffsetY + yAlignment);
		}

		BaseScrollBar scrollBar = _scrollBar;
		if (scrollBar != null) {
			dc.save();
		}
		dc.translate(xBaseOffset, yBaseOffset);

		if (getWidth() >= clientViewWidth || getHeight() >= clientViewHeight) {
			dc.clipRect(
				0,
				0,
				clientViewWidth,
				clientViewHeight
			);
		}
		textRenderer.drawText(dc, _scrollX, _scrollY, clientViewWidth, clientViewHeight);

		if (scrollBar != null) {
			dc.restore();
			scrollBar.draw(dc, rect, XulUtils.roundToInt(xBase + viewRect.left), XulUtils.roundToInt(yBase + viewRect.top));
		}

		dc.restore();

		if (_scrollTargetY == _scrollY) {
			if (textRenderer.getHeight() <= clientViewHeight) {
				_scrollTargetY = 0;
			} else if (_scrollY + textRenderer.getHeight() < clientViewHeight) {
				_scrollTargetY = (int) (clientViewHeight - textRenderer.getHeight());
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

	protected void syncTextInfo() {
		syncTextInfo(false);
	}

	protected void syncTextInfo(boolean recalAutoWrap) {
		if (!_isViewChanged()) {
			return;
		}

		XulMarqueeTextRenderer.XulMarqueeTextEditor textEditor = _textRenderer.edit();
		XulAttr text = _view.getAttr(XulPropNameCache.TagId.TEXT);
		if (text != null && text.getValue() != null) {
			String newText = text.getStringValue();
			textEditor.setText(newText);
		} else {
			textEditor.setText(XulUtils.STR_EMPTY);
		}

		XulStyle fixHalfCharStyle = _view.getStyle(XulPropNameCache.TagId.STYLE_FIX_HALF_CHAR);
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
		XulStyle startIndentStyle = _view.getStyle(XulPropNameCache.TagId.START_INDENT);
		XulStyle endIndentStyle = _view.getStyle(XulPropNameCache.TagId.END_INDENT);
		XulStyle fontResampleStyle = _view.getStyle(XulPropNameCache.TagId.FONT_RESAMPLE);

		if (fontResampleStyle != null) {
			textEditor.setSuperResample(((XulPropParser.xulParsedProp_Float) fontResampleStyle.getParsedValue()).val);
		} else {
			textEditor.setSuperResample(1.0f);
		}

		if (fontScaleXStyle != null) {
			textEditor.fontScaleX(((XulPropParser.xulParsedStyle_FontScaleX) fontScaleXStyle.getParsedValue()).val);
		} else {
			textEditor.fontScaleX(1.0f);
		}

		if (fontStrikeStyle != null) {
			textEditor.setFontStrikeThrough(((XulPropParser.xulParsedStyle_FontStyleStrike) fontStrikeStyle.getParsedValue()).val);
		} else {
			textEditor.setFontStrikeThrough(false);
		}

		if (fontFaceStyle != null) {
			textEditor.setFontFace(fontFaceStyle.getStringValue());
		} else {
			textEditor.setFontFace(null);
		}

		if (lineHeightStyle != null) {
			textEditor.setLineHeightScalar(((XulPropParser.xulParsedStyle_LineHeight) lineHeightStyle.getParsedValue()).val);
		} else {
			textEditor.setLineHeightScalar(1.0f);
		}

		if (fontUnderlineStyle != null) {
			textEditor.setUnderline(((XulPropParser.xulParsedProp_booleanValue) fontUnderlineStyle.getParsedValue()).val);
		} else {
			textEditor.setUnderline(false);
		}

		if (fontItalicStyle != null) {
			textEditor.setItalic(((XulPropParser.xulParsedProp_booleanValue) fontItalicStyle.getParsedValue()).val);
		} else {
			textEditor.setItalic(false);
		}

		if (fixHalfCharStyle != null) {
			textEditor.setFixHalfChar(((XulPropParser.xulParsedStyle_FixHalfChar) fixHalfCharStyle.getParsedValue()).val);
		} else {
			textEditor.setFixHalfChar(false);
		}

		double xScalar = getXScalar();
		double yScalar = getYScalar();

		if (fontSizeStyle != null) {
			XulPropParser.xulParsedStyle_FontSize fontSize = fontSizeStyle.getParsedValue();
			textEditor.setFontSize((float) (xScalar * fontSize.val));
		} else {
			textEditor.setFontSize((float) (xScalar * 12));
		}

		if (startIndentStyle != null) {
			textEditor.setStartIndent((float) (xScalar * ((XulPropParser.xulParsedProp_Float) startIndentStyle.getParsedValue()).val));
		} else {
			textEditor.setStartIndent(0.0f);
		}

		if (endIndentStyle != null) {
			textEditor.setEndIndent((float) (xScalar * ((XulPropParser.xulParsedProp_Float) endIndentStyle.getParsedValue()).val));
		} else {
			textEditor.setEndIndent(0.0f);
		}

		if (fontColorStyle != null) {
			XulPropParser.xulParsedStyle_FontColor fontColor = fontColorStyle.getParsedValue();
			textEditor.setFontColor(fontColor.val);
		} else {
			textEditor.setFontColor(Color.BLACK);
		}

		if (fontWeightStyle != null) {
			XulPropParser.xulParsedStyle_FontWeight fontWeight = fontWeightStyle.getParsedValue();
			textEditor.setFontWeight((float) (xScalar * fontWeight.val));
		} else {
			textEditor.setFontWeight((float) (xScalar * 1.0));
		}

		if (fontShadowStyle != null) {
			XulPropParser.xulParsedStyle_FontShadow fontShadow = fontShadowStyle.getParsedValue();
			textEditor.setFontShadow((float) (fontShadow.xOffset * xScalar),
				(float) (fontShadow.yOffset * yScalar),
				(float) (fontShadow.size * xScalar),
				fontShadow.color
			);
		} else {
			textEditor.setFontShadow(0f, 0f, 0f, 0);
		}

		if (fontAlignStyle != null) {
			XulPropParser.xulParsedStyle_FontAlign fontAlign = fontAlignStyle.getParsedValue();
			textEditor.setFontAlignment(fontAlign.xAlign, fontAlign.yAlign);
		} else {
			textEditor.setFontAlignment(0f, 0f);
		}

		String multilineAttr = _view.getAttrString(XulPropNameCache.TagId.MULTI_LINE);
		if (TextUtils.isEmpty(multilineAttr)) {
			textEditor.setMultiline(textEditor.defMultiline());
		} else if ("true".equals(multilineAttr)) {
			textEditor.setMultiline(true);
		} else {
			textEditor.setMultiline(false);
		}

		String autoWrapAttr = _view.getAttrString(XulPropNameCache.TagId.AUTO_WRAP);
		if (TextUtils.isEmpty(autoWrapAttr)) {
			textEditor.setAutoWrap(textEditor.defAutoWrap());
		} else if ("true".equals(autoWrapAttr)) {
			textEditor.setAutoWrap(true);
		} else {
			textEditor.setAutoWrap(false);
		}

		String ellipsisAttr = _view.getAttrString(XulPropNameCache.TagId.ELLIPSIS);
		if (TextUtils.isEmpty(ellipsisAttr)) {
			textEditor.setDrawEllipsis(textEditor.defDrawEllipsis());
		} else if ("true".equals(ellipsisAttr)) {
			textEditor.setDrawEllipsis(true);
		} else {
			textEditor.setDrawEllipsis(false);
		}

		XulAttr marqueeAttr = _view.getAttr(XulPropNameCache.TagId.MARQUEE);
		if (marqueeAttr == null) {
			textEditor.setTextMarquee(null);
		} else {
			XulPropParser.xulParsedAttr_Text_Marquee marquee = marqueeAttr.getParsedValue();
			textEditor.setTextMarquee(marquee);
		}

		textEditor.finish(recalAutoWrap);
	}

	@Override
	public boolean onKeyEvent(KeyEvent event) {
		XulMarqueeTextRenderer textRenderer = _textRenderer;
		if (textRenderer == null) {
			return false;
		}
		if (textRenderer.isMultiline() && !(textRenderer.isAutoWrap() && textRenderer.isDrawingEllipsis())) {
			int scrollLines = 3;
			// 处理滚动事件
			int viewHeight = XulUtils.calRectHeight(_rect) - _padding.top - _padding.bottom;
			float textHeight = textRenderer.getHeight();
			float lineHeight = textRenderer.getLineHeight();
			if (textHeight > viewHeight && event.getAction() == KeyEvent.ACTION_DOWN) {
				int maxMumScrollStep = (int) (viewHeight / lineHeight - 1);
				if (maxMumScrollStep <= 0) {
					maxMumScrollStep = 1;
				}
				if (scrollLines > maxMumScrollStep) {
					scrollLines = maxMumScrollStep;
				}
				float scrollDelta = lineHeight * scrollLines;
				switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_DPAD_UP:
					if (_scrollTargetY < 0) {
						_scrollTargetY += scrollDelta;
						_scrollTargetY -= (_scrollTargetY % lineHeight);
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
					int minScrollYPos = (int) (viewHeight - textHeight);
					if (_scrollTargetY > minScrollYPos) {
						_scrollTargetY -= scrollDelta;
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
		}
		return false;
	}

	public void scrollToY(int yPos) {
		_scrollTargetY = yPos;
	}

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		super.syncData();
		XulAttr attrScrollbar = _view.getAttr(XulPropNameCache.TagId.SCROLLBAR);
		if (attrScrollbar == null || attrScrollbar.getValue() == null) {
			XulMarqueeTextRenderer textRenderer = _textRenderer;
			if (textRenderer != null && textRenderer.isMultiline() && !(textRenderer.isAutoWrap() && textRenderer.isDrawingEllipsis())) {
				_scrollBar = XulRenderFactory.buildScrollBar(_scrollBar, "simple", _getScrollBarHelper(), this);
			} else {
				_scrollBar = XulRenderFactory.buildScrollBar(_scrollBar, "", _getScrollBarHelper(), this);
			}
		} else {
			_scrollBar = XulRenderFactory.buildScrollBar(_scrollBar, attrScrollbar.getStringValue(), _getScrollBarHelper(), this);
		}

		XulAttr attrScrollPosX = _view.getAttr(XulPropNameCache.TagId.SCROLL_POS_X);
		XulAttr attrScrollPosY = _view.getAttr(XulPropNameCache.TagId.SCROLL_POS_Y);

		if (attrScrollPosX != null) {
			int val = ((XulPropParser.xulParsedAttr_XY) attrScrollPosX.getParsedValue()).val;
			if (val <= XulManager.SIZE_MAX) {
				_scrollX = val;
				markDirtyView();
			}
		}

		if (attrScrollPosY != null) {
			int val = ((XulPropParser.xulParsedAttr_XY) attrScrollPosY.getParsedValue()).val;
			if (val <= XulManager.SIZE_MAX) {
				_scrollY = val;
				_scrollTargetY = val;
				markDirtyView();
			}
		}
	}

	protected void prepareRenderer() {
		String textEngine = _view.getStyleString(XulPropNameCache.TagId.FONT_RENDER);
		if ("spannable".equals(textEngine)) {
			if (!(_baseTextRenderer instanceof XulSpannableTextRenderer)) {
				_baseTextRenderer = new XulSpannableTextRenderer(this);
			}
		} else {
			if (!(_baseTextRenderer instanceof XulSimpleTextRenderer)) {
				_baseTextRenderer = new XulSimpleTextRenderer(this);
			}
		}
		if (_textRenderer == null) {
			_textRenderer = new XulMarqueeTextRenderer(this, _baseTextRenderer);
		} else {
			_textRenderer.updateBaseTextRender(_baseTextRenderer);
		}
	}

	public BaseScrollBar getScrollBar() {
		return _scrollBar;
	}

	public BaseScrollBar.ScrollBarHelper getScrollBarHelper() {
		return _getScrollBarHelper();
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
					return (int) _textRenderer.getWidth();
				}

				@Override
				public int getContentHeight() {
					return (int) _textRenderer.getHeight();
				}
			};
		}
		return _scrollBarHelper;
	}

	protected Paint getTextPaint() {
		return _textRenderer.getTextPaint();
	}

	protected XulTextRenderer getTextRenderer() {
		return _textRenderer;
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_NOFOCUS;
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutElement();
	}

	protected class LayoutElement extends XulViewRender.LayoutElement {
		int _initialWidth = 0;
		boolean _recalAutoWrap = false;
		boolean _paddingChanged = false;
		boolean _autoHeight = false;

		@Override
		public int prepare() {
			_initialWidth = _rect == null ? 0 : XulUtils.calRectWidth(_rect);
			_recalAutoWrap = false;
			_paddingChanged = false;
			_autoHeight = false;

			int paddingX = _padding != null ? _padding.left + _padding.right : 0;
			super.prepare();
			paddingX -= _padding != null ? _padding.left + _padding.right : 0;
			if (paddingX != 0) {
				_paddingChanged = true;
			}
			prepareRenderer();
			if (_isVisible()) {
				syncTextInfo();
			}

			if (_rect != null) {
				int curWidth = XulUtils.calRectWidth(_rect);
				int curHeight = XulUtils.calRectHeight(_rect);
				_autoHeight = curHeight == XulManager.SIZE_MATCH_CONTENT || curHeight == XulManager.SIZE_AUTO;
				_recalAutoWrap = _textRenderer.isMultiline() && _textRenderer.isAutoWrap() && _autoHeight && curWidth == XulManager.SIZE_MATCH_PARENT;
			}
			return 0;
		}

		@Override
		public boolean setWidth(int w) {
			if (_recalAutoWrap && _initialWidth != w) {
				boolean ret = super.setWidth(w);
				syncTextInfo(true);
				setHeight(constrainHeight((int) (_textRenderer.getHeight() + _padding.top + _padding.bottom)));
				return ret;
			} else if (_textRenderer.isMultiline() && _textRenderer.isAutoWrap() && _paddingChanged) {
				boolean ret = super.setWidth(w);
				syncTextInfo(true);
				if (_autoHeight) {
					setHeight(constrainHeight((int) (_textRenderer.getHeight() + _padding.top + _padding.bottom)));
				}
				return ret;
			}
			return super.setWidth(w);
		}

		@Override
		public int getContentWidth() {
			return (int) _textRenderer.getWidth();
		}

		@Override
		public int getContentHeight() {
			return (int) _textRenderer.getHeight();
		}
	}

}
