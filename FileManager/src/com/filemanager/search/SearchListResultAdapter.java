package com.filemanager.search;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.filemanager.R;
import com.filemanager.files.FileHolder;
import com.filemanager.view.ViewHolder;

import java.util.ArrayList;

public class SearchListResultAdapter extends BaseAdapter {
    private Context context;
    private String mQueryWord;
    private ArrayList<FileHolder> arrayList = new ArrayList<>();

    public SearchListResultAdapter(Context context) {
        this.context = context;
    }

    public void setData(ArrayList<FileHolder> list) {
        arrayList = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    public void setQueryWord(String query) {
        if (!TextUtils.isEmpty(query)) {
            mQueryWord = query;
        }
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int position) {
        FileHolder fileHolder = null;
        try {
            fileHolder = arrayList.get(position);
        } catch (Exception e) {

        }
        return fileHolder;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        if (convertView == null) {
            convertView = ((LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                    R.layout.serch_item_filelist, null);
            holder = new ViewHolder();
            holder.primaryInfo = (TextView) convertView.findViewById(R.id.primary_info);
            holder.secondaryInfo = (TextView) convertView.findViewById(R.id.secondary_info);
            convertView.findViewById(R.id.tertiary_info).setVisibility(View.GONE);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String name = arrayList.get(position).getName();
        SpannableStringBuilder style = new SpannableStringBuilder(name);
        if (!TextUtils.isEmpty(mQueryWord)) {
            int start = (name.toLowerCase()).indexOf(mQueryWord.toLowerCase());
            int end = start + mQueryWord.length();
            style.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.search_highlight)), start, end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            holder.primaryInfo.setText(style);
        } else {
            holder.primaryInfo.setText(name);
        }
        holder.secondaryInfo.setText(arrayList.get(position).getFile().getPath());
        return convertView;
    }
}