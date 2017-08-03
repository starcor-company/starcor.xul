package com.starcor.xul.Factory;

import com.starcor.xul.Utils.XulCachedHashMap;
import com.starcor.xul.Utils.XulPullParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class XulFactory {
	public static abstract class ItemBuilder {
		public interface FinalCallback<T> {
			void onFinal(T obj);
		}

		public boolean initialize(String name, Attributes attrs) {
			return true;
		}

		public ItemBuilder pushSubItem(IPullParser parser, String path, String name, Attributes attrs) {
			return null;
		}

		public boolean pushText(String path, IPullParser parser) {
			return false;
		}

		public Object finalItem() {
			return null;
		}
	}

	public static abstract class ResultBuilderContext {
		public abstract String getName();
	}

	public static abstract class ResultBuilder {
		public ItemBuilder build(ResultBuilderContext ctx, String name, Attributes attrs) {
			return null;
		}
	}

	public static class DummyItemBuilder extends ItemBuilder {
		public ItemBuilder pushSubItem(IPullParser parser, String path, String name, Attributes attrs) {
			return this;
		}

		public boolean pushText(String path, IPullParser parser) {
			return true;
		}
	}

	public static abstract class Attributes {
		public abstract String getValue(String name);

		public abstract String getValue(int i);

		public abstract int getLength();

		public abstract String getName(int i);
	}

	public static class SaxAttributes extends Attributes {

		org.xml.sax.Attributes _attr;

		public void attach(org.xml.sax.Attributes attr) {
			_attr = attr;
		}

		@Override
		public String getValue(String name) {
			if (_attr == null) {
				return null;
			}
			return _attr.getValue("", name);
		}

		@Override
		public String getValue(int i) {
			if (_attr == null) {
				return null;
			}
			return _attr.getValue(i);
		}

		@Override
		public int getLength() {
			if (_attr == null) {
				return 0;
			}
			return _attr.getLength();
		}

		@Override
		public String getName(int i) {
			if (_attr == null) {
				return null;
			}
			return _attr.getLocalName(i);
		}
	}

	public interface IPullParser {
		int nextToken() throws Exception;

		String getName();

		String getAttributeValue(String name);

		String getAttributeValue(int idx);

		String getAttributeName(int idx);

		int getAttributeCount();

		String getText();

		Object storeParserPos();

		int restoreParserPos(Object store_point);
	}

	public static class TextParser implements XulFactory.IPullParser {
		public String text;

		@Override
		public int nextToken() throws Exception {
			return 0;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getAttributeValue(String name) {
			return null;
		}

		@Override
		public String getAttributeValue(int idx) {
			return null;
		}

		@Override
		public String getAttributeName(int idx) {
			return null;
		}

		@Override
		public int getAttributeCount() {
			return 0;
		}

		@Override
		public String getText() {
			return text;
		}

		@Override
		public Object storeParserPos() {
			return null;
		}

		@Override
		public int restoreParserPos(Object store_point) {
			return -1;
		}
	}

	static class ResultFactory {
		XulCachedHashMap<String, ResultBuilder> _builderMap = new XulCachedHashMap<String, ResultBuilder>();

		public void addBuilder(String contentType, ResultBuilder builder) {
			_builderMap.put(contentType, builder);
		}

		public Object build(String contentType, byte[] data, String pathName) throws Exception {
			return build(contentType, new ByteArrayInputStream(data), pathName);
		}

		public Object build(String contentType, InputStream data, final String pathName) throws Exception {
			if (data == null) {
				return null;
			}

			ResultBuilderContext ctx = new ResultBuilderContext() {
				@Override
				public String getName() {
					return pathName;
				}
			};
			ResultBuilder builder = _builderMap.get(contentType);
			if (builder == null) {
				throw new Exception("无法创建目标对象 " + contentType);
			}
			if (false) {
				// parse with JAVA pull parser
				final XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
				pullParser.setInput(data, "UTF-8");
				return _doBuild(ctx, builder, new IPullParser() {

					@Override
					public int nextToken() throws Exception {
						return pullParser.nextToken();
					}

					@Override
					public String getName() {
						return pullParser.getName();
					}

					@Override
					public String getAttributeValue(String name) {
						return pullParser.getAttributeValue(null, name);
					}

					@Override
					public String getAttributeValue(int idx) {
						return pullParser.getAttributeValue(idx);
					}

					@Override
					public String getAttributeName(int idx) {
						return pullParser.getAttributeName(idx);
					}

					@Override
					public int getAttributeCount() {
						return pullParser.getAttributeCount();
					}

					@Override
					public String getText() {
						return pullParser.getText();
					}

					@Override
					public Object storeParserPos() {
						return null;
					}

					@Override
					public int restoreParserPos(Object store_point) {
						return -1;
					}
				});
			} else {
				// parse with customized JNI pull parser
				final XulPullParser pullParser = new XulPullParser(data);
				return _doBuild(ctx, builder, new IPullParser() {

					@Override
					public int nextToken() throws Exception {
						return pullParser.nextToken();
					}

					@Override
					public String getName() {
						return pullParser.getName();
					}

					@Override
					public String getAttributeValue(String name) {
						return pullParser.getAttributeValue(null, name);
					}

					@Override
					public String getAttributeValue(int idx) {
						return pullParser.getAttributeValue(idx);
					}

					@Override
					public String getAttributeName(int idx) {
						return pullParser.getAttributeName(idx);
					}

					@Override
					public int getAttributeCount() {
						return pullParser.getAttributeCount();
					}

					@Override
					public String getText() {
						return pullParser.getText();
					}

					@Override
					public Object storeParserPos() {
						return pullParser.storePosition();
					}

					@Override
					public int restoreParserPos(Object store_point) {
						return pullParser.loadPosition(store_point);
					}
				});
			}
		}

		private Object _doBuild(ResultBuilderContext ctx, final ResultBuilder builder, final IPullParser pullParser) throws Exception {
			final ArrayList<ItemBuilder> buildStack = new ArrayList<ItemBuilder>();
			Object result = null;
			Attributes pullAttributes = new Attributes() {
				@Override
				public String getValue(String name) {
					return pullParser.getAttributeValue(name);
				}

				@Override
				public String getValue(int i) {
					return pullParser.getAttributeValue(i);
				}

				@Override
				public int getLength() {
					return pullParser.getAttributeCount();
				}

				@Override
				public String getName(int i) {
					return pullParser.getAttributeName(i);
				}
			};

			while (true) {
				int nextTokenId = pullParser.nextToken();
				switch (nextTokenId) {
				case XmlPullParser.START_DOCUMENT:
					buildStack.clear();
					break;
				case XmlPullParser.END_DOCUMENT:
					buildStack.clear();
					break;
				case XmlPullParser.START_TAG: {
					String name = pullParser.getName();
					if (buildStack.isEmpty()) {
						ItemBuilder itemBuilder = builder.build(ctx, name, pullAttributes);
						if (itemBuilder == null) {
							throw new Exception("can not initialize item builder");
						}
						buildStack.add(itemBuilder);
					} else {
						ItemBuilder itemBuilder = buildStack.get(buildStack.size() - 1);
						ItemBuilder subItemBuilder = itemBuilder.pushSubItem(pullParser, "", name, pullAttributes);
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
					ItemBuilder itemBuilder = buildStack.remove(buildStack.size() - 1);
					result = itemBuilder.finalItem();
				}
				break;
				case XmlPullParser.TEXT:
				case XmlPullParser.CDSECT: {
					ItemBuilder itemBuilder = buildStack.get(buildStack.size() - 1);
					itemBuilder.pushText("", pullParser);

				}
				break;
				}
				if (nextTokenId == XmlPullParser.END_DOCUMENT) {
					break;
				}
			}
			return result;
		}
	}

	final static ResultFactory factories = new ResultFactory();

	public static void registerBuilder(Class<?> c, ResultBuilder builder) {
		synchronized (factories) {
			factories.addBuilder(c.getName(), builder);
		}
	}

	public static <T> T build(Class<T> cls, byte[] data, String dataPath) throws Exception {
		if (data == null) {
			return null;
		}
		return build(cls, new ByteArrayInputStream(data), dataPath);
	}

	public static <T> T build(Class<T> cls, InputStream data, String dataPath) throws Exception {
		if (data == null) {
			return null;
		}
		Object obj;
		synchronized (factories) {
			obj = factories.build(cls.getName(), data, dataPath);
		}
		if (obj == null) {
			throw new Exception("创建对象失败!");
		}
		if (!cls.isInstance(obj)) {
			throw new Exception("创建的结果对象类型不符合预期!");
		}
		return (T) obj;
	}
}
