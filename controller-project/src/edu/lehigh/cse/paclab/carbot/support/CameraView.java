package edu.lehigh.cse.paclab.carbot.support;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public class CameraView extends CameraViewBase{
	public static final int VIEW_MODE_RGBA = 0;
	
	private Mat mYuv;
	private Mat mRgba;
	private Mat mIntermediateMat;
	
	private int mViewMode;
	private Bitmap mBitmap;
	
	public CameraView (Context context) {
		super(context);
	}

	@Override
	protected void onPreviewStarted(int previewWidth, int previewHeight) {
		// initialize Mats
		mYuv = new Mat(getFrameHeight() + getFrameHeight()/2, getFrameWidth(), CvType.CV_8UC1);
		
		mRgba = new Mat();
		mIntermediateMat = new Mat();
		
		mBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
	}

	@Override
	protected void onPreviewStopped() {
		
		if (mBitmap != null) {
			mBitmap.recycle();
			mBitmap = null;
		}
		
		// Explicitly deallocate Mats
        if (mYuv != null)
            mYuv.release();
        if (mRgba != null)
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mYuv = null;
        mRgba = null;
        mIntermediateMat = null;
		
	}
	@Override
    protected Bitmap processFrame(byte[] data) {
        mYuv.put(0, 0, data);

        final int viewMode = mViewMode;

        switch (viewMode) {
        case VIEW_MODE_RGBA:
            Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4);
            break;
        }

        Bitmap bmp = mBitmap;

        try {
            Utils.matToBitmap(mRgba, bmp);
        } catch(Exception e) {
            Log.e("org.opencv.samples.puzzle15", "Utils.matToBitmap() throws an exception: " + e.getMessage());
            bmp.recycle();
            bmp = null;
        }

        return bmp;
    }
	
    public void setViewMode(int viewMode) {
		mViewMode = viewMode;
    }

}