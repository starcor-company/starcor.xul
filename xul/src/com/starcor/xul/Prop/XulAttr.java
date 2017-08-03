package com.starcor.xul.Prop;

import com.starcor.xul.*;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/4.
 */
public class XulAttr extends XulProp {
	Object _parsedVal;

	XulAttr() {
	}

	XulAttr(XulAttr orgProp) {
		super(orgProp);
		_parsedVal = orgProp._parsedVal;
	}

	XulAttr(String name) {
		_nameId = XulPropNameCache.name2Id(name);
	}

	XulAttr(int nameId) {
		_nameId = nameId;
	}

	public static XulAttr obtain(int nameId) {
		XulAttr xulAttr = new XulAttr(nameId);
		xulAttr._referent = false;
		return xulAttr;
	}

	public static XulAttr obtain(String name) {
		XulAttr xulAttr = new XulAttr(name);
		xulAttr._referent = false;
		return xulAttr;
	}

	public XulAttr makeClone() {
		if (isBinding()) {
			return new XulAttr(this);
		}
		return this;
	}

	@Override
	public void setValue(Object value) {
		_parsedVal = null;
		super.setValue(value);
	}

	public <T> T getParsedValue() {
		if (_parsedVal == null) {
			_parsedVal = XulPropParser.parse(this);
		}
		if (_parsedVal == null) {
			return null;
		}
		return (T) _parsedVal;
	}

	public static class _Builder extends ItemBuilder {
		XulAttr _attr;
		FinalCallback<XulAttr> _callback;
		String _content;
		ArrayList<XulDataNode> _dataContent;

		private void init(final XulView area) {
			_attr = new XulAttr();
			_callback = new FinalCallback<XulAttr>() {
				@Override
				public void onFinal(XulAttr prop) {
					area.addInplaceProp(prop);
				}
			};
			_content = null;
			_dataContent = null;
		}

		private void init(final XulTemplate template) {
			_attr = new XulAttr();
			_callback = new FinalCallback<XulAttr>() {
				@Override
				public void onFinal(XulAttr prop) {
					template.addProp(prop);
				}
			};
			_content = null;
			_dataContent = null;
		}

		private void init(final XulSelect select) {
			_attr = new XulAttr();
			_callback = new FinalCallback<XulAttr>() {
				@Override
				public void onFinal(XulAttr prop) {
					select.addProp(prop);
				}
			};
			_content = null;
			_dataContent = null;
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_attr._nameId = XulPropNameCache.name2Id(attrs.getValue("name"));
			_attr._desc = attrs.getValue("desc");
			_attr._binding = XulUtils.getCachedString(attrs.getValue("binding"));
			return true;
		}

		@Override
		public Object finalItem() {
			if (_dataContent != null) {
				_attr.setValue(_dataContent);
			} else if (_content != null) {
				_attr._value = XulUtils.getCachedString(_content);
			}
			XulAttr attr = _attr;
			FinalCallback<XulAttr> callback = _callback;

			_Builder.recycle(this);

			callback.onFinal(attr);
			return attr;
		}

		@Override
		public boolean pushText(String path, XulFactory.IPullParser parser) {
			if (_content == null) {
				_content = parser.getText();
			} else {
				_content += parser.getText();
			}
			return true;
		}

		FinalCallback<XulDataNode> _xulDataNodeCallback = new FinalCallback<XulDataNode>() {

			@Override
			public void onFinal(XulDataNode obj) {
				if (_dataContent == null) {
					_dataContent = new ArrayList<XulDataNode>();
				}
				XulDataNode dataNode = obj;
				_dataContent.add(dataNode);
			}
		};

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			XulDataNode._Builder builder = XulDataNode._Builder.create(name, _xulDataNodeCallback);

			builder.initialize(name, attrs);
			return builder;
		}


		public static _Builder create(XulView area) {
			_Builder builder = create();
			builder.init(area);
			return builder;
		}

		public static _Builder create(XulTemplate template) {
			_Builder builder = create();
			builder.init(template);
			return builder;
		}

		public static _Builder create(XulSelect select) {
			_Builder builder = create();
			builder.init(select);
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
			_recycled_builder._callback = null;
			_recycled_builder._attr = null;
			_recycled_builder._content = null;
			_recycled_builder._dataContent = null;
		}
	}
}
