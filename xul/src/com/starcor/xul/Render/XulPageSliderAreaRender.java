package com.starcor.xul.Render;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Render.Effect.FlipAnimation;
import com.starcor.xul.Render.Effect.SimpleTransformAnimation;
import com.starcor.xul.Utils.XulAreaChildrenVisibleChangeNotifier;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.Utils.XulRenderDrawableItem;
import com.starcor.xul.Utils.XulSimpleArray;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulLayout;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulPage;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulTaskCollector;
import com.starcor.xul.XulTemplate;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;
import com.starcor.xul.XulWorker;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/12.
 */
public class XulPageSliderAreaRender extends XulViewContainerBaseRender {
	public static final String TAG = XulPageSliderAreaRender.class.getSimpleName();
	private static final float MAX_PAGE_SIZE = 16384;
	public static final int DEFAULT_IMAGE_GC_LEVEL = 2;
	private FlipAnimation _flipAnimation;
	private boolean _switchingByFocus = true;

	private ArrayList<XulTaskCollector.XulVolatileReference<XulView>> _pendingItems = new ArrayList<XulTaskCollector.XulVolatileReference<XulView>>();

	public static void register() {
		XulRenderFactory.registerBuilder("area", "page_slider", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				return new XulPageSliderAreaRender(ctx, (XulArea) view);
			}
		});
	}

	boolean canPrefetch(XulView view) {
		ArrayList<XulTaskCollector.XulVolatileReference<XulView>> pendingItems = _pendingItems;
		if (pendingItems.isEmpty()) {
			return false;
		}
		synchronized (pendingItems) {
			for (int i = 0, pendingItemsSize = pendingItems.size(); i < pendingItemsSize; i++) {
				XulTaskCollector.XulVolatileReference<XulView> ref = pendingItems.get(i);
				XulView xulView = ref.get();
				if (xulView == null) {
					continue;
				}
				if (view.isChildOf(xulView)) {
					return true;
				}
			}
		}
		return false;
	}

	private class _PageSliderChildrenCollector extends XulArea.XulAreaIterator {
		private void collectChild(XulView item) {
			if ("none".equals(item.getStyleString(XulPropNameCache.TagId.DISPLAY))) {
				return;
			}
			_contents.add(item);
		}

		@Override
		public boolean onXulArea(int pos, XulArea area) {
			collectChild(area);
			return true;
		}

		@Override
		public boolean onXulItem(int pos, XulItem item) {
			collectChild(item);
			return true;
		}

		@Override
		public boolean onXulTemplate(int pos, XulTemplate template) {
			return true;
		}
	}

	_PageSliderChildrenCollector _contentCollector = new _PageSliderChildrenCollector();
	ArrayList<XulView> _contents = new ArrayList<XulView>();
	int _curPage;
	int _preloadPages = 1;
	boolean _isVertical = false;
	boolean _isLoopMode = true;
	int _autoImageGCLevel = DEFAULT_IMAGE_GC_LEVEL;
	boolean _peekViewNextPage = false;

	// 页面滑动速度，-1无滑动动画
	int _animationSpeed = 400;
	int _pageOffsetX;
	int _pageOffsetY;

	boolean _clipChildren = true;
	boolean _clipFocus = false;

	private void _updatePageOffsetY(float offsetY) {
		if (_pageOffsetY == offsetY) {
			return;
		}
		if (_contents.size() > 1) {
			XulRenderContext.suspendDrawableWorker();
		}
		_pageOffsetY = XulUtils.roundToInt(offsetY);
	}

	private void _updatePageOffsetX(float offsetX) {
		if (_pageOffsetX == offsetX) {
			return;
		}
		if (_contents.size() > 1) {
			XulRenderContext.suspendDrawableWorker();
		}
		_pageOffsetX = XulUtils.roundToInt(offsetX);
	}

	enum indicatorStyle {
		STYLE_DOT,
		STYLE_DASH,
		STYLE_IMAGE
	}

	class IndicatorDrawableInfo extends XulRenderDrawableItem {
		String _url;
		int _width = 0;
		int _height = 0;
		volatile boolean _isLoading = false;
		volatile boolean _isRecycled = false;
		volatile long _lastLoadFailedTime = 0;
		volatile int _loadFailedCounter = 0;
		XulDrawable _bmp = null;

		@Override
		public void onImageReady(XulDrawable bmp) {
			_isLoading = false;
			if (_isRecycled) {
				return;
			}
			if (bmp == null) {
				_lastLoadFailedTime = XulUtils.timestamp();
				_loadFailedCounter++;
			} else {
				_bmp = bmp;
				_lastLoadFailedTime = 0;
				_loadFailedCounter = 0;
			}
			markDirtyView();
		}
	}

	static class indicatorInfo {
		float xAlign, yAlign;

		int gap = 12;

		float indicatorAlign = 0.5f;

		indicatorStyle normalStyle = indicatorStyle.STYLE_DOT;
		float normalCx = 7, normalCy = 7, normalPadding = 0;
		int normalColor = Color.WHITE;
		IndicatorDrawableInfo focusImage;


		indicatorStyle focusStyle = indicatorStyle.STYLE_DOT;
		float focusCx = 10, focusCy = 10, focusPadding = 0;
		int focusColor = Color.WHITE;
		IndicatorDrawableInfo normalImage;
	}

	indicatorInfo _indicator;

	public XulPageSliderAreaRender(XulRenderContext ctx, XulArea area) {
		super(ctx, area);
		init();
	}

	private void init() {
		_curPage = 0;
		if (_view != null) {
			XulAttr initPage = _view.getAttr(XulPropNameCache.TagId.INIT_PAGE);
			if (initPage != null) {
				_curPage = XulUtils.tryParseInt(initPage.getStringValue(), 0);
			}
		}
		collectContent();
		notifyPageChanged(_curPage);
	}

	private void collectContent() {
		_contents.clear();
		_area.eachChild(_contentCollector);
		if (_curPage >= _contents.size()) {
			_curPage = 0;
			notifyPageChanged(_curPage);
		}
		for (int i = 0; i < _contents.size(); i++) {
			XulView item = _contents.get(i);
			if (i == _curPage) {
				item.setEnabled(true);
			} else {
				item.setEnabled(false);
			}
		}
	}

	private void syncDirection() {
		if (!_isViewChanged()) {
			return;
		}
		XulAttr directionAttr = _area.getAttr(XulPropNameCache.TagId.DIRECTION);
		if (directionAttr != null) {
			XulPropParser.xulParsedAttr_Direction direction = directionAttr.getParsedValue();
			_isVertical = direction.vertical;
			// _reverseLayout = direction.reverse;
		}
	}

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		super.syncData();

		XulAttr aniSpeed = _view.getAttr(XulPropNameCache.TagId.ANIMATION_SPEED);
		if (aniSpeed == null) {
			// for compatible
			aniSpeed = _view.getAttr("slide_speed");
		}
		_animationSpeed = XulUtils.tryParseInt(aniSpeed == null ? "400" : aniSpeed.getStringValue(), 400);


		XulAttr aniType = _view.getAttr(XulPropNameCache.TagId.ANIMATION_TYPE);
		if (aniType != null) {
			String aniTypeStringValue = aniType.getStringValue();
			if ("flip".equals(aniTypeStringValue)) {
				initFlipAnimation();
			} else {
				clearFlipAnimation();
			}
		} else {
			clearFlipAnimation();
		}

		XulAttr peekAttr = _view.getAttr(XulPropNameCache.TagId.PEEK_NEXT_PAGE);
		if (peekAttr != null) {
			String peekAttrStringValue = peekAttr.getStringValue();
			_peekViewNextPage = "enabled".equals(peekAttrStringValue) || "true".equals(peekAttrStringValue);
		} else {
			_peekViewNextPage = false;
		}

		XulAttr switchingMode = _view.getAttr(XulPropNameCache.TagId.SWITCHING_MODE);
		_switchingByFocus = true;
		if (switchingMode != null) {
			String switchingModeStringValue = switchingMode.getStringValue();
			if ("none".equals(switchingModeStringValue)) {
				_switchingByFocus = false;
			} else if ("focus".equals(switchingModeStringValue)) {
			} else if ("auto".equals(switchingModeStringValue)) {
			}
		} else {
		}

		XulAttr attrLoop = _view.getAttr(XulPropNameCache.TagId.LOOP);
		if (attrLoop != null) {
			String loopVal = attrLoop.getStringValue();
			if ("true".equals(loopVal) || "enabled".equals(loopVal)) {
				_isLoopMode = true;
			} else {
				_isLoopMode = false;
			}
		} else {
			_isLoopMode = true;
		}

		XulAttr autoImageGc = _view.getAttr(XulPropNameCache.TagId.IMAGE_GC);
		if (autoImageGc != null) {
			String val = autoImageGc.getStringValue();
			if ("auto".equals(val)) {
				_autoImageGCLevel = DEFAULT_IMAGE_GC_LEVEL;
			} else {
				_autoImageGCLevel = XulUtils.tryParseInt(val, -1);
			}
		} else {
			_autoImageGCLevel = DEFAULT_IMAGE_GC_LEVEL;
		}

		XulAttr preloadEnabled = _view.getAttr(XulPropNameCache.TagId.PRELOAD_PAGE);
		if (preloadEnabled != null) {
			String val = preloadEnabled.getStringValue();
			if ("disabled".equals(val)) {
				_preloadPages = 0;
			} else {
				_preloadPages = XulUtils.tryParseInt(val, 1);
			}
		} else {
			_preloadPages = 1;
		}

		String clipChildren = _view.getStyleString(XulPropNameCache.TagId.CLIP_CHILDREN);
		if ("false".equals(clipChildren)) {
			_clipChildren = false;
		} else {
			_clipChildren = true;
			String clipFocus = _area.getStyleString(XulPropNameCache.TagId.CLIP_FOCUS);
			if ("true".equals(clipFocus)) {
				_clipFocus = true;
			} else {
				_clipFocus = false;
			}
		}

		double xScalar = _ctx.getXScalar();
		double yScalar = _ctx.getYScalar();
		XulAttr indicator = _view.getAttr(XulPropNameCache.TagId.INDICATOR);

		if (indicator != null && ((XulPropParser.xulParsedProp_booleanValue) indicator.getParsedValue()).val) {
			_indicator = new indicatorInfo();
			XulAttr indicatorStyle = _view.getAttr(XulPropNameCache.TagId.INDICATOR_STYLE);
			XulAttr indicatorGap = _view.getAttr(XulPropNameCache.TagId.INDICATOR_GAP);
			XulAttr indicatorAlign = _view.getAttr(XulPropNameCache.TagId.INDICATOR_ALIGN);

			String styleInfo = indicatorStyle == null ? null : indicatorStyle.getStringValue();
			if (!TextUtils.isEmpty(styleInfo)) {
				String[] styleParams = styleInfo.split(",");
				int readPos = 0;

				if (styleParams.length > 0) {
					float iAlign = XulUtils.tryParseFloat(styleParams[readPos], -1);
					if (iAlign >= 0) {
						_indicator.indicatorAlign = iAlign;
						readPos++;
					}
				}
				{
					String style = readPos < styleParams.length ? styleParams[readPos++] : null;
					if ("dot".equals(style)) {
						_indicator.normalStyle = _indicator.focusStyle = XulPageSliderAreaRender.indicatorStyle.STYLE_DOT;
						double size = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 4);
						double padding = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "0", 0);
						int color = (int) XulUtils.tryParseHex(readPos < styleParams.length ? styleParams[readPos++] : "FFFFFFFF", Color.WHITE);
						_indicator.focusCx = _indicator.focusCy = (float) (size * xScalar);
						_indicator.normalCx = _indicator.normalCy = (float) (size * yScalar * 0.6f);
						_indicator.normalPadding = _indicator.focusPadding = (float) (padding * xScalar);
						_indicator.normalColor = _indicator.focusColor = color;
					} else if ("dash".equals(style)) {
						_indicator.normalStyle = _indicator.focusStyle = XulPageSliderAreaRender.indicatorStyle.STYLE_DASH;
						double width = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 4);
						double height = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 4);
						double padding = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "0", 0);
						int color = (int) XulUtils.tryParseHex(readPos < styleParams.length ? styleParams[readPos++] : "FFFFFFFF", Color.WHITE);
						_indicator.focusCx = (float) (width * xScalar);
						_indicator.focusCy = (float) (height * yScalar);
						_indicator.normalCx = (float) (width * xScalar) * 0.8f;
						_indicator.normalCy = (float) (height * yScalar) * 0.6f;
						_indicator.normalPadding = _indicator.focusPadding = (float) (padding * xScalar);
						_indicator.normalColor = _indicator.focusColor = color;
					} else if ("image".equals(style)) {
						_indicator.normalStyle = _indicator.focusStyle = XulPageSliderAreaRender.indicatorStyle.STYLE_IMAGE;
						double width = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 16);
						double height = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 16);
						double padding = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "0", 0);
						String imgPath = readPos < styleParams.length ? styleParams[readPos++] : null;

						_indicator.focusCx = (float) (width * xScalar);
						_indicator.focusCy = (float) (height * yScalar);
						_indicator.normalCx = (float) (width * xScalar);
						_indicator.normalCy = (float) (height * yScalar);
						_indicator.normalPadding = _indicator.focusPadding = (float) (padding * xScalar);
						_indicator.normalColor = _indicator.focusColor = 0xFFFFFFFF;
						if (TextUtils.isEmpty(imgPath)) {
							_indicator.normalImage = _indicator.focusImage = null;
						} else {
							IndicatorDrawableInfo imageInfo = new IndicatorDrawableInfo();
							imageInfo._url = imgPath;
							imageInfo._bmp = XulWorker.loadDrawableFromCache(imageInfo._url);
							if (imageInfo._bmp != null) {
								markDirtyView();
							}
							imageInfo._height = XulUtils.roundToInt(height * yScalar);
							imageInfo._width = XulUtils.roundToInt(width * xScalar);
							_indicator.normalImage = _indicator.focusImage = imageInfo;
						}
					}

					style = readPos < styleParams.length ? styleParams[readPos++] : null;
					if ("dot".equals(style)) {
						_indicator.normalStyle = XulPageSliderAreaRender.indicatorStyle.STYLE_DOT;
						double size = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 4);
						double padding = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "0", 0);
						int color = (int) XulUtils.tryParseHex(readPos < styleParams.length ? styleParams[readPos++] : "FFFFFFFF", Color.WHITE);
						_indicator.normalCx = _indicator.normalCy = (float) (size * xScalar);
						_indicator.normalPadding = (float) (padding * xScalar);
						_indicator.normalColor = color;
					} else if ("dash".equals(style)) {
						_indicator.normalStyle = XulPageSliderAreaRender.indicatorStyle.STYLE_DASH;
						double width = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 4);
						double height = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 4);
						double padding = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "0", 0);
						int color = (int) XulUtils.tryParseHex(readPos < styleParams.length ? styleParams[readPos++] : "FFFFFFFF", Color.WHITE);
						_indicator.normalCx = (float) (width * xScalar);
						_indicator.normalCy = (float) (height * yScalar);
						_indicator.normalPadding = (float) (padding * xScalar);
						_indicator.normalColor = color;
					} else if ("image".equals(style)) {
						_indicator.normalStyle = _indicator.focusStyle = XulPageSliderAreaRender.indicatorStyle.STYLE_IMAGE;
						double width = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 16);
						double height = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "4", 16);
						double padding = XulUtils.tryParseFloat(readPos < styleParams.length ? styleParams[readPos++] : "0", 0);
						String imgPath = readPos < styleParams.length ? styleParams[readPos++] : null;

						_indicator.normalCx = (float) (width * xScalar);
						_indicator.normalCy = (float) (height * yScalar);
						_indicator.normalPadding = (float) (padding * xScalar);
						_indicator.normalColor = 0xFFFFFFFF;
						if (TextUtils.isEmpty(imgPath)) {
							_indicator.normalImage = null;
						} else {
							IndicatorDrawableInfo imageInfo = new IndicatorDrawableInfo();
							imageInfo._url = imgPath;
							imageInfo._bmp = XulWorker.loadDrawableFromCache(imageInfo._url);
							if (imageInfo._bmp != null) {
								markDirtyView();
							}
							imageInfo._height = XulUtils.roundToInt(height * yScalar);
							imageInfo._width = XulUtils.roundToInt(width * xScalar);
							_indicator.normalImage = imageInfo;
						}
					}
				}
			}
			_indicator.gap = XulUtils.roundToInt(XulUtils.tryParseInt(indicatorGap != null ? indicatorGap.getStringValue() : "12", 12) * xScalar);

			float xAlign;
			float yAlign;
			if (this._isVertical) {
				xAlign = 0.0f;
				yAlign = 0.5f;
			} else {
				xAlign = 0.5f;
				yAlign = 1.0f;
			}
			if (indicatorAlign != null) {
				XulPropParser.xulParsedProp_FloatArray parsedAlign = indicatorAlign.getParsedValue();
				xAlign = parsedAlign.tryGetVal(0, xAlign);
				yAlign = parsedAlign.tryGetVal(1, yAlign);
			}
			_indicator.xAlign = xAlign;
			_indicator.yAlign = yAlign;
		} else {
			_indicator = null;
		}

	}

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		if (_contents.isEmpty()) {
			return;
		}
		drawPage(dc, rect, xBase, yBase);
		drawIndicator(dc, xBase, yBase);
	}

	private void initFlipAnimation() {
		if (_flipAnimation != null) {
			return;
		}

		_flipAnimation = new FlipAnimation(this) {
			float _stepX = 0;
			float _stepY = 0;

			@Override
			public boolean updateAnimation(long timestamp) {
				if (_pageOffsetX == 0 && _pageOffsetY == 0) {
					_stepX = 0;
					_stepY = 0;
					return true;
				}

				float speed = (float) (0.85f * Math.sqrt(_animationSpeed / 500.0f));
				if (speed >= 0.99f) {
					speed = 0.99f;
				}
				if (_pageOffsetX != 0 && _stepX == 0) {
					_stepX = 180.0f / _pageOffsetX;
				}
				if (_pageOffsetY != 0 && _stepY == 0) {
					_stepY = 180.0f / _pageOffsetY;
				}
				if (Math.abs(_pageOffsetX) * (1 - speed) < 2) {
					if (Math.abs(_pageOffsetX) < 2) {
						_updatePageOffsetX(0);
					} else {
						_updatePageOffsetX(_pageOffsetX - (_pageOffsetX < 0 ? -2 : 2));
					}
				} else {
					_updatePageOffsetX((int) (_pageOffsetX * speed));
				}

				if (Math.abs(_pageOffsetY) * (1 - speed) < 2) {
					if (Math.abs(_pageOffsetY) < 2) {
						_updatePageOffsetY(0);
					} else {
						_updatePageOffsetY(_pageOffsetY - (_pageOffsetY < 0 ? -2 : 2));
					}
				} else {
					_updatePageOffsetY((int) (_pageOffsetY * speed));
				}

				float angle = _stepX * Math.abs(_pageOffsetX);
				if (angle >= -90 && angle < 90) {
					setAngle(angle);
				} else {
					setAngle(180 + 720 + angle);
				}
				markDirtyView();
				return true;
			}
		};
	}

	private void clearFlipAnimation() {
		if (_flipAnimation != null) {
			removeAnimation(_flipAnimation);
			_flipAnimation = null;
		}
	}

	private void drawPage(XulDC dc, Rect rect, int xBase, int yBase) {
		super.draw(dc, rect, xBase, yBase);
		int pageNum = _contents.size();

		assert _curPage < pageNum;

		RectF thisRc = getAnimRect();
		final XulView curPageArea = _contents.get(_curPage);
		dc.save();
		XulUtils.offsetRect(thisRc, _screenX + xBase, _screenY + yBase);
		if (_clipChildren) {
			dc.clipRect(thisRc);
		}
		XulUtils.offsetRect(thisRc, -(_screenX + xBase), -(_screenY + yBase));

		RectF curPageFocusRc = curPageArea.getFocusRc();
		if (pageNum > 1 && (_pageOffsetX != 0 || _pageOffsetY != 0) && _animationSpeed > 100) {
			if (_flipAnimation != null) {
				drawFlipAnimation(dc, rect, xBase, yBase, pageNum, curPageArea, curPageFocusRc, thisRc);
			} else {
				drawSlideAnimation(dc, rect, xBase, yBase, pageNum, curPageArea, curPageFocusRc);
			}
		} else {
			if (curPageArea.getRender().needPostDraw()) {
				dc.postDraw(curPageArea, rect, xBase, yBase, curPageArea.getRender()._zIndex);
			} else {
				curPageArea.draw(dc, rect, xBase, yBase);
			}

			if (_peekViewNextPage && pageNum > 1) {
				int nextPageIdx = (_curPage + 1);
				if (nextPageIdx < pageNum || _isLoopMode) {
					nextPageIdx %= pageNum;
					XulView nextPage = _contents.get(nextPageIdx);
					if (_isVertical) {
						nextPage.draw(dc, rect, xBase, XulUtils.roundToInt(yBase + XulUtils.calRectHeight(curPageFocusRc)));
					} else {
						nextPage.draw(dc, rect, XulUtils.roundToInt(xBase + XulUtils.calRectWidth(curPageFocusRc)), yBase);
					}
				}
			}
		}

		if (_clipChildren && _clipFocus) {
			int zIndex = getZIndex() - 1;
			if (zIndex < 0) {
				zIndex = 0;
			}
			dc.doPostDraw(zIndex, _area);
		}
		dc.restore();
	}

	private void drawFlipAnimation(XulDC dc, Rect rect, int xBase, int yBase, int counter, XulView curPageArea, RectF curPageFocusRc, RectF thisRc) {
		_flipAnimation.preDraw(dc, xBase + _screenX + thisRc.left, yBase + _screenY + thisRc.top, XulUtils.calRectWidth(thisRc), XulUtils.calRectHeight(thisRc));
		if (_pageOffsetX > 0 || _pageOffsetY > 0) {
			XulView prevPageArea = _contents.get(_curPage == 0 ? counter - 1 : _curPage - 1);
			if (_flipAnimation.getAngle() < 90) {
				curPageArea.draw(dc, rect, xBase, yBase);
			} else {
				prevPageArea.draw(dc, rect, xBase, yBase);
			}
		} else {
			XulView nextPageArea = _contents.get(_curPage + 1 == counter ? 0 : _curPage + 1);
			if (_flipAnimation.getAngle() > -90 && _flipAnimation.getAngle() < 90) {
				curPageArea.draw(dc, rect, xBase, yBase);
			} else {
				nextPageArea.draw(dc, rect, xBase, yBase);
			}
		}
		dc.doPostDraw(0, _area);
		_flipAnimation.postDraw(dc, xBase + _screenX + thisRc.left, yBase + _screenY + thisRc.top, XulUtils.calRectWidth(thisRc), XulUtils.calRectHeight(thisRc));
	}

	private void drawSlideAnimation(XulDC dc, Rect rect, int xBase, int yBase, int counter, XulView curPageArea, RectF curPageFocusRc) {
		if (_pageOffsetX > 0 || _pageOffsetY > 0) {
			XulView prevPageArea = _contents.get(_curPage == 0 ? counter - 1 : _curPage - 1);
			float prevXoffset = _pageOffsetX != 0 ? _pageOffsetX - XulUtils.calRectWidth(curPageFocusRc) : 0;
			float prevYoffset = _pageOffsetY != 0 ? _pageOffsetY - XulUtils.calRectHeight(curPageFocusRc) : 0;
			prevPageArea.draw(dc, rect, XulUtils.roundToInt(xBase + prevXoffset), XulUtils.roundToInt(yBase + prevYoffset));
			curPageArea.draw(dc, rect, xBase + _pageOffsetX, yBase + _pageOffsetY);
		} else {
			XulView nextPageArea = _contents.get(_curPage + 1 == counter ? 0 : _curPage + 1);
			float nextXoffset = _pageOffsetX != 0 ? XulUtils.calRectWidth(curPageFocusRc) + _pageOffsetX : 0;
			float prevYoffset = _pageOffsetY != 0 ? XulUtils.calRectHeight(curPageFocusRc) + _pageOffsetY : 0;
			nextPageArea.draw(dc, rect, XulUtils.roundToInt(xBase + nextXoffset), XulUtils.roundToInt(yBase + prevYoffset));
			curPageArea.draw(dc, rect, xBase + _pageOffsetX, yBase + _pageOffsetY);
		}

		_updateSlideAnimation();
	}


	SimpleTransformAnimation _sliderAnimation;
	private void _updateSlideAnimation() {
		if (!_hasAnimation()) {
			_updatePageOffsetX(0);
			_updatePageOffsetY(0);
			return;
		}

		if (_sliderAnimation != null && _sliderAnimation.isRunning()) {
			return;
		}

		if (_aniTransformer != null) {
			if (_sliderAnimation == null) {
				_sliderAnimation = new SimpleTransformAnimation(this, _aniTransformer) {
					float offsetX;
					float offsetY;

					@Override
					public void storeSrc() {
						_srcVal = 1.0f;
						offsetX = _pageOffsetX;
						offsetY = _pageOffsetY;
					}

					@Override
					public void storeDest() {
						_destVal = 0.0f;
					}

					@Override
					public void restoreValue() {
						_pageOffsetX = (int) (offsetX * _val);
						_pageOffsetY = (int) (offsetY * _val);
					}
				};
			}
			addAnimation(_sliderAnimation);
			_sliderAnimation.prepareAnimation(_aniTransformDuration);
			_sliderAnimation.startAnimation();
			return;
		}

		float speed = (float) (0.85f * Math.sqrt(_animationSpeed / 500.0f));
		if (speed >= 0.99f) {
			speed = 0.99f;
		}
		if (Math.abs(_pageOffsetX) * (1 - speed) < 2) {
			if (Math.abs(_pageOffsetX) < 2) {
				_updatePageOffsetX(0);
			} else {
				_updatePageOffsetX(_pageOffsetX - (_pageOffsetX < 0 ? -2 : 2));
			}
		} else {
			_updatePageOffsetX((int) (_pageOffsetX * speed));
		}

		if (Math.abs(_pageOffsetY) * (1 - speed) < 2) {
			if (Math.abs(_pageOffsetY) < 2) {
				_updatePageOffsetY(0);
			} else {
				_updatePageOffsetY(_pageOffsetY - (_pageOffsetY < 0 ? -2 : 2));
			}
		} else {
			_updatePageOffsetY((int) (_pageOffsetY * speed));
		}
		markDirtyView();
	}

	private void drawIndicator(XulDC dc, int xBase, int yBase) {
		if (_indicator == null) {
			return;
		}
		int counter = _contents.size();
		dc.save();
		dc.translate(_screenX + xBase, _screenY + yBase);

		RectF thisRc = getAnimRect();
		if (_isVertical) {
			int indicatorGap = _indicator.gap;
			int lineWidth = (int) Math.max(_indicator.focusCx, _indicator.normalCx);
			int totalHeight = (int) ((counter - 1) * (indicatorGap + _indicator.normalCy + 2 * _indicator.normalPadding) + (_indicator.focusCy + 2 * _indicator.focusPadding));
			int yStart = (int) ((XulUtils.calRectHeight(thisRc) - totalHeight - _padding.top - _padding.bottom) * _indicator.yAlign + thisRc.top + _padding.top);
			int xPos = (int) ((XulUtils.calRectWidth(thisRc) - lineWidth - _padding.left - _padding.right) * _indicator.xAlign + thisRc.left + _padding.left);
			Paint defSolidPaint = _ctx.getDefSolidPaint();
			for (int i = 0; i < _contents.size(); i++) {
				indicatorStyle style;
				IndicatorDrawableInfo imgInfo;
				float cx;
				float cy;
				float padding;
				int color;
				if (i == _curPage) {
					defSolidPaint.setColor(_indicator.focusColor);
					style = _indicator.focusStyle;
					cx = _indicator.focusCx;
					cy = _indicator.focusCy;
					padding = _indicator.focusPadding;
					imgInfo = _indicator.focusImage;
					color = _indicator.focusColor;
				} else {
					defSolidPaint.setColor(_indicator.normalColor);
					style = _indicator.normalStyle;
					cx = _indicator.normalCx;
					cy = _indicator.normalCy;
					padding = _indicator.normalPadding;
					imgInfo = _indicator.normalImage;
					color = _indicator.normalColor;
				}
				if (style == indicatorStyle.STYLE_DOT) {
					defSolidPaint.setColor(color);
					dc.drawCircle(xPos + lineWidth * _indicator.indicatorAlign, yStart + cy / 2 + padding, cx / 2, defSolidPaint);
				} else if (style == indicatorStyle.STYLE_DASH) {
					float x = xPos + (lineWidth - cx) * _indicator.indicatorAlign;
					float y = yStart + padding;
					float radius = Math.min(cx / 2, cy / 2);
					defSolidPaint.setColor(color);
					dc.drawRoundRect(x, y, cx, cy, radius, radius, defSolidPaint);
				} else if (style == indicatorStyle.STYLE_IMAGE) {
					if (imgInfo != null && imgInfo._bmp != null) {
						float x = xPos + (lineWidth - cx) * _indicator.indicatorAlign;
						float y = yStart + padding;
						dc.drawBitmap(imgInfo._bmp, x, y, cx, cy, _ctx.getDefPicPaint());
					}
				}
				yStart += cy + 2 * padding;
				yStart += indicatorGap;
			}
		} else {
			int indicatorGap = _indicator.gap;
			int lineHeight = (int) Math.max(_indicator.focusCy, _indicator.normalCy);
			int totalWidth = (int) ((counter - 1) * (indicatorGap + _indicator.normalCx + 2 * _indicator.normalPadding) + (_indicator.focusCx + 2 * _indicator.focusPadding));
			int xStart = (int) ((XulUtils.calRectWidth(thisRc) - totalWidth - _padding.left - _padding.right) * _indicator.xAlign + thisRc.left + _padding.left);
			int yPos = (int) ((XulUtils.calRectHeight(thisRc) - lineHeight - _padding.top - _padding.bottom) * _indicator.yAlign + thisRc.top + _padding.top);
			Paint defSolidPaint = _ctx.getDefSolidPaint();
			for (int i = 0; i < _contents.size(); i++) {
				indicatorStyle style;
				IndicatorDrawableInfo imgInfo;
				float cx;
				float cy;
				float padding;
				int color;
				if (i == _curPage) {
					defSolidPaint.setColor(_indicator.focusColor);
					style = _indicator.focusStyle;
					cx = _indicator.focusCx;
					cy = _indicator.focusCy;
					padding = _indicator.focusPadding;
					imgInfo = _indicator.focusImage;
					color = _indicator.focusColor;
				} else {
					defSolidPaint.setColor(_indicator.normalColor);
					style = _indicator.normalStyle;
					cx = _indicator.normalCx;
					cy = _indicator.normalCy;
					padding = _indicator.normalPadding;
					imgInfo = _indicator.normalImage;
					color = _indicator.normalColor;
				}
				if (style == indicatorStyle.STYLE_DOT) {
					defSolidPaint.setColor(color);
					dc.drawCircle(xStart + cx / 2 + padding, yPos + lineHeight * _indicator.indicatorAlign, cx / 2, defSolidPaint);
				} else if (style == indicatorStyle.STYLE_DASH) {
					float x = xStart + padding;
					float y = yPos + (lineHeight - cy) * _indicator.indicatorAlign;
					float radius = Math.min(cx / 2, cy / 2);
					defSolidPaint.setColor(color);
					dc.drawRoundRect(x, y, cx, cy, radius, radius, defSolidPaint);
				} else if (style == indicatorStyle.STYLE_IMAGE) {
					if (imgInfo != null && imgInfo._bmp != null) {
						float x = xStart + padding;
						float y = yPos + (lineHeight - cy) * _indicator.indicatorAlign;
						dc.drawBitmap(imgInfo._bmp, x, y, cx, cy, _ctx.getDefPicPaint());
					}
				}
				xStart += cx + 2 * padding;
				xStart += indicatorGap;
			}
		}

		dc.restore();
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_NOFOCUS | XulFocus.MODE_NEARBY | XulFocus.MODE_PRIORITY;
	}

	@Override
	public XulView postFindFocus(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		if (!_switchingByFocus) {
			return null;
		}
		if (_contents.size() <= 1) {
			return null;
		}
		assert _area.findChildPos(src) >= 0;
		if (!_isVertical) {
			if (direction == XulLayout.FocusDirection.MOVE_RIGHT) {
				return slideRight(srcRc, src);
			}
			if (direction == XulLayout.FocusDirection.MOVE_LEFT) {
				return slideLeft(srcRc, src);
			}
		} else {
			if (direction == XulLayout.FocusDirection.MOVE_UP) {
				return slideUp(srcRc, src);
			}
			if (direction == XulLayout.FocusDirection.MOVE_DOWN) {
				return slideDown(srcRc, src);
			}
		}
		return null;
	}

	private void cleanPageCache() {
		if (_autoImageGCLevel >= 0) {
			imageGC(_autoImageGCLevel);
		}
		synchronized (_pendingItems) {
			cleanPendItems(_pendingItems);
		}
	}

	XulWorker.DrawableItem getIndicatorsImageItem() {
		if (_indicator == null) {
			return null;
		}
		long timestamp = XulUtils.timestamp();
		IndicatorDrawableInfo normalImage = _indicator.normalImage;
		IndicatorDrawableInfo focusImage = _indicator.focusImage;
		while (normalImage != focusImage && normalImage != null) {
			IndicatorDrawableInfo imageInfo = normalImage;
			if (imageInfo._isLoading) {
				break;
			}
			if (imageInfo._bmp != null) {
				break;
			}
			if (TextUtils.isEmpty(imageInfo._url)) {
				break;
			}
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
				break;
			}
			imageInfo.width = imageInfo._width;
			imageInfo.height = imageInfo._height;
			imageInfo._isLoading = true;
			imageInfo.url = imageInfo._url;
			return imageInfo;
		}

		while (focusImage != null) {
			IndicatorDrawableInfo imageInfo = focusImage;
			if (imageInfo._isLoading) {
				break;
			}
			if (imageInfo._bmp != null) {
				break;
			}
			if (TextUtils.isEmpty(imageInfo._url)) {
				break;
			}
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
				break;
			}
			imageInfo.width = imageInfo._width;
			imageInfo.height = imageInfo._height;
			imageInfo._isLoading = true;
			imageInfo.url = imageInfo._url;
			return imageInfo;
		}
		return null;
	}

	@Override
	public XulWorker.DrawableItem collectPendingImageItem() {
		XulWorker.DrawableItem myPendingDrawableItem = super.collectPendingImageItem();
		int pageNum = _contents.size();
		if (myPendingDrawableItem != null || _contents == null || _contents.isEmpty() || _curPage >= pageNum) {
			return myPendingDrawableItem;
		}

		XulWorker.DrawableItem indicatorsDrawableItem = getIndicatorsImageItem();
		if (indicatorsDrawableItem != null) {
			return indicatorsDrawableItem;
		}

		return null;
	}

	private XulView slideRight(RectF srcRc, XulView src) {
		int lastPage = _curPage;
		++_curPage;
		if (_curPage >= _contents.size()) {
			if (_isLoopMode) {
				_curPage = 0;
			} else {
				--_curPage;
				return null;
			}
		}
		RectF rect = new RectF(srcRc);
		XulView lastPageView = _contents.get(lastPage);
		XulView curPageView = _contents.get(_curPage);
		XulView newFocus;

		if (curPageView instanceof XulArea) {
			RectF focusRc = curPageView.getFocusRc();
			rect.offsetTo(-MAX_PAGE_SIZE, rect.top);

			float width = XulUtils.calRectWidth(focusRc);
			_updatePageOffsetX(width);

			XulArea.rightFocusFilter rightFocusFilter = new XulArea.rightFocusFilter(src, rect);
			lastPageView.setEnabled(false);
			curPageView.setEnabled(true);
			newFocus = XulArea.findSubFocusByFilter((XulArea) curPageView, rightFocusFilter);
			if (newFocus == null) {
				rect.top = focusRc.top;
				rect.bottom = focusRc.bottom;
				newFocus = XulArea.findSubFocusByFilter((XulArea) curPageView, rightFocusFilter);
			}
		} else {
			RectF focusRc = curPageView.getFocusRc();
			newFocus = curPageView;
			_updatePageOffsetX(XulUtils.calRectWidth(focusRc));
			lastPageView.setEnabled(false);
			curPageView.setEnabled(true);
		}
		cleanPageCache();
		notifyPageChanged(_curPage);
		return newFocus;
	}

	private XulView slideLeft(RectF srcRc, XulView src) {
		int lastPage = _curPage;
		--_curPage;
		if (_curPage < 0) {
			if (_isLoopMode) {
				_curPage += _contents.size();
			} else {
				_curPage = 0;
				return null;
			}
		}
		RectF rect = new RectF(srcRc);
		XulView lastPageView = _contents.get(lastPage);
		XulView curPageView = _contents.get(_curPage);
		XulView newFocus;

		if (curPageView instanceof XulArea) {
			RectF focusRc = curPageView.getFocusRc();
			rect.offsetTo(MAX_PAGE_SIZE, rect.top);

			_updatePageOffsetX(-XulUtils.calRectWidth(focusRc));

			XulArea.leftFocusFilter leftFocusFilter = new XulArea.leftFocusFilter(src, rect);
			lastPageView.setEnabled(false);
			curPageView.setEnabled(true);
			newFocus = XulArea.findSubFocusByFilter((XulArea) curPageView, leftFocusFilter);
			if (newFocus == null) {
				rect.top = focusRc.top;
				rect.bottom = focusRc.bottom;
				newFocus = XulArea.findSubFocusByFilter((XulArea) curPageView, leftFocusFilter);
			}
		} else {
			RectF focusRc = curPageView.getFocusRc();
			newFocus = curPageView;
			_updatePageOffsetX(-XulUtils.calRectWidth(focusRc));
			lastPageView.setEnabled(false);
			curPageView.setEnabled(true);
		}
		cleanPageCache();
		notifyPageChanged(_curPage);
		return newFocus;
	}

	private XulView slideDown(RectF srcRc, XulView src) {
		int lastPage = _curPage;
		++_curPage;
		if (_curPage >= _contents.size()) {
			if (_isLoopMode) {
				_curPage = 0;
			} else {
				--_curPage;
				return null;
			}
		}
		RectF rect = new RectF(srcRc);
		XulView lastPageView = _contents.get(lastPage);
		XulView curPageView = _contents.get(_curPage);
		XulView newFocus;

		if (curPageView instanceof XulArea) {
			RectF focusRc = curPageView.getFocusRc();
			rect.offsetTo(rect.left, -MAX_PAGE_SIZE);

			_updatePageOffsetY(XulUtils.calRectHeight(focusRc));

			XulArea.bottomFocusFilter bottomFocusFilter = new XulArea.bottomFocusFilter(src, rect);
			lastPageView.setEnabled(false);
			curPageView.setEnabled(true);
			newFocus = XulArea.findSubFocusByFilter((XulArea) curPageView, bottomFocusFilter);
			if (newFocus == null) {
				rect.left = focusRc.left;
				rect.right = focusRc.right;
				newFocus = XulArea.findSubFocusByFilter((XulArea) curPageView, bottomFocusFilter);
			}
		} else {
			RectF focusRc = curPageView.getFocusRc();
			newFocus = curPageView;
			lastPageView.setEnabled(false);
			curPageView.setEnabled(true);
			_updatePageOffsetY(XulUtils.calRectHeight(focusRc));
		}
		cleanPageCache();
		notifyPageChanged(_curPage);
		return newFocus;
	}

	private XulView slideUp(RectF srcRc, XulView src) {
		int lastPage = _curPage;
		--_curPage;
		if (_curPage < 0) {
			if (_isLoopMode) {
				_curPage += _contents.size();
			} else {
				_curPage = 0;
				return null;
			}
		}
		RectF rect = new RectF(srcRc);
		XulView lastPageView = _contents.get(lastPage);
		XulView curPageView = _contents.get(_curPage);
		XulView newFocus;

		if (curPageView instanceof XulArea) {
			RectF focusRc = curPageView.getFocusRc();
			rect.offsetTo(rect.left, MAX_PAGE_SIZE);

			_updatePageOffsetY(-XulUtils.calRectHeight(focusRc));

			XulArea.topFocusFilter topFocusFilter = new XulArea.topFocusFilter(src, rect);
			lastPageView.setEnabled(false);
			curPageView.setEnabled(true);
			newFocus = XulArea.findSubFocusByFilter((XulArea) curPageView, topFocusFilter);
			if (newFocus == null) {
				rect.left = focusRc.left;
				rect.right = focusRc.right;
				newFocus = XulArea.findSubFocusByFilter((XulArea) curPageView, topFocusFilter);
			}
		} else {
			RectF focusRc = curPageView.getFocusRc();
			newFocus = curPageView;
			lastPageView.setEnabled(false);
			curPageView.setEnabled(true);
			_updatePageOffsetY(-XulUtils.calRectHeight(focusRc));
		}
		cleanPageCache();
		notifyPageChanged(_curPage);
		return newFocus;
	}

	@Override
	public boolean hitTest(int event, float x, float y) {
		if (event == XulManager.HIT_EVENT_SCROLL) {
			return super.hitTest(XulManager.HIT_EVENT_DUMMY, x, y);
		}
		return super.hitTest(event, x, y);
	}

	@Override
	public boolean handleScrollEvent(float hScroll, float vScroll) {
		float delta;
		if (_isVertical) {
			delta = vScroll;
			if (delta == 0) {
				delta = hScroll;
			}
		} else {
			delta = vScroll;
			if (delta == 0) {
				delta = vScroll;
			}
		}
		if (delta == 0) {
			return false;
		}
		if (delta > 0) {
			if (_isLoopMode || _curPage > 0) {
				slidePrev();
			} else {
				return false;
			}
		} else {
			if (_isLoopMode || _curPage + 1 < _contents.size()) {
				slideNext();
			} else {
				return false;
			}
		}
		return true;
	}

	Runnable _pageChangedNotifier;

	private void notifyPageChanged(int curPage) {
		if (_contents.isEmpty()) {
			return;
		}
		XulRenderContext renderContext = getRenderContext();
		renderContext.resetUiTaskCollector();
		if (_pageChangedNotifier == null) {
			_pageChangedNotifier = new Runnable() {
				@Override
				public void run() {
					XulPage.invokeActionNoPopup(_view, "pageChanged");
				}
			};
		}
		renderContext.uiRun(_pageChangedNotifier);
	}

	public void slideRight() {
		if (_contents.size() <= 1) {
			return;
		}
		int lastPage = _curPage;
		XulView lastPageView = _contents.get(lastPage);

		if (lastPageView instanceof XulArea) {
			XulLayout rootLayout = _area.getRootLayout();
			XulView focus = rootLayout.getFocus();
			if (lastPageView == focus || ((XulArea) lastPageView).hasChild(focus)) {
				RectF focusRc = focus.getFocusRc();
				slideRight(focusRc, focus);
				return;
			}
		} else if (lastPageView.isFocused()) {
			RectF focusRc = lastPageView.getFocusRc();
			slideRight(focusRc, lastPageView);
			return;
		}

		++_curPage;
		if (_curPage >= _contents.size()) {
			_curPage = 0;
		}
		XulView curPageView = _contents.get(_curPage);
		RectF focusRc = curPageView.getFocusRc();
		_updatePageOffsetX(XulUtils.calRectWidth(focusRc));
		lastPageView.setEnabled(false);
		curPageView.setEnabled(true);
		cleanPageCache();
		notifyPageChanged(_curPage);
	}

	public void slideLeft() {
		if (_contents.size() <= 1) {
			return;
		}
		int lastPage = _curPage;
		XulView lastPageView = _contents.get(lastPage);

		if (lastPageView instanceof XulArea) {
			XulLayout rootLayout = _area.getRootLayout();
			XulView focus = rootLayout.getFocus();
			if (lastPageView == focus || ((XulArea) lastPageView).hasChild(focus)) {
				RectF focusRc = focus.getFocusRc();
				slideLeft(focusRc, focus);
				return;
			}
		} else if (lastPageView.isFocused()) {
			RectF focusRc = lastPageView.getFocusRc();
			slideLeft(focusRc, lastPageView);
			return;
		}

		--_curPage;
		if (_curPage < 0) {
			_curPage += _contents.size();
		}
		XulView curPageView = _contents.get(_curPage);
		RectF focusRc = curPageView.getFocusRc();
		_updatePageOffsetX(-XulUtils.calRectWidth(focusRc));
		lastPageView.setEnabled(false);
		curPageView.setEnabled(true);
		cleanPageCache();
		notifyPageChanged(_curPage);
	}

	public void slideDown() {
		if (_contents.size() <= 1) {
			return;
		}
		int lastPage = _curPage;
		XulView lastPageView = _contents.get(lastPage);

		if (lastPageView instanceof XulArea) {
			XulLayout rootLayout = _area.getRootLayout();
			XulView focus = rootLayout.getFocus();
			if (lastPageView == focus || ((XulArea) lastPageView).hasChild(focus)) {
				RectF focusRc = focus.getFocusRc();
				slideDown(focusRc, focus);
				return;
			}
		} else if (lastPageView.isFocused()) {
			RectF focusRc = lastPageView.getFocusRc();
			slideDown(focusRc, lastPageView);
			return;
		}

		++_curPage;
		if (_curPage >= _contents.size()) {
			_curPage = 0;
		}
		XulView curPageView = _contents.get(_curPage);
		RectF focusRc = curPageView.getFocusRc();
		float height = XulUtils.calRectHeight(focusRc);
		_updatePageOffsetY(height);
		lastPageView.setEnabled(false);
		curPageView.setEnabled(true);
		cleanPageCache();
		notifyPageChanged(_curPage);
	}

	public void slideUp() {
		if (_contents.size() <= 1) {
			return;
		}
		int lastPage = _curPage;
		XulView lastPageView = _contents.get(lastPage);

		if (lastPageView instanceof XulArea) {
			XulLayout rootLayout = _area.getRootLayout();
			XulView focus = rootLayout.getFocus();
			if (lastPageView == focus || ((XulArea) lastPageView).hasChild(focus)) {
				RectF focusRc = focus.getFocusRc();
				slideUp(focusRc, focus);
				return;
			}
		} else if (lastPageView.isFocused()) {
			RectF focusRc = lastPageView.getFocusRc();
			slideUp(focusRc, lastPageView);
			return;
		}

		--_curPage;
		if (_curPage < 0) {
			_curPage += _contents.size();
		}
		XulView curPageView = _contents.get(_curPage);
		RectF focusRc = curPageView.getFocusRc();
		_updatePageOffsetY(-XulUtils.calRectHeight(focusRc));
		lastPageView.setEnabled(false);
		curPageView.setEnabled(true);
		cleanPageCache();
		notifyPageChanged(_curPage);
	}

	public void slideNext() {
		if (_isVertical) {
			slideDown();
		} else {
			slideRight();
		}
	}

	public void slidePrev() {
		if (_isVertical) {
			slideUp();
		} else {
			slideLeft();
		}
	}

	public boolean collectPendingItems(XulTaskCollector xulTaskCollector) {
		if (_isViewChanged() || _rect == null) {
			return true;
		}

		if (_contents == null || _contents.isEmpty()) {
			return true;
		}

		final XulView view = _contents.get(_curPage);

		ArrayList<XulTaskCollector.XulVolatileReference<XulView>> pendingItems = _pendingItems;
		synchronized (pendingItems) {
			cleanPendItems(pendingItems);
			pendingItems.add(xulTaskCollector.addVolatilePendingItem(view));

			int pageNum = _contents.size();
			if (pageNum <= 1) {
				return true;
			}

			for (int offset = 1; offset <= _preloadPages; ++offset) {
				int prevPage = (_curPage - offset + pageNum) % pageNum;
				int nextPage = (_curPage + offset) % pageNum;

				final XulView nextView = _contents.get(nextPage);
				pendingItems.add(xulTaskCollector.addVolatilePendingItem(nextView));
				if (nextPage != prevPage) {
					final XulView prevView = _contents.get(prevPage);
					pendingItems.add(xulTaskCollector.addVolatilePendingItem(prevView));
				}
				if (Math.abs(nextPage - prevPage) <= 1) {
					break;
				}
			}
		}
		return true;
	}

	private void cleanPendItems(ArrayList<XulTaskCollector.XulVolatileReference<XulView>> pendingItems) {
		for (int i = 0, pendingItemsSize = pendingItems.size(); i < pendingItemsSize; i++) {
			XulTaskCollector.XulVolatileReference<XulView> ref = pendingItems.get(i);
			ref.invalidate();
		}
		pendingItems.clear();
	}

	public int getPageCount() {
		return _contents.size();
	}

	public int getCurrentPage() {
		return _curPage;
	}

	public boolean setCurrentPage(int page) {
		if (_curPage == page) {
			return true;
		}
		if (page >= getPageCount()) {
			return false;
		}
		XulView lastPage = _contents.get(_curPage);
		XulView curPage = _contents.get(page);
		lastPage.setEnabled(false);
		curPage.setEnabled(true);
		if (lastPage.hasFocus()) {
			lastPage.getRootLayout().killFocus();
		}
		_curPage = page;
		_pageOffsetY = 0;
		_pageOffsetX = 0;
		cleanPageCache();
		notifyPageChanged(_curPage);
		return true;
	}

	public boolean setCurrentPage(XulView view) {
		if (_contents == null || _contents.isEmpty() || _curPage >= _contents.size()) {
			return false;
		}
		for (int i = 0, contentsSize = _contents.size(); i < contentsSize; i++) {
			XulView topView = _contents.get(i);
			if (topView == view || view.isChildOf(topView)) {
				return setCurrentPage(i);
			}
		}
		return false;
	}

	public XulView getCurrentView() {
		if (_contents == null || _contents.isEmpty() || _curPage >= _contents.size()) {
			return null;
		}
		XulView page = _contents.get(_curPage);
		return page;
	}

	public ArrayList<XulView> getAllChildViews() {
		if (_contents == null || _contents.isEmpty()) {
			return null;
		}
		return _contents;
	}

	public void syncPages() {
		collectContent();
	}

	@Override
	public void destroy() {
		clearFlipAnimation();
		super.destroy();
	}

	@Override
	public void onVisibilityChanged(boolean isVisible, XulView eventSource) {
		super.onVisibilityChanged(isVisible, eventSource);
		XulAreaChildrenVisibleChangeNotifier notifier = XulAreaChildrenVisibleChangeNotifier.getNotifier();
		notifier.begin(isVisible, (XulArea) eventSource);
		_area.eachChild(notifier);
		notifier.end();
	}

	public void imageGC(int range) {
		if (_contents == null || _contents.isEmpty() || _curPage >= _contents.size()) {
			return;
		}
		int pageNum = _contents.size();
		int first = 0, last = 0;
		if (range <= 0) {
			first = 0;
			last = pageNum;
		} else {
			if (range > pageNum / 2) {
				range = pageNum / 2;
			}
			first = _curPage + range + 1;
			last = _curPage - range;
			while (last < 0) {
				last += pageNum;
			}
		}

		do {
			first %= pageNum;
			if (first == last) {
				break;
			}
			_contents.get(first).cleanImageItems();
			first++;
			if (first == last) {
				break;
			}
		} while (true);
	}

	protected class LayoutContainer extends XulViewContainerBaseRender.LayoutContainer {
		@Override
		public int prepare() {
			super.prepare();
			if (!_isLayoutChangedByChild()) {
				syncDirection();
			}
			collectContent();
			return 0;
		}

		@Override
		public int getChildNum() {
			if (_contents == null || _contents.isEmpty()) {
				return 0;
			}
			return _contents.size();
		}

		@Override
		public XulLayoutHelper.ILayoutElement getChild(int idx) {
			if (idx < 0 || _contents == null || _contents.isEmpty()) {
				return null;
			}
			if (idx >= _contents.size()) {
				return null;
			}
			XulView xulView = _contents.get(idx);
			XulViewRender render = xulView.getRender();
			if (render == null) {
				return null;
			}
			return render.getLayoutElement();
		}

		@Override
		public XulLayoutHelper.ILayoutElement getVisibleChild(int idx) {
			if (idx < 0 || _contents == null || _contents.isEmpty()) {
				return null;
			}
			int pageCount = _contents.size();
			int offset = Math.min((pageCount - idx + _curPage) % pageCount, (pageCount + idx - _curPage) % pageCount);
			if (offset > Math.max(_preloadPages, 1)) {
				return null;
			}
			if (idx >= pageCount) {
				return null;
			}

			XulView xulView = _contents.get(idx);
			XulViewRender render = xulView.getRender();
			if (render == null) {
				return null;
			}
			return render.getLayoutElement();
		}

		@Override
		public int getAllVisibleChildren(XulSimpleArray<XulLayoutHelper.ILayoutElement> array) {
			if (_contents == null || _contents.isEmpty()) {
				return 0;
			}

			int pageCount = _contents.size();
			int firstItem = 0, lastItem = pageCount;
			int preloadPages = Math.max(_preloadPages, 1);
			if (preloadPages * 2 + 1 >= pageCount) {
				firstItem = 0;
				lastItem = pageCount;
			} else {
				firstItem = (_curPage - preloadPages + pageCount) % pageCount;
				lastItem = (_curPage + preloadPages) % pageCount + 1;
			}

			while (firstItem != lastItem) {
				firstItem %= pageCount;

				XulView xulView = _contents.get(firstItem);
				XulViewRender render = xulView.getRender();
				if (render != null) {
					array.add(render.getLayoutElement());
				}

				++firstItem;
			}

			return array.size();
		}
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutContainer();
	}
}
