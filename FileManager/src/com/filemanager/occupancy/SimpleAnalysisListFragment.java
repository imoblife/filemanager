package com.filemanager.occupancy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import base.util.os.StatFsUtil;
import base.util.ui.titlebar.ISearchBarActionListener;
import base.util.ui.titlebar.ITitlebarActionMenuListener;
import com.filemanager.PreferenceActivity;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.util.*;
import com.filemanager.view.PathBar;
import com.readystatesoftware.systembartint.SystemBarTintUtil;
import imoblife.view.ListViewScrollHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wuhao on 2015/6/15.
 */
public class SimpleAnalysisListFragment extends StorageListFragment implements
        ITitlebarActionMenuListener {
    private static final String INSTANCE_STATE_PATHBAR_MODE = "pathbar_mode";

    private static final int MENU_ID_SORT = 253;
    private static final int MENU_ID_STORAGE_ANALYSIS = 254;

    private static final int MSG_REFRESH_TREENODE = 201;

    protected static final int REQUEST_CODE_MULTISELECT = 2;
    private ListViewScrollHelper mListViewScrollHelper;
    private RelativeLayout mTitleContent;
    private ListView mListView;

    private int mOffset = 0;
    private PathBar mPathBar;
    private boolean mActionsEnabled = true;

    private int mSingleSelectionMenu = R.menu.context;
    private int mMultiSelectionMenu = R.menu.multiselect;

    private LinearLayout mSearchActionBarLayout;

    private RelativeLayout mStorageAnalysisLayout;
    private TextView mCurrentSizeTextView;
    private TextView mAvailSizeTextView;
    private TextView mTotalSizeTextView;

    private FileHolderLongClickListener mLongClickListener;

    private Handler mHandler;

    private Preference mPreference;

    private ExecutorService mExecutors;

    private LinearLayout mTitleLayout;
    private int mTitleHeight;
    private RelativeLayout mHeaderLayout;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mHeaderLayout = (RelativeLayout) inflater.inflate(R.layout.place_holder_header, null);
        return inflater.inflate(R.layout.storage_filelist_browse, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= 19) {
            mOffset = SystemBarTintUtil.getStatusBarHeight(getActivity());
        }
        mListView = getListView();
        mListViewScrollHelper = new ListViewScrollHelper(mListView);
        mListView.addHeaderView(mHeaderLayout);
        super.onViewCreated(view, savedInstanceState);

        mExecutors = Executors.newSingleThreadExecutor();
        // Pathbar init.
        mPathBar = (PathBar) view.findViewById(R.id.pathbar);
        mPathBar.setStorageAnalysis(true);
        mTitleLayout = (LinearLayout) view.findViewById(R.id.titlebar);
        mTitleContent = (RelativeLayout) view.findViewById(R.id.rl_title);

        mTitleLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mTitleHeight == 0) {
                            mTitleHeight = mTitleLayout.getHeight();
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                                mTitleLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                mTitleLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                        }
                    }
                });
        mHeaderLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewGroup.LayoutParams params = mHeaderLayout.getLayoutParams();
                params.height = mTitleHeight + UIUtils.dip2px(getContext(), 48);
                mHeaderLayout.setLayoutParams(params);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    mHeaderLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mHeaderLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
        if (savedInstanceState == null)
            mPathBar.setInitialDirectory(getPath());
        else
            mPathBar.cd(getPath());
        mPathBar.setOnDirectoryChangedListener(new PathBar.OnDirectoryChangedListener() {

            @Override
            public void directoryChanged(File newCurrentDir, FileHolder fileHolder) {
                open(fileHolder == null ? new FileHolder(newCurrentDir, getActivity()) : fileHolder);
            }
        });
        if (savedInstanceState != null
                && savedInstanceState.getBoolean(INSTANCE_STATE_PATHBAR_MODE))
            mPathBar.switchToManualInput();
        // Removed else clause as the other mode is the default. It seems faster
        // this way on Nexus S.

        initContextualActions();

        initSearchActionBar(view);

        initCurrentSort(getContext());

        mLongClickListener = new FileHolderLongClickListener();
        getListView().setOnItemLongClickListener(mLongClickListener);

        mStorageAnalysisLayout = (RelativeLayout) view.findViewById(R.id.bottom_layout);
        mCurrentSizeTextView = (TextView) view.findViewById(R.id.tv_current_info);
        mAvailSizeTextView = (TextView) view.findViewById(R.id.tv_avail_info);
        mTotalSizeTextView = (TextView) view.findViewById(R.id.tv_total_info);

        hideBottomLayout();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH_TREENODE:
                        if(mAdapter == null){
                            return;
                        }
                        mAdapter.clearCache();
                        mAdapter.setNodeData(getFileList(mCurrentNode));
                        mAdapter.notifyDataSetChanged();
                        setLoading(false);
                        break;
                    default:
                        break;
                }
            }
        };


    }

    @Override
    void onScrollCall(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        if (mPathBar == null || mTitleLayout == null ||  mTitleContent == null || mListViewScrollHelper == null) {
            return;
        }


        if (firstVisibleItem > mListViewScrollHelper.getOldVisibleItem()) {
            mListViewScrollHelper.hideQuickReturnTopAnim(mTitleContent, 0, -mTitleHeight + mOffset, firstVisibleItem);
            mListViewScrollHelper.showQuickReturnBottomAnim(mStorageAnalysisLayout, 0, UIUtils.dip2px(getContext(), 40), firstVisibleItem);
        } else if (firstVisibleItem < mListViewScrollHelper.getOldVisibleItem()) {
            mListViewScrollHelper.showQuickReturnTopAnim(mTitleContent, -mTitleHeight + mOffset, 0, firstVisibleItem);
            mListViewScrollHelper.showQuickReturnBottomAnim(mStorageAnalysisLayout, UIUtils.dip2px(getContext(), 40), 0, firstVisibleItem);
        }

    }


    @Override
    protected ArrayList<FileTreeNode<String>> getFileList(FileTreeNode<String> parentNode) {
        mPathBar.addNode(parentNode);
        updateCurrentPageInfo(parentNode);
        return super.getFileList(parentNode);
    }

    private void initCurrentSort(Context context) {
        mPreference = new Preference(context);
        mCurrentSort = mPreference.getInt(Preference.PREFS_KEY_SORT_TYPE, Preference.SORT_TYPE_DEFAULT);
    }

    private void initSearchActionBar(View root) {
        mSearchActionBarLayout = (LinearLayout) root.findViewById(R.id.titlebar_ad_ll);
        mSearchActionBarLayout.setVisibility(View.VISIBLE);
        ImageView search = (ImageView) mSearchActionBarLayout.findViewById(R.id.titlebar_ad_iv);
        search.setImageResource(R.drawable.ic_action_search);
        mSearchActionBarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() == null) {
                    return;
                }
                if (getActivity() instanceof ISearchBarActionListener) {
                    ((ISearchBarActionListener) getActivity()).onSearch();
                }
            }
        });
    }

    /**
     * Override this to handle initialization of list item long clicks.
     */
    void initContextualActions() {
//        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = new MenuInflater(getActivity());

        // Obtain context menu info
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            e.printStackTrace();
            return;
        }

//        MenuUtils.fillContextMenu((FileHolder) mAdapter.getItem(info.position),
//                menu, mSingleSelectionMenu, inflater, getActivity());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        FileHolder fh = (FileHolder) mAdapter
                .getItem(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
//        return MenuUtils.handleSingleSelectionAction(this, item, fh,
//                getActivity());
        return false;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        FileHolder item = (FileHolder) l.getAdapter().getItem(position);
        mPreviousPosition = getListView().getFirstVisiblePosition();
        openInformingPathBar(item);
        mPathBar.updatePosition(mPreviousPosition);
    }

    @Override
    protected void selectInList(File selectFile) {
        getListView().setSelection(mPathBar.getPathPosition(mPreviousDirectory));
    }

    /**
     * Use this to open files and folders using this fragment. Appropriately
     * handles pathbar updates.
     *
     * @param item
     *            The dir/file to open.
     */
    public void openInformingPathBar(FileHolder item) {
        if (mPathBar == null)
            open(item);
        else
            mPathBar.cd(item.getFile(), item);
    }

    /**
     * Point this Fragment to show the contents of the passed file.
     *
     * @param f
     *            If same as current, does nothing.
     */
    private void open(FileHolder f) {
        if (!f.getFile().exists())
            return;

        if (f.getFile().isDirectory()) {
            openDir(f);
        } else if (f.getFile().isFile()) {
            openFile(f);
        }
    }

    private void openFile(FileHolder fileholder) {
        FileUtils.openFile(fileholder, getActivity());
    }

    /**
     * Attempts to open a directory for browsing. Override this to handle folder
     * click behavior.
     *
     * @param fileholder
     *            The holder of the directory to open.
     */
    protected void openDir(FileHolder fileholder) {
        // Avoid unnecessary attempts to load.
        if (fileholder.getFile().getAbsolutePath().equals(getPath()))
            return;

        setPath(fileholder.getFile());
        //use treeNode to refresh
        if (mRootNode == null) {
            refresh();
        } else {
            //data is already complete,just update ui
            if (fileholder.getFileNode() == null || fileholder.getFileNode().children == null) {
                return;
            }
            setLoading(true);
            hideBottomLayout();
            mCurrentNode = fileholder.getFileNode();
            mAdapter.clearCache();
            mAdapter.setNodeData(getFileList(mCurrentNode));
            mAdapter.notifyDataSetChanged();
            if (mPreviousDirectory != null) {
                selectInList(mPreviousDirectory);
            }
            setLoading(false);
        }
    }

    protected void setLongClickMenus(int singleSelectionResource,
                                     int multiSelectionResource) {
        mSingleSelectionMenu = singleSelectionResource;
        mMultiSelectionMenu = multiSelectionResource;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.simple_file_list, menu);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // We only know about ".nomedia" once scanning is finished.
        boolean showMediaScanMenuItem = PreferenceActivity
                .getMediaScanFromPreference(getActivity());
            menu.findItem(R.id.menu_media_scan_include).setVisible(false);
            menu.findItem(R.id.menu_media_scan_exclude).setVisible(false);

        if (CopyHelper.get(getActivity()).canPaste()) {
            menu.findItem(R.id.menu_paste).setVisible(true);
        } else {
            menu.findItem(R.id.menu_paste).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return handleOptionMenu(item.getItemId());
    }

    private boolean handleOptionMenu(int id) {
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Automatically refresh to display possible changes done through the
        // multiselect fragment.
        if (requestCode == REQUEST_CODE_MULTISELECT)
            refresh();
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void browseToHome() {
        mPathBar.cd(mPathBar.getInitialDirectory());
    }

    public boolean pressBack() {
        if (mCurrentNode != null && mCurrentNode.parent != null) {
            return mPathBar.pressBack(mCurrentNode.parent);
        } else {
            return mPathBar.pressBack();
        }
    }

    public class FileHolderLongClickListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (mAdapter != null) {
                FileHolder holder = (FileHolder) parent.getAdapter().getItem(position);
                DeleteDialog deleteDialog = new DeleteDialog(holder);
                deleteDialog.show();
                return true;
            }
            return false;
        }
    }

    /**
     * Set whether to show menu and selection actions. Must be set before
     * OnViewCreated is called.
     *
     * @param enabled
     */
    public void setActionsEnabled(boolean enabled) {
        mActionsEnabled = enabled;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(INSTANCE_STATE_PATHBAR_MODE,
                mPathBar.getMode() == PathBar.Mode.MANUAL_INPUT);
    }

    public void onTitlebarActionMenuClick(int position) {
        if (position == 0) {
            handleOptionMenu(R.id.menu_multiselect);
        } else if (position == 1) {
            handleOptionMenu(R.id.menu_create_folder);
        } else if (position == 2) {
            handleOptionMenu(MENU_ID_SORT);
        } else if (position == 3) {
            handleOptionMenu(MENU_ID_STORAGE_ANALYSIS);
        }
    }

    private class DeleteDialog implements
            android.content.DialogInterface.OnClickListener {
        private AlertDialog alertDialog;
        private FileHolder mFileHolder;

        public DeleteDialog(FileHolder holder) {
            mFileHolder = holder;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(holder.getFile().getName());
            builder.setItems(new String[]{getString(R.string.storage_analysis_delete)}, this);

            alertDialog = builder.create();
            alertDialog.setCancelable(true);
        }

        public void show() {
            alertDialog.show();
        }

        public void onClick(DialogInterface dialog, int actionId) {
            ConfirmDialog deleteDialog = new ConfirmDialog(mFileHolder);
            deleteDialog.show();
            //delete file
            alertDialog.dismiss();
        }
    }

    private class ConfirmDialog {
        private AlertDialog alertDialog;
        private FileTreeNode<String> node;

        public ConfirmDialog(FileHolder holder) {
            node = holder.getFileNode();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.really_delete, holder.getName()));
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RefreshTreeNodeRunnable refreshTreeNodeRunnable = new RefreshTreeNodeRunnable(node, mHandler);
                    setLoading(true);
                    hideBottomLayout();
                    mExecutors.execute(refreshTreeNodeRunnable);
                }
            });
            builder.setIcon(holder.getIcon());
            builder.setNegativeButton(android.R.string.cancel, null);

            alertDialog = builder.create();
            alertDialog.setCancelable(true);
        }

        public void show() {
            alertDialog.show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_REFRESH_TREENODE);
        mHandler = null;
    }

    class RefreshTreeNodeRunnable implements Runnable{
        /**
         * If 0 some failed, if 1 all succeeded.
         */
        private int mResult = 1;

        FileTreeNode<String> deleteNode;
        Handler handler;

        public RefreshTreeNodeRunnable(FileTreeNode<String> deleteNode, Handler handler) {
            this.deleteNode = deleteNode;
            this.handler = handler;
        }

        private void recursiveDelete(File file) {
            File[] files = file.listFiles();
            if (files != null && files.length != 0)
                // If it's a directory delete all children.
                for (File childFile : files) {
                    if (childFile.isDirectory()) {
                        recursiveDelete(childFile);
                    } else {
                        mResult *= childFile.delete() ? 1 : 0;
                    }
                }

            // And then delete parent. -- or just delete the file.
            mResult *= file.delete() ? 1 : 0;
        }

        @Override
        public void run() {

            FileTreeNode<String> parent = deleteNode.parent;
            long deleteSize = deleteNode.size;
            try {

                recursiveDelete(deleteNode.data);

                if (parent != null) {
                    parent.children.remove(deleteNode);
                }

                FileTreeNode<String> tmpNode = deleteNode;
                while (tmpNode.parent != null) {
                    FileTreeNode<String> tmpParent = tmpNode.parent;
                    tmpParent.size = tmpParent.size - deleteSize;
                    tmpNode = tmpParent;
                }
            } catch (Exception e) {

            }

            Message msg = handler.obtainMessage(MSG_REFRESH_TREENODE);
            msg.sendToTarget();
        }
    }

    private void hideBottomLayout(){
        mStorageAnalysisLayout.setVisibility(View.GONE);
    }

    private void updateCurrentPageInfo(FileTreeNode<String> currentTreeNode) {

        File file = currentTreeNode.data;
        if (file.exists()) {
            mStorageAnalysisLayout.setVisibility(View.VISIBLE);
            mCurrentSizeTextView.setText(Formatter.formatFileSize(getContext(), currentTreeNode.size));

            String format1 = getResources().getString(R.string.storage_analysis_avail_size);
            String result1 = String.format(format1, Formatter.formatFileSize(getContext(), StatFsUtil.getFreeSdcard(getContext())));
            mAvailSizeTextView.setText(result1);
            String format2 = getResources().getString(R.string.storage_analysis_total_size);
            String result2 = String.format(format2, Formatter.formatFileSize(getContext(), StatFsUtil.getTotalSdcard(getContext())));
            mTotalSizeTextView.setText(result2);
        } else {
            mStorageAnalysisLayout.setVisibility(View.GONE);
        }
    }
}
