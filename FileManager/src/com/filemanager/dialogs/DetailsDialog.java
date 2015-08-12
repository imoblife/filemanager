package com.filemanager.dialogs;

import com.afollestad.materialdialogs.MaterialDialog;
import imoblife.android.os.ModernAsyncTask;

import java.io.File;


import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.util.FileUtils;
import com.filemanager.util.UIUtils;
import com.intents.FileManagerIntents;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class DetailsDialog extends DialogFragment {
	private FileHolder mFileHolder;
	private TextView mSizeView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mFileHolder = getArguments().getParcelable(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		File f = mFileHolder.getFile();
		// Inflate the view to display
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		final View v = inflater.inflate(R.layout.dialog_details, null);
		
		// Fill the views
		((TextView) v.findViewById(R.id.details_type_value)).setText((f.isDirectory() ? R.string.details_type_folder :
																		(f.isFile() ? R.string.details_type_file : R.string.details_type_other) ));
		
		mSizeView = (TextView) v.findViewById(R.id.details_size_value);
		new SizeRefreshTask().execute();
		
		String perms = (f.canRead() ? "R" : "-") + (f.canWrite() ? "W" : "-") + (FileUtils.canExecute(f) ? "X" : "-");
		((TextView) v.findViewById(R.id.details_permissions_value)).setText(perms);
		
		((TextView) v.findViewById(R.id.details_hidden_value)).setText(f.isHidden() ? R.string.details_yes : R.string.details_no);
		
		((TextView) v.findViewById(R.id.details_lastmodified_value)).setText(mFileHolder.getFormattedModificationDate(getActivity()));
		
		// Finally create the dialog
        return new MaterialDialog.Builder(getActivity())
                .title(mFileHolder.getName())
                .icon(mFileHolder.getIcon())
                .customView(v, true)
                .positiveText(android.R.string.ok)
                .build();
    }
	
	/**
	 * This task doesn't update the text viewed to the user until it's finished, 
	 * so that the user knows the size he sees is indeed the final one.
	 * 
	 * @author George Venios
	 *
	 */
	private class SizeRefreshTask extends ModernAsyncTask<Void, Void, String> {

		@Override
		protected void onPreExecute() {
			mSizeView.setText(R.string.loading);
		}
		
		@Override
		protected String doInBackground(Void... params) {
			return mFileHolder.getFormattedSize(getActivity(), true);
		}
		
		@Override
		protected void onPostExecute(String result) {
			mSizeView.setText(result);
		}
	}
}