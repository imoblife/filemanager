package com.filemanager.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import base.util.PackageUtil;
import com.filemanager.FileImageActivity;
import com.filemanager.R;
import com.filemanager.iconicdroid.FmFont;
import com.iconics.IconFontDrawable;
import com.iconics.typeface.IIcon;

import java.io.File;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by Administrator on 2016/6/16.
 */
public class HomeFunctionView extends RelativeLayout implements View.OnClickListener {

    private Context mContext;
    private FunctionItem mFunctionItem1;
    private FunctionItem mFunctionItem2;
    private FunctionItem mFunctionItem3;
    private FunctionItem mFunctionItem4;
    private FunctionItem mFunctionItem5;
    private FunctionItem mFunctionItem6;
    private FunctionItem mFunctionItem7;
    private FunctionItem mFunctionItem8;


    public HomeFunctionView(Context context) {
        super(context);
        mContext = context;
    }

    public HomeFunctionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public HomeFunctionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    private void initViews() {
        mFunctionItem1 = new FunctionItem(findViewById(R.id.function_item_1), R.string.file_function_item_1, getFunctionDrawable(mContext, 1, R.color.function_item_color_1));
        mFunctionItem2 = new FunctionItem(findViewById(R.id.function_item_2), R.string.file_function_item_2, getFunctionDrawable(mContext, 2, R.color.function_item_color_2));
        mFunctionItem3 = new FunctionItem(findViewById(R.id.function_item_3), R.string.file_function_item_3, getFunctionDrawable(mContext, 3, R.color.function_item_color_3));
        mFunctionItem4 = new FunctionItem(findViewById(R.id.function_item_4), R.string.file_function_item_4, getFunctionDrawable(mContext, 4, R.color.function_item_color_4));
        mFunctionItem5 = new FunctionItem(findViewById(R.id.function_item_5), R.string.file_function_item_5, getFunctionDrawable(mContext, 5, R.color.function_item_color_5));
        mFunctionItem6 = new FunctionItem(findViewById(R.id.function_item_6), R.string.file_function_item_6, getFunctionDrawable(mContext, 6, R.color.function_item_color_6));
        mFunctionItem7 = new FunctionItem(findViewById(R.id.function_item_7), R.string.file_function_item_7, getFunctionDrawable(mContext, 7, R.color.function_item_color_7));
        mFunctionItem8 = new FunctionItem(findViewById(R.id.function_item_8), R.string.file_function_item_8, getFunctionDrawable(mContext, 8, R.color.function_item_color_8));

        mFunctionItem1.textViewCount.setText(getFilePaths((Activity) mContext).size() + "");
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        Intent i = new Intent();
        if (viewId == R.id.function_item_1) {
            i.setClass(mContext, FileImageActivity.class);
        } else if (viewId == R.id.function_item_2) {

        } else if (viewId == R.id.function_item_3) {

        } else if (viewId == R.id.function_item_4) {

        } else if (viewId == R.id.function_item_5) {

        } else if (viewId == R.id.function_item_6) {

        } else if (viewId == R.id.function_item_7) {

        } else if (viewId == R.id.function_item_8) {

        }
        mContext.startActivity(i);
    }


    private class FunctionItem {
        public ImageView imageViewIcon;
        public TextView textViewName;
        public TextView textViewCount;

        private FunctionItem(View rootView, int stringRes, Drawable drawable) {
            rootView.setOnClickListener(HomeFunctionView.this);
            imageViewIcon = (ImageView) rootView.findViewById(R.id.iv_icon);
            textViewName = (TextView) rootView.findViewById(R.id.tv_name);
            textViewCount = (TextView) rootView.findViewById(R.id.tv_count);

            imageViewIcon.setImageDrawable(drawable);
            textViewName.setText(stringRes);
            textViewCount.setText("0");
        }

    }

    private Drawable getFunctionDrawable(Context context, int type, int colorRes) {
        IconFontDrawable drawable = null;
        IIcon iIcon = null;
        switch (type) {
            case 1:
                iIcon = FmFont.Icon.FMT_ICON_IMAGE;
                break;
            case 2:
                iIcon = FmFont.Icon.FMT_ICON_MUSIC;
                break;
            case 3:
                iIcon = FmFont.Icon.FMT_ICON_VIDEO;
                break;
            case 4:
                iIcon = FmFont.Icon.FMT_ICON_DOCUMENT;
                break;
            case 5:
                iIcon = FmFont.Icon.FMT_ICON_APK;
                break;
            case 6:
                iIcon = FmFont.Icon.FMT_ICON_DOWNLOAD;
                break;
            case 7:
                iIcon = FmFont.Icon.FMT_ICON_COMPRESS;
                break;
            case 8:
                iIcon = FmFont.Icon.FMT_ICON_RECENT;
                break;
        }
        if (iIcon != null) {
            drawable = new IconFontDrawable(context)
                    .icon(iIcon)
                    .colorRes(colorRes)
                    .sizeDp(24);
        }
        return drawable;
    }

    public static ArrayList<String> getFilePaths(Activity activity) {
        Uri u = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.ImageColumns.DATA};
        Cursor c = null;
        SortedSet<String> dirList = new TreeSet<String>();
        ArrayList<String> resultIAV = new ArrayList<String>();

        String[] directories = null;
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
            directories = new String[dirList.size()];
            dirList.toArray(directories);

        }

        for (int i = 0; i < dirList.size(); i++) {
            File imageDir = new File(directories[i]);
            File[] imageList = imageDir.listFiles();
            if (imageList == null)
                continue;
            for (File imagePath : imageList) {
                try {

                    if (imagePath.isDirectory()) {
                        imageList = imagePath.listFiles();

                    }
                    if (imagePath.getName().contains(".jpg") || imagePath.getName().contains(".JPG")
                            || imagePath.getName().contains(".jpeg") || imagePath.getName().contains(".JPEG")
                            || imagePath.getName().contains(".png") || imagePath.getName().contains(".PNG")
                            || imagePath.getName().contains(".gif") || imagePath.getName().contains(".GIF")
                            || imagePath.getName().contains(".bmp") || imagePath.getName().contains(".BMP")
                            ) {


                        String path = imagePath.getAbsolutePath();
                        resultIAV.add(path);

                    }
                }
                //  }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return resultIAV;
    }
}
