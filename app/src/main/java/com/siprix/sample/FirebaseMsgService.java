package com.siprix.sample;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * FirebaseMsgService
 * - Receives push messages and starts 'CallNotifService'
 */
public class FirebaseMsgService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "onMessageReceived from: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Intent srvIntent = new Intent(this, CallNotifService.class);
            srvIntent.setAction(CallNotifService.kActionPushNotif);
            for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
                srvIntent.putExtra(entry.getKey(), entry.getValue());
            }
            startService(srvIntent);
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }
}