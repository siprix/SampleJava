package com.siprix.sample.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;


import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.siprix.sample.MainActivity;
import com.siprix.sample.R;
import com.siprix.sample.model.AccountModel;
import com.siprix.sample.model.AccountsModel;
import com.siprix.sample.model.DestModel;
import com.siprix.sample.model.ObjModel;

public class CallAddDialog extends DialogFragment {
    private androidx.appcompat.widget.SwitchCompat videoSwitch_;
    private Spinner accSpinner_;
    private EditText destEdit_;
    private ObjModel objModel_;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.call_add_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> dismiss());

        Button addButton = view.findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> onAddCallClick());

        MainActivity ma = (MainActivity)getActivity();
        objModel_ = (ma != null) ? ma.getObjModel() : null;
        if((objModel_==null)||(objModel_.accounts_.isEmpty())) {
            TextView cantMakeCallText = view.findViewById(R.id.cant_make_call);
            cantMakeCallText.setVisibility(View.VISIBLE);
            addButton.setEnabled(false);
            return;
        }

        destEdit_ = view.findViewById(R.id.dest);
        videoSwitch_ = view.findViewById(R.id.video_switch);
        accSpinner_ = view.findViewById(R.id.acc_spinner);

        AccAdapter adapter = new AccAdapter(objModel_.accounts_, getActivity());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        accSpinner_.setAdapter(adapter);
        accSpinner_.setSelection(objModel_.accounts_.getSelectedAccPosition());

        view.findViewById(R.id.toggle_keyboard).setOnClickListener(this::onToggleKeyboard);
    }

    static private class AccAdapter extends ArrayAdapter<AccountModel> {
        private final AccountsModel dataSet_;

        public AccAdapter(AccountsModel dataSet, Context context) {
            super(context, android.R.layout.simple_spinner_item);
            this.dataSet_ = dataSet;
        }
        @Override
        public int getCount() { return dataSet_.size(); }
        @Override
        public AccountModel getItem(int position) {  return dataSet_.get(position); }
        @Override
        public long getItemId(int position) { return dataSet_.get(position).getAccId(); }
    }

    @Override
    public void onStart() {
        super.onStart();
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.95);
        getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    void onToggleKeyboard(View v) {
        boolean isNumber =destEdit_.getInputType()== InputType.TYPE_CLASS_NUMBER;
        destEdit_.setInputType(isNumber ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_CLASS_NUMBER);
    }

    void onAddCallClick() {
        final String destExt = destEdit_.getText().toString();
        if(destExt.isEmpty()) {
            Toast.makeText(getActivity(), "Destination phone number can't be empty",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            DestModel dest = new DestModel();
            dest.setToExt(destExt);
            dest.setSrcAccId(((AccountModel)accSpinner_.getSelectedItem()).getAccId());
            dest.setWithVideo(videoSwitch_.isChecked());
            objModel_.calls_.invite(dest);

            dismiss();//End dialog when call started

        }catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }
}
