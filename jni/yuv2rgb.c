#include <jni.h>
#include <android/log.h>

#define  LOG_TAG    "YUV2RGB"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


//*******************************************
// YUV420をRGBに変換する
// データフォーマットは、最初に画面サイズ(Width*Height)分のY値が並び、
// 以降は、横方向、縦方向共に、V,Uの順番に2画素分を示して並ぶ
//
// 4×3ドットがあったとすると、YUV420のデータは
//  0 1 2 3
// 0○○○○　Y00 Y01 Y02 Y03 Y10 Y11 Y12 Y13 Y20 Y21 Y22 Y23 V00 U00 V02 U02 V20 U20 V22 U22 となる。
// 1○○○○　V00はY00,Y01,Y10,Y11の4ピクセルの赤色差を表し、U00はY00,Y01,Y10,Y11の4ピクセルの青色差を表す
// 2○○○○
//
// width×heightの画像から (offsetX,offsetY)座標を左上座標としたgetWidth,GetHeightサイズのrgb画像を取得する
//*******************************************
jint
Java_com_luaridaworks_cameraviewpitcher_SmartWatchCameraView_yuv2rgb( JNIEnv* env, jobject thiz,
												jintArray int_rgb, jbyteArray yuv420sp, jint width, jint height,
	 											jint offsetX, jint offsetY, jint getWidth, jint getHeight )
{
	//配列のポインタを受け取る
	jint* rgbp=(*env)->GetIntArrayElements( env, int_rgb, 0 );  
	jbyte* yuvp=(*env)->GetByteArrayElements( env, yuv420sp, 0 );  

	//全体ピクセル数を求める
	long frameSize = width * height;
	int uvp, y;
	int y1164, r, g, b;
	int i, j, yp;
	int u = 0;
	int v = 0;
	int uvs = 0;

	if(offsetY+getHeight>height){
		getHeight = height - offsetY;
	}

	if(offsetX+getWidth>width){
		getWidth = width - offsetX;
	}

	int qp = 0;	//rgb配列番号
	for ( j = offsetY; j < offsetY + getHeight; j++) {
		//1ライン毎の処理
		uvp = frameSize + (j >> 1) * width;

		//offsetXが奇数の場合は、1つ前のU,Vの値を取得する
		if((offsetX & 1)!=0){
			uvs = uvp + offsetX-1;
			// VとUのデータは、2つに1つしか存在しない。よって、iが偶数のときに読み出す
			v = (0xff & yuvp[uvs]) - 128;		//無彩色(色差0)が128なので、128を引く
			u = (0xff & yuvp[uvs + 1]) - 128;		//無彩色(色差0)が128なので、128を引く
		}

		for (i = offsetX; i < offsetX + getWidth; i++) {

			yp = j*width + i;

			//左からピクセル単位の処理
			y = (0xff & ((int) yuvp[yp])) - 16;		//Yの下限が16だから、16を引きます
			if (y < 0){
				y = 0;
			}

			if ((i & 1) == 0) {
				uvs = uvp + i;
				// VとUのデータは、2つに1つしか存在しない。よって、iが偶数のときに読み出す
				v = (0xff & yuvp[uvs]) - 128;		//無彩色(色差0)が128なので、128を引く
				u = (0xff & yuvp[uvs + 1]) - 128;		//無彩色(色差0)が128なので、128を引く
			}

			//変換の計算式によりR,G,Bを求める(Cb=U, Cr=V)
			// R = 1.164(Y-16)                 + 1.596(Cr-128)
			// G = 1.164(Y-16) - 0.391(Cb-128) - 0.813(Cr-128)
			// B = 1.164(Y-16) + 2.018(Cb-128)
			y1164 = 1164 * y;
			r = (y1164 + 1596 * v);
			g = (y1164 - 391 * u - 813 * v);
			b = (y1164 + 2018 * u);

			if (r < 0){
				r = 0;
			}
			else if (r > 262143){
				r = 262143;
			}
			
			if (g < 0){
				g = 0;
			}
			else if (g > 262143){
				g = 262143;
			}
			
			if (b < 0){
				b = 0;
			}
			else if (b > 262143){
				b = 262143;
			}

			rgbp[qp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			qp++;
		}
	}
	//(*env)->SetIntArrayRegion(env, int_rgb, 0, qp, rgbp);
	
	//開放する
	(*env)->ReleaseIntArrayElements(env, int_rgb, rgbp, 0);
	(*env)->ReleaseByteArrayElements(env, yuv420sp, yuvp, 0);

	LOGI("Tranport Finish.");
	return 0;
}

//*******************************************
// サイズを半分にします
//*******************************************
jint
Java_com_luaridaworks_cameraviewpitcher_SmartWatchCameraView_halfsize( JNIEnv* env, jobject thiz,
												jintArray int_rgb, jint width, jint height )
{
	//配列のポインタを受け取る
	jint* rgbp=(*env)->GetIntArrayElements( env, int_rgb, 0 );  

	int x,y;
	int i=0;
	for( y=0; y<height; y+=2 ){
		for( x=0; x<width; x+=2 ){
			rgbp[i] = rgbp[x + y * width ];
			i++;
		}
	}

	//開放する
	(*env)->ReleaseIntArrayElements(env, int_rgb, rgbp, 0);
	LOGI("Shurink Finish.");
	return 0;
}