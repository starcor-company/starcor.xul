package com.starcor.xul.Render.Effect;

import com.starcor.xul.Render.Drawer.IXulAnimation;
import com.starcor.xul.Render.XulViewRender;
import com.starcor.xul.Utils.XulSimpleArray;
import com.starcor.xul.XulView;

import java.lang.ref.WeakReference;

/**
 * Created by hy on 2015/7/29.
 */
public class CompositeAnimation implements IXulAnimation {

	public interface BasicAnimation {
		void start(long timestamp);
		boolean isRunning();
		boolean updateAnimation(long timestamp, CompositeAnimation ownerAnimation);
	}

	public static class SimpleAnimation implements BasicAnimation {
		protected WeakReference<XulView> _view;
		protected WeakReference<XulViewRender> _render;

		protected long _duration;
		private long _beginTime;
		private BasicAnimation _nextAnimation;
		private boolean _isRunning = false;

		public SimpleAnimation(XulView view) {
			_view = view.getWeakReference();
			_render = new WeakReference<XulViewRender>(view.getRender());
		}

		@Override
		public void start(long timestamp) {

		}

		@Override
		public boolean isRunning() {
			return _isRunning;
		}

		@Override
		public boolean updateAnimation(long timestamp, CompositeAnimation ownerAnimation) {
			return false;
		}
	}

	public static class BasicAnimationArray extends XulSimpleArray<BasicAnimation> implements BasicAnimation {
		private boolean _isRunning = false;

		@Override
		protected BasicAnimation[] allocArrayBuf(int size) {
			return new BasicAnimation[size];
		}

		@Override
		public void start(long timestamp) {

		}

		@Override
		public boolean isRunning() {
			return _isRunning;
		}

		@Override
		public boolean updateAnimation(long timestamp, CompositeAnimation ownerAnimation) {
			return false;
		}
	}

	BasicAnimationArray _animations;
	@Override
	public boolean updateAnimation(long timestamp) {
		return false;
	}
}
