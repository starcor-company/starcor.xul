package com.starcor.xul.Render.Text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.Transform.ITransformer;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulLayout;
import com.starcor.xul.XulWorker;

/**
 * Created by hy on 2015/12/11.
 */
public class XulMarqueeTextRenderer extends XulTextRenderer {
	final XulViewRender _render;
	XulTextRenderer _textRenderer;
	TextMarqueeAnimation _marqueeAnimation;
	float _marqueePosition = -1;
	int _marqueeDirection = 1;
	private XulMarqueeTextEditor _editor;

	public XulMarqueeTextRenderer(XulViewRender render, XulTextRenderer textDrawer) {
		_render = render;
		_textRenderer = textDrawer;
	}

	@Override
	public void drawText(XulDC dc, float xBase, float yBase, float clientViewWidth, float clientViewHeight) {
		if (_marqueeAnimation == null || !_marqueeAnimation._running || isMultiline() || isAutoWrap() || getWidth() <= clientViewWidth) {
			stopMarqueeAnimation();
			_textRenderer.drawText(dc, xBase, yBase, clientViewWidth, clientViewHeight);
			return;
		}

		int marqueeSpace = _marqueeAnimation._marqueeSpace;
		float space;
		if (marqueeSpace > 0) {
			space = marqueeSpace;
		} else {
			space = -clientViewWidth * marqueeSpace / 100;
		}

		boolean isRTL = _render.isRTL();
		float textWidth = this.getWidth();
		float textXPos = 0;
		if (isRTL) {
			textXPos = clientViewWidth - textWidth;
		}

		int marqueeDirection = isRTL ? -_marqueeDirection : _marqueeDirection;

		textXPos -= _marqueePosition * marqueeDirection;
		if (textXPos < clientViewWidth || textXPos + textWidth > 0) {
			dc.save(Canvas.MATRIX_SAVE_FLAG);
			_textRenderer.drawText(dc, textXPos + xBase, yBase, textWidth, clientViewHeight);
			dc.restore();
		}

		float drawPos = textXPos + (textWidth + space) * marqueeDirection;
		if (drawPos < clientViewWidth || drawPos + textWidth > 0) {
			_textRenderer.drawText(dc, drawPos, yBase, textWidth, clientViewHeight);
		}
	}

	@Override
	public void stopAnimation() {
		_textRenderer.stopAnimation();
		stopMarqueeAnimation();
	}

	protected void stopMarqueeAnimation() {
		if (_marqueeAnimation == null) {
			return;
		}
		_marqueeAnimation.stop();
		if (_marqueePosition < 0) {
			return;
		}
		_render.markDirtyView();
		_marqueePosition = -1;
	}

	@Override
	public Paint getTextPaint() {
		return _textRenderer.getTextPaint();
	}

	@Override
	public boolean isMultiline() {
		return _textRenderer.isMultiline();
	}

	@Override
	public boolean isAutoWrap() {
		return _textRenderer.isAutoWrap();
	}

	@Override
	public boolean isDrawingEllipsis() {
		return _textRenderer.isDrawingEllipsis();
	}

	@Override
	public float getHeight() {
		return _textRenderer.getHeight();
	}

	@Override
	public float getLineHeight() {
		return _textRenderer.getLineHeight();
	}

	@Override
	public float getWidth() {
		return _textRenderer.getWidth();
	}

	@Override
	public boolean isEmpty() {
		return _textRenderer.isEmpty();
	}

	@Override
	public XulMarqueeTextEditor edit() {
		if (_editor == null) {
			_editor = new XulMarqueeTextEditor(_textRenderer.edit());
		} else {
			_editor.update(_textRenderer.edit());
		}
		return _editor;
	}

	@Override
	public XulWorker.DrawableItem collectPendingImageItem() {
		return _textRenderer.collectPendingImageItem();
	}

	@Override
	public String getText() {
		return _textRenderer != null ? _textRenderer.getText() : null;
	}

	public void updateBaseTextRender(XulTextRenderer baseTextRenderer) {
		_textRenderer = baseTextRenderer;
	}

	public class XulMarqueeTextEditor extends XulTextEditor {
		private XulTextEditor _upperEditor;

		public XulMarqueeTextEditor(XulTextEditor editor) {
			_upperEditor = editor;
		}

		public XulMarqueeTextEditor setTextMarquee(XulPropParser.xulParsedAttr_Text_Marquee marquee) {
			if (marquee == null) {
				stopAnimation();
				return this;
			}
			if (_marqueeAnimation == null) {
				_marqueeAnimation = new TextMarqueeAnimation(_render);
			}
			_marqueeAnimation.prepareMarqueeAnimation(marquee);
			return this;
		}

		public void update(XulTextEditor editor) {
			_upperEditor = editor;
		}

		@Override
		public XulMarqueeTextEditor setText(String text) {
			_upperEditor.setText(text);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setSuperResample(float superResample) {
			_upperEditor.setSuperResample(superResample);
			return this;
		}

		@Override
		public XulMarqueeTextEditor fontScaleX(float fontScaleX) {
			_upperEditor.fontScaleX(fontScaleX);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setFontStrikeThrough(boolean fontStrikeThrough) {
			_upperEditor.setFontStrikeThrough(fontStrikeThrough);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setFontFace(String fontFace) {
			_upperEditor.setFontFace(fontFace);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setLineHeightScalar(float lineHeightScalar) {
			_upperEditor.setLineHeightScalar(lineHeightScalar);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setUnderline(boolean underline) {
			_upperEditor.setUnderline(underline);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setItalic(boolean italic) {
			_upperEditor.setItalic(italic);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setFixHalfChar(boolean fixHalfChar) {
			_upperEditor.setFixHalfChar(fixHalfChar);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setFontSize(float fontSize) {
			_upperEditor.setFontSize(fontSize);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setStartIndent(float startIndent) {
			_upperEditor.setStartIndent(startIndent);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setEndIndent(float endIndent) {
			_upperEditor.setEndIndent(endIndent);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setFontColor(int color) {
			_upperEditor.setFontColor(color);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setFontWeight(float weight) {
			_upperEditor.setFontWeight(weight);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setFontShadow(float xOff, float yOff, float size, int color) {
			_upperEditor.setFontShadow(xOff, yOff, size, color);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setFontAlignment(float xAlign, float yAlign) {
			_upperEditor.setFontAlignment(xAlign, yAlign);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setMultiline(boolean multiline) {
			_upperEditor.setMultiline(multiline);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setAutoWrap(boolean autoWrap) {
			_upperEditor.setAutoWrap(autoWrap);
			return this;
		}

		@Override
		public XulMarqueeTextEditor setDrawEllipsis(boolean drawEllipsis) {
			_upperEditor.setDrawEllipsis(drawEllipsis);
			return this;
		}

		@Override
		public void finish(boolean recalAutoWrap) {
			if (_marqueeAnimation != null && !isEmpty()) {
				_render.addAnimation(_marqueeAnimation);
			}
			_upperEditor.finish(recalAutoWrap);
		}

		@Override
		public boolean defMultiline() {
			return _upperEditor.defMultiline();
		}

		@Override
		public boolean defAutoWrap() {
			return _upperEditor.defAutoWrap();
		}

		@Override
		public boolean defDrawEllipsis() {
			return _upperEditor.defDrawEllipsis();
		}
	}

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
			if (isEmpty()) {
				_running = false;
			}

			if (_marqueeSpeed <= 0 || !_running) {
				_marqueePosition = -1;
				return false;
			}
			long progress = timestamp - _beginTime;
			if (progress <= _marqueeDelay) {
				return true;
			}

			if (isMultiline()) {
				return false;
			}

			if (_render.isInvisible()) {
				return false;
			}

			Rect _padding = _render.getPadding();

			if (_intervalBeginTime == _beginTime) {
				progress -= _marqueeDelay;
				_marqueePosition = getHeight() / 1.1f * progress / _marqueeSpeed;

				float marqueeTextWidth = getWidth();
				if (_marqueeSpace >= 0) {
					marqueeTextWidth += _marqueeSpace;
				} else {
					int viewWidth = _render.getWidth();
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
				XulArea parent = _render.getParentView();
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
					if (render.isInvisible()) {
						return false;
					}
					parent = parent.getParent();
				}
			}
			_render.markDirtyView();
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
				_intervalBeginTime = _beginTime = _render.animationTimestamp();
				_marqueeSpeed = marquee.speed;
				if (_marqueeSpeed == 0) {
					_marqueePosition = -1;
				} else {
					_marqueePosition = 0;
				}
			}
		}
	}
}
