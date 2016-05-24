package com.filemanager.occupancy;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import base.util.FileUtil;
import base.util.PreferenceHelper;
import base.util.ui.titlebar.ISearchBarActionListener;
import com.filemanager.DistributionLibraryFragmentActivity;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.util.FileUtils;
import com.intents.FileManagerIntents;

import java.io.File;

/**
 * Created by wuhao on 2015/6/15.
 */
public class StorageAnalysisActivity extends DistributionLibraryFragmentActivity implements ISearchBarActionListener {
    public static final String EXTRA_CHANGE_TITLE = "changeTitle";
    public static final String EXTRA_FILE_URI = "fileUri";

    public static final String FRAGMENT_TAG = "AnalysisFragment";

    protected static final int REQUEST_CODE_BOOKMARKS = 1;

    private SimpleAnalysisListFragment mFragment;

    @Override
    protected void onNewIntent(Intent intent) {
        try {
            if (intent.getData() != null)
                mFragment.openInformingPathBar(new FileHolder(FileUtil
                        .getFile(intent.getData()), this));
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), e);
        }
    }

    /**
     * Either open the file and finish, or navigate to the designated directory.
     * This gives FileManagerActivity the flexibility to actually handle file
     * scheme data of any type.
     *
     * @return The folder to navigate to, if applicable. Null otherwise.
     */
    private File resolveIntentData() {
        File data = FileUtil.getFile(getIntent().getData());
        if (data == null)
            return null;

        if (data.isFile()
                && !getIntent().getBooleanExtra(
                FileManagerIntents.EXTRA_FROM_OI_FILEMANAGER, false)) {
            FileUtils.openFile(new FileHolder(data, this), this);

            finish();
            return null;
        } else
            return FileUtil.getFile(getIntent().getData());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setTitle(R.string.storage_analysis);

        // Search when the user types.
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        File data = null;
        if (getIntent().getStringExtra(EXTRA_FILE_URI) != null) {
            data = FileUtil.getFile(Uri.parse(getIntent().getStringExtra(
                    EXTRA_FILE_URI)));
            if (getIntent().getStringExtra(EXTRA_CHANGE_TITLE) != null) {
                setTitle(getIntent().getStringExtra(EXTRA_CHANGE_TITLE));
            }
        } else {
            // If not called by name, open on the requested location.
            data = resolveIntentData();
        }

        // Add fragment only if it hasn't already been added.
        mFragment = (SimpleAnalysisListFragment) getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG);
        if (mFragment == null) {
            mFragment = new SimpleAnalysisListFragment();
            Bundle args = new Bundle();
            if (data == null)
                args.putString(
                        FileManagerIntents.EXTRA_DIR_PATH,
                        Environment.getExternalStorageState().equals(
                                Environment.MEDIA_MOUNTED) ? PreferenceHelper
                                .getSdcardPath(getApplicationContext()) : "/");
            else
                args.putString(FileManagerIntents.EXTRA_DIR_PATH,
                        data.toString());
            mFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, mFragment, FRAGMENT_TAG)
                    .commit();
        } else {
            // If we didn't rotate and data wasn't null.

            if (icicle == null && data != null)
                mFragment.openInformingPathBar(new FileHolder(new File(data
                        .toString()), this));
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        this.setActionVisibility(View.GONE);
    }


    // The following methods should properly handle back button presses on every
    // API Level.
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.DONUT) {
            if (keyCode == KeyEvent.KEYCODE_BACK && mFragment.pressBack())
                return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT) {
            if (keyCode == KeyEvent.KEYCODE_BACK && mFragment.pressBack())
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * This is called after the file manager finished.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CODE_BOOKMARKS:
                if (resultCode == RESULT_OK && data != null) {
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onTitlebarActionClick(View view) {
        super.onTitlebarActionClick(view);
    }

    @Override
    public void onSearch() {
        onSearchRequested();
    }


    public String getTrackModule() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean onTitlebarBackClick(View view) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.DONUT) {
            if (mFragment.pressBack()) {
                return false;
            } else {
                return true;
            }
        }
        return true;
    }
}
