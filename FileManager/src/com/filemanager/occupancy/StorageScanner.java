package com.filemanager.occupancy;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;

import java.io.File;
import java.util.*;

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
    private ArrayList<FileTreeNode<String>> mDir;
    private int mResult = 0;

    public StorageScanner(File directory, Context context, Handler handler) {
        super("Storage analysis Scanner");
        currentDirectory = directory;
        this.handler = handler;
        this.mRoot = new FileTreeNode<>(directory);
        this.mDirectory = directory;
        this.mContext = context.getApplicationContext();

        this.mDir = new ArrayList<>();
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

        // Scan files
        long time = System.currentTimeMillis();
        try {
            mDir.add(mRoot);
            createTreeNodes(mRoot);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {

        }

        // Return lists
        if (!cancelled) {
            int size = mDir.size();
            for (int i = size - 1; i < size && i >= 0; i--) {
                FileTreeNode<String> node = mDir.get(i);
                node.refresh();
            }
            Log.e(TAG, "Sending data back to main thread cost time ==>>" + (System.currentTimeMillis() - time) + " size==>>" + Formatter.formatFileSize(mContext, mRoot.size));
            Message msg = handler.obtainMessage(MESSAGE_SHOW_STORAGE_ANALYSIS);
            msg.obj = mRoot;
            msg.sendToTarget();
        }

        running = false;
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

    private void createTreeNodes(FileTreeNode<String> node) {
        Stack<FileTreeNode<String>> dirList = new Stack<FileTreeNode<String>>();
        dirList.push(node);

        while (!dirList.isEmpty()) {
            if (cancelled) {
                return;
            }
            FileTreeNode<String> dirCurrent = dirList.pop();

            File[] fileList = dirCurrent.data.listFiles();
            if(fileList == null){
                continue;
            }
            for (File f : fileList) {
                if (cancelled) {
                    return;
                }
                if (f == null) {
                    continue;
                }
                mResult++;
                FileTreeNode<String> tmp = dirCurrent.addChild(f);
                if (f.isDirectory()) {
                    if (cancelled) {
                        return;
                    }
                    mDir.add(tmp);
                    dirList.push(tmp);
                }
            }
        }
    }

    public int getResultCount() {
        return mResult;
    }
}



