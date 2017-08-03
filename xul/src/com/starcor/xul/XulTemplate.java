package com.starcor.xul;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulBinding;
import com.starcor.xul.PropMap.XulPropContainer;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.Utils.XulSimpleArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hy on 2014/5/6.
 */
public class XulTemplate extends XulElement {
	private static final String TAG = XulTemplate.class.getSimpleName();
	public static final String TEMPLATE_CONTAINER = "@@@@template-container@@@@";
	Object _parent;
	XulArrayList<Pair<Object, String>> _children = new XulArrayList<Pair<Object, String>>();
	String _id;
	String _desc;
	String _binding;
	ArrayList<XulDataNode> _bindingData;

	public void cleanBindingCtx() {
		_bindingSource = null;
	}

	public static class XulViewArrayList extends XulSimpleArray<XulView> {

		@Override
		protected XulView[] allocArrayBuf(int size) {
			return new XulView[size];
		}
	}
	XulViewArrayList _instance;
	private XulBinding _bindingSource;
	XulPropContainer<XulAttr> _attrs;
	int _incrementalDelay = 0;
	int _incrementalInitialSize = 1;

	public void addProp(XulAttr prop) {
		if (prop.getName().equals("incremental")) {
			parseIncrementalProp(prop.getStringValue());
			return;
		}
		if (_attrs == null) {
			_attrs = new XulPropContainer<XulAttr>();
		}
		_attrs.add(prop);
	}

	private void parseIncrementalProp(String val) {
		if (TextUtils.isEmpty(val)) {
			return;
		}
		if ("true".equals(val) || "enabled".equals(val)) {
			_incrementalDelay = 200;
		}
		String[] split = val.split(",");
		if (split.length == 1) {
			_incrementalInitialSize = 1;
			_incrementalDelay = XulUtils.tryParseInt(split[0], 0);
		} else if (split.length >= 2) {
			_incrementalInitialSize = XulUtils.tryParseInt(split[0], 1);
			_incrementalDelay = XulUtils.tryParseInt(split[1], 0);
		} else {
			_incrementalDelay = 0;
		}
	}

	private boolean isIncremental() {
		return _incrementalDelay > 0;
	}

	class IncrementalCreator implements Runnable {
		private XulArea parentArea;
		private int parentAreaChildrenNum;
		private int itemBasePos;
		private int itemInsertPos;
		private int dataPos;
		private ArrayList<XulDataNode> dataNodes;

		public IncrementalCreator(XulArea parentArea, int itemBasePos, int itemInsertPos, int dataPos, ArrayList<XulDataNode> dataNodes) {
			this.parentArea = parentArea;
			this.parentAreaChildrenNum = parentArea.getChildNum();
			this.itemBasePos = itemBasePos;
			this.itemInsertPos = itemInsertPos;
			this.dataPos = dataPos;
			this.dataNodes = dataNodes;
		}

		@Override
		public void run() {
			if (_inst != this) {
				return;
			}
			int bindingDataSize = dataNodes.size();
			if (dataPos >= bindingDataSize) {
				parentArea.getOwnerPage().invokeActionNoPopupWithArgs(parentArea, "incrementalBindingFinished", _id);
				return;
			}

			if (parentAreaChildrenNum != parentArea.getChildNum()) {
				// children number changed by other template
				int newItemBasePos = parentArea.findChildPos(XulTemplate.this) + 1;
				itemInsertPos += newItemBasePos - itemBasePos;
				itemBasePos = newItemBasePos;
			}

			XulDataNode dataNode = dataNodes.get(dataPos);
			XulView instance = createInstance(parentArea, itemInsertPos, dataPos, bindingDataSize, dataNode);
			if (instance != null) {
				++itemInsertPos;
				++parentAreaChildrenNum;
				XulPage ownerPage = parentArea.getOwnerPage();
				XulRenderContext renderContext = ownerPage.getRenderContext();
				ownerPage.rebindView(instance, renderContext.getDefaultActionCallback());
				parentArea.resetRender();
			}

			parentArea.getOwnerPage().invokeActionNoPopupWithArgs(parentArea, "incrementalBindingUpdate", _id, dataPos, bindingDataSize, instance);
			++dataPos;
			addIncrementalCreator(this);
		}
	}

	private void addIncrementalCreator(IncrementalCreator incrementalCreator) {
		XulRenderContext ctx = ((XulView) _parent).getOwnerPage().getRenderContext();
		ctx.uiRun(incrementalCreator, _incrementalDelay);
	}

	IncrementalCreator _inst;

	public XulTemplate(Object parent) {
		super(XulElement.TEMPLATE_TYPE);
		_parent = parent;
	}

	public void addChild(XulArea area, String filter) {
		_children.add(Pair.create((Object) area, filter));
	}

	public void addChild(XulItem item, String filter) {
		_children.add(Pair.create((Object) item, filter));
	}

	public void addChild(XulTemplate template, String filter) {
		_children.add(Pair.create((Object) template, filter));
	}

	public void bindingData(ArrayList<XulDataNode> dataNodes) {
		_bindingData = dataNodes;
		_inst = null;
		XulArea parentArea = (XulArea) _parent;
		if (_instance != null) {
			for (int i = 0; i < _instance.size(); i++) {
				XulView xulItem = _instance.get(i);
				xulItem.removeSelf();
			}

			XulLayout rootLayout = parentArea.getRootLayout();
			if (rootLayout != null) {
				XulView focus = rootLayout.getFocus();
				if (focus != null && focus.isDiscarded()) {
					rootLayout.killFocus();
				}
			}
			XulArea parent = parentArea;
			while (parent != null) {
				XulView lastFocusChild = parent.getDynamicFocus();
				if (lastFocusChild != null && lastFocusChild.isDiscarded()) {
					parent.setDynamicFocus(null);
				}
				parent = parent.getParent();
			}

			_instance.clear();
		}
		if (_bindingData == null || _bindingData.isEmpty()) {
			return;
		}
		_instance = new XulViewArrayList();
		int itemBasePos = parentArea.findChildPos(this) + 1;
		int itemInsertPos = itemBasePos;

		if (isIncremental()) {
			int bindingDataSize = _bindingData.size();
			int initNum = Math.min(_incrementalInitialSize, bindingDataSize);
			for (int i = 0; i < initNum; ++i) {
				XulDataNode node = _bindingData.get(i);
				if (createInstance(parentArea, itemInsertPos, i, bindingDataSize, node) != null) {
					++itemInsertPos;
				}
			}
			_inst = new IncrementalCreator(parentArea, itemBasePos, itemInsertPos, initNum, _bindingData);
			addIncrementalCreator(_inst);
		} else {
			int bindingDataSize = _bindingData.size();
			for (int i = 0; i < bindingDataSize; ++i) {
				XulDataNode node = _bindingData.get(i);
				if (createInstance(parentArea, itemInsertPos, i, bindingDataSize, node) != null) {
					++itemInsertPos;
				}
			}
		}
		parentArea.resetRender();
	}

	private XulView createInstance(XulArea parentArea, int basePos, int dataPos, int dataCount, XulDataNode dataNode) {
		XulView newInstance = null;
		int childNum = _children.size();
		Object[] childrenArray = _children.getArray();
		for (int childIdx = 0; childIdx < childNum; childIdx++) {
			Pair<Object, String> childInfo = (Pair<Object, String>) childrenArray[childIdx];
			String filter = childInfo.second;
			if (!testFilter(filter, dataNode, dataPos, dataCount)) {
				continue;
			}

			Object obj = childInfo.first;
			if (obj instanceof XulArea) {
				XulArea area = (XulArea) obj;
				String binding = area._binding;
				if (TextUtils.isEmpty(binding)) {
					newInstance = area.makeClone(parentArea, basePos);
				} else {
					XulArea container = new XulArea(parentArea, basePos, TEMPLATE_CONTAINER);
					newInstance = container;
					area.makeClone(container);
				}
			} else if (obj instanceof XulTemplate) {
				XulArea container = new XulArea(parentArea, basePos, TEMPLATE_CONTAINER);
				newInstance = container;
				XulTemplate template = (XulTemplate) obj;
				template.makeClone(container, basePos);
			} else if (obj instanceof XulItem) {
				XulItem item = (XulItem) obj;
				String binding = item._binding;
				if (TextUtils.isEmpty(binding)) {
					newInstance = item.makeClone(parentArea, basePos);
				} else {
					XulArea container = new XulArea(parentArea, basePos, TEMPLATE_CONTAINER);
					newInstance = container;
					item.makeClone(container);
				}
			} else {
				Log.d(TAG, "unsupported children type!!! - " + obj.getClass().getSimpleName());
			}
			break;
		}
		if (newInstance == null) {
			return newInstance;
		}

		newInstance._binding = "(" + _binding + ")[" + dataPos + "]";
		newInstance.setBindingCtx(dataNode);
		newInstance.setBindingSource(dataNode.getOwnerBinding());
		_instance.add(newInstance);

		XulRenderContext renderContext = parentArea.getOwnerPage().getRenderContext();
		if (renderContext != null && parentArea.getRender() != null) {
			newInstance.prepareRender(renderContext);
		}
		XulLayout xulLayout = parentArea._root;
		if (xulLayout != null) {
			xulLayout.addSelectorTarget(newInstance);
			xulLayout.applySelectors(newInstance);
		}
		return newInstance;
	}

	public Object getItemTemplate(XulDataNode xulDataNode, int idx, int size) {
		int childNum = _children.size();
		Object[] childrenArray = _children.getArray();
		for (int childIdx = 0; childIdx < childNum; childIdx++) {
			Pair<Object, String> childInfo = (Pair<Object, String>) childrenArray[childIdx];
			String filter = childInfo.second;
			if (!testFilter(filter, xulDataNode, idx, size)) {
				continue;
			}
			return childInfo.first;
		}
		if (XulManager.DEBUG) {
			Log.e(TAG, "getItemTemplate Failed " + idx + "/" + size);
		}
		return null;
	}

	private static Pattern _POS_FILTER_n = Pattern.compile("^\\[-?\\d+(,-?\\d+)*\\]$");
	private static Pattern _POS_FILTER_n_range = Pattern.compile("^\\[-?\\d+~(-?\\d+)?\\]$");
	private static Pattern _POS_FILTER_n_mod = Pattern.compile("^\\[\\d+N\\+\\d+\\]$");
	private static Pattern _POS_FILTER_prop = Pattern.compile("^\\[[\\w\\._:\\d]+(=[^\\n\\r\\[\\]]+)?\\]$");

	public String getId() {
		return _id;
	}

	private static abstract class _Filter {
		abstract boolean test(XulDataNode dataNode, int i, int size);
	}

	private static class _N_Filter extends _Filter {
		HashSet<Integer> _set = new HashSet<Integer>();

		public _N_Filter(String filter) {
			String[] s = filter.split(",");
			for (int i = 0, sLength = s.length; i < sLength; i++) {
				String v = s[i];
				_set.add(XulUtils.tryParseInt(v, Integer.MAX_VALUE));
			}
		}

		@Override
		boolean test(XulDataNode dataNode, int i, int size) {
			return _set.contains(i) || _set.contains(i - size);
		}
	}

	private static class _N_Range_Filter extends _Filter {
		int _begin;
		int _end;

		public _N_Range_Filter(String filter) {
			String[] s = filter.split("~");
			if (s.length >= 1) {
				_begin = XulUtils.tryParseInt(s[0], 0);
				if (filter.endsWith("~")) {
					_end = Integer.MAX_VALUE;
				} else if (s.length >= 2) {
					_end = XulUtils.tryParseInt(s[1], Integer.MAX_VALUE);
				}
			}
		}

		@Override
		boolean test(XulDataNode dataNode, int i, int size) {
			return (_begin >= 0 ? _begin <= i : (size + _begin > 0 && size + _begin <= i))
				&& (_end >= 0 ? i <= _end : (size + _end > 0 && i <= size + _end));
		}
	}

	private static class _N_Mod_Filter extends _Filter {
		int _mod;
		int _delta;

		public _N_Mod_Filter(String filter) {
			String[] s = filter.split("N\\+");
			if (s.length >= 2) {
				_mod = XulUtils.tryParseInt(s[0], 1);
				_delta = XulUtils.tryParseInt(s[1], 1);
				if (_mod == 0) {
					_mod = 1;
				}
				if (_delta >= _mod || _delta < 0) {
					_delta = -1;
				}
			}
		}

		@Override
		boolean test(XulDataNode dataNode, int i, int size) {
			return i % _mod == _delta;
		}
	}

	private static class _Prop_Filter extends _Filter {
		String _name;
		String _val;

		public _Prop_Filter(String filter) {
			String[] s = filter.split("=");
			if (s.length >= 1) {
				_name = s[0];
				if (s.length >= 2) {
					_val = s[1];
				}
			}
		}

		@Override
		boolean test(XulDataNode xulDataNode, int i, int size) {
			XulDataNode attribute = xulDataNode.getAttribute(_name);
			if (_val == null) {
				return attribute != null;
			}
			return attribute != null && _val.equals(attribute.getValue());
		}
	}

	private static class _False_Filter extends _Filter {

		@Override
		boolean test(XulDataNode dataNode, int i, int size) {
			return false;
		}
	}

	private static class _True_Filter extends _Filter {

		@Override
		boolean test(XulDataNode dataNode, int i, int size) {
			return true;
		}
	}

	private static XulCachedHashMap<String, _Filter> _filterCache = new XulCachedHashMap<String, _Filter>();

	private static _True_Filter _TRUE_FILTER = new _True_Filter();
	private static _False_Filter _FALSE_FILTER = new _False_Filter();

	private static _Filter getFilter(String filter) {
		if (TextUtils.isEmpty(filter)) {
			return _TRUE_FILTER;
		}
		{
			_Filter f = _filterCache.get(filter);
			if (f != null) {
				return f;
			}
		}

		{
			Matcher m = _POS_FILTER_n.matcher(filter);
			if (m != null && m.matches()) {
				_N_Filter f = new _N_Filter(filter.substring(1, filter.length() - 1));
				_filterCache.put(filter, f);
				return f;
			}
		}
		{
			Matcher m = _POS_FILTER_n_range.matcher(filter);
			if (m != null && m.matches()) {
				_N_Range_Filter f = new _N_Range_Filter(filter.substring(1, filter.length() - 1));
				_filterCache.put(filter, f);
				return f;
			}
		}
		{
			Matcher m = _POS_FILTER_n_mod.matcher(filter);
			if (m != null && m.matches()) {
				_N_Mod_Filter f = new _N_Mod_Filter(filter.substring(1, filter.length() - 1));
				_filterCache.put(filter, f);
				return f;
			}
		}
		{
			Matcher m = _POS_FILTER_prop.matcher(filter);
			if (m != null && m.matches()) {
				_Prop_Filter f = new _Prop_Filter(filter.substring(1, filter.length() - 1));
				_filterCache.put(filter, f);
				return f;
			}
		}
		_filterCache.put(filter, _FALSE_FILTER);
		return _FALSE_FILTER;
	}

	private boolean testFilter(String filter, XulDataNode node, int i, int size) {
		_Filter f = getFilter(filter);
		if (f == null) {
			return true;
		}
		return f.test(node, i, size);
	}

	void makeClone(XulArea parent, int pos) {
		XulTemplate xulTemplate = new XulTemplate(parent);
		xulTemplate._children = this._children;
		xulTemplate._id = this._id;
		xulTemplate._binding = this._binding;
		xulTemplate._desc = this._desc;
		xulTemplate._bindingSource = this._bindingSource;
		xulTemplate._attrs = this._attrs;
		xulTemplate._incrementalDelay = this._incrementalDelay;
		xulTemplate._incrementalInitialSize = this._incrementalInitialSize;
		parent.addChild(pos, xulTemplate);
	}

	void makeClone(XulArea parent) {
		makeClone(parent, -1);
	}

	public String getBinding() {
		return _binding;
	}

	public void setBindingSource(XulBinding bindingSource) {
		this._bindingSource = bindingSource;
	}

	public XulBinding getBindingSource() {
		return _bindingSource;
	}

	public static class _Builder extends ItemBuilder {
		XulTemplate _template;
		XulTemplate _ownerTemplate;

		private void init(XulLayout layout) {
			_template = new XulTemplate(layout);
			layout.addChild(_template);
		}

		private void init(XulArea area) {
			_template = new XulTemplate(area);
			area.addChild(_template);
		}

		private void init(XulTemplate template) {
			_template = new XulTemplate(template);
			_ownerTemplate = template;
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_template._id = attrs.getValue("id");
			_template._desc = attrs.getValue("desc");
			_template._binding = attrs.getValue("binding");
			_template.parseIncrementalProp(attrs.getValue("incremental"));
			if (_ownerTemplate != null) {
				_ownerTemplate.addChild(_template, attrs.getValue("filter"));
			}
			return true;
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			if ("area".equals(name)) {
				XulArea._Builder builder = XulArea._Builder.create(_template);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("item".equals(name)) {
				XulItem._Builder builder = XulItem._Builder.create(_template);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("template".equals(name)) {
				XulTemplate._Builder builder = XulTemplate._Builder.create(_template);
				builder.initialize(name, attrs);
				return builder;
			}
			if ("attr".equals(name)) {
				XulAttr._Builder builder = XulAttr._Builder.create(_template);
				builder.initialize(name, attrs);
				return builder;
			}
			return XulManager.CommonDummyBuilder;
		}

		@Override
		public Object finalItem() {
			XulTemplate template = _template;
			_Builder.recycle(this);
			return template;
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
			_Builder builder = _recycled_builder.isEmpty() ? null : _recycled_builder.remove(_recycled_builder.size() - 1);
			if (builder == null) {
				builder = new _Builder();
			}
			return builder;
		}

		private static ArrayList<_Builder> _recycled_builder = new ArrayList<_Builder>();

		private static void recycle(_Builder builder) {
			_recycled_builder.add(builder);
			builder._template = null;
			builder._ownerTemplate = null;
		}
	}
}
