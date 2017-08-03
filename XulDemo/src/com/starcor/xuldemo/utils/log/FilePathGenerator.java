package com.starcor.xuldemo.utils.log;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Decide the absolute path of the file which log will be written to.
 *
 * @author zhangfeibiao
 */
public abstract class FilePathGenerator {

    // Log path
    protected static final String DEFAULT_LOG_DIR = "/starcor/log";
    protected static final String DEFAULT_LOG_FILE_NAME = "starcor";
    protected static final String DEFAULT_LOG_SUFFIX = ".log";
    protected static final int DEFAULT_LOG_MAX_SIZE = 2 * 1024 * 1024; // 2M

    protected String mPath = null;
    protected String mRootDir = Environment.getExternalStorageDirectory().getPath();
    protected String mDir = mRootDir + DEFAULT_LOG_DIR;
    protected File mFile = null;

    /**
     * if not set, LOG_FILE_NAME will be used.
     */
    protected String mFilename = DEFAULT_LOG_FILE_NAME;

    /**
     * if not set, DEFAULT_LOG_SUFFIX will be used.
     */
    protected String mSuffix = DEFAULT_LOG_SUFFIX;

    // Supress default constructor for noninstantiability
    @SuppressWarnings("unused")
    private FilePathGenerator() {
        throw new AssertionError();
    }

    /**
     * dir, filename, suffix, all will be default value.
     */
    public FilePathGenerator(Context context) {
        if (context == null) {
            throw new NullPointerException("The Context should not be null.");
        }
    }

    /**
     * filename will be decided by the param filename and suffix together.
     */
    public FilePathGenerator(Context context, String filename, String suffix) {
        if (context == null) {
            throw new NullPointerException("The Context should not be null.");
        }

        if (!TextUtils.isEmpty(filename)) {
            this.mFilename = filename;
        }

        if (!TextUtils.isEmpty(suffix)) {
            this.mSuffix = suffix;
        }
    }

    /**
     * dir is from the param dir. filename will be decided by the param filename and suffix
     * together.
     */
    public FilePathGenerator(String dir, String filename, String suffix) {
        if (dir != null) {
            this.mDir = dir;
        }

        if (!TextUtils.isEmpty(filename)) {
            this.mFilename = filename;
        }

        if (!TextUtils.isEmpty(suffix)) {
            this.mSuffix = suffix;
        }
    }

    /**
     * Generate the file path of the log.
     *
     * The file path should be absolute.
     *
     * @return the file path of the log
     */
    public abstract String generateFilePath();

    /**
     * Whether to generate the file path of the log.
     *
     * @return if true,generate the file path of the log, otherwise not.
     */
    public abstract Boolean isGenerate();

    /**
     * It is time to generate the new file path of the log. You can get the new and the old file
     * path of the log.
     *
     * The file path should be absolute.
     *
     * @param newPath the new file path
     * @param oldPath the old file path
     */
    public abstract void onGenerate(String newPath, String oldPath);

    /**
     * Get the file path of the log. generate a new file path if needed.
     *
     * @return the file path of the log.
     */
    public final String getPath() {
        if (isGenerate()) {
            String newPath = generateFilePath();

            onGenerate(newPath, mPath);
            mPath = newPath;
        }

        return mPath;
    }

    /**
     * Default FilePathGenerator
     */
    public static class DefaultFilePathGenerator extends FilePathGenerator {

        /**
         * dir, filename, suffix, all will be default value.
         */
        public DefaultFilePathGenerator(Context context) {
            super(context);
        }

        /**
         * dir will be default log directory. filename will be decided by the param filename and
         * suffix together.
         */
        public DefaultFilePathGenerator(Context context, String filename, String suffix) {
            super(context, filename, suffix);

        }

        /**
         * dir is from the param dir. filename will be decided by the param filename and suffix
         * together.
         */
        public DefaultFilePathGenerator(String dir, String filename, String suffix) {
            super(dir, filename, suffix);
        }

        @Override
        public String generateFilePath() {
            String path = null;

            if (TextUtils.isEmpty(mDir)) {
                return path;
            }

            File logDir = new File(mDir);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            mFile = new File(logDir, mFilename + mSuffix);

            if (!mFile.exists()) {
                try {
                    mFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return mFile.getAbsolutePath();
        }

        @Override
        public Boolean isGenerate() {
            return (mFile == null) || !mFile.exists();
        }

        @Override
        public void onGenerate(String newPath, String oldPath) {
        }
    }

    /**
     * Date FilePathGenerator
     */
    public static class DateFilePathGenerator extends FilePathGenerator {

        /**
         * dir, filename, suffix, all will be default value.
         */
        public DateFilePathGenerator(Context context) {
            super(context);
        }

        /**
         * dir will be default log directory. filename will be decided by the param filename and
         * suffix together.
         */
        public DateFilePathGenerator(Context context, String filename, String suffix) {
            super(context, filename, suffix);

        }

        /**
         * dir is from the param dir. filename will be decided by the param filename and suffix
         * together.
         */
        public DateFilePathGenerator(String dir, String filename, String suffix) {
            super(dir, filename, suffix);
        }

        @Override
        public String generateFilePath() {
            String path = null;

            if (TextUtils.isEmpty(mDir)) {
                return path;
            }

            File logDir = new File(mDir);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            Date myDate = new Date();
            SimpleDateFormat fdf = new SimpleDateFormat("yyyy-MM-dd");
            String myDateString = fdf.format(myDate);

            StringBuffer buffer = new StringBuffer();
            buffer.append(mFilename);
            buffer.append("-");
            buffer.append(myDateString);
            buffer.append(mSuffix);

            mFile = new File(logDir, buffer.toString());

            if (!mFile.exists()) {
                try {
                    mFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return mFile.getAbsolutePath();
        }

        @Override
        public Boolean isGenerate() {
            return (mFile == null) || !mFile.exists();
        }

        @Override
        public void onGenerate(String newPath, String oldPath) {
        }
    }

    /**
     * LimitSize FilePathGenerator
     */
    public static class LimitSizeFilePathGenerator extends FilePathGenerator {

        private int maxSize = DEFAULT_LOG_MAX_SIZE;

        /**
         * dir, filename, suffix, maxSize, all will be default value.
         */
        public LimitSizeFilePathGenerator(Context context) {
            super(context);
        }

        /**
         * dir will be default log directory. filename will be decided by the param filename and
         * suffix together.
         */
        public LimitSizeFilePathGenerator(Context context, String filename, String suffix,
                                          int maxSize) {
            super(context, filename, suffix);
            this.maxSize = maxSize;
        }

        /**
         * filename will be decided by the param filename and suffix together.
         */
        public LimitSizeFilePathGenerator(String dir, String filename, String suffix, int maxSize) {
            super(dir, filename, suffix);
            this.maxSize = maxSize;
        }

        @Override
        public String generateFilePath() {
            String path = null;

            if (TextUtils.isEmpty(mDir)) {
                return path;
            }

            File logDir = new File(mDir);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            Date myDate = new Date();

            SimpleDateFormat fdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            String myDateString = fdf.format(myDate);

            StringBuffer buffer = new StringBuffer();
            buffer.append(mFilename);
            buffer.append("-");
            buffer.append(myDateString);
            buffer.append(mSuffix);

            mFile = new File(logDir, buffer.toString());

            if (!mFile.exists()) {
                try {
                    mFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return mFile.getAbsolutePath();
        }

        @Override
        public Boolean isGenerate() {
            return (mFile == null) || !mFile.exists() || mFile.length() >= maxSize;
        }

        @Override
        public void onGenerate(String newPath, String oldPath) {
        }
    }
}
