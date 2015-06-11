package com.filemanager.search;

import android.content.Context;
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
    private ArrayList<FileHolder> arrayList = new ArrayList<>();

    public SearchListResultAdapter(Context context) {
        this.context = context;
    }

    public void setData(ArrayList<FileHolder> list) {
        arrayList = new ArrayList<>(list);
        notifyDataSetChanged();
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

        holder.primaryInfo.setText(arrayList.get(position).getName());
        holder.secondaryInfo.setText(arrayList.get(position).getFile().getPath());
        return convertView;
    }
}