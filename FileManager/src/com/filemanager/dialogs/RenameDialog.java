package com.filemanager.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.provider.DocumentFile;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Toast;
import base.util.FileUtils;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.FileListFragment;
import com.filemanager.util.MediaScannerUtils;
import com.intents.FileManagerIntents;

import java.io.File;

public class RenameDialog extends DialogFragment {
	private FileHolder mFileHolder;
    private Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
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
	
	private void renameTo(String to) {
        boolean res = false;

        if (to.length() > 0) {
            File from = mFileHolder.getFile();
            File dest = new File(mFileHolder.getFile().getParent() + File.separator + to);

            if (FileUtils.isAndroid5() && FileUtils.isOnExtSdCard(from, mContext) && !dest.exists() && !TextUtils.isEmpty(to)) {
                DocumentFile documentFile;
                if (from.isDirectory()) {
                    documentFile = FileUtils.getDocumentFile(from, true, false, mContext);
                } else {
                    documentFile = FileUtils.getDocumentFile(from, false, false, mContext);
                }
                if(documentFile != null) {
                    res = documentFile.renameTo(to);
                    ((FileListFragment) getTargetFragment()).refresh();
                    // Inform media scanner
                    MediaScannerUtils.informFileDeleted(getActivity().getApplicationContext(), from);
                    MediaScannerUtils.informFileAdded(getActivity().getApplicationContext(), dest);
                }

            } else {
                if (!dest.exists()) {
                    res = mFileHolder.getFile().renameTo(dest);
                    ((FileListFragment) getTargetFragment()).refresh();
                    // Inform media scanner
                    MediaScannerUtils.informFileDeleted(getActivity().getApplicationContext(), from);
                    MediaScannerUtils.informFileAdded(getActivity().getApplicationContext(), dest);
                }
            }

        }

        Toast.makeText(getActivity(), res ? R.string.rename_success : R.string.rename_failure, Toast.LENGTH_SHORT).show();
    }
}