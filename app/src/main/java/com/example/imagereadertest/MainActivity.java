package com.example.imagereadertest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final int REQUEST_CODE = 100;
    public MediaProjectionManager mProjectionManager;

    private static int IMAGES_PRODUCED;
    private static final String SCREENCAP_NAME = "screencap";

    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;


    ImageView image;
    TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = findViewById(R.id.image);
        textView = findViewById(R.id.start_time);

        //存储权限
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //没有获取权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "勾选了不再提醒，需要跳转到设置页面", Toast.LENGTH_SHORT).show();
            }
        }





    }

    public void get(View view) {
        Log.d("start_time:", String.valueOf(System.currentTimeMillis()));
        getMediaProjectionManger();
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });*/


    }

    public void getMediaProjectionManger() {
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mProjectionManager != null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            mDensity = metrics.densityDpi;

            WindowManager wm = (WindowManager) getBaseContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            mWidth= size.x;
            mHeight= size.y;
            Log.d(TAG,"width:"+mWidth+"   height："+mHeight);

            // start capture reader
            //mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGB_8888, 2);
            mImageReader = ImageReader.newInstance(mWidth, mHeight, 0x01, 2);
            //mVirtualDisplay = mMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, null);
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
            mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //已经获取了权限了，执行相应的操作
            } else {
                //权限被拒绝，可以弹出自定义弹窗提示用户，权限被拒绝不能使用某些功能
            }
        }
    }


    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireLatestImage()) {
                if (image != null) {
                    String name = String.valueOf(System.currentTimeMillis());
                    IMAGES_PRODUCED++;
                    Log.e("captured image: ", String.valueOf(IMAGES_PRODUCED));

                    if (IMAGES_PRODUCED%10 == 0){
                        saveJpeg(image,name);
                    }
                    image.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    private void saveJpeg(Image image,String name) {
/*        new Thread(new Runnable() {
            @Override
            public void run() {


            }
        }).start();*/

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * mWidth;

        Bitmap bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        ImageSaveUtil.saveBitmap2file(bitmap,getApplicationContext(),name);

    }


}
