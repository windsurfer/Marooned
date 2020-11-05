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

package com.abielinski.marooned.Reddit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.abielinski.marooned.activities.BugReportActivity;
import com.abielinski.marooned.cache.CacheRequest;
import com.abielinski.marooned.common.RRError;
import com.abielinski.marooned.Reddit.things.RedditSubreddit;
import com.abielinski.marooned.Reddit.things.RedditUser;

import java.util.List;

public abstract class APIResponseHandler {

	protected final AppCompatActivity context;

	public enum APIFailureType {
		INVALID_USER,
		BAD_CAPTCHA,
		NOTALLOWED,
		SUBREDDIT_REQUIRED,
		URL_REQUIRED,
		UNKNOWN,
		TOO_FAST,
		TOO_LONG,
		ALREADY_SUBMITTED
	}

	private APIResponseHandler(final AppCompatActivity context) {
		this.context = context;
	}

	protected abstract void onCallbackException(Throwable t);

	protected abstract void onFailure(
			@CacheRequest.RequestFailureType int type,
			Throwable t,
			Integer status,
			String readableMessage);

	protected abstract void onFailure(APIFailureType type);

	public final void notifyFailure(
			final @CacheRequest.RequestFailureType int type,
			final Throwable t,
			final Integer status,
			final String readableMessage) {
		try {
			onFailure(type, t, status, readableMessage);
		} catch(final Throwable t1) {
			try {
				onCallbackException(t1);
			} catch(final Throwable t2) {
				BugReportActivity.addGlobalError(new RRError(null, null, t1));
				BugReportActivity.handleGlobalError(context, t2);
			}
		}
	}

	public final void notifyFailure(final APIFailureType type) {
		try {
			onFailure(type);
		} catch(final Throwable t1) {
			try {
				onCallbackException(t1);
			} catch(final Throwable t2) {
				BugReportActivity.addGlobalError(new RRError(null, null, t1));
				BugReportActivity.handleGlobalError(context, t2);
			}
		}
	}

	public static abstract class ActionResponseHandler extends APIResponseHandler {

		protected ActionResponseHandler(final AppCompatActivity context) {
			super(context);
		}

		public final void notifySuccess(@Nullable final String redirectUrl) {
			try {
				onSuccess(redirectUrl);
			} catch(final Throwable t1) {
				try {
					onCallbackException(t1);
				} catch(final Throwable t2) {
					BugReportActivity.addGlobalError(new RRError(null, null, t1));
					BugReportActivity.handleGlobalError(context, t2);
				}
			}
		}

		protected abstract void onSuccess(@Nullable final String redirectUrl);
	}

	public static abstract class NewCaptchaResponseHandler extends APIResponseHandler {

		protected NewCaptchaResponseHandler(final AppCompatActivity context) {
			super(context);
		}

		public final void notifySuccess(final String captchaId) {
			try {
				onSuccess(captchaId);
			} catch(final Throwable t1) {
				try {
					onCallbackException(t1);
				} catch(final Throwable t2) {
					BugReportActivity.addGlobalError(new RRError(null, null, t1));
					BugReportActivity.handleGlobalError(context, t2);
				}
			}
		}

		protected abstract void onSuccess(String captchaId);
	}

	public static abstract class SubredditResponseHandler extends APIResponseHandler {

		protected SubredditResponseHandler(final AppCompatActivity context) {
			super(context);
		}

		public final void notifySuccess(
				final List<RedditSubreddit> result,
				final long timestamp) {
			try {
				onSuccess(result, timestamp);
			} catch(final Throwable t1) {
				try {
					onCallbackException(t1);
				} catch(final Throwable t2) {
					BugReportActivity.addGlobalError(new RRError(null, null, t1));
					BugReportActivity.handleGlobalError(context, t2);
				}
			}
		}

		public final void notifyDownloadNecessary() {
			try {
				onDownloadNecessary();
			} catch(final Throwable t1) {
				try {
					onCallbackException(t1);
				} catch(final Throwable t2) {
					BugReportActivity.addGlobalError(new RRError(null, null, t1));
					BugReportActivity.handleGlobalError(context, t2);
				}
			}
		}

		public final void notifyDownloadStarted() {
			try {
				onDownloadStarted();
			} catch(final Throwable t1) {
				try {
					onCallbackException(t1);
				} catch(final Throwable t2) {
					BugReportActivity.addGlobalError(new RRError(null, null, t1));
					BugReportActivity.handleGlobalError(context, t2);
				}
			}
		}

		protected abstract void onDownloadNecessary();

		protected abstract void onDownloadStarted();

		protected abstract void onSuccess(List<RedditSubreddit> result, long timestamp);
	}

	public static abstract class UserResponseHandler extends APIResponseHandler {

		protected UserResponseHandler(final AppCompatActivity context) {
			super(context);
		}

		public final void notifySuccess(final RedditUser result, final long timestamp) {
			try {
				onSuccess(result, timestamp);
			} catch(final Throwable t1) {
				try {
					onCallbackException(t1);
				} catch(final Throwable t2) {
					BugReportActivity.addGlobalError(new RRError(null, null, t1));
					BugReportActivity.handleGlobalError(context, t2);
				}
			}
		}

		public final void notifyDownloadStarted() {
			try {
				onDownloadStarted();
			} catch(final Throwable t1) {
				try {
					onCallbackException(t1);
				} catch(final Throwable t2) {
					BugReportActivity.addGlobalError(new RRError(null, null, t1));
					BugReportActivity.handleGlobalError(context, t2);
				}
			}
		}

		protected abstract void onDownloadStarted();

		protected abstract void onSuccess(RedditUser result, long timestamp);
	}
}
