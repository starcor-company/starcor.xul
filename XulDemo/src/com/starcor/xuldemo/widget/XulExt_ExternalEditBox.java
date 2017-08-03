package com.starcor.xuldemo.widget;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.starcor.xul.IXulExternalView;
import com.starcor.xul.Prop.XulAttr;
import com.starcor.xul.Prop.XulStyle;
import com.starcor.xul.Utils.XulPropParser;
import com.starcor.xul.XulLayout;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/10/2.
 */
public class XulExt_ExternalEditBox extends EditText implements IXulExternalView {
    XulView _view;

    public XulExt_ExternalEditBox(Context context, XulView view) {
        super(context);
        _view = view;
        double xScalar = view.getOwnerPage().getXScalar();
        double yScalar = view.getOwnerPage().getYScalar();

        this.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        XulAttr attrMaxLines = _view.getAttr("max-lines");
        int maxLines = 1;
        if (attrMaxLines != null) {
            maxLines = XulUtils.tryParseInt(attrMaxLines.getStringValue(), maxLines);
        }
        if (maxLines == 1) {
            this.setSingleLine(true);
        } else {
            this.setMaxLines(maxLines);
        }

        XulAttr attrText = _view.getAttr("text");
        if (attrText != null) {
            this.setText(attrText.getStringValue());
        }

        XulAttr attrHintText = _view.getAttr("hint-text");
        if (attrHintText != null) {
            this.setHint(attrHintText.getStringValue());
        }

        XulAttr attrMaxLength = _view.getAttr("max-length");
        if (attrMaxLength != null) {
            this.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Integer.valueOf(attrMaxLength.getStringValue()))});
        }

        XulAttr attrDivision = _view.getAttr("space-division");
        if (attrDivision != null) {
            int max = -1;
            if (attrMaxLength != null) {
                max = Integer.valueOf(attrMaxLength.getStringValue());
            }
            int len = Integer.valueOf(attrDivision.getStringValue());
            this.addTextChangedListener(new DivisionTextWatcher(len, max));
        }

        XulStyle styleFontColor = view.getStyle("font-color");
        if (styleFontColor != null) {
            XulPropParser.xulParsedStyle_FontColor color = styleFontColor.getParsedValue();
            this.setTextColor(color.val);
        }

        String hintTextColor = view.getStyleString("hint-text-color");
        if (!TextUtils.isEmpty(hintTextColor)) {
            long color = XulUtils.tryParseHex(hintTextColor, -1);
            if (color == -1) {
                color = this.getCurrentTextColor();
            }
            this.setHintTextColor((int) color);
        }

        XulStyle styleFontSize = _view.getStyle("font-size");
        if (styleFontSize != null) {
            XulPropParser.xulParsedStyle_FontSize fontSize = styleFontSize.getParsedValue();
            this.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (fontSize.val * xScalar));
        }

        XulStyle styleBackgroundColor = _view.getStyle("background-color");
        if (styleBackgroundColor != null) {
            XulPropParser.xulParsedStyle_BackgroundColor bkgColor = styleBackgroundColor.getParsedValue();
            this.setBackgroundColor(bkgColor.val);
        } else {
            this.setBackgroundColor(0xECFFFFFF);
        }

        XulStyle stylePadding = view.getStyle("padding");
        int paddingLeft, paddingTop, paddingRight, paddingBottom;
        paddingLeft = paddingTop = paddingRight = paddingBottom = 4;
        if (stylePadding != null) {
            XulPropParser.xulParsedProp_PaddingMargin padding = stylePadding.getParsedValue();
            paddingLeft = padding.left;
            paddingTop = padding.top;
            paddingRight = padding.right;
            paddingBottom = padding.bottom;
        }

        XulStyle stylePaddingLeft = view.getStyle("padding-left");
        XulStyle stylePaddingTop = view.getStyle("padding-top");
        XulStyle stylePaddingRight = view.getStyle("padding-right");
        XulStyle stylePaddingBottom = view.getStyle("padding-bottom");

        if (stylePaddingLeft != null) {
            XulPropParser.xulParsedStyle_PaddingMarginVal padding = stylePaddingLeft.getParsedValue();
            paddingLeft = padding.val;
        }

        if (stylePaddingTop != null) {
            XulPropParser.xulParsedStyle_PaddingMarginVal padding = stylePaddingTop.getParsedValue();
            paddingTop = padding.val;
        }

        if (stylePaddingRight != null) {
            XulPropParser.xulParsedStyle_PaddingMarginVal padding = stylePaddingRight.getParsedValue();
            paddingRight = padding.val;
        }

        if (stylePaddingBottom != null) {
            XulPropParser.xulParsedStyle_PaddingMarginVal padding = stylePaddingBottom.getParsedValue();
            paddingBottom = padding.val;
        }

        this.setPadding(
                XulUtils.roundToInt(paddingLeft * xScalar),
                XulUtils.roundToInt(paddingTop * yScalar),
                XulUtils.roundToInt(paddingRight * xScalar),
                XulUtils.roundToInt(paddingBottom * yScalar));

        setImeOptions(getImeOptions() | EditorInfo.IME_FLAG_NO_FULLSCREEN);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            XulLayout rootLayout = _view.getRootLayout();
            if (rootLayout != null) {
                rootLayout.requestFocus(_view);
                //设定光标位置
                Editable editable = this.getText();
                if (editable instanceof Editable) {
                    Selection.setSelection((Spannable) this.getText(), this.getText().length());
                }
            }
        }
    }

    @Override
    public void extMoveTo(int x, int y, int width, int height) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.getLayoutParams();
        layoutParams.leftMargin = x;
        layoutParams.topMargin = y;
        layoutParams.width = width;
        layoutParams.height = height;
        this.requestLayout();
    }

    @Override
    public void extMoveTo(Rect rect) {
        extMoveTo(rect.left, rect.top, rect.width(), rect.height());
    }

    @Override
    public boolean extOnKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return dispatchKeyEvent(event);
        }
        return false;
    }

    @Override
    public void extOnFocus() {
        this.requestFocus();
    }

    @Override
    public void extOnBlur() {
        this.clearFocus();
    }

    @Override
    public void extShow() {
        this.setVisibility(VISIBLE);
    }

    @Override
    public void extHide() {
        this.setVisibility(GONE);
    }

    @Override
    public void extDestroy() {
    }

    @Override
    public String getAttr(String key, String defVal) {
        if ("text".equals(key)) {
            return this.getText().toString();
        }
        return defVal;
    }

    @Override
    public boolean setAttr(String key, String val) {
        if ("text".equals(key)) {
            this.setText(val);
            return true;
        }
        if ("hint-text".equals(key)) {
            this.setHint(val);
            return true;
        }
        return false;
    }

    @Override
    public void extSyncData() {

    }

    private class DivisionTextWatcher implements TextWatcher {

        private int division_length = 0;
        private int max_length = -1;
        private int callLevel = 0;
        private boolean isInsert = true;
        private int _deletePos;
        private int _changePos;

        public DivisionTextWatcher(int div_len, int max_len) {
            this.division_length = div_len;
            this.max_length = max_len;
        }

        @Override
        public void afterTextChanged(Editable s) {
            ++callLevel;
            for (int i = 0; i < s.length(); ++i) {
                boolean isCurrentPosSpace = i > 0 && (i % (division_length + 1)) == division_length;
                if ((s.charAt(i) == ' ') == isCurrentPosSpace) {
                    continue;
                }
                if (isInsert) {
                    if (isCurrentPosSpace) {
                        if (max_length != -1 && (s.length() == max_length)) {
                            for (int j = i; j < s.length(); j++) {
                                if (s.charAt(j) == ' ') {
                                    s.delete(j, j + 1);
                                    break;
                                }
                            }
                        }
                        s.insert(i, " ");
                        ++i;
                        continue;
                    }
                    s.delete(i, i + 1);
                    --i;
                } else {
                    isInsert = true;
                    if (i == _changePos && isCurrentPosSpace) {
                        // 如果删除了空格则需要多删除一个字符
                        s.delete(_deletePos, _deletePos + 1);
                        --i;
                    }
                }
            }
            while (s.length() > 0 && s.charAt(s.length() - 1) == ' ') {
                int length = s.length();
                s.delete(length - 1, length);
            }
            --callLevel;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (callLevel == 0) {
                isInsert = after > 0;
                _changePos = start;
                if (start == getSelectionStart()) {
                    _deletePos = start;
                } else {
                    _deletePos = start - 1;
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
//			if (before < count) { //增加字符
//				String str = s.toString().trim();
//				str = str.replaceAll("\\s*", "");
//				StringBuffer sb = new StringBuffer(str);
//				int len = str.length();
//
//				for (int i = len - 1; i > 0; i--) {
//					int index = i + 1;
//					if (index % division_length == 0) {
//						if (index != len)
//							sb = sb.insert(index, " ");
//					}
//				}
//				setText(sb);
//			} else if (before > count) { //删除
//				String str = s.toString().trim();
//				setText(str);
//			}
        }
    }
}
