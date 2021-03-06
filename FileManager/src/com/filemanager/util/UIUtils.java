package com.filemanager.util;


import android.content.Context;
import com.filemanager.R;

import android.app.Activity;
import android.preference.PreferenceManager;

public abstract class UIUtils {

	public static boolean shouldDialogInverseBackground(Activity act){
		return !PreferenceManager.getDefaultSharedPreferences(act).getBoolean("usedarktheme", true);
	}

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
