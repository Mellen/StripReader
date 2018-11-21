package com.lateralimaging.stripreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by matt on 13/07/15.
 */
public class DrawableView extends View {

    private Bitmap mPhoto;
    private Bitmap mRefZero;
    private Bitmap mRefOne;
    private Bitmap mRefTwo;
    private Bitmap mRefThree;
    private Bitmap mRefFour;
    private Bitmap mRefFive;
    private Paint mPaint = new Paint();

    public DrawableView(Context context)
    {
        super(context);
    }

    public DrawableView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawableView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPhotos(Bitmap patient, Bitmap refZero, Bitmap refOne, Bitmap refTwo, Bitmap refThree, Bitmap refFour, Bitmap refFive)
    {
        if(mPhoto != null)
        {
            mPhoto.recycle();
            refZero.recycle();
            refOne.recycle();
            refTwo.recycle();
            refThree.recycle();
            refFour.recycle();
            refFive.recycle();
        }
        mPhoto = patient;
        mRefZero = refZero;
        mRefOne = refOne;
        mRefTwo = refTwo;
        mRefThree = refThree;
        mRefFour = refFour;
        mRefFive = refFive;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if(mPhoto != null) {

            canvas.drawBitmap(mPhoto, 0, 0, mPaint);

            canvas.drawBitmap(mRefZero, mPhoto.getWidth(), 0, mPaint);
            canvas.drawBitmap(mRefOne, mPhoto.getWidth(), mRefZero.getHeight(), mPaint);
            canvas.drawBitmap(mRefTwo, mPhoto.getWidth(), mRefZero.getHeight() * 2, mPaint);
            canvas.drawBitmap(mRefThree, mPhoto.getWidth(), mRefZero.getHeight() * 3, mPaint);
            canvas.drawBitmap(mRefFour, mPhoto.getWidth(), mRefZero.getHeight() * 4, mPaint);
            canvas.drawBitmap(mRefFive, mPhoto.getWidth(), mRefZero.getHeight() * 5, mPaint);
        }
    }
}
