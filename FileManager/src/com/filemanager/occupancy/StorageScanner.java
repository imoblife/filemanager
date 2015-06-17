package com.filemanager.occupancy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import com.filemanager.R;
import com.filemanager.files.FileHolder;

import java.io.File;
import java.util.Comparator;

/**
 * Created by wuhao on 2015/6/15.
 */
public class StorageScanner extends Thread {
    /**
     * List of contents is ready.
     */
    public static final int MESSAGE_SHOW_STORAGE_ANALYSIS = 526;

    private static final String TAG = "StorageScanner";

    private File currentDirectory;

    private boolean running = false;
    boolean cancelled;

    private Handler handler;

    private FileTreeNode<String> mRoot;
    private File mDirectory;
    private Context mContext;
    private long mBlockSize = 512;

    public StorageScanner(File directory, Context context, Handler handler) {
        super("Storage analysis Scanner");
        currentDirectory = directory;
        this.handler = handler;
        this.mRoot = new FileTreeNode<>("root");
        this.mRoot.data = directory.getPath();
        this.mDirectory = directory;
        this.mContext = context.getApplicationContext();

        StatFs fs = new StatFs(mDirectory.getPath());
        mBlockSize = fs.getBlockSize();
    }

    private void init() {
        Log.v(TAG, "Scanning directory " + currentDirectory);

        if (cancelled) {
            Log.v(TAG, "Scan aborted");
            return;
        }
    }

    public void run() {
        running = true;
        init();

        Log.e("wuhao", "Storage Scan start..." + mDirectory.getAbsolutePath());
        // Scan files
        long time = System.currentTimeMillis();
        try {
            mRoot.size = folderSize(mDirectory, mRoot);
        } catch (Exception e) {

        }
        Log.e("wuhao", "Storage Scan end..." + (System.currentTimeMillis() - time));

        // Return lists
        if (!cancelled) {
            Log.v(TAG, "Sending data back to main thread");

            Message msg = handler.obtainMessage(MESSAGE_SHOW_STORAGE_ANALYSIS);
            msg.obj = mRoot;
            msg.sendToTarget();
        }

        running = false;
    }


    public long folderSize(File directory, FileTreeNode<String> node) {
        long length = 0;

        if (cancelled) {
            throw new RuntimeException();
        }
        File[] contents = directory.listFiles();
        // the directory file is not really a directory..
        if (contents == null) {
            return 0;
        }

        try {
            for (File file : contents) {
                FileHolder holder = null;
                FileTreeNode<String> child = node.addChild(file.getPath());
                long tmpSize;
                if (file.isFile()) {
                    tmpSize = file.length();
                    child.size = tmpSize;
                    length += tmpSize;
                } else {
                    tmpSize = folderSize(file, child);
                    child.size = tmpSize;
                    length += tmpSize;
                }
            }
        } catch (Exception e) {

        } catch (OutOfMemoryError error) {

        }

        if (cancelled) {
            throw new RuntimeException();
        }

        if (directory.isDirectory()) {
            length = length + mBlockSize;
        }

        return length;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isRunning() {
        return running;
    }

    public static class ComparatorBySize implements Comparator<FileTreeNode<String>> {
        public int compare(FileTreeNode<String> f1, FileTreeNode<String> f2) {
            long diff = f1.size - f2.size;
            if (diff > 0)
                return -1;
            else if (diff == 0)
                return 0;
            else
                return 1;
        }

        public boolean equals(Object obj) {
            return true;
        }
    }
}



