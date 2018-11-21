package com.lateralimaging.stripreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.UUID;


public class Start extends Activity implements TaskCheckPoints{

    public static String PREFS = "tideprefs";

    public String TAG = "Start";

    private String myVersion = "";

    private UUID phoneID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        }
        catch(PackageManager.NameNotFoundException nnfe)
        {
            Log.d(TAG, "onCreate: no version name");
        }

        myVersion = pInfo.versionName;

        final ConnectivityManager cm =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        SharedPreferences settings = getSharedPreferences(PREFS, 0);

        int x = settings.getInt("x", -4);
        int y = settings.getInt("y", 0);

        phoneID = UUID.fromString(settings.getString("phoneID", UUID.randomUUID().toString()));

        StripMaskView.setOffsets(x, y);

        setContentView(R.layout.activity_start);

        final View captureButton = findViewById(R.id.btnCapture);

        final Start s = this;

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent i = new Intent(s, Reader.class);
                startActivity(i);
            }
        });

        final View checkIn = findViewById(R.id.btnCheckIn);

        checkIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if(netInfo != null && netInfo.isConnected())
                {
                    final AsyncTask serverCheckInFromButton = new ContactServerTask(s);

                    serverCheckInFromButton.execute(myVersion, cm, phoneID.toString(), getString(R.string.remote_server));
                }
                else
                {
                    new AlertDialog.Builder(Start.this).setTitle(getString(R.string.no_wifi_title))
                            .setMessage(getString(R.string.no_wifi_message))
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .create()
                            .show();
                }
            }
        });

        TextView versionView = (TextView)findViewById(R.id.txtVersion);
        versionView.setText(getString(R.string.version) + " " + myVersion);
    }

    @Override
    public void OnTaskStart() {
        final View captureButton = findViewById(R.id.btnCapture);
        captureButton.setEnabled(false);
        final View checkIn = findViewById(R.id.btnCheckIn);
        checkIn.setEnabled(false);
        final View progress = findViewById(R.id.loading_spinner);
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void OnTaskEnd(Object result) {
        final View captureButton = findViewById(R.id.btnCapture);
        captureButton.setEnabled(true);
        final View checkIn = findViewById(R.id.btnCheckIn);
        checkIn.setEnabled(true);
        final View progress = findViewById(R.id.loading_spinner);
        progress.setVisibility(View.INVISIBLE);

        if(((String)result).startsWith("not the latest release"))
        {
            File appFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "app-release.apk");

            Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.fromFile(appFile), "application/vnd.android.package-archive");
            promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(promptInstall);
        }
        else if(((String)result).contains("EHOSTUNREACH"))
        {
            new AlertDialog.Builder(Start.this).setTitle(getString(R.string.host_unreachable_title))
                    .setMessage(getString(R.string.host_unreachable_message))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create()
                    .show();
        }
    }
}
