package com.starcor.xul.Utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.starcor.xul.Prop.XulBinding;
import com.starcor.xul.XulDataNode;
import com.starcor.xul.XulUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hy on 2014/7/2.
 */
@TargetApi(Build.VERSION_CODES.ECLAIR)
public class XulBindingSelector {
	static final Pattern attrFilterPat = Pattern.compile("^\\[([-_\\d\\w]+)=(.+)\\]$");
	private static final String TAG = XulBindingSelector.class.getSimpleName();

	public interface IXulDataSelectContext {
		boolean isEmpty();

		XulBinding getDefaultBinding();

		XulBinding getBindingById(String id);
	}

	private static abstract class xulSelector {
		abstract boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx);
	}

	private static class xulDataSourceSelect extends xulSelector {
		String _dataSource;

		xulDataSourceSelect(String datasource) {
			_dataSource = XulUtils.getCachedString(datasource);
		}

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			ctx.clear();
			XulBinding binding = dataCtx.getBindingById(_dataSource);
			if (binding == null) {
				return true;
			}
			if (!binding.isDataReady() && !binding.isUpdated()) {
				return false;
			}
			XulDataNode dataNode = binding.getData();
			if (dataNode != null) {
				ctx.add(dataNode);
			}
			return true;
		}
	}

	private static class xulDefaultDataSourceSelect extends xulSelector {
		xulDefaultDataSourceSelect() {
		}

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			if (!ctx.isEmpty()) {
				// use current context
				return true;
			}
			// use default binding
			XulBinding binding = dataCtx.getDefaultBinding();
			if (binding == null) {
				return true;
			}
			if (!binding.isDataReady() && !binding.isUpdated()) {
				return false;
			}
			XulDataNode dataNode = binding.getData();
			if (dataNode != null) {
				ctx.add(dataNode);
			}
			return true;
		}

		static final xulDefaultDataSourceSelect DEFAULT_INSTANCE = new xulDefaultDataSourceSelect();
	}

	private static class xulChildItemSelect extends xulSelector {
		String _item;

		public xulChildItemSelect(String item) {
			_item = XulUtils.getCachedString(item);
		}

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			int orgSize = ctx.size();
			for (int i = 0; i < orgSize; ++i) {
				XulDataNode dataNode = ctx.get(i);
				if (dataNode == null) {
					continue;
				}
				for (XulDataNode childDataNode = dataNode.getFirstChild(); childDataNode != null; childDataNode = childDataNode.getNext()) {
					if (TextUtils.isEmpty(_item) || _item.equals(childDataNode.getName())) {
						ctx.add(childDataNode);
					}
				}
			}
			for (int i = 0; i < orgSize; ++i) {
				ctx.remove(0);
			}
			return true;
		}
	}

	private static class xulContentSelect extends xulSelector {
		String _content;
		Pattern _pattern;

		public xulContentSelect(String content) {
			_content = content;
			if (content.startsWith("=") && content.endsWith("=")) {
				_pattern = Pattern.compile(content.substring(1, content.length() - 1));
			} else if (content.startsWith("~") && content.endsWith("~")) {
				_pattern = Pattern.compile(content.substring(1, content.length() - 1), Pattern.CASE_INSENSITIVE);
			}
		}

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			if (_pattern == null) {
				for (int i = ctx.size() - 1; i >= 0; i--) {
					if (!_content.equals(ctx.get(i).getValue())) {
						ctx.remove(i);
					}
				}
			} else {
				for (int i = ctx.size() - 1; i >= 0; i--) {
					Matcher matcher = _pattern.matcher(ctx.get(i).getValue());
					if (matcher == null || !matcher.matches()) {
						ctx.remove(i);
					}
				}
			}
			return true;
		}
	}

	private static class xulParentSelect extends xulSelector {

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			for (int i = ctx.size() - 1; i >= 0; i--) {
				XulDataNode parent = ctx.get(i).getParent();
				if (parent != null) {
					ctx.set(i, parent);
				} else {
					ctx.remove(i);
				}
			}
			return true;
		}
	}

	private static class xulNextSiblingSelect extends xulSelector {

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			for (int i = ctx.size() - 1; i >= 0; i--) {
				XulDataNode next = ctx.get(i).getNext();
				if (next != null) {
					ctx.set(i, next);
				} else {
					ctx.remove(i);
				}
			}
			return true;
		}
	}

	private static class xulPrevSiblingSelect extends xulSelector {

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			for (int i = ctx.size() - 1; i >= 0; i--) {
				XulDataNode prev = ctx.get(i).getPrev();
				if (prev != null) {
					ctx.set(i, prev);
				} else {
					ctx.remove(i);
				}
			}
			return true;
		}
	}

	private static class xulPositionSelect extends xulSelector {
		ArrayList<Pair<Integer, Integer>> _ranges = new ArrayList<Pair<Integer, Integer>>();

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			HashSet<Integer> keepPositions = new HashSet<Integer>();

			int orgSize = ctx.size();
			for (int i1 = 0; i1 < _ranges.size(); i1++) {
				Pair<Integer, Integer> range = _ranges.get(i1);
				int begin = range.first;
				int end = range.second;
				if (begin < 0) {
					begin += orgSize;
				}
				if (end < 0) {
					end += orgSize;
				}
				if (end < 0 || begin < 0) {
					// invalid range
					continue;
				}
				if (end > orgSize) {
					end = orgSize;
				}
				for (int i = begin; i <= end; ++i) {
					keepPositions.add(i);
				}
			}
			while (orgSize > 0) {
				--orgSize;
				if (!keepPositions.contains(orgSize)) {
					ctx.remove(orgSize);
				}
			}
			return true;
		}

		public void addRange(int begin, int end) {
			_ranges.add(Pair.create(begin, end));
		}
	}

	private static class xulAttrFilterSelect extends xulSelector {
		String _attr;
		String _value;

		public xulAttrFilterSelect(String attr, String val) {
			_attr = XulUtils.getCachedString(attr);
			_value = XulUtils.getCachedString(val);
		}

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			int orgSize = ctx.size();
			while (orgSize > 0) {
				--orgSize;
				XulDataNode node = ctx.get(orgSize);
				if (_value == null && node.hasAttribute(_attr)) {
				} else if (_value != null && node.hasAttribute(_attr, _value)) {
				} else {
					ctx.remove(orgSize);
				}
			}
			return true;
		}
	}

	private static class xulInverseAttrFilterSelect extends xulSelector {
		String _attr;
		String _value;

		public xulInverseAttrFilterSelect(String attr, String val) {
			_attr = XulUtils.getCachedString(attr);
			_value = XulUtils.getCachedString(val);
		}

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			int orgSize = ctx.size();
			while (orgSize > 0) {
				--orgSize;
				XulDataNode node = ctx.get(orgSize);
				if (_value == null && node.hasAttribute(_attr)) {
					ctx.remove(orgSize);
				} else if (_value != null && node.hasAttribute(_attr, _value)) {
					ctx.remove(orgSize);
				} else {
				}
			}
			return true;
		}
	}

	private static class xulAttrGetterSelect extends xulSelector {
		String _attr;

		xulAttrGetterSelect(String attr) {
			_attr = XulUtils.getCachedString(attr);
		}

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			int orgSize = ctx.size();
			while (orgSize > 0) {
				XulDataNode node = ctx.remove(0);
				XulDataNode attrNode = node.getAttribute(_attr);
				if (attrNode != null) {
					ctx.add(attrNode);
				}
				--orgSize;
			}
			return true;
		}
	}

	private static class xulCombinedSelect extends xulSelector {
		ArrayList<xulSelector> _selectors = new ArrayList<xulSelector>();

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			for (int i = 0; i < _selectors.size(); i++) {
				xulSelector selector = _selectors.get(i);
				if (!selector.apply(dataCtx, ctx)) {
					return false;
				}
			}
			return true;
		}

		public void addSelector(xulSelector selector) {
			_selectors.add(selector);
		}

		public void fixDataSource() {
			if (_selectors.isEmpty()) {
				return;
			}
			xulSelector xulSelector = _selectors.get(0);
			if (xulSelector instanceof xulCombinedSelect) {
				((xulCombinedSelect) xulSelector).fixDataSource();
				return;
			}

			if (xulSelector instanceof xulDataSourceSelect) {
				return;
			}
			_selectors.add(0, xulDefaultDataSourceSelect.DEFAULT_INSTANCE);
		}
	}

	private static class xulAssignSelector extends xulSelector {
		private static class xasPartition {
			String val = XulUtils.STR_EMPTY;

			boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx, XulDataNode node) {
				return true;
			}
		}

		private static class valXASPartition extends xasPartition {
			public valXASPartition(String val) {
				this.val = val;
			}
		}

		private static class subXASPartition extends xasPartition {
			xulSelector _selector;

			public subXASPartition(xulSelector selector) {
				_selector = selector;
			}

			@Override
			boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx, XulDataNode node) {
				val = XulUtils.STR_EMPTY;
				ArrayList<XulDataNode> cloneCtx = (ArrayList<XulDataNode>) ctx.clone();
				if (!_selector.apply(dataCtx, cloneCtx)) {
					return false;
				}

				if (cloneCtx.isEmpty()) {
					return true;
				}

				XulDataNode xulDataNode = cloneCtx.get(0);
				node.setOwnerBinding(xulDataNode.getOwnerBinding());
				this.val = xulDataNode.getValue();
				return true;
			}
		}

		ArrayList<xasPartition> _partitions = new ArrayList<xasPartition>();

		@Override
		boolean apply(IXulDataSelectContext dataCtx, ArrayList<XulDataNode> ctx) {
			if (_partitions.isEmpty()) {
				return false;
			}
			XulDataNode node = XulDataNode.obtainDataNode(XulUtils.STR_EMPTY);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < _partitions.size(); i++) {
				xasPartition partition = _partitions.get(i);
				if (!partition.apply(dataCtx, ctx, node)) {
					return false;
				}
				sb.append(partition.val);
			}
			node.setValue(sb.toString());
			ctx.clear();
			ctx.add(node);
			return true;
		}

		void addValPart(String val) {
			_partitions.add(new valXASPartition(val));
		}

		void addSelectorPart(String selector) {
			_partitions.add(new subXASPartition(_buildSelector(selector)));
		}
	}

	static ArrayList<String> _splitSubParts_result = new ArrayList<String>();

	private static String[] _splitSubParts(String selector) {
		ArrayList<String> result = _splitSubParts_result;
		result.clear();
		int begin = 0;
		int selectorLen = selector.length();
		for (int i = 0; i < selectorLen; ++i) {
			if (begin == i) {
				continue;
			}
			char c = selector.charAt(i);
			if (c == '<') {
				result.add(selector.substring(begin, i));
				begin = i;
				for (; i < selectorLen && c != '>'; ++i) {
					c = selector.charAt(i);
				}
				result.add(selector.substring(begin, i));
				begin = i;
			} else if (c == '[' || c == ':') {
				result.add(selector.substring(begin, i));
				begin = i;
			}
		}
		if (begin < selectorLen) {
			result.add(selector.substring(begin));
		}
		String[] array = new String[result.size()];
		result.toArray(array);
		return array;
	}

	static final XulCachedHashMap<String, xulSelector> _selectorCache = new XulCachedHashMap<String, xulSelector>();
	static XulCachedHashMap<String, xulDataSourceSelect> _dataSourceCache = new XulCachedHashMap<String, xulDataSourceSelect>();
	static XulCachedHashMap<String, xulAttrGetterSelect> _attrGetterCache = new XulCachedHashMap<String, xulAttrGetterSelect>();
	static XulCachedHashMap<String, xulAttrFilterSelect> _attrFilterCache = new XulCachedHashMap<String, xulAttrFilterSelect>();
	static XulCachedHashMap<String, xulChildItemSelect> _childItemCache = new XulCachedHashMap<String, xulChildItemSelect>();

	private static xulSelector _buildSelector(String selector) {
		synchronized (_selectorCache) {
			xulSelector xulSelector = _selectorCache.get(selector);
			if (xulSelector != null) {
				return xulSelector;
			}
		}

		if (selector.startsWith("=")) {
			xulAssignSelector xulAssignSelector = new xulAssignSelector();
			String[] parts = selector.split("(^=|[{}])");
			for (int i = 1, partsLength = parts.length; i < partsLength; i++) {
				String part = parts[i];
				if (part.length() == 0) {
					continue;
				}
				if ((i % 2) == 1) {
					xulAssignSelector.addValPart(part);
				} else {
					xulAssignSelector.addSelectorPart(part);
				}
			}
			synchronized (_selectorCache) {
				_selectorCache.put(selector, xulAssignSelector);
			}
			return xulAssignSelector;
		}
		xulCombinedSelect xulCombinedSelect = new xulCombinedSelect();

		String[] parts = selector.split("/");
		for (int i = 0; i < parts.length; i++) {
			String partSelector = parts[i];
			if (TextUtils.isEmpty(partSelector)) {
				// ROOT selector
				continue;
			}
			if ("*".equals(partSelector)) {
				xulCombinedSelect.addSelector(new xulChildItemSelect(""));
				continue;
			}
			String[] subParts = _splitSubParts(partSelector);
			xulCombinedSelect combinedSelector;
			if (subParts.length > 1) {
				combinedSelector = new xulCombinedSelect();
				xulCombinedSelect.addSelector(combinedSelector);
			} else {
				combinedSelector = xulCombinedSelect;
			}

			if (i > 0 && subParts.length > 0 && subParts[0].startsWith("[")) {
				// [xxx] 从当前结果集中选择属性
				// xxx/[xxx] 从当前结果集的所有子节点中选择属性
				combinedSelector.addSelector(new xulChildItemSelect(""));
			}

			for (String subPart : subParts) {
				if (subPart.startsWith("#")) {
					String datasource = subPart.substring(1);
					xulDataSourceSelect dataSourceSelect = _dataSourceCache.get(datasource);
					if (dataSourceSelect == null) {
						dataSourceSelect = new xulDataSourceSelect(datasource);
						_dataSourceCache.put(datasource, dataSourceSelect);
					}
					combinedSelector.addSelector(dataSourceSelect);
				} else if (subPart.startsWith(":")) {
					if (subPart.equals(":parent")) {
						combinedSelector.addSelector(new xulParentSelect());
					} else if (subPart.equals(":prev")) {
						combinedSelector.addSelector(new xulPrevSiblingSelect());
					} else if (subPart.equals(":next")) {
						combinedSelector.addSelector(new xulNextSiblingSelect());
					} else {
						Log.d(TAG, "invalid selector info - " + subPart);
					}
				} else if (subPart.startsWith("<") && subPart.endsWith(">")) {
					combinedSelector.addSelector(new xulContentSelect(subPart.substring(1, subPart.length() - 1)));
				} else if (subPart.startsWith("[") && subPart.endsWith("]")) {
					if (subPart.startsWith("[=")) {
						String attrName = subPart.substring(2, subPart.length() - 1);
						xulAttrGetterSelect attrGetterSelect = _attrGetterCache.get(attrName);
						if (attrGetterSelect == null) {
							attrGetterSelect = new xulAttrGetterSelect(attrName);
							_attrGetterCache.put(attrName, attrGetterSelect);
						}
						combinedSelector.addSelector(attrGetterSelect);
					} else {
						Matcher matcher = attrFilterPat.matcher(subPart);
						if (matcher.matches()) {
							String attr = matcher.group(1);
							String val = matcher.group(2);
							String attr_filter = subPart;
							xulAttrFilterSelect attrFilterSelect = _attrFilterCache.get(attr_filter);
							if (attrFilterSelect == null) {
								attrFilterSelect = new xulAttrFilterSelect(attr, val);
								_attrFilterCache.put(attr_filter, attrFilterSelect);
							}
							combinedSelector.addSelector(attrFilterSelect);
						} else {
							xulPositionSelect xulPositionSelect = new xulPositionSelect();
							combinedSelector.addSelector(xulPositionSelect);

							String[] ranges = subPart.substring(1, subPart.length() - 1).split(",");

							for (String range : ranges) {
								if (range.endsWith("~")) {
									range = range + "-1";
								}
								String[] vals = range.split("~");
								if (vals.length == 1) {
									if (!TextUtils.isEmpty(vals[0])) {
										int val = XulUtils.tryParseInt(vals[0]);
										xulPositionSelect.addRange(val, val);
									} else {
										Log.d(TAG, "invalid range info - " + range + " / " + subPart);
									}
								} else {
									if (TextUtils.isEmpty(vals[0])) {
										Log.d(TAG, "invalid range info - " + range + " / " + subPart);
									} else {
										int val1 = XulUtils.tryParseInt(vals[0]);
										int val2 = TextUtils.isEmpty(vals[1]) ? Integer.MAX_VALUE : XulUtils.tryParseInt(vals[1]);
										xulPositionSelect.addRange(val1, val2);
									}
								}
							}

						}
					}
				} else {
					xulChildItemSelect childItemSelect = _childItemCache.get(subPart);
					if (childItemSelect == null) {
						childItemSelect = new xulChildItemSelect(subPart);
						_childItemCache.put(subPart, childItemSelect);
					}
					combinedSelector.addSelector(childItemSelect);
				}
			}
		}

		xulCombinedSelect.fixDataSource();
		synchronized (_selectorCache) {
			_selectorCache.put(selector, xulCombinedSelect);
		}
		return xulCombinedSelect;
	}

	// 返回null表示数据未准备好
	// 返回空数组表示无数据
	public static ArrayList<XulDataNode> selectData(IXulDataSelectContext selectContext, String selector, ArrayList<XulDataNode> dataContext) {
		if (selectContext == null) {
			return null;
		}
		xulSelector xulSelector = _buildSelector(selector);
		ArrayList<XulDataNode> result = new ArrayList<XulDataNode>();

		if (dataContext != null && !dataContext.isEmpty()) {
			result.addAll(dataContext);
		}

		if (xulSelector.apply(selectContext, result)) {
			return result;
		}
		return null;
	}
}
