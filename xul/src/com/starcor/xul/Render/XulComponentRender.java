package com.starcor.xul.Render;

import android.text.TextUtils;
import com.starcor.xul.Prop.XulAction;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Utils.XulLayoutHelper;
import com.starcor.xul.*;

/**
 * Created by hy on 2015/4/27.
 */
public class XulComponentRender extends XulAreaRender {
	public static void register() {
		XulRenderFactory.registerBuilder("area", "component", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				return new XulComponentRender(ctx, (XulArea) view);
			}
		});
	}

	public XulComponentRender(XulRenderContext ctx, XulArea area) {
		super(ctx, area);
	}

	@Override
	public void onRenderCreated() {
		String componentId = _area.getAttrString(XulPropNameCache.TagId.COMPONENT);

		if (testComponentId(componentId)) {
			return;
		}
		_syncComponent(componentId);
	}

	String _componentId;
	XulComponent _component;

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		super.syncData();
		String componentId = _area.getAttrString(XulPropNameCache.TagId.COMPONENT);

		if (testComponentId(componentId)) {
			return;
		}

		setUpdateLayout(true);
	}

	private boolean testComponentId(String componentId) {
		if (_componentId == componentId) {
			return true;
		}

		if (TextUtils.isEmpty(componentId)) {
			_cleanOldComponent();
			return true;
		}

		if (componentId.equals(_componentId)) {
			return true;
		}
		return false;
	}

	private void _syncComponentAttr() {
		if (!_isLayoutChanged()) {
			return;
		}
		String componentId = _area.getAttrString(XulPropNameCache.TagId.COMPONENT);

		if (testComponentId(componentId)) {
			return;
		}

		_cleanOldComponent();
		_syncComponent(componentId);

		if (_component != null) {
			XulPage ownerPage = _area.getOwnerPage();
			XulAction action = _component.getAction(XulPropNameCache.TagId.ACTION_COMPONENT_CHANGED);
			if (action != null) {
				ownerPage.invokeActionNoPopup(_area, action);
			}
		}
	}

	private void _syncComponent(String componentId) {
		_componentId = componentId;
		_component = XulManager.getComponent(componentId);
		if (_component == null) {
			return;
		}
		_component.makeInstanceOn(_area);
		_area.prepareChildrenRender(getRenderContext());
		XulPage ownerPage = _area.getOwnerPage();
		ownerPage.addSelectors(_component);
		if (!ownerPage.rebindView(_area, getRenderContext().getDefaultActionCallback())) {
			// if rebindView failed, add binding context into pending binding context queue
			ownerPage.updateBindingContext(_area);
		}
		ownerPage.addSelectorTargets(_area);
		ownerPage.applySelectors(_area);

		XulAction action = _component.getAction(XulPropNameCache.TagId.ACTION_COMPONENT_INSTANCED);
		if (action != null) {
			ownerPage.invokeActionNoPopup(_area, action);
		}
	}

	private void _cleanOldComponent() {
		// 若component有焦点，需要先移除焦点
		if (_componentId != null) {
			XulLayout rootLayout = _area.getRootLayout();
			XulView focus = rootLayout.getFocus();
			if ((focus != null) && focus.isChildOf(_area)) {
				rootLayout.killFocus();
			}
		}

		_componentId = null;
		_component = null;
		_area.removeAllChildrenUpdateSelector();
	}

	protected class ComponentLayoutContainer extends XulAreaRender.LayoutContainer {
		@Override
		public int prepare() {
			_syncComponentAttr();
			return super.prepare();
		}
	}

	@Override
	protected XulLayoutHelper.ILayoutElement createElement() {
		return new ComponentLayoutContainer();
	}
}
