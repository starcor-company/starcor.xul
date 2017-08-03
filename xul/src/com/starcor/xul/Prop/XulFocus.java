package com.starcor.xul.Prop;

import android.text.TextUtils;
import com.starcor.xul.*;
import com.starcor.xul.Factory.XulFactory;
import com.starcor.xul.Factory.XulFactory.ItemBuilder;
import com.starcor.xul.Factory.XulFactory.Attributes;

/**
 * Created by hy on 2014/5/5.
 */
public class XulFocus extends XulProp {
	public static final int MODE_NEARBY = 0x001;
	public static final int MODE_LOOP = 0x002;
	public static final int MODE_NOFOCUS = 0x004;
	public static final int MODE_FOCUSABLE = 0x008;
	public static final int MODE_DYNAMIC = 0x010;
	public static final int MODE_PRIORITY = 0x020;
	public static final int MODE_WRAP = 0x040;
	public static final int MODE_EXT_NEARBY = 0x080;

	public static final int MODE_SEARCH_ORDER_MASK = 0xF000;
	public static final int MODE_SEARCH_ORDER_DPN = 0x0000; // dynamic, priority, nearby
	public static final int MODE_SEARCH_ORDER_DNP = 0x1000; // dynamic, nearby, priority
	public static final int MODE_SEARCH_ORDER_NDP = 0x2000; // nearby, dynamic, priority
	public static final int MODE_SEARCH_ORDER_NPD = 0x3000; // nearby, priority, dynamic
	public static final int MODE_SEARCH_ORDER_PDN = 0x4000; // priority, dynamic, nearby
	public static final int MODE_SEARCH_ORDER_PND = 0x5000; // priority, nearby, dynamic

	int _mode = MODE_NEARBY|MODE_PRIORITY;
	boolean _defaultFocused = false;
	int _focusPriority = -1;    // 焦点优先级，焦点移动时先选择高优先级元素

	public XulFocus makeClone() {
		return this;
	}

	public void bindNextFocus(String direction, XulSelect select) {
	}

	public int getFocusOrder() {
		return _mode & MODE_SEARCH_ORDER_MASK;
	}

	public boolean hasModeBits(int mode) {
		return (_mode & mode) != 0;
	}

	public boolean focusable() {
		return MODE_FOCUSABLE == (_mode & MODE_FOCUSABLE);
	}

	public boolean isDefaultFocused() {
		return _defaultFocused;
	}

	public int getFocusPriority() {
		return _focusPriority;
	}

	public int getFocusMode() {
		return _mode;
	}

	public static class _Builder extends ItemBuilder {
		XulFocus _focus;
		FinalCallback<XulFocus> _callback;

		private void init(final XulSelect select) {
			_focus = new XulFocus();
			_callback = new FinalCallback<XulFocus>() {
				@Override
				public void onFinal(XulFocus prop) {
					select.addProp(prop);
				}
			};
		}

		private void init(final XulView view) {
			_focus = new XulFocus();
			_callback = new FinalCallback<XulFocus>() {
				@Override
				public void onFinal(XulFocus prop) {
					view.addInplaceProp(prop);
				}
			};
		}

		@Override
		public boolean initialize(String name, Attributes attrs) {
			_focus._nameId = XulPropNameCache.name2Id("focus");
			String mode = attrs.getValue("mode");
			if (!TextUtils.isEmpty(mode)) {
				String[] modes = mode.split(",");
				int orderSeq = 0;
				int newMode = 0;
				for (int i = 0; i < modes.length; i++) {
					String m = modes[i];
					if ("nearby".equals(m)) {
						newMode |= MODE_NEARBY;
						orderSeq = (orderSeq * 0x10)|0x1;
					} else if ("~nearby".equals(m)) {
						newMode &= ~MODE_NEARBY;
					} else if ("dynamic".equals(m)) {
						newMode |= MODE_DYNAMIC;
						orderSeq = (orderSeq * 0x10)|0x2;
					} else if ("~dynamic".equals(m)) {
						newMode &= ~MODE_DYNAMIC;
					} else if ("priority".equals(m)) {
						newMode |= MODE_PRIORITY;
						orderSeq = (orderSeq * 0x10)|0x3;
					} else if ("~priority".equals(m)) {
						newMode &= ~MODE_PRIORITY;
					} else if ("loop".equals(m)) {
						newMode |= MODE_LOOP;
					} else if ("wrap".equals(m)) {
						newMode |= MODE_WRAP;
					} else if ("nofocus".equals(m)) {
						newMode |= MODE_NOFOCUS;
						newMode &= ~MODE_FOCUSABLE;
					} else if ("focusable".equals(m)) {
						newMode |= MODE_FOCUSABLE;
						newMode &= ~MODE_NOFOCUS;
					} else if ("extend-nearby".equals(m)) {
						newMode |= MODE_EXT_NEARBY;
					}
				}
				switch (orderSeq&0xFFF) {
				case 0x001:
				case 0x012:
				case 0x123:
					newMode |= MODE_SEARCH_ORDER_NDP;
					break;
				case 0x013:
				case 0x132:
					newMode |= MODE_SEARCH_ORDER_NPD;
					break;
				case 0x021:
				case 0x213:
					newMode |= MODE_SEARCH_ORDER_DNP;
					break;
				case 0x002:
				case 0x023:
				case 0x231:
					newMode |= MODE_SEARCH_ORDER_DPN;
					break;
				case 0x031:
				case 0x312:
					newMode |= MODE_SEARCH_ORDER_PND;
					break;
				case 0x003:
				case 0x032:
				case 0x321:
					newMode |= MODE_SEARCH_ORDER_PDN;
					break;
				default:
					newMode |= MODE_SEARCH_ORDER_DPN;
					break;
				}
				_focus._mode = newMode;
			}
			String focused = attrs.getValue("focused");
			if ("true".equals(focused)) {
				_focus._defaultFocused = true;
			}

			String priority = attrs.getValue("priority");
			if (!TextUtils.isEmpty(priority) && TextUtils.isDigitsOnly(priority)) {
				_focus._focusPriority = XulUtils.tryParseInt(priority, -1);
			} else {
				_focus._focusPriority = -1;
			}
			return true;
		}

		@Override
		public ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, Attributes attrs) {
			return XulManager.CommonDummyBuilder;
		}

		@Override
		public Object finalItem() {
			XulFocus focus = _focus;
			FinalCallback<XulFocus> callback = _callback;
			_Builder.recycle(this);
			callback.onFinal(focus);
			return focus;
		}

		static public _Builder create(XulSelect select) {
			_Builder builder = create();
			builder.init(select);
			return builder;

		}

		public static _Builder create(XulView view) {
			_Builder builder = create();
			builder.init(view);
			return builder;
		}

		private static _Builder create() {
			_Builder builder = _recycled_builder;
			if (builder == null) {
				builder = new _Builder();
			} else {
				_recycled_builder = null;
			}
			return builder;
		}

		private static _Builder _recycled_builder;
		private static void recycle(_Builder builder) {
			_recycled_builder = builder;
			_recycled_builder._callback = null;
			_recycled_builder._focus = null;
		}
	}
}
