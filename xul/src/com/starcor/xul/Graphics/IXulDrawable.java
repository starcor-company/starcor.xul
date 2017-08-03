package com.starcor.xul.Graphics;

import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Created by hy on 2014/6/4.
 */
public interface IXulDrawable {
	boolean draw(XulDC dc, Rect updateRc, int xBase, int yBase);
	boolean draw(XulDC dc, RectF updateRc, float xBase, float yBase);
}
