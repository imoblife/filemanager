package com.filemanager.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import base.util.FileUtils;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;
import com.filemanager.dialogs.OverwriteFileDialog.Overwritable;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.FileListFragment;
import com.filemanager.util.CompressManager;
import com.filemanager.util.MediaScannerUtils;
import com.intents.FileManagerIntents;

import java.io.File;
import java.util.List;

public class MultiCompressDialog extends DialogFragment implements Overwritable {
	private List<FileHolder> mFileHolders;
	private CompressManager mCompressManager;
    private Context mContext;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();
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
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.menu_rename)
                .inputType(InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_CLASS_TEXT)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .input(R.string.compressed_file_name, 0, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        compress(input.toString());
                    }
                }).build();
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
        deleteFolder();
		compress(zipname);
	}

    private boolean deleteFolder(){
        return FileUtils.deleteFile(tbcreated, mContext);
    }
}