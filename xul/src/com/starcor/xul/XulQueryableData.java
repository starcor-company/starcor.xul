package com.starcor.xul;

import com.starcor.xul.Prop.XulBinding;
import com.starcor.xul.Utils.XulBindingSelector;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by hy on 2014/8/19.
 */
public class XulQueryableData {
	ArrayList<XulDataNode> _dataSet = new ArrayList<XulDataNode>();

	public XulQueryableData(InputStream stream) {
		try {
			XulDataNode build = XulDataNode.build(stream);
			if (build != null) {
				_dataSet.add(build);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public XulQueryableData(ArrayList<XulDataNode> dataSet) {
		for (int i = 0; i < dataSet.size(); i++) {
			XulDataNode node = dataSet.get(i);
			_dataSet.add(node);
		}
	}

	public XulQueryableData(XulDataNode node) {
		_dataSet.add(node);
	}

	ArrayList<XulDataNode> query(String selector) {
		return XulBindingSelector.selectData(new XulBindingSelector.IXulDataSelectContext() {
			@Override
			public boolean isEmpty() {
				return false;
			}

			@Override
			public XulBinding getDefaultBinding() {
				return null;
			}

			@Override
			public XulBinding getBindingById(String id) {
				return null;
			}
		}, selector, _dataSet);
	}

	public String queryString(String selector) {
		ArrayList<XulDataNode> result = query(selector);
		if (result == null || result.isEmpty()) {
			return null;
		}
		XulDataNode xulDataNode = result.get(0);
		return xulDataNode.getValue();
	}
}
