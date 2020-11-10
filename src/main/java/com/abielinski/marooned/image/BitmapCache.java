package com.abielinski.marooned.image;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;

public class BitmapCache {

	private static int maxCacheSize = 64 * 1024 * 1024; // 64MiB
	private static BitmapCache singleton;
	private LruCache<String, Bitmap> bitmapCache;
	public BitmapCache(){
		// from https://developer.android.com/topic/performance/graphics/cache-bitmap#java

		// Get max available VM memory, exceeding this amount will throw an
		// OutOfMemory exception. Stored in kilobytes as LruCache takes an
		// int in its constructor.
		//final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

		// Use 1/8th of the available memory for this memory cache.
		final int cacheSize = maxCacheSize;
		Log.i("BitmapCache", "Init cache with " + cacheSize / 1024 + "KB");

		bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount();
			}
		};
	}

	public static BitmapCache get(){
		if (singleton == null){
			singleton = new BitmapCache();
		}
		return singleton;
	}

	public static LruCache<String, Bitmap> getCache(){
		return get().bitmapCache;
	}

	public static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null) {
			getCache().put(key, bitmap);
		}
	}

	public static Bitmap getBitmapFromMemCache(String key) {
		return getCache().get(key);
	}

	public static Bitmap getScaledBitmap(Activity mActivity, Uri imageCacheUri, int imageWidth, int imageHeight){
		final String key = imageCacheUri.toString() + "|" + imageWidth + "|" + imageHeight;

		final Bitmap cachedBitmap = getBitmapFromMemCache(key);
		if (cachedBitmap != null){
			//Log.i("BitmapCache", "Cache hit");
			return cachedBitmap;
		}
		//Log.i("BitmapCache", "Cache miss (cache is " + getCache().size()/1024 + "KB )");
		try {

			final Bitmap rawBitmap = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), imageCacheUri);
			final Bitmap scaledBitmap = ThumbnailScaler.scaleToFit(
					rawBitmap,
					imageWidth,
					imageHeight);

			addBitmapToMemoryCache(key, scaledBitmap);
			return scaledBitmap;

		} catch (
				IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
