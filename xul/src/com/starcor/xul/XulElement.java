package com.starcor.xul;

/**
 * Created by hy on 2015/6/8.
 */
public abstract class XulElement {
	public static final int VIEW_TYPE = 1;
	public static final int TEMPLATE_TYPE = 2;

	public final int elementType;

	public XulElement(int t) {
		this.elementType = t;
	}
}
