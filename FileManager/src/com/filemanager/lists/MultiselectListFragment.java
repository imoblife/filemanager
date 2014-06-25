package com.filemanager.lists;

import java.util.ArrayList;

import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.util.MenuUtils;
import com.filemanager.view.LegacyActionContainer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.filelist_legacy_multiselect, null);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
		setHasOptionsMenu(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		super.onViewCreated(view, savedInstanceState);

		mAdapter.setItemLayout(R.layout.item_filelist_multiselect);

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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.options_multiselect, menu);
	}

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