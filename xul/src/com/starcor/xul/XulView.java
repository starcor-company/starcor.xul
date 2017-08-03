package com.starcor.xul;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.starcor.xul.Events.XulStateChangeEvent;
import com.starcor.xul.Graphics.IXulDrawable;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.*;
import com.starcor.xul.PropMap.IXulPropIterator;
import com.starcor.xul.PropMap.XulPropContainer;
import com.starcor.xul.Render.XulCustomViewRender;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.Script.XulScriptableObject;
import com.starcor.xul.ScriptWrappr.XulViewScriptableObject;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.Utils.XulPropParser;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by hy on 2014/5/4.
 */
public abstract class XulView extends XulElement implements IXulDrawable {
	private static final String TAG = XulView.class.getSimpleName();
	public static final ArrayList<String> EMPTY_CLASS = new ArrayList<String>();
	public static final IXulPropIterator<XulProp> CLEAN_PROP_BINDING_ITERATOR = new IXulPropIterator<XulProp>() {
		@Override
		public void onProp(XulProp prop, int state) {
			prop.setBindingSource(null);
		}
	};

	public static final int STATE_NORMAL = 0;
	public static final int STATE_FOCUSED = 1;
	public static final int STATE_DISABLED = 2;
	public static final int STATE_VISIBLE = 4;
	public static final int STATE_INVISIBLE = 8;

	@Override
	public String toString() {
		String name = this.getClass().getSimpleName();
		return name + "{" +
			"_id='" + _id + '\'' +
			", _type='" + _type + '\'' +
			", _desc='" + _desc + '\'' +
			", _binding='" + _binding + '\'' +
			", _class=" + _class +
			'}';
	}

	XulLayout _root;
	XulArea _parent;
	XulBinding _bindingSource;

	WeakReference<XulView> _refView = null;

	int _focusPriority = -2;
	String _id;
	ArrayList<String> _class;
	String _type;
	String _desc;
	String _binding;
	boolean _bindingDataReady = false;
	ArrayList<XulDataNode> _bindingData;

	XulPropContainer<XulStyle> _styles;
	XulPropContainer<XulAttr> _attrs;
	XulPropContainer<XulData> _data;
	XulPropContainer<XulFocus> _focus;
	XulPropContainer<XulAction> _action;

	XulViewRender _render;

	WeakReference<XulView> _weakRef;

	public XulView() {
		super(XulElement.VIEW_TYPE);
		_root = null;
		_parent = null;
	}

	public XulView(XulArea parent) {
		super(XulElement.VIEW_TYPE);
		_parent = parent;
		_root = parent._root;
	}

	public XulView(XulLayout root) {
		super(XulElement.VIEW_TYPE);
		_root = root;
		_parent = null;
	}

	public XulView(XulLayout root, XulArea parent) {
		super(XulElement.VIEW_TYPE);
		_root = root;
		_parent = parent;
	}

	public WeakReference<XulView> getWeakReference() {
		if (_weakRef == null) {
			_weakRef = new WeakReference<XulView>(this);
		}
		return _weakRef;
	}

	public String getType() {
		return _type;
	}

	public boolean hasBindingCtx() {
		return !TextUtils.isEmpty(_binding);
	}

	public boolean isBindingCtxReady() {
		if (_bindingSource != null) {
			return _bindingSource.isDataReady();
		}
		return _bindingDataReady;
	}

	public boolean isBindingSuccess() {
		return _bindingDataReady && _bindingSource != null && _bindingData != null && _bindingSource.isDataReady();
	}

	public void setBindingCtx(String bindingSelector, ArrayList<XulDataNode> bindingData) {
		_binding = bindingSelector;
		_bindingData = bindingData;
		_bindingDataReady = true;
		if (_render != null) {
			_render.resetBinding();
		}
	}

	public void setBindingCtx(ArrayList<XulDataNode> bindingData) {
		setBindingCtx(_binding, bindingData);
	}

	public void setBindingCtx(XulDataNode node) {
		ArrayList<XulDataNode> dataNodes = new ArrayList<XulDataNode>();
		dataNodes.add(node);
		setBindingCtx(dataNodes);
	}

	public ArrayList<XulDataNode> getBindingData() {
		if (!_bindingDataReady) {
			return null;
		}
		return _bindingData;
	}

	public XulDataNode getBindingData(int idx) {
		if (idx < 0) {
			return null;
		}
		if (!_bindingDataReady) {
			return null;
		}
		ArrayList<XulDataNode> bindingData = _bindingData;
		if (bindingData == null) {
			return null;
		}
		if (idx >= bindingData.size()) {
			return null;
		}
		return bindingData.get(idx);
	}

	public void eachProp(final IXulPropIterator<XulProp> iterator) {
		_commonPropIterator.iterator = iterator;

		if (_focus != null) {
			_focus.each(_commonPropIterator);
		}
		if (_action != null) {
			_action.each(_commonPropIterator);
		}
		if (_attrs != null) {
			_attrs.each(_commonPropIterator);
		}
		if (_styles != null) {
			_styles.each(_commonPropIterator);
		}
		if (_data != null) {
			_data.each(_commonPropIterator);
		}
		_commonPropIterator.iterator = null;
	}

	public static int stateFromString(String state) {
		if ("focused".equals(state)) {
			return STATE_FOCUSED;
		}
		if ("disabled".equals(state)) {
			return STATE_DISABLED;
		}
		return STATE_NORMAL;
	}

	public XulArea findParentById(String id) {
		XulArea parent = _parent;
		while (parent != null && !id.equals(parent._id)) {
			parent = parent._parent;
		}
		return parent;
	}

	public XulArea findParentByType(String type) {
		XulArea parent = _parent;
		while (parent != null && !type.equals(parent._type)) {
			parent = parent._parent;
		}
		return parent;
	}

	public XulArea findParentByClass(String clsName) {
		XulArea parent = _parent;
		while (parent != null && !parent.hasClass(clsName)) {
			parent = parent._parent;
		}
		return parent;
	}

	public XulView findItemById(String id) {
		return null;
	}

	public XulArrayList<XulView> findItemsByClass(String clzz) {
		return null;
	}

	public XulView findCustomItemByExtView(IXulExternalView extView) {
		return null;
	}

	private static class _PropIterator implements IXulPropIterator<XulProp> {
		public IXulPropIterator<XulProp> iterator;

		@Override
		public void onProp(XulProp prop, int state) {
			iterator.onProp(prop, state);
		}
	}

	private static _PropIterator _commonPropIterator = new _PropIterator();

	public void eachInlineProp(final IXulPropIterator<XulProp> iterator) {
		_commonPropIterator.iterator = iterator;
		// if (_focus!=null) {
		// 	_focus.each(_commonPropIterator);
		// }
		if (_action != null) {
			_action.eachInlineProp(_commonPropIterator);
		}
		if (_attrs != null) {
			_attrs.eachInlineProp(_commonPropIterator);
		}
		if (_styles != null) {
			_styles.eachInlineProp(_commonPropIterator);
		}
		if (_data != null) {
			_data.eachInlineProp(_commonPropIterator);
		}
		_commonPropIterator.iterator = null;
	}

	public void eachStyle(IXulPropIterator<XulStyle> iterator) {
		if (_styles != null) {
			_styles.each(iterator);
		}
	}

	public void eachData(IXulPropIterator<XulData> iterator) {
		if (_data != null) {
			_data.each(iterator);
		}
	}

	public void eachAttr(IXulPropIterator<XulAttr> iterator) {
		if (_attrs != null) {
			_attrs.each(iterator);
		}
	}

	public void eachAction(IXulPropIterator<XulAction> iterator) {
		if (_action != null) {
			_action.each(iterator);
		}
	}

	public int getHeight() {
		XulAttr val = getAttr(XulPropNameCache.TagId.HEIGHT);
		if (val == null) {
			return XulManager.SIZE_AUTO;
		}
		XulPropParser.xulParsedAttr_WidthHeight height = val.getParsedValue();
		return height.val;
	}

	public int getWidth() {
		XulAttr val = getAttr(XulPropNameCache.TagId.WIDTH);
		if (val == null) {
			return XulManager.SIZE_AUTO;
		}
		XulPropParser.xulParsedAttr_WidthHeight width = val.getParsedValue();
		return width.val;
	}

	public int getX() {
		XulAttr val = getAttr(XulPropNameCache.TagId.X);
		if (val == null) {
			return XulManager.SIZE_AUTO;
		}
		XulPropParser.xulParsedAttr_XY x = val.getParsedValue();
		return x.val;
	}

	public int getY() {
		XulAttr val = getAttr(XulPropNameCache.TagId.Y);
		if (val == null) {
			return XulManager.SIZE_AUTO;
		}
		XulPropParser.xulParsedAttr_XY y = val.getParsedValue();
		return y.val;
	}

	public boolean focusable() {
		if (!isEnabled()) {
			return false;
		}
		if (!isVisible()) {
			return false;
		}
		if (_focus == null) {
			return XulFocus.MODE_FOCUSABLE == (getDefaultFocusMode() & XulFocus.MODE_FOCUSABLE);
		}
		XulFocus focus = _focus.get(XulPropNameCache.TagId.FOCUS);
		if (focus == null || !focus.hasModeBits(XulFocus.MODE_FOCUSABLE | XulFocus.MODE_NOFOCUS)) {
			return XulFocus.MODE_FOCUSABLE == (getDefaultFocusMode() & XulFocus.MODE_FOCUSABLE);
		}

		return focus.focusable();
	}

	public int getDefaultFocusMode() {
		if (_render == null) {
			return XulFocus.MODE_NEARBY | XulFocus.MODE_PRIORITY;
		}
		return _render.getDefaultFocusMode();
	}

	public int getFocusPriority() {
		if (_focusPriority == -2) {
			if (_focus == null) {
				_focusPriority = -1;
			} else {
				XulFocus focus = _focus.get(XulPropNameCache.TagId.FOCUS);
				if (focus == null) {
					_focusPriority = -1;
				} else {
					_focusPriority = focus.getFocusPriority();
				}
			}
		}
		return _focusPriority;
	}

	public boolean isFocused() {
		return _root != null && _root.isFocused(this);
	}

	public boolean hasFocus() {
		return isFocused();
	}

	public int getFocusMode() {
		if (_focus == null) {
			return getDefaultFocusMode();
		}
		XulFocus focus = _focus.get(XulPropNameCache.TagId.FOCUS);
		if (focus == null) {
			return getDefaultFocusMode();
		}
		int focusMode = focus.getFocusMode();
		if ((focusMode & (XulFocus.MODE_FOCUSABLE | XulFocus.MODE_NOFOCUS)) == 0) {
			focusMode |= (getDefaultFocusMode() & ~XulFocus.MODE_SEARCH_ORDER_MASK);
		}
		return focusMode;
	}

	public boolean testFocusModeBits(int bits) {
		return (getFocusMode() & bits) == bits;
	}

	public boolean isDefaultFocus() {
		if (_focus == null) {
			return false;
		}
		XulFocus focus = _focus.get(XulPropNameCache.TagId.FOCUS);
		if (focus == null) {
			return false;
		}
		return focus.isDefaultFocused();
	}

	public RectF getFocusRc() {
		return _render.getFocusRect();
	}

	public RectF getUpdateRc() {
		return _render.getUpdateRect();
	}

	public XulView findNextFocus(XulLayout.FocusDirection direction, RectF src) {
		return this.getParent().findSubFocus(direction, src, this);
	}

	public void setClass(String cls) {
		if (TextUtils.isEmpty(cls)) {
			_class = EMPTY_CLASS;
			return;
		}
		String[] clsList = cls.split(",");
		_initClass();
		for (int i = 0; i < clsList.length; i++) {
			String cachedString = XulUtils.getCachedString(clsList[i].trim());
			if (_class.contains(cachedString)) {
				continue;
			}
			_class.add(cachedString);
		}
	}

	private void _initClass() {
		if (_class == null || _class == EMPTY_CLASS) {
			_class = new ArrayList<String>();
		}
	}

	public String[] getAllClass() {
		if (_class == null) {
			if (_refView == null) {
				return null;
			}
			XulView xulView = _refView.get();
			if (xulView == null) {
				return null;
			}
			return xulView.getAllClass();
		}
		if (_class == EMPTY_CLASS || _class.isEmpty()) {
			return null;
		}
		String[] clsNames = new String[_class.size()];
		_class.toArray(clsNames);
		return clsNames;
	}

	public boolean hasClass(String cls) {
		if (_class == null) {
			if (_refView == null) {
				return false;
			}
			XulView xulView = _refView.get();
			if (xulView == null) {
				return false;
			}
			return xulView.hasClass(cls);
		}
		return _class.contains(cls);
	}

	public boolean removeClass(String clsName) {
		if (_class == null) {
			if (_refView == null) {
				return false;
			}
			XulView xulView = _refView.get();
			if (xulView == null) {
				return false;
			}
			if (xulView._class == null || xulView._class == EMPTY_CLASS) {
				return false;
			}
			_class = (ArrayList<String>) xulView._class.clone();
		}
		if (_class == EMPTY_CLASS) {
			return false;
		}
		int clsIdx = _class.indexOf(clsName);
		if (clsIdx < 0) {
			return false;
		}
		ArrayList<String> clsKeys = getSelectKeysByClass(clsName);
		_class.remove(clsIdx);
		_root.removeSelectorTarget(this, clsKeys);
		return _root.unApplySelectors(this, clsKeys);
	}

	public boolean addClass(String clsName) {
		if (_class == null) {
			while (true) {
				if (_refView == null) {
					_initClass();
					break;
				}
				XulView xulView = _refView.get();
				if (xulView == null) {
					_initClass();
					break;
				}
				if (xulView._class == null || xulView._class == EMPTY_CLASS) {
					_initClass();
					break;
				}
				if (xulView._class.contains(clsName)) {
					return false;
				}
				_class = (ArrayList<String>) xulView._class.clone();
				break;
			}
		} else {
			_initClass();
			if (_class.contains(clsName)) {
				return false;
			}
		}

		_class.add(XulUtils.getCachedString(clsName));

		ArrayList<String> clsKeys = getSelectKeysByClass(clsName);
		_root.addSelectorTarget(this, clsKeys);
		return _root.applySelectors(this, clsKeys);
	}

	public XulStyle getStyle(String name) {
		if (_styles == null) {
			return null;
		}
		return _styles.get(name);
	}

	public XulStyle setStyle(String name, String value) {
		if (value == null) {
			if (_styles != null) {
				_styles.removeOwnProp(name);
			}
			return null;
		}

		_initStyleProp();
		XulStyle style = _styles.getOwnProp(name);
		if (style == null || style.isReferent()) {
			style = XulStyle.obtain(name);
			_styles.put(style);
		}
		style.setValue(value);
		return style;
	}

	public String getStyleString(String name) {
		XulStyle style = getStyle(name);
		if (style != null) {
			return style.getStringValue();
		}
		return XulUtils.STR_EMPTY;
	}

	public XulStyle getStyle(int key) {
		if (_styles == null) {
			return null;
		}
		return _styles.get(key);
	}

	public XulStyle setStyle(int key, String value) {
		if (value == null) {
			if (_styles != null) {
				_styles.removeOwnProp(key);
			}
			return null;
		}
		_initStyleProp();
		XulStyle style = _styles.getOwnProp(key);
		if (style == null || style.isReferent()) {
			style = XulStyle.obtain(key);
			_styles.put(style);
		}
		style.setValue(value);
		return style;
	}

	public String getStyleString(int key) {
		XulStyle style = getStyle(key);
		if (style != null) {
			return style.getStringValue();
		}
		return XulUtils.STR_EMPTY;
	}

	// 重置当前元素的render状态，强制render重新加载数据
	public void resetRender() {
		if (_render != null) {
			_render.reset();
		}
	}

	public XulAttr getAttr(String name) {
		if (_attrs == null) {
			return null;
		}
		return _attrs.get(name);
	}

	public XulAttr getAttr(int key) {
		if (_attrs == null) {
			return null;
		}
		return _attrs.get(key);
	}

	public XulAction getAction(String name) {
		if (_action == null) {
			return null;
		}
		return _action.get(name);
	}

	public XulAction getAction(int nameId) {
		if (_action == null) {
			return null;
		}
		return _action.get(nameId);
	}

	public XulData getData(String name) {
		if (_data == null) {
			return null;
		}
		return _data.get(name);
	}

	public XulData getData(int nameId) {
		if (_data == null) {
			return null;
		}
		return _data.get(nameId);
	}

	public final void prepareRender(XulRenderContext ctx) {
		prepareRender(ctx, false);
	}

	public abstract void prepareRender(XulRenderContext ctx, boolean preload);

	public boolean draw(XulDC dc, Rect updateRc, int xBase, int yBase) {
		_render.preDraw(dc, updateRc, xBase, yBase);
		_render.draw(dc, updateRc, xBase, yBase);
		_render.postDraw(dc, updateRc, xBase, yBase);
		return true;
	}

	public boolean draw(XulDC dc, RectF updateRc, float xBase, float yBase) {
		Rect rect = new Rect();
		XulUtils.copyRect(updateRc, rect);
		int xB = XulUtils.roundToInt(xBase);
		int yB = XulUtils.roundToInt(yBase);
		_render.preDraw(dc, rect, xB, yB);
		_render.draw(dc, rect, xB, yB);
		_render.postDraw(dc, rect, xB, yB);
		return true;
	}

	private void _initStyleProp() {
		if (_styles == null) {
			_styles = new XulPropContainer<XulStyle>();
		}
	}

	private void _initAttrProp() {
		if (_attrs == null) {
			_attrs = new XulPropContainer<XulAttr>();
		}
	}

	private void _initDataProp() {
		if (_data == null) {
			_data = new XulPropContainer<XulData>();
		}
	}

	private void _initFocusProp() {
		if (_focus == null) {
			_focus = new XulPropContainer<XulFocus>();
		}
	}

	private void _initActionProp() {
		if (_action == null) {
			_action = new XulPropContainer<XulAction>();
		}
	}

	public void addInplaceProp(XulStyle prop) {
		_initStyleProp();
		_styles.put(prop);
	}

	public void addInplaceProp(XulAttr prop) {
		_initAttrProp();
		_attrs.put(prop);
	}

	public void addInplaceProp(XulData prop) {
		_initDataProp();
		_data.put(prop);
	}

	public void addInplaceProp(XulFocus prop) {
		_initFocusProp();
		_focus.put(prop);
	}

	public void addInplaceProp(XulAction prop) {
		_initActionProp();
		_action.put(prop);
	}

	public String getBinding() {
		return _binding;
	}

	void copyContent(XulView src) {
		if (src._refView == null) {
			this._refView = src.getWeakReference();
		} else {
			this._refView = src._refView;
		}
		this._id = src._id;
		if (src._class == null) {
			this._class = null;
		} else if (src._class == EMPTY_CLASS) {
			this._class = EMPTY_CLASS;
		} else {
			this._class = null; // copy on write with _refView._class
		}
		this._type = src._type;
		this._desc = src._desc;
		this._binding = src._binding;
		this._bindingDataReady = src._bindingDataReady;
		this._bindingData = src._bindingData;
		this._bindingSource = src._bindingSource;

		copyContainers(src);
	}

	public void copyContainers(XulView src) {
		this._styles = XulPropContainer.makeClone(src._styles);
		this._attrs = XulPropContainer.makeClone(src._attrs);
		this._data = XulPropContainer.makeClone(src._data);
		this._focus = XulPropContainer.makeClone(src._focus);
		this._action = XulPropContainer.makeClone(src._action);
	}

	public void cleanContainers() {
		this._styles = null;
		this._attrs = null;
		this._data = null;
		this._focus = null;
		this._action = null;
	}


	private static XulCachedHashMap<String, XulCachedHashMap<String, XulCachedHashMap<String, ArrayList<String>>>> _keyCache_By_ID_Type_ClsName = new XulCachedHashMap<String, XulCachedHashMap<String, XulCachedHashMap<String, ArrayList<String>>>>();

	private static ArrayList<String> getSelectKeyByClassFromCache(String id, String type, String clsName) {
		XulCachedHashMap<String, XulCachedHashMap<String, ArrayList<String>>> idMap = _keyCache_By_ID_Type_ClsName.get(id);
		if (idMap == null) {
			return null;
		}
		XulCachedHashMap<String, ArrayList<String>> idTypeMap = idMap.get(type);
		if (idTypeMap == null) {
			return null;
		}
		return idTypeMap.get(clsName);
	}

	private static void putSelectKeyByClassFromCache(String id, String type, String clsName, ArrayList<String> result) {
		XulCachedHashMap<String, XulCachedHashMap<String, ArrayList<String>>> idMap = _keyCache_By_ID_Type_ClsName.get(id);
		if (idMap == null) {
			idMap = new XulCachedHashMap<String, XulCachedHashMap<String, ArrayList<String>>>();
			_keyCache_By_ID_Type_ClsName.put(id, idMap);
		}
		XulCachedHashMap<String, ArrayList<String>> idTypeMap = idMap.get(type);
		if (idTypeMap == null) {
			idTypeMap = new XulCachedHashMap<String, ArrayList<String>>();
			idMap.put(type, idTypeMap);
		}
		idTypeMap.put(clsName, result);
	}

	public ArrayList<String> getSelectKeysByClass(String clsName) {
		if (_class == null) {
			if (_refView != null) {
				XulView xulView = _refView.get();
				if (xulView != null) {
					return xulView.getSelectKeysByClass(clsName);
				}
			}
		}

		if (TextUtils.isEmpty(_id) && TextUtils.isEmpty(_type) && TextUtils.isEmpty(clsName)) {
			return null;
		}

		if (_class == null || _class == EMPTY_CLASS || _class.isEmpty()) {
			return null;
		}

		ArrayList<String> keys = getSelectKeyByClassFromCache(_id, _type, clsName);
		if (keys != null) {
			return keys;
		}
		keys = new ArrayList<String>();
		String idKey = XulUtils.STR_EMPTY;
		String typeKey = XulUtils.STR_EMPTY;
		if (!TextUtils.isEmpty(_id)) {
			idKey = "#" + _id;
		}

		if (!TextUtils.isEmpty(_type)) {
			typeKey = "@" + _type;
		}
		String clsKey = "." + clsName;
		keys.add(clsKey);
		if (!TextUtils.isEmpty(idKey)) {
			keys.add(idKey + clsKey);
		}
		if (!TextUtils.isEmpty(typeKey)) {
			keys.add(clsKey + typeKey);
		}
		if (!TextUtils.isEmpty(idKey) && !TextUtils.isEmpty(typeKey)) {
			keys.add(idKey + clsKey + typeKey);
		}
		putSelectKeyByClassFromCache(_id, _type, clsName, keys);
		return keys;
	}

	private ArrayList<String> _cachedSelectKeys;

	public ArrayList<String> getSelectKeys() {
		if (_class == null) {
			if (_refView != null) {
				XulView xulView = _refView.get();
				if (xulView != null) {
					return xulView.getSelectKeys();
				}
			}
		}

		if (TextUtils.isEmpty(_id) && TextUtils.isEmpty(_type) && _class == null) {
			return null;
		}

		if (_cachedSelectKeys != null) {
			return _cachedSelectKeys;
		}

		ArrayList<String> keys = new ArrayList<String>();
		_cachedSelectKeys = keys;
		String idKey = XulUtils.STR_EMPTY;
		String typeKey = XulUtils.STR_EMPTY;
		if (!TextUtils.isEmpty(_id)) {
			idKey = "#" + _id;
			keys.add(idKey);
		}

		if (!TextUtils.isEmpty(_type)) {
			typeKey = "@" + _type;
			keys.add(typeKey);
		}
		if (!TextUtils.isEmpty(idKey) && !TextUtils.isEmpty(typeKey)) {
			keys.add(idKey + typeKey);
		}
		if (_class != null && _class != EMPTY_CLASS) {
			for (String cls : _class) {
				String clsKey = "." + cls;
				keys.add(clsKey);
				if (!TextUtils.isEmpty(idKey)) {
					keys.add(idKey + clsKey);
				}
				if (!TextUtils.isEmpty(typeKey)) {
					keys.add(clsKey + typeKey);
				}
				if (!TextUtils.isEmpty(idKey) && !TextUtils.isEmpty(typeKey)) {
					keys.add(idKey + clsKey + typeKey);
				}
			}
		}
		return keys;
	}

	public void addIndirectProp(XulAction prop) {
		_initActionProp();
		_action.add(prop);
	}

	public void addIndirectProp(XulStyle prop) {
		_initStyleProp();
		_styles.add(prop);
	}

	public void addIndirectProp(XulAttr prop) {
		_initAttrProp();
		_attrs.add(prop);
	}

	public void addIndirectProp(XulAction prop, int state) {
		_initActionProp();
		_action.add(prop, state);
	}

	public void addIndirectProp(XulStyle prop, int state) {
		_initStyleProp();
		_styles.add(prop, state);
	}

	public void addIndirectProp(XulAttr prop, int state) {
		_initAttrProp();
		_attrs.add(prop, state);
	}

	public void addIndirectProp(XulFocus prop) {
		_initFocusProp();
		_focus.add(prop);
	}

	public void removeIndirectProp(XulAction prop) {
		if (_action == null) {
			return;
		}
		_action.remove(prop);
	}

	public void removeIndirectProp(XulAttr prop) {
		if (_attrs == null) {
			return;
		}
		_attrs.remove(prop);
	}

	public void removeIndirectProp(XulStyle prop) {
		if (_styles == null) {
			return;
		}
		_styles.remove(prop);
	}

	public void removeIndirectProp(XulFocus prop) {
		if (_focus == null) {
			return;
		}
		_focus.remove(prop);
	}

	public void removeIndirectProp(XulAction prop, int state) {
		if (_action == null) {
			return;
		}
		_action.remove(prop, state);
	}

	public void removeIndirectProp(XulAttr prop, int state) {
		if (_attrs == null) {
			return;
		}
		_attrs.remove(prop, state);
	}

	public void removeIndirectProp(XulStyle prop, int state) {
		if (_styles == null) {
			return;
		}
		_styles.remove(prop, state);
	}

	// 因为焦点事件同时只会有一个，这里的事件对象可以共用一个
	static XulStateChangeEvent _focusChangeEvent = new XulStateChangeEvent();

	// 元素失去焦点
	public void onBlur() {
		updateFocusState(XulView.STATE_NORMAL);
		if (_parent != null) {
			_focusChangeEvent.event = "blur";
			_focusChangeEvent.oldState = XulView.STATE_FOCUSED;
			_focusChangeEvent.state = XulView.STATE_NORMAL;
			_focusChangeEvent.eventSource = this;
			_focusChangeEvent.alteredEventSource = null;
			_focusChangeEvent.notifySource = this;
			_focusChangeEvent.adjustFocusView = false;
			_parent.onChildStateChanged(_focusChangeEvent);
			_focusChangeEvent.eventSource = null;
			_focusChangeEvent.alteredEventSource = null;
			_focusChangeEvent.notifySource = null;
		}
	}

	// 元素获得焦点
	public void onFocus() {
		updateFocusState(XulView.STATE_FOCUSED);
		if (_parent != null) {
			_focusChangeEvent.event = "focus";
			_focusChangeEvent.oldState = XulView.STATE_NORMAL;
			_focusChangeEvent.state = XulView.STATE_FOCUSED;
			_focusChangeEvent.eventSource = this;
			_focusChangeEvent.alteredEventSource = null;
			_focusChangeEvent.notifySource = this;
			_focusChangeEvent.adjustFocusView = true;
			_parent.onChildStateChanged(_focusChangeEvent);
			_focusChangeEvent.eventSource = null;
			_focusChangeEvent.alteredEventSource = null;
			_focusChangeEvent.notifySource = null;
		}
	}

	void updateFocusState(int state) {
		if (_styles != null) {
			_styles.switchState(state);
		}
		if (_action != null) {
			_action.switchState(state);
		}
		if (_attrs != null) {
			_attrs.switchState(state);
		}
		if (_render != null) {
			_render.switchState(state);
		}
	}

	public void updateEnableState(boolean enable) {
		if (_styles != null) {
			_styles.switchEnabled(enable);
		}
		if (_action != null) {
			_action.switchEnabled(enable);
		}
		if (_attrs != null) {
			_attrs.switchEnabled(enable);
		}
	}

	public void setEnabled(boolean enable) {
		setAttr(XulPropNameCache.TagId.ENABLED, enable ? "true" : "false");
		if (_render != null) {
			_render.setEnabled(enable);
		}
	}

	public boolean isEnabled() {
		return _render == null || _render.isEnabled();
	}

	public boolean isVisible() {
		return _render == null || _render.isVisible();
	}

	public boolean isParentVisible() {
		XulView parent = this._parent;
		while (parent != null) {
			if (!parent.isVisible()) {
				return false;
			}
			parent = parent._parent;
		}
		return true;
	}

	public XulViewRender getRender() {
		return _render;
	}

	public XulArea getParent() {
		return _parent;
	}

	public void cleanImageItems() {
		if (_render == null) {
			return;
		}
		_render.cleanImageItems();
	}

	public XulAttr setAttr(String name, String val) {
		XulViewRender render = _render;
		if (render != null && render instanceof XulCustomViewRender) {
			IXulExternalView externalView = render.getExternalView();
			if (externalView != null) {
				if (externalView.setAttr(name, val)) {
					return null;
				}
			}
		}

		_initAttrProp();
		int nameId = XulPropNameCache.name2Id(name);

		if (val == null) {
			_attrs.removeOwnProp(nameId);
			return null;
		}

		XulAttr attr = _attrs.getOwnProp(nameId);
		if (attr == null || attr.isReferent()) {
			attr = XulAttr.obtain(nameId);
			_attrs.put(attr);
		}
		attr.setValue(val);
		return attr;
	}

	public XulAttr setAttr(int nameId, String val) {
		XulViewRender render = _render;
		if (render != null && render instanceof XulCustomViewRender) {
			IXulExternalView externalView = render.getExternalView();
			if (externalView != null) {
				String name = XulPropNameCache.id2Name(nameId);
				if (externalView.setAttr(name, val)) {
					return null;
				}
			}
		}

		_initAttrProp();
		if (val == null) {
			_attrs.removeOwnProp(nameId);
			return null;
		}

		XulAttr attr = _attrs.getOwnProp(nameId);
		if (attr == null || attr.isReferent()) {
			attr = XulAttr.obtain(nameId);
			_attrs.put(attr);
		}
		attr.setValue(val);
		return attr;
	}

	public String getAttrString(String key) {
		XulViewRender render = _render;
		if (render != null && render instanceof XulCustomViewRender) {
			IXulExternalView externalView = render.getExternalView();
			if (externalView != null) {
				String val = externalView.getAttr(key, key);
				if (val != key) {
					return val;
				}
			}
		}

		XulAttr xulAttr = getAttr(key);
		if (xulAttr != null) {
			return xulAttr.getStringValue();
		}
		return "";
	}

	public String getAttrString(int key) {
		XulViewRender render = _render;
		if (render != null && render instanceof XulCustomViewRender) {
			IXulExternalView externalView = render.getExternalView();
			if (externalView != null) {
				String keyStr = XulPropNameCache.id2Name(key);
				String val = externalView.getAttr(keyStr, keyStr);
				if (val != keyStr) {
					return val;
				}
			}
		}

		XulAttr xulAttr = getAttr(key);
		if (xulAttr != null) {
			return xulAttr.getStringValue();
		}
		return "";
	}

	public String getActionString(String key) {
		XulAction xulAction = getAction(key);
		if (xulAction != null) {
			return xulAction.getStringValue();
		}
		return "";
	}

	public String getActionString(int key) {
		XulAction xulAction = getAction(key);
		if (xulAction != null) {
			return xulAction.getStringValue();
		}
		return "";
	}

	public String getDataString(String key) {
		XulData data = getData(key);
		if (data != null) {
			return data.getStringValue();
		}
		return "";
	}

	public String getDataString(int key) {
		XulData data = getData(key);
		if (data != null) {
			return data.getStringValue();
		}
		return "";
	}

	public void updateLayout() {
		if (_render == null) {
			return;
		}
		_render.setUpdateLayout();
	}

	public void markDirtyView() {
		if (_render == null) {
			return;
		}
		_render.markDirtyView();
	}

	public boolean onKeyEvent(KeyEvent event) {
		if (_render == null) {
			return false;
		}
		return _render.onKeyEvent(event);
	}

	public IXulExternalView getExternalView() {
		if (_render == null) {
			return null;
		}
		return _render.getExternalView();
	}

	public void destroy() {
		internalDestroy();
	}

	protected void internalDestroy() {
		if (_render != null) {
			_render.destroy();
		}
		if (_attrs != null) {
			_attrs.destroy();
		}
		if (_styles != null) {
			_styles.destroy();
		}
		if (_action != null) {
			_action.destroy();
		}
		if (_data != null) {
			_data.destroy();
		}
		if (_focus != null) {
			_focus.destroy();
		}
	}

	public XulLayout getRootLayout() {
		return _root;
	}

	public void setBindingSource(XulBinding bindingSource) {
		this._bindingSource = bindingSource;
	}

	public XulBinding getBindingSource() {
		return _bindingSource;
	}

	XulCachedHashMap<String, IScriptableObject> _cachedScriptableObjects;
	XulScriptableObject _scriptableObject;

	public final IScriptableObject getScriptableObject(String scriptType) {
		if (_scriptableObject == null) {
			_scriptableObject = createScriptableObject();
		}

		if (_cachedScriptableObjects == null) {
			_cachedScriptableObjects = new XulCachedHashMap<String, IScriptableObject>();
		}
		IScriptableObject result = _cachedScriptableObjects.get(scriptType);
		if (result == null) {
			IScriptContext scriptContext = XulManager.getScriptContext(scriptType);
			if (scriptContext != null) {
				result = scriptContext.createScriptObject(_scriptableObject);
			}
			_cachedScriptableObjects.put(scriptType, result);
		}
		return result;
	}

	protected XulScriptableObject createScriptableObject() {
		return new XulViewScriptableObject<XulView>(this);
	}

	public String getId() {
		return _id;
	}

	public XulPage getOwnerPage() {
		return _root.getOwnerPage();
	}

	public boolean isChildOf(XulView view) {
		XulView parent = this._parent;
		while (parent != null) {
			if (parent == view) {
				return true;
			}
			parent = parent._parent;
		}
		return false;
	}

	public void removeSelf() {
		if (this.hasFocus()) {
			getOwnerPage().getLayout().requestFocus(null);
		}
		if (_parent == null) {
			return;
		}
		getOwnerPage().removeSelectorTarget(this, getSelectKeys());
		_parent.removeChild(this);
		_parent = null;
	}

	public boolean isDiscarded() {
		return _parent == null || !_parent.hasChild(this) || _parent.isDiscarded();
	}

	public boolean hitTest(int event, float x, float y) {
		if (_render == null || !isEnabled()) {
			return false;
		}
		return _render.hitTest(event, x, y);
	}

	public boolean hitTestTranslate(PointF pt) {
		if (_render == null || !isEnabled()) {
			return false;
		}
		return _render.hitTestTranslate(pt);
	}

	public boolean handleScrollEvent(float hScroll, float vScroll) {
		if (_render == null || !isEnabled()) {
			return false;
		}
		return _render.handleScrollEvent(hScroll, vScroll);
	}

	public WeakReference<XulView> getRefView() {
		return _refView;
	}

	public void cleanBindingCtx() {
		eachInlineProp(CLEAN_PROP_BINDING_ITERATOR);
		_bindingDataReady = false;
		_bindingSource = null;
		_bindingData = null;
	}
}
