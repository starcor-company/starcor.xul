package com.starcor.xul;

import android.text.TextUtils;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Prop.*;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Factory.XulFactory.Attributes;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/5.
 */
public class XulSelect {
	ArrayList<XulProp> _prop = new ArrayList<XulProp>();
	ArrayList<XulSelect> _selectors;

	String _id;
	String _class;
	String _type;
	int _state = -1;

	String _selectKey;

	public void addProp(XulProp prop) {
		_prop.add(prop);
	}

	public void setPriorityLevel(int priorityLevel, int baseLevel) {
		int complexLevel = 0;
		complexLevel += TextUtils.isEmpty(_id) ? 0 : 1;
		complexLevel += TextUtils.isEmpty(_class) ? 0 : 1;
		complexLevel += TextUtils.isEmpty(_type) ? 0 : 1;
		complexLevel += (_state <= 0) ? 0 : 1;
		complexLevel *= 0x10000;
		if (_state == XulView.STATE_DISABLED) {
			complexLevel += 0x8000;
		}
		priorityLevel = complexLevel + priorityLevel;

		for (int i = 0; i < _prop.size(); i++) {
			XulProp prop = _prop.get(i);
			prop.setPriority(priorityLevel + baseLevel);
		}
	}

	public String getSelectKey() {
		if (_selectKey != null) {
			return _selectKey;
		}
		StringBuilder sb = new StringBuilder();
		if (!TextUtils.isEmpty(_id)) {
			sb.append("#").append(_id);
		}
		if (!TextUtils.isEmpty(_class)) {
			sb.append(".").append(_class);
		}
		if (!TextUtils.isEmpty(_type)) {
			sb.append("@").append(_type);
		}
		_selectKey = sb.toString();
		return _selectKey;
	}

	public void apply(XulView xulView) {
		int size = _prop.size();
		for (int i = 0; i < size; i++) {
			XulProp prop = _prop.get(i);
			if (prop instanceof XulAttr) {
				xulView.addIndirectProp((XulAttr) prop, _state);
			} else if (prop instanceof XulStyle) {
				xulView.addIndirectProp((XulStyle) prop, _state);
			} else if (prop instanceof XulAction) {
				xulView.addIndirectProp((XulAction) prop, _state);
			} else if (prop instanceof XulFocus) {
				// 选择器绑定焦点时，不绑定状态
				xulView.addIndirectProp((XulFocus) prop);
			} else {
				// TODO: 添加未支持的属性
			}
		}
	}

	public void unApply(XulView xulView) {
		for (int i = 0; i < _prop.size(); i++) {
			XulProp prop = _prop.get(i);
			if (prop instanceof XulAttr) {
				xulView.removeIndirectProp((XulAttr) prop, _state);
			} else if (prop instanceof XulStyle) {
				xulView.removeIndirectProp((XulStyle) prop, _state);
			} else if (prop instanceof XulAction) {
				xulView.removeIndirectProp((XulAction) prop, _state);
			} else if (prop instanceof XulFocus) {
				// 选择器绑定焦点时，不绑定状态
				xulView.removeIndirectProp((XulFocus) prop);
			} else {
				// TODO: 添加未支持的属性
			}
		}
	}

	public static class _Builder extends ItemBuilder {
		XulSelect _select;

		private void init(XulManager mgr) {
			_select = new XulSelect();
			mgr.addSelector(_select);
		}

		private void init(XulPage page) {
			_select = new XulSelect();
			page.addSelector(_select);
		}

		private void init(XulComponent component) {
			_select = new XulSelect();
			component.addSelector(_select);
		}

		private void init(XulFocus focus, String direction) {
			_select = new XulSelect();
			focus.bindNextFocus(direction, _select);
		}

		private void init(XulSelect select) {
			_select = new XulSelect();
			select.addSelector(_select);
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_select._id = attrs.getValue("id");
			_select._class = XulUtils.getCachedString(attrs.getValue("class"));
			_select._type = XulUtils.getCachedString(attrs.getValue("type"));
			_select._state = XulView.stateFromString(attrs.getValue("state"));
			return true;
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			if ("select".equals(name)) {
				XulSelect._Builder builder = XulSelect._Builder.create(_select);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("action".equals(name)) {
				XulAction._Builder builder = XulAction._Builder.create(_select);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("data".equals(name)) {
				XulData._Builder builder = XulData._Builder.create(_select);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("attr".equals(name)) {
				XulAttr._Builder builder = XulAttr._Builder.create(_select);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("style".equals(name)) {
				XulStyle._Builder builder = XulStyle._Builder.create(_select);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("focus".equals(name)) {
				XulFocus._Builder builder = XulFocus._Builder.create(_select);
				builder.initialize(name, attrs);
				return builder;
			}

			return XulManager.CommonDummyBuilder;
		}

		@Override
		public boolean pushText(String path, XulFactory.IPullParser parser) {
			return super.pushText(path, parser);
		}

		@Override
		public Object finalItem() {
			XulSelect select = _select;
			_Builder.recycle(this);
			return select;
		}

		static public _Builder create(XulSelect select) {
			_Builder builder = create();
			builder.init(select);
			return builder;

		}

		public static _Builder create(XulFactory.ResultBuilderContext _ctx, XulManager manager) {
			_Builder builder = create();
			builder.init(manager);
			return builder;
		}

		public static _Builder create(XulPage page) {
			_Builder builder = create();
			builder.init(page);
			return builder;
		}

		public static _Builder create(XulComponent component) {
			_Builder builder = create();
			builder.init(component);
			return builder;
		}

		public static _Builder create(XulFocus focus, String direction) {
			_Builder builder = create();
			builder.init(focus, direction);
			return builder;
		}

		private static _Builder create() {
			_Builder builder = _recycled_builder;
			if (builder == null) {
				builder = new _Builder();
			} else {
				_recycled_builder = null;
			}
			return builder;
		}

		private static _Builder _recycled_builder;

		private static void recycle(_Builder builder) {
			_recycled_builder = builder;
			_recycled_builder._select = null;
		}
	}

	private void addSelector(XulSelect select) {
		if (_selectors == null) {
			_selectors = new ArrayList<XulSelect>();
		}
		_selectors.add(select);
	}
}
