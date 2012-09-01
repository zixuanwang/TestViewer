package com.microsoft.testviewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class ARViewBase extends SurfaceView implements
		SurfaceHolder.Callback, Runnable {
	private static final String TAG = "ARViewBase";
	private Camera mCamera;
	private SurfaceHolder mHolder;
	private int mFrameWidth;
	private int mFrameHeight;
	private int mFrameFormat;
	private int mBufferSize;
	private byte[] mFrame;
	private byte[] mBuffer;
	private boolean mThreadRun;
	private long mCurrentTimestamp;
	private volatile long mFrameCounter = 0;

	public ARViewBase(Context context) {
		super(context);
		init();
	}

	public ARViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ARViewBase(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public void init() {
		mHolder = getHolder();
		mHolder.addCallback(this);
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	public int getFrameWidth() {
		return mFrameWidth;
	}

	public int getFrameHeight() {
		return mFrameHeight;
	}

	public long getFrameCount() {
		return mFrameCounter;
	}

	public void setPreview() throws IOException {
		mCamera.setPreviewDisplay(null);
	}

	public boolean openCamera(int cameraId) {
		Log.i(TAG, "openCamera");
		releaseCamera();
		mCamera = Camera.open(cameraId);
		if (mCamera == null) {
			Log.e(TAG, "Can't open camera!");
			return false;
		}
		mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				synchronized (ARViewBase.this) {
					mCurrentTimestamp = System.currentTimeMillis();
					System.arraycopy(data, 0, mFrame, 0, data.length);
					++mFrameCounter;
					ARViewBase.this.notify();
				}
				camera.addCallbackBuffer(mBuffer);
			}
		});
		return true;
	}

	public void releaseCamera() {
		Log.i(TAG, "releaseCamera");
		mThreadRun = false;
		synchronized (this) {
			if (mCamera != null) {
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
			}
		}
		onPreviewStopped();
	}

	public void setupCamera(int width, int height) {
		Log.i(TAG, "setupCamera");
		synchronized (this) {
			if (mCamera != null) {
				Camera.Parameters params = mCamera.getParameters();
				List<Camera.Size> sizes = params.getSupportedPreviewSizes();
				mFrameWidth = width;
				mFrameHeight = height;
				mFrameFormat = params.getPreviewFormat();
				int minDiff = Integer.MAX_VALUE;
				for (Camera.Size size : sizes) {
					if (Math.abs(size.width - width) < minDiff) {
						mFrameWidth = size.width;
						mFrameHeight = size.height;
						minDiff = Math.abs(size.width - width);
					}
				}
				params.setPreviewSize(getFrameWidth(), getFrameHeight());
				mCamera.setParameters(params);
				params = mCamera.getParameters();
				mBufferSize = params.getPreviewSize().width
						* params.getPreviewSize().height;
				mBufferSize = mBufferSize
						* ImageFormat
								.getBitsPerPixel(params.getPreviewFormat()) / 8;
				mBuffer = new byte[mBufferSize];
				mFrame = new byte[mBufferSize];
				mCamera.addCallbackBuffer(mBuffer);

				try {
					setPreview();
				} catch (IOException e) {
					Log.e(TAG,
							"mCamera.setPreviewDisplay/setPreviewTexture fails: "
									+ e);
				}
				/*
				 * Notify that the preview is about to be started and deliver
				 * preview size
				 */
				onPreviewStarted(params.getPreviewSize().width,
						params.getPreviewSize().height);
				/* Now we can start a preview */
				mCamera.startPreview();
			}
		}
	}

	public void surfaceChanged(SurfaceHolder _holder, int format, int width,
			int height) {
		Log.i(TAG, "surfaceChanged");
		setupCamera(width, height);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated");
		(new Thread(this)).start();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyed");
		releaseCamera();
	}

	/*
	 * The bitmap returned by this method shall be owned by the child and
	 * released in onPreviewStopped()
	 */
	protected abstract Bitmap processFrame(byte[] data, long timestamp);

	/**
	 * This method is called when the preview process is being started. It is
	 * called before the first frame delivered and processFrame is called It is
	 * called with the width and height parameters of the preview process. It
	 * can be used to prepare the data needed during the frame processing.
	 * 
	 * @param previewWidth
	 *            - the width of the preview frames that will be delivered via
	 *            processFrame
	 * @param previewHeight
	 *            - the height of the preview frames that will be delivered via
	 *            processFrame
	 */
	protected abstract void onPreviewStarted(int previewWidtd, int previewHeight);

	/**
	 * This method is called when preview is stopped. When this method is called
	 * the preview stopped and all the processing of frames already completed.
	 * If the Bitmap object returned via processFrame is cached - it is a good
	 * time to recycle it. Any other resources used during the preview can be
	 * released.
	 */
	protected abstract void onPreviewStopped();

	public void run() {
		mThreadRun = true;
		Log.i(TAG, "Starting processing thread");
		while (mThreadRun) {
			Bitmap bmp = null;
			synchronized (this) {
				try {
					this.wait();
					bmp = processFrame(mFrame, mCurrentTimestamp);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (bmp != null) {
				Canvas canvas = mHolder.lockCanvas();
				if (canvas != null) {
					canvas.drawBitmap(bmp,
							(canvas.getWidth() - getFrameWidth()) / 2,
							(canvas.getHeight() - getFrameHeight()) / 2, null);
					mHolder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}

	public void saveImage() {
		try {
			YuvImage image;
			synchronized (ARViewBase.this) {
				image = new YuvImage(mFrame, mFrameFormat, mFrameWidth,
						mFrameHeight, null);
			}
			File file = new File("/mnt/sdcard/tmp/"
					+ System.currentTimeMillis() + ".jpg");
			FileOutputStream filecon = new FileOutputStream(file);
			image.compressToJpeg(
					new Rect(0, 0, image.getWidth(), image.getHeight()), 100,
					filecon);
		} catch (FileNotFoundException e) {
		}
	}
}
