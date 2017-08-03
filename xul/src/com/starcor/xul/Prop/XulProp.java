package com.starcor.xul.Prop;

import android.text.TextUtils;
import com.starcor.xul.XulUtils;

/**
 * Created by hy on 2014/5/4.
 */
public class XulProp {
	boolean _referent = true;
	int _nameId = -1;
	Object _value;
	String _binding;
	String _desc;
	boolean _pending = true;
	int _priority = 0;
	private XulBinding _bindingSource;

	XulProp() {
	}

	XulProp(XulProp orgProp) {
		this._nameId = orgProp._nameId;
		this._value = orgProp._value;
		this._binding = orgProp._binding;
		this._desc = orgProp._desc;
		this._pending = orgProp._pending;
		this._priority = orgProp._priority;
		this._bindingSource = orgProp._bindingSource;
		this._referent = false;
	}

	public boolean isReferent() {
		return _referent;
	}

	public int getPriority() {
		return _priority;
	}

	// 属性数据是否为动态绑定模式
	public boolean isBinding() {
		return !TextUtils.isEmpty(_binding);
	}

	// 数据是否等待绑定
	public boolean isBindingPending() {
		if (_bindingSource != null) {
			return _bindingSource.isUpdated();
		}
		return isBinding() && _pending;
	}

	// 设置数据已绑定
	public void setBindingReady() {
		_pending = false;
	}

	public String getBinding() {
		return _binding;
	}

	public String getName() {
		return XulPropNameCache.id2Name(_nameId);
	}

	public int getNameId() {
		return _nameId;
	}

	public String getStringValue() {
		return _value == null ? XulUtils.STR_EMPTY : String.valueOf(_value);
	}

	public Object getValue() {
		return _value;
	}

	public void setValue(Object value) {
		if (value instanceof String) {
			_value = XulUtils.getCachedString((String) value);
		} else {
			_value = value;
		}
	}

	public XulProp makeClone() {
		return this;
	}

	public void setBinding(String binding) {
		this._binding = XulUtils.getCachedString(binding);
	}

	public void setPriority(int priority) {
		this._priority = priority;
	}

	public void setBindingSource(XulBinding bindingSource) {
		this._bindingSource = bindingSource;
		if (this._bindingSource == null) {
			_pending = true;
		}
	}

	public XulBinding getBindingSource() {
		return _bindingSource;
	}
}
