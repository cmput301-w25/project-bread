package com.example.bread;

import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.bread.utils.ImageHandler;

@RunWith(AndroidJUnit4.class)
public class ImageHandlerTest {

    private Bitmap testBitmap;

    @Before
    public void setUp() {
        // Create a simple 10x10 bitmap for testing
        testBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    }

    @Test
    public void testCompressBitmapToBase64_NotNull() {
        String base64 = ImageHandler.compressBitmapToBase64(testBitmap);
        assertNotNull("Base64 string should not be null", base64);
        assertFalse("Base64 string should not be empty", base64.isEmpty());
    }

    @Test
    public void testCompressBitmapToBase64_NullBitmap_ReturnsNull() {
        assertNull("Null bitmap should return null base64 string", ImageHandler.compressBitmapToBase64(null));
    }

    @Test
    public void testBase64ToBitmap_ValidBase64_ReturnsBitmap() {
        String base64 = ImageHandler.compressBitmapToBase64(testBitmap);
        Bitmap resultBitmap = ImageHandler.base64ToBitmap(base64);

        assertNotNull("Decoded bitmap should not be null", resultBitmap);
        assertEquals("Bitmap width should match", testBitmap.getWidth(), resultBitmap.getWidth());
        assertEquals("Bitmap height should match", testBitmap.getHeight(), resultBitmap.getHeight());
    }

    @Test
    public void testBase64ToBitmap_WithDataUriPrefix_ReturnsBitmap() {
        String base64 = ImageHandler.compressBitmapToBase64(testBitmap);
        String withPrefix = "data:image/jpeg;base64," + base64;

        Bitmap resultBitmap = ImageHandler.base64ToBitmap(withPrefix);

        assertNotNull("Bitmap should be decoded from data URI prefix", resultBitmap);
    }

    @Test
    public void testBase64ToBitmap_NullInput_ReturnsNull() {
        assertNull("Null base64 string should return null bitmap", ImageHandler.base64ToBitmap(null));
    }

    @Test
    public void testBase64ToBitmap_EmptyInput_ReturnsNull() {
        assertNull("Empty base64 string should return null bitmap", ImageHandler.base64ToBitmap(""));
    }
}
