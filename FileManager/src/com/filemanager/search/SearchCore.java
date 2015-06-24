package com.filemanager.search;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.Queue;

import com.filemanager.R;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

/**
 * Provides the search core, used by every search subsystem that provides results.
 * 
 * @author George Venios
 * 
 */
public class SearchCore {
	private String mQuery;
	private Uri mContentURI;
	private Context mContext;
	/** See {@link #setRoot(File)} */
	private File root = Environment.getExternalStorageDirectory();

	private int mResultCount = 0;
	private int mMaxResults = -1;
	
	private long mMaxNanos = -1;
	private long mStart;
	private boolean isRun;

	public SearchCore(Context context) {
		mContext = context;
	}


	public void setQuery(String q) {
		mQuery = q;
	}

	public String getQuery() {
		return mQuery;
	}

	/**
	 * Set the first directory to recursively search.
	 * 
	 * @param root
	 *            The directory to search first.
	 */
	public void setRoot(File root) {
		this.root = root;
	}

	/**
	 * Set the content URI, of which the results are. Used for operations on the correct search content providers.
	 * 
	 * @param URI
	 *            The URI.
	 */
	public void setURI(Uri URI) {
		mContentURI = URI;
	}

	/**
	 * Set the maximum number of results to get before search ends.
	 * 
	 * @param i
	 *            Zero or less will be ignored. The desired number of results.
	 */
	public void setMaxResults(int i) {
		mMaxResults = i;
	}
	
	/**
	 * Call this to start the search stopwatch. First call of this should be right before the call to {@link #search(File)}.
	 * @param maxNanos The search duration in nanos.
	 */
	public void startClock(long maxNanos){
		mMaxNanos = maxNanos;
		mStart = System.nanoTime();
	}

	private void insertResult(File f) {
        mResultCount++;
		ContentValues values = new ContentValues();

		if (mContentURI == SearchResultsProvider.CONTENT_URI) {
            // for 5.2.0 use no db to notify activity update ui
            SearchResultContainer.getInstance().setResult(mResultCount);
            SearchResultContainer.getInstance().addFilePath(f.getAbsolutePath());
        } else if (mContentURI == SearchSuggestionsProvider.CONTENT_URI) {
			values.put(SearchManager.SUGGEST_COLUMN_ICON_1,
					f.isDirectory() ? R.drawable.file_ic_launcher
							: R.drawable.ic_launcher_file);
			values.put(SearchManager.SUGGEST_COLUMN_TEXT_1, f.getName());
			values.put(SearchManager.SUGGEST_COLUMN_TEXT_2, f.getAbsolutePath());
			values.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                    f.getAbsolutePath());
            mContext.getContentResolver().insert(mContentURI, values);
        }
	}

	/**
	 * Reset the results of the previous queries.
	 * 
	 * @return The previous result count.
	 */
	public int dropPreviousResults() {
		mResultCount = 0;
		return mContext.getContentResolver().delete(mContentURI, null, null);
	}

    public void search(File dir) {
        Queue<File> dirList = new LinkedList<>();
        dirList.offer(dir);

        while (!dirList.isEmpty()) {
            File dirCurrent = dirList.poll();

            for (File f : dirCurrent.listFiles()) {
                if (!isRun()) {
                    throw new RuntimeException();
                }
                if (f == null) {
                    continue;
                }
                if (f.getName().toLowerCase().contains(mQuery.toLowerCase())) {
                    insertResult(f);
                }
                if (!isRun()) {
                    throw new RuntimeException();
                }
                if (f.isDirectory()) {
                    dirList.offer(f);
                }
            }
        }
    }

	/**
	 * @param f1
	 * @param f2
	 * @return If f1 is child of f2. Also true if f1 equals f2.
	 */
	private boolean isChildOf(File f1, File f2) {
		return f2.getAbsolutePath().startsWith(f1.getAbsolutePath());
	}

	public boolean isRun() {
		return isRun;
	}

	public void setRun(boolean isRun) {
		this.isRun = isRun;
	}

    public int getResultCount() {
        return mResultCount;
    }
}