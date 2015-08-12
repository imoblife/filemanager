package com.filemanager.dialogs;

import java.io.File;


import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;
import com.filemanager.dialogs.OverwriteFileDialog.Overwritable;
import com.filemanager.lists.FileListFragment;
import com.filemanager.util.UIUtils;
import com.intents.FileManagerIntents;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CreateDirectoryDialog extends DialogFragment implements Overwritable {
	private File mIn;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mIn = new File(getArguments().getString(FileManagerIntents.EXTRA_DIR_PATH));
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.create_new_folder)
                .iconRes(android.R.drawable.ic_dialog_alert)
                .input(R.string.folder_name, 0, false, new MaterialDialog.InputCallback() {

                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        createFolder(input, getActivity());
                    }
                })
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .build();
    }

	private void createFolder(final CharSequence text, Context c) {
        if (c == null) {
            return;
        }
        if (text != null && text.length() != 0) {
			tbcreated = new File(mIn + File.separator + text.toString());
			if (tbcreated.exists()) {
				this.text = text;
				this.c = c;
				OverwriteFileDialog dialog = new OverwriteFileDialog();
				dialog.setTargetFragment(this, 0);
                if (getFragmentManager() == null) {
                    return;
                }
                dialog.show(getFragmentManager(), "OverwriteFileDialog");
			} else {
				if (tbcreated.mkdirs())
					Toast.makeText(c, R.string.create_dir_success, Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(c, R.string.create_dir_failure, Toast.LENGTH_SHORT).show();

				((FileListFragment) getTargetFragment()).refresh();
				dismiss();
			}
		}
	}
	
	private File tbcreated;
	private CharSequence text;
	private Context c;
	
	@Override
	public void overwrite() {
		tbcreated.delete();
		createFolder(text, c);
	}
}