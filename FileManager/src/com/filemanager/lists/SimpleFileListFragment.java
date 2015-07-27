package com.filemanager.lists;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import base.util.ui.titlebar.ISearchBarActionListener;
import base.util.ui.titlebar.ITitlebarActionMenuListener;
import com.filemanager.FileManagerActivity;
import com.filemanager.PreferenceActivity;
import com.filemanager.R;
import com.filemanager.dialogs.CreateDirectoryDialog;
import com.filemanager.files.FileHolder;
import com.filemanager.occupancy.StorageAnalysisActivity;
import com.filemanager.util.*;
import com.filemanager.view.PathBar;
import com.filemanager.view.PathBar.Mode;
import com.filemanager.view.PathBar.OnDirectoryChangedListener;
import com.intents.FileManagerIntents;
import com.readystatesoftware.systembartint.SystemBarTintUtil;
import imoblife.view.ListViewScrollHelper;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

/**
 * A file list fragment that supports context menu and CAB selection.
 * 
 * @author George Venios
 */
public class SimpleFileListFragment extends FileListFragment implements
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

    private ListViewScrollHelper mListViewScrollHelper;
    private RelativeLayout mTitleContent;
    private ListView mListView;
    private int mOffset = 0;
    private LinearLayout mTitleLayout;
    private int mTitleHeight;
    private RelativeLayout mHeaderLayout;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        mHeaderLayout = (RelativeLayout) inflater.inflate(R.layout.place_holder_header, null);
        return inflater.inflate(R.layout.filelist_browse, null);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= 19) {
            mOffset = SystemBarTintUtil.getStatusBarHeight(getActivity());
        }
        mListView = getListView();
        mListView.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.topBar_height) + mOffset + 5, 0, 0);
        mListViewScrollHelper = new ListViewScrollHelper(mListView);
        mListView.addHeaderView(mHeaderLayout);
		super.onViewCreated(view, savedInstanceState);

		// Pathbar init.
		mPathBar = (PathBar) view.findViewById(R.id.pathbar);
		// Handle mPath differently if we restore state or just initially create
		// the view.
		/*	LinearLayout base_titlebar_ll = (LinearLayout) view.findViewById(R.id.base_titlebar_ll);
			base_titlebar_ll.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					getActivity().finish();
				}
			});*/
		if (savedInstanceState == null)
			mPathBar.setInitialDirectory(getPath());
		else
		mPathBar.cd(getPath());
		mPathBar.setOnDirectoryChangedListener(new OnDirectoryChangedListener() {

			@Override
            public void directoryChanged(File newCurrentDir, FileHolder fileHolder) {
                open(new FileHolder(newCurrentDir, getActivity()));
            }
        });
        if (savedInstanceState != null
				&& savedInstanceState.getBoolean(INSTANCE_STATE_PATHBAR_MODE))
			mPathBar.switchToManualInput();
		// Removed else clause as the other mode is the default. It seems faster
		// this way on Nexus S.

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
                params.height = mTitleHeight - mOffset;
                mHeaderLayout.setLayoutParams(params);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    mHeaderLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mHeaderLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        Bundle bundle = getArguments();
        String keyword = null;
        if (bundle != null) {
            mPathBar.setPathButtonClickable(bundle.getBoolean(FileManagerActivity.EXTRA_PATH_CLICK, true));
            keyword = bundle.getString(FileManagerActivity.EXTRA_PATH_KEYWORD);
            if (!TextUtils.isEmpty(keyword)) {
                mAdapter.setHighlightKeyword(keyword);
            }
        }
        initContextualActions();

        initSearchActionBar(view);

        initCurrentSort(getContext());

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SORT_BY_NAME:
                        if (mFiles != null && !mFiles.isEmpty()) {
                            mCurrentSort = SORT_BY_NAME;
                            mPreference.setInt(Preference.PREFS_KEY_SORT_TYPE, SORT_BY_NAME);
                            Collections.sort(mFiles, new ComparatorByAlphabet());
                            mAdapter.notifyDataSetChanged();
                        }
                        break;
                    case SORT_BY_TIME:
                        if (mFiles != null && !mFiles.isEmpty()) {
                            mCurrentSort = SORT_BY_TIME;
                            mPreference.setInt(Preference.PREFS_KEY_SORT_TYPE, SORT_BY_TIME);
                            Collections.sort(mFiles, new ComparatorByLastModified());
                            mAdapter.notifyDataSetChanged();
                        }
                        break;
                    default:
                        if (mFiles != null && !mFiles.isEmpty() && mCurrentSort != SORT_BY_DEFAULT) {
                            mCurrentSort = SORT_BY_DEFAULT;
                            mPreference.setInt(Preference.PREFS_KEY_SORT_TYPE, SORT_BY_DEFAULT);
                            refresh();
                            mAdapter.notifyDataSetChanged();
                        }
                        break;
                }
            }
        };
    }

    @Override
    void onScrollCall(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (mPathBar == null || mTitleLayout == null || mTitleContent == null || mListViewScrollHelper == null) {
            return;
        }

        if (firstVisibleItem > mListViewScrollHelper.getOldVisibleItem()) {
            mListViewScrollHelper.hideQuickReturnTopAnim(mTitleContent, 0, -mTitleHeight + mOffset, firstVisibleItem);
        } else if (firstVisibleItem < mListViewScrollHelper.getOldVisibleItem()) {
            mListViewScrollHelper.showQuickReturnTopAnim(mTitleContent, -mTitleHeight + mOffset, 0, firstVisibleItem);
        }
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
		//		if (mActionsEnabled) {
		//			if (VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
		registerForContextMenu(getListView());
		//			} else {
		//				FileMultiChoiceModeHelper multiChoiceModeHelper = new FileMultiChoiceModeHelper(
		//						mSingleSelectionMenu, mMultiSelectionMenu);
		//				multiChoiceModeHelper.setListView(getListView());
		//				multiChoiceModeHelper.setPathBar(mPathBar);
		//				multiChoiceModeHelper.setContext(this);
		//				getListView()
		//						.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		//			}
		//			setHasOptionsMenu(true);
		//		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = new MenuInflater(getActivity());

		// Obtain context menu info
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			e.printStackTrace();
			return;
		}

		MenuUtils.fillContextMenu((FileHolder) mAdapter.getItem(info.position),
                menu, mSingleSelectionMenu, inflater, getActivity());
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		FileHolder fh = (FileHolder) mAdapter
				.getItem(((AdapterContextMenuInfo) item.getMenuInfo()).position);
		return MenuUtils.handleSingleSelectionAction(this, item, fh,
				getActivity());
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
			mPathBar.cd(item.getFile());
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
		refresh();
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
		if (!mScanner.isRunning() && showMediaScanMenuItem) {
			menu.findItem(R.id.menu_media_scan_include).setVisible(
					mScanner.getNoMedia());
			menu.findItem(R.id.menu_media_scan_exclude).setVisible(
					!mScanner.getNoMedia());
		} else {
			menu.findItem(R.id.menu_media_scan_include).setVisible(false);
			menu.findItem(R.id.menu_media_scan_exclude).setVisible(false);
		}

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
		if (id == R.id.menu_create_folder) {
			CreateDirectoryDialog dialog = new CreateDirectoryDialog();
			dialog.setTargetFragment(this, 0);
			Bundle args = new Bundle();
			args.putString(FileManagerIntents.EXTRA_DIR_PATH, getPath());
			dialog.setArguments(args);
			dialog.show(getActivity().getSupportFragmentManager(),
                    CreateDirectoryDialog.class.getName());
			return true;
		} else if (id == R.id.menu_media_scan_include) {
			includeInMediaScan();
			return true;
		} else if (id == R.id.menu_media_scan_exclude) {
			excludeFromMediaScan();
			return true;
		} else if (id == R.id.menu_paste) {
			if (CopyHelper.get(getActivity()).canPaste())
				CopyHelper.get(getActivity()).paste(new File(getPath()),
						new CopyHelper.OnOperationFinishedListener() {
							public void operationFinished(boolean success) {
								refresh();

							}
						});
			else
				Toast.makeText(getActivity(), R.string.nothing_to_paste,
						Toast.LENGTH_LONG).show();
			return true;
		} else if (id == R.id.menu_multiselect) {
			Intent intent = new Intent(FileManagerIntents.ACTION_MULTI_SELECT);
			intent.putExtra(FileManagerIntents.EXTRA_DIR_PATH, getPath());
			startActivityForResult(intent, REQUEST_CODE_MULTISELECT);
            return true;
        } else if (id == MENU_ID_SORT) {
            new SortDialog();
            return true;
        } else if (id == MENU_ID_STORAGE_ANALYSIS) {
            Intent intent = new Intent();
            intent.setClass(getActivity(), StorageAnalysisActivity.class);
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    public static class ComparatorByLastModified implements Comparator<FileHolder> {
        public int compare(FileHolder f1, FileHolder f2) {
            long diff = f1.getFile().lastModified() - f2.getFile().lastModified();
            if (diff > 0)
                return -1;
            else if (diff == 0)
                return 0;
            else
                return 1;
        }

        public boolean equals(Object obj) {
            return true;
        }
    }

    public static class ComparatorByAlphabet implements Comparator<FileHolder> {
        public int compare(FileHolder f1, FileHolder f2) {
            return String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
        }
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Automatically refresh to display possible changes done through the
		// multiselect fragment.
		if (requestCode == REQUEST_CODE_MULTISELECT)
			refresh();
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void includeInMediaScan() {
		// Delete the .nomedia file.
		File file = FileUtils.getFile(mPathBar.getCurrentDirectory(),
				FileUtils.NOMEDIA_FILE_NAME);
		if (file.delete()) {
			Toast.makeText(getActivity(),
					getString(R.string.media_scan_included), Toast.LENGTH_LONG)
					.show();
		} else {
			// That didn't work.
			Toast.makeText(getActivity(), getString(R.string.error_generic),
					Toast.LENGTH_LONG).show();
		}
		refresh();
	}

	private void excludeFromMediaScan() {
		// Create the .nomedia file.
		File file = FileUtils.getFile(mPathBar.getCurrentDirectory(),
				FileUtils.NOMEDIA_FILE_NAME);
		try {
			if (file.createNewFile()) {
				Toast.makeText(getActivity(),
						getString(R.string.media_scan_excluded),
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getActivity(),
						getString(R.string.error_media_scan), Toast.LENGTH_LONG)
						.show();
			}
		} catch (IOException e) {
			// That didn't work.
			Toast.makeText(getActivity(),
					getString(R.string.error_generic) + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
		refresh();
	}

	public void browseToHome() {
		mPathBar.cd(mPathBar.getInitialDirectory());
	}

	public boolean pressBack() {
		return mPathBar.pressBack();
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
				mPathBar.getMode() == Mode.MANUAL_INPUT);
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

    private class SortDialog implements
            android.content.DialogInterface.OnClickListener {
        private AlertDialog alertDialog;

        public SortDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.file_sort_dialog_title));
            String[] items = new String[]{getString(R.string.file_sort_by_default)
                    , getString(R.string.file_sort_by_name),
                    getString(R.string.file_sort_by_time)};

            builder.setSingleChoiceItems(items, mCurrentSort, this);
            alertDialog = builder.create();
            alertDialog.show();
        }

        public void onClick(DialogInterface dialog, int actionId) {
            Message msg = null;
            if (actionId == 0) {
                msg = mHandler.obtainMessage(SORT_BY_DEFAULT);
            } else if (actionId == 1) {
                msg = mHandler.obtainMessage(SORT_BY_NAME);
            } else if (actionId == 2) {
                msg = mHandler.obtainMessage(SORT_BY_TIME);
            }
            mHandler.sendMessage(msg);
            alertDialog.dismiss();
        }
    }
}