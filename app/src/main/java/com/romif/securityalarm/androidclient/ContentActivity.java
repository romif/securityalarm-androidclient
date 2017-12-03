/**
 * Copyright Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.romif.securityalarm.androidclient;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.io.IOException;
import java.util.Properties;

public class ContentActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener  {

    private static final String TAG = "ContentActivity";

    private GoogleApiClient mGoogleApiClient;
    private Properties properties = new Properties();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

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

        Button logoutButton = (Button) findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CredentialRequest request = new CredentialRequest.Builder()
                        .setSupportsPasswordLogin(true)
                        .build();

                Auth.CredentialsApi.request(mGoogleApiClient, request).setResultCallback(
                        new ResultCallback<CredentialRequestResult>() {
                            @Override
                            public void onResult(CredentialRequestResult credentialRequestResult) {
                                Status status = credentialRequestResult.getStatus();
                                if (credentialRequestResult.getStatus().isSuccess()) {
                                    // Successfully read the credential without any user interaction, this
                                    // means there was only a single credential and the user has auto
                                    // sign-in enabled.
                                    Credential credential = credentialRequestResult.getCredential();
                                    deleteCredential(credential);


                                }
                            }
                        }
                );



            }
        });

        Button changeCredsButton = (Button) findViewById(R.id.changeCredsButton);
        changeCredsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(v.getContext(), ChangeCredActivity.class));
            }
        });
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

    private void deleteCredential(Credential credential) {

        new AsyncTask<Credential, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Credential... credentials) {

                try {
                    OAuth2RestTemplate restTemplate = ResourceConfiguration.restTemplate(properties.getProperty("security.oauth2.client.access-token-uri"), properties.getProperty("security.oauth2.client.client-id"), properties.getProperty("security.oauth2.client.client-secret"), credentials[0].getId(), credentials[0].getPassword());
                    String macAddress = WifiReceiver.getMacAddress(getBaseContext());
                    restTemplate.delete(properties.getProperty("securityalarm.server.url") + "/api/account/mac_address/" + macAddress);
                    return true;
                } catch (Exception e) {
                    Log.e("VerifyCredentialsTask", e.getMessage(), e);
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                Auth.CredentialsApi.disableAutoSignIn(mGoogleApiClient);
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }.execute(credential);


    }
}
