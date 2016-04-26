package com.filemanager.lists;

import java.util.ArrayList;
import java.util.Collections;

import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.util.MenuUtils;
import com.filemanager.util.Preference;
import com.filemanager.view.LegacyActionContainer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Dedicated file list fragment, used for multiple selection on platforms older
 * than Honeycomb. OnDestroy sets RESULT_OK on the parent activity so that
 * callers refresh their lists if appropriate.
 * 
 * @author George Venios
 */
public class MultiselectListFragment extends FileListFragment {
	private LegacyActionContainer mLegacyActionContainer;
    private Preference mPreference;

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.filelist_legacy_multiselect, null);
		TextView title_tv = (TextView) view.findViewById(R.id.title_tv);
		title_tv.setText(getString(R.string.menu_multiselect));
		return view;

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
		//		setHasOptionsMenu(true);

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		super.onViewCreated(view, savedInstanceState);
		LinearLayout lin = (LinearLayout) getActivity().findViewById(
				R.id.titlebar_ll);
		lin.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getActivity().finish();
			}
		});

        mPreference = new Preference(getContext());

        mCurrentSort = mPreference.getInt(Preference.PREFS_KEY_SORT_TYPE, Preference.SORT_TYPE_DEFAULT);

        if (mCurrentSort == Preference.SORT_TYPE_NAME) {
            Collections.sort(mFiles, new SimpleFileListFragment.ComparatorByAlphabet());
        } else if (mCurrentSort == Preference.SORT_TYPE_MODIFY_TIME) {
            Collections.sort(mFiles, new SimpleFileListFragment.ComparatorByLastModified());
        }
        mAdapter.setItemLayout(R.layout.item_filelist_multiselect);
        mAdapter.setData(mFiles);


        // Init members
		mLegacyActionContainer = (LegacyActionContainer) view
				.findViewById(R.id.action_container);
		mLegacyActionContainer.setMenuResource(R.menu.multiselect);
		mLegacyActionContainer
				.setOnActionSelectedListener(new LegacyActionContainer.OnActionSelectedListener() {
					@Override
					public void actionSelected(MenuItem item) {
						if (getListView().getCheckItemIds().length == 0) {
							Toast.makeText(getActivity(),
									R.string.no_selection, Toast.LENGTH_SHORT)
									.show();
							return;
						}

						ArrayList<FileHolder> fItems = new ArrayList<FileHolder>();

						for (long i : getListView().getCheckItemIds()) {
							fItems.add((FileHolder) mAdapter.getItem((int) i));
						}

						MenuUtils.handleMultipleSelectionAction(
								MultiselectListFragment.this, item, fItems,
								getActivity());
					}
				});
	}

	@Override
	protected void onDirectoryContentShowed() {
	}

//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		inflater.inflate(R.menu.options_multiselect, menu);
//	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		ListView list = getListView();
		int id = item.getItemId();
		if (id == R.id.check_all) {
			for (int i = 0; i < mAdapter.getCount(); i++) {
				list.setItemChecked(i, true);
			}
			return true;
		} else if (id == R.id.uncheck_all) {
			for (int i = 0; i < mAdapter.getCount(); i++) {
				list.setItemChecked(i, false);
			}
			return true;
		} else {
			return false;
		}
	}
}