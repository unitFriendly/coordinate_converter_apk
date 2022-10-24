package com.truiton.screencapture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.Provider;
import java.util.Random;

public class ServiceGeneral extends Service
{
    private static final int REQUEST_CODE = 1000;
    private static final String TAG = "ServiceGeneral";
    public static final int RESULT_OK = -1;

//    private MediaProjectionManager projectionManager;
//    private MediaProjection mediaProjection;
//    private VirtualDisplay virtualDisplay;
    private DisplayMetrics metrics;
    private ImageReader imageReader;
    private TextView textView;
    private Rect areaFromSrc;
//    private Intent intent;

    public ServiceGeneral()
    {
        super();
    }

    public ServiceGeneral(DisplayMetrics _metrics, TextView _textView, Rect _areaFromSrc, ImageReader _imageReader)
    {
        super();

        metrics = _metrics;
        textView = _textView;
//        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        areaFromSrc = _areaFromSrc;
        imageReader = _imageReader;
//        intent = _intent;
//
//        mediaProjection = projectionManager.getMediaProjection(resultCode, intent);
//        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 1);
//        virtualDisplay = mediaProjection.createVirtualDisplay("Capture", metrics.widthPixels, metrics.heightPixels, 2, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);
//
//        projectionManager.createScreenCaptureIntent();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate()
    {
    }

    private Bitmap cutRightTop(Bitmap origialBitmap, Rect areaFromSrc)
    {
        Rect desRect = new Rect(0, 0, areaFromSrc.right - areaFromSrc.left, areaFromSrc.bottom - areaFromSrc.top);
        Bitmap cutBitmap = Bitmap.createBitmap(desRect.right, desRect.bottom, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cutBitmap);
        canvas.drawBitmap(origialBitmap, areaFromSrc, desRect, null);
        return cutBitmap;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.v(TAG, "onStartCommand");

//        Image image = imageReader.acquireNextImage();
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
////            imageView.setImageBitmap(cropBM);
//
////            SaveImage(cropBM);
//
//            String DATA_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
//            String lang = "eng";
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

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
    }
}
