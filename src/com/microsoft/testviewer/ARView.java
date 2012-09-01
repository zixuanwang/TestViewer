package com.microsoft.testviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

public class ARView extends ARViewBase {
	private int mFrameSize;
	private Bitmap mBitmap;
	private int[] mRGBA;

	public ARView(Context context) {
		super(context);
	}

	public ARView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ARView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onPreviewStarted(int previewWidtd, int previewHeight) {
		mFrameSize = previewWidtd * previewHeight;
		mRGBA = new int[mFrameSize];
		mBitmap = Bitmap.createBitmap(previewWidtd, previewHeight,
				Bitmap.Config.ARGB_8888);
	}

	@Override
	protected void onPreviewStopped() {
		if (mBitmap != null) {
			mBitmap.recycle();
			mBitmap = null;
		}
		mRGBA = null;
	}

	@Override
	protected Bitmap processFrame(byte[] data, long timestamp) {
		int[] rgba = mRGBA;
		processFrame(getFrameWidth(), getFrameHeight(), data, rgba, timestamp);
		Bitmap bmp = mBitmap;
		bmp.setPixels(rgba, 0, getFrameWidth(), 0, 0, getFrameWidth(),
				getFrameHeight());
		return bmp;
	}

	public native void processFrame(int width, int height, byte yuv[],
			int bgra[], long timestamp);
	
	public native void startTracker();
	
	public native void stopTracker();
	
	public native void initTrack();

	static {
		System.loadLibrary("native_sample");
	}
}
