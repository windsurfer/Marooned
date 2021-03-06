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

package com.abielinski.marooned.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.commons.text.StringEscapeUtils;
import com.abielinski.marooned.R;
import com.abielinski.marooned.account.RedditAccount;
import com.abielinski.marooned.account.RedditAccountManager;
import com.abielinski.marooned.activities.BaseActivity;
import com.abielinski.marooned.activities.BugReportActivity;
import com.abielinski.marooned.activities.OptionsMenuUtility;
import com.abielinski.marooned.activities.SessionChangeListener;
import com.abielinski.marooned.adapters.PostListingManager;
import com.abielinski.marooned.cache.CacheEntry;
import com.abielinski.marooned.cache.CacheManager;
import com.abielinski.marooned.cache.CacheRequest;
import com.abielinski.marooned.cache.downloadstrategy.DownloadStrategy;
import com.abielinski.marooned.cache.downloadstrategy.DownloadStrategyAlways;
import com.abielinski.marooned.cache.downloadstrategy.DownloadStrategyIfNotCached;
import com.abielinski.marooned.cache.downloadstrategy.DownloadStrategyIfTimestampOutsideBounds;
import com.abielinski.marooned.cache.downloadstrategy.DownloadStrategyNever;
import com.abielinski.marooned.common.AndroidCommon;
import com.abielinski.marooned.common.Constants;
import com.abielinski.marooned.common.FileUtils;
import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.LinkHandler;
import com.abielinski.marooned.common.PrefsUtility;
import com.abielinski.marooned.common.RRError;
import com.abielinski.marooned.common.RRTime;
import com.abielinski.marooned.common.TimestampBound;
import com.abielinski.marooned.image.GetImageInfoListener;
import com.abielinski.marooned.image.ImageInfo;
import com.abielinski.marooned.io.RequestResponseHandler;
import com.abielinski.marooned.jsonwrap.JsonBufferedArray;
import com.abielinski.marooned.jsonwrap.JsonBufferedObject;
import com.abielinski.marooned.jsonwrap.JsonValue;
import com.abielinski.marooned.listingcontrollers.CommentListingController;
import com.abielinski.marooned.reddit.PostSort;
import com.abielinski.marooned.reddit.RedditPostListItem;
import com.abielinski.marooned.reddit.RedditSubredditManager;
import com.abielinski.marooned.reddit.api.RedditSubredditSubscriptionManager;
import com.abielinski.marooned.reddit.api.SubredditRequestFailure;
import com.abielinski.marooned.reddit.prepared.RedditParsedPost;
import com.abielinski.marooned.reddit.prepared.RedditPreparedPost;
import com.abielinski.marooned.reddit.things.InvalidSubredditNameException;
import com.abielinski.marooned.reddit.things.RedditPost;
import com.abielinski.marooned.reddit.things.RedditSubreddit;
import com.abielinski.marooned.reddit.things.RedditThing;
import com.abielinski.marooned.reddit.things.SubredditCanonicalId;
import com.abielinski.marooned.reddit.url.PostCommentListingURL;
import com.abielinski.marooned.reddit.url.PostListingURL;
import com.abielinski.marooned.reddit.url.RedditURLParser;
import com.abielinski.marooned.reddit.url.SearchPostListURL;
import com.abielinski.marooned.reddit.url.SubredditPostListURL;
import com.abielinski.marooned.views.PostListingHeader;
import com.abielinski.marooned.views.RedditPostView;
import com.abielinski.marooned.views.ScrollbarRecyclerViewManager;
import com.abielinski.marooned.views.SearchListingHeader;
import com.abielinski.marooned.views.liststatus.ErrorView;

import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PostListingFragment extends RRFragment
		implements RedditPostView.PostSelectionListener {

	private static final String TAG = "PostListingFragment";

	private static final String SAVEDSTATE_FIRST_VISIBLE_POS = "firstVisiblePosition";

	private PostListingURL mPostListingURL;

	private RedditSubreddit mSubreddit;

	private UUID mSession;
	private final int mPostCountLimit;
	private TextView mLoadMoreView;

	private final SharedPreferences mSharedPreferences;

	private final PostListingManager mPostListingManager;
	private final RecyclerView mRecyclerView;

	private final View mOuter;

	private String mAfter = null, mLastAfter = null;
	private CacheRequest mRequest;
	private boolean mReadyToDownloadMore = false;
	private boolean mHideReadUntilRefresh = false;
	private long mTimestamp;

	private int mPostCount = 0;
	private final AtomicInteger mPostRefreshCount = new AtomicInteger(0);

	private final HashSet<String> mPostIds = new HashSet<>(200);

	private Integer mPreviousFirstVisibleItemPosition;

	private HashSet<String> postedURLs = new HashSet<>(200);

	// Session may be null
	public PostListingFragment(
			final AppCompatActivity parent,
			final Bundle savedInstanceState,
			final Uri url,
			final UUID session,
			final boolean forceDownload) {

		super(parent, savedInstanceState);

		mPostListingManager = new PostListingManager(parent);

		if(savedInstanceState != null) {
			mPreviousFirstVisibleItemPosition = savedInstanceState.getInt(
					SAVEDSTATE_FIRST_VISIBLE_POS);
		}

		try {
			mPostListingURL
					= (PostListingURL)RedditURLParser.parseProbablePostListing(url);
		} catch(final ClassCastException e) {
			Toast.makeText(getActivity(), "Invalid post listing URL.", Toast.LENGTH_LONG)
					.show();
			// TODO proper error handling -- show error view
			throw new RuntimeException("Invalid post listing URL");
		}

		mSession = session;

		final Context context = getContext();
		mSharedPreferences = General.getSharedPrefs(context);

		// TODO output failed URL
		if(mPostListingURL == null) {
			mPostListingManager.addFooterError(
					new ErrorView(
							getActivity(),
							new RRError(
									"Invalid post listing URL",
									"Could not navigate to that URL.")));
			// TODO proper error handling
			throw new RuntimeException("Invalid post listing URL");
		}

		switch(PrefsUtility.pref_behaviour_post_count(context, mSharedPreferences)) {
			case ALL:
				mPostCountLimit = -1;
				break;
			case R25:
				mPostCountLimit = 25;
				break;
			case R50:
				mPostCountLimit = 50;
				break;
			case R100:
				mPostCountLimit = 100;
				break;
			default:
				mPostCountLimit = 0;
				break;
		}

		if(mPostCountLimit > 0) {
			restackRefreshCount();
		}

		final ScrollbarRecyclerViewManager recyclerViewManager
				= new ScrollbarRecyclerViewManager(context, null, false);

		if(parent instanceof OptionsMenuUtility.OptionsMenuPostsListener
				&& PrefsUtility.pref_behaviour_enable_swipe_refresh(
				context,
				mSharedPreferences)) {

			recyclerViewManager.enablePullToRefresh(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					((OptionsMenuUtility.OptionsMenuPostsListener)parent).onRefreshPosts();
				}
			});
		}

		mRecyclerView = recyclerViewManager.getRecyclerView();
		mPostListingManager.setLayoutManager((LinearLayoutManager)mRecyclerView.getLayoutManager());

		mRecyclerView.setAdapter(mPostListingManager.getAdapter());

		mOuter = recyclerViewManager.getOuterView();

		mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(
					final RecyclerView recyclerView,
					final int dx,
					final int dy) {
			mRecyclerView.post(new Runnable() {
				@Override
				public void run() {
					onLoadMoreItemsCheck();
				}
			});
			}
		});

		mRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
			@Override
			public void onChildViewAttachedToWindow(@NonNull View view) {
				//nothing?
			}

			@Override
			public void onChildViewDetachedFromWindow(@NonNull View view) {
				// TODO: Find out half of the screen size
				if (view != null && view instanceof RedditPostView && view.getBottom() < 200){
					if (PrefsUtility.pref_behavior_mark_inline_read(
							context,
							mSharedPreferences)){
						((RedditPostView)view).markAsReadIfInline();
					}
				}
			}
		});

		mRecyclerView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;

		int limit = 25;

		if(mPostCountLimit > 0 && limit > mPostCountLimit) {
			limit = mPostCountLimit;
		}

		mPostListingURL = mPostListingURL.limit(limit);

		final DownloadStrategy downloadStrategy;

		if(forceDownload) {
			downloadStrategy = DownloadStrategyAlways.INSTANCE;

		} else{
			downloadStrategy = getDownloadStrategy(context);
		}

		mRequest = new PostListingRequest(
				mPostListingURL.generateJsonUri(),
				RedditAccountManager.getInstance(context).getDefaultAccount(),
				session,
				downloadStrategy,
				true);

		// The request doesn't go ahead until the header is in place.

		switch(mPostListingURL.pathType()) {

			case RedditURLParser.SEARCH_POST_LISTING_URL:
				setHeader(new SearchListingHeader(
						getActivity(),
						(SearchPostListURL)mPostListingURL));
				CacheManager.getInstance(context).makeRequest(mRequest);
				break;

			case RedditURLParser.USER_POST_LISTING_URL:
			case RedditURLParser.MULTIREDDIT_POST_LISTING_URL:
				setHeader(
						mPostListingURL.humanReadableName(getActivity(), true),
						mPostListingURL.humanReadableUrl(),
						null);
				CacheManager.getInstance(context).makeRequest(mRequest);
				break;

			case RedditURLParser.SUBREDDIT_POST_LISTING_URL:

				final SubredditPostListURL subredditPostListURL
						= (SubredditPostListURL)mPostListingURL;

				switch(subredditPostListURL.type) {

					case FRONTPAGE:
					case ALL:
					case SUBREDDIT_COMBINATION:
					case ALL_SUBTRACTION:
					case POPULAR:
						setHeader(
								mPostListingURL.humanReadableName(getActivity(), true),
								mPostListingURL.humanReadableUrl(),
								null);
						CacheManager.getInstance(context).makeRequest(mRequest);
						break;

					case RANDOM:
					case SUBREDDIT: {

						// Request the subreddit data

						final RequestResponseHandler<RedditSubreddit, SubredditRequestFailure>
								subredditHandler = new RequestResponseHandler<
								RedditSubreddit,
								SubredditRequestFailure>() {
							@Override
							public void onRequestFailed(
									final SubredditRequestFailure failureReason) {
								// Ignore
								AndroidCommon.UI_THREAD_HANDLER.post(() ->
										CacheManager.getInstance(context).makeRequest(mRequest));
							}

							@Override
							public void onRequestSuccess(
									final RedditSubreddit result,
									final long timeCached) {
								AndroidCommon.UI_THREAD_HANDLER.post(() -> {
									mSubreddit = result;

									if(mSubreddit.over18
											&& !PrefsUtility.pref_behaviour_nsfw(
											context,
											mSharedPreferences)) {
										mPostListingManager.setLoadingVisible(false);

										final int title
												= R.string.error_nsfw_subreddits_disabled_title;

										final int message
												= R.string.error_nsfw_subreddits_disabled_message;

										mPostListingManager.addFooterError(new ErrorView(
												getActivity(),
												new RRError(
														context.getString(title),
														context.getString(message))));
									} else {
										onSubredditReceived();
										CacheManager.getInstance(context)
												.makeRequest(mRequest);
									}
								});
							}
						};

						try {
							RedditSubredditManager
									.getInstance(
											getActivity(),
											RedditAccountManager.getInstance(getActivity())
													.getDefaultAccount())
									.getSubreddit(
											new SubredditCanonicalId(
													subredditPostListURL.subreddit),
											TimestampBound.NONE,
											subredditHandler,
											null);
						} catch(final InvalidSubredditNameException e) {
							throw new RuntimeException(e);
						}
						break;
					}
				}

				break;
		}
	}

	private DownloadStrategy getDownloadStrategy(Context context){

		if(mSession == null
			&& General.isNetworkConnected(context)) {

			final long maxAgeMs = PrefsUtility.pref_cache_rerequest_postlist_age_ms(
					context,
					mSharedPreferences);
			return new DownloadStrategyIfTimestampOutsideBounds(TimestampBound
					.notOlderThan(
							maxAgeMs));

		} else {
			return DownloadStrategyIfNotCached.INSTANCE;
		}
	}

	@Override
	public View getView() {
		return mOuter;
	}

	@Override
	public Bundle onSaveInstanceState() {

		final Bundle bundle = new Bundle();

		final LinearLayoutManager layoutManager
				= (LinearLayoutManager)mRecyclerView.getLayoutManager();
		bundle.putInt(
				SAVEDSTATE_FIRST_VISIBLE_POS,
				layoutManager.findFirstVisibleItemPosition());

		return bundle;
	}

	public void cancel() {
		if(mRequest != null) {
			mRequest.cancel();
		}
	}

	public synchronized void restackRefreshCount() {
		while(mPostRefreshCount.get() <= 0) {
			mPostRefreshCount.addAndGet(mPostCountLimit);
		}
	}

	private void onSubredditReceived() {

		if(mPostListingURL.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL
				&& mPostListingURL.asSubredditPostListURL().type
				== SubredditPostListURL.Type.RANDOM) {
			try {
				mPostListingURL = mPostListingURL.asSubredditPostListURL()
						.changeSubreddit(RedditSubreddit.stripRPrefix(mSubreddit.url));
				mRequest = new PostListingRequest(
						mPostListingURL.generateJsonUri(),
						RedditAccountManager.getInstance(getContext())
								.getDefaultAccount(),
						mSession,
						mRequest.downloadStrategy,
						true);
			} catch(final InvalidSubredditNameException e) {
				throw new RuntimeException(e);
			}
		}
		final String subtitle;

		if(mPostListingURL.getOrder() == null
				|| mPostListingURL.getOrder() == PostSort.HOT) {
			String subredditName = mSubreddit.display_name;
			if(mSubreddit.subscribers == null) {
				subtitle = getString(R.string.header_subscriber_count_unknown) + " " + subredditName;
			} else {
				subtitle = getContext().getString(
						R.string.header_subscriber_count,
						NumberFormat.getNumberInstance(Locale.getDefault())
								.format(mSubreddit.subscribers))  + " " + subredditName;;
			}

		} else {
			subtitle = mPostListingURL.humanReadableUrl();
		}

		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setHeader(
						StringEscapeUtils.unescapeHtml4(mSubreddit.title),
						subtitle,
						mSubreddit);
				getActivity().invalidateOptionsMenu();
			}
		});

	}

	private void setHeader(
			@NonNull final String title,
			@NonNull final String subtitle,
			@Nullable final RedditSubreddit subreddit) {

		final PostListingHeader postListingHeader = new PostListingHeader(
				getActivity(),
				title,
				subtitle,
				mPostListingURL,
				subreddit);

		setHeader(postListingHeader);
	}

	private void setHeader(final View view) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mPostListingManager.addPostListingHeader(view);
			}
		});
	}


	@Override
	public void onPostSelected(final RedditPreparedPost post) {
		((RedditPostView.PostSelectionListener)getActivity()).onPostSelected(post);

		new Thread() {
			@Override
			public void run() {
				post.markAsRead(getActivity());
			}
		}.start();
	}

	@Override
	public void onPostCommentsSelected(final RedditPreparedPost post) {

		((RedditPostView.PostSelectionListener)getActivity()).onPostCommentsSelected(post);

		new Thread() {
			@Override
			public void run() {
				post.markAsRead(getActivity());
			}
		}.start();
	}

	private void onLoadMoreItemsCheck() {

		General.checkThisIsUIThread();

		if(mReadyToDownloadMore && mAfter != null && !mAfter.equals(mLastAfter)) {

			final LinearLayoutManager layoutManager
					= (LinearLayoutManager)mRecyclerView.getLayoutManager();

			if((layoutManager.getItemCount() - layoutManager.findLastVisibleItemPosition()
					< 20
					&& (mPostCountLimit <= 0 || mPostRefreshCount.get() > 0)
					|| (mPreviousFirstVisibleItemPosition != null
					&& layoutManager.getItemCount()
					<= mPreviousFirstVisibleItemPosition))) {

				mLastAfter = mAfter;
				mReadyToDownloadMore = false;

				int limit = 25;

				if(mPostCountLimit > 0 && limit > mPostRefreshCount.get()) {
					limit = mPostRefreshCount.get();
				}

				mPostListingURL = mPostListingURL.limit(limit);

				final Uri newUri = mPostListingURL.after(mAfter).generateJsonUri();

				// TODO customise (currently 3 hrs)
				final DownloadStrategy strategy = (RRTime.since(mTimestamp)
						< 3 * 60 * 60 * 1000)
						? DownloadStrategyIfNotCached.INSTANCE
						: DownloadStrategyNever.INSTANCE;


				mRequest = new PostListingRequest(
						newUri,
						RedditAccountManager.getInstance(getActivity())
								.getDefaultAccount(),
						mSession,
						strategy,
						false);
				mPostListingManager.setLoadingVisible(true);
				CacheManager.getInstance(getActivity()).makeRequest(mRequest);

			} else if(mPostCountLimit > 0 && mPostRefreshCount.get() <= 0) {

				if(mLoadMoreView == null) {

					mLoadMoreView = (TextView)LayoutInflater.from(getContext())
							.inflate(R.layout.load_more_posts, null);
					mLoadMoreView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(final View view) {
							mPostListingManager.removeLoadMoreButton();
							mLoadMoreView = null;
							restackRefreshCount();
							onLoadMoreItemsCheck();
						}
					});

					mPostListingManager.addLoadMoreButton(mLoadMoreView);
				}
			}
		}
	}

	public void onSubscribe() {

		if(mPostListingURL.pathType() != RedditURLParser.SUBREDDIT_POST_LISTING_URL) {
			return;
		}

		try {
			RedditSubredditSubscriptionManager
					.getSingleton(
							getActivity(),
							RedditAccountManager.getInstance(getActivity())
									.getDefaultAccount())
					.subscribe(
							new SubredditCanonicalId(
									mPostListingURL.asSubredditPostListURL().subreddit),
							getActivity());
		} catch(final InvalidSubredditNameException e) {
			throw new RuntimeException(e);
		}
	}

	public void onUnsubscribe() {

		if(mSubreddit == null) {
			return;
		}

		try {
			RedditSubredditSubscriptionManager
					.getSingleton(
							getActivity(),
							RedditAccountManager.getInstance(getActivity())
									.getDefaultAccount())
					.unsubscribe(mSubreddit.getCanonicalId(), getActivity());
		} catch(final InvalidSubredditNameException e) {
			throw new RuntimeException(e);
		}
	}

	public RedditSubreddit getSubreddit() {
		return mSubreddit;
	}

	private static Uri setUriDownloadCount(final Uri input, final int count) {
		return input.buildUpon()
				.appendQueryParameter("limit", String.valueOf(count))
				.build();
	}

	public void onPostsAdded() {

		if(mPreviousFirstVisibleItemPosition == null) {
			return;
		}

		final LinearLayoutManager layoutManager
				= (LinearLayoutManager)mRecyclerView.getLayoutManager();

		if(layoutManager.getItemCount() > mPreviousFirstVisibleItemPosition) {
			layoutManager.scrollToPositionWithOffset(
					mPreviousFirstVisibleItemPosition,
					0);
			mPreviousFirstVisibleItemPosition = null;

		} else {
			layoutManager.scrollToPosition(layoutManager.getItemCount() - 1);
		}
	}

	public void setHideReadUntilRefresh() {
		mHideReadUntilRefresh = true;
		Log.i(TAG, "Hiding read items until refreshed");
		mPostListingManager.getAdapter().hideReadItems();
	}

	private class PostListingRequest extends CacheRequest {

		private final boolean firstDownload;

		protected PostListingRequest(
				final Uri url,
				final RedditAccount user,
				final UUID requestSession,
				final DownloadStrategy downloadStrategy,
				final boolean firstDownload) {
			super(
					General.uriFromString(url.toString()),
					user,
					requestSession,
					Constants.Priority.API_POST_LIST,
					0,
					downloadStrategy,
					Constants.FileType.POST_LIST,
					DOWNLOAD_QUEUE_REDDIT_API,
					true,
					false,
					getActivity());
			this.firstDownload = firstDownload;
		}

		@Override
		protected void onDownloadNecessary() {
		}

		@Override
		protected void onDownloadStarted() {
		}

		@Override
		protected void onCallbackException(final Throwable t) {
			BugReportActivity.handleGlobalError(context, t);
		}

		@Override
		protected void onFailure(
				final @CacheRequest.RequestFailureType int type,
				final Throwable t,
				final Integer status,
				final String readableMessage) {

			AndroidCommon.UI_THREAD_HANDLER.post(() -> {

				mPostListingManager.setLoadingVisible(false);

				final RRError error;

				if(type == CacheRequest.REQUEST_FAILURE_CACHE_MISS) {
					error = new RRError(
							context.getString(R.string.error_postlist_cache_title),
							context.getString(R.string.error_postlist_cache_message),
							t,
							status,
							url.toString());

				} else {
					error = General.getGeneralErrorForFailure(
							context,
							type,
							t,
							status,
							url.toString());
				}

				mPostListingManager.addFooterError(new ErrorView(
						getActivity(),
						error));
			});
		}

		@Override
		protected void onProgress(
				final boolean authorizationInProgress,
				final long bytesRead,
				final long totalBytes) {
		}

		@Override
		protected void onSuccess(
				final CacheManager.ReadableCacheFile cacheFile,
				final long timestamp,
				final UUID session,
				final boolean fromCache,
				final String mimetype) {
		}

		@Override
		public void onJsonParseStarted(
				final JsonValue value,
				final long timestamp,
				final UUID session,
				final boolean fromCache) {

			final BaseActivity activity = (BaseActivity)getActivity();

			// One hour (matches default refresh value)
			if(firstDownload && fromCache && RRTime.since(timestamp) > 60 * 60 * 1000) {
				AndroidCommon.UI_THREAD_HANDLER.post(() -> {

					final TextView cacheNotif
							= (TextView)LayoutInflater.from(activity).inflate(
									R.layout.cached_header,
									null,
									false);

					cacheNotif.setText(getActivity().getString(
							R.string.listing_cached,
							RRTime.formatDateTime(timestamp, getActivity())));

					mPostListingManager.addNotification(cacheNotif);
				});
			} // TODO resuming a copy

			if(firstDownload) {
				((SessionChangeListener)activity).onSessionChanged(
						session,
						SessionChangeListener.SessionChangeType.POSTS,
						timestamp);
				PostListingFragment.this.mSession = session;
				PostListingFragment.this.mTimestamp = timestamp;
			}

			// TODO {"error": 403} is received for unauthorized subreddits

			try {

				final JsonBufferedObject thing = value.asObject();
				final JsonBufferedObject listing = thing.getObject("data");
				final JsonBufferedArray posts = listing.getArray("children");

				final boolean isNsfwAllowed = PrefsUtility.pref_behaviour_nsfw(
						activity,
						mSharedPreferences);
				final boolean hideReadPosts = PrefsUtility.pref_behaviour_hide_read_posts(
						activity,
						mSharedPreferences);
				final boolean isConnectionWifi = General.isConnectionWifi(activity);
				final boolean hideDuplicatePosts = PrefsUtility.pref_behaviour_duplicates(
						activity,
						mSharedPreferences);

				final PrefsUtility.AppearanceThumbnailsShow thumbnailsPref
						= PrefsUtility.appearance_thumbnails_show(
						activity, mSharedPreferences);
				final boolean downloadThumbnails = thumbnailsPref
						== PrefsUtility.AppearanceThumbnailsShow.ALWAYS
						|| (thumbnailsPref
						== PrefsUtility.AppearanceThumbnailsShow.WIFIONLY
						&& isConnectionWifi);

				final int thumbnailSize
						= PrefsUtility.pref_appearance_thumbnails_size(
						activity,
						mSharedPreferences);

				final boolean showNsfwThumbnails
						= PrefsUtility.appearance_thumbnails_nsfw_show(
						activity,
						mSharedPreferences);

				final PrefsUtility.CachePrecacheImages imagePrecachePref
						= PrefsUtility.cache_precache_images(
						activity,
						mSharedPreferences);

				final PrefsUtility.CachePrecacheVideos videoPrecachePref
						= PrefsUtility.cache_precache_videos(
						activity,
						mSharedPreferences);

				final PrefsUtility.CachePrecacheComments commentPrecachePref
						= PrefsUtility.cache_precache_comments(
						activity,
						mSharedPreferences);

				final boolean precacheImages
						= (imagePrecachePref == PrefsUtility.CachePrecacheImages.ALWAYS
								|| (imagePrecachePref == PrefsUtility.CachePrecacheImages.WIFIONLY
								&& isConnectionWifi))
						&& !FileUtils.isCacheDiskFull(activity);

				final boolean precacheVideos
						= (videoPrecachePref == PrefsUtility.CachePrecacheVideos.ALWAYS
						|| (videoPrecachePref == PrefsUtility.CachePrecacheVideos.WIFIONLY
						&& isConnectionWifi))
						&& !FileUtils.isCacheDiskFull(activity);

				final long maxSizePrecacheVideos
						= PrefsUtility.pref_cache_videos_size(activity,mSharedPreferences);
				final long maxSizePrecacheImages
						= PrefsUtility.pref_cache_images_size(activity,mSharedPreferences);

				final boolean precacheComments
						= (commentPrecachePref == PrefsUtility.CachePrecacheComments.ALWAYS
								|| (commentPrecachePref
										== PrefsUtility.CachePrecacheComments.WIFIONLY
						&& isConnectionWifi));

				final PrefsUtility.ImageViewMode imageViewMode
						= PrefsUtility.pref_behaviour_imageview_mode(
								activity,
								mSharedPreferences);

				final PrefsUtility.GifViewMode gifViewMode
						= PrefsUtility.pref_behaviour_gifview_mode(
								activity,
								mSharedPreferences);

				final PrefsUtility.VideoViewMode videoViewMode
						= PrefsUtility.pref_behaviour_videoview_mode(
								activity,
								mSharedPreferences);

				final boolean leftHandedMode = PrefsUtility.pref_appearance_left_handed(
						activity,
						mSharedPreferences);

				final boolean subredditFilteringEnabled =
						mPostListingURL.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL
								&& (mPostListingURL.asSubredditPostListURL().type
										== SubredditPostListURL.Type.ALL
								|| mPostListingURL.asSubredditPostListURL().type
										== SubredditPostListURL.Type.ALL_SUBTRACTION
								|| mPostListingURL.asSubredditPostListURL().type
										== SubredditPostListURL.Type.POPULAR);

				// Grab this so we don't have to pull from the prefs every post
				final HashSet<SubredditCanonicalId> blockedSubreddits
						= new HashSet<>(PrefsUtility.pref_blocked_subreddits(
								activity,
								mSharedPreferences));

				Log.i(TAG, "Precaching images: " + (precacheImages ? "ON" : "OFF"));
				Log.i(TAG, "Precaching videos: " + (precacheVideos ? "ON" : "OFF"));
				Log.i(TAG, "Precaching comments: " + (precacheComments ? "ON" : "OFF"));

				final CacheManager cm = CacheManager.getInstance(activity);

				final boolean showSubredditName = !(mPostListingURL != null
						&& mPostListingURL.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL
						&& mPostListingURL.asSubredditPostListURL().type
								== SubredditPostListURL.Type.SUBREDDIT);

				final ArrayList<RedditPostListItem> downloadedPosts = new ArrayList<>(25);

				for(final JsonValue postThingValue : posts) {

					final RedditThing postThing
							= postThingValue.asObject(RedditThing.class);

					if(!postThing.getKind().equals(RedditThing.Kind.POST)) {
						continue;
					}

					final RedditPost post = postThing.asPost();

					mAfter = post.name;

					final boolean isPostBlocked = subredditFilteringEnabled
							&& blockedSubreddits.contains(new SubredditCanonicalId(post.subreddit));

					if(!isPostBlocked
							&& (!post.over_18 || isNsfwAllowed)
							&& mPostIds.add(post.getIdAlone())) {

						final boolean downloadThisThumbnail
								= downloadThumbnails && (!post.over_18 || showNsfwThumbnails);

						final int positionInList = mPostCount;

						final RedditParsedPost parsedPost = new RedditParsedPost(
								activity,
								post,
								false);

						final RedditPreparedPost preparedPost = new RedditPreparedPost(
								activity,
								cm,
								positionInList,
								parsedPost,
								timestamp,
								showSubredditName,
								downloadThisThumbnail,
								thumbnailSize);

						// Skip adding this post (go to next iteration) if it
						// has been clicked on AND user preference
						// "hideReadPosts" is true
						if((hideReadPosts || mHideReadUntilRefresh) && preparedPost.isRead()) {
							continue;
						}

						// skip adding this post if it has been read AND user pref is enabled
						if (hideDuplicatePosts && !postedURLs.add(preparedPost.src.getUrl())){
							Log.i(TAG, "Skipping post because duplicate: " + preparedPost.src.getUrl());
							continue;
						}

						if(precacheComments) {

							final CommentListingController controller
									= new CommentListingController(
									PostCommentListingURL.forPostId(preparedPost.src.getIdAlone()),
									activity);

							CacheManager.getInstance(activity)
									.makeRequest(new CacheRequest(
											General.uriFromString(controller.getUri()
													.toString()),
											RedditAccountManager.getInstance(activity)
													.getDefaultAccount(),
											null,
											Constants.Priority.COMMENT_PRECACHE,
											positionInList,
											new DownloadStrategyIfTimestampOutsideBounds(
													TimestampBound.notOlderThan(RRTime.minsToMs(
															15))),
											Constants.FileType.COMMENT_LIST,
											DOWNLOAD_QUEUE_REDDIT_API,
											false, // Don't parse the JSON
											false,
											activity) {

										@Override
										protected void onCallbackException(final Throwable t) {
										}

										@Override
										protected void onDownloadNecessary() {
										}

										@Override
										protected void onDownloadStarted() {
										}

										@Override
										protected void onFailure(
												final @CacheRequest.RequestFailureType int type,
												final Throwable t,
												final Integer status,
												final String readableMessage) {
											Log.e(
													TAG,
													"Failed to precache "
															+ url.toString()
															+ "(RequestFailureType code: "
															+ type
															+ ")");
										}

										@Override
										protected void onProgress(
												final boolean authorizationInProgress,
												final long bytesRead,
												final long totalBytes) {
										}

										@Override
										protected void onSuccess(
												final CacheManager.ReadableCacheFile cacheFile,
												final long timestamp,
												final UUID session,
												final boolean fromCache,
												final String mimetype) {
											Log.i(
													TAG,
													"Successfully precached comments "
															+ url.toString());
										}
									});
						}

						RedditPostListItem postItem = new RedditPostListItem(
								preparedPost,
								PostListingFragment.this,
								activity,
								leftHandedMode);
						downloadedPosts.add(postItem);

						boolean isCached = isImageCached(activity, parsedPost.getUrl());
						postItem.setCached(isCached, activity);

						if (!isCached) {
							LinkHandler.getImageInfo(
									activity,
									parsedPost.getUrl(),
									Constants.Priority.IMAGE_PRECACHE,
									positionInList,
									new GetImageInfoListener() {

										@Override
										public void onFailure(
												final @CacheRequest.RequestFailureType int type,
												final Throwable t,
												final Integer status,
												final String readableMessage) {
											preparedPost.setFailToCache(activity);
										}

										@Override
										public void onNotAnImage() {
										}

										@Override
										public void onSuccess(final ImageInfo info) {

											postItem.setCached(isImageCached(activity, info.urlOriginal), activity);

											// not strictly required, just an optimization
											if (!precacheImages && !precacheVideos) {
												return;
											}

											// Don't precache huge images
											final long size = info.size != null ? info.size : 0;


											// Don't precache gifs if they're opened externally
											if (ImageInfo.MediaType.GIF.equals(info.mediaType)) {

												if (!precacheVideos) {
													Log.i(TAG, String.format(
															"Not precaching '%s': GIFs on current connection",
															post.getUrl()));
													return;
												}

												if (!gifViewMode.downloadInApp) {

													Log.i(TAG, String.format(
															"Not precaching '%s': GIFs opened externally",
															post.getUrl()));
													return;
												}

												if (size > maxSizePrecacheVideos) {
													Log.i(TAG, String.format(
															"Not precaching '%s': too big (%d kB)",
															post.getUrl(),
															size / 1024));
													return;
												}
											}

											// Don't precache images if they're opened externally
											if (ImageInfo.MediaType.IMAGE.equals(info.mediaType)) {
												if (!imageViewMode.downloadInApp) {
													Log.i(TAG, String.format(
															"Not precaching '%s': images opened externally",
															post.getUrl()));
													preparedPost.setFailToCache(activity);
													return;
												}
												if (!precacheImages) {
													Log.i(TAG, String.format(
															"Not precaching '%s': images on current connection",
															post.getUrl()));
													preparedPost.setFailToCache(activity);
													return;
												}

												if (size > maxSizePrecacheImages) {
													Log.i(TAG, String.format(
															"Not precaching '%s': too big (%d kB)",
															post.getUrl(),
															size / 1024));
													preparedPost.setFailToCache(activity);
													return;
												}
											}


											// Don't precache videos if they're opened externally
											if (ImageInfo.MediaType.VIDEO.equals(info.mediaType)) {
												if (!videoViewMode.downloadInApp) {
													Log.i(TAG, String.format(
															"Not precaching '%s': videos opened externally",
															post.getUrl()));
													return;
												}
												if (!precacheVideos) {
													Log.i(TAG, String.format(
															"Not precaching '%s': videos on current connection",
															post.getUrl()));
													return;
												}

												if (size > maxSizePrecacheVideos) {
													Log.i(TAG, String.format(
															"Not precaching '%s': too big (%d kB)",
															post.getUrl(),
															size / 1024));
													return;
												}
											}

											precacheImage(
													activity,
													info.urlOriginal,
													positionInList,
													postItem,
													parsedPost.getUrl());

											if (info.urlAudioStream != null) {
												precacheImage(
														activity,
														info.urlAudioStream,
														positionInList,
														postItem);
											}
										}
									});
						}

						mPostCount++;
						mPostRefreshCount.decrementAndGet();
					}
				}

				AndroidCommon.UI_THREAD_HANDLER.post(() -> {

					mPostListingManager.addPosts(downloadedPosts);
					mPostListingManager.setLoadingVisible(false);
					onPostsAdded();

					mRequest = null;
					mReadyToDownloadMore = true;
					onLoadMoreItemsCheck();
				});

			} catch(final Throwable t) {
				notifyFailure(
						CacheRequest.REQUEST_FAILURE_PARSE,
						t,
						null,
						"Parse failure");
			}
		}
	}

	private boolean isImageCached(
			final Activity activity,
			final String url){

		if(url == null) {
			return false;
		}
		final URI uri = General.uriFromString(url);

		if(uri == null) {
			return false;
		}

		final List<CacheEntry> result = CacheManager.getInstance(activity).getSessions(uri, "");

		if(!result.isEmpty()) {

			CacheEntry entry = null;

			for(final CacheEntry e : result) {
				if(entry == null || entry.timestamp < e.timestamp) {
					entry = e;
				}
			}

			if (getDownloadStrategy(getContext()).shouldDownloadIfCacheEntryFound(entry)) {
				return false;
			}
			return true;
		}

		return false;
	}
	private void precacheImage(
			final Activity activity,
			final String url,
			final int positionInList,
			RedditPostListItem postItem) {
		precacheImage(activity, url, positionInList, postItem, null);
	}

	private void precacheImage(
			final Activity activity,
			final String url,
			final int positionInList,
			RedditPostListItem postItem,
			final String originalUrl) {

		final URI uri = General.uriFromString(url);
		if(uri == null) {
			Log.i(TAG, String.format(
					"Not precaching '%s': failed to parse URL", url));
			return;
		}

		final URI original_uri = General.uriFromString(originalUrl);


		CacheManager.getInstance(activity).makeRequest(new CacheRequest(
				uri,
				RedditAccountManager.getAnon(),
				null,
				Constants.Priority.IMAGE_PRECACHE,
				positionInList,
				DownloadStrategyIfNotCached.INSTANCE,
				Constants.FileType.IMAGE,
				CacheRequest.DOWNLOAD_QUEUE_IMAGE_PRECACHE,
				false,
				false,
				activity,
				original_uri
		) {

			@Override
			protected void onCallbackException(final Throwable t) {
			}

			@Override
			protected void onDownloadNecessary() {
			}

			@Override
			protected void onDownloadStarted() {
			}

			@Override
			protected void onFailure(
					final @CacheRequest.RequestFailureType int type,
					final Throwable t,
					final Integer status,
					final String readableMessage) {

				Log.e(TAG, String.format(
						Locale.US,
						"Failed to precache %s (RequestFailureType %d, status %s, readable '%s')",
						url,
						type,
						status == null ? "NULL" : status.toString(),
						readableMessage == null ? "NULL" : readableMessage));
			}

			@Override
			protected void onProgress(
					final boolean authorizationInProgress,
					final long bytesRead,
					final long totalBytes) {
			}

			@Override
			protected void onSuccess(
					final CacheManager.ReadableCacheFile cacheFile,
					final long timestamp,
					final UUID session,
					final boolean fromCache,
					final String mimetype) {
				Log.i(TAG, "Successfully precached content " + url);
				postItem.setCached(true, activity);
			}
		});
	}
}
