package com.filemanager.util;

import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.Toast;

import base.util.CustomToast;
import base.util.FileUtil;
import base.util.PermissionUtil;
import de.greenrobot.event.EventBus;
import imoblife.android.os.ModernAsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.filemanager.R;
import com.filemanager.files.FileHolder;

/**
 * This class helps simplify copying and moving of files and folders by providing a simple interface to the operations and handling the actual operation transparently.
 * @author George Venios
 *
 */
public class CopyHelper {
    private static final int COPY_BUFFER_SIZE = 32 * 1024;
    private static CopyHelper instance;
    private boolean mCancelAction = false;

    public static enum Operation {
        COPY, CUT
    }

    private Context mContext;
    private List<FileHolder> mClipboard;
    private Operation mOperation;
    private OnOperationFinishedListener mListener;
    private OnOperationProgressListener mProgressListener;
    private ModernAsyncTask mActionTask;


    public CopyHelper(Context c) {
        mContext = c;
    }

    public static CopyHelper get(Context c) {
        if (instance == null) {
            instance = new CopyHelper(c.getApplicationContext());
        }
        return instance;
    }

    public int getItemsCount() {
        if (canPaste()) {
            return mClipboard.size();
        } else {
            return 0;
        }
    }

    public void copy(List<FileHolder> tbc) {
        mOperation = Operation.COPY;

        mClipboard = tbc;
    }

    public void copy(FileHolder tbc) {
        ArrayList<FileHolder> tbcl = new ArrayList<FileHolder>();
        tbcl.add(tbc);
        copy(tbcl);
    }

    public void cut(List<FileHolder> tbc) {
        mOperation = Operation.CUT;

        mClipboard = tbc;
    }

    public void cut(FileHolder tbc) {
        ArrayList<FileHolder> tbcl = new ArrayList<FileHolder>();
        tbcl.add(tbc);
        cut(tbcl);
    }

    public void clear() {
        mClipboard.clear();
    }

    /**
     * Call this to check whether there are file references on the clipboard.
     */
    public boolean canPaste() {
        return mClipboard != null && !mClipboard.isEmpty();
    }

    public Operation getOperationType() {
        return mOperation;
    }

    /**
     * Call this to actually copy.
     *
     * @param dest The path to copy the clipboard into.
     * @return false if ANY error has occurred. This may mean that some files have been successfully copied, but not all.
     */
    private boolean performCopy(File dest) {
        boolean res = true;
        int progress = 0;
        try {
            if (PermissionUtil.isAndroid5()) {
                for (FileHolder fh : mClipboard) {

                    if (mCancelAction) {
                        res = false;
                        break;
                    }

                    if (dest.getAbsolutePath().contains(fh.getFile().getAbsolutePath())) {
                        res &= false;
                    } else {
                        if (fh.getFile().isFile()) {
                            res &= copyFileAndroid5(
                                    fh.getFile(),
                                    FileUtils.createUniqueCopyName(mContext, dest,
                                            fh.getName()));
                        } else {
                            res &= copyFolderAndroid5(
                                    fh.getFile(),
                                    FileUtils.createUniqueCopyName(mContext, dest,
                                            fh.getName()));
                        }
                    }

                    if (mProgressListener != null) {
                        mProgressListener.onProgress(progress);
                    }
                    progress++;
                }
            } else {
                for (FileHolder fh : mClipboard) {
                    if (mCancelAction) {
                        res = false;
                        break;
                    }

                    if (dest.getAbsolutePath().contains(fh.getFile().getAbsolutePath())) {
                        res &= false;
                    } else {
                        if (fh.getFile().isFile())
                            res &= copyFile(
                                    fh.getFile(),
                                    FileUtils.createUniqueCopyName(mContext, dest,
                                            fh.getName()));
                        else
                            res &= copyFolder(
                                    fh.getFile(),
                                    FileUtils.createUniqueCopyName(mContext, dest,
                                            fh.getName()));
                    }

                    if (mProgressListener != null) {
                        mProgressListener.onProgress(progress);
                    }
                    progress++;
                }

            }
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), e);
        }
        return res;
    }

    /**
     * Copy a file.
     *
     * @param oldFile File to copy.
     * @param newFile The file to be created.
     * @return Was copy successful?
     */
    private boolean copyFile(File oldFile, File newFile) {
        try {
            FileInputStream input = new FileInputStream(oldFile);
            FileOutputStream output = new FileOutputStream(newFile);

            byte[] buffer = new byte[COPY_BUFFER_SIZE];

            while (true) {
                int bytes = input.read(buffer);

                if (bytes <= 0) {
                    break;
                }

                output.write(buffer, 0, bytes);
            }

            output.close();
            input.close();

            // Request media scan
            MediaScannerUtils.informFileAdded(mContext, newFile);

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Recursively copy a folder.
     *
     * @param oldFile Folder to copy.
     * @param newFile The dir to be created.
     * @return Was copy successful?
     */
    private boolean copyFolder(File oldFile, File newFile) {
        boolean res = true;

        if (oldFile.isDirectory()) {
            // if directory not exists, create it
            if (!newFile.exists()) {
                newFile.mkdir();
                // System.out.println("Directory copied from " + src + "  to " + dest);
            }

            // list all the directory contents
            String files[] = oldFile.list();

            for (String file : files) {
                // construct the src and dest file structure
                File srcFile = new File(oldFile, file);
                File destFile = new File(newFile, file);
                // recursive copy
                res &= copyFolder(srcFile, destFile);
            }
        } else {
            res &= copyFile(oldFile, newFile);
        }

        return res;
    }

    private boolean copyFileAndroid5(File oldFile, File newFile) {
        try {
            if (mCancelAction) {
                return false;
            }
            FileUtil.copyFile(oldFile, newFile, mContext);
            // Request media scan
            MediaScannerUtils.informFileAdded(mContext, newFile);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean copyFolderAndroid5(File oldFile, File newFile) {
        boolean res = true;
        DocumentFile documentFile = null;
        if (mCancelAction) {
            return false;
        }

        if (oldFile.isDirectory()) {
            boolean result = false;
            // if directory not exists, create it
            if (!newFile.exists()) {
                result = newFile.mkdir();
                // System.out.println("Directory copied from " + src + "  to " + dest);
            }

            if (!result) {
                documentFile = FileUtil.getDocumentFile(newFile, true, true, mContext);
                if (documentFile == null) {
                    return false;
                }
            }

            // list all the directory contents
            String files[] = oldFile.list();

            for (String file : files) {
                // construct the src and dest file structure
                File srcFile = new File(oldFile, file);
                File destFile = new File(newFile, file);
                // recursive copy
                if (srcFile.isDirectory()) {
                    res &= copyFolderAndroid5(srcFile, destFile);
                } else {
                    res &= copyFileAndroid5(srcFile, destFile);
                }
            }
        } else {
            res &= FileUtil.copyFile(oldFile, newFile, mContext);
        }

        return res;
    }

    private boolean cutFileAndroid5(File oldFile, File newFile) {
        try {
            if (mCancelAction) {
                return false;
            }
            FileUtil.moveFile(oldFile, newFile, mContext);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean cutFolderAndroid5(File oldFile, File newFile) {
        boolean res = true;
        DocumentFile documentFile = null;

        if (mCancelAction) {
            return false;
        }
        if (oldFile.isDirectory()) {
            boolean result = false;
            // if directory not exists, create it
            if (!newFile.exists()) {
                result = newFile.mkdir();
                // System.out.println("Directory copied from " + src + "  to " + dest);
            }

            if (!result) {
                documentFile = FileUtil.getDocumentFile(newFile, true, true, mContext);
                if (documentFile == null) {
                    return false;
                }
            }

            // list all the directory contents
            String files[] = oldFile.list();
            if (files != null && files.length == 0) {
                return FileUtil.deleteFile(oldFile, mContext);
            }

            for (String file : files) {
                // construct the src and dest file structure
                File srcFile = new File(oldFile, file);
                File destFile = new File(newFile, file);
                // recursive copy
                if (srcFile.isDirectory()) {
                    res &= cutFolderAndroid5(srcFile, destFile);
                } else {
                    res &= cutFileAndroid5(srcFile, destFile);
                }
            }
        } else {
            res &= FileUtil.moveFile(oldFile, newFile, mContext);
        }

        return res;
    }

    /**
     * Call this to actually move.
     *
     * @param dest The path to move the clipboard into.
     * @return false if ANY error has occurred. This may mean that some files have been successfully moved, but not all.
     */
    private boolean performCut(File dest) {
        boolean res = true;
        boolean deleteOk = true;
        boolean createOk = true;
        int progress = 0;

        try {
            File from;
            if (PermissionUtil.isAndroid5()) {
                for (FileHolder fh : mClipboard) {
                    from = fh.getFile().getAbsoluteFile();

                    if (mCancelAction) {
                        res = false;
                        break;
                    }

                    try {
                        if (dest.getAbsolutePath().contains(from.getAbsolutePath())) {
                            res &= false;
                        } else {
                            if (!from.isDirectory()) {
                                createOk = cutFileAndroid5(from, FileUtil.getFile(dest, fh.getName()));
                            } else {
                                createOk = cutFolderAndroid5(from, FileUtil.getFile(dest, fh.getName()));
                            }

                            res &= createOk;

                            if (createOk) {
                                MediaScannerUtils.informFileDeleted(mContext, from);
                                MediaScannerUtils.informFileAdded(mContext,
                                        FileUtil.getFile(dest, fh.getName()));
                            }
                        }

                    } catch (Exception e) {
                        res = false;
                    }
                    if (mProgressListener != null) {
                        mProgressListener.onProgress(progress);
                    }
                    progress++;
                }

            } else {
                for (FileHolder fh : mClipboard) {

                    if (mCancelAction) {
                        res = false;
                        break;
                    }
                    try {
                        from = fh.getFile().getAbsoluteFile();

                        if (dest.getAbsolutePath().contains(from.getAbsolutePath())) {
                            res &= false;
                        } else {
                            deleteOk = fh.getFile().renameTo(
                                    FileUtil.getFile(dest, fh.getName()));

                            // Inform media scanner
                            if (deleteOk) {
                                MediaScannerUtils.informFileDeleted(mContext, from);
                                MediaScannerUtils.informFileAdded(mContext,
                                        FileUtil.getFile(dest, fh.getName()));
                            }

                            res &= deleteOk;
                        }
                    } catch (Exception e) {
                        res = false;
                    }
                    if (mProgressListener != null) {
                        mProgressListener.onProgress(progress);
                    }
                    progress++;
                }
            }
        } catch (Exception e) {

        }

        return res;
    }

    public void paste(File copyTo, OnOperationFinishedListener listener) {
        paste(copyTo, listener, null);
    }

    /**
     * Paste the copied/cut items.
     *
     * @param copyTo   Path to paste to.
     * @param listener Listener that will be informed on operation finish. CAN'T BE NULL.
     */
    public void paste(File copyTo, OnOperationFinishedListener listener, OnOperationProgressListener progressListener) {
        mListener = listener;
        mProgressListener = progressListener;
        try {
            EventBus.getDefault().register(this);
        } catch (Exception e) {
        }

        mCancelAction = false;

        try {
            // Quick check just to make sure. Normally this should never be the case as the path we get is not user-generated.
            if (!copyTo.isDirectory())
                return;

            switch (mOperation) {
                case COPY:
                    mActionTask =  new CopyAsync().execute(copyTo);
                    break;
                case CUT:
                    mActionTask = new MoveAsync().execute(copyTo);
                    break;
                default:
                    return;
            }
        } catch (Exception e) {
        }
    }

    private class CopyAsync extends ModernAsyncTask<File, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            CustomToast.getInstance().toast(mContext, mContext.getResources().getString(R.string.copying), Toast.LENGTH_SHORT);
        }

        @Override
        protected Boolean doInBackground(File... params) {
            return performCopy(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            handleResult(result);
        }

        @Override
        protected void onCancelled(Boolean result) {
            handleResult(result);
        }

        private void handleResult(Boolean result) {
            CustomToast.getInstance().toast(mContext,
                    result ? mContext.getResources().getString(R.string.copied) : mContext.getResources().getString(R.string.copy_error), Toast.LENGTH_SHORT);

            // Clear as the references have been invalidated.
            mClipboard.clear();

            if (mListener != null) {
                mListener.operationFinished(result);
                // Invalidate listener.
                mListener = null;
            }
            try {
                EventBus.getDefault().unregister(this);
            } catch (Exception e) {
            }
        }
    }

    private class MoveAsync extends ModernAsyncTask<File, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            CustomToast.getInstance().toast(mContext, mContext.getResources().getString(R.string.moving), Toast.LENGTH_SHORT);
        }

        @Override
        protected Boolean doInBackground(File... params) {
            return performCut(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            handleResult(result);
        }

        @Override
        protected void onCancelled(Boolean result) {
            handleResult(result);
        }

        private void handleResult(Boolean result) {
            CustomToast.getInstance().toast(mContext,
                    result ? mContext.getResources().getString(R.string.moved) : mContext.getResources().getString(R.string.move_error), Toast.LENGTH_SHORT);
            // Clear as the references have been invalidated.
            mClipboard.clear();

            if (mListener != null) {
                mListener.operationFinished(result);
                // Invalidate listener.
                mListener = null;
            }
            try {
                EventBus.getDefault().unregister(this);
            } catch (Exception e) {
            }
        }
    }

    public interface OnOperationFinishedListener {
        /**
         * @param success Whether the operation was entirely successful.
         */
        public void operationFinished(boolean success);
    }

    public interface OnOperationProgressListener {
        public void onProgress(int progress);
    }

    public void onEvent(CancelEvent event) {
        mCancelAction = true;
        if (mActionTask != null && mActionTask.getStatus() == ModernAsyncTask.Status.RUNNING) {
            mActionTask.cancel(true);
        }
    }
}