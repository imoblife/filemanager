package com.filemanager.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.FileListFragment;
import com.filemanager.util.MediaScannerUtils;
import com.intents.FileManagerIntents;

import java.io.File;

public class RenameDialog extends DialogFragment {
	private FileHolder mFileHolder;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mFileHolder = getArguments().getParcelable(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.menu_rename)
                .inputType(InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_CLASS_TEXT)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                        .input(mFileHolder.getName(), mFileHolder.getName(), false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                renameTo(input.toString());
                            }
                        }).build();
    }
	
	private void renameTo(String to){
		boolean res = false;
		
		if(to.length() > 0){
			File from = mFileHolder.getFile();
			
			File dest = new File(mFileHolder.getFile().getParent() + File.separator + to);
			if(!dest.exists()){
				res = mFileHolder.getFile().renameTo(dest);
				((FileListFragment) getTargetFragment()).refresh();

				// Inform media scanner
				MediaScannerUtils.informFileDeleted(getActivity().getApplicationContext(), from);
				MediaScannerUtils.informFileAdded(getActivity().getApplicationContext(), dest);
			}
		}
		
		Toast.makeText(getActivity(), res ? R.string.rename_success : R.string.rename_failure, Toast.LENGTH_SHORT).show();
	}
}