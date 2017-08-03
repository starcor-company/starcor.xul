package com.starcor.xul;

import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Prop.*;
import com.starcor.xul.Render.XulImageRender;
import com.starcor.xul.Render.XulRenderFactory;
import com.starcor.xul.Script.XulScriptableObject;
import com.starcor.xul.ScriptWrappr.XulImageScriptableObject;
import com.starcor.xul.ScriptWrappr.XulItemScriptableObject;

/**
 * Created by hy on 2014/5/4.
 */
public class XulItem extends XulView {
	public static final XulItem NULL_FOCUS = new XulItem();
	public static final XulItem KEEP_FOCUS = new XulItem();

	public XulItem(XulArea parent) {
		super(parent);
		parent.addChild(this);
	}

	public XulItem(XulArea parent, int pos) {
		super(parent);
		parent.addChild(pos, this);
	}

	public XulItem(XulLayout root) {
		super(root);
		root.addChild(this);
	}

	public XulItem(XulLayout root, XulArea parent) {
		super(root, parent);
		parent.addChild(this);
	}

	public XulItem() {
	}

	public XulItem makeClone(XulArea parent) {
		return makeClone(parent, -1);
	}

	public XulItem makeClone(XulArea parent, int pos) {
		XulItem xulItem = new XulItem(parent, pos);
		xulItem.copyContent(this);
		return xulItem;
	}

	@Override
	public void prepareRender(XulRenderContext ctx, boolean preload) {
		if (_render != null) {
			return;
		}

		_render = XulRenderFactory.createRender("item", _type, ctx, this, preload);
		_render.onRenderCreated();
	}

	public static class _Builder extends ItemBuilder {
		XulItem _item;
		XulTemplate _ownerTemplate;

		private void init(XulLayout layout) {
			_item = new XulItem(layout._root, layout);
		}

		private void init(XulArea area) {
			_item = new XulItem(area._root, area);
		}

		private void init(XulTemplate template) {
			_item = new XulItem();
			_ownerTemplate = template;
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_item._id = attrs.getValue("id");
			_item.setClass(attrs.getValue("class"));
			_item._type = XulUtils.getCachedString(attrs.getValue("type"));
			_item._binding = XulUtils.getCachedString(attrs.getValue("binding"));
			_item._desc = attrs.getValue("desc");
			if (_ownerTemplate != null) {
				_ownerTemplate.addChild(_item, attrs.getValue("filter"));
			}
			return true;
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			if ("action".equals(name)) {
				XulAction._Builder builder = XulAction._Builder.create(_item);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("data".equals(name)) {
				XulData._Builder builder = XulData._Builder.create(_item);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("attr".equals(name)) {
				XulAttr._Builder builder = XulAttr._Builder.create(_item);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("style".equals(name)) {
				XulStyle._Builder builder = XulStyle._Builder.create(_item);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("focus".equals(name)) {
				XulFocus._Builder builder = XulFocus._Builder.create(_item);
				builder.initialize(name, attrs);
				return builder;
			}

			return XulManager.CommonDummyBuilder;
		}

		@Override
		public Object finalItem() {
			XulItem item = _item;
			_Builder.recycle(this);
			return item;
		}

		static public _Builder create(XulLayout select) {
			_Builder builder = create();
			builder.init(select);
			return builder;

		}

		public static _Builder create(XulArea view) {
			_Builder builder = create();
			builder.init(view);
			return builder;
		}

		public static _Builder create(XulTemplate view) {
			_Builder builder = create();
			builder.init(view);
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
			_recycled_builder._item = null;
			_recycled_builder._ownerTemplate = null;
		}
	}

	@Override
	protected XulScriptableObject createScriptableObject() {
		if ("image".equals(_type) || _render instanceof XulImageRender) {
			return new XulImageScriptableObject(this);
		}
		XulItemScriptableObject scriptableObject = new XulItemScriptableObject(this);
		return scriptableObject;
	}
}
