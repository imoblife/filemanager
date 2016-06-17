package com.filemanager;

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import base.util.os.EnvironmentUtil;
import base.util.ui.titlebar.BaseTitlebarFragmentActivity;
import com.filemanager.view.HomeFunctionView;
import imoblife.android.os.ModernAsyncTask;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * Created by Administrator on 2016/6/17.
 */
public class FileImageActivity extends BaseTitlebarFragmentActivity {
    private ArrayList<String> mImageGroupList = new ArrayList<>();

    private LinearLayout mLoadingLayout;

    private RecyclerView mRecyclerView;
    private GetImagesTask mGetImagesTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(R.string.file_image_name);
        setContentView(R.layout.file_image_activity_layout);
        mLoadingLayout = (LinearLayout) findViewById(R.id.ln_loading);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        mGetImagesTask = new GetImagesTask();
        mGetImagesTask.execute();
        mImageGroupList = new ArrayList<>();
    }

    @Override
    public String getTrackModule() {
        return FileImageActivity.class.getSimpleName();
    }

    private void setLoading(boolean isLoading){
        mLoadingLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private class GetImagesTask extends ModernAsyncTask<Void, Void, Void> {
        private ArrayList<String> dirList;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setLoading(true);
            mImageGroupList.clear();
        }

        @Override
        protected Void doInBackground(Void... params) {
            dirList = getImageDirPaths(FileImageActivity.this);
            List<String> list = FindFiles();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            setLoading(false);
        }
    }

    public static ArrayList<String> getImageDirPaths(Activity activity) {
        Uri u = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.ImageColumns.DATA};
        Cursor c = null;
        ArrayList<String> dirList = new ArrayList<String>();

        if (u != null) {
            c = activity.managedQuery(u, projection, null, null, null);
        }

        if ((c != null) && (c.moveToFirst())) {
            do {
                String tempDir = c.getString(0);
                tempDir = tempDir.substring(0, tempDir.lastIndexOf("/"));
                try {
                    dirList.add(tempDir);
                } catch (Exception e) {

                }
            }
            while (c.moveToNext());

        }

        return dirList;
    }





    private List<String> FindFiles() {
        final List<String> tFileList = new ArrayList<String>();
        // array of valid image file extensions
        String[] imageTypes = new String[]{"bmp","cmx","cod","gif","ico","ief"
        ,"jpe","jpeg","jpg","jfif","pbm","pgm","png","pnm","ppm","ras","rgb"
        ,"svg","tif","tiff","xbm","xpm","xwd"};
        FilenameFilter[] filter = new FilenameFilter[imageTypes.length];

        int i = 0;
        for (final String type : imageTypes) {
            filter[i] = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith("." + type);
                }
            };
            i++;
        }

        File[] allMatchingFiles = listFilesAsArray(
                new File(EnvironmentUtil.getStoragePath(getContext(), false)), filter, -1);
        for (File f : allMatchingFiles) {
            tFileList.add(f.getAbsolutePath());
            Log.e("wuhao"," image path==>>"+f.getAbsolutePath());
        }
        return tFileList;
    }

    public File[] listFilesAsArray(File directory, FilenameFilter[] filter,
                                   int recurse) {
        Collection<File> files = listFiles(directory, filter, recurse);

        File[] arr = new File[files.size()];
        return files.toArray(arr);
    }

    public Collection<File> listFiles(File directory,
                                      FilenameFilter[] filter, int recurse) {

        Vector<File> files = new Vector<File>();

        File[] entries = directory.listFiles();

        if (entries != null) {
            for (File entry : entries) {
                for (FilenameFilter filefilter : filter) {
                    if (filter == null
                            || filefilter
                            .accept(directory, entry.getName())) {
                        files.add(entry);
                    }
                }
                if ((recurse <= -1) || (recurse > 0 && entry.isDirectory())) {
                    recurse--;
                    files.addAll(listFiles(entry, filter, recurse));
                    recurse++;
                }
            }
        }
        return files;
    }
}
