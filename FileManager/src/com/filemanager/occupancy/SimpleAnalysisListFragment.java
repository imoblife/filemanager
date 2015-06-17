package com.filemanager.occupancy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import base.util.ui.titlebar.ISearchBarActionListener;
import base.util.ui.titlebar.ITitlebarActionMenuListener;
import com.filemanager.PreferenceActivity;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.util.CopyHelper;
import com.filemanager.util.FileUtils;
import com.filemanager.util.Preference;
import com.filemanager.view.PathBar;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by wuhao on 2015/6/15.
 */
public class SimpleAnalysisListFragment extends StorageListFragment implements
        ITitlebarActionMenuListener {
    private static final String INSTANCE_STATE_PATHBAR_MODE = "pathbar_mode";

    private static final int MENU_ID_SORT = 253;
    private static final int MENU_ID_STORAGE_ANALYSIS = 254;

    private static final int SORT_BY_DEFAULT = Preference.SORT_TYPE_DEFAULT;
    private static final int SORT_BY_NAME = Preference.SORT_TYPE_NAME;
    private static final int SORT_BY_TIME = Preference.SORT_TYPE_MODIFY_TIME;

    protected static final int REQUEST_CODE_MULTISELECT = 2;



    private PathBar mPathBar;
    private boolean mActionsEnabled = true;

    private int mSingleSelectionMenu = R.menu.context;
    private int mMultiSelectionMenu = R.menu.multiselect;

    private LinearLayout mSearchActionBarLayout;

    private Handler mHandler;

    private Preference mPreference;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.storage_filelist_browse, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Pathbar init.
        mPathBar = (PathBar) view.findViewById(R.id.pathbar);
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

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    default:
                        break;
                }
            }
        };
    }

    @Override
    protected ArrayList<FileTreeNode<String>> getFileList(FileTreeNode<String> parentNode) {
        mPathBar.addNode(parentNode);
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

        FileHolder item = (FileHolder) mAdapter.getItem(position);

        openInformingPathBar(item);
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
                Log.e("wuhao"," refresh return==>>");
                return;
            }
            setLoading(true);
            mCurrentNode = fileholder.getFileNode();
//            mFiles.clear();
//            mFiles.addAll(getFileList(mCurrentNode));
            mAdapter.clearCache();
            mAdapter.setNodeData(getFileList(mCurrentNode));
            mAdapter.notifyDataSetChanged();
            getListView().setOnItemLongClickListener(new FileHolderLongClickListener());
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
                FileHolder holder = (FileHolder) mAdapter.getItem(position);
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

        public DeleteDialog(FileHolder holder) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(holder.getFile().getName());
            builder.setItems(new String[]{"delete"}, this);

            alertDialog = builder.create();
            alertDialog.setCancelable(true);
        }

        public void show() {
            alertDialog.show();
        }

        public void onClick(DialogInterface dialog, int actionId) {

            //delete file
            alertDialog.dismiss();
        }
    }
}
