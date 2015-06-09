package com.filemanager.lists;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import base.util.ui.titlebar.ITitlebarActionMenuListener;
import com.filemanager.PreferenceActivity;
import com.filemanager.R;
import com.filemanager.dialogs.CreateDirectoryDialog;
import com.filemanager.files.FileHolder;
import com.filemanager.util.CopyHelper;
import com.filemanager.util.FileUtils;
import com.filemanager.util.MenuUtils;
import com.filemanager.view.PathBar;
import com.filemanager.view.PathBar.Mode;
import com.filemanager.view.PathBar.OnDirectoryChangedListener;
import com.intents.FileManagerIntents;

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

    private static final int SORT_BY_DEFAULT = 0;
    private static final int SORT_BY_NAME = 1;
    private static final int SORT_BY_TIME = 2;

    protected static final int REQUEST_CODE_MULTISELECT = 2;

    private int mCurrentSort = SORT_BY_DEFAULT;

    private PathBar mPathBar;
	private boolean mActionsEnabled = true;

	private int mSingleSelectionMenu = R.menu.context;
	private int mMultiSelectionMenu = R.menu.multiselect;

    private Handler mHandler;

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
			public void directoryChanged(File newCurrentDir) {
				open(new FileHolder(newCurrentDir, getActivity()));
			}
		});
		if (savedInstanceState != null
				&& savedInstanceState.getBoolean(INSTANCE_STATE_PATHBAR_MODE))
			mPathBar.switchToManualInput();
		// Removed else clause as the other mode is the default. It seems faster
		// this way on Nexus S.

		initContextualActions();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SORT_BY_NAME:
                        if (mFiles != null && !mFiles.isEmpty()) {
                            mCurrentSort = SORT_BY_NAME;
                            Collections.sort(mFiles, new ComparatorByAlphabet());
                            mAdapter.notifyDataSetChanged();
                        }
                        break;
                    case SORT_BY_TIME:
                        if (mFiles != null && !mFiles.isEmpty()) {
                            mCurrentSort = SORT_BY_TIME;
                            Collections.sort(mFiles, new ComparatorByLastModified());
                            mAdapter.notifyDataSetChanged();
                        }
                        break;
                    default:
                        if (mFiles != null && !mFiles.isEmpty() && mCurrentSort != SORT_BY_DEFAULT) {
                            refresh();
                        }
                        break;
                }
            }
        };
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
        } else {
            return false;
        }
    }

    static class ComparatorByLastModified implements Comparator<FileHolder> {
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

    static class ComparatorByAlphabet implements Comparator<FileHolder> {
        public int compare(FileHolder f1, FileHolder f2) {
            return String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        mCurrentSort = SORT_BY_DEFAULT;
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
        } else if (position == 3) {
            handleOptionMenu(MENU_ID_SORT);
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