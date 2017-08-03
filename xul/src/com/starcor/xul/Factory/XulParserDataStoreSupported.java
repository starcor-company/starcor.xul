package com.starcor.xul.Factory;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

/**
 * Created by hy on 2015/4/28.
 */
public class XulParserDataStoreSupported extends XulParserData {
	private final XulFactory.IPullParser _parser;
	private Object _store_pos;

	private int next_event() throws Exception {
		if (_store_pos != null) {
			Object store_pos = _store_pos;
			_store_pos = null;
			return _parser.restoreParserPos(store_pos);
		}
		return _parser.nextToken();
	}

	public XulParserDataStoreSupported(XulFactory.IPullParser parser, Object store_pos) {
		this._parser = parser;
		this._store_pos = store_pos;
	}

	@Override
	public void buildItem(XulFactory.ItemBuilder pageBuilder) {
		final ArrayList<XulFactory.ItemBuilder> buildStack = new ArrayList<XulFactory.ItemBuilder>();
		buildStack.add(pageBuilder);
		Object result = null;
		XulFactory.Attributes pullAttributes = new XulFactory.Attributes() {
			@Override
			public String getValue(String name) {
				return _parser.getAttributeValue(name);
			}

			@Override
			public String getValue(int i) {
				return _parser.getAttributeValue(i);
			}

			@Override
			public int getLength() {
				return _parser.getAttributeCount();
			}

			@Override
			public String getName(int i) {
				return _parser.getAttributeName(i);
			}
		};
		try {
			while (true) {
				int nextTokenId = next_event();
				switch (nextTokenId) {
				case XmlPullParser.START_DOCUMENT:
					buildStack.clear();
					break;
				case XmlPullParser.END_DOCUMENT:
					buildStack.clear();
					break;
				case XmlPullParser.START_TAG: {
					String name = _parser.getName();
					if (buildStack.isEmpty()) {
						throw new Exception("can not initialize item builder");
					} else {
						XulFactory.ItemBuilder itemBuilder = buildStack.get(buildStack.size() - 1);
						XulFactory.ItemBuilder subItemBuilder = itemBuilder.pushSubItem(_parser, "", name, pullAttributes);
						if (subItemBuilder == null) {
							itemBuilder = buildStack.remove(buildStack.size() - 1);
							result = itemBuilder.finalItem();
							continue;
						}
						buildStack.add(subItemBuilder);
					}
				}
				break;
				case XmlPullParser.END_TAG: {
					if (buildStack.isEmpty()) {
						throw new Exception("result content not match");
					}
					XulFactory.ItemBuilder itemBuilder = buildStack.remove(buildStack.size() - 1);
					result = itemBuilder.finalItem();
				}
				break;
				case XmlPullParser.TEXT:
				case XmlPullParser.CDSECT: {
					XulFactory.ItemBuilder itemBuilder = buildStack.get(buildStack.size() - 1);
					itemBuilder.pushText("", _parser);

				}
				break;
				}
				if (nextTokenId == XmlPullParser.END_DOCUMENT) {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
