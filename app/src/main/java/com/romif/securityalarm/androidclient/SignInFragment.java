/**
 * Copyright Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.romif.securityalarm.androidclient;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.credentials.Credential;

import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import java.io.IOException;
import java.util.Properties;

public class SignInFragment extends Fragment {

    private static final String TAG = "SignInFragment";
    private TextInputLayout mUsernameTextInputLayout;
    private EditText mUsernameEditText;
    private TextInputLayout mPasswordTextInputLayout;
    private EditText mPasswordEditText;
    private Button mSignInButton;
    private ProgressBar mSignInProgressBar;
    private Properties properties = new Properties();

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_sign_in, container, false);
        mUsernameTextInputLayout = (TextInputLayout) view.findViewById(R.id.usernameTextInputLayout);
        mPasswordTextInputLayout = (TextInputLayout) view.findViewById(R.id.passwordTextInputLayout);

        mUsernameEditText = (EditText) view.findViewById(R.id.usernameEditText);
        mPasswordEditText = (EditText) view.findViewById(R.id.passwordEditText);

        try {
            properties.load(getContext().getAssets().open("application.properties"));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        mSignInButton = (Button) view.findViewById(R.id.signInButton);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                setSignEnabled(false);
                String username = mUsernameTextInputLayout.getEditText().getText().toString();
                String password = mPasswordTextInputLayout.getEditText().getText().toString();
                password = "sqAQks7asyc3a5qT";

                final Credential credential = new Credential.Builder(username)
                        .setPassword(password)
                        .build();
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

                    @Override
                    protected void onPostExecute(Boolean result) {
                        if (result) {
                            ((MainActivity) getActivity()).saveCredential(credential);
                        } else {
                            Log.d(TAG, "Credentials are invalid. Username or password are " +
                                    "incorrect.");
                            Toast.makeText(view.getContext(), R.string.invalid_creds_toast_msg,
                                    Toast.LENGTH_SHORT).show();
                            setSignEnabled(true);
                        }
                    }

                    @Override
                    protected void onCancelled(Boolean s) {
                        Log.d(TAG, "Credentials are invalid. Username or password are " +
                                "incorrect.");
                        Toast.makeText(view.getContext(), R.string.invalid_creds_toast_msg,
                                Toast.LENGTH_SHORT).show();
                        setSignEnabled(true);

                    }
                }.execute(credential);

            }
        });

        Button clearButton = (Button) view.findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUsernameTextInputLayout.getEditText().setText("");
                mPasswordTextInputLayout.getEditText().setText("");
            }
        });

        mSignInProgressBar = (ProgressBar) view.findViewById(R.id.signInProgress);
        mSignInProgressBar.setVisibility(ProgressBar.INVISIBLE);

        return view;
    }

    public void onResume() {
        super.onResume();
        if (((MainActivity) getActivity()).isResolving() || ((MainActivity) getActivity()).isRequesting()) {
            setSignEnabled(false);
        } else {
            setSignEnabled(true);
        }
    }

    /**
     * Enable or disable Sign In form.
     *
     * @param enable Enable form when true, disable when false.
     */
    protected void setSignEnabled(boolean enable) {
        mSignInButton.setEnabled(enable);
        mUsernameEditText.setEnabled(enable);
        mPasswordEditText.setEnabled(enable);
        if (!enable) {
            mSignInProgressBar.setVisibility(ProgressBar.VISIBLE);
        } else {
            mSignInProgressBar.setVisibility(ProgressBar.INVISIBLE);
        }
    }

}
