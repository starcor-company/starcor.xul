package com.starcor.xuldemo.behavior;

import android.widget.Toast;

import com.starcor.xul.Wrapper.XulMassiveAreaWrapper;
import com.starcor.xul.XulDataNode;
import com.starcor.xul.XulUtils;
import com.starcor.xul.XulView;

/**
 * Created by hy on 2015/8/7.
 */
public class XABMassiveDemo extends XulActivityBehavior {

	public static void register() {
		XulActivityBehavior.registerBehavior("behavior_massive_demo", new IBehaviorFactory() {
			@Override
			public XulActivityBehavior create() {
				return new XABMassiveDemo();
			}
		});
	}

	@Override
	public void onDoAction(XulView view, String action, String type, String command, Object userdata) {
		if ("add-items".equals(action)) {
			XulView itemById = mXulRenderContext.getPage().findItemById("massive-area");
			XulMassiveAreaWrapper xulMassiveAreaWrapper = XulMassiveAreaWrapper.fromXulView(itemById);
			int baseId = xulMassiveAreaWrapper.itemNum();
			int num = XulUtils.tryParseInt(command, 0);
			for (int i = 0; i < num; i++) {
				XulDataNode dataNode = XulDataNode.obtainDataNode("item");
				dataNode.setValue(String.valueOf(baseId + i + 1));
				xulMassiveAreaWrapper.addItem(dataNode);
			}
			xulMassiveAreaWrapper.syncContentView();
		} else if ("clean-items".equals(action)) {
			XulView itemById = mXulRenderContext.getPage().findItemById("massive-area");
			XulMassiveAreaWrapper xulMassiveAreaWrapper = XulMassiveAreaWrapper.fromXulView(itemById);
			xulMassiveAreaWrapper.clear();
		} else if ("update-items".equals(action)) {
			XulDataNode dataNode = XulDataNode.obtainDataNode("item");
			dataNode.setValue("new value");
			XulView itemById = mXulRenderContext.getPage().findItemById("massive-area");
			XulMassiveAreaWrapper xulMassiveAreaWrapper = XulMassiveAreaWrapper.fromXulView(itemById);
			xulMassiveAreaWrapper.updateItems(2, dataNode,dataNode,dataNode,dataNode);
		} else if ("make-child-visible".equals(action)) {
			XulView itemById = mXulRenderContext.getPage().findItemById("massive-area");
			final XulMassiveAreaWrapper xulMassiveAreaWrapper = XulMassiveAreaWrapper.fromXulView(itemById);

			final int targetItem = (int) (Math.random()*xulMassiveAreaWrapper.itemNum());

			Toast.makeText(mXulActivity, "Make child visible" + targetItem, Toast.LENGTH_SHORT).show();

			xulMassiveAreaWrapper.makeChildVisible(itemById.findParentByType("slider"), targetItem, 0.5f, Float.NaN, true, new Runnable() {
				@Override
				public void run() {
					XulView itemView = xulMassiveAreaWrapper.getItemView(targetItem);
					if (itemView != null) {
						mXulRenderContext.getLayout().requestFocus(itemView);
					}
				}
			});
		}
	}
}
