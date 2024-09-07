package com.siprix.sample.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.siprix.sample.MainActivity;
import com.siprix.sample.R;
import com.siprix.sample.adapter.AccountsListAdapter;
import com.siprix.sample.model.ObjModel;


public class AccountsFragment extends BaseFragment {
    private ListView accountsListView_;
    private ObjModel objModel_;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.accounts_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        accountsListView_ = view.findViewById(R.id.accounts_list_view);

        view.findViewById(R.id.add_acc_fab).setOnClickListener(this::onAddAccClick);
        view.findViewById(R.id.menu_btn).setOnClickListener(this::onExtraMenuClick);

        setModelToFragment();
    }

    @Override
    public void setModel(ObjModel objModel) {
        objModel_ = objModel;
        AccountsListAdapter adapter = new AccountsListAdapter(objModel_.accounts_, getActivity());
        if(accountsListView_ != null) accountsListView_.setAdapter(adapter);
        objModel_.accounts_.setObserver(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        objModel_.accounts_.setObserver(null);
        objModel_ = null;
    }

    public void onAddAccClick(View view) {
        AccountAddDialog dlg = new AccountAddDialog();
        dlg.setCancelable(false);
        dlg.show(requireActivity().getSupportFragmentManager(),"AccountAddDialog");
    }

    static final int kMenuIdToggleForeground=14;
    public void onExtraMenuClick(View view) {
        MainActivity ma = (MainActivity)getActivity();
        if(ma != null) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getActivity(), view);
            popup.getMenu().add(Menu.NONE, kMenuIdToggleForeground, 1,
                    ma.isForegroundMode() ? "Stop Foreground" : "Start Foreground");
        popup.setOnMenuItemClickListener(this::onMenuItemClick);
        popup.show();
    }
    }

    public boolean onMenuItemClick(MenuItem item) {
        MainActivity ma = (MainActivity)getActivity();
        if((ma != null)&&(item.getItemId() == kMenuIdToggleForeground)) {
            ma.toggleForegroundMode();
        }
        return true;
    }

}