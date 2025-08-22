package com.company.ppe.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public final class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int MAX_IMAGE_SIZE = 1024; // Max width/height in pixels

    private ImageUtils() {}

    /**
     * Converts bitmap to base64 string with compression and resizing
     */
    public static String bitmapToBase64(Bitmap bitmap, float quality) {
        if (bitmap == null) return "";
        
        try {
            // Resize if too large
            Bitmap resized = resizeBitmap(bitmap, MAX_IMAGE_SIZE);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int qualityInt = Math.max(10, Math.min(100, (int)(quality * 100)));
            
            resized.compress(Bitmap.CompressFormat.JPEG, qualityInt, baos);
            byte[] bytes = baos.toByteArray();
            
            // Clean up
            if (resized != bitmap) {
                resized.recycle();
            }
            baos.close();
            
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to base64", e);
            return "";
        }
    }

    /**
     * Resize bitmap if it's larger than maxSize while maintaining aspect ratio
     */
    private static Bitmap resizeBitmap(Bitmap original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        if (width <= maxSize && height <= maxSize) {
            return original;
        }
        
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        
        Matrix matrix = new Matrix();
        matrix.postScale(ratio, ratio);
        
        return Bitmap.createBitmap(original, 0, 0, width, height, matrix, true);
    }

    /**
     * Decode byte array to bitmap with memory optimization
     */
    public static Bitmap decodeByteArray(byte[] data, int maxSize) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);
            
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
            
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Use less memory
            
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding byte array to bitmap", e);
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
