package com.luaridaworks.cameraviewpitcher;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class SmartWatchCameraView extends SurfaceView implements SurfaceHolder.Callback {

	public static final String LOG_TAG = "SmartWatchCameraView";
	public static boolean SurfaceCreateFlag = false;

	private int surWidth = -1;
	private int surHeight = -1;
	private Matrix matrix90 = new Matrix();			//90度回転用
	private int[] rgb_bitmap = new int[256 * 256];	//画像切り出しよう
	private long waittiming = 0;
	private Bitmap cameraBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.RGB_565);
	private Canvas cameraCanvas = new Canvas(cameraBitmap);

	private Context context;
	private SurfaceHolder mholder;
	private Camera camera;

	public native int yuv2rgb(int[] int_rgb, byte[] yuv420sp, int width, int height, int offsetX, int offsetY, int getWidth, int getHeight);
	public native int halfsize(int[] int_rgb, int width, int height);
    static {	System.loadLibrary("yuv2rgb_module");	}

	//*******************************************
	// コンストラクタ
	//*******************************************
	SmartWatchCameraView(Context context) {
		super(context);
		
		this.context = context;
		mholder = getHolder();
		mholder.addCallback(this);
		mholder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		//横向き画面固定する
		((Activity)getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		//90度回転用
		matrix90.postRotate(90);
	}

	//*******************************************
	// サーフェイスの生成
	//*******************************************
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (camera == null) {
			try {
				camera = Camera.open();
			} catch (RuntimeException e) {
				((Activity)context).finish();
				Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			camera.release();
			camera = null;
			((Activity)context).finish();
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		SurfaceCreateFlag = true;
	}

	//*******************************************
	// サーフェイスの破壊
	//*******************************************
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
		SurfaceCreateFlag = false;
	}
	
	//*******************************************
	// 画面サイズ変更イベント
	//*******************************************
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(LOG_TAG, "surfaceChanged");

		if (camera == null) {
			((Activity)context).finish();
			return;
		}

		//画面が切り替わったのでストップする
		camera.stopPreview();

		//プレビューCallbackを一応nullにする。
		camera.setPreviewCallback(null);

		//プレビュ画面のサイズ設定
		Log.d(LOG_TAG, "Width= " + width + " Height= " + height);
		surWidth = width;
		surHeight = height;
		setPictureFormat(format);
		setPreviewSize(surWidth, surHeight);

		//コールバックを再定義する
		camera.setPreviewCallback(_previewCallback);

		//プレビュスタート
		camera.startPreview();
	}

	//*******************************************
	// カメラ画像フォーマットの設定
	//*******************************************
	private void setPictureFormat(int format) {
		try {
			Camera.Parameters params = camera.getParameters();
			List<Integer> supported = params.getSupportedPictureFormats();
			if (supported != null) {
				for (int f : supported) {
					if (f == format) {
						params.setPreviewFormat(format);
						camera.setParameters(params);
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//*******************************************
	// カメラ画像サイズの設定
	//*******************************************
	private void setPreviewSize(int width, int height) {
		Camera.Parameters params = camera.getParameters();
		List<Camera.Size> supported = params.getSupportedPreviewSizes();
		if (supported != null) {
			for (Camera.Size size : supported) {
				if (size.width <= width && size.height <= height) {
					params.setPreviewSize(size.width, size.height);
					camera.setParameters(params);
					break;
				}
			}
		}
	}
	
	//*******************************************
	// フレームデータを取得するためのプレビューコールバック
	//*******************************************
	private final Camera.PreviewCallback _previewCallback =
			new Camera.PreviewCallback() {

		//*******************************************
		// dataは YUV420は 1画素が12ビット
		//*******************************************
		public void onPreviewFrame(byte[] data, Camera backcamera) {
			//Log.d(LOG_TAG, "_previewCallback data.length=" + data.length + " data=" + data);

			if (camera == null) { 	return; }	//カメラが死んだ時用のブロック

			//プレビュを一時止める
			camera.stopPreview();

			//一応コールバックをnullにする
			camera.setPreviewCallback(null);

			//YUV420からRGBに変換しつつ画像中心の256×256エリアを切り出す
			yuv2rgb(rgb_bitmap, data, surWidth, surHeight, surWidth/2-128, surHeight/2-128, 256, 256);
			
			//半分のサイズに圧縮する
			halfsize(rgb_bitmap, 256, 256);
			
			//ARGBデータをcameraBitmapに転送する
			cameraCanvas.drawBitmap(rgb_bitmap, 0, 128, 0, 0, 128, 128, false, null);

			//画像を90゜回転する
			Bitmap angle90Bitmap = Bitmap.createBitmap(cameraBitmap, 0, 0, 128, 128, matrix90, true);

			//intentを出す
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        angle90Bitmap.compress(CompressFormat.PNG, 100, baos);
	        byte[] bytebmp = baos.toByteArray();

	        Intent intent = new Intent("com.luaridaworks.extras.BITMAP_SEND");
	        intent.putExtra("BITMAP", bytebmp);
	        getContext().sendBroadcast(intent);
			
	        //5fpsとなるように待っている
	        while(waittiming>System.currentTimeMillis()){}
	        waittiming = System.currentTimeMillis() + 200L;

			if (camera == null) {
				return;
			}
			else{
				//コールバックを再セットする
				camera.setPreviewCallback(_previewCallback);

				//プレビューを開始する
				camera.startPreview();
			}
		}
	};

}
