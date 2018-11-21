package com.lateralimaging.stripreader;

import android.graphics.Point;

public class StripDataCoordinates
{
    public final Point mPatientTopLeft;
    public final Point mPatientBottomRight;
    public final Point mControlTopLeft;
    public final Point mControlBottomRight;



    public StripDataCoordinates(Point patientTopLeft, Point patientBottomRight,
                                Point controlTopLeft, Point controlBottomRight)
    {
        mPatientTopLeft = patientTopLeft;
        mPatientBottomRight = patientBottomRight;
        mControlTopLeft = controlTopLeft;
        mControlBottomRight = controlBottomRight;

    }
}
