package com.starcor.xul.ScriptWrappr;

import com.starcor.xul.Script.IScriptArguments;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.Script.XulScriptableObject;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptGetter;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptMethod;
import com.starcor.xul.Wrapper.XulMassiveAreaWrapper;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/25.
 */
public class XulMassiveScriptableObject extends XulAreaScriptableObject {
	XulMassiveAreaWrapper _massiveAdapter;

	public XulMassiveScriptableObject(XulArea item) {
		super(item);
		_massiveAdapter = XulMassiveAreaWrapper.fromXulView(item);
	}

	@ScriptMethod("getItemIdx")
	public Boolean _script_getItemIdx(IScriptContext ctx, IScriptArguments args) {
		if (_massiveAdapter == null) {
			return Boolean.FALSE;
		}
		IScriptableObject scriptableObject = args.getScriptableObject(0);
		if (scriptableObject == null) {
			args.setResult(-1);
		} else {
			XulScriptableObject objectValue = scriptableObject.getObjectValue();
			XulView xulItem = (XulView) ((XulViewScriptableObject) objectValue)._xulItem;
			args.setResult(_massiveAdapter.getItemIdx(xulItem));
		}
		return Boolean.TRUE;
	}

	@ScriptMethod("removeItem")
	public Boolean _script_removeItem(IScriptContext ctx, IScriptArguments args) {
		if (_massiveAdapter == null) {
			return Boolean.FALSE;
		}
		IScriptableObject scriptableObject = args.getScriptableObject(0);
		if (scriptableObject == null) {
			_massiveAdapter.removeItem(XulUtils.tryParseInt(args.getString(0), -1));
		} else {
			XulScriptableObject objectValue = scriptableObject.getObjectValue();
			XulView xulItem = (XulView) ((XulViewScriptableObject) objectValue)._xulItem;
			_massiveAdapter.removeItem(xulItem);
		}
		return Boolean.TRUE;
	}

	@ScriptMethod("makeChildVisible")
	public Boolean _script_makeChildVisible(IScriptContext ctx, IScriptArguments args) {
		if (_massiveAdapter == null) {
			return Boolean.FALSE;
		}

		int argSize = args.size();
		if (argSize < 2 || argSize > 6) {
			return Boolean.FALSE;
		}

		XulArea ownerSlider = null;
		IScriptableObject scriptableObject = args.getScriptableObject(0);
		if (scriptableObject == null) {
			// no owner slider
			ownerSlider = _xulItem.findParentByType("slider");
		} else {
			XulScriptableObject objectValue = scriptableObject.getObjectValue();
			XulView view = (XulView) ((XulViewScriptableObject) objectValue)._xulItem;
			if (view instanceof XulArea) {
				ownerSlider = (XulArea) view;
			}
		}

		if (ownerSlider == null) {
			return Boolean.FALSE;
		}

		int itemIdx = args.getInteger(1);

		switch (argSize) {
		case 2: // ownerSlider, item
			_massiveAdapter.makeChildVisible(ownerSlider, itemIdx);
			break;
		case 3: // ownerSlider, item, animation
			// ownerSlider, item, align, {true}
		{
			String arg3Str = args.getString(2);
			if ("true".equals(arg3Str)) {
				_massiveAdapter.makeChildVisible(ownerSlider, itemIdx, true);
			} else if ("false".equals(arg3Str)) {
				_massiveAdapter.makeChildVisible(ownerSlider, itemIdx, false);
			} else {
				_massiveAdapter.makeChildVisible(ownerSlider, itemIdx, XulUtils.tryParseFloat(arg3Str, 0), true);
			}
		}
		break;
		case 4: // ownerSlider, item, align, animation
			// ownerSlider, item, align, alignPoint, {true}
		{
			float align = args.getFloat(2);
			String arg4Str = args.getString(3);
			if ("true".equals(arg4Str)) {
				_massiveAdapter.makeChildVisible(ownerSlider, itemIdx, align, true);
			} else if ("false".equals(arg4Str)) {
				_massiveAdapter.makeChildVisible(ownerSlider, itemIdx, align, false);
			} else {
				_massiveAdapter.makeChildVisible(ownerSlider, itemIdx, align, XulUtils.tryParseFloat(arg4Str, 0), true);
			}
		}
		break;
		case 5: // ownerSlider, item, align, animation, alignPoint
		{
			float align = args.getFloat(2);
			float alignPoint = args.getFloat(3);
			_massiveAdapter.makeChildVisible(ownerSlider, itemIdx, align, alignPoint);
		}
		break;
		case 6: // ownerSlider, item, align, animation, alignPoint, animation
		{
			float align = args.getFloat(2);
			float alignPoint = args.getFloat(3);
			boolean animation = args.getBoolean(4);
			_massiveAdapter.makeChildVisible(ownerSlider, itemIdx, align, alignPoint, animation);
		}
		break;
		}
		return Boolean.TRUE;
	}

	@ScriptMethod("syncContentView")
	public Boolean _script_removeItem(IScriptContext ctx) {
		if (_massiveAdapter == null) {
			return Boolean.FALSE;
		}
		_massiveAdapter.syncContentView();
		return Boolean.TRUE;
	}

	@ScriptMethod("clear")
	public Boolean _script_clear(IScriptContext ctx) {
		if (_massiveAdapter == null) {
			return Boolean.FALSE;
		}
		_massiveAdapter.clear();
		return Boolean.TRUE;
	}

	@ScriptGetter("isFixed")
	public Boolean _script_getter_isFixed(IScriptContext ctx) {
		if (_massiveAdapter == null) {
			return Boolean.FALSE;
		}
		return _massiveAdapter.fixedItem();
	}
	@ScriptGetter("itemNum")
	public Integer _script_getter_itemNum(IScriptContext ctx) {
		if (_massiveAdapter == null) {
			return 0;
		}
		return _massiveAdapter.itemNum();
	}
}
