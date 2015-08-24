package com.filemanager.search;

import android.app.SearchManager;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import base.util.ui.titlebar.BaseTitlebarListActivity;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.FileManagerActivity;
import com.filemanager.R;
import com.filemanager.compatibility.HomeIconHelper;
import com.filemanager.files.FileHolder;
import com.intents.FileManagerIntents;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.util.ArrayList;

/**
 * The activity that handles queries and shows search results. Also handles
 * search-suggestion triggered intents.
 * 
 * @author George Venios
 * 
 */
public class SearchableActivity extends BaseTitlebarListActivity {
    public static final String KEY_FILE_RESULT_COUNT = "result_count";

    private static final int MIN_REFRESH_LIST_COUNT = 1000;

	private LocalBroadcastManager lbm;
    private MaterialDialog mProgressDialog;
    private SearchListResultAdapter mAdapter;
    private TextView mEmptyTextView;
    private String mTitle;

    private Handler mHandler = new Handler();
    private boolean mFileSearchFinished = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// UIUtils.setThemeFor(this);
		// Presentation settings
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		this.setTitle(getString(R.string.file_manage));
		setContentView(R.layout.searchfile);
		// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		// HomeIconHelper.activity_actionbar_setDisplayHomeAsUpEnabled(this);
		// }
		//		this.setTitle(getString(R.string.file_manage));
		lbm = LocalBroadcastManager.getInstance(getApplicationContext());
        mEmptyTextView = (TextView) findViewById(R.id.tv_empty);
        hideEmptyView();
        mProgressDialog = new MaterialDialog.Builder(SearchableActivity.this).progressIndeterminateStyle(false).progress(true, 0).build();
//        mProgressDialog.setInverseBackgroundForced(UIUtils.shouldDialogInverseBackground(this));
        mProgressDialog.setContent(getString(R.string.file_search_message));
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                stopSearchFileTask();
            }
        });
        mAdapter = new SearchListResultAdapter(this);

        // Handle the search request.
		handleIntent();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			HomeIconHelper.showHome(this);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent();
	}

	private void handleIntent() {
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			// Get the query.
			String query = intent.getStringExtra(SearchManager.QUERY);
            mTitle = query;
            mAdapter.setQueryWord(mTitle);
            setTitle(query);

			// Get the current path, which allows us to refine the search.
			String path = null;
			if (intent.getBundleExtra(SearchManager.APP_DATA) != null)
				path = intent.getBundleExtra(SearchManager.APP_DATA).getString(
						FileManagerIntents.EXTRA_SEARCH_INIT_PATH);

			// Add query to recents.
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this, RecentsSuggestionsProvider.AUTHORITY,
					RecentsSuggestionsProvider.MODE);
			suggestions.saveRecentQuery(query, null);
            setListAdapter(mAdapter);
			// Register broadcast receivers
			lbm.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    setProgressBarIndeterminateVisibility(false);
                    if (mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    mFileSearchFinished = true;
                }
            }, new IntentFilter(FileManagerIntents.ACTION_SEARCH_FINISHED));

			lbm.registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
                    setProgressBarIndeterminateVisibility(true);
                    mHandler.post(new UpdateRunnable());
                }
			}, new IntentFilter(FileManagerIntents.ACTION_SEARCH_STARTED));

            mProgressDialog.show();
			// Start the search service.
			Intent in = new Intent(this, SearchService.class);
			in.putExtra(FileManagerIntents.EXTRA_SEARCH_INIT_PATH, path);
			in.putExtra(FileManagerIntents.EXTRA_SEARCH_QUERY, query);
			startService(in);
		} // We're here because of a clicked suggestion
		else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			browse(intent.getData());
		} else
			// Intent contents error.
			setTitle(R.string.query_error);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        stopSearchFileTask();
        lbm = null;
        mAdapter = null;
        setListAdapter(null);
		stopService(new Intent(this, SearchService.class));
	}

	/**
	 * Clear the recents' history.
	 */
	public static void clearSearchRecents(Context c) {
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(c,
				RecentsSuggestionsProvider.AUTHORITY,
				RecentsSuggestionsProvider.MODE);
		suggestions.clearHistory();
	}

	// @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
        FileHolder fileHolder = (FileHolder) mAdapter.getItem(position);

        if (fileHolder == null) {
            return;
        }

        File file = fileHolder.getFile();
        if (file == null || !file.exists()) {
            return;
        }
        browseFromListItem(Uri.parse(file.getAbsolutePath()));
        if (!file.isFile()) {
            finish();
        }
    }

    private void browseFromListItem(Uri path) {
        Intent intent = new Intent(this, FileManagerActivity.class);
        intent.setData(path);
        startActivity(intent);
    }

    private void browse(Uri path) {
        Intent intent = new Intent(this, FileManagerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setData(path);
        startActivity(intent);
        finish();
    }

    public String getTrackModule() {
        return getClass().getSimpleName();
    }

    private void stopSearchFileTask() {
        CancelTaskEvent event = new CancelTaskEvent();
        event.isCancelTask = true;
        EventBus.getDefault().post(event);
    }

    private void showEmptyView() {
        mEmptyTextView.setVisibility(View.VISIBLE);
    }

    private void hideEmptyView() {
        mEmptyTextView.setVisibility(View.GONE);
    }

    private class UpdateRunnable implements Runnable {

        @Override
        public void run() {
            updateTitle(mTitle + "(" + SearchResultContainer.getInstance().getResult() + ")");
            ArrayList<FileHolder> path = SearchResultContainer.getInstance().getPaths();

            if (mAdapter == null || path == null) {
                return;
            }
            if (mFileSearchFinished) {
                if (path.size() == 0) {
                    showEmptyView();
                }
                //finally refresh the listView
                mAdapter.setData(path);
                return;
            }

            // refresh the listView
            if (path.size() <= MIN_REFRESH_LIST_COUNT) {
                mAdapter.setData(path);
            }
            mHandler.postDelayed(this, 1);
        }
    }
}