package com.microsoft.testviewer;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class ARActivity extends Activity {
	private static final int UPDATE_FPS = 0;
	ARView mARView;
	Button mTrackButton;
	Button mCaptureButton;
	Button mFPSButton;
	Timer mTimer;
	long mLastFPSTime = 0;
	long mLastFrameCount = 0;
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_FPS:
				long delay = System.currentTimeMillis() - mLastFPSTime;
				// If last FPS was calculated more than 1 second ago
				if (delay > 1000) {
					long currentFrameCount = mARView.getFrameCount();
					double fps = (((double) currentFrameCount - mLastFrameCount) / delay) * 1000;
					mLastFrameCount = currentFrameCount;
					mLastFPSTime = System.currentTimeMillis();
					mFPSButton.setText("FPS: " + String.format("%.2f", fps));
				}
				break;
			}
		}
	};

	protected void onStop() {
		super.onStop();
	}

	protected void onPause() {
		super.onPause();
		mARView.releaseCamera();
		mARView.stopTracker();
	}

	protected void onResume() {
		super.onResume();
		mARView.openCamera(3);
		mARView.startTracker();
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.aractivity);
		mARView = (ARView) findViewById(R.id.arview_surface);
		mTrackButton = (Button) findViewById(R.id.button_track);
		mTrackButton.setOnClickListener(mTrackHandler);
		mCaptureButton = (Button) findViewById(R.id.button_capture);
		mCaptureButton.setOnClickListener(mCaptureHandler);
		mFPSButton = (Button) findViewById(R.id.button_ar_fps);
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Message.obtain(mHandler, UPDATE_FPS, "").sendToTarget();
			}

		}, 0, 1000);
	}

	private View.OnClickListener mCaptureHandler = new View.OnClickListener() {
		public void onClick(View arg0) {
			mARView.saveImage();
		}
	};

	private View.OnClickListener mTrackHandler = new View.OnClickListener() {
		public void onClick(View arg0) {
			mARView.initTrack();
		}
	};
}
