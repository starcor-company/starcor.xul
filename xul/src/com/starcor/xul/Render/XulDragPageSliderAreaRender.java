package com.starcor.xul.Render;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.XulArea;
import com.starcor.xul.XulRenderContext;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

/**
 * Created by skycnlr on 2018/4/23.
 *支持拖动的pageSlider
 */
public class XulDragPageSliderAreaRender extends XulPageSliderAreaRender {
    public static final String TAG = XulDragPageSliderAreaRender.class.getSimpleName();

    private boolean autoAnimation = true;
    private boolean enableDragOnEdge = false;

    public static void register() {
        XulRenderFactory.registerBuilder("area", "drag_page_slider", new XulRenderFactory.RenderBuilder() {
            @Override
            protected XulViewRender createRender(XulRenderContext ctx, XulView view) {
                assert view instanceof XulArea;
                return new XulDragPageSliderAreaRender(ctx, (XulArea) view);
            }
        });
    }

    public XulDragPageSliderAreaRender(XulRenderContext ctx, XulArea area) {
        super(ctx, area);
    }

    @Override
    public boolean handleScrollEvent(float hScroll, float vScroll) {
        if (hScroll == 0 && vScroll == 0) {
            return false;
        }
        if (_curPage >= _contents.size()) {
            return false;
        }
        setTag(false);

        if (_isVertical) {
            if (!enableDragOnEdge) {
                if (!_isLoopMode && (_pageOffsetY == 0)) {
                    if (_curPage == 0 && vScroll > 0) { //第一页禁止向上边拖拽
                        return false;
                    }
                    if (_curPage == _contents.size() - 1 && vScroll < 0) { //最后一页禁止向下边拖拽
                        return false;
                    }
                }
            }
            _updatePageOffsetX(vScroll + _pageOffsetY);
        } else {
            if (!enableDragOnEdge) {
                if (!_isLoopMode && (_pageOffsetX == 0)) {
                    if (_curPage == 0 && hScroll > 0) { //第一页禁止向右边拖拽
                        return false;
                    }
                    if (_curPage == _contents.size() - 1 && hScroll < 0) { //最后一页禁止向左边拖拽
                        return false;
                    }
                }
            }
            _updatePageOffsetX(hScroll + _pageOffsetX);
        }
        markDirtyView();
        return true;
    }


    @Override
    protected void drawSlideAnimation(XulDC dc, Rect rect, int xBase, int yBase, int counter, XulView curPageArea, RectF
            curPageFocusRc) {
        float nextXoffset = _pageOffsetX != 0 ? XulUtils.calRectWidth(curPageFocusRc) + _pageOffsetX : 0;
        float nextYoffset = _pageOffsetY != 0 ? XulUtils.calRectHeight(curPageFocusRc) + _pageOffsetY : 0;

        float prevXoffset = _pageOffsetX != 0 ? _pageOffsetX - XulUtils.calRectWidth(curPageFocusRc) : 0;
        float prevYoffset = _pageOffsetY != 0 ? _pageOffsetY - XulUtils.calRectHeight(curPageFocusRc) : 0;

        int nextPageIndex = _curPage + 1 == counter ? (_isLoopMode ?0 : -1) : _curPage + 1;
        int prePageIndex = _curPage == 0 ? (_isLoopMode ? counter - 1 : -1) : _curPage - 1;

        XulView nextPageArea = nextPageIndex == -1? null : _contents.get(nextPageIndex);
        XulView prevPageArea = prePageIndex == -1? null : _contents.get(prePageIndex);

        if (_isVertical) {
            if (nextPageArea != null) {
                nextPageArea.draw(dc, rect, xBase, XulUtils.roundToInt(yBase + nextYoffset));
            }
            if (prevPageArea != null) {
                prevPageArea.draw(dc, rect, xBase, XulUtils.roundToInt(yBase + prevYoffset));
            }
        } else {
            if (nextPageArea != null) {
                nextPageArea.draw(dc, rect, XulUtils.roundToInt(xBase + nextXoffset), yBase);
            }
            if (prevPageArea != null) {
                prevPageArea.draw(dc, rect, XulUtils.roundToInt(xBase + prevXoffset), yBase);
            }
        }
        curPageArea.draw(dc, rect, xBase + _pageOffsetX, yBase + _pageOffsetY);
        if (autoAnimation) {
            _updateSlideAnimation();
        } else {
            Log.i("drag_page_slider", "drag_page_slider return.");
        }
    }

    @Override
    public void slideRight() {
        setTag(true);
        super.slideRight();
    }

    @Override
    public void slideLeft() {
        super.slideLeft();
    }

    @Override
    public void slideUp() {
        setTag(true);
        super.slideUp();
    }

    @Override
    public void slideNext() {
        setTag(true);
        super.slideNext();
    }

    @Override
    public void slidePrev() {
        setTag(true);
        super.slidePrev();
    }

    @Override
    public void slideDown() {
        setTag(true);
        super.slideDown();
    }

    @Override
    public boolean setCurrentPage(int page) {
        setTag(true);
        boolean isAlign = _isVertical ? (_pageOffsetY == 0) : (_pageOffsetX == 0);
        if (_curPage == page && isAlign) {
            return true;
        }
        if (page >= getPageCount()) {
            return false;
        }
        XulView lastPage = _contents.get(_curPage);
        XulView curPage = _contents.get(page);
        if (_curPage == page) {
            _pageOffsetY = 0;
            _pageOffsetX = 0;
            markDirtyView();
            return true;
        }
        lastPage.setEnabled(false);
        curPage.setEnabled(true);
        if (lastPage.hasFocus()) {
            lastPage.getRootLayout().killFocus();
        }
        _curPage = page;
        _pageOffsetY = 0;
        _pageOffsetX = 0;
        cleanPageCache();
        notifyPageChanged(_curPage);
        return true;
    }

    private void setTag(boolean animation) {
        autoAnimation = animation;
    }

    @Override
    public void doSyncData() {
        super.doSyncData();
        XulAttr xulAttr = _view.getAttr("drag_on_edge");
        if (xulAttr != null && "true".equals(xulAttr.getStringValue())) {
            enableDragOnEdge = true;
        } else {
            enableDragOnEdge = false;
        }
    }
}
