package com.starcor.xul.PropMap;

/**
 * Created by hy on 2014/5/5.
 */
interface IXulPropChain<T> {
	T getVal(int key, int state);
	T getValEx(int key, int state, int state2);
}
