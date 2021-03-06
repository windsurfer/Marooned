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

package com.abielinski.marooned.views;

import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.abielinski.marooned.activities.BaseActivity;
import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.PrefsUtility;
import com.abielinski.marooned.common.RRThemeAttributes;
import com.abielinski.marooned.reddit.prepared.RedditChangeDataManager;
import com.abielinski.marooned.reddit.prepared.RedditRenderableInboxItem;

public class RedditInboxItemView extends LinearLayout {

	private final View mDivider;
	private final TextView mHeader;
	private final FrameLayout mBodyHolder;

	private final RRThemeAttributes mTheme;

	private final boolean showLinkButtons;

	private RedditRenderableInboxItem currentItem = null;

	private final BaseActivity mActivity;

	public RedditInboxItemView(
			final BaseActivity activity,
			final RRThemeAttributes theme) {

		super(activity);

		mActivity = activity;
		mTheme = theme;

		setOrientation(VERTICAL);

		mDivider = new View(activity);
		mDivider.setBackgroundColor(Color.argb(128, 128, 128, 128)); // TODO better
		addView(mDivider);

		mDivider.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
		mDivider.getLayoutParams().height = 1;

		final LinearLayout inner = new LinearLayout(activity);
		inner.setOrientation(VERTICAL);

		mHeader = new TextView(activity);
		mHeader.setTextSize(11.0f * theme.rrCommentHeaderFontScale);
		mHeader.setTextColor(theme.rrCommentHeaderCol);
		inner.addView(mHeader);
		mHeader.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;

		mBodyHolder = new FrameLayout(activity);
		mBodyHolder.setPadding(0, General.dpToPixels(activity, 2), 0, 0);
		inner.addView(mBodyHolder);
		mBodyHolder.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;

		final int paddingPixels = General.dpToPixels(activity, 8.0f);
		inner.setPadding(
				paddingPixels + paddingPixels,
				paddingPixels,
				paddingPixels,
				paddingPixels);

		addView(inner);
		inner.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;

		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

		showLinkButtons = PrefsUtility.pref_appearance_linkbuttons(
				activity,
				General.getSharedPrefs(activity));

		setOnClickListener(v -> handleInboxClick(mActivity));

		setOnLongClickListener(v -> {
			handleInboxLongClick(mActivity);
			return true;
		});
	}

	public void reset(
			final BaseActivity context,
			final RedditChangeDataManager changeDataManager,
			final RRThemeAttributes theme,
			final RedditRenderableInboxItem item,
			final boolean showDividerAtTop) {

		currentItem = item;

		mDivider.setVisibility(showDividerAtTop ? VISIBLE : GONE);
		mHeader.setText(item.getHeader(theme, changeDataManager, context));

		final View body = item.getBody(
				context,
				mTheme.rrCommentBodyCol,
				13.0f * mTheme.rrCommentFontScale,
				showLinkButtons);

		mBodyHolder.removeAllViews();
		mBodyHolder.addView(body);
		body.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
	}

	public void handleInboxClick(final BaseActivity activity) {
		if(currentItem != null) {
			currentItem.handleInboxClick(activity);
		}
	}

	public void handleInboxLongClick(final BaseActivity activity) {
		if(currentItem != null) {
			currentItem.handleInboxLongClick(activity);
		}
	}
}
