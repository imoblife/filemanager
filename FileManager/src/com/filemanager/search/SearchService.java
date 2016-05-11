package com.filemanager.search;

import java.io.File;

import com.intents.FileManagerIntents;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import de.greenrobot.event.EventBus;

/**
 * Service that asynchronously executes file searches.
 * 
 * @author George Venios.
 * 
 */
public class SearchService extends IntentService {
	/**
	 * Used to inform the SearchableActivity of search start and end.
	 */
	private LocalBroadcastManager lbm;
	private SearchCore searcher;

	public SearchService() {
		super("SearchService");
	}

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
	public void onCreate() {
		super.onCreate();
		EventBus.getDefault().register(this);
		lbm = LocalBroadcastManager.getInstance(getApplicationContext());
		searcher = new SearchCore(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {

			// The search query
			searcher.setQuery(intent
					.getStringExtra(FileManagerIntents.EXTRA_SEARCH_QUERY));

			// Set initial path. To be searched first!
			String path = intent
					.getStringExtra(FileManagerIntents.EXTRA_SEARCH_INIT_PATH);
			File root = null;
			if (path != null)
				root = new File(path);
			else
				root = new File("/");


            // Search in current path.
            searcher.dropPreviousResults();
            SearchResultContainer.getInstance().initContext(this);
            SearchResultContainer.getInstance().clear();

			// Search started, let Receivers know.
			lbm.sendBroadcast(new Intent(
					FileManagerIntents.ACTION_SEARCH_STARTED));

            searcher.setRoot(root);
            try {
                searcher.search(root);
            } catch (Exception e) {
                //Exception use for exit loop
                //do noting
            }

            // Search is over, let Receivers know.
            lbm.sendBroadcast(new Intent(
                    FileManagerIntents.ACTION_SEARCH_FINISHED).putExtra(SearchableActivity.KEY_FILE_RESULT_COUNT, searcher.getResultCount()));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTask();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
		super.onDestroy();
	}

    private void startTask() {
        searcher.setRun(true);
    }

    private void cancelAllTask() {
        searcher.setRun(false);
    }

    public void onEvent(CancelTaskEvent event){
        cancelAllTask();
    }
}