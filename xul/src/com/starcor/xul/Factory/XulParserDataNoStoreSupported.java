package com.starcor.xul.Factory;

import android.util.Pair;
import com.starcor.xul.XulUtils;

import java.util.ArrayList;

/**
 * Created by hy on 2015/4/28.
 */
public class XulParserDataNoStoreSupported extends XulParserData {
	public static final Pair<Integer, Object> END_TAG = Pair.create(ITEM_TAG_END, (Object) null);

	ArrayList<Pair<Integer, Object>> _data = new ArrayList<Pair<Integer, Object>>();

	XulFactory.ItemBuilder _dataBuilderEx = new XulFactory.ItemBuilder() {
		@Override
		public XulFactory.ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, XulFactory.Attributes attrs) {
			XulParserCachedTag cachedTag = new XulParserCachedTag(name, attrs);
			_data.add(Pair.create(ITEM_TAG_BEGIN, (Object) cachedTag));
			return this;
		}

		@Override
		public boolean pushText(String path, XulFactory.IPullParser parser) {
			_data.add(Pair.create(ITEM_TEXT, (Object) XulUtils.getCachedString(parser.getText())));
			return true;
		}

		@Override
		public Object finalItem() {
			_data.add(END_TAG);
			return null;
		}
	};

	XulFactory.ItemBuilder _dataBuilder = new XulFactory.ItemBuilder() {
		@Override
		public XulFactory.ItemBuilder pushSubItem(XulFactory.IPullParser parser, String path, String name, XulFactory.Attributes attrs) {
			XulParserCachedTag cachedTag = new XulParserCachedTag(name, attrs);

			_data.add(Pair.create(ITEM_TAG_BEGIN, (Object) cachedTag));
			switch (name.charAt(0)) {
			case 'a':
				if ("attr".equals(name) || "action".equals(name)) {
					return _dataBuilderEx;
				}
				break;
			case 'b':
				if ("binding".equals(name)) {
					return _dataBuilderEx;
				}
				break;
			case 'd':
				if ("data".equals(name)) {
					return _dataBuilderEx;
				}
				break;
			case 's':
				if ("style".equals(name)) {
					return _dataBuilderEx;
				}
				break;
			}
			return this;
		}

		@Override
		public boolean pushText(String path, XulFactory.IPullParser parser) {
			return true;
		}

		@Override
		public Object finalItem() {
			_data.add(END_TAG);
			return null;
		}
	};

	public XulFactory.ItemBuilder pushSubItem(String path, String name, XulFactory.Attributes attrs) {
		return _dataBuilder.pushSubItem(null, path, name, attrs);
	}

	boolean pushText(String path, XulFactory.IPullParser parser) {
		return _dataBuilder.pushText(path, parser);
	}

	XulFactory.TextParser _textParser = new XulFactory.TextParser();

	@Override
	public void buildItem(XulFactory.ItemBuilder pageBuilder) {
		int size = _data.size();
		ArrayList<XulFactory.ItemBuilder> buildStack = new ArrayList<XulFactory.ItemBuilder>();
		buildStack.add(pageBuilder);
		for (int i = 0; i < size; i++) {
			Pair<Integer, Object> item = _data.get(i);
			int lastBuildIdx = buildStack.size() - 1;
			XulFactory.ItemBuilder itemBuilder = buildStack.get(lastBuildIdx);
			switch (item.first) {
			case ITEM_TAG_BEGIN:
				XulParserCachedTag tag = (XulParserCachedTag) item.second;
				XulFactory.ItemBuilder builder = itemBuilder.pushSubItem(null, null, tag.getTagName(), tag);
				builder.initialize(tag.getTagName(), tag);
				buildStack.add(builder);
				break;
			case ITEM_TEXT:
				_textParser.text = (String) item.second;
				itemBuilder.pushText(null, _textParser);
				break;
			case ITEM_TAG_END:
				itemBuilder.finalItem();
				buildStack.remove(lastBuildIdx);
				break;
			}
		}
	}
}
