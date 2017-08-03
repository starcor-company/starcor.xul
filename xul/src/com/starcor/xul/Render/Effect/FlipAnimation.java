package com.starcor.xul.Render.Effect;

import android.graphics.Camera;
import android.graphics.Matrix;
import com.starcor.xul.Graphics.XulDC;
import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.XulViewRender;

/**
 * Created by hy on 2015/1/26.
 */
public class FlipAnimation implements IXulAnimation {

	Camera _camera = new Camera();
	Matrix _matrix = new Matrix();

	protected XulViewRender _render;

	float _angle = 0;

	public FlipAnimation(XulViewRender render) {
		this._render = render;
		_render.addAnimation(this);
	}

	@Override
	public boolean updateAnimation(long timestamp) {
		if (_render == null) {
			return false;
		}
		_angle += 3;
		_angle %= 360.0;
		_render.markDirtyView();
		return true;
	}

	public void setAngle(float angle) {
		_angle = angle;
	}

	public float getAngle() {
		return _angle;
	}

	public void preDraw(XulDC dc, float x, float y, float cx, float cy) {
		dc.save();
		_camera.save();
		_camera.rotateY(_angle);
		_camera.getMatrix(_matrix);
		_matrix.preTranslate(-x - cx / 2.0f, -y - cy / 2.0f);
		_matrix.postTranslate(+x + cx / 2.0f, y + cy / 2.0f);
		dc.setMatrix(_matrix);
	}

	public void postDraw(XulDC dc, float x, float y, float cx, float cy) {
		_camera.restore();
		dc.restore();
	}
}
