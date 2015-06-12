package com.filemanager.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by wuhao on 2015/6/12.
 */
public class Preference {
    public static final String PREFS_NAME = "file_manager_config";
    public static final String PREFS_KEY_SORT_TYPE = "file_search_type";

    //file sort type
    public static final int SORT_TYPE_DEFAULT = 0;
    public static final int SORT_TYPE_NAME = 1;
    public static final int SORT_TYPE_MODIFY_TIME = 2;


    private SharedPreferences mPreferences;


    public Preference(Context context) {
        mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    public int getInt(String key, int defaultValue) {
        return mPreferences.getInt(key, defaultValue);
    }

    public void setInt(String key, int value) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }
}
