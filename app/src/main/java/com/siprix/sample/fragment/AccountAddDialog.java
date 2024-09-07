package com.siprix.sample.fragment;

import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.siprix.AccData;
import com.siprix.sample.MainActivity;
import com.siprix.sample.R;
import com.siprix.sample.model.AccountModel;
import com.siprix.sample.model.ObjModel;

import java.util.ArrayList;
import java.util.List;

public class AccountAddDialog extends DialogFragment {
    private ObjModel objModel_;

    EditText domainEdit_, extensionEdit_, passwordEdit_, expireEdit_;
    Spinner transpSpinner_;
    CheckBox rewriteCheck_;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.account_add_dialog,
                container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> dismiss());

        Button addButton = view.findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> onAddAccountClick());

        MainActivity ma = (MainActivity)getActivity();
        objModel_ = (ma != null) ? ma.getObjModel() : null;
        if(objModel_==null) {
            addButton.setEnabled(false);
            return;
        }

        ArrayAdapter<AccData.SipTransport> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, AccData.SipTransport.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        domainEdit_ = view.findViewById(R.id.domain);
        extensionEdit_ = view.findViewById(R.id.extension);
        passwordEdit_ = view.findViewById(R.id.password);
        transpSpinner_ = view.findViewById(R.id.transp_spinner);
        expireEdit_ = view.findViewById(R.id.expire);
        rewriteCheck_ = view.findViewById(R.id.rewriteContactIp);
        transpSpinner_.setAdapter(adapter);
    }

    void onAddAccountClick() {
        String domain = domainEdit_.getText().toString();
        String extension = extensionEdit_.getText().toString();
        String password = passwordEdit_.getText().toString();
        String expireStr = expireEdit_.getText().toString();

        if(domain.isEmpty()||extension.isEmpty()||password.isEmpty()) {
            Toast.makeText(getActivity(), "Domain/extension/password can't be empty",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try{
            AccountModel acc = new AccountModel();
            acc.setSipServer(domain);
            acc.setSipExtension(extension);
            acc.setSipPassword(password);
            acc.setExpireTime(expireStr.isEmpty() ? 0 : Integer.parseInt(expireStr));
            acc.setSipTranspProtocol((AccData.SipTransport) transpSpinner_.getSelectedItem());
            acc.setRewriteContactIp(rewriteCheck_.isChecked());

            acc.resetAudioCodecs();
            acc.addAudioCodec(AccData.AudioCodec.PCMU);
            acc.addAudioCodec(AccData.AudioCodec.PCMA);
            acc.addAudioCodec(AccData.AudioCodec.DTMF);

            if(objModel_.accounts_.add(acc))
                dismiss();

        }catch(Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.95);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.60);
        getDialog().getWindow().setLayout(width, height);
    }
}