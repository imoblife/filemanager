package com.filemanager.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;
import base.util.FileUtil;
import base.util.PermissionUtil;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.FileListFragment;
import com.filemanager.lists.MultiselectListFragment;
import com.filemanager.util.FileUtils;
import com.filemanager.util.MediaScannerUtils;
import com.intents.FileManagerIntents;
import imoblife.android.os.ModernAsyncTask;

import java.io.File;
import java.util.List;

public class MultiDeleteDialog extends DialogFragment {
	private List<FileHolder> mFileHolders;
    private Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
		mFileHolders = getArguments().getParcelableArrayList(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.really_delete_multiselect, mFileHolders.size()))
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .iconRes(android.R.drawable.ic_dialog_alert)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        new RecursiveDeleteTask().execute();
                    }
                }).build();
    }
	
	private class RecursiveDeleteTask extends ModernAsyncTask<Void, Void, Void> {
		private Context mContext;
		
		public RecursiveDeleteTask() {
			// Init before having the fragment in an undefined state because of dialog dismissal
			mContext = getTargetFragment().getActivity().getApplicationContext();
		}
		
		/**
		 * If 0 some failed, if 1 all succeeded. 
		 */
		private int mResult = 1;
		private MaterialDialog dialog = new MaterialDialog.Builder(getActivity()).progressIndeterminateStyle(false).progress(true, 0).build();

		/**
		 * Recursively delete a file or directory and all of its children.
		 * 
		 * @returns 0 if successful, error value otherwise.
		 */
		private void recursiveDelete(File file) {
            if (PermissionUtil.isAndroid5() && (FileUtil.getDocumentFile(file, false, false, mContext) != null
                    || FileUtil.getDocumentFile(file, true, false, mContext) != null)) {
                mResult = (FileUtil.deleteFile(file, mContext)) ? 1 : 0;
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
			dialog.setContent(getActivity().getString(R.string.deleting));
			dialog.show();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			for(FileHolder fh : mFileHolders)
				recursiveDelete(fh.getFile());
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				Toast.makeText(dialog.getContext(), mResult == 0 ? R.string.delete_failure : R.string.delete_success, Toast.LENGTH_LONG).show();
                if (getTargetFragment() instanceof MultiselectListFragment && mResult == 1) {
                    ((FileListFragment) getTargetFragment()).getListView().clearChoices();
                }
                ((FileListFragment) getTargetFragment()).refresh();
				dialog.dismiss();
				
				mContext = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}