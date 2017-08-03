package com.starcor.xul;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Prop.XulBinding;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.Utils.XulQuery;
import com.starcor.xul.Utils.XulSimpleStack;

import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by hy on 2014/5/12.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class XulDataNode implements Serializable, Parcelable {
	XulBinding _ownerBinding;
	XulDataNode _parent;
	XulDataNode _next;
	XulDataNode _prev;
	String _name;
	String _value;
	XulDataNode _firstChild;
	XulDataNode _lastChild;
	int _childrenNum = 0;
	XulCachedHashMap<String, XulDataNode> _attr;

	public synchronized static XulDataNode obtainDataNode(String name) {
		return obtainDataNode(name, null);
	}

	public synchronized static XulDataNode obtainDataNode(String name, String value) {
		return new XulDataNode(name, value);
	}

	private synchronized static XulCachedHashMap<String, XulDataNode> obtainAttrContainer() {
		XulCachedHashMap<String, XulDataNode> attrs = new XulCachedHashMap<String, XulDataNode>();
		return attrs;
	}

	private XulDataNode(String name) {
		_name = XulUtils.getCachedString(name);
		_value = XulUtils.STR_EMPTY;
	}

	private XulDataNode(String name, String value) {
		_name = XulUtils.getCachedString(name);
		_value = XulUtils.getCachedString(value);
	}

	public XulDataNode makeClone() {
		return makeClone(null);
	}

	public XulDataNode makeClone(String newNodeName) {
		if (newNodeName == null) {
			newNodeName = _name;
		}
		final XulDataNode clonedNode = obtainDataNode(newNodeName, _value);
		XulDataNode child = getFirstChild();
		while (child != null) {
			final XulDataNode newChild = child.makeClone();
			clonedNode.internalAppendChild(newChild);
			newChild._parent = clonedNode;
			child = child.getNext();
		}
		if (_attr != null) {
			XulCachedHashMap<String, XulDataNode> attrMap = clonedNode._attr = obtainAttrContainer();
			for (Map.Entry<String, XulDataNode> entry : _attr.entrySet()) {
				attrMap.put(entry.getKey(), entry.getValue().makeClone());
			}
		}
		return clonedNode;
	}

	private XulDataNode internalAppendChild(XulDataNode newChild) {
		if (_firstChild == null) {
			_firstChild = _lastChild = newChild;
		} else {
			_lastChild._next = newChild;
			newChild._prev = _lastChild;
			_lastChild = newChild;
		}
		newChild._parent = this;
		++_childrenNum;
		return newChild;
	}

	private XulDataNode internalPrependChild(XulDataNode newChild) {
		if (_firstChild == null) {
			_firstChild = _lastChild = newChild;
		} else {
			_firstChild._prev = newChild;
			newChild._next = _firstChild;
			_firstChild = newChild;
		}
		newChild._parent = this;
		++_childrenNum;
		return newChild;
	}

	public XulDataNode removeChild(XulDataNode child) {
		if (child._parent != this) {
			return null;
		}
		XulDataNode prev = child._prev;
		XulDataNode next = child._next;

		if (prev != null) {
			prev._next = next;
		}
		if (next != null) {
			next._prev = prev;
		}

		if (child == _firstChild) {
			_firstChild = next;
		}
		if (child == _lastChild) {
			_lastChild = prev;
		}
		child._next = null;
		child._prev = null;
		child._parent = null;
		--_childrenNum;
		return child;
	}

	public XulDataNode appendChild(XulDataNode newChild) {
		internalAppendChild(newChild);
		newChild.setOwnerBinding(_ownerBinding);
		return newChild;
	}

	public XulDataNode prependChild(XulDataNode newChild) {
		internalPrependChild(newChild);
		newChild.setOwnerBinding(_ownerBinding);
		return newChild;
	}

	public XulDataNode setAttribute(String name, int value) {
		return setAttribute(name, String.valueOf(value));
	}

	public XulDataNode setAttribute(String name, String value) {
		if (_attr == null) {
			_attr = obtainAttrContainer();
		}
		XulDataNode xulDataNode = obtainDataNode(name, value);
		_attr.put(XulUtils.getCachedString(name), xulDataNode);
		xulDataNode.setOwnerBinding(_ownerBinding);
		return this;
	}

	public Map<String, XulDataNode> getAttributes() {
		return _attr;
	}

	public XulDataNode getAttribute(String name) {
		if (_attr == null) {
			return null;
		}
		return _attr.get(name);
	}

	public String getAttributeValue(String name) {
		if (_attr == null) {
			return null;
		}
		final XulDataNode xulDataNode = _attr.get(name);
		if (xulDataNode == null) {
			return null;
		}
		return xulDataNode.getValue();
	}

	public boolean hasAttribute(String name, String value) {
		if (_attr == null) {
			return false;
		}
		XulDataNode dataNode = _attr.get(name);
		if (dataNode == null) {
			return false;
		}
		if (value.equals(dataNode.getValue())) {
			return true;
		}
		return false;
	}

	public boolean hasAttribute(String name) {
		return _attr != null && _attr.containsKey(name);
	}

	public XulDataNode appendChild(String name) {
		return appendChild(name, XulUtils.STR_EMPTY);
	}

	public XulDataNode appendChild(String name, String value) {
		XulDataNode dataNode = obtainDataNode(name, value);
		if (_firstChild == null) {
			_firstChild = _lastChild = dataNode;
		} else {
			_lastChild._next = dataNode;
			dataNode._prev = _lastChild;
			_lastChild = dataNode;
		}
		dataNode._parent = this;
		dataNode.setOwnerBinding(_ownerBinding);
		++_childrenNum;
		return dataNode;
	}

	public boolean hasChild() {
		return _firstChild != null;
	}

	public int size() {
		return _childrenNum;
	}

	public XulDataNode setValue(String val) {
		_value = XulUtils.getCachedString(val);
		return this;
	}

	public String getValue() {
		return _value;
	}

	public XulDataNode getFirstChild() {
		return _firstChild;
	}

	public XulDataNode getLastChild() {
		return _lastChild;
	}

	public String getName() {
		return _name;
	}

	public void setOwnerBinding(XulBinding ownerBinding) {
		if (_ownerBinding != null) {
			return;
		}
		_ownerBinding = ownerBinding;
		if (_attr != null) {
			for (XulDataNode attr : _attr.values()) {
				attr.setOwnerBinding(ownerBinding);
			}
		}
		XulDataNode node = _firstChild;
		while (node != null) {
			node.setOwnerBinding(ownerBinding);
			node = node._next;
		}
	}

	public XulBinding getOwnerBinding() {
		return _ownerBinding;
	}

	public XulDataNode getChildNode(String... names) {
		if (names == null || names.length <= 0) {
			return _firstChild;
		}
		XulDataNode childNode = this;
		for (int i = 0, namesLength = names.length; i < namesLength && childNode != null; i++) {
			String name = names[i];
			childNode = childNode.getChildNode(name);
		}
		return childNode;
	}

	public XulDataNode getChildNode(String name) {
		XulDataNode node = _firstChild;
		if (node == null || TextUtils.isEmpty(name)) {
			return null;
		}
		while (node != null) {
			if (name.equals(node.getName())) {
				return node;
			}
			node = node._next;
		}
		return null;
	}

	public String getChildNodeValue(String name, String defValue) {
		final String childNodeValue = getChildNodeValue(name);
		return childNodeValue == null ? defValue : childNodeValue;
	}

	public String getChildNodeValue(String name) {
		final XulDataNode childNode = getChildNode(name);
		if (childNode != null) {
			return childNode.getValue();
		}
		return null;
	}

	public String getChildNodeValueEx(String... names) {
		final XulDataNode childNode = getChildNode(names);
		if (childNode != null) {
			return childNode.getValue();
		}
		return null;
	}

	public String getChildNodeValueExWithDefaultValue(String defValue, String... names) {
		final String childNodeValue = getChildNodeValueEx(names);
		return childNodeValue == null ? defValue : childNodeValue;
	}

	public XulDataNode getParent() {
		return _parent;
	}

	public XulDataNode getNext() {
		return _next;
	}

	public XulDataNode getPrev() {
		return _prev;
	}

	public XulDataNode getNext(String name) {
		XulDataNode node = _next;
		while (node != null && !name.equals(node._name)) {
			node = node._next;
		}
		return node;
	}

	public XulDataNode getPrev(String name) {
		XulDataNode node = _prev;
		while (node != null && !name.equals(node._name)) {
			node = node._prev;
		}
		return node;
	}

	@Override
	public String toString() {
		return "XulDataNode{" +
			"_name='" + _name + '\'' +
			'}';
	}

	public XulDataNode selectNode(String... selectors) {
		return XulQuery.select(this, selectors);
	}

	public static class _Builder extends XulFactory.ItemBuilder {
		XulDataNode _node;
		StringBuilder _content;
		boolean _eliminateContent = false;
		FinalCallback<XulDataNode> _callback;

		private void init(XulDataNode owner, String name) {
			_node = owner.appendChild(name);
			_content = null;
			_callback = null;
		}

		private void init(XulBinding binding, String name) {
			_node = obtainDataNode(name);
			binding.setData(_node);
			_content = null;
			_callback = null;
		}

		private void init(String name) {
			_node = obtainDataNode(name);
			_content = null;
			_callback = null;
		}

		private void init(String name, FinalCallback<XulDataNode> callback) {
			_node = obtainDataNode(name);
			_content = null;
			_callback = callback;
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			if (attrs == null) {
				return true;
			}
			for (int i = 0; i < attrs.getLength(); i++) {
				_node.setAttribute(attrs.getName(i), attrs.getValue(i));
			}
			return true;
		}

		@Override
		public boolean pushText(String path, XulFactory.IPullParser parser) {
			if (_eliminateContent) {
				_content = null;
				return true;
			}
			if (_content == null) {
				_content = new StringBuilder();
			}
			_content.append(parser.getText());
			return true;
		}

		@Override
		public XulFactory.ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			_eliminateContent = true;
			_Builder builder = _Builder.create(_node, name);
			builder.initialize(name, attrs);
			return builder;
		}

		@Override
		public Object finalItem() {
			XulDataNode node = _node;
			if (_content != null) {
				node.setValue(_content.toString());
				_content = null;
			}

			FinalCallback<XulDataNode> callback = _callback;
			_Builder.recycle(this);
			if (callback != null) {
				callback.onFinal(node);
			}
			return node;
		}

		static public _Builder create(XulDataNode owner, String name) {
			_Builder builder = create();
			builder.init(owner, name);
			return builder;

		}

		public static _Builder create(XulBinding binding, String name) {
			_Builder builder = create();
			builder.init(binding, name);
			return builder;
		}

		public static _Builder create(String name) {
			_Builder builder = create();
			builder.init(name);
			return builder;
		}

		public static _Builder create(String name, FinalCallback<XulDataNode> finalCallback) {
			_Builder builder = create();
			builder.init(name, finalCallback);
			return builder;
		}


		private static _Builder create() {
			_Builder builder = _recycled_builder.pop();
			if (builder == null) {
				builder = new _Builder();
			}
			return builder;
		}

		private static XulSimpleStack<_Builder> _recycled_builder = new XulSimpleStack<_Builder>(256);

		private static void recycle(_Builder builder) {
			_recycled_builder.push(builder);
			builder._node = null;
			builder._content = null;
			builder._eliminateContent = false;
			builder._callback = null;
		}
	}

	static class _Factory extends XulFactory.ResultBuilder {
		@Override
		public XulFactory.ItemBuilder build(XulFactory.ResultBuilderContext ctx, String name, Attributes attrs) {
			_Builder nodeBuilder = _Builder.create(name);
			nodeBuilder.initialize(name, attrs);
			return nodeBuilder;
		}
	}

	public static XulDataNode build(byte[] data) throws Exception {
		XulUtils.ticketMarker tm = null;
		if (XulManager.PERFORMANCE_BENCH) {
			tm = new XulUtils.ticketMarker("XulDataNode.build", true);
			tm.mark();
		}
		XulDataNode dataNode = XulFactory.build(XulDataNode.class, data, null);
		if (tm != null) {
			tm.mark("build");
			Log.d("BENCH!!!", tm.toString());
		}
		return dataNode;
	}

	public static XulDataNode build(InputStream data) throws Exception {
		XulUtils.ticketMarker tm = null;
		if (XulManager.PERFORMANCE_BENCH) {
			tm = new XulUtils.ticketMarker("XulDataNode.build", true);
			tm.mark();
		}
		XulDataNode dataNode = XulFactory.build(XulDataNode.class, data, null);
		if (tm != null) {
			tm.mark("build");
			Log.d("BENCH!!!", tm.toString());
		}
		return dataNode;
	}

	public static XulDataNode build(String data) {
		return obtainDataNode("", data);
	}

	public static XulDataNode buildFromJson(String str) {
		if (TextUtils.isEmpty(str)) {
			return null;
		}
		return buildFromJson(new StringReader(str));
	}

	public static XulDataNode buildFromJson(InputStream is) {
		if (is == null) {
			return null;
		}
		return buildFromJson(new InputStreamReader(is));
	}

	public static XulDataNode buildFromJson(byte[] data) {
		if (data == null) {
			return null;
		}
		return buildFromJson(new ByteArrayInputStream(data));
	}

	public static XulDataNode buildFromJson(Reader reader) {
		JsonReader jsonReader = new JsonReader(reader);
		ArrayList<XulDataNode> parseStack = new ArrayList<XulDataNode>();
		XulDataNode lastDataNode = null;
		try {
			JsonToken peek;
			while ((peek = jsonReader.peek()) != JsonToken.END_DOCUMENT) {
				switch (peek) {
				case NAME:
					String nameStr = jsonReader.nextName();
					if (!jsonReader.hasNext()) {
						continue;
					}
					switch ((peek = jsonReader.peek())) {
					case BEGIN_OBJECT:
					case BEGIN_ARRAY:
						if (peek == JsonToken.BEGIN_OBJECT) {
							jsonReader.beginObject();
						} else {
							jsonReader.beginArray();
						}
						lastDataNode = lastDataNode.appendChild(nameStr);
						parseStack.add(lastDataNode);
						break;
					case STRING:
					case NUMBER:
						lastDataNode.setAttribute(nameStr, jsonReader.nextString());
						break;
					case BOOLEAN:
						lastDataNode.setAttribute(nameStr, String.valueOf(jsonReader.nextBoolean()));
						break;
					case NULL:
						jsonReader.nextNull();
						lastDataNode.setAttribute(nameStr, null);
						break;
					}
					break;
				case BEGIN_OBJECT:
				case BEGIN_ARRAY:
					if (peek == JsonToken.BEGIN_OBJECT) {
						jsonReader.beginObject();
					} else {
						jsonReader.beginArray();
					}
					if (lastDataNode != null) {
						lastDataNode = lastDataNode.appendChild(String.valueOf(lastDataNode.size()));
					} else {
						lastDataNode = XulDataNode.obtainDataNode(null);
					}
					parseStack.add(lastDataNode);
					break;
				case END_OBJECT:
				case END_ARRAY:
					if (peek == JsonToken.END_OBJECT) {
						jsonReader.endObject();
					} else {
						jsonReader.endArray();
					}
					parseStack.remove(parseStack.size() - 1);
					if (!parseStack.isEmpty()) {
						lastDataNode = parseStack.get(parseStack.size() - 1);
					}
					break;
				default:
					// array
					switch (peek) {
					case STRING:
					case NUMBER:
						lastDataNode.appendChild(String.valueOf(lastDataNode.size()), jsonReader.nextString());
						break;
					case BOOLEAN:
						lastDataNode.appendChild(String.valueOf(lastDataNode.size()), String.valueOf(jsonReader.nextBoolean()));
						break;
					case NULL:
						jsonReader.nextNull();
						lastDataNode.appendChild(String.valueOf(lastDataNode.size()));
						break;
					default:
						jsonReader.skipValue();
						break;
					}
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lastDataNode;
	}

	static {
		XulFactory.registerBuilder(XulDataNode.class, new _Factory());
	}

	public static void dumpXulDataNode(XulDataNode data, XmlSerializer writer) throws IOException {
		String nodeName = data.getName();
		if (TextUtils.isEmpty(nodeName)) {
			nodeName = "NONAME";
		}
		writer.startTag(null, nodeName);
		final Map<String, XulDataNode> attributes = data.getAttributes();
		if (attributes != null) {
			for (Map.Entry<String, XulDataNode> entry : attributes.entrySet()) {
				String value = entry.getValue().getValue();
				if (value == null) {
					value = "";
				}
				writer.attribute(null, entry.getKey(), value);
			}
		}
		XulDataNode childNode = data.getFirstChild();
		while (childNode != null) {
			dumpXulDataNode(childNode, writer);
			childNode = childNode.getNext();
		}
		final String value = data.getValue();
		if (!TextUtils.isEmpty(value)) {
			writer.text(value);
		}
		writer.endTag(null, nodeName);
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		try {
			XmlSerializer writer = XmlPullParserFactory.newInstance().newSerializer();
			writer.setOutput(out, "utf-8");
			writer.startDocument("utf-8", true);
			dumpXulDataNode(this, writer);
			writer.endDocument();
			writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		try {
			XulDataNode build = build(in);
			this._attr = build._attr;
			this._firstChild = build._firstChild;
			this._lastChild = build._lastChild;
			this._childrenNum = build._childrenNum;
			this._name = build._name;
			this._value = build._value;
			this._prev = null;
			this._next = null;
			for (XulDataNode child = _firstChild; child != null; child = child.getNext()) {
				child._parent = this;
			}
			build._attr = null;
			build._firstChild = null;
			build._lastChild = null;
			build._childrenNum = 0;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new ClassNotFoundException("can't rebuild XulDataNode");
		}
	}

	private static final long serialVersionUID = 0x78756C444E6F6465L;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeSerializable(this);
	}

	public static final Creator<XulDataNode> CREATOR = new Creator<XulDataNode>() {
		@Override
		public XulDataNode createFromParcel(Parcel in) {
			return (XulDataNode) in.readSerializable();
		}

		@Override
		public XulDataNode[] newArray(int size) {
			return new XulDataNode[size];
		}
	};



}
