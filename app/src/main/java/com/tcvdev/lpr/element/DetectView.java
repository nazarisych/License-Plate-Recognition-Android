package com.tcvdev.lpr.element;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.tcvdev.lpr.model.LPRResultData;
import com.crashlytics.android.Crashlytics;
import com.tcvdev.lpr.R;
import com.tcvdev.lpr.common.Util;


/**
 * Created by Pinky on 12/11/2017.
 */

public class DetectView extends View {


    private Paint mRectPaint;
    private Paint mTextNumberPaint;
    private Paint mTextConfPaint;
    private Paint mNumberBGPaint;
    private Paint mConfBGPaint;


    private LPRResultData mLprResultData;
    private int m_nImgWidth;
    private int m_nImgHeight;
    private int m_nCntPlate;
    private Context mContext;

    public DetectView(Context context) {
        super(context);
        this.mContext = context;
        this.m_nCntPlate = 0;
    }

    public DetectView(Context context,  AttributeSet attrs) {
        super(context, attrs);

        this.mContext = context;
        this.m_nCntPlate = 0;

        mRectPaint = new Paint();
        mRectPaint.setColor(context.getResources().getColor(R.color.colorRect));
        mRectPaint.setStrokeWidth(4);
        mRectPaint.setAntiAlias(true);
        mRectPaint.setStyle(Paint.Style.STROKE);

        mTextNumberPaint = new Paint();
        mTextNumberPaint.setTextSize(50);
        mTextNumberPaint.setColor(context.getResources().getColor(R.color.colorText));

        mTextConfPaint = new Paint();
        mTextConfPaint.setTextSize(30);
        mTextConfPaint.setColor(context.getResources().getColor(R.color.colorText));

        mNumberBGPaint = new Paint();
        mNumberBGPaint.setColor(context.getResources().getColor(R.color.colorTextBG));
        mNumberBGPaint.setAntiAlias(true);
        mNumberBGPaint.setStyle(Paint.Style.FILL);
    }

    public DetectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        try {
            if (mLprResultData != null && mLprResultData.plateData != null) {

                int cnt = mLprResultData.nPlateNum;

                float scaleX = (float) width / m_nImgWidth;
                float scaleY = (float) height / m_nImgHeight;
                float scale = Math.min(scaleX, scaleY);

                int offsetLeft = (int) (width - m_nImgWidth * scale) / 2;
                int offsetTop = (int) (height - m_nImgHeight * scale) / 2;

                for (int i = 0; i < cnt; i++) {

                    LPRResultData.OnePlateData plateData = mLprResultData.plateData[i];
                    if (plateData != null) {

                        RectF rectPlate = new RectF(mLprResultData.plateData[i].lprRect.left * scale, mLprResultData.plateData[i].lprRect.top * scale,
                                mLprResultData.plateData[i].lprRect.right * scale, mLprResultData.plateData[i].lprRect.bottom * scale);

                        rectPlate.offset(offsetLeft, offsetTop);
                        canvas.drawRect(rectPlate, mRectPaint);

                        String strResult = Util.getValidString(mLprResultData.plateData[i].lprStr, 20);

                        Rect rectNumberBound = Util.getTextBounds(mTextNumberPaint, strResult);
                        int textPadding = 20;
                        int offset = 50;
                        RectF rectNumberBG = new RectF(rectPlate.left, rectPlate.bottom + offset,
                                rectPlate.left + rectNumberBound.width() + textPadding * 2, rectPlate.bottom + offset + rectNumberBound.height() + textPadding * 2);

                        canvas.drawRect(rectNumberBG, mNumberBGPaint);
                        canvas.drawText(strResult, rectNumberBG.left + textPadding, rectNumberBG.bottom - textPadding, mTextNumberPaint);

                        // Draw Conf;
                        String strConf = String.format("Conf: %.3f%%", plateData.conf);
                        Rect rectConfBound = Util.getTextBounds(mTextConfPaint, strConf);
                        RectF rectConfBG = new RectF(rectPlate.left, rectPlate.top - offset - rectConfBound.height() - textPadding * 2,
                                rectPlate.left + rectConfBound.width() + textPadding * 2, rectPlate.top - offset);

                        canvas.drawRect(rectConfBG, mNumberBGPaint);
                        canvas.drawText(strConf, rectConfBG.left + textPadding, rectConfBG.bottom - textPadding, mTextConfPaint);

                    }
                }
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            Crashlytics.logException(ex);
            Crashlytics.log(1, "Pinky", "NullPointException");
        }

    }

    public void setLPRResult(LPRResultData lprResultData) {

        this.mLprResultData = lprResultData;



        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                invalidate();
            }
        });
    }

    public void setImageSize(int imgWidth, int imgHeight) {

        this.m_nImgWidth = imgWidth;
        this.m_nImgHeight = imgHeight;
    }


}
