package com.truiton.screencapture;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private MediaProjectionManager mProjectionManager; // нужен
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ToggleButton mToggleButton;
//    private ImageView mImageView;
//    private TextView textView;
    private static final int REQUEST_PERMISSIONS = 10; // нужен
    private String PATH_TO_ENG_DATA_TRAIN;

    private ImageView borderArea;
    private ImageView touchCircle;

    private TextView outCoordinateTextView;

    private Handler handler;
    private Runnable runnable;
    private String extractedText;

//    TimerTask timerTask;

    private int orientation;

//    private ImageView testcpp;

//    int POSITION_X_BORDER_AREA = 0;
//    int POSITION_Y_BORDER_AREA = 0;

    int GLOBAL_POSITION_X_BORDER_AREA = 0;
    int GLOBAL_POSITION_Y_BORDER_AREA = 0;

    int WIDTH_STROKE = 5;

    int WIDTH_BORDER_AREA = 500;
    int HEIGHT_BORDER_AREA = 250;

    int WIDTH_TOUCH_CIRCLE = 50;
    int HEIGHT_TOUCH_CIRCLE = 50;


    private ImageReader _imageReader;
//    String textCoord;
    //private DisplayMetrics metrics;

//    Intent _intent;
//    Intent bubbleIntent;

    String lang;

    TessBaseAPI tessBaseApi;
    GaussaKrugera gaussaKrugera;
    Convertor convertor;
    WGS84 wgs84;
    PZ90 pz90;

//    int requestCode;
//    int resultCode;

//    ServiceGeneral serviseGeneral;

    private Timer mTimer;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        if(orientation != newConfig.orientation)
        {
            orientation = newConfig.orientation;

            if (mMediaProjection != null)
            {
                DisplayMetrics metrics;
                metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

                mTimer.cancel();
                mTimer = null;
                mTimer = new Timer();

                mVirtualDisplay.release();
                mVirtualDisplay = null;

                _imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 1);
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("Capture", metrics.widthPixels, metrics.heightPixels, 2, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, _imageReader.getSurface(), null, null);

                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        TimerTick();
                    }
                };

                mTimer.schedule(timerTask, 1000, 1000);
            }
        }

        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        convertor = new Convertor();
        wgs84 = new WGS84(0, 0, 0);
        pz90 = new PZ90();
        gaussaKrugera = new GaussaKrugera();

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1);
        }

//        int permissionStatusRecordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
//        int permissionStatusWriteExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        int permissionStatusReadExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
//
//        if ((permissionStatusRecordAudio == PackageManager.PERMISSION_DENIED) || (permissionStatusReadExternalStorage == PackageManager.PERMISSION_DENIED) || (permissionStatusWriteExternalStorage == PackageManager.PERMISSION_DENIED)) {
//            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
//        }

        int permissionStatusWriteExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionStatusReadExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if ((permissionStatusReadExternalStorage == PackageManager.PERMISSION_DENIED) || (permissionStatusWriteExternalStorage == PackageManager.PERMISSION_DENIED)) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        }

        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                outCoordinateTextView.setText(extractedText);
            }
        };

//        timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                TimerTick();
//            }
//        };

        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleScreenShare(v);
            }
        });

        lang = "eng";

        Context context = (MainActivity.this);

        File engPath = new File(context.getDir("", Context.MODE_PRIVATE), File.separator + "tessdata");
        engPath.mkdirs();
        File engData = new File(engPath, "eng.traineddata");

        if (!engData.exists())
        {
            try {
                InputStream is = context.getResources().openRawResource(R.raw.eng);

                OutputStream os = new FileOutputStream(engData);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                is.close();
                os.close();
            } catch (IOException e) {
                Log.i(TAG, "face cascade not found");
            }
        }

        PATH_TO_ENG_DATA_TRAIN = context.getDir("", Context.MODE_PRIVATE).getAbsolutePath() + File.separator;

        tessBaseApi = new TessBaseAPI();

        try
        {
            tessBaseApi.init(PATH_TO_ENG_DATA_TRAIN, lang);
        }
        catch (Exception ex)
        {
            Log.e(TAG, ex.getMessage());
        }

        orientation = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getOrientation();

//
//        handler = new Handler();
//
//        runnable = new Runnable() {
//            @Override
//            public void run() {
//                outCoordinateTextView.setText(extractedText);
//            }
//        };
//
//        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
//        startActivityForResult(intent, 1);
//
////        Convertor convertor = new Convertor();
////
////        WGS84 wgs84 = new WGS84(44.89930111, 37.35263611, 0);
////        CK42 ck42 = new CK42();
////        ck42 = convertor.WGS84ToCK42(wgs84);
////
////        GsKr gskr = new GsKr();
////
////        gskr = convertor.CK42ToGsKr(ck42);
////        GsKr gskr2 = Convertor.ToGsKr(wgs84);
//
//
//
//
//
//
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
////        DisplayMetrics metrics;
////        metrics = new DisplayMetrics();
////        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
//
////        tmpRect = new Rect((int)((double)(metrics.widthPixels) * 0.0), (int)((double)(metrics.heightPixels) * 0.5), (int)((double)(metrics.widthPixels) * 1.0), (int)((double)(metrics.heightPixels) * 1.0));
//
//
//        lang = "eng";
//
////        testcpp = (ImageView) findViewById(R.id.imageView4);
//
//        mImageView = (ImageView) findViewById(R.id.imageView);
//        textView = (TextView) findViewById(R.id.textView2);
//
//        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//
////        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
//
//        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
//        mToggleButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
//                {
//                    // хз чё тут должно быть
//                }
//                else
//                    {
//                    onToggleScreenShare(v);
//                }
//            }
//        });
//
//        Context context = (MainActivity.this);
//
//        File engPath = new File(context.getDir("", Context.MODE_PRIVATE), File.separator + "tessdata");
//        engPath.mkdirs();
//        File engData = new File(engPath, "eng.traineddata");
//
//        if (!engData.exists())
//        {
//            try {
//                InputStream is = context.getResources().openRawResource(R.raw.eng);
//
//                OutputStream os = new FileOutputStream(engData);
//
//                byte[] buffer = new byte[4096];
//                int bytesRead;
//                while ((bytesRead = is.read(buffer)) != -1) {
//                    os.write(buffer, 0, bytesRead);
//                }
//
//                is.close();
//                os.close();
//            } catch (IOException e) {
//                Log.i(TAG, "face cascade not found");
//            }
//        }
//
//        PATH_TO_ENG_DATA_TRAIN = context.getDir("", Context.MODE_PRIVATE).getAbsolutePath() + File.separator;
//
//        tessBaseApi = new TessBaseAPI();
//
//        try
//        {
//            tessBaseApi.init(PATH_TO_ENG_DATA_TRAIN, lang);
//        }
//        catch (Exception ex)
//        {
//            Log.e(TAG, ex.getMessage());
//        }
//
////        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
////        testcpp.setImageResource(R.drawable.shape);
//
//        borderArea = new ImageView(this);
//        touchCircle = new ImageView(this);
//        outCoordinateTextView = new TextView(this);
//
//        outCoordinateTextView.setText("Координаты тут");
//        outCoordinateTextView.setTextColor(Color.argb(255, 255, 255, 255));
//        outCoordinateTextView.setTextSize(16);
//        outCoordinateTextView.setBackgroundResource(R.drawable.background_text_view);
//
//
////        borderArea.setLayoutParams(new ViewGroup.LayoutParams(50, 50));
//
////        borderArea.getLayoutParams().height = 150;
////        borderArea.getLayoutParams().width = 150;
//
//        borderArea.setImageResource(R.drawable.shape);
//        touchCircle.setImageResource(R.mipmap.segment);
////        borderArea.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.shape));
////        borderArea.setImageResource(R.mipmap.truiton_short);
////        borderArea.setLayoutParams(new android.view.ViewGroup.LayoutParams(80,60));
//
////        borderArea.setMaxHeight(20);
////        borderArea.setMaxWidth(20);
//
//        int LAYOUT_FLAG;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
//        } else {
//            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
//        }
//
//        final WindowManager.LayoutParams paramsOutCoordinateTextView = new WindowManager.LayoutParams(
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                LAYOUT_FLAG,
////                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//                PixelFormat.TRANSLUCENT);
//        paramsOutCoordinateTextView.gravity = Gravity.CENTER;
//
//        WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
//        windowManager.addView(outCoordinateTextView, paramsOutCoordinateTextView);
//
//        try{
//            //for moving the picture on touch and slide
//            outCoordinateTextView.setOnTouchListener(new View.OnTouchListener() {
//                WindowManager.LayoutParams paramsT = paramsOutCoordinateTextView;
//                private int initialX;
//                private int initialY;
//                private float initialTouchX;
//                private float initialTouchY;
//                private long touchStartTime = 0;
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    //remove face bubble on long press
//                    if(System.currentTimeMillis()-touchStartTime>ViewConfiguration.getLongPressTimeout() && initialTouchX== event.getX()){
//                        windowManager.removeView(outCoordinateTextView);
//                        return false;
//                    }
//                    switch(event.getAction()){
//                        case MotionEvent.ACTION_DOWN:
//                            touchStartTime = System.currentTimeMillis();
//                            initialX = paramsOutCoordinateTextView.x;
//                            initialY = paramsOutCoordinateTextView.y;
//                            initialTouchX = event.getRawX();
//                            initialTouchY = event.getRawY();
//                            break;
//                        case MotionEvent.ACTION_UP:
//                            break;
//                        case MotionEvent.ACTION_MOVE:
//                            paramsOutCoordinateTextView.x = initialX + (int) (event.getRawX() - initialTouchX);
//                            paramsOutCoordinateTextView.y = initialY + (int) (event.getRawY() - initialTouchY);
//                            windowManager.updateViewLayout(v, paramsOutCoordinateTextView);
//                            break;
//                    }
//                    return false;
//                }
//            });
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//
//        final WindowManager.LayoutParams myParams = new WindowManager.LayoutParams(
//                WIDTH_BORDER_AREA,
//                HEIGHT_BORDER_AREA,
//                LAYOUT_FLAG,
////                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//                PixelFormat.TRANSLUCENT);
//        myParams.gravity = Gravity.CENTER;
//
////        WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
//        windowManager.addView(borderArea, myParams);
//
//        final WindowManager.LayoutParams paramsTouchCircle = new WindowManager.LayoutParams(
//                WIDTH_TOUCH_CIRCLE,
//                HEIGHT_TOUCH_CIRCLE,
//                LAYOUT_FLAG,
////                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//                PixelFormat.TRANSLUCENT);
//        paramsTouchCircle.gravity = Gravity.CENTER;
//
////        paramsTouchCircle.width = 50;
////        paramsTouchCircle.height = 50;
//        paramsTouchCircle.y = -HEIGHT_BORDER_AREA / 2;
//        paramsTouchCircle.x = WIDTH_BORDER_AREA / 2;
//        windowManager.addView(touchCircle, paramsTouchCircle);
//
//        try{
//            //for moving the picture on touch and slide
//            borderArea.setOnTouchListener(new View.OnTouchListener() {
//                WindowManager.LayoutParams paramsT = myParams;
//                private int initialX;
//                private int initialY;
//                private float initialTouchX;
//                private float initialTouchY;
//                private long touchStartTime = 0;
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    //remove face bubble on long press
//                    if(System.currentTimeMillis()-touchStartTime> ViewConfiguration.getLongPressTimeout() && initialTouchX== event.getX()){
//                        windowManager.removeView(borderArea);
//                        return false;
//                    }
//                    switch(event.getAction()){
//                        case MotionEvent.ACTION_DOWN:
//                            touchStartTime = System.currentTimeMillis();
//                            initialX = myParams.x;
//                            initialY = myParams.y;
//                            initialTouchX = event.getRawX();
//                            initialTouchY = event.getRawY();
//                            break;
//                        case MotionEvent.ACTION_UP:
//                            Log.i(TAG, "WIDTH_BORDER_AREA:" + WIDTH_BORDER_AREA);
//                            Log.i(TAG, "HEIGHT_BORDER_AREA:" + HEIGHT_BORDER_AREA);
//
////                            Log.i(TAG, "myParams.x:" + myParams.x);
////                            Log.i(TAG, "myParams.y:" + myParams.y);
//                            break;
//                        case MotionEvent.ACTION_MOVE:
//                            myParams.x = initialX + (int) (event.getRawX() - initialTouchX);
//                            myParams.y = initialY + (int) (event.getRawY() - initialTouchY);
//
////                            GLOBAL_POSITION_X_BORDER_AREA = (int)event.getX();
////                            GLOBAL_POSITION_Y_BORDER_AREA = (int)event.getY();
//
//                            paramsTouchCircle.x = myParams.x + (WIDTH_BORDER_AREA / 2);
//                            paramsTouchCircle.y = myParams.y - (HEIGHT_BORDER_AREA / 2);
//
//                            windowManager.updateViewLayout(v, myParams);
//                            windowManager.updateViewLayout(touchCircle.getRootView(), paramsTouchCircle);
//                            break;
//                    }
//                    return false;
//                }
//            });
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//
//        try{
//            //for moving the picture on touch and slide
//            touchCircle.setOnTouchListener(new View.OnTouchListener() {
//                WindowManager.LayoutParams paramsT = paramsTouchCircle;
//                private int initialX;
//                private int initialY;
//                private float initialTouchX;
//                private float initialTouchY;
//                private long touchStartTime = 0;
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    //remove face bubble on long press
//                    if(System.currentTimeMillis()-touchStartTime> ViewConfiguration.getLongPressTimeout() && initialTouchX== event.getX()){
//                        windowManager.removeView(touchCircle);
//                        return false;
//                    }
//                    switch(event.getAction()){
//                        case MotionEvent.ACTION_DOWN:
//                            touchStartTime = System.currentTimeMillis();
//                            initialX = paramsTouchCircle.x;
//                            initialY = paramsTouchCircle.y;
//                            initialTouchX = event.getRawX();
//                            initialTouchY = event.getRawY();
//                            break;
//                        case MotionEvent.ACTION_UP:
//                            break;
//                        case MotionEvent.ACTION_MOVE:
//                            paramsTouchCircle.x = initialX + (int) (event.getRawX() - initialTouchX);
//                            paramsTouchCircle.y = initialY + (int) (event.getRawY() - initialTouchY);
//
////                            myParams.height = HEIGHT_BORDER_AREA = Math.abs(paramsTouchCircle.y - myParams.y) * 2;
////                            myParams.width = WIDTH_BORDER_AREA = Math.abs(paramsTouchCircle.x - myParams.x) * 2;
//
//                            HEIGHT_BORDER_AREA = Math.abs(paramsTouchCircle.y - myParams.y) * 2;
//                            WIDTH_BORDER_AREA = Math.abs(paramsTouchCircle.x - myParams.x) * 2;
//
//                            myParams.width = WIDTH_BORDER_AREA;
//                            myParams.height = HEIGHT_BORDER_AREA;
//
//                            windowManager.updateViewLayout(v, paramsTouchCircle);
//                            windowManager.updateViewLayout(borderArea.getRootView(), myParams);
//                            break;
//                    }
//                    return false;
//                }
//            });
//        } catch (Exception e){
//            e.printStackTrace();
//        }
    }

    public void onClickBubble(View v){
        ShowBubble();
    }

    public void onClickHideBubble(View v){
        HideBubble();
    }

    void ShowBubble(){
//        bubbleIntent = new Intent(this, FloatingFaceBubbleService.class);
//        textCoord="Типа координаты";
//        bubbleIntent.putExtra("ImageReader", textCoord);
//        startService(bubbleIntent);
    }

    void HideBubble(){
//        stopService(bubbleIntent);

//        Convertor convertor = new Convertor();
//
//        WGS84 wgs84 = new WGS84(58.3245, 24.3456, 0);
//
//        wgs84 = convertor.WGS84ToWGS84_XYZ(wgs84);
//
//        PZ90 pz90 = new PZ90();
//
//        pz90 = convertor.WGS84toPZ90(wgs84);111
    }

    @Override
    public void onActivityResult(int _requestCode, int _resultCode, Intent data) {
        int resultCode = _resultCode;
        int requestCode = _requestCode;

        if (_requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (_resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            mToggleButton.setChecked(false);
            return;
        }

//        _intent = data;

//        Rect tmpRect = new Rect((int)((double)(metrics.widthPixels) * 0.0), (int)((double)(metrics.heightPixels) * 0.5), (int)((double)(metrics.widthPixels) * 1.0), (int)((double)(metrics.heightPixels) * 1.0));

        DisplayMetrics metrics;
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

//        Log.i(TAG, metrics.toString());

        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        _imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 1);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("Capture", metrics.widthPixels, metrics.heightPixels, 2, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, _imageReader.getSurface(), null, null);

        if (mTimer == null) {
            mTimer = new Timer();

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    TimerTick();
                }
            };

            mTimer.schedule(timerTask, 1000, 1000);
        }

//        serviseGeneral = new ServiceGeneral(metrics, textView, tmpRect, _imageReader);
//        serviseGeneral.startService(data);
    }

    private Bitmap cutRightTop(Bitmap origialBitmap, Rect areaFromSrc) {
        Rect desRect = new Rect(0, 0, areaFromSrc.right - areaFromSrc.left, areaFromSrc.bottom - areaFromSrc.top);
        Bitmap cutBitmap = Bitmap.createBitmap(desRect.right, desRect.bottom, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cutBitmap);
        canvas.drawBitmap(origialBitmap, areaFromSrc, desRect, null);
        return cutBitmap;
    }

    private void SaveImage(Bitmap finalBitmap)
    {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();

        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

//    int i=0;
    public void onClick(View view){

//        if (mTimer == null) {
//            mTimer = new Timer();
//
//            TimerTask t = new TimerTask() {
//                @Override
//                public void run() {
//
//                    TimerTick();
//
//
//                }
//            };
//
//            mTimer.schedule(t, 1000, 1000);
//        }



//        ServiseGeneral serviseGeneral = new ServiseGeneral(metrics, textView, requestCode, resultCode, tmpRect, _intent);
//
//        Intent i=new Intent(this, ServiceGeneral.class);
//       serviseGeneral.startService(i);
//        Image image = _imageReader.acquireNextImage();
//
//        if (image != null)
//        {
//            Bitmap bitmap;
//
//            final Image.Plane[] planes = image.getPlanes();
//            final ByteBuffer buffer = planes[0].getBuffer();
//
//            int pixelStride = planes[0].getPixelStride();
//            int rowStride = planes[0].getRowStride();
//            int rowPadding = rowStride - pixelStride * image.getWidth();
//
//            bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
//            bitmap.copyPixelsFromBuffer(buffer);
//
//            Rect areaFromSrc = new Rect((int)((double)(metrics.widthPixels) * 0.0), (int)((double)(metrics.heightPixels) * 0.5), (int)((double)(metrics.widthPixels) * 1.0), (int)((double)(metrics.heightPixels) * 1.0));
//
//            Bitmap cropBM = cutRightTop(bitmap, areaFromSrc);
//
//            mImageView.setImageBitmap(cropBM);
//
//            SaveImage(cropBM);
//
//            String DATA_PATH_RESOURCES =getPackageResourcePath();
//            DATA_PATH_RESOURCES =getPackageCodePath();
//            Log.i(TAG, DATA_PATH_RESOURCES);
//
////            String DATA_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
//            String DATA_PATH = DATA_PATH_RESOURCES;
//            String lang = "eng";
//
//
//            TessBaseAPI tessBaseApi = new TessBaseAPI();
//
//            try
//            {
//                tessBaseApi.init(DATA_PATH, lang);
//            }
//            catch (Exception ex)
//            {
//                Log.e(TAG, ex.getMessage());
//            }
//
//            tessBaseApi.setImage(cropBM);
//            String extractedText = "empty result";
//            extractedText = tessBaseApi.getUTF8Text();
//            tessBaseApi.end();
//
//            textView.setText(extractedText);
//
//            image.close();
//        }
    }

    void TimerTick(){
//        textCoord="dd: "+ ++i;
//        if(bubbleIntent!=null) {
//            startService(bubbleIntent.putExtra("ImageReader", textCoord));
//        }

         Image image = _imageReader.acquireNextImage();

        if (image != null)
        {
            Bitmap bitmap;

            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();

            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * image.getWidth();

            bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

//            Rect areaFromSrc;

//            DisplayMetrics metrics;
//            metrics = new DisplayMetrics();
//            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

//            Rect areaFromSrc = new Rect((int)((double)(metrics.widthPixels) * 0.0), (int)((double)(metrics.heightPixels) * 0.5), (int)((double) (metrics.widthPixels)), (int)((double) (metrics.heightPixels)));

//            Log.i(TAG, "X: " + touchCircle.getLeft() + "      Y: " + touchCircle.getTop());
            int[] location = new int[2];
            borderArea.getLocationOnScreen(location);
//            Log.i(TAG, "X: " + location[0] + "      Y: " + location[1]);

            Rect areaFromSrc = new Rect(location[0] + WIDTH_STROKE, location[1] + WIDTH_STROKE, location[0] + WIDTH_BORDER_AREA - WIDTH_STROKE, location[1] + HEIGHT_BORDER_AREA - WIDTH_STROKE);

//            Log.i(TAG, "~top~: " + areaFromSrc.top);
//            Log.i(TAG, "~left~: " + areaFromSrc.left);
//            Log.i(TAG, "~right~: " + areaFromSrc.right);
//            Log.i(TAG, "~bottom~: " + areaFromSrc.bottom);

            Bitmap cropBM = cutRightTop(bitmap, areaFromSrc);

//            if(orientation == Configuration.ORIENTATION_LANDSCAPE)
//            {
//                SaveImage(bitmap);
//            }

            //mImageView.setImageBitmap(cropBM);

//            SaveImage(cropBM);

//            String DATA_PATH_RESOURCES =getPackageResourcePath();
//            DATA_PATH_RESOURCES =getPackageCodePath();
//            Log.i(TAG, DATA_PATH_RESOURCES);

//            String DATA_PATH = getResources().openRawResource(R.raw.eng).toString();// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
//            File cascadeDir = (MainActivity.this).getDir("", Context.MODE_PRIVATE);
//            mCascadeFile = new File(cascadeDir, "eng.traineddata");
//            String DATA_PATH = DATA_PATH_RESOURCES;





            tessBaseApi.setImage(cropBM);
            extractedText = tessBaseApi.getUTF8Text();

            if(extractedText.isEmpty())
            {
                extractedText = "Координаты не обнаружены";
            }
            else
            {
                extractedText = extractedText.trim();
                extractedText = extractedText.replaceAll(" ", "");

                int indexNChar = extractedText.indexOf("N");
                int indexEChar = extractedText.indexOf("E");

                if((indexEChar == -1) || (indexNChar == -1))
                {
                    extractedText = "Координаты не обнаружены. \nОбнаружено:\n" + extractedText;
                }
                else
                {
                    String strCoordinateN = extractedText.substring(0, indexNChar);
                    String strCoordinateE = extractedText.substring(indexNChar + 1, indexEChar);

                    try
                    {
                        double N = Double.parseDouble(strCoordinateN);
                        double E = Double.parseDouble(strCoordinateE);

                        wgs84.longt = N;
                        wgs84.latt = E;

                        gaussaKrugera.calcCoordinate(N, E);

//                        wgs84 = convertor.WGS84ToWGS84_XYZ(wgs84);
//                        pz90 = convertor.WGS84toPZ90(wgs84);

                        extractedText = "X: " + gaussaKrugera.getX() + "\nY: " + gaussaKrugera.getY();
//                        extractedText = "X: " + pz90.X_PZ90 + " Y: " + pz90.Y_PZ90;
                    }
                    catch (Exception e)
                    {
                        extractedText = "Координаты не обнаружены. \nОбнаружено:\n" + extractedText;
                    }
                }





//                Convertor convertor = new Convertor();
//
//                WGS84 wgs84 = new WGS84(58.3245, 24.3456, 0);
//
//                wgs84 = convertor.WGS84ToWGS84_XYZ(wgs84);
//
//                PZ90 pz90 = new PZ90();
//
//                pz90 = convertor.WGS84toPZ90(wgs84);


            }

            handler.post(runnable);

            //textView.setText(extractedText);
//            if(bubbleIntent!=null) {
//                startService(bubbleIntent.putExtra("ImageReader", extractedText));
//            }
            image.close();
        }
    }

    public void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {

            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1);
            }

//            int permissionStatusRecordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
//            int permissionStatusWriteExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//            int permissionStatusReadExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
//
//            if ((permissionStatusRecordAudio == PackageManager.PERMISSION_DENIED) || (permissionStatusReadExternalStorage == PackageManager.PERMISSION_DENIED) || (permissionStatusWriteExternalStorage == PackageManager.PERMISSION_DENIED)) {
//                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
//                ((ToggleButton) view).setChecked(false);
//            }
            int permissionStatusWriteExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int permissionStatusReadExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            if ((permissionStatusReadExternalStorage == PackageManager.PERMISSION_DENIED) || (permissionStatusWriteExternalStorage == PackageManager.PERMISSION_DENIED)) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
            }
            else {
                shareScreen();
            }


        } else {
            Log.v(TAG, "Stopping Recording");
            stopScreenSharing();
        }
    }

    private void shareScreen() {
        if (mMediaProjection == null) {

            mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

//            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);

            outCoordinateTextView = new TextView(this);
            touchCircle = new ImageView(this);
            borderArea = new ImageView(this);

            outCoordinateTextView.setText("Координаты тут");
            outCoordinateTextView.setTextColor(Color.argb(255, 255, 255, 255));
            outCoordinateTextView.setTextSize(16);
            outCoordinateTextView.setBackgroundResource(R.drawable.background_text_view);

            borderArea.setImageResource(R.drawable.shape);
            touchCircle.setImageResource(R.mipmap.segment);

            int LAYOUT_FLAG;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
            }

            final WindowManager.LayoutParams paramsOutCoordinateTextView = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    LAYOUT_FLAG,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            paramsOutCoordinateTextView.gravity = Gravity.CENTER;

            WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
            windowManager.addView(outCoordinateTextView, paramsOutCoordinateTextView);

            try{
                outCoordinateTextView.setOnTouchListener(new View.OnTouchListener() {
                    WindowManager.LayoutParams paramsT = paramsOutCoordinateTextView;
                    private int initialX;
                    private int initialY;
                    private float initialTouchX;
                    private float initialTouchY;
                    private long touchStartTime = 0;
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(System.currentTimeMillis()-touchStartTime>ViewConfiguration.getLongPressTimeout() && initialTouchX== event.getX()){
                            windowManager.removeView(outCoordinateTextView);
                            return false;
                        }
                        switch(event.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                touchStartTime = System.currentTimeMillis();
                                initialX = paramsOutCoordinateTextView.x;
                                initialY = paramsOutCoordinateTextView.y;
                                initialTouchX = event.getRawX();
                                initialTouchY = event.getRawY();
                                break;
                            case MotionEvent.ACTION_UP:
                                break;
                            case MotionEvent.ACTION_MOVE:
                                paramsOutCoordinateTextView.x = initialX + (int) (event.getRawX() - initialTouchX);
                                paramsOutCoordinateTextView.y = initialY + (int) (event.getRawY() - initialTouchY);
                                windowManager.updateViewLayout(v, paramsOutCoordinateTextView);
                                break;
                        }
                        return false;
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
            }

            final WindowManager.LayoutParams paramsBorderArea = new WindowManager.LayoutParams(
                    WIDTH_BORDER_AREA,
                    HEIGHT_BORDER_AREA,
                    LAYOUT_FLAG,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            paramsBorderArea.gravity = Gravity.CENTER;

            windowManager.addView(borderArea, paramsBorderArea);

            final WindowManager.LayoutParams paramsTouchCircle = new WindowManager.LayoutParams(
                    WIDTH_TOUCH_CIRCLE,
                    HEIGHT_TOUCH_CIRCLE,
                    LAYOUT_FLAG,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            paramsTouchCircle.gravity = Gravity.CENTER;

            paramsTouchCircle.y = -HEIGHT_BORDER_AREA / 2;
            paramsTouchCircle.x = WIDTH_BORDER_AREA / 2;
            windowManager.addView(touchCircle, paramsTouchCircle);

            try{
                borderArea.setOnTouchListener(new View.OnTouchListener() {
                    WindowManager.LayoutParams paramsT = paramsBorderArea;
                    private int initialX;
                    private int initialY;
                    private float initialTouchX;
                    private float initialTouchY;
                    private long touchStartTime = 0;
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(System.currentTimeMillis()-touchStartTime> ViewConfiguration.getLongPressTimeout() && initialTouchX== event.getX()){
                            windowManager.removeView(borderArea);
                            return false;
                        }
                        switch(event.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                touchStartTime = System.currentTimeMillis();
                                initialX = paramsBorderArea.x;
                                initialY = paramsBorderArea.y;
                                initialTouchX = event.getRawX();
                                initialTouchY = event.getRawY();
                                break;
                            case MotionEvent.ACTION_UP:
//                                Log.i(TAG, "WIDTH_BORDER_AREA:" + WIDTH_BORDER_AREA);
//                                Log.i(TAG, "HEIGHT_BORDER_AREA:" + HEIGHT_BORDER_AREA);

                                break;
                            case MotionEvent.ACTION_MOVE:
                                paramsBorderArea.x = initialX + (int) (event.getRawX() - initialTouchX);
                                paramsBorderArea.y = initialY + (int) (event.getRawY() - initialTouchY);

                                paramsTouchCircle.x = paramsBorderArea.x + (WIDTH_BORDER_AREA / 2);
                                paramsTouchCircle.y = paramsBorderArea.y - (HEIGHT_BORDER_AREA / 2);

                                windowManager.updateViewLayout(v, paramsBorderArea);
                                windowManager.updateViewLayout(touchCircle.getRootView(), paramsTouchCircle);
                                break;
                        }
                        return false;
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
            }

            try{
                touchCircle.setOnTouchListener(new View.OnTouchListener() {
                    WindowManager.LayoutParams paramsT = paramsTouchCircle;
                    private int initialX;
                    private int initialY;
                    private float initialTouchX;
                    private float initialTouchY;
                    private long touchStartTime = 0;
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(System.currentTimeMillis()-touchStartTime> ViewConfiguration.getLongPressTimeout() && initialTouchX== event.getX()){
                            windowManager.removeView(touchCircle);
                            return false;
                        }
                        switch(event.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                touchStartTime = System.currentTimeMillis();
                                initialX = paramsTouchCircle.x;
                                initialY = paramsTouchCircle.y;
                                initialTouchX = event.getRawX();
                                initialTouchY = event.getRawY();
                                break;
                            case MotionEvent.ACTION_UP:
                                break;
                            case MotionEvent.ACTION_MOVE:
                                paramsTouchCircle.x = initialX + (int) (event.getRawX() - initialTouchX);
                                paramsTouchCircle.y = initialY + (int) (event.getRawY() - initialTouchY);

                                HEIGHT_BORDER_AREA = Math.abs(paramsTouchCircle.y - paramsBorderArea.y) * 2;
                                WIDTH_BORDER_AREA = Math.abs(paramsTouchCircle.x - paramsBorderArea.x) * 2;

                                paramsBorderArea.width = WIDTH_BORDER_AREA;
                                paramsBorderArea.height = HEIGHT_BORDER_AREA;

                                windowManager.updateViewLayout(v, paramsTouchCircle);
                                windowManager.updateViewLayout(borderArea.getRootView(), paramsBorderArea);
                                break;
                        }
                        return false;
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
            }

            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }
        return;
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        destroyMediaProjection();

        WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        windowManager.removeView(outCoordinateTextView);
        windowManager.removeView(borderArea);
        windowManager.removeView(touchCircle);

        outCoordinateTextView = null;
        borderArea = null;
        touchCircle = null;

        mTimer.cancel();
        mTimer = null;
    }

    @Override
    public void onDestroy() {
        stopScreenSharing();
        super.onDestroy();

//        destroyMediaProjection();
//        HideBubble();
//        tessBaseApi.end();
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }
}
