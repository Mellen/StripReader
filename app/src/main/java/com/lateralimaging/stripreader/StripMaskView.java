package com.lateralimaging.stripreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by matt on 17/07/15.
 */
public class StripMaskView extends View {

    static final String TAG = "StripMaskView";

    private static double StripHeightToWidthRatio = 0.3/10.0;
    private static double PatientStart = 3.40/10.0;
    private static double PatientEnd = 4.10/10.0;
    private static double ControlStart = 4.45/10.0;
    private static double ControlEnd = 5.20/10.0;
    private static int XOFFSET = -4;
    private static int YOFFSET = 0;

    public static void setOffsets(int newX, int newY)
    {
        XOFFSET = newX;
        YOFFSET = newY;
    }

    public static Point getOffsets()
    {
        return new Point(XOFFSET, YOFFSET);
    }

    private Paint mPaint = new Paint();

    private String mPatientLabel;
    private String mControlLabel;

    public StripMaskView(Context context)
    {
        super(context);
    }

    public StripMaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StripMaskView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPatientLabel(String patientLabel)
    {
        mPatientLabel = patientLabel;
    }

    public void setControlLabel(String controlLabel)
    {
        mControlLabel = controlLabel;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        mPaint.setColor(Color.rgb(255, 255, 255));
        mPaint.setAlpha(196);

        StripDataCoordinates stripDataCoordinates = GetPatientCoordinates(height, width);

        canvas.drawRect(0, 0, stripDataCoordinates.mPatientTopLeft.x, height, mPaint);
        canvas.drawRect(stripDataCoordinates.mPatientBottomRight.x, 0, width, height, mPaint);
        canvas.drawRect(stripDataCoordinates.mControlTopLeft.x, 0, stripDataCoordinates.mControlBottomRight.x, stripDataCoordinates.mControlTopLeft.y, mPaint);
        canvas.drawRect(stripDataCoordinates.mControlTopLeft.x, stripDataCoordinates.mControlBottomRight.y, stripDataCoordinates.mControlBottomRight.x, stripDataCoordinates.mPatientTopLeft.y, mPaint);
        canvas.drawRect(stripDataCoordinates.mPatientTopLeft.x, stripDataCoordinates.mPatientBottomRight.y, stripDataCoordinates.mPatientBottomRight.x, height, mPaint);

        int textSize = (stripDataCoordinates.mPatientBottomRight.y - stripDataCoordinates.mPatientTopLeft.y) / 2;

        int patientTextY = stripDataCoordinates.mPatientBottomRight.y - ((stripDataCoordinates.mPatientBottomRight.y - stripDataCoordinates.mPatientTopLeft.y)/4);
        int controlTextY = stripDataCoordinates.mControlBottomRight.y - ((stripDataCoordinates.mControlBottomRight.y - stripDataCoordinates.mControlTopLeft.y)/4);

        mPaint.setTextSize(textSize);
        mPaint.setAlpha(255);
        mPaint.setColor(Color.BLACK);

        canvas.drawText(mPatientLabel, stripDataCoordinates.mPatientBottomRight.x, patientTextY, mPaint);
        canvas.drawText(mControlLabel, stripDataCoordinates.mControlBottomRight.x, controlTextY, mPaint);
    }

    public StripDataCoordinates GetPatientCoordinates(int height, int width)
    {
        int left = (width / 2) - (int)(width * StripHeightToWidthRatio) + XOFFSET;
        int right =  (width / 2) + (int)(width * StripHeightToWidthRatio) + XOFFSET;
        int patientTop = height - (int)Math.round(height * PatientEnd) + YOFFSET;
        int patientBottom = height - (int)Math.round(height * PatientStart) + YOFFSET;
        int controlTop = height - (int)Math.round(height * ControlEnd) + YOFFSET;
        int controlBottom = height - (int)Math.round(height * ControlStart) + YOFFSET;

        int pHeight = patientBottom - patientTop;

        double pHeightFraction = pHeight/10.0;

        return new StripDataCoordinates(
                new Point(left, (int)(patientTop - pHeightFraction)),
                new Point(right, (int)(patientBottom + pHeightFraction)),
                new Point(left, (int)(controlTop - pHeightFraction)),
                new Point(right, (int)(controlBottom + pHeightFraction))
        );
    }

    public static void saveOffsets(SharedPreferences.Editor edit) {
        edit.putInt("x", XOFFSET);
        edit.putInt("y", YOFFSET);
        edit.commit();
    }
}
