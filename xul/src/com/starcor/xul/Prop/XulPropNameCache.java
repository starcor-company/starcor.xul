package com.starcor.xul.Prop;

import com.starcor.xul.Utils.XulArrayList;

/**
 * Created by hy on 2015/1/20.
 */
public class XulPropNameCache {
	public static class TagId {
		// 0~31
		public static final int PADDING = name2Id("padding");
		public static final int PADDING_LEFT = name2Id("padding-left");
		public static final int PADDING_TOP = name2Id("padding-top");
		public static final int PADDING_RIGHT = name2Id("padding-right");
		public static final int PADDING_BOTTOM = name2Id("padding-bottom");
		public static final int DISPLAY = name2Id("display");
		public static final int WIDTH = name2Id("width");
		public static final int HEIGHT = name2Id("height");
		public static final int X = name2Id("x");
		public static final int Y = name2Id("y");
		public static final int FOCUS = name2Id("focus");
		public static final int ALIGN = name2Id("align");
		public static final int DIRECTION = name2Id("direction");

		public static final int BORDER = name2Id("border");
		public static final int BACKGROUND_COLOR = name2Id("background-color");
		public static final int BACKGROUND_IMAGE = name2Id("background-image");

		public static final int SCALE = name2Id("scale");
		public static final int ANIMATION = name2Id("animation");
		public static final int ANIMATION_DURATION = name2Id("animation-duration");
		public static final int ANIMATION_MODE = name2Id("animation-mode");
		public static final int COMPONENT = name2Id("component");

		public static final int TEXT = name2Id("text");
		public static final int FONT_SIZE = name2Id("font-size");
		public static final int FONT_COLOR = name2Id("font-color");
		public static final int FONT_ALIGN = name2Id("font-align");
		public static final int FONT_FACE = name2Id("font-face");
		public static final int FONT_SHADOW = name2Id("font-shadow");
		public static final int FONT_WEIGHT = name2Id("font-weight");
		public static final int FONT_STYLE_UNDERLINE = name2Id("font-style-underline");
		public static final int FONT_STYLE_ITALIC = name2Id("font-style-italic");

		public static final int Z_INDEX = name2Id("z-index");
		public static final int PREFERRED_FOCUS_PADDING = name2Id("preferred-focus-padding");

		// 32~63
		public static final int IMAGE_0 = name2Id("img.0");
		public static final int IMAGE_0_WIDTH = name2Id("img.0.width");
		public static final int IMAGE_0_HEIGHT = name2Id("img.0.height");
		public static final int IMAGE_0_VISIBLE = name2Id("img.0.visible");
		public static final int IMAGE_0_MODE = name2Id("img.0.mode");
		public static final int IMAGE_0_ROUND_RECT = name2Id("img.0.round-rect");
		public static final int IMAGE_0_PADDING = name2Id("img.0.padding");
		public static final int IMAGE_0_ALIGN = name2Id("img.0.align");

		public static final int IMAGE_1 = name2Id("img.1");
		public static final int IMAGE_1_WIDTH = name2Id("img.1.width");
		public static final int IMAGE_1_HEIGHT = name2Id("img.1.height");
		public static final int IMAGE_1_VISIBLE = name2Id("img.1.visible");
		public static final int IMAGE_1_MODE = name2Id("img.1.mode");
		public static final int IMAGE_1_ROUND_RECT = name2Id("img.1.round-rect");
		public static final int IMAGE_1_PADDING = name2Id("img.1.padding");
		public static final int IMAGE_1_ALIGN = name2Id("img.1.align");

		public static final int IMAGE_2 = name2Id("img.2");
		public static final int IMAGE_2_WIDTH = name2Id("img.2.width");
		public static final int IMAGE_2_HEIGHT = name2Id("img.2.height");
		public static final int IMAGE_2_VISIBLE = name2Id("img.2.visible");
		public static final int IMAGE_2_MODE = name2Id("img.2.mode");
		public static final int IMAGE_2_ROUND_RECT = name2Id("img.2.round-rect");
		public static final int IMAGE_2_PADDING = name2Id("img.2.padding");
		public static final int IMAGE_2_ALIGN = name2Id("img.2.align");

		public static final int IMAGE_3 = name2Id("img.3");
		public static final int IMAGE_3_WIDTH = name2Id("img.3.width");
		public static final int IMAGE_3_HEIGHT = name2Id("img.3.height");
		public static final int IMAGE_3_VISIBLE = name2Id("img.3.visible");
		public static final int IMAGE_3_MODE = name2Id("img.3.mode");
		public static final int IMAGE_3_ROUND_RECT = name2Id("img.3.round-rect");
		public static final int IMAGE_3_PADDING = name2Id("img.3.padding");
		public static final int IMAGE_3_ALIGN = name2Id("img.3.align");

		// 64~95
		public static final int IMAGE_4 = name2Id("img.4");
		public static final int IMAGE_4_WIDTH = name2Id("img.4.width");
		public static final int IMAGE_4_HEIGHT = name2Id("img.4.height");
		public static final int IMAGE_4_VISIBLE = name2Id("img.4.visible");
		public static final int IMAGE_4_MODE = name2Id("img.4.mode");
		public static final int IMAGE_4_ROUND_RECT = name2Id("img.4.round-rect");
		public static final int IMAGE_4_PADDING = name2Id("img.4.padding");
		public static final int IMAGE_4_ALIGN = name2Id("img.4.align");

		public static final int IMAGE_5 = name2Id("img.5");
		public static final int IMAGE_5_WIDTH = name2Id("img.5.width");
		public static final int IMAGE_5_HEIGHT = name2Id("img.5.height");
		public static final int IMAGE_5_VISIBLE = name2Id("img.5.visible");
		public static final int IMAGE_5_MODE = name2Id("img.5.mode");
		public static final int IMAGE_5_ROUND_RECT = name2Id("img.5.round-rect");
		public static final int IMAGE_5_PADDING = name2Id("img.5.padding");
		public static final int IMAGE_5_ALIGN = name2Id("img.5.align");

		public static final int IMAGE_6 = name2Id("img.6");
		public static final int IMAGE_6_WIDTH = name2Id("img.6.width");
		public static final int IMAGE_6_HEIGHT = name2Id("img.6.height");
		public static final int IMAGE_6_VISIBLE = name2Id("img.6.visible");
		public static final int IMAGE_6_MODE = name2Id("img.6.mode");
		public static final int IMAGE_6_ROUND_RECT = name2Id("img.6.round-rect");
		public static final int IMAGE_6_PADDING = name2Id("img.6.padding");
		public static final int IMAGE_6_ALIGN = name2Id("img.6.align");

		public static final int IMAGE_7 = name2Id("img.7");
		public static final int IMAGE_7_WIDTH = name2Id("img.7.width");
		public static final int IMAGE_7_HEIGHT = name2Id("img.7.height");
		public static final int IMAGE_7_VISIBLE = name2Id("img.7.visible");
		public static final int IMAGE_7_MODE = name2Id("img.7.mode");
		public static final int IMAGE_7_ROUND_RECT = name2Id("img.7.round-rect");
		public static final int IMAGE_7_PADDING = name2Id("img.7.padding");
		public static final int IMAGE_7_ALIGN = name2Id("img.7.align");

		// 96+
		public static final int IMAGE_0_FADE_IN = name2Id("img.0.fade-in");
		public static final int IMAGE_0_AUTO_HIDE = name2Id("img.0.auto-hide");
		public static final int IMAGE_0_SHADOW = name2Id("img.0.shadow");

		public static final int IMAGE_1_FADE_IN = name2Id("img.1.fade-in");
		public static final int IMAGE_1_AUTO_HIDE = name2Id("img.1.auto-hide");
		public static final int IMAGE_1_SHADOW = name2Id("img.1.shadow");

		public static final int IMAGE_2_FADE_IN = name2Id("img.2.fade-in");
		public static final int IMAGE_2_AUTO_HIDE = name2Id("img.2.auto-hide");
		public static final int IMAGE_2_SHADOW = name2Id("img.2.shadow");

		public static final int IMAGE_3_FADE_IN = name2Id("img.3.fade-in");
		public static final int IMAGE_3_AUTO_HIDE = name2Id("img.3.auto-hide");
		public static final int IMAGE_3_SHADOW = name2Id("img.3.shadow");

		public static final int IMAGE_4_FADE_IN = name2Id("img.4.fade-in");
		public static final int IMAGE_4_AUTO_HIDE = name2Id("img.4.auto-hide");
		public static final int IMAGE_4_SHADOW = name2Id("img.4.shadow");

		public static final int IMAGE_5_FADE_IN = name2Id("img.5.fade-in");
		public static final int IMAGE_5_AUTO_HIDE = name2Id("img.5.auto-hide");
		public static final int IMAGE_5_SHADOW = name2Id("img.5.shadow");

		public static final int IMAGE_6_FADE_IN = name2Id("img.6.fade-in");
		public static final int IMAGE_6_AUTO_HIDE = name2Id("img.6.auto-hide");
		public static final int IMAGE_6_SHADOW = name2Id("img.6.shadow");

		public static final int IMAGE_7_FADE_IN = name2Id("img.7.fade-in");
		public static final int IMAGE_7_AUTO_HIDE = name2Id("img.7.auto-hide");
		public static final int IMAGE_7_SHADOW = name2Id("img.7.shadow");

		public static final int MAX_LAYERS = name2Id("max-layers");
		public static final int PRELOAD = name2Id("preload");
		public static final int QUIVER = name2Id("quiver");
		public static final int QUIVER_MODE = name2Id("quiver-mode");
		public static final int LINE_HEIGHT = name2Id("line-height");
		public static final int BORDER_DASH_PATTERN = name2Id("border-dash-pattern");
		public static final int FONT_STYLE_STRIKE = name2Id("font-style-strike");
		public static final int FONT_SCALE_X = name2Id("font-scale-x");
		public static final int START_INDENT = name2Id("start-indent");
		public static final int END_INDENT = name2Id("end-indent");
		public static final int FONT_RESAMPLE = name2Id("font-resample");

		public static final int MARGIN = name2Id("margin");
		public static final int MARGIN_LEFT = name2Id("margin-left");
		public static final int MARGIN_TOP = name2Id("margin-top");
		public static final int MARGIN_RIGHT = name2Id("margin-right");
		public static final int MARGIN_BOTTOM = name2Id("margin-bottom");

		public static final int AUTO_SCROLL = name2Id("auto-scroll");
		public static final int SCROLLBAR = name2Id("scrollbar");
		public static final int INDICATOR = name2Id("indicator");
		public static final int INDICATOR_GAP = name2Id("indicator.gap");
		public static final int INDICATOR_STYLE = name2Id("indicator.style");
		public static final int INDICATOR_ALIGN = name2Id("indicator.align");
		public static final int INDICATOR_LEFT = name2Id("indicator.left");
		public static final int INDICATOR_RIGHT = name2Id("indicator.right");
		public static final int INDICATOR_UP = name2Id("indicator.up");
		public static final int INDICATOR_DOWN = name2Id("indicator.down");
		public static final int ANIMATION_SCALE = name2Id("animation-scale");

		public static final int KEEP_FOCUS_VISIBLE = name2Id("keep-focus-visible");
		public static final int ACTION_COMPONENT_CHANGED = name2Id("componentChanged");
		public static final int ACTION_COMPONENT_INSTANCED = name2Id("componentInstanced");
		public static final int ACTION_MASSIVE_UPDATED = name2Id("massiveUpdated");
		public static final int MINIMUM_ITEM = name2Id("minimum-item");
		public static final int CACHE_PAGES = name2Id("cache-pages");
		public static final int DRAWING_CACHE = name2Id("drawing-cache");

		public static final int STYLE_TRANSLATE = name2Id("translate");
		public static final int STYLE_TRANSLATE_X = name2Id("translate-x");
		public static final int STYLE_TRANSLATE_Y = name2Id("translate-y");
		public static final int STYLE_OPACITY = name2Id("opacity");

		public static final int STYLE_ROTATE = name2Id("rotate");
		public static final int STYLE_ROTATE_X = name2Id("rotate-x");
		public static final int STYLE_ROTATE_Y = name2Id("rotate-y");
		public static final int STYLE_ROTATE_Z = name2Id("rotate-z");

		public static final int STYLE_ROTATE_CENTER = name2Id("rotate-center");
		public static final int STYLE_ROTATE_CENTER_X = name2Id("rotate-center-x");
		public static final int STYLE_ROTATE_CENTER_Y = name2Id("rotate-center-y");
		public static final int STYLE_ROTATE_CENTER_Z = name2Id("rotate-center-z");

		public static final int STYLE_ROUND_RECT = name2Id("round-rect");
		public static final int STYLE_LIGHTING_COLOR_FILTER = name2Id("lighting-color-filter");


		public static final int STYLE_FIX_HALF_CHAR = name2Id("fix-half-char");
		public static final int STYLE_ANIMATE_TEXT_CHANGE = name2Id("animation-text-change");
		public static final int MULTI_LINE = name2Id("multi-line");
		public static final int AUTO_WRAP = name2Id("auto-wrap");
		public static final int ELLIPSIS = name2Id("ellipsis");
		public static final int MARQUEE = name2Id("marquee");
		public static final int ARRANGEMENT = name2Id("arrangement");

		public static final int CLIP_CHILDREN = name2Id("clip-children");
		public static final int CLIP_FOCUS = name2Id("clip-focus");

		public static final int ANIMATION_SIZING = name2Id("animation-sizing");
		public static final int ANIMATION_MOVING = name2Id("animation-moving");

		public static final int LOOP = name2Id("loop");
		public static final int LOCK_FOCUS = name2Id("lock-focus");
		public static final int LOCK_FOCUS_TARGET = name2Id("lock-focus-target");
		public static final int PRELOAD_PAGE = name2Id("preload-page");
		public static final int IMAGE_GC = name2Id("image-gc");
		public static final int SWITCHING_MODE = name2Id("switching-mode");
		public static final int PEEK_NEXT_PAGE = name2Id("peek-next-page");
		public static final int ANIMATION_TYPE = name2Id("animation-type");
		public static final int ANIMATION_SPEED = name2Id("animation-speed");
		public static final int INIT_PAGE = name2Id("init-page");
		public static final int LAYOUT_MODE = name2Id("layout-mode");
		public static final int FONT_RENDER = name2Id("font-render");
		public static final int MAX_WIDTH = name2Id("max-width");
		public static final int MAX_HEIGHT = name2Id("max-height");
		public static final int MIN_WIDTH = name2Id("min-width");
		public static final int MIN_HEIGHT = name2Id("min-height");

		public static final int IMAGE_0_PIX_FMT = name2Id("img.0.pixfmt");
		public static final int IMAGE_1_PIX_FMT = name2Id("img.1.pixfmt");
		public static final int IMAGE_2_PIX_FMT = name2Id("img.2.pixfmt");
		public static final int IMAGE_3_PIX_FMT = name2Id("img.3.pixfmt");
		public static final int IMAGE_4_PIX_FMT = name2Id("img.4.pixfmt");
		public static final int IMAGE_5_PIX_FMT = name2Id("img.5.pixfmt");
		public static final int IMAGE_6_PIX_FMT = name2Id("img.6.pixfmt");
		public static final int IMAGE_7_PIX_FMT = name2Id("img.7.pixfmt");

		// 需要统一整理tag排序
		public static final int ENABLED = name2Id("enabled");

		public static final int ACTION_READY = name2Id("ready");
		public static final int ACTION_SCROLL_STOPPED = name2Id("scrollStopped");
		public static final int DISABLE_GLOBAL_BINDING = name2Id("disable-global-binding");
		public static final int DO_NOT_MATCH_TEXT = name2Id("do-not-match-text");

		public static final int SCROLL_POS_X = name2Id("scroll-pos-x");
		public static final int SCROLL_POS_Y = name2Id("scroll-pos-y");
		public static final int LOCK_DYNAMIC_FOCUS = name2Id("lock-dynamic-focus");

		static void init() {
		}
	}

	static private XulArrayList<String> _nameArray = new XulArrayList<String>(384);

	static class NameIdPair {
		String name;
		int id;

		public NameIdPair(String name, int id) {
			this.name = name;
			this.id = id;
		}
	}
	static private NameIdPair[] _nameMap = new NameIdPair[2048];
	static private NameIdPair[] _nameMap2 = new NameIdPair[2048];
	static private int _nameMapSize = 0;
	static private int _nameMap2Size = 0;

	static {
		TagId.init();
	}

	public static int name2Id(String name) {
		if (name == null) {
			return -1;
		}
		int hashCode = name.hashCode();
		if (hashCode == 0) {
			return -1;
		}
		int idx = hashCode & 0x7FF;
		int id;
		NameIdPair val = _nameMap[idx];
		if (val == null) {
			id = _nameArray.size();
			_nameArray.add(name);
			_nameMap[idx] = new NameIdPair(name, id);
			++_nameMapSize;
		} else if (val.name.equals(name)) {
			id = val.id;
		} else {
			idx = (hashCode >> 11) & 0x7FF;
			val = _nameMap2[idx];
			if (val == null) {
				id = _nameArray.size();
				_nameArray.add(name);
				_nameMap2[idx] = new NameIdPair(name, id);
				++_nameMap2Size;
			} else if (val.name.equals(name)) {
				id = val.id;
			} else {
				id = _nameArray.lastIndexOf(name);
			}
		}
		return id;
	}

	public static String id2Name(int id) {
		if (id < 0 || id >= _nameArray.size()) {
			return null;
		}
		return _nameArray.get(id);
	}
}
