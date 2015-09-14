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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Video;
import android.support.v4.provider.DocumentFile;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;
import base.util.PreferenceDefault;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.FileManagerActivity;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.intents.FileManagerIntents;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
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
     * use it to calculate file count in the directory recursively
     */
    private static int fileCount = 0;

    /**
     * Whether the filename is a video file.
     *
     * @param filename
     * @return
     */
    /*
    public static boolean isVideo(String filename) {
	String mimeType = getMimeType(filename);
	if (mimeType != null && mimeType.startsWith("video/")) {
		return true;
	} else {
		return false;
	}
	}*/

    /**
     * Whether the URI is a local one.
     *
     * @param uri
     * @return
     */
    public static boolean isLocal(String uri) {
        if (uri != null && !uri.startsWith("http://")) {
            return true;
        }
        return false;
    }

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     *
     * @param uri
     * @return Extension including the dot("."); "" if there is no extension;
     * null if uri was null.
     */
    public static String getExtension(String uri) {
        if (uri == null) {
            return null;
        }

        int dot = uri.lastIndexOf(".");
        if (dot >= 0) {
            return uri.substring(dot);
        } else {
            // No extension.
            return "";
        }
    }

    /**
     * Returns true if uri is a media uri.
     *
     * @param uri
     * @return
     */
    public static boolean isMediaUri(String uri) {
        if (uri.startsWith(Audio.Media.INTERNAL_CONTENT_URI.toString())
                || uri.startsWith(Audio.Media.EXTERNAL_CONTENT_URI.toString())
                || uri.startsWith(Video.Media.INTERNAL_CONTENT_URI.toString())
                || uri.startsWith(Video.Media.EXTERNAL_CONTENT_URI.toString())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Convert File into Uri.
     *
     * @param file
     * @return uri
     */
    public static Uri getUri(File file) {
        if (file != null) {
            return Uri.fromFile(file);
        }
        return null;
    }

    /**
     * Convert Uri into File.
     *
     * @param uri
     * @return file
     */
    public static File getFile(Uri uri) {
        if (uri != null) {
            String filepath = uri.getPath();
            if (filepath != null) {
                return new File(filepath);
            }
        }
        return null;
    }

    /**
     * Returns the path only (without file name).
     *
     * @param file
     * @return
     */
    public static File getPathWithoutFilename(File file) {
        if (file != null) {
            if (file.isDirectory()) {
                // no file to be split off. Return everything
                return file;
            } else {
                String filename = file.getName();
                String filepath = file.getAbsolutePath();

                // Construct path without file name.
                String pathwithoutname = filepath.substring(0,
                        filepath.length() - filename.length());
                if (pathwithoutname.endsWith("/")) {
                    pathwithoutname = pathwithoutname.substring(0,
                            pathwithoutname.length() - 1);
                }
                return new File(pathwithoutname);
            }
        }
        return null;
    }

    /**
     * Constructs a file from a path and file name.
     *
     * @param curdir
     * @param file
     * @return
     */
    public static File getFile(String curdir, String file) {
        String separator = "/";
        if (curdir.endsWith("/")) {
            separator = "";
        }
        File clickedFile = new File(curdir + separator + file);
        return clickedFile;
    }

    public static File getFile(File curdir, String file) {
        return getFile(curdir.getAbsolutePath(), file);
    }

    public static String formatSize(Context context, long sizeInBytes) {
        return Formatter.formatFileSize(context, sizeInBytes);
    }

    public static long folderSize(File directory) {
        long length = 0;
        File[] files = directory.listFiles();
        if (files != null)
            for (File file : files)
                if (file.isFile())
                    length += file.length();
                else
                    length += folderSize(file);
        return length;
    }

    public static String formatDate(Context context, long dateTime) {
        return DateFormat.getDateFormat(context).format(new Date(dateTime));
    }

    public static int getFileCount(File file) {
        fileCount = 0;
        calculateFileCount(file);
        return fileCount;
    }

    /**
     * @param f - file which need be checked
     * @return if is archive - returns true othewise
     */
    public static boolean checkIfZipArchive(File f) {
        int l = f.getName().length();
        // TODO test
        if (f.isFile()
                && FileUtils.getExtension(f.getAbsolutePath()).equals(".zip"))
            return true;
        return false;

        // Old way. REALLY slow. Too slow for realtime action loading.
        //        try {
        //            new ZipFile(f);
        //            return true;
        //        } catch (Exception e){
        //            return false;
        //        }
    }

    /**
     * Recursively count all files in the <code>file</code>'s subtree.
     *
     * @param file The root of the tree to count.
     */
    private static void calculateFileCount(File file) {
        if (!file.isDirectory()) {
            fileCount++;
            return;
        }
        if (file.list() == null) {
            return;
        }
        for (String fileName : file.list()) {
            File f = new File(file.getAbsolutePath() + File.separator
                    + fileName);
            calculateFileCount(f);
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
        File file = FileUtils.getFile(path, fileName);

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
        file = FileUtils.getFile(
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
            file = FileUtils.getFile(
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

        Uri data = FileUtils.getUri(fileholder.getFile());
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
                        - getExtension(getUri(f).toString()).length());
    }

    public static void locateFile(Context context, String uri, String title) {
        locateFile(context, new File(uri), title);
    }

    public static void locateFile(Context context, String uri) {
        locateFile(context, new File(uri), null);
    }

    public static void locateFile(Context context, File file, String title) {
        try {
            File path = FileUtils.getPathWithoutFilename(file);
            Bundle bundle = new Bundle();
            bundle.putString(FileManagerActivity.EXTRA_FILE_URI,
                    path.getAbsolutePath());
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
        try {
            File path = FileUtils.getPathWithoutFilename(file);
            Bundle bundle = new Bundle();
            bundle.putString(FileManagerActivity.EXTRA_FILE_URI,
                    path.getAbsolutePath());
            if (keyword != null) {
                bundle.putString(FileManagerActivity.EXTRA_PATH_KEYWORD, keyword);
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


    public static final int REQUEST_STORAGE_CODE = 21;
    /**
     * The name of the primary volume (LOLLIPOP).
     */
    private static final String PRIMARY_VOLUME_NAME = "primary";

    /**
     * Get an Uri from an file path.
     *
     * @param path The file path.
     * @return The Uri.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static Uri getUriFromFile(final String path, Context context) {
        ContentResolver resolver = context.getContentResolver();

        Cursor filecursor = resolver.query(MediaStore.Files.getContentUri("external"),
                new String[]{BaseColumns._ID}, MediaStore.MediaColumns.DATA + " = ?",
                new String[]{path}, MediaStore.MediaColumns.DATE_ADDED + " desc");
        filecursor.moveToFirst();

        if (filecursor.isAfterLast()) {
            filecursor.close();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, path);
            return resolver.insert(MediaStore.Files.getContentUri("external"), values);
        } else {
            int imageId = filecursor.getInt(filecursor.getColumnIndex(BaseColumns._ID));
            Uri uri = MediaStore.Files.getContentUri("external").buildUpon().appendPath(
                    Integer.toString(imageId)).build();
            filecursor.close();
            return uri;
        }
    }

    /**
     * Copy a file. The target file may even be on external SD card for Kitkat.
     *
     * @param source The source file
     * @param target The target file
     * @return true if the copying was successful.
     */
    public static boolean copyFile(final File source, final File target, Context context) {
        FileInputStream inStream = null;
        OutputStream outStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inStream = new FileInputStream(source);

            // First try the normal way
            if (isWritable(target)) {
                // standard way
                outStream = new FileOutputStream(target);
                inChannel = inStream.getChannel();
                outChannel = ((FileOutputStream) outStream).getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    DocumentFile targetDocument = getDocumentFile(target, false, true, context);
                    outStream =
                            context.getContentResolver().openOutputStream(targetDocument.getUri());
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    Uri uri = getUriFromFile(target.getAbsolutePath(), context);
                    outStream = context.getContentResolver().openOutputStream(uri);
                } else {
                    return false;
                }

                if (outStream != null) {
                    // Both for SAF and for Kitkat, write to output stream.
                    byte[] buffer = new byte[4096]; // MAGIC_NUMBER
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }

            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
            return false;
        } finally {
            try {
                inStream.close();
            } catch (Exception e) {
                // ignore exception
            }
            try {
                outStream.close();
            } catch (Exception e) {
                // ignore exception
            }
            try {
                inChannel.close();
            } catch (Exception e) {
                // ignore exception
            }
            try {
                outChannel.close();
            } catch (Exception e) {
                // ignore exception
            }
        }
        return true;
    }

    /**
     * Delete a file. May be even on external SD card.
     *
     * @param file the file to be deleted.
     * @return True if successfully deleted.
     */
    public static boolean deleteFile(final File file, Context context) {
        // First try the normal deletion.
        if (file.delete()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile document = getDocumentFile(file, false, true, context);
            if (document == null) {
                return false;
            }
            return document.delete();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            ContentResolver resolver = context.getContentResolver();

            try {
                Uri uri = getUriFromFile(file.getAbsolutePath(), context);
                resolver.delete(uri, null, null);
                return !file.exists();
            } catch (Exception e) {
                Log.e(TAG, "Error when deleting file " + file.getAbsolutePath(), e);
                return false;
            }
        }

        return !file.exists();
    }

    /**
     * Delete all files in a folder.
     *
     * @param folder the folder
     * @return true if successful.
     */
    public static boolean deleteFilesInFolder(final File folder, Context context) {
        boolean totalSuccess = true;

        String[] children = folder.list();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                File file = new File(folder, children[i]);
                if (!file.isDirectory()) {
                    boolean success = FileUtils.deleteFile(file, context);
                    if (!success) {
                        Log.w(TAG, "Failed to delete file" + children[i]);
                        totalSuccess = false;
                    }
                }
            }
        }
        return totalSuccess;
    }

    /**
     * Move a file. The target file may even be on external SD card.
     *
     * @param source The source file
     * @param target The target file
     * @return true if the copying was successful.
     */
    public static boolean moveFile(final File source, final File target, Context context) {
        // First try the normal rename.
        if (source.renameTo(target)) {
            return true;
        }

        boolean success = copyFile(source, target, context.getApplicationContext());
        if (success) {
            success = deleteFile(source, context);
        }
        return success;
    }

    /**
     * Determine if a file is on external sd card. (Kitkat or higher.)
     *
     * @param file The file.
     * @return true if on external sd card.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isOnExtSdCard(final File file, Context context) {
        return getExtSdCardFolder(file, context.getApplicationContext()) != null;
    }

    /**
     * Check for a directory if it is possible to create files within this directory, either via normal writing or via
     * Storage Access Framework.
     *
     * @param folder The directory
     * @return true if it is possible to write in this directory.
     */
    public static boolean isWritableNormalOrSaf(final File folder, Context context) {
        // Verify that this is a directory.
        if (!folder.exists() || !folder.isDirectory()) {
            return false;
        }

        // Find a non-existing file in this directory.
        int i = 0;
        File file;
        do {
            String fileName = "AugendiagnoseDummyFile" + (++i);
            file = new File(folder, fileName);
        }
        while (file.exists());

        // First check regular writability
        if (isWritable(file)) {
            return true;
        }

        // Next check SAF writability.
        DocumentFile document = getDocumentFile(file, false, false, context.getApplicationContext());

        if (document == null) {
            return false;
        }

        // This should have created the file - otherwise something is wrong with access URL.
        boolean result = document.canWrite() && file.exists();

        // Ensure that the dummy file is not remaining.
        document.delete();

        return result;
    }

    /**
     * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5). If the file is not
     * existing, it is created.
     *
     * @param file              The file.
     * @param isDirectory       flag indicating if the file should be a directory.
     * @param createDirectories flag indicating if intermediate path directories should be created if not existing.
     * @return The DocumentFile
     */
    public static DocumentFile getDocumentFile(final File file, final boolean isDirectory,
                                               final boolean createDirectories, Context context) {
        Context appContext = context.getApplicationContext();
        Uri[] treeUris = PreferenceDefault.getTreeUris(context);
        Uri treeUri = null;

        if (treeUris == null || treeUris.length == 0) {
            return null;
        }

        String fullPath = null;
        try {
            fullPath = file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }

        String baseFolder = null;

        // First try to get the base folder via unofficial StorageVolume API from the URIs.

        for (int i = 0; baseFolder == null && i < treeUris.length; i++) {
            String treeBase = getFullPathFromTreeUri(treeUris[i], context);
            if (fullPath.startsWith(treeBase)) {
                treeUri = treeUris[i];
                baseFolder = treeBase;
            }
        }

        if (baseFolder == null) {
            // Alternatively, take root folder from device and assume that base URI works.
            treeUri = treeUris[0];
            baseFolder = getExtSdCardFolder(file, context);
        }

        if (baseFolder == null) {
            return null;
        }

        String relativePath = fullPath.substring(baseFolder.length() + 1);

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(appContext, treeUri);

        String[] parts = relativePath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);

            if (nextDocument == null) {
                if (i < parts.length - 1) {
                    if (createDirectories) {
                        nextDocument = document.createDirectory(parts[i]);
                    } else {
                        return null;
                    }
                } else if (isDirectory) {
                    nextDocument = document.createDirectory(parts[i]);
                } else {
                    nextDocument = document.createFile("", parts[i]);
                }
            }
            document = nextDocument;
        }

        return document;
    }


    /**
     * Get the full path of a document from its tree URI.
     *
     * @param treeUri The tree RI.
     * @return The path (without trailing file separator).
     */
    public static String getFullPathFromTreeUri(final Uri treeUri, Context context) {
        if (treeUri == null) {
            return null;
        }
        String volumePath = FileUtils.getVolumePath(FileUtils.getVolumeIdFromTreeUri(treeUri), context);
        if (volumePath == null) {
            return File.separator;
        }
        if (volumePath.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length() - 1);
        }

        String documentPath = FileUtils.getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }

        if (documentPath != null && documentPath.length() > 0) {
            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath;
            } else {
                return volumePath + File.separator + documentPath;
            }
        } else {
            return volumePath;
        }
    }

    /**
     * Get the document path (relative to volume name) for a tree URI (LOLLIPOP).
     *
     * @param treeUri The tree URI.
     * @return the document path.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) {
            return split[1];
        } else {
            return File.separator;
        }
    }

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
     * null is returned.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getExtSdCardFolder(final File file, Context context) {
        String[] extSdPaths = getExtSdCardPaths(context);
        try {
            for (int i = 0; i < extSdPaths.length; i++) {
                if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
                    return extSdPaths[i];
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String[] getExtSdCardPaths(Context context) {
        List<String> paths = new ArrayList<String>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w(TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        return paths.toArray(new String[0]);
    }

    /**
     * Get the volume ID from the tree URI.
     *
     * @param treeUri The tree URI.
     * @return The volume ID.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");

        if (split.length > 0) {
            return split[0];
        } else {
            return null;
        }
    }

    /**
     * Get the path of a certain volume.
     *
     * @param volumeId The volume id.
     * @return The path.
     */
    private static String getVolumePath(final String volumeId, Context context) {
        if (Build.VERSION.SDK_INT < 21) {
            return null;
        }

        try {
            StorageManager mStorageManager =
                    (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary.booleanValue() && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid.equals(volumeId)) {
                        return (String) getPath.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Check is a file is writable. Detects write issues on external SD card.
     *
     * @param file The file
     * @return true if the file is writable.
     */
    public static boolean isWritable(final File file) {
        boolean isExisting = file.exists();

        try {
            FileOutputStream output = new FileOutputStream(file, true);
            try {
                output.close();
            } catch (IOException e) {
                // do nothing.
            }
        } catch (FileNotFoundException e) {
            return false;
        }
        boolean result = file.canWrite();

        // Ensure that file is not created during this process.
        if (!isExisting) {
            file.delete();
        }

        return result;
    }

    public static boolean isAndroid5() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean checkSdCardAndroid5(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (PreferenceDefault.getTreeUris(context.getApplicationContext()).length == 0) {
                return true;
            } else {
                String path = FileUtils.getFullPathFromTreeUri(PreferenceDefault.getTreeUris(context)[0], context.getApplicationContext());
                return !isWritableNormalOrSaf(new File(path), context);
            }
        }

        return false;
    }

    public static String getSelectedExSdCardPath(Context context) {
        return FileUtils.getFullPathFromTreeUri(PreferenceDefault.getTreeUris(context)[0], context.getApplicationContext());
    }

    public static void showStorageAccessDialog(final android.support.v4.app.Fragment fragment) {
        if (null == fragment) {
            return;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(fragment.getActivity())
                .title(imoblife.toolbox.full.baseresources.R.string.require_exsd_access_title)
                .customView(imoblife.toolbox.full.baseresources.R.layout.require_sd_access_layout, false)
                .positiveText(imoblife.toolbox.full.baseresources.R.string.require_exsd_access_confirm)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        try {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                            fragment.startActivityForResult(intent, REQUEST_STORAGE_CODE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .build();
        dialog.show();
    }

    public static void showStorageAccessDialog(final Activity activity) {
        if (null == activity) {
            return;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .title(imoblife.toolbox.full.baseresources.R.string.require_exsd_access_title)
                .customView(imoblife.toolbox.full.baseresources.R.layout.require_sd_access_layout, false)
                .positiveText(imoblife.toolbox.full.baseresources.R.string.require_exsd_access_confirm)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        try {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                            activity.startActivityForResult(intent, REQUEST_STORAGE_CODE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .build();
        dialog.show();
    }

}
