package com.starcor.xulapp.model;

import com.starcor.xul.XulDataNode;
import com.starcor.xulapp.utils.XulExecutable;
import com.starcor.xulapp.utils.XulLog;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hy on 2015/9/24.
 */
public class XulQueryScheduler implements XulExecutable {
	public static final int ON_SUCCESS = 0;
	public static final int ON_ERROR = 1;
	public static final int ON_FINISH = 2;
	private static final String TAG = XulQueryScheduler.class.getSimpleName();
	private static final int MAX_EVENT_NUM = ON_FINISH + 1;
	private static final int ST_UNKNOWN = -1;
	private static final int ST_RUNNING = -2;
	private static final int ST_SUCCESS = 0;
	private static final int ST_ERROR = 1;
	private static final int ST_FAILED = 2;
	XulDataCallback _callback;
	HashMap<XulPendingDataCallback, Boolean> _taskMap = new HashMap<XulPendingDataCallback, Boolean>();
	ArrayList<ScheduledOperation> _scheduledOperation = new ArrayList<ScheduledOperation>();
	int _status = ST_UNKNOWN;
	ArrayList<XulExecutable>[] _pendingSchedulers;
	volatile int _parallelExecutableCount = 0;
	DisposableExecutable _disposableExecutable;

	public XulQueryScheduler(XulDataCallback dataCallback) {
		_callback = dataCallback;
	}

	static public XulQueryScheduler create(XulDataCallback dataCallback) {
		return new XulQueryScheduler(dataCallback);
	}

	static public XulQueryScheduler create() {
		return new XulQueryScheduler(null);
	}

	public XulDataCallback wrap(final XulDataCallback orgCallback) {
		final XulPendingDataCallback pendingDataCallback = new XulPendingDataCallback() {
			@Override
			public boolean scheduleExec(final XulDataOperation operation, XulDataCallback dataCallback) {
				_scheduledOperation.add(new ScheduledOperation(operation, dataCallback));
				if (_status == ST_RUNNING) {
					try {
						return operation.exec(this);
					} catch (XulDataException e) {
						XulLog.e(TAG, e);
					}
					if (_status == ST_RUNNING) {
						_status = ST_ERROR;
						_notifyError();
						_activatePendingSchedulers(ON_ERROR, null);
					}
					return false;
				}
				return true;
			}

			@Override
			public void onResult(XulDataService.Clause clause, int code, XulDataNode data) {
				if (_taskMap == null) {
					return;
				}
				_taskMap.remove(this);
				if (orgCallback != null) {
					orgCallback.onResult(clause, code, data);
				}

				if (clause.getError() != 0) {
					onClauseError(clause);
					return;
				}

				onClauseSuccess(clause);
			}

			@Override
			public void onError(XulDataService.Clause clause, int code) {
				if (_taskMap == null) {
					return;
				}
				_taskMap.remove(this);

				if (orgCallback != null) {
					orgCallback.onError(clause, code);
				}

				onClauseError(clause);
			}

			@Override
			public Object getUserData() {
				return orgCallback.getUserData();
			}

			protected void onClauseSuccess(XulDataService.Clause clause) {
				if (_taskMap.isEmpty()) {
					if (_status == ST_RUNNING) {
						_status = ST_SUCCESS;
						if (_callback != null) {
							_callback.onResult(null, 0, null);
						}
						_activatePendingSchedulers(ON_SUCCESS, clause);
					} else if (_status == ST_ERROR) {
						_status = ST_FAILED;
					}
					_activatePendingSchedulers(ON_FINISH, clause);
				}
			}

			protected void onClauseError(XulDataService.Clause clause) {
				if (_status == ST_RUNNING) {
					_status = ST_ERROR;
					_notifyError();
					_activatePendingSchedulers(ON_ERROR, clause);
				}

				if (_status == ST_ERROR && _taskMap.isEmpty()) {
					_status = ST_FAILED;
					_activatePendingSchedulers(ON_FINISH, clause);
				}
			}
		};
		_taskMap.put(pendingDataCallback, Boolean.FALSE);
		return pendingDataCallback;
	}

	private void _activatePendingSchedulers(int event, XulDataService.Clause clause) {
		if (_pendingSchedulers == null) {
			return;
		}

		final ArrayList<XulExecutable> pendingScheduler = _pendingSchedulers[event];
		if (pendingScheduler == null) {
			return;
		}
		for (int i = 0, size = pendingScheduler.size(); i < size; i++) {
			final XulExecutable xulQueryScheduler = pendingScheduler.get(i);
			xulQueryScheduler.exec(clause);
		}
	}

	public XulQueryScheduler schedule(XulQueryScheduler scheduler, int condition) {
		if (condition >= MAX_EVENT_NUM) {
			return this;
		}
		return schedule(scheduler.obtainBarrierExecutable(), condition);
	}

	public XulExecutable obtainBarrierExecutable() {
		++_parallelExecutableCount;
		return new BarrierExecutable(this);
	}

	public XulExecutable obtainDisposableExecutable() {
		if (_disposableExecutable == null) {
			_disposableExecutable = new DisposableExecutable(this);
		} else {
			_disposableExecutable.reset(this);
		}
		return _disposableExecutable;
	}

	public XulQueryScheduler schedule(XulExecutable execTarget, int condition) {
		if (condition >= MAX_EVENT_NUM) {
			return this;
		}

		if (_pendingSchedulers == null) {
			_pendingSchedulers = new ArrayList[MAX_EVENT_NUM];
		}

		ArrayList<XulExecutable> pendingScheduler = _pendingSchedulers[condition];
		if (pendingScheduler == null) {
			pendingScheduler = _pendingSchedulers[condition] = new ArrayList<XulExecutable>();
		}
		pendingScheduler.add(execTarget);
		return this;
	}

	@Override
	public boolean exec(XulDataService.Clause clause) {
		return exec();
	}

	public boolean exec() {
		if (_scheduledOperation == null || _scheduledOperation.isEmpty()) {
			_status = ST_SUCCESS;
			if (_callback != null) {
				_callback.onResult(null, 0, null);
			}
			return true;
		}
		_status = ST_RUNNING;
		for (ScheduledOperation operation : _scheduledOperation) {
			try {
				if (operation.operation.exec(operation.dataCallback)) {
					continue;
				}
			} catch (XulDataException e) {
				XulLog.e(TAG, e);
			}

			if (_status == ST_RUNNING) {
				_status = ST_ERROR;
				_notifyError();
				_activatePendingSchedulers(ON_ERROR, null);
			}
			return false;
		}
		return true;
	}

	private void _notifyError() {
		if (_callback != null) {
			_callback.onError(null, 0);
			_callback = null;
		}
	}

	private static class BarrierExecutable implements XulExecutable {
		XulQueryScheduler _target;

		public BarrierExecutable(XulQueryScheduler target) {
			_target = target;
		}

		@Override
		public boolean exec(XulDataService.Clause clause) {
			XulQueryScheduler target = _target;
			if (target == null) {
				return false;
			}
			_target = null;
			boolean execNow = (--target._parallelExecutableCount == 0);
			return execNow && target.exec(clause);
		}
	}

	private static class DisposableExecutable implements XulExecutable {
		XulQueryScheduler _target;

		public DisposableExecutable(XulQueryScheduler target) {
			_target = target;
		}

		@Override
		public boolean exec(XulDataService.Clause clause) {
			XulQueryScheduler target = _target;
			if (target == null) {
				return false;
			}
			_target = null;
			return target.exec(clause);
		}

		public void reset(XulQueryScheduler target) {
			_target = target;
		}
	}

	static class ScheduledOperation {
		XulDataOperation operation;
		XulDataCallback dataCallback;

		public ScheduledOperation(XulDataOperation operation, XulDataCallback dataCallback) {
			this.operation = operation;
			this.dataCallback = dataCallback;
		}
	}
}
