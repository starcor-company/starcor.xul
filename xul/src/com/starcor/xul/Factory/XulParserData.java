package com.starcor.xul.Factory;

/**
 * Created by hy on 2015/4/28.
 */
public abstract class XulParserData {
	public static final int ITEM_TAG_BEGIN = 0;
	public static final int ITEM_TEXT = 1;
	public static final int ITEM_TAG_END = 2;

	public abstract void buildItem(XulFactory.ItemBuilder pageBuilder);
}
