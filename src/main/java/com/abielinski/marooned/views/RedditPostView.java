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

package com.abielinski.marooned.views;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import com.abielinski.marooned.R;
import com.abielinski.marooned.activities.BaseActivity;
import com.abielinski.marooned.cache.CacheEntry;
import com.abielinski.marooned.cache.CacheManager;
import com.abielinski.marooned.common.FileUtils;
import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.PrefsUtility;
import com.abielinski.marooned.fragments.PostListingFragment;
import com.abielinski.marooned.image.BitmapCache;
import com.abielinski.marooned.reddit.prepared.RedditPreparedPost;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class RedditPostView extends FlingableItemView
		implements RedditPreparedPost.ThumbnailLoadedCallback {

	private final float dpScale;
	private final int thumbnailSize;
	private final boolean displayInlineImages;

	private RedditPreparedPost post = null;
	private final TextView title, subtitle, title_alternate;

	private final ImageView thumbnailView, overlayIcon, postImageView;
	private final TextView thumbnailOverlay;

	private final LinearLayout mOuterView;
	private final LinearLayout mInnerView;
	private final LinearLayout commentsButton;
	private final TextView commentsText;

	private int usageId = 0;

	private final Handler thumbnailHandler;

	private final BaseActivity mActivity;

	private final PrefsUtility.PostFlingAction mLeftFlingPref, mRightFlingPref;
	private ActionDescriptionPair mLeftFlingAction, mRightFlingAction;

	private final boolean mCommentsButtonPref;

	private boolean mImageIsRendering = false;
	private long mImageStartRender = -1;

	private final int
			rrPostTitleReadCol,
			rrPostTitleCol,
			rrListItemBackgroundCol,
			rrPostCommentsButtonBackCol;

	@Override
	protected void onSetItemFlingPosition(final float position) {
		mOuterView.setTranslationX(position);
	}

	@NonNull
	@Override
	protected String getFlingLeftText() {

		mLeftFlingAction = chooseFlingAction(mLeftFlingPref);

		if(mLeftFlingAction != null) {
			return mActivity.getString(mLeftFlingAction.descriptionRes);
		} else {
			return "Disabled";
		}
	}

	@NonNull
	@Override
	protected String getFlingRightText() {

		mRightFlingAction = chooseFlingAction(mRightFlingPref);

		if(mRightFlingAction != null) {
			return mActivity.getString(mRightFlingAction.descriptionRes);
		} else {
			return "Disabled";
		}
	}

	@Override
	protected boolean allowFlingingLeft() {
		return mLeftFlingAction != null;
	}

	@Override
	protected boolean allowFlingingRight() {
		return mRightFlingAction != null;
	}

	@Override
	protected void onFlungLeft() {
		RedditPreparedPost.onActionMenuItemSelected(
				post,
				mActivity,
				mLeftFlingAction.action);
	}

	@Override
	protected void onFlungRight() {
		RedditPreparedPost.onActionMenuItemSelected(
				post,
				mActivity,
				mRightFlingAction.action);
	}

	private static final class ActionDescriptionPair {
		public final RedditPreparedPost.Action action;
		public final int descriptionRes;

		private ActionDescriptionPair(
				final RedditPreparedPost.Action action,
				final int descriptionRes) {
			this.action = action;
			this.descriptionRes = descriptionRes;
		}
	}

	private ActionDescriptionPair chooseFlingAction(final PrefsUtility.PostFlingAction pref) {

		switch(pref) {

			case UPVOTE:
				if(post.isUpvoted()) {
					return new ActionDescriptionPair(
							RedditPreparedPost.Action.UNVOTE,
							R.string.action_vote_remove);
				} else {
					return new ActionDescriptionPair(
							RedditPreparedPost.Action.UPVOTE,
							R.string.action_upvote);
				}

			case DOWNVOTE:
				if(post.isDownvoted()) {
					return new ActionDescriptionPair(
							RedditPreparedPost.Action.UNVOTE,
							R.string.action_vote_remove);
				} else {
					return new ActionDescriptionPair(
							RedditPreparedPost.Action.DOWNVOTE,
							R.string.action_downvote);
				}

			case SAVE:
				if(post.isSaved()) {
					return new ActionDescriptionPair(
							RedditPreparedPost.Action.UNSAVE,
							R.string.action_unsave);
				} else {
					return new ActionDescriptionPair(
							RedditPreparedPost.Action.SAVE,
							R.string.action_save);
				}

			case HIDE:
				if(post.isHidden()) {
					return new ActionDescriptionPair(
							RedditPreparedPost.Action.UNHIDE,
							R.string.action_unhide);
				} else {
					return new ActionDescriptionPair(
							RedditPreparedPost.Action.HIDE,
							R.string.action_hide);
				}

			case COMMENTS:
				return new ActionDescriptionPair(
						RedditPreparedPost.Action.COMMENTS,
						R.string.action_comments_short);

			case LINK:
				return new ActionDescriptionPair(
						RedditPreparedPost.Action.LINK,
						R.string.action_link_short);

			case BROWSER:
				return new ActionDescriptionPair(
						RedditPreparedPost.Action.EXTERNAL,
						R.string.action_external_short);

			case ACTION_MENU:
				return new ActionDescriptionPair(
						RedditPreparedPost.Action.ACTION_MENU,
						R.string.action_actionmenu_short);

			case BACK:
				return new ActionDescriptionPair(
						RedditPreparedPost.Action.BACK,
						R.string.action_back);
		}

		return null;
	}

	public RedditPostView(
			final Context context,
			final PostListingFragment fragmentParent,
			final BaseActivity activity,
			final boolean leftHandedMode) {

		super(context);
		mActivity = activity;

		thumbnailHandler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(@NonNull final Message msg) {
				if(usageId != msg.what) {
					return;
				}
				thumbnailView.setImageBitmap((Bitmap)msg.obj);
			}
		};

		dpScale = context.getResources().getDisplayMetrics().density; // TODO xml?
		thumbnailSize = General.getThumbnailSize(context);

		final float titleFontScale = PrefsUtility.appearance_fontscale_posts(
				context,
				PreferenceManager.getDefaultSharedPreferences(context));
		final float subtitleFontScale = PrefsUtility.appearance_fontscale_post_subtitles(
				context,
				PreferenceManager.getDefaultSharedPreferences(context));
		displayInlineImages = PrefsUtility.appearance_inline_images_show(
				context,
				PreferenceManager.getDefaultSharedPreferences(context));

		final View rootView =
				LayoutInflater.from(context).inflate(R.layout.reddit_post, this, true);

		mOuterView = rootView.findViewById(R.id.reddit_post_wrapper);
		mInnerView = rootView.findViewById(R.id.reddit_post_layout);

		mOuterView.setOnClickListener(v -> fragmentParent.onPostSelected(post));

		mOuterView.setOnLongClickListener(v -> {
			RedditPreparedPost.showActionMenu(mActivity, post);
			return true;
		});

		thumbnailView = rootView.findViewById(R.id.reddit_post_thumbnail_view);
		overlayIcon = rootView.findViewById(R.id.reddit_post_overlay_icon);
		postImageView = rootView.findViewById(R.id.reddit_post_image_view);
		thumbnailOverlay = rootView.findViewById(R.id.reddit_post_thumbnail_overlay);

		title = rootView.findViewById(R.id.reddit_post_title);
		title_alternate = rootView.findViewById(R.id.reddit_post_title_alternate);
		subtitle = rootView.findViewById(R.id.reddit_post_subtitle);

		final SharedPreferences sharedPreferences =
				PreferenceManager.getDefaultSharedPreferences(context);

		mCommentsButtonPref =
				PrefsUtility.appearance_post_show_comments_button(context, sharedPreferences);

		commentsButton =
				rootView.findViewById(R.id.reddit_post_comments_button);
		commentsText =
				commentsButton.findViewById(R.id.reddit_post_comments_text);

		if(!mCommentsButtonPref) {
			mOuterView.removeView(commentsButton);
		}

		if(leftHandedMode) {
			final ArrayList<View> outerViewElements = new ArrayList<>(3);
			for(int i = mInnerView.getChildCount() - 1; i >= 0; i--) {
				outerViewElements.add(mInnerView.getChildAt(i));
				mInnerView.removeViewAt(i);
			}

			for(int i = 0; i < outerViewElements.size(); i++) {
				mInnerView.addView(outerViewElements.get(i));
			}

			mInnerView.setNextFocusRightId(NO_ID);
			if (mCommentsButtonPref) {
				mInnerView.setNextFocusLeftId(commentsButton.getId());

				commentsButton.setNextFocusForwardId(R.id.reddit_post_wrapper);
				commentsButton.setNextFocusRightId(R.id.reddit_post_wrapper);
				commentsButton.setNextFocusLeftId(NO_ID);
			}
		}

		if(mCommentsButtonPref) {
			commentsButton.setOnClickListener(v -> fragmentParent.onPostCommentsSelected(post));
		}

		title.setTextSize(
				TypedValue.COMPLEX_UNIT_PX,
				title.getTextSize() * titleFontScale);
		title_alternate.setTextSize(
				TypedValue.COMPLEX_UNIT_PX,
				title.getTextSize() * titleFontScale);
		subtitle.setTextSize(
				TypedValue.COMPLEX_UNIT_PX,
				subtitle.getTextSize() * subtitleFontScale);

		mLeftFlingPref =
				PrefsUtility.pref_behaviour_fling_post_left(context, sharedPreferences);
		mRightFlingPref =
				PrefsUtility.pref_behaviour_fling_post_right(context, sharedPreferences);

		{
			final TypedArray attr = context.obtainStyledAttributes(new int[] {
					R.attr.rrPostTitleCol,
					R.attr.rrPostTitleReadCol,
					R.attr.rrListItemBackgroundCol,
					R.attr.rrPostCommentsButtonBackCol
			});

			rrPostTitleCol = attr.getColor(0, 0);
			rrPostTitleReadCol = attr.getColor(1, 0);
			rrListItemBackgroundCol = attr.getColor(2, 0);
			rrPostCommentsButtonBackCol = attr.getColor(3, 0);
			attr.recycle();
		}
	}

	private Uri getURIFromCache(String url, Activity activity){

		if(url == null) {
			return null;
		}
		final URI uri = General.uriFromString(url);

		if(uri == null) {
			return null;
		}

		CacheManager cacheMgr = CacheManager.getInstance(activity);

		final List<CacheEntry> result = cacheMgr.getSessions(uri, "");

		if(!result.isEmpty()) {

			CacheEntry entry = null;

			for(final CacheEntry e : result) {
				if(entry == null || entry.timestamp < e.timestamp) {
					entry = e;
				}
			}
			CacheManager.ReadableCacheFile cacheFile = cacheMgr.getExistingCacheFileById(entry.id);
			return cacheFile.getUri();
		}

		return null;
	}

	@UiThread
	public void reset(final RedditPreparedPost data, boolean oldCached) {

		if(data != post) {

			usageId++;

			resetSwipeState();

			final Bitmap thumbnail = data.getThumbnail(this, usageId);
			thumbnailView.setImageBitmap(thumbnail);
			thumbnailView.setVisibility(VISIBLE);

			postImageView.setVisibility(GONE);
			postImageView.setImageResource(android.R.color.transparent);
			mImageIsRendering = false;

			title.setVisibility(VISIBLE);
			title_alternate.setVisibility(GONE);

			title.setText(data.src.getTitle());
			title_alternate.setText(data.src.getTitle());
			if(mCommentsButtonPref) {
				commentsText.setText(General.shortScore(data.src.getSrc().num_comments));
			}


			thumbnailView.setMinimumWidth(thumbnailSize);
			thumbnailView.setMinimumHeight(thumbnailSize);
			if(data.hasThumbnail) {
				thumbnailView.setVisibility(VISIBLE);
				thumbnailView.getLayoutParams().height =
						ViewGroup.LayoutParams.MATCH_PARENT;
				thumbnailView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			} else {
				thumbnailView.setVisibility(INVISIBLE);
			}

			overlayIcon.setMinimumHeight(thumbnailSize);
			overlayIcon.setMinimumWidth(thumbnailSize);

			int durationSeconds = data.src.getDuration();
			if (durationSeconds != 0){
				int minutes = durationSeconds / 60;
				String seconds = String.format("%1$2s", durationSeconds % 60).replace(' ', '0');
				thumbnailOverlay.setText(minutes + ":" + seconds);
				thumbnailOverlay.setVisibility(VISIBLE);
			}else if (data.src.isGallery()) {
				thumbnailOverlay.setText(R.string.image_gallery);
				thumbnailOverlay.setVisibility(VISIBLE);
			}else{
				thumbnailOverlay.setVisibility(GONE);
			}

			if (data.mIsProbablyDisplayableInline && displayInlineImages){


				thumbnailView.setVisibility(GONE);
				postImageView.setVisibility(VISIBLE);
				title.setVisibility(GONE);
				title_alternate.setVisibility(VISIBLE);

				postImageView.setImageResource(R.drawable.ic_loading_dark);
				postImageView.setScaleType(ImageView.ScaleType.CENTER);

			}else{

				postImageView.setVisibility(GONE);
				postImageView.setImageResource(android.R.color.transparent);

			}
		}

		if(post != null) {
			post.unbind(this);
		}
		data.bind(this);
		data.cached = oldCached;

		this.post = data;

		updateAppearance();
	}

	public void updateAppearance() {

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

			mOuterView.setBackgroundResource(
					R.drawable.rr_postlist_item_selector_main);

			if(mCommentsButtonPref) {
				commentsButton.setBackgroundResource(
						R.drawable.rr_postlist_commentbutton_selector_main);
			}

		} else {
			// On KitKat and lower, we can't do easily themed highlighting
			mOuterView.setBackgroundColor(rrListItemBackgroundCol);
			if(mCommentsButtonPref) {
				commentsButton.setBackgroundColor(rrPostCommentsButtonBackCol);
			}
		}

		if(post.isRead()) {
			title.setTextColor(rrPostTitleReadCol);
			title_alternate.setTextColor(rrPostTitleReadCol);
		} else {
			title.setTextColor(rrPostTitleCol);
			title_alternate.setTextColor(rrPostTitleCol);
		}

		subtitle.setText(post.postListDescription);

		boolean overlayVisible = true;

		if(post.isSaved()) {
			overlayIcon.setImageResource(R.drawable.star_dark);

		} else if(post.isHidden()) {
			overlayIcon.setImageResource(R.drawable.ic_action_cross_dark);

		} else if(post.isUpvoted()) {
			overlayIcon.setImageResource(R.drawable.arrow_up_bold_orangered);

		} else if(post.isDownvoted()) {
			overlayIcon.setImageResource(R.drawable.arrow_down_bold_periwinkle);

		} else if(thumbnailView.getVisibility() == View.INVISIBLE && !post.mIsProbablyDisplayableInline){
			if (post.isSelf()){
				overlayIcon.setImageResource(R.drawable.ic_action_comments_dark);
			}else {
				overlayIcon.setImageResource(R.drawable.ic_action_link_dark);
			}

		} else {
			overlayVisible = false;
		}


		if (post.mIsProbablyDisplayableInline && displayInlineImages){


			if (!mImageIsRendering || post.failToShowInline){

				mImageStartRender = System.currentTimeMillis();

				Rect windowSize = new Rect ();
				final int margin = (int)(128.0f * dpScale);
				mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(windowSize);

				final int availableHeight = Math.min((int)(800.0f * dpScale), (int)windowSize.height() - margin);
				final int availableWidth = Math.min((int)(800.0f * dpScale), (int)windowSize.width());

				final int imageHeight = post.renderedImageHeight != 0 ?
						Math.min(post.renderedImageHeight, availableHeight)
						: availableHeight;

				final int imageWidth = availableWidth;

				final int sourceWidth = post.src.getSrc().getSourceWidth();
				final int sourceHeight = post.src.getSrc().getSourceHeight();

				float xScale = ((float)imageWidth) / (float)sourceWidth;
				float yScale = ((float)imageHeight) / (float)sourceHeight;
				float smallerScale = (xScale <= yScale) ? xScale : yScale;
				final int predictedHeight = Math.round(smallerScale * sourceHeight);

				postImageView.setMinimumHeight(predictedHeight);

				String postUrl = post.src.getUrl();
				Uri imageCacheUri = getURIFromCache(postUrl, mActivity);
				if (imageCacheUri != null && !post.failToShowInline){

					mImageIsRendering = true;
					overlayVisible = false;

					RedditPreparedPost oldPost = post;
					new Thread("Image rendering thread"){
						@Override
						public void run() {

							Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

							final Bitmap scaledBitmap =
									BitmapCache.getScaledBitmap(
											mActivity,
											imageCacheUri,
											imageWidth,
											imageHeight);

							final BitmapDrawable result = new BitmapDrawable(mActivity.getResources(), scaledBitmap);
							mActivity.runOnUiThread(()->{
								if (postImageView != null && oldPost != null && oldPost.src == post.src && mImageIsRendering) {
									postImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
									postImageView.setImageDrawable(result);
									post.renderedImageHeight = result.getIntrinsicHeight();
									postImageView.setMinimumHeight(post.renderedImageHeight);

									long totalMilis = System.currentTimeMillis() - mImageStartRender;
									//Log.i("ImageRender", "Took " + totalMilis + "ms to render");

									postImageView.clearAnimation();
									if (totalMilis > 100) {
										final Animation animation = AnimationUtils.loadAnimation(
												getContext(),
												R.anim.fade_in);

										postImageView.startAnimation(animation);
									}
								}
							});
						}
					}.start();
				}else{
					// image isn't cached... will it be downloaded?

					final PrefsUtility.CachePrecacheImages imagePrecachePref
							= PrefsUtility.cache_precache_images(
							mActivity,
							PreferenceManager.getDefaultSharedPreferences(getContext()));
					final boolean precacheImages
							= (imagePrecachePref == PrefsUtility.CachePrecacheImages.ALWAYS
							|| (imagePrecachePref == PrefsUtility.CachePrecacheImages.WIFIONLY
							&& General.isConnectionWifi(mActivity)))
							&& !FileUtils.isCacheDiskFull(mActivity);
					if (precacheImages && !post.failToShowInline){
						// continue waiting....
						// TODO: check if we are waiting to cache or if it failed
					}else{
						// not downloading, give up

						thumbnailView.setVisibility(VISIBLE);
						title.setVisibility(VISIBLE);
						title_alternate.setVisibility(GONE);
						postImageView.setVisibility(GONE);
						postImageView.setImageResource(android.R.color.transparent);

						mImageIsRendering = true; // TODO: Fix this hack
					}
				}
			}
		}

		if(overlayVisible) {
			overlayIcon.setVisibility(VISIBLE);
		} else {
			overlayIcon.setVisibility(GONE);
		}
	}

	@Override
	public void betterThumbnailAvailable(
			final Bitmap thumbnail,
			final int callbackUsageId) {
		final Message msg = Message.obtain();
		msg.obj = thumbnail;
		msg.what = callbackUsageId;
		thumbnailHandler.sendMessage(msg);
	}

	public interface PostSelectionListener {
		void onPostSelected(RedditPreparedPost post);

		void onPostCommentsSelected(RedditPreparedPost post);
	}
}
