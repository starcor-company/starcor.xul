package com.starcor.xul.ScriptWrappr;

import com.starcor.xul.Script.*;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptGetter;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptMethod;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulItem;
import com.starcor.xul.XulPage;
import com.starcor.xul.XulView;

import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * Created by hy on 2014/6/25.
 */
public class XulAreaScriptableObject extends XulViewScriptableObject<XulArea> {

	public XulAreaScriptableObject(XulArea item) {
		super(item);
	}

	private WeakHashMap<XulArrayList<XulView>, IScriptArray> _cachedScriptArray;

	@ScriptMethod("findItemById")
	public Boolean _script_findItemById(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		String id = args.getString(0);
		XulView itemById = _xulItem.getOwnerPage().findItemById(_xulItem, id);
		if (itemById == null) {
			return Boolean.FALSE;
		}
		IScriptableObject scriptableObject = itemById.getScriptableObject(ctx.getScriptType());
		args.setResult(scriptableObject);
		return Boolean.TRUE;
	}

	@ScriptMethod("findFirstItemByClass")
	public Boolean _script_findFirstItemByClass(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		XulView firstItemByClass;
		if (args.size() == 1) {
			String id = args.getString(0);
			firstItemByClass = XulPage.findFirstItemByClass(_xulItem, id);
		} else {
			String[] clsArray = new String[args.size()];
			for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
				clsArray[i] = args.getString(i);
			}
			firstItemByClass = XulPage.findFirstItemByClass(_xulItem, clsArray);
		}
		if (firstItemByClass != null) {
			args.setResult(firstItemByClass.getScriptableObject(ctx.getScriptType()));
		}
		return Boolean.TRUE;
	}

	@ScriptMethod("findItemsByClass")
	public Boolean _script_findItemsByClass(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}

		XulArrayList<XulView> itemsByClass;
		if (args.size() == 1) {
			String id = args.getString(0);
			itemsByClass = XulPage.findItemsByClass(_xulItem, id);
		} else {
			String[] clsArray = new String[args.size()];
			for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
				clsArray[i] = args.getString(i);
			}
			itemsByClass = XulPage.findItemsByClass(_xulItem, clsArray);
		}

		if (_cachedScriptArray == null) {
			_cachedScriptArray = new WeakHashMap<XulArrayList<XulView>, IScriptArray>();
		}

		// FIXME: cached array may contains invalid view object which will cause invalid JNI global object reference
		// FIXME: itemsByClass may contains dirty view object
		IScriptArray scriptArray = _cachedScriptArray.get(itemsByClass);
		if (scriptArray == null) {
			scriptArray = ctx.createScriptArray();
			scriptArray.addAll(itemsByClass);
			_cachedScriptArray.put(itemsByClass, scriptArray);
		}

		args.setResult(scriptArray);
		return Boolean.TRUE;
	}

	@ScriptMethod("setDynamicFocus")
	public Boolean _script_setDynamicFocus(IScriptContext ctx, IScriptArguments args) {
		IScriptableObject scriptableObject = args.getScriptableObject(0);
		if (scriptableObject == null) {
			return _xulItem.setDynamicFocus(null);
		} else {
			XulScriptableObject objectValue = scriptableObject.getObjectValue();
			XulView xulItem = (XulView) ((XulViewScriptableObject) objectValue)._xulItem;
			_xulItem.setDynamicFocus(xulItem);
		}
		return Boolean.TRUE;
	}

	@ScriptMethod("getDynamicFocus")
	public Boolean _script_getDynamicFocus(IScriptContext ctx, IScriptArguments args) {
		final XulView dynamicFocus = _xulItem.getDynamicFocus();
		if (dynamicFocus == null) {
			return Boolean.FALSE;
		}
		final String scriptType = ctx.getScriptType();
		args.setResult(dynamicFocus.getScriptableObject(scriptType));
		return Boolean.TRUE;
	}

	@ScriptMethod("getItem")
	public Boolean _script_getChild(IScriptContext ctx, IScriptArguments args) {
		int argNum = args.size();
		if (argNum < 1) {
			return Boolean.FALSE;
		}
		if (argNum == 1) {
			XulView child = _xulItem.getChild(args.getInteger(0));
			args.setResult(child.getScriptableObject(ctx.getScriptType()));
			return Boolean.TRUE;
		}

		IScriptArray children = ctx.createScriptArray();

		for (int i = 0; i < argNum; i++) {
			XulView child = _xulItem.getChild(args.getInteger(i));
			children.add(child.getScriptableObject(ctx.getScriptType()));
		}
		args.setResult(children);
		return Boolean.TRUE;
	}

	@ScriptGetter("children")
	public Object _script_getter_children(IScriptContext ctx) {
		final ArrayList<Object> children = new ArrayList<Object>();
		final String scriptType = ctx.getScriptType();

		_xulItem.eachChild(new XulArea.XulAreaIterator() {
			@Override
			public boolean onXulArea(int pos, XulArea area) {
				collectChild(area);
				return true;
			}

			@Override
			public boolean onXulItem(int pos, XulItem item) {
				collectChild(item);
				return true;
			}

			private void collectChild(XulView area) {
				IScriptableObject scriptableObject = area.getScriptableObject(scriptType);
				children.add(scriptableObject);
			}

		});
		return children;
	}

}
