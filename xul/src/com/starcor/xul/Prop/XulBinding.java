package com.starcor.xul.Prop;

import android.text.TextUtils;
import com.starcor.xul.*;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * Created by hy on 2014/5/5.
 */
public class XulBinding extends XulProp {

	public static XulBinding createBinding(String bindingId) {
		XulBinding binding = new XulBinding();
		binding._id = bindingId;
		return binding;
	}

	enum BindingStatus {
		INITIAL,
		READY,
		REFRESHING,
		UPDATED,
	}

	String _id;
	String _dataUrl;
	BindingStatus _state = BindingStatus.INITIAL;
	XulDataNode _data;
	boolean _preload = false;

	private void _updateStateOnDataReady() {
		if (_state == BindingStatus.REFRESHING) {
			_state = BindingStatus.UPDATED;
		} else {
			_state = BindingStatus.READY;
		}
	}

	public XulBinding() {
	}

	public XulBinding(XulBinding orgBinding) {
		super(orgBinding);
		_id = orgBinding._id;
		_dataUrl = orgBinding._dataUrl;
		_state = orgBinding._state;
		_data = orgBinding._data;
		_preload = orgBinding._preload;
	}

	public XulBinding makeClone() {
		return new XulBinding(this);
	}

	public boolean isPreloadBinding() {
		return _preload;
	}

	public boolean isRemoteData() {
		return !TextUtils.isEmpty(_dataUrl);
	}

	public boolean isRefreshing() {
		return _state == BindingStatus.REFRESHING;
	}

	public void refreshBinding() {
		_state = BindingStatus.REFRESHING;
	}

	public void refreshBinding(InputStream dataStream) {
		setData(dataStream);
		_state = BindingStatus.UPDATED;
	}

	public void refreshBinding(XulDataNode data) {
		setData(data);
		_state = BindingStatus.UPDATED;
	}

	public void refreshBinding(String url) {
		_dataUrl = url;
		_data = null;
		_state = BindingStatus.REFRESHING;
	}

	// 将已经处理UPDATED状态的数据源设置为READY状态
	public boolean markReady() {
		if (_state == BindingStatus.UPDATED) {
			_state = BindingStatus.READY;
			return true;
		}
		return false;
	}

	// 数据是否已经变更
	public boolean isUpdated() {
		return _state == BindingStatus.UPDATED;
	}

	// 数据源是否已经准备完成
	public boolean isDataReady() {
		return _state == BindingStatus.READY;
	}

	public String getId() {
		return _id;
	}

	public void setDataUrl(String value) {
		_dataUrl = value;
		_data = null;
		if (_state != BindingStatus.REFRESHING) {
			_state = BindingStatus.INITIAL;
		}
	}

	public String getDataUrl() {
		return _dataUrl;
	}

	public void setEmptyData() {
		_updateStateOnDataReady();
		_data = null;
	}

	public void setData(XulDataNode value) {
		_updateStateOnDataReady();
		_data = value;
		_data.setOwnerBinding(this);
	}

	public void setData(InputStream stream) {
		_updateStateOnDataReady();

		InputStreamReader streamReader = new InputStreamReader(stream);
		StringBuilder cb = new StringBuilder();
		try {
			char[] buf = new char[1024];
			int len;
			while ((len = streamReader.read(buf)) > 0) {
				cb.append(buf, 0, len);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String content = cb.toString();

		XulDataNode dataNode = null;
		boolean isContentEmpty = TextUtils.isEmpty(content);
		try {
			char firstChar = isContentEmpty ? '\0' : content.charAt(0);
			if (!isContentEmpty && firstChar == '<') {
				dataNode = XulDataNode.build(content.getBytes());
			} else if (isContentEmpty || (firstChar != '{' && firstChar != '[')) {
				dataNode = XulDataNode.build(content);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (dataNode == null && !isContentEmpty) {
			try {
				String trimContent = content.trim();
				if (!TextUtils.isEmpty(trimContent) && (trimContent.charAt(0) == '{' || trimContent.charAt(0) == '[')) {
					Object jsonObj = new JSONTokener(trimContent).nextValue();
					_Builder builder = _Builder.create(this);
					if (jsonObj instanceof JSONObject || jsonObj instanceof JSONArray) {
						parseJsonObject(jsonObj, builder, null);
					} else {
						XulDataNode data = XulDataNode.obtainDataNode(XulUtils.STR_EMPTY);
						data.setValue(content);
						_data = data;
						_data.setOwnerBinding(this);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			_data = dataNode;
			_data.setOwnerBinding(this);
		}
	}

	static XulFactory.TextParser _textParser = new XulFactory.TextParser();
	static void parseJsonObject(Object obj, ItemBuilder ctx, String name) {
		if (obj instanceof JSONArray) {
			name = TextUtils.isEmpty(name) ? "array" : name;
			ItemBuilder builder = ctx.pushSubItem(null, null, name, null);
			builder.initialize(name, null);
			JSONArray array = (JSONArray) obj;
			for (int i = 0; i < array.length(); i++) {
				try {
					Object o = array.get(i);
					parseJsonObject(o, builder, "value");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} else if (obj instanceof JSONObject) {
			name = TextUtils.isEmpty(name) ? "object" : name;
			ItemBuilder builder = ctx.pushSubItem(null, null, name, null);
			builder.initialize(name, null);
			JSONObject object = (JSONObject) obj;
			Iterator keys = object.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				try {
					Object o = object.get(key);
					parseJsonObject(o, builder, key);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} else {
			ItemBuilder builder = ctx.pushSubItem(null, null, name, null);
			builder.initialize(name, null);
			if (obj != null) {
				_textParser.text = String.valueOf(obj);
				builder.pushText(null, _textParser);
			}
			builder.finalItem();
		}
	}

	public XulDataNode getData() {
		return _data;
	}

	public static class _Builder extends ItemBuilder {
		XulBinding _binding;
		FinalCallback<XulBinding> _callback;
		String _content;

		private void init(XulBinding binding) {
			_binding = binding;
			_callback = new FinalCallback<XulBinding>() {
				@Override
				public void onFinal(XulBinding prop) {
				}
			};
			_content = null;
		}

		private void init(final XulPage page) {
			_binding = new XulBinding();
			_callback = new FinalCallback<XulBinding>() {
				@Override
				public void onFinal(XulBinding prop) {
					page.addBinding(prop);
				}
			};
			_content = null;
		}

		private void init(final XulManager mgr) {
			_binding = new XulBinding();
			_callback = new FinalCallback<XulBinding>() {
				@Override
				public void onFinal(XulBinding prop) {
					mgr.addGlobalBinding(prop);
				}
			};
			_content = null;
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
		public boolean initialize(String name, Attributes attrs) {
			_binding._id = attrs.getValue("id");
			_binding._desc = attrs.getValue("desc");
			_binding._binding = attrs.getValue("binding");
			_binding._preload = "true".equals(attrs.getValue("preload"));
			return true;
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			XulDataNode._Builder builder = XulDataNode._Builder.create(_binding, name);
			builder.initialize(name, attrs);
			return builder;
		}

		@Override
		public Object finalItem() {
			if (_content != null) {
				String content = _content.trim();
				_content = null;
				try {
					Object jsonObj = new JSONTokener(content).nextValue();
					if (jsonObj instanceof JSONObject || jsonObj instanceof JSONArray) {
						parseJsonObject(jsonObj, this, null);
						content = null;
					}
				} catch (JSONException e) {
				}
				if (!TextUtils.isEmpty(content)) {
					_binding._dataUrl = content;
				}
			}

			XulBinding binding = _binding;
			FinalCallback<XulBinding> callback = _callback;

			_Builder.recycle(this);

			callback.onFinal(binding);
			return binding;
		}


		public static _Builder create(XulBinding select) {
			_Builder builder = create();
			builder.init(select);
			return builder;
		}

		public static _Builder create(XulManager mgr) {
			_Builder builder = create();
			builder.init(mgr);
			return builder;
		}

		public static _Builder create(XulPage view) {
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
			_recycled_builder._binding = null;
			_recycled_builder._content = null;
		}

	}
}
