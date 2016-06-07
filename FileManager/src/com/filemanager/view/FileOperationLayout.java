package com.filemanager.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import base.util.FileUtil;
import com.filemanager.FileHolderListAdapter;
import com.filemanager.R;
import com.filemanager.dialogs.CutAndCopyDialog;
import com.filemanager.dialogs.MultiCompressDialog;
import com.filemanager.dialogs.MultiDeleteDialog;
import com.filemanager.dialogs.RenameDialog;
import com.filemanager.files.FileHolder;
import com.filemanager.iconicdroid.FmFont;
import com.filemanager.lists.FileListFragment;
import com.filemanager.lists.SimpleFileListFragment;
import com.filemanager.util.CopyHelper;
import com.filemanager.util.MenuUtils;
import com.iconics.IconFontDrawable;
import com.iconics.typeface.IIcon;
import com.iconics.view.IconicsTextView;
import com.intents.FileManagerIntents;
import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/6/1.
 */
public class FileOperationLayout extends LinearLayout {

    private static final int DRAWABLE_KEY_SEND = 0;
    private static final int DRAWABLE_KEY_RENAME = 1;
    private static final int DRAWABLE_KEY_SHORT = 2;

    private IconicsTextView mOperationView1;
    private IconicsTextView mOperationView2;
    private IconicsTextView mOperationView3;
    private IconicsTextView mOperationView4;
    private IconicsTextView mOperationView5;

    private Context mContext;
    private Fragment mFragment;
    private FileHolderListAdapter mAdapter;

    public FileOperationLayout(Context context) {
        super(context);
        mContext = context;
    }

    public FileOperationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setDataAdapter(Fragment target, FileHolderListAdapter adapter) {
        mFragment = target;
        mAdapter = adapter;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    public void updateOperationButtonState() {
        if (mAdapter.getSelectedItemList().size() == 1) {
            final FileHolder item = mAdapter.getSelectedItemList().get(0);

            // If selected item is a zip archive
            if (FileUtil.checkIfZipArchive(item.getFile())) {
                mOperationView4.setText("{FMT_ICON_UNZIP}");
                mOperationView4.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MenuUtils.extractFile((SimpleFileListFragment) mFragment, item);
                    }
                });
            } else {
                mOperationView4.setText("{FMT_ICON_ZIP}");
                mOperationView4.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        compressFileList();
                    }
                });
            }

            mOperationView5.setText("{FMT_ICON_MORE}");
            mOperationView5.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    new OperationAction(view);
                }
            });
        } else {
            mOperationView4.setText("{FMT_ICON_SEND}");
            mOperationView4.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    MenuUtils.sendFileList(mAdapter.getSelectedItemList(), mContext);
                    ((FileListFragment) mFragment).refresh();
                }
            });
            mOperationView5.setText("{FMT_ICON_ZIP}");
            mOperationView5.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    compressFileList();
                }
            });
        }

    }

    private void initViews() {
        mOperationView1 = (IconicsTextView) findViewById(R.id.tv_opt_1);
        mOperationView2 = (IconicsTextView) findViewById(R.id.tv_opt_2);
        mOperationView3 = (IconicsTextView) findViewById(R.id.tv_opt_3);
        mOperationView4 = (IconicsTextView) findViewById(R.id.tv_opt_4);
        mOperationView5 = (IconicsTextView) findViewById(R.id.tv_opt_5);

        mOperationView2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                CopyHelper.get(mContext).cut(mAdapter.getSelectedItemList());
                handleCutAndCopyAction();
            }
        });

        mOperationView3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                CopyHelper.get(mContext).copy(mAdapter.getSelectedItemList());
                handleCutAndCopyAction();
            }
        });
        mOperationView1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialog = new MultiDeleteDialog();
                dialog.setTargetFragment(mFragment, 0);
                Bundle args = new Bundle();
                args.putParcelableArrayList(
                        FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER,
                        new ArrayList<Parcelable>(mAdapter.getSelectedItemList()));
                dialog.setArguments(args);
                dialog.show(mFragment.getFragmentManager(),
                        MultiDeleteDialog.class.getName());

            }
        });

    }

    private void handleCutAndCopyAction() {
        DialogFragment dialog = new CutAndCopyDialog();
        dialog.setTargetFragment(mFragment, 0);
        Bundle args = new Bundle();
        args.putParcelableArrayList(
                FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER,
                new ArrayList<Parcelable>(mAdapter.getSelectedItemList()));
        dialog.setArguments(args);
        dialog.show(mFragment.getFragmentManager(),
                CutAndCopyDialog.class.getName());
    }

    private void compressFileList() {
        DialogFragment dialog = new MultiCompressDialog();
        dialog.setTargetFragment(mFragment, 0);
        Bundle args = new Bundle();
        args.putParcelableArrayList(
                FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER,
                new ArrayList<Parcelable>(mAdapter.getSelectedItemList()));
        dialog.setArguments(args);
        dialog.show(mFragment.getFragmentManager(),
                MultiCompressDialog.class.getName());
    }

    private class OperationAction implements QuickAction.OnActionItemClickListener {
        private QuickAction quickAction;

        public OperationAction(View view) {

            String string1 = mContext.getString(R.string.menu_create_shortcut);
            Drawable icon1 = getOperationDrawable(mContext, DRAWABLE_KEY_SHORT, R.color.grey_999999);
            ActionItem item1 = new ActionItem(0, string1, icon1);
            String string2 = mContext.getString(R.string.menu_rename);
            Drawable icon2 = getOperationDrawable(mContext, DRAWABLE_KEY_RENAME, R.color.grey_999999);
            ActionItem item2 = new ActionItem(1, string2, icon2);
            String string3 = mContext.getString(R.string.menu_send);
            Drawable icon3 = getOperationDrawable(mContext, DRAWABLE_KEY_SEND, R.color.grey_999999);
            ActionItem item3 = new ActionItem(2, string3, icon3);
            quickAction = new QuickAction(mContext, QuickAction.VERTICAL);
            quickAction.setOnActionItemClickListener(this);
            quickAction.addActionItem(item1, true);
            quickAction.addActionItem(item2, true);
            quickAction.addActionItem(item3, false);
            quickAction.show(view);
        }

        public void onItemClick(QuickAction source, int pos, int actionId) {
            FileHolder item = mAdapter.getSelectedItemList().get(0);
            if (item == null) {
                return;
            }
            if (pos == 0) {
                MenuUtils.createShortcut(item, getContext());
                ((FileListFragment) mFragment).refresh();
            } else if (pos == 1) {
                RenameDialog tmpDialog1 = new RenameDialog();
                tmpDialog1.setTargetFragment(mFragment, 0);
                Bundle args1 = new Bundle();
                args1.putParcelable(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER,
                        item);
                tmpDialog1.setArguments(args1);
                tmpDialog1.show(mFragment.getFragmentManager(),
                        RenameDialog.class.getName());
            } else if (pos == 2) {
                MenuUtils.sendFile(item, mContext);
                ((FileListFragment) mFragment).refresh();
            }
        }
    }

    private Drawable getOperationDrawable(Context context, int drawableType, int colorRes) {
        IconFontDrawable drawable = null;
        IIcon iIcon = null;
        switch (drawableType) {
            case DRAWABLE_KEY_SEND:
                iIcon = FmFont.Icon.FMT_ICON_SEND;
                break;
            case DRAWABLE_KEY_RENAME:
                iIcon = FmFont.Icon.FMT_ICON_RENAME;
                break;
            case DRAWABLE_KEY_SHORT:
                iIcon = FmFont.Icon.FMT_ICON_SHORT;
                break;
        }

        if (iIcon != null) {
            drawable = new IconFontDrawable(context)
                    .icon(iIcon)
                    .colorRes(colorRes)
                    .sizeDp(20);
        }

        return drawable;
    }
}
