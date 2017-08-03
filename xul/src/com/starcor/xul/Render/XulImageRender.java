package com.starcor.xul.Render;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;

import com.starcor.xul.Graphics.*;
import com.starcor.xul.Prop.*;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.Drawer.XulAnimationDrawer;
import com.starcor.xul.Render.Drawer.XulDrawer;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.Utils.XulRenderDrawableItem;
import com.starcor.xul.Utils.XulSimpleArray;
import com.starcor.xul.*;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/13.
 */
public class XulImageRender extends XulLabelRender {
	private static final String TAG = XulImageRender.class.getSimpleName();

	public static void register() {
		XulRenderFactory.registerBuilder("item", "image", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulItem;
				return new XulImageRender(ctx, (XulItem) view);
			}
		});
	}

	protected XulItem _item;

	static abstract class ImageEffectDrawer extends XulDrawer implements IXulAnimation {
	}

	static class FadeEffectDrawer extends ImageEffectDrawer {
		int _step;
		int _from;
		int _to;
		XulDrawable _drawable;
		XulDrawer _drawer;
		XulViewRender _render;
		long _begin;
		long _duration = 300;

		public FadeEffectDrawer(XulViewRender render, XulDrawer drawer, XulDrawable drawable, int from, int to, int duration) {
			_render = render;
			_drawer = drawer;
			_drawable = drawable;
			_step = _from = from;
			_to = to;
			_begin = render.animationTimestamp();
			_duration = duration;
		}

		@Override
		public void draw(XulDC dc, XulDrawable drawable, Rect src, Rect dst, Paint paint) {
			paint.setAlpha(_step);
			_drawer.draw(dc, _drawable, src, dst, paint);
		}

		@Override
		public void draw(XulDC dc, XulDrawable drawable, Rect src, RectF dst, Paint paint) {
			paint.setAlpha(_step);
			_drawer.draw(dc, _drawable, src, dst, paint);
		}

		@Override
		public void draw(XulDC dc, XulDrawable drawable, Rect dst, Paint paint) {
			paint.setAlpha(_step);
			_drawer.draw(dc, _drawable, dst, paint);
		}

		@Override
		public void draw(XulDC dc, XulDrawable drawable, RectF dst, Paint paint) {
			paint.setAlpha(_step);
			_drawer.draw(dc, _drawable, dst, paint);
		}

		@Override
		public boolean updateAnimation(long timestamp) {
			long t = timestamp - _begin;
			boolean isEnd = false;
			if (t > _duration) {
				t = _duration;
				isEnd = true;
			}
			float percent = ((float) t) / _duration;
			_step = (int) ((_to - _from) * percent + _from);
			_render.markDirtyView();
			return !isEnd;
		}
	}

	static abstract class OpacityTransformDrawer extends ImageEffectDrawer {
		XulDrawable _drawable;
		XulDrawer _drawer;
		XulViewRender _render;

		long _begin;
		long _duration;
		float _from;
		float _to;
		float _val;

		public OpacityTransformDrawer(XulViewRender render, XulDrawer drawer, XulDrawable drawable, float fromOpacity, float toOpacity, int duration) {
			_render = render;
			_drawer = drawer;
			_drawable = drawable;
			_val = _from = fromOpacity;
			_to = toOpacity;
			_begin = _render.animationTimestamp();
			_duration = duration;
		}

		@Override
		public void draw(XulDC dc, XulDrawable drawable, Rect src, Rect dst, Paint paint) {
			paint.setAlpha((int) (_val * 255));
			_drawer.draw(dc, _drawable, src, dst, paint);
		}

		@Override
		public void draw(XulDC dc, XulDrawable drawable, Rect dst, Paint paint) {
			paint.setAlpha((int) (_val * 255));
			_drawer.draw(dc, _drawable, dst, paint);
		}

		@Override
		public void draw(XulDC dc, XulDrawable drawable, Rect src, RectF dst, Paint paint) {
			paint.setAlpha((int) (_val * 255));
			_drawer.draw(dc, _drawable, src, dst, paint);
		}

		@Override
		public void draw(XulDC dc, XulDrawable drawable, RectF dst, Paint paint) {
			paint.setAlpha((int) (_val * 255));
			_drawer.draw(dc, _drawable, dst, paint);
		}

		@Override
		public boolean updateAnimation(long timestamp) {
			long t = timestamp - _begin;
			boolean isEnd = false;
			if (t > _duration) {
				t = _duration;
				isEnd = true;
			}
			float percent = ((float) t) / _duration;
			_val = ((_to - _from) * percent + _from);

			updateOpacity(_val);
			_render.markDirtyView();
			if (isEnd) {
				endUp();
			}
			return !isEnd;
		}

		public void updateTargetOpacity(float targetOpacity) {
			float percent = (_val - _from) / (_to - _from);
			_from = _val;
			_to = targetOpacity;
			_begin = _render.animationTimestamp();
			_begin -= (1.0f - percent) * _duration;
		}

		abstract void endUp();

		abstract void updateOpacity(float opacity);
	}


	protected class DrawableInfo extends XulRenderDrawableItem {
		int _idx;
		String _name;
		String _url;
		int _width = 0;
		int _height = 0;
		volatile boolean _isLoading = false;
		volatile long _lastLoadFailedTime = 0;
		volatile int _loadFailedCounter = 0;
		volatile boolean _isRecycled = false;
		XulDrawable _bmp = null;
		float _opacity = 1.0f;
		float _targetOpacity = 1.0f;
		OpacityTransformDrawer _visibleFadeEffectDrawer = null;

		boolean _isStretch = true;
		boolean _autoHide = false;
		int _autoHideTarget = -1;
		float _alignX = 0.5f;
		float _alignY = 0.5f;
		float[] _roundRadius;
		float _shadowSize = 0;
		float _shadowX = 0;
		float _shadowY = 0;
		int _shadowColor = 0xFF000000;
		boolean _skipDraw = false;
		XulDrawer _drawer;
		int _paddingLeft = 0;
		int _paddingRight = 0;
		int _paddingTop = 0;
		int _paddingBottom = 0;

		long _loadBeginTime = 0;
		long _loadEndTime = 0;

		ArrayList<ImageEffectDrawer> _fadeEffect;
		XulDrawable _fadeInBkg = null;
		int _fadeInDuration = 300;
		boolean _reuse = false;
		Bitmap.Config _pixFmt = XulManager.DEF_PIXEL_FMT;

		public int getIdx() {
			return _idx;
		}

		@Override
		public void onImageReady(XulDrawable bmp) {
			_isLoading = false;
			_loadEndTime = XulUtils.timestamp();
			if (_isRecycled || _url == null) {
				return;
			}
			if (bmp == null) {
				_lastLoadFailedTime = XulUtils.timestamp();
				_loadFailedCounter++;
				notifyLoadEvent(false);
			} else if (_bmp != bmp) {
				setBitmap(bmp);
				_lastLoadFailedTime = 0;
				_loadFailedCounter = 0;
				syncLayoutOnImageChanged(this);
				notifyLoadEvent(true);
			}
			markDirtyView();
		}

		private FadeEffectDrawer createFadeEffect(XulDrawer drawer, XulDrawable bmp, int from, int to) {
			return new FadeEffectDrawer(XulImageRender.this, drawer, bmp, from, to, _fadeInDuration) {
				@Override
				public boolean updateAnimation(long timestamp) {
					if (_fadeEffect == null || _fadeEffect.isEmpty()) {
						return false;
					}
					boolean ret = super.updateAnimation(timestamp);
					if (!ret) {
						_fadeEffect.remove(this);
					}
					return ret;
				}
			};
		}

		void notifyLoadEvent(boolean success) {
			int duration = (int) (_loadEndTime - _loadBeginTime);
			String eventName;
			if (!success) {
				eventName = "loadImageFailed";
			} else {
				eventName = "loadImageSuccess";
			}
			XulAction action = _view.getAction(eventName);

			if (action != null) {
				XulPage.invokeActionNoPopupWithArgs(_view, eventName, _idx, duration);
			}
		}

		private void setBitmap(XulDrawable bmp) {
			if (_bmp == bmp) {
				return;
			}
			if (_fadeEffect != null) {
				if (_drawer != null && _fadeInBkg != null) {
					FadeEffectDrawer effect = createFadeEffect(_drawer, _fadeInBkg, 255, 255);
					_fadeEffect.add(effect);
					addAnimation(effect);
				}
				_bmp = bmp;
				_fadeInBkg = bmp;
				_drawer = XulDrawer.create(_bmp, _item, _ctx);
				FadeEffectDrawer effect = createFadeEffect(_drawer, _bmp, 0, 255);
				_fadeEffect.add(effect);
				addAnimation(effect);
			} else {
				_bmp = bmp;
				_fadeInBkg = null;
				_drawer = XulDrawer.create(_bmp, _item, _ctx);
			}
		}

		public XulDrawable prepareBitmap() {
			XulDrawable bmp = this._bmp;
			if (TextUtils.isEmpty(this._url)) {
				return null;
			}
			if (!_reuse && (bmp == null || bmp.isRecycled())) {
				bmp = XulWorker.loadDrawableFromCache(this._url);
				if (bmp == null) {
					_imageChanged = true;
					if (_fadeInBkg != null) {
						return _fadeInBkg;
					}
					return null;
				}
				setBitmap(bmp);
				if (bmp.isRecycled()) {
					// 强制重新加载回收的图片
					_imageChanged = true;
				} else {
					syncLayoutOnImageChanged(this);
					if (this._isLoading) {
						this._loadEndTime = XulUtils.timestamp();
					}
					this.notifyLoadEvent(true);
				}
			}
			return bmp;
		}

		public void setVisible(boolean visible) {
			setOpacity(visible ? 1.0f : 0.0f);
		}

		public void setOpacity(float newOpacity) {
			if (_opacity == _targetOpacity && _opacity == newOpacity) {
				return;
			}
			if (_fadeEffect != null && _bmp != null && _drawer != null) {
				_targetOpacity = newOpacity;
				if (_visibleFadeEffectDrawer == null) {
					_visibleFadeEffectDrawer = new OpacityTransformDrawer(XulImageRender.this, _drawer, _bmp, _opacity, _targetOpacity, _fadeInDuration) {
						@Override
						void endUp() {
							_visibleFadeEffectDrawer = null;
							if (_fadeEffect != null) {
								_fadeEffect.remove(this);
							}
							_opacity = _targetOpacity;
						}

						@Override
						void updateOpacity(float opacity) {
							boolean oldVisibility = isVisible();
							_opacity = opacity;
							if (oldVisibility != isVisible()) {
								syncLayoutOnImageChanged(DrawableInfo.this);
							}
						}
					};

					_fadeEffect.add(_visibleFadeEffectDrawer);
				} else {
					_visibleFadeEffectDrawer.updateTargetOpacity(_targetOpacity);
				}
				addAnimation(_visibleFadeEffectDrawer);
			} else {
				boolean oldVisibility = isVisible();
				_opacity = _targetOpacity = newOpacity;
				if (oldVisibility != isVisible()) {
					syncLayoutOnImageChanged(this);
				}
			}
		}

		public float getOpacity() {
			return _opacity;
		}

		public boolean isOpacity() {
			return _opacity >= 1.0f;
		}

		public boolean isLoading() {
			return _isLoading;
		}

		public boolean isImageLoaded() {
			return !_isLoading && _bmp != null;
		}

		public boolean isVisible() {
			return _opacity >= 0.01f;
		}

		public int getHeight() {
			return _height;
		}

		public int getWidth() {
			return _width;
		}

		public int getPaddingLeft() {
			return _paddingLeft;
		}

		public int getPaddingTop() {
			return _paddingTop;
		}

		public int getPaddingRight() {
			return _paddingRight;
		}

		public int getPaddingBottom() {
			return _paddingBottom;
		}

		public void setPadding(int top, int left, int right, int bottom) {
			_paddingTop = top;
			_paddingLeft = left;
			_paddingRight = right;
			_paddingBottom = bottom;
		}

		public void setPadding(int padding) {
			setPadding(padding, padding, padding, padding);
		}

		public void setPadding(int topBottom, int leftRight) {
			setPadding(topBottom, leftRight, leftRight, topBottom);
		}

		public void setPaddingLeft(int padding) {
			_paddingLeft = padding;
		}

		public void setPaddingTop(int padding) {
			_paddingTop = padding;
		}

		public void setPaddingRight(int padding) {
			_paddingRight = padding;
		}

		public void setPaddingBottom(int padding) {
			_paddingBottom = padding;
		}

		public int getImageWidth() {
			return _bmp.getWidth();
		}

		public int getImageHeight() {
			return _bmp.getHeight();
		}

		public void setAlign(float alignX, float alignY) {
			_alignX = alignX;
			_alignY = alignY;
		}

		public void setAlignX(float alignX) {
			_alignX = alignX;
		}

		public void setAlignY(float alignY) {
			_alignY = alignY;
		}

		public float getAlignX() {
			return _alignX;
		}

		public float getAlignY() {
			return _alignY;
		}
	}

	private static class DrawableArray extends XulSimpleArray<DrawableInfo> {
		public DrawableArray(int size) {
			super(size);
			resize(size);
		}

		@Override
		protected DrawableInfo[] allocArrayBuf(int size) {
			return new DrawableInfo[size];
		}
	}

	private boolean _imageChanged = true;
	// 图片变化时，是否重新布局
	private boolean _layoutOnImageChanged = true;
	private DrawableArray _images = new DrawableArray(_MaxImgLayers);
	private boolean _doNotMatchTextWidth = false;
	private boolean _doNotMatchTextHeight = false;
	private int _layerCount = _DefImgLayers;

	public XulImageRender(XulRenderContext ctx, XulItem item) {
		super(ctx, item);
		_item = item;
	}

	static public final int _DefImgLayers = 4;
	static public final int _MaxImgLayers = 8;
	static public final int[] _imgXName = new int[_MaxImgLayers];
	static public final int[] _imgXVisible = new int[_MaxImgLayers];
	static public final int[] _imgXMode = new int[_MaxImgLayers];
	static public final int[] _imgXWidth = new int[_MaxImgLayers];
	static public final int[] _imgXHeight = new int[_MaxImgLayers];
	static public final int[] _imgXShadow = new int[_MaxImgLayers];
	static public final int[] _imgXAlign = new int[_MaxImgLayers];
	static public final int[] _imgXRoundRect = new int[_MaxImgLayers];
	static public final int[] _imgXAutoHide = new int[_MaxImgLayers];
	static public final int[] _imgXPadding = new int[_MaxImgLayers];
	static public final int[] _imgXFadeIn = new int[_MaxImgLayers];
	static public final int[] _imgXReuse = new int[_MaxImgLayers];
	static public final int[] _imgXPixFmt = new int[_MaxImgLayers];

	static {
		for (int i = 0; i < _MaxImgLayers; ++i) {
			_imgXName[i] = XulPropNameCache.name2Id(String.format("img.%d", i));
			_imgXVisible[i] = XulPropNameCache.name2Id(String.format("img.%d.visible", i));
			_imgXMode[i] = XulPropNameCache.name2Id(String.format("img.%d.mode", i));
			_imgXWidth[i] = XulPropNameCache.name2Id(String.format("img.%d.width", i));
			_imgXHeight[i] = XulPropNameCache.name2Id(String.format("img.%d.height", i));
			_imgXRoundRect[i] = XulPropNameCache.name2Id(String.format("img.%d.round-rect", i));
			_imgXShadow[i] = XulPropNameCache.name2Id(String.format("img.%d.shadow", i));
			_imgXAlign[i] = XulPropNameCache.name2Id(String.format("img.%d.align", i));
			_imgXAutoHide[i] = XulPropNameCache.name2Id(String.format("img.%d.auto-hide", i));
			_imgXPadding[i] = XulPropNameCache.name2Id(String.format("img.%d.padding", i));
			_imgXFadeIn[i] = XulPropNameCache.name2Id(String.format("img.%d.fade-in", i));
			_imgXReuse[i] = XulPropNameCache.name2Id(String.format("img.%d.reuse", i));
			_imgXPixFmt[i] = XulPropNameCache.name2Id(String.format("img.%d.pixfmt", i));
		}
	}

	protected void syncLayoutOnImageChanged(DrawableInfo info) {
		if (_layoutOnImageChanged) {
			this.setUpdateLayout();
		}
	}

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		super.syncData();
		double xScalar = _ctx.getXScalar();
		double yScalar = _ctx.getYScalar();

		XulAttr maxLayers = _view.getAttr(XulPropNameCache.TagId.MAX_LAYERS);
		if (maxLayers == null) {
			_layerCount = _DefImgLayers;
		} else {
			XulPropParser.xulParsedProp_Integer parsedVal = maxLayers.getParsedValue();
			_layerCount = parsedVal.val;
			if (_layerCount <= 0) {
				_layerCount = _DefImgLayers;
			} else if (_layerCount > _MaxImgLayers) {
				_layerCount = _MaxImgLayers;
			}
		}

		// 准备图片数据
		for (int i = 0; i < _MaxImgLayers; ++i) {
			XulAttr attrImg = null;
			if (i < _layerCount) {
				attrImg = _view.getAttr(_imgXName[i]);
			}

			DrawableInfo imgInfo = _images.get(i);
			if (attrImg == null) {
				if (imgInfo != null) {
					imgInfo._url = null;
					imgInfo._bmp = null;
					imgInfo._fadeInBkg = null;
					imgInfo._drawer = null;
					if (imgInfo._fadeEffect != null) {
						imgInfo._fadeEffect.clear();
					}
					imgInfo.setVisible(false);
				}
				continue;
			}

			XulAttr attrVisible = _view.getAttr(_imgXVisible[i]);
			XulAttr attrScale = _view.getAttr(_imgXMode[i]);
			XulAttr attrWidth = _view.getAttr(_imgXWidth[i]);
			XulAttr attrHeight = _view.getAttr(_imgXHeight[i]);
			XulAttr attrRoundRect = _view.getAttr(_imgXRoundRect[i]);
			XulAttr attrShadow = _view.getAttr(_imgXShadow[i]);
			XulAttr attrAlign = _view.getAttr(_imgXAlign[i]);
			XulAttr attrAutoHide = _view.getAttr(_imgXAutoHide[i]);
			XulAttr attrPadding = _view.getAttr(_imgXPadding[i]);
			XulAttr attrFadeIn = _view.getAttr(_imgXFadeIn[i]);
			XulAttr attrReuse = _view.getAttr(_imgXReuse[i]);
			XulAttr attrPixFmt = _view.getAttr(_imgXPixFmt[i]);

			if (imgInfo == null) {
				imgInfo = new DrawableInfo();
				imgInfo._idx = i;
				imgInfo._name = attrImg.getName();
			}

			String newUrl = attrImg.getStringValue();
			if (newUrl != imgInfo._url) {
				if (!TextUtils.isEmpty(imgInfo._url) && !imgInfo._url.equals(newUrl)) {
					// 需要重新加载
					XulWorker.removeDrawableCache(imgInfo._url);
					XulDrawable fadeInBkg = imgInfo._fadeInBkg;
					if (imgInfo._isLoading) {
						// 如果正在加载，则废弃当前item
						imgInfo._isRecycled = true;

						// 重新创建
						ArrayList<ImageEffectDrawer> fadeEffect = imgInfo._fadeEffect;
						XulDrawer oldDrawer = imgInfo._drawer;

						imgInfo = new DrawableInfo();
						imgInfo._idx = i;
						imgInfo._name = attrImg.getName();
						imgInfo._url = newUrl;
						imgInfo._fadeEffect = fadeEffect;
						imgInfo._drawer = oldDrawer;
					}
					if (imgInfo._fadeEffect != null) {
						// keep the old fade in bkg
						imgInfo._fadeInBkg = fadeInBkg;
					}
					imgInfo._bmp = null;
					imgInfo._lastLoadFailedTime = 0;
					imgInfo._loadFailedCounter = 0;
					imgInfo._loadEndTime = 0;
					imgInfo._loadBeginTime = 0;
					_imageChanged = true;
				} else if (!TextUtils.isEmpty(newUrl) && !newUrl.equals(imgInfo._url)) {
					_imageChanged = true;
				}
				imgInfo._url = newUrl;
			}

			if (attrAutoHide != null) {
				XulPropParser.xulParsedAttr_Img_AutoHide autoHideVal = attrAutoHide.getParsedValue();
				imgInfo._autoHide = autoHideVal.enabled;
				imgInfo._autoHideTarget = autoHideVal.target;
			}

			if (attrFadeIn == null) {
				imgInfo._fadeEffect = null;
			} else {
				XulPropParser.xulParsedAttr_Img_FadeIn fadeInVal = attrFadeIn.getParsedValue();
				int duration = fadeInVal.duration;
				if (!fadeInVal.enabled) {
					imgInfo._fadeEffect = null;
					duration = 0;
				}
				imgInfo._fadeInDuration = duration;
				if (duration <= 0) {
					imgInfo._fadeEffect = null;
				} else if (imgInfo._fadeEffect == null) {
					imgInfo._fadeEffect = new ArrayList<ImageEffectDrawer>();
				}
			}

			if (attrPixFmt == null) {
				imgInfo._pixFmt = XulManager.DEF_PIXEL_FMT;
			} else {
				XulPropParser.xulParsedAttr_Img_PixFmt pixFmtVal = attrPixFmt.getParsedValue();
				imgInfo._pixFmt = pixFmtVal.fmt;
			}

			String visible = "true";
			if (attrVisible != null) {
				visible = attrVisible.getStringValue();
			}

			String scale = "stretch";
			if (attrScale != null) {
				scale = attrScale.getStringValue();
			}

			if ("stretch".equals(scale)) {
				imgInfo._isStretch = true;
			} else if ("center".equals(scale)) {
				imgInfo._isStretch = false;
			} else {
				imgInfo._isStretch = true;
			}

			if ("false".equals(visible)) {
				imgInfo.setVisible(false);
			} else if ("true".equals(visible) || TextUtils.isEmpty(visible)) {
				imgInfo.setVisible(true);
			} else {
				final float opacity = XulUtils.tryParseFloat(visible, 0.0f);
				imgInfo.setOpacity(opacity);
			}

			if (attrReuse != null && ((XulPropParser.xulParsedProp_booleanValue) attrReuse.getParsedValue()).val) {
				imgInfo._reuse = true;
			} else {
				imgInfo._reuse = false;
			}

			if (attrWidth != null) {
				XulPropParser.xulParsedAttr_Img_SizeVal width = attrWidth.getParsedValue();
				imgInfo._width = width.val <= XulManager.SIZE_MAX ? XulUtils.roundToInt(width.val * xScalar) : width.val;
			} else {
				imgInfo._width = 0;
			}

			if (attrHeight != null) {
				XulPropParser.xulParsedAttr_Img_SizeVal height = attrHeight.getParsedValue();
				imgInfo._height = height.val < XulManager.SIZE_MAX ? XulUtils.roundToInt(height.val * yScalar) : height.val;
			} else {
				imgInfo._height = 0;
			}

			if (attrRoundRect != null) {
				XulPropParser.xulParsedAttr_Img_RoundRect roundRect = attrRoundRect.getParsedValue();
				imgInfo._roundRadius = roundRect.getRoundRadius(xScalar, yScalar);
			} else {
				imgInfo._roundRadius = null;
			}


			if (attrShadow != null) {
				XulPropParser.xulParsedAttr_Img_Shadow shadow = attrShadow.getParsedValue();
				imgInfo._shadowSize = (float) (shadow.size * xScalar);
				imgInfo._shadowX = (float) (shadow.xOffset * xScalar);
				imgInfo._shadowY = (float) (shadow.yOffset * yScalar);
				imgInfo._shadowColor = shadow.color;
			} else {
				imgInfo._shadowSize = 0;
			}

			if (attrAlign != null) {
				XulPropParser.xulParsedAttr_Img_Align align = attrAlign.getParsedValue();
				imgInfo._alignX = align.xAlign;
				imgInfo._alignY = align.yAlign;
			} else {
				imgInfo._alignX = 0.5f;
				imgInfo._alignY = 0.5f;
			}

			if (attrPadding != null) {
				XulPropParser.xulParsedProp_PaddingMargin padding = attrPadding.getParsedValue();
				imgInfo._paddingTop = XulUtils.roundToInt(padding.top * yScalar);
				imgInfo._paddingBottom = XulUtils.roundToInt(padding.bottom * yScalar);
				imgInfo._paddingLeft = XulUtils.roundToInt(padding.left * xScalar);
				imgInfo._paddingRight = XulUtils.roundToInt(padding.right * xScalar);
			} else {
				imgInfo._paddingTop = 0;
				imgInfo._paddingBottom = 0;
				imgInfo._paddingLeft = 0;
				imgInfo._paddingRight = 0;
			}
			_images.put(i, imgInfo);
		}
	}

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		drawBackground(dc, rect, xBase, yBase);
		drawImages(dc, rect, xBase, yBase);
		drawText(dc, rect, xBase, yBase);
		drawBorder(dc, rect, xBase, yBase);
	}

	protected void drawImages(XulDC dc, Rect rect, int xBase, int yBase) {
		Paint paint = _ctx.getDefPicPaint();
		Paint alphaPicPaint = _ctx.getDefAlphaPicPaint();
		int drawCount = 0;
		int xImageBase = _screenX + xBase;
		int yImageBase = _screenY + yBase;

		for (int i = _layerCount - 1; i >= 0; --i) {
			DrawableInfo imgInfo = _images.get(i);
			if (imgInfo == null) {
				continue;
			}
			XulDrawable bmp = imgInfo.prepareBitmap();
			if (bmp == null) {
				if (!imgInfo._isLoading && !TextUtils.isEmpty(imgInfo._url)) {
					_imageChanged = true;
				}
				continue;
			}
			if (!imgInfo.isVisible()) {
				imgInfo._skipDraw = true;
				continue;
			}

			if (imgInfo._autoHide && drawCount > 0) {
				imgInfo._skipDraw = true;
				continue;
			}
			imgInfo._skipDraw = false;
			if (imgInfo._isStretch && (imgInfo._fadeEffect == null || imgInfo._fadeEffect.isEmpty())) {
				++drawCount;
			}
		}

		for (int i = 0; i < _layerCount; i++) {
			DrawableInfo imgInfo = _images.get(i);
			if (imgInfo == null) {
				continue;
			}
			XulDrawable bmp = imgInfo.prepareBitmap();
			if (bmp == null) {
				if (!imgInfo._isLoading && !TextUtils.isEmpty(imgInfo._url)) {
					_imageChanged = true;
				}
				continue;
			}
			XulDrawer drawer = imgInfo._drawer;
			ArrayList<ImageEffectDrawer> fadeEffect = imgInfo._fadeEffect;
			if (imgInfo._skipDraw) {
				if (imgInfo._visibleFadeEffectDrawer != null) {
					continue;
				}
				if (drawer != null) {
					drawer.reset();
				}
				if (fadeEffect != null) {
					fadeEffect.clear();
				}
				continue;
			}
			if (fadeEffect == null || fadeEffect.isEmpty()) {
				drawImage(dc, paint, imgInfo, bmp, drawer, xImageBase, yImageBase);
			} else {
				for (int idx = 0, fadeEffectSize = fadeEffect.size(); idx < fadeEffectSize; idx++) {
					ImageEffectDrawer fadeEffectDrawer = fadeEffect.get(idx);
					drawImage(dc, alphaPicPaint, imgInfo, bmp, fadeEffectDrawer, xImageBase, yImageBase);
				}
			}
		}
	}

	protected void drawImage(XulDC dc, Paint paint, DrawableInfo imgInfo, XulDrawable bmp, XulDrawer drawer, int xBase, int yBase) {
		float height = bmp.getHeight();
		float width = bmp.getWidth();
		float scalarY = _scalarY;
		float scalarX = _scalarX;
		float opacity = imgInfo._opacity;

		int paddingLeft = XulUtils.roundToInt(imgInfo._paddingLeft * scalarX);
		int paddingRight = XulUtils.roundToInt(imgInfo._paddingRight * scalarX);
		int paddingTop = XulUtils.roundToInt(imgInfo._paddingTop * scalarY);
		int paddingBottom = XulUtils.roundToInt(imgInfo._paddingBottom * scalarY);
		float alignX = imgInfo._alignX;
		float alignY = imgInfo._alignY;

		if (opacity != 0.0f && opacity != 1.0f) {
			paint = _ctx.getDefAlphaPicPaint();
			paint.setAlpha((int) (0xFF * opacity));
		}

		if (_isRTL()) {
			int tmp = paddingLeft;
			paddingLeft = paddingRight;
			paddingRight = tmp;
			alignX = 1.0f - alignX;
		}

		if (imgInfo._isStretch) {
			RectF dstRc = getAnimRect();
			if (imgInfo._shadowSize > 0.5) {
				int borderSize = XulUtils.roundToInt(imgInfo._shadowSize);
				dstRc.left += paddingLeft - borderSize;
				dstRc.top += paddingTop - borderSize;
				dstRc.right -= paddingRight - borderSize;
				dstRc.bottom -= paddingBottom - borderSize;
			} else {
				dstRc.left += paddingLeft;
				dstRc.top += paddingTop;
				dstRc.right -= paddingRight;
				dstRc.bottom -= paddingBottom;
			}
			XulUtils.offsetRect(dstRc, xBase, yBase);
			drawer.draw(dc, bmp, dstRc, paint);
		} else if (width > 0 && height > 0) {
			RectF targetRect = getAnimRect();
			float targetWidth = imgInfo._width;
			float targetHeight = imgInfo._height;
			if (targetWidth <= 0) {
				targetWidth = 0;
			} else if (targetWidth == XulManager.SIZE_MATCH_PARENT) {
				width = targetWidth = _rect.width();
				height = targetHeight;
			}
			if (targetHeight <= 0) {
				targetHeight = 0;
			} else if (targetHeight == XulManager.SIZE_MATCH_PARENT) {
				height = targetHeight = _rect.height();
				width = targetWidth;
			}

			if (targetWidth == 0 && targetHeight == 0) {
				double xScalar = _ctx.getXScalar();
				double yScalar = _ctx.getYScalar();
				height = XulUtils.roundToInt(height * yScalar);
				width = XulUtils.roundToInt(width * xScalar);
			} else if (targetHeight != 0) {
				if (targetWidth != 0) {
					height = targetHeight;
					width = targetWidth;
				} else {
					width = XulUtils.roundToInt(targetHeight * width / height);
					height = targetHeight;
				}
			} else {
				height = XulUtils.roundToInt(targetWidth * height / width);
				width = targetWidth;
			}

			float newHeight = height * scalarY;
			float newWidth = width * scalarX;

			float offsetX = (XulUtils.calRectWidth(targetRect) - newWidth - paddingLeft - paddingRight) * alignX + paddingLeft;
			float offsetY = (XulUtils.calRectHeight(targetRect) - newHeight - paddingTop - paddingBottom) * alignY + paddingTop;

			targetRect.left += offsetX + xBase;
			targetRect.top += offsetY + yBase;
			targetRect.right = targetRect.left + newWidth;
			targetRect.bottom = targetRect.top + newHeight;

			drawer.draw(dc, bmp, targetRect, paint);
		}
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_FOCUSABLE;
	}

	@Override
	public XulWorker.DrawableItem collectPendingImageItem() {
		if (_isDataChanged() || _isLayoutChanged()) {
			return super.collectPendingImageItem();
		}
		if (_imageChanged) {
			long timestamp = XulUtils.timestamp();
			boolean allImageReady = true;
			for (int i = 0; i < _layerCount; ++i) {
				DrawableInfo imageInfo = _images.get(i);
				if (imageInfo == null) {
					continue;
				}
				if (imageInfo._isLoading) {
					continue;
				}
				if (imageInfo._bmp != null && !imageInfo._bmp.isRecycled()) {
					continue;
				}
				if (TextUtils.isEmpty(imageInfo._url)) {
					continue;
				}
				allImageReady = false;

				int loadInterval;
				if (imageInfo._loadFailedCounter < 3) {
					// 如果失败次数小于3次则5秒后重试
					loadInterval = 5 * 1000;
				} else {
					// 如果失败次数过多则30分钟重试一次
					loadInterval = 30 * 60 * 1000;
				}
				if (timestamp - imageInfo._lastLoadFailedTime < loadInterval) {
					// 一定时间内不重新加载同一图片
					continue;
				}
				if (imageInfo._isStretch) {
					if (_rect == null) {
						continue;
					}
					imageInfo.target_width = XulUtils.calRectWidth(_rect) - (imageInfo._paddingLeft + imageInfo._paddingRight);
					if (imageInfo.target_width < 0) {
						imageInfo.target_width = 0;
					}
					imageInfo.target_height = XulUtils.calRectHeight(_rect) - (imageInfo._paddingTop + imageInfo._paddingBottom);
					if (imageInfo.target_height < 0) {
						imageInfo.target_height = 0;
					}
				}
				if (imageInfo._width <= XulManager.SIZE_MAX) {
					imageInfo.width = imageInfo._width;
				} else {
					// 图片自动适应内容时，原尺寸加载图片
					imageInfo.width = 0;

				}
				if (imageInfo._height <= XulManager.SIZE_MAX) {
					imageInfo.height = imageInfo._height;
				} else {
					// 图片自动适应内容时，原尺寸加载图片
					imageInfo.height = 0;
				}
				imageInfo.setRoundRect(imageInfo._roundRadius);
				imageInfo.scalarX = getRenderContext().getXScalar();
				imageInfo.scalarY = getRenderContext().getYScalar();
				imageInfo.shadowSize = imageInfo._shadowSize;
				imageInfo.shadowColor = imageInfo._shadowColor;
				imageInfo.reusable = imageInfo._reuse;
				imageInfo.pixFmt = imageInfo._pixFmt;
				imageInfo._isLoading = true;
				imageInfo.url = imageInfo._url;
				imageInfo._loadBeginTime = XulUtils.timestamp();
				return imageInfo;
			}
			_imageChanged = !allImageReady;
		}
		return super.collectPendingImageItem();
	}

	@Override
	public void cleanImageItems() {
		for (int i = 0; i < _MaxImgLayers; ++i) {
			DrawableInfo imageInfo = _images.get(i);
			if (imageInfo == null) {
				continue;
			}
			if (imageInfo._reuse) {
				XulDrawable bmp = imageInfo._bmp;
				if (bmp != null && bmp instanceof XulBitmapDrawable) {
					if (XulManager.DEBUG) {
						Log.d("TAG", "recycle(0) reusable:" + imageInfo._url);
					}
					BitmapTools.recycleBitmap(XulBitmapDrawable.detachBitmap((XulBitmapDrawable) bmp));
					bmp.recycle();
				}
			}
			imageInfo._bmp = null;
			imageInfo._fadeInBkg = null;
			imageInfo._drawer = null;
			imageInfo._lastLoadFailedTime = 0;
			imageInfo._loadFailedCounter = 0;
			if (imageInfo._fadeEffect != null) {
				imageInfo._fadeEffect.clear();
			}
			_imageChanged = true;
		}
		super.cleanImageItems();
	}

	@Override
	public boolean doSuspendRecycle(int recycleLevel) {
		if (recycleLevel <= 0 && internalRejectTest()) {
			boolean anyImageRecycled = false;
			// recycle only reusable images
			for (int i = 0; i < _MaxImgLayers; ++i) {
				DrawableInfo imageInfo = _images.get(i);
				if (imageInfo == null) {
					continue;
				}
				if (!imageInfo._reuse) {
					continue;
				}
				if (imageInfo._bmp == null) {
					continue;
				}
				XulDrawable bmp = imageInfo._bmp;
				if (bmp != null && bmp instanceof XulBitmapDrawable) {
					if (XulManager.DEBUG) {
						Log.d("TAG", "recycle(1) reusable:" + imageInfo._url);
					}
					BitmapTools.recycleBitmap(XulBitmapDrawable.detachBitmap((XulBitmapDrawable) bmp));
					bmp.recycle();
				}
				imageInfo._bmp = null;
				imageInfo._fadeInBkg = null;
				imageInfo._drawer = null;
				imageInfo._lastLoadFailedTime = 0;
				imageInfo._loadFailedCounter = 0;
				if (imageInfo._fadeEffect != null) {
					imageInfo._fadeEffect.clear();
				}
				_imageChanged = true;
				anyImageRecycled = true;
			}
			if (anyImageRecycled) {
				return true;
			}
		}
		return super.doSuspendRecycle(recycleLevel);
	}

	public boolean hasImageLayer(int idx) {
		if (idx >= _images.size()) {
			return false;
		}
		DrawableInfo info = _images.get(idx);
		return info != null;
	}

	public int getImageWidth(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return -2;
		}
		XulDrawable bmp = info._bmp;
		if (bmp == null || bmp.isRecycled()) {
			return -1;
		}
		return bmp.getWidth();
	}

	public int getImageHeight(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return -2;
		}
		XulDrawable bmp = info._bmp;
		if (bmp == null || bmp.isRecycled()) {
			return -1;
		}
		return bmp.getHeight();
	}

	public boolean reloadImage(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return false;
		}
		if (info._isLoading) {
			return true;
		}
		if (info._reuse) {
			XulDrawable bmp = info._bmp;
			if (bmp != null && bmp instanceof XulBitmapDrawable) {
				BitmapTools.recycleBitmap(XulBitmapDrawable.detachBitmap((XulBitmapDrawable) bmp));
				bmp.recycle();
			}
		}
		info._bmp = null;
		info._loadFailedCounter = 0;
		info._lastLoadFailedTime = 0;
		info._isRecycled = false;
		info._isLoading = false;
		return true;
	}

	public String getImageUrl(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return null;
		}
		return info._url;
	}

	public String getImageResolvedUrl(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return null;
		}
		return info.getInternalResolvedPath();
	}

	public Rect getImagePadding(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return null;
		}
		return new Rect(info._paddingLeft, info._paddingTop, info._paddingRight, info._paddingBottom);
	}

	public boolean resetAnimation(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return false;
		}
		final XulDrawer drawer = info._drawer;
		if (drawer == null) {
			return false;
		}
		if (!(drawer instanceof XulAnimationDrawer)) {
			return false;
		}
		drawer.reset();
		markDirtyView();
		return true;
	}

	public boolean isImageLoaded(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return false;
		}
		return info._bmp != null;
	}

	public boolean isImageVisible(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return false;
		}
		return info._bmp != null && info.isVisible();
	}

	public float getImageOpacity(int layer) {
		DrawableInfo info = _images.get(layer);
		if (info == null) {
			return 0;
		}
		return info._opacity;
	}

	@Override
	public void onVisibilityChanged(boolean isVisible, XulView eventSource) {
		super.onVisibilityChanged(isVisible, eventSource);
		if (!isVisible) {
			for (int i = 0; i < _layerCount; ++i) {
				DrawableInfo imageInfo = _images.get(i);
				if (imageInfo == null) {
					continue;
				}
				XulDrawer drawer = imageInfo._drawer;
				if (drawer != null) {
					drawer.reset();
				}
			}
		}
	}

	protected class LayoutElement extends XulLabelRender.LayoutElement {
		@Override
		public int prepare() {
			if (_isVisible()) {
				syncDoNotMatchTextStyle();
			}
			super.prepare();
			syncImageSize();
			return 0;
		}

		void syncImageSize() {
			if (_isInvisible()) {
				return;
			}
			if (_layoutOnImageChanged) {
				int itemWidth = getWidth();
				int itemHeight = getHeight();
				if (itemWidth < XulManager.SIZE_MAX && itemHeight < XulManager.SIZE_MAX) {
					_layoutOnImageChanged = false;
					return;
				}
				double xScalar = _ctx.getXScalar();
				double yScalar = _ctx.getYScalar();

				int imgWidth = -1;
				int imgHeight = -1;
				for (int i = 0; i < _layerCount; ++i) {
					DrawableInfo imgInfo = _images.get(i);
					if (imgInfo == null || !imgInfo.isVisible()) {
						continue;
					}
					if (imgInfo._bmp == null || imgInfo._bmp.isRecycled()) {
						continue;
					}
					XulDrawable bmp = imgInfo._bmp;
					final int imgInfoWidth = imgInfo._width;
					final int imgInfoHeight = imgInfo._height;

					if (imgInfoWidth <= XulManager.SIZE_MAX &&
						imgInfoHeight <= XulManager.SIZE_MAX) {
						int height = bmp.getHeight();
						int width = bmp.getWidth();
						if (imgInfo._isStretch) {
							// fix incorrect image size if image was loaded before first-time layout
							if (itemHeight < XulManager.SIZE_MAX) {
								if (height > itemHeight) {
									if (XulManager.DEBUG) {
										Log.w(TAG, "image loaded with incorrect size! " + itemWidth + "x" + itemHeight + " -> " + width + "x" + height);
									}
									width = width * itemHeight / height;
									height = itemHeight;
								}
							} else if (itemWidth < XulManager.SIZE_MAX) {
								if (width > itemWidth) {
									if (XulManager.DEBUG) {
										Log.w(TAG, "image loaded with incorrect size! " + itemWidth + "x" + itemHeight + " -> " + width + "x" + height);
									}
									height = height * width / itemWidth;
									width = itemWidth;
								}
							}
						} else if (width > 0 && height > 0) {
							int targetWidth = imgInfoWidth;
							int targetHeight = imgInfoHeight;
							if (targetWidth <= 0) {
								targetWidth = 0;
							}
							if (targetHeight <= 0) {
								targetHeight = 0;
							}
							if (targetWidth == 0 && targetHeight == 0) {
								height = XulUtils.roundToInt(height * yScalar);
								width = XulUtils.roundToInt(width * xScalar);
							} else if (targetHeight != 0) {
								if (targetWidth != 0) {
									height = targetHeight;
									width = targetWidth;
								} else {
									width = XulUtils.roundToInt(((float) targetHeight) * width / height);
									height = targetHeight;
								}
							} else {
								height = XulUtils.roundToInt(((float) targetWidth) * height / width);
								width = targetWidth;
							}
						}

						if (imgInfoWidth <= XulManager.SIZE_MAX) {
							if (width > imgWidth) {
								imgWidth = width + imgInfo._paddingLeft + imgInfo._paddingRight;
							}
						}
						if (imgInfoHeight <= XulManager.SIZE_MAX) {
							if (height > imgHeight) {
								imgHeight = height + imgInfo._paddingTop + imgInfo._paddingBottom;
							}
						}
					}
				}
				_imgWidth = imgWidth;
				_imgHeight = imgHeight;
			}
		}

		int _imgWidth = -1;
		int _imgHeight = -1;


		@Override
		public int getContentWidth() {
			int contentWidth = _doNotMatchTextWidth ? 0 : super.getContentWidth();
			if (_layoutOnImageChanged && _imgWidth > 0) {
				int paddingWidth = _padding.left + _padding.right;
				return Math.max(_imgWidth - paddingWidth, contentWidth);
			}
			return contentWidth;
		}

		@Override
		public int getContentHeight() {
			int contentHeight = _doNotMatchTextHeight ? 0 : super.getContentHeight();
			if (_layoutOnImageChanged && _imgHeight > 0) {
				int paddingHeight = _padding.top + _padding.bottom;
				return Math.max(_imgHeight - paddingHeight, contentHeight);
			}
			return contentHeight;
		}
	}

	private void syncDoNotMatchTextStyle() {
		if (!_isViewChanged()) {
			return;
		}
		XulStyle doNotMatchTextStyle = _view.getStyle(XulPropNameCache.TagId.DO_NOT_MATCH_TEXT);
		if (doNotMatchTextStyle == null) {
			_doNotMatchTextWidth = false;
			_doNotMatchTextHeight = false;
		} else {
			XulPropParser.xulParsedStyle_DoNotMatchText val = doNotMatchTextStyle.getParsedValue();
			_doNotMatchTextWidth = val.doNotMatchWidth;
			_doNotMatchTextHeight = val.doNotMatchHeight;
		}
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutElement();
	}

}
