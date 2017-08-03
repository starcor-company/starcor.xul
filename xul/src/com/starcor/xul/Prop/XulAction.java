package com.starcor.xul.Prop;

import android.text.TextUtils;
import com.starcor.xul.*;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Script.IScript;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Factory.XulFactory.Attributes;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/4.
 */
public class XulAction extends XulProp {
	private int _event;
	private String _type;
	private IScript _script;

	public XulAction() {
	}

	public XulAction(XulAction orgProp) {
		super(orgProp);
		this._event = orgProp._event;
		this._type = orgProp._type;
		this._script = orgProp._script;
	}

	@Override
	public void setValue(Object value) {
		do {
			if (!TextUtils.isEmpty(_type) && _type.startsWith("script/")) {
				String scriptType = _type.substring(7);
				if (TextUtils.isEmpty(scriptType)) {
					break;
				}
				IScriptContext scriptContext = XulManager.getScriptContext(scriptType);
				if (scriptContext == null) {
					break;
				}
				String s = String.valueOf(value).trim();
				IScript function = scriptContext.getFunction(null, s);
				if (function == null) {
					_script = scriptContext.compileFunction(s, XulUtils.STR_EMPTY, 0);
				} else {
					_script = function;
				}
				break;
			}
			super.setValue(value);
		} while (false);
	}

	public XulAction makeClone() {
		if (isBinding()) {
			return new XulAction(this);
		}
		return this;
	}

	public String getType() {
		return _type;
	}

	@Override
	public String getName() {
		if (_nameId < 0) {
			return XulPropNameCache.id2Name(_event);
		}
		return super.getName();
	}

	@Override
	public int getNameId() {
		if (_nameId < 0) {
			return _event;
		}
		return _nameId;
	}

	public IScript getScript() {
		return _script;
	}

	public static class _Builder extends ItemBuilder {
		XulAction _action;
		FinalCallback<XulAction> _callback;
		StringBuilder _content;
		ArrayList<XulDataNode> _dataContent;

		private void init(final XulView area) {
			_action = new XulAction();
			_callback = new FinalCallback<XulAction>() {
				@Override
				public void onFinal(XulAction prop) {
					area.addInplaceProp(prop);
				}
			};
			_content = null;
			_dataContent = null;
		}

		private void init(final XulSelect select) {
			_action = new XulAction();
			_callback = new FinalCallback<XulAction>() {
				@Override
				public void onFinal(XulAction prop) {
					select.addProp(prop);
				}
			};
			_content = null;
			_dataContent = null;
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_action._nameId = XulPropNameCache.name2Id(attrs.getValue("name"));
			_action._desc = attrs.getValue("desc");
			_action._event = XulPropNameCache.name2Id(attrs.getValue("event"));
			_action._binding = XulUtils.getCachedString(attrs.getValue("binding"));
			_action._type = XulUtils.getCachedString(attrs.getValue("type"));
			return true;
		}

		@Override
		public Object finalItem() {
			XulAction action = _action;
			FinalCallback<XulAction> callback = _callback;
			if (_dataContent != null) {
				action.setValue(_dataContent);
			} else if (_content != null) {
				action.setValue(_content.toString());
			}
			_Builder.recycle(this);
			callback.onFinal(action);
			return action;
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

		@Override
		public boolean pushText(String path, XulFactory.IPullParser parser) {
			if (_content == null) {
				_content = new StringBuilder();
			}
			_content.append(parser.getText());
			return true;
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
			_recycled_builder._action = null;
			_recycled_builder._content = null;
			_recycled_builder._dataContent = null;
		}

	}
}
