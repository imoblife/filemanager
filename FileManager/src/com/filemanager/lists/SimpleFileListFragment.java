package com.filemanager.lists;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import base.util.*;
import base.util.ui.titlebar.ISearchBarActionListener;
import base.util.ui.titlebar.ITitlebarActionMenuListener;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.FileManagerActivity;
import com.filemanager.PreferenceActivity;
import com.filemanager.R;
import com.filemanager.dialogs.CreateDirectoryDialog;
import com.filemanager.dialogs.RenameDialog;
import com.filemanager.dialogs.SingleDeleteDialog;
import com.filemanager.files.FileHolder;
import com.filemanager.occupancy.StorageAnalysisActivity;
import com.filemanager.util.*;
import com.filemanager.util.FileUtils;
import com.filemanager.view.PathBar;
import com.filemanager.view.PathBar.Mode;
import com.filemanager.view.PathBar.OnDirectoryChangedListener;
import com.intents.FileManagerIntents;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * A file list fragment that supports context menu and CAB selection.
 * 
 * @author George Venios
 */
public class SimpleFileListFragment extends FileListFragment implements
		ITitlebarActionMenuListener,AdapterView.OnItemLongClickListener {
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
		return inflater.inflate(R.layout.filelist_browse, null);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
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

        Bundle bundle = getArguments();
        String keyword = null;
        if (bundle != null) {
            mPathBar.setPathButtonClickable(bundle.getBoolean(FileManagerActivity.EXTRA_PATH_CLICK, true));
            keyword = bundle.getString(FileManagerActivity.EXTRA_PATH_KEYWORD);
            if (!TextUtils.isEmpty(keyword)) {
                mAdapter.setHighlightKeyword(keyword);
            }
        }
        getListView().setOnItemLongClickListener(this);

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

        if (FileUtils.checkSdCardAndroid5(getContext().getApplicationContext())) {
            FileUtils.showStorageAccessDialog(this);
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
//		MenuInflater inflater = new MenuInflater(getActivity());
//
//		// Obtain context menu info
//		AdapterView.AdapterContextMenuInfo info;
//		try {
//			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
//		} catch (ClassCastException e) {
//			e.printStackTrace();
//			return;
//		}
//
//		MenuUtils.fillContextMenu((FileHolder) mAdapter.getItem(info.position),
//                menu, mSingleSelectionMenu, inflater, getActivity());
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

		FileHolder item = (FileHolder) mAdapter.getItem(position);
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

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mAdapter != null && mAdapter.getItem(position) != null) {
            new OperationDialog((FileHolder) mAdapter.getItem(position));
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
        onActivityResultLollipop(requestCode, resultCode, data);
        // multiselect fragment.
		if (requestCode == REQUEST_CODE_MULTISELECT)
			refresh();
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void includeInMediaScan() {
		// Delete the .nomedia file.
		File file = FileUtil.getFile(mPathBar.getCurrentDirectory(),
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
		File file = FileUtil.getFile(mPathBar.getCurrentDirectory(),
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

    @Override
    public void refresh() {
        super.refresh();
        mAdapter.clearFileChildrenCache();
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
            MaterialDialog.ListCallbackSingleChoice {

        public SortDialog() {
            String[] items = new String[]{getString(R.string.file_sort_by_default)
                    , getString(R.string.file_sort_by_name),
                    getString(R.string.file_sort_by_time)};

            new MaterialDialog.Builder(getActivity())
                    .title(getString(R.string.file_sort_dialog_title))
                    .items(items)
                    .itemsCallbackSingleChoice(mCurrentSort, this)
                    .show();
        }

        @Override
        public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
            Message msg = null;
            if (which == 0) {
                msg = mHandler.obtainMessage(SORT_BY_DEFAULT);
            } else if (which == 1) {
                msg = mHandler.obtainMessage(SORT_BY_NAME);
            } else if (which == 2) {
                msg = mHandler.obtainMessage(SORT_BY_TIME);
            }
            mHandler.sendMessage(msg);
            return true;
        }
    }

    private class OperationDialog implements
            MaterialDialog.ListCallback {
        private FileHolder item;

        public OperationDialog(FileHolder holder) {
            if (holder == null) {
                return;
            }
            ArrayList<String> items = new ArrayList<>();

            item = holder;
            File file = item.getFile();
            items.add(getString(R.string.menu_open));
            items.add(getString(R.string.menu_delete));
            items.add(getString(R.string.menu_move));
            items.add(getString(R.string.menu_copy));
            items.add(getString(R.string.menu_create_shortcut));
            items.add(getString(R.string.menu_rename));
            // If selected item is a directory
            if (file.isDirectory()) {
                items.add(getString(R.string.menu_send));
            }

            // If selected item is a zip archive
            if (FileUtil.checkIfZipArchive(file)) {
                items.add(getString(R.string.menu_extract));
            } else {
                items.add(getString(R.string.menu_compress));
            }

            String[] array = (String[])items.toArray(new String[items.size()]);
            new MaterialDialog.Builder(getActivity())
                    .title(holder.getName())
                    .icon(holder.getIcon())
                    .items(array)
                    .itemsCallback(this)
                    .build()
                    .show();
        }

        @Override
        public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
            switch (which) {
                case 0:
                    openInformingPathBar(item);
                    break;
                case 1:
                    SingleDeleteDialog tmpDialog = new SingleDeleteDialog();
                    tmpDialog.setTargetFragment(SimpleFileListFragment.this, 0);
                    Bundle args = new Bundle();
                    args.putParcelable(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER,
                            item);
                    tmpDialog.setArguments(args);
                    tmpDialog.show(getFragmentManager(),
                            SingleDeleteDialog.class.getName());
                    break;
                case 2:
                    CopyHelper.get(getActivity()).cut(item);
                    updateClipboardInfo();
                    break;
                case 3:
                    CopyHelper.get(getActivity()).copy(item);
                    updateClipboardInfo();
                    break;
                case 4:
                    MenuUtils.createShortcut(item, getContext());
                    break;
                case 5:
                    RenameDialog tmpDialog1 = new RenameDialog();
                    tmpDialog1.setTargetFragment(SimpleFileListFragment.this, 0);
                    Bundle args1 = new Bundle();
                    args1.putParcelable(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER,
                            item);
                    tmpDialog1.setArguments(args1);
                    tmpDialog1.show(getFragmentManager(),
                            RenameDialog.class.getName());
                    break;
                default:
                    //else ..
                    if (item.getFile().isDirectory()) {
                        if (which == 6) {
                            MenuUtils.sendFile(item, getActivity());
                        } else if (which == 7) {
                            if (FileUtil.checkIfZipArchive(item.getFile())) {
                                MenuUtils.extractFile(SimpleFileListFragment.this, item);
                            } else {
                                MenuUtils.compressFile(SimpleFileListFragment.this, item);
                            }
                        }
                    } else {
                        if (FileUtil.checkIfZipArchive(item.getFile())) {
                            MenuUtils.extractFile(SimpleFileListFragment.this, item);
                        } else {
                            MenuUtils.compressFile(SimpleFileListFragment.this, item);
                        }
                    }
                    break;
            }
        }
    }

    /**
     * After triggering the Storage Access Framework, ensure that folder is really writable. Set preferences
     * accordingly.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who
     *                    this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param data        An Intent, which can return result data to the caller (various data can be attached to Intent
     *                    "extras").
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public final void onActivityResultLollipop(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == FileUtils.REQUEST_STORAGE_CODE) {
            Uri uri;
            // Get Uri from Storage Access Framework.
            uri = data.getData();
            // Persist URI - this is required for verification of writability.
            PreferenceDefault.setTreeUris(getContext(), uri.toString());
            // Persist access permissions.
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } else {
            return;
        }
    }
}