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

package com.abielinski.marooned.reddit.things;

import com.abielinski.marooned.jsonwrap.JsonBufferedArray;
import com.abielinski.marooned.jsonwrap.JsonValue;
import com.abielinski.marooned.reddit.url.PostCommentListingURL;
import com.abielinski.marooned.reddit.url.RedditURLParser;

import java.util.ArrayList;
import java.util.List;

public class RedditMoreComments {
	public int count;
	public JsonBufferedArray children;
	public String parent_id;

	public List<PostCommentListingURL> getMoreUrls(
			final RedditURLParser.RedditURL commentListingURL) {

		final ArrayList<PostCommentListingURL> urls = new ArrayList<>(16);

		if(commentListingURL.pathType() == RedditURLParser.POST_COMMENT_LISTING_URL) {

			if(count > 0) {
				for(final JsonValue child : children) {
					if(child.getType() == JsonValue.TYPE_STRING) {
						urls.add(commentListingURL.asPostCommentListURL()
								.commentId(child.asString()));
					}
				}

			} else {
				urls.add(commentListingURL.asPostCommentListURL().commentId(parent_id));
			}
		}

		return urls;
	}

	public int getCount() {
		return count;
	}
}
