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

package com.abielinski.marooned.adapters;

import android.content.Context;
import com.abielinski.marooned.reddit.RedditPostListItem;

import java.util.Collection;
import java.util.Collections;

public class PostListingManager extends RedditListingManager {

	private int mPostCount;

	public PostListingManager(final Context context) {
		super(context);
	}

	public void addPosts(final Collection<RedditPostListItem> posts) {
		addItems(Collections.<GroupedRecyclerViewAdapter.Item>unmodifiableCollection(posts));
		mPostCount += posts.size();
	}

	public int getPostCount() {
		return mPostCount;
	}
}
