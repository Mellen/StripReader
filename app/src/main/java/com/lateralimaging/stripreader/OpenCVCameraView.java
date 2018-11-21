package com.lateralimaging.stripreader;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import org.opencv.android.JavaCameraView;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by matt on 10/07/15.
 */
public class OpenCVCameraView  extends JavaCameraView implements SurfaceHolder.Callback {

    private static final String TAG = "OpenCVCameraView";

    private int mWinWidth;

    private int mWinHeight;

    public OpenCVCameraView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3)
    {
        super.surfaceChanged(arg0, arg1, arg2, arg3);

        try {
            mCamera.stopPreview();
        }
        catch(Exception e) {
        }

        try
        {
            Context ctx = getContext();
            Display display = ((WindowManager) ctx.getSystemService(ctx.WINDOW_SERVICE)).getDefaultDisplay();

            if (display.getRotation() == Surface.ROTATION_0) {
                mCamera.setDisplayOrientation(90);
            }

            if (display.getRotation() == Surface.ROTATION_270) {
                mCamera.setDisplayOrientation(180);
            }

            if (display.getRotation() == Surface.ROTATION_90) {
                mCamera.setDisplayOrientation(0);
            }

            mCamera.setPreviewDisplay(getHolder());
            mCamera.startPreview();

            setWindowWidthHeight(display.getWidth(), display.getHeight());

            fixCameraSize();
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void setWindowWidthHeight(int width, int height)
    {
        mWinWidth = width;
        mWinHeight = height;
    }

    private void fixCameraSize()
    {
        Camera.Parameters par = mCamera.getParameters();
        par.setPreviewSize(640, 480);
        mCamera.setParameters(par);
    }

    public int getCallbackBufferSize() {
        Camera.Parameters par = mCamera.getParameters();

        int w = par.getPreviewSize().width;
        int h = par.getPreviewSize().height;

        int bitsPerPixel =  ImageFormat.getBitsPerPixel(par.getPreviewFormat());

        return w * h * bitsPerPixel;
    }

    public void addCallbackBuffer(byte[] buffer) {
        mCamera.addCallbackBuffer(buffer);
    }

    public void setPreviewCallbackWithBuffer(Camera.PreviewCallback previewCallback) {
        mCamera.setPreviewCallbackWithBuffer(previewCallback);
    }

    public Camera.Size getPreviewSize() {
        return mCamera.getParameters().getPreviewSize();
    }

    public int getImageFormat()
    {
        return mCamera.getParameters().getPreviewFormat();
    }

    public void autoFocus(Camera.AutoFocusCallback autoFocusCallback) {
        mCamera.autoFocus(autoFocusCallback);
    }

    public void turnOnFlash() {
        if(mCamera != null)
        {
            Camera.Parameters par = mCamera.getParameters();
            par.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(par);
        }
    }

    public void turnOffFlash()
    {
        if(mCamera != null)
        {
            Camera.Parameters par = mCamera.getParameters();
            par.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(par);
        }
    }
}
