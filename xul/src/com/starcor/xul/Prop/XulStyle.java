package com.starcor.xul.Prop;

import com.starcor.xul.*;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;

/**
 * Created by hy on 2014/5/4.
 */
public class XulStyle extends XulProp {
	Object _parsedVal;

	XulStyle() {
	}

	XulStyle(String name) {
		_nameId = XulPropNameCache.name2Id(name);
	}

	XulStyle(int nameId) {
		_nameId = nameId;
	}

	XulStyle(XulStyle orgProp) {
		super(orgProp);
		_parsedVal = orgProp._parsedVal;
	}

	public static XulStyle obtain(int nameId) {
		XulStyle xulStyle = new XulStyle(nameId);
		xulStyle._referent = false;
		return xulStyle;
	}

	public static XulStyle obtain(String name) {
		XulStyle xulStyle = new XulStyle(name);
		xulStyle._referent = false;
		return xulStyle;
	}

	@Override
	public void setValue(Object value) {
		_parsedVal = null;
		super.setValue(value);
	}

	public XulStyle makeClone() {
		if (isBinding()) {
			return new XulStyle(this);
		}
		return this;
	}

	public <T> T getParsedValue() {
		if ( _parsedVal == null ) {
			_parsedVal = XulPropParser.parse(this);
		}
		if (_parsedVal == null) {
			return null;
		}
		return (T) _parsedVal;
	}

	public static class _Builder extends ItemBuilder {
		XulStyle _style;
		FinalCallback<XulStyle> _callback;
		String _content;

		private void init(final XulSelect select) {
			_style = new XulStyle();
			_callback = new FinalCallback<XulStyle>() {
				@Override
				public void onFinal(XulStyle prop) {
					select.addProp(prop);
				}
			};
			_content = null;
		}


		private void init(final XulView view) {
			_style = new XulStyle();
			_callback = new FinalCallback<XulStyle>() {
				@Override
				public void onFinal(XulStyle prop) {
					view.addInplaceProp(prop);
				}
			};
			_content = null;
		}


		@Override
		public boolean initialize(String name, Attributes attrs) {
			_style._nameId = XulPropNameCache.name2Id(attrs.getValue("name"));
			_style._desc = attrs.getValue("desc");
			_style._binding = attrs.getValue("binding");
			return true;
		}

		@Override
		public Object finalItem() {
			if (_content != null) {
				_style.setValue(_content);
				_content = null;
			}
			XulStyle style = _style;
			FinalCallback<XulStyle> callback = _callback;

			_Builder.recycle(this);

			callback.onFinal(style);
			return style;
		}

		static public _Builder create(XulSelect select) {
			_Builder builder = create();
			builder.init(select);
			return builder;

		}

		public static _Builder create(XulView view) {
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
			_recycled_builder._callback = null;
			_recycled_builder._style = null;
			_recycled_builder._content = null;
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

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			return XulManager.CommonDummyBuilder;
		}

	}
}
