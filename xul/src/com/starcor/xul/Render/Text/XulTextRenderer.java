package com.starcor.xul.Render.Text;

import android.graphics.Paint;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.XulWorker;

/**
 * Created by hy on 2015/12/11.
 */
public abstract class XulTextRenderer {
	public abstract void drawText(XulDC dc, float xBase, float yBase, float clientViewWidth, float clientViewHeight);

	public abstract void stopAnimation();

	public abstract Paint getTextPaint();

	public abstract boolean isMultiline();

	public abstract boolean isAutoWrap();

	public abstract boolean isDrawingEllipsis();

	public abstract float getHeight();

	public abstract float getLineHeight();

	public abstract float getWidth();

	public abstract boolean isEmpty();

	public abstract XulTextEditor edit();

	public abstract XulWorker.DrawableItem collectPendingImageItem();

	public abstract String getText();

	public abstract class XulTextEditor {
		public abstract XulTextEditor setText(String newText);

		public abstract XulTextEditor setSuperResample(float superResample);

		public abstract XulTextEditor fontScaleX(float fontScaleX);

		public abstract XulTextEditor setFontStrikeThrough(boolean fontStrikeThrough);

		public abstract XulTextEditor setFontFace(String fontFace);

		public abstract XulTextEditor setLineHeightScalar(float lineHeightScalar);

		public abstract XulTextEditor setUnderline(boolean underline);

		public abstract XulTextEditor setItalic(boolean italic);

		public abstract XulTextEditor setFixHalfChar(boolean fixHalfChar);

		public abstract XulTextEditor setFontSize(float fontSize);

		public abstract XulTextEditor setStartIndent(float startIndent);

		public abstract XulTextEditor setEndIndent(float endIndent);

		public abstract XulTextEditor setFontColor(int color);

		public abstract XulTextEditor setFontWeight(float weight);

		public abstract XulTextEditor setFontShadow(float xOff, float yOff, float size, int color);

		public abstract XulTextEditor setFontAlignment(float xAlign, float yAlign);

		public abstract XulTextEditor setMultiline(boolean multiline);

		public abstract XulTextEditor setAutoWrap(boolean autoWrap);

		public abstract XulTextEditor setDrawEllipsis(boolean drawEllipsis);

		public abstract void finish(boolean recalAutoWrap);

		public abstract boolean defMultiline();

		public abstract boolean defAutoWrap();

		public abstract boolean defDrawEllipsis();
	}
}
