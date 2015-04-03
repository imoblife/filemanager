package com.filemanager.dialogs;

import java.io.File;
import java.util.List;


import com.filemanager.R;
import com.filemanager.dialogs.OverwriteFileDialog.Overwritable;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.FileListFragment;
import com.filemanager.util.CompressManager;
import com.filemanager.util.MediaScannerUtils;
import com.intents.FileManagerIntents;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MultiCompressDialog extends DialogFragment implements Overwritable {
	private List<FileHolder> mFileHolders;
	private CompressManager mCompressManager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mFileHolders = getArguments().getParcelableArrayList(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER);
		
		mCompressManager = new CompressManager(getActivity());
		mCompressManager.setOnCompressFinishedListener(new CompressManager.OnCompressFinishedListener() {
			
			@Override
			public void compressFinished() {
				try {
					((FileListFragment) getTargetFragment()).refresh();
					
					MediaScannerUtils.informFileAdded(getTargetFragment().getActivity().getApplicationContext(), tbcreated);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		LinearLayout view = (LinearLayout) inflater.inflate(R.layout.dialog_text_input, null);
		final EditText v = (EditText) view.findViewById(R.id.foldername);
		v.setHint(R.string.compressed_file_name);
		
		v.setOnEditorActionListener(new TextView.OnEditorActionListener(){
			public boolean onEditorAction(TextView text, int actionId, KeyEvent event) {
				   if (actionId == EditorInfo.IME_ACTION_GO)
					   compress(v.getText().toString());
				   dismiss();
				   return true;
				}
		});
		
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.menu_compress)
				.setView(view)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						compress(v.getText().toString());
					}
				}).setNegativeButton(android.R.string.cancel, null).create();
	}
	
	private void compress(final String zipname){
		this.zipname = zipname;
		tbcreated = new File(mFileHolders.get(0).getFile().getParent() + File.separator + zipname + ".zip");
		if (tbcreated.exists()) {
			this.zipname = zipname;
			OverwriteFileDialog dialog = new OverwriteFileDialog();
			dialog.setTargetFragment(this, 0);
			dialog.show(getFragmentManager(), "OverwriteFileDialog");
		} else {
			mCompressManager.compress(mFileHolders, tbcreated.getName());
		}
	}

	private File tbcreated;
	private String zipname;
	
	@Override
	public void overwrite() {
		tbcreated.delete();
		compress(zipname);
	}
}