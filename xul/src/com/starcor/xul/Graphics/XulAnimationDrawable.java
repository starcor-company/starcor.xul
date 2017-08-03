package com.starcor.xul.Graphics;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.starcor.xul.Utils.XulCachedHashMap;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by hy on 2014/6/20.
 */
public abstract class XulAnimationDrawable extends XulDrawable {
	static class AnimationPackage {
		XulCachedHashMap<String, Object> _contents = new XulCachedHashMap<String, Object>();
		ArrayList<String> _aniDesc = new ArrayList<String>();

		public AnimationPackage(InputStream stream) {
			ZipInputStream zipInputStream = new ZipInputStream(stream);
			ZipEntry entry;
			byte[] buffer = new byte[1024];
			try {
				while ((entry = zipInputStream.getNextEntry()) != null) {
					int size = (int) entry.getSize();
					ByteArrayOutputStream baos;
					if (size > 0) {
						baos = new ByteArrayOutputStream(size);
					} else {
						baos = new ByteArrayOutputStream();
					}

					int len;
					while ((len = zipInputStream.read(buffer)) > 0) {
						baos.write(buffer, 0, len);
					}
					_contents.put(entry.getName(), baos.toByteArray());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				zipInputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			byte[] aniDesc = (byte[]) _contents.get("ani.txt");
			if (aniDesc != null) {
				try {
					BufferedReader stringReader = new BufferedReader(new StringReader(new String(aniDesc)));
					while (stringReader.ready()) {
						String line = stringReader.readLine();
						if (line == null) {
							break;
						}
						line = line.trim();
						if (line.startsWith(";")) {
							continue;
						}
						if (line.isEmpty()) {
							continue;
						}
						_aniDesc.add(line);
					}
					stringReader.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public Bitmap loadFrameImage(String name) {
			byte[] entry = (byte[]) _contents.get(name);
			try {
				InputStream inputStream = new ByteArrayInputStream(entry);
				return BitmapTools.decodeStream(inputStream, Bitmap.Config.ARGB_8888, 0, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public ArrayList<String> getAniDesc() {
			return _aniDesc;
		}
	}

	public static abstract class AnimationDrawingContext {
		public abstract boolean updateAnimation(long timestamp);

		public abstract boolean isAnimationFinished();

		public abstract void reset();
	}

	public static XulDrawable buildAnimation(InputStream stream, String url, String imageKey) {
		if (stream == null) {
			return null;
		}
		AnimationPackage aniPkg = new AnimationPackage(stream);
		XulFrameAnimationDrawable drawable = XulFrameAnimationDrawable.buildAnimation(aniPkg);
		if (drawable != null) {
			drawable._url = url;
			drawable._key = imageKey;
		}
		return drawable;
	}

	public abstract boolean drawAnimation(AnimationDrawingContext ctx, XulDC dc, Rect dst, Paint paint);

	public abstract boolean drawAnimation(AnimationDrawingContext ctx, XulDC dc, RectF dst, Paint paint);

	public abstract AnimationDrawingContext createDrawingCtx();
}
