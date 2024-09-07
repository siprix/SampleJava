package com.siprix.sample;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.google.android.material.tabs.TabLayout;

import com.siprix.sample.fragment.AccountsFragment;
import com.siprix.sample.fragment.BaseFragment;
import com.siprix.sample.fragment.CallsFragment;
import com.siprix.sample.fragment.LogsFragment;
import com.siprix.sample.model.ObjModel;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ObjModel objModel_;
    private CallNotifService bgService_;

    private TextView netLostIndicator_;
    private TabLayout tabLayout_;

    private final int kTabAcc=0;
    private final int kTabCalls=1;
    private final int kTabLog=2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        netLostIndicator_ = findViewById(R.id.network_lost_indicator);
        Log.d(TAG, "onCreate");

        configureTabs();
        setWindowFlags();
        requestsPermissions();

        startAndBindBgService();
    }

    void configureTabs() {
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        viewPager.setUserInputEnabled(false);
        viewPager.setAdapter(new FragmentStateAdapter(getSupportFragmentManager(), getLifecycle()) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case kTabAcc:   return new AccountsFragment();
                    case kTabCalls: return new CallsFragment();
                    default:        return new LogsFragment();
                }
            }
            @Override
            public int getItemCount() { return 3; }
        });

        tabLayout_ = findViewById(R.id.tab_layout);
        tabLayout_.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    void selectTabCalls() {
        TabLayout.Tab tab = tabLayout_.getTabAt(kTabCalls);
        if(tab != null) tab.select();
    }

    void startAndBindBgService() {
        Intent srvIntent = new Intent(this, CallNotifService.class);
        srvIntent.setAction(CallNotifService.kActionAppStarted);
        startService(srvIntent);

        bindService(new Intent(this, CallNotifService.class),
                serviceConnection_, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection_ = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            CallNotifService.LocalBinder binder = (CallNotifService.LocalBinder) service;
            bgService_ = binder.getService();

            //Handle case when activity was started by tap on notification or its buttons
            bgService_.handleIncomingCallIntent(getIntent());

            //Get initialized model instance and set it to fragments
            objModel_ = bgService_.getObjModel();
            setModelToFragments();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "onServiceDisconnected");
            bgService_ = null;
        }
    };

    void setModelToFragments() {
        objModel_.netState_.setObserver(this::onModelChanged);
        for(Fragment fr : getSupportFragmentManager().getFragments()) {
            if (fr instanceof BaseFragment)
                ((BaseFragment)fr).onModelCreated(objModel_);
        }
    }

    public ObjModel getObjModel() {
        return objModel_;
    }

    public void onModelChanged() {
        //Track network changes
        int vis = objModel_.netState_.isNetworkLost() ? View.VISIBLE :  View.GONE;
        netLostIndicator_.setVisibility(vis);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (objModel_ != null) {
            objModel_.resetObservers();
            objModel_ = null;
        }

        if(bgService_ != null) {
            unbindService(serviceConnection_);
            bgService_ = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        selectTabCalls();
        if(bgService_!=null)
            bgService_.handleIncomingCallIntent(intent);
    }

    public void toggleForegroundMode() {
        if(bgService_!=null)
            bgService_.toggleForegroundMode();
    }

    public boolean isForegroundMode() {
        return bgService_ != null && bgService_.isForegroundMode();
    }

    public String getPathToPlayMp3() {
        return writeAssetAndGetFilePath("music.mp3", R.raw.music);
    }


    ActivityResultLauncher<String[]> mAskPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::onPermissionsResult);

    private void requestsPermissions() {
        String[] permissions = (Build.VERSION.SDK_INT >= 34)
                ? new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}
                : new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        mAskPermissionLauncher.launch(permissions);
    }

    void onPermissionsResult(Map<String, Boolean> permissions) {
        boolean firstRun = isRunningFirstTime();
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            if (entry.getValue()) continue;//granted
            String permission = entry.getKey();

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                displayPermissionAlert(permission, false);
            } else if(firstRun) {
                requestPermissionAgain(permission, false);
            }else {
                displayPermissionAlert(permission, true);
            }
        }
    }

    boolean isRunningFirstTime() {
        SharedPreferences pref = getSharedPreferences(TAG, Context.MODE_PRIVATE);
        boolean firstRun = pref.getBoolean("firstRun", true);
        if (firstRun)  pref.edit().putBoolean("firstRun", false).apply();
        return firstRun;
    }

    public void displayPermissionAlert(String permission, boolean openAppDetailsActivity) {
        if(openAppDetailsActivity && (permission==Manifest.permission.CAMERA)) return;
        String message;
        switch (permission) {
            case Manifest.permission.CAMERA: message = "Permission 'Camera' is required for video calls."; break;
            case Manifest.permission.RECORD_AUDIO: message = "Permission 'Record audio' is required to access microphone.\nApplication can't make calls without it."; break;
            case Manifest.permission.POST_NOTIFICATIONS: message = "Permission 'Notifications' is required for displaying incoming call notifications when app is in background"; break;
            default: message = permission + " is required [?]";//shouldn't happen
        }

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Permission required")
                .setMessage(message)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .setPositiveButton(openAppDetailsActivity ? "Go to settings" : "Allow",
                        (dialog, which) -> requestPermissionAgain(permission, openAppDetailsActivity))
                .show();
    }

    void requestPermissionAgain(String permission, boolean openAppDetailsActivity) {
        if(openAppDetailsActivity) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        }
        else {
            mAskPermissionLauncher.launch(new String[]{ permission });
        }
    }

    void setWindowFlags() {
        if (Build.VERSION.SDK_INT < 27) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        } else {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
        }
    }


    private String writeAssetAndGetFilePath(String assetFile, int rawResourceId) {
        try{
            File f = new File(getFilesDir()+"/"+assetFile);
            if(!f.exists()) {
                Resources res = getResources();
                InputStream inputStream = res.openRawResource(rawResourceId);

                FileOutputStream outputStream = new FileOutputStream(f);
                byte buffer[] = new byte[1024];
                int length = 0;

                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();
            }
            return f.getAbsolutePath();
        }catch (IOException e) {
            //Logging exception
        }
        return "";
    }

}//MainActivity