package com.lateralimaging.stripreader;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class Reader extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener{

    static {
        System.loadLibrary("opencv_java3");
    }

    private int lastPStartY = -1;
    private int lastCStartY = -1;
    private int lastCEndY = -1;
    private int lastPEndY = -1;

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

    private OpenCVCameraView mOpenCvCameraView;
    private StripMaskView mStripMask;

    private static final String TAG = "Reader";

    private Bitmap[] mPatientData;
    private Bitmap[] mControlData;

    boolean mDone = false;
    boolean mCaptureRun = false;

    private final Reader r = this;
    private final int mScanCount = 47;

    private Camera.PreviewCallback mPicture = new Camera.PreviewCallback()
    {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera)
        {
            if (data == null)
            {
                return;
            }

            Bitmap[] pdcd = null;

            if(mPatientData[mScanCount - 6] == null)
            {
                pdcd = processBitmap(createBitmap(data));
            }
            else
            {
                pdcd = processNoFlashBitmap(createBitmap(data));
            }

            int completedScanCount = updateData(pdcd);

            log("completed scans: %d", completedScanCount);

            if(mPatientData[mScanCount - 1] != null && !mDone)
            {
                mDone = true;
                Intent i = new Intent(r, Analysis.class);
                i.putExtra("data", mPatientData);
                i.putExtra("controlData", mControlData);
                startActivity(i);

                View pleaseWait = findViewById(R.id.txtPleaseWait);
                pleaseWait.setVisibility(View.GONE);

                View PWBG = findViewById(R.id.txtPWBG);
                PWBG.setVisibility(View.GONE);
            }
        }
    };

    private int updateData(Bitmap[] newData)
    {
        int count = 0;

        Bitmap patientData = newData[0];
        Bitmap controlData = newData[1];

        for(int i = 0; i < mScanCount; i++, count++)
        {
            if(mPatientData[i] == null)
            {
                mPatientData[i] = patientData;
                break;
            }
        }

        for(int i = 0; i < mScanCount; i++)
        {
            if(mControlData[i] == null)
            {
                mControlData[i] = controlData;
                break;
            }
        }

        return count+1;
    }

    private Bitmap[] processBitmap(Bitmap data)
    {
        Bitmap[] result = new Bitmap[2];

        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        Bitmap rotated = Bitmap.createBitmap(data, 0, 0, data.getWidth(), data.getHeight(), matrix, true);

        StripDataCoordinates patientCoords = mStripMask.GetPatientCoordinates(rotated.getHeight(), rotated.getWidth());

        int patientWidth = patientCoords.mPatientBottomRight.x - patientCoords.mPatientTopLeft.x;
        int patientHeight = patientCoords.mPatientBottomRight.y - patientCoords.mPatientTopLeft.y;

        int[] brightnessRightColours = new int[patientWidth];
        rotated.getPixels(brightnessRightColours, 0, rotated.getWidth(), patientCoords.mPatientBottomRight.x, patientCoords.mPatientTopLeft.y + (patientHeight/2), patientWidth, 1);

        int[] brightnessLeftColours = new int[patientWidth];
        rotated.getPixels(brightnessLeftColours, 0, rotated.getWidth(), patientCoords.mPatientTopLeft.x - patientWidth, patientCoords.mPatientTopLeft.y + (patientHeight/2), patientWidth, 1);

        Bitmap patientData = Bitmap.createBitmap(rotated, patientCoords.mPatientTopLeft.x, patientCoords.mPatientTopLeft.y, patientWidth, patientHeight);

        log("processBitmap: width: %d, height: %d", rotated.getWidth(), rotated.getHeight());

        patientData = trimCyan(patientData);

        result[0] = patientData;

        Bitmap controlData = Bitmap.createBitmap(rotated, patientCoords.mControlTopLeft.x, patientCoords.mControlTopLeft.y, patientWidth, patientHeight);

        controlData = trimCyan(controlData);

        result[1] = controlData;

        if(result[0] == null || result[1] == null)
        {
            result = null;
        }

        return result;
    }

    private Bitmap[] processNoFlashBitmap(Bitmap data)
    {
        Bitmap[] result = new Bitmap[2];

        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        Bitmap rotated = Bitmap.createBitmap(data, 0, 0, data.getWidth(), data.getHeight(), matrix, true);

        StripDataCoordinates dataCoords = mStripMask.GetPatientCoordinates(rotated.getHeight(), rotated.getWidth());

        int patientWidth = dataCoords.mPatientBottomRight.x - dataCoords.mPatientTopLeft.x;
        int patientHeight = dataCoords.mPatientBottomRight.y - dataCoords.mPatientTopLeft.y;

        Bitmap patientData = Bitmap.createBitmap(rotated, dataCoords.mPatientTopLeft.x, dataCoords.mPatientTopLeft.y, patientWidth, patientHeight);

        Bitmap controlData = Bitmap.createBitmap(rotated, dataCoords.mControlTopLeft.x, dataCoords.mControlTopLeft.y, patientWidth, patientHeight);

        result[0] = Bitmap.createBitmap(patientData, 0, this.lastPStartY, patientWidth, this.lastPEndY - this.lastPStartY);
        result[1] = Bitmap.createBitmap(controlData, 0, this.lastCStartY, patientWidth, this.lastCEndY - this.lastCStartY);

        return result;
    }

    private Bitmap trimCyan(Bitmap data)
    {
        int width = data.getWidth();
        int height = data.getHeight();
        int startY = 0;
        int endY = height;
        int x = width/2;
        int[] pixels = new int[height*width];

        data.getPixels(pixels, 0, width, 0, 0, width, height);

        int[] topScores = new int[height/3];
        for(int y = 0; y < height/3; y++)
        {
            int index = x + (y * width);
            int r = Color.red(pixels[index]);
            topScores[y] = r;
        }

        int tolerance = 11;

        if(topScores[topScores.length - 1] - topScores[0] > tolerance)
        {
            int last = topScores[0];
            for(int y = 1; y < topScores.length; y++)
            {
                int cur = topScores[y];
                if(cur - last > tolerance)
                {
                    startY = y+1;
                    break;
                }
                last = cur;
            }
        }

        int[] bottomScores = new int[height/3];

        int bottomStart = (2 * height/3)+1;

        for(int y = bottomStart; y < height; y++)
        {
            int index = x + (y * width);
            int r = Color.red(pixels[index]);

            bottomScores[y - bottomStart] = r;
        }

        if(bottomScores[0] - bottomScores[bottomScores.length-1] > 10)
        {
            int last = bottomScores[0];
            for(int y = 1; y < bottomScores.length; y++)
            {
                int cur = bottomScores[y];
                if(last - cur > tolerance)
                {
                    endY = y+bottomStart;
                    break;
                }
                last = cur;
            }
        }

        if(this.lastPStartY != -1 && this.lastCStartY == -1)
        {
            this.lastCStartY = startY;
            this.lastCEndY = endY;
        }

        if(this.lastPStartY == -1)
        {
            this.lastPStartY = startY;
            this.lastPEndY = endY;
        }

        return Bitmap.createBitmap(data, 0, startY, width, endY - startY);
    }

    private Bitmap createBitmap(byte[] data) {
        Camera.Size previewSize = mOpenCvCameraView.getPreviewSize();
        YuvImage img = new YuvImage(data, mOpenCvCameraView.getImageFormat(), previewSize.width, previewSize.height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        img.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);
        byte[] jdata = baos.toByteArray();

        Bitmap realData = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);

        return realData;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(Reader.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        if (mOpenCvCameraView != null)
        {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            {
                mOpenCvCameraView.turnOffFlash();
            }
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mDone = false;

        mPatientData = new Bitmap[mScanCount];
        mControlData = new Bitmap[mScanCount];
        for(int i = 0; i < mScanCount; i++) {
            mPatientData[i] = null;
            mControlData[i] = null;
        }

        if (!OpenCVLoader.initDebug()) {
            log("Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        } else {
            log("OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        final TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
                {
                    mOpenCvCameraView.turnOnFlash();
                }
            }
        };

        final Timer timer = new Timer();

        timer.schedule(task, 1000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPatientData = new Bitmap[mScanCount];
        mControlData = new Bitmap[mScanCount];
        for(int i = 0; i <  mScanCount; i++)
        {
            mPatientData[i] = null;
            mControlData[i] = null;
        }

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_reader);

        mOpenCvCameraView = (OpenCVCameraView) findViewById(R.id.camera_view);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mStripMask = (StripMaskView) findViewById(R.id.camera_mask);

        mOpenCvCameraView.setWindowWidthHeight(mStripMask.getWidth(), mStripMask.getHeight());

        mStripMask.setPatientLabel(getString(R.string.patient));
        mStripMask.setControlLabel(getString(R.string.control));

        final Reader r = this;

        Button btnConfig = (Button)findViewById(R.id.btnConfig);
        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(r, StripMaskConfiguration.class);
                startActivity(i);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_reader, menu);
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

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        View pleaseWait = findViewById(R.id.txtPleaseWait);
        pleaseWait.setVisibility(View.VISIBLE);

        View PWBG = findViewById(R.id.txtPWBG);
        PWBG.setVisibility(View.VISIBLE);


        for (int i = 0; i < mScanCount; i++)
        {
            capturePreview();
        }

        return false;
    }

    private void capturePreview()
    {
        mCaptureRun = true;
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    mOpenCvCameraView.setPreviewCallbackWithBuffer(mPicture);
                    int bufferSize = mOpenCvCameraView.getCallbackBufferSize();
                    byte[] buffer = new byte[bufferSize];
                    mOpenCvCameraView.addCallbackBuffer(buffer);
                }
            };

            final Timer timer = new Timer();

            timer.schedule(task, 400);

            mOpenCvCameraView.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    task.cancel();
                    mOpenCvCameraView.setPreviewCallbackWithBuffer(mPicture);
                    int bufferSize = mOpenCvCameraView.getCallbackBufferSize();
                    byte[] buffer = new byte[bufferSize];
                    mOpenCvCameraView.addCallbackBuffer(buffer);
                }
            });

        } else {
            mOpenCvCameraView.setPreviewCallbackWithBuffer(mPicture);
            int bufferSize = mOpenCvCameraView.getCallbackBufferSize();
            byte[] buffer = new byte[bufferSize];
            mOpenCvCameraView.addCallbackBuffer(buffer);
        }
    }

    private void log(String message, Object... values)
    {
        Log.d(TAG, String.format(message, values));
    }

}
