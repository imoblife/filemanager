/* 
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.filemanager;

import java.io.File;

import android.text.TextUtils;
import base.util.FileUtil;
import base.util.ui.titlebar.ISearchBarActionListener;
import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.londatiga.android.QuickAction.OnActionItemClickListener;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import base.util.PreferenceHelper;
import base.util.ui.titlebar.ITitlebarActionMenuListener;

import com.filemanager.files.FileHolder;
import com.filemanager.lists.SimpleFileListFragment;
import com.filemanager.util.FileUtils;
import com.intents.FileManagerIntents;
import com.util.MenuIntentOptionsWithIcons;

public class FileManagerActivity extends DistributionLibraryFragmentActivity implements ISearchBarActionListener {
	public static final String EXTRA_CHANGE_TITLE = "changeTitle";
	public static final String EXTRA_FILE_URI = "fileUri";
	public static final String EXTRA_PATH_CLICK = "pathBarClickable";
	public static final String EXTRA_PATH_KEYWORD = "locateKeyword";

	public static final String FRAGMENT_TAG = "ListFragment";

	protected static final int REQUEST_CODE_BOOKMARKS = 1;

	private SimpleFileListFragment mFragment;

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
		// UIUtils.setThemeFor(this);

		super.onCreate(icicle);
		this.setTitle(R.string.file_manage);
		// mDistribution.setFirst(MENU_DISTRIBUTION_START,
		// DIALOG_DISTRIBUTION_START);

		// Check whether EULA has been accepted
		// or information about new version can be presented.
		// if (mDistribution.showEulaOrNewVersion()) {
		// return;
		// }

		// // Enable home button.
		// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		// HomeIconHelper.activity_actionbar_setHomeButtonEnabled(this);

		// Search when the user types.
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		File data = null;
        boolean pathBarClickable = true;
        String keyword = null;
        if (getIntent().getStringExtra(EXTRA_FILE_URI) != null) {
			data = FileUtil.getFile(Uri.parse(getIntent().getStringExtra(EXTRA_FILE_URI)));
			if (getIntent().getStringExtra(EXTRA_CHANGE_TITLE) != null) {
				setTitle(getIntent().getStringExtra(EXTRA_CHANGE_TITLE));
			}
            pathBarClickable = getIntent().getBooleanExtra(EXTRA_PATH_CLICK, true);
            keyword = getIntent().getStringExtra(EXTRA_PATH_KEYWORD);
        } else {
			// If not called by name, open on the requested location.
			data = resolveIntentData();
		}

		// Add fragment only if it hasn't already been added.
		mFragment = (SimpleFileListFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
		if (mFragment == null) {
			mFragment = new SimpleFileListFragment();
			Bundle args = new Bundle();
			if (data == null) {
				String sdcardPath = PreferenceHelper.getSdcardPath(getApplicationContext());
				String value = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ? sdcardPath : "/";
				args.putString(FileManagerIntents.EXTRA_DIR_PATH, value);
			} else {
				args.putString(FileManagerIntents.EXTRA_DIR_PATH, data.toString());
			}
            args.putBoolean(EXTRA_PATH_CLICK, pathBarClickable);
            if (!TextUtils.isEmpty(keyword)) {
                args.putString(EXTRA_PATH_KEYWORD, keyword);
            }
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

		this.setActionVisibility(View.VISIBLE);
	}


	// The following methods should properly handle back button presses on every
	// API Level.
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (VERSION.SDK_INT > VERSION_CODES.DONUT) {
			if (keyCode == KeyEvent.KEYCODE_BACK && mFragment.pressBack())
				return true;
		}

		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (VERSION.SDK_INT <= VERSION_CODES.DONUT) {
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

	/**
	 * We override this, so that we get informed about the opening of the search
	 * dialog and start scanning silently.
	 */
	@Override
	public boolean onSearchRequested() {
		Bundle appData = new Bundle();
		appData.putString(FileManagerIntents.EXTRA_SEARCH_INIT_PATH,
				mFragment.getPath());
		startSearch(null, false, appData, false);
		return true;
	}

	@Override
	public void onTitlebarActionClick(View view) {
		super.onTitlebarActionClick(view);
		new QuickActionMenu(view);
	}

    @Override
    public void onSearch() {
        onSearchRequested();
    }

    private class QuickActionMenu implements OnActionItemClickListener {
		public QuickActionMenu(View view) {
			QuickAction qa = new QuickAction(FileManagerActivity.this,
					QuickAction.VERTICAL);
			qa.setOnActionItemClickListener(this);
			qa.addActionItem(new ActionItem(0,
					getString(R.string.create_new_folder), null), true);
            qa.addActionItem(new ActionItem(1, getString(R.string.file_sort),
                    null), true);
            qa.addActionItem(new ActionItem(2, getString(R.string.storage_analysis),
                    null), false);
            qa.show(view);
		}

		public void onItemClick(QuickAction source, int pos, int actionId) {
            ITitlebarActionMenuListener l = (ITitlebarActionMenuListener) mFragment;
            l.onTitlebarActionMenuClick(pos);
        }
    }

	@Override
	public boolean isTrackEnabled() {
		return true;
	}

	public String getTrackModule() {
		return "v6_file_manager";
	}

	@Override
    public boolean onTitlebarBackClick(View view) {
        if (VERSION.SDK_INT > VERSION_CODES.DONUT) {
            if (mFragment.pressBack()) {
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
