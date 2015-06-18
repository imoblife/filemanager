package com.filemanager.occupancy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.filemanager.R;
import com.filemanager.ThumbnailLoader;
import com.filemanager.files.FileHolder;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileSizeHolderListAdapter extends BaseAdapter {
    private ArrayList<FileTreeNode<String>> mNodes = new ArrayList<>();
    private HashMap<Integer, FileHolder> mCacheData = new HashMap<>();
    private LayoutInflater mInflater;
    private HashMap<Integer, Double> mCacheRatio = new HashMap<>();
    private Context mContext;
    private int mItemLayoutId = R.layout.item_file_analysis_list;
    private Drawable folderIcon;
    private long mListTotalSize = -1;
    private int mListSize = 0;

    // Thumbnail specific
    private ThumbnailLoader mThumbnailLoader;
    private boolean scrolling = false;


    public FileSizeHolderListAdapter(Context c) {
        folderIcon = c.getResources().getDrawable(R.drawable.file_ic_launcher);
        mInflater = (LayoutInflater) c
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = c;

        mThumbnailLoader = new ThumbnailLoader(c);
    }

    public void seFileListTotalSize(long size) {
        mCacheRatio.clear();
        mListTotalSize = size;
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
        return mListSize;
    }

    @Override
    public Object getItem(int position) {
        return getFileHolder(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Set the layout to be used for item drawing.
     *
     * @param resId The item layout id. 0 to reset.
     */
    public void setItemLayout(int resId) {
        if (resId > 0)
            mItemLayoutId = resId;
        else
            mItemLayoutId = R.layout.item_filelist;
    }

    /**
     * Creates a new list item view, along with it's ViewHolder set as a tag.
     *
     * @return The new view.
     */
    protected View newView() {
        View view = mInflater.inflate(mItemLayoutId, null);

        ViewHolder holder = new ViewHolder();
        holder.icon = (ImageView) view.findViewById(R.id.icon);
        holder.primaryInfo = (TextView) view.findViewById(R.id.primary_info);
        holder.secondaryInfo = (TextView) view
                .findViewById(R.id.secondary_info);
        holder.tertiaryInfo = (TextView) view.findViewById(R.id.tertiary_info);
        holder.progressBar = (ProgressBar) view.findViewById(R.id.size_progress);

        view.setTag(holder);
        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final FileHolder item = getFileHolder(position);

        if (convertView == null) {
            convertView = newView();
        }

        final ViewHolder holder = (ViewHolder) convertView.getTag();

        holder.icon.setImageDrawable(item.getFile().isDirectory() ? folderIcon : item.getIcon());
        holder.primaryInfo.setText(item.getName());
        holder.secondaryInfo.setText(item
                .getFormattedModificationDate(mContext));
        holder.secondaryInfo.setVisibility(View.GONE);

        int childrenCount = item.getFileNode().children.size();
        StringBuilder builder = new StringBuilder();

        if (childrenCount > 0) {
            builder.append(item.getFile().isDirectory() ? childrenCount + " "
                    + mContext.getString(R.string.items) + " | " + item
                    .getFormattedSize(mContext, false) : item
                    .getFormattedSize(mContext, false));
        } else {
            builder.append(item.getFile().isDirectory() ? "0"
                    + " " + mContext.getString(R.string.items) : item
                    .getFormattedSize(mContext, false));
        }

        if (mListTotalSize != -1) {
            double tmpProgress = 0;
            if (mListTotalSize > 0) {
                if (mCacheRatio.get(position) == null) {
                    double tmp = (((double) item.getSize()) / mListTotalSize);
                    mCacheRatio.put(position, tmp);
                }
                DecimalFormat df2 = new DecimalFormat("###.00");
                tmpProgress = Double.valueOf(df2.format(mCacheRatio.get(position) * 100));
                builder.append(" | " + tmpProgress + "%");
            } else {
                builder.append(" | " + "0.00%");
            }
            holder.progressBar.setProgress((int) tmpProgress);
        }

        holder.tertiaryInfo.setText(builder);


        if (shouldLoadIcon(item)) {
            if (mThumbnailLoader != null) {
                mThumbnailLoader.loadImage(item, holder.icon);
            }
        }

        return convertView;
    }

    /**
     * Inform this adapter about scrolling state of list so that lists don't lag
     * due to cache ops.
     *
     * @param isScrolling True if the ListView is still scrolling.
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

    private FileHolder getFileHolder(int position) {
        if (mCacheData.containsKey(position)) {
            return mCacheData.get(position);
        } else {
            FileTreeNode<String> tmpNode = mNodes.get(position);
            FileHolder fileHolder = new FileHolder(new File(mNodes.get(position).data), mContext);
            fileHolder.setNode(tmpNode);
            fileHolder.setSize(tmpNode.size);
            mCacheData.put(position, fileHolder);
            return fileHolder;
        }
    }

    public void setNodeData(ArrayList<FileTreeNode<String>> nodes) {
        if (nodes == null) {
            return;
        }
        mNodes = nodes;
        mListSize = nodes.size();
    }

    public void clearCache() {
        mCacheData.clear();
    }

    public class ViewHolder {
        ImageView icon;
        TextView primaryInfo;
        TextView secondaryInfo;
        TextView tertiaryInfo;
        ProgressBar progressBar;
    }
}