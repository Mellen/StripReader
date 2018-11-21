package com.lateralimaging.stripreader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Analysis extends Activity
{
    private static final String TAG = "Analysis";

    private Bitmap[] mControlData;
    private Bitmap[] mPatientData;

    private double[] mControlResults;
    private double[] mPatientResults;

    private HashMap<Integer, Integer> luminositiesList = new HashMap<>();

    private String mHealthCentreName = "";

    LocationManager mLocationManager;

    Location mLocation = null;

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location)
        {
            mLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = getSharedPreferences(Start.PREFS, 0);

        setContentView(R.layout.activity_analysis);
        mHealthCentreName = settings.getString("HealthCentre", "");

        Intent i = getIntent();
        Parcelable[] dataParcel = i.getParcelableArrayExtra("data");

        mPatientData = new Bitmap[dataParcel.length];

        for(int index = 0; index < mPatientData.length; index++)
        {
            mPatientData[index] = (Bitmap)dataParcel[index];
        }

        Parcelable[] controlParcel = i.getParcelableArrayExtra("controlData");

        mControlData = new Bitmap[controlParcel.length];

        for(int index = 0; index < mControlData.length; index++)
        {
            mControlData[index] = (Bitmap)controlParcel[index];
        }

        View btnSave = findViewById(R.id.btnSaveData);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int value = -1;

                HashMap<Integer, Integer> intToID = new HashMap<Integer, Integer>();

                intToID.put(0, R.id.rdoZero);
                intToID.put(1, R.id.rdoOne);
                intToID.put(2, R.id.rdoTwo);
                intToID.put(3, R.id.rdoThree);
                intToID.put(4, R.id.rdoFour);

                for(int i = 0; i < 5; i++)
                {
                    RadioButton rb = (RadioButton)findViewById(intToID.get(i));
                    if(rb.isChecked())
                    {
                        value = i;
                        break;
                    }
                }

                if(mControlData != null && mPatientData != null && mHealthCentreName.length() > 0 && value > -1)
                {
                    saveData(mControlResults, mPatientResults, value);
                    finish();
                }
                else if (mHealthCentreName.length() == 0)
                {
                    new AlertDialog.Builder(Analysis.this).setTitle(getString(R.string.missing_information))
                            .setMessage(getString(R.string.health_centre_name_request))
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .create()
                            .show();
                }
                else if (value == -1)
                {
                    new AlertDialog.Builder(Analysis.this).setTitle(getString(R.string.missing_information))
                            .setMessage(getString(R.string.observed_value_request))
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

        EditText txtHealthCentre = (EditText)findViewById(R.id.txtHealthCentre);

        txtHealthCentre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mHealthCentreName = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        txtHealthCentre.setText(mHealthCentreName);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        analyse();
    }

    private void analyse()
    {
        //changeAllLuminosities(mControlData, mPatientData);

        mControlResults = getAccumulatedResults(mControlData);

        String controlResultText = ((mControlResults[1] > mControlResults[0]) && (mControlResults[1] > mControlResults[2])) ? getString(R.string.analysis_complete_control_good) : getString(R.string.analysis_complete_control_bad);
        String patientResultText = getString(R.string.unanalysed);

        if(controlResultText == getString(R.string.analysis_complete_control_good))
        {
            mPatientResults = getAccumulatedResults(mPatientData);
            boolean detected = (mPatientResults[1] > mPatientResults[0]) && (mPatientResults[1] > mPatientResults[2]);
            patientResultText = detected ? getString(R.string.analysis_complete_detected) : getString(R.string.analysis_complete_no_tb);
        }

        TextView patientView = (TextView)findViewById(R.id.txtResult);

        patientView.setText(patientResultText);

        TextView controlView = (TextView)findViewById(R.id.txtControlResult);

        controlView.setText(controlResultText);
    }

    private void changeAllLuminosities(Bitmap[] controlData, Bitmap[] patientData)
    {
        for(int i = 0; i < controlData.length; i++)
        {
            Bitmap control = controlData[i];

            int width = control.getWidth();
            int height = control.getHeight();

            int[] pixels = new int[height * width];

            control.getPixels(pixels, 0, width, 0, 0, width, height);

            double lumSum = 0.0;
            double brightestLuminosity = 0.0;

            for(int pixelIndex = 0; pixelIndex < pixels.length; pixelIndex++)
            {
                int r = Color.red(pixels[pixelIndex]);
                int g = Color.green(pixels[pixelIndex]);
                int b = Color.blue(pixels[pixelIndex]);

                double luminosity = ((Math.min(r, Math.min(g, b)) + Math.max(r, Math.max(g, b)))/2.0)/255.0;

                lumSum += luminosity;

                if(luminosity > brightestLuminosity)
                {
                    brightestLuminosity = luminosity;
                }
            }

            double lumAvg = lumSum / pixels.length;

            changeLuminosity(control, brightestLuminosity, lumAvg);

            Bitmap patient = patientData[i];

            changeLuminosity(patient, brightestLuminosity, lumAvg);
            removeGlare(patient, lumAvg);
        }
    }

    private void removeGlare(Bitmap bitmap, double avgLuminosity)
    {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width*height];
        int[] pixels2 = new int[width*height];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int brightCount = 0;

        for(int pixelIndex = 0; pixelIndex < pixels.length; pixelIndex++)
        {
            int r = Color.red(pixels[pixelIndex]);
            int g = Color.green(pixels[pixelIndex]);
            int b = Color.blue(pixels[pixelIndex]);

            double luminosity = ((Math.min(r, Math.min(g, b)) + Math.max(r, Math.max(g, b))) / 2.0) / 255.0;

            if(luminosity > 0.99999)
            {
                pixels2[pixelIndex] = pixels[pixelIndex];
                brightCount++;
            }
        }

        if(brightCount > 5)
        {
            int[] glares = findGlare(pixels2, width, height, brightCount, 0);

            for(int gi = 0; gi < glares.length; gi++)
            {
                int index = glares[gi];

                pixels[index] = changeLuminosity(pixels[index], avgLuminosity);
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    private int[] findGlare(int[] pixels, int width, int height, int brightCount, int startIndex)
    {
        ArrayList<Integer> glares = new ArrayList<>();

        int smallWidth = width/4;
        int smallHeight = height/2;

        double brightPercent = brightCount/(double)pixels.length;

        for(int i = 0; i < 8; i++)
        {
            int sectionBrightCount = 0;

            ArrayList<String> coords = new ArrayList<>();

            int minX = smallWidth * (i % 4);
            int minY = smallHeight * width * (i/4);

            for(int j = 0; j < 100; j++)
            {
                int x = (int) ((Math.random() * smallWidth) + minX);
                int y = (int) ((Math.random() * smallHeight) + minY);

                String coord = x + "," + y;

                while(coords.indexOf(coord) > -1)
                {
                    x = (int) ((Math.random() * smallWidth) + minX);
                    y = (int) ((Math.random() * smallHeight) + minY);

                    coord = x + "," + y;
                }

                coords.add(coord);
                int index = (x + (y*width) + startIndex);

                if(pixels[index] > 0)
                {
                    sectionBrightCount++;
                }
            }

            double sectionBrightPercent = sectionBrightCount/100.0;

            if(sectionBrightPercent >= brightPercent)
            {
                int smallIndex = startIndex + minX + (minY * smallWidth);

                if(smallWidth > 6)
                {
                    int[] smallResult = findGlare(pixels, smallWidth, smallHeight, brightCount, smallIndex);
                    for(int sri = 0; sri < smallResult.length; sri++)
                    {
                        glares.add(smallResult[sri]);
                    }
                }
                else
                {
                    if(sectionBrightCount >= 6)
                    {
                        for(int x = 0; x < smallWidth; x++)
                        {
                            for(int y = 0; y < smallHeight; y++)
                            {
                                int index = (x + (y*width) + smallIndex);

                                int whiteCount = 0;

                                if(pixels[index] > 0)
                                {
                                    whiteCount++;
                                    if(x < smallWidth - 1)
                                    {
                                        int nextIndex = ((x+1) + (y*width) + smallIndex);
                                        if(pixels[nextIndex] > 0)
                                        {
                                            whiteCount++;
                                        }
                                    }
                                    if(y < smallHeight - 1)
                                    {
                                        int nextIndex = (x + ((y+1)*width) + smallIndex);
                                        if(pixels[nextIndex] > 0)
                                        {
                                            whiteCount++;
                                        }
                                    }
                                    if((y < smallHeight - 1) && (x < smallWidth - 1))
                                    {
                                        int nextIndex = ((x+1) + ((y+1)*width) + smallIndex);
                                        if(pixels[nextIndex] > 0)
                                        {
                                            whiteCount++;
                                        }
                                    }
                                    if(x > 0)
                                    {
                                        int nextIndex = ((x-1) + (y*width) + smallIndex);
                                        if(pixels[nextIndex] > 0)
                                        {
                                            whiteCount++;
                                        }
                                    }
                                    if(y > 0)
                                    {
                                        int nextIndex = (x + ((y-1)*width) + smallIndex);
                                        if(pixels[nextIndex] > 0)
                                        {
                                            whiteCount++;
                                        }
                                    }
                                    if(y > 0 && x > 0)
                                    {
                                        int nextIndex = ((x-1) + ((y-1)*width) + smallIndex);
                                        if(pixels[nextIndex] > 0)
                                        {
                                            whiteCount++;
                                        }
                                    }
                                    if(y > 0 && (x < smallWidth - 1))
                                    {
                                        int nextIndex = ((x+1) + ((y-1)*width) + smallIndex);
                                        if(pixels[nextIndex] > 0)
                                        {
                                            whiteCount++;
                                        }
                                    }
                                    if((y < smallHeight - 1) && x >0)
                                    {
                                        int nextIndex = ((x-1) + ((y+1)*width) + smallIndex);
                                        if(pixels[nextIndex] > 0)
                                        {
                                            whiteCount++;
                                        }
                                    }
                                }
                                if(whiteCount >= 6)
                                {
                                    glares.add(index);
                                }
                            }
                        }
                    }
                }
            }
        }

        int [] result = new int[glares.size()];
        for(int i = 0; i < result.length; i++)
        {
            result[i] = glares.get(i);
        }

        return result;
    }

    private void changeLuminosity(Bitmap bitmap, double newLuminosity, double avgLuminosity)
    {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width*height];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for(int pixelIndex = 0; pixelIndex < pixels.length; pixelIndex++)
        {
            int r = Color.red(pixels[pixelIndex]);
            int g = Color.green(pixels[pixelIndex]);
            int b = Color.blue(pixels[pixelIndex]);

            double luminosity = ((Math.min(r, Math.min(g, b)) + Math.max(r, Math.max(g, b)))/2.0)/255.0;

            if(luminosity > (avgLuminosity * (7.0/6.0)))
            {
                pixels[pixelIndex] = changeLuminosity(pixels[pixelIndex], newLuminosity);
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    private int changeLuminosity(int colour, double newLuminosity)
    {
        int r = Color.red(colour);
        int g = Color.green(colour);
        int b = Color.blue(colour);

        int maxC = Math.max(r, Math.max(g, b));
        int minC = Math.min(r, Math.min(g, b));

        double saturation;

        if (newLuminosity > 0.5) {
            saturation = (maxC - minC) / (2.0 - maxC - minC);
        } else {
            saturation = (maxC - minC) / (maxC + minC);
        }

        double hue;

        if (b == maxC) {
            hue = (4.0 + (b - r)) / (maxC - minC);
        } else if (g == maxC) {
            hue = (2.0 + (b - r)) / (maxC - minC);
        } else {
            hue = (g - b) / (maxC - minC);
        }

        hue = hue * 60.0;

        if (hue < 0) {
            hue += 360.0;
        }

        hue = hue / 360;

        double lumPart;

        if (newLuminosity < 0.5) {
            lumPart = newLuminosity * (1.0 + saturation);
        } else {
            lumPart = (newLuminosity + saturation) - (newLuminosity * saturation);
        }

        double invLumPart = (2 * newLuminosity) - lumPart;

        double newR = hue + 0.333;
        double newG = hue;
        double newB = hue - 0.333;

        if (newB < 0)
        {
            newB += 1.0;
        }

        newR = calculateColour(newR, lumPart, invLumPart);
        newG = calculateColour(newG, lumPart, invLumPart);
        newB = calculateColour(newB, lumPart, invLumPart);

        r = (int) (newR * 255);
        g = (int) (newG * 255);
        b = (int) (newB * 255);

        return Color.argb(255, r, g, b);
    }

    private double calculateColour(double currentColour, double lumPart, double invLumPart)
    {
        double result = invLumPart;

        if(currentColour * 6 < 1.0)
        {
            result = invLumPart + (lumPart - invLumPart) * 6 * currentColour;
        }
        else if(2 * currentColour < 1.0)
        {
            result = lumPart;
        }
        else if(currentColour * 3 < 2.0)
        {
            result = invLumPart + (lumPart - invLumPart) * (0.666 - currentColour) * 6;
        }

        return result;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void saveData(double[] controlResults, double[] patientResults, int expectedValue)
    {
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        }
        catch(PackageManager.NameNotFoundException nnfe)
        {
            Log.d(TAG, "onCreate: no version name");
        }

        String appVersion = pInfo.versionName;

        SharedPreferences settings = getSharedPreferences(Start.PREFS, 0);
        SharedPreferences.Editor edit = settings.edit();

        edit.putString("HealthCentre", mHealthCentreName);
        edit.commit();

        int currentResult = 1;

        File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File[] files = f.listFiles();

        for (int i = 0; files != null && i < files.length; i++) {
            File file = files[i];

            if(file.isDirectory())
            {
                try
                {
                    int curFileNum = Integer.parseInt(file.getName());
                    if (currentResult <= curFileNum) {
                        currentResult = curFileNum + 1;
                    }
                }
                catch(NumberFormatException nfe)
                {
                    log("This ain't no number: " + nfe.getMessage());
                }
            }
        }

        File newDir = new File(f, ((Integer)(currentResult)).toString());
        newDir.mkdir();

        FileWriter fw = null;

        try
        {
            fw = new FileWriter(new File(newDir, "results.csv"));
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage());
        }

        if(patientResults != null)
        {
            try
            {
                fw.write("Upper,Mid,Lower\n");
                fw.write("patient\n");
            }
            catch (IOException ex)
            {
                log("%s", ex.getMessage());
            }

            for (int i = 0; i < patientResults.length; i++)
            {
                try
                {
                    fw.write(patientResults[i] + ",");
                }
                catch (IOException ex)
                {
                    log("%s", ex.getMessage());
                }
            }
        }

        try
        {
            fw.write("\n\ncontrol\n");
        }
        catch (IOException ex)
        {
            log("%s", ex.getMessage());
        }

        for(int i = 0; i < controlResults.length; i++)
        {
            try
            {
                fw.write(controlResults[i]+",");
            }
            catch (IOException ex)
            {
                log("%s", ex.getMessage());
            }
        }

        try
        {
            fw.write("\n\n");
            fw.write(getString(R.string.analysis_control_label));
            fw.write(",");
            if((mControlResults[1] > mControlResults[0]) && (mControlResults[1] > mControlResults[2]))
            {
                fw.write(getString(R.string.analysis_complete_control_good));
            }
            else
            {
                fw.write(getString(R.string.analysis_complete_control_bad));
            }
            fw.write("\n\n");
            fw.write(getString(R.string.analysis_result_label));
            fw.write(",");
            if(patientResults != null)
            {
                if ((mPatientResults[1] > mPatientResults[0]) && (mPatientResults[1] > mPatientResults[2]))
                {
                    fw.write(getString(R.string.analysis_complete_detected));
                }
                else
                {
                    fw.write(getString(R.string.analysis_complete_no_tb));
                }
            }
            else
            {
                fw.write(getString(R.string.unanalysed));
            }
            fw.write("\n\n");
            fw.write("Health Centre");
            fw.write(",");
            fw.write(mHealthCentreName);
            fw.write("\n\n");
            fw.write("Observed score");
            fw.write(",");
            fw.write(((Integer)expectedValue).toString());
            fw.write("\n\n");
            fw.write("Location");
            fw.write(",");

            if(mLocation != null)
            {
                String lat = Double.toString(mLocation.getLatitude());
                fw.write(lat);
                fw.write(",");

                String lon = Double.toString(mLocation.getLongitude());
                fw.write(lon);
            }
            else
            {
                fw.write("unknown,unknown");
            }

            fw.write("\n\n");

            fw.write("Time stamp");
            fw.write(",");
            fw.write((new Date()).toString());
            fw.write("\n\n");
            fw.write("App Version");
            fw.write(",");
            fw.write(appVersion);
        }
        catch (IOException ex)
        {
            log("%s", ex.getMessage());
        }

        try
        {
            fw.close();
        } catch (IOException ex) {
            log("%s", ex.getMessage());
        }

        for(int i = 0; i < mPatientData.length; i++)
        {
            Bitmap b = mPatientData[i];

            saveBitmap(b, currentResult+"/patient" + i + ".png");
        }

        for(int i = 0; i < mControlData.length; i++)
        {
            Bitmap b = mControlData[i];

            saveBitmap(b, currentResult+"/control" + i + ".png");
        }
    }

    private void saveBitmap(Bitmap bitmap, String filename)
    {
        FileOutputStream stream = null;

        try
        {
            stream = new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), filename));
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage());
        }

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    }

    private double[] getAccumulatedResults(Bitmap[] data)
    {
        double[] acc = new double[3];

        for(int i = 0; i < data.length; i++)
        {
            int height = data[i].getHeight();
            int width = data[i].getWidth();

            Bitmap imageData = data[i];

            int third = height/3;

            //upper
            int[] upperPixels = new int[width*third];

            imageData.getPixels(upperPixels, 0, width, 0, 0, width, third);

            acc[0] += accumulateGrey(upperPixels);

            //mid
            int[] midPixels = new int[width*third];

            imageData.getPixels(midPixels, 0, width, 0, third+1, width, third);

            acc[1] += accumulateGrey(midPixels);

            //lower
            int[] lowerPixels = new int[width*third];

            imageData.getPixels(lowerPixels, 0, width, 0, third*2, width, third);

            acc[2] += accumulateGrey(lowerPixels);
        }

        return acc;
    }

    private double accumulateGrey(int[] pixels) {
        double sum = 0;

        for(int i = 0; i < pixels.length; i++)
        {
            sum += 255 - Color.blue(pixels[i]);
        }

        return sum;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void log(String message, Object... values)
    {
        Log.d(TAG, String.format(message, values));
    }
}
