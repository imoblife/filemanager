package com.filemanager.dialogs;

import java.io.File;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import base.util.FileUtil;
import base.util.FileUtils;
import base.util.PreferenceDefault;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;
import com.filemanager.dialogs.OverwriteFileDialog.Overwritable;
import com.filemanager.lists.FileListFragment;
import com.intents.FileManagerIntents;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

public class CreateDirectoryDialog extends DialogFragment implements Overwritable {
	private File mIn;
    private Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
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
                                try {
                                    if (FileUtils.checkSdCardAndroid5(mContext.getApplicationContext()) && (text != null && text.length() != 0)
                                            && FileUtils.isOnExtSdCard(new File(mIn + File.separator + text.toString()), mContext)) {
                                        triggerStorageAccessFramework(20);
                                    } else {
                                        createFolder(input, getActivity());
                                    }
                                } catch (Exception e) {
                                    dismiss();
                                }
                            }
                        }

                )
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .build();
                }

    private void createFolder(final CharSequence text, Context c) {
        if (c == null) {
            return;
        }
        //below Android 5.0
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
                if(FileUtils.isAndroid5()){
                    if (tbcreated.mkdirs()) {
                        Toast.makeText(c, R.string.create_dir_success, Toast.LENGTH_SHORT).show();
                    } else if (FileUtils.isOnExtSdCard(tbcreated, mContext)) {
                        DocumentFile documentFile = FileUtils.getDocumentFile(tbcreated, true, true, mContext);
                        if (documentFile != null) {
                            Toast.makeText(c, R.string.create_dir_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(c, R.string.create_dir_failure, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(c, R.string.create_dir_failure, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (tbcreated.mkdirs())
                        Toast.makeText(c, R.string.create_dir_success, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(c, R.string.create_dir_failure, Toast.LENGTH_SHORT).show();
                }

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
        deleteFolder();
        createFolder(text, c);
    }

    private boolean deleteFolder(){
        return FileUtils.deleteFile(tbcreated, c);
    }

    /**
     * Trigger the storage access framework to access the base folder of the ext sd card.
     *
     * @param code
     *            The request code to be used.
     *
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void triggerStorageAccessFramework(final int code) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, code);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onActivityResultLollipop(requestCode, resultCode, data);
        }
    }

    /**
     * After triggering the Storage Access Framework, ensure that folder is really writable. Set preferences
     * accordingly.
     *
     * @param requestCode
     *            The integer request code originally supplied to startActivityForResult(), allowing you to identify who
     *            this result came from.
     * @param resultCode
     *            The integer result code returned by the child activity through its setResult().
     * @param data
     *            An Intent, which can return result data to the caller (various data can be attached to Intent
     *            "extras").
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public final void onActivityResultLollipop(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri;
            // Get Uri from Storage Access Framework.
            uri = data.getData();
            // Persist URI - this is required for verification of writability.
            PreferenceDefault.setTreeUris(mContext, uri.toString());
            // Persist access permissions.
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } else {
            return;
        }
    }
}