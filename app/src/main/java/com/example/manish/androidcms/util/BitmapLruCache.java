package com.example.manish.androidcms.util;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader;

/**
 * Created by Manish on 4/21/2015.
 */
public class BitmapLruCache extends LruCache<String, Bitmap> implements ImageLoader.ImageCache {
    public BitmapLruCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        // The cache size will be measured in kilobytes rather than
        // number of items.
        int bytes = (value.getRowBytes() * value.getHeight());
        return (bytes / 1024); //value.getByteCount() introduced in HONEYCOMB_MR1 or higher.
    }

    @Override
    public Bitmap getBitmap(String key) {
        return this.get(key);
    }

    @Override
    public void putBitmap(String key, Bitmap bitmap) {
        this.put(key, bitmap);
    }
}
