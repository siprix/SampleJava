# SampleJava

Project contains ready to use SIP VoIP Client application for Android, written on Java, includes PushNotification implementation.
As SIP engine it uses Siprix SDK, included in binary form.

Application (Siprix) has ability to:

- Add multiple SIP accounts
- Send/receive multiple calls (Audio and Video)
- Manage calls with:
   - Hold
   - Mute microphone/camera
   - Play sound to call from mp3 file
   - Record received sound to file
   - Send/receive DTMF
   - Transfer
   - ...

Application's UI may not contain all the features, avialable in the SDK, they will be added later.

## Adding push notifications
To enable push notifications implementation make following steps:
1. Uncomment line `app\build.gradle.kts:4`

```
 id("com.google.gms.google-services")
```

2. Update file `app\google-services.json`
See more: [Add a Firebase configuration file](https://firebase.google.com/docs/android/setup#add-config-file)

3. Modify `app\src\main\java\com\siprix\sample\model\ObjModel.java:44`

```
    private static final boolean kFcmPushNotifEnabled = true;
```

4. Modify code, which adds push token to the REGISTER request

See method 'appendPushTokenToAccount' in `app\src\main\java\com\siprix\sample\model\AccountsModel.java:172`.

## Limitations

Siprix doesn't provide VoIP services, but in the same time doesn't have backend limitations and can connect to any SIP (Server) PBX or make direct calls between clients.
For testing app you need an account(s) credentials from a SIP service provider(s).
Some features may be not supported by all SIP providers.

Some features may be not supported by all SIP providers.

Attached Siprix SDK works in trial mode and has limited call duration - it drops call after 60sec.
Upgrading to a paid license removes this restriction, enabling calls of any length.

Please contact [sales@siprix-voip.com](mailto:sales@siprix-voip.com) for more details.

## More resources

Product web site: https://siprix-voip.com

Manual: https://docs.siprix-voip.com


## Screeshots

<a href="https://docs.siprix-voip.com/screenshots/SampleJava_Accounts.png"  title="Accounts screenshot">
<img src="https://docs.siprix-voip.com/screenshots/SampleJava_Accounts_Mini.png" width="50"></a>,<a href="https://docs.siprix-voip.com/screenshots/SampleJava_Calls.png"  title="Calls screenshot">
<img src="https://docs.siprix-voip.com/screenshots/SampleJava_Calls_Mini.png" width="50"></a>,<a href="https://docs.siprix-voip.com/screenshots/SampleJava_Logs.PNG"  title="Logs screenshot">
<img src="https://docs.siprix-voip.com/screenshots/SampleJava_Logs_Mini.png" width="50"></a>
