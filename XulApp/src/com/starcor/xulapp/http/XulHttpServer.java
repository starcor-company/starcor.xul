package com.starcor.xulapp.http;

import android.text.TextUtils;

import com.starcor.xul.Utils.XulMemoryOutputStream;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulWorker;
import com.starcor.xulapp.utils.XulLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hy on 2015/11/30.
 */
public class XulHttpServer {
	private static final String TAG = XulHttpServer.class.getSimpleName();
	private volatile ServerSocketChannel _socketChannel;
	private volatile Selector _selector;
	private ThreadGroup _workerGroup;
	private Thread _listeningWorker;
	private ThreadPoolExecutor _reactorPool;
	private String _localAddr;
	private int _localPort;

	public XulHttpServer(String addr, int port) {
		_initServer(addr, port);
	}

	public XulHttpServer(int port) {
		_initServer(null, port);
	}

	private void _initServer(String addr, int port) {
		_localAddr = addr;
		_localPort = port;
		_workerGroup = new ThreadGroup("Xul HTTP Server");
		ThreadFactory threadFactory = new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(_workerGroup, r, "Reactor");
			}
		};
		_reactorPool = new ThreadPoolExecutor(1, 16, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(16), threadFactory);

		_listeningWorker = new Thread(_workerGroup, "Acceptor") {
			@Override
			public void run() {
				_doWork();
			}
		};
		_listeningWorker.start();
	}

	private void _doWork() {
		synchronized (this) {
			if (_selector == null) {
				try {
					_selector = Selector.open();
					SelectorProvider provider = _selector.provider();
					InetSocketAddress localAddr = TextUtils.isEmpty(_localAddr) ?
						new InetSocketAddress(_localPort) :
						new InetSocketAddress(_localAddr, _localPort);
					_socketChannel = provider.openServerSocketChannel();
					_socketChannel.configureBlocking(false);
					ServerSocket socket = _socketChannel.socket();
					socket.bind(localAddr);
					_socketChannel.register(_selector, _socketChannel.validOps(), _socketChannel);
				} catch (Exception e) {
					XulLog.e(TAG, e);
				}
			}
		}

		Selector selector;
		while ((selector = _selector) != null && selector.isOpen()) {
			try {
				selector.select();
				SelectionKey selectionKey;
				Set<SelectionKey> selectionKeys;
				synchronized (selector) {
					selectionKeys = selector.selectedKeys();
				}

				Iterator<SelectionKey> iterator = selectionKeys.iterator();
				while (true) {
					synchronized (selectionKeys) {
						if (iterator.hasNext()) {
							selectionKey = iterator.next();
							iterator.remove();
						} else {
							break;
						}
					}

					if (selectionKey == null) {
						continue;
					}

					if (!selectionKey.isValid()) {
						XulHttpServerHandler handler = (XulHttpServerHandler) selectionKey.attachment();
						handler.terminate();
						continue;
					}

					try {
						if (selectionKey.isAcceptable()) {
							SocketChannel socketChannel = _socketChannel.accept();
							if (socketChannel == null) {
								continue;
							}

							socketChannel.configureBlocking(false);
							XulHttpServerHandler handler = createHandler(this, socketChannel);
							socketChannel.register(selector, SelectionKey.OP_READ, handler);
							continue;
						}
					} catch (Exception e) {
						XulLog.e(TAG, e);
						continue;
					}

					XulHttpServerHandler handler = (XulHttpServerHandler) selectionKey.attachment();
					try {
						if (selectionKey.isWritable()) {
							handler.notifyWritable();
							continue;
						}

						if (selectionKey.isReadable()) {
							handler.notifyReadable();
							continue;
						}
					} catch (CancelledKeyException e) {
						handler.terminate();
					} catch (Exception e) {
						handler.terminate();
						XulLog.e(TAG, e);
					}
				}
			} catch (IOException e) {
				XulLog.e(TAG, e);
			}

		}
	}

	protected XulHttpServerHandler createHandler(XulHttpServer server, SocketChannel socketChannel) {
		return new XulHttpServerHandler(server, socketChannel);
	}

	private void dispatchRequest(final XulHttpServerHandler xulHttpServerHandler, final XulHttpServerRequest request) {
		_reactorPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					xulHttpServerHandler.handleHttpRequest(request);
				} catch (IOException e) {
					XulLog.e(TAG, e);
					xulHttpServerHandler.terminate();
				}
			}
		});
	}

	public static class XulHttpServerRequest extends XulHttpRequest {
		public String protocolVer;
		public byte[] body;
		public HashMap<String, String> headers = new HashMap<String, String>();

		public void addHeader(String key, String value) {
			headers.put(key, value);
		}

		public String getHeader(String key) {
			return headers.get(key);
		}
	}

	public static class XulHttpServerResponse extends XulHttpResponse {
		public String protocolVer;
		public LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
		private XulHttpServerHandler _handler;
		private XulMemoryOutputStream _outputStream;
		private InputStream _bodyStream;
		private Runnable _cleanup;

		public XulHttpServerResponse(XulHttpServerHandler handler) {
			_handler = handler;
			_outputStream = XulWorker.obtainDownloadBuffer(2048);
			_outputStream.reset(2048);
		}

		static String httpMessageFromCode(int code) {
			switch (code) {
			case 200:
				return "OK";
			case 401:
				return "Unauthorized";
			case 403:
				return "Forbidden";
			case 404:
				return "Not Found";
			case 301:
				return "Moved Permanently";
			case 302:
				return "Redirect";
			case 304:
				return "Not Modified";
			case 500:
				return "Internal Server Error";
			case 501:
				return "Not implemented";
			case 502:
				return "Proxy Error";
			case 100:
				return "Continue";
			}
			return null;
		}

		public void send() {
			_handler.reply(this);
		}

		void prepareResponseData() {
			StringBuilder responseHdr = new StringBuilder();
			responseHdr.append(protocolVer.toUpperCase());
			responseHdr.append(" ");
			responseHdr.append(code);
			if (TextUtils.isEmpty(message)) {
				message = httpMessageFromCode(code);
			}
			if (!TextUtils.isEmpty(message)) {
				responseHdr.append(" ");
				responseHdr.append(message);
			}
			responseHdr.append("\r\n");

			String transferEncoding = headers.get("Transfer-Encoding");
			boolean isChunked = "chunked".equals(transferEncoding);
			int contentLength = _outputStream.getDataSize();
			if (!hasUserBodyStream()) {
				if (!isChunked) {
					addHeader("Content-Length", String.valueOf(contentLength));
				}
			}

			for (Map.Entry<String, String> hdr : headers.entrySet()) {
				responseHdr.append(hdr.getKey());
				responseHdr.append(":");
				responseHdr.append(hdr.getValue());
				responseHdr.append("\r\n");
			}
			responseHdr.append("\r\n");

			byte[] responseHdrBytes = null;
			try {
				responseHdrBytes = responseHdr.toString().getBytes("utf-8");
			} catch (UnsupportedEncodingException e) {
				XulLog.e(TAG, e);
			}

			byte[] dataBuffer = _outputStream.getDataBuffer();

			int headerSize = responseHdrBytes.length;
			int newDataSize = contentLength + headerSize;
			_outputStream.expand(newDataSize);
			byte[] newDataBuffer = _outputStream.getDataBuffer();
			System.arraycopy(dataBuffer, 0, newDataBuffer, headerSize, contentLength);
			System.arraycopy(responseHdrBytes, 0, newDataBuffer, 0, responseHdrBytes.length);
			_outputStream.setDataSize(newDataSize);
		}

		public XulHttpServerResponse addHeader(String key, String val) {
			headers.put(key, val);
			return this;
		}

		public XulHttpServerResponse setStatus(int code) {
			this.code = code;
			return this;
		}

		public XulHttpServerResponse addHeaderIfNotExists(String key, String val) {
			if (!headers.containsKey(key)) {
				headers.put(key, val);
			}
			return this;
		}

		public XulHttpServerResponse writeBody(String data) {
			try {
				_outputStream.write(data.getBytes("utf-8"));
			} catch (IOException e) {
				XulLog.e(TAG, e);
			}
			return this;
		}

		public XulHttpServerResponse writeBody(byte[] data) {
			try {
				_outputStream.write(data);
			} catch (IOException e) {
				XulLog.e(TAG, e);
			}
			return this;
		}

		public XulHttpServerResponse writeBody(byte[] data, int offset, int length) {
			try {
				_outputStream.write(data, offset, length);
			} catch (IOException e) {
				XulLog.e(TAG, e);
			}
			return this;
		}

		public XulHttpServerResponse writeBody(InputStream inputStream) {
			while (true) {
				int dataSize = _outputStream.getDataSize();
				_outputStream.expand(dataSize + 1024);
				byte[] dataBuffer = _outputStream.getDataBuffer();
				int bufferAvailableLength = dataBuffer.length - dataSize;

				try {
					int readLen = inputStream.read(dataBuffer, dataSize, bufferAvailableLength);
					if (readLen > 0) {
						_outputStream.setDataSize(dataSize + readLen);
						continue;
					}
					break;
				} catch (IOException e) {
					XulLog.e(TAG, e);
					break;
				}
			}
			return this;
		}

		public XulHttpServerResponse writeStream(InputStream inputStream) {
			_bodyStream = inputStream;
			return this;
		}

		public OutputStream getBodyStream(int size) {
			_outputStream.expand(size);
			return _outputStream;
		}

		public OutputStream getBodyStream() {
			return _outputStream;
		}

		public XulHttpServerResponse setMessage(String msg) {
			this.message = msg;
			return this;
		}

		public XulHttpServerResponse setCleanUp(Runnable runnable) {
			_cleanup = runnable;
			return this;
		}

		byte[] getData() {
			return _outputStream.getDataBuffer();
		}

		int getDataSize() {
			return _outputStream.getDataSize();
		}

		boolean hasUserBodyStream() {
			return _bodyStream != null;
		}

		public XulHttpResponse cleanBody() {
			_outputStream.setDataSize(0);
			return this;
		}

		public void destroy() {
			if (_outputStream != null) {
				_outputStream.onClose();
			}

			try {
				final InputStream bodyStream = _bodyStream;
				_bodyStream = null;
				if (bodyStream != null) {
					bodyStream.close();
				}
			} catch (Exception e) {}

			try {
				final Runnable cleanup = _cleanup;
				_cleanup = null;
				if (cleanup != null) {
					cleanup.run();
				}
			} catch (Exception e) {}
		}

		boolean prepareUserBodyData() {
			return prepareUserBodyData(0, 0, -1);
		}

		boolean prepareUserBodyData(int startOffset, int endOffset) {
			return prepareUserBodyData(startOffset, endOffset, -1);
		}

		boolean prepareUserBodyData(int startOffset, int endOffset, int sizeLimit) {
			if (sizeLimit < 0) {
				_outputStream.expand(startOffset + endOffset + 128);
			} else {
				_outputStream.expand(startOffset + endOffset + sizeLimit);
			}

			final byte[] dataBuffer = _outputStream.getDataBuffer();
			if (sizeLimit < 0 || sizeLimit > dataBuffer.length) {
				sizeLimit = dataBuffer.length;
			}
			try {
				int readLength = _bodyStream.read(dataBuffer, startOffset, sizeLimit - startOffset - endOffset);
				_outputStream.setDataSize(readLength);
				return true;
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			return false;
		}
	}

	public static class XulHttpServerHandler {
		private final XulHttpServer _server;
		private final SocketChannel _socketChannel;
		private ByteBuffer _requestBuffer = ByteBuffer.allocate(2048);
		private HttpRequestBuilder _requestBuilder;
		private ByteBuffer _responseBuffer;
		private XulHttpServerResponse _response;
		private boolean _sendChunkedData = false;

		public XulHttpServerHandler(XulHttpServer server, SocketChannel socketChannel) {
			_server = server;
			_socketChannel = socketChannel;
		}

		SocketChannel getSocketChannel() {
			return _socketChannel;
		}

		void notifyWritable() throws IOException {
			if (_responseBuffer == null) {
				return;
			}

			final SocketChannel socketChannel = _socketChannel;
			socketChannel.write(_responseBuffer);
			if (!_responseBuffer.hasRemaining()) {
				if (_response.hasUserBodyStream()) {
					final Selector selector = _server._selector;
					final XulHttpServerHandler attachment = this;
					socketChannel.register(selector, 0, attachment);
					selector.wakeup();
					_server._reactorPool.execute(new Runnable() {
						@Override
						public void run() {
							try {
								int beginOffset = _sendChunkedData ? 32 : 0;
								int endOffset = _sendChunkedData ? 2 : 0;
								int sizeLimit = _sendChunkedData ? 8192 : -1;
								if (_response == null || !_response.prepareUserBodyData(beginOffset, endOffset, sizeLimit)) {
									terminate();
									return;
								}
								int dataSize = _response.getDataSize();
								if (dataSize <= 0) {
									if (_sendChunkedData) {
										_response.writeStream(null);
										_responseBuffer = ByteBuffer.wrap("0\r\n\r\n".getBytes());
									} else {
										terminate();
										return;
									}
								} else {
									final byte[] data = _response.getData();
									if (_sendChunkedData) {
										String dataLength = String.format("%X\r\n", dataSize);
										final byte[] dataLengthBytes = dataLength.getBytes();
										beginOffset -= dataLengthBytes.length;
										System.arraycopy(dataLengthBytes, 0, data, beginOffset, dataLengthBytes.length);
										dataSize += dataLengthBytes.length;
										data[beginOffset + dataSize++] = '\r';
										data[beginOffset + dataSize++] = '\n';
									}
									_responseBuffer = ByteBuffer.wrap(data, beginOffset, dataSize);
								}
								socketChannel.register(selector, SelectionKey.OP_WRITE, attachment);
								selector.wakeup();
							} catch (Exception e) {
								terminate();
								XulLog.e(TAG, e);
							}
						}
					});
				} else {
					socketChannel.close();
				}
				return;
			}
		}

		void notifyReadable() throws IOException {
			_requestBuffer.rewind();
			int readBytes = _socketChannel.read(_requestBuffer);

			if (readBytes < 0) {
				if (_requestBuilder == null) {
					_socketChannel.close();
					return;
				}
				if (_requestBuilder.isFinished()) {
					// wait for handling request
					return;
				}
				// if request has no Content-Length, try to finish the request building
				_requestBuffer.rewind();
				XulHttpServerRequest xulHttpRequest = _requestBuilder.buildRequest(_requestBuffer, readBytes);
				if (xulHttpRequest == null) {
					// build request failed
					_socketChannel.close();
				} else {
					_internalHandleHttpRequest(xulHttpRequest);
				}
				return;
			}

			if (_requestBuilder == null) {
				_requestBuilder = new HttpRequestBuilder();
			}
			_requestBuffer.rewind();
			XulHttpServerRequest xulHttpRequest = _requestBuilder.buildRequest(_requestBuffer, readBytes);

			if (xulHttpRequest != null) {
				_internalHandleHttpRequest(xulHttpRequest);
			}
		}

		public XulHttpServerResponse getResponse(XulHttpServerRequest httpRequest) {
			XulHttpServerResponse serverResponse = new XulHttpServerResponse(this);
			serverResponse.protocolVer = httpRequest.protocolVer;
			serverResponse.setStatus(200)
				.addHeader("Host", httpRequest.getHostString());
			return serverResponse;
		}

		void reply(XulHttpServerResponse serverResponse) {
			_response = serverResponse;

			serverResponse.addHeaderIfNotExists("Content-Type", "text/html")
				.addHeaderIfNotExists("Connection", "close");

			final String transferEncoding = _response.headers.get("Transfer-Encoding");
			_sendChunkedData = "chunked".equals(transferEncoding);
			serverResponse.prepareResponseData();

			_responseBuffer = ByteBuffer.wrap(serverResponse.getData(), 0, serverResponse.getDataSize());
			try {
				Selector selector = _server._selector;
				_socketChannel.register(selector, SelectionKey.OP_WRITE, this);
				selector.wakeup();
			} catch (ClosedChannelException e) {
				clear();
				XulLog.e(TAG, e);
			}
		}

		private void _internalHandleHttpRequest(final XulHttpServerRequest request) throws IOException {
			_socketChannel.register(_server._selector, 0, this);
			_server.dispatchRequest(this, request);
		}

		protected void handleHttpRequest(XulHttpServerRequest request) throws IOException {
			getResponse(request)
				.setStatus(404)
				.setMessage("Page Not Found")
				.send();
		}

		void terminate() {
			SocketChannel socketChannel = _socketChannel;
			try {
				if (socketChannel != null) {
					socketChannel.close();
				}
			} catch (Exception e) {
				XulLog.e(TAG, e);
			}
			clear();
		}

		private void clear() {
			HttpRequestBuilder requestBuilder = _requestBuilder;
			_requestBuilder = null;
			if (requestBuilder != null) {
				requestBuilder.destroy();
			}

			XulHttpServerResponse response = _response;
			_response = null;
			if (response != null) {
				response.destroy();
			}
		}

		class HttpRequestBuilder {
			private XulMemoryOutputStream _readBuffer;
			private int _parseState = 0;    // 0 for REQUEST line, 1 for HEADER, 2 for BODY
			private int _requestBodySize = 0;
			private int _readPos = 0;
			private int _scanPos = 0;
			private boolean _finished = false;

			private XulHttpServerRequest _request;

			public HttpRequestBuilder() {
				_readBuffer = XulWorker.obtainDownloadBuffer(4096);
			}

			public XulHttpServerRequest buildRequest(ByteBuffer netBuffer, int readBytes) {
				int dataSize = _readBuffer.getDataSize();
				if (readBytes <= 0) {
					readBytes = 0;
					if (_parseState == 2) {
						_requestBodySize = dataSize - _readPos;
					}
				}
				int newSize = dataSize + readBytes;
				_readBuffer.expand(newSize);
				byte[] dataBuffer = _readBuffer.getDataBuffer();
				netBuffer.get(dataBuffer, dataSize, readBytes);
				_readBuffer.setDataSize(dataSize + readBytes);

				String line;

				while (_parseState != 2 && (line = readLine()) != null) {
					if (_parseState == 0) {
						String[] requestLine = line.split(" ");

						String method = requestLine[0];
						String path = requestLine[1];
						String httpVer = requestLine.length > 2 ? requestLine[2] : "HTTP/1.1";

						_request = new XulHttpServerRequest();

						_request.method = method.toLowerCase();
						_request.protocolVer = httpVer.toLowerCase();
						try {
							_request.schema = httpVer.split("/")[0].toLowerCase();
						} catch (Exception e) {
							_request.schema = "http";
						}

						int queryStart = path.indexOf("?");
						int fragmentStart = path.indexOf("#");

						if (fragmentStart > 0) {
							_request.fragment = path.substring(fragmentStart + 1);
							path = path.substring(0, fragmentStart);
						}

						if (queryStart < 0) {
							_request.path = path;
						} else {
							_request.path = path.substring(0, queryStart);
							String queryString = path.substring(queryStart + 1);
							Pattern queryParamPattern = Pattern.compile("([^&=]+)(?:=([^&]*))?");
							Matcher matcher = queryParamPattern.matcher(queryString);
							while (matcher.find()) {
								String key = matcher.group(1);
								String value = matcher.group(2);
								try {
									key = URLDecoder.decode(key, "utf-8");
									if (!TextUtils.isEmpty(value)) {
										value = URLDecoder.decode(value, "utf-8");
									}
								} catch (UnsupportedEncodingException e) {
									XulLog.e(TAG, e);
								}
								_request.addQueryString(key, value);
							}
						}
						_parseState = 1;
					} else if (_parseState == 1) {
						if (line.isEmpty()) {
							_parseState = 2;
							if (_requestBodySize == 0 && !"get".equals(_request.method)) {
								String connection = _request.getHeader("connection");
								if (connection != null && connection.toLowerCase().contains("close")) {
									_requestBodySize = Integer.MAX_VALUE;
								}
							}

							String expect = _request.getHeader("expect");
							if (expect != null && expect.contains("100-continue")) {
								if (_requestBodySize > 1024 * 1024) {
									// don't receive file larger then 1MB
									try {
										_socketChannel.close();
									} catch (IOException e) {
										XulLog.e(TAG, e);
									}
									return null;
								}
								try {
									String resp100Continue = _request.protocolVer.toUpperCase() + " 100 Continue\r\n\r\n";
									_socketChannel.write(ByteBuffer.wrap(resp100Continue.getBytes("utf-8")));
								} catch (IOException e) {
									XulLog.e(TAG, e);
								}
							}
							break;
						}
						int i = line.indexOf(':');
						String headerKey = line.substring(0, i).trim();
						String headerValue = line.substring(i + 1).trim();

						_request.addHeader(headerKey.toLowerCase(), headerValue);

						// handle content-length
						if (headerKey.equalsIgnoreCase("content-length")) {
							_requestBodySize = XulUtils.tryParseInt(headerValue);
						} else if (headerKey.equalsIgnoreCase("host")) {
							int hostPortPos = headerValue.indexOf(":");
							if (hostPortPos > 0) {
								_request.port = XulUtils.tryParseInt(headerValue.substring(hostPortPos + 1));
								_request.host = headerValue.substring(0, hostPortPos);
							} else {
								_request.host = headerValue;
							}
						}
					} else {
						// error
					}

				}

				if (_parseState == 2) {
					if (_requestBodySize <= newSize - _readPos) {
						// body ready
						if (_requestBodySize > 0) {
							_request.body = Arrays.copyOfRange(dataBuffer, _readPos, _readPos + _requestBodySize);
						}
						return _request;
					}
				}
				return null;
			}

			private String readLine() {
				int dataSize = _readBuffer.getDataSize();
				byte[] dataBuffer = _readBuffer.getDataBuffer();
				for (int i = _scanPos; i < dataSize; i++) {
					byte ch = dataBuffer[i];
					if (ch != '\r' && ch != '\n') {
						_scanPos = i;
					} else if (i + 1 < dataSize &&
						ch == '\r' &&
						dataBuffer[i + 1] == '\n'
						) {
						int lineHead = _readPos;
						int lineEnd = i;
						_readPos = _scanPos = i + 2;
						return new String(dataBuffer, lineHead, lineEnd - lineHead);
					}
				}
				return null;
			}

			public boolean isFinished() {
				return _finished;
			}

			public void destroy() {
				if (_readBuffer != null) {
					_readBuffer.onClose();
				}
			}
		}
	}
}
