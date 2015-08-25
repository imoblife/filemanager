package com.filemanager.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;
import base.util.FileUtils;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.FileListFragment;
import com.filemanager.util.MediaScannerUtils;
import com.intents.FileManagerIntents;
import imoblife.android.os.ModernAsyncTask;

import java.io.File;

public class SingleDeleteDialog extends DialogFragment {
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
        MaterialDialog.Builder adb = new MaterialDialog.Builder(getActivity());
        try {
            adb.title(getString(R.string.really_delete, mFileHolder.getName()))
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            new RecursiveDeleteTask().execute(mFileHolder.getFile());
                        }
                    })
                    .icon(mFileHolder.getIcon());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return adb.build();
	}
	
	private class RecursiveDeleteTask extends ModernAsyncTask<File, Void, Void> {
		/**
		 * If 0 some failed, if 1 all succeeded. 
		 */
		private int mResult = 1;
		private ProgressDialog dialog = new ProgressDialog(getActivity());

		/**
		 * Recursively delete a file or directory and all of its children.
		 * 
		 * @returns 0 if successful, error value otherwise.
		 */
		private void recursiveDelete(File file) {
            if (FileUtils.isAndroid5() && (FileUtils.getDocumentFile(file, false, false, mContext) != null
                    || FileUtils.getDocumentFile(file, true, false, mContext) != null)) {
                mResult = (FileUtils.deleteFile(file, mContext)) ? 1 : 0;
            } else {
                File[] files = file.listFiles();
                if (files != null && files.length != 0)
                    // If it's a directory delete all children.
                    for (File childFile : files) {
                        if (childFile.isDirectory()) {
                            recursiveDelete(childFile);
                        } else {
                            mResult *= childFile.delete() ? 1 : 0;
                        }
                    }

                // And then delete parent. -- or just delete the file.
                mResult *= file.delete() ? 1 : 0;
            }
        }

        @Override
		protected void onPreExecute() {		
			dialog.setMessage(getActivity().getString(R.string.deleting));
			dialog.setIndeterminate(true);
			dialog.show();
		}
		
		@Override
		protected Void doInBackground(File... params) {
			recursiveDelete(params[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				Toast.makeText(dialog.getContext(), mResult == 0 ? R.string.delete_failure : R.string.delete_success, Toast.LENGTH_LONG).show();
				((FileListFragment) getTargetFragment()).refresh();
				dialog.dismiss();
				
				MediaScannerUtils.informFileDeleted(getTargetFragment().getActivity().getApplicationContext(), mFileHolder.getFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public interface OnDeleteListener{
		public void deleted();
	}
}