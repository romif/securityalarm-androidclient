package com.romif.securityalarm.androidclient;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.romif.securityalarm.api.config.Constants;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.Properties;

public class PingDeviceService extends JobService {

    private OAuth2RestTemplate restTemplate;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        String[] jobParams = jobParameters.getExtras().getStringArray("jobParams");

        restTemplate = ResourceConfiguration.restTemplate(jobParams[0], jobParams[1], jobParams[2], jobParams[3], jobParams[4]);

        ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setConnectTimeout(10 * 1000);

        final String url = jobParams[5] + Constants.PING_DEVICE_PATH;

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    restTemplate.getForObject(url, String.class);
                } catch (ResourceAccessException e) {
                    Log.e("PingDeviceService", e.getMessage());
                    return false;
                } catch (Exception e) {
                    Log.e("PingDeviceService", e.getMessage(), e);
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                jobFinished(jobParameters, true);
            }
        }.execute();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

}
