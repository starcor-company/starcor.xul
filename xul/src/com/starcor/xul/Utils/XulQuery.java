package com.starcor.xul.Utils;

import android.text.TextUtils;

import com.starcor.xul.XulDataNode;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * execute a query on XulDataNode
 * <pre>
 * selector syntax:
 * :parent             retrieve parent
 * :next               retrieve next siblings
 * :prev               retrieve previous siblings
 * [ATTR]              filter node with ATTR
 * [!ATTR]             filter node without ATTR
 * [ATTR=VALUE]        filter node with ATTR=VALUE
 * [ATTR!=VALUE]       filter node with ATTR!=VALUE
 * [=ATTR]             retrieve node attribute ATTR
 * TAG_NAME            find child with TAG_NAME
 * </pre>
 */
public class XulQuery {
	private static ConcurrentHashMap<String, selectAction> _selectActionCache = new ConcurrentHashMap<String, selectAction>();

	public static XulDataNode select(XulDataNode node, String... selectors) {
		if (selectors == null || node == null) {
			return null;
		}
		int length = selectors.length;
		if (length <= 0) {
			return null;
		}
		selectAction[] selectActions = buildSelectActions(selectors);
		XulDataNode[] selectStack = new XulDataNode[length + 1];
		selectStack[0] = node;
		execSelect(0, length, selectActions, selectStack);
		return selectStack[length];
	}

	public static String selectValue(XulDataNode node, String... selectors) {
		XulDataNode result = select(node, selectors);
		if (result == null) {
			return null;
		}
		return result.getValue();
	}

	private static int execSelect(int startLevel, int maxLevel, selectAction[] selectActions, XulDataNode[] selectStack) {
		XulDataNode node;
		int i = startLevel;
		while (i < maxLevel) {
			selectAction curAction = selectActions[i];
			if (selectStack[i + 1] == null) {
				node = selectStack[i + 1] = curAction.selectFirst(selectStack[i]);
			} else {
				node = selectStack[i + 1] = curAction.selectNext(selectStack[i + 1]);
			}
			if (node == null) {
				--i;
				if (i < 0) {
					break;
				}
			} else {
				++i;
			}
		}
		return i;
	}

	public static XulQueryContext compile(String... selectors) {
		if (selectors == null) {
			return null;
		}
		int length = selectors.length;
		if (length <= 0) {
			return null;
		}
		XulQueryContext ctx = new XulQueryContext(selectors);
		return ctx;
	}

	private static selectAction[] buildSelectActions(String[] selectors) {
		selectAction[] selectActions = new selectAction[selectors.length];
		for (int i = 0, selectorsLength = selectors.length; i < selectorsLength; i++) {
			String selector = selectors[i];
			if (selector.startsWith(":")) {
				if (":parent".equals(selector)) {
					selectActions[i] = saParent.instance;
				} else if (":prev".equals(selector)) {
					selectActions[i] = saPrev.instance;
				} else if (":next".equals(selector)) {
					selectActions[i] = saNext.instance;
				} else {
					selectActions[i] = saNull.instance;
				}
				continue;
			}
			selectAction cachedAction = _selectActionCache.get(selector);
			if (cachedAction != null) {
				selectActions[i] = cachedAction;
				continue;
			}
			if (selector.startsWith("[") && selector.endsWith("]")) {
				int splitPos = selector.indexOf('=');
				if (splitPos <= 0) {
					if (selector.startsWith("[!")) {
						String key = selector.substring(2, selector.length() - 1);
						selectActions[i] = new saNoAttr(key);
					} else {
						String key = selector.substring(1, selector.length() - 1);
						selectActions[i] = new saHasAttr(key);
					}
				} else if (selector.charAt(splitPos - 1) == '!') {
					String key = selector.substring(1, splitPos - 1);
					String val = selector.substring(splitPos + 1, selector.length() - 1);
					selectActions[i] = new saAttrNotEqualsVal(key, val);
				} else {
					String key = selector.substring(1, splitPos);
					String val = selector.substring(splitPos + 1, selector.length() - 1);
					if (TextUtils.isEmpty(key)) {
						selectActions[i] = new saGetAttr(val);
					} else {
						selectActions[i] = new saAttrEqualsVal(key, val);
					}
				}
			} else {
				selectActions[i] = new saFindTag(selector);
			}
			_selectActionCache.put(selector, selectActions[i]);
		}
		return selectActions;
	}

	private static abstract class selectAction {
		abstract XulDataNode selectFirst(XulDataNode node);

		abstract XulDataNode selectNext(XulDataNode node);
	}

	private static class saParent extends selectAction {
		static saParent instance = new saParent();

		private saParent() {
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return node.getParent();
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return node.getParent();
		}
	}

	private static class saPrev extends selectAction {
		static saPrev instance = new saPrev();

		private saPrev() {
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return node.getPrev();
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return node.getPrev();
		}
	}

	private static class saNext extends selectAction {
		static saNext instance = new saNext();

		private saNext() {
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return node.getNext();
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return node.getNext();
		}
	}

	private static class saNull extends selectAction {
		static saNull instance = new saNull();

		private saNull() {
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return null;
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return null;
		}
	}

	private static class saFindTag extends selectAction {
		String _tagName;

		public saFindTag(String tagName) {
			_tagName = tagName;
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return node.getChildNode(_tagName);
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return node.getNext(_tagName);
		}
	}

	private static class saGetAttr extends selectAction {
		String _key;

		public saGetAttr(String key) {
			_key = key;
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return node.getAttribute(_key);
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return null;
		}
	}

	private static class saHasAttr extends selectAction {
		String _key;

		public saHasAttr(String key) {
			_key = key;
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return node.getAttribute(_key) != null ? node : null;
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return null;
		}
	}

	private static class saAttrEqualsVal extends selectAction {
		String _key;
		String _val;

		public saAttrEqualsVal(String key, String val) {
			_key = key;
			_val = val;
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return _val.equals(node.getAttributeValue(_key)) ? node : null;
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return null;
		}
	}

	private static class saNoAttr extends selectAction {
		String _key;

		public saNoAttr(String key) {
			_key = key;
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return node.getAttribute(_key) == null ? node : null;
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return null;
		}
	}

	private static class saAttrNotEqualsVal extends selectAction {
		String _key;
		String _val;

		public saAttrNotEqualsVal(String key, String val) {
			_key = key;
			_val = val;
		}

		@Override
		XulDataNode selectFirst(XulDataNode node) {
			return _val.equals(node.getAttributeValue(_key)) ? null : node;
		}

		@Override
		XulDataNode selectNext(XulDataNode node) {
			return null;
		}
	}

	public static class XulQueryContext {
		selectAction[] _selectActions;
		XulDataNode[] _selectStack;
		int _curLevel;

		XulQueryContext(String[] selectors) {
			_selectActions = buildSelectActions(selectors);
			_selectStack = new XulDataNode[selectors.length + 1];
		}

		public XulDataNode select(XulDataNode node) {
			if (node == null) {
				return null;
			}
			Arrays.fill(_selectStack, null);
			_selectStack[0] = node;
			_curLevel = 0;
			int maxLevel = _selectActions.length;
			_curLevel = execSelect(_curLevel, maxLevel, _selectActions, _selectStack);
			return _selectStack[maxLevel];
		}

		public XulDataNode selectNext() {
			--_curLevel;
			if (_curLevel < 0) {
				return null;
			}
			int maxLevel = _selectActions.length;
			_curLevel = execSelect(_curLevel, maxLevel, _selectActions, _selectStack);
			return _selectStack[maxLevel];
		}

		public String selectValue(XulDataNode node) {
			XulDataNode result = select(node);
			if (result == null) {
				return null;
			}
			return result.getValue();
		}

		public String selectNextValue() {
			XulDataNode result = selectNext();
			if (result == null) {
				return null;
			}
			return result.getValue();
		}
	}
}
