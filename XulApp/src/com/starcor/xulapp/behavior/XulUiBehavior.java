package com.starcor.xulapp.behavior;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.starcor.xul.IXulExternalView;
import com.starcor.xul.Script.IScriptContext;
import com.starcor.xul.Script.IScriptableObject;
import com.starcor.xul.Script.V8.V8Arguments;
import com.starcor.xul.Script.V8.V8MethodCallback;
import com.starcor.xul.Script.V8.V8ScriptContext;
import com.starcor.xul.Script.V8.V8ScriptObject;
import com.starcor.xul.ScriptWrappr.XulScriptableObjectWrapper;
import com.starcor.xul.Wrapper.XulMassiveAreaWrapper;
import com.starcor.xul.XulDataNode;
import com.starcor.xul.XulLayout;
import com.starcor.xul.XulManager;
import com.starcor.xul.XulPage;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulView;
import com.starcor.xul.XulWorker;
import com.starcor.xulapp.XulPresenter;
import com.starcor.xulapp.message.XulMessageCenter;
import com.starcor.xulapp.model.XulDataService;
import com.starcor.xulapp.model.XulPullDataCollection;
import com.starcor.xulapp.utils.XulMassiveHelper;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by hy on 2015/8/31.
 */
public class XulUiBehavior extends XulScriptableObjectWrapper<XulUiBehavior> implements XulBehavior {
	protected final XulPresenter _presenter;
	protected XulRenderContext _xulRenderContext;
	private IScriptableObject _scriptableObject;
	private XulDataService _dataService;
	private ArrayList<XulActionFilter> _xulActionFilters = new ArrayList<XulActionFilter>();

	public XulUiBehavior(XulPresenter xulPresenter) {
		super(null);
		this._presenter = xulPresenter;
	}

	@Deprecated
	public static void xulAppendData(XulDataNode data, XulMassiveAreaWrapper massiveAreaWrapper) {
		XulMassiveHelper.appendData(data, massiveAreaWrapper);
	}

	@Deprecated
	public static void xulUpdateData(XulDataNode data, int index, XulMassiveAreaWrapper massiveAreaWrapper) {
		XulMassiveHelper.updateData(data, index, massiveAreaWrapper);
	}

	@Deprecated
	public static void xulUpdateData(XulDataNode data, int offset, int index, XulMassiveAreaWrapper massiveAreaWrapper) {
		XulMassiveHelper.updateData(data, offset, index, massiveAreaWrapper);
	}

	public void initBehavior() {
		_xulRenderContext = _presenter.xulGetRenderContext();
		if (_xulRenderContext != null) {
			XulPage page = _xulRenderContext.getPage();
			if (page != null) {
				final IScriptableObject scriptableObject = page.getScriptableObject(V8ScriptContext.DEFAULT_SCRIPT_TYPE);
				V8ScriptObject v8Object = (V8ScriptObject) scriptableObject;
				v8Object.addPropertyGetter("xulBehavior", new V8MethodCallback() {
					@Override
					public boolean get(V8ScriptObject thisObject, V8Arguments args) {
						args.setResult(getScriptableBehaviorObject());
						return true;
					}
				});
			}
		}
		XulMessageCenter.getDefault().register(this);
	}

	public View initRenderContextView(View renderContextView) {
		return renderContextView;
	}

	@Override
	protected XulUiBehavior initUnwrappedObject(XulUiBehavior item) {
		return this;
	}

	protected IScriptableObject getScriptableBehaviorObject() {
		if (_scriptableObject != null) {
			return _scriptableObject;
		}
		final IScriptContext scriptContext = XulManager.getScriptContext(V8ScriptContext.DEFAULT_SCRIPT_TYPE);
		_scriptableObject = scriptContext.createScriptObject(this);
		return _scriptableObject;
	}

	public void xulOnDestroy() {
		cleanDataService();
		XulMessageCenter.getDefault().unregister(this);
	}

	public void xulOnResume() {
	}

	public void xulOnPause() {
	}

	public void xulOnRestart() {
	}

	public void xulOnStart() {
	}

	public void xulOnStop() {
	}

	public boolean xulOnBackPressed() {
		return false;
	}

	public void xulOnNewIntent(Intent intent) {

	}

	public void xulOnSaveInstanceState(Bundle outState) {
	}

	public void xulOnRestoreInstanceState(Bundle savedInstanceState) {
	}

	public XulDataService xulGetDataService() {
		if (_dataService == null) {
			_dataService = XulDataService.obtainDataService();
			_dataService.setUserData(this);
		}
		return _dataService;
	}

	private void cleanDataService() {
		XulDataService dataService = _dataService;
		if (dataService != null) {
			_dataService = null;
			dataService.cancelClause();
		}
	}

	public boolean xulDefaultDispatchKeyEvent(KeyEvent event) {
		return _presenter.xulDefaultDispatchKeyEvent(event);
	}

	public boolean xulDefaultDispatchTouchEvent(MotionEvent event) {
		return _presenter.xulDefaultDispatchTouchEvent(event);
	}

	public boolean xulOnDispatchTouchEvent(MotionEvent event) {
		if (_xulRenderContext != null && _xulRenderContext.onMotionEvent(event)) {
			return true;
		}

		return false;
	}

	public boolean xulOnDispatchKeyEvent(KeyEvent event) {
		XulRenderContext xulRenderContext = _xulRenderContext;
		if (xulRenderContext != null) {
			XulLayout layout = xulRenderContext.getLayout();
			XulView oldFocus = layout != null ? layout.getFocus() : null;
			if (xulRenderContext.onKeyEvent(event)) {
				XulView newFocus = layout != null ? layout.getFocus() : null;
				if (oldFocus != newFocus) {
					xulOnFocusChanged(oldFocus, newFocus);
				}
				return true;
			}
		}
		return false;
	}

	protected void xulOnFocusChanged(XulView oldFocus, XulView newFocus) {}

	public void xulLoadLayoutFile(String layoutFile) {
		_presenter.xulLoadLayoutFile(layoutFile);
	}

	public String xulGetIntentPageId() {
		return _presenter.xulGetIntentPageId();
	}

	public String xulGetCurPageId() {
		return _presenter.xulGetCurPageId();
	}

	public Context getContext() {
		return _presenter.xulGetContext();
	}

	public XulRenderContext xulGetRenderContext() {
		return _xulRenderContext;
	}

	public XulView xulGetFocus() {
		if (_xulRenderContext == null) {
			return null;
		}
		XulLayout layout = _xulRenderContext.getLayout();
		return layout.getFocus();
	}

	public FrameLayout xulGetRenderContextView() {
		return _presenter.xulGetRenderContextView();
	}

	public Bundle xulGetBehaviorParams() {
		return _presenter.xulGetBehaviorParams();
	}

	public void xulFinishActivity() {
		_presenter.xulDestroy();
	}

	public IXulExternalView xulCreateExternalView(String cls, int x, int y, int width, int height, XulView view) {
		return null;
	}

	public String xulResolvePath(XulWorker.DownloadItem item, String path) {
		return null;
	}

	public InputStream xulGetAssets(XulWorker.DownloadItem item, String path) {
		return null;
	}

	public InputStream xulGetAppData(XulWorker.DownloadItem item, String path) {
		return null;
	}

	public InputStream xulGetSdcardData(XulWorker.DownloadItem item, String path) {
		return null;
	}

	public void xulOnRenderIsReady() {
	}

	public void xulOnRenderEvent(int eventId, int param1, int param2, Object msg) {
	}

	public void xulDoAction(XulView view, String action, String type, String command, Object userdata) {
		if (!_xulActionFilters.isEmpty()) {
			for (XulActionFilter actionFilter : _xulActionFilters) {
				if (actionFilter.accept(view, action, type, command, userdata)) {
					return;
				}
			}
		}
	}

	public void xulOnRenderCreated() {
		initBehavior();
	}

	public XulCancelable xulSyncMassiveData(final XulView massiveView, final XulPullDataCollection pullDS, XulDataNode data) {
		return xulSyncMassiveData(massiveView, pullDS, data, 16);
	}

	public XulCancelable xulSyncMassiveData(final XulView massiveView, final XulPullDataCollection pullDS, XulDataNode data, final int prefetchSize) {
		return xulSyncMassiveData(null, massiveView, pullDS, data, prefetchSize);
	}

	public XulCancelable xulSyncMassiveData(XulDataNodeHelper helper, XulView massiveView, XulPullDataCollection pullDS, XulDataNode data, int prefetchSize) {
		final com.starcor.xulapp.utils.XulCancelable cancelable = XulMassiveHelper.syncContent(this, helper, massiveView, pullDS, data, prefetchSize);
		return new XulCancelable() {
			@Override
			public void cancel() {
				cancelable.cancel();
			}
		};
	}

	public void xulDelActionFilter(XulActionFilter actionFilter) {
		_xulActionFilters.remove(actionFilter);
	}

	public void xulAddActionFilter(XulActionFilter actionFilter) {
		if (_xulActionFilters.contains(actionFilter)) {
			return;
		}
		_xulActionFilters.add(actionFilter);
	}

	@Override
	public int getBehaviorType() {
		return BEHAVIOR_TYPE_UI;
	}

	public interface XulActionFilter {
		boolean accept(XulView view, String action, String type, String command, Object userdata);
	}

	@Deprecated
	public interface XulCancelable extends com.starcor.xulapp.utils.XulCancelable {}

	@Deprecated
	public interface XulDataNodeHelper extends com.starcor.xulapp.utils.XulMassiveHelper.XulDataNodeHelper {}
}
