package com.filemanager.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import base.util.FileUtil;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;
import com.filemanager.dialogs.OverwriteFileDialog.Overwritable;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.FileListFragment;
import com.filemanager.util.CompressManager;
import com.filemanager.util.FileUtils;
import com.filemanager.util.MediaScannerUtils;
import com.intents.FileManagerIntents;

import java.io.File;

public class SingleCompressDialog extends DialogFragment implements Overwritable {
	private FileHolder mFileHolder;
	private CompressManager mCompressManager;
    private Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
		mFileHolder = getArguments().getParcelable(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER);
		
		mCompressManager = new CompressManager(getActivity());
		mCompressManager.setOnCompressFinishedListener(new CompressManager.OnCompressFinishedListener() {
			
			@Override
			public void compressFinished() {
                try {
                    ((FileListFragment) SingleCompressDialog.this.getTargetFragment()).refresh();

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
                .title(R.string.menu_compress)
                .inputType(InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_CLASS_TEXT)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .input(getString(R.string.compressed_file_name), "", false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        compress(input.toString());
                    }
                }).build();
    }

    private void compress(final String zipname){
		tbcreated = new File(mFileHolder.getFile().getParent() + File.separator + zipname + ".zip");
		if (tbcreated.exists()) {
			this.zipname = zipname;
			OverwriteFileDialog dialog = new OverwriteFileDialog();
			dialog.setTargetFragment(this, 0);
			dialog.show(getFragmentManager(), "OverwriteFileDialog");
		} else {
			mCompressManager.compress(mFileHolder, tbcreated.getName());
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
        return FileUtil.deleteFile(tbcreated, mContext);
    }
}