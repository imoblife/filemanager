package com.filemanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import base.util.os.EnvironmentUtil;
import base.util.os.StatFsUtil;
import base.util.ui.titlebar.BaseTitlebarFragmentActivity;
import com.filemanager.view.HomeFunctionView;

/**
 * Created by Administrator on 2016/6/15.
 */
public class FileManagerMainActivity extends BaseTitlebarFragmentActivity {

    private HomeFunctionView mHomeFunctionView;

    private RelativeLayout mDeviceContent;
    private RelativeLayout mSdContent;

    private TextView mDeviceInfoTextView;
    private TextView mSdInfoTextView;
    private ProgressBar mDeviceProgressBar;
    private ProgressBar mSdProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_home_activity_layout);
        this.setTitle(R.string.file_manage);
        mHomeFunctionView = (HomeFunctionView) findViewById(R.id.function_layout);

        mDeviceContent = (RelativeLayout) findViewById(R.id.rl_content_1);
        mDeviceInfoTextView = (TextView) findViewById(R.id.tv_device_storage);
        mSdContent = (RelativeLayout) findViewById(R.id.rl_content_2);
        mSdInfoTextView = (TextView) findViewById(R.id.tv_sd_storage);

        mDeviceProgressBar = (ProgressBar) findViewById(R.id.size_progress_device);
        mSdProgressBar = (ProgressBar) findViewById(R.id.size_progress_sd);
    }

    @Override
    public void onResume() {
        super.onResume();
        initStorageInfos();
    }

    private void initStorageInfos() {
        String internalPath = EnvironmentUtil.getStoragePath(this, false);
        final String sdPath = EnvironmentUtil.getStoragePath(this, true);

        if (TextUtils.isEmpty(internalPath)) {
            internalPath = "/";
        }
        getShowInfo(internalPath, mDeviceInfoTextView, mDeviceProgressBar);
        final String finalInternalPath = internalPath;
        mDeviceContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cdStorage(finalInternalPath);
            }
        });

        if (TextUtils.isEmpty(sdPath)) {
            mSdContent.setVisibility(View.GONE);
        } else {
            mSdContent.setVisibility(View.VISIBLE);
            getShowInfo(sdPath, mSdInfoTextView, mSdProgressBar);
            mSdContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cdStorage(sdPath);
                }
            });
        }

    }

    private void cdStorage(String path) {
        try {
            if (TextUtils.isEmpty(path)) {
                return;
            }
            Intent intent = new Intent(this, FileManagerActivity.class);
            intent.putExtra(FileManagerActivity.EXTRA_FILE_URI, path);
            startActivity(intent);
        } catch (Exception e) {
        }
    }

    private String getShowInfo(String path, TextView textView, ProgressBar progressBar) {
        long totalSize = StatFsUtil.getTotalSdcard(getContext(), path);
        long usedSize = totalSize - StatFsUtil.getFreeSdcard(getContext(), path);
        String format1 = getResources().getString(R.string.examine_ram_free);
        String result1 = Formatter.formatFileSize(this, usedSize);
        String result2 = "/ " + Formatter.formatFileSize(getContext(), totalSize);
        textView.setText(format1 + result1 + result2);
        int progress = (int) (usedSize * 100 / totalSize);
        progressBar.setProgress(progress);
        return format1 + result1 + result2;
    }

    @Override
    public String getTrackModule() {
        return FileManagerMainActivity.class.getSimpleName();
    }


}
