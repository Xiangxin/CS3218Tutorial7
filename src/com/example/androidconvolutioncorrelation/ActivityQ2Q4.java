package com.example.androidconvolutioncorrelation;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class ActivityQ2Q4 extends Activity {

	private static final String TAG = ActivityQ2Q4.class.getSimpleName();
	public CSurfaceView surfaceView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.activity_q2q4);
		surfaceView = (CSurfaceView) findViewById(R.id.surfaceView2);
		surfaceView.drawAnimation = false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		surfaceView.startDrawing();
	}
}
