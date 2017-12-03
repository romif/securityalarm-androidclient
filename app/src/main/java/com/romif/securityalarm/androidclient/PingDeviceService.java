package com.romif.securityalarm.androidclient;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;

import com.google.android.gms.auth.api.credentials.Credential;
import com.romif.securityalarm.api.config.Constants;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Properties;

public class PingDeviceService extends JobService {

    private static final int PING_INTERVAL = 30;

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
                    String time = restTemplate.getForObject(url, String.class);
                    Log.d("PingDeviceService", time.toString());
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
                jobFinished(jobParameters, false);
                scheduleJob(getBaseContext(), jobParams);
            }
        }.execute();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    public static void scheduleJob(Context context, Properties properties, Credential credential) {
        String[] jobParams = new String[]{properties.getProperty("security.oauth2.client.access-token-uri"), properties.getProperty("security.oauth2.client.client-id"), properties.getProperty("security.oauth2.client.client-secret"), credential.getId(), credential.getPassword(), properties.getProperty("securityalarm.client.url")};

        scheduleJob(context, jobParams);

        AlarmNotification.notify(context, AlarmState.PAUSED, 333);
    }

    private static void scheduleJob(Context context, String[] jobParams) {
        ComponentName serviceComponent = new ComponentName(context, PingDeviceService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        //builder.setMinimumLatency(PING_INTERVAL * 1000);
        //builder.setOverrideDeadline((long)(PING_INTERVAL * 1000 * 1.1));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPeriodic(PING_INTERVAL * 1000);
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putStringArray("jobParams", jobParams);
        builder.setExtras(bundle);
        jobScheduler.schedule(builder.build());
    }


    public static void cancelJob(Context context) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.cancel(0);
        AlarmNotification.notify(context, AlarmState.RESUMED, 333);
    }

}
