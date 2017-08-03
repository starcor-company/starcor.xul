package com.starcor.xul.Utils;

import android.graphics.Rect;

import com.starcor.xul.XulManager;
import com.starcor.xul.XulUtils;

/**
 * Created by hy on 2015/2/5.
 */
public class XulLayoutHelper {
	public static final int MODE_LINEAR_HORIZONTAL = 0x00000001;
	public static final int MODE_LINEAR_VERTICAL = 0x00000002;
	public static final int MODE_GRID_HORIZONTAL = 0x00000004;
	public static final int MODE_GRID_VERTICAL = 0x00000008;
	public static final int MODE_GRID_INVERSE_HORIZONTAL = 0x01000000 | MODE_GRID_HORIZONTAL;
	// public static final int MODE_GRID_INVERSE_VERTICAL = 0x01000000 | MODE_GRID_VERTICAL;
	public static final int MODE_LINEAR_INVERSE_HORIZONTAL = 0x01000000 | MODE_LINEAR_HORIZONTAL;
	public static final int MODE_LINEAR_INVERSE_VERTICAL = 0x01000000 | MODE_LINEAR_VERTICAL;
	static LayoutElementArray _tmpElementArray = new LayoutElementArray(4096);
	private static MatchParentUpdateException matchParentUpdateException = new MatchParentUpdateException();

	public static boolean isVerticalLayoutMode(int mode) {
		return mode == XulLayoutHelper.MODE_GRID_HORIZONTAL
			|| mode == XulLayoutHelper.MODE_LINEAR_VERTICAL
			|| mode == XulLayoutHelper.MODE_LINEAR_INVERSE_VERTICAL
			|| mode == XulLayoutHelper.MODE_GRID_INVERSE_HORIZONTAL;
	}

	public static boolean isGridLayoutMode(int mode) {
		return mode == XulLayoutHelper.MODE_GRID_HORIZONTAL
			|| mode == XulLayoutHelper.MODE_GRID_VERTICAL
			|| mode == XulLayoutHelper.MODE_GRID_INVERSE_HORIZONTAL;
	}

	public static boolean isLinearLayoutMode(int mode) {
		return mode == XulLayoutHelper.MODE_LINEAR_VERTICAL
			|| mode == XulLayoutHelper.MODE_LINEAR_HORIZONTAL
			|| mode == XulLayoutHelper.MODE_LINEAR_INVERSE_VERTICAL
			|| mode == XulLayoutHelper.MODE_LINEAR_INVERSE_HORIZONTAL;
	}

	static boolean _isLayoutRunning = false;
	public static  boolean isLayoutRunning() {
		return _isLayoutRunning;
	}

	public static void updateLayout(ILayoutContainer root, int x, int y, int cx, int cy) {
		if (!root.changed()) {
			return;
		}

		_isLayoutRunning = true;
		/**
		 *  WARNING!!!! ticketMarker was limited by number of marks, DO NOT keep any benchmark code in release version
		 */

		// XulUtils.ticketMarker tm = new XulUtils.ticketMarker("updateLayout ", false);
		// tm.mark();
		_prepareContainer(root);
		// tm.mark("prepare");
		if (!root.isVisible()) {
			_isLayoutRunning = false;
			return;
		}
		while (root.changed()) {
			try {
				_calContainerSizeP1(root);
				// tm.mark("p1");
				_calContainerSizeP2(root, x, y, cx, cy);
				// tm.mark("p2");
				_calContainerSizeP3(root);
				// tm.mark("p3");
			} catch (MatchParentUpdateException e) {
				// tm.mark("p2-UPD");
			}
		}

		_isLayoutRunning = false;
		// Log.d("BENCH!!", tm.toString());
	}

	private static void _prepareElement(ILayoutElement element) {
		element.prepare();
		if (!element.isVisible()) {
			element.doFinal();
		}
	}

	private static void _prepareContainer(ILayoutContainer element) {
		element.prepare();
		if (!element.isVisible()) {
			element.doFinal();
			return;
		}
		int childNum = element.getChildNum();
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getChild(i);
			if (child == null) {
				continue;
			}

			if (!child.changed()) {
				continue;
			}
			if (child instanceof ILayoutContainer) {
				_prepareContainer((ILayoutContainer) child);
			} else {
				_prepareElement(child);
			}
		}
	}

	private static void _calElementSizeP1(ILayoutElement element) {
		Rect padding = element.getPadding();
		switch (element.getWidth()) {
		case XulManager.SIZE_AUTO:
		case XulManager.SIZE_MATCH_CONTENT:
			int newWidth = element.getContentWidth() + padding.left + padding.right;
			element.setWidth(element.constrainWidth(newWidth));
			break;
		}

		switch (element.getHeight()) {
		case XulManager.SIZE_AUTO:
		case XulManager.SIZE_MATCH_CONTENT:
			int newHeight = element.getContentHeight() + padding.top + padding.bottom;
			element.setHeight(element.constrainHeight(newHeight));
			break;
		}
	}

	private static void _calContainerSizeP1(ILayoutContainer element) {
		int childNum = element.getChildNum();
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}
			if (!child.changed()) {
				continue;
			}
			if (child instanceof ILayoutContainer) {
				_calContainerSizeP1((ILayoutContainer) child);
			} else {
				_calElementSizeP1(child);
			}
		}
	}

	private static void _calElementSizeP2(ILayoutElement element, int x, int y, int cx, int cy) {
		element.setBase(x, y);

		switch (element.getWidth()) {
		case XulManager.SIZE_MATCH_PARENT:
			element.setWidth(element.constrainWidth(cx - element.getLeft()));
			break;
		}

		switch (element.getHeight()) {
		case XulManager.SIZE_MATCH_PARENT:
			element.setHeight(element.constrainHeight(cy - element.getTop()));
			break;
		}
	}

	private static void _calContainerSizeP2(ILayoutContainer element, int x, int y, int cx, int cy) throws MatchParentUpdateException {
		_rebase(element, x, y);

		Rect padding = element.getPadding();
		int paddingWidth = padding.left + padding.right;
		int paddingHeight = padding.top + padding.bottom;

		int contentOffsetX = element.getOffsetX();
		int contentOffsetY = element.getOffsetY();

		int childBaseX = x + element.getLeft() + padding.left + contentOffsetX;
		int childBaseY = y + element.getTop() + padding.top + contentOffsetY;

		int width = element.getWidth();
		switch (width) {
		case XulManager.SIZE_AUTO:
		case XulManager.SIZE_MATCH_CONTENT:
			cx = element.constrainWidth(cx - (element.getLeft() + paddingWidth));
			break;
		case XulManager.SIZE_MATCH_PARENT:
			cx = element.constrainWidth(cx - element.getLeft());
			element.setWidth(cx);
			cx -= paddingWidth;
			break;
		default:
			cx = width - paddingWidth;
			break;
		}

		int height = element.getHeight();
		switch (height) {
		case XulManager.SIZE_AUTO:
		case XulManager.SIZE_MATCH_CONTENT:
			cy = element.constrainHeight(cy - (element.getTop() + paddingHeight));
			break;
		case XulManager.SIZE_MATCH_PARENT:
			cy = element.constrainHeight(cy - element.getTop());
			element.setHeight(cy);
			cy -= paddingHeight;
			break;
		default:
			cy = height - paddingHeight;
			break;
		}

		cx -= contentOffsetX;
		cy -= contentOffsetY;

		int layoutMode = element.getLayoutMode();
		switch (layoutMode) {
		case MODE_LINEAR_HORIZONTAL:
			_calChildrenSizeP2_HorizontalLinear(element, cx, cy, childBaseX, childBaseY);
			break;
		case MODE_LINEAR_VERTICAL:
			_calChildrenSizeP2_VerticalLinear(element, cx, cy, childBaseX, childBaseY);
			break;
		case MODE_LINEAR_INVERSE_HORIZONTAL:
			_calChildrenSizeP2_InverseHorizontalLinear(element, cx, cy, childBaseX, childBaseY);
			break;
		case MODE_LINEAR_INVERSE_VERTICAL:
			_calChildrenSizeP2_InverseVerticalLinear(element, cx, cy, childBaseX, childBaseY);
			break;
		case MODE_GRID_HORIZONTAL:
			_calChildrenSizeP2_HorizontalGrid(element, cx, cy, childBaseX, childBaseY);
			break;
		case MODE_GRID_VERTICAL:
			_calChildrenSizeP2_VerticalGrid(element, cx, cy, childBaseX, childBaseY);
			break;
		case MODE_GRID_INVERSE_HORIZONTAL:
			_calChildrenSizeP2_InverseHorizontalGrid(element, cx, cy, childBaseX, childBaseY);
			break;
		default:
			_calChildrenSizeP2_Normal(element, cx, cy, childBaseX, childBaseY);
			break;
		}
	}

	private static ItemsInfo _rangedCalChildrenSize_HLinear(ILayoutContainer element, int begin, int end, int lastMarginRight) {
		int size = 0;
		int count = 0;
		int countMatchParent = 0;

		for (int i = begin; i < end; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			++count;
			Rect margin = child.getMargin();
			int marginOffsetX = _judgeMargin(lastMarginRight, margin.left);

			size += marginOffsetX;

			int width = child.getWidth();
			if (width == XulManager.SIZE_MATCH_PARENT) {
				++countMatchParent;
			} else if (width < XulManager.SIZE_MAX) {
				size += width;
			}
			lastMarginRight = margin.right;
		}
		size += lastMarginRight;

		return new ItemsInfo(count, countMatchParent, size);
	}

	private static ItemsInfo _rangedCalChildrenSize_VLinear(ILayoutContainer element, int begin, int end, int lastMarginBottom) {
		int size = 0;
		int count = 0;
		int countMatchParent = 0;

		for (int i = begin; i < end; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			++count;
			Rect margin = child.getMargin();
			int marginOffsetX = _judgeMargin(lastMarginBottom, margin.top);

			size += marginOffsetX;

			int height = child.getHeight();
			if (height == XulManager.SIZE_MATCH_PARENT) {
				++countMatchParent;
			} else if (height < XulManager.SIZE_MAX) {
				size += height;
			}
			lastMarginBottom = margin.bottom;
		}
		size += lastMarginBottom;

		return new ItemsInfo(count, countMatchParent, size);
	}

	private static void _calChildrenSizeP2_HorizontalLinear(ILayoutContainer element, int cx, int cy, int childBaseX, int childBaseY) throws MatchParentUpdateException {
		int childNum = element.getChildNum();
		int xOffset = 0;
		int lastMarginRight = 0;

		ItemsInfo extItemsInfo = null;
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			Rect margin = child.getMargin();
			int marginOffsetX = _judgeMargin(lastMarginRight, margin.left);
			int marginOffsetTop = margin.top;
			int marginOffsetRight = margin.right;
			int marginOffsetBottom = margin.bottom;
			xOffset += marginOffsetX;

			int initialWidth = child.getWidth();
			int extElementReservedSize = marginOffsetRight;
			int matchParentElementNum = 1;
			if (initialWidth == XulManager.SIZE_MATCH_PARENT) {
				if (extItemsInfo == null) {
					extItemsInfo = _rangedCalChildrenSize_HLinear(element, i + 1, childNum, marginOffsetRight);
				}
				extElementReservedSize = extItemsInfo.size;
				matchParentElementNum += extItemsInfo.countMatchParent;
				--extItemsInfo.countMatchParent;
			} else if (extItemsInfo != null) {
				extElementReservedSize = extItemsInfo.size;
				if (initialWidth < XulManager.SIZE_MAX) {
					extItemsInfo.size -= marginOffsetX + initialWidth;
				}
			}

			int maxCx = (cx - extElementReservedSize - xOffset) / matchParentElementNum;
			if (child.changed()) {
				if (child instanceof ILayoutContainer) {
					_calContainerSizeP2((ILayoutContainer) child, childBaseX + xOffset, childBaseY + marginOffsetTop, maxCx, cy - marginOffsetTop - marginOffsetBottom);
					_calContainerSizeP3((ILayoutContainer) child);
				} else {
					_calElementSizeP2(child, childBaseX + xOffset, childBaseY + marginOffsetTop, maxCx, cy - marginOffsetTop - marginOffsetBottom);
					child.doFinal();
				}
			} else {
				_rebaseAndCheckUpdateMatchParent(child, childBaseX + xOffset, childBaseY + marginOffsetTop, maxCx, cy - marginOffsetTop - marginOffsetBottom);
			}
			int finalChildWidth = child.getWidth();
			xOffset += finalChildWidth;
			lastMarginRight = margin.right;
		}
	}

	private static void _calChildrenSizeP2_VerticalLinear(ILayoutContainer element, int cx, int cy, int childBaseX, int childBaseY) throws MatchParentUpdateException {
		int childNum = element.getChildNum();
		int yOffset = 0;
		int lastMarginBottom = 0;

		ItemsInfo extItemsInfo = null;
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			Rect margin = child.getMargin();
			int marginOffsetY = _judgeMargin(lastMarginBottom, margin.top);
			int marginOffsetLeft = margin.left;
			int marginOffsetRight = margin.right;
			int marginOffsetBottom = margin.bottom;
			yOffset += marginOffsetY;

			int initialHeight = child.getHeight();
			int extElementReservedSize = marginOffsetBottom;
			int matchParentElementNum = 1;
			if (initialHeight == XulManager.SIZE_MATCH_PARENT) {
				if (extItemsInfo == null) {
					extItemsInfo = _rangedCalChildrenSize_VLinear(element, i + 1, childNum, marginOffsetBottom);
				}
				extElementReservedSize = extItemsInfo.size;
				matchParentElementNum += extItemsInfo.countMatchParent;
				--extItemsInfo.countMatchParent;
			} else if (extItemsInfo != null) {
				extElementReservedSize = extItemsInfo.size;
				if (initialHeight < XulManager.SIZE_MAX) {
					extItemsInfo.size -= marginOffsetY + initialHeight;
				}
			}
			int maxCy = (cy - extElementReservedSize - yOffset) / matchParentElementNum;
			if (child.changed()) {
				if (child instanceof ILayoutContainer) {
					_calContainerSizeP2((ILayoutContainer) child, childBaseX + marginOffsetLeft, childBaseY + yOffset, cx - marginOffsetLeft - marginOffsetRight, maxCy);
					_calContainerSizeP3((ILayoutContainer) child);
				} else {
					_calElementSizeP2(child, childBaseX + marginOffsetLeft, childBaseY + yOffset, cx - marginOffsetLeft - marginOffsetRight, maxCy);
					child.doFinal();
				}
			} else {
				_rebaseAndCheckUpdateMatchParent(child, childBaseX + marginOffsetLeft, childBaseY + yOffset, cx - marginOffsetLeft - marginOffsetRight, maxCy);
			}
			yOffset += child.getHeight();
			lastMarginBottom = margin.bottom;
		}
	}

	private static void _calChildrenSizeP2_InverseHorizontalLinear(ILayoutContainer element, int cx, int cy, int childBaseX, int childBaseY) throws MatchParentUpdateException {
		int childNum = element.getChildNum();
		int xOffset = 0;
		int lastMarginLeft = 0;
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			Rect margin = child.getMargin();
			int marginOffsetX = _judgeMargin(lastMarginLeft, margin.right);
			int marginOffsetTop = margin.top;
			int marginOffsetBottom = margin.bottom;
			int marginOffsetRight = margin.right;
			xOffset += marginOffsetX;

			if (child.changed()) {
				if (child instanceof ILayoutContainer) {
					_calContainerSizeP2((ILayoutContainer) child, childBaseX + xOffset, childBaseY + marginOffsetTop, cx - xOffset - marginOffsetRight, cy - marginOffsetTop - marginOffsetBottom);
					_calContainerSizeP3((ILayoutContainer) child);
				} else {
					_calElementSizeP2(child, childBaseX + xOffset, childBaseY + marginOffsetTop, cx - xOffset - marginOffsetRight, cy - marginOffsetTop - marginOffsetBottom);
					child.doFinal();
				}
			} else {
				_rebaseAndCheckUpdateMatchParent(child, childBaseX + xOffset, childBaseY + marginOffsetTop, cx - xOffset - marginOffsetRight, cy - marginOffsetTop - marginOffsetBottom);
			}
			xOffset += child.getWidth();
			lastMarginLeft = margin.left;
		}
		_calContainerSizeP3(element);

		Rect padding = element.getPadding();
		xOffset = element.getViewRight() + element.getOffsetX() - padding.right;
		lastMarginLeft = 0;
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}
			Rect margin = child.getMargin();
			int marginOffsetX = _judgeMargin(lastMarginLeft, margin.right);
			xOffset -= marginOffsetX;
			xOffset -= child.getRight();
			_offsetBase(child, xOffset - child.getBaseX(), 0);
			lastMarginLeft = margin.left;
		}
	}

	private static void _calChildrenSizeP2_InverseVerticalLinear(ILayoutContainer element, int cx, int cy, int childBaseX, int childBaseY) throws MatchParentUpdateException {
		int childNum = element.getChildNum();
		int yOffset = 0;
		int lastMarginTop = 0;
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			Rect margin = child.getMargin();
			int marginOffsetY = _judgeMargin(lastMarginTop, margin.bottom);
			int marginOffsetLeft = margin.left;
			int marginOffsetRight = margin.right;
			int marginOffsetBottom = margin.bottom;
			yOffset += marginOffsetY;

			if (child.changed()) {
				if (child instanceof ILayoutContainer) {
					_calContainerSizeP2((ILayoutContainer) child, childBaseX + marginOffsetLeft, childBaseY + yOffset, cx - marginOffsetLeft - marginOffsetRight, cy - yOffset - marginOffsetBottom);
					_calContainerSizeP3((ILayoutContainer) child);
				} else {
					_calElementSizeP2(child, childBaseX + marginOffsetLeft, childBaseY + yOffset, cx - marginOffsetLeft - marginOffsetRight, cy - yOffset - marginOffsetBottom);
					child.doFinal();
				}
			} else {
				_rebaseAndCheckUpdateMatchParent(child, childBaseX + marginOffsetLeft, childBaseY + yOffset, cx - marginOffsetLeft - marginOffsetRight, cy - yOffset - marginOffsetBottom);
			}
			yOffset += child.getHeight();
			lastMarginTop = margin.top;
		}

		_calContainerSizeP3(element);

		Rect padding = element.getPadding();
		yOffset = element.getViewBottom() + element.getOffsetY() - padding.bottom;
		lastMarginTop = 0;
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			Rect margin = child.getMargin();
			int marginOffsetY = _judgeMargin(lastMarginTop, margin.bottom);
			yOffset -= marginOffsetY;
			yOffset -= child.getBottom();
			_offsetBase(child, 0, yOffset - child.getBaseY());
		}
	}

	private static void _calChildrenSizeP2_HorizontalGrid(ILayoutContainer element, int cx, int cy, int childBaseX, int childBaseY) throws MatchParentUpdateException {
		int childNum = element.getChildNum();
		int xOffset = 0;
		int yOffset = 0;
		int maxHeight = 0;
		int lastMarginRight = 0;
		int lastMarginBottom = 0;
		int lastLineMarginBottom = 0;

		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			Rect margin = child.getMargin();
			int marginOffsetLeft = margin.left;
			int marginOffsetTop = margin.top;
			int marginOffsetRight = margin.right;
			int marginOffsetBottom = margin.bottom;

			int marginOffsetX = _judgeMargin(lastMarginRight, marginOffsetLeft);
			int marginOffsetY = _judgeMargin(lastMarginBottom, marginOffsetTop);
			xOffset += marginOffsetX;

			boolean changed = child.changed();
			if (changed) {
				if (child instanceof ILayoutContainer) {
					_calContainerSizeP2((ILayoutContainer) child, childBaseX + xOffset, childBaseY + yOffset + marginOffsetY
						, cx - xOffset - marginOffsetRight
						, cy - yOffset - marginOffsetY - marginOffsetBottom);
					_calContainerSizeP3((ILayoutContainer) child);
				} else {
					_calElementSizeP2(child, childBaseX + xOffset, childBaseY + yOffset + marginOffsetY
						, cx - xOffset - marginOffsetRight
						, cy - yOffset - marginOffsetY - marginOffsetBottom);
					child.doFinal();
				}
			}
			int childW = child.getWidth();
			if (xOffset + childW > cx) {
				xOffset = marginOffsetLeft;
				yOffset += maxHeight;
				maxHeight = 0;
				lastMarginBottom = lastLineMarginBottom;
				lastLineMarginBottom = marginOffsetBottom;
				marginOffsetY = _judgeMargin(lastMarginBottom, marginOffsetTop);
			} else {
				lastLineMarginBottom = _judgeMargin(lastLineMarginBottom, marginOffsetBottom);
			}
			lastMarginRight = marginOffsetRight;
			_rebase(child, childBaseX + xOffset, childBaseY + yOffset + marginOffsetY);
			if (!changed && child.checkUpdateMatchParent(cx - xOffset - marginOffsetRight, cy - yOffset - marginOffsetY - marginOffsetBottom)) {
				child.prepare();
				throw matchParentUpdateException;
			}

			xOffset += childW;
			int childH = child.getHeight() + marginOffsetY;
			if (maxHeight < childH) {
				maxHeight = childH;
			}
		}
	}

	private static void _calChildrenSizeP2_InverseHorizontalGrid(ILayoutContainer element, int cx, int cy, int childBaseX, int childBaseY) throws MatchParentUpdateException {
		int childNum = element.getChildNum();
		int xOffset = 0;
		int yOffset = 0;
		int maxHeight = 0;
		int lastMarginRight = 0;
		int lastMarginBottom = 0;
		int lastLineMarginBottom = 0;

		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			Rect margin = child.getMargin();
			int marginOffsetLeft = margin.left;
			int marginOffsetTop = margin.top;
			int marginOffsetRight = margin.right;
			int marginOffsetBottom = margin.bottom;

			int marginOffsetX = _judgeMargin(lastMarginRight, marginOffsetLeft);
			int marginOffsetY = _judgeMargin(lastMarginBottom, marginOffsetTop);
			xOffset += marginOffsetX;

			boolean changed = child.changed();
			if (changed) {
				if (child instanceof ILayoutContainer) {
					_calContainerSizeP2((ILayoutContainer) child, childBaseX + xOffset, childBaseY + yOffset + marginOffsetY, cx - xOffset, cy - yOffset - marginOffsetY);
					_calContainerSizeP3((ILayoutContainer) child);
				} else {
					_calElementSizeP2(child, childBaseX + xOffset, childBaseY + yOffset + marginOffsetY, cx - xOffset, cy - yOffset - marginOffsetY);
					child.doFinal();
				}
			}
			int childW = child.getWidth();
			if (xOffset + childW > cx) {
				xOffset = marginOffsetLeft;
				yOffset += maxHeight;
				maxHeight = 0;
				lastMarginBottom = lastLineMarginBottom;
				lastLineMarginBottom = marginOffsetBottom;
				marginOffsetY = _judgeMargin(lastMarginBottom, marginOffsetTop);
			} else {
				lastLineMarginBottom = _judgeMargin(lastLineMarginBottom, marginOffsetBottom);
			}
			lastMarginRight = marginOffsetRight;
			_rebase(child, childBaseX + xOffset, childBaseY + yOffset + marginOffsetY);
			if (!changed && child.checkUpdateMatchParent(cx - xOffset, cy - yOffset - marginOffsetY)) {
				child.prepare();
				throw matchParentUpdateException;
			}

			xOffset += childW;
			int childH = child.getHeight() + marginOffsetY;
			if (maxHeight < childH) {
				maxHeight = childH;
			}
		}
		_calContainerSizeP3(element);

		Rect padding = element.getPadding();
		int paddingRight = padding == null ? 0 : padding.right;
		int maxWidth = element.getWidth() - paddingRight;
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}
			int baseX = child.getBaseX();
			int width = child.getWidth();

			int baseXOffset = (maxWidth - 2 * (baseX - childBaseX) - width);
			_offsetBase(child, baseXOffset, 0);
		}
	}

	private static void _calChildrenSizeP2_VerticalGrid(ILayoutContainer element, int cx, int cy, int childBaseX, int childBaseY) throws MatchParentUpdateException {
		int childNum = element.getChildNum();
		int xOffset = 0;
		int yOffset = 0;
		int maxWidth = 0;
		int lastMarginRight = 0;
		int lastLineMarginRight = 0;
		int lastMarginBottom = 0;

		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			Rect margin = child.getMargin();
			int marginOffsetLeft = margin.left;
			int marginOffsetTop = margin.top;
			int marginOffsetRight = margin.right;
			int marginOffsetBottom = margin.bottom;

			int marginOffsetX = _judgeMargin(lastMarginRight, marginOffsetLeft);
			int marginOffsetY = _judgeMargin(lastMarginBottom, marginOffsetTop);
			yOffset += marginOffsetY;

			boolean changed = child.changed();
			if (changed) {
				if (child instanceof ILayoutContainer) {
					_calContainerSizeP2((ILayoutContainer) child, childBaseX + xOffset + marginOffsetX, childBaseY + yOffset
						, cx - xOffset - marginOffsetX - marginOffsetRight
						, cy - yOffset - marginOffsetBottom);
					_calContainerSizeP3((ILayoutContainer) child);
				} else {
					_calElementSizeP2(child, childBaseX + xOffset + marginOffsetX, childBaseY + yOffset
						, cx - xOffset - marginOffsetX - marginOffsetRight
						, cy - yOffset - marginOffsetBottom);
					child.doFinal();
				}
			}

			int childH = child.getHeight();
			if (yOffset + childH > cy) {
				yOffset = marginOffsetTop;
				xOffset += maxWidth;
				maxWidth = 0;
				lastMarginRight = lastLineMarginRight;
				lastLineMarginRight = marginOffsetRight;
				marginOffsetX = _judgeMargin(lastMarginRight, marginOffsetLeft);
			} else {
				lastLineMarginRight = _judgeMargin(lastLineMarginRight, marginOffsetRight);
			}
			lastMarginBottom = marginOffsetBottom;
			_rebase(child, childBaseX + xOffset + marginOffsetX, childBaseY + yOffset);
			if (!changed && child.checkUpdateMatchParent(cx - xOffset - marginOffsetX - marginOffsetRight, cy - yOffset - marginOffsetBottom)) {
				child.prepare();
				throw matchParentUpdateException;
			}

			yOffset += childH;
			int childW = child.getWidth() + marginOffsetX;
			if (maxWidth < childW) {
				maxWidth = childW;
			}
		}
	}

	private static void _calChildrenSizeP2_Normal(ILayoutContainer element, int cx, int cy, int childBaseX, int childBaseY) throws MatchParentUpdateException {
		int childNum = element.getChildNum();
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			if (child.changed()) {
				if (child instanceof ILayoutContainer) {
					_calContainerSizeP2((ILayoutContainer) child, childBaseX, childBaseY, cx, cy);
					_calContainerSizeP3((ILayoutContainer) child);
				} else {
					_calElementSizeP2(child, childBaseX, childBaseY, cx, cy);
					child.doFinal();
				}
			} else {
				_rebaseAndCheckUpdateMatchParent(child, childBaseX, childBaseY, cx, cy);
			}
		}
	}

	private static void _rebaseAndCheckUpdateMatchParent(ILayoutElement child, int baseX, int baseY, int maxW, int maxH) throws MatchParentUpdateException {
		_rebase(child, baseX, baseY);
		if (child.checkUpdateMatchParent(maxW, maxH)) {
			child.prepare();
			throw matchParentUpdateException;
		}
	}

	private static void _calContainerSizeP3(ILayoutContainer element) {
		Rect padding = element.getPadding();
		int childNum = element.getChildNum();
		int childRight = XulManager.SIZE_MAX;
		int childBottom = XulManager.SIZE_MAX;
		boolean updateW = false, updateH = false;
		switch (element.getWidth()) {
		case XulManager.SIZE_MATCH_CONTENT:
		case XulManager.SIZE_AUTO:
			childRight = Integer.MIN_VALUE;
			updateW = true;
			break;
		}
		switch (element.getHeight()) {
		case XulManager.SIZE_MATCH_CONTENT:
		case XulManager.SIZE_AUTO:
			childBottom = Integer.MIN_VALUE;
			updateH = true;
			break;
		}

		if (element.updateContentSize()) {
			childRight = Integer.MIN_VALUE;
			childBottom = Integer.MIN_VALUE;
		}

		boolean noMargin = element.getLayoutMode() == 0;

		// recursive invoke doFinal
		// calculate the content size
		for (int i = 0; i < childNum; ++i) {
			ILayoutElement child = element.getVisibleChild(i);
			if (child == null) {
				continue;
			}

			if (child.changed()) {
				if (child instanceof ILayoutContainer) {
					_calContainerSizeP3((ILayoutContainer) child);
				} else {
					child.doFinal();
				}
			}

			if (noMargin) {
				if (childRight < XulManager.SIZE_MAX) {
					int r = child.getViewRight();
					if (r > childRight) {
						childRight = r;
					}
				}
				if (childBottom < XulManager.SIZE_MAX) {
					int b = child.getViewBottom();
					if (b > childBottom) {
						childBottom = b;
					}
				}
			} else {
				Rect margin = child.getMargin();

				if (childRight < XulManager.SIZE_MAX) {
					int r = child.getViewRight() + margin.right;
					if (r > childRight) {
						childRight = r;
					}
				}
				if (childBottom < XulManager.SIZE_MAX) {
					int b = child.getViewBottom() + margin.bottom;
					if (b > childBottom) {
						childBottom = b;
					}
				}
			}
		}
		if (updateW) {
			int newWidth;
			if (childRight > Integer.MIN_VALUE) {
				newWidth = childRight - element.getLeft() - element.getBaseX() + padding.right;
			} else {
				newWidth = 0;
			}
			newWidth = element.constrainWidth(newWidth);
			element.setWidth(newWidth);
		}
		if (updateH) {
			int newHeight;
			if (childBottom > Integer.MIN_VALUE) {
				newHeight = childBottom - element.getTop() - element.getBaseY() + padding.bottom;
			} else {
				newHeight = 0;
			}
			newHeight = element.constrainHeight(newHeight);
			element.setHeight(newHeight);
		}

		if (childRight > Integer.MIN_VALUE && childBottom > Integer.MIN_VALUE && element.updateContentSize()) {
			int contentWidth = childRight - element.getLeft() - element.getBaseX() - padding.left - element.getOffsetX();
			int contentHeight = childBottom - element.getTop() - element.getBaseY() - padding.top - element.getOffsetY();
			element.setContentSize(
				contentWidth,
				contentHeight
			);

			float alignmentX = element.getAlignmentX();
			float alignmentY = element.getAlignmentY();

			int alignmentOffsetX = 0;
			int alignmentOffsetY = 0;
			if (!Float.isNaN(alignmentX)) {
				int viewWidth = element.getWidth() - padding.left - padding.right;
				if (viewWidth > contentWidth) {
					alignmentOffsetX = XulUtils.roundToInt((viewWidth - contentWidth) * alignmentX);
				}
			}
			if (!Float.isNaN(alignmentY)) {
				int viewHeight = element.getHeight() - padding.top - padding.bottom;
				if (viewHeight > contentHeight) {
					alignmentOffsetY = XulUtils.roundToInt((viewHeight - contentHeight) * alignmentY);
				}
			}

			int oldAlignmentOffsetY = element.getAlignmentOffsetY();
			int oldAlignmentOffsetX = element.getAlignmentOffsetX();
			if (alignmentOffsetY != oldAlignmentOffsetY || alignmentOffsetX != oldAlignmentOffsetX) {
				offsetChild(element, alignmentOffsetX - oldAlignmentOffsetX, alignmentOffsetY - oldAlignmentOffsetY);
				element.setAlignmentOffset(alignmentOffsetX, alignmentOffsetY);
			}
		}

		element.doFinal();
	}

	private static boolean _rebase(ILayoutElement e, int x, int y) {
		int baseX = e.getBaseX();
		int baseY = e.getBaseY();
		if (x == baseX && y == baseY) {
			return false;
		}
		e.setBase(x, y);

		int dx = x - baseX;
		int dy = y - baseY;
		if (e instanceof ILayoutContainer) {
			_offsetChild((ILayoutContainer) e, dx, dy);
		}
		return true;
	}

	private static void _offsetBase(ILayoutElement e, int dx, int dy) {
		if (dx == 0 && dy == 0) {
			return;
		}
		e.offsetBase(dx, dy);

		if (e instanceof ILayoutContainer) {
			_offsetChild((ILayoutContainer) e, dx, dy);
		}
	}

	public static void offsetChild(ILayoutContainer element, int dx, int dy) {
		if (dx == 0 && dy == 0) {
			return;
		}

		_offsetChild(element, dx, dy);
	}

	public static void offsetBase(ILayoutElement element, int dx, int dy) {
		if (dx == 0 && dy == 0) {
			return;
		}

		_offsetBase(element, dx, dy);
	}

	private static void _offsetChild(ILayoutContainer element, int dx, int dy) {
		assert _tmpElementArray.isEmpty();
		element.getAllVisibleChildren(_tmpElementArray);
		int readPos = 0;
		int childNum = _tmpElementArray.size();
		while (readPos < childNum) {
			ILayoutElement[] array = _tmpElementArray.getArray();
			for (; readPos < childNum; ++readPos) {
				ILayoutElement child = array[readPos];
				if (child == null) {
					continue;
				}
				child.offsetBase(dx, dy);
				if (child instanceof ILayoutContainer) {
					((ILayoutContainer) child).getAllVisibleChildren(_tmpElementArray);
				}
			}
			childNum = _tmpElementArray.size();
		}
		_tmpElementArray.clear();
	}

	private static int _judgeMargin(int a, int b) {
		return a < b ? b : a;
	}

	public interface ILayoutElement {
		boolean changed();

		boolean isVisible();

		int prepare();

		int doFinal();

		int getLeft();

		int getTop();

		int getWidth();

		int getHeight();

		int getRight();

		int getBottom();

		int getViewRight();

		int getViewBottom();

		int getContentWidth();

		int getContentHeight();

		Rect getPadding();

		Rect getMargin();

		int getBaseX();

		int getBaseY();

		boolean setWidth(int w);

		boolean setHeight(int h);

		boolean setBase(int x, int y);

		boolean offsetBase(int dx, int dy);

		// 检查元素是否需要更新match_parent布局
		boolean checkUpdateMatchParent(int maxW, int maxH);

		int getMinWidth();

		int getMinHeight();

		int getMaxWidth();

		int getMaxHeight();

		int constrainWidth(int newWidth);

		int constrainHeight(int newHeight);
	}

	public interface ILayoutContainer extends ILayoutElement {
		int getLayoutMode();

		int getChildNum();

		ILayoutElement getChild(int idx);

		ILayoutElement getVisibleChild(int idx);

		int getAllVisibleChildren(XulSimpleArray<ILayoutElement> array);

		int getContentOffsetX();

		int getContentOffsetY();

		boolean updateContentSize();

		int setContentSize(int w, int h);

		int getAlignmentOffsetX();

		int getAlignmentOffsetY();

		int setAlignmentOffset(int x, int y);

		float getAlignmentX();

		float getAlignmentY();

		// total offset x
		int getOffsetX();

		// total offset y
		int getOffsetY();
	}

	private static class MatchParentUpdateException extends Exception {
		public MatchParentUpdateException() {
			super("MatchParentUpdateException");
		}
	}

	private static class ItemsInfo {
		int count;
		int countMatchParent;
		int size;

		public ItemsInfo(int count, int countMatchParent, int size) {
			this.count = count;
			this.countMatchParent = countMatchParent;
			this.size = size;
		}
	}

	private static class LayoutElementArray extends XulSimpleArray<ILayoutElement> {

		public LayoutElementArray(int sz) {
			super(sz);
		}

		@Override
		protected ILayoutElement[] allocArrayBuf(int size) {
			return new ILayoutElement[size];
		}
	}
}
