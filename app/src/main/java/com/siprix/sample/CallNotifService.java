package com.siprix.sample;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessaging;

import com.siprix.AccData;
import com.siprix.SiprixCore;
import com.siprix.SiprixRinger;
import com.siprix.ISiprixRinger;
import com.siprix.ISiprixModelListener;
import com.siprix.ISiprixServiceListener;

import com.siprix.sample.model.ObjModel;

import java.util.List;
import java.util.Arrays;

/**
 *  CallNotifService
 *  - Aggregates object model.
 *  - Continues working when UI destroyed/recreating.
 *  - Displays notifications/starts activity when incoming call received.
 */

public class CallNotifService extends Service {
    private static final String TAG = "CallNotifService";
    private static final String kMsgChannelId  = "kSiprixMsgChannelId";
    public static final String kCallChannelId = "kSiprixCallChannelId";

    public static final String kActionAppStarted = "kActionAppStarted";
    public static final String kActionPushNotif = "kActionPushNotif";
    public static final String kActionForeground = "kActionForeground";
    public static final String kActionIncomingCall = "kActionIncomingCall";
    public static final String kActionIncomingCallAccept = "kActionIncomingCallAccept";
    public static final String kActionIncomingCallReject = "kActionIncomingCallReject";
    public static final String kIntentExtraCallId = "kIntentExtraCallId";

    final private int kCallBaseNotifId = 555;
    final private int kForegroundId = 777;

    //Siprix object model
    private ObjModel objModel_;

    private ISiprixRinger ringer_;
    protected PowerManager.WakeLock wakeLock_;
    private final IBinder binder_ = new LocalBinder();
    private boolean isForeground_ = false;

    public class LocalBinder extends Binder {
        CallNotifService getService() {
            // Return this instance of LocalService so clients can call public methods.
            return CallNotifService.this;
        }
    }

    public ObjModel getObjModel() { return objModel_; }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        ringer_ = new SiprixRinger(this);

        createNotifChannel();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopForegroundMode();
        getNotifMgr().cancelAll();

        ringer_ = null;

        if(objModel_ != null) {
            objModel_.unInitialize();
            objModel_ = null;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder_;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        int result = super.onStartCommand(intent, flags, startId);
        String action = (intent!=null) ? intent.getAction() : null;

        if (kActionIncomingCallReject.equals(action))
            handleIncomingCallIntent(intent);

        if (kActionPushNotif.equals(action)||
            kActionAppStarted.equals(action))
            initModelRestoreAccounts();

        return result;
    }

    public void handleIncomingCallIntent(Intent intent) {
        Bundle args = intent.getExtras();
        final int callId = (args==null) ? SiprixCore.kEmptyCallId : args.getInt(kIntentExtraCallId);
        if(callId <= SiprixCore.kEmptyCallId) return;

        if(kActionIncomingCallAccept.equals(intent.getAction())) {
            objModel_.getCore().callAccept(callId, true);
        }else if(kActionIncomingCallReject.equals(intent.getAction())) {
            objModel_.getCore().callReject(callId);
        }
        cancelNotification(callId);
    }

    void initModelRestoreAccounts() {
        if(objModel_ == null) {
            //App just started by user/push:
            // - create/initialize core/objModel
            // - restore accounts and update registration
            // - don't display any notifications, wait on  INVITE from PBX
            objModel_ = new ObjModel(getApplicationContext());
            objModel_.initializeCore();
            objModel_.getCore().setServiceListener(new SiprixServiceListener(this));

            //objModel_.setNoCameraImg(writeAssetAndGetFilePath("no_camera.jpg", R.raw.no_camera));
            objModel_.getFcmTokenAndRestoreAccounts();

        } else  if (!isAppInForeground() && !isForegroundMode()) {
            //Update registration only when there is no foreground activity nor service
            objModel_.refreshRegistration();
        }
    }

    void createNotifChannel() {
        String appName = getString(R.string.app_name);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //NotificationChannel msgChannel = new NotificationChannel(kMsgChannelId,
            //        appName, NotificationManager.IMPORTANCE_DEFAULT);
            //msgChannel.enableLights(true);
            //notifMgr_.createNotificationChannel(msgChannel);

            NotificationChannel callChannel = new NotificationChannel(kCallChannelId,
                    appName, NotificationManager.IMPORTANCE_HIGH);
            callChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            callChannel.setDescription("Incoming calls notifications channel");//"getString(R.string.calls_channel_descr));
            callChannel.setVibrationPattern(new long[]{ 0, 100 });
            //callChannel.enableLights(true);
            getNotifMgr().createNotificationChannel(callChannel);
        }
    }

    PendingIntent getIntentActivity(String action, int callId) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.putExtra(kIntentExtraCallId, callId);
        activityIntent.setAction(action);

        return PendingIntent.getActivity(this, 1,
                activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    PendingIntent getIntentService(String action, int callId) {
        Intent srvIntent = new Intent(action);
        srvIntent.setClassName(this, CallNotifService.class.getName());
        srvIntent.putExtra(kIntentExtraCallId, callId);

        return PendingIntent.getService(this, 1,
                srvIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    void cancelNotification(int callId) {
        getNotifMgr().cancel(kCallBaseNotifId + callId);
    }

    NotificationManager getNotifMgr() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    void displayIncomingCallNotification(int callId, int accId, 
                                          boolean withVideo, String hdrFrom, String hdrTo) {
        PendingIntent contentIntent = getIntentActivity(kActionIncomingCall, callId);
        PendingIntent pendingAcceptCall = getIntentActivity(kActionIncomingCallAccept, callId);
        PendingIntent pendingRejectCall = getIntentService(kActionIncomingCallReject, callId);

        //Popup style
        String title = "Incoming call";//getString(R.string.calls_notif_title);
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(hdrFrom);
        bigTextStyle.setBigContentTitle(title);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, kCallChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(hdrFrom)
            .setAutoCancel(true)
            .setChannelId(kCallChannelId)
            .setDefaults(Notification.DEFAULT_ALL)
            .setCategory(Notification.CATEGORY_CALL)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .setOngoing(true)
            .setStyle(bigTextStyle)
            .addAction(0, "Reject call", pendingRejectCall)
            .addAction(0, "Accept call", pendingAcceptCall)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
            .setPriority(Notification.PRIORITY_MAX);

        getNotifMgr().notify(kCallBaseNotifId + callId, builder.build());

        ringer_.start();
    }

    public boolean isForegroundMode() {
        return isForeground_;
    }

    public void toggleForegroundMode() {
      if(isForeground_)  stopForegroundMode();
      else               startForegroundMode();
    }

    private void stopForegroundMode() {
        isForeground_ = false;
        releaseWakelock();

        if(Build.VERSION.SDK_INT >= 33) stopForeground(STOP_FOREGROUND_REMOVE);
        else                            stopForeground(true);
    }

    private void startForegroundMode() {
        isForeground_ = true;
        acquireWakelock();

        PendingIntent contentIntent = getIntentActivity(kActionForeground, 0);

        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, kCallChannelId);
        }else{
            builder = new Notification.Builder(this);
        }
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)//TODO set own icon
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Siprix call notification service")
                .setContentIntent(contentIntent)
                .build();// getNotification()

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            startForeground(kForegroundId, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
        }else{
            startForeground(kForegroundId, builder.build());
        }
    }

    public void acquireWakelock() {
        if(wakeLock_== null) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock_ = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Siprix:WakeLock.");
        }

        if ((wakeLock_ != null) && !wakeLock_.isHeld()) {
            wakeLock_.acquire();
        }
    }

    void releaseWakelock() {
        if((wakeLock_ != null) && wakeLock_.isHeld()) {
            wakeLock_.release();
        }
    }

    static private class SiprixServiceListener implements ISiprixServiceListener {
        CallNotifService service_;

        SiprixServiceListener(CallNotifService service) {
            service_ = service;
        }

        @Override
        public void onRingerState(boolean start) {
            if (start) service_.ringer_.start();
            else service_.ringer_.stop();
        }

        @Override
        public void onCallTerminated(int callId, int statusCode) {
            service_.cancelNotification(callId);
        }

        @Override
        public void onCallIncoming(int callId, int accId, boolean withVideo, String hdrFrom, String hdrTo) {
            if (!service_.isAppInForeground()) {
                service_.displayIncomingCallNotification(callId, accId, withVideo, hdrFrom, hdrTo);
            }else{
                Intent intent = new Intent(service_, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(kActionIncomingCall);
                service_.startActivity(intent);//switch UI (if required)
            }
        }
    }//SiprixServiceListener

    private boolean isAppInForeground() {
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> appProcs = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo app : appProcs) {
            if(app.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                boolean found = Arrays.asList(app.pkgList).contains(getPackageName());
                if(found) return true;
            }
        }
        return false;
    }
}
