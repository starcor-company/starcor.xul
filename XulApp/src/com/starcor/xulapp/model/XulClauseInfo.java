package com.starcor.xulapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.starcor.xul.Utils.XulSimpleArray;
import com.starcor.xul.XulUtils;

/**
 * Created by hy on 2015/9/25.
 */
public class XulClauseInfo implements Parcelable {
	XulDataService.Clause clause;
	XulDataOperation dataOperation;

	String target;
	String func;
	int verb;
	XulSimpleArray<Conditions> conditions;

	DataItem dataItems;
	DataItem dataItemsLast;

	static Conditions _createClauseInfoCondition(String key, XulDataService.DataComparator comparator, String value) {
		final Conditions val = new Conditions();
		val.key = key;
		val.cmp = comparator;
		val.value = value;
		val.values = null;
		return val;
	}

	static Conditions _createClauseInfoCondition(String key, XulDataService.DataComparator comparator, String[] values) {
		final Conditions val = new Conditions();
		val.key = key;
		val.cmp = comparator;
		val.value = null;
		val.values = values;
		return val;
	}

	public int getVerb() {
		return verb & XulDataService.XVERB_MASK;
	}

	public String getFunc() {
		return func;
	}

	public XulDataService.Clause getClause() {
		return clause;
	}

	public Conditions getCondition(String key) {
		if (conditions == null) {
			return null;
		}
		for (int i = 0, size = conditions.size(); i < size; i++) {
			final Conditions cond = conditions.get(i);
			if (key.equals(cond.key)) {
				return cond;
			}
		}
		return null;
	}

	public String getConditionValue(String key, String defValue) {
		String conditionValue = getConditionValue(key);
		return conditionValue == null ? defValue : conditionValue;
	}

	public String getConditionValue(String key) {
		if (conditions == null) {
			return null;
		}
		for (int i = 0, size = conditions.size(); i < size; i++) {
			final Conditions cond = conditions.get(i);
			if (key.equals(cond.key)) {
				return cond.getValue();
			}
		}
		return null;
	}

	public void addDataItem(String key, String value) {
		final DataItem dataItem = new DataItem(key, value);
		addDataItem(dataItem);
	}

	public void addDataItem(String key, String... values) {
		final DataItem dataItem = new DataItem(key, values);
		addDataItem(dataItem);
	}

	private void addDataItem(DataItem dataItem) {
		if (dataItemsLast != null) {
			dataItemsLast.next = dataItem;
		} else {
			dataItems = dataItem;
		}
		dataItemsLast = dataItem;
	}

	public void addDataItem(String key, int value) {
		addDataItem(key, String.valueOf(value));
	}

	public void addDataItem(Object[] objs) {
		for (int i = 0, size = objs.length; i < size; i++) {
			addDataItem(new DataItem(objs[i]));
		}
	}

	public String getDataItemString(String key) {
		DataItem item = this.dataItems;
		while (item != null) {
			if (item.key == key || key.equals(item.key)) {
				return item.strVal;
			}
			item = item.next;
		}
		return null;
	}

	public String getDataItemString(String key, String defValue) {
		String dataItemString = getDataItemString(key);
		return dataItemString == null ? defValue : dataItemString;
	}

	public String[] getDataItemStrings(String key) {
		DataItem item = this.dataItems;
		while (item != null) {
			if (item.key == key || key.equals(item.key)) {
				return item.strVals;
			}
			item = item.next;
		}
		return null;
	}

	public Object getDataItemByType(Class<?> dataClass) {
		DataItem item = this.dataItems;
		while (item != null) {
			if (item.data != null && dataClass.isInstance(item.data)) {
				return item.data;
			}
			item = item.next;
		}
		return null;
	}

	public Object getDataItemByType(Class<?> dataClass, Object defValue) {
		Object dataItemByType = getDataItemByType(dataClass);
		return dataItemByType == null ? defValue : dataItemByType;
	}

	public static class Conditions {
		String key;
		XulDataService.DataComparator cmp;
		String value;
		String[] values;

		public boolean test(String val) {
			if (value == null) {
				return cmp.test(values, val);
			}
			return cmp.test(value, val);
		}

		public String getValue() {
			return value;
		}

		public String[] getValues() {
			return values;
		}

		public String joinValues(String sep) {
			if (values != null) {
				return XulUtils.join(sep, values);
			}
			if (value != null) {
				return value;
			}
			return null;
		}

		public boolean hasMultiValues() {
			return values != null;
		}
	}

	public static class DataItem {
		String key = null;
		String strVal = null;
		String[] strVals = null;
		Object data = null;
		DataItem next;

		public DataItem(String key, String value) {
			this.key = key;
			this.strVal = value;
		}

		public DataItem(String key, String[] values) {
			this.key = key;
			this.strVals = values;
		}

		public DataItem(Object obj) {
			this.data = obj;
		}
	}

	void addCondition(String key, XulDataService.DataComparator comparator, String value) {
		final XulSimpleArray<Conditions> conditions = _initClauseInfoConditions();
		conditions.add(_createClauseInfoCondition(key, comparator, value));
	}

	void addCondition(String key, XulDataService.DataComparator comparator, String[] values) {
		final XulSimpleArray<Conditions> conditions = _initClauseInfoConditions();
		conditions.add(_createClauseInfoCondition(key, comparator, values));
	}

	private XulSimpleArray<Conditions> _initClauseInfoConditions() {
		if (conditions == null) {
			conditions = new XulSimpleArray<XulClauseInfo.Conditions>() {
				@Override
				protected Conditions[] allocArrayBuf(int size) {
					return new Conditions[size];
				}
			};
		}
		return conditions;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(target);
		dest.writeString(func);
		dest.writeInt(verb);
		if (conditions != null) {
			int size = conditions.size();
			Conditions[] array = conditions.getArray();
			dest.writeInt(size);
			for (int i = 0; i < size; i++) {
				Conditions condition = array[i];
				dest.writeString(condition.key);
				dest.writeInt(condition.cmp._comparator);
				dest.writeString(condition.value);
				dest.writeStringArray(condition.values);
			}
		} else {
			dest.writeInt(0);
		}

		DataItem dataItem = dataItems;
		while (dataItem != null) {
			dest.writeByte((byte) 1);
			dest.writeString(dataItem.key);
			dest.writeString(dataItem.strVal);
			dest.writeStringArray(dataItem.strVals);
			dest.writeValue(dataItem.data);
			dataItem = dataItem.next;
		}
		dest.writeByte((byte) 0);
	}

	public XulClauseInfo() {}

	protected XulClauseInfo(Parcel in) {
		target = in.readString();
		func = in.readString();
		verb = in.readInt();
		int size = in.readInt();

		if (size>0) {
			conditions = _initClauseInfoConditions();
			for (int i = 0; i < size; i++) {
				Conditions condition = new Conditions();
				condition.key = in.readString();
				condition.cmp = XulDataService.getDataComparator(in.readInt());
				condition.value = in.readString();
				condition.values = in.createStringArray();
				conditions.add(condition);
			}
		}

		ClassLoader classLoader = getClass().getClassLoader();
		for (byte v = in.readByte(); v != 0; v = in.readByte()) {
			DataItem dataItem = new DataItem(in.readString(), in.readString());
			dataItem.strVals = in.createStringArray();
			dataItem.data = in.readValue(classLoader);
			addDataItem(dataItem);
		}
	}

	public static final Creator<XulClauseInfo> CREATOR = new Creator<XulClauseInfo>() {
		@Override
		public XulClauseInfo createFromParcel(Parcel in) {
			return new XulClauseInfo(in);
		}

		@Override
		public XulClauseInfo[] newArray(int size) {
			return new XulClauseInfo[size];
		}
	};
}
