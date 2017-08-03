package com.starcor.xuldemo.utils.log;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Log辅助工具类
 *
 * @author zhangfeibiao
 */
public final class LogUtil {

    /**
     * ALL
     */
    public static final int LOG_ALL_TO_FILE = 3;
    /**
     * ERROR
     */
    public static final int LOG_ERROR_TO_FILE = 2;
    /**
     * WARN
     */
    public static final int LOG_WARN_TO_FILE = 1;
    /**
     * None
     */
    public static final int LOG_NONE_TO_FILE = 0;
    /**
     * The GLOBAL_TAG of the Application
     */
    public static String GLOBAL_TAG = "";
    /**
     * Whether to enable the log
     */
    protected static boolean mIsEnabled = true;
    /**
     * Whether to enable log to the console
     */
    protected static boolean mIsLog2ConsoleEnabled = true;
    /**
     * Whether to enable log to the file
     */
    protected static boolean mIsLog2FileEnabled = false;
    /**
     * Which will be logged into the file
     */
    protected static int mPolicy = LOG_NONE_TO_FILE;
    /**
     * log assistant information
     */
    private static String mClassName;
    private static String mMethodName;
    private static int mLineNumber;
    private static FilePathGenerator mGenerator = null;

    private static LogFormatter mFormatter = null;

    private static List<LogFilter> mFilters = null;

    // Supress default constructor for noninstantiability
    private LogUtil() {
        throw new AssertionError();
    }

    private static void log(LEVEL level, String tag, String msg, Throwable tr) {
        if (!mIsEnabled) {
            return;
        }

        String curTag = tag;
        String logMsg = msg;

        if (TextUtils.isEmpty(curTag)) {
            // Tag is empty, create tag and msg dynamically
            getMethodNames();
            curTag = getCurrentTag(tag);
            logMsg = createLog(msg);
        }

        if (mIsLog2ConsoleEnabled) {
            log2Console(level, curTag, logMsg, tr);
        }

        if (mIsLog2FileEnabled) {
            log2File(level, curTag, logMsg, tr);
        }
    }

    private static String createLog(String log) {

        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        buffer.append(mMethodName);
        buffer.append(":");
        buffer.append(mLineNumber);
        buffer.append("]");
        buffer.append(log);

        return buffer.toString();
    }

    private static void getMethodNames() {
        // StackTraceElement[] sElements = new Throwable().getStackTrace();

        StackTraceElement[] sElements = Thread.currentThread().getStackTrace();
        if (sElements.length >= 6) {
            // 5 is the external function level
            mClassName = sElements[5].getFileName();
            mMethodName = sElements[5].getMethodName();
            mLineNumber = sElements[5].getLineNumber();
        }

    }

    /**
     * Get the final tag from the tag.
     */
    private static String getCurrentTag(String tag) {
        if (!TextUtils.isEmpty(tag)) {
            return tag;
        }

        if (!TextUtils.isEmpty(mClassName)) {
            return mClassName;
        }

        if (!TextUtils.isEmpty(GLOBAL_TAG)) {
            return GLOBAL_TAG;
        }

        return null;
    }

    /**
     * write the log messages to the console.
     */
    protected static void log2Console(LEVEL level, String tag, String msg, Throwable thr) {
        switch (level) {
            case VERBOSE:
                if (thr == null) {
                    android.util.Log.v(tag, msg);
                } else {
                    android.util.Log.v(tag, msg, thr);
                }
                break;
            case DEBUG:
                if (thr == null) {
                    android.util.Log.d(tag, msg);
                } else {
                    android.util.Log.d(tag, msg, thr);
                }
                break;
            case INFO:
                if (thr == null) {
                    android.util.Log.i(tag, msg);
                } else {
                    android.util.Log.i(tag, msg, thr);
                }
                break;
            case WARN:
                if (thr == null) {
                    android.util.Log.w(tag, msg);
                } else if (TextUtils.isEmpty(msg)) {
                    android.util.Log.w(tag, thr);
                } else {
                    android.util.Log.w(tag, msg, thr);
                }
                break;
            case ERROR:
                if (thr == null) {
                    android.util.Log.e(tag, msg);
                } else {
                    android.util.Log.e(tag, msg, thr);
                }
                break;
            case ASSERT:
                if (thr == null) {
                    android.util.Log.wtf(tag, msg);
                } else if (TextUtils.isEmpty(msg)) {
                    android.util.Log.wtf(tag, thr);
                } else {
                    android.util.Log.wtf(tag, msg, thr);
                }
                break;
            default:
                break;
        }
    }

    /**
     * write the log messages to the file.
     */
    private static void log2File(LEVEL level, String tag, String msg, Throwable tr) {
        if (mGenerator == null) {
            mGenerator = new FilePathGenerator.DefaultFilePathGenerator("", "", "");
        }

        if (mFormatter == null) {
            mFormatter = new LogFormatter.IDEAFormatter();
        }

        boolean isFilter = false;

        if (mFilters != null) {
            for (LogFilter f : mFilters) {
                if (f.filter(level, tag, msg)) {
                    isFilter = true;
                    break;
                }
            }
        }

        if (!isFilter) {
            Log2File.log2file(mGenerator.getPath(), mFormatter.format(level, tag, msg, tr));
        }
    }

    /**
     * Get the ExecutorService
     *
     * @return the ExecutorService
     */
    public static ExecutorService getExecutor() {
        return Log2File.getExecutor();
    }

    /**
     * Set the ExecutorService
     *
     * @param executor the ExecutorService
     */
    public static void setExecutor(ExecutorService executor) {
        Log2File.setExecutor(executor);
    }

    /**
     * get the log file path
     *
     * @return path
     */
    public static String getPath() {
        if (mGenerator == null) {
            return null;
        }

        return mGenerator.getPath();
    }

    /**
     * get the FilePathGenerator
     *
     * @return the FilePathGenerator
     */
    public static FilePathGenerator getFilePathGenerator() {
        return mGenerator;
    }

    /**
     * set the FilePathGenerator
     *
     * @param generator the FilePathGenerator
     */
    public static void setFilePathGenerator(FilePathGenerator generator) {
        LogUtil.mGenerator = generator;
    }

    /**
     * get the log formatter
     */
    public static LogFormatter getLogFormatter() {
        return mFormatter;
    }

    /**
     * set the log formatter
     */
    public static void setLogFormatter(LogFormatter formatter) {
        LogUtil.mFormatter = formatter;
    }

    /**
     * add log filter
     *
     * each one kind
     *
     * @return true if added successfully,else the filter is null or any filter with the same kind
     * has been added.
     */
    public static boolean addLogFilter(LogFilter filter) {
        boolean ret = true;

        if (filter == null) {
            ret = false;
            return ret;
        }

        if (mFilters == null) {
            mFilters = new ArrayList<LogFilter>();
        }

        for (LogFilter f : mFilters) {
            if (filter.getClass().getName().equals(f.getClass().getName())) {
                ret = false;
                break;
            }
        }

        if (ret) {
            mFilters.add(filter);
        }

        return ret;
    }

    /**
     * get the log filters
     */
    public static List<LogFilter> getLogFilters() {
        return mFilters;
    }

    /**
     * remove the log filter
     */
    public static void removeLogFilter(LogFilter filter) {
        if (filter == null || mFilters == null || mFilters.isEmpty()) {
            return;
        }

        if (mFilters.contains(filter)) {
            mFilters.remove(filter);
        }
    }

    /**
     * remove all the log filters which have been added before.
     */
    public static void clearLogFilters() {
        if (mFilters == null || mFilters.isEmpty()) {
            return;
        }

        mFilters.clear();
    }

    /**
     * is the log enabled
     */
    public static boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * enable or disable the log, the default value is true.
     *
     * @param enabled whether to enable the log
     */
    public static void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    /**
     * is the Log2Console enabled
     */
    public static boolean isLog2ConsoleEnabled() {
        return mIsLog2ConsoleEnabled;
    }

    /**
     * enable or disable writing the log to the console. the default value is true.
     *
     * @param enabled whether to enable the log
     */
    public static void setLog2ConsoleEnabled(boolean enabled) {
        mIsLog2ConsoleEnabled = enabled;
    }

    /**
     * is the Log2Console enabled
     */
    public static boolean isLog2FileEnabled() {
        return mIsLog2FileEnabled;
    }

    /**
     * enable or disable writing the log to the file. the default value is false.
     *
     * @param enabled whether to enable the log
     */
    public static void setLog2FileEnabled(boolean enabled) {
        mIsLog2FileEnabled = enabled;
    }

    /**
     * Checks to see whether or not a log for the specified tag is loggable at the specified level.
     * The default level of any tag is set to INFO. This means that any level above and including
     * INFO will be logged. Before you make any calls to a logging method you should check to see if
     * your tag should be logged.
     *
     * @param tag   The tag to check
     * @param level The level to check
     * @return Whether or not that this is allowed to be logged.
     */
    public static boolean isLoggable(String tag, int level) {
        return android.util.Log.isLoggable(tag, level);
    }

    /**
     * Low-level logging call.
     *
     * @param priority The priority/type of this log message
     * @param tag      Used to identify the source of a log message. It usually identifies the class
     *                 or activity where the log call occurs.
     * @param msg      The message you would like logged.
     * @return The number of bytes written.
     */
    public static int println(int priority, String tag, String msg) {
        return android.util.Log.println(priority, tag, msg);
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable
     *
     * @param tr An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        return android.util.Log.getStackTraceString(tr);
    }

    /**
     * Get the Tag of the application
     */
    public static String getGlobalTag() {
        return GLOBAL_TAG;
    }

    /**
     * Set the Tag of the application
     *
     * @param tag the Tag of the application
     */
    public static void setGlobalTag(String tag) {
        GLOBAL_TAG = tag;
    }

    /**
     * Send a DEBUG log message.
     *
     * @param msg The message you would like logged.
     */
    public static void d(String tag, Object... msg) {
        String concatLog = TextUtils.join(" , ", msg);
        log(LEVEL.DEBUG, tag, concatLog, null);
    }

    /**
     * Send a DEBUG log message.
     */
    public static void d(String msg) {
        log(LEVEL.DEBUG, null, msg, null);
    }

    /**
     * Send a DEBUG log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void d(String tag, String msg, Throwable thr) {
        log(LEVEL.DEBUG, tag, msg, thr);
    }

    /**
     * Send a DEBUG log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void d(String msg, Throwable thr) {
        log(LEVEL.DEBUG, null, msg, thr);
    }

    /**
     * Send a ERROR log message.
     *
     * @param msg The message you would like logged.
     */
    public static void e(String tag, Object... msg) {
        String concatMsg = TextUtils.join(",", msg);
        log(LEVEL.ERROR, tag, concatMsg, null);
    }

    /**
     * Send an ERROR log message.
     *
     * @param msg The message you would like logged.
     */
    public static void e(String msg) {
        log(LEVEL.ERROR, null, msg, null);
    }

    /**
     * Send a ERROR log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void e(String tag, String msg, Throwable thr) {
        log(LEVEL.ERROR, tag, msg, thr);
    }

    /**
     * Send an ERROR log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void e(String msg, Throwable thr) {
        log(LEVEL.ERROR, null, msg, thr);
    }

    /**
     * Send a INFO log message.
     *
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
        log(LEVEL.INFO, tag, msg, null);
    }

    /**
     * Send an INFO log message.
     *
     * @param msg The message you would like logged.
     */
    public static void i(String msg) {
        log(LEVEL.INFO, null, msg, null);
    }

    /**
     * Send a INFO log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void i(String tag, String msg, Throwable thr) {
        log(LEVEL.INFO, tag, msg, thr);
    }

    /**
     * Send a INFO log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void i(String msg, Throwable thr) {
        log(LEVEL.INFO, null, msg, thr);
    }

    /**
     * Send a VERBOSE log message.
     *
     * @param msg The message you would like logged.
     */
    public static void v(String tag, String msg) {
        log(LEVEL.VERBOSE, tag, msg, null);
    }

    /**
     * Send a VERBOSE log message.
     *
     * @param msg The message you would like logged.
     */
    public static void v(String msg) {
        log(LEVEL.VERBOSE, null, msg, null);
    }

    /**
     * Send a VERBOSE log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void v(String tag, String msg, Throwable thr) {
        log(LEVEL.VERBOSE, tag, msg, thr);
    }

    /**
     * Send a VERBOSE log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void v(String msg, Throwable thr) {
        log(LEVEL.VERBOSE, null, msg, thr);
    }

    /**
     * Send an empty WARN log message and log the exception.
     *
     * @param thr An exception to log
     */
    public static void w(Throwable thr) {
        log(LEVEL.WARN, null, null, thr);
    }

    /**
     * Send a WARN log message.
     *
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
        log(LEVEL.WARN, tag, msg, null);
    }

    /**
     * Send a WARN log message
     *
     * @param msg The message you would like logged.
     */
    public static void w(String msg) {
        log(LEVEL.WARN, null, msg, null);
    }

    /**
     * Send a WARN log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void w(String tag, String msg, Throwable thr) {
        log(LEVEL.WARN, tag, msg, thr);
    }

    /**
     * Send a WARN log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void w(String msg, Throwable thr) {
        log(LEVEL.WARN, null, msg, thr);
    }

    /**
     * Send an empty What a Terrible Failure log message and log the exception.
     *
     * @param thr An exception to log
     */
    public static void wtf(Throwable thr) {
        log(LEVEL.ASSERT, null, null, thr);
    }

    /**
     * Send a What a Terrible Failure log message.
     *
     * @param msg The message you would like logged.
     */
    public static void wtf(String tag, String msg) {
        log(LEVEL.ASSERT, tag, msg, null);
    }

    /**
     * Send a What a Terrible Failure log message
     *
     * @param msg The message you would like logged.
     */
    public static void wtf(String msg) {
        log(LEVEL.ASSERT, null, msg, null);
    }

    /**
     * Send a What a Terrible Failure log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void wtf(String tag, String msg, Throwable thr) {
        log(LEVEL.ASSERT, tag, msg, thr);
    }

    /**
     * Send a What a Terrible Failure log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param thr An exception to log
     */
    public static void wtf(String msg, Throwable thr) {
        log(LEVEL.ASSERT, null, msg, thr);
    }

    public enum LEVEL {
        VERBOSE(2, "V"), DEBUG(3, "D"), INFO(4, "I"), WARN(5, "W"), ERROR(6, "E"), ASSERT(7, "A");

        final String levelString;
        final int level;

        // Supress default constructor for noninstantiability
        private LEVEL() {
            throw new AssertionError();
        }

        private LEVEL(int level, String levelString) {
            this.level = level;
            this.levelString = levelString;
        }

        public String getLevelString() {
            return this.levelString;
        }

        public int getLevel() {
            return this.level;
        }
    }
}
