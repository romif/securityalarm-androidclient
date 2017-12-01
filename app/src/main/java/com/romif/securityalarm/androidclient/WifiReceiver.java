package com.romif.securityalarm.androidclient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WifiReceiver extends BroadcastReceiver implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "WifiReceiver";

    private GoogleApiClient mGoogleApiClient;
    private Properties properties = new Properties();

    public static void scheduleJob(Context context, Properties properties, Credential credential) {
        String[] jobParams = new String[]{properties.getProperty("security.oauth2.client.access-token-uri"), properties.getProperty("security.oauth2.client.client-id"), properties.getProperty("security.oauth2.client.client-secret"), credential.getId(), credential.getPassword(), properties.getProperty("securityalarm.client.url")};

        ComponentName serviceComponent = new ComponentName(context, PingDeviceService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        //builder.setMinimumLatency(5 * 1000); // wait at least
        //builder.setOverrideDeadline(20 * 1000); // maximum delay
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPeriodic(20 * 1000);
        //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putStringArray("jobParams", jobParams);
        builder.setExtras(bundle);
        jobScheduler.schedule(builder.build());

        AlarmNotification.notify(context, AlarmState.PAUSED, 333);
    }

    public static void cancelJob(Context context) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.cancel(0);
        AlarmNotification.notify(context, AlarmState.RESUMED, 333);
    }

    public static boolean isConnectedToDevice(final Context context, String ssid) {
        WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr != null && wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            return ssid.equals(wifiInfo.getSSID()); // Connected to an access point
        } else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info != null && info.isConnected()) {

                Properties properties = new Properties();
                properties.load(context.getAssets().open("application.properties"));

                if (!isConnectedToDevice(context, properties.getProperty("securityalarm.client.ssid"))) {
                    return;
                }

                if (mGoogleApiClient == null) {
                    mGoogleApiClient = new GoogleApiClient.Builder(context)
                            .addApi(Auth.CREDENTIALS_API)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .build();
                }

                CredentialRequest request = new CredentialRequest.Builder()
                        .setSupportsPasswordLogin(true)
                        .build();

                mGoogleApiClient.connect();

                Auth.CredentialsApi.request(mGoogleApiClient, request).setResultCallback((credentialRequestResult) -> {
                            Status status = credentialRequestResult.getStatus();
                            if (credentialRequestResult.getStatus().isSuccess()) {

                                Credential credential = credentialRequestResult.getCredential();
                                Log.d(TAG, "User: " + credential.getId());
                                scheduleJob(context, properties, credential);

                            } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                                AlarmNotification.notify(context, AlarmState.INCORRECT_CREDENTIALS, 333);
                                Log.d(TAG, "Sign in required");

                            } else {
                                AlarmNotification.notify(context, AlarmState.INCORRECT_CREDENTIALS, 333);
                                Log.w(TAG, "Unrecognized status code: " + status.getStatusCode());
                            }
                        }, 30, TimeUnit.SECONDS
                );


            } else if (info != null && !info.isConnected()) {
                cancelJob(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "GoogleApiClient is suspended with cause code: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient failed to connect: " + connectionResult);
    }
}
