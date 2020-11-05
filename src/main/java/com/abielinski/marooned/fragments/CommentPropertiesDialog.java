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

import android.content.Context;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import org.apache.commons.text.StringEscapeUtils;
import com.abielinski.marooned.R;
import com.abielinski.marooned.activities.BaseActivity;
import com.abielinski.marooned.common.RRTime;
import com.abielinski.marooned.Reddit.things.RedditComment;

public final class CommentPropertiesDialog extends PropertiesDialog {

	public static CommentPropertiesDialog newInstance(final RedditComment comment) {

		final CommentPropertiesDialog pp = new CommentPropertiesDialog();

		final Bundle args = new Bundle();
		args.putParcelable("comment", comment);
		pp.setArguments(args);

		return pp;
	}

	@Override
	protected String getTitle(final Context context) {
		return context.getString(R.string.props_comment_title);
	}

	@Override
	protected void prepare(
			@NonNull final BaseActivity context,
			@NonNull final LinearLayout items) {

		final RedditComment comment = getArguments().getParcelable("comment");

		items.addView(propView(context, "ID", comment.name, true));

		items.addView(propView(context, R.string.props_author, comment.author, false));

		if(comment.author_flair_text != null && comment.author_flair_text.length() > 0) {
			items.addView(propView(
					context,
					R.string.props_author_flair,
					comment.author_flair_text,
					false));
		}

		items.addView(propView(
				context,
				R.string.props_created,
				RRTime.formatDateTime(comment.created_utc * 1000, context),
				false));

		if(comment.edited instanceof Long) {
			items.addView(propView(
					context,
					R.string.props_edited,
					RRTime.formatDateTime((Long)comment.edited * 1000, context),
					false));
		} else {
			items.addView(propView(
					context,
					R.string.props_edited,
					R.string.props_never,
					false));
		}

		items.addView(propView(
				context,
				R.string.props_score,
				String.valueOf(comment.ups - comment.downs),
				false));

		items.addView(propView(
				context,
				R.string.props_subreddit,
				comment.subreddit,
				false));

		if(comment.body != null && comment.body.length() > 0) {
			items.addView(propView(
					context,
					R.string.props_body_markdown,
					StringEscapeUtils.unescapeHtml4(comment.body),
					false));

			if(comment.body_html != null) {
				items.addView(propView(
						context,
						R.string.props_body_html,
						StringEscapeUtils.unescapeHtml4(comment.body_html),
						false));
			}
		}
	}
}
