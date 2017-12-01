package com.romif.securityalarm.androidclient;

import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final int DELAY_MILLIS = 3000;
    private static final String TAG = "MainActivity";
    private static final int RC_SAVE = 1;
    private static final int RC_READ = 3;
    private static final String IS_RESOLVING = "is_resolving";
    private static final String IS_REQUESTING = "is_requesting";
    private static final String SPLASH_TAG = "splash_fragment";
    private static final String SIGN_IN_TAG = "sign_in_fragment";
    // Add mGoogleApiClient and mIsResolving fields here.
    private boolean mIsResolving;
    private boolean mIsRequesting;
    private Handler mHandler;

    private GoogleApiClient mGoogleApiClient;
    private Properties properties = new Properties();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            properties.load(getBaseContext().getAssets().open("application.properties"));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        setFragment(getIntent());

        if (savedInstanceState != null) {
            mIsResolving = savedInstanceState.getBoolean(IS_RESOLVING);
            mIsRequesting = savedInstanceState.getBoolean(IS_REQUESTING);
        }

        // When not using Smart Lock show set Fragment in onCreate.
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setFragment(null);
            }
        }, DELAY_MILLIS);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current sign in state
        savedInstanceState.putBoolean(IS_RESOLVING, mIsResolving);
        savedInstanceState.putBoolean(IS_REQUESTING, mIsRequesting);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStop() {
        mHandler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    /**
     * Set the appropriate fragment given the state of the Activity and the Intent used to start it.
     * If the intent is a launcher intent the Splash Fragment is shown otherwise the SignIn Fragment is shown.
     *
     * @param intent Intent used to determine which Fragment is used.
     */
    private void setFragment(Intent intent) {
        Fragment fragment;
        String tag;
        if (intent != null && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            fragment = new SplashFragment();
            tag = SPLASH_TAG;
        } else {
            fragment = new SignInFragment();
            tag = SIGN_IN_TAG;
        }
        String currentTag = getCurrentFragmentTag();
        if (currentTag == null || !currentTag.equals(tag)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .commit();
        }
    }

    /**
     * Start the Content Activity and finish this one.
     */
    protected void goToContent() {
        startActivity(new Intent(this, ContentActivity.class));
        finish();
    }

    /**
     * If the currently displayed Fragment is the SignIn Fragment then enable or disable the sign in form.
     *
     * @param enable Enable form when true, disable form when false.
     */
    private void setSignInEnabled(boolean enable) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SIGN_IN_TAG);
        if (fragment != null && fragment.isVisible()) {
            ((SignInFragment) fragment).setSignEnabled(enable);
        }
    }

    /**
     * Get the tag of the currently set Fragment.
     *
     * @return Tag of currently set Fragment, or null if no fragment is set.
     */
    private String getCurrentFragmentTag() {
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        if (fragmentList == null || fragmentList.size() == 0) {
            return null;
        }
        Fragment fragment = fragmentList.get(0);
        if (fragment != null) {
            return fragment.getTag();
        }
        return null;
    }

    /**
     * Check if the Splash Fragment is the currently selected Fragment.
     *
     * @return true if Splash Fragment is the current Fragment, false otherwise.
     */
    private boolean onSplashFragment() {
        return getCurrentFragmentTag().equals(SPLASH_TAG);
    }

    protected boolean isResolving() {
        return mIsResolving;
    }

    protected boolean isRequesting() {
        return mIsRequesting;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        // Request Credentials once connected. If credentials are retrieved
        // the user will either be automatically signed in or will be
        // presented with credential options to be used by the application
        // for sign in.
        requestCredentials();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }

    protected void saveCredential(Credential credential) {
        // Credential is valid so save it.
        Auth.CredentialsApi.save(mGoogleApiClient,
                credential).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "Credential saved");

                    if (WifiReceiver.isConnectedToDevice(getBaseContext(), properties.getProperty("securityalarm.client.ssid"))) {
                        WifiReceiver.scheduleJob(getBaseContext(), properties, credential);
                    }

                    goToContent();
                } else {
                    Log.d(TAG, "Attempt to save credential failed " +
                            status.getStatusMessage() + " " +
                            status.getStatusCode());
                    resolveResult(status, RC_SAVE);
                }
            }
        });
    }

    private void resolveResult(Status status, int requestCode) {
        // We don't want to fire multiple resolutions at once since that
        // can   result in stacked dialogs after rotation or another
        // similar event.
        if (mIsResolving) {
            Log.w(TAG, "resolveResult: already resolving.");
            return;
        }

        Log.d(TAG, "Resolving: " + status);
        if (status.hasResolution()) {
            Log.d(TAG, "STATUS: RESOLVING");
            try {
                status.startResolutionForResult(this, requestCode);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "STATUS: Failed to send resolution.", e);
            }
        } else {
            Log.e(TAG, "STATUS: FAIL");
            if (requestCode == RC_SAVE) {
                goToContent();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" +
                data);
        if (requestCode == RC_SAVE) {
            Log.d(TAG, "Result code: " + resultCode);
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Credential Save: OK");
            } else {
                Log.e(TAG, "Credential Save Failed");
            }
            goToContent();
        }
        mIsResolving = false;
    }

    private void requestCredentials() {
        setSignInEnabled(false);
        mIsRequesting = true;

        CredentialRequest request = new CredentialRequest.Builder()
                .setSupportsPasswordLogin(true)
                .build();

        Auth.CredentialsApi.request(mGoogleApiClient, request).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(CredentialRequestResult credentialRequestResult) {
                        mIsRequesting = false;
                        Status status = credentialRequestResult.getStatus();
                        if (credentialRequestResult.getStatus().isSuccess()) {
                            // Successfully read the credential without any user interaction, this
                            // means there was only a single credential and the user has auto
                            // sign-in enabled.
                            Credential credential = credentialRequestResult.getCredential();
                            processRetrievedCredential(credential);
                        } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                            setFragment(null);
                            // This is most likely the case where the user does not currently
                            // have any saved credentials and thus needs to provide a username
                            // and password to sign in.
                            Log.d(TAG, "Sign in required");
                            setSignInEnabled(true);
                        } else {
                            Log.w(TAG, "Unrecognized status code: " + status.getStatusCode());
                            setFragment(null);
                            setSignInEnabled(true);
                        }
                    }
                }
        );

    }

    private void processRetrievedCredential(Credential credential) {

        new AsyncTask<Credential, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Credential... credentials) {

                try {
                    OAuth2RestTemplate restTemplate = ResourceConfiguration.restTemplate(properties.getProperty("security.oauth2.client.access-token-uri"), properties.getProperty("security.oauth2.client.client-id"), properties.getProperty("security.oauth2.client.client-secret"), credentials[0].getId(), credentials[0].getPassword());
                    OAuth2AccessToken oAuth2AccessToken = restTemplate.getAccessToken();
                    return !oAuth2AccessToken.isExpired();
                } catch (Exception e) {
                    Log.e("VerifyCredentialsTask", e.getMessage(), e);
                }

                return false;
            }

            protected void onPostExecute(Boolean result) {
                if (result) {
                    goToContent();
                } else {
                    // This is likely due to the credential being changed outside of
                    // Smart Lock,
                    // ie: away from Android or Chrome. The credential should be deleted
                    // and the user allowed to enter a valid credential.
                    Log.d(TAG, "Retrieved credential invalid, so delete retrieved" +
                            " credential.");

                }
            }
        }.execute(credential);


    }

}
