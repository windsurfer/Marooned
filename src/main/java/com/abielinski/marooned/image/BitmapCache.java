package com.abielinski.marooned.image;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE;

public class BitmapCache {

	private static int maxCacheSize = 256 * 1024; // 256MiB stored as KiB
	private static BitmapCache singleton;
	private LruCache<String, Bitmap> bitmapCache;
	public BitmapCache(){
		// from https://developer.android.com/topic/performance/graphics/cache-bitmap#java

		// Get max available VM memory, exceeding this amount will throw an
		// OutOfMemory exception. Stored in kilobytes as LruCache takes an
		// int in its constructor.
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);


		final int cacheSize = Math.min(maxCacheSize, maxMemory);
		Log.i("BitmapCache", "Init BitmapCache with " + cacheSize + "KiB");

		bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override // returns KB
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount() / 1024;
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

	public static void trimMemory(int level) {
		if (level >= TRIM_MEMORY_MODERATE) {
			Log.v("BitmapCache", "Emptying entire BitmapCache");
			getCache().evictAll();
		}
		else if (level >= TRIM_MEMORY_BACKGROUND) {
			Log.v("BitmapCache", "Shrinking BitmapCache");
			getCache().trimToSize(getCache().size() / 2);
		}
	}

	public static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null) {
			getCache().put(key, bitmap);
		}
	}

	public static float getPercentFull(){
		return (float)getCache().size() / (float)getCache().maxSize();
	}

	public static Bitmap getBitmapFromMemCache(String key) {
		return getCache().get(key);
	}

	public static Bitmap getScaledBitmap(Activity mActivity, Uri imageCacheUri, int imageWidth, int imageHeight){
		final String key = imageCacheUri.toString() + "|" + imageWidth + "|" + imageHeight;

		final Bitmap cachedBitmap = getBitmapFromMemCache(key);
		if (cachedBitmap != null){
			Log.v("BitmapCache", "BitmapCache hit (cache is " + getPercentFull() *100 + "% full )");
			return cachedBitmap;
		}
		Log.v("BitmapCache", "BitmapCache miss (cache is " + getPercentFull()*100 + "% full )");
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
