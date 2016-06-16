package com.filemanager;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import base.util.ui.titlebar.BaseTitlebarFragmentActivity;
import com.filemanager.view.HomeFunctionView;

import java.io.File;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by Administrator on 2016/6/15.
 */
public class FileManagerMainActivity extends BaseTitlebarFragmentActivity {

    private HomeFunctionView mHomeFunctionView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_home_activity_layout);
        this.setTitle(R.string.file_manage);
        mHomeFunctionView = (HomeFunctionView) findViewById(R.id.function_layout);

    }

    @Override
    public String getTrackModule() {
        return FileManagerMainActivity.class.getSimpleName();
    }





}
