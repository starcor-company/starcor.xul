package com.starcor.xul.ScriptWrappr;

import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Script.IScriptArguments;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptGetter;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptMethod;
import com.starcor.xul.ScriptWrappr.Annotation.ScriptSetter;
import com.starcor.xul.XulLayout;
import com.starcor.xul.XulPage;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/25.
 */
public class XulViewScriptableObject<T extends XulView> extends XulScriptableObjectWrapper<T> {
	public XulViewScriptableObject(T item) {
		super(item);
	}

	@ScriptMethod("getStyle")
	public Boolean _script_getStyle(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int nameId = args.getStringId(0);
		if (nameId < 0) {
			String name = args.getString(0);
			nameId = XulPropNameCache.name2Id(name);
			ctx.addIndexedString(name, nameId);
		}
		args.setResult(_xulItem.getStyleString(nameId));
		return Boolean.TRUE;
	}

	@ScriptMethod("setStyle")
	public Boolean _script_setStyle(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() != 2) {
			return Boolean.FALSE;
		}
		int nameId = args.getStringId(0);
		String value = args.getString(1);
		if (nameId < 0) {
			String name = args.getString(0);
			nameId = XulPropNameCache.name2Id(name);
			ctx.addIndexedString(name, nameId);
		}
		_xulItem.setStyle(nameId, value);
		_xulItem.resetRender();
		return Boolean.TRUE;
	}

	@ScriptMethod("getAttr")
	public Boolean _script_getAttr(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int nameId = args.getStringId(0);
		if (nameId < 0) {
			String name = args.getString(0);
			nameId = XulPropNameCache.name2Id(name);
			ctx.addIndexedString(name, nameId);
		}
		args.setResult(_xulItem.getAttrString(nameId));
		return Boolean.TRUE;
	}

	@ScriptMethod("getAction")
	public Boolean _script_getAction(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int nameId = args.getStringId(0);
		if (nameId < 0) {
			String name = args.getString(0);
			nameId = XulPropNameCache.name2Id(name);
			ctx.addIndexedString(name, nameId);
		}
		args.setResult(_xulItem.getActionString(nameId));
		return Boolean.TRUE;
	}

	@ScriptMethod("getData")
	public Boolean _script_getData(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		int nameId = args.getStringId(0);
		if (nameId < 0) {
			String name = args.getString(0);
			nameId = XulPropNameCache.name2Id(name);
			ctx.addIndexedString(name, nameId);
		}
		args.setResult(_xulItem.getDataString(nameId));
		return Boolean.TRUE;
	}

	@ScriptMethod("setAttr")
	public Boolean _script_setAttr(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() != 2) {
			return Boolean.FALSE;
		}
		String value = args.getString(1);
		int nameId = args.getStringId(0);
		if (nameId < 0) {
			String name = args.getString(0);
			nameId = XulPropNameCache.name2Id(name);
			ctx.addIndexedString(name, nameId);
		}
		_xulItem.setAttr(nameId, value);
		_xulItem.resetRender();
		return Boolean.TRUE;
	}

	@ScriptGetter("viewX")
	public Integer _script_getter_viewX(IScriptContext ctx) {
		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return 0;
		}
		return XulUtils.roundToInt(render.getFocusRect().left / render.getXScalar());
	}

	@ScriptGetter("viewY")
	public Integer _script_getter_viewY(IScriptContext ctx) {
		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return 0;
		}
		return XulUtils.roundToInt(render.getFocusRect().top / render.getYScalar());
	}

	@ScriptGetter("viewWidth")
	public Integer _script_getter_viewWidth(IScriptContext ctx) {
		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return 0;
		}
		return XulUtils.roundToInt(XulUtils.calRectWidth(render.getFocusRect()) / render.getXScalar());
	}

	@ScriptGetter("viewHeight")
	public Integer _script_getter_viewHeight(IScriptContext ctx) {
		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return 0;
		}
		return XulUtils.roundToInt(XulUtils.calRectHeight(render.getFocusRect()) / render.getYScalar());
	}

	@ScriptGetter("text")
	public String _script_getter_text(IScriptContext ctx) {
		return _xulItem.getAttrString("text");
	}

	@ScriptSetter("text")
	public String _script_setter_text(IScriptContext ctx, Object val) {
		String text = String.valueOf(val);
		_xulItem.setAttr("text", text);
		_xulItem.resetRender();
		return text;
	}

	@ScriptGetter("id")
	public String _script_getter_id(IScriptContext ctx) {
		return _xulItem.getId();
	}

	@ScriptGetter("type")
	public String _script_getter_type(IScriptContext ctx) {
		return _xulItem.getType();
	}

	@ScriptGetter("binding")
	public String _script_getter_binding(IScriptContext ctx) {
		return _xulItem.getBinding();
	}

	@ScriptGetter("ownerPage")
	public Object _script_getter_ownerPage(IScriptContext ctx) {
		XulPage page = _xulItem.getOwnerPage();
		IScriptableObject scriptableObject = page.getScriptableObject(ctx.getScriptType());
		return scriptableObject;
	}

	@ScriptGetter("parent")
	public Object _script_getter_parent(IScriptContext ctx) {
		if (_xulItem instanceof XulPage) {
			return null;
		}
		XulView parent = _xulItem;
		do {
			parent = _xulItem.getParent();
		} while (parent instanceof XulLayout);
		IScriptableObject scriptableObject = parent.getScriptableObject(ctx.getScriptType());
		return scriptableObject;
	}

	@ScriptGetter("isFocusable")
	public Boolean _script_getter_isFocusable(IScriptContext ctx) {
		return _xulItem.focusable();
	}

	@ScriptGetter("isVisible")
	public Boolean _script_getter_isVisible(IScriptContext ctx) {
		XulView parent = _xulItem;
		while (parent != null && parent.isVisible()) {
			parent = parent.getParent();
		}
		return parent == null;
	}

	@ScriptGetter("isEnabled")
	public Boolean _script_getter_isEnabled(IScriptContext ctx) {
		return Boolean.valueOf(_xulItem.isEnabled());
	}

	@ScriptSetter("isEnabled")
	public void _script_setter_isEnabled(IScriptContext ctx, Object val) {
		boolean enableState = !(val == null || "false".equalsIgnoreCase(String.valueOf(val)));
		if (_xulItem.isEnabled() != enableState) {
			_xulItem.setEnabled(enableState);
			_xulItem.resetRender();
		}
	}

	@ScriptGetter("isFocused")
	public Boolean _script_getter_isFocus(IScriptContext ctx) {
		return Boolean.valueOf(_xulItem.isFocused());
	}

	@ScriptGetter("hasFocus")
	public Boolean _script_getter_hasFocus(IScriptContext ctx) {
		return Boolean.valueOf(_xulItem.hasFocus());
	}

	@ScriptMethod("requestFocus")
	public Boolean _script_requestFocus(IScriptContext ctx) {
		if (_xulItem instanceof XulPage || _xulItem instanceof XulLayout) {
			return Boolean.FALSE;
		}
		if (_xulItem.isFocused()) {
			return Boolean.FALSE;
		}
		_xulItem.getRootLayout().requestFocus(_xulItem);
		return Boolean.TRUE;
	}

	@ScriptMethod("killFocus")
	public Boolean _script_killFocus(IScriptContext ctx) {
		if (_xulItem instanceof XulPage || _xulItem instanceof XulLayout) {
			return Boolean.FALSE;
		}
		if (!_xulItem.isFocused()) {
			return Boolean.FALSE;
		}
		_xulItem.getRootLayout().requestFocus(null);
		return Boolean.TRUE;
	}

	@ScriptMethod("classList")
	public Object _script_getter_classList(IScriptContext ctx) {
		return _xulItem.getAllClass();
	}

	@ScriptMethod("hasClass")
	public Boolean _script_hasClass(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
			if (!_xulItem.hasClass(args.getString(i))) {
				args.setResult(Boolean.FALSE);
				return Boolean.TRUE;
			}
		}
		args.setResult(Boolean.TRUE);
		return Boolean.TRUE;
	}

	@ScriptMethod("addClass")
	public Boolean _script_addClass(IScriptContext ctx, IScriptArguments args) {
		boolean update = false;
		for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
			try {
				update = _xulItem.addClass(args.getString(i)) || update;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (update) {
			_xulItem.resetRender();
		}
		return Boolean.valueOf(update);
	}

	@ScriptMethod("removeClass")
	public Boolean _script_removeClass(IScriptContext ctx, IScriptArguments args) {
		boolean update = false;
		for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
			try {
				update = _xulItem.removeClass(args.getString(i)) || update;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (update) {
			_xulItem.resetRender();
		}
		return Boolean.valueOf(update);
	}

	@ScriptGetter("isBindingSuccess")
	public Boolean _script_getter_isBindingSuccess(IScriptContext ctx) {
		return Boolean.valueOf(_xulItem.isBindingSuccess());
	}

	@ScriptGetter("isBindingReady")
	public Boolean _script_getter_isBindingReady(IScriptContext ctx) {
		return Boolean.valueOf(_xulItem.isBindingCtxReady());
	}

	// dispatchEvent("event", "type", "command");
	@ScriptMethod("dispatchEvent")
	public Boolean _script_dispatchEvent(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() <= 0) {
			return Boolean.FALSE;
		}
		String action = args.getString(0);
		String type = args.size() > 1 ? args.getString(1) : "";
		String command = args.size() > 2 ? args.getString(2) : "";
		return Boolean.valueOf(_xulItem.getOwnerPage().dispatchAction(_xulItem, action, type, command));
	}

	// fireEvent("action")
	@ScriptMethod("fireEvent")
	public Boolean _script_fireEvent(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() <= 0) {
			return Boolean.FALSE;
		}
		String action = args.getString(0);

		if (args.size() == 1) {
			XulPage.invokeAction(_xulItem, action);
		} else {
			int argsSize = args.size();
			Object[] passArgs = new Object[argsSize - 1];
			for (int i = 1; i < argsSize; i++) {
				IScriptableObject scriptableObject = args.getScriptableObject(i);
				if (scriptableObject == null) {
					passArgs[i - 1] = args.getString(i);
				} else {
					passArgs[i - 1] = scriptableObject;
				}
			}
			XulPage.invokeActionWithArgs(_xulItem, action, passArgs);
		}
		return Boolean.TRUE;
	}

	@ScriptMethod("findParentById")
	public Boolean _script_findParentById(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		String id = args.getString(0);

		XulView parent = _xulItem.findParentById(id);
		if (parent != null) {
			IScriptableObject scriptableObject = parent.getScriptableObject(ctx.getScriptType());
			args.setResult(scriptableObject);
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	@ScriptMethod("findParentByClass")
	public Boolean _script_findParentByClass(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		String clsName = args.getString(0);
		XulView parent = _xulItem.findParentByClass(clsName);
		if (parent != null) {
			IScriptableObject scriptableObject = parent.getScriptableObject(ctx.getScriptType());
			args.setResult(scriptableObject);
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	@ScriptMethod("findParentByType")
	public Boolean _script_findParentByType(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() == 0) {
			return Boolean.FALSE;
		}
		String typeName = args.getString(0);
		XulView parent = _xulItem.findParentByType(typeName);
		if (parent != null) {
			IScriptableObject scriptableObject = parent.getScriptableObject(ctx.getScriptType());
			args.setResult(scriptableObject);
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	@ScriptMethod("isChildOf")
	public Boolean _script_isChildOf(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() != 1) {
			return Boolean.FALSE;
		}
		IScriptableObject scriptableObject = args.getScriptableObject(0);
		if (scriptableObject == null) {
			return Boolean.FALSE;
		}
		XulView view = (XulView) (scriptableObject).getObjectValue().getUnwrappedObject();
		args.setResult(_xulItem.isChildOf(view));
		return Boolean.TRUE;
	}

	@ScriptMethod("blinkClass")
	public Boolean _script_blinkClass(IScriptContext ctx, IScriptArguments args) {
		if (args == null || args.size() <= 0) {
			return Boolean.FALSE;
		}

		XulViewRender render = _xulItem.getRender();
		if (render == null) {
			return Boolean.FALSE;
		}
		final String[] clsNames = new String[args.size()];
		for (int i = 0, argsLength = args.size(); i < argsLength; i++) {
			clsNames[i] = args.getString(i);
		}
		render.blinkClass(clsNames);
		return Boolean.TRUE;
	}
}
