package com.siprix.sample.adapter;

import static android.view.View.VISIBLE;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.siprix.sample.R;
import com.siprix.sample.model.DevicesModel;
import com.siprix.sample.model.ModelObserver;
import com.siprix.SiprixCore;

public class DevicesListAdapter extends BaseAdapter
        implements ModelObserver, PopupMenu.OnMenuItemClickListener {

    private final DevicesModel dataSet_;
    private final Context context_;

    public DevicesListAdapter(DevicesModel dataSet, Context context) {
        this.dataSet_ = dataSet;
        this.context_ = context;
    }

    @Override
    public int getCount() {
        return dataSet_.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onModelChanged() {
        notifyDataSetChanged();
    }

    static class ViewHolder {
        public TextView tvName;
        public Button menuBtn;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context_);
            convertView = inflater.inflate(R.layout.dvc_list_item, parent, false);

            holder = new ViewHolder();
            convertView.setTag(holder);

            holder.tvName = (TextView) convertView.findViewById(R.id.textview_name);
            holder.menuBtn = (Button) convertView.findViewById(R.id.menu_button);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }

        SiprixCore.AudioDevice dvc = dataSet_.get(position);
        holder.tvName.setText(dvc.name());
        holder.menuBtn.setOnClickListener(v -> onMenuBtnClick(position, holder.menuBtn));

        convertView.setBackgroundColor(dataSet_.isSelected(dvc) ? 0x9934B5E4 : 0x1134B5E4);
        holder.menuBtn.setVisibility(dataSet_.isSelected(dvc) ? View.INVISIBLE : View.VISIBLE);
        return convertView;
    }

    static final int kMenuIdSelect=1;
    private int popupMenuPosition_=-1;

    void onMenuBtnClick(int position, View v) {
        popupMenuPosition_ = position;
        SiprixCore.AudioDevice dvc = dataSet_.get(position);
        if(dataSet_.isSelected(dvc)) return;

        PopupMenu popup = new PopupMenu(context_, v);
        popup.getMenu().add(Menu.NONE, kMenuIdSelect, 1, "Select");
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        try {
            SiprixCore.AudioDevice dvc = dataSet_.get(popupMenuPosition_);
            if(item.getItemId()==kMenuIdSelect)
            {
                dataSet_.selectDevice(dvc);
                return true;
            }
        } catch (Exception e) {
            Toast.makeText(context_, e.toString(), Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}

