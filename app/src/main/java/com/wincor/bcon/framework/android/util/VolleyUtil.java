package com.wincor.bcon.framework.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;

/**
 * Created by thorsten on 13/12/15.
 */
public class VolleyUtil {

    public final static int IMAGE_MEMORY_CACHE_SIZE = 10;

    private static Context mContext;

    private static RequestQueue mRequestQueue;
    private static ImageLoader mImageLoader;

    public static void initializeContext(Context context) {
        mContext = context;
    }

    public static RequestQueue getRequestQueue() {
        if (mRequestQueue == null){
            mRequestQueue=com.android.volley.toolbox.Volley.newRequestQueue(mContext);
        }
        return mRequestQueue;
    }

    public static ImageLoader getImageLoader() {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(getRequestQueue(), new ImageLoader.ImageCache() {
                private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(IMAGE_MEMORY_CACHE_SIZE);
                public void putBitmap(String url, Bitmap bitmap) {
                    mCache.put(url, bitmap);
                }
                public Bitmap getBitmap(String url) {
                    return mCache.get(url);
                }
            });
        }
        return mImageLoader;
    }
}