package com.starcor.xulapp.debug;

import com.starcor.xulapp.http.XulHttpServer;

/**
 * Created by hy on 2015/12/3.
 */
public interface IXulDebugCommandHandler {
	XulHttpServer.XulHttpServerResponse execCommand(String url, XulHttpServer.XulHttpServerHandler serverHandler, XulHttpServer.XulHttpServerRequest request);
}
