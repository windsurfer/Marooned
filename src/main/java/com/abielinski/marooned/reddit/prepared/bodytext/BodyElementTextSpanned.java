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

package com.abielinski.marooned.reddit.prepared.bodytext;

import android.text.Spanned;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.abielinski.marooned.activities.BaseActivity;
import com.abielinski.marooned.views.LinkifiedTextView;

public class BodyElementTextSpanned extends BodyElement {

	@NonNull private final Spanned mSpanned;

	public BodyElementTextSpanned(
			@NonNull final BlockType blockType,
			@NonNull final Spanned spanned) {
		super(blockType);
		mSpanned = spanned;
	}

	@Override
	public View generateView(
			@NonNull final BaseActivity activity,
			@Nullable final Integer textColor,
			@Nullable final Float textSize,
			final boolean showLinkButtons) {

		final LinkifiedTextView tv = new LinkifiedTextView(activity);

		if(textColor != null) {
			tv.setTextColor(textColor);
		}
		if(textSize != null) {
			tv.setTextSize(textSize);
		}

		tv.setText(mSpanned, LinkifiedTextView.BufferType.SPANNABLE);

		return tv;
	}
}
