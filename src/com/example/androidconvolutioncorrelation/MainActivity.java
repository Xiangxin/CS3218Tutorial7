package com.example.androidconvolutioncorrelation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getSimpleName();
	TextView tv1, tv2;
	ProgressDialog mDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.activity_main);
		tv1 = (TextView) findViewById(R.id.btn_q1q3);
		tv2 = (TextView) findViewById(R.id.btn_q2q4);
		
		tv1.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// spin();
				Intent intent = new Intent(MainActivity.this, ActivityQ1Q3.class);
				startActivity(intent);
			}
		});
		
		tv2.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// spin();
				Intent intent = new Intent(MainActivity.this, ActivityQ2Q4.class);
				startActivity(intent);
			}
		});
	}

	private void spin() {
		if(mDialog == null) {
			mDialog = new ProgressDialog(MainActivity.this);
	        mDialog.setMessage("Loading...");
	        mDialog.setCancelable(false);
		}
        mDialog.show();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Log.i(TAG, "onStop");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
