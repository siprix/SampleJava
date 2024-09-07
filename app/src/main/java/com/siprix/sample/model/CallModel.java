package com.siprix.sample.model;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.siprix.SiprixCore;
import com.siprix.SiprixVideoRenderer;

import java.util.Date;
import java.util.Locale;

public class CallModel {
    private SiprixVideoRenderer renderer_;
    private ModelObserver observer_;
    private final ObjModel parent_;

    private CallState state_;
    private SiprixCore.HoldState holdState_;

    private final int myCallId_;
    private final String accUri_;
    private final String remoteExt_;
    private String receivedDtmf_="";
    private String nameAndExt_="";

    private final boolean isIncoming_;
    private final boolean hasSecureMedia_;
    private boolean withVideo_;
    private boolean isMicMuted_=false;
    private boolean isCamMuted_=false;
    private boolean isRecStarted_=false;
    private int playerId_=kEmptyPlayerId;
    private long durationSec_=0;

    protected CallModel(ObjModel parent, int callId, String accUri, String remoteExt,
              boolean isIncoming, boolean hasSecureMedia, boolean withVideo) {
        this.parent_ = parent;
        this.myCallId_ = callId;
        this.accUri_ = accUri;
        this.remoteExt_ = remoteExt;
        this.isIncoming_ = isIncoming;

        this.state_ = isIncoming ? CallState.Ringing : CallState.Dialing;
        this.holdState_ = SiprixCore.HoldState.NONE;

        this.withVideo_ = withVideo;
        this.hasSecureMedia_ = hasSecureMedia;
    }

    @NonNull
    public String toString()   { return getRemoteExt();   }

    public static final int kEmptyPlayerId=0;
    public int getCallId() { return myCallId_; }
    public String getAccUri() { return accUri_; }
    public String getRemoteExt() { return remoteExt_; }
    public String getNameAndExt() { return nameAndExt_.isEmpty() ? remoteExt_ : nameAndExt_; }
    public boolean getIsIncoming() { return isIncoming_; }
    public boolean hasVideo() { return withVideo_; }
    public boolean hasSecureMedia() { return hasSecureMedia_; }
    public CallState getState() { return state_; }
    public SiprixCore.HoldState getHoldState() { return holdState_; }
    public boolean isMicMuted() { return isMicMuted_; }
    public boolean isCamMuted() { return isCamMuted_; }
    public boolean isRecStarted() { return isRecStarted_; }
    public boolean isFilePlaying() { return playerId_ != kEmptyPlayerId; }
    public String getReceivedDtmf(){ return receivedDtmf_; }

    public void setDisplName(String s) {
        if(!s.isEmpty())  nameAndExt_ = s + "(" + remoteExt_+")";
    }

    public void notifyDisplName(String s) {
        setDisplName(s);
        notifyListeners();
    }

    void calcDuration() {
        if(state_ == CallState.Connected) {
            durationSec_ += 1;
            notifyListeners();
        }
    }

    public String getDurationStr() {
        return formatDuration(durationSec_);
    }

    static String formatDuration(long sec) {
        long h = 0, m = 0;
        if (sec >= 3600) { h = sec/3600; sec -= h*3600; }
        if (sec >= 60) { m = sec/60; sec -= m*60; }

        return (h > 0)
           ? String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
           : String.format(Locale.US, "%02d:%02d",  m, sec);
    }

    public void bye() throws Exception {
        parent_.log("Ending callId:"+myCallId_);
        int err = parent_.core_.callBye(myCallId_);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));
        
        state_ = CallState.Disconnecting;
        notifyListeners();
    }

    public void accept(boolean withVideo) throws Exception {
        parent_.log("Accepting callId:"+myCallId_);
        int err = parent_.core_.callAccept(myCallId_, withVideo);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));
            
        state_ = CallState.Accepting;
        notifyListeners();        
    }

    public void reject() throws Exception {
        parent_.log("Rejecting callId:"+myCallId_);
        int err = parent_.core_.callReject(myCallId_, 486);//Send '486 Busy now'
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));

        state_ = CallState.Rejecting;
        notifyListeners();
    }

    public void muteMic(boolean mute) throws Exception {
        parent_.log("Mute:"+mute+" mic of callId:"+myCallId_);
        int err = parent_.core_.callMuteMic(myCallId_, mute);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));

        isMicMuted_ = mute;
        notifyListeners();
    }

    public void muteCam(boolean mute) throws Exception {
        parent_.log("Mute:\"+mute+\" cam of callId:"+myCallId_);
        int err = parent_.core_.callMuteCam(myCallId_, mute);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));

        isCamMuted_ = mute;
        notifyListeners();
    }

    public void sendDtmf(String tone) throws Exception  {
        parent_.log("Sending dtmf callId:"+myCallId_ + " tone:"+tone);
        int err = parent_.core_.callSendDtmf(myCallId_, tone);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));
    }

    public void playFile(String pathToMp3File) throws Exception {
        if(TextUtils.isEmpty(pathToMp3File)) return;
        parent_.log("Starting play file callId:"+myCallId_+ " tone:"+pathToMp3File);
        SiprixCore.IdOutArg playerIdArg = new SiprixCore.IdOutArg();
        int err = parent_.core_.callPlayFile(myCallId_, pathToMp3File, false, playerIdArg);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));

        playerId_ = playerIdArg.value;
    }

    public void stopPlayFile() throws Exception {
        parent_.log("Stop play file callId:"+myCallId_+ " playerId:"+playerId_);
        if(playerId_==kEmptyPlayerId) return;
        int err = parent_.core_.callStopPlayFile(playerId_);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));
        playerId_ = kEmptyPlayerId;
    }

    public void recordFile(String pathToMp3File) throws Exception {
        parent_.log("Starting record callId:"+myCallId_+ " path:"+pathToMp3File);
        int err = parent_.core_.callRecordFile(myCallId_, pathToMp3File);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));
        isRecStarted_ = true;
    }

    public void stopRecordFile() throws Exception {
        parent_.log("Stop record file callId:"+myCallId_);

        if(!isRecStarted_) return;
        int err = parent_.core_.callStopRecordFile(myCallId_);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));
        isRecStarted_ = false;
    }

    public void hold() throws Exception {
        parent_.log("Hold callId:"+myCallId_);

        int err = parent_.core_.callHold(myCallId_);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));

        state_ = CallState.Holding;
        notifyListeners();
    }

    public void transferBlind(String toExt) throws Exception {
        parent_.log("Transfer blind callId:"+myCallId_ + " to ext:"+toExt);

        if(toExt.isEmpty()) return;
        int err = parent_.core_.callTransferBlind(myCallId_, toExt);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));

        state_ = CallState.Transferring;
        notifyListeners();
    }

    public void transferAttended(int toCallId) throws Exception {
        parent_.log("Transfer attended callId:"+myCallId_ + " to callId:"+toCallId);

        int err = parent_.core_.callTransferAttended(myCallId_, toCallId);
        if(err != SiprixCore.kOK) throw new Exception(parent_.getErrText(err));

        state_ = CallState.Transferring;
        notifyListeners();
    }

    public void setVideoRenderer(SiprixVideoRenderer r) {
        renderer_ = r;
        parent_.core_.callSetVideoRenderer(myCallId_, r);
    }

    //Event handlers
    void onProceeding(String resp) {
        state_ = CallState.Proceeding;
        //response_ = resp;
        notifyListeners();
    }

    void onConnected(String hdrFrom, String hdrTo, boolean withVideo) {
        state_ = CallState.Connected;
        withVideo_ = withVideo;
        durationSec_ = 0;
        notifyListeners();
    }

    void onDtmfReceived(int tone) {
        if(tone == 10) receivedDtmf_ += '*'; else
        if(tone == 11) receivedDtmf_ += '#';
        else           receivedDtmf_ += Integer.toString(tone);
        notifyListeners();
    }

    public void onCallTransferred(int statusCode) {
        state_ = CallState.Connected;
        notifyListeners();
    }

    public void onCallHeld(SiprixCore.HoldState holdState) {
        holdState_ = holdState;
        state_ = (holdState==SiprixCore.HoldState.NONE) ? CallState.Connected : CallState.Held;
        notifyListeners();
    }

    public void onTerminated() {
        if(renderer_ != null) {
            renderer_.release();
            renderer_ = null;
        }
    }

    public enum CallState {
        Dialing,        //Outgoing call just initiated after invoke 'call.invite'

        Proceeding,     //Outgoing call in progress, received 100Trying or 180Ringing

        Ringing,        //Incoming call just received
        Rejecting,      //Incoming call rejecting after invoke 'call.reject'
        Accepting,      //Incoming call accepting after invoke 'call.accept'

        Connected,      //Call successfully established, RTP is flowing

        Disconnecting,  //Call disconnecting after invoke 'call.bye'

        Holding,        //Call holding (renegotiating RTP stream states)
        Held,           //Call held, RTP is NOT flowing.
                        //   Use "holdState' to detect local or remote side put call on hold

        Transferring    //Call transferring after invoke 'call.transferBlind'/'transferAttended'
    };

    public void setObserver(ModelObserver observer) {
        observer_ = observer;
    }

    public void notifyListeners() {
        if(observer_!=null)
            observer_.onModelChanged();
    }
}
