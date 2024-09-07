package com.siprix.sample.fragment;

import android.database.DataSetObserver;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.siprix.sample.MainActivity;
import com.siprix.sample.R;
import com.siprix.sample.adapter.CallsListAdapter;
import com.siprix.sample.model.CallsModel;
import com.siprix.sample.model.ObjModel;


public class CallsFragment extends BaseFragment {
    private ListView callsListView_;
    private ObjModel objModel_;
    private int curCallId_ = CallsModel.kEmptyCallId;
    private FloatingActionButton addCallFab_;
    private boolean isPaused_ = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.calls_fragment, container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        callsListView_ = view.findViewById(R.id.calls_list_view);

        addCallFab_ = view.findViewById(R.id.add_call_fab);
        addCallFab_.setOnClickListener(this::onAddCallClick);

        setModelToFragment();
    }

    @Override
    public void setModel(ObjModel objModel) {
        objModel_ = objModel;
        CallsListAdapter adapter = new CallsListAdapter(objModel_.calls_, getActivity());
        if (callsListView_ != null) callsListView_.setAdapter(adapter);
        objModel_.calls_.setObserver(adapter);//when list of calls changed it notifies adapter

        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() { onCallSwitched();  }
        });
        onCallSwitched();
    }

    void onCallSwitched() {
        final int callId = objModel_.calls_.getSwitchedCallId();
        if(isPaused_ || (callId == curCallId_)) return;
        curCallId_ = callId;

        FragmentManager fragmentMgr = getChildFragmentManager();
        CallSwitchedFragment fragment =(CallSwitchedFragment)fragmentMgr.findFragmentByTag(CallSwitchedFragment.TAG);

        if(callId != CallsModel.kEmptyCallId) {
            if(fragment == null) {
                fragment = new CallSwitchedFragment();
                fragmentMgr.beginTransaction()
                    .replace(R.id.switched_call_container, fragment, CallSwitchedFragment.TAG)
                    .commit();
            }
            fragment.onCallSwitched();
        }

        if((callId == CallsModel.kEmptyCallId)&&(fragment != null)) {
            fragmentMgr.beginTransaction()
                .remove(fragment)
                .commit();
        }

        addCallFab_.setVisibility((callId == CallsModel.kEmptyCallId) ? View.VISIBLE : View.INVISIBLE);
    }

    public void onAddCallClick(View view) {
        CallAddDialog dlg = new CallAddDialog();
        dlg.setCancelable(false);
        dlg.show(requireActivity().getSupportFragmentManager(),"CallAddDialog");
    }

    public void onResume() {
        super.onResume();
        isPaused_ = false;
        onCallSwitched();
    }
    public void onPause() {
        super.onPause();
        isPaused_ = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        objModel_.calls_.setObserver(null);
        objModel_ = null;
    }
}