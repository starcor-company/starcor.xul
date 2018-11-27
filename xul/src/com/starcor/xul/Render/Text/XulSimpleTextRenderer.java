package com.starcor.xul.Render.Text;

import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;

import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulWorker;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by hy on 2014/5/13.
 */
public class XulSimpleTextRenderer extends XulBasicTextRenderer {
	private static final String TAG = XulSimpleTextRenderer.class.getSimpleName();
	// 不应该放在行首的字符
	private static char[] _chSetA;
	// 不应该放在行尾的字符
	private static char[] _chSetB;

	private static char[][][] _charSetMap;

	private static float[] tmpWidthArray = new float[16];
	private static Paint.FontMetrics _tmpFontMetrics = new Paint.FontMetrics();

	static {
		_chSetA = new char[]{
			',',
			'.',
			'`',
			'!',
			'?',
			')',
			'}',
			']',
			':',
			';',
			'>',
			'。',
			'，',
			'？',
			'”',
			'；',
			'：',
			'》',
			'）',
			'、',
			'＞',
			'］',
			'．',
			'，',
			'＇',
			'﹒',
			'﹜',
			'﹞',
			'′',
			'•',
			'｝',
			'’',
			'…',
			'〉',
			'〉',
			'》',
			'」',
			'』',
			'】',
			'〕',
			'〗',
			'〞',
		};

		Arrays.sort(_chSetA);

		_chSetB = new char[]{
			'(',
			'<',
			'[',
			'{',
			'±',
			'‘',
			'“',
			'‹',
			'∈',
			'∏',
			'∑',
			'∕',
			'∠',
			'∧',
			'∨',
			'∩',
			'∪',
			'∫',
			'∫',
			'∮',
			'∴',
			'∵',
			'≤',
			'≥',
			'≦',
			'≧',
			'≮',
			'≯',
			'∽',
			'≈',
			'≌',
			'≠',
			'≡',
			'⊙',
			'⊿',
			'⊥',
			'〈',
			'《',
			'「',
			'『',
			'【',
			'〔',
			'〖',
			'〝',
			'﹙',
			'﹛',
			'﹝',
			'﹤',
			'（',
			'＜',
			'＝',
			'＞',
			'［',
			'｛',
		};
		Arrays.sort(_chSetB);
	}

	static {
		_charSetMap = new char[][][]{
			// 数字
			new char[][]{new char[]{'0', '9'}},
			// 英文
			new char[][]{new char[]{'a', 'z'}, new char[]{'A', 'Z'}},
			// 西里尔文
			new char[][]{new char[]{'Ё', 'ё'}},
			// 阿拉伯语
			new char[][]{new char[]{'ﻼ', 'ﭑ'}},
			// 高棉语
			new char[][]{new char[]{'ក', '᧿'}},
		};
	}

	private static int getCharSetIdx(char c) {
		for (int charSetIdx = 0, charSetMapLength = _charSetMap.length; charSetIdx < charSetMapLength; charSetIdx++) {
			char[][] charSetRanges = _charSetMap[charSetIdx];
			for (int rangeIdx = 0, charSetRangesLength = charSetRanges.length; rangeIdx < charSetRangesLength; rangeIdx++) {
				char[] range = charSetRanges[rangeIdx];
				if (range[0] <= c && c <= range[1]) {
					return charSetIdx;
				}
			}
		}
		return -1;
	}

	private static boolean isCharInCharSet(int charSetIdx, char c) {
		if (charSetIdx < 0 || charSetIdx >= _charSetMap.length) {
			return false;
		}
		char[][] charSetRanges = _charSetMap[charSetIdx];
		for (int rangeIdx = 0, charSetRangesLength = charSetRanges.length; rangeIdx < charSetRangesLength; rangeIdx++) {
			char[] range = charSetRanges[rangeIdx];
			if (range[0] <= c && c <= range[1]) {
				return true;
			}
		}
		return false;
	}

	private static int inSameCharSet(char c1, char c2) {
		int charSetIdx = getCharSetIdx(c1);
		if (charSetIdx < 0) {
			return -1;
		}

		if (isCharInCharSet(charSetIdx, c2)) {
			return charSetIdx;
		}
		return -1;
	}

	private static boolean isInCharSet(char[] chSet, char c) {
		return Arrays.binarySearch(chSet, c) > 0;
	}

	float[] _textWidths;
	ArrayList<_lineInfo> _lines = null;
	private SimpleTextEditor _editor;

	public XulSimpleTextRenderer(XulViewRender render) {
		super(render);
	}

	private static boolean isRTLChar(char c) {
		final Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(c);
		return unicodeBlock == Character.UnicodeBlock.ARABIC;
	}

	@Override
	public void drawText(XulDC dc, float xBase, float yBase, float clientViewWidth, float clientViewHeight) {
		if (TextUtils.isEmpty(_text)) {
			return;
		}

		if (_multiline) {
			drawTextMultiLine(dc, xBase, yBase, clientViewWidth, clientViewHeight);
		} else {
			drawTextSingleLine(dc, xBase, yBase, clientViewWidth, clientViewHeight);
		}
	}

	public void drawTextMultiLine(XulDC dc, float xBase, float yBase, float clientViewWidth, float clientViewHeight) {
		boolean isRTL = _render.isRTL();
		Paint defPaint = _getTextPaint();
		multilineLayout(defPaint, clientViewWidth, clientViewHeight, isRTL);

		float xOffset = xBase;
		float yOffset = yBase;

		float resampleScalar = _superResample;
		float resampleScalarDelta = resampleScalar - 1.0f;
		boolean doSuperResample = Math.abs(resampleScalarDelta) > 0.01f;
		if (doSuperResample) {
			defPaint = _getTextPaint(resampleScalar);
			dc.scale(1.0f / resampleScalar, 1.0f / resampleScalar, xOffset, yOffset - _textBaseLineTop);
		}

		if (_textHeight < clientViewHeight && !_drawEllipsis && Math.abs(_fontAlignY) > 0.001) {
			yOffset += (clientViewHeight - _textHeight) * _fontAlignY;
		}

		int lineCount = _lines.size();
		for (int i = 0; i < lineCount; i++) {
			_lineInfo lineInfo = _lines.get(i);
			float curLineYOffset = lineInfo.yOffset + yOffset;
			if (curLineYOffset + _textLineHeight < 0) {
				continue;
			}
			if (curLineYOffset > clientViewHeight) {
				break;
			}
			float curLineXOffset = xOffset + lineInfo.xOffset;
			float endIndentWidth = 0;
			boolean isLastVisibleLine = curLineYOffset + _textLineHeight - _textBaseLineTop > clientViewHeight;
			if (isLastVisibleLine && _endIndent > 0) {
				endIndentWidth = _endIndent;
			}
			float resampleXDelta = lineInfo.xOffset * resampleScalarDelta;
			float lineY = curLineYOffset - _textBaseLineTop + lineInfo.yOffset * resampleScalarDelta;
			if (_drawEllipsis && (!_autoWrap || isLastVisibleLine)) {
				float lineWidth = 0;
				int lineTail = lineInfo.tail;
				if (lineInfo.lineWidth + curLineXOffset + _ellipsisWidth + endIndentWidth > clientViewWidth) {
					float maxLineWidth = clientViewWidth - _ellipsisWidth - curLineXOffset - endIndentWidth;
					for (int pos = lineInfo.head; pos < lineInfo.tail; pos++) {
						float chWidth = _textWidths[pos];
						if (lineWidth > 0 && lineWidth + chWidth >= maxLineWidth) {
							lineTail = pos;
							break;
						}
						lineWidth += chWidth;
					}
				} else {
					lineWidth = lineInfo.lineWidth;
				}
				if (lineTail < lineInfo.tail || (_autoWrap && lineCount > i + 1)) {
					final boolean rtlChar = isRTL || isRTLChar(_text.charAt(lineTail - 1));
					if (rtlChar) {
						Rect bound = XulDC._tmpRc0;
						do {
							defPaint.getTextBounds(_text, lineInfo.head, lineTail, bound);
							if (XulUtils.calRectWidth(bound) + _ellipsisWidth + endIndentWidth > lineWidth && lineTail > lineInfo.head) {
								--lineTail;
								continue;
							}
						} while (false);
						dc.drawText(_text, lineInfo.head, lineTail, curLineXOffset + resampleXDelta + _ellipsisWidth * resampleScalar, lineY, defPaint);
						dc.drawText("…", 0, 1, curLineXOffset, lineY, defPaint);
					} else {
						dc.drawText(_text, lineInfo.head, lineTail, curLineXOffset + resampleXDelta, lineY, defPaint);
						dc.drawText("…", 0, 1, curLineXOffset + resampleXDelta + lineWidth * resampleScalar, lineY, defPaint);
					}
				} else {
					dc.drawText(_text, lineInfo.head, lineTail, curLineXOffset + resampleXDelta, lineY, defPaint);
				}
				if (_autoWrap && lineCount > i + 1) {
					break;
				}
			} else {
				float deltaX = 0;
				if (lineInfo.lineWidth < clientViewWidth && _fontAlignX > 0) {
					deltaX = _fontAlignX * (clientViewWidth - lineInfo.xOffset - lineInfo.lineWidth);
				}
				float lineX = curLineXOffset + resampleXDelta + deltaX * resampleScalar;
				dc.drawText(_text, lineInfo.head, lineInfo.tail, lineX, lineY, defPaint);
			}
		}
	}

	public void drawTextSingleLine(XulDC dc, float xBase, float yBase, float clientViewWidth, float clientViewHeight) {
		boolean isRTL = _render.isRTL();
		Paint defPaint = _getTextPaint();
		float cuTextWidth = _textWidth;

		int lastChar = _text.length();
		if ((_fixHalfChar || _drawEllipsis) && _textWidth > clientViewWidth) {
			float curW = 0;
			float deltaW = 0;
			if (_drawEllipsis) {
				deltaW = _ellipsisWidth;
			}
			for (int i = 0; i < _text.length(); i++) {
				curW += _textWidths[i];
				if (curW + deltaW <= clientViewWidth) {
					cuTextWidth = XulUtils.ceilToInt(curW);
					lastChar = i + 1;
				} else {
					break;
				}
			}
		}

		float xOff = (clientViewWidth - cuTextWidth) * _fontAlignX;
		float yOff = (clientViewHeight - _textHeight) * _fontAlignY;

		if (isRTL) {
			if (cuTextWidth >= clientViewWidth) {
				xOff = clientViewWidth - cuTextWidth;
			}
		} else if (cuTextWidth >= clientViewWidth) {
			xOff = 0;
		}

		float textXPos = xOff + xBase;
		float textYPos = yOff - _textBaseLineTop + yBase;

		float resampleScalar = _superResample;
		boolean doSuperResample = Math.abs(resampleScalar - 1.0f) > 0.01f;
		if (doSuperResample) {
			defPaint = _getTextPaint(resampleScalar);
			dc.scale(1.0f / resampleScalar, 1.0f / resampleScalar, textXPos, textYPos);
		}

		if (_textWidth >= clientViewWidth) {
			dc.drawText(_text, 0, lastChar, textXPos, textYPos, defPaint);
			if (_textWidth > clientViewWidth && _drawEllipsis) {
				dc.drawText("…", 0, 1, textXPos + cuTextWidth * resampleScalar, textYPos, defPaint);
			}
		} else {
			dc.drawText(_text, 0, _text.length(), textXPos, textYPos, defPaint);
		}
	}

	@Override
	public XulTextEditor edit() {
		if (_editor == null) {
			_editor = new SimpleTextEditor();
		} else {
			_editor.reset();
		}
		return _editor;
	}

	@Override
	public XulWorker.DrawableItem collectPendingImageItem() {
		return null;
	}

	@Override
	public void stopAnimation() {}

	private void multilineLayout(Paint defPaint, float viewWidth, float viewHeight, boolean isRTL) {
		if (_lines != null) {
			return;
		}
		_lines = new ArrayList<_lineInfo>();

		float lineWidth = 0;
		float xStart = 0;
		float yPos = -_textBaseLineTop;
		_textWidth = 0;
		int lineHead = 0;
		char prevCh = 0;
		int lineTail = 0;
		int textLength = _text.length();
		boolean drawing = true;
		float startIndentOffset = 0;

		if (_autoWrap && isRTL) {
			float maxWidth = viewWidth;
			while (lineHead < textLength) {
				lineTail = lineHead + 1;
				while (lineTail < textLength) {
					char ch = _text.charAt(lineTail);
					++lineTail;
					if (ch == '\n') {
						break;
					}
				}
				float lineMaxWidth = maxWidth;
				if (lineHead == 0 && _startIndent > 0.5f) {
					lineMaxWidth -= _startIndent;
					startIndentOffset = _startIndent;
				} else {
					startIndentOffset = 0;
				}
				int lineLen = defPaint.breakText(_text, lineHead, lineTail, true, lineMaxWidth, tmpWidthArray);
				if (lineLen <= 0) {
					break;
				}
				lineTail = lineHead + lineLen;

				float curLineWidth = tmpWidthArray[0];
				_lineInfo lineInfo = new _lineInfo(lineHead, lineTail, xStart + startIndentOffset, yPos + _textBaseLineTop);
				lineInfo.lineWidth = curLineWidth;
				_lines.add(lineInfo);

				yPos += _textLineHeight;
				lineHead = lineTail;
			}
		} else {
			if (_startIndent > 0.5f) {
				startIndentOffset = _startIndent;
			}
			for (int i = 0; drawing && i < textLength; i++) {
				char c = _text.charAt(i);
				if (c == '\r') {
					prevCh = 0;
					continue;
				}
				if (c == '\n') {
					_lineInfo lineInfo = new _lineInfo(lineHead, i, xStart + startIndentOffset, yPos + _textBaseLineTop);
					lineInfo.lineWidth = lineWidth;
					lineWidth = 0;
					_lines.add(lineInfo);
					yPos += _textLineHeight;
					lineHead = i + 1;
					startIndentOffset = 0;
					prevCh = 0;
					continue;
				}
				float cWidth = _textWidths[i];
				lineWidth += cWidth;

				if (xStart + startIndentOffset + lineWidth > viewWidth) {
					if (_autoWrap) {
						lineTail = i;
						float curLineWidth = lineWidth - cWidth;
						lineWidth = 0;
						if (i - lineHead > 1) {
							// 只有当前行字符数大于2个时才做行首/尾字符调整
							int charSetIdx = inSameCharSet(c, prevCh);
							if (charSetIdx >= 0) {
								// in same char set, do not break the word!
								int newIdx = i - 1;
								float fallbackWidth = 0;
								for (; newIdx > lineHead; newIdx--) {
									if (isCharInCharSet(charSetIdx, _text.charAt(newIdx))) {
										fallbackWidth += _textWidths[newIdx];
										continue;
									}
									break;
								}
								if (newIdx == lineHead) {
									// will still break the word!!
								} else {
									lineTail = newIdx + 1;
									curLineWidth -= fallbackWidth;
									lineWidth = fallbackWidth;
								}
							} else if (isInCharSet(_chSetA, c)) {
								if (prevCh != 0 && !isInCharSet(_chSetA, prevCh)) {
									lineTail = i - 1;
									lineWidth = _textWidths[lineTail];
									curLineWidth -= lineWidth;
								}
							} else if (prevCh != 0 && isInCharSet(_chSetB, prevCh)) {
								lineTail = i - 1;
								lineWidth = _textWidths[lineTail];
								curLineWidth -= lineWidth;
							}
						}
						_lineInfo lineInfo = new _lineInfo(lineHead, lineTail, xStart + startIndentOffset, yPos + _textBaseLineTop);
						lineInfo.lineWidth = curLineWidth;

						lineWidth += _textWidths[i];
						_lines.add(lineInfo);

						yPos += _textLineHeight;
						lineHead = lineTail;
						startIndentOffset = 0;
						prevCh = 0;
						continue;
					}
				}
				prevCh = c;
			}
		}
		_textHeight = XulUtils.ceilToInt(yPos + _textBaseLineTop);
		if (textLength > lineHead) {
			_lineInfo lineInfo = new _lineInfo(lineHead, textLength, xStart + startIndentOffset, yPos + _textBaseLineTop);
			lineInfo.lineWidth = lineWidth;
			_textHeight += _textLineHeight;
			_lines.add(lineInfo);
		}
		if (_autoWrap) {
			_textWidth = viewWidth;
		}
	}

	private static class _lineInfo {
		int head = 0;
		int tail = 0;
		float yOffset = 0;
		float xOffset = 0;
		float lineWidth = 0;

		public _lineInfo(int head, int tail, float xOffset, float yOffset) {
			this.head = head;
			this.tail = tail;
			this.xOffset = xOffset;
			this.yOffset = yOffset;
		}
	}

	public class SimpleTextEditor extends BasicTextEditor {
		@Override
		public void finish(boolean recalAutoWrap) {
			boolean isRTL = _render.isRTL();
			Rect viewPadding = _render.getPadding();
			int hPadding = viewPadding == null ? 0 : viewPadding.left + viewPadding.right;
			int vPadding = viewPadding == null ? 0 : viewPadding.top + viewPadding.bottom;
			if (_testAndSetAnyChanged || _textWidths == null || (_multiline && _autoWrap && recalAutoWrap)) {
				Rect viewRect = _render.getDrawingRect();
				int viewClientWidth = XulUtils.calRectWidth(viewRect);
				if (viewClientWidth > _render.getMaxWidth()) {
					viewClientWidth = _render.getMaxWidth();
				}
				viewClientWidth -= hPadding;
				Paint defPaint = _getTextPaint();
				defPaint.getFontMetrics(_tmpFontMetrics);
				_textBaseLineTop = _tmpFontMetrics.top;
				_textLineHeight = XulUtils.ceilToInt((_tmpFontMetrics.bottom - _tmpFontMetrics.top));
				_textBaseLineTop -= _textLineHeight * (_lineHeightScalar - 1) / 2;
				_textLineHeight *= _lineHeightScalar;

				_lines = null;
				if (!TextUtils.isEmpty(_text)) {
					final int textLength = _text.length();
					if (_textWidths == null || _textWidths.length < textLength) {
						_textWidths = new float[textLength];
					}

					defPaint.getTextWidths("…", _textWidths);
					_ellipsisWidth = _textWidths[0];
					int num = defPaint.getTextWidths(_text, _textWidths);
					assert num == textLength;
					if (_multiline) {
						float width = 0;
						int line_count = 1;
						_textWidth = 0;
						int maxWidth = _autoWrap ? viewClientWidth : Integer.MAX_VALUE;
						if (isRTL) {
							int lineHead = 0;
							while (lineHead < textLength) {
								int lineTail = lineHead + 1;
								while (lineTail < textLength) {
									char ch = _text.charAt(lineTail);
									++lineTail;
									if (ch == '\n') {
										break;
									}
								}
								int lineLen = defPaint.breakText(_text, lineHead, lineTail, true, maxWidth, tmpWidthArray);
								if (lineLen <= 0) {
									break;
								}
								width = tmpWidthArray[0];
								lineHead += lineLen;
								++line_count;
							}
						} else {
							for (int i = 0; i < textLength; i++) {
								char c = _text.charAt(i);
								if (c == '\r') {
									continue;
								}
								if (c == '\n') {
									_textWidth = Math.max(_textWidth, XulUtils.ceilToInt(width));
									width = 0;
									++line_count;
								}
								float cWidth = _textWidths[i];
								if (width + cWidth > maxWidth) {
									_textWidth = Math.max(_textWidth, XulUtils.ceilToInt(width));
									width = 0;
									++line_count;
								}
								width += cWidth;
							}
						}
						_textWidth = Math.max(_textWidth, XulUtils.ceilToInt(width));
						_textHeight = line_count * _textLineHeight;
					} else {
						float textWidth = 0;
						if (isRTL) {
							Rect bound = XulDC._tmpRc0;
							defPaint.getTextBounds(_text, 0, _text.length(), bound);
							textWidth = XulUtils.calRectWidth(bound);
						} else for (int i = 0; i < textLength; i++) {
							float cWidth = _textWidths[i];
							textWidth += cWidth;
						}
						_textWidth = XulUtils.ceilToInt(textWidth);
						_textHeight = _textLineHeight;
					}
				}
			}
			super.finish(recalAutoWrap);
		}
	}
}
