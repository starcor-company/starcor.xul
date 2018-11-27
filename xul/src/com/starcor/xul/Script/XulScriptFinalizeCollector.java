package com.starcor.xul.Script;

import java.util.ArrayList;

/**
 * Created by skycnlr on 2018/6/6.
 */
public class XulScriptFinalizeCollector {
    private static volatile ArrayList<Object> _addObjects = new ArrayList<Object>();
    private static volatile ArrayList<Object> _clearObjects = new ArrayList<Object>();
    private static Object _lock = new Object();

    private static volatile Thread _collectThread;

    static {
        _collectThread = new Thread() {
            @Override
            public void run() {
                while (_collectThread != null) {
                    try {
                        doFinalize();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        _collectThread.setName("XulScriptFinalizeCollector Thread");
        _collectThread.setPriority(Thread.MIN_PRIORITY);
        _collectThread.start();
    }

    public static void register(Object o) {
        if (o == null) {
            return;
        }
        if (!(o instanceof IScriptFinalize)) {
            return;
        }
        synchronized (_lock) {
            _addObjects.add(o);
        }
    }

    public static void stop() {
        if (_collectThread != null) {
            try {
                _collectThread.interrupt();
                _collectThread = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }

    private static void doFinalize() {
        synchronized (_lock) {
            ArrayList<Object> tmp = _clearObjects;
            _clearObjects = _addObjects;
            _addObjects = tmp;
        }
        int count = _clearObjects.size();
        if (count <= 0) {
            return;
        }
        int idx = count - 1;
        for (; idx > -1; idx--) {
            Object o = _clearObjects.get(idx);
            if (o == null) {
                continue;
            }
            try {
                if (o instanceof IScriptFinalize) {
                    ((IScriptFinalize) o).doFinalize();
                }
            } catch (Exception e) {

            } finally {
                _clearObjects.remove(idx);
            }
        }

    }
}
