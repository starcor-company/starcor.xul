package com.starcor.xulapp.cache.cacheimplement;


import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.starcor.xulapp.cache.XulCacheModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by ZFB on 2015/9/21.
 */
public class XulMemoryCache extends XulCacheImpl {

	public XulMemoryCache(long maxSize, int maxCount) {
		super(maxSize, maxCount);
	}

	@Override
	public InputStream getAsStream(XulCacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof InputStream) {
			return (InputStream) data;
		}
		if (data instanceof byte[]) {
			return new ByteArrayInputStream(getAsBinary(cacheModel));
		}
		return null;
	}

	@Override
	public String getAsString(XulCacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof String) {
			return (String) data;
		}
		return String.valueOf(data);
	}

	@Override
	public JSONObject getAsJSONObject(XulCacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof JSONObject) {
			return (JSONObject) data;
		}
		return null;
	}

	@Override
	public JSONArray getAsJSONArray(XulCacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof JSONArray) {
			return (JSONArray) data;
		}
		return null;
	}

	@Override
	public byte[] getAsBinary(XulCacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof byte[]) {
			return (byte[]) data;
		}
		return null;
	}

	@Override
	public Object getAsObject(XulCacheModel cacheModel) {
		return cacheModel.getData();
	}

	@Override
	public Bitmap getAsBitmap(XulCacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof Bitmap) {
			return (Bitmap) data;
		}
		return null;
	}

	@Override
	public Drawable getAsDrawable(XulCacheModel cacheModel) {
		Object data = cacheModel.getData();
		if (data instanceof Drawable) {
			return (Drawable) data;
		}
		return null;
	}
}
