package com.starcor.xulapp.debug;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import com.starcor.xul.XulRenderContext;
import com.starcor.xulapp.XulApplication;
import com.starcor.xulapp.XulPresenter;
import com.starcor.xulapp.utils.XulLog;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by hy on 2015/12/4.
 */
public class XulDebugAdapter {
	private static final String TAG = XulDebugAdapter.class.getSimpleName();

	public static class DebugAdapter {
		private static Map<Integer, String> _intentFlags;

		public static String parseIntentFlags(int flags) {
			if (_intentFlags == null) {
				_intentFlags = new TreeMap<Integer, String>();
				Class<Intent> intentClass = Intent.class;
				Field[] fields = intentClass.getFields();
				for (Field field : fields) {
					int modifiers = field.getModifiers();
					String flagName = field.getName();
					if (Modifier.isStatic(modifiers) &
						field.getType().getName().equals("int") &
						flagName.startsWith("FLAG_")
						) {
						try {
							_intentFlags.put(field.getInt(null), flagName.substring(5));
						} catch (IllegalAccessException e) {
							XulLog.e(TAG, e);
						}
					}
				}
			}

			StringBuilder result = new StringBuilder();
			for (Map.Entry<Integer, String> entry : _intentFlags.entrySet()) {
				int key = entry.getKey();
				if ((key & flags) == key) {
					flags &= ~key;
					if (result.length() > 0) {
						result.append("|");
					}
					result.append(entry.getValue());
				}
			}
			if (flags != 0) {
				if (result.length() > 0) {
					result.append("|");
				}
				result.append(Integer.toHexString(flags));
			}
			return result.toString();
		}

		public void postToMainLooper(Runnable runnable) {
			XulApplication.getAppInstance().postToMainLooper(runnable);
		}

		public Context getAppContext() {
			return XulApplication.getAppContext();
		}

		public String getPackageName() {
			return XulApplication.getAppInstance().getPackageName();
		}

		public void startActivity(Intent intent) {
			XulApplication.getAppContext().startActivity(intent);
		}

		public void writePageSpecifiedAttribute(Object pageObj, XmlSerializer xmlWriter) throws IOException {
			XulPresenter presenter = (XulPresenter) pageObj;
			final String pageId = presenter.xulGetCurPageId();
			if (!TextUtils.isEmpty(pageId)) {
				xmlWriter.attribute(null, "pageId", pageId);
			}
			String layoutFile = presenter.xulGetCurLayoutFile();
			if (!TextUtils.isEmpty(layoutFile)) {
				xmlWriter.attribute(null, "layoutFile", layoutFile);
			}
			String behaviorName = presenter.xulGetCurBehaviorName();
			if (!TextUtils.isEmpty(behaviorName)) {
				xmlWriter.attribute(null, "behavior", behaviorName);
			}

			String pageClass = presenter.getClass().getSimpleName();
			if (!TextUtils.isEmpty(pageClass)) {
				xmlWriter.attribute(null, "pageClass", pageClass);
			}

			Activity activity = null;
			if (presenter instanceof Activity) {
				activity = (Activity) presenter;
			}
			if (presenter instanceof Dialog) {
				activity = ((Dialog) presenter).getOwnerActivity();
			}

			if (activity != null) {
				int taskId = activity.getTaskId();
				xmlWriter.attribute(null, "taskId", String.valueOf(taskId));

				int flags = activity.getIntent().getFlags();
				String intentFlags = parseIntentFlags(flags);
				xmlWriter.attribute(null, "intentFlags", intentFlags);
			}
		}

		public boolean isPageFinished(Object pageObj) {
			XulPresenter presenter = (XulPresenter) pageObj;
			return !presenter.xulIsAlive();
		}

		public void dispatchKeyEvent(Object pageObj, KeyEvent keyEvent) {
			if (pageObj instanceof Activity) {
				Activity activity = (Activity)pageObj;
				activity.dispatchKeyEvent(keyEvent);
			} else if (pageObj instanceof Dialog) {
				Dialog dialog = (Dialog) pageObj;
				dialog.dispatchKeyEvent(keyEvent);
			}
		}

		public XulRenderContext getPageRenderContext(Object pageObj) {
			XulPresenter presenter = (XulPresenter) pageObj;
			return presenter.xulGetRenderContext();
		}

		public void drawPage(Object pageObj, Canvas canvas) {
			XulPresenter presenter = (XulPresenter) pageObj;
			View rootView = presenter.xulGetRenderContextView();
			rootView.draw(canvas);
		}

		public void finishPage(Object pageObj) {
			XulPresenter presenter = (XulPresenter) pageObj;
			presenter.xulDestroy();
		}

		public String getPageId(Object pageObj) {
			XulPresenter presenter = (XulPresenter) pageObj;
			return presenter.xulGetCurPageId();
		}
	}

	private static DebugAdapter _adapter = new DebugAdapter();

	public static void setAdapter(DebugAdapter adapter) {
		_adapter = adapter;
	}

	public static void postToMainLooper(Runnable runnable) {
		_adapter.postToMainLooper(runnable);
	}

	public static Context getAppContext() {
		return _adapter.getAppContext();
	}

	public static String getPackageName() {
		return _adapter.getPackageName();
	}

	public static void startActivity(Intent intent) {
		_adapter.startActivity(intent);
	}

	public static void writePageSpecifiedAttribute(Object pageObj, XmlSerializer xmlWriter) throws IOException {
		_adapter.writePageSpecifiedAttribute(pageObj, xmlWriter);
	}

	public static boolean isPageFinished(Object pageObj) {
		return _adapter.isPageFinished(pageObj);
	}

	public static void dispatchKeyEvent(Object pageObj, KeyEvent keyEvent) {
		_adapter.dispatchKeyEvent(pageObj, keyEvent);
	}

	public static XulRenderContext getPageRenderContext(Object pageObj) {
		return _adapter.getPageRenderContext(pageObj);
	}

	public static void drawPage(Object pageObj, Canvas canvas) {
		_adapter.drawPage(pageObj, canvas);
	}

	public static void finishPage(Object pageObj) {
		_adapter.finishPage(pageObj);
	}

	public static String getPageId(Object pageObj) {
		return _adapter.getPageId(pageObj);
	}
}
