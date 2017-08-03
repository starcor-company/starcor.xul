package com.starcor.xul;

import android.util.Log;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulParserData;
import com.starcor.xul.Factory.XulParserDataNoStoreSupported;
import com.starcor.xul.Factory.XulParserDataStoreSupported;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/5.
 */
public class XulComponent extends XulArea {
	private static final String TAG = XulComponent.class.getSimpleName();

	private ArrayList<XulSelect> _selectors;
	private XulParserData _xulParserData;
	private String _ownerId;

	public XulComponent(String ownerId) {
		_ownerId = ownerId;
	}

	public String getOwnerId() {
		return _ownerId;
	}

	public void addSelector(XulSelect select) {
		if (_selectors == null) {
			_selectors = new ArrayList<XulSelect>();
		}
		_selectors.add(select);
	}

	public void makeInstanceOn(XulArea xulArea) {
		_initComponent();
		int childrenSize = _children.size();
		for (int i = 0; i < childrenSize; i++) {
			Object obj = _children.get(i);
			if (obj instanceof XulArea) {
				XulArea area = (XulArea) obj;
				area.makeClone(xulArea);
			} else if (obj instanceof XulTemplate) {
				XulTemplate template = (XulTemplate) obj;
				template.makeClone(xulArea);
			} else if (obj instanceof XulItem) {
				XulItem item = (XulItem) obj;
				item.makeClone(xulArea);
			} else {
				Log.d(TAG, "unsupported children type!!! - " + obj.getClass().getSimpleName());
			}
		}
	}

	public ArrayList<XulSelect> getSelectors() {
		return _selectors;
	}

	private void _initComponent() {
		if (_xulParserData != null) {
			XulUtils.ticketMarker marker = new XulUtils.ticketMarker("BENCH!! ", true);
			marker.mark();
			_xulParserData.buildItem(_ComponentBuilder.create(this));
			_xulParserData = null;
			this.updateSelectorPriorityLevel();
			marker.mark("_initComponent");
			Log.d(TAG, marker.toString());
		}
	}

	void updateSelectorPriorityLevel() {
		if (_selectors == null) {
			return;
		}
		for (int i = 0; i < _selectors.size(); ++i) {
			XulSelect selector = _selectors.get(i);
			selector.setPriorityLevel(i + 1, 0x4000000);
		}
	}

	public static class _ComponentBuilder extends XulArea._Builder {

		@Override
		public XulFactory.ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, XulFactory.Attributes attrs) {
			if ("selector".equals(name)) {
				return new XulFactory.ItemBuilder() {
					@Override
					public boolean initialize(String name, XulFactory.Attributes attrs) {
						return super.initialize(name, attrs);
					}

					@Override
					public XulFactory.ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, XulFactory.Attributes attrs) {
						if (name.equals("select")) {
							XulSelect._Builder builder = XulSelect._Builder.create((XulComponent) _area);
							builder.initialize(name, attrs);
							return builder;
						}

						return XulManager.CommonDummyBuilder;
					}

					@Override
					public boolean pushText(String path, XulFactory.IPullParser parser) {
						return super.pushText(path, parser);
					}

					@Override
					public Object finalItem() {
						return super.finalItem();
					}
				};
			}
			return super.pushSubItem(parser, path, name, attrs);
		}

		@Override
		public void recycle() {
			_ComponentBuilder.recycle(this);
		}

		public static _ComponentBuilder create(XulComponent component) {
			_ComponentBuilder builder = _ComponentBuilder.create();
			builder.init(component);
			return builder;
		}

		private void init(XulComponent component) {
			_area = component;
		}

		private static _ComponentBuilder create() {
			_ComponentBuilder builder = _recycled_builder.isEmpty() ? null : _recycled_builder.remove(_recycled_builder.size() - 1);
			if (builder == null) {
				builder = new _ComponentBuilder();
			}
			return builder;
		}

		private static ArrayList<_ComponentBuilder> _recycled_builder = new ArrayList<_ComponentBuilder>();

		private static void recycle(_ComponentBuilder builder) {
			_recycled_builder.add(builder);
			builder._area = null;
			builder._ownerTemplate = null;
		}
	}

	public static class _Builder extends XulFactory.ItemBuilder {
		XulManager _manager;
		XulFactory.ResultBuilderContext _ctx;
		XulComponent _component;
		XulParserData _xulParserData;

		public _Builder(XulManager mgr, XulFactory.ResultBuilderContext ctx) {
			_manager = mgr;
			_ctx = ctx;
			_component = new XulComponent(_ctx.getName());
		}

		@Override
		public boolean initialize(String name, XulFactory.Attributes attrs) {
			_component._type = XulUtils.getCachedString("component");
			_component._id = attrs.getValue("id");
			return true;
		}

		@Override
		public XulFactory.ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, XulFactory.Attributes attrs) {
			if (parser != null) {
				Object storePos = parser.storeParserPos();
				if (storePos != null) {
					_component._xulParserData = _xulParserData = new XulParserDataStoreSupported(parser, storePos);
					return null;
				}
			}
			XulParserDataNoStoreSupported parserData;
			if (_xulParserData == null) {
				parserData = new XulParserDataNoStoreSupported();
				_component._xulParserData = _xulParserData = parserData;
			} else {
				parserData = (XulParserDataNoStoreSupported) _xulParserData;
			}
			return parserData.pushSubItem(path, name, attrs);
		}

		@Override
		public Object finalItem() {
			_manager.addComponent(_component);
			return super.finalItem();
		}

		public static _Builder create(XulFactory.ResultBuilderContext _ctx, XulManager mgr) {
			_Builder builder = new _Builder(mgr, _ctx);
			return builder;
		}
	}
}
