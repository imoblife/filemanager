package com.filemanager.search;

import android.content.Context;
import android.text.TextUtils;
import com.filemanager.files.FileHolder;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by wuhao on 2015/6/11.
 */
public class SearchResultContainer {
    private static SearchResultContainer instance = null;
    private ArrayList<FileHolder> mPath = new ArrayList<>();
    private int mResult = 0;
    private Context mContext;

    public void initContext(Context context){
        mContext = context;
    }

    public static SearchResultContainer getInstance() {
        if (instance == null) {
            instance = new SearchResultContainer();
        }
        return instance;
    }


    public int getResult() {
        return mResult;
    }

    public void setResult(int result) {
        this.mResult = result;
    }

    public void addFilePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        FileHolder fileHolder = new FileHolder(new File(path), mContext);
        mPath.add(fileHolder);
    }

    public ArrayList<FileHolder> getPaths() {
        return mPath;
    }

    public void clear(){
        mResult = 0;
        mPath.clear();
    }
}
