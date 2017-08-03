package com.starcor.xuldemo.utils.log;

import android.text.TextUtils;

import java.text.SimpleDateFormat;

/**
 * Decide the format of log will be written to the file.
 *
 * @author zhangfeibiao
 */
public abstract class LogFormatter {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";

    /**
     * format the log.
     */
    public abstract String format(LogUtil.LEVEL level, String tag, String msg, Throwable tr);

    /**
     * Eclipse Style
     */
    public static class EclipseFormatter extends LogFormatter {

        private final SimpleDateFormat mFormatter;

        public EclipseFormatter() {
            mFormatter = new SimpleDateFormat(DATE_FORMAT);
        }

        public EclipseFormatter(String formatOfTime) {
            String formatStr = formatOfTime;
            if (TextUtils.isEmpty(formatStr)) {
                formatStr = DATE_FORMAT;
            }

            mFormatter = new SimpleDateFormat(formatStr);
        }

        @Override
        public String format(LogUtil.LEVEL level, String tag, String msg, Throwable tr) {
            if (level == null || TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)) {
                return "";
            }

            StringBuffer buffer = new StringBuffer();
            buffer.append(level.getLevelString());
            buffer.append("\t");
            buffer.append(mFormatter.format(System.currentTimeMillis()));
            buffer.append("\t");
            buffer.append(android.os.Process.myPid());
            buffer.append("\t");
            buffer.append(android.os.Process.myTid());
            buffer.append("\t");
            buffer.append(tag);
            buffer.append("\t");
            buffer.append(msg);
            if (tr != null) {
                buffer.append(System.getProperty("line.separator"));
                buffer.append(android.util.Log.getStackTraceString(tr));
            }

            return buffer.toString();
        }
    }

    /**
     * IDEA Style
     */
    public static class IDEAFormatter extends LogFormatter {

        private final SimpleDateFormat mFormatter;

        public IDEAFormatter() {
            mFormatter = new SimpleDateFormat(DATE_FORMAT);
        }

        public IDEAFormatter(String formatOfTime) {
            String formatStr = formatOfTime;
            if (TextUtils.isEmpty(formatStr)) {
                formatStr = DATE_FORMAT;
            }

            mFormatter = new SimpleDateFormat(formatStr);
        }

        @Override
        public String format(LogUtil.LEVEL level, String tag, String msg, Throwable tr) {
            if (level == null || TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)) {
                return "";
            }

            StringBuffer buffer = new StringBuffer();
            buffer.append(mFormatter.format(System.currentTimeMillis()));
            buffer.append("\t");
            buffer.append(android.os.Process.myPid());
            buffer.append("-");
            buffer.append(android.os.Process.myTid());
            buffer.append("/?");
            buffer.append("\t");
            buffer.append(level.getLevelString());
            buffer.append("/");
            buffer.append(tag);
            buffer.append(":");
            buffer.append("\t");
            buffer.append(msg);
            if (tr != null) {
                buffer.append(System.getProperty("line.separator"));
                buffer.append(android.util.Log.getStackTraceString(tr));
            }

            return buffer.toString();
        }
    }
}
