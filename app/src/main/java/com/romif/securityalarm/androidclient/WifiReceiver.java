package com.romif.securityalarm.androidclient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
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
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WifiReceiver extends BroadcastReceiver implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "WifiReceiver";

    private GoogleApiClient mGoogleApiClient;



    public static boolean isConnectedToDevice(final Context context, String ssid) {
        WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr != null && wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            return ssid.equals(wifiInfo.getSSID().replaceAll("\"", "")); // Connected to an access point
        } else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    public static String getMacAddress(Context context) {

        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder stringBuilder = new StringBuilder();
                for (byte b : macBytes) {
                    stringBuilder.append(String.format("%02X:",b));
                }

                if (stringBuilder.length() > 0) {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                return stringBuilder.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }


    @Override
    public void onReceive(final Context context, Intent intent) {
        /*try {
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
                                PingDeviceService.scheduleJob(context, properties, credential);

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
                PingDeviceService.cancelJob(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }*/
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
