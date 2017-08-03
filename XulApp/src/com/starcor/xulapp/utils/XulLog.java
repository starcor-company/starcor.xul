package com.starcor.xulapp.utils;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.Set;

/**
 * 日志管理器
 */
public final class XulLog {

    // 允许输出日志
    private static boolean _isAllowLog = true;

    // Log过滤级别
    private static LogType _logLevel = LogType.DEBUG;

    public enum LogType {
        VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT
    }

    /**
     * verbose输出
     */
    public static void v(String tag, String msg, Object... args) {
        logString(LogType.VERBOSE, tag, msg, args);
    }

    public static void v(String tag, Object object) {
        logObject(LogType.VERBOSE, tag, object);
    }

    /**
     * debug输出
     */
    public static void d(String tag, String msg, Object... args) {
        logString(LogType.DEBUG, tag, msg, args);
    }

    public static void d(String tag, Object object) {
        logObject(LogType.DEBUG, tag, object);
    }

    /**
     * info输出
     */
    public static void i(String tag, String msg, Object... args) {
        logString(LogType.INFO, tag, msg, args);
    }

    public static void i(String tag, Object object) {
        logObject(LogType.INFO, tag, object);
    }

    /**
     * warn输出
     */
    public static void w(String tag, String msg, Object... args) {
        logString(LogType.WARN, tag, msg, args);
    }

    public static void w(String tag, Object object) {
        logObject(LogType.WARN, tag, object);
    }

    /**
     * ERROR输出
     */
    public static void e(String tag, String msg, Object... args) {
        logString(LogType.ERROR, tag, msg, args);
    }

    public static void e(String tag, Object object) {
        logObject(LogType.ERROR, tag, object);
    }

    /**
     * assert输出
     */
    public static void wtf(String tag, String msg, Object... args) {
        logString(LogType.ASSERT, tag, msg, args);
    }

    public static void wtf(String tag, Object object) {
        logObject(LogType.ASSERT, tag, object);
    }

    /**
     * 打印json
     */
    public static void json(String tag, String json) {
        int indent = 4;
        if (TextUtils.isEmpty(json)) {
            d(tag, "JSON{json is null}");
            return;
        }
        try {
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                String msg = jsonObject.toString(indent);
                d(tag, msg);
            } else if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                String msg = jsonArray.toString(indent);
                d(tag, msg);
            }
        } catch (JSONException e) {
            e(tag, e);
        }
    }

    /**
     * 根据当前的参数log等级判断是否需要打印log
     */
    public static boolean needPrintLog(LogType type) {
        return _isAllowLog && (type.ordinal() >= _logLevel.ordinal());
    }

    /**
     * 打印字符串
     */
    private static void logString(LogType type, String tag, String msg, Object... args) {
        if (!needPrintLog(type)) {
            return;
        }

        if (args.length > 0) {
            msg = getFormatMsg(msg, args);
        }

        switch (type) {
            case VERBOSE:
                Log.v(tag, msg);
                break;
            case DEBUG:
                Log.d(tag, msg);
                break;
            case INFO:
                Log.i(tag, msg);
                break;
            case WARN:
                Log.w(tag, msg);
                break;
            case ERROR:
                Log.e(tag, msg);
                break;
            case ASSERT:
                Log.wtf(tag, msg);
                break;
            default:
                break;
        }
    }

    private static String getFormatMsg(String msg, Object[] args) {
        String result = "";

        if (msg == null) {
            msg = "<null>";
        } else {
            try {
                result = String.format(msg, args);
            } catch (MissingFormatArgumentException e) {
            }
        }

        // 简单判断是否格式化正确
        if (TextUtils.isEmpty(result.trim()) || !result
                .contains(XulSystemUtil.objectToString(args[args.length - 1]))) {
            StringBuilder builder = new StringBuilder(msg);
            for (Object arg : args) {
                builder.append(" ").append(XulSystemUtil.objectToString(arg));
            }
            result = builder.toString();
        }

        return result;
    }

    /**
     * 打印对象
     */
    private static void logObject(LogType type, String tag, Object object) {
        if (!needPrintLog(type)) {
            return;
        }

        if (object != null) {
            final String simpleName = object.getClass().getSimpleName();
            if (object instanceof Throwable) {
                String msg = "";
                Throwable throwable = (Throwable) object;
                switch (type) {
                    case VERBOSE:
                        Log.v(tag, msg, throwable);
                        break;
                    case DEBUG:
                        Log.d(tag, msg, throwable);
                        break;
                    case INFO:
                        Log.i(tag, msg, throwable);
                        break;
                    case WARN:
                        Log.w(tag, msg, throwable);
                        break;
                    case ERROR:
                        Log.e(tag, msg, throwable);
                        break;
                    case ASSERT:
                        Log.wtf(tag, msg, throwable);
                        break;
                    default:
                        break;
                }
            } else if (object instanceof String) {
                logString(type, tag, (String) object);
            } else if (object.getClass().isArray()) {
                String msg = "Temporarily not support more than two dimensional Array!";
                int dim = XulArrayUtil.getArrayDimension(object);
                switch (dim) {
                    case 1:
                        Pair pair = XulArrayUtil.arrayToString(object);
                        msg = simpleName.replace("[]", "[" + pair.first + "] {\n");
                        msg += pair.second + "\n";
                        break;
                    case 2:
                        Pair pair1 = XulArrayUtil.arrayToObject(object);
                        Pair pair2 = (Pair) pair1.first;
                        msg = simpleName
                                .replace("[][]", "[" + pair2.first + "][" + pair2.second + "] {\n");
                        msg += pair1.second;
                        break;
                    default:
                        break;
                }
                logString(type, tag, msg + "}");
            } else if (object instanceof Collection) {
                Collection collection = (Collection) object;
                String msg = "%s size = %d [\n";
                msg = String.format(msg, simpleName, collection.size());
                if (!collection.isEmpty()) {
                    Iterator<Object> iterator = collection.iterator();
                    int flag = 0;
                    while (iterator.hasNext()) {
                        String itemString = "[%d]:%s%s";
                        Object item = iterator.next();
                        msg += String.format(itemString, flag, XulSystemUtil.objectToString(item),
                                             flag++ < collection.size() - 1 ? ",\n" : "\n");
                    }
                }
                logString(type, tag, msg + "]");
            } else if (object instanceof Map) {
                String msg = simpleName + " {\n";
                Map<Object, Object> map = (Map<Object, Object>) object;
                Set<Object> keys = map.keySet();
                for (Object key : keys) {
                    String itemString = "[%s -> %s]\n";
                    Object value = map.get(key);
                    msg += String.format(itemString, XulSystemUtil.objectToString(key),
                                         XulSystemUtil.objectToString(value));
                }
                logString(type, tag, msg + "}");
            } else {
                logString(type, tag, XulSystemUtil.objectToString(object));
            }
        } else {
            logString(type, tag, XulSystemUtil.objectToString(object));
        }
    }

    /**
     * 自动生成tag
     */
    private String generateTag(StackTraceElement caller) {
        String stackTrace = caller.toString();
        stackTrace = stackTrace.substring(stackTrace.lastIndexOf('('), stackTrace.length());
        String tag = "%s%s.%s%s";
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        tag = String.format(tag, callerClazzName, caller.getMethodName(), stackTrace);
        return tag;
    }

    public static boolean getAllowLog() {
        return _isAllowLog;
    }

    public static void setAllowLog(boolean isAllowLog) {
        _isAllowLog = isAllowLog;
    }

    public static LogType getLogLevel() {
        return _logLevel;
    }

    public static void setLogLevel(LogType logLevel) {
        _logLevel = logLevel;
    }
}
