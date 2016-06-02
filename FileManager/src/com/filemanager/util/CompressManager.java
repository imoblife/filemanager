package com.filemanager.util;

import android.support.v4.provider.DocumentFile;
import base.util.FileUtil;
import base.util.PermissionUtil;
import com.afollestad.materialdialogs.MaterialDialog;
import de.greenrobot.event.EventBus;
import imoblife.android.os.ModernAsyncTask;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


import com.filemanager.R;
import com.filemanager.files.FileHolder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.util.Log;
import android.widget.Toast;

public class CompressManager {
	/**
	 * TAG for log messages.
	 */
	static final String TAG = "CompressManager";

	private static final int BUFFER_SIZE = 1024;
	private Context mContext;
	private MaterialDialog progressDialog;
	private int fileCount;
	private String fileOut;
	private OnCompressFinishedListener onCompressFinishedListener = null;

	public CompressManager(Context context) {
		mContext = context;
	}

	public void compress(FileHolder f, String out) {
		List<FileHolder> list = new ArrayList<FileHolder>(1);
		list.add(f);
		compress(list, out);
	}

	public void compress(List<FileHolder> list, String out) {
		if (list.isEmpty()) {
			Log.v(TAG, "couldn't compress empty file list");
			return;
		}
		this.fileOut = list.get(0).getFile().getParent() + File.separator + out;
		fileCount = 0;
		for (FileHolder f : list) {
			fileCount += FileUtil.getFileCount(f.getFile());
		}
		new CompressTask().execute(list);
	}

	private class CompressTask extends
	ModernAsyncTask<List<FileHolder>, Void, Integer> {
		private static final int success = 0;
		private static final int error = 1;
		private ZipOutputStream zos;
		private File zipDirectory;
        private DocumentFile zipDirectoryDocumentFile;
		private boolean cancelCompression = false;

		/**
		 * count of compressed file to update the progress bar
		 */
		private int isCompressed = 0;

		/**
		 * Recursively compress file or directory
		 * 
		 * @returns 0 if successful, error value otherwise.
		 */
		private void compressFile(File file, String path) throws IOException {
			if (!file.isDirectory()) {
				byte[] buf = new byte[BUFFER_SIZE];
				int len;
				FileInputStream in = new FileInputStream(file);
				if (path.length() > 0)
					zos.putNextEntry(new ZipEntry(path + "/" + file.getName()));
				else
					zos.putNextEntry(new ZipEntry(file.getName()));
				while ((len = in.read(buf)) > 0) {
					zos.write(buf, 0, len);
				}
				in.close();
				return;
			}
			if (file.list() == null || cancelCompression) {
				return;
			}
			for (String fileName : file.list()) {
				if (cancelCompression) {
					return;
				}
				File f = new File(file.getAbsolutePath() + File.separator
						+ fileName);
				compressFile(f, path + File.separator + file.getName());
				isCompressed++;
				progressDialog.setProgress((isCompressed * 100) / fileCount);
			}
		}

		@Override
		protected void onPreExecute() {
			FileOutputStream out = null;
			zipDirectory = new File(fileOut);
            zipDirectoryDocumentFile = FileUtil.getDocumentFile(zipDirectory, false, true, mContext);

            progressDialog = new MaterialDialog.Builder(mContext)
                    .content(mContext.getString(R.string.compressing))
                    .progress(false, fileCount, true)
                    .cancelable(false)
                    .positiveText(R.string.disableall_cancel)
                    .positiveColorRes(R.color.blue_1ca0ec)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            if (!cancelCompression) {
                                cancelCompression = true;
                                cancel(true);
                            }
                        }
                    })
                    .build();
            progressDialog.show();

			try {
                if (PermissionUtil.isAndroid5() && zipDirectoryDocumentFile != null) {

                    OutputStream tmp = mContext.getContentResolver().openOutputStream(zipDirectoryDocumentFile.getUri());
                    zos = new ZipOutputStream(new BufferedOutputStream(tmp));
                } else {
                    out = new FileOutputStream(zipDirectory);
                    zos = new ZipOutputStream(new BufferedOutputStream(out));
                }
            } catch (FileNotFoundException e) {
				Log.e(TAG, "error while creating ZipOutputStream");
			}
		}

		@Override
		protected Integer doInBackground(List<FileHolder>... params) {
			if (zos == null) {
				return error;
			}
			List<FileHolder> list = params[0];
			for (FileHolder file : list) {
				if (cancelCompression) {
					return error;
				}
				try {
					compressFile(file.getFile(), "");
				} catch (Exception e) {
					Log.e(TAG, "Error while compressing", e);
					return error;
				}
			}
			return success;
		}

		@Override
		protected void onCancelled(Integer result) {
			Log.e(TAG, "onCancelled Initialised");
			try {
				zos.flush();
				zos.close();
			} catch (IOException e) {
				Log.e(TAG, "error while closing zos", e);
			}
			if (zipDirectory.delete()) {
				Log.e(TAG, "test deleted successfully");
			} else {
				Log.e(TAG, "error while deleting test");
			}
			Toast.makeText(mContext, "Compression Canceled", Toast.LENGTH_SHORT)
					.show();

			if (onCompressFinishedListener != null)
				onCompressFinishedListener.compressFinished();
		}

		@Override
		protected void onPostExecute(Integer result) {
			try {
				zos.flush();
				zos.close();
			} catch (IOException e) {
				Log.e(TAG, "error while closing zos", e);
			} catch (NullPointerException e) {
				Log.e(TAG, "zos was null and couldn't be closed", e);
			}

			try {
				cancelCompression = true;
				progressDialog.cancel();
				if (result == error) {
					Toast.makeText(mContext, R.string.compressing_error,
							Toast.LENGTH_SHORT).show();
				} else if (result == success) {
					Toast.makeText(mContext, R.string.compressing_success,
							Toast.LENGTH_SHORT).show();
				}

				if (onCompressFinishedListener != null)
					onCompressFinishedListener.compressFinished();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public interface OnCompressFinishedListener {
		public abstract void compressFinished();
	}

	public CompressManager setOnCompressFinishedListener(
			OnCompressFinishedListener listener) {
		this.onCompressFinishedListener = listener;
		return this;
	}
}
