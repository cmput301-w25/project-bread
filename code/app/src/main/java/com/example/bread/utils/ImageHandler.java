package com.example.bread.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for handling images that are stored in Firebase.
 * <a href="https://stackoverflow.com/questions/4830711/how-can-i-convert-an-image-into-a-base64-string">Convert Image to Base64</a>
 * <a href="https://stackoverflow.com/questions/18545246/how-to-compress-image-size">Compress Image Size</a>
 */
public class ImageHandler {

    public static final int MAX_IMAGE_SIZE = 64 * 1024; // 64 KB
    public static final float SCALE_FACTOR = 0.7f;

    /**
     * Compress an image file to a base64 encoded string with size less that {@link #MAX_IMAGE_SIZE}.
     *
     * <p>
     * Firebase Firestore has a document size limit of 1MB, but this implementation ensures that
     * images are compressed to a maximum size of 64KB. This is to ensure that the images can be
     * stored in the document without exceeding the limit.
     * </p>
     *
     * <p>
     * For usage check out this android page <a href="https://developer.android.com/training/data-storage/shared/photopicker#java">Photo picker</a>
     * </p>
     *
     * @param context the context
     * @param uri     the URI of the image file
     * @return Base64 encoded string of the compressed image
     * @throws IOException if the file cannot be read
     */
    public static String compressImageToBase64(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        Bitmap orginalBitmap = BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) {
            inputStream.close();
        }

        return compressBitmapToBase64(orginalBitmap);
    }

    /**
     * Compress a bitmap to a base64 encoded string with size less that {@link #MAX_IMAGE_SIZE}.
     *
     * @param bitmap the bitmap to compress
     * @return Base64 encoded string of the compressed image
     */
    public static String compressBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        int quality = 100;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

        // Keep reducing quality until we're under the size limit
        while (outputStream.toByteArray().length > MAX_IMAGE_SIZE) {
            outputStream.reset();

            quality -= 10;
            if (quality <= 10) {
                quality = 70;
                // Scale down the bitmap
                bitmap = scaleBitmap(bitmap);
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        }
        byte[] compressedData = outputStream.toByteArray();
        return Base64.encodeToString(compressedData, Base64.DEFAULT);
    }

    /**
     * Scale a bitmap by a factor of {@link #SCALE_FACTOR}.
     *
     * @param bitmap the bitmap to scale
     * @return the scaled bitmap
     */
    private static Bitmap scaleBitmap(Bitmap bitmap) {
        int width = Math.round(bitmap.getWidth() * SCALE_FACTOR);
        int height = Math.round(bitmap.getHeight() * SCALE_FACTOR);
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    /**
     * Convert a base64 encoded string to a bitmap.
     *
     * @param base64 the base64 encoded string
     * @return the bitmap
     */
    public static Bitmap base64ToBitmap(String base64) {
        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}
