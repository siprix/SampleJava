package com.siprix.sample.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.siprix.SiprixCore;
import com.siprix.SiprixVideoRenderer;
import com.siprix.sample.MainActivity;
import com.siprix.sample.R;

import com.siprix.sample.model.CallModel;
import com.siprix.sample.model.CallsModel;
import com.siprix.sample.model.DevicesModel;
import com.siprix.sample.model.ModelObserver;
import com.siprix.sample.model.ObjModel;

import java.util.ArrayList;
import java.util.HashMap;


public class CallSwitchedFragment extends Fragment implements ModelObserver {
    public static final String TAG = "CallSwitchedFragment";
    private TextView nameAndExtText_, callStateText_, accountText_, callIdText_;
    private TextView sentDtmfText_, rcvdDtmfText_, durationText_;
    private Button muteMicBtn_, hangupBtn_, dtmfPanelOpenBtn_, holdBtn_;
    private RelativeLayout mainCtrlsPanel_, dtmfCtrlsPanel_, incCtrlsPanel_, transCtrlPanel, transAttCtrlPanel;
    private android.widget.PopupMenu popupMenu_;
    private Spinner transferToCallSpinner_;
    private EditText transferExtText_;
    private SiprixVideoRenderer previewRenderer_;
    private SiprixVideoRenderer remoteRenderer_;
    private ObjModel objModel_;
    private CallModel call_;

    enum UiMode { eUndef, eMain, eDtmf, eTransferBlind, eTransferAtt };
    HashMap<CallModel.CallState, UiMode> uiModes_ = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.call_switched_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        nameAndExtText_ = view.findViewById(R.id.name_and_ext);
        callStateText_ = view.findViewById(R.id.call_state);
        accountText_ = view.findViewById(R.id.account);
        callIdText_ = view.findViewById(R.id.call_id);
        rcvdDtmfText_ = view.findViewById(R.id.received_dtmf);
        sentDtmfText_ = view.findViewById(R.id.sent_dtmf);
        durationText_ = view.findViewById(R.id.call_duration);
        transferExtText_ = view.findViewById(R.id.transfer_to_ext);
        transferToCallSpinner_ = view.findViewById(R.id.transfer_to_call_spinner);

        previewRenderer_ = view.findViewById(R.id.preview_renderer);
        remoteRenderer_ = view.findViewById(R.id.remote_renderer);

        hangupBtn_ = view.findViewById(R.id.hangup_button);
        holdBtn_= view.findViewById(R.id.main_hold_btn);

        incCtrlsPanel_ = view.findViewById(R.id.incoming_call_ctrls_panel);
        mainCtrlsPanel_ = view.findViewById(R.id.main_ctrls_panel);
        dtmfCtrlsPanel_ = view.findViewById(R.id.dtmf_panel);
        transCtrlPanel= view.findViewById(R.id.transfer_panel);
        transAttCtrlPanel= view.findViewById(R.id.transfer_att_panel);

        muteMicBtn_ = view.findViewById(R.id.main_mute_btn);
        dtmfPanelOpenBtn_ = view.findViewById(R.id.dtmf_panel_open);

        view.findViewById(R.id.main_mute_btn).setOnClickListener(this::onMuteMicClick);
        view.findViewById(R.id.main_add_call_btn).setOnClickListener(this::onAddCallClick);
        view.findViewById(R.id.main_hold_btn).setOnClickListener(this::onHoldCallClick);
        view.findViewById(R.id.main_menu_btn).setOnClickListener(this::onExtraMenuOpenClick);
        view.findViewById(R.id.dtmf_panel_open).setOnClickListener(this::onToggleDtmfPanelClick);
        view.findViewById(R.id.dtmf_panel_close).setOnClickListener(this::onToggleDtmfPanelClick);

        view.findViewById(R.id.preview_renderer).setOnClickListener(this::onMuteCameraClick);
        view.findViewById(R.id.main_speaker_btn).setOnClickListener(this::onSpeakerMenuOpenClick);
        view.findViewById(R.id.do_transfer_btn).setOnClickListener(this::onDoTransferClick);
        view.findViewById(R.id.do_transfer_att_btn).setOnClickListener(this::onDoTransferAttClick);
        view.findViewById(R.id.transfer_panel_close).setOnClickListener(this::onToggleTransferPanelClick);
        view.findViewById(R.id.transfer_att_panel_close).setOnClickListener(this::onToggleTransferAttPanelClick);

        view.findViewById(R.id.dtmf_btn_0).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_1).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_2).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_3).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_4).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_5).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_6).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_7).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_8).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_9).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_0).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_hash).setOnClickListener(this::onSendDtmfClick);
        view.findViewById(R.id.dtmf_btn_ast).setOnClickListener(this::onSendDtmfClick);

        view.findViewById(R.id.accept_btn).setOnClickListener(this::onAcceptCallClick);
        view.findViewById(R.id.reject_btn).setOnClickListener(this::onRejectCallClick);
        view.findViewById(R.id.hangup_button).setOnClickListener(this::onHangupClick);

        MainActivity ma = (MainActivity)getActivity();
        objModel_ = (ma != null) ? ma.getObjModel() : null;
        subscribeToSwitchedCall();
    }

    void unsubscribeFromCurrentCall() {
        if(call_ != null) {
            call_.setVideoRenderer(null);
            call_.setObserver(null);
            call_ = null;
        }
        if(objModel_ != null) {
            objModel_.calls_.setPreviewVideoRenderer(null);
        }
        if(popupMenu_ != null) {
            popupMenu_.dismiss();
            popupMenu_ = null;
        }
    }

    public void onCallSwitched() {
        unsubscribeFromCurrentCall();
        subscribeToSwitchedCall();
    }

    void subscribeToSwitchedCall() {
        call_ = (objModel_==null) ? null : objModel_.calls_.getSwitchedCall();
        if(call_ == null) return;

        call_.setObserver(this);

        if(call_.hasVideo()) {
            call_.setVideoRenderer(remoteRenderer_);
            objModel_.calls_.setPreviewVideoRenderer(previewRenderer_);
        }
        remoteRenderer_.setVisibility(call_.hasVideo() ? View.VISIBLE : View.INVISIBLE);
        previewRenderer_.setVisibility(call_.hasVideo() ? View.VISIBLE : View.INVISIBLE);
        resetUiModes();
        onModelChanged();
    }

    @Override
    public void onModelChanged() {//some of Call attributes has changed
        nameAndExtText_.setText(call_.getNameAndExt());
        accountText_.setText("Acc: " + call_.getAccUri());
        callIdText_.setText("CallId: "+call_.getCallId());
        callStateText_.setText("State: "+call_.getState().name());
        durationText_.setText(call_.getDurationStr());
        rcvdDtmfText_.setText("DTMF: " + call_.getReceivedDtmf());
        rcvdDtmfText_.setVisibility(call_.getReceivedDtmf().isEmpty() ? View.GONE:View.VISIBLE);

        int holdColor = call_.getHoldState().isLocal() ? R.color.green : R.color.purple_500;
        holdBtn_.setBackgroundColor(getResources().getColor(holdColor));

        updateVisibility();
    }

    void updateVisibility() {
        final boolean isConnected = (call_.getState()==CallModel.CallState.Connected);
        final boolean isRinging   = (call_.getState()==CallModel.CallState.Ringing);
        UiMode uiMode = getUiMode(call_.getState());

        dtmfPanelOpenBtn_.setVisibility(isConnected ? View.VISIBLE : View.INVISIBLE);
        incCtrlsPanel_.setVisibility(isRinging ? View.VISIBLE : View.GONE);
        hangupBtn_.setVisibility(isRinging ? View.GONE : View.VISIBLE);

        //bnRedirect.setVisibility((uiMode == UiMode.eUndef)&&isRinging ? View.VISIBLE : View.GONE);
        mainCtrlsPanel_.setVisibility((uiMode == UiMode.eMain)         ? View.VISIBLE : View.GONE);
        dtmfCtrlsPanel_.setVisibility((uiMode == UiMode.eDtmf)         ? View.VISIBLE : View.GONE);
        transCtrlPanel.setVisibility((uiMode == UiMode.eTransferBlind) ? View.VISIBLE : View.GONE);
        transAttCtrlPanel.setVisibility((uiMode == UiMode.eTransferAtt) ? View.VISIBLE : View.GONE);
        //bnTransfer.Content = callModel_.IsRinging ? "Redirect" : "Transfer";
    }

    public void onMuteMicClick(View view) {
        try {
            call_.muteMic(!call_.isMicMuted());

            if(muteMicBtn_ instanceof MaterialButton) {
                ((MaterialButton) muteMicBtn_).setIconResource(
                        call_.isMicMuted() ? R.drawable.baseline_mic_off_24
                                : R.drawable.baseline_mic_24);
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onMuteCameraClick(View v) {
        try {
            call_.muteCam(!call_.isCamMuted());
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onToggleTransferPanelClick(View v) {
        UiMode uiMode = getUiMode(call_.getState());
        if (uiMode == UiMode.eTransferBlind) {
            transferExtText_.setText("");
            setUiMode(call_.getState(), UiMode.eMain);//Hide transfer/redirect panel
        }else{
            transferExtText_.requestFocus();
            setUiMode(call_.getState(), UiMode.eTransferBlind);//Show transfer/redirect panel
        }
        updateVisibility();
    }

    void onToggleTransferAttPanelClick(View v) {
        UiMode uiMode = getUiMode(call_.getState());
        if (uiMode == UiMode.eTransferAtt) {
            transferToCallSpinner_.setAdapter(null);
            setUiMode(call_.getState(), UiMode.eMain);
        }else{
            //Collect calls
            ArrayList<CallModel> callsList = new ArrayList<>();
            for(int i=0; i <  objModel_.calls_.size(); ++i) {
                CallModel m = objModel_.calls_.get(i);
                if(m.getCallId() != call_.getCallId()) callsList.add(m);
            }
            if(callsList.isEmpty()) return;

            ArrayAdapter<CallModel> adapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_item, callsList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            transferToCallSpinner_.setAdapter(adapter);

            setUiMode(call_.getState(), UiMode.eTransferAtt);//Show att transfer panel
        }
        updateVisibility();
    }

    void onToggleDtmfPanelClick(View v) {
        UiMode uiMode = getUiMode(call_.getState());
        if (uiMode == UiMode.eDtmf) {
            setUiMode(call_.getState(), UiMode.eMain);//Hide DTMF panel
        } else {
            sentDtmfText_.setText("");
            setUiMode(call_.getState(), UiMode.eDtmf);//Show DTMF panel
        }
        updateVisibility();
    }

    void onSendDtmfClick(View v) {
        String tag = ((Button)v).getText().toString();
        try {
            call_.sendDtmf(tag);
            sentDtmfText_.append(tag);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onAddCallClick(View v) {
        CallAddDialog dlg = new CallAddDialog();
        dlg.setCancelable(false);
        dlg.show(requireActivity().getSupportFragmentManager(),"CallAddDialog");
    }

    void onHoldCallClick(View v) {
        try {
            call_.hold();
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onAcceptCallClick(View v) {
        try {
            call_.accept(true);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onRejectCallClick(View v) {
        try {
            call_.reject();
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onHangupClick(View v) {
        try {
            call_.bye();
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onDoTransferClick(View v) {
        try {
            String toExt = transferExtText_.getText().toString();
            call_.transferBlind(toExt);
            onToggleTransferPanelClick(null);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onDoTransferAttClick(View v) {
        try {
            CallModel m =  (CallModel) transferToCallSpinner_.getSelectedItem();
            if(m != null) call_.transferAttended(m.getCallId());
            onToggleTransferAttPanelClick(null);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onTogglePlayFileClick() {
        try {
            if(call_.isFilePlaying()) {
                call_.stopPlayFile();
            }
            else {
                MainActivity ma = (MainActivity) getActivity();
                String pathToMp3 = (ma == null) ? null : ma.getPathToPlayMp3();
                call_.playFile(pathToMp3);
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onToggleRecordFileClick() {
        //try{
        //    if(call_.isRecStarted())    call_.stopRecordFile();
        //    else                        call_.recordFile();//TODO path to record file
        //} catch (Exception e) {
        //    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        //}
        Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
    }

    void onToggleConferenceClick() {
        try {
            if(objModel_.calls_.isConfModeStarted())
                objModel_.calls_.switchToCall(objModel_.calls_.getSwitchedCallId());
            else
                objModel_.calls_.makeConference();
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void onSwitchCameraClick() {
        objModel_.devices_.switchCamera();
    }

    static final int kMenuIdConf=1;
    static final int kMenuIdTransferAtt=2;
    static final int kMenuIdTransfer=3;
    static final int kMenuIdPlay=4;
    static final int kMenuIdRecord=5;
    static final int kMenuIdSwitchCamera=6;
    static final int kMenuIdSpeaker=100;

    void onSpeakerMenuOpenClick(View v) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getActivity(), v);
        for(int i=0; i<objModel_.devices_.size(); ++i) {
            SiprixCore.AudioDevice dvc = objModel_.devices_.get(i);
            MenuItem m = popup.getMenu().add(Menu.NONE, kMenuIdSpeaker+i, 1, dvc.name());
            m.setChecked(objModel_.devices_.isSelected(dvc));
        }
        popupMenu_ = popup;
        popup.setOnMenuItemClickListener(this::onSelectSpeakerMenuSelectItemClick);
        popup.show();
    }

    boolean onSelectSpeakerMenuSelectItemClick(MenuItem item) {
        int selIdx = item.getItemId()-kMenuIdSpeaker;
        objModel_.devices_.selectDevice(selIdx);
        popupMenu_ = null;
        return true;
    }

    void onExtraMenuOpenClick(View v) {
        final boolean has2Calls = (objModel_.calls_.size() > 1);
        final boolean isConnected = (call_.getState()==CallModel.CallState.Connected);

        android.widget.PopupMenu popup = new android.widget.PopupMenu(getActivity(), v);
        MenuItem m = popup.getMenu().add(Menu.NONE, kMenuIdConf, 1,
                objModel_.calls_.isConfModeStarted() ? "Stop conference" : "Make conference");
        m.setEnabled(has2Calls);

        m = popup.getMenu().add(Menu.NONE, kMenuIdTransferAtt, 2, "Transfer attended");
        m.setEnabled(has2Calls);

        m = popup.getMenu().add(Menu.NONE, kMenuIdTransfer, 3, "Transfer");
        m.setEnabled(isConnected);

        m = popup.getMenu().add(Menu.NONE, kMenuIdPlay, 4, call_.isFilePlaying() ? "Stop play":"Play file");
        m.setEnabled(isConnected);

        m = popup.getMenu().add(Menu.NONE, kMenuIdRecord, 5, call_.isRecStarted() ? "Stop recording": "Record");
        m.setEnabled(isConnected);

        m = popup.getMenu().add(Menu.NONE, kMenuIdSwitchCamera, 6, "Switch camera");
        m.setEnabled(call_.hasVideo());

        popupMenu_ = popup;
        popup.setOnMenuItemClickListener(this::onExtraMenuSelectItemClick);
        popup.show();
    }

    boolean onExtraMenuSelectItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case kMenuIdConf:           onToggleConferenceClick(); break;
            case kMenuIdTransferAtt:    onToggleTransferAttPanelClick(null); break;
            case kMenuIdTransfer:       onToggleTransferPanelClick(null); break;
            case kMenuIdPlay:           onTogglePlayFileClick(); break;
            case kMenuIdRecord:         onToggleRecordFileClick(); break;
            case kMenuIdSwitchCamera:   onSwitchCameraClick(); break;
        }
        popupMenu_ = null;
        return true;
    }

    void resetUiModes() {
        uiModes_.put(CallModel.CallState.Ringing,   UiMode.eUndef);
        uiModes_.put(CallModel.CallState.Connected, UiMode.eMain);
        uiModes_.put(CallModel.CallState.Held,      UiMode.eMain);
    }

    UiMode getUiMode(CallModel.CallState state) {
        UiMode mode = uiModes_.get(state);
        return (mode==null) ? UiMode.eUndef : mode;
    }

    void setUiMode(CallModel.CallState state, UiMode mode) {
        if ((state == CallModel.CallState.Ringing) && (mode == UiMode.eMain))
            mode = UiMode.eUndef;

        uiModes_.put(state, mode);
    }

    public void onResume() {        
        super.onResume();
    }
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unsubscribeFromCurrentCall();

        previewRenderer_.release();
        remoteRenderer_.release();
    }
}
