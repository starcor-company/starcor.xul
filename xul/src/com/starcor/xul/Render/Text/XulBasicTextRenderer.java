package com.starcor.xul.Render.Text;

import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;

import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.XulRenderContext;

/**
 * Created by hy on 2015/12/11.
 */
public abstract class XulBasicTextRenderer extends XulTextRenderer {
	protected final XulViewRender _render;
	protected float _fontSize = 12;
	protected int _fontColor = Color.BLACK;
	protected float _fontWeight = 0.0f;
	protected float _fontShadowX = 0;
	protected float _fontShadowY = 0;
	protected float _fontShadowSize = 0;
	protected int _fontShadowColor = 0;
	protected float _fontAlignY = 0.0f;
	protected float _fontAlignX = 0.0f;
	protected float _lineHeightScalar = 1.0f;
	protected float _fontScaleX = 1.0f;
	protected String _fontFace = null;
	protected String _text = "";
	protected float _textWidth;
	protected float _textHeight;
	protected float _textBaseLineTop;
	protected float _textLineHeight;
	protected float _ellipsisWidth;
	protected float _startIndent = 0;
	protected float _endIndent = 0;
	protected float _superResample = 1.0f;
	protected boolean _fontItalic = false;
	protected boolean _fontUnderline = false;
	protected boolean _fontStrikeThrough = false;
	protected boolean _fixHalfChar = false;
	protected boolean _multiline = false;
	protected boolean _autoWrap = false;
	protected boolean _drawEllipsis = false; // …

	public XulBasicTextRenderer(XulViewRender render) {_render = render;}

	@Override
	public Paint getTextPaint() {
		return _getTextPaint();
	}

	public String getText() {
		return _text;
	}

	protected Paint _getTextPaint() {
		return _getTextPaint(1.0f);
	}

	protected Paint _getTextPaint(float fontSizeScale) {
		XulRenderContext ctx = _render.getRenderContext();
		Paint defPaint = ctx.getTextPaintByName(_fontFace);

		if (!(_fontShadowSize == 0 || (_fontShadowColor & 0xFF000000) == 0)) {
			defPaint = ctx.getShadowTextPaintByName(_fontFace);
			defPaint.setShadowLayer(_fontShadowSize, _fontShadowX, _fontShadowY, _fontShadowColor);
		}

		defPaint.setColor(_fontColor);
		if (Math.abs(fontSizeScale - 1.0f) > 0.01f) {
			defPaint.setTextSize(_fontSize * fontSizeScale);
		} else {
			defPaint.setTextSize(_fontSize);
		}

		if (_fontWeight > 1.0) {
			if (_fontWeight > 2.5) {
				defPaint.setStrokeWidth(_fontWeight*fontSizeScale/2);
			} else {
				defPaint.setFakeBoldText(true);
			}
		} else {
			defPaint.setFakeBoldText(false);
		}
		defPaint.setTextScaleX(_fontScaleX);
		defPaint.setUnderlineText(_fontUnderline);
		defPaint.setStrikeThruText(_fontStrikeThrough);
		defPaint.setTextSkewX(_fontItalic ? -0.25f : 0);
		defPaint.setTextAlign(Paint.Align.LEFT);
		return defPaint;
	}

	@Override
	public boolean isMultiline() {
		return _multiline;
	}

	@Override
	public boolean isAutoWrap() {
		return _autoWrap;
	}

	@Override
	public boolean isDrawingEllipsis() {
		return _drawEllipsis;
	}

	@Override
	public float getHeight() {
		return _textHeight;
	}

	@Override
	public float getLineHeight() {
		return _textLineHeight;
	}

	@Override
	public float getWidth() {
		return _textWidth;
	}

	@Override
	public boolean isEmpty() {
		return TextUtils.isEmpty(_text);
	}

	class BasicTextEditor extends XulTextEditor {
		protected boolean _testAndSetAnyChanged = false;

		protected <T> T _testAndSet(T oldVal, T newVal) {
			if (oldVal == newVal) {
				return oldVal;
			}
			_testAndSetAnyChanged = true;
			return newVal;
		}

		protected float _testAndSet(float oldVal, float newVal) {
			if (Math.abs(oldVal - newVal) < 0.001f) {
				return oldVal;
			}
			_testAndSetAnyChanged = true;
			return newVal;
		}

		protected String _testAndSet(String oldVal, String newVal) {
			if (oldVal == newVal || (newVal == null ? TextUtils.isEmpty(oldVal) : newVal.equals(oldVal))) {
				return oldVal;
			}
			_testAndSetAnyChanged = true;
			return newVal;
		}

		@Override
		public XulTextEditor setText(String newText) {
			_text = _testAndSet(_text, newText);
			return this;
		}

		@Override
		public XulTextEditor setSuperResample(float superResample) {
			_superResample = superResample;
			return this;
		}

		@Override
		public XulTextEditor fontScaleX(float fontScaleX) {
			_fontScaleX = _testAndSet(_fontScaleX, fontScaleX);
			return this;
		}

		@Override
		public XulTextEditor setFontStrikeThrough(boolean fontStrikeThrough) {
			_fontStrikeThrough = fontStrikeThrough;
			return this;
		}

		@Override
		public XulTextEditor setFontFace(String fontFace) {
			_fontFace = _testAndSet(_fontFace, fontFace);
			return this;
		}

		@Override
		public XulTextEditor setLineHeightScalar(float lineHeightScalar) {
			_lineHeightScalar = _testAndSet(_lineHeightScalar, lineHeightScalar);
			return this;
		}

		@Override
		public XulTextEditor setUnderline(boolean val) {
			_fontUnderline = val;
			return this;
		}

		@Override
		public XulTextEditor setItalic(boolean val) {
			_fontItalic = _testAndSet(_fontItalic, val);
			return this;
		}

		@Override
		public XulTextEditor setFixHalfChar(boolean fixHalfChar) {
			_fixHalfChar = fixHalfChar;
			return this;
		}

		@Override
		public XulTextEditor setFontSize(float fontSize) {
			_fontSize = _testAndSet(_fontSize, fontSize);
			return this;
		}

		@Override
		public XulTextEditor setStartIndent(float v) {
			_startIndent = _testAndSet(_startIndent, v);

			return this;
		}

		@Override
		public XulTextEditor setEndIndent(float v) {
			_endIndent = _testAndSet(_endIndent, v);
			return this;
		}

		@Override
		public XulTextEditor setFontColor(int color) {
			_fontColor = color;
			return this;
		}

		@Override
		public XulTextEditor setFontWeight(float weight) {
			_fontWeight = _testAndSet(_fontWeight, weight);
			return this;
		}

		@Override
		public XulTextEditor setFontShadow(float xOff, float yOff, float size, int color) {
			_fontShadowX = xOff;
			_fontShadowY = yOff;
			_fontShadowSize = size;
			_fontShadowColor = color;
			return this;
		}

		@Override
		public XulTextEditor setFontAlignment(float xAlign, float yAlign) {
			_fontAlignX = xAlign;
			_fontAlignY = yAlign;
			return this;
		}

		@Override
		public XulTextEditor setMultiline(boolean multiline) {
			_multiline = _testAndSet(_multiline, multiline);
			return this;
		}

		@Override
		public XulTextEditor setAutoWrap(boolean autoWrap) {
			_autoWrap = _testAndSet(_autoWrap, autoWrap);
			return this;
		}

		@Override
		public XulTextEditor setDrawEllipsis(boolean drawEllipsis) {
			_drawEllipsis = _testAndSet(_drawEllipsis, drawEllipsis);
			return this;
		}

		@Override
		public void finish(boolean recalAutoWrap) {
			if (_render.isRTL()) {
				_fontAlignX = 1.0f - _fontAlignX;
			}
		}

		@Override
		public boolean defMultiline() {
			return false;
		}

		@Override
		public boolean defAutoWrap() {
			return false;
		}

		@Override
		public boolean defDrawEllipsis() {
			return false;
		}

		public void reset() {
			_testAndSetAnyChanged = false;
		}
	}
}
