package com.starcor.xul;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;

import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.Attributes;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Factory.XulFactory.ResultBuilder;
import com.starcor.xul.Prop.XulBinding;
import com.starcor.xul.Script.IScript;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Script.V8.V8ScriptContext;
import com.starcor.xul.Script.XulScriptFactory;
import com.starcor.xul.Utils.XulBindingSelector;
import com.starcor.xul.Utils.XulCachedHashMap;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hy on 2014/5/4.
 */
public class XulManager {
	// do not use Integer.MAX_VALUE directly, it will cause some bug with switch/case
	public static final int SIZE_AUTO = Integer.MAX_VALUE - 1;
	public static final int SIZE_MATCH_CONTENT = Integer.MAX_VALUE - 2;
	public static final int SIZE_MATCH_PARENT = Integer.MAX_VALUE - 3;
	public static final int SIZE_MAX = Integer.MAX_VALUE - 100;

	public static final int HIT_EVENT_DUMMY = -1;
	public static final int HIT_EVENT_SCROLL = MotionEvent.ACTION_SCROLL;
	public static final int HIT_EVENT_UP = MotionEvent.ACTION_UP;
	public static final int HIT_EVENT_DOWN = MotionEvent.ACTION_DOWN;

	public static Bitmap.Config DEF_PIXEL_FMT = Bitmap.Config.ARGB_8888;

	///////////////////////////////////////////////////////////////////////////////////////
	private static final String TAG = XulManager.class.getSimpleName();
	public static final boolean PERFORMANCE_BENCH = false;
	public static final boolean DEBUG = false;
	public static final boolean DEBUG_BINDING = false;
	public static final boolean DEBUG_XUL_WORKER = false;
	public static final boolean DEBUG_CANVAS_SAVE_RESTORE = false;
	public static final boolean DEBUG_V8_ENGINE = false;

	private static XulManager _instance;
	private static XulCachedHashMap<String, XulPage> _pages = new XulCachedHashMap<String, XulPage>(128);
	private static XulCachedHashMap<String, XulComponent> _components = new XulCachedHashMap<String, XulComponent>(64);

	private static ArrayList<XulSelect> _selectors = new ArrayList<XulSelect>();

	private static ArrayList<XulBinding> _globalBinding = new ArrayList<XulBinding>();

	public static final ItemBuilder CommonDummyBuilder = new ItemBuilder() {
		@Override
		public boolean initialize(String name, Attributes attrs) {
			return true;
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			Log.d(TAG, "drop sub item: " + name);
			return this;
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

	private static int _pageWidth = 1280;
	private static int _pageHeight = 720;

	private static int _targetPageWidth = 1280;
	private static int _targetPageHeight = 720;

	private XulManager() {
	}

	public static ArrayList<XulSelect> getSelectors() {
		return _selectors;
	}

	// 当前显示页面宽度
	public static int getPageWidth() {
		return _pageWidth;
	}

	// 当前显示页面高度
	public static int getPageHeight() {
		return _pageHeight;
	}

	public static void setPageSize(int pageWidth, int pageHeight) {
		_pageWidth = pageWidth;
		_pageHeight = pageHeight;
	}

	public static float getGlobalXScalar() {
		return (float) (_pageWidth) / _targetPageWidth;
	}

	public static float getGlobalYScalar() {
		return (float) (_pageHeight) / _targetPageHeight;
	}

	// 当前设计页面宽度
	public static int getDesignWidth() {
		return _targetPageWidth;
	}

	// 当前设计页面高度
	public static int getDesignHeight() {
		return _targetPageHeight;
	}

	// 百分比位置转换为屏幕坐标
	public static int percentageToScreenX(float p) {
		return XulUtils.roundToInt(p * _pageWidth);
	}

	// 百分比位置转换为屏幕坐标
	public static int percentageToScreenY(float p) {
		return XulUtils.roundToInt(p * _pageHeight);
	}

	// 百分比位置转换为设计坐标
	public static int percentageToDesignX(float p) {
		return XulUtils.roundToInt(p * _targetPageWidth);
	}

	// 百分比位置转换为设计坐标
	public static int percentageToDesignY(float p) {
		return XulUtils.roundToInt(p * _targetPageHeight);
	}

	public static boolean isXulLoaded() {
		return !_pages.isEmpty();
	}

	public static boolean isXulLoaded(String xmlPath) {
		if (_pages.isEmpty()) {
			return false;
		}
		if (TextUtils.isEmpty(xmlPath)) {
			return isXulLoaded();
		}

		for (XulPage xulPage : _pages.values()) {
			if (xmlPath.equals(xulPage.getOwnerId())) {
				return true;
			}
		}

		for (XulComponent xulComponent : _components.values()) {
			if (xmlPath.equals(xulComponent.getOwnerId())) {
				return true;
			}
		}
		// FIXME: the xul file without page elements, returns false
		return false;
	}

	public void addPage(XulPage xulPage) {
		_pages.put(xulPage.getId(), xulPage);
	}

	public void addSelector(XulSelect select) {
		_selectors.add(select);
	}

	private static IScriptContext _lastScriptContext = null;
	private static String _lastScriptContextType = "";
	private static final XulCachedHashMap<String, IScriptContext> _scriptContextMap = new XulCachedHashMap<String, IScriptContext>();

	public static IScriptContext getScriptContext(String scriptType) {
		if (_lastScriptContext != null && _lastScriptContextType.equals(scriptType)) {
			return _lastScriptContext;
		}

		synchronized (_scriptContextMap) {
			IScriptContext scriptContext = _scriptContextMap.get(scriptType);
			if (scriptContext != null) {
				_lastScriptContext = scriptContext;
				_lastScriptContextType = scriptType;
				return scriptContext;
			}
			long t;
			if (XulManager.PERFORMANCE_BENCH) {
				t = XulUtils.timestamp_us();
			}
			scriptContext = XulScriptFactory.createScriptContext(scriptType);
			scriptContext.init();
			if (XulManager.PERFORMANCE_BENCH) {
				Log.d(TAG, String.format("BENCH!!! createScriptContext %d", (XulUtils.timestamp_us() - t)));
			}
			_scriptContextMap.put(scriptType, scriptContext);
			_lastScriptContext = scriptContext;
			_lastScriptContextType = scriptType;
			return scriptContext;
		}
	}

	public static void addGlobalBinding(XulBinding binding) {
		if (!_globalBinding.contains(binding)) {
			_globalBinding.add(binding);
		}
	}

	public static XulBinding getGlobalBinding(String id) {
		for (XulBinding binding : _globalBinding) {
			if (binding.getId().equals(id)) {
				return binding;
			}
		}
		return null;
	}

	public static boolean isXulPageLoaded(String pageId) {
		return _pages.containsKey(pageId);
	}

	public static void addComponent(XulComponent component) {
		_components.put(component.getId(), component);
	}

	public static XulComponent getComponent(String componentName) {
		return _components.get(componentName);
	}

	private static File _baseTmpFile;

	public static void setBaseTempPath(File path) {
		_baseTmpFile = path;
	}

	public static File getTempPath(String category, String name) {
		File categoryPath = new File(_baseTmpFile, category);
		categoryPath.mkdirs();
		return new File(categoryPath, name);
	}

	static class _ScriptBuilder extends ItemBuilder {
		@Override
		public boolean initialize(String name, Attributes attrs) {
			String type = attrs.getValue("type");
			String source = attrs.getValue("src");

			if (type.startsWith("script/")) {
				IScriptContext scriptContext = getScriptContext(type.substring(7));
				if (scriptContext == null) {
					if (XulManager.DEBUG) {
						Log.e(TAG, "Can't get script context: " + type);
					}
					return false;
				}
				InputStream inputStream = XulWorker.loadData(source, true);
				if (inputStream == null) {
					if (XulManager.DEBUG) {
						Log.e(TAG, "Load script failed: " + source);
					}
					return false;
				}
				String sourceFile;
				if (source.startsWith("file:///.assets/")) {
					sourceFile = source.substring(16);
				} else {
					sourceFile = source;
				}

				XulUtils.ticketMarker tm = null;
				if (PERFORMANCE_BENCH) {
					tm = new XulUtils.ticketMarker("load js ", false);
					tm.mark();
				}
				IScript script = scriptContext.compileScript(inputStream, sourceFile, 1);
				if (tm != null) {
					tm.mark("compile");
				}
				if (script == null) {
					if (XulManager.DEBUG) {
						Log.e(TAG, "Compile script failed: " + source);
					}
					if (tm != null) {
						Log.d("BENCH!!!", tm.toString());
					}
					return false;
				}
				script.run(scriptContext, null);
				if (tm != null) {
					tm.mark("run");
					Log.d("BENCH!!!", tm.toString());
				}
			}
			return true;
		}
	}

	private static _ScriptBuilder _scriptBuilder;

	static _ScriptBuilder getScriptBuilder() {
		if (_scriptBuilder == null) {
			_scriptBuilder = new _ScriptBuilder();
		}
		return _scriptBuilder;
	}

	static class _Builder extends ItemBuilder {
		private final XulFactory.ResultBuilderContext _ctx;

		public _Builder(XulFactory.ResultBuilderContext ctx) {
			_ctx = ctx;
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			if (_instance == null) {
				_instance = new XulManager();
			}
			String screen = attrs.getValue("screen");
			Pattern screenPat = Pattern.compile("(\\d+)x(\\d+)");
			if (TextUtils.isEmpty(screen)) {
				screen = "1280x720";
			}
			Matcher matcher = screenPat.matcher(screen);
			if (matcher.matches()) {
				_targetPageWidth = XulUtils.tryParseInt(matcher.group(1));
				_targetPageHeight = XulUtils.tryParseInt(matcher.group(2));
			} else {
				_targetPageWidth = 1280;
				_targetPageHeight = 720;
			}
			return true;
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			if (name.equals("page")) {
				XulPage._Builder builder = new XulPage._Builder(_ctx, _instance, _targetPageWidth, _targetPageHeight);
				builder.initialize(name, attrs);
				return builder;
			}
			if (name.equals("component")) {
				XulComponent._Builder builder = XulComponent._Builder.create(_ctx, _instance);
				builder.initialize(name, attrs);
				return builder;
			}
			if (name.equals("selector")) {
				return new ItemBuilder() {
					@Override
					public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
						if (name.equals("select")) {
							XulSelect._Builder builder = XulSelect._Builder.create(_ctx, _instance);
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
						return null;
					}
				};
			}
			if (name.equals("binding")) {
				XulBinding._Builder builder = XulBinding._Builder.create(_instance);
				builder.initialize(name, attrs);
				return builder;
			}

			if (name.equals("script")) {
				ItemBuilder itemBuilder = getScriptBuilder();
				itemBuilder.initialize(name, attrs);
				return itemBuilder;
			}

			if (name.equals("import")) {

			}
			return XulManager.CommonDummyBuilder;
		}

		@Override
		public Object finalItem() {
			return _instance;
		}
	}

	static class _Factory extends ResultBuilder {
		@Override
		public ItemBuilder build(XulFactory.ResultBuilderContext ctx, String name, Attributes attrs) {
			if (name.equals("starcor.xul")) {
				_Builder pageBuilder = new _Builder(ctx);
				pageBuilder.initialize(name, attrs);
				return pageBuilder;
			}
			return null;
		}
	}

	static {
		XulFactory.registerBuilder(XulManager.class, new _Factory());
		V8ScriptContext.register();
	}

	static public XulManager build(byte[] xml, String xmlPath) throws Exception {
		return XulFactory.build(XulManager.class, xml, xmlPath);
	}

	static public XulManager build(InputStream xul, String xmlPath) throws Exception {
		return XulFactory.build(XulManager.class, xul, xmlPath);
	}

	static public XulManager build(byte[] xml) throws Exception {
		return XulFactory.build(XulManager.class, xml, "");
	}

	static public XulManager build(InputStream xul) throws Exception {
		return XulFactory.build(XulManager.class, xul, "");
	}

	public static boolean clear() {
		if (_instance != null) {
			synchronized (_instance) {
				_instance._pages.clear();
				_instance._components.clear();
				_instance._selectors.clear();
				_instance._globalBinding.clear();
			}
		}
		return true;
	}

	public static boolean loadXul(String xul, String path) {
		XulUtils.ticketMarker marker = new XulUtils.ticketMarker("BENCH!! ", true);
		marker.mark();
		try {
			build(xul.getBytes("utf-8"), path);
			_updateSelectors();
			marker.mark("loadXul");
			Log.d(TAG, marker.toString());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean loadXul(InputStream xul, String path) {
		XulUtils.ticketMarker marker = new XulUtils.ticketMarker("BENCH!! ", true);
		marker.mark();
		try {
			build(xul, path);
			_updateSelectors();
			marker.mark("loadXul");
			Log.d(TAG, marker.toString());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private static void _updateSelectors() {
		for (int i = 0; i < _selectors.size(); ++i) {
			XulSelect selector = _selectors.get(i);
			selector.setPriorityLevel(i + 1, 0);
		}

		for (XulPage page : _pages.values()) {
			page.updateSelectorPriorityLevel();
		}
	}

	public static void initPage(String pageId) {
		XulPage xulPage = _pages.get(pageId);
		if (xulPage != null) {
			xulPage.initPage();
		}
	}

	public static XulRenderContext createXulRender(String pageId, XulRenderContext.IXulRenderHandler handler) {
		return createXulRender(pageId, handler, 0, 0);
	}

	public static XulRenderContext createXulRender(String pageId, XulRenderContext.IXulRenderHandler handler, int pageWidth, int pageHeight) {
		return createXulRender(pageId, handler, pageWidth, pageHeight, false, false);
	}

	public static XulRenderContext createXulRender(String pageId, XulRenderContext.IXulRenderHandler handler, int pageWidth, int pageHeight, boolean suspend) {
		return createXulRender(pageId, handler, pageWidth, pageHeight, suspend, false);
	}

	public static XulRenderContext createXulRender(String pageId, XulRenderContext.IXulRenderHandler handler, int pageWidth, int pageHeight, boolean suspend, boolean noGlobalBinding) {
		return createXulRender(pageId, handler, pageWidth, pageHeight, suspend, noGlobalBinding, false);
	}

	public static XulRenderContext createXulRender(String pageId, XulRenderContext.IXulRenderHandler handler, int pageWidth, int pageHeight, boolean suspend, boolean noGlobalBinding, boolean doNotInit) {
		XulUtils.ticketMarker ticketMarker = new XulUtils.ticketMarker("BENCH!! createXulRender(" + pageId + ") ", true);
		ticketMarker.mark();
		XulRenderContext xulRenderContext = null;
		XulPage xulPage = _pages.get(pageId);
		if (xulPage != null) {
			xulRenderContext = new XulRenderContext(xulPage, _selectors, _globalBinding, handler, pageWidth, pageHeight, suspend, noGlobalBinding, doNotInit);
		}
		ticketMarker.mark("createXulRender");
		Log.d(TAG, ticketMarker.toString());
		return xulRenderContext;
	}

	public static ArrayList<XulDataNode> queryGlobalBinding(String selector) {
		ArrayList<XulDataNode> dataSet = new ArrayList<XulDataNode>();
		return XulBindingSelector.selectData(new XulBindingSelector.IXulDataSelectContext() {
			@Override
			public boolean isEmpty() {
				return _globalBinding == null || _globalBinding.isEmpty();
			}

			@Override
			public XulBinding getDefaultBinding() {
				return null;
			}

			@Override
			public XulBinding getBindingById(String id) {
				for (int i = 0, globalBindingSize = _globalBinding.size(); i < globalBindingSize; i++) {
					XulBinding xulBinding = _globalBinding.get(i);
					if (id.equals(xulBinding.getId())) {
						return xulBinding;
					}
				}
				return null;
			}
		}, selector, dataSet);
	}

	public static String queryGlobalBindingString(String selector) {
		ArrayList<XulDataNode> dataSet = queryGlobalBinding(selector);
		if (dataSet == null || dataSet.isEmpty()) {
			return null;
		}
		return dataSet.get(0).getValue();
	}
}
