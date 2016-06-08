package com.filemanager.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;
import base.util.CustomToast;
import base.util.FileUtil;
import base.util.PreferenceDefault;
import base.util.ViewUtil;
import base.util.os.EnvironmentUtil;
import com.filemanager.FileHolderListAdapter;
import com.filemanager.R;
import com.filemanager.dialogs.ChangeDialogButtonEvent;
import com.filemanager.files.DirectoryContents;
import com.filemanager.files.DirectoryScanner;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.SimpleFileListFragment;
import com.filemanager.util.MimeTypes;
import com.filemanager.util.Preference;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Administrator on 2016/6/2.
 */
public class CutAndCopyLayout extends LinearLayout {


    private int mCurrentSort = Preference.SORT_TYPE_DEFAULT;

    private String mPath;
    private String mExSdPath;

    private Context mContext;
    private PathBar mPathBar;
    private ViewFlipper mFlipper;
    private TextView mSelectSdTextView;
    private ListView mListView;

    private FileHolderListAdapter mAdapter;
    private DirectoryScanner mScanner;
    private ArrayList<FileHolder> mFiles = new ArrayList<FileHolder>();

    public CutAndCopyLayout(Context context) {
        super(context);
        mContext = context;
    }

    public CutAndCopyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    private void initViews() {
        Preference preference = new Preference(mContext);
        mCurrentSort = preference.getInt(Preference.PREFS_KEY_SORT_TYPE, Preference.SORT_TYPE_DEFAULT);

        mFlipper = (ViewFlipper) findViewById(R.id.flipper);
        mSelectSdTextView = (TextView) findViewById(R.id.tv_select_sd);
        mSelectSdTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mScanner != null && mScanner.isRunning()) {
                    mScanner.cancel();
                }
                initSdcardList();
            }
        });
        mPathBar = (PathBar) findViewById(R.id.pathbar);
        mListView = (ListView) findViewById(R.id.dialog_list);
        mListView.setEmptyView(findViewById(R.id.tv_empty));

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    mAdapter.setScrolling(false);
                    startUpdatingFileIcons();
                } else {
                    mAdapter.setScrolling(true);
                    stopUpdatingFileIcons();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });
        mListView.requestFocus();
        mListView.requestFocusFromTouch();


        mPathBar.setOnDirectoryChangedListener(new PathBar.OnDirectoryChangedListener() {
            @Override
            public void directoryChanged(File newCurrentDir, FileHolder holder) {
                open(new FileHolder(newCurrentDir, mContext));
            }
        });

        mAdapter = new FileHolderListAdapter(mFiles, mContext);
        mAdapter.setPadding(ViewUtil.dip2px(mContext, 5));
        mAdapter.setOnlyShowDir(true);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i >= mAdapter.getCount()) {
                    return;
                }
                FileHolder item = (FileHolder) mAdapter.getItem(i);
                if (mPathBar.getVisibility() != VISIBLE) {
                    mPathBar.setVisibility(VISIBLE);
                }
                if (mPathBar.getInitialDirectory() == null) {
                    mPathBar.setInitialDirectory(item.getFile().getAbsolutePath());
                } else {
                    openInformingPathBar(item);
                    ChangeDialogButtonEvent event = new ChangeDialogButtonEvent();
                    event.isRootPath = false;
                    EventBus.getDefault().post(event);
                }
            }
        });

        setLoading(false);
        initSdcardList();
    }

    private void initSdcardList() {
        mPath = null;
        mPathBar.setVisibility(INVISIBLE);
        mPathBar.switchToStandardInput();
        String internalSdcard = EnvironmentUtil.getStoragePath(mContext, false);
        mExSdPath = EnvironmentUtil.getStoragePath(mContext, true);
        mFiles.clear();
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.ic_sdcard);
        if (!TextUtils.isEmpty(internalSdcard)) {
            mFiles.add(new FileHolder(new File(internalSdcard), drawable, mContext));
        } else {
            mFiles.add(new FileHolder(new File("/"), drawable, mContext));
        }
        if ((Build.VERSION.SDK_INT < 19 || Build.VERSION.SDK_INT >= 21) && !TextUtils.isEmpty(mExSdPath)) {
            mFiles.add(new FileHolder(new File(mExSdPath), drawable, mContext));
        }
        mAdapter.notifyDataSetChanged();
        ChangeDialogButtonEvent event = new ChangeDialogButtonEvent();
        event.isRootPath = true;
        EventBus.getDefault().post(event);
    }

    public void openInformingPathBar(FileHolder item) {
        if (mPathBar == null) {
            open(item);
        } else {
            mPathBar.cd(item.getFile());
        }
    }

    private void startUpdatingFileIcons() {
        mAdapter.startProcessingThumbnailLoaderQueue();
    }

    private void stopUpdatingFileIcons() {
        mAdapter.stopProcessingThumbnailLoaderQueue();
    }

    public final String getPath() {
        return mPath;
    }

    private void open(FileHolder f) {
        if (!f.getFile().exists())
            return;

        if (f.getFile().isDirectory()) {
            openDir(f);
        }
    }

    protected void openDir(FileHolder fileholder) {
        // Avoid unnecessary attempts to load.
        if (fileholder.getFile().getAbsolutePath().equals(getPath()))
            return;

        setPath(fileholder.getFile());
        refresh();
    }

    public final void setPath(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            mPath = dir.getAbsolutePath();
        }
    }


    protected DirectoryScanner renewScanner() {

        mScanner = new DirectoryScanner(new File(mPath), mContext,
                new FileListMessageHandler(),
                MimeTypes.newInstance(mContext),
                "", "", false, true);
        return mScanner;
    }

    public void refresh() {
        if (mScanner != null && mScanner.isRunning()) {
            mScanner.cancel();
        }
        mScanner = null;

        // Indicate loading and start scanning.
        setLoading(true);
        renewScanner().start();
        mAdapter.clearFileChildrenCache();
    }

    private void setLoading(boolean show) {
        mFlipper.setDisplayedChild(show ? 0 : 1);
    }

    private class FileListMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case DirectoryScanner.MESSAGE_SHOW_DIRECTORY_CONTENTS:
                        DirectoryContents c = (DirectoryContents) msg.obj;
                        mFiles.clear();
                        mFiles.addAll(c.listSdCard);
                        mFiles.addAll(c.listDir);
                        mFiles.addAll(c.listFile);

                        if (mCurrentSort == Preference.SORT_TYPE_NAME) {
                            Collections.sort(mFiles, new SimpleFileListFragment.ComparatorByAlphabet());
                        } else if (mCurrentSort == Preference.SORT_TYPE_MODIFY_TIME) {
                            Collections.sort(mFiles, new SimpleFileListFragment.ComparatorByLastModified());
                        }

                        mAdapter.notifyDataSetChanged();

                        // Reset list position.
                        if (mFiles.size() > 0) {
                            mListView.setSelection(0);
                        }
                        setLoading(false);

                        break;
                    case DirectoryScanner.MESSAGE_SET_PROGRESS:
                        // Irrelevant.
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getExSdPath() {
        return mExSdPath;
    }

    public static boolean checkExSdCardWritable(Context context, String sdPath) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (PreferenceDefault.getTreeUris(context.getApplicationContext()).length == 0) {
                return false;
            } else {
                String path = FileUtil.getFullPathFromTreeUri(PreferenceDefault.getTreeUris(context)[0], context.getApplicationContext());
                boolean result = false;
                if (sdPath.contains(path)) {
                    try {
                        result = FileUtil.isWritableNormalOrSaf(new File(path), context);

                    } catch (Exception e) {
                        PreferenceDefault.setTreeUris(context, "");
                    }
                } else {
                    CustomToast.show(context, context.getResources().getString(R.string.file_path_no_permission_message, sdPath), Toast.LENGTH_SHORT);
                }
                return result;
            }
        }

        return true;
    }
}
