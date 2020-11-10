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


import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import com.abielinski.marooned.jsonwrap.JsonBufferedObject;

public final class RedditPost implements Parcelable, RedditThingWithIdAndType {

	public String id, name;
	public String title, url, author, domain, subreddit, subreddit_id;
	public int num_comments, score, ups, downs, gilded, duration;
	public boolean archived, over_18, hidden, saved, is_self, clicked, stickied, is_gallery;
	public Object edited;
	public Boolean likes;
	public Boolean spoiler;

	public long created, created_utc;

	public String selftext, selftext_html, permalink, link_flair_text, author_flair_text;
	public String thumbnail; // an image URL

	public JsonBufferedObject media;
	public JsonBufferedObject preview;
	@Nullable public String rr_internal_dash_url;

	public int source_width, source_height;

	public RedditPost() {
	}

	@Nullable
	public String getDashUrl() {

		if(rr_internal_dash_url != null) {
			return rr_internal_dash_url;

		} else if(media != null) {
			try {
				rr_internal_dash_url = media.getObject("reddit_video")
						.getString("fallback_url");
			} catch(final Exception e) {
				rr_internal_dash_url = null;
			}
		}

		return rr_internal_dash_url;
	}

	public int getDuration() {

		if(duration != 0) {
			return duration;

		} else if(media != null) {
			try {
				duration = Integer.parseInt(media.getObject("reddit_video")
						.getString("duration"));
			} catch(final Exception e) {
				duration = 0;
			}
		}
		if(duration == 0 && preview != null){
			try {
				duration = Integer.parseInt(preview.getObject("reddit_video_preview")
						.getString("duration"));
			} catch(final Exception e) {
				duration = 0;
			}
		}

		return duration;
	}

	public int getSourceWidth(){
		if (source_width != 0){
			return source_width;
		}else if (preview != null){
			try {

				source_width = Integer.parseInt(preview
						.getArray("images")
						.get(0).asObject()
						.getObject("source")
						.getString("width"));
				return source_width;

			} catch(final Exception e) {
				source_width = 0;
			}
		}

		return 0;
	}

	public int getSourceHeight(){
		if (source_height != 0){
			return source_height;
		}else if (preview != null){
			try {

				source_height = Integer.parseInt(preview
						.getArray("images")
						.get(0).asObject()
						.getObject("source")
						.getString("height"));
				return source_height;

			} catch(final Exception e) {
				source_height = 0;
			}
		}

		return 0;
	}

	public String getUrl() {

		if(getDashUrl() != null) {
			return rr_internal_dash_url;
		}

		return url;
	}

	// one of the many reasons why the Android API is awful
	private RedditPost(final Parcel in) {
		id = in.readString();
		name = in.readString();
		title = in.readString();
		url = in.readString();
		author = in.readString();
		domain = in.readString();
		subreddit = in.readString();
		subreddit_id = in.readString();
		num_comments = in.readInt();
		score = in.readInt();
		ups = in.readInt();
		downs = in.readInt();
		gilded = in.readInt();
		duration = in.readInt();
		archived = in.readInt() == 1;
		over_18 = in.readInt() == 1;
		hidden = in.readInt() == 1;
		saved = in.readInt() == 1;
		is_self = in.readInt() == 1;
		clicked = in.readInt() == 1;
		stickied = in.readInt() == 1;
		is_gallery = in.readInt() == 1;

		final long in_edited = in.readLong();
		if(in_edited == -1) {
			edited = false;
		} else {
			edited = in_edited;
		}

		switch(in.readInt()) {
			case -1:
				likes = false;
				break;
			case 0:
				likes = null;
				break;
			case 1:
				likes = true;
				break;
		}

		created = in.readLong();
		created_utc = in.readLong();
		selftext = in.readString();
		selftext_html = in.readString();
		permalink = in.readString();
		link_flair_text = in.readString();
		author_flair_text = in.readString();
		thumbnail = in.readString();

		switch(in.readInt()) {
			case -1:
				spoiler = false;
				break;
			case 0:
				spoiler = null;
				break;
			case 1:
				spoiler = true;
				break;
		}

		rr_internal_dash_url = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel parcel, final int flags) {

		parcel.writeString(id);
		parcel.writeString(name);
		parcel.writeString(title);
		parcel.writeString(url);
		parcel.writeString(author);
		parcel.writeString(domain);
		parcel.writeString(subreddit);
		parcel.writeString(subreddit_id);
		parcel.writeInt(num_comments);
		parcel.writeInt(score);
		parcel.writeInt(ups);
		parcel.writeInt(downs);
		parcel.writeInt(gilded);
		parcel.writeInt(duration);
		parcel.writeInt(archived ? 1 : 0);
		parcel.writeInt(over_18 ? 1 : 0);
		parcel.writeInt(hidden ? 1 : 0);
		parcel.writeInt(saved ? 1 : 0);
		parcel.writeInt(is_self ? 1 : 0);
		parcel.writeInt(clicked ? 1 : 0);
		parcel.writeInt(stickied ? 1 : 0);
		parcel.writeInt(is_gallery ? 1 : 0);

		if(edited instanceof Long) {
			parcel.writeLong((Long)edited);
		} else {
			parcel.writeLong(-1);
		}

		if(likes == null) {
			parcel.writeInt(0);
		} else {
			parcel.writeInt(likes ? 1 : -1);
		}

		parcel.writeLong(created);
		parcel.writeLong(created_utc);
		parcel.writeString(selftext);
		parcel.writeString(selftext_html);
		parcel.writeString(permalink);
		parcel.writeString(link_flair_text);
		parcel.writeString(author_flair_text);
		parcel.writeString(thumbnail);

		if(spoiler == null) {
			parcel.writeInt(0);
		} else {
			parcel.writeInt(spoiler ? 1 : -1);
		}

		getDashUrl();
		parcel.writeString(rr_internal_dash_url);
		getDuration();
	}

	public static final Parcelable.Creator<RedditPost> CREATOR
			= new Parcelable.Creator<RedditPost>() {
		@Override
		public RedditPost createFromParcel(final Parcel in) {
			return new RedditPost(in);
		}

		@Override
		public RedditPost[] newArray(final int size) {
			return new RedditPost[size];
		}
	};

	@Override
	public String getIdAlone() {
		return id;
	}

	@Override
	public String getIdAndType() {
		return name;
	}
}
