/* 
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.filemanager.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import base.util.FileUtil;
import com.filemanager.FileManagerActivity;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.intents.FileManagerIntents;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Peli
 * @version 2009-07-03
 */
public class FileUtils {

    /**
     * TAG for log messages.
     */
    static final String TAG = "FileUtils";
    private static final int X_OK = 1;
    public static final String NOMEDIA_FILE_NAME = ".nomedia";

    private static boolean libLoadSuccess;

    static {
        try {
            System.loadLibrary("access");
            libLoadSuccess = true;
        } catch (UnsatisfiedLinkError e) {
            libLoadSuccess = false;
            Log.d(TAG, "libaccess.so failed to load.");
        }
    }

    /**
     * Native helper method, returns whether the current process has execute privilages.
     *
     * @param mContextFile
     * @return returns TRUE if the current process has execute privilages.
     */
    public static boolean canExecute(File mContextFile) {
        try {
            // File.canExecute() was introduced in API 9.  If it doesn't exist, then
            // this will throw an exception and the NDK version will be used.
            Method m = File.class.getMethod("canExecute", new Class[]{});
            Boolean result = (Boolean) m.invoke(mContextFile);
            return result;
        } catch (Exception e) {
            if (libLoadSuccess) {
                return access(mContextFile.getPath(), X_OK);
            } else {
                return false;
            }
        }
    }

    // Native interface to unistd.h's access(*char, int) method.
    public static native boolean access(String path, int mode);

    /**
     * @param path     The path that the file is supposed to be in.
     * @param fileName Desired file name. This name will be modified to create a unique file if necessary.
     * @return A file name that is guaranteed to not exist yet. MAY RETURN NULL!
     */
    public static File createUniqueCopyName(Context context, File path,
                                            String fileName) {
        // Does that file exist?
        File file = FileUtil.getFile(path, fileName);

        if (!file.exists()) {
            // Nope - we can take that.
            return file;
        }

        // Split file's name and extension to fix internationalization issue #307
        int fromIndex = fileName.lastIndexOf('.');
        String extension = "";
        if (fromIndex > 0) {
            extension = fileName.substring(fromIndex);
            fileName = fileName.substring(0, fromIndex);
        }

        // Try a simple "copy of".
        file = FileUtil.getFile(
                path,
                context.getString(R.string.copied_file_name, fileName).concat(
                        extension));

        if (!file.exists()) {
            // Nope - we can take that.
            return file;
        }

        int copyIndex = 2;

        // Well, we gotta find a unique name at some point.
        while (copyIndex < 500) {
            file = FileUtil.getFile(
                    path,
                    context.getString(R.string.copied_file_name_2, copyIndex,
                            fileName).concat(extension));

            if (!file.exists()) {
                // Nope - we can take that.
                return file;
            }

            copyIndex++;
        }

        // I GIVE UP.
        return null;
    }

    /**
     * Attempts to open a file for viewing.
     *
     * @param fileholder The holder of the file to open.
     */
    public static void openFile(FileHolder fileholder, Context c) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);

        Uri data = FileUtil.getUri(fileholder.getFile());
        String type = fileholder.getMimeType();

        if ("*/*".equals(type)) {
            //if it's unknown mime type file, directly open it with other text editors.
            intent.setDataAndType(data, type);
            intent.putExtra(FileManagerIntents.EXTRA_FROM_OI_FILEMANAGER, true);
        } else {
            intent.setDataAndType(data, type);
        }

        try {
            List<ResolveInfo> activities = c.getPackageManager()
                    .queryIntentActivities(intent,
                            PackageManager.GET_ACTIVITIES);
            if (activities.size() == 0
                    || (activities.size() == 1 && c.getApplicationInfo().packageName
                    .equals(activities.get(0).activityInfo.packageName))) {
                Toast.makeText(c, R.string.application_not_available,
                        Toast.LENGTH_SHORT).show();
                return;
            } else {
                c.startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(c.getApplicationContext(),
                    R.string.application_not_available, Toast.LENGTH_SHORT)
                    .show();
        } catch (SecurityException e) {
            Toast.makeText(c.getApplicationContext(),
                    R.string.application_not_available, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public static String getNameWithoutExtension(File f) {
        return f.getName().substring(
                0,
                f.getName().length()
                        - FileUtil.getExtension(FileUtil.getUri(f).toString()).length());
    }

    public static void locateFile(Context context, String uri, String title) {
        locateFile(context, new File(uri), title);
    }

    public static void locateFile(Context context, String uri) {
        locateFile(context, new File(uri), null);
    }

    public static void locateFile(Context context, File file, String title) {
        try {
            File path = FileUtil.getPathWithoutFilename(file);
            Bundle bundle = new Bundle();
            bundle.putString(FileManagerActivity.EXTRA_FILE_URI, path.getAbsolutePath());
            if (title != null) {
                bundle.putString(FileManagerActivity.EXTRA_CHANGE_TITLE, title);
            }
            bundle.putBoolean(FileManagerActivity.EXTRA_PATH_CLICK, false);
            Intent intent = new Intent(context, FileManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtras(bundle);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void locateFileAndHighlight(Context context, File file, String keyword) {
//        try {
            File path = FileUtil.getPathWithoutFilename(file);
            Bundle bundle = new Bundle();
            bundle.putString(FileManagerActivity.EXTRA_FILE_URI, path.getAbsolutePath());
            if (keyword != null) {
                bundle.putString(FileManagerActivity.EXTRA_PATH_KEYWORD, keyword);
            }
            bundle.putBoolean(FileManagerActivity.EXTRA_PATH_CLICK, false);
            Intent intent = new Intent(context, FileManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtras(bundle);
            context.startActivity(intent);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


    public static void locateFolderAndHighlight(Context context, File file, String keyword) {
//        try {
        Bundle bundle = new Bundle();
        bundle.putString(FileManagerActivity.EXTRA_FILE_URI, file.getAbsolutePath());
        if (keyword != null) {
            bundle.putString(FileManagerActivity.EXTRA_PATH_KEYWORD, keyword);
        }
        bundle.putBoolean(FileManagerActivity.EXTRA_PATH_CLICK, false);
        Intent intent = new Intent(context, FileManagerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtras(bundle);
        context.startActivity(intent);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
