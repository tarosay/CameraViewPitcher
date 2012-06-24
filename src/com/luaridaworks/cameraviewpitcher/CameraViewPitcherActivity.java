package com.luaridaworks.cameraviewpitcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class CameraViewPitcherActivity extends Activity {
	public static final String CONTROL_START_REQUEST_INTENT = "com.sonyericsson.extras.aef.control.START_REQUEST";
	public static final String CONTROL_STOP_REQUEST_INTENT = "com.sonyericsson.extras.aef.control.STOP_REQUEST";
	public static final String EXTRA_AEA_PACKAGE_NAME = "aea_package_name";
	public static final String HOSTAPP_PERMISSION = "com.sonyericsson.extras.liveware.aef.HOSTAPP_PERMISSION";
	public static final String HOST_APP_PACKAGE_NAME = "com.sonyericsson.extras.smartwatch";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    
	@Override
	public void onResume(){
		super.onResume();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		//タイトルを非表示
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		//Bitmapキャッチャを起動する
		//一度停止リクエストを出す
		Intent intent = new Intent(CONTROL_STOP_REQUEST_INTENT);
		intent.putExtra(EXTRA_AEA_PACKAGE_NAME, "com.luaridaworks.smartwatch.bitmapcatcher");
		intent.setPackage(HOST_APP_PACKAGE_NAME);
		this.sendBroadcast(intent, HOSTAPP_PERMISSION);		

		//100ms待つ
		long sTime = System.currentTimeMillis() + 100;
		while(sTime>System.currentTimeMillis());
		
		//そして起動リクエストを出す
		intent = new Intent(CONTROL_START_REQUEST_INTENT);
		intent.putExtra(EXTRA_AEA_PACKAGE_NAME, "com.luaridaworks.smartwatch.bitmapcatcher");
		intent.setPackage(HOST_APP_PACKAGE_NAME);
		this.sendBroadcast(intent, HOSTAPP_PERMISSION);

		setContentView(new SmartWatchCameraView(this));
	}

	@Override
	public void onStop(){
		super.onStop();

		//Bitmapキャッチャを停止する
		Intent intent = new Intent(CONTROL_STOP_REQUEST_INTENT);
		intent.putExtra(EXTRA_AEA_PACKAGE_NAME, "com.luaridaworks.smartwatch.bitmapcatcher");
		intent.setPackage(HOST_APP_PACKAGE_NAME);
		this.sendBroadcast(intent, HOSTAPP_PERMISSION);	
		//finish();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		//Bitmapキャッチャを停止する
		Intent intent = new Intent(CONTROL_STOP_REQUEST_INTENT);
		intent.putExtra(EXTRA_AEA_PACKAGE_NAME, "com.luaridaworks.smartwatch.bitmapcatcher");
		intent.setPackage(HOST_APP_PACKAGE_NAME);
		this.sendBroadcast(intent, HOSTAPP_PERMISSION);	
	}
	
}