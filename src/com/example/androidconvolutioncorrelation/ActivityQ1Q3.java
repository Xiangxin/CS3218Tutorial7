package com.example.androidconvolutioncorrelation;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class ActivityQ1Q3 extends Activity {

	private static final String TAG = ActivityQ1Q3.class.getSimpleName();
	public CSurfaceView surfaceView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.activity_q1q3);
		surfaceView = (CSurfaceView) findViewById(R.id.surfaceView1);
		surfaceView.drawAnimation = true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		surfaceView.startDrawing();
	}
}
