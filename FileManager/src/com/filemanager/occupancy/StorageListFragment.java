package com.filemanager.occupancy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import base.util.ui.fragment.BaseListFragment;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.util.CopyHelper;
import com.filemanager.util.Preference;
import com.intents.FileManagerIntents;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by wuhao on 2015/6/15.
 */
public abstract class StorageListFragment extends BaseListFragment {
    private static final String INSTANCE_STATE_PATH = "path";
    private static final String INSTANCE_STATE_FILES = "files";
    File mPreviousDirectory = null;
    int mPreviousPosition = 0;
    private Handler mHandler = new Handler();

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
        }
    };

    protected FileSizeHolderListAdapter mAdapter;
    protected StorageScanner mScanner;
    protected ArrayList<FileHolder> mFiles = new ArrayList<FileHolder>();
    private String mPath;
    private String mFilename;

    private ViewFlipper mFlipper;
    private TextView mLoadingDefaultTextView;
    private TextView mLoadingTextView;
    private File mCurrentDirectory;
    private View mClipboardInfo;
    private TextView mClipboardContent;
    private TextView mClipboardAction;

    protected FileTreeNode<String> mRootNode;
    protected FileTreeNode<String> mCurrentNode;

    protected int mCurrentSort = Preference.SORT_TYPE_DEFAULT;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(INSTANCE_STATE_PATH, mPath);
        outState.putParcelableArrayList(INSTANCE_STATE_FILES, mFiles);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.filelist, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        // Set auto refresh on preference change.
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(preferenceListener);

        // Set list properties
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    mAdapter.setScrolling(false);
                } else
                    mAdapter.setScrolling(true);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });
        getListView().requestFocus();
        getListView().requestFocusFromTouch();

        // Init flipper
        mFlipper = (ViewFlipper) view.findViewById(R.id.flipper);
        mLoadingDefaultTextView = (TextView) view.findViewById(R.id.tv_loading_default);
        mLoadingDefaultTextView.setVisibility(View.GONE);
        mLoadingTextView = (TextView) view.findViewById(R.id.tv_loading);
        mLoadingTextView.setVisibility(View.VISIBLE);
        mClipboardInfo = view.findViewById(R.id.clipboard_info);
        mClipboardContent = (TextView) view
                .findViewById(R.id.clipboard_content);

        mClipboardContent.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (CopyHelper.get(getActivity()).canPaste())
                    CopyHelper.get(getActivity()).paste(new File(getPath()),
                            new CopyHelper.OnOperationFinishedListener() {
                                public void operationFinished(boolean success) {
                                    refresh();

                                }
                            });
                else {
                    Toast.makeText(getActivity(), R.string.nothing_to_paste,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        mClipboardAction = (TextView) view.findViewById(R.id.clipboard_action);
        mClipboardAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CopyHelper.get(getActivity()).clear();
                updateClipboardInfo();

                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                // ActionbarRefreshHelper
                // .activity_invalidateOptionsMenu(getActivity());
            }
        });

        // Get arguments
        if (savedInstanceState == null) {
            mPath = getArguments().getString(FileManagerIntents.EXTRA_DIR_PATH);
            mFilename = getArguments().getString(
                    FileManagerIntents.EXTRA_FILENAME);
        } else {
            mPath = savedInstanceState.getString(INSTANCE_STATE_PATH);
            mFiles = savedInstanceState
                    .getParcelableArrayList(INSTANCE_STATE_FILES);
        }
        pathCheckAndFix();
        renewScanner();
        mAdapter = new FileSizeHolderListAdapter(getActivity());

        mHandler.post(new UpdateRunnable());
        setListAdapter(mAdapter);
        mScanner.start();

    }

    @Override
    public void onDestroy() {
        mScanner.cancel();
        super.onDestroy();
    }

    /**
     * Reloads {@link #mPath}'s contents.
     */
    public void refresh() {
        // Prevent NullPointerException caused from this getting called
        // after we have finish()ed the activity.
        if (getActivity() == null)
            return;

        // Cancel and GC previous scanner so that it doesn't load on top of the
        // new list.
        // Race condition seen if a long list is requested, and a short list is
        // requested before the long one loads.
        mScanner.cancel();
        mScanner = null;

        // Indicate loading and start scanning.
        setLoading(true);
        renewScanner().start();
    }

    /**
     * Make the UI indicate loading.
     */
    protected void setLoading(boolean show) {
        mFlipper.setDisplayedChild(show ? 0 : 1);
        onLoadingChanged(show);
    }

    protected void selectInList(File selectFile) {
        String filename = selectFile.getName();

        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            FileHolder it = (FileHolder) mAdapter.getItem(i);
            if (it.getName().equals(filename)) {
                getListView().setSelection(i);
                break;
            }
        }
    }

    protected StorageScanner renewScanner() {
        mScanner = new StorageScanner(new File(mPath), getActivity(),
                new FileListMessageHandler());
        return mScanner;
    }

    protected ArrayList<FileTreeNode<String>> getFileList(FileTreeNode<String> parentNode) {
        ArrayList<FileTreeNode<String>> arrayList = new ArrayList<>();
        long size = 0;
        if (parentNode.children != null && !parentNode.children.isEmpty()) {
            //sort
            Collections.sort(parentNode.children, new StorageScanner.ComparatorBySize());

            long time = System.currentTimeMillis();
            for (FileTreeNode<String> tmpNode : parentNode.children) {
                arrayList.add(tmpNode);
                size = size + tmpNode.size;
            }
        }
        mAdapter.seFileListTotalSize(size);
        return arrayList;
    }

    private class FileListMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case StorageScanner.MESSAGE_SHOW_STORAGE_ANALYSIS:

                        FileTreeNode<String> c = (FileTreeNode<String>) msg.obj;
                        mRootNode = c;
                        mCurrentNode = c;
                        mAdapter.clearCache();
                        mAdapter.setNodeData(getFileList(c));

                        mAdapter.notifyDataSetChanged();

                        if (mPreviousDirectory != null) {
                            selectInList(mPreviousDirectory);
                        } else {
                            // Reset list position.
                            if (mFiles.size() > 0)
                                getListView().setSelection(0);
                        }
                        setLoading(false);
                        updateClipboardInfo();
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateClipboardInfo() {
        if (CopyHelper.get(getActivity()).canPaste()) {
            mClipboardInfo.setVisibility(View.VISIBLE);
            int count = CopyHelper.get(getActivity()).getItemsCount();
            if (CopyHelper.Operation.COPY.equals(CopyHelper.get(getActivity())
                    .getOperationType())) {
                // mClipboardContent.setText(getResources().getQuantityString(
                // R.plurals.clipboard_info_items_to_copy, count, count));
                mClipboardContent.setText(getString(R.string.paste));
                mClipboardAction.setText(getString(R.string.clipboard_dismiss));
            } else if (CopyHelper.Operation.CUT.equals(CopyHelper.get(
                    getActivity()).getOperationType())) {
                mClipboardContent.setText(getResources().getQuantityString(
                        R.plurals.clipboard_info_items_to_move, count, count));
                mClipboardAction.setText(getString(R.string.clipboard_undo));
            }
        } else {
            mClipboardInfo.setVisibility(View.GONE);
        }
    }

    /**
     * Used to inform subclasses about loading state changing. Can be used to
     * make the ui indicate the loading state of the fragment.
     *
     * @param loading
     *            If the list started or stopped loading.
     */
    protected void onLoadingChanged(boolean loading) {
    }

    /**
     * @return The currently displayed directory's absolute path.
     */
    public final String getPath() {
        return mPath;
    }

    /**
     * This will be ignored if path doesn't pass check as valid.
     *
     * @param dir
     *            The path to set.
     */
    public final void setPath(File dir) {

        if (dir.exists() && dir.isDirectory()) {
            mPreviousDirectory = mCurrentDirectory;
            mCurrentDirectory = dir;
            mPath = dir.getAbsolutePath();

        }
    }

    private void pathCheckAndFix() {
        File dir = new File(mPath);
        // Sanity check that the path (coming from extras_dir_path) is indeed a
        // directory
        if (!dir.isDirectory() && dir.getParentFile() != null) {
            // remember the filename for picking.
            mFilename = dir.getName();
            dir = dir.getParentFile();
            mPath = dir.getAbsolutePath();
        }
    }

    public String getFilename() {
        return mFilename;
    }


    private class UpdateRunnable implements Runnable {

        @Override
        public void run() {
            if (mFlipper != null && mFlipper.getChildAt(0).getVisibility() != View.VISIBLE || mScanner == null) {
                mLoadingDefaultTextView.setVisibility(View.VISIBLE);
                mLoadingTextView.setVisibility(View.GONE);
                return;
            }
            if(getActivity() == null){
                return;
            }
            String tmp = getResources().getString(R.string.storage_analysis_scan_count);
            String tmpCount = String.format(tmp, mScanner.getResultCount() + "");
            if (mLoadingTextView != null) {
                mLoadingTextView.setText(tmpCount);
            }
            mHandler.postDelayed(this, 100);
        }
    }
}
