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

package com.abielinski.marooned.Reddit.prepared.bodytext;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.abielinski.marooned.activities.BaseActivity;

public abstract class BodyElement {

	@NonNull private final BlockType mType;

	protected BodyElement(@NonNull final BlockType type) {
		mType = type;
	}

	@NonNull
	public final BlockType getType() {
		return mType;
	}

	public abstract View generateView(
			@NonNull BaseActivity activity,
			@Nullable Integer textColor,
			@Nullable Float textSize,
			boolean showLinkButtons);
}
