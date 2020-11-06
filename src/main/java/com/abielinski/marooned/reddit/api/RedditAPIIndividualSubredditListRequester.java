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

package com.abielinski.marooned.reddit.api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.abielinski.marooned.account.RedditAccount;
import com.abielinski.marooned.cache.CacheManager;
import com.abielinski.marooned.cache.CacheRequest;
import com.abielinski.marooned.cache.downloadstrategy.DownloadStrategyAlways;
import com.abielinski.marooned.common.Constants;
import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.TimestampBound;
import com.abielinski.marooned.common.UnexpectedInternalStateException;
import com.abielinski.marooned.io.CacheDataSource;
import com.abielinski.marooned.io.RequestResponseHandler;
import com.abielinski.marooned.io.WritableHashSet;
import com.abielinski.marooned.jsonwrap.JsonBuffered;
import com.abielinski.marooned.jsonwrap.JsonBufferedArray;
import com.abielinski.marooned.jsonwrap.JsonBufferedObject;
import com.abielinski.marooned.jsonwrap.JsonValue;
import com.abielinski.marooned.reddit.RedditSubredditManager;
import com.abielinski.marooned.reddit.things.InvalidSubredditNameException;
import com.abielinski.marooned.reddit.things.RedditSubreddit;
import com.abielinski.marooned.reddit.things.RedditThing;
import com.abielinski.marooned.reddit.things.SubredditCanonicalId;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class RedditAPIIndividualSubredditListRequester implements CacheDataSource<
		RedditSubredditManager.SubredditListType, WritableHashSet, SubredditRequestFailure> {

	private final Context context;
	private final RedditAccount user;

	public RedditAPIIndividualSubredditListRequester(
			final Context context,
			final RedditAccount user) {
		this.context = context;
		this.user = user;
	}

	@Override
	public void performRequest(
			final RedditSubredditManager.SubredditListType type,
			final TimestampBound timestampBound,
			final RequestResponseHandler<WritableHashSet, SubredditRequestFailure> handler) {

		if(type == RedditSubredditManager.SubredditListType.DEFAULTS) {

			final long now = System.currentTimeMillis();

			final HashSet<String> data =
					new HashSet<>(Constants.Reddit.DEFAULT_SUBREDDITS.size() + 1);

			for(final SubredditCanonicalId id : Constants.Reddit.DEFAULT_SUBREDDITS) {
				data.add(id.toString());
			}

			final WritableHashSet result = new WritableHashSet(data, now, "DEFAULTS");
			handler.onRequestSuccess(result, now);

			return;
		}

		if(type == RedditSubredditManager.SubredditListType.MOST_POPULAR) {
			doSubredditListRequest(
					RedditSubredditManager.SubredditListType.MOST_POPULAR,
					handler,
					null);

		} else if(user.isAnonymous()) {
			switch(type) {

				case SUBSCRIBED:
					performRequest(
							RedditSubredditManager.SubredditListType.DEFAULTS,
							timestampBound,
							handler);
					return;

				case MODERATED: {
					final long curTime = System.currentTimeMillis();
					handler.onRequestSuccess(
							new WritableHashSet(
									new HashSet<String>(),
									curTime,
									RedditSubredditManager.SubredditListType.MODERATED.name()),
							curTime);
					return;
				}

				case MULTIREDDITS: {
					final long curTime = System.currentTimeMillis();
					handler.onRequestSuccess(
							new WritableHashSet(
									new HashSet<String>(),
									curTime,
									RedditSubredditManager.SubredditListType.MULTIREDDITS.name()),
							curTime);
					return;
				}

				default:
					throw new RuntimeException(
							"Internal error: unknown subreddit list type '"
									+ type.name()
									+ "'");
			}

		} else {
			doSubredditListRequest(type, handler, null);
		}
	}

	private void doSubredditListRequest(
			final RedditSubredditManager.SubredditListType type,
			final RequestResponseHandler<WritableHashSet, SubredditRequestFailure> handler,
			final String after) {

		URI uri;

		switch(type) {
			case SUBSCRIBED:
				uri =
						Constants.Reddit.getUri(Constants.Reddit.PATH_SUBREDDITS_MINE_SUBSCRIBER);
				break;
			case MODERATED:
				uri =
						Constants.Reddit.getUri(Constants.Reddit.PATH_SUBREDDITS_MINE_MODERATOR);
				break;
			case MOST_POPULAR:
				uri = Constants.Reddit.getUri(Constants.Reddit.PATH_SUBREDDITS_POPULAR);
				break;
			default:
				throw new UnexpectedInternalStateException(type.name());
		}

		if(after != null) {
			// TODO move this logic to General?
			final Uri.Builder builder = Uri.parse(uri.toString()).buildUpon();
			builder.appendQueryParameter("after", after);
			uri = General.uriFromString(builder.toString());
		}

		final CacheRequest aboutSubredditCacheRequest = new CacheRequest(
				uri,
				user,
				null,
				Constants.Priority.API_SUBREDDIT_INVIDIVUAL,
				0,
				DownloadStrategyAlways.INSTANCE,
				Constants.FileType.SUBREDDIT_LIST,
				CacheRequest.DOWNLOAD_QUEUE_REDDIT_API,
				true,
				false,
				context
		) {

			@Override
			protected void onCallbackException(final Throwable t) {
				handler.onRequestFailed(new SubredditRequestFailure(
						CacheRequest.REQUEST_FAILURE_PARSE,
						t,
						null,
						"Internal error",
						url));
			}

			@Override
			protected void onDownloadNecessary() {
			}

			@Override
			protected void onDownloadStarted() {
			}

			@Override
			protected void onProgress(
					final boolean authorizationInProgress,
					final long bytesRead,
					final long totalBytes) {
			}

			@Override
			protected void onFailure(
					@CacheRequest.RequestFailureType final int type,
					final Throwable t,
					final Integer status,
					final String readableMessage) {
				handler.onRequestFailed(new SubredditRequestFailure(
						type,
						t,
						status,
						readableMessage,
						url.toString()));
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
					final JsonValue result,
					final long timestamp,
					final UUID session,
					final boolean fromCache) {

				try {

					final HashSet<String> output = new HashSet<>();
					final ArrayList<RedditSubreddit> toWrite = new ArrayList<>();

					final JsonBufferedObject redditListing =
							result.asObject().getObject("data");

					final JsonBufferedArray subreddits =
							redditListing.getArray("children");

					final @JsonBuffered.Status int joinStatus = subreddits.join();
					if(joinStatus == JsonBuffered.STATUS_FAILED) {
						handler.onRequestFailed(new SubredditRequestFailure(
								CacheRequest.REQUEST_FAILURE_PARSE,
								null,
								null,
								"Unknown parse error",
								url.toString()));
						return;
					}

					if(type == RedditSubredditManager.SubredditListType.SUBSCRIBED
							&& subreddits.getCurrentItemCount() == 0
							&& after == null) {
						performRequest(
								RedditSubredditManager.SubredditListType.DEFAULTS,
								TimestampBound.ANY,
								handler);
						return;
					}

					for(final JsonValue v : subreddits) {
						final RedditThing thing = v.asObject(RedditThing.class);
						final RedditSubreddit subreddit = thing.asSubreddit();

						subreddit.downloadTime = timestamp;

						try {
							output.add(subreddit.getCanonicalId().toString());
							toWrite.add(subreddit);
						} catch(final InvalidSubredditNameException e) {
							Log.e(
									"SubredditListRequester",
									"Ignoring invalid subreddit",
									e);
						}

					}

					RedditSubredditManager.getInstance(context, user)
							.offerRawSubredditData(toWrite, timestamp);
					final String receivedAfter = redditListing.getString("after");
					if(receivedAfter != null
							&& type
							!= RedditSubredditManager.SubredditListType.MOST_POPULAR) {

						doSubredditListRequest(
								type,
								new RequestResponseHandler<
										WritableHashSet,
										SubredditRequestFailure>() {
									@Override
									public void onRequestFailed(
											final SubredditRequestFailure failureReason) {
										handler.onRequestFailed(failureReason);
									}

									@Override
									public void onRequestSuccess(
											final WritableHashSet result,
											final long timeCached) {
										output.addAll(result.toHashset());
										handler.onRequestSuccess(new WritableHashSet(
												output,
												timeCached,
												type.name()), timeCached);

										if(after == null) {
											Log.i(
													"SubredditListRequester",
													"Got "
															+ output.size()
															+ " subreddits in multiple requests");
										}
									}
								},
								receivedAfter);

					} else {
						handler.onRequestSuccess(new WritableHashSet(
								output,
								timestamp,
								type.name()), timestamp);

						if(after == null) {
							Log.i(
									"SubredditListRequester",
									"Got " + output.size() + " subreddits in 1 request");
						}
					}

				} catch(final Exception e) {
					handler.onRequestFailed(new SubredditRequestFailure(
							CacheRequest.REQUEST_FAILURE_PARSE,
							e,
							null,
							"Parse error",
							url.toString()));
				}
			}
		};

		CacheManager.getInstance(context).makeRequest(aboutSubredditCacheRequest);
	}

	@Override
	public void performRequest(
			final Collection<RedditSubredditManager.SubredditListType> keys,
			final TimestampBound timestampBound,
			final RequestResponseHandler<
					HashMap<RedditSubredditManager.SubredditListType, WritableHashSet>,
					SubredditRequestFailure> handler) {
		// TODO batch API? or just make lots of requests and build up a hash map?
		throw new UnsupportedOperationException();
	}

	@Override
	public void performWrite(final WritableHashSet value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void performWrite(final Collection<WritableHashSet> values) {
		throw new UnsupportedOperationException();
	}
}
