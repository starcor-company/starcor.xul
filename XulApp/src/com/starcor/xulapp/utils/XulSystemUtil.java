package com.starcor.xulapp.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import dalvik.system.DexFile;

public class XulSystemUtil {

    private static final String TAG = XulSystemUtil.class.getSimpleName();

    public static final String NULL_OBJECT_STRING = "<null>";

    /**
     * 判断当前线程是否为主线程
     */
    public static boolean isMainThread() {
        return Looper.getMainLooper().getThread().getId() == Thread.currentThread().getId();
    }

    /**
     * 获取StackTraceElement对象
     */
    public static StackTraceElement getStackTrace() {
        return Thread.currentThread().getStackTrace()[4];
    }


    // 基本数据类型
    private final static String[] types = {"int", "java.lang.String", "boolean", "char",
                                           "float", "double", "long", "short", "byte"};

    /**
     * 将对象转化为String
     */
    public static <T> String objectToString(T object) {
        if (object == null) {
            return NULL_OBJECT_STRING;
        }
        if (object.toString().startsWith(object.getClass().getName() + "@")) {
            StringBuilder builder = new StringBuilder(object.getClass().getSimpleName() + "{");
            Field[] fields = object.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                boolean flag = false;
                for (String type : types) {
                    if (field.getType().getName().equalsIgnoreCase(type)) {
                        flag = true;
                        Object value = null;
                        try {
                            value = field.get(object);
                        } catch (IllegalAccessException e) {
                            value = e;
                        } finally {
                            builder.append(String.format("%s=%s, ", field.getName(),
                                                         value == null ? "null"
                                                                       : value.toString()));
                            break;
                        }
                    }
                }
                if (!flag) {
                    builder.append(String.format("%s=%s, ", field.getName(), "Object"));
                }
            }
            return builder.replace(builder.length() - 2, builder.length() - 1, "}").toString();
        } else {
            return object.toString();
        }
    }

    public static String getDiskCacheDir(Context context) {
        String cacheDir;

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
            && !Environment.isExternalStorageRemovable()) {
            cacheDir = getExternalCacheDir(context);
            // 部分机型返回了null
            if (cacheDir == null) {
                cacheDir = getInternalCacheDir(context);
            }
        } else {
            cacheDir = getInternalCacheDir(context);
        }
        if(!checkCacheAvailable(cacheDir)){
            return getBackUpPath(context);
        }
        return cacheDir;
    }

    private static String getBackUpPath(Context context){
        return context.getDir("cache_backup", android.content.Context.MODE_PRIVATE).toString();
    }

    private static boolean checkCacheAvailable(String path){
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        String dir = "test";
        File file = new File(path, dir);
        if (file.exists()) {
            return true;
        }
        if (file.mkdirs() && file.exists()) {
            return true;
        }
        return false;
    }




    /**
     * 获取缓存文件路径(优先选择sd卡)
     *
     * @param context      上下文
     * @param cacheDirName 缓存文件夹名称
     */
    public static File getDiskCacheDir(Context context, String cacheDirName) {
        String cacheDir = getDiskCacheDir(context);
        File dir = new File(cacheDir, cacheDirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static String getExternalCacheDir(Context context) {
        File dir = context.getExternalCacheDir();
        if (dir == null) {
            return null;
        }
        if (!dir.mkdirs() && !dir.exists()) {
            return null;
        }
        return dir.getPath();
    }

    private static String getInternalCacheDir(Context context) {
        File dir = context.getCacheDir();
        if (!dir.mkdirs() && !dir.exists()) {
            return null;
        }
        return dir.getPath();
    }

    /**
     * 获取当前app版本号
     *
     * @param context 上下文
     * @return 当前app版本号
     */
    public static int getAppVersion(Context context) {
        PackageManager manager = context.getPackageManager();
        int code = 0;
        try {
            code = manager.getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            XulLog.e(TAG, e);
        }
        return code;
    }

    // TODO: 2015/10/22 获取正确的version
    public static String getCurrentVersion(Context context) {
        return "test-version";
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     *
     * @param dir 将要删除的文件目录
     * @return boolean Returns "true" if all deletions were successful. If a deletion fails, the
     * method stops attempting to delete and returns "false".
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    /**
     * 获取指定包下的所有类型
     *
     * @param context 当前上下文
     * @param pkgName 指定包完整包名
     * @return 包下的类型集合，未找到则类型集合为empty
     */
    public static Set<String> getClassInPackage(Context context, String pkgName) {
        Set<String> classSet = new HashSet<String>();
        DexFile dexFile = null;
        try {
            dexFile = new DexFile(context.getPackageResourcePath());
            Enumeration<String> classes = dexFile.entries();
            String classPath;
            while (classes.hasMoreElements()) {
                classPath = classes.nextElement();
                if (classPath.startsWith(pkgName)) {
                    // 编译后的类型以$分隔
                    classSet.add(classPath.split("\\$")[0]);
                }
            }
        } catch (IOException e) {
            XulLog.w(TAG, "Parse dex file failed!", e);
        } finally {
            if (dexFile != null) {
                try {
                    dexFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return classSet;
    }

    /**
     * 将给定的毫秒数转换为可读字符串
     *
     * @param milliseconds 待转换的毫秒数
     * @return 该毫秒数转换为 * days * hours * minutes * seconds 后的格式
     */
    public static String formatDuring(long milliseconds) {
        long days = milliseconds / DateUtils.DAY_IN_MILLIS;
        long hours = (milliseconds % DateUtils.DAY_IN_MILLIS) / DateUtils.HOUR_IN_MILLIS;
        long minutes = (milliseconds % DateUtils.HOUR_IN_MILLIS) / DateUtils.MINUTE_IN_MILLIS;
        long seconds = (milliseconds % DateUtils.MINUTE_IN_MILLIS) / DateUtils.SECOND_IN_MILLIS;
        return days + " days " + hours + " hours " + minutes + " minutes "
               + seconds + " seconds ";
    }

    /**
     * 将给定的size大小转换为可读字符串
     *
     * @param size 待转换的size，单位为bytes
     * @return 该size转换为 * M * K * B 后的格式
     */
    public static String formatSize(long size) {
        long megabytes = size / (1024 * 1024);
        long kilobytes = (size % (1024 * 1024)) / 1024;
        long bytes = size % 1024;
        return megabytes + " M " + kilobytes + " K " + bytes + " B ";
    }
}
