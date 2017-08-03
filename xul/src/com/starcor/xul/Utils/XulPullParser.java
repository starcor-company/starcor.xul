package com.starcor.xul.Utils;

import android.text.TextUtils;

import java.io.InputStream;

/**
 * Created by hy on 2014/12/4.
 */
public class XulPullParser {
	public static final int EVENT_ERROR = -1;
	public static final int START_DOCUMENT = 0;
	public static final int END_DOCUMENT = 1;
	public static final int START_TAG = 2;
	public static final int END_TAG = 3;
	public static final int TEXT = 4;
	public static final int CDSECT = 5;

	static String[] _NAME_ID_MAP_ = new String[]{
		"",
		"action",
		"area",
		"attr",
		"background-color",
		"background-image",
		"binding",
		"bindingUpdated",
		"bindingFinished",
		"blur",
		"border",
		"check",
		"checked",
		"class",
		"click",
		"custom",
		"data",
		"desc",
		"direction",
		"event",
		"focus",
		"focusable",
		"font-align",
		"font-color",
		"font-shadow",
		"font-size",
		"font-style-italic",
		"font-style-underline",
		"font-weight",
		"grid",
		"group",
		"height",
		"id",
		"image",
		"img.0",
		"img.0.align",
		"img.0.auto_hide",
		"img.0.height",
		"img.0.mode",
		"img.0.padding",
		"img.0.round_rect",
		"img.0.shadow",
		"img.0.visible",
		"img.0.width",
		"img.1",
		"img.1.align",
		"img.1.auto_hide",
		"img.1.height",
		"img.1.mode",
		"img.1.padding",
		"img.1.round_rect",
		"img.1.shadow",
		"img.1.visible",
		"img.1.width",
		"img.2",
		"img.2.align",
		"img.2.auto_hide",
		"img.2.height",
		"img.2.mode",
		"img.2.padding",
		"img.2.round_rect",
		"img.2.shadow",
		"img.2.visible",
		"img.2.width",
		"img.3",
		"img.3.align",
		"img.3.auto_hide",
		"img.3.height",
		"img.3.mode",
		"img.3.padding",
		"img.3.round_rect",
		"img.3.shadow",
		"img.3.visible",
		"img.3.width",
		"item",
		"label",
		"layout",
		"load",
		"margin",
		"margin-bottom",
		"margin-left",
		"margin-right",
		"margin-top",
		"marquee",
		"name",
		"nofocus",
		"padding",
		"padding-bottom",
		"padding-left",
		"padding-right",
		"padding-top",
		"page",
		"priority",
		"radio",
		"ready",
		"script/javascript",
		"slider",
		"style",
		"template",
		"text",
		"type",
		"unchecked",
		"width",
		"x",
		"starcor.xul",
		"y",
		"z-index",
	};


	static {
		System.loadLibrary("starcor_xul");
	}

	private long _parser;

	private int _curToken;

	private native long _load_xml(InputStream stream);

	private native int _next_event(long parser);

	private native int _reset(long parser);

	private native int _get_attr_count(long parser);

	private native String _get_attr_value(long parser, String name);

	private native String _get_attr_value(long parser, int idx);

	private native String _get_attr_name(long parser, int idx);

	private native String _get_tag_name(long parser);

	private native String _get_text(long parser);

	private native int _get_tag_id(long parser);

	private native int _get_attr_name_id(long parser, int idx);

	private native int _get_attr_value_id(long parser, int idx);

	private native int _get_attr_value_id(long parser, String name);

	private native long _store_position(long parser);

	private native int _load_position(long parser, long store_point);

	private native void _release(long parser);

	public XulPullParser(InputStream stream) {
		_parser = _load_xml(stream);
	}

	@Override
	protected void finalize() throws Throwable {
		_release(_parser);
		super.finalize();
	}

	public int nextToken() {
		_curToken = _next_event(_parser);
		return _curToken;
	}

	public String getName() {
		int id = _get_tag_id(_parser);
		if (id < 0) {
			return _get_tag_name(_parser);
		}
		return _NAME_ID_MAP_[id];
	}

	public String getAttributeValue(String namespace, String name) {
		int id = -1;//_get_attr_value_id(_parser, name);
		if (id < 0) {
			return _get_attr_value(_parser, name);
		}
		return _NAME_ID_MAP_[id];
	}

	public String getAttributeValue(int idx) {
		int id = _get_attr_value_id(_parser, idx);
		if (id < 0) {
			return _get_attr_value(_parser, idx);
		}
		return _NAME_ID_MAP_[id];
	}

	public String getAttributeName(int idx) {
		int id = _get_attr_name_id(_parser, idx);
		if (id < 0) {
			return _get_attr_name(_parser, idx);
		}
		return _NAME_ID_MAP_[id];
	}

	public int getAttributeCount() {
		return _get_attr_count(_parser);
	}

	public String getText() {
		String s = _get_text(_parser);
		if (_curToken == CDSECT && !TextUtils.isEmpty(s)) {
			int beginPos = s.startsWith(" ") ? 1 : 0;
			int length = s.length();
			int endPos = length > 1 && s.endsWith(" ") ? length - 1 : length;
			if (beginPos == 1 || endPos != length) {
				s = s.substring(beginPos, endPos);
			}
		}
		return s;
	}

	public int loadPosition(Object store_point) {
		return _load_position(_parser, (Long) store_point);
	}

	public Object storePosition() {
		return _store_position(_parser);
	}
}
