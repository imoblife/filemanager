package com.filemanager.util;

import android.app.AlertDialog;
import android.content.*;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import base.util.FileUtil;
import com.filemanager.*;
import com.filemanager.dialogs.*;
import com.filemanager.files.FileHolder;
import com.filemanager.lists.FileListFragment;
import com.filemanager.lists.SimpleFileListFragment;
import com.intents.FileManagerIntents;

/**
 * Utility class that helps centralize multiple and single selection actions for
 * all API levels.
 * 
 * @author George Venios
 */
public abstract class MenuUtils {


	/**
	 * Creates a home screen shortcut.
	 * 
	 * @param fileholder
	 *            The {@link File} to create the shortcut to.
	 */
	static public void createShortcut(FileHolder fileholder, Context context) {
		Intent shortcutintent = new Intent(
				"com.android.launcher.action.INSTALL_SHORTCUT");
		shortcutintent.putExtra("duplicate", false);
		shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				fileholder.getName());
		Parcelable icon = Intent.ShortcutIconResource.fromContext(
				context.getApplicationContext(),
				R.drawable.ic_launcher_shortcut);
		shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

		// Intent to load
		Intent itl = new Intent(Intent.ACTION_VIEW);
		if (fileholder.getFile().isDirectory())
			itl.setData(Uri.fromFile(fileholder.getFile()));
		else
			itl.setDataAndType(Uri.fromFile(fileholder.getFile()),
					fileholder.getMimeType());
		itl.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);

		shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, itl);
		context.sendBroadcast(shortcutintent);
	}

	/**
	 * Creates an activity picker to send a file.
	 * 
	 * @param fHolder
	 *            A {@link FileHolder} containing the {@link File} to send.
	 * @param context
	 *            {@link Context} in which to create the picker.
	 */
	public static void sendFile(FileHolder fHolder, Context context) {
		String filename = fHolder.getName();

		Intent i = new Intent();
		i.setAction(Intent.ACTION_SEND);
		i.setType(fHolder.getMimeType());
		i.putExtra(Intent.EXTRA_SUBJECT, filename);
		i.putExtra(Intent.EXTRA_STREAM, FileUtil.getUri(fHolder.getFile()));
		i.putExtra(
				Intent.EXTRA_STREAM,
				Uri.parse("content://" + FileManagerProvider.AUTHORITY
						+ fHolder.getFile().getAbsolutePath()));

		i = Intent.createChooser(i, context.getString(R.string.menu_send));

		try {
			context.startActivity(i);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context, R.string.send_not_available,
					Toast.LENGTH_SHORT).show();
		}
	}

    public static void sendFileList(ArrayList<FileHolder> list, Context context) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        ArrayList<Uri> uris = new ArrayList<Uri>();
        intent.setType("text/plain");

        for (FileHolder fh : list) {
            uris.add(FileUtil.getUri(fh.getFile()));
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        try {
            context.startActivity(Intent.createChooser(intent,
                    context.getString(R.string.send_chooser_title)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.send_not_available,
                    Toast.LENGTH_SHORT).show();
        }
    }


	/**
	 * Call this to show the "More" dialog for the passed {@link FileHolder}.
	 * 
	 * @param context
	 *            Always useful, isn't it?
	 */
	private static void showMoreCommandsDialog(FileHolder holder,
			final Context context) {
		final Uri data = Uri.fromFile(holder.getFile());
		final Intent intent = new Intent();
		intent.setDataAndType(data, holder.getMimeType());

		if (holder.getMimeType() != null) {
			// Add additional options for the MIME type of the selected file.
			PackageManager pm = context.getPackageManager();
			final List<ResolveInfo> lri = pm.queryIntentActivityOptions(
					new ComponentName(context, FileManagerActivity.class),
					null, intent, 0);
			final int N = lri != null ? lri.size() : 0;

			// Create name list for menu item.
			final List<CharSequence> items = new ArrayList<CharSequence>();
			/*
			 * Some of the options don't go to the list hence we have to remove
			 * them to keep the lri correspond with the menu items. In the
			 * addition, we have to remove them after the first iteration,
			 * otherwise the iteration breaks.
			 */
			List<ResolveInfo> toRemove = new ArrayList<ResolveInfo>();
			for (int i = 0; i < N; i++) {
				final ResolveInfo ri = lri.get(i);
				Intent rintent = new Intent(intent);
				rintent.setComponent(new ComponentName(
						ri.activityInfo.applicationInfo.packageName,
						ri.activityInfo.name));
				ActivityInfo info = rintent.resolveActivityInfo(pm, 0);
				String permission = info.permission;
				if (info.exported
						&& (permission == null || context
								.checkCallingPermission(permission) == PackageManager.PERMISSION_GRANTED))
					items.add(ri.loadLabel(pm));
				else
					toRemove.add(ri);
			}

			for (ResolveInfo ri : toRemove) {
				lri.remove(ri);
			}

			new AlertDialog.Builder(context)
					.setTitle(holder.getName())
					.setIcon(holder.getIcon())
					.setItems(items.toArray(new CharSequence[0]),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									final ResolveInfo ri = lri.get(item);
									Intent rintent = new Intent(intent)
											.setComponent(new ComponentName(
													ri.activityInfo.applicationInfo.packageName,
													ri.activityInfo.name));
									context.startActivity(rintent);
								}
							}).create().show();
		}
	}

    public static void compressFile(SimpleFileListFragment fragment, FileHolder fileHolder) {
        SingleCompressDialog dialog = new SingleCompressDialog();
        dialog.setTargetFragment(fragment, 0);
        Bundle args = new Bundle();
        args.putParcelable(FileManagerIntents.EXTRA_DIALOG_FILE_HOLDER,
                fileHolder);
        dialog.setArguments(args);
        dialog.show(fragment.getFragmentManager(), SingleCompressDialog.class.getName());
    }

    public static void extractFile(final SimpleFileListFragment fragment, FileHolder fileHolder) {
        File dest = new File(fileHolder.getFile().getParentFile(),
                FileUtils.getNameWithoutExtension(fileHolder.getFile()));
        dest.mkdirs();

        // Changed from the previous behavior.
        // We just extract on the current directory. If the user needs to
        // put it in another dir,
        // he/she can copy/cut the file with the new, equally easy to use
        // way.
        new ExtractManager(fragment.getActivity()).setOnExtractFinishedListener(
                new ExtractManager.OnExtractFinishedListener() {

                    @Override
                    public void extractFinished() {
                        fragment.refresh();
                    }
                }).extract(fileHolder.getFile(), dest.getAbsolutePath());
    }
}