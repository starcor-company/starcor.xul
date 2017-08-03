package com.starcor.xulapp.debug;

import android.graphics.Bitmap;

import com.starcor.xul.Graphics.BitmapTools;
import com.starcor.xulapp.http.XulHttpServer;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Created by hy on 2016/12/30.
 */

public class XulDebugUtils {
	static public void registerBitmapCacheDebugger() {
		XulDebugServer.getMonitor().registerDebuggableObject(new IXulDebuggableObject() {
			@Override
			public String name() {
				return "BitmapCache";
			}

			@Override
			public boolean isValid() {
				return true;
			}

			@Override
			public boolean runInMainThread() {
				return false;
			}

			@Override
			public boolean buildBriefInfo(XulHttpServer.XulHttpServerRequest request, XmlSerializer infoWriter) {
				try {
					infoWriter.attribute("", "enabled", String.valueOf(BitmapTools.hasBitmapReuse()));
					infoWriter.attribute("", "recycled", String.valueOf(BitmapTools.countRecycledBitmap()));
					infoWriter.attribute("", "recycled-pixels", String.valueOf((BitmapTools.countRecycledPixel() * 10 / 1024 / 1024) / 10.0f) + "M");
					infoWriter.attribute("", "gc", String.valueOf(BitmapTools.countGCBitmap()));
					infoWriter.attribute("", "statistic", "Recycled:" + String.valueOf(BitmapTools.getRecycleCount())
						+ "/Reused:" + String.valueOf(BitmapTools.getReuseCount())
						+ "/GC:" + String.valueOf(BitmapTools.getReuseGCCount())
						+ "/Created:" + String.valueOf(BitmapTools.getNewCount()));
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
			}

			@Override
			public boolean buildDetailInfo(XulHttpServer.XulHttpServerRequest request, final XmlSerializer infoWriter) {
				buildBriefInfo(request, infoWriter);
				try {
					infoWriter.startTag("", "statistic");
					BitmapTools.eachReuseStatistic(new BitmapTools.IReuseStatisticEnumerator() {
						@Override
						public boolean onItem(BitmapTools.ReuseStatisticInfo info) {
							try {
								infoWriter.startTag("", "type");
								infoWriter.attribute("", "width", String.valueOf(info.width));
								infoWriter.attribute("", "height", String.valueOf(info.height));
								infoWriter.attribute("", "pixel", info.config.name());
								infoWriter.attribute("", "reuse", String.valueOf(info.reusePercent()) + "%");
								infoWriter.attribute("", "drop", String.valueOf(info.dropPercent()) + "%");
								infoWriter.attribute("", "recycled", String.valueOf(info.recycled()));
								infoWriter.endTag("", "type");
							} catch (IOException e) {
								e.printStackTrace();
							}
							return true;
						}
					});
					infoWriter.endTag("", "statistic");

					infoWriter.startTag("", "recycled");
					BitmapTools.eachRecycledBitmaps(new BitmapTools.ICacheEnumerator() {
						@Override
						public boolean onItem(Bitmap bmp) {
							try {
								infoWriter.startTag("", "bitmap");
								infoWriter.attribute("", "width", String.valueOf(bmp.getWidth()));
								infoWriter.attribute("", "height", String.valueOf(bmp.getHeight()));
								infoWriter.attribute("", "pixel", bmp.getConfig().name());
								infoWriter.endTag("", "bitmap");
							} catch (IOException e) {
								e.printStackTrace();
							}
							return true;
						}
					});
					infoWriter.endTag("", "recycled");

					infoWriter.startTag("", "gc");
					BitmapTools.eachGCBitmaps(new BitmapTools.ICacheEnumerator() {
						@Override
						public boolean onItem(Bitmap bmp) {
							try {
								infoWriter.startTag("", "bitmap");
								infoWriter.attribute("", "width", String.valueOf(bmp.getWidth()));
								infoWriter.attribute("", "height", String.valueOf(bmp.getHeight()));
								infoWriter.attribute("", "pixel", bmp.getConfig().name());
								infoWriter.endTag("", "bitmap");
							} catch (IOException e) {
								e.printStackTrace();
							}
							return true;
						}
					});
					infoWriter.endTag("", "gc");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
			}

			@Override
			public XulHttpServer.XulHttpServerResponse execCommand(String command, XulHttpServer.XulHttpServerRequest request, XulHttpServer.XulHttpServerHandler serverHandler) {
				return null;
			}
		});
	}
}
