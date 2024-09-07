package com.siprix.sample.adapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.siprix.SiprixCore;
import com.siprix.SiprixVideoRenderer;
import com.siprix.sample.R;
import com.siprix.sample.model.CallModel;
import com.siprix.sample.model.CallsModel;
import com.siprix.sample.model.ModelObserver;

public class CallsListAdapter extends BaseAdapter
        implements ModelObserver, PopupMenu.OnMenuItemClickListener {

    private final CallsModel dataSet_;
    private final Context context_;

    public CallsListAdapter(CallsModel dataSet, Context context) {
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
        return dataSet_.get(position).getCallId();
    }

    @Override
    public void onModelChanged() {
        if(popup_!=null) popup_.dismiss();
        notifyDataSetChanged();
    }

    static class ViewHolder {
        ImageView imgDirection, imgSecure;
        ProgressBar progress;
        TextView tvRemote, tvState;
        Button menuBtn;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        CallModel m = dataSet_.get(position);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context_);
            convertView = inflater.inflate(R.layout.call_list_item, parent, false);

            holder = new ViewHolder();
            convertView.setTag(holder);

            holder.imgDirection = convertView.findViewById(R.id.image_direction);
            holder.imgSecure = convertView.findViewById(R.id.secure_media);
            holder.tvRemote = convertView.findViewById(R.id.textview_remote_ext);
            holder.tvState = convertView.findViewById(R.id.textview_state);
            holder.progress= convertView.findViewById(R.id.progressbar);

            holder.menuBtn = (Button) convertView.findViewById(R.id.menu_button);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }

        holder.imgDirection.setImageResource(m.getIsIncoming() ? R.drawable.baseline_call_received_24
                                                                : R.drawable.baseline_call_made_24);
        boolean bStable = (m.getState() == CallModel.CallState.Connected)||
                          (m.getState() == CallModel.CallState.Held);
        holder.progress.setVisibility(bStable ? View.INVISIBLE : View.VISIBLE);
        holder.imgSecure.setVisibility(m.hasSecureMedia() ? View.VISIBLE : View.INVISIBLE);
        holder.tvRemote.setText(m.getNameAndExt());
        holder.tvState.setText(m.getState().name());

        holder.menuBtn.setOnClickListener(v -> onMenuBtnClick(position, holder.menuBtn));
        holder.menuBtn.setEnabled(!dataSet_.isSwitchedCall(m));

        convertView.setBackgroundColor(dataSet_.isSwitchedCall(m) ? 0x9934B5E4 : 0x1134B5E4);
        return convertView;
    }

    static final int kMenuIdAccept=1;
    static final int kMenuIdReject=2;
    static final int kMenuIdHangup=3;
    static final int kMenuIdSendTone=4;
    static final int kMenuIdMuteMic=5;
    static final int kMenuIdMuteCam=6;
    static final int kMenuIdSwitchTo=7;
    static final int kMenuIdTransfer=8;
    static final int kMenuIdHold=0;
    private int popupMenuCallPos_=-1;
    private PopupMenu popup_;

    void onMenuBtnClick(int position, View v) {
        popupMenuCallPos_ = position;

        PopupMenu popup = new PopupMenu(context_, v);
        CallModel m = dataSet_.get(position);

        if(m.getState()==CallModel.CallState.Ringing) {
            popup.getMenu().add(Menu.NONE, kMenuIdAccept, 1, "Accept");
            popup.getMenu().add(Menu.NONE, kMenuIdReject, 2, "Reject");
        }
        else {
            popup.getMenu().add(Menu.NONE, kMenuIdSwitchTo, 3, "SwitchTo");

            if (m.getState() == CallModel.CallState.Connected) {
                popup.getMenu().add(Menu.NONE, kMenuIdHold, 3, "Hold");
            }

            if (m.getHoldState().isLocal()) {
                popup.getMenu().add(Menu.NONE, kMenuIdHold, 3, "UnHold");
            }

            popup.getMenu().add(Menu.NONE, kMenuIdHangup, 3, "Hangup");
        }

        popup_ = popup;
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        try {
            CallModel m = dataSet_.get(popupMenuCallPos_);
            switch (item.getItemId()) {
                case kMenuIdAccept:   m.accept(true);          return true;
                case kMenuIdReject:   m.reject();                       return true;
                case kMenuIdMuteMic:  m.muteMic(!m.isMicMuted());       return true;
                case kMenuIdMuteCam:  m.muteCam(!m.isCamMuted());       return true;
                case kMenuIdSwitchTo: dataSet_.switchToCall(m.getCallId()); return true;
                case kMenuIdTransfer: transferCall(m);                  return true;
                case kMenuIdHold:     m.hold();                         return true;
                case kMenuIdHangup:   m.bye();                          return true;
            }
        } catch (Exception e) {
            Toast.makeText(context_, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void transferCall(CallModel m) {
        EditText toExtEdit = new EditText(context_);
        AlertDialog dialog = new AlertDialog.Builder(context_)
            .setTitle("Transfer")
            .setMessage("Destination extension:")
            .setView(toExtEdit)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String toExt = toExtEdit.getText().toString();
                    try {
                        m.transferBlind(toExt);
                    } catch (Exception e) {
                        Toast.makeText(context_, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
        dialog.show();
    }
}
