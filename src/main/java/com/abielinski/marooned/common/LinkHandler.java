/*******************************************************************************
 * This file is part of Marooned.
 *
 * Marooned is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Marooned is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Marooned.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.abielinski.marooned.common;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.abielinski.marooned.R;
import com.abielinski.marooned.activities.AlbumListingActivity;
import com.abielinski.marooned.activities.BaseActivity;
import com.abielinski.marooned.activities.CommentListingActivity;
import com.abielinski.marooned.activities.ImageViewActivity;
import com.abielinski.marooned.activities.PostListingActivity;
import com.abielinski.marooned.activities.WebViewActivity;
import com.abielinski.marooned.cache.CacheRequest;
import com.abielinski.marooned.fragments.ShareOrderDialog;
import com.abielinski.marooned.fragments.UserProfileDialog;
import com.abielinski.marooned.image.AlbumInfo;
import com.abielinski.marooned.image.DeviantArtAPI;
import com.abielinski.marooned.image.GetAlbumInfoListener;
import com.abielinski.marooned.image.GetImageInfoListener;
import com.abielinski.marooned.image.GfycatAPI;
import com.abielinski.marooned.image.ImageInfo;
import com.abielinski.marooned.image.ImgurAPI;
import com.abielinski.marooned.image.ImgurAPIV3;
import com.abielinski.marooned.image.RedditGalleryAPI;
import com.abielinski.marooned.image.RedditVideosAPI;
import com.abielinski.marooned.image.RedgifsAPI;
import com.abielinski.marooned.image.StreamableAPI;
import com.abielinski.marooned.reddit.things.RedditPost;
import com.abielinski.marooned.reddit.url.RedditURLParser;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkHandler {

	public static final Pattern
			youtubeDotComPattern
			= Pattern.compile("^https?://[\\.\\w]*youtube\\.\\w+/.*"),
			youtuDotBePattern = Pattern.compile(
					"^https?://[\\.\\w]*youtu\\.be/([A-Za-z0-9\\-_]+)(\\?.*|).*"),
			vimeoPattern = Pattern.compile("^https?://[\\.\\w]*vimeo\\.\\w+/.*"),
			googlePlayPattern = Pattern.compile(
					"^https?://[\\.\\w]*play\\.google\\.\\w+/.*");

	public enum LinkAction {
		SHARE(R.string.action_share),
		COPY_URL(R.string.action_copy_link),
		SHARE_IMAGE(R.string.action_share_image),
		SAVE_IMAGE(R.string.action_save),
		EXTERNAL(R.string.action_external);

		public final int descriptionResId;

		LinkAction(final int descriptionResId) {
			this.descriptionResId = descriptionResId;
		}
	}

	public static void onLinkClicked(final AppCompatActivity activity, final String url) {
		onLinkClicked(activity, url, false);
	}

	public static void onLinkClicked(
			final AppCompatActivity activity,
			final String url,
			final boolean forceNoImage) {
		onLinkClicked(activity, url, forceNoImage, null);
	}

	public static void onLinkClicked(
			final AppCompatActivity activity,
			final String url,
			final boolean forceNoImage,
			final RedditPost post) {

		onLinkClicked(activity, url, forceNoImage, post, null, 0);
	}

	public static void onLinkClicked(
			final AppCompatActivity activity,
			final String url,
			final boolean forceNoImage,
			final RedditPost post,
			final AlbumInfo albumInfo,
			final int albumImageIndex) {
		onLinkClicked(
				activity,
				url,
				forceNoImage,
				post,
				albumInfo,
				albumImageIndex,
				false);
	}

	public static void onLinkClicked(
			final AppCompatActivity activity,
			String url,
			final boolean forceNoImage,
			final RedditPost post,
			final AlbumInfo albumInfo,
			final int albumImageIndex,
			final boolean fromExternalIntent) {

		if(url == null) {
			General.quickToast(activity, R.string.link_does_not_exist);
			return;
		}

		final SharedPreferences sharedPreferences
				= PreferenceManager.getDefaultSharedPreferences(activity);

		if(url.startsWith("rr://")) {

			final Uri rrUri = Uri.parse(url);

			if(rrUri.getAuthority().equals("msg")) {
				new Handler().post(new Runnable() {
					@Override
					public void run() {
						final AlertDialog.Builder builder = new AlertDialog.Builder(
								activity);
						builder.setTitle(rrUri.getQueryParameter("title"));
						builder.setMessage(rrUri.getQueryParameter("message"));
						final AlertDialog alert = builder.create();
						alert.show();
					}
				});

				return;
			}
		}

		if(url.startsWith("r/") || url.startsWith("u/")) {
			url = "/" + url;
		}

		if(url.startsWith("/")) {
			url = "https://reddit.com" + url;
		}

		if(!url.contains("://")) {
			url = "http://" + url;
		}

		if(!forceNoImage && isProbablyAnImage(url)) {
			final Intent intent = new Intent(activity, ImageViewActivity.class);
			intent.setData(Uri.parse(url));
			intent.putExtra("post", post);

			if(albumInfo != null) {
				intent.putExtra("albumUrl", albumInfo.url);
				intent.putExtra("albumImageIndex", albumImageIndex);
			}

			activity.startActivity(intent);
			return;
		}

		if(!forceNoImage && (imgurAlbumPattern.matcher(url).matches()
				|| redditGalleryPattern.matcher(url).matches())) {

			final PrefsUtility.AlbumViewMode albumViewMode
					= PrefsUtility.pref_behaviour_albumview_mode(
					activity,
					sharedPreferences);

			switch(albumViewMode) {

				case INTERNAL_LIST: {
					final Intent intent = new Intent(
							activity,
							AlbumListingActivity.class);
					intent.setData(Uri.parse(url));
					intent.putExtra("post", post);
					activity.startActivity(intent);
					return;
				}

				case INTERNAL_BROWSER: {
					if(PrefsUtility.pref_behaviour_usecustomtabs(activity, sharedPreferences)
							&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
						openCustomTab(activity, Uri.parse(url), post);

					} else {
						openInternalBrowser(activity, url, post);
					}
					return;
				}

				case EXTERNAL_BROWSER: {
					openWebBrowser(activity, Uri.parse(url), fromExternalIntent);
					return;
				}
			}
		}

		final RedditURLParser.RedditURL redditURL = RedditURLParser.parse(Uri.parse(url));
		if(redditURL != null) {

			switch(redditURL.pathType()) {

				case RedditURLParser.SUBREDDIT_POST_LISTING_URL:
				case RedditURLParser.MULTIREDDIT_POST_LISTING_URL:
				case RedditURLParser.USER_POST_LISTING_URL:
				case RedditURLParser.UNKNOWN_POST_LISTING_URL: {
					final Intent intent = new Intent(activity, PostListingActivity.class);
					intent.setData(redditURL.generateJsonUri());
					activity.startActivityForResult(intent, 1);
					return;
				}

				case RedditURLParser.POST_COMMENT_LISTING_URL:
				case RedditURLParser.USER_COMMENT_LISTING_URL: {
					final Intent intent = new Intent(
							activity,
							CommentListingActivity.class);
					intent.setData(redditURL.generateJsonUri());
					activity.startActivityForResult(intent, 1);
					return;
				}

				case RedditURLParser.USER_PROFILE_URL: {
					UserProfileDialog.newInstance(redditURL.asUserProfileURL().username)
							.show(activity.getSupportFragmentManager(), null);
					return;
				}
			}
		}

		// Use a browser

		if(!PrefsUtility.pref_behaviour_useinternalbrowser(activity, sharedPreferences)) {
			if(openWebBrowser(activity, Uri.parse(url), fromExternalIntent)) {
				return;
			}
		}

		if(youtubeDotComPattern.matcher(url).matches()
				|| vimeoPattern.matcher(url).matches()
				|| googlePlayPattern.matcher(url).matches()) {
			if(openWebBrowser(activity, Uri.parse(url), fromExternalIntent)) {
				return;
			}
		}

		final Matcher youtuDotBeMatcher = youtuDotBePattern.matcher(url);

		if(youtuDotBeMatcher.find() && youtuDotBeMatcher.group(1) != null) {
			final String youtuBeUrl = "http://youtube.com/watch?v="
					+ youtuDotBeMatcher.group(1)
					+ (youtuDotBeMatcher.group(2).length() > 0
							? "&" + youtuDotBeMatcher.group(2) .substring(1)
							: "");
			if(openWebBrowser(activity, Uri.parse(youtuBeUrl), fromExternalIntent)) {
				return;
			}
		}

		if(PrefsUtility.pref_behaviour_usecustomtabs(activity, sharedPreferences)
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			openCustomTab(activity, Uri.parse(url), post);

		} else {
			openInternalBrowser(activity, url, post);
		}

	}

	public static void onLinkLongClicked(final BaseActivity activity, final String uri) {
		onLinkLongClicked(activity, uri, false);
	}

	public static void onLinkLongClicked(
			final BaseActivity activity,
			final String uri,
			final boolean forceNoImage) {
		if(uri == null) {
			return;
		}

		final EnumSet<LinkHandler.LinkAction> itemPref
				= PrefsUtility.pref_menus_link_context_items(
				activity,
				PreferenceManager.getDefaultSharedPreferences(activity));

		if(itemPref.isEmpty()) {
			return;
		}

		final ArrayList<LinkMenuItem> menu = new ArrayList<>();

		if(itemPref.contains(LinkAction.COPY_URL)) {
			menu.add(new LinkMenuItem(
					activity,
					R.string.action_copy_link,
					LinkAction.COPY_URL));
		}
		if(itemPref.contains(LinkAction.EXTERNAL)) {
			menu.add(new LinkMenuItem(
					activity,
					R.string.action_external,
					LinkAction.EXTERNAL));
		}
		if(itemPref.contains(LinkAction.SAVE_IMAGE)
				&& isProbablyAnImage(uri)
				&& !forceNoImage) {
			menu.add(new LinkMenuItem(
					activity,
					R.string.action_save_image,
					LinkAction.SAVE_IMAGE));
		}
		if(itemPref.contains(LinkAction.SHARE)) {
			menu.add(new LinkMenuItem(activity, R.string.action_share, LinkAction.SHARE));
		}
		if(itemPref.contains(LinkAction.SHARE_IMAGE)
				&& isProbablyAnImage(uri)
				&& !forceNoImage) {
			menu.add(new LinkMenuItem(
					activity,
					R.string.action_share_image,
					LinkAction.SHARE_IMAGE));
		}
		final String[] menuText = new String[menu.size()];

		for(int i = 0; i < menuText.length; i++) {
			menuText[i] = menu.get(i).title;
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setItems(menuText,
				(dialog, which) -> onActionMenuItemSelected(uri, activity, menu.get(which).action));

		//builder.setNeutralButton(R.string.dialog_cancel, null);

		final AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(true);
		alert.show();
	}

	public static void onActionMenuItemSelected(
			final String uri,
			final BaseActivity activity,
			final LinkAction action) {
		switch(action) {
			case SHARE:
				shareText(activity, null, uri);
				break;
			case COPY_URL:
				final ClipboardManager clipboardManager
						= (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
				if(clipboardManager != null) {
					// Using newPlainText here instead of newRawUri because links from
					// comments/self-text are often not valid URIs
					final ClipData data = ClipData.newPlainText(null, uri);
					clipboardManager.setPrimaryClip(data);

					General.quickToast(
							activity.getApplicationContext(),
							R.string.link_copied_to_clipboard);
				}
				break;

			case EXTERNAL:
				try {
					final Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(uri));
					activity.startActivity(intent);
				} catch(final ActivityNotFoundException e) {
					General.quickToast(
							activity,
							R.string.error_no_suitable_apps_available);
				}
				break;
			case SHARE_IMAGE:
				FileUtils.shareImageAtUri(activity, uri);
				break;
			case SAVE_IMAGE:
				FileUtils.saveImageAtUri(activity, uri);
				break;
		}
	}

	public static boolean openWebBrowser(
			final AppCompatActivity activity,
			final Uri uri,
			final boolean fromExternalIntent) {

		if(!fromExternalIntent) {
			try {
				final Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(uri);
				activity.startActivity(intent);
				return true;

			} catch(final Exception e) {
				General.quickToast(
						activity,
						String.format(
								activity.getString(
										R.string.error_toast_failed_open_external_browser),
								uri.toString()));
			}

		} else {

			// We want to make sure we don't just pass this back to ourselves

			final Intent baseIntent = new Intent(Intent.ACTION_VIEW);
			baseIntent.setData(uri);

			final ArrayList<Intent> targetIntents = new ArrayList<>();

			for(final ResolveInfo info : activity.getPackageManager()
					.queryIntentActivities(baseIntent, 0)) {

				final String packageName = info.activityInfo.packageName;
				Log.i("RRDEBUG", "Considering " + packageName);

				if(packageName != null && !packageName.startsWith(
						"org.quantumbadger.redreader")) {
					final Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(uri);
					intent.setPackage(packageName);
					targetIntents.add(intent);
				}
			}

			if(!targetIntents.isEmpty()) {

				final Intent chooserIntent = Intent.createChooser(
						targetIntents.remove(0),
						activity.getString(R.string.open_with));

				if(!targetIntents.isEmpty()) {
					chooserIntent.putExtra(
							Intent.EXTRA_INITIAL_INTENTS,
							targetIntents.toArray(new Parcelable[] {}));
				}
				activity.startActivity(chooserIntent);

				return true;
			}
		}

		return false;
	}

	public static void openInternalBrowser(
			final AppCompatActivity activity,
			final String url,
			@Nullable final RedditPost post) {

		final Intent intent = new Intent();
		intent.setClass(activity, WebViewActivity.class);
		intent.putExtra("url", url);
		intent.putExtra("post", post);
		activity.startActivity(intent);
	}

	@TargetApi(18)
	public static void openCustomTab(
			final AppCompatActivity activity,
			final Uri uri,
			@Nullable final RedditPost post) {

		try {
			final Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(uri);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			final Bundle bundle = new Bundle();
			bundle.putBinder("android.support.customtabs.extra.SESSION", null);
			intent.putExtras(bundle);

			intent.putExtra("android.support.customtabs.extra.SHARE_MENU_ITEM", true);

			final TypedValue typedValue = new TypedValue();
			activity.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);

			intent.putExtra(
					"android.support.customtabs.extra.TOOLBAR_COLOR",
					typedValue.data);

			intent.putExtra("android.support.customtabs.extra.ENABLE_URLBAR_HIDING", true);

			activity.startActivity(intent);

		} catch(final ActivityNotFoundException e) {
			// No suitable web browser installed. Use internal browser.
			openInternalBrowser(activity, uri.toString(), post);
		}
	}

	public static final Pattern imgurPattern = Pattern.compile(
			".*[^A-Za-z]imgur\\.com/(\\w+).*"),
			imgurAlbumPattern = Pattern.compile(
					".*[^A-Za-z]imgur\\.com/(a|gallery)/(\\w+).*"),
			redditGalleryPattern = Pattern.compile(
					".*[^A-Za-z]reddit\\.com/gallery/(\\w+).*"),
			qkmePattern1 = Pattern.compile(".*[^A-Za-z]qkme\\.me/(\\w+).*"),
			qkmePattern2 = Pattern.compile(".*[^A-Za-z]quickmeme\\.com/meme/(\\w+).*"),
			lvmePattern = Pattern.compile(".*[^A-Za-z]livememe\\.com/(\\w+).*"),
			gfycatPattern = Pattern.compile(
					".*[^A-Za-z]gfycat\\.com/(?:gifs/detail/)?(\\w+).*"),
			redgifsPattern = Pattern.compile(
					".*[^A-Za-z]redgifs\\.com/watch/(?:gifs/detail/)?(\\w+).*"),
			streamablePattern = Pattern.compile(".*[^A-Za-z]streamable\\.com/(\\w+).*"),
			reddituploadsPattern = Pattern.compile(
					".*[^A-Za-z]i\\.reddituploads\\.com/(\\w+).*"),
			redditVideosPattern = Pattern.compile(".*[^A-Za-z]v.redd.it/(\\w+).*"),
			imgflipPattern = Pattern.compile(".*[^A-Za-z]imgflip\\.com/i/(\\w+).*"),
			makeamemePattern = Pattern.compile(
					".*[^A-Za-z]makeameme\\.org/meme/([\\w\\-]+).*"),
			deviantartPattern = Pattern.compile(
					"https://www\\.deviantart\\.com/([\\w\\-]+)/art/([\\w\\-]+)"),
			giphyPattern = Pattern.compile(".*[^A-Za-z]giphy\\.com/gifs/(\\w+).*");

	public static boolean isProbablyAnImage(final String url) {

		{
			final Matcher matchImgur = imgurPattern.matcher(url);

			if(matchImgur.find()) {
				final String imgId = matchImgur.group(1);
				if(imgId.length() > 2 && !imgId.startsWith("gallery")) {
					return true;
				}
			}
		}

		{
			final Matcher matchGfycat = gfycatPattern.matcher(url);

			if(matchGfycat.find()) {
				final String imgId = matchGfycat.group(1);
				if(imgId.length() > 5) {
					return true;
				}
			}
		}

		{
			final Matcher matchRedgifs = redgifsPattern.matcher(url);

			if(matchRedgifs.find()) {
				final String imgId = matchRedgifs.group(1);
				if(imgId.length() > 5) {
					return true;
				}
			}
		}

		{
			final Matcher matchStreamable = streamablePattern.matcher(url);

			if(matchStreamable.find()) {
				final String imgId = matchStreamable.group(1);
				if(imgId.length() > 2) {
					return true;
				}
			}
		}

		{
			final Matcher matchRedditUploads = reddituploadsPattern.matcher(url);

			if(matchRedditUploads.find()) {
				final String imgId = matchRedditUploads.group(1);
				if(imgId.length() > 10) {
					return true;
				}
			}
		}

		{
			final Matcher matchImgflip = imgflipPattern.matcher(url);

			if(matchImgflip.find()) {
				final String imgId = matchImgflip.group(1);
				if(imgId.length() > 3) {
					return true;
				}
			}
		}

		{
			final Matcher matchMakeameme = makeamemePattern.matcher(url);

			if(matchMakeameme.find()) {
				final String imgId = matchMakeameme.group(1);
				if(imgId.length() > 3) {
					return true;
				}
			}
		}

		{
			final Matcher matchDeviantart = deviantartPattern.matcher(url);

			if(matchDeviantart.find()) {
				final String imgId = url;
				if(imgId.length() > 40) {
					return true;
				}
			}
		}

		{
			final Matcher matchRedditVideos = redditVideosPattern.matcher(url);

			if(matchRedditVideos.find()) {
				final String imgId = matchRedditVideos.group(1);
				if(imgId.length() > 3) {
					return true;
				}
			}
		}

		return getImageUrlPatternMatch(url) != null;
	}

	private static abstract class ImageInfoRetryListener implements GetImageInfoListener {

		private final GetImageInfoListener mListener;

		private ImageInfoRetryListener(final GetImageInfoListener listener) {
			mListener = listener;
		}

		@Override
		public abstract void onFailure(
				final @CacheRequest.RequestFailureType int type,
				final Throwable t,
				final Integer status,
				final String readableMessage);

		@Override
		public void onSuccess(final ImageInfo info) {
			mListener.onSuccess(info);
		}

		@Override
		public void onNotAnImage() {
			mListener.onNotAnImage();
		}
	}

	public static void getImgurImageInfo(
			final Context context,
			final String imgId,
			final int priority,
			final int listId,
			final boolean returnUrlOnFailure,
			final GetImageInfoListener listener) {

		Log.i("getImgurImageInfo", "Image " + imgId + ": trying API v3 with auth");

		ImgurAPIV3.getImageInfo(
				context,
				imgId,
				priority,
				listId,
				true,
				new ImageInfoRetryListener(listener) {
					@Override
					public void onFailure(
							final @CacheRequest.RequestFailureType int type,
							final Throwable t,
							final Integer status,
							final String readableMessage) {

						Log.i(
								"getImgurImageInfo",
								"Image " + imgId + ": trying API v3 without auth");

						ImgurAPIV3.getImageInfo(
								context,
								imgId,
								priority,
								listId,
								false,
								new ImageInfoRetryListener(listener) {
									@Override
									public void onFailure(
											final @CacheRequest.RequestFailureType int type,
											final Throwable t,
											final Integer status,
											final String readableMessage) {

										Log.i(
												"getImgurImageInfo",
												"Image " + imgId + ": trying API v2");

										ImgurAPI.getImageInfo(
												context,
												imgId,
												priority,
												listId,
												new ImageInfoRetryListener(listener) {
													@Override
													public void onFailure(
															final @CacheRequest.RequestFailureType
																	int type,
															final Throwable t,
															final Integer status,
															final String readableMessage) {

														Log.i(
																"getImgurImageInfo",
																"All API requests failed!");

														if(returnUrlOnFailure) {
															listener.onSuccess(new ImageInfo(
																	"https://i.imgur.com/"
																			+ imgId
																			+ ".jpg",
																	null,
																	ImageInfo.HasAudio.MAYBE_AUDIO
															));

														} else {
															listener.onFailure(
																	type,
																	t,
																	status,
																	readableMessage);
														}
													}
												});
									}
								});
					}
				});
	}

	private static abstract class AlbumInfoRetryListener implements GetAlbumInfoListener {

		private final GetAlbumInfoListener mListener;

		private AlbumInfoRetryListener(final GetAlbumInfoListener listener) {
			mListener = listener;
		}

		@Override
		public abstract void onFailure(
				final @CacheRequest.RequestFailureType int type,
				final Throwable t,
				final Integer status,
				final String readableMessage);

		@Override
		public void onGalleryRemoved() {
			mListener.onGalleryRemoved();
		}

		@Override
		public void onGalleryDataNotPresent() {
			mListener.onGalleryDataNotPresent();
		}

		@Override
		public void onSuccess(@NonNull final AlbumInfo info) {
			mListener.onSuccess(info);
		}
	}

	public static void getImgurAlbumInfo(
			final Context context,
			final String albumUrl,
			final String albumId,
			final int priority,
			final int listId,
			final GetAlbumInfoListener listener) {

		Log.i("getImgurAlbumInfo", "Album " + albumId + ": trying API v3 with auth");

		ImgurAPIV3.getAlbumInfo(
				context,
				albumUrl,
				albumId,
				priority,
				listId,
				true,
				new AlbumInfoRetryListener(listener) {
					@Override
					public void onFailure(
							final @CacheRequest.RequestFailureType int type,
							final Throwable t,
							final Integer status,
							final String readableMessage) {

						Log.i(
								"getImgurAlbumInfo",
								"Album " + albumId + ": trying API v3 without auth");

						ImgurAPIV3.getAlbumInfo(
								context,
								albumUrl,
								albumId,
								priority,
								listId,
								false,
								new AlbumInfoRetryListener(listener) {
									@Override
									public void onFailure(
											final @CacheRequest.RequestFailureType int type,
											final Throwable t,
											final Integer status,
											final String readableMessage) {

										Log.i(
												"getImgurAlbumInfo",
												"Album " + albumId + ": trying API v2");

										ImgurAPI.getAlbumInfo(
												context,
												albumUrl,
												albumId,
												priority,
												listId,
												new AlbumInfoRetryListener(listener) {
													@Override
													public void onFailure(
															final @CacheRequest.RequestFailureType
																	int type,
															final Throwable t,
															final Integer status,
															final String readableMessage) {

														Log.i(
																"getImgurImageInfo",
																"All API requests failed!");
														listener.onFailure(
																type,
																t,
																status,
																readableMessage);
													}
												});

									}
								});
					}
				});
	}

	public static void getAlbumInfo(
			final Context context,
			final String url,
			final int priority,
			final int listId,
			final GetAlbumInfoListener listener) {

		{
			final Matcher matchImgur = imgurAlbumPattern.matcher(url);

			if(matchImgur.find()) {
				final String albumId = matchImgur.group(2);
				if(albumId.length() > 2) {
					getImgurAlbumInfo(context, url, albumId, priority, listId, listener);
					return;
				}
			}
		}

		{
			final Matcher matchReddit = redditGalleryPattern.matcher(url);

			if(matchReddit.find()) {
				final String albumId = matchReddit.group(1);
				if(albumId.length() > 2) {
					RedditGalleryAPI.getAlbumInfo(
							context,
							url,
							albumId,
							priority,
							listId,
							listener);
					return;
				}
			}
		}

		listener.onFailure(
				CacheRequest.REQUEST_FAILURE_MALFORMED_URL,
				null,
				null,
				"Cannot parse '" + url + "' as an album URL");
	}

	public static void getImageInfo(
			final Context context,
			final String url,
			final int priority,
			final int listId,
			final GetImageInfoListener listener) {

		{
			final Matcher matchImgur = imgurPattern.matcher(url);

			if(matchImgur.find()) {
				final String imgId = matchImgur.group(1);
				if(imgId.length() > 2 && !imgId.startsWith("gallery")) {
					getImgurImageInfo(context, imgId, priority, listId, true, listener);
					return;
				}
			}
		}

		{
			final Matcher matchGfycat = gfycatPattern.matcher(url);

			if(matchGfycat.find()) {
				final String imgId = matchGfycat.group(1);
				if(imgId.length() > 5) {
					GfycatAPI.getImageInfo(context, imgId, priority, listId, listener);
					return;
				}
			}
		}

		{
			final Matcher matchRedgifs = redgifsPattern.matcher(url);

			if(matchRedgifs.find()) {
				final String imgId = matchRedgifs.group(1);
				if(imgId.length() > 5) {
					RedgifsAPI.getImageInfo(context, imgId, priority, listId, listener);
					return;
				}
			}
		}

		{
			final Matcher matchStreamable = streamablePattern.matcher(url);

			if(matchStreamable.find()) {
				final String imgId = matchStreamable.group(1);
				if(imgId.length() > 2) {
					StreamableAPI.getImageInfo(
							context,
							imgId,
							priority,
							listId,
							listener);
					return;
				}
			}
		}
		{
			final Matcher matchDeviantart = deviantartPattern.matcher(url);

			if(matchDeviantart.find()) {
				final String imgId = url;
				if(imgId.length() > 40) {
					DeviantArtAPI.getImageInfo(
							context,
							imgId,
							priority,
							listId,
							listener);
					return;
				}
			}
		}
		{
			final Matcher matchRedditVideos = redditVideosPattern.matcher(url);

			if(matchRedditVideos.find()) {

				final String imgId = matchRedditVideos.group(1);
				if(imgId.length() > 3) {
					RedditVideosAPI.getImageInfo(
							context,
							imgId,
							priority,
							listId,
							listener);
					return;
				}
			}
		}

		final ImageInfo imageUrlPatternMatch = getImageUrlPatternMatch(url);

		if(imageUrlPatternMatch != null) {
			listener.onSuccess(imageUrlPatternMatch);
		} else {
			listener.onNotAnImage();
		}
	}


	public static boolean isProbablyDisplayableInline(final String url) {

		final String urlLower = StringUtils.asciiLowercase(url);

		{
			final Matcher matchRedditUploads = reddituploadsPattern.matcher(url);

			if(matchRedditUploads.find()) {
				final String imgId = matchRedditUploads.group(1);
				if(imgId.length() > 10) {
					return new ImageInfo(
							url,
							ImageInfo.MediaType.IMAGE,
							ImageInfo.HasAudio.NO_AUDIO) != null;
				}
			}
		}


		final String[] imageExtensions = {".jpg", ".jpeg", ".png"};

		for(final String ext : imageExtensions) {
			if(urlLower.endsWith(ext)) {
				return new ImageInfo(
						url,
						ImageInfo.MediaType.IMAGE,
						ImageInfo.HasAudio.NO_AUDIO) != null;
			}
		}

		if(url.contains("?")) {

			final String urlBeforeQ = urlLower.split("\\?")[0];

			for(final String ext : imageExtensions) {
				if(urlBeforeQ.endsWith(ext)) {
					return new ImageInfo(
							url,
							ImageInfo.MediaType.IMAGE,
							ImageInfo.HasAudio.MAYBE_AUDIO) != null;
				}
			}

		}


		final Matcher matchQkme1 = qkmePattern1.matcher(url);

		if(matchQkme1.find()) {
			final String imgId = matchQkme1.group(1);
			if(imgId.length() > 2) {
				return new ImageInfo(String.format(
						Locale.US,
						"http://i.qkme.me/%s.jpg",
						imgId), ImageInfo.MediaType.IMAGE, ImageInfo.HasAudio.NO_AUDIO) != null;
			}
		}

		final Matcher matchQkme2 = qkmePattern2.matcher(url);

		if(matchQkme2.find()) {
			final String imgId = matchQkme2.group(1);
			if(imgId.length() > 2) {
				return new ImageInfo(String.format(
						Locale.US,
						"http://i.qkme.me/%s.jpg",
						imgId), ImageInfo.MediaType.IMAGE, ImageInfo.HasAudio.NO_AUDIO) != null;
			}
		}

		final Matcher matchLvme = lvmePattern.matcher(url);

		if(matchLvme.find()) {
			final String imgId = matchLvme.group(1);
			if(imgId.length() > 2) {
				return new ImageInfo(String.format(
						Locale.US,
						"http://www.livememe.com/%s.jpg",
						imgId), ImageInfo.MediaType.IMAGE, ImageInfo.HasAudio.NO_AUDIO) != null;
			}
		}
		return false;
	}

	public static ImageInfo getImageUrlPatternMatch(final String url) {

		final String urlLower = StringUtils.asciiLowercase(url);

		{
			final Matcher matchRedditUploads = reddituploadsPattern.matcher(url);

			if(matchRedditUploads.find()) {
				final String imgId = matchRedditUploads.group(1);
				if(imgId.length() > 10) {
					return new ImageInfo(
							url,
							ImageInfo.MediaType.IMAGE,
							ImageInfo.HasAudio.NO_AUDIO);
				}
			}
		}

		{
			final Matcher matchImgflip = imgflipPattern.matcher(url);

			if(matchImgflip.find()) {
				final String imgId = matchImgflip.group(1);
				if(imgId.length() > 3) {
					final String imageUrl = "https://i.imgflip.com/" + imgId + ".jpg";
					return new ImageInfo(
							imageUrl,
							ImageInfo.MediaType.IMAGE,
							ImageInfo.HasAudio.NO_AUDIO);
				}
			}
		}

		{
			final Matcher matchMakeameme = makeamemePattern.matcher(url);

			if(matchMakeameme.find()) {
				final String imgId = matchMakeameme.group(1);
				if(imgId.length() > 3) {
					final String imageUrl = "https://media.makeameme.org/created/"
							+ imgId
							+ ".jpg";
					return new ImageInfo(
							imageUrl,
							ImageInfo.MediaType.IMAGE,
							ImageInfo.HasAudio.NO_AUDIO);
				}
			}
		}

		{
			final Matcher matchGiphy = giphyPattern.matcher(url);

			if(matchGiphy.find()) {
				return new ImageInfo(
						"https://media.giphy.com/media/"
								+ matchGiphy.group(1)
								+ "/giphy.mp4",
						ImageInfo.MediaType.VIDEO,
						ImageInfo.HasAudio.NO_AUDIO);
			}
		}

		final String[] imageExtensions = {".jpg", ".jpeg", ".png"};

		final String[] videoExtensions = {
				".webm",
				".mp4",
				".h264",
				".gifv",
				".mkv",
				".3gp"};


		for(final String ext : imageExtensions) {
			if(urlLower.endsWith(ext)) {
				return new ImageInfo(
						url,
						ImageInfo.MediaType.IMAGE,
						ImageInfo.HasAudio.MAYBE_AUDIO);
			}
		}

		for(final String ext : videoExtensions) {
			if(urlLower.endsWith(ext)) {
				return new ImageInfo(
						url,
						ImageInfo.MediaType.VIDEO,
						ImageInfo.HasAudio.MAYBE_AUDIO);
			}
		}

		if(urlLower.endsWith(".gif")) {
			return new ImageInfo(
					url,
					ImageInfo.MediaType.GIF,
					ImageInfo.HasAudio.MAYBE_AUDIO);
		}


		if(url.contains("?")) {

			final String urlBeforeQ = urlLower.split("\\?")[0];

			for(final String ext : imageExtensions) {
				if(urlBeforeQ.endsWith(ext)) {
					return new ImageInfo(
							url,
							ImageInfo.MediaType.IMAGE,
							ImageInfo.HasAudio.MAYBE_AUDIO);
				}
			}

			for(final String ext : videoExtensions) {
				if(urlBeforeQ.endsWith(ext)) {
					return new ImageInfo(
							url,
							ImageInfo.MediaType.VIDEO,
							ImageInfo.HasAudio.MAYBE_AUDIO);
				}
			}

			if(urlBeforeQ.endsWith(".gif")) {
				return new ImageInfo(
						url,
						ImageInfo.MediaType.GIF,
						ImageInfo.HasAudio.MAYBE_AUDIO);
			}
		}

		final Matcher matchQkme1 = qkmePattern1.matcher(url);

		if(matchQkme1.find()) {
			final String imgId = matchQkme1.group(1);
			if(imgId.length() > 2) {
				return new ImageInfo(String.format(
						Locale.US,
						"http://i.qkme.me/%s.jpg",
						imgId), ImageInfo.MediaType.IMAGE, ImageInfo.HasAudio.NO_AUDIO);
			}
		}

		final Matcher matchQkme2 = qkmePattern2.matcher(url);

		if(matchQkme2.find()) {
			final String imgId = matchQkme2.group(1);
			if(imgId.length() > 2) {
				return new ImageInfo(String.format(
						Locale.US,
						"http://i.qkme.me/%s.jpg",
						imgId), ImageInfo.MediaType.IMAGE, ImageInfo.HasAudio.NO_AUDIO);
			}
		}

		final Matcher matchLvme = lvmePattern.matcher(url);

		if(matchLvme.find()) {
			final String imgId = matchLvme.group(1);
			if(imgId.length() > 2) {
				return new ImageInfo(String.format(
						Locale.US,
						"http://www.livememe.com/%s.jpg",
						imgId), ImageInfo.MediaType.IMAGE, ImageInfo.HasAudio.NO_AUDIO);
			}
		}

		return null;

	}

	public static LinkedHashSet<String> computeAllLinks(final String text) {

		final LinkedHashSet<String> result = new LinkedHashSet<>();

		// From http://stackoverflow.com/a/1806161/1526861
		// TODO may not handle .co.uk, similar (but should handle .co/.us/.it/etc fine)
		final Pattern urlPattern = Pattern.compile(
				"\\b((((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
						"(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
						"|mil|biz|info|mobi|name|aero|jobs|museum" +
						"|travel|[a-z]{2}))(:[\\d]{1,5})?" +
						"(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
						"((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
						"([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
						"(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
						"([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
						"(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?)\\b");

		final Matcher urlMatcher = urlPattern.matcher(text);

		while(urlMatcher.find()) {
			result.add(urlMatcher.group(1));
		}

		final Matcher subredditMatcher = Pattern.compile("(?<!\\w)(/?[ru]/\\w+)\\b")
				.matcher(text);

		while(subredditMatcher.find()) {
			result.add(subredditMatcher.group(1));
		}

		return result;
	}

	private static class LinkMenuItem {
		public final String title;
		public final LinkAction action;

		private LinkMenuItem(final Context context, final int titleRes, final LinkAction action) {
			this.title = context.getString(titleRes);
			this.action = action;
		}
	}

	public static void shareText(
			@NonNull final AppCompatActivity activity,
			@Nullable final String subject,
			@NonNull final String text) {

		final Intent mailer = new Intent(Intent.ACTION_SEND);
		mailer.setType("text/plain");
		mailer.putExtra(Intent.EXTRA_TEXT, text);

		if(subject != null) {
			mailer.putExtra(Intent.EXTRA_SUBJECT, subject);
		}

		if(PrefsUtility.pref_behaviour_sharing_dialog(
				activity,
				PreferenceManager.getDefaultSharedPreferences(activity))) {
			ShareOrderDialog.newInstance(mailer).show(
					activity.getSupportFragmentManager(),
					null);

		} else {
			activity.startActivity(Intent.createChooser(
					mailer,
					activity.getString(R.string.action_share)));
		}
	}
}
