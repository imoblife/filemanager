package com.filemanager.search;

import java.io.File;
import java.util.HashMap;

import com.filemanager.R;
import com.filemanager.ThumbnailLoader;
import com.filemanager.files.FileHolder;
import com.filemanager.view.ViewHolder;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Simple adapter for displaying search results.
 * 
 * @author George Venios
 * 
 */
public class SearchListAdapter extends CursorAdapter {
	private HashMap<String, FileHolder> itemCache = new HashMap<String, FileHolder>();

	public SearchListAdapter(Context context, Cursor c) {

		super(context, c, true);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String path = cursor.getString(cursor
				.getColumnIndex(SearchResultsProvider.COLUMN_PATH));

		FileHolder fHolder;

		if ((fHolder = itemCache.get(path)) == null) {
			fHolder = new FileHolder(new File(path), context);
			itemCache.put(path, fHolder);
		}

		ViewHolder h = (ViewHolder) view.getTag();
		h.primaryInfo.setText(fHolder.getName());
		h.secondaryInfo.setText(path);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// Inflate the view
		ViewGroup v = (ViewGroup) ((LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.serch_item_filelist, null);

		// Set the viewholder optimization.
		ViewHolder holder = new ViewHolder();
		holder.primaryInfo = (TextView) v.findViewById(R.id.primary_info);
		holder.secondaryInfo = (TextView) v.findViewById(R.id.secondary_info);
		v.findViewById(R.id.tertiary_info).setVisibility(View.GONE);

		v.setTag(holder);

		return v;
	}
}