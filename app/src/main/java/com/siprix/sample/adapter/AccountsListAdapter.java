package com.siprix.sample.adapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.siprix.AccData;
import com.siprix.sample.R;
import com.siprix.sample.model.AccountModel;
import com.siprix.sample.model.AccountsModel;
import com.siprix.sample.model.ModelObserver;

public class AccountsListAdapter extends BaseAdapter implements ModelObserver {

    private final AccountsModel dataSet_;
    private final Context context_;

    public AccountsListAdapter(AccountsModel dataSet, Context context) {
        this.dataSet_ = dataSet;
        this.context_ = context;
    }

    @Override
    public int getCount() {
        return dataSet_.size();
    }
    @Override
    public Object getItem(int position) {  return dataSet_.get(position);   }
    @Override
    public long getItemId(int position) {
        return dataSet_.get(position).getAccId();
    }

    @Override
    public void onModelChanged() {
        notifyDataSetChanged();
    }

    static class ViewHolder {
        ImageView imgState;
        ProgressBar progress;
        public TextView tvName, tvRegText;
        public Button menuBtn;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context_);
            convertView = inflater.inflate(R.layout.account_list_item, parent, false);

            holder = new ViewHolder();
            convertView.setTag(holder);

            holder.imgState = convertView.findViewById(R.id.image_state);
            holder.progress= convertView.findViewById(R.id.progressbar);
            holder.tvName = convertView.findViewById(R.id.textview_name);
            //holder.tvState = (TextView) convertView.findViewById(R.id.textview_state);
            holder.tvRegText = (TextView) convertView.findViewById(R.id.textview_regtext);
            holder.menuBtn = (Button) convertView.findViewById(R.id.menu_button);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }

        AccountModel m = dataSet_.get(position);
        boolean regInProgress = (m.getState()== AccData.RegState.INPROGRES);
        holder.progress.setVisibility(regInProgress ? View.VISIBLE : View.INVISIBLE);
        holder.imgState.setVisibility(regInProgress ? View.INVISIBLE : View.VISIBLE);
        if(!regInProgress) setAccStateIcon(holder.imgState, m.getState());

        holder.tvName.setText(m.getUri());
        holder.tvRegText.setText(String.format("ID: %d REG: %s", m.getAccId(), m.getRegText()));
        holder.menuBtn.setOnClickListener(v -> onMenuBtnClick(position, holder.menuBtn));
        holder.menuBtn.setEnabled(!dataSet_.isNetworkLost());

        convertView.setBackgroundColor(dataSet_.getSelectedAccId() == m.getAccId() ? 0x9934B5E4 : 0x1134B5E4);
        convertView.setOnClickListener(v -> { dataSet_.setSelectedAccId(m.getAccId()); });
        return convertView;
    }

    void setAccStateIcon(ImageView imgState, AccData.RegState s) {
        if(s == AccData.RegState.SUCCESS) {
            imgState.setImageResource(R.drawable.baseline_cloud_done_24);
            imgState.setColorFilter(ContextCompat.getColor(context_, R.color.green));
        }else
        if(s == AccData.RegState.FAILED) {
            imgState.setImageResource(R.drawable.baseline_cloud_off_24);
            imgState.setColorFilter(ContextCompat.getColor(context_, R.color.red));
        }else {
            imgState.setImageResource(R.drawable.baseline_done_24);
            imgState.setColorFilter(ContextCompat.getColor(context_, R.color.grey));
        }
    }


    static final int kMenuIdRegister=1;
    static final int kMenuIdUnegister=2;
    static final int kMenuIdRemove=3;
    private int popupMenuPosition_=-1;

    void onMenuBtnClick(int position, View v) {
        popupMenuPosition_ = position;

        PopupMenu popup = new PopupMenu(context_, v);
        popup.getMenu().add(Menu.NONE, kMenuIdRegister, 1, "Register");
        popup.getMenu().add(Menu.NONE, kMenuIdUnegister, 2, "Unregister");
        popup.getMenu().add(Menu.NONE, kMenuIdRemove, 3, "Delete");
        popup.setOnMenuItemClickListener(this::onMenuItemClick);
        popup.show();
    }

    boolean onMenuItemClick(MenuItem item) {
        try {
            AccountModel m = dataSet_.get(popupMenuPosition_);

            switch (item.getItemId()) {
                case kMenuIdRegister:
                    dataSet_.register(m.getAccId(), 300);
                    return true;
                case kMenuIdUnegister:
                    dataSet_.unregister(m.getAccId());
                    return true;
                case kMenuIdRemove:
                    dataSet_.remove(m.getAccId());
                    return true;
            }
        } catch (Exception e) {
            Toast.makeText(context_, e.toString(), Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
