package com.filemanager.util;

import android.support.v4.provider.DocumentFile;
import base.util.*;
import com.afollestad.materialdialogs.MaterialDialog;
import imoblife.android.os.ModernAsyncTask;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.filemanager.R;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class ExtractManager {
    /**
     * TAG for log messages.
     */
    static final String TAG = "ExtractManager";

    private static final int BUFFER_SIZE = 1024;
    private Context context;
    private MaterialDialog progressDialog;
	private OnExtractFinishedListener onExtractFinishedListener = null;

    public ExtractManager(Context context) {
        this.context = context;
    }

    public void extract(File f, String destinationPath) {
            new ExtractTask().execute(f, destinationPath);
    }

    private class ExtractTask extends ModernAsyncTask<Object, Void, Integer> {
        private static final int success = 0;
        private static final int error = 1;
        private boolean mCanceled = false;

        /**
         * count of extracted files to update the progress bar
         */
        private int isExtracted = 0;

        /**
         * Recursively extract file or directory
         */
        public boolean extract(File archive, String destinationPath) {
            try {
                ZipFile zipfile = new ZipFile(archive);
                int fileCount = zipfile.size();
                for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
                    if (mCanceled) {
                        return false;
                    }
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    unzipEntry(zipfile, entry, destinationPath);
                    isExtracted++;
                    progressDialog.setProgress((isExtracted * 100)/ fileCount);
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error while extracting file " + archive, e);
                return false;
            }
        }        
        
        private void createDir(File dir) {
            if (dir.exists()) {
                return;
            }
            if (PermissionUtil.isAndroid5() && FileUtil.isOnExtSdCard(dir, context)) {

                if (FileUtil.getDocumentFile(dir, true, true, context) == null) {
                    throw new RuntimeException("Can not create dir " + dir);
                }

            } else {
                if (!dir.mkdirs()) {
                    throw new RuntimeException("Can not create dir " + dir);
                }
            }
        }        
        
        private void unzipEntry(ZipFile zipfile, ZipEntry entry,
                                String outputDir) throws IOException {
            if (mCanceled) {
                return;
            }
            if (entry.isDirectory()) {
                createDir(new File(outputDir, entry.getName()));
                return;
            }
            File outputFile = new File(outputDir, entry.getName());

            if (mCanceled) {
                return;
            }

            if (!outputFile.getParentFile().exists()) {
                createDir(outputFile.getParentFile());
            }

            if (mCanceled) {
                return;
            }

            BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
            BufferedOutputStream outputStream = null;

            DocumentFile documentFile = FileUtil.getDocumentFile(outputFile, false, true, context);
            if (PermissionUtil.isAndroid5() && documentFile != null) {

                OutputStream tmp = context.getContentResolver().openOutputStream(documentFile.getUri());
                outputStream = new BufferedOutputStream(tmp);
            } else {
                outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
            }

            if (mCanceled) {
                return;
            }

            try {
                int len;
                byte buf[] = new byte[BUFFER_SIZE];
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
            } finally {
                outputStream.close();
                inputStream.close();
            }
        }

        @Override
        protected void onPreExecute() {
            mCanceled = false;
            progressDialog = new MaterialDialog.Builder(context)
                    .content(context.getString(R.string.extracting))
                    .progress(false, 100, true)
                    .cancelable(false)
                    .positiveText(R.string.disableall_cancel)
                    .positiveColorRes(R.color.blue_1ca0ec)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            if (!mCanceled) {
                                mCanceled = true;
                                cancel(true);
                            }
                        }
                    })
                    .build();
            progressDialog.show();

            isExtracted = 0;
        }

        @Override
        protected Integer doInBackground(Object... params) {
            File f= (File) params[0];
            String destination = (String) params[1];
            boolean result = extract(f, destination);
            return result ? success : error;
        }

        @Override
        protected void onPostExecute(Integer result) {
            mCanceled = true;
            progressDialog.cancel();
            if (result == error){
                Toast.makeText(context, R.string.extracting_error, Toast.LENGTH_SHORT).show();
            } else if (result == success){
                Toast.makeText(context, R.string.extracting_success, Toast.LENGTH_SHORT).show();
            }
            
            if(onExtractFinishedListener != null)
            	onExtractFinishedListener.extractFinished();
        }

        @Override
        protected void onCancelled(Integer result) {
            mCanceled = true;
            progressDialog.cancel();
            if (result == error){
                Toast.makeText(context, R.string.extracting_error, Toast.LENGTH_SHORT).show();
            } else if (result == success){
                Toast.makeText(context, R.string.extracting_success, Toast.LENGTH_SHORT).show();
            }

            if(onExtractFinishedListener != null)
                onExtractFinishedListener.extractFinished();
        }
    }
    
    public interface OnExtractFinishedListener{
    	public abstract void extractFinished();
    }

	public ExtractManager setOnExtractFinishedListener(OnExtractFinishedListener listener) {
		this.onExtractFinishedListener = listener;
		return this;
	}
}
