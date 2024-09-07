package com.siprix.sample.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.siprix.sample.MainActivity;
import com.siprix.sample.R;
import com.siprix.sample.model.ModelObserver;
import com.siprix.sample.model.ObjModel;


public class LogsFragment extends BaseFragment {
    private ObjModel objModel_;
    android.widget.TextView textViewLogs_;
    FragmentActivity f;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.logs_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        textViewLogs_ = view.findViewById(R.id.text_logs);

        setModelToFragment();
    }

    @Override
    public void setModel(ObjModel objModel) {
        objModel_ = objModel;
        objModel_.setObserver(this::onModelChanged);
        onModelChanged();
    }

    public void onModelChanged() {
        textViewLogs_.setText(objModel_.getLogsText());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        objModel_.setObserver(null);
        objModel_ = null;
    }
}