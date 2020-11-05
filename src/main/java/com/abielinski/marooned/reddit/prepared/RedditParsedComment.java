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

package com.abielinski.marooned.reddit.prepared;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import org.apache.commons.text.StringEscapeUtils;
import com.abielinski.marooned.reddit.prepared.bodytext.BodyElement;
import com.abielinski.marooned.reddit.prepared.html.HtmlReader;
import com.abielinski.marooned.reddit.things.RedditComment;
import com.abielinski.marooned.reddit.things.RedditThingWithIdAndType;

public class RedditParsedComment implements RedditThingWithIdAndType {

	private final RedditComment mSrc;

	@NonNull private final BodyElement mBody;

	private final String mFlair;

	public RedditParsedComment(
			final RedditComment comment,
			final AppCompatActivity activity) {

		mSrc = comment;

		mBody = HtmlReader.parse(
				StringEscapeUtils.unescapeHtml4(comment.body_html),
				activity);

		if(comment.author_flair_text != null) {
			mFlair = StringEscapeUtils.unescapeHtml4(comment.author_flair_text);
		} else {
			mFlair = null;
		}
	}

	public BodyElement getBody() {
		return mBody;
	}

	public String getFlair() {
		return mFlair;
	}

	@Override
	public String getIdAlone() {
		return mSrc.getIdAlone();
	}

	@Override
	public String getIdAndType() {
		return mSrc.getIdAndType();
	}

	public RedditComment getRawComment() {
		return mSrc;
	}
}
