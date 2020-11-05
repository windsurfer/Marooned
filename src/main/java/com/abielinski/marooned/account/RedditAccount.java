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

package com.abielinski.marooned.account;

import com.abielinski.marooned.common.StringUtils;
import com.abielinski.marooned.reddit.api.RedditOAuth;

public class RedditAccount {

	public final String username;
	public final RedditOAuth.RefreshToken refreshToken;
	public final boolean usesNewClientId;

	private RedditOAuth.AccessToken accessToken;

	public final long priority;

	public RedditAccount(
			final String username,
			final RedditOAuth.RefreshToken refreshToken,
			final boolean usesNewClientId,
			final long priority) {

		if(username == null) {
			throw new RuntimeException("Null user in RedditAccount");
		}

		this.username = username.trim();
		this.refreshToken = refreshToken;
		this.usesNewClientId = usesNewClientId;
		this.priority = priority;
	}

	public boolean isAnonymous() {
		return username.length() == 0;
	}

	public String getCanonicalUsername() {
		return StringUtils.asciiLowercase(username.trim());
	}

	public synchronized RedditOAuth.AccessToken getMostRecentAccessToken() {
		return accessToken;
	}

	public synchronized void setAccessToken(final RedditOAuth.AccessToken token) {
		accessToken = token;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof RedditAccount
				&& username.equalsIgnoreCase(((RedditAccount)o).username);
	}

	@Override
	public int hashCode() {
		return getCanonicalUsername().hashCode();
	}
}
