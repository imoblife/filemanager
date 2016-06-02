package com.filemanager.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import com.afollestad.materialdialogs.MaterialDialog;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.FileListFragment;
import com.filemanager.util.CancelEvent;
import com.filemanager.util.CopyHelper;
import com.filemanager.view.CutAndCopyLayout;
import com.intents.FileManagerIntents;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.util.ArrayList;

public class CopyAndCutDialog extends DialogFragment {
    private ArrayList<FileHolder> mFileHolders;
    private boolean mIsCopy;
    private MaterialDialog mProgressDialog;
    private CutAndCopyLayout mContentView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFileHolders = getArguments().getParcelableArrayList(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate the view to display
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        mContentView = (CutAndCopyLayout) inflater.inflate(R.layout.dialog_cut_copy_layout, null);


        mIsCopy = CopyHelper.get(getContext()).getOperationType().equals(CopyHelper.Operation.COPY);
        // Finally create the dialog
        return new MaterialDialog.Builder(getActivity())
                .title(mIsCopy ? R.string.file_dialog_operation_copy_title : R.string.file_dialog_operation_move_title)
                .customView(mContentView, false)
                .positiveText(mIsCopy ? R.string.file_dialog_operation_copy_button : R.string.file_dialog_operation_move_button)
                .positiveColorRes(R.color.blue_1ca0ec)
                .negativeText(R.string.dialog_cancle)
                .negativeColorRes(R.color.grey_999999)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                    }

                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        showProgressDialog(mFileHolders.size());
                        CopyHelper.get(getContext()).paste(new File(mContentView.getPath()), new CopyHelper.OnOperationFinishedListener() {
                            public void operationFinished(boolean success) {
                                try {
                                    if (mProgressDialog != null) {
                                        mProgressDialog.dismiss();
                                    }
                                } catch (Exception e) {
                                }
                                ((FileListFragment) getTargetFragment()).refresh();
                            }
                        }, new CopyHelper.OnOperationProgressListener() {
                            @Override
                            public void onProgress(int progress) {
                                try {
                                    if (mProgressDialog != null) {
                                        mProgressDialog.setProgress(progress);
                                    }
                                } catch (Exception e) {
                                }
                            }
                        });
                    }
                })
                .build();
    }

    private void showProgressDialog(int max){
        try {
            mProgressDialog = new MaterialDialog.Builder(getActivity())
                    .title(getString(R.string.file_dialog_operation_in_progress))
                    .progress(false, max, true)
                    .cancelable(false)
                    .positiveText(R.string.disableall_cancel)
                    .positiveColorRes(R.color.blue_1ca0ec)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            EventBus.getDefault().post(new CancelEvent());
                        }
                    })
                    .build();
            mProgressDialog.show();
        } catch (Exception e) {
        }
    }

}