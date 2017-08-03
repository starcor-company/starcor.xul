package com.starcor.xul.Factory;

import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.XulUtils;

/**
 * Created by hy on 2015/4/28.
 */
public class XulParserCachedTag extends XulFactory.Attributes {
	String _name;
	XulCachedHashMap<String, String> _attrs = null;
	int _length = 0;

	@Override
	public String getValue(String name) {
		if (_attrs == null) {
			return null;
		}
		return _attrs.get(name);
	}

	@Override
	public String getValue(int i) {
		return null;
	}

	@Override
	public int getLength() {
		return _length;
	}

	@Override
	public String getName(int i) {
		return null;
	}

	public XulParserCachedTag(String tag_name, XulFactory.Attributes attrs) {
		_name = XulUtils.getCachedString(tag_name);
		if (attrs == null) {
			return;
		}
		int attrsLength = attrs.getLength();
		_length = attrsLength;
		if (attrsLength == 0) {
			return;
		}
		_attrs = new XulCachedHashMap<String, String>();

		for (int i = 0; i < attrsLength; i++) {
			String name = XulUtils.getCachedString(attrs.getName(i));
			String value = XulUtils.getCachedString(attrs.getValue(i));
			_attrs.put(name, value);
		}
	}

	public String getTagName() {
		return _name;
	}
}
