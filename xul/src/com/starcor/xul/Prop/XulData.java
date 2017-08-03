package com.starcor.xul.Prop;

import com.starcor.xul.*;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Factory.XulFactory.Attributes;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/4.
 */
public class XulData extends XulProp {
	public XulData() {
	}

	public XulData(XulData orgProp) {
		super(orgProp);
	}

	public XulData makeClone() {
		return new XulData(this);
	}

	public static class _Builder extends ItemBuilder {
		XulData _data;
		FinalCallback<XulData> _callback;
		ArrayList<XulDataNode> _dataContent;
		String _text;

		private void init(final XulView area) {
			_data = new XulData();
			_callback = new FinalCallback<XulData>() {
				@Override
				public void onFinal(XulData prop) {
					area.addInplaceProp(prop);
				}
			};
		}

		private void init(final XulSelect select) {
			_data = new XulData();
			_callback = new FinalCallback<XulData>() {
				@Override
				public void onFinal(XulData prop) {
					select.addProp(prop);
				}
			};
			_text = null;
			_dataContent = null;
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_data._nameId = XulPropNameCache.name2Id(attrs.getValue("name"));
			_data._desc = attrs.getValue("desc");
			_data._binding = XulUtils.getCachedString(attrs.getValue("binding"));
			return true;
		}

		@Override
		public boolean pushText(String path, XulFactory.IPullParser parser) {
			if (_text == null) {
				_text = parser.getText();
			} else {
				_text += parser.getText();
			}
			return true;
		}

		@Override
		public Object finalItem() {
			if (_dataContent != null) {
				_data.setValue(_dataContent);
			} else if (_text != null) {
				_data.setValue(XulUtils.getCachedString(_text));
			}

			XulData data = _data;
			FinalCallback<XulData> callback = _callback;

			_Builder.recycle(this);

			callback.onFinal(data);
			return data;
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
			_recycled_builder._data = null;
			_recycled_builder._text = null;
			_recycled_builder._dataContent = null;
		}
	}
}
