package com.starcor.xuldemo.utils.log;

import android.text.TextUtils;

import java.security.InvalidParameterException;

/**
 * Decide which log will be written to the file.
 *
 * @author zhangfeibiao
 */
public abstract class LogFilter {

    /**
     * if true, filter the log.
     */
    public abstract boolean filter(LogUtil.LEVEL level, String tag, String msg);

    public static class TagFilter extends LogFilter {

        private String mTag = null;

        /**
         * set the tag which will not be filtered.
         *
         * if the tag is null or empty, then nothing will be filtered..
         */
        public TagFilter(String tag) {
            this.mTag = tag;
        }

        @Override
        public boolean filter(LogUtil.LEVEL level, String tag, String msg) {
            if (TextUtils.isEmpty(this.mTag)) {
                return false;
            }

            if (this.mTag.equals(tag)) {
                return false;
            }

            return true;
        }

    }

    public static class LevelFilter extends LogFilter {

        private LogUtil.LEVEL mLevel = null;

        /**
         * set the Level.Any log with the level below it will be filtered.
         *
         * @param level the minimum level which will not be filtered.
         */
        public LevelFilter(LogUtil.LEVEL level) {
            if (level == null) {
                throw new InvalidParameterException("level is null or not valid.");
            }

            this.mLevel = level;
        }

        @Override
        public boolean filter(LogUtil.LEVEL level, String tag, String msg) {
            return level.getLevel() < this.mLevel.getLevel();
        }
    }

    public static class ContentFilter extends LogFilter {

        private String mMsg = null;

        public ContentFilter(String msg) {
            this.mMsg = msg;
        }

        @Override
        public boolean filter(LogUtil.LEVEL level, String tag, String msg) {
            if (level == null || TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)) {
                return true;
            }

            if (TextUtils.isEmpty(this.mMsg)) {
                return false;
            }

            if (tag.contains(this.mMsg) || msg.contains(this.mMsg)) {
                return false;
            }

            return true;
        }
    }
}
