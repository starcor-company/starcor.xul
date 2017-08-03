package com.starcor.xul.ScriptWrappr;

import com.starcor.xul.Script.IScriptArguments;
import com.starcor.xul.Script.IScriptArray;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptGetter;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptMethod;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.XulDataNode;
import com.starcor.xul.XulPage;
import com.starcor.xul.XulView;

import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * Created by hy on 2014/6/25.
 */
public class XulPageScriptableObject extends XulViewScriptableObject<XulPage> {

	public XulPageScriptableObject(XulPage item) {
		super(item);
	}
	private WeakHashMap<XulArrayList<XulView>, IScriptArray> _cachedScriptArray;

	@ScriptMethod("findItemById")
	public Boolean _script_findItemById(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		String id = args.getString(0);
		XulView itemById = _xulItem.findItemById(id);
		if (itemById == null) {
			return Boolean.FALSE;
		}
		IScriptableObject scriptableObject = itemById.getScriptableObject(ctx.getScriptType());
		args.setResult(scriptableObject);
		return Boolean.FALSE;
	}

	@ScriptMethod("findFirstItemByClass")
	public Boolean _script_findFirstItemByClass(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		XulView firstItemByClass;
		if (args.size() == 1) {
			String id = args.getString(0);
			firstItemByClass = XulPage.findFirstItemByClass(_xulItem.getLayout(), id);
		} else {
			String[] clsArray = new String[args.size()];
			for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
				clsArray[i] = args.getString(i);
			}
			firstItemByClass = XulPage.findFirstItemByClass(_xulItem.getLayout(), clsArray);
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
			itemsByClass = XulPage.findItemsByClass(_xulItem.getLayout(), id);
		} else {
			String[] clsArray = new String[args.size()];
			for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
				Object arg = args.getString(i);
				clsArray[i] = String.valueOf(arg);
			}
			itemsByClass = XulPage.findItemsByClass(_xulItem.getLayout(), clsArray);
		}


		if (_cachedScriptArray == null) {
			_cachedScriptArray = new WeakHashMap<XulArrayList<XulView>, IScriptArray>();
		}

		IScriptArray scriptArray = _cachedScriptArray.get(itemsByClass);
		if (scriptArray == null) {
			scriptArray = ctx.createScriptArray();
			scriptArray.addAll(itemsByClass);
			_cachedScriptArray.put(itemsByClass, scriptArray);
		}

		args.setResult(scriptArray);
		return Boolean.TRUE;
	}

	@ScriptGetter("currentFocus")
	public Object _script_getter_currentFocus(IScriptContext ctx) {
		XulView focus = _xulItem.getLayout().getFocus();
		if (focus == null) {
			return null;
		}
		IScriptableObject scriptableObject = focus.getScriptableObject(ctx.getScriptType());
		return scriptableObject;
	}

	@ScriptMethod("queryBindingDataString")
	public Boolean _script_queryBindingDataString(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		String selector = args.getString(0);
		ArrayList<XulDataNode> xulDataNodes = _xulItem.queryBindingData(selector);
		if (xulDataNodes == null || xulDataNodes.isEmpty()) {
			return Boolean.FALSE;
		}
		args.setResult(xulDataNodes.get(0).getValue());
		return Boolean.TRUE;
	}

	@ScriptMethod("refreshBinding")
	public Boolean _script_refreshBinding(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0 || args.size() > 2) {
			return Boolean.FALSE;
		}
		String bindingId = args.getString(0);
		switch (args.size()) {
		case 1:
			_xulItem.refreshBinding(bindingId);
			break;
		case 2:
			_xulItem.refreshBinding(bindingId, args.getString(1));
			break;
		}
		return Boolean.TRUE;
	}

	@ScriptMethod("pushStates")
	public Boolean _script_pushStates(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() <= 2) {
			return Boolean.FALSE;
		}
		Object[] newArgs = new Object[args.size()];

		for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
			IScriptableObject scriptableObject = args.getScriptableObject(i);
			if (scriptableObject != null) {
				newArgs[i] = scriptableObject.getObjectValue().getUnwrappedObject();
			} else {
				newArgs[i] = args.getString(i);
			}
		}
		_xulItem.pushStates(newArgs);
		return Boolean.TRUE;
	}

	@ScriptMethod("popStates")
	public Boolean _script_popStates(IScriptContext ctx, IScriptArguments args) {
		if (args != null && args.size() == 1) {
			args.setResult(_xulItem.popStates(args.getBoolean(0)));
			return Boolean.TRUE;
		}
		args.setResult(_xulItem.popStates());
		return Boolean.TRUE;
	}

	@ScriptMethod("popAllStates")
	public Boolean _script_popAllStates(IScriptContext ctx, IScriptArguments args) {
		if (args != null && args.size() == 1) {
			args.setResult(_xulItem.popAllStates(args.getBoolean(0)));
			return Boolean.TRUE;
		}
		args.setResult(_xulItem.popAllStates(false));
		return Boolean.TRUE;
	}
}
