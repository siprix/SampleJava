package com.siprix.sample.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.siprix.AccData;
import com.siprix.IniData;
import com.siprix.SubscrData;
import com.siprix.VideoData;
import com.siprix.SiprixCore;
import com.siprix.ISiprixModelListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ObjModel  {
    protected final SiprixCore core_;
    public final AccountsModel accounts_;
    public final CallsModel calls_;
    public final CdrsModel cdrs_;
    public final DevicesModel devices_;
    public final NetworkStateModel netState_;
    private final Context context_;
    private final Handler mainHandler_;
    private final StringBuilder logsStrBuilder_;
    private ModelObserver observer_;
    private String fcmPushToken_="";
    private String fcmProjectId_="";

    final String kObjModel = "kObjModel";
    final String kAccounts = "kAccounts";
    private static final boolean kFcmPushNotifEnabled = false;

    public ObjModel(Context context) {
        context_ = context;

        core_ = new SiprixCore(context_);
        core_.setModelListener(new SiprixModelListener(this));

        cdrs_    = new CdrsModel();//set to null if not required
        accounts_= new AccountsModel(this);
        calls_   = new CallsModel(this);
        devices_ = new DevicesModel(this);
        netState_ = new NetworkStateModel(this);

        mainHandler_ = new Handler(context.getMainLooper());
        logsStrBuilder_ = new StringBuilder();
    }

    public int initializeCore() {
        if (core_.isInitialized()) return SiprixCore.kOK;

        IniData ini = new IniData();
        ini.setLicense("...license-credentials...");
        ini.setLogLevelFile(IniData.LogLevel.NONE);
        ini.setLogLevelIde(IniData.LogLevel.DEBUG);
        ini.setTlsVerifyServer(false);
        ini.setUseExternalRinger(true);
        ini.setUseProximity(true);

        //Initialize
        int err = core_.initialize(ini);
        if(err==SiprixCore.kOK) {
            log("Siprix core initialized successfully");
            log("Version: "+core_.getVersion());
        }
        else {
            log("Can't initialize Siprix core Err: "+err);
        }
        return err;
    }

    public void unInitialize() {
        core_.setServiceListener(null);
        core_.setModelListener(null);
        core_.unInitialize();
    }

    public SiprixCore getCore() { return core_; }

    public void setNoCameraImg(String noCameraImgPath) {
        VideoData vdoData = new VideoData();
        vdoData.setNoCameraImgPath(noCameraImgPath);
        core_.dvcSetVideoParams(vdoData);
    }

    public void refreshRegistration() {
        Log.i(kObjModel, "RefreshRegistration");
        accounts_.refreshRegistration();
    }

    public void getFcmTokenAndRestoreAccounts() {
        if(kFcmPushNotifEnabled) {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    fcmProjectId_ = FirebaseApp.getInstance().getOptions().getProjectId();
                    fcmPushToken_ = task.getResult();
                    Log.e(kObjModel, "Fetch FCM token success: " + fcmPushToken_);
                } else {
                    fcmProjectId_ = "";
                    fcmPushToken_ = "";
                    Log.e(kObjModel, "Fetch FCM token failed: ", task.getException());
                }
                restoreAccounts();
            });
        } else {
            restoreAccounts();
        }
    }

    void restoreAccounts() {
        String jsonStr = getSharedPref().getString(kAccounts, "");
        accounts_.loadFromJson(jsonStr);
    }

    void postSaveAccounts() {
        mainHandler_.post(() -> {
            getSharedPref().edit()
                .putString(kAccounts, accounts_.storeToJson())
                .apply();
        });
    }

    void postResolveContactName(CallModel newCall) {
        mainHandler_.post(() -> {
            //TODO Add here own code which matches 'extension'/'number' with contact name.
            //Example
            if(newCall.getRemoteExt().equals("1012"))
                newCall.notifyDisplName("MyFriend-From1012");
        });
    }

    SharedPreferences getSharedPref() {
        return context_.getSharedPreferences(kObjModel, Context.MODE_PRIVATE);
    }

    String getErrText(int err) {
        return core_.getErrText(err);
    }

    public void log(String msg) {
        Log.d(kObjModel, msg);

        Date d = new Date();
        logsStrBuilder_.append(DateFormat.format("HH:mm:ss ", d.getTime()));
        logsStrBuilder_.append(msg);
        logsStrBuilder_.append("\n");

        if(observer_ != null) {
            mainHandler_.post(() -> observer_.onModelChanged());
        }
    }

    public void resetObservers() {
        accounts_.setObserver(null);
        netState_.setObserver(null);
        calls_.setObserver(null);
        devices_.setObserver(null);
        this.setObserver(null);
    }

    public void setObserver(ModelObserver observer) {
        observer_ = observer;
    }

    public String getLogsText() {
        return logsStrBuilder_.toString();
    }

    String getFcmPushToken() { return fcmPushToken_; }
    String getFcmProjectId() { return fcmProjectId_; }

    protected void startCallDurationTimer() {
        mainHandler_.postDelayed(mTickRunnable, 1000);
    }

    private final Runnable mTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!calls_.isEmpty()) {
                calls_.calcDuration();
                mainHandler_.postDelayed(mTickRunnable, 1000);
            }
        }
    };

    static private class SiprixModelListener implements ISiprixModelListener {
        final ObjModel parent_;

        public SiprixModelListener(ObjModel parent) {
            parent_ = parent;
        }

        @Override
        public void onTrialModeNotified() {
            parent_.log("--- SIPRIX SDK works in TRIAL mode ---");
        }

        @Override
        public void onDevicesAudioChanged() {
            parent_.devices_.onDevicesAudioChanged();
        }

        @Override
        public void onNetworkState(String name, SiprixCore.NetworkState state) {
            parent_.netState_.onNetworkState(name, state);
            parent_.accounts_.onNetworkState(name, state);
            parent_.calls_.onNetworkState(name, state);
            parent_.log(String.format(Locale.getDefault(),
                    "onNetworkChanged name:%s state:%d", name, state.getValue()));
        }

        @Override
        public void onAccountRegState(int accId, AccData.RegState regState, String response) {
            parent_.accounts_.onRegStateChanged(accId, regState, response);
            parent_.log(String.format(Locale.getDefault(),
                    "onRegStateChanged accId:%d state:%d response:%s",
                    accId, regState.getValue(), response));
        }

        @Override
        public void onPlayerState(int playerId, SiprixCore.PlayerState playerState) {
            parent_.calls_.onPlayerState(playerId, playerState);
            parent_.log(String.format(Locale.getDefault(),
                    "onPlayerState playerId:%s playerState:%d", playerId, playerState.getValue()));
        }

        @Override
        public void onCallProceeding(int callId, String response) {
            parent_.calls_.onProceeding(callId, response);
            parent_.log(String.format(Locale.getDefault(),
                    "onCallProceeding callId:%s response:%s", callId, response));
        }

        public void onCallTerminated(int callId, int statusCode) {
            parent_.calls_.onTerminated(callId, statusCode);
            parent_.log(String.format(Locale.getDefault(),
                    "onTerminated callId:%d statusCode:%d", callId, statusCode));
        }

        @Override
        public void onCallConnected(int callId, String hdrFrom, String hdrTo, boolean withVideo) {
            parent_.calls_.onConnected(callId, hdrFrom, hdrTo, withVideo);

            if(parent_.calls_.size()==1) parent_.startCallDurationTimer();//start timer when first call connected

            parent_.log(String.format(Locale.getDefault(),
                    "onConnected callId:%d from:%s to:%s", callId, hdrFrom, hdrTo));
        }

        @Override
        public void onCallIncoming(int callId, int accId, boolean withVideo,
                                   String hdrFrom, String hdrTo) {
            parent_.calls_.onIncoming(callId, accId, withVideo, hdrFrom, hdrTo);
            parent_.log(String.format(Locale.getDefault(),
                    "onIncoming callId:%d from:%s to:%s", callId, hdrFrom, hdrTo));
        }

        @Override
        public void onCallDtmfReceived(int callId, int tone) {
            parent_.calls_.onDtmfReceived(callId, tone);
            parent_.log(String.format(Locale.getDefault(),
                    "onDtmfReceived callId:%d tone:%d", callId, tone));
        }

        @Override
        public void onCallTransferred(int callId, int statusCode) {
            parent_.calls_.onCallTransferred(callId, statusCode);
            parent_.log(String.format(Locale.getDefault(),
                    "onCallTransferred callId:%d statusCode:%d",
                    callId, statusCode));
        }

        @Override
        public void onCallRedirected(int origCallId, int relatedCallId, String referTo) {
            parent_.calls_.onCallRedirected(origCallId, relatedCallId, referTo);
            parent_.log(String.format(Locale.getDefault(),
                    "onCallRedirected origCallId:%d relatedCallId:%d referTo:%s",
                    origCallId, relatedCallId, referTo));
        }

        @Override
        public void onCallHeld(int callId, SiprixCore.HoldState holdState) {
            parent_.calls_.onCallHeld(callId, holdState);
            parent_.log(String.format(Locale.getDefault(),
                    "onCallTransferred onCallHeld:%d holdState:%d",
                    callId, holdState.getValue()));
        }

        public void onCallSwitched(int callId) {
            parent_.calls_.onSwitched(callId);
            parent_.log(String.format(Locale.getDefault(),
                    "onSwitched callId:%d", callId));
        }

        @Override
        public void onSubscriptionState(int subscriptionId, SubscrData.SubscrState state, String response) {
            //Handle subscription state
        }

        @Override
        public void onMessageSentState(int messageId, boolean success, String response) {
            //Handle message sent status
        }

        @Override
        public void onMessageIncoming(int accId, String hdrFrom, String body) {
            //Handle incoming message request
        }

    }//SiprixModelListener

}
