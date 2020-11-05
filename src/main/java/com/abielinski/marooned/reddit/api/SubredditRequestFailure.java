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

import android.annotation.SuppressLint;
import android.content.Context;

import com.abielinski.marooned.cache.CacheRequest;
import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.RRError;

import java.net.URI;

public class SubredditRequestFailure {
	public final @CacheRequest.RequestFailureType int requestFailureType;
	public final Throwable t;
	public final Integer statusLine;
	public final String readableMessage;
	public final String url;

	public SubredditRequestFailure(
			@CacheRequest.RequestFailureType final int requestFailureType, final Throwable t,
			final Integer statusLine, final String readableMessage, final String url) {
		this.requestFailureType = requestFailureType;
		this.t = t;
		this.statusLine = statusLine;
		this.readableMessage = readableMessage;
		this.url = url;
	}

	public SubredditRequestFailure(
			@CacheRequest.RequestFailureType final int requestFailureType, final Throwable t,
			final Integer statusLine, final String readableMessage, final URI url) {
		this(
				requestFailureType,
				t,
				statusLine,
				readableMessage,
				url != null ? url.toString() : null);
	}

	@SuppressLint("WrongConstant")
	public RRError asError(final Context context) {
		return General.getGeneralErrorForFailure(
				context,
				requestFailureType,
				t,
				statusLine,
				url);
	}
}
