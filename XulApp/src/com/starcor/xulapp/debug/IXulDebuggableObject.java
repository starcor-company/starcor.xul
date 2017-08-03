package com.starcor.xulapp.debug;

import com.starcor.xulapp.http.XulHttpServer;

import org.xmlpull.v1.XmlSerializer;

/**
 * Created by hy on 2015/12/3.
 */
public interface IXulDebuggableObject {
	String name();

	boolean isValid();

	boolean runInMainThread();

	boolean buildBriefInfo(XulHttpServer.XulHttpServerRequest request, XmlSerializer infoWriter);

	boolean buildDetailInfo(XulHttpServer.XulHttpServerRequest request, XmlSerializer infoWriter);

	XulHttpServer.XulHttpServerResponse execCommand(String command, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerHandler serverHandler);
}
