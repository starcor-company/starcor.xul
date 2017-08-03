package com.starcor.xul.Wrapper;

import android.graphics.Rect;
import com.starcor.xul.Render.XulImageRender;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2014/6/3.
 */
public class XulImageItemWrapper extends XulViewWrapper {
	public static XulImageItemWrapper fromXulView(XulView view) {
		if (view == null) {
			return null;
		}
		if (!(view.getRender() instanceof XulImageRender)) {
			return null;
		}
		return new XulImageItemWrapper(view);
	}

	XulImageItemWrapper(XulView view) {
		super(view);
	}

	public boolean hasImageLayer(int idx) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.hasImageLayer(idx);
	}

	public int getImageWidth(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return -1;
		}
		return render.getImageWidth(layer);
	}

	public int getImageHeight(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return -1;
		}
		return render.getImageHeight(layer);
	}

	public boolean resetAnimation(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.resetAnimation(layer);
	}

	public boolean isImageLoaded(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.isImageLoaded(layer);
	}

	public boolean reloadImage(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.reloadImage(layer);
	}

	public boolean isImageVisible(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return false;
		}
		return render.isImageVisible(layer);
	}

	public float getImageOpacity(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return -1;
		}
		return render.getImageOpacity(layer);
	}

	public String getImageUrl(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return null;
		}
		return render.getImageUrl(layer);
	}

	public String getImageResolvedUrl(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return null;
		}
		return render.getImageResolvedUrl(layer);
	}

	public Rect getImagePadding(int layer) {
		XulImageRender render = (XulImageRender) _view.getRender();
		if (render == null) {
			return null;
		}
		return render.getImagePadding(layer);
	}
}
