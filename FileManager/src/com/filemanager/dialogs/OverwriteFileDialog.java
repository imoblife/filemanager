package com.filemanager.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;

public class OverwriteFileDialog extends DialogFragment {
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.file_exists)
                .content(R.string.overwrite_question)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        ((Overwritable) getTargetFragment()).overwrite();
                    }
                })
                .build();
	}
	
	public interface Overwritable {
		public void overwrite();
	}
}