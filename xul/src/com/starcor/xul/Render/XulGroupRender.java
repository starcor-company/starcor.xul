package com.starcor.xul.Render;

import android.text.TextUtils;
import android.util.Log;
import com.starcor.xul.Events.XulActionEvent;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Utils.XulAreaChildrenCollectorAllFocusable;
import com.starcor.xul.Utils.XulAreaChildrenCollectorByClass;
import com.starcor.xul.Utils.XulArrayList;
import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.*;

import java.util.ArrayList;

/**
 * Created by hy on 2014/5/31.
 */
public class XulGroupRender extends XulAreaRender {
	public static final String DEFAULT_GROUP_CHECKED_CLASS = "group-checked";
	private static final String TAG = XulGroupRender.class.getSimpleName();

	enum GroupMode {
		GM_RADIO,
		GM_CHECK
	}

	class GroupInfo {
		GroupMode mode;
		String groupClass;
		String checkClass;
	}

	GroupMode _defaultGroupMode = GroupMode.GM_CHECK;
	String _defaultCheckStateClass;
	XulCachedHashMap<String, GroupInfo> _groupClsMap;

	public static void register() {
		XulRenderFactory.registerBuilder("area", "group", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				return new XulGroupRender(ctx, (XulArea) view);
			}
		});

		XulRenderFactory.registerBuilder("area", "radio", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				XulGroupRender xulGroupRender = new XulGroupRender(ctx, (XulArea) view);
				xulGroupRender._defaultGroupMode = GroupMode.GM_RADIO;
				return xulGroupRender;
			}
		});

		XulRenderFactory.registerBuilder("area", "check", new XulRenderFactory.RenderBuilder() {
			@Override
			protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
				assert view instanceof XulArea;
				XulGroupRender xulGroupRender = new XulGroupRender(ctx, (XulArea) view);
				xulGroupRender._defaultGroupMode = GroupMode.GM_CHECK;
				return xulGroupRender;
			}
		});
	}

	public XulGroupRender(XulRenderContext ctx, XulArea area) {
		super(ctx, area);
	}

	private void addGroup(String modeName, String groupClass, String checkClass) {
		if (_groupClsMap == null) {
			_groupClsMap = new XulCachedHashMap<String, GroupInfo>();
		}

		GroupMode mode = GroupMode.GM_CHECK;
		if ("check".equals(modeName)) {
			mode = GroupMode.GM_CHECK;
		} else if ("radio".equals(modeName)) {
			mode = GroupMode.GM_RADIO;
		} else {
			Log.w(TAG, "unsupported group mode! " + modeName);
		}
		GroupInfo info = new GroupInfo();
		info.groupClass = groupClass;
		info.mode = mode;
		info.checkClass = checkClass;
		_groupClsMap.put(groupClass, info);
	}

	@Override
	public void syncData() {
		if (!_isDataChanged()) {
			return;
		}
		super.syncData();
		String checkedClass = _area.getAttrString("checked-class");
		if (TextUtils.isEmpty(checkedClass)) {
			_defaultCheckStateClass = DEFAULT_GROUP_CHECKED_CLASS;
		} else {
			_defaultCheckStateClass = checkedClass.trim();
		}

		while (_groupClsMap == null) {
			XulAttr groupAttr = _area.getAttr("group");
			if (groupAttr == null) {
				break;
			}
			try {
				ArrayList<XulDataNode> groupInfo = (ArrayList<XulDataNode>) groupAttr.getValue();
				for (XulDataNode info : groupInfo) {
					if (!"group".equals(info.getName())) {
						continue;
					}
					String value = info.getValue();
					if (TextUtils.isEmpty(value)) {
						continue;
					}
					String[] groupParams = value.split(",");
					if (groupParams.length != 3) {
						continue;
					}
					String groupMode = groupParams[0];
					String groupClass = groupParams[1];
					String checkClass = groupParams[2];
					addGroup(groupMode, groupClass, checkClass);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
	}

	private static XulAreaChildrenCollectorByClass _collectCheckedChildren = new XulAreaChildrenCollectorByClass();

	private static XulAreaChildrenCollectorAllFocusable _focusCollector = new XulAreaChildrenCollectorAllFocusable();

	private GroupInfo getGroupInfoByView(XulView clickedView) {
		GroupInfo info = null;
		if (_groupClsMap == null) {
			info = new GroupInfo();
			info.groupClass = null;
			info.mode = _defaultGroupMode;
			info.checkClass = _defaultCheckStateClass;
		} else {
			for (String groupCls : _groupClsMap.keySet()) {
				if (clickedView.hasClass(groupCls)) {
					info = _groupClsMap.get(groupCls);
					break;
				}
			}
		}
		return info;
	}

	private boolean radioClick(GroupInfo groupInfo, XulView clickedView) {
		if (groupInfo.mode != GroupMode.GM_RADIO) {
			return false;
		}
		String checkClass = groupInfo.checkClass;
		if (TextUtils.isEmpty(checkClass)) {
			checkClass = _defaultCheckStateClass;
		}
		if (groupInfo.groupClass == null) {
			_collectCheckedChildren.begin(checkClass);
		} else {
			_collectCheckedChildren.begin(false, groupInfo.groupClass, checkClass);
		}
		_area.eachChild(_collectCheckedChildren);
		XulArrayList<XulView> results = _collectCheckedChildren.end();
		if (results != null) {
			for (int i = 0, resultSize = results.size(); i < resultSize; i++) {
				XulView view = results.get(i);
				// 选中的为同一view，无需处理
				if (view == clickedView) {
					continue;
				}

				if (view.hasClass(checkClass)) {
					if (view.removeClass(checkClass)) {
						notifyUncheckedEvent(view);
						view.resetRender();
					} else {
						notifyUncheckedEvent(view);
					}
				}

			}
		}
		_collectCheckedChildren.clear();
		if (!clickedView.hasClass(checkClass)) {
			if (clickedView.addClass(checkClass)) {
				notifyCheckedEvent(clickedView);
				clickedView.resetRender();
			} else {
				notifyCheckedEvent(clickedView);
			}
		}
		return true;
	}

	private boolean checkClick(GroupInfo groupInfo, XulView clickedView) {
		if (groupInfo.mode != GroupMode.GM_CHECK) {
			return false;
		}
		String checkClass = groupInfo.checkClass;
		if (TextUtils.isEmpty(checkClass)) {
			checkClass = _defaultCheckStateClass;
		}
		if (clickedView.hasClass(checkClass)) {
			if (clickedView.removeClass(checkClass)) {
				notifyUncheckedEvent(clickedView);
				clickedView.resetRender();
			} else {
				notifyUncheckedEvent(clickedView);
			}
		} else {
			if (clickedView.addClass(checkClass)) {
				notifyCheckedEvent(clickedView);
				clickedView.resetRender();
			} else {
				notifyCheckedEvent(clickedView);
			}
		}
		return true;
	}

	public void setAllUnchecked() {
		if (_groupClsMap == null) {
			_collectCheckedChildren.begin(_defaultCheckStateClass);
			_area.eachChild(_collectCheckedChildren);
			XulArrayList<XulView> result = _collectCheckedChildren.end();
			for (int i = 0; i < result.size(); i++) {
				XulView view = result.get(i);
				if (view.hasClass(_defaultCheckStateClass)) {
					if (view.removeClass(_defaultCheckStateClass)) {
						notifyUncheckedEvent(view);
						view.resetRender();
					} else {
						notifyUncheckedEvent(view);
					}
				}
			}
			_collectCheckedChildren.clear();
		} else {
			for (GroupInfo groupInfo : _groupClsMap.values()) {
				_collectCheckedChildren.begin(false, groupInfo.groupClass, groupInfo.checkClass);
				_area.eachChild(_collectCheckedChildren);
				XulArrayList<XulView> result = _collectCheckedChildren.end();
				for (int i = 0; i < result.size(); i++) {
					XulView view = result.get(i);
					if (view.hasClass(groupInfo.checkClass)) {
						if (view.removeClass(groupInfo.checkClass)) {
							notifyUncheckedEvent(view);
							view.resetRender();
						} else {
							notifyUncheckedEvent(view);
						}
					}
				}
			}
			_collectCheckedChildren.clear();
		}
	}

	public void setAllChecked() {
		if (_groupClsMap == null) {
			_focusCollector.begin();
			_area.eachChild(_focusCollector);
			XulArrayList<XulView> result = _focusCollector.end();
			for (int i = 0; i < result.size(); i++) {
				XulView view = result.get(i);
				if (!view.hasClass(_defaultCheckStateClass)) {
					if (view.addClass(_defaultCheckStateClass)) {
						notifyCheckedEvent(view);
						view.resetRender();
					} else {
						notifyCheckedEvent(view);
					}
				}
				if (_defaultGroupMode == GroupMode.GM_RADIO) {
					break;
				}
			}
			_focusCollector.clear();
		} else {
			for (GroupInfo groupInfo : _groupClsMap.values()) {
				_collectCheckedChildren.begin(false, groupInfo.groupClass);
				_area.eachChild(_collectCheckedChildren);
				XulArrayList<XulView> result = _collectCheckedChildren.end();
				for (int i = 0; i < result.size(); i++) {
					XulView view = result.get(i);
					if (!view.hasClass(groupInfo.checkClass)) {
						if (view.addClass(groupInfo.checkClass)) {
							notifyCheckedEvent(view);
							view.resetRender();
						} else {
							notifyCheckedEvent(view);
						}
					}
					if (groupInfo.mode == GroupMode.GM_RADIO) {
						break;
					}
				}
			}
			_collectCheckedChildren.clear();
		}
	}

	private void notifyUncheckedEvent(XulView view) {
		XulPage.invokeActionNoPopup(view, "unchecked");
	}

	private void notifyCheckedEvent(XulView view) {
		XulPage.invokeActionNoPopup(view, "checked");
	}

	public void setUnchecked(XulView view) {
		GroupInfo groupInfo = getGroupInfoByView(view);
		if (groupInfo == null) {
			return;
		}
		if (!view.hasClass(groupInfo.checkClass)) {
			return;
		}
		switch (groupInfo.mode) {
		case GM_CHECK:
			checkClick(groupInfo, view);
			break;
		case GM_RADIO:
			radioClick(groupInfo, view);
			break;
		}
	}

	public void setChecked(XulView view) {
		GroupInfo groupInfo = getGroupInfoByView(view);
		if (groupInfo == null) {
			return;
		}
		if (view.hasClass(groupInfo.checkClass)) {
			return;
		}
		switch (groupInfo.mode) {
		case GM_CHECK:
			checkClick(groupInfo, view);
			break;
		case GM_RADIO:
			radioClick(groupInfo, view);
			break;
		}
	}

	public boolean isChecked(XulView view) {
		GroupInfo groupInfo = getGroupInfoByView(view);
		if (groupInfo == null) {
			return false;
		}
		return view.hasClass(groupInfo.checkClass);
	}

	public ArrayList<XulView> getAllGroupItems() {
		ArrayList<XulView> allItems = new ArrayList<XulView>();
		if (_groupClsMap == null) {
			_focusCollector.begin();
			_area.eachChild(_focusCollector);
			XulArrayList<XulView> result = _focusCollector.end();
			allItems.addAll(result);
			_focusCollector.clear();
		} else {
			for (GroupInfo groupInfo : _groupClsMap.values()) {
				_collectCheckedChildren.begin(false, groupInfo.groupClass);
				_area.eachChild(_collectCheckedChildren);
				XulArrayList<XulView> result = _collectCheckedChildren.end();
				allItems.addAll(result);
			}
			_collectCheckedChildren.clear();
		}
		return allItems;
	}

	public ArrayList<ArrayList<XulView>> getAllGroups() {
		ArrayList<ArrayList<XulView>> allGroups = new ArrayList<ArrayList<XulView>>();
		if (_groupClsMap == null) {
			_focusCollector.begin();
			_area.eachChild(_focusCollector);
			XulArrayList<XulView> result = _focusCollector.end();
			allGroups.add((ArrayList<XulView>) result.clone());
			_focusCollector.clear();
		} else {
			for (GroupInfo groupInfo : _groupClsMap.values()) {
				_collectCheckedChildren.begin(false, groupInfo.groupClass);
				_area.eachChild(_collectCheckedChildren);
				XulArrayList<XulView> result = _collectCheckedChildren.end();
				result.add((XulView) result.clone());
			}
			_collectCheckedChildren.clear();
		}
		return allGroups;
	}

	public ArrayList<XulView> getGroupByItem(XulView view) {
		if (!_area.hasChild(view)) {
			return null;
		}
		ArrayList<XulView> allItems = null;
		if (_groupClsMap == null) {
			_focusCollector.begin();
			_area.eachChild(_focusCollector);
			XulArrayList<XulView> result = _focusCollector.end();
			allItems = new ArrayList<XulView>();
			allItems.addAll(result);
			_focusCollector.clear();
		} else {
			GroupInfo groupInfo = getGroupInfoByView(view);
			if (groupInfo != null) {
				allItems = new ArrayList<XulView>();
				_collectCheckedChildren.begin(false, groupInfo.groupClass);
				_area.eachChild(_collectCheckedChildren);
				XulArrayList<XulView> result = _collectCheckedChildren.end();
				allItems.addAll(result);
				_collectCheckedChildren.clear();
			}
		}
		return allItems;
	}

	public ArrayList<ArrayList<XulView>> getAllCheckedGroups() {
		ArrayList<ArrayList<XulView>> allGroups = new ArrayList<ArrayList<XulView>>();
		if (_groupClsMap == null) {
			_collectCheckedChildren.begin(_defaultCheckStateClass);
			_area.eachChild(_collectCheckedChildren);
			XulArrayList<XulView> result = _collectCheckedChildren.end();
			allGroups.add((ArrayList<XulView>) result.clone());
			_collectCheckedChildren.clear();
		} else {
			for (GroupInfo groupInfo : _groupClsMap.values()) {
				_collectCheckedChildren.begin(false, groupInfo.groupClass, groupInfo.checkClass);
				_area.eachChild(_collectCheckedChildren);
				XulArrayList<XulView> result = _collectCheckedChildren.end();
				allGroups.add((ArrayList<XulView>) result.clone());
			}
			_collectCheckedChildren.clear();
		}
		return allGroups;
	}

	public ArrayList<XulView> getAllCheckedItems() {
		ArrayList<XulView> allItems = new ArrayList<XulView>();
		if (_groupClsMap == null) {
			_collectCheckedChildren.begin(_defaultCheckStateClass);
			_area.eachChild(_collectCheckedChildren);
			XulArrayList<XulView> result = _collectCheckedChildren.end();
			allItems.addAll(result);
			_collectCheckedChildren.clear();
		} else {
			for (GroupInfo groupInfo : _groupClsMap.values()) {
				_collectCheckedChildren.begin(false, groupInfo.groupClass, groupInfo.checkClass);
				_area.eachChild(_collectCheckedChildren);
				XulArrayList<XulView> result = _collectCheckedChildren.end();
				allItems.addAll(result);
			}
			_collectCheckedChildren.clear();
		}
		return allItems;
	}

	@Override
	public boolean onChildDoActionEvent(XulActionEvent event) {
		if ("click".equals(event.action)) {
			XulView clickedView = event.eventSource;
			GroupInfo groupInfo = getGroupInfoByView(clickedView);
			if (groupInfo != null) {
				switch (groupInfo.mode) {
				case GM_CHECK:
					if (checkClick(groupInfo, clickedView)) {
						event.noPopup = true;
					}
					break;
				case GM_RADIO:
					if (radioClick(groupInfo, clickedView)) {
						event.noPopup = true;
					}
					break;
				}
				return true;
			}
		}
		return false;
	}
}
