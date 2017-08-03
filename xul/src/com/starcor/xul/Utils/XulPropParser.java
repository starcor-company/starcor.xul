package com.starcor.xul.Utils;

import android.graphics.Bitmap;
import android.graphics.DashPathEffect;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulProp;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Prop.XulStyle;
import com.starcor.xul.Render.XulImageRender;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by hy on 2014/6/13.
 */
public class XulPropParser {
	private static final String TAG = XulPropParser.class.getSimpleName();

	abstract static class ParseInvoker {
		abstract <T extends XulProp> Object doParse(T prop);
	}

	static XulCachedHashMap<String, ParseInvoker> _xulStyleParserMap = new XulCachedHashMap<String, ParseInvoker>();
	static XulCachedHashMap<String, ParseInvoker> _xulAttrParserMap = new XulCachedHashMap<String, ParseInvoker>();

	static public class xulParsedAttr_WidthHeight {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_WidthHeight val = new xulParsedAttr_WidthHeight();
			String strVal = prop.getStringValue();
			if (TextUtils.isEmpty(strVal) || "auto".equals(strVal)) {
				val.val = XulManager.SIZE_AUTO;
			} else if ("match_parent".equals(strVal)) {
				val.val = XulManager.SIZE_MATCH_PARENT;
			} else if ("match_content".equals(strVal)) {
				val.val = XulManager.SIZE_AUTO;
			} else {
				val.val = XulUtils.tryParseInt(strVal, XulManager.SIZE_AUTO);
			}
			return val;
		}

		public int val;
	}

	static public class xulParsedAttr_Img_Align {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_Img_Align val = new xulParsedAttr_Img_Align();
			String[] shadowParams = prop.getStringValue().split(",");
			if (shadowParams.length == 2) {
				val.xAlign = XulUtils.tryParseFloat(shadowParams[0], 0.5f);
				val.yAlign = XulUtils.tryParseFloat(shadowParams[1], 0.5f);
			} else if (shadowParams.length == 1) {
				val.xAlign = val.yAlign = XulUtils.tryParseFloat(shadowParams[0], 0.5f);
			}
			return val;
		}

		public float xAlign = 0.5f;
		public float yAlign = 0.5f;
	}

	static public class xulParsedAttr_Img_Shadow {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_Img_Shadow val = new xulParsedAttr_Img_Shadow();
			String[] shadowParams = prop.getStringValue().split(",");
			if (shadowParams.length == 4) {
				val.xOffset = XulUtils.tryParseFloat(shadowParams[0], 0);
				val.yOffset = XulUtils.tryParseFloat(shadowParams[1], 0);
				val.size = XulUtils.tryParseFloat(shadowParams[2], 0);
				val.color = (int) XulUtils.tryParseHex(shadowParams[3], 0xFF000000);
			} else if (shadowParams.length == 3) {
				val.xOffset = XulUtils.tryParseFloat(shadowParams[0], 0);
				val.yOffset = XulUtils.tryParseFloat(shadowParams[1], 0);
				val.size = XulUtils.tryParseFloat(shadowParams[2], 0);
			} else if (shadowParams.length == 2) {
				val.size = XulUtils.tryParseFloat(shadowParams[0], 0);
				val.color = (int) XulUtils.tryParseHex(shadowParams[1], 0xFF000000);
			} else if (shadowParams.length == 1) {
				val.size = XulUtils.tryParseFloat(shadowParams[0], 0);
			}
			return val;
		}

		public float xOffset = 0;
		public float yOffset = 0;
		public float size = 0;
		public int color = 0xFF000000;
	}

	static public class xulParsedAttr_Text_Marquee {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_Text_Marquee val = new xulParsedAttr_Text_Marquee();

			while (true) {
				String[] marqueeParam = prop.getStringValue().split(",");
				int baseIdx = 0;
				if (marqueeParam.length == 5) {
					if ("inverse".equals(marqueeParam[0])) {
						val.direction = -1;
					}
					baseIdx = 1;
				} else if (marqueeParam.length == 4) {

				} else {
					break;
				}

				String speed = marqueeParam[baseIdx + 0];
				String delay = marqueeParam[baseIdx + 1];
				String interval = marqueeParam[baseIdx + 2];
				String space = marqueeParam[baseIdx + 3];

				val.speed = XulUtils.tryParseInt(speed, 0);
				val.delay = XulUtils.tryParseInt(delay, 0);
				val.interval = XulUtils.tryParseInt(interval, 0);
				if (space.endsWith("%")) {
					val.space = -XulUtils.tryParseInt(space.substring(0, space.length() - 1), 0);
				} else {
					val.space = XulUtils.tryParseInt(space, 0);
				}

				if (val.speed != 0 && val.speed < 100) {
					val.speed = 100;
				}
				break;
			}
			return val;
		}

		public int speed = 0;
		public int delay = 0;
		public int interval = 0;
		public int space = 0;
		public int direction = 1;
	}

	static public class xulParsedAttr_Img_SizeVal {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_Img_SizeVal val = new xulParsedAttr_Img_SizeVal();
			String stringValue = prop.getStringValue();
			if ("match_content".equals(stringValue)) {
				val.val = XulManager.SIZE_MATCH_CONTENT;
			} else if ("match_parent".equals(stringValue)) {
				val.val = XulManager.SIZE_MATCH_PARENT;
			} else {
				val.val = XulUtils.tryParseInt(stringValue);
			}
			return val;
		}

		public int val;
	}

	static public class xulParsedAttr_Img_AutoHide {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_Img_AutoHide val = new xulParsedAttr_Img_AutoHide();
			String stringValue = prop.getStringValue();
			if ("below".equals(stringValue)) {
				val.enabled = true;
				val.target = -1;
			} else if (!TextUtils.isEmpty(stringValue) && TextUtils.isDigitsOnly(stringValue)) {
				val.enabled = true;
				val.target = XulUtils.tryParseInt(stringValue);
				val.target = Math.min(Math.max(-1, val.target), XulImageRender._MaxImgLayers);
			} else {
				val.enabled = false;
			}
			return val;
		}

		public boolean enabled;
		public int target;
	}

	static public class xulParsedAttr_Img_FadeIn {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_Img_FadeIn val = new xulParsedAttr_Img_FadeIn();
			String stringValue = prop.getStringValue();
			if ("true".equals(stringValue) || "enabled".equals(stringValue)) {
				val.enabled = true;
				val.duration = 300;
			} else if (TextUtils.isEmpty(stringValue) || "disabled".equals(stringValue) || "false".equals(stringValue)) {
				val.enabled = false;
				val.duration = 0;
			} else if (TextUtils.isDigitsOnly(stringValue)) {
				val.enabled = true;
				val.duration = XulUtils.tryParseInt(stringValue, 300);
			} else {
				val.enabled = false;
				val.duration = 300;
			}
			return val;
		}

		public boolean enabled;
		public int duration;
	}

	static public class xulParsedAttr_Img_PixFmt {
		public static Object doParse(XulProp prop) {
			xulParsedAttr_Img_PixFmt val = new xulParsedAttr_Img_PixFmt();
			String stringValue = prop.getStringValue();
			if (TextUtils.isEmpty(stringValue) || "default".equals(stringValue)) {
				val.fmt = XulManager.DEF_PIXEL_FMT;
			} else if ("rgba32".equals(stringValue) || "rgb32".equals(stringValue) || "rgba8888".equals(stringValue)) {
				val.fmt = Bitmap.Config.ARGB_8888;
			} else if ("rgb16".equals(stringValue) || "rgb565".equals(stringValue)) {
				val.fmt = Bitmap.Config.RGB_565;
			} else if ("rgb24".equals(stringValue) || "rgb888".equals(stringValue)) {
				val.fmt = Bitmap.Config.ARGB_8888;
			} else {
				val.fmt = XulManager.DEF_PIXEL_FMT;
			}
			return val;
		}

		public Bitmap.Config fmt;
	}

	static public class xulParsedAttr_Img_RoundRect {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_Img_RoundRect val = new xulParsedAttr_Img_RoundRect();
			String stringValue = prop.getStringValue();
			if (stringValue != null) {
				String[] strVals = stringValue.split(",");
				if (strVals.length == 2 || strVals.length == 4 || strVals.length == 8) {
					val.val = new float[strVals.length];
					for (int i = 0, strValsLength = strVals.length; i < strValsLength; i++) {
						String strVal = strVals[i];
						val.val[i] = XulUtils.tryParseFloat(strVal, Float.NaN);
					}
				}
			}
			return val;
		}

		float[] val;
		double xScalar;
		double yScalar;
		float[] scaledVals;

		public float[] getRoundRadius(double xScalar, double yScalar) {
			if (val == null) {
				return null;
			}
			if (scaledVals != null && Math.abs(this.xScalar - xScalar) < 0.0001f && Math.abs(this.yScalar - yScalar) < 0.0001f) {
				return scaledVals;
			}
			float[] result;
			if (val.length == 4) {
				result = new float[8];
				for (int i = 0, valLength = val.length; i < valLength; i++) {
					float v = val[i];
					result[i * 2] = v;
					result[i * 2 + 1] = v;
				}
			} else {
				result = val.clone();
			}

			for (int i = 0, resultLength = result.length; i < resultLength; i += 2) {
				result[i] *= xScalar;
				result[i + 1] *= yScalar;

			}
			scaledVals = result;
			this.xScalar = xScalar;
			this.yScalar = yScalar;
			return result;
		}
	}

	static public class xulParsedAttr_XY {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_XY val = new xulParsedAttr_XY();
			String strVal = prop.getStringValue();
			if (TextUtils.isEmpty(strVal) || "auto".equals(strVal)) {
				val.val = XulManager.SIZE_AUTO;
			} else {
				val.val = XulUtils.tryParseInt(strVal, XulManager.SIZE_AUTO);
			}
			return val;
		}

		public int val;
	}

	static public class xulParsedAttr_Direction {
		public static Object doParse(XulAttr prop) {
			xulParsedAttr_Direction val = new xulParsedAttr_Direction();
			String direction = prop.getStringValue();

			if ("vertical".equals(direction)) {
				val.vertical = true;
				val.reverse = false;
			} else if ("horizontal".equals(direction)) {
				val.vertical = false;
				val.reverse = false;
			} else if ("reverse-vertical".equals(direction)) {
				val.vertical = true;
				val.reverse = true;
			} else if ("reverse-horizontal".equals(direction)) {
				val.vertical = false;
				val.reverse = true;
			} else {
				val.vertical = false;
				val.reverse = false;
			}
			return val;
		}

		public boolean vertical;
		public boolean reverse;
	}

	static public class xulParsedProp_PaddingMargin {
		public int left;
		public int top;
		public int right;
		public int bottom;

		public static Object doParse(XulProp prop) {
			String paddingStr = prop.getStringValue();
			String[] paddingParam = paddingStr.split(",");
			int leftVal, rightVal, topVal, bottomVal;
			if (paddingParam.length == 1) {
				int paddingVal = XulUtils.tryParseInt(paddingParam[0], 0);
				leftVal = rightVal = topVal = bottomVal = paddingVal;
			} else if (paddingParam.length == 2) {
				int paddingVal1 = XulUtils.tryParseInt(paddingParam[0], 0);
				int paddingVal2 = XulUtils.tryParseInt(paddingParam[1], 0);
				topVal = paddingVal1;
				leftVal = paddingVal2;
				rightVal = paddingVal2;
				bottomVal = paddingVal1;
			} else if (paddingParam.length == 3) {
				int paddingVal1 = XulUtils.tryParseInt(paddingParam[0], 0);
				int paddingVal2 = XulUtils.tryParseInt(paddingParam[1], 0);
				int paddingVal3 = XulUtils.tryParseInt(paddingParam[2], 0);
				topVal = paddingVal1;
				leftVal = paddingVal2;
				rightVal = paddingVal2;
				bottomVal = paddingVal3;
			} else if (paddingParam.length == 4) {
				topVal = XulUtils.tryParseInt(paddingParam[0], 0);
				leftVal = XulUtils.tryParseInt(paddingParam[1], 0);
				rightVal = XulUtils.tryParseInt(paddingParam[2], 0);
				bottomVal = XulUtils.tryParseInt(paddingParam[3], 0);
			} else {
				leftVal = rightVal = topVal = bottomVal = 0;
			}
			xulParsedProp_PaddingMargin padding = new xulParsedProp_PaddingMargin();
			padding.left = leftVal;
			padding.right = rightVal;
			padding.top = topVal;
			padding.bottom = bottomVal;
			return padding;
		}
	}

	static public class xulParsedStyle_PaddingMarginVal {
		public int val;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_PaddingMarginVal val = new xulParsedStyle_PaddingMarginVal();
			val.val = XulUtils.tryParseInt(prop.getStringValue());
			return val;
		}
	}

	static public class xulParsedProp_booleanValue {
		public boolean val;

		public static Object doParse(XulProp prop) {
			xulParsedProp_booleanValue val = new xulParsedProp_booleanValue();
			val.val = ("true".equals(prop.getStringValue())) || ("enabled".equals(prop.getStringValue()));
			return val;
		}
	}

	static public class xulParsedAttr_Enabled {
		public boolean val;

		public static Object doParse(XulProp prop) {
			xulParsedAttr_Enabled val = new xulParsedAttr_Enabled();
			// 只在字面值为false或disable时设置enable属性为false
			val.val = !("false".equals(prop.getStringValue()) || "disabled".equals(prop.getStringValue()));
			return val;
		}
	}

	static public class xulParsedStyle_Display {
		public static Object doParse(XulStyle prop) {
			xulParsedStyle_Display val = new xulParsedStyle_Display();
			if ("none".equals(prop.getStringValue())) {
				val.mode = DisplayMode.None;
			} else {
				val.mode = DisplayMode.Block;
			}
			return val;
		}

		public enum DisplayMode {
			None,
			Block
		}

		public DisplayMode mode;
	}

	static public class xulParsedStyle_DoNotMatchText {
		public static Object doParse(XulStyle prop) {
			xulParsedStyle_DoNotMatchText val = new xulParsedStyle_DoNotMatchText();
			String propVal = prop.getStringValue();
			if (propVal == null) {
				val.doNotMatchWidth = false;
				val.doNotMatchHeight = false;
			} else {
				String[] vals = propVal.split(",");
				switch (vals.length) {
				case 0:
					val.doNotMatchWidth = false;
					val.doNotMatchHeight = false;
					break;
				case 1:
					val.doNotMatchWidth = val.doNotMatchHeight = vals[0].trim().equals("true");
					break;
				case 2:
				default:
					val.doNotMatchWidth = vals[0].trim().equals("true");
					val.doNotMatchHeight = vals[1].trim().equals("true");
					break;
				}
			}
			return val;
		}

		public boolean doNotMatchWidth;
		public boolean doNotMatchHeight;
	}

	static public class xulParsedStyle_Scale {
		public float xScalar = 1.0f;
		public float yScalar = 1.0f;
		public float xAlign = 0.5f;
		public float yAlign = 0.5f;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_Scale val = new xulParsedStyle_Scale();
			String scaleStr = prop.getStringValue();

			if (TextUtils.isEmpty(scaleStr)) {
			} else {
				String[] scaleParams = scaleStr.split(",");
				switch (scaleParams.length) {
				case 1:
					val.xScalar = val.yScalar = XulUtils.tryParseFloat(scaleParams[0], 1.0f);
					val.xAlign = val.yAlign = 0.5f;
					break;
				case 2:
					val.xScalar = XulUtils.tryParseFloat(scaleParams[0], 1.0f);
					val.yScalar = XulUtils.tryParseFloat(scaleParams[1], 1.0f);
					val.xAlign = val.yAlign = 0.5f;
					break;
				case 3:
					val.xScalar = XulUtils.tryParseFloat(scaleParams[0], 1.0f);
					val.yScalar = XulUtils.tryParseFloat(scaleParams[0], 1.0f);
					val.xAlign = XulUtils.tryParseFloat(scaleParams[1], 0.5f);
					val.yAlign = XulUtils.tryParseFloat(scaleParams[2], 0.5f);
					break;
				case 4:
					val.xScalar = XulUtils.tryParseFloat(scaleParams[0], 1.0f);
					val.yScalar = XulUtils.tryParseFloat(scaleParams[1], 1.0f);
					val.xAlign = XulUtils.tryParseFloat(scaleParams[2], 0.5f);
					val.yAlign = XulUtils.tryParseFloat(scaleParams[3], 0.5f);
					break;
				}
			}
			return val;
		}
	}

	static public class xulParsedStyle_FontSize {
		public float val;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_FontSize val = new xulParsedStyle_FontSize();
			val.val = XulUtils.tryParseFloat(prop.getStringValue(), 24.0f);
			return val;
		}
	}

	static public class xulParsedStyle_FontScaleX {
		public float val;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_FontScaleX val = new xulParsedStyle_FontScaleX();
			val.val = XulUtils.tryParseFloat(prop.getStringValue(), 1.0f);
			return val;
		}
	}

	static public class xulParsedStyle_LineHeight {
		public float val;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_LineHeight val = new xulParsedStyle_LineHeight();
			val.val = XulUtils.tryParseFloat(prop.getStringValue(), 1.0f);
			return val;
		}
	}

	static public class xulParsedStyle_FontStyleStrike {
		public boolean val;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_FontStyleStrike val = new xulParsedStyle_FontStyleStrike();
			val.val = "true".equals(prop.getStringValue());
			return val;
		}
	}

	static public class xulParsedStyle_FontColor {
		public int val;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_FontColor val = new xulParsedStyle_FontColor();
			val.val = (int) XulUtils.tryParseHex(prop.getStringValue(), 0);
			return val;
		}
	}

	static public class xulParsedStyle_FontWeight {
		public float val;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_FontWeight val = new xulParsedStyle_FontWeight();
			val.val = XulUtils.tryParseFloat(prop.getStringValue(), 24.0f);
			return val;
		}
	}

	static public class xulParsedStyle_FontAlign {
		public float xAlign = 0;
		public float yAlign = 0;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_FontAlign val = new xulParsedStyle_FontAlign();
			String[] alignParams = prop.getStringValue().split(",");
			if (alignParams.length == 2) {
				val.xAlign = XulUtils.tryParseFloat(alignParams[0], 0);
				val.yAlign = XulUtils.tryParseFloat(alignParams[1], 0);
			} else if (alignParams.length == 1) {
				val.xAlign = val.yAlign = XulUtils.tryParseFloat(alignParams[0], 0);
			}
			return val;
		}
	}

	static public class xulParsedStyle_FontShadow {
		public float xOffset = 0;
		public float yOffset = 0;
		public float size = 0;
		public int color = 0xFF000000;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_FontShadow val = new xulParsedStyle_FontShadow();
			String[] fontShadowParams = prop.getStringValue().split(",");
			if (fontShadowParams.length == 4) {
				val.xOffset = XulUtils.tryParseFloat(fontShadowParams[0], 0);
				val.yOffset = XulUtils.tryParseFloat(fontShadowParams[1], 0);
				val.size = XulUtils.tryParseFloat(fontShadowParams[2], 0);
				val.color = (int) XulUtils.tryParseHex(fontShadowParams[3], 0);
			}
			return val;
		}
	}

	static public class xulParsedAttr_AnimationMode {
		public String mode;
		public float[] params;

		public static Object doParse(XulProp prop) {
			xulParsedAttr_AnimationMode val = new xulParsedAttr_AnimationMode();
			String aniModeAttr = prop.getStringValue();


			if (!TextUtils.isEmpty(aniModeAttr)) {
				int pos = aniModeAttr.indexOf(':');
				float[] vals = val.params = new float[6];
				if (pos > 0) {
					String[] params = aniModeAttr.substring(pos + 1).split(",");
					aniModeAttr = aniModeAttr.substring(0, pos);
					int paramsLength = Math.min(params.length, vals.length);
					for (int i = 0; i < paramsLength; i++) {
						String param = params[i];
						vals[i] = XulUtils.tryParseFloat(param, 1f);
					}
					Arrays.fill(vals, paramsLength, vals.length, 1.0f);
				} else {
					Arrays.fill(vals, 1.0f);
				}
				val.mode = aniModeAttr;
			}
			return val;
		}
	}

	static public class xulParsedStyle_FixHalfChar {
		public boolean val;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_FixHalfChar val = new xulParsedStyle_FixHalfChar();
			val.val = "true".equals(prop.getStringValue());
			return val;
		}
	}

	static public class xulParsedStyle_AnimationTextChange {
		public boolean val = true;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_AnimationTextChange val = new xulParsedStyle_AnimationTextChange();
			val.val = true;
			return val;
		}
	}

	static public class xulParsedStyle_BackgroundImage {
		public String url;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_BackgroundImage val = new xulParsedStyle_BackgroundImage();
			val.url = prop.getStringValue().trim();
			return val;
		}
	}

	static public class xulParsedStyle_BackgroundColor {
		public int val;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_BackgroundColor val = new xulParsedStyle_BackgroundColor();
			val.val = (int) XulUtils.tryParseHex(prop.getStringValue(), 0);
			return val;
		}
	}

	static public class xulParsedStyle_Border {
		public float size = 0;
		public float pos = 0.5f;
		public float xRadius = 0;
		public float yRadius = 0;
		public int color = 0xFF000000;

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_Border val = new xulParsedStyle_Border();
			String[] borderParams = prop.getStringValue().split(",");
			if (borderParams.length == 5) {
				val.size = XulUtils.tryParseFloat(borderParams[0], 0);
				val.color = (int) XulUtils.tryParseHex(borderParams[1], 0);
				val.xRadius = XulUtils.tryParseFloat(borderParams[2], 0);
				val.yRadius = XulUtils.tryParseFloat(borderParams[3], 0);
				val.pos = XulUtils.tryParseFloat(borderParams[4], 0);
			} else if (borderParams.length == 4) {
				val.size = XulUtils.tryParseFloat(borderParams[0], 0);
				val.color = (int) XulUtils.tryParseHex(borderParams[1], 0);
				val.xRadius = XulUtils.tryParseFloat(borderParams[2], 0);
				val.yRadius = XulUtils.tryParseFloat(borderParams[3], 0);
			} else if (borderParams.length == 3) {
				val.size = XulUtils.tryParseFloat(borderParams[0], 0);
				val.color = (int) XulUtils.tryParseHex(borderParams[1], 0);
				val.pos = XulUtils.tryParseFloat(borderParams[2], 0);
			} else if (borderParams.length == 2) {
				val.size = XulUtils.tryParseFloat(borderParams[0], 0);
				val.color = (int) XulUtils.tryParseHex(borderParams[1], 0);
			} else if (borderParams.length == 1) {
				val.size = XulUtils.tryParseFloat(borderParams[0], 0);
			}
			return val;
		}
	}

	static public class xulParsedStyle_Border_Dash_Pattern {
		public float phase = 0.0f;
		public float[] pattern;
		private ArrayList<Pair<Integer, DashPathEffect>> _cachedEffectObject;
		static final int MAX_CACHED_EFFECT_OBJECT = 4;

		public DashPathEffect getEffectObjectByXYScalar(float xScalar, float yScalar) {
			if (pattern == null) {
				return null;
			}
			if (_cachedEffectObject == null) {
				_cachedEffectObject = new ArrayList<Pair<Integer, DashPathEffect>>();
			}
			int scalarVal = XulUtils.roundToInt(xScalar * 100);
			for (int i = 0; i < _cachedEffectObject.size(); i++) {
				Pair<Integer, DashPathEffect> effectPair = _cachedEffectObject.get(i);
				if (effectPair.first == scalarVal) {
					if (i != 0) {
						_cachedEffectObject.remove(i);
						_cachedEffectObject.add(0, effectPair);
					}
					return effectPair.second;
				}
			}
			// not found
			while (_cachedEffectObject.size() >= MAX_CACHED_EFFECT_OBJECT) {
				_cachedEffectObject.remove(_cachedEffectObject.size() - 1);
			}

			float[] scaledPattern = new float[pattern.length];
			for (int i = 0; i < pattern.length; i++) {
				float v = pattern[i];
				scaledPattern[i] = v * xScalar;
			}
			DashPathEffect effect = new DashPathEffect(scaledPattern, phase * xScalar);
			_cachedEffectObject.add(Pair.create(scalarVal, effect));
			return effect;
		}

		public static Object doParse(XulStyle prop) {
			xulParsedStyle_Border_Dash_Pattern val = new xulParsedStyle_Border_Dash_Pattern();
			String[] patternParams = prop.getStringValue().split(",");
			if (patternParams.length < 2) {
			} else if (patternParams.length == 2) {
				val.pattern = new float[2];
				val.pattern[0] = XulUtils.tryParseFloat(patternParams[0], 5.0f);
				val.pattern[1] = XulUtils.tryParseFloat(patternParams[1], 5.0f);
			} else {
				val.pattern = new float[patternParams.length - 1];
				val.phase = XulUtils.tryParseFloat(patternParams[0], 0.0f);
				for (int i = 0; i < val.pattern.length; i++) {

					val.pattern[i] = XulUtils.tryParseFloat(patternParams[i + 1], 5.0f);
				}
			}
			return val;
		}
	}

	static public class xulParsedProp_Integer {
		public int val;

		public static Object doParse(XulProp prop) {
			xulParsedProp_Integer val = new xulParsedProp_Integer();
			val.val = XulUtils.tryParseInt(prop.getStringValue(), 0);
			return val;
		}
	}

	static public class xulParsedProp_IntegerArray {
		public int[] val;

		public static Object doParse(XulProp prop) {
			xulParsedProp_IntegerArray val = new xulParsedProp_IntegerArray();
			String stringValue = prop.getStringValue();
			if (stringValue != null) {
				String[] strVals = stringValue.split(",");
				val.val = new int[strVals.length];
				for (int i = 0, strValsLength = strVals.length; i < strValsLength; i++) {
					String strVal = strVals[i];
					val.val[i] = XulUtils.tryParseInt(strVal, 0);
				}
			}
			return val;
		}
	}

	static public class xulParsedProp_Float {
		public float val;

		public static Object doParse(XulProp prop) {
			xulParsedProp_Float val = new xulParsedProp_Float();
			val.val = XulUtils.tryParseFloat(prop.getStringValue(), Float.NaN);
			return val;
		}

		public float tryGetVal(float defVal) {
			if (Float.isNaN(val)) {
				return defVal;
			}
			return val;
		}
	}

	static public class xulParsedProp_FloatArray {
		public float[] val;

		public static Object doParse(XulProp prop) {
			xulParsedProp_FloatArray val = new xulParsedProp_FloatArray();
			String stringValue = prop.getStringValue();
			if (stringValue != null) {
				String[] strVals = stringValue.split(",");
				val.val = new float[strVals.length];
				for (int i = 0, strValsLength = strVals.length; i < strValsLength; i++) {
					String strVal = strVals[i];
					val.val[i] = XulUtils.tryParseFloat(strVal, Float.NaN);
				}
			}
			return val;
		}

		public float tryGetVal(int idx, float defVal) {
			if (idx >= val.length) {
				return defVal;
			}
			float v = val[idx];
			if (Float.isNaN(v)) {
				return defVal;
			}
			return v;
		}

		public int getLength() {
			if (val == null) {
				return 0;
			}
			return val.length;
		}
	}

	static public class xulParsedProp_HexArray {
		public long[] val;

		public static Object doParse(XulProp prop) {
			xulParsedProp_HexArray val = new xulParsedProp_HexArray();
			String stringValue = prop.getStringValue();
			if (stringValue != null) {
				String[] strVals = stringValue.split(",");
				val.val = new long[strVals.length];
				for (int i = 0, strValsLength = strVals.length; i < strValsLength; i++) {
					String strVal = strVals[i];
					val.val[i] = XulUtils.tryParseHex(strVal, Long.MIN_VALUE);
				}
			}
			return val;
		}

		public long tryGetVal(int idx, long defVal) {
			if (idx >= val.length) {
				return defVal;
			}
			long v = val[idx];
			if (Long.MIN_VALUE == v) {
				return defVal;
			}
			return v;
		}

		public int getLength() {
			if (val == null) {
				return 0;
			}
			return val.length;
		}
	}

	static {

		_xulStyleParserMap.put("animation-text-change", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_AnimationTextChange.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("fix-half-char", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_FixHalfChar.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("padding", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_PaddingMargin.doParse(prop);
			}
		});

		_xulStyleParserMap.put("margin", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_PaddingMargin.doParse(prop);
			}
		});

		_xulStyleParserMap.put("preferred-focus-padding", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_PaddingMargin.doParse(prop);
			}
		});

		_xulStyleParserMap.put("do-not-match-text", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_DoNotMatchText.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("padding-left", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_PaddingMarginVal.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("padding-top", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_PaddingMarginVal.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("padding-right", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_PaddingMarginVal.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("padding-bottom", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_PaddingMarginVal.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("margin-left", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_PaddingMarginVal.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("margin-top", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_PaddingMarginVal.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("margin-right", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_PaddingMarginVal.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("margin-bottom", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_PaddingMarginVal.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("animation-scale", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});

		_xulStyleParserMap.put("display", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_Display.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("scale", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_Scale.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("line-height", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_LineHeight.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("font-size", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_FontSize.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("font-weight", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_FontWeight.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("font-color", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_FontColor.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("font-align", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_FontAlign.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("font-shadow", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_FontShadow.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("font-scale-x", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_FontScaleX.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("font-style-strike", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_FontStyleStrike.doParse((XulStyle) prop);
			}
		});

		_xulStyleParserMap.put("font-style-italic", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});

		_xulStyleParserMap.put("font-style-underline", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});

		_xulStyleParserMap.put("start-indent", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});

		_xulStyleParserMap.put("end-indent", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});

		_xulStyleParserMap.put("font-resample", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});

		_xulStyleParserMap.put("background-color", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_BackgroundColor.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("background-image", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_BackgroundImage.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("border", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_Border.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("border-dash-pattern", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedStyle_Border_Dash_Pattern.doParse((XulStyle) prop);
			}
		});
		_xulStyleParserMap.put("z-index", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Integer.doParse(prop);
			}
		});

		_xulStyleParserMap.put("opacity", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});

		_xulStyleParserMap.put("rotate-x", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});

		_xulStyleParserMap.put("rotate-y", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});

		_xulStyleParserMap.put("rotate-z", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});

		_xulStyleParserMap.put("rotate", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});

		_xulStyleParserMap.put("rotate-center-x", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});

		_xulStyleParserMap.put("rotate-center-y", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});

		_xulStyleParserMap.put("rotate-center-z", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});

		_xulStyleParserMap.put("rotate-center", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});

		_xulStyleParserMap.put("translate-x", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});

		_xulStyleParserMap.put("translate-y", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});

		_xulStyleParserMap.put("translate", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});

		_xulStyleParserMap.put("round-rect", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});

		_xulStyleParserMap.put("quiver", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});

		_xulStyleParserMap.put("quiver-mode", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_AnimationMode.doParse(prop);
			}
		});

		_xulStyleParserMap.put("lighting-color-filter", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_HexArray.doParse(prop);
			}
		});

		_xulStyleParserMap.put("preload", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});

		_xulStyleParserMap.put("keep-focus-visible", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});

		_xulStyleParserMap.put("max-width", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Integer.doParse(prop);
			}
		});

		_xulStyleParserMap.put("max-height", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Integer.doParse(prop);
			}
		});

		_xulStyleParserMap.put("min-width", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Integer.doParse(prop);
			}
		});

		_xulStyleParserMap.put("min-height", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Integer.doParse(prop);
			}
		});
	}


	static {
		{
			ParseInvoker imgAlignParserInvoker = new ParseInvoker() {
				@Override
				<T extends XulProp> Object doParse(T prop) {
					return xulParsedAttr_Img_Align.doParse((XulAttr) prop);
				}
			};
			ParseInvoker imgRoundRectParserInvoker = new ParseInvoker() {
				@Override
				<T extends XulProp> Object doParse(T prop) {
					return xulParsedAttr_Img_RoundRect.doParse((XulAttr) prop);
				}
			};
			ParseInvoker imgShadowParserInvoker = new ParseInvoker() {
				@Override
				<T extends XulProp> Object doParse(T prop) {
					return xulParsedAttr_Img_Shadow.doParse((XulAttr) prop);
				}
			};
			ParseInvoker imgSizeValParserInvoker = new ParseInvoker() {
				@Override
				<T extends XulProp> Object doParse(T prop) {
					return xulParsedAttr_Img_SizeVal.doParse((XulAttr) prop);
				}
			};
			ParseInvoker imgPaddingParserInvoker = new ParseInvoker() {
				@Override
				<T extends XulProp> Object doParse(T prop) {
					return xulParsedProp_PaddingMargin.doParse(prop);
				}
			};
			ParseInvoker imgFadeInParserInvoker = new ParseInvoker() {
				@Override
				<T extends XulProp> Object doParse(T prop) {
					return xulParsedAttr_Img_FadeIn.doParse((XulAttr) prop);
				}
			};
			ParseInvoker imgAutoHideParserInvoker = new ParseInvoker() {
				@Override
				<T extends XulProp> Object doParse(T prop) {
					return xulParsedAttr_Img_AutoHide.doParse((XulAttr) prop);
				}
			};
			ParseInvoker imgReuseParserInvoker = new ParseInvoker() {
				@Override
				<T extends XulProp> Object doParse(T prop) {
					return xulParsedProp_booleanValue.doParse(prop);
				}
			};
			ParseInvoker imgPixFmtParserInvoker = new ParseInvoker() {
				@Override
				<T extends XulProp> Object doParse(T prop) {
					return xulParsedAttr_Img_PixFmt.doParse(prop);
				}
			};
			for (int i = 0; i < XulImageRender._MaxImgLayers; i++) {
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXAlign[i]), imgAlignParserInvoker);
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXWidth[i]), imgSizeValParserInvoker);
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXHeight[i]), imgSizeValParserInvoker);
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXShadow[i]), imgShadowParserInvoker);
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXRoundRect[i]), imgRoundRectParserInvoker);
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXPadding[i]), imgPaddingParserInvoker);
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXAutoHide[i]), imgAutoHideParserInvoker);
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXFadeIn[i]), imgFadeInParserInvoker);
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXReuse[i]), imgReuseParserInvoker);
				_xulAttrParserMap.put(XulPropNameCache.id2Name(XulImageRender._imgXPixFmt[i]), imgPixFmtParserInvoker);
			}
		}

		_xulAttrParserMap.put("auto-scroll", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});

		_xulAttrParserMap.put("marquee", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_Text_Marquee.doParse((XulAttr) prop);
			}
		});

		_xulAttrParserMap.put("direction", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_Direction.doParse((XulAttr) prop);
			}
		});

		_xulAttrParserMap.put("x", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_XY.doParse((XulAttr) prop);
			}
		});

		_xulAttrParserMap.put("y", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_XY.doParse((XulAttr) prop);
			}
		});

		_xulAttrParserMap.put("scroll-pos-x", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_XY.doParse((XulAttr) prop);
			}
		});

		_xulAttrParserMap.put("scroll-pos-y", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_XY.doParse((XulAttr) prop);
			}
		});

		_xulAttrParserMap.put("width", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_WidthHeight.doParse((XulAttr) prop);
			}
		});

		_xulAttrParserMap.put("height", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_WidthHeight.doParse((XulAttr) prop);
			}
		});

		_xulAttrParserMap.put("enabled", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_Enabled.doParse(prop);
			}

		});

		_xulAttrParserMap.put("animation", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});

		_xulAttrParserMap.put("animation-mode", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedAttr_AnimationMode.doParse(prop);
			}
		});
		_xulAttrParserMap.put("max-layers", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Integer.doParse(prop);
			}
		});

		_xulAttrParserMap.put("align", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});
		_xulAttrParserMap.put("lock-focus", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});
		_xulAttrParserMap.put("minimum-item", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Integer.doParse(prop);
			}
		});
		_xulAttrParserMap.put("cache-pages", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_Float.doParse(prop);
			}
		});
		_xulAttrParserMap.put("animation-moving", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}

		});
		_xulAttrParserMap.put("animation-sizing", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});
		_xulAttrParserMap.put("loop", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});
		_xulAttrParserMap.put("indicator.align", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_FloatArray.doParse(prop);
			}
		});
		_xulAttrParserMap.put("indicator", new ParseInvoker() {
			@Override
			<T extends XulProp> Object doParse(T prop) {
				return xulParsedProp_booleanValue.doParse(prop);
			}
		});
	}

	static public Object parse(XulAttr attr) {
		ParseInvoker parserInvoker = _xulAttrParserMap.get(attr.getName());
		if (parserInvoker == null) {
			Log.w(TAG, "unsupported attr " + attr);
			return null;
		}
		return parserInvoker.doParse(attr);
	}

	static public Object parse(XulStyle style) {
		ParseInvoker parserInvoker = _xulStyleParserMap.get(style.getName());
		if (parserInvoker == null) {
			Log.w(TAG, "unsupported style " + style);
			return null;
		}
		return parserInvoker.doParse(style);
	}
}
