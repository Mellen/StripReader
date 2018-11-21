package com.lateralimaging.stripreader;

import com.lateralimaging.stripreader.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.Timer;
import java.util.TimerTask;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class StripMaskConfiguration extends Activity implements CameraBridgeViewBase.CvCameraViewListener2
{
    private static String TAG = "StripMaskConfig";

    private final int UP = 1;
    private final int DOWN = 2;
    private final int LEFT = 3;
    private final int RIGHT = 4;

    OpenCVCameraView mOpenCvCameraView;
    StripMaskView mStripMask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_strip_mask_configuration);

        mOpenCvCameraView = (OpenCVCameraView)findViewById(R.id.camera_config_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mStripMask = (StripMaskView)findViewById(R.id.camera_config_mask);
        mStripMask.setPatientLabel(getString(R.string.patient));
        mStripMask.setControlLabel(getString(R.string.control));

        Button up = (Button)findViewById(R.id.btnUp);
        up.setOnClickListener(createDirectionListener(UP));

        Button down = (Button)findViewById(R.id.btnDown);
        down.setOnClickListener(createDirectionListener(DOWN));

        Button left = (Button)findViewById(R.id.btnLeft);
        left.setOnClickListener(createDirectionListener(LEFT));

        Button right = (Button)findViewById(R.id.btnRight);
        right.setOnClickListener(createDirectionListener(RIGHT));
    }

    private View.OnClickListener createDirectionListener(int direction)
    {
        View.OnClickListener listener = null;

        switch (direction)
        {
            case UP:
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        moveMask(0, -1);
                    }
                };
                break;
            case DOWN:
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        moveMask(0, 1);
                    }
                };
                break;
            case LEFT:
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        moveMask(-1, 0);
                    }
                };
                break;
            case RIGHT:
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        moveMask(1, 0);
                    }
                };
                break;
            default:
                break;
        }

        return listener;
    }

    private void moveMask(int x, int y) {
        Point xy = StripMaskView.getOffsets();

        StripMaskView.setOffsets(xy.x + x, xy.y + y);

        mStripMask.invalidate();
    }

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

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
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
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            log("Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        } else {
            log("OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            mOpenCvCameraView.turnOnFlash();
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

    private void log(String message, Object... values) {
        Log.d(TAG, String.format(message, values));
    }

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

    @Override
    protected void onStop()
    {
        super.onStop();

        SharedPreferences settings = getSharedPreferences(Start.PREFS, 0);
        SharedPreferences.Editor edit = settings.edit();

        StripMaskView.saveOffsets(edit);
    }
}
