package com.filemanager;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.filemanager.files.FileHolder;
import com.filemanager.view.ViewHolder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileHolderListAdapter extends BaseAdapter {
	private List<FileHolder> mItems;
	private LayoutInflater mInflater;
	private Context mContext;
	private int mItemLayoutId = R.layout.item_filelist;
    private HashMap<File,Integer> mFileListsSize;
    private boolean mIsSelectMod = false;

	// Thumbnail specific
	private ThumbnailLoader mThumbnailLoader;
	private boolean scrolling = false;
    private String mKeyword;
    private boolean mIsOnlyShowDir = false;
    private DirFilter mDirFilter;

	//	private ExecutorService executorService = Executors.newFixedThreadPool(3);

	public FileHolderListAdapter(List<FileHolder> files, Context c) {
		mItems = files;
		mInflater = (LayoutInflater) c
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mContext = c;

        mFileListsSize = new HashMap<>();
		mThumbnailLoader = new ThumbnailLoader(c);
        mDirFilter = new DirFilter();
	}

    public boolean isSelectMod() {
        return mIsSelectMod;
    }

    public void setSelectMod(boolean selectMod) {
        this.mIsSelectMod = selectMod;
        notifyDataSetChanged();
    }

    public boolean isOnlyShowDiy() {
        return mIsOnlyShowDir;
    }

    public void setOnlyShowDir(boolean onlyShowDir) {
        this.mIsOnlyShowDir = onlyShowDir;
    }

    public ArrayList<FileHolder> getSelectedItemList() {
        ArrayList<FileHolder> list = new ArrayList<>();
        for (FileHolder item : mItems) {
            if (item.isSelect) {
                list.add(item);
            }
        }
        return list;
    }

    public void toggleAllItemState(boolean selected) {
        for (FileHolder item : mItems) {
            item.isSelect = selected;
        }
    }

    public Context getContext() {
		return mContext;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getCount() {
		return mItems.size();
	}

	@Override
	public Object getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Set the layout to be used for item drawing.
	 * 
	 * @param resId
	 *            The item layout id. 0 to reset.
	 */
	public void setItemLayout(int resId) {
		if (resId > 0)
			mItemLayoutId = resId;
		else
			mItemLayoutId = R.layout.item_filelist;
	}

    public void setData(ArrayList<FileHolder> list){
        mItems = list;
        notifyDataSetChanged();
    }

	/**
	 * Creates a new list item view, along with it's ViewHolder set as a tag.
	 * 
	 * @return The new view.
	 */
	protected View newView() {
		View view = mInflater.inflate(mItemLayoutId, null);

		ViewHolder holder = new ViewHolder();
        holder.content = (LinearLayout) view.findViewById(R.id.item_ll);
		holder.icon = (ImageView) view.findViewById(R.id.icon);
		holder.primaryInfo = (TextView) view.findViewById(R.id.primary_info);
		holder.secondaryInfo = (TextView) view
				.findViewById(R.id.secondary_info);
		holder.tertiaryInfo = (TextView) view.findViewById(R.id.tertiary_info);
        holder.checkBox = (CheckBox) view.findViewById(R.id.checkbox_cb);

		view.setTag(holder);
		return view;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final FileHolder item = mItems.get(position);

		if (convertView == null)
			convertView = newView();

		final ViewHolder holder = (ViewHolder) convertView.getTag();

		holder.icon.setImageDrawable(item.getIcon());
		holder.primaryInfo.setText(item.getName());
		holder.secondaryInfo.setText(item
				.getFormattedModificationDate(mContext));
		// Hide directories' size as it's irrelevant if we can't recursively
		// find it.
        holder.checkBox.setVisibility(mIsSelectMod ? View.VISIBLE : View.GONE);
        int bottom = holder.content.getPaddingBottom();
        int top = holder.content.getPaddingTop();
        int right = holder.content.getPaddingRight();
        int left = holder.content.getPaddingLeft();
        if (mIsSelectMod) {
            holder.checkBox.setChecked(item.isSelect);
            holder.content.setBackgroundResource(item.isSelect ? R.drawable.item_selected : R.drawable.base_card_selector);
        } else {
            holder.content.setBackgroundResource(R.drawable.base_card_selector);
        }
        holder.content.setPadding(left, top, right, bottom);

        String tertiaryInfo;
        File file = item.getFile();
        if (!mFileListsSize.containsKey(file)) {
            String[] files = file.list(mIsOnlyShowDir ? mDirFilter : null);

            if (files != null) {
                mFileListsSize.put(file, files.length);
                tertiaryInfo = file.isDirectory() ? files.length + " "
                        + mContext.getString(R.string.items) : item
                        .getFormattedSize(mContext, false);
            } else {
                mFileListsSize.put(file, 0);
                tertiaryInfo = file.isDirectory() ? "0"
                        + " " + mContext.getString(R.string.items) : item
                        .getFormattedSize(mContext, false);
            }
        } else {
            tertiaryInfo = file.isDirectory() ? mFileListsSize.get(file) + " "
                    + mContext.getString(R.string.items) : item
                    .getFormattedSize(mContext, false);
        }

        holder.tertiaryInfo.setText(tertiaryInfo);

		if (shouldLoadIcon(item)) {
            try {
                if (mThumbnailLoader != null) {
                    mThumbnailLoader.loadImage(item, holder.icon);
                }
            }catch (OutOfMemoryError outOfMemoryError){
            }catch (Exception e){
            }
		}

        if (!TextUtils.isEmpty(mKeyword) && mKeyword.equals(file.getName())) {
            holder.content.setPressed(true);
        } else {
			holder.content.setPressed(false);
		}
        return convertView;
	}

	/**
	 * Inform this adapter about scrolling state of list so that lists don't lag
	 * due to cache ops.
	 * 
	 * @param isScrolling
	 *            True if the ListView is still scrolling.
	 */
	public void setScrolling(boolean isScrolling) {
		scrolling = isScrolling;
		if (!isScrolling)
			notifyDataSetChanged();
	}

	private boolean shouldLoadIcon(FileHolder item) {
		return !scrolling && item.getFile().isFile()
				&& !item.getMimeType().equals("video/mpeg");
	}

    public void setHighlightKeyword(String keyword) {
        mKeyword = keyword;
    }

	public int findHighlightPosition() {
		for(int i = 0; i < getCount(); i++) {
			FileHolder item = (FileHolder) getItem(i);
			if(!TextUtils.isEmpty(mKeyword)
					&& mKeyword.equals(item.getName())) {
				return i;
			}
		}
		return -1;
	}

    public void clearFileChildrenCache(){
        mFileListsSize.clear();
    }

    public void startProcessingThumbnailLoaderQueue() {
        if (mThumbnailLoader != null) {
            mThumbnailLoader.startProcessingLoaderQueue();
        }
    }

    public void stopProcessingThumbnailLoaderQueue() {
        if (mThumbnailLoader != null) {
            mThumbnailLoader.stopProcessingLoaderQueue();
        }
    }

    private class DirFilter implements FilenameFilter{

        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            return file.isDirectory();
        }
    }
}