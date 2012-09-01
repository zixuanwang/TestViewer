package com.microsoft.testviewer;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
	public native void processFrame(int width, int height, byte yuv[],
			long timestamp);

	public native void startFrameSaver();

	public native void stopFrameSaver();

	public native void startSensor();

	public native void stopSensor();

	static {
		System.loadLibrary("native_sample");
	}
	private static final int UPDATE_FPS = 0;
	private static final String TAG = "MainActivity";
	private Preview mPreview;
	private MyGLSurfaceView mGLView;
	private Button mStartButton;
	private Button mStopButton;
	private Button mClearButton;
	private Button mFPSButton;
	private Button mToggleGLButton;// the button to toggle the GL layer.
	private FrameLayout mFrameLayout;
	private boolean mGLEnabled = true;
	private Camera mCamera;
	private byte[] mBuffer = new byte[497664];// hard coding here
	private byte[] mFrame = new byte[497664];
	private long mCurrentTimestamp;
	private volatile int mFrameCounter = 0;
	private long mLastFpsTime = 0L;
	private double mFPS;
	private Timer mTimer;
	private boolean mSavingThreadRunning = false;
	private Thread mThread;
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_FPS:
				long delay = System.currentTimeMillis() - mLastFpsTime;
				// If last FPS was calculated more than 1 second ago
				if (delay > 1000) {
					mFPS = (((double) mFrameCounter) / delay) * 1000;
					mFrameCounter = 0;
					mLastFpsTime = System.currentTimeMillis();
				}
				mFPSButton.setText("FPS: " + String.format("%.2f", mFPS));
				break;
			}
		}
	};
	private Runnable mRunnable = new Runnable() {
		public void run() {
			while (true) {
				synchronized (MainActivity.this) {
					try {
						MainActivity.this.wait();
						processFrame(mPreview.getFrameWidth(),
								mPreview.getFrameHeight(), mFrame,
								mCurrentTimestamp);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	};

	private View.OnClickListener mToggleGLHandler = new View.OnClickListener() {
		public void onClick(View arg0) {
			if (!mGLEnabled) {
				mFrameLayout.addView(mGLView);
				mGLEnabled = true;
				mToggleGLButton.setText("DISABLE GL");
			} else {
				((ViewGroup) mGLView.getParent()).removeView(mGLView);
				// mFrameLayout.removeView(mGLView);
				mGLEnabled = false;
				mToggleGLButton.setText("ENABLE GL");
			}
		}
	};

	private View.OnClickListener mStartHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mSavingThreadRunning = true;
			startSensor();
		}
	};

	private View.OnClickListener mStopHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mSavingThreadRunning = false;
			stopSensor();
			startFrameSaver();
		}
	};

	private View.OnClickListener mClearHandler = new View.OnClickListener() {
		public void onClick(View v) {
			new DeleteFilesTask().execute("/mnt/sdcard/tmp");
		}
	};

	private class DeleteFilesTask extends AsyncTask<String, Integer, Long> {
		protected Long doInBackground(String... folders) {
			File imageDir = new File(folders[0]);
			String[] children = imageDir.list();
			for (int i = 0; i < children.length; i++) {
				new File(imageDir, children[i]).delete();
			}
			return 0L;
		}

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(Long result) {
			Toast.makeText(MainActivity.this, "All files are cleared",
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// setContentView(R.layout.activity_main);
		// mGLView = (MyGLSurfaceView) findViewById(R.id.gl_surface);
		// mPreview = (Preview) findViewById(R.id.camera_surface);
		// mStartButton = (Button) findViewById(R.id.button_start);
		// mStopButton = (Button) findViewById(R.id.button_stop);
		// mClearButton = (Button) findViewById(R.id.button_clear);
		// mFPSButton = (Button) findViewById(R.id.button_fps);
		// mStartButton.setOnClickListener(mStartHandler);
		// mStopButton.setOnClickListener(mStopHandler);
		// mClearButton.setOnClickListener(mClearHandler);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		LinearLayout ll = new LinearLayout(this);
		ll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mFPSButton = new Button(this);
		mFPSButton.setTextColor(Color.WHITE);
		mToggleGLButton = new Button(this);
		mToggleGLButton.setText("DISABLE GL");
		mToggleGLButton.setTextColor(Color.WHITE);
		mToggleGLButton.setOnClickListener(mToggleGLHandler);
		ll.addView(mFPSButton);
		ll.addView(mToggleGLButton);
		mGLView = new MyGLSurfaceView(this);
		mPreview = new Preview(this);
		mFrameLayout = new FrameLayout(this);
		mFrameLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mFrameLayout.addView(mGLView);
		mFrameLayout.addView(mPreview);
		mFrameLayout.addView(ll);
		setContentView(mFrameLayout);
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Message.obtain(mHandler, UPDATE_FPS, "").sendToTarget();
			}

		}, 0, 1000);
		mThread = new Thread(mRunnable);
		mThread.start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		openCamera(2);
		mPreview.setCamera(mCamera);
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseCamera();
	}

	public void releaseCamera() {
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	public void openCamera(int cameraId) {
		releaseCamera();
		mCamera = Camera.open(cameraId);
		mCamera.addCallbackBuffer(mBuffer);
		mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				if (mSavingThreadRunning) {
					long currentTimestamp = System.currentTimeMillis();
					synchronized (MainActivity.this) {
						mCurrentTimestamp = currentTimestamp;
						System.arraycopy(data, 0, mFrame, 0, data.length);
						MainActivity.this.notify();
					}
				}
				++mFrameCounter;
				camera.addCallbackBuffer(mBuffer);
			}
		});
	}
}
