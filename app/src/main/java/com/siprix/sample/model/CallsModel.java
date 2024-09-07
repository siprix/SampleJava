package com.siprix.sample.model;

import com.siprix.SiprixCore;
import com.siprix.SiprixVideoRenderer;

import java.util.ArrayList;
import java.util.List;


public class CallsModel {
    final private List<CallModel> callItems_ = new ArrayList<>();
    final private ObjModel parent_;
    private ModelObserver observer_;

    private SiprixVideoRenderer renderer_;

    public static final int kLocalVideoCallId=0;
    public static final int kEmptyCallId=0;
    int switchedCallId_ =  kEmptyCallId;
    int lastIncomingCallId_ = kEmptyCallId;
    boolean confModeStarted_ = false;

    protected CallsModel(ObjModel parent) {
        parent_ = parent;
    }

    public CallModel get(int index) {   return callItems_.get(index);  }
    public boolean isEmpty() { return callItems_.isEmpty(); }
    public int size() { return callItems_.size(); }

    public int getSwitchedCallId() { return switchedCallId_; }
    public int getLastIncomingCallId() { return lastIncomingCallId_; }

    public CallModel getSwitchedCall() {
        return findCall(switchedCallId_);
    }
    public boolean isSwitchedCall(int callId) { return switchedCallId_==callId; }
    public boolean isSwitchedCall(CallModel m) { return switchedCallId_==m.getCallId(); }
    public boolean isConfModeStarted() { return confModeStarted_; }
    public boolean isNetworkLost() { return parent_.netState_.isNetworkLost(); }


    private int findCallIdx(int callId) {
        for (int i = 0; i < callItems_.size(); i++) {
            if(callItems_.get(i).getCallId() == callId)
                return i;
        }
        return -1;
    }

    private CallModel findCall(int callId) {
        for (int i = 0; i < callItems_.size(); i++) {
            if(callItems_.get(i).getCallId() == callId)
                return callItems_.get(i);
        }
        return null;
    }

    protected void calcDuration() {
        for (CallModel c : callItems_)
            c.calcDuration();
    }

    public void invite(DestModel destModel) throws Exception {
        parent_.log("Trying to invite "+destModel.getToExt()+" from account:"+destModel.getSrcAccId());
        //Invite
        SiprixCore.IdOutArg callIdArg = new SiprixCore.IdOutArg();
        int err = parent_.core_.callInvite(destModel.getData(), callIdArg);
        if(err != SiprixCore.kOK)
            throw new Exception(parent_.getErrText(err));

        //Add model
        String accUri = parent_.accounts_.getUri(destModel.getSrcAccId());
        boolean hasSecureMedia = parent_.accounts_.hasSecureMedia(destModel.getSrcAccId());
        CallModel newCall = new CallModel(parent_, callIdArg.value, accUri, destModel.getToExt(),
                            false, hasSecureMedia, destModel.getWithVideo());
        callItems_.add(newCall);

        if(parent_.cdrs_ != null)
            parent_.cdrs_.add(newCall);//Add cdr

        parent_.postResolveContactName(newCall);//Resolve contact name

        //Notify
        notifyListeners();
    }

    public void switchToCall(int callId) throws Exception {
        parent_.log("Switching mixer to call "+callId);
        int err = parent_.core_.mixerSwitchToCall(callId);
        if(err != SiprixCore.kOK)
            throw new Exception(parent_.getErrText(err));

        notifyListeners();
    }

    public void makeConference() throws Exception {
        if(confModeStarted_){
            parent_.log("Ending conference, switch mixer to call: "+switchedCallId_);
            parent_.core_.mixerSwitchToCall(switchedCallId_);
            confModeStarted_ = false;
        }
        else {
            parent_.log("Joining all calls to conference");
            int err = parent_.core_.mixerMakeConference();
            if (err != SiprixCore.kOK)
                throw new Exception(parent_.getErrText(err));
        }
    }

    public void setPreviewVideoRenderer(SiprixVideoRenderer r) {
        renderer_ = r;
        parent_.core_.callSetVideoRenderer(kLocalVideoCallId, r);
    }

    //Events handlers
    public void onProceeding(int callId, String response) {
        CallModel callModel = findCall(callId);
        if(callModel != null) callModel.onProceeding(response);

        notifyListeners();
    }

    public void onIncoming(int callId, int accId, boolean withVideo, String hdrFrom, String hdrTo) {
        String accUri = parent_.accounts_.getUri(accId);
        boolean hasSecureMedia = parent_.accounts_.hasSecureMedia(accId);
        CallModel newCall = new CallModel(parent_, callId, accUri, parseExt(hdrFrom),
                                    true, hasSecureMedia, withVideo);
        newCall.setDisplName(parseDisplayName(hdrFrom));
        callItems_.add(newCall);

        if(parent_.cdrs_ != null)
            parent_.cdrs_.add(newCall);//Add cdr

        parent_.postResolveContactName(newCall);//Resolve contact name
        
        lastIncomingCallId_ = callId;
        
        notifyListeners();
    }

    public void onConnected(int callId, String from, String to, boolean withVideo) {
        if(parent_.cdrs_!=null)
            parent_.cdrs_.setConnected(callId, from, to, withVideo);

        CallModel callModel = findCall(callId);
        if(callModel != null) callModel.onConnected(from, to, withVideo);

        notifyListeners();
    }

    public void onTerminated(int callId, int statusCode) {
        int index = findCallIdx(callId);
        if(index != -1) {
            if(parent_.cdrs_ != null)
                parent_.cdrs_.setTerminated(callId, statusCode, callItems_.get(index).getDurationStr());

            callItems_.get(index).onTerminated();
            callItems_.remove(index);
        }

        if (callItems_.isEmpty() && (renderer_ != null)) {
            renderer_.release();
            renderer_ = null;
        }

        notifyListeners();
    }

    public void onDtmfReceived(int callId, int tone) {
        CallModel callModel = findCall(callId);
        if(callModel != null) callModel.onDtmfReceived(tone);

        notifyListeners();
    }

    public void onCallTransferred(int callId, int statusCode) {
        CallModel callModel = findCall(callId);
        if(callModel != null) callModel.onCallTransferred(statusCode);

        notifyListeners();
    }

    public void onCallHeld(int callId, SiprixCore.HoldState holdState) {
        CallModel callModel = findCall(callId);
        if(callModel != null) callModel.onCallHeld(holdState);

        notifyListeners();
    }

    public void onSwitched(int callId) {
        switchedCallId_ = callId;
        notifyListeners();
    }

    public void onCallRedirected(int origCallId, int relatedCallId, String referTo) {
        //Find 'origCallId'
        CallModel origCall = findCall(origCallId);
        if(origCall == null) return;

        //Clone 'origCallId' and add to collection of calls as related one
        CallModel relatedCall = new CallModel(parent_, relatedCallId, origCall.getAccUri(), parseExt(referTo),
                                false, origCall.hasSecureMedia(), origCall.hasVideo());
        callItems_.add(relatedCall);

        notifyListeners();
    }

    public void onNetworkState(String name, SiprixCore.NetworkState state) {
        notifyListeners();
    }

    public void onPlayerState(int playerId, SiprixCore.PlayerState playerState) {
    }

    String parseExt(String uri) {
        //uri format: "displName" <sip:ext@domain:port> //=>returns 'ext'
        final int startIndex = uri.indexOf(':');
        if(startIndex == -1) return "";

        final int endIndex = uri.indexOf('@', startIndex + 1);
        return (endIndex == -1) ? "" : uri.substring(startIndex+1, endIndex);
    }

    String parseDisplayName(String uri) {
        //uri format: "displName" <sip:ext@domain:port> //=>returns 'displName'
        final int startIndex = uri.indexOf('"');
        if(startIndex == -1) return "";

        final int endIndex = uri.indexOf('"', startIndex + 1);
        return (endIndex == -1) ? "" : uri.substring(startIndex+1, endIndex);
    }

    public void setObserver(ModelObserver observer) {
        observer_ = observer;
    }

    public void notifyListeners() {
        if(observer_!=null)
           observer_.onModelChanged();
    }
}
