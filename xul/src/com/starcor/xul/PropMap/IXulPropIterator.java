package com.starcor.xul.PropMap;

import com.starcor.xul.Prop.XulProp;

public interface IXulPropIterator<T extends XulProp> {
	void onProp(T prop, int state);
}
