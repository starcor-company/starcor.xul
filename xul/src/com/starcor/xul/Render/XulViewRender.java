package com.starcor.xul.Render;

import android.graphics.*;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.starcor.xul.Events.XulActionEvent;
import com.starcor.xul.Events.XulStateChangeEvent;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Graphics.XulDrawable;
import com.starcor.xul.*;
import com.starcor.xul.Prop.XulAction;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Prop.XulStyle;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.Effect.QuiverAnimation;
import com.starcor.xul.Render.Effect.SimpleTransformAnimation;
import com.starcor.xul.Render.Effect.TransformAnimation;
import com.starcor.xul.Render.Transform.ITransformer;
import com.starcor.xul.Render.Transform.TransformerFactory;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.Utils.XulRenderDrawableItem;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/12.
 */
public abstract class XulViewRender {
	private static final String TAG = XulViewRender.class.getSimpleName();
	public static final int FLAGS_LAYOUT_INITIALIZED = 0x0001;
	public static final int FLAGS_DATA_INITIALED = 0x0002;
	public static final int FLAGS_PENDING_ITEMS_LOADED = 0x0004;
	public static final int FLAGS_RIGHT_TO_LEFT = 0x0008;
	public static final int FLAGS_INITIALIZED = FLAGS_LAYOUT_INITIALIZED | FLAGS_DATA_INITIALED;

	public static final int FLAGS_LAYOUT_CHANGED = 0x010;
	public static final int FLAGS_CHILDREN_LAYOUT_CHANGED = 0x020;
	public static final int FLAGS_DATA_CHANGED = 0x040;
	public static final int FLAGS_VIEW_CHANGED_MASK = 0xF0;

	public static final int FLAGS_SCALED_RECT_CHANGED = 0x100;
	public static final int FLAGS_KEEP_FOCUS_VISIBLE = 0x200;
	public static final int FLAGS_INITIAL_PRELOAD = 0x400;
	public static final int FLAGS_PRELOAD = 0x800;

	public static final int FLAGS_SIZING_ANIMATION = 0x1000;
	public static final int FLAGS_MOVING_ANIMATION = 0x2000;
	public static final int FLAGS_SCALE_ANIMATION = 0x4000;
	public static final int FLAGS_ANIMATION = 0x8000;

	public static final int FLAGS_VISIBLE = 0x10000;

	public static final int FLAGS_ENABLE = 0x20000;

	public static final int FLAGS_UPDATE_WIDTH = 0x40000;
	public static final int FLAGS_UPDATE_HEIGHT = 0x80000;

	public static final int FLAGS_DRAWING_SKIPPED = 0x100000;
	public static final int FLAGS_USER_FLAGS_BASE = 0x200000;

	protected int _flags = FLAGS_SCALED_RECT_CHANGED | FLAGS_SCALE_ANIMATION | FLAGS_ANIMATION | FLAGS_VISIBLE | FLAGS_ENABLE;

	protected boolean _hasFlags(int flags) {
		return (_flags & flags) == flags;
	}

	protected boolean _hasAnyFlag(int flags) {
		return (_flags & flags) != 0;
	}

	protected void _addFlags(int flags) {
		_flags |= flags;
	}

	protected void _clearFlags(int flags) {
		_flags &= ~flags;
	}

	protected void _modifyFlags(int addFlags, int removeFlags) {
		_flags = (_flags | addFlags) & ~removeFlags;
	}

	protected boolean _isBooting() {
		return (_flags & FLAGS_INITIALIZED) != FLAGS_INITIALIZED;
	}

	protected boolean _isLayoutChanged() {
		return (_flags & (FLAGS_LAYOUT_CHANGED | FLAGS_CHILDREN_LAYOUT_CHANGED)) != 0;
	}

	protected boolean _isLayoutChangedByChild() {
		return (_flags & (FLAGS_LAYOUT_CHANGED | FLAGS_CHILDREN_LAYOUT_CHANGED)) == FLAGS_CHILDREN_LAYOUT_CHANGED;
	}

	protected boolean _isDataChanged() {
		return (_flags & (FLAGS_DATA_CHANGED)) != 0;
	}

	protected boolean _isViewChanged() {
		return (_flags & FLAGS_VIEW_CHANGED_MASK) != 0;
	}

	protected void _setLayoutUpdated() {
		if ((_flags & FLAGS_INITIALIZED) == FLAGS_DATA_INITIALED) {
			_flags = (_flags | FLAGS_LAYOUT_INITIALIZED) & ~(FLAGS_LAYOUT_CHANGED | FLAGS_CHILDREN_LAYOUT_CHANGED);
			_notifyReady();
		} else {
			_flags = (_flags | FLAGS_LAYOUT_INITIALIZED) & ~(FLAGS_LAYOUT_CHANGED | FLAGS_CHILDREN_LAYOUT_CHANGED);
		}
	}

	private void _notifyReady() {
		final XulAction readyAction = _view.getAction(XulPropNameCache.TagId.ACTION_READY);
		if (readyAction != null) {
			getRenderContext().scheduleLayoutFinishedTask(new Runnable() {
				@Override
				public void run() {
					XulPage ownerPage = _view.getOwnerPage();
					ownerPage.invokeActionNoPopup(_view, readyAction);
				}
			});
		}
	}

	protected void _setLayoutChanged() {
		_flags = _flags | FLAGS_LAYOUT_CHANGED;
	}

	protected void _setLayoutChangedByChildren() {
		_flags = _flags | FLAGS_CHILDREN_LAYOUT_CHANGED;
	}

	protected void _setDataUpdated() {
		if ((_flags & FLAGS_INITIALIZED) == FLAGS_LAYOUT_INITIALIZED) {
			_flags = (_flags | FLAGS_DATA_INITIALED) & ~(FLAGS_DATA_CHANGED);
			_notifyReady();
		} else {
			_flags = (_flags | FLAGS_DATA_INITIALED) & ~(FLAGS_DATA_CHANGED);
		}
	}

	protected void _setDataChanged() {
		_flags = _flags | FLAGS_DATA_CHANGED;
	}

	protected void _setScaledRectChanged() {
		_flags = _flags | FLAGS_SCALED_RECT_CHANGED;
	}

	protected void _setScaledRectUpdated() {
		_flags = _flags & ~(FLAGS_SCALED_RECT_CHANGED);
	}

	protected boolean _isScaledRectUpdated() {
		return (_flags & FLAGS_SCALED_RECT_CHANGED) == 0;
	}

	protected void _setKeepFocusVisible(boolean keep) {
		if (keep) {
			_flags = _flags | FLAGS_KEEP_FOCUS_VISIBLE;
		} else {
			_flags = _flags & ~(FLAGS_KEEP_FOCUS_VISIBLE);
		}
	}

	protected boolean _isKeepFocusVisible() {
		return (_flags & FLAGS_KEEP_FOCUS_VISIBLE) != 0;
	}

	protected void _setPendingItemsLoaded() {
		_flags = _flags | FLAGS_PENDING_ITEMS_LOADED;
	}

	protected void _setInitialPreload(boolean preload) {
		if (preload) {
			_flags = _flags | FLAGS_INITIAL_PRELOAD;
		} else {
			_flags = _flags & ~FLAGS_INITIAL_PRELOAD;
		}
	}

	protected void _setPreload(boolean preload) {
		if (preload) {
			_flags = _flags | FLAGS_PRELOAD;
		} else {
			_flags = _flags & ~FLAGS_PRELOAD;
		}
	}

	protected boolean _isInitialPreload() {
		return (_flags & FLAGS_INITIAL_PRELOAD) != 0;
	}

	protected boolean _canPreload() {
		return (_flags & FLAGS_PRELOAD) != 0;
	}

	protected boolean _hasAnimation() {
		return (_flags & FLAGS_ANIMATION) != 0;
	}

	protected boolean _hasScaleAnimation() {
		return (_flags & (FLAGS_ANIMATION | FLAGS_SCALE_ANIMATION)) == (FLAGS_ANIMATION | FLAGS_SCALE_ANIMATION);
	}

	protected boolean _hasSizingAnimation() {
		return (_flags & (FLAGS_ANIMATION | FLAGS_SIZING_ANIMATION)) == (FLAGS_ANIMATION | FLAGS_SIZING_ANIMATION);
	}

	protected boolean _hasMovingAnimation() {
		return (_flags & (FLAGS_ANIMATION | FLAGS_MOVING_ANIMATION)) == (FLAGS_ANIMATION | FLAGS_MOVING_ANIMATION);
	}

	protected boolean _hasSizingMovingAnimation() {
		return (_flags & (FLAGS_ANIMATION | FLAGS_SIZING_ANIMATION | FLAGS_MOVING_ANIMATION)) > FLAGS_ANIMATION;
	}

	protected void _setHasAnimation(boolean ani) {
		if (ani) {
			_flags = _flags | FLAGS_ANIMATION;
		} else {
			_flags = _flags & ~FLAGS_ANIMATION;
		}
	}

	protected void _setHasScaleAnimation(boolean ani) {
		if (ani) {
			_flags = _flags | FLAGS_SCALE_ANIMATION;
		} else {
			_flags = _flags & ~FLAGS_SCALE_ANIMATION;
		}
	}

	protected void _setHasSizingAnimation(boolean ani) {
		if (ani) {
			_flags = _flags | FLAGS_SIZING_ANIMATION;
		} else {
			_flags = _flags & ~FLAGS_SIZING_ANIMATION;
		}
	}

	protected void _setHasMovingAnimation(boolean ani) {
		if (ani) {
			_flags = _flags | FLAGS_MOVING_ANIMATION;
		} else {
			_flags = _flags & ~FLAGS_MOVING_ANIMATION;
		}
	}

	protected boolean _isRTL() {
		return (_flags & FLAGS_RIGHT_TO_LEFT) == FLAGS_RIGHT_TO_LEFT;
	}

	protected void _setRTL(boolean rtl) {
		if (rtl) {
			_flags = _flags | FLAGS_RIGHT_TO_LEFT;
		} else {
			_flags = _flags & ~FLAGS_RIGHT_TO_LEFT;
		}
	}

	protected boolean _isVisible() {
		return (_flags & FLAGS_VISIBLE) == FLAGS_VISIBLE;
	}

	protected boolean _isEnabled() {
		return (_flags & FLAGS_ENABLE) == FLAGS_ENABLE;
	}

	protected void _setEnabled(boolean enable) {
		if (_isEnabled() == enable) {
			return;
		}
		if (enable) {
			_flags |= FLAGS_ENABLE;
		} else {
			_flags &= ~FLAGS_ENABLE;
		}
		// 切换view enable状态
		_view.updateEnableState(enable);
		getRenderContext().uiRun(new Runnable() {
			@Override
			public void run() {
				setUpdateAll();
			}
		});
	}

	protected boolean _isInvisible() {
		return (_flags & FLAGS_VISIBLE) != FLAGS_VISIBLE;
	}

	protected void _setVisibility(boolean visible) {
		if (visible) {
			_flags = _flags | FLAGS_VISIBLE;
		} else {
			_flags = _flags & ~FLAGS_VISIBLE;
		}
	}

	long _dirtyTimestamp;

	protected XulRenderContext _ctx;
	protected XulView _view;

	// 元素在屏幕上的位置
	protected int _screenX = 0;
	protected int _screenY = 0;

	protected Rect _rect;

	// 元素的内边距
	protected Rect _padding;
	// 元素外边距
	protected Rect _margin;

	private RectF _focusRect;
	private RectF _updateRect;
	private RectF _animRect;
	private RectF _scaledRect;

	protected int _minWidth = 0;
	protected int _minHeight = 0;
	protected int _maxWidth = XulManager.SIZE_MAX;
	protected int _maxHeight = XulManager.SIZE_MAX;

	// 元素显示缩放比例（元素缩放不影响布局）
	protected float _scalarX = 1.0f;
	protected float _scalarY = 1.0f;
	// 元素缩放对齐位置
	protected float _scalarXAlign = 0.5f;
	protected float _scalarYAlign = 0.5f;

	private _QuiverInfo _quiver = null;

	private SizingMovingAnimation _sizingMovingAnimation;

	public void refreshSizingMovingAnimation() {
		if (_sizingMovingAnimation != null) {
			_sizingMovingAnimation._updateDest = true;
		}
	}

	public void setPreload(boolean initialPreload, boolean preload) {
		int newFlags = (initialPreload ? FLAGS_INITIAL_PRELOAD : 0) | (preload ? FLAGS_PRELOAD : 0);
		_flags = _flags | newFlags;
	}

	public boolean isRTL() {
		return _isRTL();
	}

	public boolean isInvisible() {
		return _isInvisible();
	}

	public XulArea getParentView() {
		return _view.getParent();
	}

	public XulView getView() {
		return _view;
	}

	public int getMaxWidth() {
		return _maxWidth;
	}

	public int getMaxHeight() {
		return _maxHeight;
	}

	public int getMinWidth() {
		return _minWidth;
	}

	public int getMinHeight() {
		return _minHeight;
	}

	public void onRenderCreated() {}

	class SizingMovingAnimation extends SimpleTransformAnimation {
		float _srcLeft;
		float _srcTop;
		float _srcWidth;
		float _srcHeight;

		float _dstLeft;
		float _dstTop;
		float _dstWidth;
		float _dstHeight;

		boolean _updateDest = false;

		public SizingMovingAnimation(XulViewRender render) {
			super(render);
		}

		public SizingMovingAnimation(XulViewRender render, ITransformer aniTransformer) {
			super(render, aniTransformer);
		}

		@Override
		public void storeSrc() {
			_srcVal = 0f;
			if (_rect != null) {
				_srcLeft = _rect.left;
				_srcTop = _rect.top;
				_srcWidth = XulUtils.calRectWidth(_rect);
				_srcHeight = XulUtils.calRectHeight(_rect);
			} else {
				_srcLeft = Float.NaN;
				_srcTop = Float.NaN;
				_srcWidth = Float.NaN;
				_srcHeight = Float.NaN;
			}
		}

		@Override
		public void storeDest() {
			_updateDest = false;
			_destVal = 100.0f;
			if (_rect != null) {
				_dstLeft = _rect.left;
				_dstTop = _rect.top;
				_dstWidth = XulUtils.calRectWidth(_rect);
				_dstHeight = XulUtils.calRectHeight(_rect);

				float newDestVal = Math.max(
					Math.max(Math.abs(_dstHeight - _srcHeight), Math.abs(_dstWidth - _srcWidth)),
					Math.max(Math.abs(_dstLeft - _srcLeft), Math.abs(_dstTop - _srcTop))
				);
				if (!Float.isNaN(newDestVal) && newDestVal > 0) {
					_destVal = newDestVal;
				}
			}
		}

		@Override
		public void restoreValue() {
			if (_rect == null) {
				return;
			}
			if (_hasMovingAnimation()) {
				int newLeft = XulUtils.roundToInt(_srcLeft + (_dstLeft - _srcLeft) * _val / _destVal);
				int newTop = XulUtils.roundToInt(_srcTop + (_dstTop - _srcTop) * _val / _destVal);
				_rect.offsetTo(newLeft, newTop);
				_setScaledRectChanged();
				setUpdateLayout();
			}

			if (_hasSizingAnimation()) {
				int newWidth = XulUtils.roundToInt(_srcWidth + (_dstWidth - _srcWidth) * _val / _destVal);
				int newHeight = XulUtils.roundToInt(_srcHeight + (_dstHeight - _srcHeight) * _val / _destVal);
				XulUtils.resizeRect(_rect, newWidth, newHeight);
				_setScaledRectChanged();
				setUpdateLayout();
			}
		}

		@Override
		public boolean updateAnimation(long timestamp) {
			if (!isRunning()) {
				return false;
			}
			if (_isInvisible()) {
				_val = _destVal;
				restoreValue();
				stopAnimation();
				return false;
			}
			if (!_hasSizingMovingAnimation()) {
				stopAnimation();
				return false;
			}
			return super.updateAnimation(timestamp);
		}

		@Override
		public void startAnimation() {
			if (!_hasSizingMovingAnimation() || _isInvisible()) {
				return;
			}
			if (isRunning()) {
				return;
			}
			storeDest();
			if (_dstWidth >= XulManager.SIZE_MAX || _dstHeight >= XulManager.SIZE_MAX) {
				return;
			}
			_val = 0f;
			if (_hasSizingAnimation() && !Float.isNaN(_srcWidth) && !Float.isNaN(_srcHeight)
				&& (!XulUtils.isEqual(_srcWidth, _dstWidth) || !XulUtils.isEqual(_srcHeight, _dstHeight))) {
				super.startAnimation();
				addAnimation(_sizingMovingAnimation);
				return;
			}
			if (_hasMovingAnimation() && !Float.isNaN(_srcLeft) && !Float.isNaN(_srcTop)
				&& (!XulUtils.isEqual(_srcLeft, _dstLeft) || !XulUtils.isEqual(_srcTop, _dstTop))) {
				super.startAnimation();
				addAnimation(_sizingMovingAnimation);
				return;
			}
			return;
		}
	}

	public boolean keepFocusVisible() {
		return _isKeepFocusVisible();
	}

	// reject test with preload detection
	public boolean rejectTest() {
		if (_rect == null) {
			return true;
		}
		if (_canPreload()) {
			return false;
		}
		RectF focusRc = _view.getRootLayout().getFocusRc();
		RectF targetRect = getFocusRect();

		if (XulUtils.intersects(focusRc, targetRect)) {
			return false;
		}

		if (targetRect.isEmpty() && focusRc.contains(targetRect.left, targetRect.top)) {
			return false;
		}

		XulArea parent = _view.getParent();
		while (parent != null) {
			XulViewRender render = parent.getRender();
			if (render instanceof XulPageSliderAreaRender) {
				if (!((XulPageSliderAreaRender) render).canPrefetch(_view)) {
					return true;
				}
			}
			if (render._canPreload()) {
				return false;
			}
			parent = parent.getParent();
		}
		return true;
	}

	// reject test
	protected boolean internalRejectTest() {
		if (_rect == null) {
			return true;
		}
		RectF focusRc = _view.getRootLayout().getFocusRc();
		RectF targetRect = getFocusRect();

		if (XulUtils.intersects(focusRc, targetRect)) {
			return false;
		}

		if (targetRect.isEmpty() && focusRc.contains(targetRect.left, targetRect.top)) {
			return false;
		}
		return true;
	}

	public boolean isLayoutChanged() {
		return _isLayoutChanged();
	}

	public boolean doSuspendRecycle(int recycleLevel) {
		return false;
	}

	static class _QuiverInfo {
		static final int DEFAULT_DURATION = 480;
		float _quiverX = 0.0f;
		float _quiverY = 0.0f;
		int _duration = DEFAULT_DURATION;
		float _quiverXDelta = 0.0f;
		float _quiverYDelta = 0.0f;
		String _mode = "linear";
		float[] _params = null;

		public _QuiverInfo(float x, float y, int duration, String quiverMode, float[] quiverModeParams) {
			resetQuiver(x, y, duration, quiverMode, quiverModeParams);
		}

		boolean isQuivering() {
			return Math.abs(_quiverYDelta) > 0.5f || Math.abs(_quiverXDelta) > 0.5f;
		}

		void resetQuiver(float xStrength, float yStrength, int quiverDuration, String quiverMode, float[] quiverModeParams) {
			_quiverX = xStrength;
			_quiverY = yStrength;
			_duration = quiverDuration;
			_mode = quiverMode;
			_params = quiverModeParams;
		}

		boolean doQuiver(long duration, float xDelta, float yDelta, int viewWidth, int viewHeight, double xScalar, double yScalar) {
			if (_duration > 0 && _duration < duration) {
				return false;
			}
			float quiverX = Math.abs(_quiverX);
			if (quiverX > 1.0f) {
				_quiverXDelta = (float) (xDelta * xScalar);
			} else if (quiverX > 0.00001f) {
				_quiverXDelta = xDelta * viewWidth;
			} else {
				_quiverXDelta = 0;
			}

			float quiverY = Math.abs(_quiverY);
			if (quiverY > 1.0f) {
				_quiverYDelta = (float) (yDelta * yScalar);
			} else if (quiverY > 0.00001f) {
				_quiverYDelta = yDelta * viewHeight;
			} else {
				_quiverYDelta = 0;
			}

			if (XulManager.DEBUG) {
				Log.d(TAG, String.format("doQuiver x(%d,%f,%f,%f), y(%d,%f,%f,%f)", viewWidth, xDelta, _quiverXDelta, _quiverX, viewHeight, yDelta, _quiverYDelta, _quiverY));
			}
			return true;
		}
	}

	protected float _borderSize = 0;
	protected float _borderRoundX = 0;
	protected float _borderRoundY = 0;
	protected int _borderColor = 0;
	protected float _borderPos = 0.5f;
	protected DashPathEffect _borderEffect = null;

	protected int _zIndex = 0;

	private static final Rect _DUMMY_PADDING_ = new Rect(0, 0, 0, 0);

	public boolean hitTest(int event, float x, float y) {
		if (_isInvisible() || _rect == null) {
			return false;
		}
		if (event != XulManager.HIT_EVENT_DOWN
			&& event != XulManager.HIT_EVENT_UP
			&& event != XulManager.HIT_EVENT_DUMMY
			) {
			return false;
		}
		x -= _screenX;
		y -= _screenY;
		RectF animRect = getAnimRect();
		return animRect.contains(x, y);
	}

	public boolean handleScrollEvent(float hScroll, float vScroll) {
		return false;
	}

	public boolean hitTestTranslate(PointF pt) {
		return false;
	}

	public void resetBinding() {
	}

	protected int _aniTransformDuration;
	protected ITransformer _aniTransformer;
	protected TransformAnimation _transformAnimation;

	private void syncAnimationInfo() {
		if (!_isDataChanged()) {
			return;
		}
		XulAttr attrAnimation = _view.getAttr(XulPropNameCache.TagId.ANIMATION);
		if (attrAnimation != null && "disabled".equals(attrAnimation.getStringValue())) {
			_setHasAnimation(false);
		} else {
			_setHasAnimation(true);
		}

		XulAttr sizingAnimationAttr = _view.getAttr(XulPropNameCache.TagId.ANIMATION_SIZING);
		if (sizingAnimationAttr != null) {
			_setHasSizingAnimation(((XulPropParser.xulParsedProp_booleanValue) sizingAnimationAttr.getParsedValue()).val);
		} else {
			_setHasSizingAnimation(false);
		}

		XulAttr movingAnimationAttr = _view.getAttr(XulPropNameCache.TagId.ANIMATION_MOVING);
		if (movingAnimationAttr != null) {
			_setHasMovingAnimation(((XulPropParser.xulParsedProp_booleanValue) movingAnimationAttr.getParsedValue()).val);
		} else {
			_setHasMovingAnimation(false);
		}

		XulAttr durationAttr = _view.getAttr(XulPropNameCache.TagId.ANIMATION_DURATION);
		XulAttr aniModeAttr = _view.getAttr(XulPropNameCache.TagId.ANIMATION_MODE);
		int duration = 100;
		if (durationAttr != null) {
			duration = XulUtils.tryParseInt(durationAttr.getStringValue());
		}
		if (!_hasAnimation()) {
			duration = 0;
		}

		if (aniModeAttr != null) {
			XulPropParser.xulParsedAttr_AnimationMode mode = aniModeAttr.getParsedValue();
			if (mode.mode != null) {
				if (_aniTransformer == null) {
					_aniTransformer = TransformerFactory.createTransformer(mode.mode, mode.params);
				} else {
					_aniTransformer.switchParams(mode.params);
					_aniTransformer.switchAlgorithm(mode.mode);
				}
			} else {
				_aniTransformer = null;
			}
		} else {
			_aniTransformer = null;
		}
		_aniTransformDuration = duration;

		XulStyle quiverStyle = _view.getStyle(XulPropNameCache.TagId.QUIVER);
		if (quiverStyle == null) {
			_quiver = null;
		} else {
			XulStyle quiverModeStyle = _view.getStyle(XulPropNameCache.TagId.QUIVER_MODE);
			String quiverMode = QuiverAnimation.DEFAULT_MODE;
			float[] quiverModeParams = QuiverAnimation.DEFAULT_PARAMS;
			if (quiverModeStyle != null) {
				XulPropParser.xulParsedAttr_AnimationMode mode = quiverModeStyle.getParsedValue();
				quiverMode = mode.mode;
				quiverModeParams = mode.params;
			}
			XulPropParser.xulParsedProp_FloatArray quiver = quiverStyle.getParsedValue();
			if (quiver.getLength() >= 2) {
				final float xStrength = quiver.tryGetVal(0, 0);
				final float yStrength = quiver.tryGetVal(1, 0);
				final int quiverDuration = (int) quiver.tryGetVal(2, _QuiverInfo.DEFAULT_DURATION);
				final int repeat = XulUtils.roundToInt(quiver.tryGetVal(3, 1));
				final float repeatStrength = quiver.tryGetVal(4, 1);
				if (Math.abs(xStrength) < 0.00001f && Math.abs(yStrength) < 0.00001f) {
					_quiver = null;
				} else if (_quiver == null) {
					_quiver = new _QuiverInfo(xStrength, yStrength, quiverDuration, quiverMode, quiverModeParams);
					QuiverAnimation quiverAnimation = new QuiverAnimation(this, xStrength, yStrength, repeat, repeatStrength) {
						@Override
						public boolean updateAnimation(long timestamp) {
							if (_quiver == null) {
								onAnimationFinished(false);
								return false;
							}
							switchMode(_quiver._mode, _quiver._params);
							updateDuration(_quiver._duration);
							return super.updateAnimation(timestamp);
						}

						@Override
						public boolean doQuiver(long duration, float xDelta, float yDelta) {
							if (_quiver == null) {
								return false;
							}

							boolean result = _quiver.doQuiver(duration, xDelta, yDelta, XulUtils.calRectWidth(_rect), XulUtils.calRectHeight(_rect), getXScalar(), getYScalar());
							if (!result) {
								_quiver = null;
							}
							markDirtyView();
							return result;
						}
					};
					addAnimation(quiverAnimation);
				} else {
					_quiver.resetQuiver(xStrength, yStrength, quiverDuration, quiverMode, quiverModeParams);
				}
			} else {
				_quiver = null;
			}
		}
	}

	public void collectTransformValues(TransformAnimation animation) {
		animation.addTransformValues(new TransformAnimation.TransformValues() {
			float _srcScalarY;
			float _destScalarY;
			float _scalarYVal;

			float _srcScalarXAlign;
			float _destScalarXAlign;
			float _scalarXAlignVal;

			float _srcScalarYAlign;
			float _destScalarYAlign;
			float _scalarYAlignVal;

			@Override
			public void storeSrc() {
				_srcVal = _scalarX;
				_srcScalarY = _scalarY;
				_srcScalarXAlign = _scalarXAlign;
				_srcScalarYAlign = _scalarYAlign;
			}

			@Override
			public void storeDest() {
				_destVal = _scalarX;
				_destScalarY = _scalarY;
				_destScalarXAlign = _scalarXAlign;
				_destScalarYAlign = _scalarYAlign;
			}

			@Override
			public void restoreValue() {
				_setScaledRectChanged();
				_scalarX = _val;
				_scalarY = _scalarYVal;
				_scalarXAlign = _scalarXAlignVal;
				_scalarYAlign = _scalarYAlignVal;
			}

			@Override
			public boolean identicalValue() {
				return super.identicalValue() &&
					_srcScalarY == _destScalarY &&
					_srcScalarXAlign == _destScalarXAlign &&
					_srcScalarYAlign == _destScalarYAlign;
			}

			@Override
			public boolean updateValue(float percent) {
				if (!_hasScaleAnimation()) {
					_val = _destVal;
					_scalarYVal = _destScalarY;
					_scalarXAlignVal = _destScalarXAlign;
					_scalarYAlignVal = _destScalarYAlign;
					return false;
				}
				boolean result = updateValueScalarX(percent);
				result = updateValueScalarY(percent) || result;
				result = updateValueScalarXAlign(percent) || result;
				result = updateValueScalarYAlign(percent) || result;
				return result;
			}

			public boolean updateValueScalarX(float percent) {
				return super.updateValue(percent);
			}

			public boolean updateValueScalarY(float percent) {
				float delta = _destScalarY - _srcScalarY;
				_scalarYVal = _srcScalarY;
				if (delta == 0) {
					return false;
				}
				_scalarYVal += delta * percent;
				return true;
			}

			public boolean updateValueScalarXAlign(float percent) {
				float delta = _destScalarXAlign - _srcScalarXAlign;
				_scalarXAlignVal = _srcScalarXAlign;
				if (delta == 0) {
					return false;
				}
				_scalarXAlignVal += delta * percent;
				return true;
			}

			public boolean updateValueScalarYAlign(float percent) {
				float delta = _destScalarYAlign - _srcScalarYAlign;
				_scalarYAlignVal = _srcScalarYAlign;
				if (delta == 0) {
					return false;
				}
				_scalarYAlignVal += delta * percent;
				return true;
			}
		});
	}

	private void initTransformAnimation(boolean rollback) {
		if (_transformAnimation == null) {
			_transformAnimation = new TransformAnimation(this);
			collectTransformValues(_transformAnimation);
		}
		_transformAnimation.setTransformer(_aniTransformer);
		_transformAnimation.startAnimation(_aniTransformDuration, rollback);
	}

	public boolean collectPendingItems(XulTaskCollector xulTaskCollector) {
		if (_isViewChanged() || _rect == null) {
			return true;
		}
		return false;
	}

	protected class LayoutElement implements XulLayoutHelper.ILayoutElement {

		private boolean _initialVisibility;

		@Override
		public boolean changed() {
			return _isLayoutChanged();
		}

		@Override
		public boolean isVisible() {
			return _isVisible();
		}

		@Override
		public int prepare() {
			if (_isLayoutChangedByChild()) {
				fastSyncLayout();
			} else {
				syncLayout();
			}
			return 0;
		}

		void fastSyncLayout() {
			if (_isInvisible()) {
				return;
			}
			syncAnimationInfo();
			if (_hasSizingMovingAnimation()) {
				if (_sizingMovingAnimation != null && _sizingMovingAnimation.isRunning()) {
					if (_sizingMovingAnimation._updateDest) {
						syncLayoutParametersFast();
						_sizingMovingAnimation.storeDest();
					}
					return;
				}
				prepareSizingMovingAnimation();
			} else if (_sizingMovingAnimation != null) {
				removeAnimation(_sizingMovingAnimation);
				_sizingMovingAnimation.stopAnimation();
			}
			syncLayoutParametersFast();
			updateSizingMovingAnimation();
		}

		void syncLayout() {
			_initialVisibility = _isVisible();
			if (syncVisibility()) {
				return;
			}
			if (_isInvisible()) {
				return;
			}
			syncAnimationInfo();
			if (_initialVisibility == _isVisible()) {
				if (_hasSizingMovingAnimation()) {
					if (_sizingMovingAnimation != null && _sizingMovingAnimation.isRunning()) {
						if (_sizingMovingAnimation._updateDest) {
							syncLayoutParameters();
							_sizingMovingAnimation.storeDest();
						}
						return;
					}
					prepareSizingMovingAnimation();
				} else if (_sizingMovingAnimation != null) {
					removeAnimation(_sizingMovingAnimation);
					_sizingMovingAnimation.stopAnimation();
				}
				syncLayoutParameters();
				updateSizingMovingAnimation();
			} else {
				syncLayoutParameters();
			}
		}

		@Override
		public int doFinal() {
			if (!_isLayoutChangedByChild() && _initialVisibility == _isVisible()) {
				finalizeSizingMovingAnimation();
			}
			_setLayoutUpdated();
			return 0;
		}

		@Override
		public int getLeft() {
			return _rect.left;
		}

		@Override
		public int getTop() {
			return _rect.top;
		}

		@Override
		public int getWidth() {
			return XulUtils.calRectWidth(_rect);
		}

		@Override
		public int getHeight() {
			return XulUtils.calRectHeight(_rect);
		}

		@Override
		public int getRight() {
			return _rect.right;
		}

		@Override
		public int getBottom() {
			return _rect.bottom;
		}

		@Override
		public int getViewRight() {
			return _screenX + _rect.right;
		}

		@Override
		public int getViewBottom() {
			return _screenY + _rect.bottom;
		}

		@Override
		public int getContentWidth() {
			return 0;
		}

		@Override
		public int getContentHeight() {
			return 0;
		}

		@Override
		public Rect getPadding() {
			if (_padding == null) {
				return _DUMMY_PADDING_;
			}
			return _padding;
		}

		@Override
		public Rect getMargin() {
			if (_margin == null) {
				return _DUMMY_PADDING_;
			}
			return _margin;
		}

		@Override
		public int getBaseX() {
			return _screenX;
		}

		@Override
		public int getBaseY() {
			return _screenY;
		}

		@Override
		public boolean setWidth(int w) {
			_rect.right = _rect.left + w;
			return true;
		}

		@Override
		public boolean setHeight(int h) {
			_rect.bottom = _rect.top + h;
			return true;
		}

		@Override
		public boolean setBase(int x, int y) {
			_screenX = x;
			_screenY = y;
			return true;
		}

		@Override
		public boolean offsetBase(int dx, int dy) {
			_screenX += dx;
			_screenY += dy;
			return true;
		}

		@Override
		public boolean checkUpdateMatchParent(int maxW, int maxH) {
			if (_isLayoutChanged()) {
				return false;
			}

			boolean updateWidth = _hasFlags(FLAGS_UPDATE_WIDTH);
			boolean updateHeight = _hasFlags(FLAGS_UPDATE_HEIGHT);

			if (!updateWidth && !updateHeight) {
				return false;
			}

			if ((updateWidth && _rect.right != maxW)
				|| (updateHeight && _rect.bottom != maxH)) {
				_setLayoutChanged();
				updateParentLayout();
				return true;
			}
			return false;
		}

		@Override
		public int getMinWidth() {
			return _minWidth;
		}

		@Override
		public int getMinHeight() {
			return _minHeight;
		}

		@Override
		public int getMaxWidth() {
			return _maxWidth;
		}

		@Override
		public int getMaxHeight() {
			return _maxHeight;
		}

		@Override
		public int constrainWidth(int newWidth) {
			return Math.max(Math.min(newWidth, _maxWidth), _minWidth);
		}

		@Override
		public int constrainHeight(int newHeight) {
			return Math.max(Math.min(newHeight, _maxHeight), _minHeight);
		}
	}

	private void updateSizingMovingAnimation() {
		if (!_hasSizingMovingAnimation() || _aniTransformDuration < 1) {
			return;
		}
		if (_sizingMovingAnimation != null) {
			_sizingMovingAnimation.startAnimation();
			if (_sizingMovingAnimation.isRunning()) {
				_sizingMovingAnimation.restoreValue();
			}
		}
	}

	private void finalizeSizingMovingAnimation() {
		if (!_hasSizingMovingAnimation() || _aniTransformDuration < 1 || _isInvisible()) {
			return;
		}
		if (_sizingMovingAnimation != null) {
			_sizingMovingAnimation.startAnimation();
			if (_sizingMovingAnimation.isRunning()) {
				int orgLeft = _rect.left;
				int orgTop = _rect.top;
				_sizingMovingAnimation.restoreValue();
				if ((this instanceof XulViewContainerBaseRender)
					&& ((orgLeft != _rect.left) || (orgTop != _rect.top))) {
					final XulLayoutHelper.ILayoutElement layoutElement = getLayoutElement();
					XulLayoutHelper.offsetChild((XulLayoutHelper.ILayoutContainer) layoutElement, _rect.left - orgLeft, _rect.top - orgTop);
				}
			}
		}
	}

	private void prepareSizingMovingAnimation() {
		if (!_hasSizingMovingAnimation()) {
			return;
		}
		if (_sizingMovingAnimation == null) {
			_sizingMovingAnimation = new SizingMovingAnimation(this);
		}
		_sizingMovingAnimation.setTransformer(_aniTransformer);
		_sizingMovingAnimation.prepareAnimation(_aniTransformDuration);
	}

	private boolean syncLayoutParametersFast() {
		if (!_isLayoutChanged()) {
			return true;
		}
		if (_isInvisible()) {
			return true;
		}

		double xScalar = _ctx.getXScalar();
		double yScalar = _ctx.getYScalar();

		if (_padding == null) {
			XulStyle paddingAll = null;
			XulStyle paddingLeft = _view.getStyle(XulPropNameCache.TagId.PADDING_LEFT);
			XulStyle paddingTop = _view.getStyle(XulPropNameCache.TagId.PADDING_TOP);
			XulStyle paddingRight = _view.getStyle(XulPropNameCache.TagId.PADDING_RIGHT);
			XulStyle paddingBottom = _view.getStyle(XulPropNameCache.TagId.PADDING_BOTTOM);
			_padding = new Rect();

			if (paddingLeft == null ||
				paddingRight == null ||
				paddingTop == null ||
				paddingBottom == null) {
				paddingAll = _view.getStyle(XulPropNameCache.TagId.PADDING);
			}

			_padding.left = paddingLeft == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) paddingLeft.getParsedValue()).val * xScalar);
			_padding.top = paddingTop == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) paddingTop.getParsedValue()).val * yScalar);
			_padding.right = paddingRight == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) paddingRight.getParsedValue()).val * xScalar);
			_padding.bottom = paddingBottom == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) paddingBottom.getParsedValue()).val * yScalar);

			if (paddingAll != null) {
				XulPropParser.xulParsedProp_PaddingMargin padding = paddingAll.getParsedValue();
				if (paddingLeft == null || paddingLeft.getPriority() < paddingAll.getPriority()) {
					_padding.left = XulUtils.roundToInt(padding.left * xScalar);
				}
				if (paddingTop == null || paddingTop.getPriority() < paddingAll.getPriority()) {
					_padding.top = XulUtils.roundToInt(padding.top * yScalar);
				}
				if (paddingRight == null || paddingRight.getPriority() < paddingAll.getPriority()) {
					_padding.right = XulUtils.roundToInt(padding.right * xScalar);
				}
				if (paddingBottom == null || paddingBottom.getPriority() < paddingAll.getPriority()) {
					_padding.bottom = XulUtils.roundToInt(padding.bottom * yScalar);
				}
			}
		}

		if (_rect == null) {
			_rect = new Rect();
			_setScaledRectChanged();
		}

		int width = _view.getWidth();
		if (width >= XulManager.SIZE_MAX) {
			_setScaledRectChanged();
			_rect.right = _rect.left + width;
		}

		int height = _view.getHeight();
		if (height >= XulManager.SIZE_MAX) {
			_setScaledRectChanged();
			_rect.bottom = _rect.top + height;
		}

		if (width == XulManager.SIZE_MATCH_PARENT) {
			_addFlags(FLAGS_UPDATE_WIDTH);
		} else {
			_clearFlags(FLAGS_UPDATE_WIDTH);
		}

		if (height == XulManager.SIZE_MATCH_PARENT) {
			_addFlags(FLAGS_UPDATE_HEIGHT);
		} else {
			_clearFlags(FLAGS_UPDATE_HEIGHT);
		}
		return false;
	}

	private boolean syncVisibility() {
		if (!_isLayoutChanged()) {
			return true;
		}
		{
			boolean oldVisible = _isVisible();
			boolean newVisible;
			if ("none".equals(_view.getStyleString(XulPropNameCache.TagId.DISPLAY))) {
				newVisible = false;
			} else {
				newVisible = true;
			}
			if (oldVisible != newVisible) {
				_setVisibility(newVisible);
				onVisibilityChanged(newVisible, _view);
			}
		}

		if (!_isVisible()) {
			return true;
		}
		return false;
	}

	private boolean syncLayoutParameters() {
		if (!_isLayoutChanged()) {
			return true;
		}

		if (_isInvisible()) {
			return true;
		}

		boolean oldEnable = _isEnabled();
		boolean newEnable = true;
		XulAttr enableAttr = _view.getAttr(XulPropNameCache.TagId.ENABLED);
		if (enableAttr != null) {
			newEnable = ((XulPropParser.xulParsedAttr_Enabled) enableAttr.getParsedValue()).val;
		}
		if (newEnable != oldEnable) {
			// enable 状态改变
			_setEnabled(newEnable);
			onUsabilityChanged(newEnable, _view);
		}

		double xScalar = _ctx.getXScalar();
		double yScalar = _ctx.getYScalar();

		XulStyle paddingAll = null;
		XulStyle paddingLeft = _view.getStyle(XulPropNameCache.TagId.PADDING_LEFT);
		XulStyle paddingTop = _view.getStyle(XulPropNameCache.TagId.PADDING_TOP);
		XulStyle paddingRight = _view.getStyle(XulPropNameCache.TagId.PADDING_RIGHT);
		XulStyle paddingBottom = _view.getStyle(XulPropNameCache.TagId.PADDING_BOTTOM);

		if (_padding == null) {
			_padding = new Rect();
		}

		if (paddingLeft == null ||
			paddingRight == null ||
			paddingTop == null ||
			paddingBottom == null) {
			paddingAll = _view.getStyle(XulPropNameCache.TagId.PADDING);
		}

		_padding.left = paddingLeft == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) paddingLeft.getParsedValue()).val * xScalar);
		_padding.top = paddingTop == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) paddingTop.getParsedValue()).val * yScalar);
		_padding.right = paddingRight == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) paddingRight.getParsedValue()).val * xScalar);
		_padding.bottom = paddingBottom == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) paddingBottom.getParsedValue()).val * yScalar);

		if (paddingAll != null) {
			XulPropParser.xulParsedProp_PaddingMargin padding = paddingAll.getParsedValue();
			if (paddingLeft == null || paddingLeft.getPriority() < paddingAll.getPriority()) {
				_padding.left = XulUtils.roundToInt(padding.left * xScalar);
			}
			if (paddingTop == null || paddingTop.getPriority() < paddingAll.getPriority()) {
				_padding.top = XulUtils.roundToInt(padding.top * yScalar);
			}
			if (paddingRight == null || paddingRight.getPriority() < paddingAll.getPriority()) {
				_padding.right = XulUtils.roundToInt(padding.right * xScalar);
			}
			if (paddingBottom == null || paddingBottom.getPriority() < paddingAll.getPriority()) {
				_padding.bottom = XulUtils.roundToInt(padding.bottom * yScalar);
			}
		}

		{
			XulStyle marginAll = null;
			XulStyle marginLeft = _view.getStyle(XulPropNameCache.TagId.MARGIN_LEFT);
			XulStyle marginTop = _view.getStyle(XulPropNameCache.TagId.MARGIN_TOP);
			XulStyle marginRight = _view.getStyle(XulPropNameCache.TagId.MARGIN_RIGHT);
			XulStyle marginBottom = _view.getStyle(XulPropNameCache.TagId.MARGIN_BOTTOM);

			if (marginLeft == null ||
				marginRight == null ||
				marginTop == null ||
				marginBottom == null) {
				marginAll = _view.getStyle(XulPropNameCache.TagId.MARGIN);
			}

			if (marginLeft == null &&
				marginRight == null &&
				marginTop == null &&
				marginBottom == null &&
				marginAll == null
				) {
				if (_margin != null) {
					_margin.set(0, 0, 0, 0);
				}
			} else {
				if (_margin == null) {
					_margin = new Rect();
				}
				_margin.left = marginLeft == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) marginLeft.getParsedValue()).val * xScalar);
				_margin.top = marginTop == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) marginTop.getParsedValue()).val * yScalar);
				_margin.right = marginRight == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) marginRight.getParsedValue()).val * xScalar);
				_margin.bottom = marginBottom == null ? 0 : XulUtils.roundToInt(((XulPropParser.xulParsedStyle_PaddingMarginVal) marginBottom.getParsedValue()).val * yScalar);

				if (marginAll != null) {
					XulPropParser.xulParsedProp_PaddingMargin margin = marginAll.getParsedValue();
					if (marginLeft == null || marginLeft.getPriority() < marginAll.getPriority()) {
						_margin.left = XulUtils.roundToInt(margin.left * xScalar);
					}
					if (marginTop == null || marginTop.getPriority() < marginAll.getPriority()) {
						_margin.top = XulUtils.roundToInt(margin.top * yScalar);
					}
					if (marginRight == null || marginRight.getPriority() < marginAll.getPriority()) {
						_margin.right = XulUtils.roundToInt(margin.right * xScalar);
					}
					if (marginBottom == null || marginBottom.getPriority() < marginAll.getPriority()) {
						_margin.bottom = XulUtils.roundToInt(margin.bottom * yScalar);
					}
				}
			}
		}

		if (_rect == null) {
			_rect = new Rect();
		}

		XulStyle maxWidth = _view.getStyle(XulPropNameCache.TagId.MAX_WIDTH);
		XulStyle maxHeight = _view.getStyle(XulPropNameCache.TagId.MAX_HEIGHT);
		XulStyle minWidth = _view.getStyle(XulPropNameCache.TagId.MIN_WIDTH);
		XulStyle minHeight = _view.getStyle(XulPropNameCache.TagId.MIN_HEIGHT);

		if (maxWidth != null) {
			_maxWidth = XulUtils.roundToInt(((XulPropParser.xulParsedProp_Integer) maxWidth.getParsedValue()).val * xScalar);
		} else {
			_maxWidth = XulManager.SIZE_MAX;
		}

		if (maxHeight != null) {
			_maxHeight = XulUtils.roundToInt(((XulPropParser.xulParsedProp_Integer) maxHeight.getParsedValue()).val * yScalar);
		} else {
			_maxHeight = XulManager.SIZE_MAX;
		}

		if (minWidth != null) {
			_minWidth = XulUtils.roundToInt(((XulPropParser.xulParsedProp_Integer) minWidth.getParsedValue()).val * xScalar);
		} else {
			_minWidth = 0;
		}

		if (minHeight != null) {
			_minHeight = XulUtils.roundToInt(((XulPropParser.xulParsedProp_Integer) minHeight.getParsedValue()).val * yScalar);
		} else {
			_minHeight = 0;
		}

		_setScaledRectChanged();
		_rect.left = _view.getX();
		_rect.top = _view.getY();

		if (_rect.left > XulManager.SIZE_MAX) {
			_rect.left = 0;
		}

		if (_rect.top > XulManager.SIZE_MAX) {
			_rect.top = 0;
		}

		int width = _view.getWidth();
		if (width < XulManager.SIZE_MAX) {
			_rect.right = XulUtils.roundToInt((_rect.left + width) * xScalar);
			_rect.left = XulUtils.roundToInt(_rect.left * xScalar);
		} else {
			_rect.left = XulUtils.roundToInt(_rect.left * xScalar);
			_rect.right = _rect.left + width;
		}

		int height = _view.getHeight();
		if (height < XulManager.SIZE_MAX) {
			_rect.bottom = XulUtils.roundToInt((_rect.top + height) * yScalar);
			_rect.top = XulUtils.roundToInt(_rect.top * yScalar);
		} else {
			_rect.top = XulUtils.roundToInt(_rect.top * yScalar);
			_rect.bottom = _rect.top + height;
		}

		if (width == XulManager.SIZE_MATCH_PARENT) {
			_addFlags(FLAGS_UPDATE_WIDTH);
		} else {
			_clearFlags(FLAGS_UPDATE_WIDTH);
		}

		if (height == XulManager.SIZE_MATCH_PARENT) {
			_addFlags(FLAGS_UPDATE_HEIGHT);
		} else {
			_clearFlags(FLAGS_UPDATE_HEIGHT);
		}

		final String layoutModeStyle = _view.getStyleString(XulPropNameCache.TagId.LAYOUT_MODE);
		if ("rtl".equals(layoutModeStyle)) {
			_setRTL(true);
		} else {
			_setRTL(false);
		}

		if (_isRTL()) {
			if (_padding != null) {
				int tmp = _padding.left;
				_padding.left = _padding.right;
				_padding.right = tmp;
			}
		}
		return false;
	}

	private XulLayoutHelper.ILayoutElement _layoutElement;

	protected XulLayoutHelper.ILayoutElement createElement() {
		return new LayoutElement();
	}

	public XulLayoutHelper.ILayoutElement getLayoutElement() {
		if (_layoutElement == null) {
			_layoutElement = createElement();
		}
		return _layoutElement;
	}

	public boolean isVisible() {
		return _isVisible();
	}

	public boolean isEnabled() {
		return _isEnabled();
	}

	public void setEnabled(boolean enable) {
		_setEnabled(enable);
	}

	public void destroy() {
		cleanImageItems();
		if (_transformAnimation != null) {
			removeAnimation(_transformAnimation);
		}
	}

	public void switchState(int state) {
		setUpdateAll();
	}

	public int getZIndex() {
		return _zIndex;
	}

	public XulRenderContext getRenderContext() {
		return _ctx;
	}

	public int getScreenX() {
		return _screenX;
	}

	public int getScreenY() {
		return _screenY;
	}

	public void addAnimation(IXulAnimation animation) {
		_ctx.addAnimation(animation);
	}

	public void removeAnimation(IXulAnimation animation) {
		_ctx.removeAnimation(animation);
	}

	public long animationTimestamp() {
		return _ctx.animationTimestamp();
	}

	public Rect getPadding() {
		return _padding;
	}

	class BkgDrawableInfo extends XulRenderDrawableItem {
		String _url;
		volatile boolean _isLoading = false;
		volatile boolean _forceReload = false;
		volatile boolean _isRecycled = false;
		volatile long _lastLoadFailedTime = 0;
		volatile long _loadFailedCounter = 0;
		XulDrawable _bmp = null;

		@Override
		public void onImageReady(XulDrawable bmp) {
			_isLoading = false;
			_forceReload = false;
			if (_isRecycled) {
				return;
			}
			if (bmp == null) {
				_lastLoadFailedTime = XulUtils.timestamp();
				++_loadFailedCounter;
			} else {
				_bmp = bmp;
				_lastLoadFailedTime = 0;
				_loadFailedCounter = 0;
			}
			assert _url.equals(url);
			markDirtyView();
		}
	}

	public void markDirtyView() {
		boolean drawingSkipped = isDrawingSkipped();
		XulArea parent = _view.getParent();
		while (parent != null) {
			XulViewRender render = parent.getRender();
			if (render != null) {
				if (!drawingSkipped && render.isDrawingSkipped()) {
					drawingSkipped = true;
				}
				render.onDirtyChild(_view);
			}
			parent = parent.getParent();
		}
		if (!drawingSkipped) {
			_ctx.markDirtyView(_view);
		}
	}

	public void markDataChanged() {
		if (_isDataChanged()) {
			return;
		}
		_setDataChanged();
		_ctx.markDataChanged(this);
	}

	public void onDirtyChild(XulView view) {
		_dirtyTimestamp = getRenderContext().animationTimestamp();
	}

	private BkgDrawableInfo _bkgInfo;
	private int _bkgColor = 0;

	public XulViewRender(XulRenderContext ctx, XulView view) {
		this._ctx = ctx;
		this._view = view;
	}

	public void reset() {
		if (_isViewChanged()) {
			markDataChanged();
			_setLayoutChanged();
			updateParentLayout();
			markDirtyView();
			return;
		}
		if (_bkgInfo != null && !_bkgInfo._isLoading) {
			_bkgInfo._forceReload = true;
			_bkgInfo._lastLoadFailedTime = 0;
		}
		setUpdateAll();
		markDirtyView();
	}

	public void updateParentLayout() {
		updateParentLayout(false);
	}

	public void updateParentLayout(boolean force) {
		XulArea parent = _view.getParent();
		while (parent != null) {
			XulViewRender render = parent.getRender();
			if (render != null) {
				if (!force && render._isLayoutChanged()) {
					break;
				}
				render.setUpdateLayout(true);
			}
			parent = parent.getParent();
		}
	}

	public double getXScalar() {
		return _ctx.getXScalar();
	}

	public double getYScalar() {
		return _ctx.getYScalar();
	}

	public float getViewXScalar() {
		return _scalarX;
	}

	public float getViewYScalar() {
		return _scalarY;
	}

	static XulStateChangeEvent _stateChangeEvent = new XulStateChangeEvent();

	public void onVisibilityChanged(boolean isVisible, XulView eventSource) {
		XulArea parent = _view.getParent();
		if (parent != null) {
			_stateChangeEvent.event = "visibilityChanged";
			_stateChangeEvent.eventSource = eventSource;
			_stateChangeEvent.alteredEventSource = null;
			_stateChangeEvent.oldState = isVisible ? XulView.STATE_INVISIBLE : XulView.STATE_VISIBLE;
			_stateChangeEvent.state = isVisible ? XulView.STATE_VISIBLE : XulView.STATE_INVISIBLE;
			_stateChangeEvent.notifySource = _view;
			_stateChangeEvent.adjustFocusView = false;
			parent.onChildStateChanged(_stateChangeEvent);
			_stateChangeEvent.eventSource = null;
			_stateChangeEvent.alteredEventSource = null;
			_stateChangeEvent.notifySource = null;
		}
		if (isVisible && eventSource != _view) {
			if (_isViewChanged()) {
				updateParentLayout();
			}
		}
	}

	// 若需要监听enable状态改变，需要实现该接口
	public void onUsabilityChanged(boolean isEnable, XulView eventSource) {
	}

	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		double xScalar = _ctx.getXScalar();
		double yScalar = _ctx.getYScalar();

		XulStyle scale = _view.getStyle(XulPropNameCache.TagId.SCALE);
		XulStyle scaleAni = _view.getStyle(XulPropNameCache.TagId.ANIMATION_SCALE);
		if (scaleAni != null) {
			XulPropParser.xulParsedProp_booleanValue parsedValue = scaleAni.getParsedValue();
			_setHasScaleAnimation(parsedValue.val);
		} else {
			_setHasScaleAnimation(true);
		}

		XulStyle preloadStyle = _view.getStyle(XulPropNameCache.TagId.PRELOAD);
		if (preloadStyle != null) {
			XulPropParser.xulParsedProp_booleanValue val = preloadStyle.getParsedValue();
			_setPreload(val.val);
		} else {
			_setPreload(_isInitialPreload());
		}

		_setScaledRectChanged();
		if (scale == null) {
			_scalarX = _scalarY = 1.0f;
		} else {
			XulPropParser.xulParsedStyle_Scale scaleVal = scale.getParsedValue();
			_scalarX = scaleVal.xScalar;
			_scalarY = scaleVal.yScalar;
			_scalarXAlign = scaleVal.xAlign;
			_scalarYAlign = scaleVal.yAlign;
		}

		XulStyle borderStyle = _view.getStyle(XulPropNameCache.TagId.BORDER);
		if (borderStyle != null) {
			XulPropParser.xulParsedStyle_Border border = borderStyle.getParsedValue();
			_borderSize = XulUtils.roundToInt(border.size * xScalar);
			_borderColor = border.color;
			_borderRoundX = (float) (border.xRadius * xScalar);
			_borderRoundY = (float) (border.yRadius * yScalar);
			_borderPos = border.pos;

			XulStyle borderDashPatternStyle = _view.getStyle(XulPropNameCache.TagId.BORDER_DASH_PATTERN);
			if (borderDashPatternStyle != null) {
				XulPropParser.xulParsedStyle_Border_Dash_Pattern pattern = borderDashPatternStyle.getParsedValue();
				_borderEffect = pattern.getEffectObjectByXYScalar((float) xScalar, (float) yScalar);
			} else {
				_borderEffect = null;
			}

		} else {
			_borderColor = 0;
			_borderSize = 0;
			_borderRoundX = 0;
			_borderRoundY = 0;
			_borderEffect = null;
		}

		XulStyle bkgStyle = _view.getStyle(XulPropNameCache.TagId.BACKGROUND_IMAGE);
		if (bkgStyle != null) {
			XulPropParser.xulParsedStyle_BackgroundImage bkgImgInfo = bkgStyle.getParsedValue();
			BkgDrawableInfo bkg = _bkgInfo;
			if (bkg == null) {
				bkg = new BkgDrawableInfo();
			}
			if (TextUtils.isEmpty(bkgImgInfo.url)) {
				_bkgInfo = null;
			} else {
				String bkgImgUrl = bkgImgInfo.url;
				if (bkg._url != bkgImgUrl) {
					if (!TextUtils.isEmpty(bkg._url) && !bkg._url.equals(bkgImgUrl)) {
						XulWorker.removeDrawableCache(_bkgInfo._url);
						if (bkg._isLoading) {
							bkg._isRecycled = true;
							bkg = new BkgDrawableInfo();
						}
						bkg._forceReload = true;
						bkg._lastLoadFailedTime = 0;
					}
					bkg._url = bkgImgUrl;

					// 从缓存中查找新图片
					XulDrawable newBmp = XulWorker.loadDrawableFromCache(bkg._url);
					if (newBmp != null) {
						bkg._bmp = newBmp;
					}
				}
				_bkgInfo = bkg;
			}
		} else {
			_bkgInfo = null;
		}

		XulStyle bkgColorStyle = _view.getStyle(XulPropNameCache.TagId.BACKGROUND_COLOR);
		if (bkgColorStyle != null) {
			XulPropParser.xulParsedStyle_BackgroundColor backgroundColor = bkgColorStyle.getParsedValue();
			_bkgColor = backgroundColor.val;
		} else {
			_bkgColor = 0;
		}

		XulStyle zIndexStyle = _view.getStyle(XulPropNameCache.TagId.Z_INDEX);
		if (zIndexStyle != null) {
			XulPropParser.xulParsedProp_Integer zIndex = zIndexStyle.getParsedValue();
			_zIndex = zIndex.val;
		} else {
			_zIndex = 0;
		}

		XulStyle keepFocusVisibleStyle = _view.getStyle(XulPropNameCache.TagId.KEEP_FOCUS_VISIBLE);
		if (keepFocusVisibleStyle != null) {
			XulPropParser.xulParsedProp_booleanValue keepFocusVisible = keepFocusVisibleStyle.getParsedValue();
			_setKeepFocusVisible(keepFocusVisible.val);
		} else {
			_setKeepFocusVisible(false);
		}
	}

	public boolean hasAnimation() {
		if (!_hasAnimation() || _aniTransformDuration <= 0) {
			return false;
		}
		// do not create animation object automatically for reduce the resource usage
		return false || _transformAnimation != null;
	}

	public void doSyncData() {
		if (_isDataChanged()) {
			float initScalarX = _scalarX;
			float initScalarY = _scalarY;
			float initScalarXAlign = _scalarXAlign;
			float initScalarYAlign = _scalarYAlign;
			syncAnimationInfo();
			if (hasAnimation() && !_isBooting()) {
				initTransformAnimation(true);
			}
			this.syncData();
			if (_isBooting()) {
			} else if (_transformAnimation != null) {
				_transformAnimation.storeDest();
				if (!_transformAnimation.identicalValues()) {
					_transformAnimation.restoreSrcValue();
					addAnimation(_transformAnimation);
				}
			} else if (_hasScaleAnimation() && _aniTransformDuration > 0) {
				if (Math.abs(_scalarX - initScalarX) > 0.001f ||
					Math.abs(_scalarY - initScalarY) > 0.001f ||
					Math.abs(_scalarXAlign - initScalarXAlign) > 0.001f ||
					Math.abs(_scalarYAlign - initScalarYAlign) > 0.001f
					) {
					// animation not initialized
					float newScalarX = _scalarX;
					float newScalarY = _scalarY;
					float newScalarXAlign = _scalarXAlign;
					float newScalarYAlign = _scalarYAlign;
					_scalarX = initScalarX;
					_scalarY = initScalarY;
					_scalarXAlign = initScalarXAlign;
					_scalarYAlign = initScalarYAlign;
					initTransformAnimation(true);
					_scalarX = newScalarX;
					_scalarY = newScalarY;
					_scalarXAlign = newScalarXAlign;
					_scalarYAlign = newScalarYAlign;
					_transformAnimation.storeDest();
					_transformAnimation.restoreSrcValue();
					addAnimation(_transformAnimation);
				}
			}
		}
		_setDataUpdated();
	}

	public boolean needPostDraw() {
		return _zIndex > 0 || _view.isFocused();
	}

	protected RectF calScaledRect() {
		RectF rc = _scaledRect;
		if (rc == null) {
			_scaledRect = rc = new RectF();
		} else if (_isScaledRectUpdated()) {
			return rc;
		}

		Rect viewRc = _rect;
		if (Math.abs(_scalarX - 1.0f) < 0.001f && Math.abs(_scalarY - 1.0f) < 0.001f) {
			XulUtils.copyRect(viewRc, rc);
			_setScaledRectUpdated();
			return rc;
		}

		int width = viewRc.right - viewRc.left;
		int height = viewRc.bottom - viewRc.top;

		rc.left = viewRc.left + (1.0f - _scalarX) * _scalarXAlign * width;
		rc.top = viewRc.top + (1.0f - _scalarY) * _scalarYAlign * height;
		rc.right = rc.left + _scalarX * width;
		rc.bottom = rc.top + _scalarY * height;
		_setScaledRectUpdated();
		return rc;
	}

	// 返回值仅作临时变量用
	public RectF getAnimRect() {
		RectF scaledRc = calScaledRect();
		RectF animRc = _animRect;
		if (animRc == null) {
			_animRect = animRc = new RectF();
		}
		animRc.left = scaledRc.left;
		animRc.top = scaledRc.top;
		animRc.right = scaledRc.right;
		animRc.bottom = scaledRc.bottom;
		return animRc;
	}

	public void preDraw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_quiver != null && _quiver.isQuivering()) {
			dc.save();
			dc.translate(_quiver._quiverXDelta, _quiver._quiverYDelta);
		}
		if (_view.isFocused()) {
			getRenderContext().preDrawFocus(this, dc, rect, xBase, yBase);
		}
	}

	public void postDraw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_view.isFocused()) {
			getRenderContext().postDrawFocus(this, dc, rect, xBase, yBase);
		}
		if (_quiver != null && _quiver.isQuivering()) {
			dc.restore();
		}
	}

	public void draw(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		drawBackground(dc, rect, xBase, yBase);
		drawBorder(dc, rect, xBase, yBase);
	}

	public void drawBackgroundNoScale(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		RectF targetRc = getFocusRect();
		XulUtils.offsetRect(targetRc, xBase, yBase);
		internalDrawBackground(dc, targetRc);
	}

	public void drawBackground(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		RectF targetRc = getAnimRect();
		XulUtils.offsetRect(targetRc, _screenX + xBase, _screenY + yBase);
		internalDrawBackground(dc, targetRc);
	}

	protected void internalDrawBackground(XulDC dc, RectF targetRc) {
		if (_bkgInfo != null) {
			XulDrawable bmp = _bkgInfo._bmp;
			if (bmp == null) {
				bmp = _bkgInfo._bmp = XulWorker.loadDrawableFromCache(_bkgInfo.url);
			} else if (bmp.isRecycled()) {
				XulDrawable newBmp = XulWorker.loadDrawableFromCache(_bkgInfo.url);
				if (newBmp != null) {
					bmp = _bkgInfo._bmp = newBmp;
				}
			}
			if (bmp != null) {
				Paint defPicPaint = _ctx.getDefPicPaint();
				dc.drawBitmap(bmp, targetRc, defPicPaint);
				return;
			}
		}

		if ((_bkgColor & 0xFF000000) != 0) {
			Paint defSolidPaint = _ctx.getDefSolidPaint();
			defSolidPaint.setColor(_bkgColor);
			if (_borderRoundX > 0.5 && _borderRoundY > 0.5) {
				dc.drawRoundRect(targetRc, _borderRoundX * _scalarX, _borderRoundY * _scalarY, defSolidPaint);
			} else {
				dc.drawRect(targetRc, defSolidPaint);
			}
		}
	}

	public void drawBorder(XulDC dc, Rect rect, int xBase, int yBase) {
		if (_isInvisible()) {
			return;
		}
		if (_borderSize > 0.1f && (_borderColor & 0xFF000000) != 0) {
			Paint defStrokePaint = _ctx.getDefStrokePaint();
			RectF targetRc = getAnimRect();
			float borderSize = _borderSize * _scalarX;
			defStrokePaint.setStrokeWidth(borderSize);
			defStrokePaint.setColor(_borderColor);
			if (_borderEffect != null) {
				defStrokePaint.setPathEffect(_borderEffect);
			}
			if (_borderRoundX > 0.5 && _borderRoundY > 0.5) {
				float borderDelta = (borderSize / 2) - borderSize * _borderPos;
				float x = targetRc.left + borderDelta + _screenX + xBase;
				float y = targetRc.top + borderDelta + _screenY + yBase;
				float cx = XulUtils.calRectWidth(targetRc) - 2 * borderDelta;
				float cy = XulUtils.calRectHeight(targetRc) - 2 * borderDelta;
				dc.drawRoundRect(x, y, cx, cy, _borderRoundX * _scalarX, _borderRoundY * _scalarY, defStrokePaint);
			} else {
				XulUtils.offsetRect(targetRc, _screenX + xBase, _screenY + yBase);
				dc.drawRect(targetRc, defStrokePaint);
			}
			if (_borderEffect != null) {
				defStrokePaint.setPathEffect(null);
			}
		}
	}

	public void setUpdateAll() {
		markDataChanged();
		setUpdateLayout();
	}

	public boolean setUpdateLayout() {
		return setUpdateLayout(false);
	}

	public boolean setUpdateLayout(boolean updateByChild) {
		if (XulManager.DEBUG && XulLayoutHelper.isLayoutRunning()) {
			Log.w(TAG, "reset layout during doing layout.");
			new Exception().printStackTrace();
		}
		boolean isLayoutChanged = _isLayoutChanged();
		if (updateByChild) {
			_setLayoutChangedByChildren();
		} else {
			_setLayoutChanged();
		}

		if (!isLayoutChanged) {
			updateParentLayout();
		}
		return true;
	}

	public int getHeight() {
		return XulUtils.calRectHeight(_rect);
	}

	public int getWidth() {
		return XulUtils.calRectWidth(_rect);
	}

	public abstract int getDefaultFocusMode();

	// 获取一个未下载的图片任务
	public XulWorker.DrawableItem getPendingImageItem() {
		XulWorker.DrawableItem pendingImageItem = collectPendingImageItem();
		if (pendingImageItem == null) {
			_setPendingItemsLoaded();
		}
		return pendingImageItem;
	}

	// 收集当前元素中的可下载任务
	public XulWorker.DrawableItem collectPendingImageItem() {
		if (_bkgInfo == null) {
			return null;
		}
		if (_bkgInfo._isLoading) {
			return null;
		}
		XulDrawable bmp = _bkgInfo._bmp;
		if (_bkgInfo._forceReload == false && !(bmp == null || bmp.isRecycled())) {
			return null;
		}
		if (TextUtils.isEmpty(_bkgInfo._url)) {
			return null;
		}
		long timestamp = XulUtils.timestamp();
		int loadInterval;
		if (_bkgInfo._loadFailedCounter < 3) {
			// 如果失败次数小于3次则5秒后重试
			loadInterval = 5 * 1000;
		} else {
			// 如果失败次数过多则30分钟重试一次
			loadInterval = 30 * 60 * 1000;
		}
		if (timestamp - _bkgInfo._lastLoadFailedTime < loadInterval) {
			// 一定时间内不重新加载同一图片
			return null;
		}
		_bkgInfo._isLoading = true;
		_bkgInfo.url = _bkgInfo._url;
		_bkgInfo.scalarX = getRenderContext().getXScalar();
		_bkgInfo.scalarY = getRenderContext().getYScalar();
		_bkgInfo.target_width = XulUtils.calRectWidth(_rect);
		_bkgInfo.target_height = XulUtils.calRectHeight(_rect);
		return _bkgInfo;
	}

	// 焦点查找前置钩子
	// 查找当前VIEW的焦点前调用
	public XulView preFindFocus(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		return null;
	}

	// 焦点查找后置钩子
	// 当前VIEW查找焦点失败时调用
	public XulView postFindFocus(XulLayout.FocusDirection direction, RectF srcRc, XulView src) {
		return null;
	}

	public void cleanImageItems() {
		if (_bkgInfo != null) {
			if (_bkgInfo._bmp != null) {
				_bkgInfo._bmp = null;
				_bkgInfo._lastLoadFailedTime = 0;
			}
		}
	}

	public Rect getDrawingRect() {
		return _rect;
	}

	// 返回的Rect只用做临时变量，不应该长时间持有
	public RectF getFocusRect() {
		RectF rect = _focusRect;
		if (rect == null) {
			_focusRect = rect = new RectF();
		}
		Rect viewRc = _rect;
		if (viewRc != null) {
			rect.set(viewRc.left + _screenX, viewRc.top + _screenY, viewRc.right + _screenX, viewRc.bottom + _screenY);
		} else {
			rect.set(_screenX, _screenY, _screenX, _screenY);
		}
		return rect;
	}

	// 返回的Rect只用做临时变量，不应该长时间持有
	public RectF getUpdateRect() {
		RectF rect = _updateRect;
		RectF scaledRect = calScaledRect();
		if (rect == null) {
			_updateRect = rect = new RectF();
		}
		rect.left = scaledRect.left + _screenX;
		rect.top = scaledRect.top + _screenY;
		rect.right = scaledRect.right + _screenX;
		rect.bottom = scaledRect.bottom + _screenY;
		return rect;
	}

	public boolean onChildStateChanged(XulStateChangeEvent event) {
		return false;
	}

	public boolean onChildDoActionEvent(XulActionEvent event) {
		return false;
	}

	public boolean onKeyEvent(KeyEvent event) {
		return false;
	}

	public IXulExternalView getExternalView() {
		return null;
	}

	public void onAnimationFinished(boolean success) {
		popupBlinkClassStack();
	}

	public void popupBlinkClassStack() {
		ArrayList<String[]> blinkClassStack = _blinkClassStack;
		if (blinkClassStack == null) {
			return;
		}
		boolean updated = false;
		while (!blinkClassStack.isEmpty()) {
			final String[] className = blinkClassStack.remove(blinkClassStack.size() - 1);
			for (int i = 0, classNameLength = className.length; i < classNameLength; i++) {
				String name = className[i];
				updated = _view.removeClass(name) || updated;
			}
		}
		if (updated) {
			reset();
		}
		return;
	}

	private ArrayList<String[]> _blinkClassStack;

	public boolean blinkClass(String... clsName) {
		if (clsName == null || clsName.length <= 0) {
			return false;
		}
		boolean updated = false;
		for (int i = 0, clsNameLength = clsName.length; i < clsNameLength; i++) {
			String name = clsName[i];
			updated = _view.addClass(name) || updated;
		}
		if (!updated) {
			return false;
		}
		if (_blinkClassStack == null) {
			_blinkClassStack = new ArrayList<String[]>();
		}
		_blinkClassStack.add(clsName);
		reset();
		return true;
	}

	public boolean setDrawingSkipped(boolean skipped) {
		boolean isSkippedNow = _hasFlags(FLAGS_DRAWING_SKIPPED);
		if (skipped != isSkippedNow) {
			if (skipped) {
				_addFlags(FLAGS_DRAWING_SKIPPED);
			} else {
				_modifyFlags(0, FLAGS_DRAWING_SKIPPED);
			}
			return true;
		}
		return false;
	}

	public boolean isDrawingSkipped() {
		return _hasFlags(FLAGS_DRAWING_SKIPPED);
	}
}
