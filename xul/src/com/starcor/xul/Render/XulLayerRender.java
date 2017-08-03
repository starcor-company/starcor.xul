package com.starcor.xul.Render;

import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.shapes.RoundRectShape;

import com.starcor.xul.Graphics.BitmapTools;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Prop.XulStyle;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.Transform.ITransformer;
import com.starcor.xul.Render.Transform.TransformerFactory;
import com.starcor.xul.Utils.XulAreaChildrenRender;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by hy on 2015/4/13.
 */
public class XulLayerRender extends XulAreaRender {
	public static void register() {
		XulRenderFactory.registerBuilder("area", "layer", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				return new XulLayerRender(ctx, (XulArea) view);
			}
		});
	}

	static ArrayList<WeakReference<XulLayerRender>> _cachedView = new ArrayList<WeakReference<XulLayerRender>>();

	Bitmap createBitmapCache(int w, int h) {
		for (int i = 0, cachedViewSize = _cachedView.size(); i < cachedViewSize; ) {
			WeakReference<XulLayerRender> cachedLayer = _cachedView.get(i);
			XulLayerRender xulLayerRender = cachedLayer.get();
			if (xulLayerRender == null) {
				_cachedView.remove(i);
				--cachedViewSize;
				continue;
			}
			Bitmap bitmap = xulLayerRender._cachedBitmap;
			if (bitmap == null) {
				_cachedView.remove(i);
				--cachedViewSize;
				continue;
			}
			int bmpW = bitmap.getWidth();
			int bmpH = bitmap.getHeight();

			if (bmpW != w || bmpH != h) {
				++i;
				continue;
			}

			XulView view = xulLayerRender._view;
			RectF focusRect = xulLayerRender.getFocusRect();
			RectF rootRc = view.getRootLayout().getFocusRc();
			if (XulUtils.intersects(rootRc, focusRect) && view.isVisible() && view.isParentVisible()) {
				++i;
				continue;
			}
			xulLayerRender._cachedBitmap = null;
			_cachedView.remove(i);

			_cacheBmpCanvas.setBitmap(bitmap);
			_cacheBmpCanvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
			_cacheBmpCanvas.setBitmap(null);

			_cachedView.add(new WeakReference<XulLayerRender>(this));
			return bitmap;
		}

		_cachedView.add(new WeakReference<XulLayerRender>(this));
		return BitmapTools.createBitmapFromRecycledBitmaps(w, h, XulManager.DEF_PIXEL_FMT);
	}

	public static final String LAYER_MASK_ID = "@layer-mask";

	public XulLayerRender(XulRenderContext ctx, XulArea area) {
		super(ctx, area);
		int childNum = _area.getChildNum();
		for (int i = 0; i < childNum; i++) {
			XulView child = _area.getChild(i);
			if (child == null) {
				return;
			}
			if (child.getId().equals(LAYER_MASK_ID)) {
				_hasMaskLayer = true;
				break;
			}
		}
	}

	@Override
	public boolean setUpdateLayout(boolean updateByChild) {
		_refreshCache = true;
		return super.setUpdateLayout(updateByChild);
	}

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		_contentCache = "enabled".equals(_area.getAttrString(XulPropNameCache.TagId.DRAWING_CACHE));

		int duration = _aniTransformDuration;
		if (!_hasAnimation()) {
			duration = 0;
		}

		if (_aniTransformer != null) {
			_ani.setTransformer(_aniTransformer);
		} else {
			_ani.setTransformer(_LINEAR_TRANSFORMER);
		}

		if (duration <= 0 || _isBooting()) {
			terminateAnimation();
		} else {
			_ani.startAnimation(duration);
		}

		super.syncData();
		float xScalar = (float) getXScalar();
		float yScalar = (float) getYScalar();

		XulStyle translateXY = _area.getStyle(XulPropNameCache.TagId.STYLE_TRANSLATE);
		_offsetX = _offsetY = 0f;
		if (translateXY != null) {
			XulPropParser.xulParsedProp_FloatArray parsedVal = translateXY.getParsedValue();
			switch (parsedVal.getLength()) {
			case 1:
				_offsetX = _offsetY = parsedVal.tryGetVal(0, 0f) * xScalar;
				break;
			case 2:
				_offsetX = parsedVal.tryGetVal(0, 0f) * xScalar;
				_offsetY = parsedVal.tryGetVal(1, 0f) * yScalar;
				break;
			default:
				break;
			}
		}

		XulStyle translateX = _area.getStyle(XulPropNameCache.TagId.STYLE_TRANSLATE_X);
		XulStyle translateY = _area.getStyle(XulPropNameCache.TagId.STYLE_TRANSLATE_Y);
		if (translateX != null) {
			_offsetX = ((XulPropParser.xulParsedProp_Float) translateX.getParsedValue()).tryGetVal(0) * xScalar;
		}

		if (translateY != null) {
			_offsetY = ((XulPropParser.xulParsedProp_Float) translateY.getParsedValue()).tryGetVal(0) * xScalar;
		}

		XulStyle opacityStyle = _area.getStyle(XulPropNameCache.TagId.STYLE_OPACITY);
		if (opacityStyle != null) {
			_opacity = ((XulPropParser.xulParsedProp_Float) opacityStyle.getParsedValue()).tryGetVal(0);
		} else {
			_opacity = 1f;
		}

		XulStyle rotateXYZ = _area.getStyle(XulPropNameCache.TagId.STYLE_ROTATE);

		_rotateCenterX = 0.5f;
		_rotateCenterY = 0.5f;
		_rotateCenterZ = 0f;
		_rotateX = 0f;
		_rotateY = 0f;
		_rotateZ = 0f;
		if (rotateXYZ != null) {
			XulPropParser.xulParsedProp_FloatArray parsedVal = rotateXYZ.getParsedValue();
			switch (parsedVal.getLength()) {
			case 2:
				_rotateX = parsedVal.tryGetVal(0, 0f);
				_rotateY = parsedVal.tryGetVal(1, 0f);
				break;
			case 3:
				_rotateX = parsedVal.tryGetVal(0, 0f);
				_rotateY = parsedVal.tryGetVal(1, 0f);
				_rotateZ = parsedVal.tryGetVal(2, 0f);
				break;
			case 4:
				_rotateX = parsedVal.tryGetVal(0, 0f);
				_rotateY = parsedVal.tryGetVal(1, 0f);
				_rotateCenterX = parsedVal.tryGetVal(2, _rotateCenterX);
				_rotateCenterY = parsedVal.tryGetVal(3, _rotateCenterY);
				break;
			case 6:
				_rotateX = parsedVal.tryGetVal(0, 0f);
				_rotateY = parsedVal.tryGetVal(1, 0f);
				_rotateZ = parsedVal.tryGetVal(2, 0f);
				_rotateCenterX = parsedVal.tryGetVal(3, _rotateCenterX);
				_rotateCenterY = parsedVal.tryGetVal(4, _rotateCenterY);
				_rotateCenterZ = parsedVal.tryGetVal(5, _rotateCenterZ) * xScalar;
				break;
			}
		}

		XulStyle rotateX = _area.getStyle(XulPropNameCache.TagId.STYLE_ROTATE_X);
		XulStyle rotateY = _area.getStyle(XulPropNameCache.TagId.STYLE_ROTATE_Y);
		XulStyle rotateZ = _area.getStyle(XulPropNameCache.TagId.STYLE_ROTATE_Z);

		if (rotateX != null) {
			XulPropParser.xulParsedProp_FloatArray parsedVal = rotateX.getParsedValue();
			switch (parsedVal.getLength()) {
			case 1:
				_rotateX = parsedVal.tryGetVal(0, 0f);
				_rotateCenterX = 0.5f;
				break;
			case 2:
				_rotateX = parsedVal.tryGetVal(0, 0f);
				_rotateCenterX = parsedVal.tryGetVal(1, _rotateCenterX);
				break;
			}
		}

		if (rotateY != null) {
			XulPropParser.xulParsedProp_FloatArray parsedVal = rotateY.getParsedValue();
			switch (parsedVal.getLength()) {
			case 1:
				_rotateY = parsedVal.tryGetVal(0, 0f);
				_rotateCenterY = 0.5f;
				break;
			case 2:
				_rotateY = parsedVal.tryGetVal(0, 0f);
				_rotateCenterY = parsedVal.tryGetVal(1, _rotateCenterY);
				break;
			}
		}

		if (rotateZ != null) {
			XulPropParser.xulParsedProp_FloatArray parsedVal = rotateZ.getParsedValue();
			switch (parsedVal.getLength()) {
			case 1:
				_rotateZ = parsedVal.tryGetVal(0, 0f);
				_rotateCenterZ = 0.5f;
				break;
			case 2:
				_rotateZ = parsedVal.tryGetVal(0, 0f);
				_rotateCenterZ = parsedVal.tryGetVal(1, _rotateCenterZ) * xScalar;
				break;
			}
		}

		XulStyle rotateCenterXYZ = _area.getStyle(XulPropNameCache.TagId.STYLE_ROTATE_CENTER);

		if (rotateCenterXYZ != null) {
			XulPropParser.xulParsedProp_FloatArray parsedVal = rotateCenterXYZ.getParsedValue();
			switch (parsedVal.getLength()) {
			case 1:
				_rotateCenterX = _rotateCenterY = parsedVal.tryGetVal(0, _rotateCenterX);
			case 2:
				_rotateCenterX = parsedVal.tryGetVal(0, _rotateCenterX);
				_rotateCenterY = parsedVal.tryGetVal(1, _rotateCenterY);
				break;
			case 3:
				_rotateCenterX = parsedVal.tryGetVal(0, _rotateCenterX);
				_rotateCenterY = parsedVal.tryGetVal(1, _rotateCenterY);
				_rotateCenterZ = parsedVal.tryGetVal(2, _rotateCenterZ) * xScalar;
				break;
			}
		}

		XulStyle rotateCenterX = _area.getStyle(XulPropNameCache.TagId.STYLE_ROTATE_CENTER_X);
		XulStyle rotateCenterY = _area.getStyle(XulPropNameCache.TagId.STYLE_ROTATE_CENTER_Y);
		XulStyle rotateCenterZ = _area.getStyle(XulPropNameCache.TagId.STYLE_ROTATE_CENTER_Z);
		if (rotateCenterX != null) {
			_rotateCenterX = ((XulPropParser.xulParsedProp_Float) rotateCenterX.getParsedValue()).tryGetVal(_rotateCenterX);
		}
		if (rotateCenterY != null) {
			_rotateCenterY = ((XulPropParser.xulParsedProp_Float) rotateCenterY.getParsedValue()).tryGetVal(_rotateCenterY);
		}
		if (rotateCenterZ != null) {
			_rotateCenterZ = ((XulPropParser.xulParsedProp_Float) rotateCenterZ.getParsedValue()).tryGetVal(_rotateCenterZ) * xScalar;
		}

		XulStyle roundRectStyle = _area.getStyle(XulPropNameCache.TagId.STYLE_ROUND_RECT);
		if (roundRectStyle != null) {
			_noConner = false;
			if (_conner == null) {
				_conner = new float[8];
			}
			XulPropParser.xulParsedProp_FloatArray parsedVal = roundRectStyle.getParsedValue();
			switch (parsedVal.getLength()) {
			case 1:
				_conner[0] =
					_conner[1] =
						_conner[2] =
							_conner[3] =
								_conner[4] =
									_conner[5] =
										_conner[6] =
											_conner[7] =
												parsedVal.tryGetVal(0, 0f) * xScalar;
				break;
			case 2:
				_conner[0] =
					_conner[2] =
						_conner[4] =
							_conner[6] =
								parsedVal.tryGetVal(0, 0f) * xScalar;

				_conner[1] =
					_conner[3] =
						_conner[5] =
							_conner[7] =
								parsedVal.tryGetVal(1, 0f) * yScalar;
				break;
			case 4:
				_conner[0] = parsedVal.tryGetVal(0, 0f) * xScalar;
				_conner[1] = parsedVal.tryGetVal(0, 0f) * yScalar;
				_conner[2] = parsedVal.tryGetVal(1, 0f) * xScalar;
				_conner[3] = parsedVal.tryGetVal(1, 0f) * yScalar;
				_conner[4] = parsedVal.tryGetVal(2, 0f) * xScalar;
				_conner[5] = parsedVal.tryGetVal(2, 0f) * yScalar;
				_conner[6] = parsedVal.tryGetVal(3, 0f) * xScalar;
				_conner[7] = parsedVal.tryGetVal(3, 0f) * yScalar;
				break;
			case 8:
				_conner[0] = parsedVal.tryGetVal(0, 0f) * xScalar;
				_conner[1] = parsedVal.tryGetVal(1, 0f) * yScalar;
				_conner[2] = parsedVal.tryGetVal(2, 0f) * xScalar;
				_conner[3] = parsedVal.tryGetVal(3, 0f) * yScalar;
				_conner[4] = parsedVal.tryGetVal(4, 0f) * xScalar;
				_conner[5] = parsedVal.tryGetVal(5, 0f) * yScalar;
				_conner[6] = parsedVal.tryGetVal(6, 0f) * xScalar;
				_conner[7] = parsedVal.tryGetVal(7, 0f) * yScalar;
				break;
			default:
				_noConner = true;
				break;
			}
		} else {
			_noConner = true;
			if (_conner != null) {
				_conner[0] =
					_conner[1] =
						_conner[2] =
							_conner[3] =
								_conner[4] =
									_conner[5] =
										_conner[6] =
											_conner[7] = 0;
			}
		}

		XulStyle lightingColorFilterStyle = _area.getStyle(XulPropNameCache.TagId.STYLE_LIGHTING_COLOR_FILTER);
		_lightingMulColor = Color.WHITE;
		_lightingAddColor = Color.BLACK;
		if (lightingColorFilterStyle != null) {
			XulPropParser.xulParsedProp_HexArray parsedVal = lightingColorFilterStyle.getParsedValue();
			switch (parsedVal.getLength()) {
			case 1:
				_lightingMulColor = (int) parsedVal.tryGetVal(0, _lightingMulColor);
				break;
			case 2:
				_lightingMulColor = (int) parsedVal.tryGetVal(0, _lightingMulColor);
				_lightingAddColor = (int) parsedVal.tryGetVal(1, _lightingAddColor);
				break;
			}
		}

		if (duration > 0 && !_isBooting()) {
			_ani.storeDest();
			if (!_ani.identicalValues()) {
				_ani.restoreValues();
				this.addAnimation(_ani);
			} else {
				terminateAnimation();
			}
		}
	}

	@Override
	public void onVisibilityChanged(boolean isVisible, XulView eventSource) {
		if (!isVisible || !_isVisible()) {
			terminateAnimation();
		}
		super.onVisibilityChanged(isVisible, eventSource);
	}

	private void terminateAnimation() {
		this.removeAnimation(_ani);
		_ani.stopAnimation();
	}

	@Override
	public void destroy() {
		terminateAnimation();
		super.destroy();
	}

	private static final ITransformer _LINEAR_TRANSFORMER = TransformerFactory.createTransformer(TransformerFactory.ALGORITHM_LINEAR, null);

	class LayerTransformAnimation implements IXulAnimation {
		public static final int CONNER_ITEM_OFFSET = 16;
		float[] _srcVals = new float[24];
		int[] _srcColors = new int[4];

		float[] _destVals = new float[24];
		int[] _destColors = new int[4];

		float[] _curVals = new float[24];
		int[] _curColors = new int[4];

		boolean _running = false;
		long _duration;
		long _begin;
		long _progress;

		private ITransformer _transformer = _LINEAR_TRANSFORMER;

		@Override
		public boolean updateAnimation(long timestamp) {
			if (!_running) {
				return false;
			}
			boolean noChange = true;
			for (int i = 0; i < _srcVals.length && noChange; i++) {
				noChange = Math.abs(_srcVals[i] - _destVals[i]) < 0.001f && noChange;
			}
			for (int i = 0; i < _srcColors.length && noChange; i++) {
				noChange = Math.abs(_srcColors[i] - _destColors[i]) < 0.001f && noChange;
			}
			if (noChange) {
				_running = false;
				onAnimationFinished(false);
				return false;
			}

			boolean end = updateValues(timestamp);
			_running = !end;
			markDirtyView();
			if (end && _noConner) {
				_conner = null;
				_clipShape = null;
			}
			if (end) {
				onAnimationFinished(true);
			}
			return !end;
		}

		void restoreValues() {
			updateValues(_begin);
		}

		boolean updateValues(long timestamp) {
			long t = timestamp - _begin;
			if (t < 0) {
				t = 0;
			}
			if (t > _duration) {
				t = _duration;
			}
			_progress = t;
			float percent = _transformer.transform(t, _duration, 0, 0);
			for (int i = 0; i < _srcVals.length; i++) {
				float srcVal = _srcVals[i];
				float destVal = _destVals[i];
				if (srcVal == destVal) {
					_curVals[i] = srcVal;
					continue;
				}
				_curVals[i] = srcVal + (destVal - srcVal) * percent;
			}

			for (int i = 0; i < _srcColors.length; i++) {
				int srcColor = _srcColors[i];
				int destColor = _destColors[i];
				if (srcColor == destColor) {
					_curColors[i] = srcColor;
					continue;
				}
				int a = (int) (Color.alpha(srcColor) + (Color.alpha(destColor) - Color.alpha(srcColor)) * percent);
				int r = (int) (Color.red(srcColor) + (Color.red(destColor) - Color.red(srcColor)) * percent);
				int g = (int) (Color.green(srcColor) + (Color.green(destColor) - Color.green(srcColor)) * percent);
				int b = (int) (Color.blue(srcColor) + (Color.blue(destColor) - Color.blue(srcColor)) * percent);
				_curColors[i] = Color.argb(a, r, g, b);
			}

			_offsetX = _curVals[0];
			_offsetY = _curVals[1];
			// _scalarXAlign = _curVals[2];
			// _scalarYAlign = _curVals[3];
			// scalarX = _curVals[4];
			// scalarY = _curVals[5];

			boolean rotateChanged = false;
			if (_rotateCenterX != _curVals[6]) {
				rotateChanged = true;
				_rotateCenterX = _curVals[6];
			}
			if (_rotateCenterY != _curVals[7]) {
				rotateChanged = true;
				_rotateCenterY = _curVals[7];
			}
			if (_rotateCenterZ != _curVals[8]) {
				rotateChanged = true;
				_rotateCenterZ = _curVals[8];
			}
			if (_rotateX != _curVals[9]) {
				rotateChanged = true;
				_rotateX = _curVals[9];

			}
			if (_rotateY != _curVals[10]) {
				rotateChanged = true;
				_rotateY = _curVals[10];

			}
			if (_rotateZ != _curVals[11]) {
				rotateChanged = true;
				_rotateZ = _curVals[11];
			}

			if (rotateChanged && _camera != null) {
				_camera.restore();
				_camera.save();
				_camera.rotate(_rotateX, _rotateY, _rotateZ);
			}
			_opacity = _curVals[12];

			if (_conner != null) {
				boolean connerChanged = false;
				for (int i = 0; i < _conner.length; i++) {
					float curVal = _curVals[CONNER_ITEM_OFFSET + i];
					if (_conner[i] != curVal) {
						connerChanged = true;
					}
					_conner[i] = curVal;
				}
				if (connerChanged) {
					_clipShape = null;
				}
			}

			if (_lightingMulColor != _curColors[0] || _lightingAddColor != _curColors[1]) {
				_lightingMulColor = _curColors[0];
				_lightingAddColor = _curColors[1];
				_lightingColorFilter = null;
			}
			_borderColor = _curColors[2];
			return t >= _duration;
		}

		void startAnimation(long duration) {
			if (!_running) {
				setDuration(duration);
				storeSrc();
				_running = true;
			} else {
				long beginDelta = (long) (duration * (1.0 - (double) _progress / _duration));
				if (beginDelta < duration) {
					setDuration(duration - beginDelta);
				} else {
					setDuration(1);
				}
				storeSrc();
			}
		}

		void stopAnimation() {
			if (_running) {
				_running = false;
				onAnimationFinished(true);
			}
		}

		void setDuration(long duration) {
			_duration = duration;
			_begin = animationTimestamp();
		}

		void storeSrc() {
			_srcVals[0] = _offsetX;
			_srcVals[1] = _offsetY;
			_srcVals[2] = _scalarXAlign;
			_srcVals[3] = _scalarYAlign;
			_srcVals[4] = _scalarX;
			_srcVals[5] = _scalarY;
			_srcVals[6] = _rotateCenterX;
			_srcVals[7] = _rotateCenterY;
			_srcVals[8] = _rotateCenterZ;
			_srcVals[9] = _rotateX;
			_srcVals[10] = _rotateY;
			_srcVals[11] = _rotateZ;
			_srcVals[12] = _opacity;
			if (_conner != null) {
				System.arraycopy(_conner, 0, _srcVals, CONNER_ITEM_OFFSET, _conner.length);
			} else {
				Arrays.fill(_srcVals, CONNER_ITEM_OFFSET, _srcVals.length, 0.0f);
			}
			_srcColors[0] = _lightingMulColor;
			_srcColors[1] = _lightingAddColor;
			_srcColors[2] = _borderColor;
		}

		void storeDest() {
			_destVals[0] = _offsetX;
			_destVals[1] = _offsetY;
			_destVals[2] = _scalarXAlign;
			_destVals[3] = _scalarYAlign;
			_destVals[4] = _scalarX;
			_destVals[5] = _scalarY;
			_destVals[6] = _rotateCenterX;
			_destVals[7] = _rotateCenterY;
			_destVals[8] = _rotateCenterZ;
			_destVals[9] = _rotateX;
			_destVals[10] = _rotateY;
			_destVals[11] = _rotateZ;
			_destVals[12] = _opacity;
			if (_conner != null) {
				System.arraycopy(_conner, 0, _destVals, CONNER_ITEM_OFFSET, _conner.length);
			} else {
				Arrays.fill(_destVals, CONNER_ITEM_OFFSET, _destVals.length, 0.0f);
			}
			_destColors[0] = _lightingMulColor;
			_destColors[1] = _lightingAddColor;
			_destColors[2] = _borderColor;

		}

		public void setTransformer(ITransformer transformer) {
			_transformer = transformer;
		}

		public boolean identicalValues() {
			for (int i = 0, srcValsLength = _srcVals.length; i < srcValsLength; i++) {
				if (_destVals[i] != _srcVals[i]) {
					return false;
				}
			}
			for (int i = 0, srcColorsLength = _srcColors.length; i < srcColorsLength; i++) {
				if (_destColors[i] != _srcColors[i]) {
					return false;
				}
			}
			return true;
		}
	}

	LayerTransformAnimation _ani = new LayerTransformAnimation();

	Camera _camera;
	Matrix _transMatrix;
	Paint _filterPaint;
	Paint _clipPaint;
	LightingColorFilter _lightingColorFilter;
	RoundRectShape _clipShape;

	float _offsetX = 0.0f;
	float _offsetY = 0.0f;

	float _rotateCenterX = 0.5f;
	float _rotateCenterY = 0.5f;
	float _rotateCenterZ = 0f;
	float _rotateX = 0f;
	float _rotateY = 0f;
	float _rotateZ = 0f;

	int _lightingMulColor = Color.WHITE;
	int _lightingAddColor = Color.BLACK;

	boolean _noConner = false;
	float[] _conner = null; // float[8]

	float _opacity = 1.0f;

	boolean _hasMaskLayer = false;

	boolean _contentCache = false;
	boolean _refreshCache = false;
	static ArrayList<Canvas> _cacheCanvas = new ArrayList<Canvas>();
	static int _cacheCanvasLevel = 0;

	static Canvas allocCacheCanvas(Bitmap bmp) {
		if (_cacheCanvasLevel >= _cacheCanvas.size()) {
			_cacheCanvas.add(new Canvas(bmp));
		}
		Canvas canvas = _cacheCanvas.get(_cacheCanvasLevel++);
		canvas.setBitmap(bmp);
		return canvas;
	}

	static void freeCacheCanvas(Canvas canvas) {
		if (canvas != _cacheCanvas.get(--_cacheCanvasLevel)) {
			new Exception("corrupted cache canvas stack").printStackTrace();
		}
		canvas.setBitmap(null);
	}

	static Canvas _cacheBmpCanvas = new Canvas();
	Bitmap _cachedBitmap;

	@Override
	public void cleanImageItems() {
		super.cleanImageItems();
		if (_cachedBitmap != null) {
			_cachedBitmap = null;
		}
	}

	static class XulLayerChildrenRender extends XulAreaChildrenRender {
		boolean drawMask = false;
		boolean hasMask = false;

		@Override
		public void init(XulDC dc, Rect rect, int xBase, int yBase) {
			hasMask = false;
			drawMask = false;
			super.init(dc, rect, xBase, yBase);
		}

		@Override
		public boolean onXulView(int pos, XulView view) {
			boolean isMask = view.getId().equals(LAYER_MASK_ID);
			hasMask = hasMask || isMask;
			if (drawMask != isMask) {
				return true;
			}
			return super.onXulView(pos, view);
		}
	}

	protected XulAreaChildrenRender createChildrenRender() {
		return new XulLayerChildrenRender();
	}

	public float getDrawingOffsetX() {
		return _offsetX;
	}

	public float getDrawingOffsetY() {
		return _offsetY;
	}

	@Override
	public void onDirtyChild(XulView view) {
		super.onDirtyChild(view);
		_refreshCache = true;
	}

	@Override
	public boolean hitTest(int event, float x, float y) {
		if (_isInvisible()) {
			return false;
		}
		if (event != XulManager.HIT_EVENT_DOWN
			&& event != XulManager.HIT_EVENT_UP
			&& event != XulManager.HIT_EVENT_DUMMY
			) {
			return false;
		}
		x -= _offsetX;
		y -= _offsetY;

		RectF focusRect = this.getFocusRect();
		if (Math.abs(_scalarX - 1.0f) > 0.001f || Math.abs(_scalarY - 1.0f) > 0.001f) {
			float scalarX = _scalarX;
			float scalarY = _scalarY;
			float dx = XulUtils.calRectWidth(focusRect) * _scalarXAlign;
			float dy = XulUtils.calRectHeight(focusRect) * _scalarYAlign;

			x -= dx + focusRect.left;
			y -= dy + focusRect.top;
			x /= scalarX;
			y /= scalarY;
			x += dx + focusRect.left;
			y += dy + focusRect.top;
		}
		return focusRect.contains((int) x, (int) y);
	}

	@Override
	public boolean hitTestTranslate(PointF pt) {
		pt.x -= _offsetX;
		pt.y -= _offsetY;

		RectF focusRect = this.getFocusRect();
		if (Math.abs(_scalarX - 1.0f) > 0.001f || Math.abs(_scalarY - 1.0f) > 0.001f) {
			float scalarX = _scalarX;
			float scalarY = _scalarY;
			float dx = XulUtils.calRectWidth(focusRect) * _scalarXAlign;
			float dy = XulUtils.calRectHeight(focusRect) * _scalarYAlign;

			pt.x -= dx + focusRect.left;
			pt.y -= dy + focusRect.top;
			pt.x /= scalarX;
			pt.y /= scalarY;
			pt.x += dx + focusRect.left;
			pt.y += dy + focusRect.top;
		}
		return true;
	}

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible() || _opacity < 0.01f) {
			return;
		}

		boolean requireLightingColorFilter = (_lightingMulColor != Color.WHITE || _lightingAddColor != Color.BLACK);

		boolean requireOpacityLayer = (_opacity < 1.0f);

		boolean requireConnerClip = (_conner != null);

		boolean requireRotate = (Math.abs(_rotateX) > 0.1f || Math.abs(_rotateY) > 0.1f ||
			Math.abs(_rotateZ) > 0.1f);

		boolean requireMaskClip = _hasMaskLayer;

		if (requireConnerClip && _clipShape == null) {
			_clipShape = new RoundRectShape(_conner, null, null);
		}

		if (requireLightingColorFilter) {
			if (_lightingColorFilter == null) {
				_lightingColorFilter = new LightingColorFilter(_lightingMulColor, _lightingAddColor);
			}
		} else {
			_lightingColorFilter = null;
		}

		if (requireOpacityLayer || requireLightingColorFilter || requireConnerClip || requireMaskClip) {
			if (_filterPaint == null) {
				_filterPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.HINTING_ON);
			}
		} else {
			_filterPaint = null;
		}

		if (requireOpacityLayer) {
			_filterPaint.setAlpha((int) (_opacity * 255));
		}

		if (_camera == null && requireRotate) {
			_camera = new Camera();
			_transMatrix = new Matrix();
			_camera.save();
			_camera.rotate(_rotateX, _rotateY, _rotateZ);
		}

		RectF focusRect = this.getFocusRect();
		RectF animRect = this.getUpdateRect();
		XulUtils.offsetRect(animRect, xBase, yBase);

		Canvas canvas = dc.getCanvas();

		XulUtils.saveCanvas(canvas, Canvas.MATRIX_SAVE_FLAG);
		if (Math.abs(_offsetX) > 0.2f || Math.abs(_offsetY) > 0.2f) {
			canvas.translate(_offsetX, _offsetY);
		}

		float xStart = focusRect.left + xBase;
		float yStart = focusRect.top + yBase;
		float focusRectHeight = focusRect.bottom - focusRect.top;
		float focusRectWidth = focusRect.right - focusRect.left;

		if (_camera != null) {
			_camera.getMatrix(_transMatrix);
			float offsetX = focusRectWidth * (0.5f - _scalarXAlign) * (_scalarX - 1f);
			float offsetY = focusRectHeight * (0.5f - _scalarYAlign) * (_scalarY - 1f);
			float dx = xStart + focusRectWidth * _rotateCenterX + offsetX;
			float dy = yStart + focusRectHeight * _rotateCenterY + offsetY;
			float dz = _rotateCenterZ;
			_transMatrix.preTranslate(-dx, -dy);
			_transMatrix.postTranslate(dx, dy);

			canvas.concat(_transMatrix);
		}

		if (_filterPaint != null) {
			if (_lightingColorFilter != null) {
				_filterPaint.setColorFilter(_lightingColorFilter);
			}

			RectF frc = XulDC._tmpFRc1;
			frc.set(animRect);
			frc.left -= _borderSize * _scalarX;
			frc.right += _borderSize * _scalarX;
			frc.top -= _borderSize * _scalarY;
			frc.bottom += _borderSize * _scalarY;
			frc.intersect(rect.left, rect.top, rect.right, rect.bottom);
			XulUtils.saveCanvasLayer(canvas, frc, _filterPaint, Canvas.CLIP_TO_LAYER_SAVE_FLAG | Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.CLIP_SAVE_FLAG);
		}

		if (Math.abs(_scalarX - 1.0f) > 0.001f || Math.abs(_scalarY - 1.0f) > 0.001f) {
			float scalarX = _scalarX;
			float scalarY = _scalarY;
			float dx = focusRectWidth * _scalarXAlign;
			float dy = focusRectHeight * _scalarYAlign;
			canvas.scale(scalarX, scalarY, xStart + dx, yStart + dy);
		}

		boolean renderCache = false;
		float xOffset = focusRect.left + xBase;
		float yOffset = focusRect.top + yBase;
		if (_contentCache && _dirtyTimestamp + 16 <= getRenderContext().animationTimestamp()) {

			if (_cachedBitmap != null &&
				(_cachedBitmap.getWidth() != focusRectWidth
					|| _cachedBitmap.getHeight() != focusRectHeight)) {
				BitmapTools.recycleBitmap(_cachedBitmap);
				_cachedBitmap = null;
			}
			if (_cachedBitmap == null) {
				_refreshCache = true;
			}
			if (_refreshCache && dc.isRenderTimeout()) {
				// timeout do not create drawing-cache
				renderCache = false;
			} else {
				renderCache = true;
			}
		}
		if (renderCache && focusRectHeight > 0 && focusRectWidth > 0) {
			if (_cachedBitmap == null) {
				_cachedBitmap = createBitmapCache(XulUtils.roundToInt(focusRectWidth), XulUtils.roundToInt(focusRectHeight));
				_refreshCache = true;
			}
			if (_refreshCache) {
				_refreshCache = false;
				Canvas cacheCanvas = allocCacheCanvas(_cachedBitmap);
				Canvas oldCanvas = dc.setCanvas(cacheCanvas);
				XulUtils.saveCanvas(cacheCanvas);
				dc.translate(-xOffset, -yOffset);
				_renderContent(dc, rect, xBase, yBase);
				XulLayerChildrenRender r = (XulLayerChildrenRender) _childrenRender;

				if (_clipPaint == null && (r.hasMask || _clipShape != null)) {
					_clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.HINTING_ON);
					_clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
				}

				if (r.hasMask) {
					XulUtils.saveCanvasLayer(cacheCanvas, null, _clipPaint, Canvas.ALL_SAVE_FLAG);
					r.drawMask = true;
					_area.eachView(_childrenRender);
					dc.doPostDraw(Math.max(getZIndex() - 1, 0), _area);
					XulUtils.restoreCanvas(cacheCanvas);
				}

				XulUtils.restoreCanvas(cacheCanvas);
				freeCacheCanvas(cacheCanvas);
				dc.setCanvas(oldCanvas);
			}
			dc.drawBitmap(_cachedBitmap, xOffset, yOffset, XulRenderContext.getDefPicPaint());
		} else {
			_renderContent(dc, rect, xBase, yBase);
			XulLayerChildrenRender r = (XulLayerChildrenRender) _childrenRender;

			if (_clipPaint == null && (r.hasMask || _clipShape != null)) {
				_clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.HINTING_ON);
				_clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
			}

			if (r.hasMask) {
				RectF frc = XulDC._tmpFRc1;
				frc.set(animRect);
				frc.left -= _borderSize * _scalarX;
				frc.right += _borderSize * _scalarX;
				frc.top -= _borderSize * _scalarY;
				frc.bottom += _borderSize * _scalarY;
				frc.intersect(rect.left, rect.top, rect.right, rect.bottom);
				XulUtils.saveCanvasLayer(canvas, frc, _clipPaint, Canvas.ALL_SAVE_FLAG);
				r.drawMask = true;
				_area.eachView(_childrenRender);
				dc.doPostDraw(Math.max(getZIndex() - 1, 0), _area);
				XulUtils.restoreCanvas(canvas);
			}
		}

		if (_clipShape != null) {
			canvas.translate(xStart, yStart);
			_clipShape.resize(focusRectWidth, focusRectHeight);
			_clipShape.draw(canvas, _clipPaint);
		}

		drawBorder(dc, rect, xBase, yBase);

		if (_filterPaint != null) {
			XulUtils.restoreCanvas(canvas);
		}

		XulUtils.restoreCanvas(canvas);
	}

	private void _renderContent(XulDC dc, Rect rect, int xBase, int yBase) {
		drawBackgroundNoScale(dc, rect, xBase, yBase);
		_childrenRender.init(dc, rect, xBase, yBase);
		_area.eachView(_childrenRender);
		dc.doPostDraw(Math.max(getZIndex() - 1, 0), _area);
	}

	@Override
	public void drawBorder(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		Paint defStrokePaint = _ctx.getDefStrokePaint();
		Rect targetRc = getDrawingRect();
		if (_borderSize > 0.1f && (_borderColor & 0xFF000000) != 0) {
			float borderSize = _borderSize * _scalarX;
			defStrokePaint.setStrokeWidth(borderSize);
			defStrokePaint.setColor(_borderColor);
			if (_borderEffect != null) {
				defStrokePaint.setPathEffect(_borderEffect);
			}
			if (_clipShape != null) {
				RectF focusRect = this.getFocusRect();
				Canvas canvas = dc.getCanvas();
				_clipShape.resize(XulUtils.calRectWidth(focusRect), XulUtils.calRectHeight(focusRect));
				_clipShape.draw(canvas, defStrokePaint);
			} else if (_borderRoundX > 0.5 && _borderRoundY > 0.5) {
				float borderDelta = (borderSize / 2) - borderSize * _borderPos;
				float x = targetRc.left + borderDelta + _screenX + xBase;
				float y = targetRc.top + borderDelta + _screenY + yBase;
				float cx = XulUtils.calRectWidth(targetRc) - 2 * borderDelta;
				float cy = XulUtils.calRectHeight(targetRc) - 2 * borderDelta;
				dc.drawRoundRect(x, y, cx, cy, _borderRoundX * _scalarX, _borderRoundY * _scalarY, defStrokePaint);
			} else {
				int xStart = _screenX + xBase;
				int yStart = _screenY + yBase;
				dc.drawRect(targetRc.left + xStart, targetRc.top + yStart, targetRc.right + xStart, targetRc.bottom + yStart, defStrokePaint);
			}
			if (_borderEffect != null) {
				defStrokePaint.setPathEffect(null);
			}
		}
	}
}
