package com.filemanager.search;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Provides the search core, used by every search subsystem that provides results.
 * 
 * @author George Venios
 * 
 */
public class SearchCore {
	private String mQuery;
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
        // for 5.2.0 use no db to notify activity update ui
        SearchResultContainer.getInstance().setResult(mResultCount);
        SearchResultContainer.getInstance().addFilePath(f.getAbsolutePath());
    }

	/**
	 * Reset the results of the previous queries.
	 * 
	 * @return The previous result count.
	 */
	public void dropPreviousResults() {
		mResultCount = 0;
	}

    public void search(File dir) {
        Queue<File> dirList = new LinkedList<>();
        dirList.offer(dir);

        while (!dirList.isEmpty()) {
            File dirCurrent = dirList.poll();
            if (dirCurrent == null) {
                continue;
            }
            File[] files = dirCurrent.listFiles();
            if (files == null || files.length == 0) {
                continue;
            }

            for (File f : files) {
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
                if (f.isDirectory() && dirCurrent.canRead()) {
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