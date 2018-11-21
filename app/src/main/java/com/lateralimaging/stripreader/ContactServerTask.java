package com.lateralimaging.stripreader;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by matt on 16/02/16.
 */
public class ContactServerTask extends AsyncTask {

    private static String TAG = "ContactServerTask";

    private String myVersion;
    private String latestVersion;
    private String remoteServer;
    private TaskCheckPoints listner;
    private int packageSize;

    public ContactServerTask(TaskCheckPoints listner) {
        this.listner = listner;
    }

    @Override
    protected Object doInBackground(Object[] params) {

        myVersion = (String) params[0];

        ConnectivityManager cm = (ConnectivityManager) params[1];

        String phoneID = (String) params[2];

        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        remoteServer = (String) params[3];

        if (netInfo != null && netInfo.isConnected()) {
            try {
                URL url = new URL("http://" + remoteServer + "/checkVersion?yourVersion=" + myVersion);
                HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                InputStream inputStream = new BufferedInputStream(huc.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    total.append(line);
                }

                latestVersion = total.toString();

                InputStream errorStream = new BufferedInputStream(huc.getErrorStream());
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                StringBuilder errorTotal = new StringBuilder();
                String errorLine;
                try {
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorTotal.append(errorLine);
                    }
                }
                catch(IOException ioex)
                {
                    String msg = ioex.getMessage();
                    if(!msg.contentEquals("BufferedInputStream is closed"))
                    {
                        Log.e(TAG, "doInBackground: " + msg);
                    }
                }

                huc.disconnect();

                if (latestVersion == null) {
                    return errorTotal.toString();
                }

                sendLatestData(phoneID);

                if (latestVersion.startsWith("not the latest release")) {
                    packageSize = Integer.parseInt(latestVersion.substring("not the latest release".length()));
                    downloadLatestVersion();
                }

            }
            catch (IOException iox) {
                latestVersion = iox.getMessage();

                Log.d(TAG, "can't contact server: " + iox.getMessage());
            }

            Log.d(TAG, latestVersion);
        }

        return latestVersion;
    }

    private void downloadLatestVersion() throws IOException {
        URL url = new URL("http://"+remoteServer+"/getLatestVersion");
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        BufferedInputStream inputStream = new BufferedInputStream(huc.getInputStream());

        File dlFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File appFile = new File(dlFolder, "app-release.apk");
        FileOutputStream fos = new FileOutputStream(appFile);

        byte[] apkChunk = new byte[inputStream.available()];
        int readResult = inputStream.read(apkChunk, 0, apkChunk.length);
        fos.write(apkChunk);
        int byteCount = readResult;
        while (byteCount < packageSize) {
            apkChunk = new byte[inputStream.available()];
            readResult = inputStream.read(apkChunk, 0, apkChunk.length);
            fos.write(apkChunk);
            byteCount += readResult;
        }

        fos.close();
        inputStream.close();
        huc.disconnect();
    }

    private void sendLatestData(String phoneID) throws IOException {
        //credit to http://stackoverflow.com/users/1174378/mihai-todor
        //for http://stackoverflow.com/a/11826317/204723
        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        URL url = new URL("http://"+remoteServer+"/receiveData");

        File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File[] files = f.listFiles();

        for (File folder : files) {
            if (!folder.isDirectory()) {
                continue;
            }

            String fileName = folder.getName() + ".zip";

            File zipfile = new File(f, fileName);

            FileOutputStream fos = new FileOutputStream(zipfile);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

            File[] resultFiles = folder.listFiles();

            for (File file : resultFiles) {
                ZipEntry ze = new ZipEntry(file.getName());
                zos.putNextEntry(ze);
                zos.write(Util.readAllBytes(file));
                zos.closeEntry();
            }

            zos.close();

            HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoOutput(true);

            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
            httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
            httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            DataOutputStream request = new DataOutputStream(httpUrlConnection.getOutputStream());

            request.writeBytes(twoHyphens + boundary + crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"phoneID\"" + crlf + crlf + phoneID + crlf);
            request.writeBytes(twoHyphens + boundary + crlf);
            request.writeBytes("Content-Disposition: attachment; name=\"dataFile\"; filename=\"" + fileName + "\"" + crlf);
            request.writeBytes("Content-Type: application/zip" + crlf + "Content-Transfer-Encoding: binary" + crlf + crlf);
            request.write(Util.readAllBytes(zipfile));
            request.writeBytes(crlf);
            request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

            request.flush();
            request.close();

            InputStream responseStream = new
                    BufferedInputStream(httpUrlConnection.getInputStream());

            BufferedReader responseStreamReader =
                    new BufferedReader(new InputStreamReader(responseStream));

            String line;
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            responseStreamReader.close();

            String response = stringBuilder.toString();

            if (response.contentEquals("saved")) {
                Log.d(TAG, "sendLatestData: woop");
                for (File fileToDelete : folder.listFiles()) {
                    fileToDelete.delete();
                }
                folder.delete();
            } else {
                Log.d(TAG, "sendLatestData: booo " + response + " booo");
            }

            zipfile.delete();

            responseStream.close();

            httpUrlConnection.disconnect();
        }
    }

    @Override
    protected void onPreExecute()
    {
        listner.OnTaskStart();
    }

    @Override
    protected void onPostExecute(Object result)
    {
        listner.OnTaskEnd(result);
    }
}
