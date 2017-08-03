package com.starcor.xul.Render;

import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;

import com.starcor.xul.Events.XulStateChangeEvent;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.Prop.XulAction;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulFocus;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Prop.XulStyle;
import com.starcor.xul.Render.Components.BaseScrollBar;
import com.starcor.xul.Render.Effect.SimpleTransformAnimation;
import com.starcor.xul.Render.Transform.ITransformer;
import com.starcor.xul.Utils.XulAreaChildrenRender;
import com.starcor.xul.Utils.XulAreaChildrenVisibleChangeNotifier;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.Utils.XulRenderDrawableItem;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulLayout;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulPage;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulTaskCollector;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;
import com.starcor.xul.XulWorker;

/**
 * Created by hy on 2014/5/12.
 */
public class XulSliderAreaRender extends XulViewContainerBaseRender {
	private static final String TAG = XulSliderAreaRender.class.getSimpleName();

	public static void register() {
		XulRenderFactory.registerBuilder("area", "slider", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				return new XulSliderAreaRender(ctx, (XulArea) view);
			}
		});
	}

	private class _SliderIndicator extends XulRenderDrawableItem {
		String _url;
		volatile boolean _isLoading = false;
		volatile boolean _isRecycled = false;
		volatile long _lastDownloadFailedTime = 0;
		int _width = 0;
		int _height = 0;
		float _xAlign;
		float _yAlign;
		XulDrawable _bmp;

		@Override
		public void onImageReady(XulDrawable bmp) {
			if (_isRecycled) {
				return;
			}
			_isLoading = false;
			_bmp = bmp;

			if (bmp == null) {
				_lastDownloadFailedTime = XulUtils.timestamp();
			} else {
				_lastDownloadFailedTime = 0;
			}
			markDirtyView();
			super.onImageReady(bmp);
		}

		public _SliderIndicator(String[] params, float xAlign, float yAlign) {
			update(params, xAlign, yAlign);
		}

		private void update(String[] params, float xAlign, float yAlign) {
			_xAlign = xAlign;
			_yAlign = yAlign;
			String newUrl = _url;

			switch (params.length) {
			case 1:
				newUrl = params[0];
				break;
			case 3:
				_width = XulUtils.tryParseInt(params[0]);
				_height = XulUtils.tryParseInt(params[1]);
				newUrl = params[2];
				break;
			case 5:
				_width = XulUtils.tryParseInt(params[0]);
				_height = XulUtils.tryParseInt(params[1]);
				_xAlign = XulUtils.tryParseFloat(params[2], xAlign);
				_yAlign = XulUtils.tryParseFloat(params[3], yAlign);
				newUrl = params[4];
				break;
			}

			if (newUrl == null || !newUrl.equals(_url)) {
				_url = newUrl;
				_bmp = null;
			}

			if (_width != 0) {
				_width = XulUtils.roundToInt(_width * _ctx.getXScalar());
			}
			if (_height != 0) {
				_height = XulUtils.roundToInt(_height * _ctx.getYScalar());
			}
		}
	}

	public _SliderIndicator _createIndicator(_SliderIndicator oldIndicator, String[] params, float xAlign, float yAlign) {
		if (params == null) {
			return null;
		}
		if (!(params.length == 1 || params.length == 3 || params.length == 5)) {
			return null;
		}

		if (oldIndicator == null) {
			return new _SliderIndicator(params, xAlign, yAlign);
		}
		oldIndicator.update(params, xAlign, yAlign);
		return oldIndicator;
	}

	private class ScrollTransformAnimation extends SimpleTransformAnimation {
		public ScrollTransformAnimation(XulViewRender render) {
			super(render);
		}

		public ScrollTransformAnimation(XulViewRender render, ITransformer aniTransformer) {
			super(render, aniTransformer);
		}

		@Override
		public float getScalar() {
			if (_isVertical) {
				return (float) getYScalar();
			}
			return (float) getXScalar();
		}

		@Override
		public void storeSrc() {
			_srcVal = _scrollPos;
		}

		@Override
		public void storeDest() {
			_destVal = _scrollTargetPos;
		}

		public void offsetVal(float delta) {
			_srcVal += delta;
			_destVal += delta;
			_val += delta;
		}

		@Override
		public void restoreValue() {
			int deltaPos = (int) (_val - _scrollPos);
			if (deltaPos == 0) {
				return;
			}
			_scrollPos += deltaPos;
			if (_isVertical) {
				XulLayoutHelper.offsetChild(getLayoutElement(), 0, deltaPos);
			} else {
				XulLayoutHelper.offsetChild(getLayoutElement(), deltaPos, 0);
			}
			_adjustLoopingContent();
		}

		@Override
		public boolean updateValue(long timestamp) {
			if (_aniTransformer == null) {
				return oldUpdateValue();
			}
			if (XulManager.DEBUG) {
				Log.d(TAG, String.format("XulSlider Scrolling from:%.3f to:%.3f, val:%.3f, %.2f%%", _srcVal, _destVal, _val, _val / (_destVal - _srcVal)));
			}
			return super.updateValue(timestamp);
		}

		@Override
		public void onAnimationStop() {
			super.onAnimationStop();
			_notifyScrollStop();
		}

		private boolean oldUpdateValue() {
			float deltaPos = _scrollTargetPos - _scrollPos;
			if (deltaPos == 0) {
				return true;
			}
			if (Math.abs(deltaPos) >= 4) {
				deltaPos /= 2;
			}
			_val = _scrollPos + deltaPos;
			return false;
		}
	}

	private void _notifyScrollStop() {
		final XulAction scrollStop = _view.getAction(XulPropNameCache.TagId.ACTION_SCROLL_STOPPED);
		if (scrollStop == null) {
			return;
		}
		XulPage ownerPage = _view.getOwnerPage();
		ownerPage.invokeActionNoPopupWithArgs(_view, scrollStop, getScrollPos(), getScrollRange());
	}

	XulAreaChildrenRender childrenRender = new XulAreaChildrenRender();

	boolean _isVertical = false;
	boolean _reverseLayout = false;
	boolean _autoScroll = true;

	// 显示滚动目标偏移
	int _scrollTargetPos = 0;
	int _scrollPos = 0;
	int _contentMaxWidth = 0;
	int _contentMaxHeight = 0;

	float _xAlign = 0.0f;
	float _yAlign = 0.0f;
	int _alignXOffset = 0;
	int _alignYOffset = 0;

	int _focusPaddingLeft = 0;
	int _focusPaddingRight = 0;
	int _focusPaddingTop = 0;
	int _focusPaddingBottom = 0;

	_SliderIndicator _leftIndicator;
	_SliderIndicator _rightIndicator;
	_SliderIndicator _upIndicator;
	_SliderIndicator _downIndicator;
	boolean _leftIndicatorVisible = false;
	boolean _rightIndicatorVisible = false;
	boolean _topIndicatorVisible = false;
	boolean _bottomIndicatorVisible = false;

	boolean _clipChildren = true;
	boolean _clipFocus = true;

	boolean _loopMode = false;

	boolean _lockDynamicFocus = false;   //  保持dynamic焦点元素处于可见区域
	boolean _lockFocus = false;
	boolean _lockFocusDynamically = false;  // 根据焦点元素的索引偏移,动态调整锁定的对齐位置
	float _lockFocusAlignment = Float.NaN;
	float _lockFocusChildAlignment = Float.NaN;

	private void _adjustLoopingContent() {
		if (!_loopMode || _area.getChildNum() < 2) {
			return;
		}

		if (_isVertical) {
			_adjustLoopingContentVertical();
		} else {
			_adjustLoopingContentHorizontal();
		}
	}

	private void _adjustLoopingContentHorizontal() {
		int viewWidth = XulUtils.calRectWidth(_rect);
		if (_padding != null) {
			viewWidth -= _padding.left + _padding.right;
		}
		int contentMaxSize = _contentMaxWidth;
		if (contentMaxSize < viewWidth) {
			return;
		}

		int scrollTargetPos = (viewWidth - contentMaxSize) / 2;

		{
			XulView lastChild = _area.getLastChild();
			XulViewRender render = lastChild.getRender();
			float lastChildWidth = XulUtils.calRectWidth(render._rect);
			Rect margin = render._margin;
			if (margin != null) {
				lastChildWidth += Math.max(margin.left, margin.right);
			}

			while (-_scrollPos <= contentMaxSize && _scrollPos - scrollTargetPos > lastChildWidth) {
				int delta = (int) -lastChildWidth;
				_scrollPos += delta;
				_scrollTargetPos += delta;
				if (_scrollAnimation != null) {
					_scrollAnimation.offsetVal(delta);
				}
				_area.removeChild(lastChild);
				_area.addChild(0, lastChild);
				XulLayoutHelper.offsetBase(lastChild.getRender().getLayoutElement(), -contentMaxSize, 0);
				lastChild = _area.getLastChild();

				render = lastChild.getRender();
				lastChildWidth = XulUtils.calRectWidth(render._rect);
				margin = render._margin;
				if (margin != null) {
					lastChildWidth += Math.max(margin.left, margin.right);
				}
			}
		}
		{
			XulView firstChild = _area.getFirstChild();
			XulViewRender render = firstChild.getRender();
			float firstChildWidth = XulUtils.calRectWidth(render._rect);
			Rect margin = render._margin;
			if (margin != null) {
				firstChildWidth += Math.max(margin.left, margin.right);
			}

			while (_scrollPos <= -firstChildWidth && scrollTargetPos - _scrollPos > firstChildWidth) {
				int delta = (int) firstChildWidth;
				_scrollPos += delta;
				_scrollTargetPos += delta;
				if (_scrollAnimation != null) {
					_scrollAnimation.offsetVal(delta);
				}
				_area.removeChild(firstChild);
				_area.addChild(firstChild);
				XulLayoutHelper.offsetBase(firstChild.getRender().getLayoutElement(), contentMaxSize, 0);
				firstChild = _area.getFirstChild();

				render = firstChild.getRender();
				firstChildWidth = XulUtils.calRectWidth(render._rect);
				margin = render._margin;
				if (margin != null) {
					firstChildWidth += Math.max(margin.left, margin.right);
				}
			}
		}
	}

	private void _adjustLoopingContentVertical() {
		int viewHeight = XulUtils.calRectHeight(_rect);
		if (_padding != null) {
			viewHeight -= _padding.top + _padding.bottom;
		}
		int contentMaxSize = _contentMaxHeight;
		if (contentMaxSize < viewHeight) {
			return;
		}

		int scrollTargetPos = (viewHeight - contentMaxSize) / 2;

		{
			XulView lastChild = _area.getLastChild();
			XulViewRender render = lastChild.getRender();
			float lastChildSize = XulUtils.calRectHeight(render._rect);
			Rect margin = render._margin;
			if (margin != null) {
				lastChildSize += Math.max(margin.top, margin.bottom);
			}

			while (-_scrollPos <= contentMaxSize && _scrollPos - scrollTargetPos > lastChildSize) {
				int delta = (int) -lastChildSize;
				_scrollPos += delta;
				_scrollTargetPos += delta;
				if (_scrollAnimation != null) {
					_scrollAnimation.offsetVal(delta);
				}
				_area.removeChild(lastChild);
				_area.addChild(0, lastChild);
				XulLayoutHelper.offsetBase(lastChild.getRender().getLayoutElement(), 0, -contentMaxSize);
				lastChild = _area.getLastChild();

				render = lastChild.getRender();
				lastChildSize = XulUtils.calRectHeight(render._rect);
				margin = render._margin;
				if (margin != null) {
					lastChildSize += Math.max(margin.top, margin.bottom);
				}
			}
		}
		{
			XulView firstChild = _area.getFirstChild();
			XulViewRender render = firstChild.getRender();
			float firstChildSize = XulUtils.calRectHeight(render._rect);
			Rect margin = render._margin;
			if (margin != null) {
				firstChildSize += Math.max(margin.top, margin.bottom);
			}

			while (_scrollPos <= -firstChildSize && scrollTargetPos - _scrollPos > firstChildSize) {
				int delta = (int) firstChildSize;
				_scrollPos += delta;
				_scrollTargetPos += delta;
				if (_scrollAnimation != null) {
					_scrollAnimation.offsetVal(delta);
				}
				_area.removeChild(firstChild);
				_area.addChild(firstChild);
				XulLayoutHelper.offsetBase(firstChild.getRender().getLayoutElement(), 0, contentMaxSize);
				firstChild = _area.getFirstChild();

				render = firstChild.getRender();
				firstChildSize = XulUtils.calRectHeight(render._rect);
				margin = render._margin;
				if (margin != null) {
					firstChildSize += Math.max(margin.top, margin.bottom);
				}
			}
		}
	}

	private boolean _updateAndNotifyIndicatorChanged(boolean oldValue, final boolean newValue, final String indicatorName) {
		if (oldValue == newValue) {
			return oldValue;
		}
		if (_view.getAction("indicatorChanged") != null) {
			getRenderContext().uiRun(new Runnable() {
				@Override
				public void run() {
					_view.getOwnerPage().invokeActionNoPopupWithArgs(_view, "indicatorChanged", indicatorName, newValue);
				}
			});
		}
		return newValue;
	}

	BaseScrollBar _scrollBar = null;
	BaseScrollBar.ScrollBarHelper _scrollBarHelper;

	public XulSliderAreaRender(XulRenderContext ctx, XulArea area) {
		super(ctx, area);
	}

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		super.syncData();
		double xScalar = _ctx.getXScalar();
		double yScalar = _ctx.getYScalar();

		XulAttr attrAutoScroll = _area.getAttr(XulPropNameCache.TagId.AUTO_SCROLL);
		if (attrAutoScroll != null && !((XulPropParser.xulParsedProp_booleanValue) attrAutoScroll.getParsedValue()).val) {
			_autoScroll = false;
		} else {
			_autoScroll = true;
		}

		if (_autoScroll) {
			XulAttr lockFocusAttr = _area.getAttr(XulPropNameCache.TagId.LOCK_FOCUS);
			if (lockFocusAttr != null) {
				XulPropParser.xulParsedProp_FloatArray lockFocusVals = lockFocusAttr.getParsedValue();
				if (lockFocusVals == null || lockFocusVals.getLength() < 1) {
					_lockFocus = false;
					_lockFocusDynamically = false;
				} else {
					_lockFocus = true;
					_lockFocusAlignment = lockFocusVals.tryGetVal(0, Float.NaN);
					_lockFocusChildAlignment = lockFocusVals.tryGetVal(1, Float.NaN);
					_lockFocusDynamically = (_lockFocusAlignment == -1);
				}
			} else {
				_lockFocus = false;
				_lockFocusDynamically = false;
			}
		} else {
			_lockFocus = false;
			_lockFocusDynamically = false;
		}

		XulAttr lockDynamicFocus = _area.getAttr(XulPropNameCache.TagId.LOCK_DYNAMIC_FOCUS);
		if (lockDynamicFocus != null) {
			_lockDynamicFocus = "true".equals(lockDynamicFocus.getStringValue());
		} else {
			_lockDynamicFocus = false;
		}

		XulAttr attrLoop = _area.getAttr(XulPropNameCache.TagId.LOOP);
		boolean newLoopMode = false;
		if (attrLoop != null && ((XulPropParser.xulParsedProp_booleanValue) attrLoop.getParsedValue()).val) {
			newLoopMode = true;
		}
		if (_loopMode != newLoopMode) {
			_loopMode = newLoopMode;
			if (_loopMode) {
				setUpdateLayout();
			}
		}

		XulAttr attrScrollbar = _area.getAttr(XulPropNameCache.TagId.SCROLLBAR);
		if (attrScrollbar == null || attrScrollbar.getValue() == null) {
			_scrollBar = XulRenderFactory.buildScrollBar(_scrollBar, "", _getScrollBarHelper(), this);
		} else {
			_scrollBar = XulRenderFactory.buildScrollBar(_scrollBar, attrScrollbar.getStringValue(), _getScrollBarHelper(), this);
		}

		XulAttr indicatorAttr = _area.getAttr(XulPropNameCache.TagId.INDICATOR);
		if (indicatorAttr == null || ((XulPropParser.xulParsedProp_booleanValue) indicatorAttr.getParsedValue()).val == false) {
			_leftIndicator = null;
			_rightIndicator = null;
			_upIndicator = null;
			_downIndicator = null;
		} else {
			String[] leftParams = _area.getAttrString(XulPropNameCache.TagId.INDICATOR_LEFT).split(",");
			_leftIndicator = _createIndicator(_leftIndicator, leftParams, 0.0f, 0.5f);

			String[] rightParams = _area.getAttrString(XulPropNameCache.TagId.INDICATOR_RIGHT).split(",");
			_rightIndicator = _createIndicator(_rightIndicator, rightParams, 1.0f, 0.5f);

			String[] upParams = _area.getAttrString(XulPropNameCache.TagId.INDICATOR_UP).split(",");
			_upIndicator = _createIndicator(_upIndicator, upParams, 0.5f, 0.0f);

			String[] downParams = _area.getAttrString(XulPropNameCache.TagId.INDICATOR_DOWN).split(",");
			_downIndicator = _createIndicator(_downIndicator, downParams, 0.5f, 1.0f);
		}

		XulStyle focusPadding = _area.getStyle(XulPropNameCache.TagId.PREFERRED_FOCUS_PADDING);
		if (focusPadding != null) {
			XulPropParser.xulParsedProp_PaddingMargin focusPaddingVal = focusPadding.getParsedValue();
			_focusPaddingTop = XulUtils.roundToInt(focusPaddingVal.top * yScalar);
			_focusPaddingLeft = XulUtils.roundToInt(focusPaddingVal.left * xScalar);
			_focusPaddingRight = XulUtils.roundToInt(focusPaddingVal.right * xScalar);
			_focusPaddingBottom = XulUtils.roundToInt(focusPaddingVal.bottom * yScalar);
		} else {
			_focusPaddingTop = 0;
			_focusPaddingLeft = 0;
			_focusPaddingRight = 0;
			_focusPaddingBottom = 0;
		}

		String clipChildren = _area.getStyleString(XulPropNameCache.TagId.CLIP_CHILDREN);
		if ("false".equals(clipChildren)) {
			_clipChildren = false;
		} else {
			_clipChildren = true;
			String clipFocus = _area.getStyleString(XulPropNameCache.TagId.CLIP_FOCUS);
			if ("false".equals(clipFocus)) {
				_clipFocus = false;
			} else {
				_clipFocus = true;
			}
		}
		startAnimation();
	}

	private BaseScrollBar.ScrollBarHelper _getScrollBarHelper() {
		if (_scrollBarHelper == null) {
			_scrollBarHelper = new BaseScrollBar.ScrollBarHelper() {
				@Override
				public boolean isVertical() {
					return _isVertical;
				}

				@Override
				public int getScrollPos() {
					return _scrollPos;
				}

				@Override
				public int getContentWidth() {
					return _contentMaxWidth;
				}

				@Override
				public int getContentHeight() {
					return _contentMaxHeight;
				}
			};
		}
		return _scrollBarHelper;
	}

	private void syncAlignInfo() {
		if (!_isViewChanged()) {
			return;
		}
		XulAttr alignAttr = _area.getAttr(XulPropNameCache.TagId.ALIGN);
		_xAlign = 0.0f;
		_yAlign = 0.0f;
		if (alignAttr != null) {
			XulPropParser.xulParsedProp_FloatArray align = alignAttr.getParsedValue();
			int length = align.getLength();
			if (length == 1) {
				_xAlign = _yAlign = align.tryGetVal(0, 0.0f);
			} else if (length == 2) {
				_xAlign = align.tryGetVal(0, 0.0f);
				_yAlign = align.tryGetVal(1, 0.0f);
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
			_reverseLayout = direction.reverse;
		}
	}

	public void drawIndicator(_SliderIndicator indicator, XulDC dc, Rect rect, int xBase, int yBase) {
		XulDrawable bmp = indicator._bmp;
		if (bmp == null || bmp.isRecycled()) {
			bmp = XulWorker.loadDrawableFromCache(indicator._url);
		}
		if (bmp == null) {
			return;
		}

		int width = bmp.getWidth();
		int height = bmp.getHeight();
		int xOffset = XulUtils.roundToInt((XulUtils.calRectWidth(_rect) - width) * indicator._xAlign);
		int yOffset = XulUtils.roundToInt((XulUtils.calRectHeight(_rect) - height) * indicator._yAlign);
		dc.drawBitmap(bmp, _screenX + _rect.left + xBase + xOffset, _screenY + _rect.top + yBase + yOffset, _ctx.getDefPicPaint());
	}

	@Override
	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		super.draw(dc, rect, xBase, yBase);
		dc.save();
		RectF focusRect = getFocusRect();
		focusRect.left += _padding.left + xBase;
		focusRect.top += _padding.top + yBase;
		focusRect.right += xBase - _padding.right;
		focusRect.bottom += yBase - _padding.bottom;

		// focusRect may changed during the children's drawing phase
		// we must judge the indicator's visibility before drawing children
		boolean bottomVisible = XulUtils.calRectHeight(focusRect) - _scrollTargetPos < _contentMaxHeight;
		boolean rightVisible = XulUtils.calRectWidth(focusRect) - _scrollTargetPos < _contentMaxWidth;

		if (_clipChildren) {
			dc.clipRect(focusRect);
		}
		childrenRender.init(dc, rect, xBase, yBase);
		_area.eachView(childrenRender);

		if (_clipChildren && _clipFocus) {
			int zIndex = getZIndex() - 1;
			if (zIndex < 0) {
				zIndex = 0;
			}
			dc.doPostDraw(zIndex, _area);
		}
		dc.restore();

		if (_isVertical) {
			boolean topVisible = _scrollTargetPos != 0;
			_topIndicatorVisible = _updateAndNotifyIndicatorChanged(_topIndicatorVisible, topVisible, "top");
			_bottomIndicatorVisible = _updateAndNotifyIndicatorChanged(_bottomIndicatorVisible, bottomVisible, "bottom");
			if (_upIndicator != null && topVisible) {
				drawIndicator(_upIndicator, dc, rect, xBase, yBase);
			}
			if (_downIndicator != null && bottomVisible) {
				drawIndicator(_downIndicator, dc, rect, xBase, yBase);
			}
		} else {
			boolean leftVisible = _scrollTargetPos != 0;
			_leftIndicatorVisible = _updateAndNotifyIndicatorChanged(_leftIndicatorVisible, leftVisible, "left");
			_rightIndicatorVisible = _updateAndNotifyIndicatorChanged(_rightIndicatorVisible, rightVisible, "right");
			if (_leftIndicator != null && leftVisible) {
				drawIndicator(_leftIndicator, dc, rect, xBase, yBase);
			}
			if (_rightIndicator != null && rightVisible) {
				drawIndicator(_rightIndicator, dc, rect, xBase, yBase);
			}
		}

		if (_scrollBar != null) {
			RectF animRect = getAnimRect();
			_scrollBar.draw(dc, rect, XulUtils.roundToInt(xBase + animRect.left), XulUtils.roundToInt(yBase + animRect.top));
		}

		int scrollPos = getScrollPos();
		int scrollRange = getScrollRange();
		if (scrollPos > scrollRange) {
			scrollTo(scrollRange, true);
		}
	}

	@Override
	public int getDefaultFocusMode() {
		return XulFocus.MODE_NOFOCUS | XulFocus.MODE_NEARBY | XulFocus.MODE_PRIORITY;
	}

	@Override
	public boolean onChildStateChanged(XulStateChangeEvent event) {
		if (_rect == null || _padding == null) {
			return false;
		}
		if (XulView.STATE_FOCUSED == event.state) {
			if (event.adjustFocusView
				&& onChildFocused(event.getEventSource(), _hasAnimation())) {
				return true;
			}
		} else if ("visibilityChanged".equals(event.event)) {
			_setLayoutChanged();
			this.updateParentLayout();
		}
		return false;
	}

	private boolean onChildFocused(XulView focusedView, boolean animation) {
		if (!_autoScroll) {
			return false;
		}
		if (_lockFocusDynamically) {
			_adjustFocusAlignment(focusedView);
		}
		if (_lockFocus && !Float.isNaN(_lockFocusAlignment)) {
			if (Float.isNaN(_lockFocusChildAlignment)) {
				return makeChildVisible(focusedView, _lockFocusAlignment, animation);
			}
			return makeChildVisible(focusedView, _lockFocusAlignment, _lockFocusChildAlignment, animation);
		}
		return makeChildVisible(focusedView, animation);
	}

	private void _adjustFocusAlignment(XulView focusedView) {
		XulArea parent = focusedView.getParent();
		while (parent != _area) {
			if (parent == null) {
				return;
			}
			focusedView = parent;
			parent = parent.getParent();
		}
		RectF sliderFocusRc = getFocusRect();
		RectF focusRc = focusedView.getFocusRc();
		if (_isVertical) {
			float itemVCenter = (focusRc.top + focusRc.bottom) / 2;
			itemVCenter -= sliderFocusRc.top;
			itemVCenter -= _scrollPos;
			_lockFocusAlignment = itemVCenter / _contentMaxHeight;
		} else {
			float itemHCenter = (focusRc.right + focusRc.left) / 2;
			itemHCenter -= sliderFocusRc.left;
			itemHCenter -= _scrollPos;
			_lockFocusAlignment = itemHCenter / _contentMaxWidth;
		}
	}

	public boolean makeChildVisible(XulView view, boolean animation) {
		if (view == null || !view.isChildOf(_area)) {
			return false;
		}
		XulViewRender render = view.getRender();
		if (render == null || render._rect == null || render._padding == null) {
			return false;
		}
		RectF focusRc = view.getFocusRc();
		return makeRectVisible(focusRc, animation);
	}

	/**
	 * make the rect of content display in visible range
	 *
	 * @param rect      content rect relative to screen
	 * @param animation enable animation
	 * @return
	 */
	public boolean makeRectVisible(RectF rect, boolean animation) {
		if (_scrollBar != null) {
			_scrollBar.reset();
		}
		int viewWidth = XulUtils.calRectWidth(_rect) - _padding.left - _padding.right;
		int viewHeight = XulUtils.calRectHeight(_rect) - _padding.top - _padding.bottom;

		if (_isVertical && _contentMaxHeight <= viewHeight) {
			return false;
		}

		if (!_isVertical && _contentMaxWidth <= viewWidth) {
			return false;
		}

		XulUtils.offsetRect(rect, -(_screenX + _rect.left + _padding.left), -(_screenY + _rect.top + _padding.top));
		if (_isVertical) {
			if (rect.top < _focusPaddingTop) {
				float newScrollPos = _scrollPos + (_focusPaddingTop - rect.top);
				if (newScrollPos > 0) {
					scrollContentTo(0, 0, animation);
				} else {
					scrollContentTo(0, newScrollPos, animation);
				}
			} else {
				if (rect.bottom > viewHeight - _focusPaddingBottom) {
					float newScrollPos = _scrollPos + (viewHeight - _focusPaddingBottom - rect.bottom);
					if (newScrollPos + _contentMaxHeight >= viewHeight) {
						scrollContentTo(0, newScrollPos, animation);
					} else {
						scrollContentTo(0, viewHeight - _contentMaxHeight, animation);
					}
				} else {
					return false;
				}
			}
		} else {
			if (rect.left < _focusPaddingLeft) {
				float newScrollPos = _scrollPos + (_focusPaddingLeft - rect.left);
				if (newScrollPos > 0) {
					scrollContentTo(0, 0, animation);
				} else {
					scrollContentTo(newScrollPos, 0, animation);
				}
			} else {
				if (rect.right > viewWidth - _focusPaddingRight) {
					float newScrollPos = _scrollPos + (viewWidth - _focusPaddingRight - rect.right);
					if (newScrollPos + _contentMaxWidth >= viewWidth) {
						scrollContentTo(newScrollPos, 0, animation);
					} else {
						scrollContentTo(viewWidth - _contentMaxWidth, 0, animation);
					}
				} else {
					return false;
				}
			}
		}
		return true;
	}

	public boolean makeChildVisible(XulView view, float align, boolean animation) {
		return makeChildVisible(view, align, Float.NaN, animation);
	}

	public boolean makeChildVisible(XulView view, float align, float alignPoint, boolean animation) {
		XulViewRender render = view.getRender();
		if (render == null || render._rect == null || render._padding == null) {
			return false;
		}

		RectF focusRc = view.getFocusRc();
		return makeRectVisible(focusRc, align, alignPoint, animation);
	}

	public boolean makeRectVisible(RectF rect, float align, float alignPoint, boolean animation) {
		if (_scrollBar != null) {
			_scrollBar.reset();
		}
		int viewWidth = XulUtils.calRectWidth(_rect) - _padding.left - _padding.right;
		int viewHeight = XulUtils.calRectHeight(_rect) - _padding.top - _padding.bottom;

		if (_isVertical && _contentMaxHeight <= viewHeight) {
			return false;
		}

		if (!_isVertical && _contentMaxWidth <= viewWidth) {
			return false;
		}
		XulUtils.offsetRect(rect, -(_screenX + _rect.left + _padding.left), -(_screenY + _rect.top + _padding.top));

		if (align < 0) {
			align = 0;
		} else if (align > 1) {
			align = 1;
		}

		if (_isVertical) {
			float itemHeight = XulUtils.calRectHeight(rect);
			int newTop;
			if (Float.isNaN(alignPoint)) {
				newTop = (int) ((viewHeight - itemHeight) * align);
			} else {
				newTop = (int) (viewHeight * align - itemHeight * alignPoint);
			}
			if (itemHeight > viewHeight) {
				newTop = 0;
			}
			float newScrollPos = _scrollPos + (newTop - rect.top);
			if (newScrollPos > 0) {
				scrollContentTo(0, 0, animation);
			} else if (newScrollPos + _contentMaxHeight >= viewHeight) {
				scrollContentTo(0, newScrollPos, animation);
			} else {
				scrollContentTo(0, viewHeight - _contentMaxHeight, animation);
			}
		} else {
			float itemWidth = XulUtils.calRectWidth(rect);
			int newLeft;
			if (Float.isNaN(alignPoint)) {
				newLeft = (int) ((viewWidth - itemWidth) * align);
			} else {
				newLeft = (int) (viewWidth * align - itemWidth * alignPoint);
			}
			if (itemWidth > viewWidth) {
				newLeft = 0;
			}
			float newScrollPos = _scrollPos + (newLeft - rect.left);
			if (newScrollPos > 0) {
				scrollContentTo(0, 0, animation);
			} else if (newScrollPos + _contentMaxWidth >= viewWidth) {
				scrollContentTo(newScrollPos, 0, animation);
			} else {
				scrollContentTo(viewWidth - _contentMaxWidth, 0, animation);
			}
		}
		return true;
	}

	private void scrollContentTo(float x, float y, boolean animation) {
		if (_isVertical) {
			_scrollTargetPos = XulUtils.roundToInt(y);
		} else {
			_scrollTargetPos = XulUtils.roundToInt(x);
		}
		if (_scrollTargetPos != _scrollPos) {
			if (animation) {
				setUpdateLayout();
			} else {
				int deltaPos = _scrollTargetPos - _scrollPos;
				_scrollPos = _scrollTargetPos;
				if (_isVertical) {
					XulLayoutHelper.offsetChild(getLayoutElement(), 0, deltaPos);
				} else {
					XulLayoutHelper.offsetChild(getLayoutElement(), deltaPos, 0);
				}
				_adjustLoopingContent();
				markDirtyView();
				if (!stopAnimation()) {
					_notifyScrollStop();
				}

			}
		}
	}

	public void activateScrollBar() {
		if (_scrollBar != null) {
			_scrollBar.reset();
		}
	}

	public boolean isVertical() {
		return _isVertical;
	}

	public void scrollTo(int pos, boolean animation) {
		if (pos < 0) {
			pos = 0;
		} else {
			int scrollRange = getScrollRange();
			if (pos > scrollRange) {
				pos = scrollRange;
			}
		}

		if (_isVertical) {
			scrollContentTo(0, -pos, animation);
		} else {
			scrollContentTo(-pos, 0, animation);
		}
	}

	public void scrollByPage(int pages, boolean animation) {
		if (pages == 0) {
			return;
		}
		Rect rect = _rect;
		if (_isVertical) {
			int pageHeight = XulUtils.calRectHeight(rect) - _padding.top - _padding.bottom;
			int scrollDelta = pageHeight * pages;
			int scrollRange = pageHeight - _contentMaxHeight;


			int newTargetPos = _scrollTargetPos - scrollDelta;
			if (newTargetPos < scrollRange) {
				newTargetPos = scrollRange;
			} else if (newTargetPos > 0) {
				newTargetPos = 0;
			}

			if (newTargetPos == _scrollTargetPos) {
				return;
			}

			XulLayout rootLayout = _area.getRootLayout();
			XulView focus = rootLayout.getFocus();
			if (focus != null && focus.isChildOf(_area)) {
				RectF focusRc = focus.getFocusRc();
				int targetDelta = _scrollPos - newTargetPos;
				final RectF sliderFocusRc = _view.getFocusRc();
				XulView newFocus = null;
				if (targetDelta >= 0) {
					float newTop = sliderFocusRc.top + targetDelta - XulUtils.calRectHeight(focusRc);
					if (newTop > focusRc.top) {
						focusRc.offsetTo(focusRc.left, newTop);
						newFocus = _area.findSubFocusByDirection(XulLayout.FocusDirection.MOVE_DOWN, focusRc, focus);
					}
				} else {
					float newTop = sliderFocusRc.left + targetDelta;
					if (newTop < focusRc.top) {
						focusRc.offsetTo(focusRc.left, newTop);
						newFocus = _area.findSubFocusByDirection(XulLayout.FocusDirection.MOVE_UP, focusRc, focus);
					}
				}
				if (newFocus != null) {
					rootLayout.requestFocus(newFocus);
				}
			}

			scrollContentTo(0, newTargetPos, animation);
		} else {
			int pageWidth = XulUtils.calRectWidth(rect) - _padding.left - _padding.right;
			int scrollDelta = pageWidth * pages;
			int scrollRange = pageWidth - _contentMaxWidth;

			int newTargetPos = _scrollTargetPos - scrollDelta;
			if (newTargetPos < scrollRange) {
				newTargetPos = scrollRange;
			} else if (newTargetPos > 0) {
				newTargetPos = 0;
			}

			if (newTargetPos == _scrollTargetPos) {
				return;
			}

			XulLayout rootLayout = _area.getRootLayout();
			XulView focus = rootLayout.getFocus();
			if (focus != null && focus.isChildOf(_area)) {
				RectF focusRc = focus.getFocusRc();
				int targetDelta = _scrollPos - newTargetPos;
				final RectF sliderFocusRc = _view.getFocusRc();
				XulView newFocus = null;
				if (targetDelta >= 0) {
					float newLeft = sliderFocusRc.left + targetDelta - XulUtils.calRectWidth(focusRc);
					if (newLeft > focusRc.left) {
						focusRc.offsetTo(newLeft, focusRc.top);
						newFocus = _area.findSubFocusByDirection(XulLayout.FocusDirection.MOVE_RIGHT, focusRc, focus);
					}
				} else {
					float newLeft = sliderFocusRc.left + targetDelta;
					if (newLeft < focusRc.left) {
						focusRc.offsetTo(newLeft, focusRc.top);
						newFocus = _area.findSubFocusByDirection(XulLayout.FocusDirection.MOVE_LEFT, focusRc, focus);
					}
				}
				if (newFocus != null) {
					rootLayout.requestFocus(newFocus);
				}
			}

			scrollContentTo(newTargetPos, 0, animation);
		}
	}

	private void initScrollAnimation() {
		if (!_hasAnimation()) {
			if (_scrollAnimation != null) {
				removeAnimation(_scrollAnimation);
			}
			return;
		}
		if (_scrollAnimation == null) {
			_scrollAnimation = new ScrollTransformAnimation(this);
		}
		_scrollAnimation.setTransformer(_aniTransformer);
	}

	private ScrollTransformAnimation _scrollAnimation;

	private boolean stopAnimation() {
		if (_scrollAnimation != null && _scrollAnimation.isRunning()) {
			removeAnimation(_scrollAnimation);
			_scrollAnimation.stopAnimation();
			return true;
		}
		return false;
	}

	private void startAnimation() {
		if (_scrollTargetPos == _scrollPos) {
			return;
		}

		if (!_hasAnimation()) {
			int deltaPos = _scrollTargetPos - _scrollPos;
			_scrollPos += deltaPos;
			if (_isVertical) {
				XulLayoutHelper.offsetChild(getLayoutElement(), 0, deltaPos);
			} else {
				XulLayoutHelper.offsetChild(getLayoutElement(), deltaPos, 0);
			}
			_adjustLoopingContent();
			markDirtyView();
			return;
		}

		initScrollAnimation();
		if (_scrollAnimation != null) {
			_scrollAnimation.prepareAnimation(_aniTransformDuration);
			_scrollAnimation.startAnimation();
			addAnimation(_scrollAnimation);
		}
	}

	private boolean isAnimationRunning() {
		return _scrollAnimation != null && _scrollAnimation.isRunning();
	}

	public int getScrollPos() {
		return -_scrollTargetPos;
	}

	public int getScrollDelta() {
		return -(_scrollPos - _scrollTargetPos);
	}

	public int getScrollRange() {
		if (_rect == null) {
			return 0;
		}
		if (_isVertical) {
			int viewHeight = XulUtils.calRectHeight(_rect) - _padding.top - _padding.bottom;

			if (_contentMaxHeight <= viewHeight) {
				return 0;
			}
			return _contentMaxHeight - viewHeight;
		}
		int viewWidth = XulUtils.calRectWidth(_rect) - _padding.left - _padding.right;
		if (_contentMaxWidth <= viewWidth) {
			return 0;
		}
		return _contentMaxWidth - viewWidth;
	}

	@Override
	public boolean hitTest(int event, float x, float y) {
		if (event == XulManager.HIT_EVENT_SCROLL) {
			return super.hitTest(XulManager.HIT_EVENT_DUMMY, x, y);
		}
		return super.hitTest(event, x, y);
	}

	public boolean isScrolling() {
		return _scrollTargetPos != _scrollPos;
	}

	@Override
	public boolean handleScrollEvent(float hScroll, float vScroll) {
		if (_lockFocus) {
			XulLayout rootLayout = _view.getRootLayout();
			XulView focus = rootLayout.getFocus();

			if (focus != null && focus.isChildOf(_view)) {
				RectF rc = focus.getFocusRc();
				XulLayout.FocusDirection direction;

				if (isVertical()) {
					if (vScroll > 0) {
						direction = XulLayout.FocusDirection.MOVE_UP;
					} else {
						direction = XulLayout.FocusDirection.MOVE_DOWN;
					}
				} else {
					if (hScroll > 0) {
						direction = XulLayout.FocusDirection.MOVE_LEFT;
					} else {
						direction = XulLayout.FocusDirection.MOVE_RIGHT;
					}
				}
				XulView nextFocus = _area.findSubFocus(direction, rc, focus);
				if (nextFocus != null) {
					rootLayout.requestFocus(nextFocus);
				}
				return true;
			}
		}

		float delta;
		if (isVertical()) {
			delta = vScroll;
			if (delta == 0) {
				delta = hScroll;
			}
			delta *= getRenderContext().getYScalar() * 32;
		} else {
			delta = vScroll;
			if (delta == 0) {
				delta = vScroll;
			}
			delta *= getRenderContext().getXScalar() * 32;
		}
		if ((delta > 0 && getScrollPos() == 0) || (getScrollPos() >= getScrollRange() && delta < 0)) {
			return false;
		}
		scrollTo((int) (getScrollPos() - delta), true);
		return true;
	}

	@Override
	public XulView preFindFocus(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		if (_isVertical) {
			if (direction == XulLayout.FocusDirection.MOVE_LEFT || direction == XulLayout.FocusDirection.MOVE_RIGHT) {
				return null;
			}
		} else {
			if (direction == XulLayout.FocusDirection.MOVE_UP || direction == XulLayout.FocusDirection.MOVE_DOWN) {
				return null;
			}
		}
		XulView item = src;
		XulArea itemParent = item.getParent();
		XulArea thisArea = _area;
		while (itemParent != thisArea && itemParent != null) {
			item = itemParent;
			itemParent = item.getParent();
		}
		if (itemParent == null) {
			return XulItem.NULL_FOCUS;
		}

		int childPos = thisArea.findChildPos(item);

		int childNum = thisArea.getChildNum();

		int nextDelta;
		if (direction == XulLayout.FocusDirection.MOVE_RIGHT || direction == XulLayout.FocusDirection.MOVE_DOWN) {
			nextDelta = 1;
		} else {
			nextDelta = -1;
		}

		if (_reverseLayout) {
			nextDelta = -nextDelta;
		}

		while (true) {
			childPos += nextDelta;
			if (childPos < 0 || childPos >= childNum) {
				break;
			}
			XulView child = thisArea.getChild(childPos);
			if (child == null || !child.isVisible() || !child.isEnabled()) {
				continue;
			}
			if (child.focusable()) {
				return child;
			}
			if (child instanceof XulArea) {
				XulView focus = ((XulArea) child).findSubFocus(direction, srcRc, src);
				if (focus != null) {
					return focus;
				}
			}
		}

		return XulItem.NULL_FOCUS;
	}

	@Override
	public XulView postFindFocus(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		if (!_area.hasChild(src)) {
			return null;
		}
		if (_area.testFocusModeBits(XulFocus.MODE_LOOP)) {
			RectF newRc = XulDC._tmpFRc0;
			XulUtils.copyRect(srcRc, newRc);
			if (!_isVertical && direction == XulLayout.FocusDirection.MOVE_RIGHT) {
				XulView lastChild = _area.getLastFocusableChild();
				if (lastChild == src || src.isChildOf(lastChild)) {
					XulUtils.offsetRect(newRc, -_contentMaxWidth, 0);
					return _area.findSubFocusByDirection(direction, newRc, src);
				}
			} else if (_isVertical && direction == XulLayout.FocusDirection.MOVE_DOWN) {
				XulView lastChild = _area.getLastFocusableChild();
				if (lastChild == src || src.isChildOf(lastChild)) {
					XulUtils.offsetRect(newRc, 0, -_contentMaxHeight);
					return _area.findSubFocusByDirection(direction, newRc, src);
				}
			} else if (!_isVertical && direction == XulLayout.FocusDirection.MOVE_LEFT) {
				XulView firstChild = _area.getFirstFocusableChild();
				if (firstChild == src || src.isChildOf(firstChild)) {
					XulUtils.offsetRect(newRc, _contentMaxWidth, 0);
					return _area.findSubFocusByDirection(direction, newRc, src);
				}
			} else if (_isVertical && direction == XulLayout.FocusDirection.MOVE_UP) {
				XulView firstChild = _area.getFirstFocusableChild();
				if (firstChild == src || src.isChildOf(firstChild)) {
					XulUtils.offsetRect(newRc, 0, _contentMaxHeight);
					return _area.findSubFocusByDirection(direction, newRc, src);
				}
			}
		}
		return null;
	}

	@Override
	public XulWorker.DrawableItem collectPendingImageItem() {
		_SliderIndicator item;
		item = tryGetPendingImageItem(_downIndicator);
		if (item != null) {
			return item;
		}
		item = tryGetPendingImageItem(_upIndicator);
		if (item != null) {
			return item;
		}
		item = tryGetPendingImageItem(_leftIndicator);
		if (item != null) {
			return item;
		}
		item = tryGetPendingImageItem(_rightIndicator);
		if (item != null) {
			return item;
		}
		return super.collectPendingImageItem();
	}

	@Override
	public boolean collectPendingItems(XulTaskCollector xulTaskCollector) {
		if (_isViewChanged() || _rect == null) {
			return true;
		}
		if (_scrollTargetPos != _scrollPos) {
			// prevent resource loading while scrolling
			return true;
		}
		return false;
	}

	private _SliderIndicator tryGetPendingImageItem(_SliderIndicator item) {
		if (item == null) {
			return null;
		}
		if (item._isLoading) {
			return null;
		}
		if (TextUtils.isEmpty(item._url)) {
			return null;
		}
		if (item._bmp != null && !item._bmp.isRecycled()) {
			return null;
		}
		item._isLoading = true;
		item.url = item._url;
		item.width = item._width;
		item.height = item._height;
		return item;
	}

	@Override
	public void onVisibilityChanged(boolean isVisible, XulView eventSource) {
		super.onVisibilityChanged(isVisible, eventSource);
		XulAreaChildrenVisibleChangeNotifier notifier = XulAreaChildrenVisibleChangeNotifier.getNotifier();
		notifier.begin(isVisible, (XulArea) eventSource);
		_area.eachChild(notifier);
		notifier.end();
		if (!isVisible && _scrollAnimation != null) {
			removeAnimation(_scrollAnimation);
		}
	}

	protected class LayoutContainer extends XulViewContainerBaseRender.LayoutContainer {
		int _lastScrollTargetPos = Integer.MAX_VALUE;

		int _initialContentWidth = Integer.MAX_VALUE;
		int _initialContentHeight = Integer.MAX_VALUE;


		class scheduledLockFocusRunnable implements Runnable {
			boolean animation;

			@Override
			public void run() {
				XulLayout rootLayout = _view.getRootLayout();
				if (rootLayout == null) {
					return;
				}
				XulView focus = rootLayout.getFocus();
				if (_lockDynamicFocus && (focus == null || !focus.isChildOf(_view))) {
					focus = _area.getDynamicFocus();
				}
				if (focus == null) {
					return;
				}
				if (focus.isChildOf(_area)) {
					XulView view = focus;
					while (view != _area && view != null) {
						if (view.getStyle(XulPropNameCache.TagId.LOCK_FOCUS_TARGET) != null) {
							focus = view;
						}
						view = view.getParent();
					}
					onChildFocused(focus, isAnimationRunning() || (animation && _hasAnimation()));
				}

			}
		}

		scheduledLockFocusRunnable _scheduledLockFocus;

		void scheduleLockFocus(boolean animation) {
			if (_scheduledLockFocus == null) {
				_scheduledLockFocus = new scheduledLockFocusRunnable();
			}

			XulRenderContext renderContext = getRenderContext();
			if (renderContext == null) {
				return;
			}
			_scheduledLockFocus.animation = animation;
			renderContext.scheduleLayoutFinishedTask(_scheduledLockFocus);
		}

		@Override
		public int doFinal() {
			int ret = super.doFinal();
			_adjustLoopingContent();
			boolean contentChanged = _isVertical ? (_initialContentHeight != _contentMaxHeight) : (_initialContentWidth != _contentMaxWidth);
			if (_lockFocus || contentChanged) {
				XulLayout rootLayout = _view.getRootLayout();
				XulView focus = rootLayout.getFocus();
				if (_lockDynamicFocus && (focus == null || !focus.isChildOf(_view))) {
					focus = _area.getDynamicFocus();
				}
				if (focus != null && focus.isChildOf(_view)) {
					if (contentChanged) {
						scheduleLockFocus(false);
						return ret;
					} else {
						scheduleLockFocus(true);
					}
				}
			}

			if (_lastScrollTargetPos != _scrollTargetPos || _scrollPos != _scrollTargetPos) {
				_lastScrollTargetPos = _scrollTargetPos;
				startAnimation();
			}
			return ret;
		}

		@Override
		public int prepare() {
			super.prepare();
			_initialContentWidth = _contentMaxWidth;
			_initialContentHeight = _contentMaxHeight;
			if (!_isLayoutChangedByChild()) {
				syncAlignInfo();
				syncDirection();
			}
			return 0;
		}

		@Override
		public int getContentOffsetX() {
			if (!_isVertical) {
				return _scrollPos;
			}
			return 0;
		}

		@Override
		public int getContentOffsetY() {
			if (_isVertical) {
				return _scrollPos;
			}
			return 0;
		}

		@Override
		public int getLayoutMode() {
			if (_isVertical) {
				return _reverseLayout ? XulLayoutHelper.MODE_LINEAR_INVERSE_VERTICAL : XulLayoutHelper.MODE_LINEAR_VERTICAL;
			}
			return _reverseLayout ? XulLayoutHelper.MODE_LINEAR_INVERSE_HORIZONTAL : XulLayoutHelper.MODE_LINEAR_HORIZONTAL;
		}

		@Override
		public int getChildNum() {
			return _area.getChildNum();
		}

		@Override
		public boolean updateContentSize() {
			return true;
		}

		@Override
		public int setContentSize(int w, int h) {
			_contentMaxWidth = w;
			_contentMaxHeight = h;
			return 0;
		}

		@Override
		public int setAlignmentOffset(int x, int y) {
			_alignXOffset = x;
			_alignYOffset = y;
			return 0;
		}

		@Override
		public float getAlignmentX() {
			if (_xAlign != 0) {
				return _xAlign;
			}
			return super.getAlignmentX();
		}

		@Override
		public float getAlignmentY() {
			if (_yAlign != 0) {
				return _yAlign;
			}
			return super.getAlignmentY();
		}

		@Override
		public int getAlignmentOffsetX() {
			return _alignXOffset;
		}

		@Override
		public int getAlignmentOffsetY() {
			return _alignYOffset;
		}

		@Override
		public int getOffsetX() {
			if (!_isVertical) {
				return _scrollPos + _alignXOffset;
			}
			return _alignXOffset;
		}

		@Override
		public int getOffsetY() {
			if (_isVertical) {
				return _scrollPos + _alignYOffset;
			}
			return _alignYOffset;
		}
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutContainer();
	}

}