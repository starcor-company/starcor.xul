package com.starcor.xulapp.behavior;

import com.starcor.xulapp.XulPresenter;

import java.util.HashMap;

/**
 * Created by hy on 2015/8/31.
 */
public class XulBehaviorManager {

	private static HashMap<String, IBehaviorFactory> _behaviorFactories = new HashMap<String, IBehaviorFactory>(256);

	public static synchronized void registerBehavior(String behaviorName, IBehaviorFactory factory) {
		_behaviorFactories.put(behaviorName, factory);
	}

	public static void shutdownBehaviorManager() {

	}

	public static void initBehaviorManager() {

	}

	public interface IBehaviorFactory {
		XulUiBehavior createBehavior(XulPresenter xulPresenter);

		Class getBehaviorClass();
	}

	public static synchronized XulUiBehavior obtainBehavior(String behaviorName, XulPresenter xulPresenter) {
		final IBehaviorFactory behaviorFactory = _behaviorFactories.get(behaviorName);
		if (behaviorFactory == null) {
			return null;
		}
		return behaviorFactory.createBehavior(xulPresenter);
	}

	public static Class obtainBehaviorClass(String behaviorName) {
		final IBehaviorFactory behaviorFactory = _behaviorFactories.get(behaviorName);
		if (behaviorFactory == null) {
			return null;
		}
		return behaviorFactory.getBehaviorClass();

	}
}
